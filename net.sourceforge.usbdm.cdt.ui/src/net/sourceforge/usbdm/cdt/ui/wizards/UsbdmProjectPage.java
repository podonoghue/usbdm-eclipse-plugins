package net.sourceforge.usbdm.cdt.ui.wizards;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.constants.ToolInformationData;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryRegion;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryRegion.MemoryRange;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.UsbdmException;

import org.eclipse.cdt.ui.templateengine.IWizardDataPage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
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
public class UsbdmProjectPage extends WizardPage implements IWizardDataPage, UsbdmProjectTypeSelection {

   public String getPageID() {
//      System.err.println("WizardPage1.getPageID() => " + pageID);
      return pageID;
   }

   // These constants are used both for the dialogue persistent storage AND the page data map keys
   private final static String TARGET_DEVICE_KEY                = "targetDevice";               //$NON-NLS-1$
   private final static String BUILDTOOLS_KEY                   = "buildTools";                 //$NON-NLS-1$
   private final static String EXTERNAL_LINKER_SCRIPT_KEY       = "externalLinkerScript";       //$NON-NLS-1$
   private final static String DONT_GENERATE_LINKER_SCRIPT_KEY  = "dontGenerateLinkerScript";   //$NON-NLS-1$ 

   private String             pageID;
   private InterfaceType      deviceType;

   private Combo              buildToolCombo;
   private Combo              targetDeviceNameCombo;

   private Button             autoGenerateLinkerScript;
   private Label              externalLinkerScript;
   private Button             externalLinkerBrowseButton;
   
   private IWizardPage        nextPage;
   
   public UsbdmProjectPage(InterfaceType deviceType) throws UsbdmException {
      super("Set project settings");
      pageID          = UsbdmConstants.PAGE_ID + deviceType.toString();
      this.deviceType = deviceType;
      setTitle("USBDM Project");
      setDescription("Select project parameters");
      setPageComplete(false);
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
      DeviceDatabase deviceDatabase = new DeviceDatabase(deviceType.targetType);
      if (!deviceDatabase.isValid()) {
         targetDeviceNameCombo.add("Device database not found");
         targetDeviceNameCombo.setEnabled(false);
      }
      else {
         Iterator<Device> it = deviceDatabase.iterator();
         while(it.hasNext()) {
            Device device = it.next();
            if (!device.isAlias()) {
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

   private String getExternalProjectHeaderFile(String deviceName) {
      
      IPath applicationPath;
      applicationPath = Usbdm.getApplicationPath();
      if (applicationPath == null) {
         return "";
      }
//    System.err.println("DeviceDatabase(): Usbdm.getUsbdmApplicationPath() => " + applicationPath);
//    String dataPath = Usbdm.getUsbdmDataPath();
//    System.err.println("DeviceDatabase(): Usbdm.getUsbdmDataPath()        => " + dataPath);
      String deviceHeaderRoot = applicationPath + UsbdmConstants.PROJECT_HEADER_PATH + deviceName;
      
      File f = new File(deviceHeaderRoot+".h");
      if(f.isFile()) {
//         System.err.println("UsbdmWizardPage.getExternalProjectHeaderFile() looking for FILE \'"+deviceHeaderRoot+".h\' => found");
         return f.toURI().toString();
      }
//      System.err.println("UsbdmWizardPage.getExternalProjectHeaderFile() looking for \'"+deviceHeaderRoot+".h\' => not found");
      f = new File(deviceHeaderRoot);
      if(f.isDirectory()) {
//         System.err.println("UsbdmWizardPage.getExternalProjectHeaderFile() looking for DIR \'"+deviceHeaderRoot+"\' => found");
         return f.toURI().toString();
      }
//      System.err.println("UsbdmWizardPage.getExternalProjectHeaderFile() looking for \'"+deviceHeaderRoot+"\' => not found");
      return "";
   }
   
   private String getExternalVectorTable(String deviceName) {
      IPath applicationPath;
      applicationPath = Usbdm.getApplicationPath();
      if (applicationPath == null) {
         return "";
      }
//    System.err.println("DeviceDatabase(): Usbdm.getUsbdmApplicationPath() => " + applicationPath);
//    String dataPath = Usbdm.getUsbdmDataPath();
//    System.err.println("DeviceDatabase(): Usbdm.getUsbdmDataPath()        => " + dataPath);
      String vectorTable = applicationPath + UsbdmConstants.VECTOR_TABLE_PATH + deviceName + ".c";
      
      File f = new File(vectorTable);
      if(f.exists()) {
//         System.err.println("getExternalVectorTable() looking for \'"+vectorTable+"\' => found");
//         System.err.println("   return: \'" + f.toURI().toString() + "\'");
         return f.toURI().toString();
      }
      else {
//         System.err.println("getExternalVectorTable() looking for \'"+vectorTable+"\' => not found");
         return "";
      }
   }
   
   /**
    * Adds C Device attributes
    * 
    * @param paramMap  Map to add attributes to
    * @param device    Device being used
    * @param deviceSubFamily 
    */
   private void addDatabaseValues(Map<String, String> paramMap, Device device, String deviceSubFamily) {
      String parameters = "";
      long soptAddress = device.getSoptAddress();
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
   
   private String createLinkerMemoryMap(Device device) {
      int flashRangeCount = 0;
      int ramRangeCount   = 0;
      int ioRangeCount    = 0;
      int flexNVMCount    = 0;
      int flexRamCount    = 0;
      long stackSize      = 0x100;
      long heapSize       = 0x000;
      
      String memoryMap = String.format(
            "/*\n" +
            " * Memory Map generated by USBDM New Project Wizard for %s\n" +
            " */\n", device.getName()); //$NON-NLS-1$
      memoryMap += MAP_PREFIX;
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
               memoryMap += String.format(template, getRangeSuffix(flashRangeCount++), memoryRange.start, memoryRange.end-memoryRange.start+1);
               break;
            case MemRAM   :
               if (ramRangeCount == 0) {
                  // 1st RM region - contains stack
                  stackSize = (memoryRange.end-memoryRange.start+1) / 4;
               }
               template = "  ram%s     (rwx) : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap += String.format(template, getRangeSuffix(ramRangeCount++), memoryRange.start, memoryRange.end-memoryRange.start+1);
               break;
            case MemIO    : 
               template = "  io%s      (rwx) : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap += String.format(template, getRangeSuffix(ioRangeCount++), memoryRange.start, memoryRange.end-memoryRange.start+1);
               break;
            case MemFlexNVM : 
               template = "  flexNVM%s (rx)  : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap += String.format(template, getRangeSuffix(flexNVMCount++), memoryRange.start, memoryRange.end-memoryRange.start+1);
               break;
            case MemFlexRAM : 
               template = "  flexRAM%s (rx)  : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap += String.format(template, getRangeSuffix(flexRamCount++), memoryRange.start, memoryRange.end-memoryRange.start+1);
               break;
            default: break;            
            }
         }
      }
      memoryMap += MAP_SUFFIX;
      memoryMap += String.format("__stack_size = 0x%X;   /* required amount of stack */\n", stackSize); //$NON-NLS-1$
      memoryMap += String.format("__heap_size  = 0x%X;   /* required amount of heap  */\n", heapSize); //$NON-NLS-1$
      return memoryMap;
   }

   /**
    * Adds device specific attributes to map
    * 
    * @param paramMap Map to add attributes to
    */
   private void addDeviceAttributes(Map<String, String> paramMap) {
      String deviceName = targetDeviceNameCombo.getText();
      String linkerInformation;
      String deviceSubFamily;
      // Set defaults
      switch(deviceType) {
      case T_ARM: 
         linkerInformation = MAP_PREFIX+LINKER_MEMORY_MAP_COLDFIRE_KINETIS+MAP_SUFFIX+LINKER_STACK_SIZE+LINKER_HEAP_SIZE;
         deviceSubFamily = UsbdmConstants.SUB_FAMILY_CORTEX_M4;
         break;
      case T_CFV1:
         linkerInformation = MAP_PREFIX+LINKER_MEMORY_MAP_COLDFIRE_V1+MAP_SUFFIX+LINKER_STACK_SIZE+LINKER_HEAP_SIZE;
         deviceSubFamily = UsbdmConstants.SUB_FAMILY_CFV1;
         break;
      case T_CFVX:
      default:
         linkerInformation = MAP_PREFIX+LINKER_MEMORY_MAP_COLDFIRE_Vx+MAP_SUFFIX+LINKER_STACK_SIZE+LINKER_HEAP_SIZE;
         deviceSubFamily = UsbdmConstants.SUB_FAMILY_CFV2;
         break;      
      }
      DeviceDatabase deviceDatabase = new DeviceDatabase(deviceType.targetType);
      if (!deviceDatabase.isValid()) {
         System.err.println("Device database not loaded - using default device information"); //$NON-NLS-1$
      }
      else {
         Device device = deviceDatabase.getDevice(deviceName);
         if (device == null) {
            System.err.println("Device \""+deviceName+"\" not found - using default memory map");             //$NON-NLS-1$ //$NON-NLS-2$
         }
         else {
            linkerInformation = createLinkerMemoryMap(device);
            deviceSubFamily = device.getFamily();
            addDatabaseValues(paramMap, device, deviceSubFamily);
            paramMap.put(UsbdmConstants.CLOCK_TRIM_FREQUENCY_KEY,       String.valueOf(device.getDefaultClockTrimFreq()));            
            paramMap.put(UsbdmConstants.NVM_CLOCK_TRIM_LOCATION_KEY,    String.valueOf(device.getDefaultClockTrimNVAddress()));            
         }
      }
      if (deviceType == InterfaceType.T_ARM) {
         paramMap.put(UsbdmConstants.ARM_LTD_STARTUP_S_FILE_KEY, "startup_ARMLtdGCC_"+deviceSubFamily+".S");
      }
      paramMap.put(UsbdmConstants.LINKER_INFORMATION_KEY,       linkerInformation);
      paramMap.put(UsbdmConstants.TARGET_DEVICE_SUBFAMILY_KEY, "DEVICE_SUBFAMILY_"+deviceSubFamily);
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
    */
   @Override
   public Map<String, String> getPageData() {

      String deviceName            = targetDeviceNameCombo.getText();
      
      Map<String, String> paramMap = new HashMap<String, String>();

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
      paramMap.put(UsbdmConstants.TARGET_DEVICE_KEY,          deviceName);
      paramMap.put(UsbdmConstants.TARGET_DEVICE_NAME_KEY,     deviceName.toLowerCase());
      paramMap.put(UsbdmConstants.TARGET_DEVICE_FAMILY_KEY,   deviceType.name());
      paramMap.put(UsbdmConstants.USBDM_GDB_SPRITE_KEY,       deviceType.gdbSprite);
      paramMap.put(UsbdmConstants.EXTERNAL_HEADER_FILE_KEY,   getExternalProjectHeaderFile(deviceName));
      paramMap.put(UsbdmConstants.EXTERNAL_VECTOR_TABLE_KEY,  getExternalVectorTable(deviceName));
         
      if (autoGenerateLinkerScript.getSelection()) {
//         System.err.println("getPageData() => "+"externalLinkerScript = \"\""); //$NON-NLS-1$ //$NON-NLS-2$
         paramMap.put(EXTERNAL_LINKER_SCRIPT_KEY,     ""); //$NON-NLS-1$
      }
      else {
         // Copy specified script
//         System.err.println("getPageData() => "+"externalLinkerScript = \""+externalLinkerScript.getText()+"\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         URL url;
         try {
            File filepath = new File(externalLinkerScript.getText());
            url = filepath.toURI().toURL();
            paramMap.put(EXTERNAL_LINKER_SCRIPT_KEY,     url.toString());
            paramMap.put(UsbdmConstants.LINKER_FILE_KEY, "Custom.ld");
         } catch (MalformedURLException e) {
            e.printStackTrace();
         }
      }
      addDeviceAttributes(paramMap);

      IDialogSettings dialogSettings = getDialogSettings();
//      System.err.println("getDialogSettings() => " + dialogSettings);             //$NON-NLS-1$
      if (dialogSettings != null) {
         dialogSettings.put(deviceType.name()+TARGET_DEVICE_KEY,                 targetDeviceNameCombo.getText());
         dialogSettings.put(deviceType.name()+BUILDTOOLS_KEY,                    buildToolCombo.getText());
         dialogSettings.put(deviceType.name()+EXTERNAL_LINKER_SCRIPT_KEY,        externalLinkerScript.getText());
         dialogSettings.put(deviceType.name()+DONT_GENERATE_LINKER_SCRIPT_KEY,   !autoGenerateLinkerScript.getSelection());
      }
      System.err.println("UsbdmProjectPage.getPageData()");
      for (String s:paramMap.keySet()) {
         System.err.println("MAP("+s+") => \""+paramMap.get(s)+"\"");
      }     
      return paramMap;
   }

   @Override
   public void setNextPage(IWizardPage next) {
      nextPage = next;
   }

   @Override
   public IWizardPage getNextPage() {
      return nextPage;
   }
}
