/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | Changes related to new device selection interface                                 | V4.10.6.250
===============================================================================================================
 */
package net.sourceforge.usbdm.peripherals.view;

import java.util.ArrayList;
import java.util.HashMap;

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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import net.sourceforge.usbdm.peripheralDatabase.SVDIdentifier;
import net.sourceforge.usbdm.peripherals.model.DeviceModel;
import net.sourceforge.usbdm.peripherals.model.FieldModel;
import net.sourceforge.usbdm.peripherals.model.PeripheralModel;
import net.sourceforge.usbdm.peripherals.model.RegisterModel;
import net.sourceforge.usbdm.peripherals.model.UpdateInterface;
import net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel;

/**
 * @author podonoghue
 * 
 */
public class UsbdmDevicePeripheralsView extends ViewPart implements GdbSessionListener  {

   private static final String SETTINGS_SVD = "usbdmDevicePeripheralView.SVD";

   /**
    * SVDID of manually selected device.<br>
    * Not used if device obtained from DSF session
    */
   private SVDIdentifier fManuallySelectedSvdId = null;

   public final static int NAME_COL           = 0;
   public final static int VALUE_COL          = 1;
   public final static int FIELD_INFO_COL     = 2;
   public final static int MODE_COL           = 3;
   public final static int LOCATION_COL       = 4;
   public final static int DESCRIPTION_COL    = 5;

   private final int fDefaultNameColumnWidth              = 200;
   private final int fDefaultValueColumnWidth             = 100;
   private final int fDefaultFieldColumnWidth             = 140;
   private       int fDefaultModeWidth                    = 0; // Should be final but has warning
   private       int fDefaultLocationColumnWidth          = 0; // Should be final but has warning
   private final int fDefaultDescriptionColumnWidth       = 300;

   private CheckboxTreeViewer          fPeripheralsTreeViewer;
   private final String[]              fTreeProperties = new String[] { "col1", "col2", "col3", "col4" };
   private PeripheralsInformationPanel fPeripheralsInformationPanel = null;

   private Action          fFilterPeripheralAction;
   private Action          fSetDeviceAction;
   private RefreshAction   fRefreshAllAction;

   private IDialogSettings fSettings = null;

   @Override
   public void init(IViewSite site, IMemento memento) throws PartInitException {
      //      System.err.println("UsbdmDevicePeripheralsView.init()");
      super.init(site, memento);
      loadSettings();
   }

   private void loadSettings() {
      //      System.err.println("UsbdmDevicePeripheralsView.loadSettings()");
      if (fSettings != null) {
         String value = fSettings.get(SETTINGS_SVD);
         if (value != null) {
            //            System.err.println("UsbdmDevicePeripheralsView.createPartControl() svdId init = " + value);
            try {
               fManuallySelectedSvdId = new SVDIdentifier(value);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
   }

   private void saveSettings() {
      //      System.err.println("UsbdmDevicePeripheralsView.saveSettings()");
      if ((fSettings != null) && (fManuallySelectedSvdId != null)) {
         //         System.err.println("UsbdmDevicePeripheralsView.createPartControl() svdId save = " + svdId);
         fSettings.put(SETTINGS_SVD, fManuallySelectedSvdId.toString());
      }
   }

   class MyAction extends Action {

      MyAction(String text, String toolTip, int style, String imageId) {
         super(text, style);

         setText(text);
         setToolTipText(toolTip);
         if ((imageId!= null) && (Activator.getDefault() != null)) {
            ImageDescriptor imageDescriptor = Activator.getImageDescriptor(imageId);
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
         TreeColumn column = fPeripheralsTreeViewer.getTree().getColumn(columnNum);
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
         Object[] visibleObjects = fPeripheralsTreeViewer.getVisibleExpandedElements();
         for (Object object : visibleObjects) {
            if (object instanceof PeripheralModel) {
               PeripheralModel peripheral = (PeripheralModel) object;
               peripheral.forceUpdate();
            }
         }
      }
   }

   class RefreshSelectionAction extends MyAction {

      RefreshSelectionAction(String text, String toolTip) {
         super(text, toolTip, IAction.AS_PUSH_BUTTON, Activator.ID_REFRESH_SELECTION_IMAGE);
      }

      public void run() {
         ISelection selection = fPeripheralsTreeViewer.getSelection();
         Object obj = ((IStructuredSelection) selection).getFirstElement();
         // System.err.println("Action1.run(), obj = " +
         // obj.toString()+", class=" + obj.getClass().toString());
         if (obj instanceof UpdateInterface) {
            UpdateInterface updateInterface = (UpdateInterface) obj;
            updateInterface.forceUpdate();
         }
      }
   }

   private Action                openFaultDialogue;
   private GdbDsfSessionListener gdbDsfSessionListener = null;

   // Current model being displayed
   private UsbdmDevicePeripheralsModel peripheralsModel = null;

   private static final String DEVICE_TOOLTIP_STRING = "Current device\nClick to change device";

   private LocalResourceManager     resManager = null;
   private HashMap<String, Image>   imageCache = new HashMap<String,Image>();

   /**
    * Testing constructor.
    */
   public UsbdmDevicePeripheralsView() {
      //      System.err.println("UsbdmDevicePeripheralsView()");
      fSettings = null;
      Activator activator = Activator.getDefault();
      if (activator != null) {
         fSettings = activator.getDialogSettings();
      }
      // Listen for DSF Sessions
      gdbDsfSessionListener = GdbDsfSessionListener.getListener();
      gdbDsfSessionListener.addListener(this);

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
   }

   public Image getMyImage(String imageId) {
      Image image = imageCache.get(imageId);
      if ((Activator.getDefault() != null) && (image == null)) {
         ImageDescriptor imageDescriptor  = Activator.getImageDescriptor(imageId);
         image = resManager.createImage(imageDescriptor);
         imageCache.put(imageId, image);
      }
      return image;
   }

   //   /**
   //    * Provides the editor for the tree elements
   //    * 
   //    * Does minor modifications to the default editor.
   //    */
   //   private class PeripheralsViewTextCellEditor extends TextCellEditor {
   //
   //      private int minHeight;
   //
   //      public PeripheralsViewTextCellEditor(Tree tree) {
   //         super(tree, SWT.BORDER);
   //         Text txt = (Text) getControl();
   //
   //         Font fnt = txt.getFont();
   //         FontData[] fontData = fnt.getFontData();
   //         if (fontData != null && fontData.length > 0) {
   //            minHeight = fontData[0].getHeight() + 10;
   //         }
   //      }
   //
   //      public LayoutData getLayoutData() {
   //         LayoutData data = super.getLayoutData();
   //         if (minHeight > 0)
   //            data.minimumHeight = minHeight;
   //         return data;
   //      }
   //   }

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
      fPeripheralsTreeViewer = new CheckboxTreeViewer(form, SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);

      Tree tree = fPeripheralsTreeViewer.getTree();
      tree.setLinesVisible(true);
      tree.setHeaderVisible(true);
      ColumnViewerToolTipSupport.enableFor(fPeripheralsTreeViewer);


      //      // Suppress tree expansion on double-click
      //      // see http://www.eclipse.org/forums/index.php/t/257325/
      //      peripheralsTreeViewer.getControl().addListener(SWT.MeasureItem, new Listener(){
      //         @Override
      //         public void handleEvent(Event event) {
      //         }});

      fPeripheralsTreeViewer.setColumnProperties(fTreeProperties);
      fPeripheralsTreeViewer.setCellEditors(new CellEditor[] { null, new TextCellEditor(fPeripheralsTreeViewer.getTree()), null });
      //      peripheralsTreeViewer.setCellEditors(new CellEditor[] { null, new PeripheralsViewTextCellEditor(peripheralsTreeViewer.getTree()), null });
      fPeripheralsTreeViewer.setCellModifier(new PeripheralsViewCellModifier(this));

      /*
       * Name column
       */
      TreeColumn column;
      column = new TreeColumn(fPeripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(fDefaultNameColumnWidth);
      column.setText("Name");

      // Add listener to column so peripherals are sorted by name when clicked
      column.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            fPeripheralsTreeViewer.setComparator(new PeripheralsViewSorter(PeripheralsViewSorter.SortCriteria.PeripheralNameOrder));
         }
      });

      /*
       * Value column
       */
      column = new TreeColumn(fPeripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(fDefaultValueColumnWidth);
      column.setText("Value");
      column.setResizable(fDefaultValueColumnWidth!=0);

      /*
       * Field column
       */
      column = new TreeColumn(fPeripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(fDefaultFieldColumnWidth );
      column.setText("Field");
      column.setResizable(fDefaultFieldColumnWidth!=0);

      /*
       * Mode column
       */
      column = new TreeColumn(fPeripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(fDefaultModeWidth);
      column.setText("Mode");
      column.setResizable(fDefaultModeWidth!=0);

      /*
       * Location column
       */
      column = new TreeColumn(fPeripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(fDefaultLocationColumnWidth);
      column.setText("Location");
      column.setResizable(fDefaultLocationColumnWidth!=0);

      // Add listener to column so peripheral are sorted by address when clicked
      column.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            fPeripheralsTreeViewer.setComparator(new PeripheralsViewSorter(PeripheralsViewSorter.SortCriteria.AddressOrder));
         }
      });

      /*
       * Description column
       */
      column = new TreeColumn(fPeripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(fDefaultDescriptionColumnWidth);
      column.setText("Description");
      column.setResizable(fDefaultDescriptionColumnWidth!=0);

      // Default to sorted by Peripheral name
      fPeripheralsTreeViewer.setComparator(new PeripheralsViewSorter(PeripheralsViewSorter.SortCriteria.PeripheralNameOrder));

      // Noting filtered
      fPeripheralsTreeViewer.addFilter(new PeripheralsViewFilter(PeripheralsViewFilter.SelectionCriteria.SelectAll));

      // Label provider
      fPeripheralsTreeViewer.setLabelProvider(new PeripheralsViewCellLabelProvider(this));

      // Content provider
      fPeripheralsTreeViewer.setContentProvider(new PeripheralsViewContentProvider(this));

      ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(fPeripheralsTreeViewer) {
         @Override
         protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
            return (event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION);// ||
            //                   (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR);
         }
      };
      TreeViewerEditor.create(fPeripheralsTreeViewer, actSupport, TreeViewerEditor.DEFAULT);

      // Create the help context id for the viewer's control
      // PlatformUI.getWorkbench().getHelpSystem().setHelp(treeViewer.getControl(),
      // "usbdmMemory.viewer");

      // =============================

      fPeripheralsInformationPanel = new PeripheralsInformationPanel(form, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY, this.fPeripheralsTreeViewer);
      form.setWeights(new int[] { 80, 20 });

      // Tree expansion/collapse
      fPeripheralsTreeViewer.addTreeListener(new ITreeViewerListener() {

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
      fPeripheralsTreeViewer.addCheckStateListener(new ICheckStateListener() {
         public void checkStateChanged(CheckStateChangedEvent event) {
            //            peripheralsTreeViewer.expandToLevel(event.getElement(), 1);
            fPeripheralsTreeViewer.setSubtreeChecked(event.getElement(), event.getChecked());
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
      Menu menu = menuMgr.createContextMenu(fPeripheralsTreeViewer.getControl());
      fPeripheralsTreeViewer.getControl().setMenu(menu);
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
    * 
    * @param manager
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
    * 
    * @param manager
    */
   private void fillContextMenu(IMenuManager manager) {
      for (MyAction action:myActions) {
         if (action instanceof HideShowColumnAction) {
            int column = ((HideShowColumnAction)action).getColumn();
            int currentWidth = fPeripheralsTreeViewer.getTree().getColumn(column).getWidth();
            action.setChecked(currentWidth != 0);
         }
         manager.add(action);
      }
      // Other plug-ins can contribute there actions here
      // manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
   }

   /**
    * Fill menu bar
    * 
    * @param manager
    */
   private void fillLocalToolBar(IToolBarManager manager) {
      manager.add(fSetDeviceAction);
      manager.add(fRefreshAllAction);
      manager.add(fFilterPeripheralAction);
      manager.add(openFaultDialogue);
   }

   /**
    * Create the various actions for the menus etc.
    */
   private void makeActions() {
      // These actions end up on the drop-down and pop-up menus
      myActions.add(new HideShowColumnAction("Toggle field information column", FIELD_INFO_COL,   fDefaultFieldColumnWidth));
      myActions.add(new HideShowColumnAction("Toggle mode column",              MODE_COL,         fDefaultModeWidth));
      myActions.add(new HideShowColumnAction("Toggle location column",          LOCATION_COL,     fDefaultLocationColumnWidth));
      myActions.add(new HideShowColumnAction("Toggle description column",       DESCRIPTION_COL,  fDefaultDescriptionColumnWidth));
      myActions.add(new RefreshSelectionAction("Refresh selection", "Refreshes currently selected registers/peripheral from target"));
      fRefreshAllAction = new RefreshAction("Refresh all",    "Refreshes visible registers from target");

      /*
       * Refresh current selection action
       */
      openFaultDialogue = new Action() {
         public void run() {
            if (peripheralsModel == null) {
               return;
            }
            FaultDialogue dialogue = new FaultDialogue(getSite().getShell());
            dialogue.setTitleImage(Activator.getImageDescriptor(Activator.ID_USBDM_IMAGE).createImage());
            dialogue.create(peripheralsModel.getGdbInterface());
            dialogue.open();
         }
      };
      openFaultDialogue.setText("Exception Report");
      openFaultDialogue.setToolTipText("Open Target Exception Report");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getImageDescriptor(Activator.ID_EXCEPTION_IMAGE);
         openFaultDialogue.setImageDescriptor(imageDescriptor);
      }

      /*
       * Filter Registers action
       */
      fFilterPeripheralAction = new Action(null, IAction.AS_CHECK_BOX) {
         public void run() {
            fPeripheralsTreeViewer.setFilters(new ViewerFilter[] { new MyViewerFilter(fFilterPeripheralAction.isChecked()) });
         }
      };
      fFilterPeripheralAction.setText("Filter registers");
      fFilterPeripheralAction.setToolTipText("Only display checked registers");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getImageDescriptor(Activator.ID_FILTER_IMAGE);
         fFilterPeripheralAction.setImageDescriptor(imageDescriptor);
      }

      /*
       * Select Device
       */
      fSetDeviceAction = new Action(null, IAction.AS_PUSH_BUTTON) {
         public void run() {
            //            TargetType interfaceType = TargetType.T_UNKNOWN;
            //            if ((peripheralsModel != null) && (peripheralsModel.getModel() != null)) {
            //               // Choose the same device class as existing
            //               interfaceType = peripheralsModel.getModel().getInterfaceType().toTargetType();
            //            }
            DevicePeripheralSelectionDialogue dialogue = 
                  new DevicePeripheralSelectionDialogue(getSite().getShell(), fManuallySelectedSvdId);
            if (dialogue.open() != Window.OK) {
               // Cancelled etc
               return;
            }
            String buttonText = "Device...";
            fManuallySelectedSvdId = dialogue.getSVDId();
            if (fManuallySelectedSvdId != null) {
               try {
                  buttonText = fManuallySelectedSvdId.getDeviceName();
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
            fSetDeviceAction.setText(buttonText);
            saveSettings();
            if ((fPeripheralsTreeViewer == null) || fPeripheralsTreeViewer.getControl().isDisposed()) {
               System.err.println("UsbdmDevicePeripheralsView.Action() - no peripheral view or already disposed()");
               return;
            }
            if (peripheralsModel == null) {
               // If there is no existing model we are finished (model will be created later)
               //               System.err.println("UsbdmDevicePeripheralsView.Action() - peripheralsModel == null");
               return;
            }
            //            System.err.println("UsbdmDevicePeripheralsView.Action() - Setting peripheral model");
            // Update model
            try {
               peripheralsModel.setDevice(fManuallySelectedSvdId);
            } catch (Exception e) {
               // Unable to generate model
               e.printStackTrace();
               MessageDialog.openError(getSite().getShell(), "Illegal device", "Device not found " + fManuallySelectedSvdId);
               return;
            }
            fPeripheralsTreeViewer.setInput(peripheralsModel.getModel());
            ColumnViewerToolTipSupport.enableFor(fPeripheralsTreeViewer);

         }
      };
      String buttonText = "Device...";
      if (fManuallySelectedSvdId != null) {
         try {
            buttonText = fManuallySelectedSvdId.getDeviceName();
         } catch (Exception e1) {
         }
      }
      fSetDeviceAction.setText(buttonText);
      fSetDeviceAction.setToolTipText(DEVICE_TOOLTIP_STRING);
   }

   /**
    * @author podonoghue
    */
   public class MyViewerFilter extends ViewerFilter {
      final boolean enabled;

      /**
       * @param enabled
       */
      MyViewerFilter(boolean enabled) {
         this.enabled = enabled;
      }

      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
       */
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
   @Override
   public void setFocus() {
      fPeripheralsTreeViewer.getControl().setFocus();
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.view.GdbSessionListener#sessionStarted(net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel)
    */
   @Override
   public void sessionStarted(final UsbdmDevicePeripheralsModel model) {
      //      System.err.println(String.format("UsbdmDevicePeripheralsView.sessionStarted(%s)", (aPeripheralsModel == null) ? "null" : aPeripheralsModel.getDeviceName()));
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((fPeripheralsTreeViewer == null) || fPeripheralsTreeViewer.getControl().isDisposed()) {
               //               System.err.println("UsbdmDevicePeripheralsView.SessionCreate() - no peripheral view or already disposed()");
               return;
            }
            if (peripheralsModel != null) {
               // Model already set - ignore
               System.err.println("UsbdmDevicePeripheralsView.sessionStarted() - peripheralsModel already set");
               return;
            }
            // Use model from DSF session as current model (may represent unknown device)
            peripheralsModel = model;

            if (peripheralsModel.getModel() == UsbdmDevicePeripheralsModel.NullDeviceModel) {
               // Model from DSF session does not model a real device
               // Try to create model from user selected item
               if (fManuallySelectedSvdId != null) {
                  try {
                     peripheralsModel.setDevice(fManuallySelectedSvdId);
                     ColumnViewerToolTipSupport.enableFor(fPeripheralsTreeViewer);
                  } catch (Exception e) {
                     e.printStackTrace();
                     MessageDialog.openError(getSite().getShell(), "Illegal device", "Device not found for " + fManuallySelectedSvdId);
                  }
               }
               fSetDeviceAction.setEnabled(true);
               fSetDeviceAction.setToolTipText("Manually selected device");
            }
            else {
               // Model automatically loaded from DSF session
               fSetDeviceAction.setEnabled(false);
               fSetDeviceAction.setToolTipText("Auto-selected device");
               fSetDeviceAction.setText(peripheralsModel.getDeviceName());
               ColumnViewerToolTipSupport.enableFor(fPeripheralsTreeViewer);
            }
            fPeripheralsTreeViewer.setInput(peripheralsModel.getModel());
         }
      });
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.view.GdbSessionListener#sessionTerminated(net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel)
    */
   @Override
   public void sessionTerminated(final UsbdmDevicePeripheralsModel model) {

      //       System.err.println("UsbdmDevicePeripheralsView.SessionTerminate()");
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((fPeripheralsTreeViewer == null) || fPeripheralsTreeViewer.getControl().isDisposed()) {
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
            fPeripheralsTreeViewer.setInput(null);
            peripheralsModel = null;
            String buttonText;
            try {
               buttonText = fManuallySelectedSvdId.getDeviceName();
            } catch (Exception e1) {
               buttonText = "Device...";
            }
            fSetDeviceAction.setText(buttonText);
            fSetDeviceAction.setEnabled(true);
            fSetDeviceAction.setToolTipText(DEVICE_TOOLTIP_STRING);
         }
      });
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.view.GdbSessionListener#sessionSuspended(net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel, net.sourceforge.usbdm.peripherals.view.GdbSessionListener.EventType)
    */
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
            if ((fPeripheralsTreeViewer == null) || fPeripheralsTreeViewer.getControl().isDisposed()) {
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
            Object[] visibleObjects = fPeripheralsTreeViewer.getExpandedElements();
            for (Object object : visibleObjects) {
               if (object instanceof PeripheralModel) {
                  ((PeripheralModel) object).forceUpdate();
               }
            }
         }
      });
   }

   public PeripheralsInformationPanel getInformationPanel() {
      return fPeripheralsInformationPanel;
   }
}
