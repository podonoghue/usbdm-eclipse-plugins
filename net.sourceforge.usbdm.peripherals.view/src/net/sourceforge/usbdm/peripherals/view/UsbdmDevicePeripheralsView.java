package net.sourceforge.usbdm.peripherals.view;

import net.sourceforge.usbdm.peripheralDatabase.Enumeration;
import net.sourceforge.usbdm.peripherals.view.UsbdmDevicePeripheralsModel.DeviceModel;
import net.sourceforge.usbdm.peripherals.view.UsbdmDevicePeripheralsModel.FieldModel;
import net.sourceforge.usbdm.peripherals.view.UsbdmDevicePeripheralsModel.PeripheralModel;
import net.sourceforge.usbdm.peripherals.view.UsbdmDevicePeripheralsModel.RegisterModel;
import net.sourceforge.usbdm.peripherals.view.UsbdmDevicePeripheralsModel.UpdateInterface;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.part.ViewPart;

/**
 * @author podonoghue
 * 
 */
public class UsbdmDevicePeripheralsView extends ViewPart implements
      GDBSessionListener {

   private CheckboxTreeViewer peripheralsTreeViewer;
   private final String[] treeProperties = new String[] { "col1", "col2",
         "col3" };
   private StyledText peripheralsInformationPanel;

   private Action refreshCurrentSelectionAction;
   private Action refreshAllAction;
   private Action resetPeripheralAction;
   private Action filterPeripheralAction;
   private Action hideShowColumnAction;

   private GDBInterface gdbInterface = null;

   /**
    * Testing constructor.
    */
   public UsbdmDevicePeripheralsView() {
      gdbInterface = new GDBInterface();
      gdbInterface.initialise();
      gdbInterface.addListener(this);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.eclipse.ui.part.WorkbenchPart#dispose()
    */
   @Override
   public void dispose() {
      super.dispose();
      if (gdbInterface != null) {
         gdbInterface.removeListener(this);
         gdbInterface.dispose();
      }
   }

   /**
    * Provides labels for the tree view cells
    */
   private class PeripheralsViewCellLabelProvider extends CellLabelProvider
         implements ITableLabelProvider, ITableFontProvider,
         ITableColorProvider {

      final FontRegistry registry = new FontRegistry();
      final Color changedColour = Display.getCurrent().getSystemColor(
            SWT.COLOR_YELLOW);
      final Font boldFont = registry.getBold(Display.getCurrent()
            .getSystemFont().getFontData()[0].getName());

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang
       * .Object, int)
       */
      Image peripheralImage = null;
      Image registerReadOnlyImage = null;
      Image registerReadWriteImage = null;
      Image fieldReadOnlyImage = null;
      Image fieldReadWriteImage = null;

      @Override
      public Image getColumnImage(Object element, int columnIndex) {
         if (columnIndex == 0) {
            // Only provide images for column 1
            if (element instanceof PeripheralModel) {
               if ((Activator.getDefault() != null)
                     && (peripheralImage == null)) {
                  peripheralImage = Activator.getDefault()
                        .getImageDescriptor(Activator.ID_PERIPHERAL_IMAGE)
                        .createImage();
               }
               return peripheralImage;
            } else if (element instanceof RegisterModel) {
               if (((RegisterModel) element).getAccessMode() == "RO") {
                  if ((Activator.getDefault() != null)
                        && (registerReadOnlyImage == null)) {
                     registerReadOnlyImage = Activator
                           .getDefault()
                           .getImageDescriptor(
                                 Activator.ID_REGISTER_READ_ONLY_IMAGE)
                           .createImage();
                  }
                  return registerReadOnlyImage;
               } else {
                  if ((Activator.getDefault() != null)
                        && (registerReadWriteImage == null)) {
                     registerReadWriteImage = Activator
                           .getDefault()
                           .getImageDescriptor(
                                 Activator.ID_REGISTER_READ_WRITE_IMAGE)
                           .createImage();
                  }
                  return registerReadWriteImage;
               }
            } else if (element instanceof FieldModel) {
               if (((FieldModel) element).getAccessMode() == "RO") {
                  if ((Activator.getDefault() != null)
                        && (fieldReadOnlyImage == null)) {
                     fieldReadOnlyImage = Activator
                           .getDefault()
                           .getImageDescriptor(
                                 Activator.ID_FIELD_READ_ONLY_IMAGE)
                           .createImage();
                  }
                  return fieldReadOnlyImage;
               } else {
                  if ((Activator.getDefault() != null)
                        && (fieldReadWriteImage == null)) {
                     fieldReadWriteImage = Activator
                           .getDefault()
                           .getImageDescriptor(
                                 Activator.ID_FIELD_READ_WRITE_IMAGE)
                           .createImage();
                  }
                  return fieldReadWriteImage;
               }
            }
         }
         return null;
      }

      @Override
      public void dispose() {
         super.dispose();
         if (changedColour != null) {
            changedColour.dispose();
         }
         if (boldFont != null) {
            boldFont.dispose();
         }
         if (peripheralImage != null) {
            peripheralImage.dispose();
         }
         if (registerReadWriteImage != null) {
            registerReadWriteImage.dispose();
         }
         if (fieldReadOnlyImage != null) {
            fieldReadOnlyImage.dispose();
         }
      }

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang
       * .Object, int)
       */
      public String getColumnText(Object element, int columnIndex) {
         BaseModel item = (BaseModel) element;
         switch (columnIndex) {
         case 0:
            return item.getName();
         case 1:
            return item.getValueAsString();
         case 2:
            return item.getAccessMode();
         case 3:
            return item.getAddressAsString();
         default:
            return "";
         }
      }

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.eclipse.jface.viewers.ITableFontProvider#getFont(java.lang.Object,
       * int)
       */
      public Font getFont(Object element, int columnIndex) {
         return (element instanceof PeripheralModel) ? boldFont : null;
      }

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.eclipse.jface.viewers.ITableColorProvider#getBackground(java.lang
       * .Object, int)
       */
      public Color getBackground(Object element, int columnIndex) {
         if ((columnIndex == 1) && (element instanceof BaseModel)) {
            try {
               return ((BaseModel) element).isChanged() ? changedColour : null;
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
         return null;
      }

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.eclipse.jface.viewers.ITableColorProvider#getForeground(java.lang
       * .Object, int)
       */
      public Color getForeground(Object element, int columnIndex) {
         return null;
      }

      @Override
      public void update(ViewerCell cell) {
         // System.err.println("PeripheralCellLabelProvider.update()");
      }

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.eclipse.jface.viewers.ViewerLabelProvider#getTooltipText(java.lang
       * .Object)
       */
      public String getToolTipText(Object element) {
         return "Tooltip (" + element + ")";
      }

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.eclipse.jface.viewers.ViewerLabelProvider#getTooltipDisplayDelayTime
       * (java.lang.Object)
       */
      public int getToolTipDisplayDelayTime(Object object) {
         return 2000;
      }

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.eclipse.jface.viewers.ViewerLabelProvider#getTooltipShift(java.
       * lang.Object)
       */
      public Point getToolTipShift(Object object) {
         return new Point(5, 5);
      }

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.eclipse.jface.viewers.ViewerLabelProvider#getTooltipTimeDisplayed
       * (java.lang.Object)
       */
      public int getToolTipTimeDisplayed(Object object) {
         return 5000;
      }
   }

   /**
    * Provides the contents from the tree view (from model)
    */
   private class PeripheralsViewContentProvider implements
         ITreeContentProvider, IModelChangeListener {

      private TreeViewer treeViewer = null;

      public void dispose() {
      }

      public Object[] getElements(Object inputElement) {
         return ((BaseModel) inputElement).getChildren().toArray();
      }

      public Object[] getChildren(Object parentElement) {
         return getElements(parentElement);
      }

      public Object getParent(Object element) {
         return ((BaseModel) element).getParent();
      }

      public boolean hasChildren(Object element) {
         return ((BaseModel) element).getChildren().size() > 0;
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
         // Save view
         this.treeViewer = (TreeViewer) viewer;
         if (oldInput != null) {
            // Remove old input as listener
            removeListenerFrom((BaseModel) oldInput);
         }
         if (newInput != null) {
            // Add new input as listener
            addListenerTo((BaseModel) newInput);
         }
      }

      protected void addListenerTo(BaseModel model) {
         // System.err.println("PeripheralsViewContentProvider.addListenerTo(), parent listener = "
         // + model.toString());
         model.addListener(this);
         for (Object childModel : model.getChildren()) {
            addListenerTo(((BaseModel) childModel));
            // System.err.println("PeripheralsViewContentProvider.addListenerTo(), listener = "
            // + childModel.toString());
         }
      }

      protected void removeListenerFrom(BaseModel model) {
         model.removeListener(this);
         for (Object childModel : model.getChildren()) {
            removeListenerFrom(((BaseModel) childModel));
         }
      }

      @Override
      public void modelElementChanged(ObservableModel model) {
         // System.err.println("modelElementChanged() model = " +
         // ((BaseModel)model).getName() );

         if (treeViewer != null) {
            // System.err.println("modelElementChanged() model is expanded");
            // treeViewer.update(model, new String[]{treeProperties[1]});
            treeViewer.refresh(model, true);
            ITreeSelection selection = (ITreeSelection) peripheralsTreeViewer
                  .getSelection();
            if (selection.getFirstElement() == model) {
               updatePeripheralsInformationPanel();
               // System.err.println("modelElementChanged(), updatePeripheralsInformationPanel() called");
            }
         }
      }

   }

   /**
    * Handles changes in selection of peripheral or register in tree view
    * 
    * Updates description in infoPanel
    */
   private class PeriperalsViewerSelectionChangeListener implements
         ISelectionChangedListener {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
         Object source = event.getSource();
         if (source == peripheralsTreeViewer) {
            updatePeripheralsInformationPanel();
         }
      }
   };

   /**
    * Handles modifying tree elements
    * 
    * This only applies to register or register field values
    * 
    */
   private class PeripheralsViewCellModifier implements ICellModifier {

      @Override
      public void modify(Object element, String property, Object value) {
         // System.err.println("PeripheralsViewCellModifier.modify("+element.getClass()+", "+value.toString()+")");
         if (element instanceof TreeItem) {
            // update element and tree model
            TreeItem treeItem = (TreeItem) element;
            Object treeItemData = treeItem.getData();
            if (treeItemData instanceof RegisterModel) {
               // System.err.println("PeripheralsViewCellModifier.modify(RegisterModel, "+value.toString()+")");
               RegisterModel registerModel = (RegisterModel) treeItemData;
               try {
                  String s = value.toString().trim();
                  if (s.startsWith("0b")) {
                     registerModel.setValue(Long.parseLong(
                           s.substring(2, s.length()), 2));
                  } else {
                     registerModel.setValue(Long.decode(s));
                  }
                  treeItem.setText(1, registerModel.getValueAsString());
               } catch (NumberFormatException e) {
                  System.err
                        .println("PeripheralsViewCellModifier.modify(RegisterModel, ...) - format error");
               }
            } else if (treeItemData instanceof FieldModel) {
               FieldModel fieldModel = (FieldModel) treeItemData;
               try {
                  String s = value.toString().trim();
                  if (s.startsWith("0b")) {
                     fieldModel.setValue(Long.parseLong(
                           s.substring(2, s.length()), 2));
                  } else {
                     fieldModel.setValue(Long.decode(s));
                  }
                  treeItem.setText(1, fieldModel.getValueAsString());
               } catch (NumberFormatException e) {
                  System.err
                        .println("PeripheralsViewCellModifier.modify(FieldModel, ...) - format error");
               }
            }
         }
         updatePeripheralsInformationPanel();
      }

      @Override
      public Object getValue(Object element, String property) {
         if (element instanceof RegisterModel) {
            // System.err.println("PeripheralsViewCellModifier.getValue(RegisterModel, "+((RegisterModel)element).getValueAsString()+")");
            return ((RegisterModel) element).getValueAsString();
         } else if (element instanceof FieldModel) {
            // System.err.println("PeripheralsViewCellModifier.getValue(FieldModel, "+((FieldModel)element).getValueAsString()+")");
            return ((FieldModel) element).getValueAsString();
         }
         // System.err.println("PeripheralsViewCellModifier.getValue("+element.getClass()+", "+element.toString()+")");
         return element.toString();
      }

      @Override
      public boolean canModify(Object element, String property) {
         return (element instanceof FieldModel)
               || (element instanceof RegisterModel);
      }
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
    * Updates the peripheralsInformationPanel according to the current tree
    * selection
    */
   void updatePeripheralsInformationPanel() {

      peripheralsInformationPanel.setText("");

      ITreeSelection selection = (ITreeSelection) peripheralsTreeViewer
            .getSelection();
      Object uModel = selection.getFirstElement();
      if ((uModel != null) && (uModel instanceof BaseModel)) {
         String description = ((BaseModel) uModel).getDescription();
         StyleRange styleRange = new StyleRange(0, description.length(), null,
               null, SWT.BOLD);
         if (!(uModel instanceof FieldModel)) {
            peripheralsInformationPanel.setText(description);
            peripheralsInformationPanel.setStyleRange(styleRange);
         } else {
            int enumerationIndex = description.length(); // Start of enumeration
                                                         // text
            int enumerationlength = 0; // Length of enumeration text
            int selectionIndex = 0; // Start of highlighted enumeration
            int selectionLength = 0; // Length of highlighted enumeration
            long enumerationValue = ((FieldModel) uModel).getValue();
            for (Enumeration enumeration : ((FieldModel) uModel)
                  .getEnumeratedDescription()) {
               description += "\n";
               String enumerationValueDescription = enumeration.getName()
                     + ": " + enumeration.getDescription();
               if (enumeration.isSelected(enumerationValue)) {
                  selectionIndex = description.length();
                  selectionLength = enumerationValueDescription.length();
               }
               enumerationlength += enumerationValueDescription.length();
               description += enumerationValueDescription;
            }
            peripheralsInformationPanel.setText(description);
            peripheralsInformationPanel.setStyleRange(styleRange);
            styleRange = new StyleRange(enumerationIndex, enumerationlength,
                  null, null, SWT.NORMAL);
            peripheralsInformationPanel.setStyleRange(styleRange);
            styleRange = new StyleRange(selectionIndex, selectionLength,
                  Display.getCurrent().getSystemColor(SWT.COLOR_RED), null,
                  SWT.NORMAL);
            peripheralsInformationPanel.setStyleRange(styleRange);
         }
      }
   }

   /**
    * Callback that creates the viewer and initializes it.
    * 
    * The View consists of a tree and a information panel
    */
   public void createPartControl(Composite parent) {

      parent.setLayoutData(new FillLayout());

      SashForm form = new SashForm(parent, SWT.VERTICAL);
      form.setLayout(new FillLayout());

      // Make sash visible
      form.setSashWidth(4);
      form.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));

      // =============================
      peripheralsTreeViewer = new CheckboxTreeViewer(form, SWT.MULTI
            | SWT.V_SCROLL | SWT.FULL_SELECTION);

      Tree tree = peripheralsTreeViewer.getTree();
      tree.setLinesVisible(true);
      tree.setHeaderVisible(true);
      ColumnViewerToolTipSupport.enableFor(peripheralsTreeViewer);

      peripheralsTreeViewer.setColumnProperties(treeProperties);
      peripheralsTreeViewer.setCellEditors(new CellEditor[] { null,
            new PeripheralsViewTextCellEditor(peripheralsTreeViewer.getTree()),
            null });
      peripheralsTreeViewer.setCellModifier(new PeripheralsViewCellModifier());

      peripheralsTreeViewer.getControl().addListener(SWT.MeasureItem,
            new Listener() {
               @Override
               public void handleEvent(Event event) {

               }
            });

      /*
       * Name column
       */
      TreeColumn column;
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(200);
      column.setText("Name");

      // Add listener to column so peripherals are sorted by name when clicked
      column.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            peripheralsTreeViewer.setComparator(new PeripheralsViewSorter(
                  PeripheralsViewSorter.SortCriteria.PeripheralNameOrder));
         }
      });

      /*
       * Value column
       */
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(100);
      column.setText("Value");
      column.setResizable(true);

      /*
       * Value column
       */
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(50);
      column.setText("Mode");
      column.setResizable(true);

      /*
       * Location column (starts out hidden)
       */
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(0);
      column.setText("Location");
      column.setResizable(false);

      // Default to sorted by Peripheral name
      peripheralsTreeViewer.setComparator(new PeripheralsViewSorter(
            PeripheralsViewSorter.SortCriteria.PeripheralNameOrder));

      // Add listener to column so peripheral are sorted by address when clicked
      column.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            peripheralsTreeViewer.setComparator(new PeripheralsViewSorter(
                  PeripheralsViewSorter.SortCriteria.AddressOrder));
         }
      });

      peripheralsTreeViewer.addFilter(new PeripheralsViewFilter(
            PeripheralsViewFilter.SelectionCriteria.SelectAll));

      peripheralsTreeViewer
            .setLabelProvider(new PeripheralsViewCellLabelProvider());
      peripheralsTreeViewer
            .setContentProvider(new PeripheralsViewContentProvider());

      // peripheralsTreeViewer.setInput(UsbdmDevicePeripheralsModel.createModel(deviceName));

      TreeViewerEditor.create(peripheralsTreeViewer,
            new ColumnViewerEditorActivationStrategy(peripheralsTreeViewer) {
               protected boolean isEditorActivationEvent(
                     ColumnViewerEditorActivationEvent event) {
                  return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION;
               }
            }, TreeViewerEditor.DEFAULT);

      // Create the help context id for the viewer's control
      // PlatformUI.getWorkbench().getHelpSystem().setHelp(treeViewer.getControl(),
      // "usbdmMemory.viewer");

      // =============================

      peripheralsInformationPanel = new StyledText(form, SWT.WRAP
            | SWT.V_SCROLL | SWT.READ_ONLY);
      form.setWeights(new int[] { 80, 20 });

      peripheralsTreeViewer
            .addSelectionChangedListener(new PeriperalsViewerSelectionChangeListener());
      // peripheralsTreeViewer.addTreeListener(new ITreeViewerListener() {
      // @Override
      // public void treeExpanded(TreeExpansionEvent event) {
      // Object element = event.getElement();
      // System.err.println("treeExpanded() => event.getElement().getClass() = "
      // + element.getClass());
      // if (element instanceof RegisterModel) {
      // ((RegisterModel)element).update();
      // }
      // if (element instanceof PeripheralModel) {
      // ((PeripheralModel)element).update();
      // }
      // }
      //
      // @Override
      // public void treeCollapsed(TreeExpansionEvent event) {
      // }
      // });

      // When user checks a checkbox in the tree, check all its children
      peripheralsTreeViewer.addCheckStateListener(new ICheckStateListener() {
         public void checkStateChanged(CheckStateChangedEvent event) {
            peripheralsTreeViewer.expandToLevel(event.getElement(), 1);
            peripheralsTreeViewer.setSubtreeChecked(event.getElement(),
                  event.getChecked());
         }
      });

      makeActions();
      hookContextMenu();
      contributeToActionBars();

   }

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
      // IWorkbenchPartSite site = getSite();
      // if (site == null) {
      // return;
      // }
      // site.registerContextMenu(menuMgr, peripheralsTreeViewer); // Don't want
      // other items
   }

   private void contributeToActionBars() {
      IViewSite site = getViewSite();
      if (site == null) {
         return;
      }
      IActionBars bars = site.getActionBars();
      fillLocalPullDown(bars.getMenuManager());
      fillLocalToolBar(bars.getToolBarManager());
   }

   private void fillLocalPullDown(IMenuManager manager) {
      manager.add(refreshCurrentSelectionAction);
      manager.add(refreshAllAction);
      manager.add(filterPeripheralAction);
      manager.add(hideShowColumnAction);

      manager.add(new Separator());
   }

   private void fillContextMenu(IMenuManager manager) {
      manager.add(refreshCurrentSelectionAction);
      manager.add(resetPeripheralAction);
      // manager.add(filterPeripheralAction);
      // manager.add(hideShowColumnAction);
      // Other plug-ins can contribute there actions here
      // manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
   }

   private void fillLocalToolBar(IToolBarManager manager) {
      manager.add(refreshCurrentSelectionAction);
      manager.add(refreshAllAction);
      manager.add(filterPeripheralAction);
   }

   /**
    * Create the various actions for the menus etc.
    * 
    */
   private void makeActions() {
      /*
       * Refresh all action
       */
      refreshAllAction = new Action() {
         public void run() {
            Object[] visibleObjects = peripheralsTreeViewer
                  .getVisibleExpandedElements();
            for (Object object : visibleObjects) {
               if (object instanceof PeripheralModel) {
                  ((PeripheralModel) object).forceUpdate();
               }
            }
         }
      };
      refreshAllAction.setText("Refresh all");
      refreshAllAction
            .setToolTipText("Refreshes visible registers from target");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getDefault()
               .getImageDescriptor(Activator.ID_REFRESH_IMAGE);
         refreshAllAction.setImageDescriptor(imageDescriptor);
      }

      /*
       * Refresh current selection action
       */
      refreshCurrentSelectionAction = new Action() {
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
      };
      refreshCurrentSelectionAction.setText("Refresh selection");
      refreshCurrentSelectionAction
            .setToolTipText("Refreshes currently selected registers/peripheral from target");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getDefault()
               .getImageDescriptor(Activator.ID_REFRESH_SELECTION_IMAGE);
         refreshCurrentSelectionAction.setImageDescriptor(imageDescriptor);
      }

      /*
       * Reset Register action
       */
      resetPeripheralAction = new Action() {
         public void run() {
            ISelection selection = peripheralsTreeViewer.getSelection();
            Object obj = ((IStructuredSelection) selection).getFirstElement();
            // System.err.println("Action1.run(), obj = " +
            // obj.toString()+", class=" + obj.getClass().toString());
            if (obj != null) {
               if (obj instanceof RegisterModel) {
                  ((RegisterModel) obj).loadResetValues();
               }
               if (obj instanceof PeripheralModel) {
                  ((PeripheralModel) obj).loadResetValues();
               }
            }
         }
      };
      resetPeripheralAction.setText("Reset registers");
      resetPeripheralAction
            .setToolTipText("Reset registers to expected reset value");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getDefault()
               .getImageDescriptor(Activator.ID_RESET_IMAGE);
         resetPeripheralAction.setImageDescriptor(imageDescriptor);
      }

      /*
       * Filter Registers action
       */
      filterPeripheralAction = new Action(null, IAction.AS_CHECK_BOX) {
         public void run() {
            peripheralsTreeViewer
                  .setFilters(new ViewerFilter[] { new MyViewerFilter(
                        filterPeripheralAction.isChecked()) });
         }
      };
      filterPeripheralAction.setText("Filter registers");
      filterPeripheralAction.setToolTipText("Only display checked registers");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getDefault()
               .getImageDescriptor(Activator.ID_FILTER_IMAGE);
         filterPeripheralAction.setImageDescriptor(imageDescriptor);
      }

      /*
       * Hide/Show Address column
       */
      hideShowColumnAction = new Action(null, IAction.AS_CHECK_BOX) {
         public void run() {
            final int defaultWidth = 100;
            TreeColumn locationColumn = peripheralsTreeViewer.getTree()
                  .getColumn(3);
            locationColumn.setWidth(this.isChecked() ? defaultWidth : 0);
            locationColumn.setResizable(this.isChecked());
         }
      };
      hideShowColumnAction.setText("Show location column");
      hideShowColumnAction.setToolTipText("Show location column");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getDefault()
               .getImageDescriptor(Activator.ID_SHOW_COLUMN_IMAGE);
         hideShowColumnAction.setImageDescriptor(imageDescriptor);
      }
   }

   /**
    * 
    * 
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
         return !enabled || ((CheckboxTreeViewer) viewer).getChecked(element)
               || (element instanceof FieldModel);
      }
   }

   /**
    * Passing the focus request to the viewer's control.
    */
   public void setFocus() {
      peripheralsTreeViewer.getControl().setFocus();
   }

   @Override
   public void SessionTerminate(DebugEvent source) {
      // System.err.println("UsbdmDevicePeripheralsView.SessionTerminate()");
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((peripheralsTreeViewer == null)
                  || peripheralsTreeViewer.getControl().isDisposed()) {
               // System.err.println("UsbdmDevicePeripheralsView.SessionTerminate() - no peripheral view or already disposed()");
               return;
            }
            peripheralsTreeViewer.setInput(UsbdmDevicePeripheralsModel
                  .createModel(null));
         }
      });
   }

   @Override
   public void SessionSuspend(DebugEvent source) {
      // System.err.println("UsbdmDevicePeripheralsView.SessionSuspend()");
      if ((source.getDetail() != DebugEvent.STEP_END)
            && (source.getDetail() != DebugEvent.BREAKPOINT)
            && (source.getDetail() != DebugEvent.CLIENT_REQUEST)) {
         return;
      }
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((peripheralsTreeViewer == null)
                  || peripheralsTreeViewer.getControl().isDisposed()) {
               // System.err.println("UsbdmDevicePeripheralsView.SessionSuspend() - no peripheral view or already disposed()");
               return;
            }
            DeviceModel model = (DeviceModel) peripheralsTreeViewer.getInput();
            if (model == null) {
               // Try to create new model for current device
               // System.err.println("UsbdmDevicePeripheralsView.SessionSuspend() - creating new model for "+gdbInterface.getDeviceName());
               peripheralsTreeViewer.setInput(UsbdmDevicePeripheralsModel
                     .createModel(gdbInterface.getDeviceName()));
               model = (DeviceModel) peripheralsTreeViewer.getInput();
            }
            if (model == null) {
               System.err
                     .println("UsbdmDevicePeripheralsView.SessionSuspend() - model == null");
            }
            if (model != null) {
               // Set current register values as the 'reference' for changed
               // values
               model.setChangeReference();
               // Set all registers as stale
               model.setNeedsUpdate(true);
               // Update the view
               peripheralsTreeViewer.refresh();
            }
         }
      });
   }

   @Override
   public void SessionCreate(GDBInterface source) {
      final String deviceName;
      if (source != null) {
         deviceName = source.getDeviceName();
      } else {
         // deviceName = "MKL25Z128M4";
         deviceName = "MK20DX128M5";
         // deviceName = "MKE02Z64M2";
      }
      // System.err.println(String.format("UsbdmDevicePeripheralsView.SessionCreate(%s)",
      // deviceName));
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((peripheralsTreeViewer == null)
                  || peripheralsTreeViewer.getControl().isDisposed()) {
               // System.err.println("UsbdmDevicePeripheralsView.SessionCreate() - no peripheral view or already disposed()");
               return;
            }
            peripheralsTreeViewer.setInput(UsbdmDevicePeripheralsModel
                  .createModel(deviceName));
         }
      });
   }

   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Task List - TableViewer Example");
      shell.setLayout(new FillLayout());

      Composite composite = new Composite(shell, SWT.NONE);
      composite.setBackground(new Color(display, 255, 0, 0));
      composite.setLayout(new FillLayout());

      UsbdmDevicePeripheralsView view = new UsbdmDevicePeripheralsView();

      view.createPartControl(composite);

      view.SessionCreate(null);

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }

}
