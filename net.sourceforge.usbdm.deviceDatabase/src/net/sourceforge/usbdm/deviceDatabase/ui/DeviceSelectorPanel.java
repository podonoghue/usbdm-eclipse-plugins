package net.sourceforge.usbdm.deviceDatabase.ui;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;

public class DeviceSelectorPanel extends Composite {

   public class ViewContentProvider implements ITreeContentProvider {
      public void inputChanged(Viewer v, Object oldInput, Object newInput) {
      }

      @Override
      public void dispose() {
      }

      @Override
      public Object[] getElements(Object inputElement) {
         ArrayList<BaseModel> children = ((BaseModel) inputElement).getChildren();
         if (children == null) {
            return new Object[0];
         }
         return children.toArray();
      }

      @Override
      public Object[] getChildren(Object parentElement) {
         return ((BaseModel) parentElement).getChildren().toArray();
      }

      @Override
      public Object getParent(Object element) {
         return ((BaseModel) element).getParent();
      }

      @Override
      public boolean hasChildren(Object element) { 
         return getChildren(element).length>0;
      }
   }
   
   class BaseModel {
      private final String                fName;
      private final BaseModel             fParent;
      private final ArrayList<BaseModel>  fChildren = new ArrayList<BaseModel>();
      
      private boolean fAvailable = true;
      
      public BaseModel(BaseModel parent, String name) {
         fName   = name;
         fParent = parent;
         if (parent != null) {
            parent.addChild(this);
         }
      }
      BaseModel getParent() {
         return fParent;
      }
      ArrayList<BaseModel> getChildren() {
         return fChildren;
      }
      void addChild(BaseModel child) {
         fChildren.add(child);
      }
      String getName() {
         return fName;
      }
      @Override
      public String toString() {
         return fName;
      }
      public void setAvailable(boolean available) {
         fAvailable = available;
      }
      public boolean isAvailable() {
         return fAvailable;
      }
   };
   
   class CategoryModel extends BaseModel {
      public CategoryModel(BaseModel parent, String name) {
         super(parent, name);
      }
   }
   
   class DeviceModel extends BaseModel {
      public DeviceModel(BaseModel parent, String name) {
         super(parent, name);
      }
      void addChild(BaseModel child) {
         throw new RuntimeException("Can't add child to device model node");
      }
   }
   
   private static class Pair {
      String pattern;
      String substitution;
      Pair(String pattern, String substitution) {
         this.pattern      = pattern;
         this.substitution = substitution;
      }
   }

   private static class Filter {
      public static final Filter NullNameFilter = new Filter();

      public boolean isVisible(String name) {
         return true;
      }
   }
   
   private static class NameFilter extends Filter {
      private final Pattern filterPattern;
      
      public NameFilter(String pattern) {
         String qPattern = ".*"+pattern.toUpperCase()+".*";
//         System.err.println("NameFilter("+qPattern+")");
         filterPattern = Pattern.compile(qPattern);
      }
      
      public boolean isVisible(String name) {
         return filterPattern.matcher(name).matches();
      }
   }
   
   // Controls
   private Text       fDeviceText   = null;
   private TreeViewer fViewer       = null;
   private Button     fButton       = null;

   private BaseModel  fModel        = null;
   
   // Internal state
   private TargetType      fTargetType     = TargetType.T_ARM;
   private DeviceDatabase  fDeviceDatabase = null;
   private String          fDeviceName     = null;

   private Filter          filter = new Filter();
   private int             fMatchingNodesCount;
   private DeviceModel     fMatchingNode;
   private String          fMatchErrorMsg = null;
   private boolean         filterPending  = false;

   private static final String NO_DEVICE_STRING = "";
   private static final int    EXPAND_THRESHOLD = 20;
   
   /** Used to force device update when setting device name */
   private boolean forceDevice = false;

   /**
    * Construct a device selection panel
    * 
    * @param parent
    * @param style
    */
   public DeviceSelectorPanel(Composite parent, int style) {
      super(parent, style);
      createControl(parent);
   }

   /**
    * Gets the the descriptive family name for a device
    * 
    * @param name Name of device e.g. MK20DX128M5
    * 
    * @return Device prefix for display e.g. "Kinetis K (MK)"
    */
   private String getFamilyName(String name) {
      Pair[] armPatterns = {
            new Pair("^(S32).*$",         "Automotive (S32K1xx)"),
            new Pair("^(S9KEA).*$",       "Kinetis E (MKE/S9KEA)"),
            new Pair("^(MKE).*$",         "Kinetis E (MKE/S9KEA)"),
            new Pair("^(MKL).*$",         "Kinetis L (MKL)"),
            new Pair("^(MKM).*$",         "Kinetis M (MKM)"),
            new Pair("^(MKV).*$",         "Kinetis V (MKV)"),
            new Pair("^(MK)[0-9].*$",     "Kinetis K (MK)"),
            new Pair("^(FRDM|R41).*$",    "Evaluation boards (FRDM)"),
            new Pair("^(TWR).*$",         "Tower boards (TWR)"),
            new Pair("^(STM32).*$",       "ST Micro ($1)"),
            new Pair("^(LPC).*$",         "NXP LPC ($1)"),
            new Pair("^([a-zA-Z]+).*$",   "$1"),
      };
      Pair[] cfv1Patterns = {
            new Pair("^(MCF[0-9]*).*$",   "$1"),
            new Pair("^([a-zA-Z]+).*$",   "$1"),
      };
      Pair[] cfvxPatterns = {
            new Pair("^(MCF[0-9]{4}).*$", "$1"),
      };
      Pair[] patterns = null;
      switch (fTargetType) {
      case T_ARM:
         patterns = armPatterns;
         break;
      case T_CFV1:
         patterns = cfv1Patterns;
         break;
      case T_CFVx:
         patterns = cfvxPatterns;
         break;
      default:
         break;
      }
      for (Pair pattern:patterns) {
         Pattern p = Pattern.compile(pattern.pattern);
         Matcher m = p.matcher(name);
         if (m.matches()) {
            return m.replaceAll(pattern.substitution);
         }
      }
      return name;
   }

   /**
    * Gets the the descriptive sub-family name for a device
    * 
    * @param name Name of device e.g. MK20DX128M5
    * 
    * @return Device prefix for display e.g. "MK20xxxM5 (50MHz)"
    */
   private String getSubFamilyNamePrefix(String name) {
      Pair[] armPatterns = {
            new Pair("(S32K[0-9][0-9]).*$",                            "$1"),
            new Pair("(FRDM[-_][a-zA-Z]*).*$",                            "$1"),
            new Pair("(TWR[-_].*)$",                                      "$1"),
            new Pair("^(STM32F[0-9]*).*$",                                "$1"),
            new Pair("^(LPC[0-9][0-9][A-Z|a-z]*).*$",                     "$1"),
            new Pair("^(PK[0-9]*).*$",                                    "$1"),
            new Pair("^(S9KEA)[a-zA-Z]*[0-9]*(M[0-9]+)$",                 "$1xxx$2"),
            new Pair("^(MKE02)Z.*(M2)$",                                  "$1xxx$2 (20MHz)"),
            new Pair("^(MKE02)Z.*(M4)$",                                  "$1xxx$2 (40MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M4)$",      "$1xxx$2 (48MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M5)$",      "$1xxx$2 (50MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M7)$",      "$1xxx$2 (70MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M10)$",     "$1xxx$2 (100MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M12)$",     "$1xxx$2 (120MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M15)$",     "$1xxx$2 (150MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M16)$",     "$1xxx$2 (160MHz)"),
            new Pair("^([a-zA-Z]+[0-9]*)(?:DN|DX|FN|FX|Z|F).*(M18)$",     "$1xxx$2 (180MHz)"),
            //            new Pair("^([a-zA-Z]+[0-9]*).*$",
      };
      Pair[] cfv1Patterns = {
            new Pair("^(MCF[0-9]*[a-zA-z]*).*$", "$1"),
            new Pair("^uuu(.*)$",                "$1"),
            new Pair("^(TWR.*)$",                "$1"),
            new Pair("^([a-zA-Z]+).*$",          "$1"),
      };
      Pair[] cfvxPatterns = {
            new Pair("^(.*)$",      "$1"),
      };
      Pair[] patterns = null;
      switch (fTargetType) {
      case T_ARM:
         patterns = armPatterns;
         break;
      case T_CFV1:
         patterns = cfv1Patterns;
         break;
      case T_CFVx:
         patterns = cfvxPatterns;
         break;
      default:
         break;
      }
      for (Pair pattern:patterns) {
         Pattern p = Pattern.compile(pattern.pattern);
         Matcher m = p.matcher(name);
         if (m.matches()) {
            return m.replaceAll(pattern.substitution);
         }
      }
      return "Unsorted";
   }

   /**
    * Find a leaf node with given name
    * 
    * @param node
    * @param name
    * 
    * @return
    */
   private DeviceModel findDeviceNode(Object node, String name) {
      BaseModel model = (BaseModel)node;
      if (node instanceof DeviceModel) {
         if (((DeviceModel)node).getName().equals(name)) {
            return (DeviceModel)node;
         }
         return null;
      }
      DeviceModel deviceModel = null;
      for (Object child:model.getChildren()) {
         deviceModel = findDeviceNode(child, name);
         if (deviceModel != null) {
            break;
         }
      }
      return deviceModel;
   }
   
   /**
    * Find a category node with given name
    * 
    * @param node
    * @param name
    * 
    * @return
    */
   private CategoryModel findCategoryNode(Object node, String name) {
      BaseModel model = (BaseModel)node;
      if (node instanceof CategoryModel) {
         if (((CategoryModel)node).getName().equals(name)) {
            return (CategoryModel)node;
         }
      }
      CategoryModel deviceModel = null;
      for (Object child:model.getChildren()) {
         deviceModel = findCategoryNode(child, name);
         if (deviceModel != null) {
            break;
         }
      }
      return deviceModel;
   }
   
   /**
    * Build the model representing the device choices
    * @param pm 
    * 
    * @return
    * @throws InterruptedException 
    */
   void buildTreeModel(IProgressMonitor pm, BaseModel root) throws InterruptedException {
      fDeviceName = null;

      if ((fDeviceDatabase == null) || (fDeviceDatabase.getTargetType() != fTargetType)) {
         fDeviceDatabase = new DeviceDatabase(fTargetType);
      }
      if (!fDeviceDatabase.isValid()) {
         fDeviceText.setText("<Device database invalid>");
         return;
      }
      String currentFamily     = null;
      BaseModel familyTree     = null;
      String currentSubFamily  = null;
      BaseModel subFamilyTree  = null;
      for (Device device : fDeviceDatabase.getDeviceList()) {
         IProgressMonitor sub = new SubProgressMonitor(pm, 1);
         try {
            if (device.isHidden()) {
               continue;
            }
            String family = getFamilyName(device.getName());
            if ((familyTree == null) || (currentFamily == null) || !currentFamily.equalsIgnoreCase(family)) {
               familyTree = findCategoryNode(root, family);
            }
            if (familyTree == null) {
               currentFamily = family;
               familyTree = new CategoryModel(root, family);
               currentSubFamily = null;
               subFamilyTree = null;
            }
            String subFamily = getSubFamilyNamePrefix(device.getName());
            if ((subFamilyTree == null) || (currentSubFamily == null) || (!currentSubFamily.equalsIgnoreCase(subFamily))) {
               subFamilyTree = findCategoryNode(familyTree, subFamily);
            }
            if (subFamilyTree == null) {
               currentSubFamily = subFamily;
               subFamilyTree = new CategoryModel(familyTree, currentSubFamily);
            }
            if (device.getName().equalsIgnoreCase(currentSubFamily)) {
               continue;
            }
            new DeviceModel(subFamilyTree, device.getName());
            if (pm.isCanceled()) {
               throw new InterruptedException();
            } 
         } finally {
            sub.done();
         }
      }
   }
   
   /**
    * Set target type<br>
    * Used to determine which device list to display
    * 
    * @param targetType
    * @return 
    */
   public void setTargetType(TargetType targetType) {
      fTargetType = targetType;
      fDeviceName = null;
      fModel      = null;
      
      class ScanConfig extends Job {

         public ScanConfig() {
            super("Scanning configurations");
         }

         @Override
         protected IStatus run(IProgressMonitor pm) {
            pm.beginTask("Scanning configurations", 1000);
            fModel = new BaseModel(null, "root");
            try {
               buildTreeModel(pm, fModel);
            } catch (InterruptedException e) {
               System.err.println("Config scan aborted, reason = "+e.getMessage());
            } finally {
               pm.done();
            }
            return Status.OK_STATUS;
         }
      }
      ScanConfig x = new ScanConfig();

      IProgressMonitor dialog = new NullProgressMonitor();

      IStatus status = x.run(dialog);
      if (status.isOK()) {
         fViewer.setInput(fModel);
      }
      else {
         System.err.println("DeviceSelectorPanel.setTargetType() failed "+status.getMessage());
         fViewer.setInput("Config scan failed");
      }
//      ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
//
//      fModel = new BaseModel(null, "root");
//
//      IRunnableWithProgress runnable = new IRunnableWithProgress() {
//         @Override
//         public void run(IProgressMonitor pm) throws InterruptedException {
//            pm.beginTask( "Scanning configurations", 1000);
//            try {
//               buildTreeModel(pm, fModel);
//            } finally {
//               pm.done();
//            }
//         }
//      };
//
//      try {
//         dialog.run(true, true, runnable);
//      } catch (Exception e) {
//         System.err.println("DeviceSelectorPanel.setTargetType() failed "+e.getMessage());
//         return;
//      }
   }

   /**
    * Set device name<br>
    * Used to determine initially selected device or filter
    *  
    * @param targetName
    */
   public void setDevice(String targetName) {
      if (targetName == null) {
         targetName = "";
      }
      fDeviceName = null;
      fDeviceText.setText(targetName);
      fDeviceText.selectAll();
      identifyFilteredDevice();
      forceDevice = true;
   }

   /**
    * Traverse models marking their visibility according to the filter set
    *  
    * @param model
    * @return
    */
   boolean filterNodes(BaseModel model) {
      boolean isAvailable = false;
      if (model instanceof DeviceModel) {
         try {
            isAvailable = filter.isVisible(model.getName());
         } catch (PatternSyntaxException e) {
            isAvailable = true;
         }
         if (isAvailable) {
            fMatchingNode = (DeviceModel)model;
            fMatchingNodesCount++;
         }
      }
      else {
         for (BaseModel child:model.getChildren()) {
            if (filterNodes(child)) {
               isAvailable = true;
            }
         }
      }
      model.setAvailable(isAvailable);
      return isAvailable;
   }
   
   /**
    * Traverse models marking their visibility according to the filter set
    * @return
    */
   void filterNodesJob() {
      if ((fDeviceText == null) || fDeviceText.isDisposed()) {
         // In case just disposed
         return;
      }
//      System.err.println("filterNodesJob()");
      testAndSetFilterPending(false);
      fMatchingNodesCount = 0;
      fMatchingNode = null;
      filterNodes(fModel);
//      System.err.println("filterNodesJob(): "+fMatchingNodesCount);
      if (fMatchingNodesCount == 1) {
         if (forceDevice) {
            fDeviceText.setText(fMatchingNode.getName());
         }
         fViewer.expandToLevel(fMatchingNode, AbstractTreeViewer.ALL_LEVELS);
      }
      else if (fMatchingNodesCount < EXPAND_THRESHOLD) {
         fViewer.expandAll();
      }
      identifyFilteredDevice();
      fViewer.refresh();
      notifyListeners(SWT.CHANGED, new Event());
      forceDevice = false;
   }

   /**
    * Sets conflict check as pending<br>
    * Returns original pending state<br>
    * This is used to fold together multiple checks
    * 
    * @return Whether a check was already pending.
    */
   synchronized boolean testAndSetFilterPending(boolean state) {
      boolean rv = filterPending;
      filterPending = state;
      return rv;
   }
   
   /**
    * Check for mapping conflicts<br>
    * This is done on a delayed thread for efficiency
    */
   public synchronized void filterNodes() {
//      System.err.println("filterNodes()");
      if (!testAndSetFilterPending(true)) {
         // Start new check
         Runnable runnable = new Runnable() {
            public void run() {
               try {
                  Thread.sleep(100);
               } catch (InterruptedException e) {
               }
               Display.getDefault().syncExec(new Runnable() {
                 public void run() {
                    filterNodesJob();
                 }
               });
            }
         };
         new Thread(runnable).start();
      }
   }
   

   /**
    * See if the current filter value corresponds to an actual device
    * 
    * @return
    */
   DeviceModel identifyFilteredDevice() {
      DeviceModel item = findDeviceNode(fModel, fDeviceText.getText());
      if (item != null) {
         fDeviceName = item.getName();
         fViewer.reveal(item);
      }
      else {
         fDeviceName = null;
      }
      return item;
   }
   
   /**
    * Create control composite
    * 
    * @param parent
    */
   private void createControl(Composite parent) {
      setLayout(new GridLayout(3, false));
      Label label = new Label(this, SWT.NONE);
      label.setText("Search Filter: ");
      fDeviceText = new Text(this, SWT.BORDER);
      fDeviceText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      fDeviceText.setToolTipText("Filter/name of device");
      fDeviceText.addFocusListener(new FocusListener() {
         @Override
         public void focusLost(FocusEvent e) {
         }
         @Override
         public void focusGained(FocusEvent e) {
            fDeviceText.selectAll();
         }
      });
      fDeviceText.setText(NO_DEVICE_STRING);
      fDeviceText.addVerifyListener(new VerifyListener() {
         public void verifyText(VerifyEvent e) {
            e.text = e.text.toUpperCase();
            if (!e.text.matches("[A-Z|0-9|_\\-]*")) {
               e.doit = false;
            }
         }
      });
      fDeviceText.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent arg0) {
            fMatchErrorMsg = null;
            try {
               filter = new NameFilter(fDeviceText.getText());
            } catch (PatternSyntaxException e) {
               fMatchErrorMsg = "Illegal filter";
               filter = Filter.NullNameFilter;
            }
            filterNodes();
         }
      });
      fButton  = new Button(this, SWT.NONE);
      fButton.setText("X");
      fButton.setToolTipText("Clear filter");
      GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
      gd.widthHint = 40;
      fButton.setLayoutData(gd);
      fButton.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent arg0) {
            fDeviceText.setText(NO_DEVICE_STRING);
            fDeviceText.setFocus();
         }
         @Override
         public void widgetDefaultSelected(SelectionEvent arg0) {
         }
      });
      fViewer = new TreeViewer(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
      fViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
      fViewer.getTree().setToolTipText("Available devices");
      fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(SelectionChangedEvent event) {
//            System.err.println("selectionChanged() "+event);
            if (!((TreeViewer)event.getSource()).getControl().isFocusControl()) {
               // Filter selection events due to re-population of tree i.e. when  tree doesn't have focus
               return;
            }
            IStructuredSelection selection = (IStructuredSelection)event.getSelection();
            for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
               Object domain = iterator.next();
               if (domain instanceof DeviceModel) {
                  DeviceModel deviceModel = (DeviceModel)domain;
                  fDeviceName = deviceModel.getName();
                  fDeviceText.setText(fDeviceName);
               }
            }
         }
      });
      fViewer.setContentProvider(new ViewContentProvider());
      fViewer.setInput(fModel);
      fViewer.addFilter(new ViewerFilter() {
         @Override
         public boolean select(Viewer viewer, Object parent, Object item) {
            if (item instanceof BaseModel) {
               boolean rv = ((BaseModel)item).isAvailable();
               return rv;
            }
            return true;
         }
      });
      fViewer.addTreeListener(new ITreeViewerListener() {
         @Override
         public void treeExpanded(TreeExpansionEvent event) {
            if (fMatchingNodesCount <= EXPAND_THRESHOLD) {
               return;
            }
            Object domain = event.getElement();
            if (domain instanceof CategoryModel) {
               CategoryModel deviceModel = (CategoryModel)domain;
               BaseModel parent = deviceModel.getParent();
               for (BaseModel sibling:parent.getChildren()) {
                  if (sibling == domain) {
                     continue;
                  }
                  fViewer.collapseToLevel(sibling, AbstractTreeViewer.ALL_LEVELS);
               }
            }
         }
         @Override
         public void treeCollapsed(TreeExpansionEvent arg0) {
         }
      });
   }
   
   /**
    * Validates control
    * 
    * @return Error message or null if no problems
    */
   public String validate() {
      if ((fDeviceDatabase == null) || (!fDeviceDatabase.isValid())) {
         return "Device database is invalid";
      }
      if (fMatchErrorMsg  != null) {
         return fMatchErrorMsg;
      }
      if ((fDeviceName == null) || (fDeviceName.isEmpty())) {
         return "Select device";
      }
      return null;
   }
   
   /**
    * Get selected device
    * 
    * @return Selected device
    */
   public Device getDevice() {
      if ((fDeviceDatabase == null) || 
          (!fDeviceDatabase.isValid()) || 
          (fDeviceDatabase.getTargetType() != fTargetType)) {
         return null;
      }
      Device device = null;
      if ((fDeviceName != null) && fDeviceDatabase.isValid()) {
         device = fDeviceDatabase.getDevice(fDeviceName);
      }
      return device;
   }

   public int getMatchingDevices() {
      return fMatchingNodesCount;
   }
   
   /**
    * Get name of selected device (or null if none)
    * 
    * @return Selected device
    */
   public String getDeviceName() {
      return fDeviceName;
   }
   
//   @Override
//   public void addListener(int eventType, Listener listener) {
//   }
//
   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);
      shell.setText("Device Selector");
      shell.setSize(500, 400);
      shell.setBackground(new Color(display, 255,0,0));
      shell.setLayout(new FillLayout());
      
      DeviceSelectorPanel deviceSelectorPanel = new DeviceSelectorPanel(shell, SWT.NONE);
      deviceSelectorPanel.setTargetType(TargetType.T_ARM);
      deviceSelectorPanel.setDevice("MK20DX128M5");
      
      shell.layout();
      
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }
}