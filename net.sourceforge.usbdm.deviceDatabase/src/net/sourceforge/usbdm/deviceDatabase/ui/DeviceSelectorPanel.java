package net.sourceforge.usbdm.deviceDatabase.ui;

import net.sourceforge.usbdm.deviceDatabase.Activator;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.jni.UsbdmException;

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

   class ErrorModel extends BaseModel {
      public ErrorModel(BaseModel parent, String name) {
         super(parent, name);
      }
   }

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

   private static class PatternMatchPair {
      Pattern pattern;
      String substitution;
      PatternMatchPair(Pattern pattern, String substitution) {
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

   private Filter          fFilter = new Filter();
   private int             fMatchingNodesCount;
   private DeviceModel     fMatchingNode;
   private String          fMatchErrorMsg = null;
   private boolean         fFilterPending  = false;

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
      initialiseTemplates();
   }

   private PatternMatchPair[] armFamilyPatterns  = null;
   private PatternMatchPair[] cfv1FamilyPatterns = null;
   private PatternMatchPair[] cfvxFamilyPatterns = null;
   private PatternMatchPair[] armSubFamilyPatterns = null;
   private PatternMatchPair[] cfv1SubFamilyPatterns = null;
   private PatternMatchPair[] cfvxSubFamilyPatterns = null;

   private Vector<PatternMatchPair[]> loadTemplates(String filename) {
      Path path = null;
      
      filename = "data/"+filename;
      
      BundleContext context = Activator.getBundleContext();
      if (context != null) {
         try {
            //            System.err.println("getDataPath() context = " + context);
            Bundle bundle = context.getBundle();
            //            System.err.println("getDataPath() bundle = " + bundle);
            URL url = FileLocator.find(bundle, new org.eclipse.core.runtime.Path(filename), null);
            //            System.err.println("getDataPath() URL = " + folder);
            url = FileLocator.resolve(url);
            //            System.err.println("getDataPath() URL = " + folder);
            path = Paths.get(url.toURI());
            //            System.err.println("getDataPath() path = " + path);
         } catch (IOException e) {
            e.printStackTrace();
         } catch (URISyntaxException e) {
            e.printStackTrace();
         }
      }
      if (path == null) {
         path = FileSystems.getDefault().getPath(filename);
         //         System.err.println("getDataPath() default path = " + path);
      }
      Vector<PatternMatchPair>armPats = new Vector<>();
      Vector<PatternMatchPair>cfv1Pats = new Vector<>();
      Vector<PatternMatchPair>cfvxPats = new Vector<>();
      Vector<PatternMatchPair[]> rv = null;

      Charset charset = Charset.forName("US-ASCII");
      try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
         int lineCount = 0;
         String line = null;
         while ((line = reader.readLine()) != null) {
            lineCount++;
            String[] values = line.split(",");
            if (values.length == 0) {
               continue;
            }
            values[0] = values[0].trim();
            if (values[0].length() == 0) {
               continue;
            }
            if (values[0].charAt(0) == '#') {
               continue;
            }
            if (values.length < 3) {
               System.err.println("File "+path.toAbsolutePath()+" has illegal format at line #" + lineCount);
               continue;
            }
            Pattern pattern = Pattern.compile(values[1].trim());
            values[2] = values[2].trim();
            if (values[0].trim().equals("ARM")) {
               armPats.add(new PatternMatchPair(pattern, values[2]));
//               System.out.println("ARM - "+line);
            }
            if (values[0].equals("CFV1")) {
               cfv1Pats.add(new PatternMatchPair(pattern, values[2]));
//               System.out.println("CFV1 - "+line);
            }
            if (values[0].equals("CFVx")) {
               cfvxPats.add(new PatternMatchPair(pattern, values[2]));
//               System.out.println("CFVx - "+line);
            }
         }
         rv = new Vector<PatternMatchPair[]>(3);
         rv.add(armPats.toArray(new PatternMatchPair[armPats.size()]));
         rv.add(cfv1Pats.toArray(new PatternMatchPair[armPats.size()]));
         rv.add(cfvxPats.toArray(new PatternMatchPair[armPats.size()]));
      } catch (IOException x) {
         System.err.format("IOException: %s%n", x);
      }
      return rv;
   }

   private void initialiseTemplates() {
      Vector<PatternMatchPair[]> patterns;
      patterns = loadTemplates("device_family_templates");
      armFamilyPatterns  = patterns.get(0);
      cfv1FamilyPatterns = patterns.get(1);
      cfvxFamilyPatterns = patterns.get(2);
      patterns = loadTemplates("device_subfamily_templates");
      armSubFamilyPatterns  = patterns.get(0);
      cfv1SubFamilyPatterns = patterns.get(1);
      cfvxSubFamilyPatterns = patterns.get(2);
   }

   /**
    * Gets the the descriptive family name for a device
    * 
    * @param name Name of device e.g. MK20DX128M5
    * 
    * @return Device prefix for display e.g. "Kinetis K (MK)"
    * @throws UsbdmException 
    */
   private String getFamilyName(String name) {
      PatternMatchPair[] patterns = null;
      switch (fTargetType) {
      case T_ARM:
         patterns = armFamilyPatterns;
         break;
      case T_CFV1:
         patterns = cfv1FamilyPatterns;
         break;
      case T_CFVx:
         patterns = cfvxFamilyPatterns;
         break;
      default:
         break;
      }
      for (PatternMatchPair pair:patterns) {
         Pattern p = pair.pattern;
         Matcher m = p.matcher(name);
         if (m.matches()) {
            return m.replaceAll(pair.substitution);
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
      PatternMatchPair[] patterns = null;
      switch (fTargetType) {
      case T_ARM:
         patterns = armSubFamilyPatterns;
         break;
      case T_CFV1:
         patterns = cfv1SubFamilyPatterns;
         break;
      case T_CFVx:
         patterns = cfvxSubFamilyPatterns;
         break;
      default:
         break;
      }
      for (PatternMatchPair pattern:patterns) {
         Pattern p = pattern.pattern;
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
   void buildTreeModel(IProgressMonitor progressMonitor, BaseModel root) throws InterruptedException {
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
      ArrayList<Device> devices = fDeviceDatabase.getDeviceList();
      SubMonitor subMonitor = SubMonitor.convert(progressMonitor, "Building devcie tree", devices.size());
      for (Device device : devices) {
         IProgressMonitor sub = subMonitor.split(1);
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
         if (sub.isCanceled()) {
            throw new InterruptedException();
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
      fModel = new ErrorModel(null, "root");
      new ErrorModel(fModel, "Scanning configurations");
      fViewer.setInput(fModel);

      Job job = new Job("Scanning configurations") {
         protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask("Scanning configurations", 1000);
            BaseModel model = new BaseModel(null, "root");
            try {
               buildTreeModel(monitor, model);
            } catch (InterruptedException e) {
               model = new ErrorModel(null, "root");
               model.addChild(new ErrorModel(fModel, "Config scan failed"));
               System.err.println("Config scan aborted, reason = "+e.getMessage());
            } finally {
               monitor.done();
            }
            fModel = model;
            Display.getDefault().syncExec(new Runnable() {
               @Override
               public void run() {
                  fViewer.setInput(fModel);
                  fViewer.setAutoExpandLevel(0);
                  //                  fViewer.refresh();
                  setFilter(fDeviceText.getText());
                  identifyFilteredDevice();
                  filterNodesJob();
               }
            });
            return Status.OK_STATUS;
         }
      };
      job.setUser(true);
      job.schedule();
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
      filterNodesJob();
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
      if (model instanceof ErrorModel) {
         isAvailable = true;
      }
      else if (model instanceof DeviceModel) {
         try {
            isAvailable = fFilter.isVisible(model.getName());
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
      fMatchingNodesCount = 0;
      fMatchingNode = null;
      filterNodes(fModel);

      Display.getDefault().syncExec(new Runnable() {
         public void run() {
            if (fMatchingNodesCount == 1) {
               if (forceDevice) {
                  fDeviceText.setText(fMatchingNode.getName());
               }
               fViewer.expandToLevel(fMatchingNode, AbstractTreeViewer.ALL_LEVELS);
               if (fViewer.getSelection().isEmpty()) {
                  fViewer.setSelection(new StructuredSelection(fMatchingNode));
               }
            }
            else if (fMatchingNodesCount < EXPAND_THRESHOLD) {
               fViewer.expandAll();
            }
            identifyFilteredDevice();
            fViewer.refresh();
            forceDevice = false;
         }
      });
      Display.getDefault().syncExec(new Runnable() {
         public void run() {
            notifyListeners(SWT.CHANGED, new Event());
         }
      });
      //      System.err.println("filterNodesJob(): "+fMatchingNodesCount);
   }

   /**
    * Sets conflict check as pending<br>
    * Returns original pending state<br>
    * This is used to fold together multiple checks
    * 
    * @return Whether a check was already pending.
    */
   synchronized boolean testAndSetFilterPending(boolean state) {
      boolean rv = fFilterPending;
      fFilterPending = state;
      return rv;
   }

   /**
    * Filter visible nodes<br>
    * This is done on a delayed thread so that typing is not delayed
    */
   public synchronized void filterNodes() {
      //      System.err.println("filterNodes()");
      if (!testAndSetFilterPending(true)) {
         // Start new check
         Job job = Job.create("", new IJobFunction() {

            @Override
            public IStatus run(IProgressMonitor arg0) {
               try {
                  Thread.sleep(100);
               } catch (InterruptedException e) {
               }
               testAndSetFilterPending(false);
               filterNodesJob();
               return Status.OK_STATUS;
            }
         });
         job.setUser(true);
         job.schedule();
         //         Runnable runnable = new Runnable() {
         //            public void run() {
         //               try {
         //                  Thread.sleep(100);
         //               } catch (InterruptedException e) {
         //               }
         //               testAndSetFilterPending(false);
         //               filterNodesJob();
         //            }
         //         };
         //         new Thread(runnable).start();
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
         fViewer.setSelection(new StructuredSelection(item));
      }
      else {
         fDeviceName = null;
      }
      return item;
   }

   String fFilterName = null;

   /**
    * Set filter for selection
    * 
    * @param name
    */
   private void setFilter(String name) {
      if ((fFilterName != null) && fFilterName.equals(name)) {
         return;
      }
      fFilterName = name;
      try {
         fFilter = new NameFilter(name);
         fMatchErrorMsg = null;
      } catch (PatternSyntaxException e) {
         fMatchErrorMsg = "Illegal filter";
         fFilter = Filter.NullNameFilter;
      }
      filterNodes();
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
            setFilter(fDeviceText.getText());
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