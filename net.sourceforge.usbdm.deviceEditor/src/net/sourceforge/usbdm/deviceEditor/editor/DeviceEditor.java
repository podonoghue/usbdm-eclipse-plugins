package net.sourceforge.usbdm.deviceEditor.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import net.sourceforge.usbdm.deviceEditor.Activator;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ModelFactory;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.RootModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp;

public class DeviceEditor extends EditorPart implements IModelChangeListener {

   /** Path from which the data was loaded */
   private Path         fPath                = null;

   /** Factory containing all data */
   private ModelFactory fFactory             = null;

   /** Folder containing all the tabs */
   private TabFolder    fTabFolder           = null;

   private TreeEditor[] fTreeEditors         = null;
   

   /** Actions to add to popup menus */
   ArrayList<MyAction>  popupActions = new ArrayList<MyAction>();

   /** Associated project */
   private IProject fProject = null;

   @Override
   public void init(IEditorSite editorSite, IEditorInput editorInput) throws PartInitException {
      super.setSite(editorSite);
      super.setInput(editorInput);
      
      fFactory = null;
      IResource input = (IResource)editorInput.getAdapter(IResource.class);
      fProject = input.getProject();
      fPath = Paths.get(input.getLocation().toPortableString());
   }
   
   /** Initialise the editor for testing */
   public void init(Path path) {
      fFactory = null;
      fPath = path;
   }
   
   @Override
   public void setFocus() {
      if (fTabFolder != null) {
         fTabFolder.setFocus();
      }
   }

   /**
    * Set the models for page 1..N<br>
    * Page 0 is assumed unchanged
    * 
    */
   private void setModels() {
      ArrayList<RootModel> models = fFactory.getModels();
      for (int index=1; index<models.size(); index++) {
         fTreeEditors[index].setModel(models.get(index));
      }
   }
   
   /**
    * Creates the editor pages.
    */
   @Override
   public void createPartControl(Composite parent) {

      fFactory = null;
      
      String failureReason = "Unknown";
      try {
         fFactory = ModelFactory.createModels(fPath, true);
      } catch (Exception e) {
         failureReason = "Failed to create editor content for '"+fPath+"'.\nReason: "+e.getMessage();
         System.err.println(failureReason);
         e.printStackTrace();
      }

      if (fFactory == null) {
         Label label = new Label(parent, SWT.NONE);
         label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
         label.setText(failureReason);
         return;
         
      }
      
      // Create the containing tab folder
      fTabFolder   = new TabFolder(parent, SWT.NONE);
      ArrayList<TreeEditor> treeEditors = new ArrayList<TreeEditor>();

      for (RootModel model:fFactory.getModels()) {
         // Pin view
         TabItem tabItem;
         tabItem = new TabItem(fTabFolder, SWT.NONE);
         tabItem.setText(model.getName());
         tabItem.setToolTipText(model.getToolTip());       
         TreeEditor treeEditor = new TreeEditor();
         treeEditors.add(treeEditor);
         tabItem.setControl(treeEditor.createControls(fTabFolder).getControl());
      }
      fTreeEditors = treeEditors.toArray(new TreeEditor[treeEditors.size()]);

      fTreeEditors[0].setModel(fFactory.getModels().get(0));
      setModels();
      
      fFactory.addListener(this);
      
      // Create the actions
      makeActions();
      // Add selected actions to context menu
      hookContextMenu();
      // Add selected actions to menu bar
//      contributeToActionBars();
   }

   //   @Override
   //   protected void pageChange(int newPageIndex) {
   //      super.pageChange(newPageIndex);
   //      IEditorPart editorPart = getEditor(newPageIndex);
   //      if (editorPart instanceof TreeEditor) {
   //         ((TreeEditor)editorPart).refresh();
   //      }
   //   }

   /** Convenience wrapper for Action */
   class MyAction extends Action {

      MyAction(String text, String toolTip, int style, String imageId) {
         super(text, style);

         setText(text);
         setToolTipText(toolTip);
         if ((imageId!= null) && (Activator.getDefault() != null)) {
            ImageDescriptor imageDescriptor = Activator.getDefault().getImageDescriptor(imageId);
            setImageDescriptor(imageDescriptor);
         }
      }

      MyAction(String text,int style, String imageId) {
         this(text, text, style, imageId);
      }

      MyAction(String text,int style) {
         this(text, text, style, null);
      }
   }

   public void generateCode() {
      if (fFactory == null) {
         MessageDialog.openError(null, "Failed", "Regenerated code files failed");
         return;
      }
      WriteFamilyCpp writer = new WriteFamilyCpp();
      try {
         if (fProject != null) {
            writer.writeCppFiles(fProject, fFactory.getDeviceInfo(), new NullProgressMonitor());
         }
         else {
            Path folder = fFactory.getDeviceInfo().getSourcePath().getParent().getParent();
            writer.writeCppFiles(folder, "", fFactory.getDeviceInfo());
            if (fProject != null) {
               fProject.refreshLocal(IResource.DEPTH_INFINITE, null);
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      } catch (CoreException e) {
         e.printStackTrace();
      }
      MessageDialog.openInformation(null, "Regenerated Code", "Regenerated all code files");

   }
   
   class GenerateCodeAction extends MyAction {
      GenerateCodeAction() {
         super("Regenerate Files", IAction.AS_PUSH_BUTTON, Activator.ID_WARNING_NODE_IMAGE);
      }

      @Override
      public void run() {
         generateCode();
      }
   }

   MyAction fActions[] = {new GenerateCodeAction()};
   
   /** 
    * Create menu actions
    */
   private void makeActions() {
      
      // These actions end up on the pop-up menu
      for(MyAction action:fActions) {
         popupActions.add(action);
      }
   }

   /**
    * Add menu manager for right click pop-up menu
    */
   private void hookContextMenu() {
      MenuManager menuMgr = new MenuManager("#PopupMenu");
      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(new IMenuListener() {
         public void menuAboutToShow(IMenuManager manager) {
            // Dynamically fill context menu
            DeviceEditor.this.fillContextMenu(manager);
         }
      });
      Menu menu = menuMgr.createContextMenu(fTabFolder);
      for (Control c:fTabFolder.getChildren()) {
         c.setMenu(menu);
      }
   }

   /**
    * Dynamically fill context menu
    * 
    * @param manager
    */
   private void fillContextMenu(IMenuManager manager) {
      for (MyAction action:popupActions) {
         manager.add(action);
      }
   }

   @SuppressWarnings("unused")
   private void contributeToActionBars() {
      IEditorSite site = getEditorSite();
      if (site == null) {
         System.err.println("site is null");
         return;
      }
      IActionBars bars = site.getActionBars();
      fillLocalPullDown(bars.getMenuManager());
      fillLocalToolBar(bars.getToolBarManager());
   }

   /**
    * Fill menu bar
    * 
    * @param manager
    */
   private void fillLocalToolBar(IToolBarManager manager) {
      for(MyAction action:fActions) {
         manager.add(action);
      }
   }

   /**
    * Fill menu bar drop-down menu
    * 
    * @param manager
    */
   private void fillLocalPullDown(IMenuManager manager) {
      for(MyAction action:fActions) {
         manager.add(action);
      }
   }

   @Override
   public void doSave(IProgressMonitor paramIProgressMonitor) {
      DeviceInfo deviceInfo = fFactory.getDeviceInfo();
      if (deviceInfo == null) {
         return;
      }
      deviceInfo.saveSettings();
      try {
         if (fProject != null) {
            IFolder settingsFolder = fProject.getFolder("usbdm");
            if (settingsFolder.exists()) {
               settingsFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
            }
         }
      } catch (CoreException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void doSaveAs() {
   }

   @Override
   public boolean isDirty() {
      return (fFactory != null) && (fFactory.getDeviceInfo().isDirty());
   }

   
   @Override
   public boolean isSaveAsAllowed() {
      return false;
   }

   /** Used when the models have been re-generated */
   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model == fFactory) {
         firePropertyChange(PROP_DIRTY);      
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel model) {
      if (model == fFactory) {
         setModels();
      }
      firePropertyChange(PROP_DIRTY);      
   }

}
