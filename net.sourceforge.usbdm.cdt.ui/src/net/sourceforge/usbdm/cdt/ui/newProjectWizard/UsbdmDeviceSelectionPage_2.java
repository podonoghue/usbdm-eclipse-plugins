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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.constants.ToolInformationData;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.MemoryRegion;
import net.sourceforge.usbdm.deviceDatabase.MemoryRegion.MemoryRange;
import net.sourceforge.usbdm.deviceDatabase.MemoryType;
import net.sourceforge.usbdm.deviceDatabase.ui.DeviceSelectorPanel;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripheralsFactory;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

/**
 *  USBDM New Project Wizard page "USBDM Project"
 *
 */
public class UsbdmDeviceSelectionPage_2 extends WizardPage implements IUsbdmProjectTypeSelection {

   static final String PAGE_NAME   = "UsbdmProjectParametersPage";
   static final String PAGE_TITLE  = "USBDM Device Selection";
   static final String DESCRIPTION = "Device selected";

   private InterfaceType         fInterfaceType;
   private Combo                 fBuildToolsCombo;
   private DeviceSelectorPanel   fDeviceSelector;
   private String                fBuildToolIds[] = null;
   private Boolean               fHasChanged     = true;

   private String                fBuildToolId = null;
   Map<String, String>           fPageData    = null;

   /**
    * Create page
    * 
    * @param paramMap               
    * @param usbdmNewProjectWizard
    */
   public UsbdmDeviceSelectionPage_2(InterfaceType interfaceType, UsbdmNewProjectWizard usbdmNewProjectWizard) {
      super(PAGE_NAME);
      try {
         fInterfaceType = interfaceType;
      } catch (Exception e) {
         fInterfaceType = InterfaceType.T_ARM;
      }
      setTitle(PAGE_TITLE);
      setDescription(DESCRIPTION);
      setPageComplete(false);
      setWizard(usbdmNewProjectWizard);
   }

   Device lastDevice = null;
   
   /**
    * 
    * @param device
    * 
    * @return true => need to update device information
    */
   synchronized boolean changeDevice(Device device) {
//    System.err.println("changeDevice("+device.getName()+")");
    if (device == lastDevice) {
       return false;
    }
    lastDevice = device;
    fHasChanged = true;
    return true;
   }
   
   /**
    * Updates the internal state
    * This is done on a worker thread as it is time consuming
    * After completion it calls page.setPageComplete() to notify wizard of changes
    */
    void updateState() {
//      System.err.println("updateState()");
      final UsbdmNewProjectWizard wizard = (UsbdmNewProjectWizard) getWizard();
      final UsbdmDeviceSelectionPage_2 page = this;
      final Device device = lastDevice;
      
      if (wizard != null) {
         Job job = new Job("Updating configuration") {
            protected IStatus run(IProgressMonitor monitor) {
               monitor.beginTask("Updating Pages...", 10);
               createPageData(device);
               wizard.setDevice(device);
               wizard.updateParamMap(page);
               Display.getDefault().syncExec(new Runnable() {
                  @Override
                  public void run() {
//                     System.err.println("page.setPageComplete()");
                     validate();
//                     page.setPageComplete(true);
                  }
               });
               monitor.done();
               return Status.OK_STATUS;
            }
         };
         job.setUser(true);
         job.schedule();
      }      
   }

   /**
    *  Validates control & sets error message
    */
   private void validate() {
//      System.err.println("validate()");
      fHasChanged = true;
      setPageComplete(false);
      String message = null;
      if (fDeviceSelector != null) {
         message = fDeviceSelector.validate();
         if (message != null) {
            message = message+" (Matching devices: "+fDeviceSelector.getMatchingDevices()+")";
         }
      }
      //      System.err.println("UsbdmProjectParametersPage.validate() - " + ((message==null)?"(null)":message));
      if (message == null) {
         if (changeDevice(getDevice())) {
            setDescription("Updating configuration...");
            updateState();
         }
         else {
            setDescription(DESCRIPTION);
            setPageComplete(true);
         }
      }
      setErrorMessage(message);
//      System.err.println("validate() - complete");
   }

   private void loadBuildtoolNames() {
      String currentTool = fBuildToolsCombo.getText();
      fBuildToolsCombo.removeAll();
      Hashtable<String, ToolInformationData> toolInformationData = ToolInformationData.getToolInformationTable();
      fBuildToolIds = new String[toolInformationData.size()];

      int index = 0;
      for (ToolInformationData toolInformation:toolInformationData.values()) {
         if (toolInformation.applicableTo(fInterfaceType)) {
            fBuildToolsCombo.add(toolInformation.getDescription());
            fBuildToolIds[index++] = toolInformation.getBuildToolId();
         }
      }
      // Try to restore current selection
      fBuildToolsCombo.setText(currentTool);
      updateBuildToolId();
   }

   private void updateBuildToolId() {
      int buildToolIndex = fBuildToolsCombo.getSelectionIndex();
      if (buildToolIndex<0) {
         buildToolIndex = 0;
         fBuildToolsCombo.select(0);
      }
      fBuildToolId = fBuildToolIds[buildToolIndex];
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
      fBuildToolsCombo = new Combo(group, SWT.BORDER|SWT.READ_ONLY);
      fBuildToolsCombo.setLayoutData(GridDataFactory.fillDefaults().hint(250, SWT.DEFAULT).create());
      loadBuildtoolNames();
      fBuildToolsCombo.addListener(SWT.CHANGED, new Listener() {
         @Override
         public void handleEvent(Event paramEvent) {
            updateBuildToolId();
         }
      });
      if (dialogSettings != null) {
         String attrValue = dialogSettings.get(fInterfaceType.name()+UsbdmConstants.BUILD_TOOLS_ID_KEY);
         if (attrValue != null) {
            fBuildToolsCombo.setText(attrValue);
            updateBuildToolId();
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

      fDeviceSelector = new DeviceSelectorPanel(group, SWT.NONE);
      fDeviceSelector.setTargetType(fInterfaceType.toTargetType());
      fDeviceSelector.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      if (dialogSettings != null) {
         String deviceName = dialogSettings.get(fInterfaceType.name()+UsbdmConstants.TARGET_DEVICE_KEY);
         if (deviceName != null) {
            fDeviceSelector.setDevice(deviceName);
         }
      }
      else {
         // TODO - Change default device for testing
         fDeviceSelector.setDevice("FRDM_K20D5");
      }

      fDeviceSelector.addListener(SWT.CHANGED, new Listener() {
         @Override
         public void handleEvent(Event event) {
            validate();
         }
      });

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

   public String getBuildToolsId() {
      return fBuildToolId;
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
            // While name
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
         // Try using subFamily header file
         externalHeaderFile = findExternalFile(UsbdmConstants.PROJECT_HEADER_PATH, device.getSubFamily(), "h");
      }
      if (externalHeaderFile == null) {
         // Try under alias name
         externalHeaderFile = findExternalFile(UsbdmConstants.PROJECT_HEADER_PATH, device.getAlias(), "h");
      }
      if (externalHeaderFile == null) {
         externalHeaderFile = new DevicePeripheralsFactory().getMappedFileName(device.getName());
         // Try under alias name
         externalHeaderFile = findExternalFile(UsbdmConstants.PROJECT_HEADER_PATH, externalHeaderFile, "h");
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
   private void addDeviceCodeValues(Map<String, String> paramMap, Device device) {
      String parameters = "";
      long soptAddress = device.getSoptAddress();
      String deviceSubFamily = device.getFamily();
      if (soptAddress != 0) {
         switch(fInterfaceType) {
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

   private final static String MAP_PREFIX = 
         "MEMORY\n" + //$NON-NLS-1$
               "{\n";         //$NON-NLS-1$

   private final static String MAP_SUFFIX = 
         "};\n\n";         //$NON-NLS-1$

   private final static String MEM_FORMAT = "  %-14s %-5s : ORIGIN = 0x%08X, LENGTH = 0x%08X\n";

   private final static String MEM_DOCUMENTATION = 
         "/*\n"                             +
               " *  <o>  %-6s address <constant>\n" +
               " *  <o1> %-6s size    <constant>\n"  +
               " */\n";

   private final static String LINKER_FLEXNVM_REGION =
         "   /* flexNVM flash region */\n"+
               "   .flexNVM (NOLOAD) :\n" + 
               "   {\n" + 
               "      . = ALIGN(4);\n" + 
               "      KEEP(*(.flexNVM))\n" + 
               "   } > flexNVM\n\n"; //$NON-NLS-1$

   private final static String LINKER_FLEXRAM_REGION =
         "   /* FlexRAM region for non-volatile variables */\n"+
               "   .flexRAM (NOLOAD) :\n" + 
               "   {\n" + 
               "      . = ALIGN(4);\n" + 
               "      KEEP(*(.flexRAM))\n" + 
               "   } > flexRAM\n\n"; //$NON-NLS-1$

   private final static String DEFAULT_RAM_REGION        = "ram";
   private final static String DEFAULT_RAM_HIGH_REGION   = "ram_high";
   private final static String DEFAULT_RAM_LOW_REGION    = "ram_low";
   
   /**
    * Write a set of memory region descriptions
    * 
    * @param memoryRanges	
    * @param pCapName
    * @param pName
    * @param string 
    * 
    * @return the description
    */
   String writeRegions(final ArrayList<MemoryRange>  memoryRanges, String pCapName, String pName, String attributes) {
      StringBuilder memoryMap = new StringBuilder();
      int suffix  = 0;
      String capName = pCapName;
      String name    = pName;
      for(MemoryRange region:memoryRanges) {
         String currentCapName = region.getName();
         String currentName    = region.getName();
         if (currentCapName == null) {
            currentCapName = capName;
         }
         if (currentName == null) {
            currentName = name;
         }
         memoryMap.append(String.format(MEM_DOCUMENTATION, currentCapName, currentCapName));
         memoryMap.append(String.format(MEM_FORMAT, currentName, "("+attributes+")", region.start, region.end-region.start+1));
         suffix++;
         capName = pCapName + suffix; 
         name    = pName + suffix; 
      }
      return memoryMap.toString();
   }

   String writeRegionAlias(String alias, String target) {
      return String.format("REGION_ALIAS(%s,%s);\n", alias, target);
            
   }
   
   
   /**
    * Adds the device memory map information to the paramMap
    * 
    * @param device    Device to get memory map for
    * @param paramMap  Map to add memory map to
    */
   private void addLinkerMemoryMap(Device device, Map<String, String> paramMap) {

      int ioRangeCount    = 0;
      int romCount        = 0;
      int unknownCount    = 0;
      long ramSize        = 0x100;
      long flashSize      = 0x1000;

      StringBuilder memoryMap = new StringBuilder(String.format(
            "/*\n" +
                  " * Memory Map generated by USBDM New Project Wizard for %s\n" +
                  " */\n", device.getName())); //$NON-NLS-1$

      memoryMap.append(MAP_PREFIX);
      long gdbGuardAddress = -1;
      ArrayList<MemoryRange> ramRegions     = new ArrayList<MemoryRange>();
      ArrayList<MemoryRange> flashRegions   = new ArrayList<MemoryRange>();
      ArrayList<MemoryRange> flexNVMRegions = new ArrayList<MemoryRange>();
      ArrayList<MemoryRange> flexRAMRegions = new ArrayList<MemoryRange>();
      for (Iterator<MemoryRegion> it = device.getMemoryRegionIterator();
            it.hasNext();) {
         MemoryRegion memoryRegion = it.next();
         for ( Iterator<MemoryRange> it1 = memoryRegion.iterator();
               it1.hasNext();) {
            MemoryRange memoryRange = it1.next();
            String name   = "";
            String access = "";
            MemoryType memType = memoryRegion.getMemoryType();
            switch (memType) {
            case MemRAM   :
               ramRegions.add(memoryRange);
               continue;
            case MemFLASH : 
               flashRegions.add(memoryRange);
               continue;
            case MemFlexRAM : 
               flexRAMRegions.add(memoryRange);
               continue;
            case MemFlexNVM : 
               flexNVMRegions.add(memoryRange);
               continue;
            case MemIO    : 
               name   = String.format("io%s", getRangeSuffix(ioRangeCount++));
               access = "(rw)";
               break;
            case MemROM:
               name   = String.format("rom%s", getRangeSuffix(romCount++));
               access = "(rx)";
               break;
            case MemDFlash:
            case MemEEPROM:
            case MemInvalid:
            case MemPFlash:
            case MemPRAM:
            case MemPROM:
            case MemXRAM:
            case MemXROM:
            default:
               name   = memType.xmlName+String.format("%s", getRangeSuffix(unknownCount++));
               access = "(r)";
               break;
            }
            if (memoryRange.getName() != null) {
               // Use supplied name
               name = memoryRange.getName();
            }
            memoryMap.append(String.format(MEM_FORMAT, name, access, memoryRange.start, memoryRange.end-memoryRange.start+1));
         }
      }
      flexRAMRegions = coalesce(flexRAMRegions);
      memoryMap.append(writeRegions(flexRAMRegions, "Flex RAM", "flexRAM", "rw"));

      flexNVMRegions = coalesce(flexNVMRegions);
      memoryMap.append(writeRegions(flexNVMRegions, "Flex NVM", "flexNVM", "rx"));

      flashRegions = coalesce(flashRegions);
      memoryMap.append(writeRegions(flashRegions, "FLASH", "flash", "rx"));

      if (flashRegions.size() == 0) {
         // Should never happen!
         System.err.println("Error = flashSize == 0!");
         flashSize = 0;
      }
      else {
         // 1st FLASH region
         flashSize = (flashRegions.get(0).end-flashRegions.get(0).start+1);
      }

      // Don't coalesce RAM regions as there are alignment issues at boundaries.
      // Just choose the largest as the default RAM region

      ramSize        = 0;
      MemoryRange ramRegion = null;
      MemoryRange ramHigh   = null;
      MemoryRange ramLow    = null;

      // Use largest RAM region as default RAM
      for(MemoryRange region:ramRegions) {
         long t = (region.end-region.start+1);
         if (t>=ramSize) {
            ramSize   = t;
            ramRegion = region;
         }
         if (region.getName().equalsIgnoreCase(DEFAULT_RAM_HIGH_REGION)) {
            ramHigh = region;
         }
         if (region.getName().equalsIgnoreCase(DEFAULT_RAM_LOW_REGION)) {
            ramLow = region;
         }
      }
      if (ramRegion == null) {
         // Should never happen!
         System.err.println("Error no RAM region found");
      }
      else {
         // Add GDB guard region at top of RAM
         gdbGuardAddress = ramRegion.end+1;
         memoryMap.append(writeRegions(ramRegions, "RAM", "ram", "rwx"));
         if (gdbGuardAddress > 0) {
            memoryMap.append("/*\n * Guard region above stack for GDB \n */\n");
            memoryMap.append(String.format(MEM_FORMAT, "gdbGuard", "(r)", gdbGuardAddress, 32));
         }
         memoryMap.append(MAP_SUFFIX);
      }
      
      if (!ramRegion.getName().equals(DEFAULT_RAM_REGION)) {
         // Add alias for main RAM region (if needed)
         memoryMap.append(String.format("REGION_ALIAS(\"%s\",\"%s\");\n", DEFAULT_RAM_REGION,   ramRegion.getName()));
      }
      if ((ramHigh != null) && (ramLow != null)) {
         // Assume separate regions for Stack and Heap.
         paramMap.put(UsbdmConstants.LINKER_STACK_SIZE_KEY,    String.format("0x%X", (ramHigh.end-ramHigh.start+1)/2));
         paramMap.put(UsbdmConstants.LINKER_HEAP_SIZE_KEY,     String.format("0x%X", (ramLow.end-ramLow.start+1)/2));
      }
      else  {
         // Assume single RAM region
         paramMap.put(UsbdmConstants.LINKER_STACK_SIZE_KEY,    String.format("0x%X", ramSize/4));
         paramMap.put(UsbdmConstants.LINKER_HEAP_SIZE_KEY,     String.format("0x%X", ramSize/4));
      }
      
      paramMap.put(UsbdmConstants.LINKER_INFORMATION_KEY,   memoryMap.toString());
//    System.err.println(memoryMap.toString());
    
      paramMap.put(UsbdmConstants.LINKER_FLASH_SIZE_KEY,    String.format("0x%X", flashSize));
      paramMap.put(UsbdmConstants.LINKER_RAM_SIZE_KEY,      String.format("0x%X", ramSize));
      
      StringBuilder sb = new StringBuilder();
      if (flexRAMRegions.size()>0) {
         sb.append(LINKER_FLEXRAM_REGION);
      }
      if (flexNVMRegions.size()>0) {
         sb.append(LINKER_FLEXNVM_REGION);
      }
      paramMap.put(UsbdmConstants.LINKER_EXTRA_REGION_KEY, sb.toString());
   }

   private ArrayList<MemoryRange> coalesce(ArrayList<MemoryRange> regions) {
      if (regions.size() <= 1) {
         // Return unchanged
         return regions;
      }
      ArrayList<MemoryRange> newRegions = new ArrayList<MemoryRange>();
      Collections.sort(regions);
      long start = regions.get(0).start;
      long end   = regions.get(0).end;
      for(int sub=1; sub<regions.size(); sub++) {
         if ((end+1) == regions.get(sub).start) {
            end = regions.get(sub).end;
         }
         else {
            newRegions.add(new MemoryRange(start, end));
            start = regions.get(sub).start;
            end   = regions.get(sub).end;
         }
      }
      newRegions.add(new MemoryRange(start, end));
      return newRegions;
   }

   static final String VECTOR_TABLE_INTRO = 
         "/* \n" +
               " * Default Map\n"+
               " * ============================\n"+
               " */\n";

   DevicePeripherals getDevicePeripherals(final Device device) {
      DevicePeripheralsFactory factory = new DevicePeripheralsFactory();
      DevicePeripherals devicePeripherals = factory.getDevicePeripherals(device.getName());
      if (devicePeripherals == null) {
         devicePeripherals = factory.getDevicePeripherals(device.getSubFamily());
      }
      return devicePeripherals;
   }

   /**
    * Adds device specific attributes to map
    * 
    * @param paramMap Map to add attributes to
    * @param device   Device needed to obtain attributes
    */
   private void addDeviceAttributes(Map<String, String> paramMap, Device device) {
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
      addDeviceCodeValues(paramMap, device);

      //      System.err.println("Header file: " + externalHeaderFile); //$NON-NLS-1$
      //      System.err.println("Vector file: " + externalVectorTableFile);  //$NON-NLS-1$

      try {
         if (externalVectorTableFile.isEmpty()) {
            // Generate vector table from SVD files if possible
            DevicePeripherals devicePeripherals = getDevicePeripherals(device);
            String cVectorTable = null;
            if (devicePeripherals != null) {
               cVectorTable = devicePeripherals.getCVectorTableEntries();
            }
            if (cVectorTable == null) {
               // Generate default vector tables
               //               System.err.println("UsbdmProjectParametersPage.addDeviceAttributes() - generating default vector table");
               switch(fInterfaceType) {
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
      String headerFilename = new Path(externalHeaderFile).lastSegment();

      paramMap.put(UsbdmConstants.EXTERNAL_HEADER_FILE_KEY,     externalHeaderFile);
      paramMap.put(UsbdmConstants.EXTERNAL_HEADER_FILENAME_KEY, headerFilename);
      paramMap.put(UsbdmConstants.EXTERNAL_VECTOR_TABLE_KEY,    externalVectorTableFile);
   }

   /**
    * Gets data from this page as a map
    * 
    * @param paramMap Map to add data to
    * 
    * buildToolsBinPath      Build tools path (usually a variable reference e.g. "${usbdm_armLtd_arm_path}"
    * buildToolsId           Build tools Id e.g. "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.armLtdGnuToolsForARM"
    * clockTrimFrequency     Clock trim frequency
    * cDeviceParameters
    * cVectorTable           Vector table for insertion into C startup code
    * externalHeaderFile     Path to external device specific header file <deviceName>.h (if found)
    * externalLinkerScript   Path to external device specific linker file (if found)
    * externalVectorTable    Path to external device specific vector table file vectors.c (if found)
    * gdbCommand             GDB command (usually involves a variable reference e.g."${codesourcery_coldfire_prefix}gdb")
    * linkerFlashSize        e.g.0x100000
    * linkerHeapSize         e.g.0x10000
    * linkerRamSize          e.g.0x40000
    * linkerStackSize        e.g.0x10000
    * linkerInformation      Information for linker file
    * nvmClockTrimLocation   Non-volatile clock trim location
    * pathSeparator          OS dependent path separator
    * startup_ARMLtdGCC_S    Name of ARM Ltd GCC target specific startup file
    * targetDevice           Target device name (mixed case e.g. MCF51cn128)
    * targetDeviceName       Target device name (lower case mcf51cn18)
    * targetDeviceFamily     Target device family (e.g. CortexM4)
    * targetDeviceSubFamily  Target device sub-family (e.g. MK50D10)
    */
   public synchronized void getPageData(Map<String, String> paramMap)  {
      if (fPageData != null) {
         paramMap.putAll(fPageData);
      }
   }

   private synchronized void createPageData(Device device)  {
      
//      System.err.println("createPageData()");

      fPageData = new HashMap<String, String>();

      if (device == null) {
         return;
      }

      fPageData.put(UsbdmConstants.PATH_SEPARATOR_KEY,             String.valueOf(File.separator));

      String buildToolsId = getBuildToolsId();
      ToolInformationData toolInfo = ToolInformationData.getToolInformationTable().get(buildToolsId);
      if (toolInfo == null) {
         fPageData.put(UsbdmConstants.BUILD_TOOLS_BIN_PATH_KEY, "");    
         fPageData.put(UsbdmConstants.GDB_COMMAND_KEY,         "gdb");
      }
      else {
         fPageData.put(UsbdmConstants.BUILD_TOOLS_BIN_PATH_KEY, "${"+toolInfo.getPathVariableName()+"}");
         fPageData.put(UsbdmConstants.GDB_COMMAND_KEY,          "${"+toolInfo.getPrefixVariableName()+"}gdb");
      }
      fPageData.put(UsbdmConstants.BUILD_TOOLS_ID_KEY,          buildToolsId);    

      fPageData.put(UsbdmConstants.INTERFACE_TYPE_KEY,          fInterfaceType.name());

      addDeviceAttributes(fPageData, device);

      // Add launch parameters from device information
      LaunchParameterUtilities.addLaunchParameters(fPageData, device, null);

      //      System.err.println("UsbdmProjectParametersPage_2.updatePageData() - exit");
   }

   public Device getDevice() {
      return fDeviceSelector.getDevice();
   }

   public InterfaceType getInterfaceType() {
      return fInterfaceType;
   }

   /**
    * Indicates if the page has changed since last checked
    * 
    * @return
    */
   public Boolean hasChanged() {
      Boolean hasChanged = fHasChanged;
      fHasChanged = false;
      return hasChanged;
   }

   public void saveSettings() {
      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings != null) {
         dialogSettings.put(fInterfaceType.name()+UsbdmConstants.BUILD_TOOLS_ID_KEY, fBuildToolsCombo.getText());
         dialogSettings.put(fInterfaceType.name()+UsbdmConstants.TARGET_DEVICE_KEY,  fDeviceSelector.getDeviceName());
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

      UsbdmDeviceSelectionPage_2 page = new UsbdmDeviceSelectionPage_2(null, null);
      page.createControl(composite);

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }

}
