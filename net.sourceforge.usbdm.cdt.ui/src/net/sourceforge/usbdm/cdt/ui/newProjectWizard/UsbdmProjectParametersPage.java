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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.constants.ToolInformationData;
//import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.MemoryRegion;
import net.sourceforge.usbdm.deviceDatabase.MemoryRegion.MemoryRange;
import net.sourceforge.usbdm.deviceDatabase.ui.DeviceSelectorPanel;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 *  USBDM New Project Wizard page "USBDM Project"
 *
 */
public class UsbdmProjectParametersPage extends WizardPage implements IUsbdmProjectTypeSelection {

   private final static String PAGE_ID    = UsbdmConstants.PROJECT_PAGE_ID;
   private final static String PAGE_NAME  = UsbdmConstants.PROJECT_PAGE_NAME;
   private final static String PAGE_TITLE = "USBDM Project Parameters";

   // These constants are used both for the dialogue persistent storage AND the page data map keys
   private final static String TARGET_DEVICE_KEY                = "targetDevice";               //$NON-NLS-1$
   private final static String BUILDTOOLS_KEY                   = "buildTools";                 //$NON-NLS-1$
   private final static String EXTERNAL_LINKER_SCRIPT_KEY       = "externalLinkerScript";       //$NON-NLS-1$

   private InterfaceType         deviceType;
   private Combo                 buildToolCombo;
   private DeviceSelectorPanel   deviceSelector;

   public UsbdmProjectParametersPage(UsbdmNewProjectPage projectSelectionPage) {
      super(PAGE_NAME);
      if (projectSelectionPage == null) {
         this.deviceType = InterfaceType.T_ARM;
      }
      else {
         this.deviceType = projectSelectionPage.getInterfaceType();
      }
      setTitle(PAGE_TITLE);
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
      if (deviceSelector != null) {
         message = deviceSelector.validate();
      }
//      System.err.println("UsbdmProjectParametersPage.validate() - " + ((message==null)?"(null)":message));
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
   
   private void createUsbdmParametersControl(Composite parent) {

      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings == null) {
         System.err.println("UsbdmProjectParametersPage.createUsbdmParametersControl() dialogSettings == null!");
      }
      /*
       * Toolchain group
       * ============================================================
       */
      Group group = new Group(parent, SWT.NONE);
      group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      group.setLayout(new GridLayout(2, false));
      group.setText("Toolchain");

      //
      // Create & Populate Combo for Build tool selection
      //
      Label label = new Label(group, SWT.NONE);
      label.setText("Build tools:"); //$NON-NLS-1$
      buildToolCombo = new Combo(group, SWT.BORDER|SWT.READ_ONLY);
      buildToolCombo.setLayoutData(GridDataFactory.fillDefaults().hint(250, SWT.DEFAULT).create());
      buildToolCombo.select(0);
      loadBuildtoolNames();

      if (dialogSettings != null) {
         String attrValue = dialogSettings.get(deviceType.name()+BUILDTOOLS_KEY);
         if (attrValue != null) {
            buildToolCombo.setText(attrValue);
         }
      }
      
      /*
       * Device selection group
       * ============================================================
       */
      group = new Group(parent, SWT.NONE);
      group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      group.setLayout(new GridLayout(1, false));
      group.setText("Device Selection");

      deviceSelector = new DeviceSelectorPanel(group, SWT.NONE);
      deviceSelector.setTargetType(deviceType.toTargetType());
      deviceSelector.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      deviceSelector.addListener(SWT.CHANGED, new Listener() {
         @Override
         public void handleEvent(Event event) {
            validate();
         }
      });

      if (dialogSettings != null) {
         String deviceName = dialogSettings.get(deviceType.name()+TARGET_DEVICE_KEY);
         if (deviceName != null) {
            deviceSelector.setDevice(deviceName);
         }
      }
      else {
         deviceSelector.setDevice("MK20DX128M5");
      }
   }
     
   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
    */
   @Override
   public void createControl(Composite parent) {
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayout(new GridLayout());
      createUsbdmParametersControl(composite);
      setControl(composite);
      validate();
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
    * @return  String path (or null if not found)
    */
   String findExternalFile(String resourceDirectory, String deviceName, String preferredExtension) {
      if ((deviceName == null) || deviceName.isEmpty()) {
         return null;
      }
      IPath applicationPath = Usbdm.getResourcePath();
      if (applicationPath == null) {
         return null;
      }
      IPath resourceFolder = applicationPath.append(resourceDirectory);
//      System.err.println("UsbdmProjectPage.findExternalFile(), resourceFolder = " + resourceFolder);
      
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
//         System.err.println("UsbdmProjectPage.findExternalFile(), checking = " + filePath.toOSString());
         if (filePath.toFile().exists() && filePath.toFile().isDirectory()) {
            success = true;
            continue;
         }
         filePath = filePath.addFileExtension(preferredExtension);
//         System.err.println("UsbdmProjectPage.findExternalFile(), checking = " + filePath.toOSString());
         if (filePath.toFile().exists() && !filePath.toFile().isDirectory()) {
            success = true;
            continue;
         }
      } while (!success && stillTrying);
      
      if (success) {
//         System.err.println("UsbdmProjectPage.findExternalFile(), found = " + filePath.toOSString());
         return filePath.toPortableString();
      }
      return null;
   }
   
   /**
    * Get external header file
    * 
    * @param   deviceName  Name of device used to determine file name
    * 
    * @return  URL
    */
   private String getExternalProjectHeaderFile(Device device) {
      
      // Try under device name
      String externalHeaderFile = findExternalFile(UsbdmConstants.PROJECT_HEADER_PATH, device.getName(), "h");
      if (externalHeaderFile == null) {
         // Try under alias name
         externalHeaderFile = findExternalFile(UsbdmConstants.PROJECT_HEADER_PATH, device.getAlias(), "h");
      }      
      if (externalHeaderFile == null) { 
         // Try to get subFamily header file
         externalHeaderFile = findExternalFile(UsbdmConstants.PROJECT_HEADER_PATH, device.getSubFamily(), "h");
      }
      if (externalHeaderFile == null) {
         externalHeaderFile = "";
      }
      return externalHeaderFile;
   }
   
   /**
    * Get external vector table file
    * 
    * @param   deviceName          Name of device used to determine file name
    * 
    * @return  device path
    */
   private String getExternalVectorTable(Device device) {

      String externalVectorTableFile = findExternalFile(UsbdmConstants.VECTOR_TABLE_PATH, device.getName(), "c");
      
      if (externalVectorTableFile == null) { 
         String deviceSubFamily = device.getSubFamily();
         if (deviceSubFamily != null) {
            // Try to get subFamily header file
//            System.err.println("Looking for subFamily vector table file: " + deviceSubFamily + ".c"); //$NON-NLS-1$
            externalVectorTableFile = findExternalFile(UsbdmConstants.VECTOR_TABLE_PATH, deviceSubFamily, "c");
         }
      }
      if (externalVectorTableFile == null) {
         return "";
      }
      return externalVectorTableFile;
   }
   
   /**
    * Adds C Device attributes
    * 
    * @param paramMap         Map to add attributes to
    * @param device           Device being used
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
   
   public final static String LINKER_MEMORY_MAP_KINETIS = 
         "  /* Default Map - Unknow device  */\n" +             //$NON-NLS-1$
         "  rom (rx)  : ORIGIN = 0x00000000, LENGTH = 128K\n"+  //$NON-NLS-1$
         "  ram (rwx) : ORIGIN = 0x00800000, LENGTH = 24K\n"+   //$NON-NLS-1$
         "  gpio (rw) : ORIGIN = 0x00c00000, LENGTH = 16\n"+    //$NON-NLS-1$
         "  io (rw)   : ORIGIN = 0x00ff8000, LENGTH = 32K\n";   //$NON-NLS-1$
   
   public final static String LINKER_STACK_SIZE =
         "__stack_size = 0x100;   /* required amount of stack */"; //$NON-NLS-1$
   
   public final static String LINKER_HEAP_SIZE =
         "__heap_size  = 0x100;   /* required amount of heap  */"; //$NON-NLS-1$
   
   public final static String MEM_FORMAT = "  %-14s %-5s : ORIGIN = 0x%08X, LENGTH = 0x%08X\n";
   
   /**
    * Adds the device memory map information to the paramMap
    * 
    * @param device    Device to get memory map for
    * @param paramMap  Map to add memory map to
    */
   private void addLinkerMemoryMap(Device device, Map<String, String> paramMap) {
      String linkerInformation;
      // Set defaults
      switch(deviceType) {
      case T_ARM: 
         linkerInformation = MAP_PREFIX+LINKER_MEMORY_MAP_KINETIS+MAP_SUFFIX+LINKER_STACK_SIZE+LINKER_HEAP_SIZE;
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
      int unknownCount    = 0;
      long ramSize        = 0x100;
      long flashSize      = 0x1000;
      
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
      long gdbGuardAddress = -1;
      for (Iterator<MemoryRegion> it = device.getMemoryRegionIterator();
            it.hasNext();) {
         MemoryRegion memoryRegion = it.next();
         for ( Iterator<MemoryRange> it1 = memoryRegion.iterator();
               it1.hasNext();) {
            MemoryRange memoryRange = it1.next();
            String name   = "";
            String access = "";
            switch (memoryRegion.getMemoryType()) {
            case MemRAM   :
               if (ramRangeCount == 0) {
                  // 1st RAM region - contains stack
                  ramSize = (memoryRange.end-memoryRange.start+1);
                  gdbGuardAddress = memoryRange.end+1;
               }
               name   = String.format("ram%s", getRangeSuffix(ramRangeCount++));
               access = "(rwx)";
               break;
            case MemFLASH : 
               if (flashRangeCount == 0) {
                  // 1st RAM region - contains stack
                  flashSize = (memoryRange.end-memoryRange.start+1);
               }
               name   = String.format("rom%s", getRangeSuffix(flashRangeCount++));
               access = "(rx)";
               break;
            case MemIO    : 
               name   = String.format("io%s", getRangeSuffix(ioRangeCount++));
               access = "(rw)";
               break;
            case MemFlexNVM : 
               name   = String.format("flexNVM%s", getRangeSuffix(flexNVMCount++));
               access = "(rx)";
               break;
            case MemFlexRAM : 
               name   = String.format("flexRAM%s", getRangeSuffix(flexRamCount++));
               access = "(rx)";
               break;
            default: 
               name   = String.format("unknown%s", getRangeSuffix(unknownCount++));
               access = "(r)";
               break;
            }
            if (memoryRange.getName() != null) {
               name = memoryRange.getName();
            }
            memoryMap.append(String.format(MEM_FORMAT, name, access, memoryRange.start, memoryRange.end-memoryRange.start+1));
         }
      }
      if (gdbGuardAddress > 0) {
         memoryMap.append("  /* Guard region above stack for GDB */\n");
         memoryMap.append(String.format(MEM_FORMAT, "gdbGuard", "(r)", gdbGuardAddress, 32));
      }
      memoryMap.append(MAP_SUFFIX);
      
      paramMap.put(UsbdmConstants.LINKER_INFORMATION_KEY,   memoryMap.toString());
      paramMap.put(UsbdmConstants.LINKER_FLASH_SIZE_KEY,    String.format("0x%X", flashSize));
      paramMap.put(UsbdmConstants.LINKER_RAM_SIZE_KEY,      String.format("0x%X", ramSize));
      paramMap.put(UsbdmConstants.LINKER_STACK_SIZE_KEY,    String.format("0x%X", ramSize/4));
      paramMap.put(UsbdmConstants.LINKER_HEAP_SIZE_KEY,     String.format("0x%X", ramSize/4));
   }

   static final String VECTOR_TABLE_INTRO = 
   "/* \n" +
   " * Default Map\n"+
   " * ============================\n"+
   " */\n";
   
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
//      System.err.println("Result for device header file: \'" + externalHeaderFile + "\'"); //$NON-NLS-1$

      // Try to locate device specific vector table file
      String externalVectorTableFile = getExternalVectorTable(device);
//      System.err.println("Result for vector table file: \'" + externalVectorTableFile + "\'"); //$NON-NLS-1$

      addLinkerMemoryMap(device, paramMap);
      String deviceFamily = device.getFamily();
      addDatabaseValues(paramMap, device);

      paramMap.put(UsbdmConstants.CLOCK_TRIM_FREQUENCY_KEY,       String.valueOf(device.getDefaultClockTrimFreq()));            
      paramMap.put(UsbdmConstants.NVM_CLOCK_TRIM_LOCATION_KEY,    String.valueOf(device.getDefaultClockTrimNVAddress()));            

//      System.err.println("Header file: " + externalHeaderFile); //$NON-NLS-1$
//      System.err.println("Vector file: " + externalVectorTableFile);  //$NON-NLS-1$

      try {
         if (externalVectorTableFile.isEmpty()) {
            String cVectorTable = null;
            // Generate vector table from SVD files if possible
            DevicePeripherals devicePeripherals = DevicePeripherals.createDatabase(device.getName());
            if (devicePeripherals == null) {
               devicePeripherals = DevicePeripherals.createDatabase(device.getSubFamily());
            }
            if (devicePeripherals != null) {
               cVectorTable = devicePeripherals.getCVectorTableEntries();
            }
            if (cVectorTable == null) {
               // Generate default vector tables
               System.err.println("UsbdmProjectParametersPage.addDeviceAttributes() - generating default vector table");
               switch(deviceType) {
               case T_ARM: 
               default:
                  cVectorTable = VECTOR_TABLE_INTRO+VectorTable.factory("CM4").getCVectorTableEntries();
                  break;
               case T_CFV1:
                  cVectorTable = VECTOR_TABLE_INTRO+VectorTable.factory("CFV1").getCVectorTableEntries();
                  break;
               case T_CFVX:
                  cVectorTable = VECTOR_TABLE_INTRO+VectorTable.factory("CFV2").getCVectorTableEntries();
                  break;      
               }
            }
            paramMap.put(UsbdmConstants.C_VECTOR_TABLE_KEY, cVectorTable);
         }
      } catch (Exception e) {
         e.printStackTrace();
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

      // No longer support sexternal linker
      paramMap.put(EXTERNAL_LINKER_SCRIPT_KEY,     ""); //$NON-NLS-1$
      paramMap.put(UsbdmConstants.LINKER_FILE_KEY, "Linker-rom.ld");
//      System.err.println("getPageData() => " + UsbdmConstants.LINKER_FILE_KEY+" = \'" + paramMap.get(UsbdmConstants.LINKER_FILE_KEY)+ "\'"); //$NON-NLS-1$ //$NON-NLS-2$
      addDeviceAttributes(device, paramMap);
//      System.err.println("UsbdmProjectPage.getPageData()");
   }

   public Device getDevice() {
      return deviceSelector.getDevice();
   }

   public InterfaceType getInterfaceType() {
      return deviceType;
   }

   public void saveSettings() {
      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings != null) {
         dialogSettings.put(deviceType.name()+BUILDTOOLS_KEY,                    buildToolCombo.getText());
         dialogSettings.put(deviceType.name()+TARGET_DEVICE_KEY,                 deviceSelector.getText());
      }
   }
   
   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Project Parameters");
      shell.setLayout(new FillLayout());
      shell.setSize(500, 350);
      
      Composite composite = new Composite(shell, SWT.NONE);
      composite.setLayout(new FillLayout());

      UsbdmProjectParametersPage page = new UsbdmProjectParametersPage(null);
      page.createControl(composite);

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
//      Map<String, String> map = new HashMap<String, String>();
//      page.getPageData(map);
      display.dispose();
   }

}
