package net.sourceforge.usbdm.cdt.ui.newProjectWizard;
/*
 Change History
+============================================================================================
| Revision History
+============================================================================================
| 16 Nov 13 | Fixed path lookup for resource files (e.g. header files) on linux   4.10.6.100
| 16 Nov 13 | Added default files header & vector files based upon subfamily      4.10.6.100
+============================================================================================
*/
import java.io.File;
import java.net.URI;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.constants.ToolInformationData;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryRegion;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryRegion.MemoryRange;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.SVD_XML_Parser;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 *  USBDM New Project Wizard page "USBDM Project"
 *
 */
public class UsbdmProjectPage extends WizardPage implements UsbdmProjectTypeSelection {

   private final static String PAGE_ID    = UsbdmConstants.PROJECT_PAGE_ID;
   private final static String PAGE_NAME  = UsbdmConstants.PROJECT_PAGE_NAME;

   // These constants are used both for the dialogue persistent storage AND the page data map keys
   private final static String TARGET_DEVICE_KEY                = "targetDevice";               //$NON-NLS-1$
   private final static String BUILDTOOLS_KEY                   = "buildTools";                 //$NON-NLS-1$
   private final static String EXTERNAL_LINKER_SCRIPT_KEY       = "externalLinkerScript";       //$NON-NLS-1$
   private final static String DONT_GENERATE_LINKER_SCRIPT_KEY  = "dontGenerateLinkerScript";   //$NON-NLS-1$ 

   private InterfaceType      deviceType;

   private Combo              buildToolCombo;
   private Combo              targetDeviceNameCombo;

   private Button             autoGenerateLinkerScript;
   private Label              externalLinkerScript;
   private Button             externalLinkerBrowseButton;
   
   private DeviceDatabase     deviceDatabase = null;

   public UsbdmProjectPage(UsbdmProjectSelectionPage projectSelectionPage) {
      super(PAGE_NAME);
      this.deviceType = projectSelectionPage.getInterfaceType();
      setTitle("USBDM Project");
      setDescription("Select project parameters");
      setPageComplete(false);
      getName();
   }

   public String getPageID() {
    return PAGE_ID;
 }

   /**
    *  Validates control & sets error message
    *  
    * @param message error message (null if none)
    */
   private void validate() {
      String message = null;
      if (!autoGenerateLinkerScript.getSelection()) {
         IPath linkerPath = new Path(externalLinkerScript.getText());
         File linkerFile = linkerPath.toFile();
         if (!linkerFile.isFile() || !linkerFile.canRead()) {
            message = "Linker script file path is invalid"; 
         }
      }
      if (!targetDeviceNameCombo.isEnabled()) {
         message = "Device database is invalid";
      }
      setErrorMessage(message);
      setPageComplete(message == null);
   }

   private String buildToolIds[] = null;
   
   private void loadBuildtoolNames() {
      String currentTool = buildToolCombo.getText();
      buildToolCombo.removeAll();
      Hashtable<String, ToolInformationData> toolInformationData = ToolInformationData.getToolInformationTable();
      buildToolIds = new String[toolInformationData.size()];
      int index = 0;
      for (ToolInformationData toolInformation:toolInformationData.values()) {
         if (toolInformation.applicableTo(deviceType)) {
            buildToolCombo.add(toolInformation.getDescription());
            buildToolIds[index++] = toolInformation.getBuildToolId();
         }
      }
      // Try to restore current selection
      buildToolCombo.setText(currentTool);
      int buildToolIndex = buildToolCombo.getSelectionIndex();
      if (buildToolIndex<0) {
         buildToolCombo.select(0);
      }
   }
   
   private void updateDevice() {
      String currentDevice = targetDeviceNameCombo.getText();
      targetDeviceNameCombo.removeAll();
      if ((deviceDatabase == null) || (deviceDatabase.getTargetType() != deviceType.targetType)) {
         deviceDatabase = new DeviceDatabase(deviceType.targetType);
      }
      if (!deviceDatabase.isValid()) {
         targetDeviceNameCombo.add("Device database not found");
         targetDeviceNameCombo.setEnabled(false);
      }
      else {
         for (Device device : deviceDatabase.getDeviceList()) {
            if (!device.isHidden()) {
               targetDeviceNameCombo.add(device.getName());
            }
         }
      }
      // Try to restore original device
      targetDeviceNameCombo.setText(currentDevice);
      int targetDeviceIndex = targetDeviceNameCombo.getSelectionIndex();
      if (targetDeviceIndex<0) {
         targetDeviceNameCombo.select(0);
      }
   }

   private void updateLinker() {
//      System.err.println("updateLinker()"); //$NON-NLS-1$
      if (autoGenerateLinkerScript.getSelection()) {
         externalLinkerScript.setEnabled(false);
         externalLinkerBrowseButton.setEnabled(false);
      }
      else {
         externalLinkerScript.setEnabled(true);
         externalLinkerBrowseButton.setEnabled(true);
      }
      validate();
   }

   private Control createUsbdmParametersControl(Composite parent) {

      Group group = new Group(parent, SWT.NONE);
      group.setLayout(new GridLayout(2, false));
      group.setText("USBDM Parameters");

      IDialogSettings dialogSettings = getDialogSettings();
      
      Label label;
      GridData gd;
      
      //
      // Create & Populate Combo for Build tool selection
      //
      label = new Label(group, SWT.NONE);
      label.setText("Build tools:"); //$NON-NLS-1$
      buildToolCombo = new Combo(group, SWT.BORDER|SWT.READ_ONLY);
      gd = new GridData();
      gd.widthHint = 250;
      buildToolCombo.setLayoutData(gd);
      buildToolCombo.select(0);
      loadBuildtoolNames();

      if (dialogSettings != null) {
         String attrValue = dialogSettings.get(deviceType.name()+BUILDTOOLS_KEY);
         if (attrValue != null) {
            buildToolCombo.setText(attrValue);
         }
      }
      
      //
      // Create & Populate Combo for USBDM devices
      //
      label = new Label(group, SWT.NONE);
      label.setText("Target Device:"); //$NON-NLS-1$
      targetDeviceNameCombo = new Combo(group, SWT.BORDER|SWT.READ_ONLY);
      gd = new GridData();
      gd.widthHint = 200;
      targetDeviceNameCombo.setLayoutData(gd);
      targetDeviceNameCombo.select(0);
      updateDevice();

      if (dialogSettings != null) {
         String attrValue = dialogSettings.get(deviceType.name()+TARGET_DEVICE_KEY);
         if (attrValue != null) {
            targetDeviceNameCombo.setText(attrValue);
         }
      }
      return group;
   }
   
   private Control createLinkerParametersControl(final Composite parent) {
      GridLayout layout;

      Group group = new Group(parent, SWT.NONE);
      group.setText("Linker Parameters");
      //
      layout = new GridLayout();
      group.setLayout(layout);

      //
      // Custom linker file checkbox
      //
      autoGenerateLinkerScript = new Button(group, SWT.CHECK);
      autoGenerateLinkerScript.setText("Auto-generate linker script");
      autoGenerateLinkerScript.setToolTipText("The Wizard will generate a basic linker script based on target device choice");
      autoGenerateLinkerScript.addSelectionListener(new SelectionListener() {
         public void widgetSelected(SelectionEvent e) {
            updateLinker();
         }
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });
      //
      // Linker file browse
      //
      Composite composite = new Composite(group, SWT.NO_TRIM | SWT.NO_FOCUS);
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      composite.setLayoutData(gd);
      layout = new GridLayout(3, false);
      composite.setLayout(layout);
      composite.setBackground(group.getParent().getBackground());

      Label label = new Label(composite, SWT.NONE);
      gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
      label.setLayoutData(gd);
      label.setText("Linker script:"); 

      externalLinkerScript = new Label(composite, SWT.BORDER);
      gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
      externalLinkerScript.setLayoutData(gd);
      externalLinkerScript.setToolTipText("This linker script will be copied to the Project_Settings directory");
      //      linkerScript.setEditable(false);
      externalLinkerBrowseButton = new Button(composite, SWT.PUSH);
      gd = new GridData(SWT.FILL, SWT.FILL, false, false);
      externalLinkerBrowseButton.setLayoutData(gd);
      externalLinkerBrowseButton.setText("Browse");      
      externalLinkerBrowseButton.setToolTipText("Browse for linker script");
      externalLinkerBrowseButton.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            String[] filterExts = {"*.ld"}; //$NON-NLS-1$
            FileDialog fd = new org.eclipse.swt.widgets.FileDialog(parent.getShell(), SWT.OPEN);
            fd.setFileName(externalLinkerScript.getText());

            fd.setText("USBDM - Select Linker File");
            fd.setFilterExtensions(filterExts);
            String fileName = fd.open();
            if (fileName != null) {
               externalLinkerScript.setText(fileName);
            }
            updateLinker();
         }

         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }}
            );

      autoGenerateLinkerScript.setSelection(true);
      externalLinkerScript.setText(""); //$NON-NLS-1$
      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings != null) {
         String stringAttrValue = dialogSettings.get(deviceType.name()+EXTERNAL_LINKER_SCRIPT_KEY);
         if (stringAttrValue != null) {
            externalLinkerScript.setText(stringAttrValue);
         }
         autoGenerateLinkerScript.setSelection(!dialogSettings.getBoolean(deviceType.name()+DONT_GENERATE_LINKER_SCRIPT_KEY));
      }
      return group;
   }
   
   private void createUsbdmControl(Composite parent) {
      GridData gd;
      Control  control;
      parent.setLayout(new GridLayout());

      control = createUsbdmParametersControl(parent);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);

      control = createLinkerParametersControl(parent);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);

   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
    */
   @Override
   public void createControl(Composite parent) {
      
      Composite control = new Composite(parent, SWT.NONE);
      control.setLayout(new GridLayout());

      createUsbdmControl(control);
      
      setControl(control);
      updateLinker();
      updateDevice();
   }

   public String getSelectedBuildToolId() {
      
      int index = buildToolCombo.getSelectionIndex();
      if (index > buildToolIds.length) {
         return "";
      }
      return buildToolIds[index];
   }
   
   static String getRangeSuffix(int count) {
      if (count == 0) {
         return "   "; //$NON-NLS-1$
      }
      else if (count <= 9) {
         return "_" + Integer.toString(count) + " "; //$NON-NLS-1$ //$NON-NLS-2$
      }
      else {
         return "_" + Integer.toString(count); //$NON-NLS-1$
      }
   }

   /**
    * Attempts to locate an external file based on the device name
    * 
    * @param   resourceDirectory   Where to look in USBDM resource area
    * @param   deviceName          Name of device used to determine file name
    * @param   preferredExtension  Preferred file extension e.g. "h" or "c"
    * 
    * @return  URL (empty one if not found)
    */
   URI findExternalFile(String resourceDirectory, String deviceName, String preferredExtension) {

      
      IPath applicationPath = Usbdm.getResourcePath();
      if (applicationPath == null) {
         return null;
      }
      IPath resourceFolder = applicationPath.append(resourceDirectory);
      System.err.println("UsbdmProjectPage.findExternalFile(), resourceFolder = " + resourceFolder);
      
      IPath   filePath = null;
      boolean success     = false;
      boolean stillTrying = true;
      int     attempt = 0;

      do {
         String  fileName = null;
         Pattern p        = null;
         switch(attempt++) {
         case 0: 
            p = Pattern.compile("^(.*)()$");
            break;
         case 1:
            // Remove speed suffix e.g.
            // MK40DN512M10 -> MK40DN512
            // MK40DX64M7   -> MK40DX64
            // MK10FN1M0    -> MK10FN1
            // MK10FX1M0    -> MK10FX1
            // MKL26Z128M4  -> MKL26Z128
            p = Pattern.compile("^(.+)M\\d*()$");
            break;
         case 2:
            // Remove size & Flash modifier
            // MK40DN512M10 -> MK40D10
            // MK40DX64M7   -> MK40D7
            // MK10FN1M10   -> MK10F10
            // MK10FX1M10   -> MK10F10
            // MKL26Z128M4  -> MKL26Z4
            p = Pattern.compile("^(.*[D|F|Z])\\d.M(\\d)*$");
            break;
         default:
            stillTrying = false;
            continue;
         }
         Matcher m = p.matcher(deviceName);
         if (!m.matches()) {
            continue;
         }
         fileName = m.replaceAll("$1$2");
         filePath = resourceFolder.append(fileName);
         System.err.println("UsbdmProjectPage.findExternalFile(), checking = " + filePath.toOSString());
         if (filePath.toFile().exists() && filePath.toFile().isDirectory()) {
            success = true;
            continue;
         }
         filePath = filePath.addFileExtension(preferredExtension);
         System.err.println("UsbdmProjectPage.findExternalFile(), checking = " + filePath.toOSString());
         if (filePath.toFile().exists() && !filePath.toFile().isDirectory()) {
            success = true;
            continue;
         }
      } while (!success && stillTrying);
      
      if (success) {
         System.err.println("UsbdmProjectPage.findExternalFile(), found = " + filePath.toOSString());
         return filePath.toFile().toURI();
      }
      return null;
   }
   
   /**
    * Get external header file
    * 
    * @param   deviceName          Name of device used to determine file name
    * 
    * @return  URL
    */
   private String getExternalProjectHeaderFile(Device device) {
      
      URI externalHeaderFile = findExternalFile(UsbdmConstants.PROJECT_HEADER_PATH, device.getName(), "h");
      
      if (externalHeaderFile == null) { 
         String deviceSubFamily = device.getSubFamily();
         if (deviceSubFamily != null) {
            // Try to get subFamily header file
            System.err.println("Looking for subFamily header file: " + deviceSubFamily + ".h"); //$NON-NLS-1$
            externalHeaderFile = findExternalFile(UsbdmConstants.PROJECT_HEADER_PATH, deviceSubFamily, "h");
         }
      }
      if (externalHeaderFile == null) {
         return "";
      }
      return externalHeaderFile.toString();
   }
   
   /**
    * Get external vector table file
    * 
    * @param   deviceName          Name of device used to determine file name
    * 
    * @return  URL
    */
   private String getExternalVectorTable(Device device) {

      URI externalVectorTableFile = findExternalFile(UsbdmConstants.VECTOR_TABLE_PATH, device.getName(), "c");
      
      if (externalVectorTableFile == null) { 
         String deviceSubFamily = device.getSubFamily();
         if (deviceSubFamily != null) {
            // Try to get subFamily header file
            System.err.println("Looking for subFamily vector table file: " + deviceSubFamily + ".c"); //$NON-NLS-1$
            externalVectorTableFile = findExternalFile(UsbdmConstants.VECTOR_TABLE_PATH, deviceSubFamily, "c");
         }
      }
      if (externalVectorTableFile == null) {
         return "";
      }
      return externalVectorTableFile.toString();
   }
   
   /**
    * Adds C Device attributes
    * 
    * @param paramMap  Map to add attributes to
    * @param device    Device being used
    * @param deviceSubFamily 
    */
   private void addDatabaseValues(Map<String, String> paramMap, Device device) {
      String parameters = "";
      long soptAddress = device.getSoptAddress();
      String deviceSubFamily = device.getFamily();
      if (soptAddress != 0) {
         switch(deviceType) {
         case T_ARM: 
            break;
         case T_CFV1:
            if (deviceSubFamily.equals(UsbdmConstants.SUB_FAMILY_CFV1)) {
               parameters +=   
                     "#ifndef SOPT1\n" +
                     String.format("#define SOPT1 (*(uint8_t*) 0x%X)\n",soptAddress) +
                     "#endif\n";
            }
            else if (deviceSubFamily.equals(UsbdmConstants.SUB_FAMILY_CFV1_PLUS)) {
               parameters +=   
                     "#ifndef SIM_COPC\n" +
                     String.format("#define SIM_COPC (*(uint8_t*) (0x%X+0x0A))\n",soptAddress) +
                     "#endif\n";
            }
            break;
         case T_CFVX:
         default:
            break;      
         }         
      }
      paramMap.put(UsbdmConstants.C_DEVICE_PARAMETERS_KEY, parameters);
   }
   
   public final static String MAP_PREFIX = 
         "MEMORY\n" + //$NON-NLS-1$
         "{\n";         //$NON-NLS-1$
   
   public final static String MAP_SUFFIX = 
         "};\n";         //$NON-NLS-1$
   
   // Default memory maps
   public final static String LINKER_MEMORY_MAP_COLDFIRE_V1 = 
         "  /* Default Map - Unknow device */\n" +              //$NON-NLS-1$
         "  rom (rx)  : ORIGIN = 0x00000000, LENGTH = 128K\n"+  //$NON-NLS-1$
         "  ram (rwx) : ORIGIN = 0x00800000, LENGTH = 24K\n"+   //$NON-NLS-1$
         "  gpio (rw) : ORIGIN = 0x00c00000, LENGTH = 16\n"+    //$NON-NLS-1$
         "  io (rw)   : ORIGIN = 0x00ff8000, LENGTH = 32K\n";   //$NON-NLS-1$
   
   public final static String LINKER_MEMORY_MAP_COLDFIRE_Vx = 
         "  /* Default Map - Unknow device  */\n" +             //$NON-NLS-1$
         "  rom (rx)  : ORIGIN = 0x00000000, LENGTH = 128K\n"+  //$NON-NLS-1$
         "  ram (rwx) : ORIGIN = 0x00800000, LENGTH = 24K\n"+   //$NON-NLS-1$
         "  gpio (rw) : ORIGIN = 0x00c00000, LENGTH = 16\n"+    //$NON-NLS-1$
         "  io (rw)   : ORIGIN = 0x00ff8000, LENGTH = 32K\n";   //$NON-NLS-1$
   
   public final static String LINKER_MEMORY_MAP_COLDFIRE_KINETIS = 
         "  /* Default Map - Unknow device  */\n" +             //$NON-NLS-1$
         "  rom (rx)  : ORIGIN = 0x00000000, LENGTH = 128K\n"+  //$NON-NLS-1$
         "  ram (rwx) : ORIGIN = 0x00800000, LENGTH = 24K\n"+   //$NON-NLS-1$
         "  gpio (rw) : ORIGIN = 0x00c00000, LENGTH = 16\n"+    //$NON-NLS-1$
         "  io (rw)   : ORIGIN = 0x00ff8000, LENGTH = 32K\n";   //$NON-NLS-1$
   
   public final static String LINKER_STACK_SIZE =
         "__stack_size = 0x100;   /* required amount of stack */"; //$NON-NLS-1$
   
   public final static String LINKER_HEAP_SIZE =
         "__heap_size  = 0x100;   /* required amount of heap  */"; //$NON-NLS-1$
   
   private void addLinkerMemoryMap(Device device, Map<String, String> paramMap) {
      String linkerInformation;
      // Set defaults
      switch(deviceType) {
      case T_ARM: 
         linkerInformation = MAP_PREFIX+LINKER_MEMORY_MAP_COLDFIRE_KINETIS+MAP_SUFFIX+LINKER_STACK_SIZE+LINKER_HEAP_SIZE;
         break;
      case T_CFV1:
         linkerInformation = MAP_PREFIX+LINKER_MEMORY_MAP_COLDFIRE_V1+MAP_SUFFIX+LINKER_STACK_SIZE+LINKER_HEAP_SIZE;
         break;
      case T_CFVX:
      default:
         linkerInformation = MAP_PREFIX+LINKER_MEMORY_MAP_COLDFIRE_Vx+MAP_SUFFIX+LINKER_STACK_SIZE+LINKER_HEAP_SIZE;
         break;      
      }

      int flashRangeCount = 0;
      int ramRangeCount   = 0;
      int ioRangeCount    = 0;
      int flexNVMCount    = 0;
      int flexRamCount    = 0;
      long ramSize        = 0x100;
      
      if (device == null) {
         // Use default map
         paramMap.put(UsbdmConstants.LINKER_INFORMATION_KEY,      linkerInformation);
         paramMap.put(UsbdmConstants.LINKER_RAM_SIZE_KEY,   "0x2000");
         paramMap.put(UsbdmConstants.LINKER_STACK_SIZE_KEY, "0x1000");
         paramMap.put(UsbdmConstants.LINKER_HEAP_SIZE_KEY,  "0x1000");
      }
      
      StringBuilder memoryMap = new StringBuilder(String.format(
            "/*\n" +
            " * Memory Map generated by USBDM New Project Wizard for %s\n" +
            " */\n", device.getName())); //$NON-NLS-1$
      
      memoryMap.append(MAP_PREFIX);
      for (Iterator<MemoryRegion> it = device.getMemoryRegionIterator();
            it.hasNext();) {
         MemoryRegion memoryRegion = it.next();
         String template = ""; //$NON-NLS-1$
         for ( Iterator<MemoryRange> it1 = memoryRegion.iterator();
               it1.hasNext();) {
            MemoryRange memoryRange = it1.next();
            switch (memoryRegion.getMemoryType()) {
            case MemFLASH : 
               template = "  rom%s     (rx)  : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap.append(String.format(template, getRangeSuffix(flashRangeCount++), memoryRange.start, memoryRange.end-memoryRange.start+1));
               break;
            case MemRAM   :
               if (ramRangeCount == 0) {
                  // 1st RAM region - contains stack
                  ramSize = (memoryRange.end-memoryRange.start+1);
               }
               template = "  ram%s     (rwx) : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap.append(String.format(template, getRangeSuffix(ramRangeCount++), memoryRange.start, memoryRange.end-memoryRange.start+1));
               break;
            case MemIO    : 
               template = "  io%s      (rwx) : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap.append(String.format(template, getRangeSuffix(ioRangeCount++), memoryRange.start, memoryRange.end-memoryRange.start+1));
               break;
            case MemFlexNVM : 
               template = "  flexNVM%s (rx)  : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap.append(String.format(template, getRangeSuffix(flexNVMCount++), memoryRange.start, memoryRange.end-memoryRange.start+1));
               break;
            case MemFlexRAM : 
               template = "  flexRAM%s (rx)  : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap.append(String.format(template, getRangeSuffix(flexRamCount++), memoryRange.start, memoryRange.end-memoryRange.start+1));
               break;
            default: break;            
            }
         }
      }
      memoryMap.append(MAP_SUFFIX);
      
      paramMap.put(UsbdmConstants.LINKER_INFORMATION_KEY,   memoryMap.toString());
      paramMap.put(UsbdmConstants.LINKER_RAM_SIZE_KEY,      String.format("0x%X", ramSize));
      paramMap.put(UsbdmConstants.LINKER_STACK_SIZE_KEY,    String.format("0x%X", ramSize/4));
      paramMap.put(UsbdmConstants.LINKER_HEAP_SIZE_KEY,     String.format("0x%X", ramSize/4));
   }

   private final String defaultArmVectorTable = 
         "/* \n" +
         " * Default Map\n"+
         " * ++++++++++++++++++++\n"+
         " * Each vector is assigned an unique name.  This is then 'weakly' assigned to the \n"+
         " * default handler. \n"+
         " * To install a handler, create a function with the name shown and it will override \n"+
         " * the weak default. \n"+
         " */ \n"+
         "void NMI_Handler(void)            WEAK_DEFAULT_HANDLER; \n"+
         "void SVC_Handler(void)            WEAK_DEFAULT_HANDLER; \n"+
         "void PendSV_Handler(void)         WEAK_DEFAULT_HANDLER; \n"+
         "void SysTick_Handler(void)        WEAK_DEFAULT_HANDLER; \n"+
         " \n"+
         "typedef struct { \n"+
         "   uint32_t *initialSP; \n"+
         "   intfunc  handlers[]; \n"+
         "} VectorTable; \n"+
         " \n"+
         "__attribute__ ((section(\".interrupt_vectors\"))) \n"+
         "VectorTable const __vector_table = { \n"+
         "    &__StackTop,                    /* Vec #0   Initial stack pointer                        */ \n"+
         "    { \n"+
         "          __HardReset,              /* Vec #1   Reset Handler                                */ \n"+
         "          NMI_Handler,              /* Vec #2   NMI Handler                                  */ \n"+
         "(intfunc) HardFault_Handler,        /* Vec #3   Hard Fault Handler                           */ \n"+
         "          Default_Handler,          /* Vec #4   Reserved                                     */ \n"+
         "          Default_Handler,          /* Vec #5   Reserved                                     */ \n"+
         "          Default_Handler,          /* Vec #6   Reserved                                     */ \n"+
         "          Default_Handler,          /* Vec #7   Reserved                                     */ \n"+
         "          Default_Handler,          /* Vec #8   Reserved                                     */ \n"+
         "          Default_Handler,          /* Vec #9   Reserved                                     */ \n"+
         "          Default_Handler,          /* Vec #10  Reserved                                     */ \n"+
         "          SVC_Handler,              /* Vec #11  SVCall Handler                               */ \n"+
         "          Default_Handler,          /* Vec #12  Reserved                                     */ \n"+
         "          Default_Handler,          /* Vec #13  Reserved                                     */ \n"+
         "          PendSV_Handler,           /* Vec #14  PendSV Handler                               */ \n"+
         "          SysTick_Handler,          /* Vec #15  SysTick Handler                              */ \n"+
         "    } \n"+
         " } \n\n";
   
   /**
    * Adds device specific attributes to map
    * 
    * @param paramMap Map to add attributes to
    */
   private void addDeviceAttributes(Device device, Map<String, String> paramMap) {
      
      if (device == null) {
         return;
      }
      // Try to locate device specific header file
      String externalHeaderFile = getExternalProjectHeaderFile(device);
      System.err.println("Result for device header file: \'" + externalHeaderFile + "\'"); //$NON-NLS-1$

      // Try to locate device specific vector table file
      String externalVectorTableFile = getExternalVectorTable(device);
      System.err.println("Result for vector table file: \'" + externalVectorTableFile + "\'"); //$NON-NLS-1$

      String deviceFamily;
      // Set defaults
      switch(deviceType) {
      case T_ARM: 
         deviceFamily = UsbdmConstants.SUB_FAMILY_CORTEX_M4;
         break;
      case T_CFV1:
         deviceFamily = UsbdmConstants.SUB_FAMILY_CFV1;
         break;
      case T_CFVX:
      default:
         deviceFamily = UsbdmConstants.SUB_FAMILY_CFV2;
         break;      
      }
      addLinkerMemoryMap(device, paramMap);
      deviceFamily = device.getFamily();
      addDatabaseValues(paramMap, device);

      paramMap.put(UsbdmConstants.CLOCK_TRIM_FREQUENCY_KEY,       String.valueOf(device.getDefaultClockTrimFreq()));            
      paramMap.put(UsbdmConstants.NVM_CLOCK_TRIM_LOCATION_KEY,    String.valueOf(device.getDefaultClockTrimNVAddress()));            

      System.err.println("Header file: " + externalHeaderFile); //$NON-NLS-1$
      System.err.println("Vector file: " + externalVectorTableFile);  //$NON-NLS-1$

      if (externalVectorTableFile.isEmpty()) {
         // Generate vector table from SVD files
         DevicePeripherals devicePeripherals = SVD_XML_Parser.createDatabase(device.getSubFamily());
         if (devicePeripherals != null) {
            paramMap.put(UsbdmConstants.C_VECTOR_TABLE_KEY, devicePeripherals.getCVectorTableEntries());
         }
         else {
            paramMap.put(UsbdmConstants.C_VECTOR_TABLE_KEY, defaultArmVectorTable);
         }
      }
      if (deviceType == InterfaceType.T_ARM) {
         paramMap.put(UsbdmConstants.ARM_LTD_STARTUP_S_FILE_KEY, "startup_ARMLtdGCC_"+deviceFamily+".S");
      }
      paramMap.put(UsbdmConstants.TARGET_DEVICE_SUBFAMILY_KEY, "DEVICE_SUBFAMILY_"+deviceFamily);
      paramMap.put(UsbdmConstants.EXTERNAL_HEADER_FILE_KEY,    externalHeaderFile);
      paramMap.put(UsbdmConstants.EXTERNAL_VECTOR_TABLE_KEY,   externalVectorTableFile);
   }
   
   /*
    Names available in template:
    
    $(buildToolBinPath)       Build tools path (usually a variable reference e.g. "${codesource_bin_path}"
    $(gdbCommand)             GDB command (usually involves a variable reference e.g."${codesourcery_coldfire_prefix}/gdb")
    $(buildToolsId)           Build tools Id e.g. "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.gnuToolsForARM"
    $(targetDevice)           Target device name (mixed case e.g. MCF51cn128)
    $(targetDeviceName)       Target device name (lower case mcf51cn18)
    $(targetDeviceFamily)     Target device family (T_ARM)
    $(targetDeviceSubFamily)  
    $(usbdmGdbSprite)         USBDM GDB sprite (e.g. "usbdm-arm-gdbServer.exe")
    $(externalHeaderFile)     Path to external device specific header file <deviceName>.h (if found)
    $(externalVectorTable)    Path to external device specific vector table file vectors.c (if found)
    $(externalLinkerScript)   Path to external device specific linker file (if found)
    $(usbdmApplicationPath)   Substitution variable ${usbdm_application_path}
    $(linkerFile)             Name of active linker file
    $(startup_ARMLtdGCC_S)    Name of ARM Ltd GCC target specific startup file
    $(clockTrimFrequency)     Clock trim frequency
    $(nvmClockTrimLocation)   Non-volatile clock trim location
    $(pathSeparator)          OS dependent path separator
    $(linkerInformation)      Information for linker file
    $(cVectorTable)           Vector table for insertion into C startup code
    */
   public void getPageData(Map<String, String> paramMap) throws Exception {
      Device device = getDevice();

      paramMap.put(UsbdmConstants.PATH_SEPARATOR_KEY,             String.valueOf(File.separator));

      String buildToolId = getSelectedBuildToolId();
      ToolInformationData toolInfo = ToolInformationData.getToolInformationTable().get(buildToolId);

      if (buildToolId.equals(UsbdmSharedConstants.ARMLTD_ARM_BUILD_ID)) {
         paramMap.put(UsbdmConstants.LINKER_FILE_KEY,             "ARMLtd_GCC-rom.ld");
      }
      else {
         // Path is relative to root of project
         paramMap.put(UsbdmConstants.EXCLUDED_FILES_PATTERN_KEY,  "Startup_Code/.*ARMLtdGCC.*");
      }
      if (buildToolId.equals(UsbdmSharedConstants.CODESOURCERY_ARM_BUILD_ID) ||
          buildToolId.equals(UsbdmSharedConstants.CODESOURCERY_COLDFIRE_BUILD_ID)) {
         paramMap.put(UsbdmConstants.LINKER_FILE_KEY,             "Codesourcery-rom.ld");
      }
      else {
         // Path is relative to root of project
         paramMap.put(UsbdmConstants.EXCLUDED_FILES_PATTERN_KEY,  "Startup_Code/.*Codesourcery.*");
      }
      if (toolInfo == null) {
         paramMap.put(UsbdmConstants.BUILD_TOOL_BIN_PATH_KEY, "");    
         paramMap.put(UsbdmConstants.GDB_COMMAND_KEY,         "gdb");
      }
      else {
         paramMap.put(UsbdmConstants.BUILD_TOOL_BIN_PATH_KEY, "${"+toolInfo.getPathVariableName()+"}");
         paramMap.put(UsbdmConstants.GDB_COMMAND_KEY,         "${"+toolInfo.getPrefixVariableName()+"}gdb");
      }
      paramMap.put(UsbdmConstants.BUILD_TOOL_ID_KEY,          buildToolId);    
      paramMap.put(UsbdmConstants.TARGET_DEVICE_KEY,          device.getName());
      paramMap.put(UsbdmConstants.TARGET_DEVICE_NAME_KEY,     device.getName().toLowerCase());
      paramMap.put(UsbdmConstants.TARGET_DEVICE_FAMILY_KEY,   deviceType.name());
      paramMap.put(UsbdmConstants.USBDM_GDB_SPRITE_KEY,       deviceType.gdbSprite);
         
      if (autoGenerateLinkerScript.getSelection()) {
//         System.err.println("getPageData() => "+"externalLinkerScript = \"\""); //$NON-NLS-1$ //$NON-NLS-2$
         paramMap.put(EXTERNAL_LINKER_SCRIPT_KEY,     ""); //$NON-NLS-1$
      }
      else {
         // Copy specified script
//         System.err.println("getPageData() => "+"externalLinkerScript = \""+externalLinkerScript.getText()+"\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         File filepath = new File(externalLinkerScript.getText());
         paramMap.put(EXTERNAL_LINKER_SCRIPT_KEY,     filepath.toString());
         paramMap.put(UsbdmConstants.LINKER_FILE_KEY, "Custom.ld");
      }
      addDeviceAttributes(device, paramMap);
      System.err.println("UsbdmProjectPage.getPageData()");
   }

   public Device getDevice() {
      if ((deviceDatabase == null) || (deviceDatabase.getTargetType() != deviceType.targetType)) {
         deviceDatabase = new DeviceDatabase(deviceType.targetType);
      }
      Device device = null;
      if (deviceDatabase.isValid()) {
         String deviceName = targetDeviceNameCombo.getText();
         device = deviceDatabase.getDevice(deviceName);
         if (device == null) {
            device = deviceDatabase.getDefaultDevice();
         }
      }
      return device;
   }

   public InterfaceType getInterfaceType() {
      return deviceType;
   }

   public void saveSettings() {
      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings != null) {
         dialogSettings.put(deviceType.name()+BUILDTOOLS_KEY,                    buildToolCombo.getText());
         dialogSettings.put(deviceType.name()+TARGET_DEVICE_KEY,                 targetDeviceNameCombo.getText());
         dialogSettings.put(deviceType.name()+DONT_GENERATE_LINKER_SCRIPT_KEY,   !autoGenerateLinkerScript.getSelection());
         dialogSettings.put(deviceType.name()+EXTERNAL_LINKER_SCRIPT_KEY,        externalLinkerScript.getText());
      }
   }
}
