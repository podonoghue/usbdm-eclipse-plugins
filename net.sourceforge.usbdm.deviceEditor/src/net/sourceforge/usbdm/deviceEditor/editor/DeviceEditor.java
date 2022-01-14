package net.sourceforge.usbdm.deviceEditor.editor;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.index.IndexerSetupParticipant;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
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
import net.sourceforge.usbdm.deviceEditor.model.IEditorPage;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.IPage;
import net.sourceforge.usbdm.deviceEditor.model.ModelFactory;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;

public class DeviceEditor extends EditorPart implements IModelChangeListener {

   private final String ActiveTab_key           = "ActiveTab";
   private final String PeripheralTabNumber_key = "PeripheralTabNumber";
   
   /** Path from which the data was loaded */
   private Path         fPath                = null;

   /** Factory containing all data */
   private ModelFactory fFactory             = null;

   /** Folder containing all the tabs */
   private CTabFolder    fTabFolder          = null;

   /** Folder containing all the peripheral parameters */
   private CTabFolder    fPeripheralParametersFolder = null;
   
   /** Editors for each page */
   private IEditorPage[] fEditors            = null;

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
      ArrayList<IPage> models = fFactory.getModels();
      int index = 0;
      for (IEditorPage page:fEditors) {
         page.update(models.get(index++));
      }
   }

   /**
    * Creates the editor page.
    */
   @Override
   public void createPartControl(final Composite parent) {

      parent.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(DisposeEvent arg0) {

            Activator activator = Activator.getDefault();
            if (activator != null) {
               IDialogSettings dialogSettings = activator.getDialogSettings();
               if (dialogSettings != null) {
                  if (fTabFolder != null)  {
                     dialogSettings.put(ActiveTab_key,           fTabFolder.getSelectionIndex());
                  }
                  if (fPeripheralParametersFolder != null) {
                     dialogSettings.put(PeripheralTabNumber_key, fPeripheralParametersFolder.getSelectionIndex());
                  }
               }
            }
         }
      });
      
      final Display display = Display.getCurrent();

      final Label label = new Label(parent, SWT.NONE);
//      label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      label.setText("Reading configuration");

      FontDescriptor boldDescriptor = FontDescriptor.createFrom(label.getFont()).setStyle(SWT.BOLD).setHeight(15);
      Font boldFont = boldDescriptor.createFont(label.getDisplay());
      label.setFont( boldFont );
      label.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(DisposeEvent arg0) {
            boldFont.dispose();
         }
      });
      
      // Populate pages in background
      Job job = Job.create("Populating editor", new IJobFunction() {
         @Override
         public IStatus run(IProgressMonitor arg0) {
            createPartControlJob(display, parent, label);
            return Status.OK_STATUS;
         }
      });
      job.setUser(true);
      job.schedule();
   }

   /**
    * Creates the editor pages.
    * @param display 
    * @param label 
    */
   public void createPartControlJob(final Display display, final Composite parent, final Label status) {

      final DeviceEditor editor = this;
      fFactory = null;

      String failureReason = "Unknown";
      try {
         // Get page data
         fFactory = ModelFactory.createModels(fPath, true);
      } catch (Exception e) {
         failureReason = "Failed to create editor content for '"+fPath+"'.\nReason: "+e.getMessage();
         Activator.logError(failureReason, e);
      }
      
      if (fFactory == null) {
         final String reason = failureReason;
         display.asyncExec(new Runnable() {
            @Override
            public void run() {
               // Remove status message form parent
               status.setText(reason);
               parent.layout();
               parent.redraw();
            }
         });
         return;
      }

      display.asyncExec(new Runnable() {
         @Override
         public void run() {

            fFactory.addListener(editor);
            
            status.dispose();
            
            // Create the containing tab folder
            fTabFolder = new CTabFolder(parent, SWT.NONE);
            fTabFolder.setSimple(false);
            fTabFolder.setBorderVisible(true);
            fTabFolder.setBackground(new Color[]{
                  display.getSystemColor(SWT.COLOR_WHITE),
                  display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT)}, 
                  new int[]{100}, true);
            fTabFolder.setSelectionBackground(new Color[]{
                  display.getSystemColor(SWT.COLOR_WHITE),
                  display.getSystemColor(SWT.COLOR_WHITE)}, 
                  new int[]{100}, true);

            fPeripheralParametersFolder = null;
            
            ArrayList<IEditorPage> editors = new ArrayList<IEditorPage>();
            for (IPage page:fFactory.getModels()) {
               // Pin view
               CTabItem tabItem;
               tabItem = new CTabItem(fTabFolder, SWT.NONE);
               tabItem.setText(page.getName());
               IEditorPage editorPage = page.createEditorPage();
               editors.add(editorPage);
               tabItem.setControl(editorPage.createComposite(fTabFolder));
               tabItem.setToolTipText(page.getToolTip());
               if (page.getName().equalsIgnoreCase("Peripheral Parameters")) {
                  fPeripheralParametersFolder = (CTabFolder) tabItem.getControl();
               }
            }
            fEditors = editors.toArray(new IEditorPage[editors.size()]);
            parent.layout();
            parent.redraw();
            refreshModels();
            fTabFolder.setSelection(0);

            // Create the actions
            makeActions();
            // Add selected actions to context menu
            hookContextMenu();
            // Add selected actions to menu bar
            //      contributeToActionBars();

            int tabNum           = fTabFolder.getItemCount()-1;
            int peripheralTabNum = 0;
            Activator activator = Activator.getDefault();
            if (activator != null) {
               IDialogSettings dialogSettings = activator.getDialogSettings();
               if (dialogSettings != null) {
                  try {
                     tabNum              = dialogSettings.getInt(ActiveTab_key);
                     peripheralTabNum = dialogSettings.getInt(PeripheralTabNumber_key);
                  } catch (NumberFormatException e) {
                  }
               }
            }
            if (tabNum>=fTabFolder.getItemCount()) {
               tabNum = 0;
            }
            if (peripheralTabNum>=fPeripheralParametersFolder.getItemCount()) {
               peripheralTabNum = 0;
            }
            fTabFolder.setSelection(tabNum);
            fTabFolder.layout();
            fPeripheralParametersFolder.setSelection(peripheralTabNum);
         }
      });
      return;
   }

   /**
    * Used to suppress C indexing
    */
   static class MyIndexerSetupParticipant extends IndexerSetupParticipant {
      IProject fProject;

      MyIndexerSetupParticipant(IProject project) {
         fProject = project;
      }
      @Override
      public boolean postponeIndexerSetup(ICProject cProject) {
         IProject project = cProject.getProject() ;
         return project == fProject;
      }
   }

   /**
    * Generate C code files
    * @return 
    */
   public void generateCode() {

      Job job = new Job("Regenerate code files") {

         protected synchronized IStatus run(IProgressMonitor monitor) {
            SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

            if (fFactory == null) {
               return new Status(IStatus.ERROR, Activator.getPluginId(), "No factory open");
            }
            //            MyIndexerSetupParticipant indexerParticipant= null;
            try {
               subMonitor.setTaskName("Regenerate project files");
               subMonitor.subTask("Starting...");
               if (fProject != null) {
                  // Regenerate files
                  fFactory.getDeviceInfo().generateCppFiles(fProject, false, subMonitor.newChild(20));

                  // Refresh project
                  subMonitor.subTask("Refreshing files...");
                  fProject.refreshLocal(IResource.DEPTH_INFINITE, subMonitor.newChild(20));

                  final IIndexManager indexManager = CCorePlugin.getIndexManager();
                  final ICProject cProject = CoreModel.getDefault().create(fProject);
                  subMonitor.subTask("Refreshing Index...");
                  indexManager.reindex(cProject);
                  indexManager.joinIndexer(IIndexManager.FOREVER, subMonitor.newChild(60)); 
               }
               else {
                  // Used for testing
                  fFactory.getDeviceInfo().generateCppFiles();
               }
            } catch (Exception e) {
               Activator.logError(e.getMessage(), e);
               return new Status(IStatus.ERROR, Activator.getPluginId(), e.toString(), e);
            } finally {
               monitor.done();
            }
            return Status.OK_STATUS;
         }
      };
      job.setUser(true);
      job.schedule();
   }

   class GenerateCodeAction extends MyAction {
      GenerateCodeAction() {
         super("Regenerate Files", IAction.AS_PUSH_BUTTON, Activator.ID_GEN_FILES_IMAGE);
      }

      @Override
      public void run() {
         generateCode();
      }
   }

   private GenerateCodeAction generateCodeAction = new GenerateCodeAction();

   MyAction fActions[] = {generateCodeAction};

   Action getGenerateCodeAction() {
      return generateCodeAction;
   }

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
   public void doSave(IProgressMonitor monitor) {
      SubMonitor.convert(monitor, 100);

      if (fFactory == null) {
         return;
      }
      DeviceInfo deviceInfo = fFactory.getDeviceInfo();
      if (deviceInfo == null) {
         return;
      }
      deviceInfo.saveSettings(fProject);
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

   
//   @Override
//   public void dispose() {
//      Activator activator = Activator.getDefault();
//      if (activator != null) {
//         IDialogSettings dialogSettings = activator.getDialogSettings();
//         if (dialogSettings != null) {
//            dialogSettings.put(ActiveTab_key,           fTabFolder.getSelectionIndex());
//            dialogSettings.put(PeripheralTabNumber_key, fPeripheralParametersFolder.getSelectionIndex());
//         }
//      }
//      super.dispose();
//   }

   @Override
   public <T> T getAdapter(Class<T> adapter) {
      if (IContentOutlinePage.class.equals(adapter)) {
         if (fOutlinePage == null) {
            fOutlinePage = new DeviceEditorOutlinePage(fFactory, this);
            fOutlinePage.setInput(getEditorInput());
         }
         return adapter.cast(fOutlinePage);
      }
      return super.getAdapter(adapter);
   }

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
   }

}
