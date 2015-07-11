/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | Changes related to new device selection interface                                 | V4.10.6.250
===============================================================================================================
*/
package net.sourceforge.usbdm.peripherals.view;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.SVDIdentifier;
import net.sourceforge.usbdm.peripherals.model.DeviceModel;
import net.sourceforge.usbdm.peripherals.model.FieldModel;
import net.sourceforge.usbdm.peripherals.model.PeripheralModel;
import net.sourceforge.usbdm.peripherals.model.RegisterModel;
import net.sourceforge.usbdm.peripherals.model.UpdateInterface;
import net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

/**
 * @author podonoghue
 * 
 */
public class UsbdmDevicePeripheralsView extends ViewPart implements GdbSessionListener  {

   private static final String SETTINGS_SVD = "usbdmDevicePeripheralView.SVD";

   private SVDIdentifier svdId = null;
   
   public final static int NAME_COL           = 0;
   public final static int VALUE_COL          = 1;
   public final static int FIELD_INFO_COL     = 2;
   public final static int MODE_COL           = 3;
   public final static int LOCATION_COL       = 4;
   public final static int DESCRIPTION_COL    = 5;

   private final int defaultNameColumnWidth              = 200;
   private final int defaultValueColumnWidth             = 100;
   private final int defaultFieldColumnWidth             = 140;
   private       int defaultModeWidth                    = 0; // Should be final but has warning
   private       int defaultLocationColumnWidth          = 0; // Should be final but has warning
   private final int defaultDescriptionColumnWidth       = 300;

   private CheckboxTreeViewer peripheralsTreeViewer;
   private final String[] treeProperties = new String[] { "col1", "col2", "col3", "col4" };
   private PeripheralsInformationPanel peripheralsInformationPanel;

   private Action        filterPeripheralAction;
   private Action        setDeviceAction;
   private RefreshAction refreshAllAction;
   
   private IDialogSettings settings = null;
   
   @Override
   public void init(IViewSite site, IMemento memento) throws PartInitException {
      System.err.println("UsbdmDevicePeripheralsView.init()");
      super.init(site, memento);
      loadSettings();
   }

   private void loadSettings() {
      System.err.println("UsbdmDevicePeripheralsView.loadSettings()");
      if (settings != null) {
         String value = settings.get(SETTINGS_SVD);
         if (value != null) {
            System.err.println("UsbdmDevicePeripheralsView.createPartControl() svdId init = " + value);
            try {
               svdId = new SVDIdentifier(value);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }

   }
   private void saveSettings() {
      System.err.println("UsbdmDevicePeripheralsView.saveSettings()");
      if ((settings != null) && (svdId != null)) {
         System.err.println("UsbdmDevicePeripheralsView.createPartControl() svdId save = " + svdId);
         settings.put(SETTINGS_SVD, svdId.toString());
      }
   }

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
   
   /*
    * Actions to add to all menus
    */
   ArrayList<MyAction> myActions = new ArrayList<MyAction>();
   
   class HideShowColumnAction extends MyAction {
      final int columnNum;
      int lastWidth = 0;   // last displayed width
      
      /**
       * @param text          Test describing the action
       * @param columnNum     Number of column being manipulated
       * @param defaultWidth  Default column width
       */
      HideShowColumnAction(String text, int columnNum, int defaultWidth) {
         super(text, IAction.AS_CHECK_BOX, Activator.ID_SHOW_COLUMN_IMAGE);
         
         this.columnNum  = columnNum;
         this.lastWidth  = defaultWidth;
      }
      
      /**   
       *  Hide/Show Location column
       */
      public void run() {
         TreeColumn column = peripheralsTreeViewer.getTree().getColumn(columnNum);
         if (this.isChecked()) {
            if (lastWidth == 0) {
               column.pack();
            }
            else {
               column.setWidth(lastWidth);
            }
         }
         else {
            lastWidth = column.getWidth();
            column.setWidth(0);
         }
         column.setResizable(this.isChecked());
      }

      /**
       * @return the column
       */
      public int getColumn() {
         return columnNum;
      }
   }

   class RefreshAction extends MyAction {
      
      RefreshAction(String text, String toolTip) {
         super(text, toolTip, IAction.AS_PUSH_BUTTON, Activator.ID_REFRESH_IMAGE);
      }

      public void run() {
         Object[] visibleObjects = peripheralsTreeViewer.getVisibleExpandedElements();
         for (Object object : visibleObjects) {
            if (object instanceof PeripheralModel) {
               ((PeripheralModel) object).forceUpdate();
            }
         }
      }
   }
   
   class RefreshSelectionAction extends MyAction {
      
      RefreshSelectionAction(String text, String toolTip) {
         super(text, toolTip, IAction.AS_PUSH_BUTTON, Activator.ID_REFRESH_SELECTION_IMAGE);
      }

      public void run() {
         ISelection selection = peripheralsTreeViewer.getSelection();
         Object obj = ((IStructuredSelection) selection).getFirstElement();
         // System.err.println("Action1.run(), obj = " +
         // obj.toString()+", class=" + obj.getClass().toString());
         if (obj != null) {
            if (obj instanceof UpdateInterface) {
               ((UpdateInterface) obj).forceUpdate();
            }
         }
      }
   }
   
   private Action openFaultDialogue;

   private GdbDsfSessionListener gdbDsfSessionListener = null;
   private GdbMiSessionListener gdbMiSessionListener = null;

   // Current model being displayed
   private UsbdmDevicePeripheralsModel peripheralsModel = null;
   
   private static final String DEVICE_TOOLTIP_STRING = "Current device\nClick to change device";
   
   private LocalResourceManager resManager = null;
   private HashMap<String, Image> imageCache = new HashMap<String,Image>();

   /**
    * Testing constructor.
    */
   public UsbdmDevicePeripheralsView() {
      System.err.println("UsbdmDevicePeripheralsView()");
      settings = null;
      Activator activator = Activator.getDefault();
      if (activator != null) {
         settings = activator.getDialogSettings();
      }
      // Listen for DSF Sessions
      gdbDsfSessionListener = GdbDsfSessionListener.getListener();
      gdbDsfSessionListener.addListener(this);

      // Listen for MI Sessions
      gdbMiSessionListener = GdbMiSessionListener.getListener();
      gdbMiSessionListener.addListener(this);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.eclipse.ui.part.WorkbenchPart#dispose()
    */
   @Override
   public void dispose() {
      super.dispose();
      if (gdbDsfSessionListener != null) {
         gdbDsfSessionListener.removeListener(this);
      }
      if (gdbMiSessionListener != null) {
         gdbMiSessionListener.removeListener(this);
      }
   }

   public Image getMyImage(String imageId) {
      Image image = imageCache.get(imageId);
      if ((Activator.getDefault() != null) && (image == null)) {
         ImageDescriptor imageDescriptor  = Activator.getDefault().getImageDescriptor(imageId);
         image = resManager.createImage(imageDescriptor);
         imageCache.put(imageId, image);
      }
      return image;
   }
   
   /**
    * Provides the editor for the tree elements
    * 
    * Does minor modifications to the default editor.
    */
   private class PeripheralsViewTextCellEditor extends TextCellEditor {

      private int minHeight;

      public PeripheralsViewTextCellEditor(Tree tree) {
         super(tree, SWT.BORDER);
         Text txt = (Text) getControl();

         Font fnt = txt.getFont();
         FontData[] fontData = fnt.getFontData();
         if (fontData != null && fontData.length > 0) {
            minHeight = fontData[0].getHeight() + 10;
         }
      }

      public LayoutData getLayoutData() {
         LayoutData data = super.getLayoutData();
         if (minHeight > 0)
            data.minimumHeight = minHeight;
         return data;
      }
   }

   /**
    * Callback that creates the viewer and initialises it.
    * 
    * The View consists of a tree and a information panel
    */
   public void createPartControl(Composite parent) {
      // Create the manager and bind to main composite
      resManager = new LocalResourceManager(JFaceResources.getResources(), parent);

      parent.setLayoutData(new FillLayout());

      SashForm form = new SashForm(parent, SWT.VERTICAL);
      form.setLayout(new FillLayout());

      // Make sash visible
      form.setSashWidth(4);
      form.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));

      // =============================
      peripheralsTreeViewer = new CheckboxTreeViewer(form, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);

      Tree tree = peripheralsTreeViewer.getTree();
      tree.setLinesVisible(true);
      tree.setHeaderVisible(true);

      // Suppress tree expansion on double-click
      // see http://www.eclipse.org/forums/index.php/t/257325/
      peripheralsTreeViewer.getControl().addListener(SWT.MeasureItem, new Listener(){
         @Override
         public void handleEvent(Event event) {
         }});

      peripheralsTreeViewer.setColumnProperties(treeProperties);
      peripheralsTreeViewer.setCellEditors(new CellEditor[] { null, new PeripheralsViewTextCellEditor(peripheralsTreeViewer.getTree()), null });
      peripheralsTreeViewer.setCellModifier(new PeripheralsViewCellModifier(this));

      peripheralsTreeViewer.getControl().addListener(SWT.MeasureItem, new Listener() {
         @Override
         public void handleEvent(Event event) {

         }
      });

      /*
       * Name column
       */
      TreeColumn column;
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(defaultNameColumnWidth);
      column.setText("Name");

      // Add listener to column so peripherals are sorted by name when clicked
      column.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            peripheralsTreeViewer.setComparator(new PeripheralsViewSorter(PeripheralsViewSorter.SortCriteria.PeripheralNameOrder));
         }
      });

      /*
       * Value column
       */
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(defaultValueColumnWidth);
      column.setText("Value");
      column.setResizable(defaultValueColumnWidth!=0);

      /*
       * Field column
       */
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(defaultFieldColumnWidth );
      column.setText("Field");
      column.setResizable(defaultFieldColumnWidth!=0);

      /*
       * Mode column
       */
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(defaultModeWidth);
      column.setText("Mode");
      column.setResizable(defaultModeWidth!=0);

      /*
       * Location column
       */
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(defaultLocationColumnWidth);
      column.setText("Location");
      column.setResizable(defaultLocationColumnWidth!=0);

      // Add listener to column so peripheral are sorted by address when clicked
      column.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            peripheralsTreeViewer.setComparator(new PeripheralsViewSorter(PeripheralsViewSorter.SortCriteria.AddressOrder));
         }
      });

      /*
       * Description column
       */
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(defaultDescriptionColumnWidth);
      column.setText("Description");
      column.setResizable(defaultDescriptionColumnWidth!=0);

      // Default to sorted by Peripheral name
      peripheralsTreeViewer.setComparator(new PeripheralsViewSorter(PeripheralsViewSorter.SortCriteria.PeripheralNameOrder));

      // Noting filtered
      peripheralsTreeViewer.addFilter(new PeripheralsViewFilter(PeripheralsViewFilter.SelectionCriteria.SelectAll));

      // Label provider
      peripheralsTreeViewer.setLabelProvider(new PeripheralsViewCellLabelProvider(this));

      // Content provider
      peripheralsTreeViewer.setContentProvider(new PeripheralsViewContentProvider(this));

      // Tooltips doesn't work???? 
      //      ColumnViewerToolTipSupport.enableFor(peripheralsTreeViewer);

      TreeViewerEditor.create(peripheralsTreeViewer, new ColumnViewerEditorActivationStrategy(peripheralsTreeViewer) {
         protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
            return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION;
         }
      }, TreeViewerEditor.DEFAULT);

      // Create the help context id for the viewer's control
      // PlatformUI.getWorkbench().getHelpSystem().setHelp(treeViewer.getControl(),
      // "usbdmMemory.viewer");

      // =============================

      peripheralsInformationPanel = new PeripheralsInformationPanel(form, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY, this.peripheralsTreeViewer);
      form.setWeights(new int[] { 80, 20 });

      // So information panel is updated when selection changes
      peripheralsTreeViewer.addSelectionChangedListener(peripheralsInformationPanel);

      // Tree expansion/collapse
      peripheralsTreeViewer.addTreeListener(new ITreeViewerListener() {

         @Override
         public void treeExpanded(TreeExpansionEvent event) {
            Object element = event.getElement();
            //            System.err.println("treeExpanded() => event.getElement().getClass() = " + element.getClass());
            if (element instanceof RegisterModel) {
               ((RegisterModel)element).update();
            }
            if (element instanceof PeripheralModel) {
               ((PeripheralModel)element).update();
            }
         }

         @Override
         public void treeCollapsed(TreeExpansionEvent event) {
         }
      });
      // When user checks a checkbox in the tree, check all its children
      peripheralsTreeViewer.addCheckStateListener(new ICheckStateListener() {
         public void checkStateChanged(CheckStateChangedEvent event) {
//            peripheralsTreeViewer.expandToLevel(event.getElement(), 1);
            peripheralsTreeViewer.setSubtreeChecked(event.getElement(), event.getChecked());
         }
      });
      // Create the actions
      makeActions();
      // Add selected actions to context menu
      hookContextMenu();
      // Add selected actions to menu bar
      contributeToActionBars();
   }

   /**
    * Add menu manager for right click pop-up menu
    */
   private void hookContextMenu() {
      MenuManager menuMgr = new MenuManager("#PopupMenu");
      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(new IMenuListener() {
         public void menuAboutToShow(IMenuManager manager) {
            UsbdmDevicePeripheralsView.this.fillContextMenu(manager);
         }
      });
      Menu menu = menuMgr.createContextMenu(peripheralsTreeViewer.getControl());
      peripheralsTreeViewer.getControl().setMenu(menu);
   }

   /**
    *  Add selected actions to menu bar
    */
   private void contributeToActionBars() {
      IViewSite site = getViewSite();
      if (site == null) {
         return;
      }
      IActionBars bars = site.getActionBars();
      fillLocalPullDown(bars.getMenuManager());
      fillLocalToolBar(bars.getToolBarManager());
   }

   /**
    * Fill menu bar drop-down menu
    */
   private void fillLocalPullDown(IMenuManager manager) {
      for (Action action:myActions) {
         manager.add(action);
      }
      // Other plug-ins can contribute there actions here
      // manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
   }
   
   /**
    * Fill Context menu
    * @param manager
    */
   private void fillContextMenu(IMenuManager manager) {
      for (MyAction action:myActions) {
         if (action instanceof HideShowColumnAction) {
            int column = ((HideShowColumnAction)action).getColumn();
            int currentWidth = peripheralsTreeViewer.getTree().getColumn(column).getWidth();
            action.setChecked(currentWidth != 0);
         }
         manager.add(action);
      }
      // Other plug-ins can contribute there actions here
      // manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
   }

   /**
    * Fill menu bar
    * @param manager
    */
   private void fillLocalToolBar(IToolBarManager manager) {
      manager.add(setDeviceAction);
      manager.add(refreshAllAction);
      manager.add(filterPeripheralAction);
      manager.add(openFaultDialogue);
   }

   /**
    * Create the various actions for the menus etc.
    */
   private void makeActions() {
      // These actions end up on the drop-down and pop-up menus
      myActions.add(new HideShowColumnAction("Toggle field information column", FIELD_INFO_COL,   defaultFieldColumnWidth));
      myActions.add(new HideShowColumnAction("Toggle mode column",              MODE_COL,         defaultModeWidth));
      myActions.add(new HideShowColumnAction("Toggle location column",          LOCATION_COL,     defaultLocationColumnWidth));
      myActions.add(new HideShowColumnAction("Toggle description column",       DESCRIPTION_COL,  defaultDescriptionColumnWidth));
      myActions.add(new RefreshSelectionAction("Refresh selection", "Refreshes currently selected registers/peripheral from target"));
      refreshAllAction = new RefreshAction("Refresh all",    "Refreshes visible registers from target");

      /*
       * Refresh current selection action
       */
      openFaultDialogue = new Action() {
         public void run() {
            if (peripheralsModel == null) {
               return;
            }
            FaultDialogue dialogue = new FaultDialogue(getSite().getShell());
            dialogue.setTitleImage(Activator.getDefault().getImageDescriptor(Activator.ID_USBDM_IMAGE).createImage());
            dialogue.create(peripheralsModel.getGdbInterface());
            dialogue.open();
         }
      };
      openFaultDialogue.setText("Exception Report");
      openFaultDialogue.setToolTipText("Open Target Exception Report");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getDefault().getImageDescriptor(Activator.ID_EXCEPTION_IMAGE);
         openFaultDialogue.setImageDescriptor(imageDescriptor);
      }

      /*
       * Filter Registers action
       */
      filterPeripheralAction = new Action(null, IAction.AS_CHECK_BOX) {
         public void run() {
            peripheralsTreeViewer.setFilters(new ViewerFilter[] { new MyViewerFilter(filterPeripheralAction.isChecked()) });
         }
      };
      filterPeripheralAction.setText("Filter registers");
      filterPeripheralAction.setToolTipText("Only display checked registers");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getDefault().getImageDescriptor(Activator.ID_FILTER_IMAGE);
         filterPeripheralAction.setImageDescriptor(imageDescriptor);
      }

      /*
       * Select Device
       */
      setDeviceAction = new Action(null, IAction.AS_PUSH_BUTTON) {
         public void run() {
//            TargetType interfaceType = TargetType.T_UNKNOWN;
//            if ((peripheralsModel != null) && (peripheralsModel.getModel() != null)) {
//               // Choose the same device class as existing
//               interfaceType = peripheralsModel.getModel().getInterfaceType().toTargetType();
//            }
            DevicePeripheralSelectionDialogue dialogue = 
                  new DevicePeripheralSelectionDialogue(getSite().getShell(), svdId);
            if (dialogue.open() != Window.OK) {
               // Cancelled etc
               return;
            }
            svdId = dialogue.getSVDId();
            if (svdId != null) {
               setDeviceAction.setText(svdId.getDeviceName());
            }
            else {
               setDeviceAction.setText("Device...");
            }
            saveSettings();
            if ((peripheralsTreeViewer == null) || peripheralsTreeViewer.getControl().isDisposed()) {
               System.err.println("UsbdmDevicePeripheralsView.Action() - no peripheral view or already disposed()");
               return;
            }
            if (peripheralsModel == null) {
               // If there is no existing model we are finished (model will be created later)
               System.err.println("UsbdmDevicePeripheralsView.Action() - peripheralsModel == null");
               return;
            }
            System.err.println("UsbdmDevicePeripheralsView.Action() - Setting peripheral model");
            // Update model
            DevicePeripherals manuallySelectedPeripheralDescription = dialogue.getDevicePeripherals();
            try {
               peripheralsModel.setDevice(manuallySelectedPeripheralDescription);
            } catch (Exception e) {
               // Unable to generate model
               e.printStackTrace();
               MessageDialog.openError(getSite().getShell(), "Illegal device", "Device not found " + manuallySelectedPeripheralDescription);
               return;
            }
            peripheralsTreeViewer.setInput(peripheralsModel.getModel());
            ColumnViewerToolTipSupport.enableFor(peripheralsTreeViewer);

         }
      };
      if (svdId != null) {
         setDeviceAction.setText(svdId.getDeviceName());
      }
      else {
         setDeviceAction.setText("Device...");
      }
      setDeviceAction.setToolTipText(DEVICE_TOOLTIP_STRING);
   }
   
   /**
    * @author podonoghue
    */
   public class MyViewerFilter extends ViewerFilter {
      final boolean enabled;

      MyViewerFilter(boolean enabled) {
         this.enabled = enabled;
      }

      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
         // System.err.println("MyViewerFilter.select(), element.getClass() = "
         // + element.getClass());
         return !enabled || ((CheckboxTreeViewer) viewer).getChecked(element) || (element instanceof FieldModel);
      }
   }

   /**
    * Passing the focus request to the viewer's control.
    */
   public void setFocus() {
      peripheralsTreeViewer.getControl().setFocus();
   }

   @Override
   public void sessionStarted(final UsbdmDevicePeripheralsModel model) {
//      System.err.println(String.format("UsbdmDevicePeripheralsView.sessionStarted(%s)", (aPeripheralsModel == null) ? "null" : aPeripheralsModel.getDeviceName()));
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((peripheralsTreeViewer == null) || peripheralsTreeViewer.getControl().isDisposed()) {
               System.err.println("UsbdmDevicePeripheralsView.SessionCreate() - no peripheral view or already disposed()");
               return;
            }
            if (peripheralsModel != null) {
               // Model already set - ignore
               System.err.println("UsbdmDevicePeripheralsView.sessionStarted() - peripheralsModel already set");
               return;
            }
            peripheralsModel = model;
            if (peripheralsModel == null) {
               peripheralsTreeViewer.setInput(null);
               setDeviceAction.setText("No model");
            }
            else {
               // No device description (model) - use manually selected value
               if (peripheralsModel.getModel() == UsbdmDevicePeripheralsModel.NullDeviceModel) {
//                  System.err.println("UsbdmDevicePeripheralsView.sessionStarted() - Using default peripheral model");
                  try {
                     peripheralsModel.setDevice(svdId);
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
               else {
                  setDeviceAction.setEnabled(false);
                  setDeviceAction.setToolTipText("Auto-selected device");
               }
//               DeviceModel x = peripheralsModel.getModel();
//               System.err.println("UsbdmDevicePeripheralsView.sessionStarted() - Setting Model : " + x);
               peripheralsTreeViewer.setInput(peripheralsModel.getModel());
               setDeviceAction.setText(peripheralsModel.getDeviceName());
            }
         }
      });
   }

   @Override
   public void sessionTerminated(final UsbdmDevicePeripheralsModel model) {
      
//       System.err.println("UsbdmDevicePeripheralsView.SessionTerminate()");
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((peripheralsTreeViewer == null) || peripheralsTreeViewer.getControl().isDisposed()) {
//                System.err.println("UsbdmDevicePeripheralsView.SessionTerminate() - no peripheral view or already disposed()");
               return;
            }
            if (peripheralsModel == null) {
//               System.err.println("UsbdmDevicePeripheralsView.sessionTerminated() - periperalsModel == null");
               return;
            }
            if (peripheralsModel != model) {
//               System.err.println("UsbdmDevicePeripheralsView.sessionTerminated() - periperalsModel != aPeriperalsModel");
               return;
            }
            peripheralsTreeViewer.setInput(null);
            peripheralsModel = null;
            if (svdId != null) {
               setDeviceAction.setText(svdId.getDeviceName());
            }
            else { 
               setDeviceAction.setText("Device...");
            }
            setDeviceAction.setEnabled(true);
            setDeviceAction.setToolTipText(DEVICE_TOOLTIP_STRING);
         }
      });
   }

   @Override
   public void sessionSuspended(final UsbdmDevicePeripheralsModel model, GdbSessionListener.EventType reason) {
      if (model != null) {
         DeviceModel deviceModel = model.getModel();
         if (deviceModel != null) {
            // Set current register values as the 'reference' for changed values
            deviceModel.setChangeReference();
            // Set all registers as stale
            deviceModel.setNeedsUpdate(true);
         }
      }

//      System.err.println("UsbdmDevicePeripheralsView.SessionSuspend() - reason = " + reason);
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((peripheralsTreeViewer == null) || peripheralsTreeViewer.getControl().isDisposed()) {
//                System.err.println("UsbdmDevicePeripheralsView.SessionTerminate() - no peripheral view or already disposed()");
               return;
            }
            if (peripheralsModel == null) {
//               System.err.println("UsbdmDevicePeripheralsView.sessionTerminated() - periperalsModel == null");
               return;
            }
            if (peripheralsModel != model) {
//               System.err.println("UsbdmDevicePeripheralsView.sessionTerminated() - periperalsModel != aPeriperalsModel");
               return;
            }
            Object[] openNodes = peripheralsTreeViewer.getVisibleExpandedElements();
//            System.err.println("UsbdmDevicePeripheralsView.SessionSuspend() - openNodes.length = " + openNodes.length);
            for (Object node : openNodes) {
               if (node instanceof PeripheralModel) {
                  peripheralsTreeViewer.refresh(node);
               }
            }
            peripheralsInformationPanel.updateContent();
         }
      });
   }

   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      try {
         Display display = new Display();

         Shell shell = new Shell(display);
         shell.setText("Task List - TableViewer Example");
         shell.setLayout(new FillLayout());

         Composite composite = new Composite(shell, SWT.NONE);
         composite.setBackground(new Color(display, 255, 0, 0));
         composite.setLayout(new FillLayout());

         UsbdmDevicePeripheralsView view = new UsbdmDevicePeripheralsView();

         view.createPartControl(composite);
         Path                        path = Paths.get("C:/Users/podonoghue/Documents/Development/USBDM/usbdm-eclipse-makefiles-build/PackageFiles/DeviceData/Device.SVD/Internal/");
         SVDIdentifier               svdId = new SVDIdentifier(path.resolve("MK22F51212.svd.xml"));
         UsbdmDevicePeripheralsModel peripheralsModel = UsbdmDevicePeripheralsModel.createModel(null, svdId);

         //    peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MKL25Z4.svd.xml", null);
         //    peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MK20D5.svd.xml", null);
         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MK10D10.svd.xml", null);
         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MK11D5.svd.xml", null);
         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MK64F12.svd.xml", null);
         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MCF5225x.svd.xml", null);
         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MCF51JF.svd.xml", null);
         // Try illegal path/name
         //      peripheralsModel = new UsbdmDevicePeripheralsModel("xxxx", null);

         view.sessionStarted(peripheralsModel);

         shell.open();
         while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
               display.sleep();
         }
         display.dispose();
      } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   public PeripheralsInformationPanel getInformationPanel() {
      return peripheralsInformationPanel;
   }
}
