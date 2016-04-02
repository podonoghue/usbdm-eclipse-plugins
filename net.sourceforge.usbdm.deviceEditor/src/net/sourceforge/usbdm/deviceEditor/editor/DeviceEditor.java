package net.sourceforge.usbdm.deviceEditor.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
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
import net.sourceforge.usbdm.deviceEditor.model.ModelFactory;
import net.sourceforge.usbdm.deviceEditor.parser.WriteFamilyCpp;

public class DeviceEditor extends EditorPart {

   /** Path from which the data was loaded */
   private Path         fPath                = null;

   /** Factory containing all data */
   private ModelFactory fFactory             = null;

   /** Pin editor */
   private TreeEditor   fPinEditor           = null;
   
   /** Peripheral editor */
   private TreeEditor   fPeripheralEditor    = null;
   
   /** Folder containing all the tabs */
   private TabFolder    fTabFolder           = null;

   /** Actions to add to popup menus */
   ArrayList<MyAction>  popupActions = new ArrayList<MyAction>();

   private TreeEditor fPackageEditor;

   @Override
   public void init(IEditorSite editorSite, IEditorInput editorInput) throws PartInitException {
      super.setSite(editorSite);
      super.setInput(editorInput);
      
      System.err.println("DeviceEditor.init()");

      fFactory = null;
      IResource input = (IResource)editorInput.getAdapter(IResource.class);
      fPath = Paths.get(input.getLocation().toPortableString());
   }
   
   public void init(Path path) {
      System.err.println("DeviceEditor.init()");

      fFactory = null;
      fPath = path;
   }
   
   @Override
   public void setFocus() {
      fTabFolder.setFocus();
   }

//   ModelFactory createModels(Path path) throws Exception {
//      DeviceInfo fDeviceInfo = null;
//      if (path.getFileName().toString().endsWith("csv")) {
//         System.err.println("DeviceEditor(), Opening as CSV" + path);
//         ParseFamilyCSV parser = new ParseFamilyCSV();
//         fDeviceInfo = parser.parseFile(path);
//      }
//      else if ((path.getFileName().toString().endsWith("xml"))||(path.getFileName().toString().endsWith("hardware"))) {
//         System.err.println("DeviceEditor(), Opening as XML = " + path);
//         ParseFamilyXML parser = new ParseFamilyXML();
//         fDeviceInfo = parser.parseFile(path);
//      }
//      else {
//         throw new Exception("Unknown file type");
//      }
//      if (fDeviceInfo != null) {
//         fDeviceInfo.loadSettings();
//         fFactory  = new ModelFactory(fDeviceInfo);
//      }
//      return fFactory;
//   }
//   
   /**
    * Creates the editor pages.
    */
   @Override
   public void createPartControl(Composite parent) {

      fFactory = null;
      System.err.println("DeviceEditor()");
      
      try {
         System.err.println("DeviceEditor(), Input = " + fPath.toAbsolutePath());
         fFactory = ModelFactory.createModel(fPath, true);
      } catch (Exception e) {
         e.printStackTrace();
      }

      if (fFactory == null) {
         Label label = new Label(parent, SWT.NONE);
         label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
         label.setText("Failed to open/parse source file " + fPath);
         return;
         
      }
      
      // Create the containing tab folder
      fTabFolder = new TabFolder(parent, SWT.NONE);

      TabItem tabItem;

      // Pin view
      tabItem = new TabItem(fTabFolder, SWT.NONE);
      tabItem.setText(fFactory.getPackageModel().getName());
      tabItem.setToolTipText(fFactory.getPackageModel().getToolTip());       
      fPackageEditor = new TreeEditor();
      tabItem.setControl(fPackageEditor.createControls(fTabFolder).getControl());
      fPackageEditor.setModel(fFactory.getPackageModel());

      // Peripheral view
      tabItem = new TabItem(fTabFolder, SWT.NONE);
      tabItem.setText(fFactory.getPeripheralModel().getName());
      tabItem.setToolTipText(fFactory.getPeripheralModel().getToolTip());       
      fPeripheralEditor = new TreeEditor();
      tabItem.setControl(fPeripheralEditor.createControls(fTabFolder).getControl());
      fPeripheralEditor.setModel(fFactory.getPeripheralModel());

      // Pin view
      tabItem = new TabItem(fTabFolder, SWT.NONE);
      tabItem.setText(fFactory.getPinModel().getName());
      tabItem.setToolTipText(fFactory.getPinModel().getToolTip());       
      fPinEditor = new TreeEditor();
      tabItem.setControl(fPinEditor.createControls(fTabFolder).getControl());
      fPinEditor.setModel(fFactory.getPinModel());

//      // Create the actions
      makeActions();
      // Add selected actions to context menu
      hookContextMenu();
//      // Add selected actions to menu bar
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

   class GenerateCode extends MyAction {
      GenerateCode() {
         super("Regenerate Files", IAction.AS_PUSH_BUTTON, Activator.ID_WARNING_NODE_IMAGE);
      }

      @Override
      public void run() {
         WriteFamilyCpp writer = new WriteFamilyCpp();
         try {
            Path folder = fFactory.getDeviceInfo().getSourcePath().getParent().getParent();
            writer.writeCppFiles(folder, fFactory.getDeviceInfo(), "MK22F12-LQFP64");
         } catch (IOException e) {
            e.printStackTrace();
         }
         super.run();
      }
   }

   GenerateCode action1 = new GenerateCode();

   private void makeActions() {
      
      // These actions end up on the pop-up menu
      popupActions.add(new GenerateCode());
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
      Menu menu = menuMgr.createContextMenu(fPinEditor.getViewer().getControl());
      fPeripheralEditor.getViewer().getControl().setMenu(menu);
      fPinEditor.getViewer().getControl().setMenu(menu);
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
      manager.add(action1);
   }

   /**
    * Fill menu bar drop-down menu
    * 
    * @param manager
    */
   private void fillLocalPullDown(IMenuManager manager) {
      manager.add(action1);
   }

   @Override
   public void doSave(IProgressMonitor paramIProgressMonitor) {
      fFactory.getDeviceInfo().saveSettings();
   }

   @Override
   public void doSaveAs() {
   }

   @Override
   public boolean isDirty() {
      return true;
   }

   @Override
   public boolean isSaveAsAllowed() {
      return false;
   }

}
