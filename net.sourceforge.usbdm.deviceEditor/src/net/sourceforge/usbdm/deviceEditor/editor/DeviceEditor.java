package net.sourceforge.usbdm.deviceEditor.editor;

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
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.model.ModelFactory;
import net.sourceforge.usbdm.deviceEditor.parser.ParseFamilyCSV;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseFamilyXML;

public class DeviceEditor extends EditorPart {

   private TreeEditor   pinEditor               = null;
   private TreeEditor   peripheralEditor        = null;
   private ModelFactory factory                 = null;
   private TabFolder    fTabFolder              = null;


   @Override
   public void init(IEditorSite editorSite, IEditorInput editorInput) throws PartInitException {
      super.setSite(editorSite);
      super.setInput(editorInput);
   }
   
   @Override
   public void setFocus() {
      fTabFolder.setFocus();
   }

   ModelFactory createModels(Path path) throws Exception {
      DeviceInfo deviceInfo = null;
      if (path.getFileName().toString().endsWith("csv")) {
         System.err.println("DeviceEditor(), Opening as CSV" + path);
         ParseFamilyCSV parser = new ParseFamilyCSV();
         deviceInfo = parser.parseFile(path);
      }
      else if ((path.getFileName().toString().endsWith("xml"))||(path.getFileName().toString().endsWith("hardware"))) {
         System.err.println("DeviceEditor(), Opening as XML = " + path);
         ParseFamilyXML parser = new ParseFamilyXML();
         deviceInfo = parser.parseFile(path);
      }
      else {
         throw new Exception("Unknown file type");
      }
      if (deviceInfo != null) {
         factory  = new ModelFactory(deviceInfo);
      }
      return factory;
   }
   
   /**
    * Creates the editor pages.
    */
   @Override
   public void createPartControl(Composite parent) {

      IEditorInput editorInput = getEditorInput();

      factory = null;
      System.err.println("DeviceEditor()");
      IResource input = (IResource)editorInput.getAdapter(IResource.class);
      
      try {
         Path path = Paths.get(input.getLocation().toPortableString());
         System.err.println("DeviceEditor(), Input = " + path.toAbsolutePath());
         factory = ModelFactory.createModel(path);
      } catch (Exception e) {
         e.printStackTrace();
      }

      if (factory == null) {
         Label label = new Label(parent, SWT.NONE);
         label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
         label.setText("Failed to open/parse source file " + input.getLocation().toPortableString());
         return;
         
      }
      // Create the containing tab folder
      fTabFolder = new TabFolder(parent, SWT.NONE);

      TabItem one = new TabItem(fTabFolder, SWT.NONE);
      one.setText(factory.getPinModel().getName());
      one.setToolTipText(factory.getPinModel().getToolTip());       
      pinEditor = new TreeEditor();
      one.setControl(pinEditor.createControls(fTabFolder).getControl());
      pinEditor.setModel(factory.getPinModel());

      TabItem two = new TabItem(fTabFolder, SWT.NONE);
      two.setText(factory.getPeripheralModel().getName());
      two.setToolTipText(factory.getPeripheralModel().getToolTip());       
      peripheralEditor = new TreeEditor();
      two.setControl(peripheralEditor.createControls(fTabFolder).getControl());
      peripheralEditor.setModel(factory.getPeripheralModel());
      
//      // Create the actions
//      makeActions();
//      // Add selected actions to context menu
//      hookContextMenu();
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

   class Action1 extends MyAction {
      Action1() {
         super("XXX", IAction.AS_CHECK_BOX, Activator.ID_WARNING_NODE_IMAGE);
      }

      @Override
      public void run() {
         super.run();
      }
   }

   Action1 action1 = new Action1();

   /*
    * Actions to add to all menus
    */
   ArrayList<MyAction> myActions = new ArrayList<MyAction>();

   @SuppressWarnings("unused")
   private void makeActions() {
      System.err.println("makeActions()");
      // These actions end up on the drop-down and pop-up menus
      myActions.add(new Action1());
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
    * Add menu manager for right click pop-up menu
    */
   @SuppressWarnings("unused")
   private void hookContextMenu() {
      MenuManager menuMgr = new MenuManager("#PopupMenu");
      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(new IMenuListener() {
         public void menuAboutToShow(IMenuManager manager) {
            DeviceEditor.this.fillContextMenu(manager);
         }
      });
      Menu menu = menuMgr.createContextMenu(pinEditor.getViewer().getControl());
      pinEditor.getViewer().getControl().setMenu(menu);
   }

   /**
    * Fill Context menu
    * 
    * @param manager
    */
   private void fillContextMenu(IMenuManager manager) {
      manager.add(action1);
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
   }

   @Override
   public void doSaveAs() {
   }

   @Override
   public boolean isDirty() {
      return false;
   }

   @Override
   public boolean isSaveAsAllowed() {
      return false;
   }

}
