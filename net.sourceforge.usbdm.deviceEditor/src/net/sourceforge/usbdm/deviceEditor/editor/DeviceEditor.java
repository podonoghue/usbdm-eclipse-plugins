package net.sourceforge.usbdm.deviceEditor.editor;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import net.sourceforge.usbdm.deviceEditor.Activator;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.model.EditorPage;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ModelFactory;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralPageModel;

public class DeviceEditor extends EditorPart implements IModelChangeListener {

   /** Path from which the data was loaded */
   private Path         fPath                = null;

   /** Factory containing all data */
   private ModelFactory fFactory             = null;

   /** Folder containing all the tabs */
   private CTabFolder    fTabFolder          = null;

   private EditorPage[] fEditors             = null;

   /** Actions to add to pop-up menus */
   ArrayList<MyAction>  popupActions         = new ArrayList<MyAction>();

   /** Associated project */
   private IProject fProject                 = null;

   /** Eclipse status line manager */
   IStatusLineManager fStatusLineManager     = null;
   
   @Override
   public void init(IEditorSite editorSite, IEditorInput editorInput) throws PartInitException {
      super.setSite(editorSite);
      super.setInput(editorInput);

      fFactory = null;
      IResource input = (IResource)editorInput.getAdapter(IResource.class);
      fProject = input.getProject();
      fPath = Paths.get(input.getLocation().toPortableString());

      setPartName(input.getName());
      IActionBars bars = getEditorSite().getActionBars();
      fStatusLineManager = bars.getStatusLineManager();
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
   private void refreshModels() {
      ArrayList<PeripheralPageModel> models = fFactory.getModels();
      int index = 0;
      for (EditorPage page:fEditors) {
         page.update(models.get(index++));
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
      Display display = Display.getCurrent();
      // Create the containing tab folder
      fTabFolder   = new CTabFolder(parent, SWT.NONE);
      fTabFolder.setSimple(false);
      fTabFolder.setBackground(new Color[]{
            display.getSystemColor(SWT.COLOR_WHITE),
            display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT)}, 
            new int[]{100}, true);
      fTabFolder.setSelectionBackground(new Color[]{
            display.getSystemColor(SWT.COLOR_WHITE),
            display.getSystemColor(SWT.COLOR_WHITE)}, 
            new int[]{100}, true);

      ArrayList<EditorPage> editors = new ArrayList<EditorPage>();
      for (PeripheralPageModel model:fFactory.getModels()) {
         // Pin view
         CTabItem tabItem;
         tabItem = new CTabItem(fTabFolder, SWT.NONE);
         tabItem.setText(model.getName());
         tabItem.setToolTipText(model.getToolTip());
         EditorPage editorPage = model.createEditorPage();
         editors.add(editorPage);
         tabItem.setControl(editorPage.createComposite(fTabFolder));
      }
      fEditors = editors.toArray(new EditorPage[editors.size()]);

      refreshModels();

      fFactory.addListener(this);

      // Create the actions
      makeActions();
      // Add selected actions to context menu
      hookContextMenu();
      // Add selected actions to menu bar
      //      contributeToActionBars();

      try {
         Activator activator = Activator.getDefault();
         if (activator != null) {
            IDialogSettings dialogSettings = activator.getDialogSettings();
            if (dialogSettings != null) {
               fTabFolder.setSelection(dialogSettings.getInt("ActiveTab"));
            }
         }
      } catch (NumberFormatException e) {
      }
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

   /**
    * Generate C code files
    */
   public void generateCode() {
      if (fFactory == null) {
         MessageDialog.openError(null, "Failed", "Regenerated code files failed");
         return;
      }
      try {
         if (fProject != null) {
            // Opened as part of a Eclipse project
            fFactory.getDeviceInfo().generateCppFiles(fProject, new NullProgressMonitor());
         }
         else {
            // Used for testing
            fFactory.getDeviceInfo().generateCppFiles();
         }
         if (fStatusLineManager != null) {
            fStatusLineManager.setMessage("File generation complete");
         }
         else {
            MessageDialog.openInformation(null, "Regenerated Code", "Regenerated all code files");
         }
      } catch (Exception e) {
         e.printStackTrace();
         MessageDialog.openError(null, "Regenerate Code Failed", "Failed to regenerate code.\nReason:\n"+e.getMessage());
      }
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
   public void doSaveAs() {  
      FileDialog dialog = new FileDialog(getSite().getShell(), SWT.SAVE);
      dialog.setFilterExtensions(new String [] {"*"+DeviceInfo.PROJECT_FILE_EXTENSION});
      dialog.setFilterPath(fPath.getParent().toString());
      String result = dialog.open();
      if (result != null) {
         DeviceInfo deviceInfo = fFactory.getDeviceInfo();
         if (deviceInfo == null) {
            return;
         }
         Path path = FileSystems.getDefault().getPath(result);
         deviceInfo.saveSettingsAs(path, fProject);
      }
   }   

   @Override
   public void doSave(IProgressMonitor paramIProgressMonitor) {
      if (fFactory == null) {
         return;
      }
      DeviceInfo deviceInfo = fFactory.getDeviceInfo();
      if (deviceInfo == null) {
         return;
      }
      deviceInfo.saveSettings(fProject);
      Activator activator = Activator.getDefault();
      if (activator != null) {
         IDialogSettings dialogSettings = activator.getDialogSettings();
         if (dialogSettings != null) {
            dialogSettings.put("ActiveTab", fTabFolder.getSelectionIndex());
         }
      }
   }

   @Override
   public boolean isDirty() {
      return (fFactory != null) && (fFactory.getDeviceInfo().isDirty());
   }


   @Override
   public boolean isSaveAsAllowed() {
      return true;
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
         refreshModels();
      }
      firePropertyChange(PROP_DIRTY);      
   }

   DeviceEditorOutlinePage fOutlinePage = null;
   
   @SuppressWarnings("rawtypes")
   @Override
   public Object getAdapter(Class required) {      
      if (IContentOutlinePage.class.equals(required)) {
         if (fOutlinePage == null) {
            fOutlinePage = new DeviceEditorOutlinePage(fFactory, this);
            fOutlinePage.setInput(getEditorInput());
         }
         return fOutlinePage;
      }
      return super.getAdapter(required);
   }

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
      // TODO Auto-generated method stub
      
   }

}
