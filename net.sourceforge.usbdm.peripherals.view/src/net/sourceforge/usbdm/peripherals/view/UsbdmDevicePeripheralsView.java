package net.sourceforge.usbdm.peripherals.view;

import java.util.HashMap;

import net.sourceforge.usbdm.peripheralDatabase.Enumeration;
import net.sourceforge.usbdm.peripherals.model.BaseModel;
import net.sourceforge.usbdm.peripherals.model.FieldModel;
import net.sourceforge.usbdm.peripherals.model.IModelChangeListener;
import net.sourceforge.usbdm.peripherals.model.MemoryException;
import net.sourceforge.usbdm.peripherals.model.ObservableModel;
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
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
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
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
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
public class UsbdmDevicePeripheralsView extends ViewPart implements GdbSessionListener {

   private CheckboxTreeViewer peripheralsTreeViewer;
   private final String[] treeProperties = new String[] { "col1", "col2", "col3", "col4" };
   private StyledText peripheralsInformationPanel;

   private Action refreshCurrentSelectionAction;
   private Action refreshAllAction;
//   private Action resetPeripheralAction;
   private Action filterPeripheralAction;
   private Action hideShowLocationColumnAction;
   private Action hideShowDescriptionColumnAction;
   private Action setDeviceAction;

   private Action openFaultDialogue;

   private GdbDsfSessionListener gdbDsfSessionListener = null;
   private GdbMiSessionListener gdbMiSessionListener = null;

   // Current model being displayed
   private UsbdmDevicePeripheralsModel peripheralsModel = null;
   
   private String defaultDeviceOrSvdFilename = null;

   private final String DEVICE_TOOLTIP_STRING = "Open change device dialogue";
   
   private LocalResourceManager resManager = null;
   private HashMap<String, Image> imageCache = new HashMap<String,Image>();

   private final int defaultNameColumnWidth        = 200;
   private final int defaultValueColumnWidth       = 100;
   private final int defaultModeWidth              = 50;
   private final int defaultLocationColumnWidth    = 100;
   private final int defaultDescriptionColumnWidth = 400;

   /**
    * Testing constructor.
    */
   public UsbdmDevicePeripheralsView() {
      // Listen for DSF Sessions
      gdbDsfSessionListener = GdbDsfSessionListener.getListener();
      gdbDsfSessionListener.addListener(this);

      // Listen for MI Sessions
      gdbMiSessionListener = GdbMiSessionListener.getListener();
      gdbMiSessionListener.addListener(this);
      
//      IViewReference[] viewReferences = getSite().getPage().getViewReferences();
//      for (IViewReference viewReference : viewReferences) {
//         System.err.println("");
//      }
//      getSite().getPage().addSelectionListener("", new ISelectionListener() {
//
//         @Override
//         public void selectionChanged(IWorkbenchPart part, ISelection selection) {
//            MessageDialog.openInformation(part.getSite().getShell(), "Info", "Info for you");
//         }
//         
//      });
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
    * Provides labels for the tree view cells
    */
   private class PeripheralsViewCellLabelProvider extends CellLabelProvider implements ITableLabelProvider, ITableFontProvider, ITableColorProvider {

      final FontRegistry registry = new FontRegistry();
      final Color changedColour = Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);
      final Font boldFont = registry.getBold(Display.getCurrent().getSystemFont().getFontData()[0].getName());

      /*
       * (non-Javadoc)
       * 
       * @see
       * org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang
       * .Object, int)
       */
      @Override
      public Image getColumnImage(Object element, int columnIndex) {
         if (columnIndex == 0) {
            // Only provide images for column 1
            if (element instanceof PeripheralModel) {
               return getMyImage(Activator.ID_PERIPHERAL_IMAGE);
            }
            else if (element instanceof RegisterModel) {
               if (((RegisterModel) element).getAccessMode() == "RO") {
                  return getMyImage(Activator.ID_REGISTER_READ_ONLY_IMAGE);
               } 
               else {
                  return getMyImage(Activator.ID_REGISTER_READ_WRITE_IMAGE);
               }
            } 
            else if (element instanceof FieldModel) {
               if (((FieldModel) element).getAccessMode() == "RO") {
                  return getMyImage(Activator.ID_FIELD_READ_ONLY_IMAGE);
               } 
               else {
                  return getMyImage(Activator.ID_FIELD_READ_WRITE_IMAGE);
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
            return item.safeGetValueAsString();
         case 2:
            return item.getAccessMode();
         case 3:
            return item.getAddressAsString();
         case 4: {
            String description = item.getDescription();
            // Truncate at newline if present
            int newlineIndex = description.indexOf("\n");
            if (newlineIndex > 0) {
               description = description.substring(0, newlineIndex);
            }
            return description;
         }
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
   private class PeripheralsViewContentProvider implements ITreeContentProvider, IModelChangeListener {

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
            ITreeSelection selection = (ITreeSelection) peripheralsTreeViewer.getSelection();
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
   private class PeriperalsViewerSelectionChangeListener implements ISelectionChangedListener {
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
                     registerModel.setValue(Long.parseLong(s.substring(2, s.length()), 2));
                  } else {
                     registerModel.setValue(Long.decode(s));
                  }
                  treeItem.setText(1, registerModel.safeGetValueAsString());
               } catch (NumberFormatException e) {
//                  System.err.println("PeripheralsViewCellModifier.modify(RegisterModel, ...) - format error");
               }
            } else if (treeItemData instanceof FieldModel) {
               FieldModel fieldModel = (FieldModel) treeItemData;
               try {
                  String s = value.toString().trim();
                  if (s.startsWith("0b")) {
                     fieldModel.setValue(Long.parseLong(s.substring(2, s.length()), 2));
                  } else {
                     fieldModel.setValue(Long.decode(s));
                  }
                  treeItem.setText(1, fieldModel.safeGetValueAsString());
               } catch (NumberFormatException e) {
//                  System.err.println("PeripheralsViewCellModifier.modify(FieldModel, ...) - format error");
               }
            }
         }
         updatePeripheralsInformationPanel();
      }

      @Override
      public Object getValue(Object element, String property) {
         if (element instanceof RegisterModel) {
            // System.err.println("PeripheralsViewCellModifier.getValue(RegisterModel, "+((RegisterModel)element).getValueAsString()+")");
            return ((RegisterModel) element).safeGetValueAsString();
         } else if (element instanceof FieldModel) {
            // System.err.println("PeripheralsViewCellModifier.getValue(FieldModel, "+((FieldModel)element).getValueAsString()+")");
            return ((FieldModel) element).safeGetValueAsString();
         }
         // System.err.println("PeripheralsViewCellModifier.getValue("+element.getClass()+", "+element.toString()+")");
         return element.toString();
      }

      @Override
      public boolean canModify(Object element, String property) {
         return (element instanceof FieldModel) || (element instanceof RegisterModel);
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
    * Updates the peripheralsInformationPanel according to the current tree selection
    */
   void updatePeripheralsInformationPanel() {

      peripheralsInformationPanel.setText("");

      ITreeSelection selection = (ITreeSelection) peripheralsTreeViewer.getSelection();
      Object uModel = selection.getFirstElement();
      if ((uModel == null) || !(uModel instanceof BaseModel)) {
         return;
      }
      String basicDescription = ((BaseModel) uModel).getDescription();
      String valueString = "";
      try {
         long value = 0;
         if (uModel instanceof RegisterModel) {
            value = ((RegisterModel)uModel).getValue();
            valueString = String.format(" (%d,0x%X,0b%s)", value, value, Long.toBinaryString(value));
         }
         else if (uModel instanceof FieldModel) {
            value = ((FieldModel)uModel).getValue();
            valueString = String.format(" (%d,0x%X,0b%s)", value, value, Long.toBinaryString(value));
         }
      } catch (MemoryException e) {
//         System.err.println("Opps");      
//         long value = 1234;
//         valueString = String.format(" (%d,0x%X,0b%s)", value, value, Long.toBinaryString(value));
      }
      StringBuffer description = new StringBuffer();
      StyleRange valueStyleRange = null;
      int splitAt = basicDescription.indexOf("\n");
      if (!valueString.isEmpty()) {
         if (splitAt != -1) {
            description.append(basicDescription.substring(0, splitAt));
            valueStyleRange = new StyleRange(description.length(), valueString.length(), Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE), null, SWT.NORMAL);
            description.append(valueString);
            description.append(basicDescription.substring(splitAt)); 
         } else {
            description.append(basicDescription);
            valueStyleRange = new StyleRange(description.length(), valueString.length(), Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE), null, SWT.NORMAL);
            description.append(valueString);
         }
      }
      else {
         description.append(basicDescription);
      }
      StyleRange styleRange = new StyleRange(0, description.length(), null, null, SWT.BOLD);
      if (uModel instanceof FieldModel) {
         FieldModel uField = (FieldModel) uModel;
         int enumerationIndex = description.length(); // Start of enumeration
         // text
         int enumerationlength = 0; // Length of enumeration text
         int selectionIndex = 0;    // Start of highlighted enumeration
         int selectionLength = 0;   // Length of highlighted enumeration
         long enumerationValue = 0;
         boolean enumerationValid = false;
         try {
            enumerationValue = uField.getValue();
            enumerationValid = true;
         } catch (MemoryException e) {
         }
         for (Enumeration enumeration : uField.getEnumeratedDescription()) {
            description.append("\n");
            String enumerationValueDescription = enumeration.getName() + ": " + enumeration.getDescription();
            if (enumerationValid && enumeration.isSelected(enumerationValue)) {
               selectionIndex  = description.length();
               selectionLength = enumerationValueDescription.length();
            }
            enumerationlength += enumerationValueDescription.length();
            description.append(enumerationValueDescription);
         }
         peripheralsInformationPanel.setText(description.toString());
         peripheralsInformationPanel.setStyleRange(styleRange);
         if (valueStyleRange != null) {
            peripheralsInformationPanel.setStyleRange(valueStyleRange);
         }
         styleRange = new StyleRange(enumerationIndex, enumerationlength, null, null, SWT.NORMAL);
         peripheralsInformationPanel.setStyleRange(styleRange);
         styleRange = new StyleRange(selectionIndex, selectionLength, Display.getCurrent().getSystemColor(SWT.COLOR_RED), null, SWT.NORMAL);
         peripheralsInformationPanel.setStyleRange(styleRange);

      } else {
         peripheralsInformationPanel.setText(description.toString());
         peripheralsInformationPanel.setStyleRange(styleRange);
         if (valueStyleRange != null) {
            peripheralsInformationPanel.setStyleRange(valueStyleRange);
         }
      }
   }

   /**
    * Callback that creates the viewer and initializes it.
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
      ColumnViewerToolTipSupport.enableFor(peripheralsTreeViewer);

      peripheralsTreeViewer.setColumnProperties(treeProperties);
      peripheralsTreeViewer.setCellEditors(new CellEditor[] { null, new PeripheralsViewTextCellEditor(peripheralsTreeViewer.getTree()), null });
      peripheralsTreeViewer.setCellModifier(new PeripheralsViewCellModifier());

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
      column.setResizable(true);

      /*
       * Mode column
       */
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(defaultModeWidth);
      column.setText("Mode");
      column.setResizable(true);

      /*
       * Location column (starts out hidden)
       */
      column = new TreeColumn(peripheralsTreeViewer.getTree(), SWT.NONE);
      column.setWidth(0);
      column.setText("Location");
      column.setResizable(false);

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
      column.setWidth(400);
      column.setText("Description");
      column.setResizable(true);

      // Default to sorted by Peripheral name
      peripheralsTreeViewer.setComparator(new PeripheralsViewSorter(PeripheralsViewSorter.SortCriteria.PeripheralNameOrder));

      peripheralsTreeViewer.addFilter(new PeripheralsViewFilter(PeripheralsViewFilter.SelectionCriteria.SelectAll));

      peripheralsTreeViewer.setLabelProvider(new PeripheralsViewCellLabelProvider());
      peripheralsTreeViewer.setContentProvider(new PeripheralsViewContentProvider());

      // peripheralsTreeViewer.setInput(UsbdmDevicePeripheralsModel.createModel(deviceName));

      TreeViewerEditor.create(peripheralsTreeViewer, new ColumnViewerEditorActivationStrategy(peripheralsTreeViewer) {
         protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
            return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION;
         }
      }, TreeViewerEditor.DEFAULT);

      // Create the help context id for the viewer's control
      // PlatformUI.getWorkbench().getHelpSystem().setHelp(treeViewer.getControl(),
      // "usbdmMemory.viewer");

      // =============================

      peripheralsInformationPanel = new StyledText(form, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
      form.setWeights(new int[] { 80, 20 });

      peripheralsTreeViewer.addSelectionChangedListener(new PeriperalsViewerSelectionChangeListener());
      
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
            peripheralsTreeViewer.expandToLevel(event.getElement(), 1);
            peripheralsTreeViewer.setSubtreeChecked(event.getElement(), event.getChecked());
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
      manager.add(hideShowLocationColumnAction);
      manager.add(hideShowDescriptionColumnAction);
      manager.add(new Separator());
   }

   private void fillContextMenu(IMenuManager manager) {
      manager.add(refreshCurrentSelectionAction);
//      manager.add(resetPeripheralAction);
      manager.add(hideShowLocationColumnAction);
      manager.add(hideShowDescriptionColumnAction);
      manager.add(openFaultDialogue);
      // manager.add(filterPeripheralAction);
      // manager.add(hideShowColumnAction);
      // Other plug-ins can contribute there actions here
      // manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
   }

   private void fillLocalToolBar(IToolBarManager manager) {
      manager.add(setDeviceAction);
      manager.add(refreshCurrentSelectionAction);
      manager.add(refreshAllAction);
      manager.add(filterPeripheralAction);
      manager.add(openFaultDialogue);
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
            Object[] visibleObjects = peripheralsTreeViewer.getVisibleExpandedElements();
            for (Object object : visibleObjects) {
               if (object instanceof PeripheralModel) {
                  ((PeripheralModel) object).forceUpdate();
               }
            }
         }
      };
      refreshAllAction.setText("Refresh all");
      refreshAllAction.setToolTipText("Refreshes visible registers from target");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getDefault().getImageDescriptor(Activator.ID_REFRESH_IMAGE);
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
      refreshCurrentSelectionAction.setToolTipText("Refreshes currently selected registers/peripheral from target");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getDefault().getImageDescriptor(Activator.ID_REFRESH_SELECTION_IMAGE);
         refreshCurrentSelectionAction.setImageDescriptor(imageDescriptor);
      }

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

//      /*
//       * Reset Register action
//       */
//      resetPeripheralAction = new Action() {
//         public void run() {
//            ISelection selection = peripheralsTreeViewer.getSelection();
//            Object obj = ((IStructuredSelection) selection).getFirstElement();
//            // System.err.println("Action1.run(), obj = " +
//            // obj.toString()+", class=" + obj.getClass().toString());
//            if (obj != null) {
//               if (obj instanceof RegisterModel) {
//                  ((RegisterModel) obj).loadResetValues();
//               }
//               if (obj instanceof PeripheralModel) {
//                  ((PeripheralModel) obj).loadResetValues();
//               }
//            }
//         }
//      };
//      resetPeripheralAction.setText("Reset registers");
//      resetPeripheralAction.setToolTipText("Reset registers to expected reset value");
//      if (Activator.getDefault() != null) {
//         ImageDescriptor imageDescriptor = Activator.getDefault().getImageDescriptor(Activator.ID_RESET_IMAGE);
//         resetPeripheralAction.setImageDescriptor(imageDescriptor);
//      }

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
       * Hide/Show Address column
       */
      hideShowLocationColumnAction = new Action(null, IAction.AS_CHECK_BOX) {
         public void run() {
            TreeColumn locationColumn = peripheralsTreeViewer.getTree().getColumn(3);
            locationColumn.setWidth(this.isChecked() ? defaultLocationColumnWidth : 0);
            locationColumn.setResizable(this.isChecked());
         }
      };
      hideShowLocationColumnAction.setText("Toggle location column");
      hideShowLocationColumnAction.setToolTipText("Toggle location column");
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getDefault().getImageDescriptor(Activator.ID_SHOW_COLUMN_IMAGE);
         hideShowLocationColumnAction.setImageDescriptor(imageDescriptor);
      }

      /*
       * Hide/Show Description column
       */
      hideShowDescriptionColumnAction = new Action(null, IAction.AS_CHECK_BOX) {
         public void run() {
            TreeColumn descriptionColumn = peripheralsTreeViewer.getTree().getColumn(4);
            descriptionColumn.setWidth(this.isChecked() ? defaultDescriptionColumnWidth : 0);
            descriptionColumn.setResizable(this.isChecked());
         }
      };
      hideShowDescriptionColumnAction.setText("Toggle description column");
      hideShowDescriptionColumnAction.setToolTipText("Toggle description column");
      hideShowDescriptionColumnAction.setChecked(true);
      if (Activator.getDefault() != null) {
         ImageDescriptor imageDescriptor = Activator.getDefault().getImageDescriptor(Activator.ID_SHOW_COLUMN_IMAGE);
         hideShowDescriptionColumnAction.setImageDescriptor(imageDescriptor);
      }

      /*
       * Select Device
       */
      setDeviceAction = new Action(null, IAction.AS_PUSH_BUTTON) {
         public void run() {
            DeviceSelectDialogue dialogue = new DeviceSelectDialogue(getSite().getShell(), defaultDeviceOrSvdFilename);
                     
            int result = dialogue.open();
            String deviceOrSvdFilename = dialogue.getDeviceOrFilename(); 
            if ((result != Window.OK) || (deviceOrSvdFilename == null)) {
               // Cancelled etc
               return;
            }
            defaultDeviceOrSvdFilename = deviceOrSvdFilename;
            if ((peripheralsTreeViewer == null) || peripheralsTreeViewer.getControl().isDisposed()) {
//                System.err.println("UsbdmDevicePeripheralsView.Action() - no peripheral view or already disposed()");
               return;
            }
            if (peripheralsModel == null) {
//               System.err.println("UsbdmDevicePeripheralsView.Action() - peripheralsModel == null");
               return;
            }
//            System.err.println("UsbdmDevicePeripheralsView.Action() - Setting peripheral model");
            peripheralsModel.setDevice(deviceOrSvdFilename);
            peripheralsTreeViewer.setInput(peripheralsModel.getModel());
            setDeviceAction.setText(peripheralsModel.getDeviceName());
            
            if (peripheralsModel != null) {
               setDeviceAction.setText(peripheralsModel.getDeviceName());
            }
            else {
               setDeviceAction.setText("Select Device");
            }
         }
      };
      if (peripheralsModel != null) {
         setDeviceAction.setText(peripheralsModel.getDeviceName());
      }
      else {
         setDeviceAction.setText("Select Device");
      }
      setDeviceAction.setToolTipText(DEVICE_TOOLTIP_STRING);
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
   public void sessionStarted(final UsbdmDevicePeripheralsModel aPeripheralsModel) {
//      System.err.println(String.format("UsbdmDevicePeripheralsView.sessionStarted(%s)", (aPeripheralsModel == null) ? "null" : aPeripheralsModel.getDeviceName()));
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((peripheralsTreeViewer == null) || peripheralsTreeViewer.getControl().isDisposed()) {
               // System.err.println("UsbdmDevicePeripheralsView.SessionCreate() - no peripheral view or already disposed()");
               return;
            }
            if (peripheralsModel != null) {
               // Model already set - ignore
//               System.err.println("UsbdmDevicePeripheralsView.sessionStarted() - peripheralsModel != null");
               return;
            }
            peripheralsModel = aPeripheralsModel;
            if (peripheralsModel == null) {
               peripheralsTreeViewer.setInput(null);
               setDeviceAction.setText("No device");
            }
            else {
               // No device description (model) - use default 
               if (peripheralsModel.getModel() == null) {
//                  System.err.println("UsbdmDevicePeripheralsView.sessionStarted() - Using default peripheral model");
                  peripheralsModel.setDevice(defaultDeviceOrSvdFilename);
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
   public void sessionTerminated(final UsbdmDevicePeripheralsModel aPeripheralsModel) {
      
      // System.err.println("UsbdmDevicePeripheralsView.SessionTerminate()");
      Display.getDefault().asyncExec(new Runnable() {
         public void run() {
            if ((peripheralsTreeViewer == null) || peripheralsTreeViewer.getControl().isDisposed()) {
               // System.err.println("UsbdmDevicePeripheralsView.SessionTerminate() - no peripheral view or already disposed()");
               return;
            }
            if (peripheralsModel == null) {
//               System.err.println("UsbdmDevicePeripheralsView.sessionTerminated() - periperalsModel == null");
               return;
            }
            if (peripheralsModel != aPeripheralsModel) {
//               System.err.println("UsbdmDevicePeripheralsView.sessionTerminated() - periperalsModel != aPeriperalsModel");
               return;
            }
            peripheralsTreeViewer.setInput(null);
            peripheralsModel = null;
            setDeviceAction.setText("Device...");
            setDeviceAction.setEnabled(true);
            setDeviceAction.setToolTipText(DEVICE_TOOLTIP_STRING);
         }
      });
   }

   @Override
   public void sessionSuspended(final UsbdmDevicePeripheralsModel aPeripheralsModel, GdbSessionListener.EventType reason) {
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
            if (peripheralsModel != aPeripheralsModel) {
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
            updatePeripheralsInformationPanel();
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
      UsbdmDevicePeripheralsModel peripheralsModel = null;
      // Try with device name
//      peripheralsModel = new UsbdmDevicePeripheralsModel("MK20DX128M5", null);
      // Try with full path
//      peripheralsModel = new UsbdmDevicePeripheralsModel("C:/Users/podonoghue/Development/USBDM/ARM_Devices/STMicro/STM32F030.svd.xml", null);
      // Try with full path
//      peripheralsModel = new UsbdmDevicePeripheralsModel("C:/Users/podonoghue/Development/USBDM/ARM_Devices/Generated/svdReducedMergedOptimisedManual/MK20D5.svd.xml", null);
      peripheralsModel = new UsbdmDevicePeripheralsModel("C:/Users/podonoghue/Development/USBDM/ARM_Devices/Generated/svdReducedMergedOptimisedManual/MK22F12.svd.xml", null);
      // Try illegal path/name
//      peripheralsModel = new UsbdmDevicePeripheralsModel("xxxx", null);
      
      view.sessionStarted(peripheralsModel);

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }

}
