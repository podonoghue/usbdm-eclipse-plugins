package net.sourceforge.usbdm.deviceEditor.information;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.w3c.dom.Element;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceEditor.Activator;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ConstantModel;
import net.sourceforge.usbdm.deviceEditor.model.DeviceInformationModel;
import net.sourceforge.usbdm.deviceEditor.model.DevicePackageModel;
import net.sourceforge.usbdm.deviceEditor.model.DeviceVariantModel;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.IModelEntryProvider;
import net.sourceforge.usbdm.deviceEditor.model.ModelFactory;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalPinMapping;
import net.sourceforge.usbdm.deviceEditor.parsers.ParseFamilyCSV;
import net.sourceforge.usbdm.deviceEditor.parsers.ParseFamilyXML;
import net.sourceforge.usbdm.deviceEditor.parsers.ParseMenuXML;
import net.sourceforge.usbdm.deviceEditor.parsers.ParseMenuXML.MenuData;
import net.sourceforge.usbdm.deviceEditor.peripherals.Customiser;
import net.sourceforge.usbdm.deviceEditor.peripherals.DocumentUtilities;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;
import net.sourceforge.usbdm.deviceEditor.peripherals.ProcessProjectActions;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForAdc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForCmp;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForCmt;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForConsole;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForControl;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForCrc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForDac;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForDma;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForDmaMux;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForEnet;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForEwm;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForFlash;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForFlexBus;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForFlexCan;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForFlexio;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForFmc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForFtm;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForFtmShared;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForGpio;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForI2c;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForI2s;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForIcs;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForInterrupt;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForKbi;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLcd;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLlwu;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLptmr;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLpuart;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForMcg;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForMcm;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForNull;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForOsc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForOscRf;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPcc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPdb;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPit;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPmc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPower;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPwt;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForQspi;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForRadio;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForRcm;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForRnga;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForRtc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForScg;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForSdhc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForSdramc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForShared;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForSim;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForSmc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForSpi;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForToDo;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForTrgmux;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForTsi;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForUart;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForUsb;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForUsbPhy;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForUsbdcd;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForUsbhs;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForVref;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForWdog;
import net.sourceforge.usbdm.deviceEditor.validators.Validator;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripheralsFactory;
import net.sourceforge.usbdm.peripheralDatabase.ModeControl;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

public class DeviceInfo extends ObservableModel implements IModelEntryProvider, IModelChangeListener {

   /** Version number */
   public static final String VERSION           = "1.3.0";

   /** DTD file to reference in XML */
   public static final String DTD_FILE          = "_Hardware.dtd";

   /** Name space for C files */
   public static final String NAME_SPACE_USBDM_LIBRARY        = "USBDM";

   /** Name space within USBDM namespace for C variables representing peripheral signals mapped to pins*/
   public static final String NAME_SPACE_SIGNALS  = "SIGNALS";

   /** How to handle existing files etc */
   public enum Mode {ignore, fail};

   /** Device families e.g mk, mke, mkl, mkm, mkv*/
   public enum DeviceFamily {mk, mke, mkl, mkm, mkv, mkw, s32k};

   /** Path of file containing device hardware description */
   private Path fHardwarePath;

   /** Path of file containing project settings */
   private Path fProjectSettingsPath;

   /** Family name of device e.g. MK20D5, MK22FA12 */
   private String fDeviceSubFamily = null;

   /** Device family for this device e.g. mk, mke, mkl, mkm, mkv*/
   private DeviceFamily fDeviceFamily = null;

   /** Map from variant names to variant information e.g. MK20DN32VLH5, FRDM_K20D50M => info*/
   private Map<String, DeviceVariantInformation> fVariantInformationTable = new TreeMap<String, DeviceVariantInformation>();

   /** Information about device variant e.g. MK20DN32VLH5, FRDM_K20D50M */
   private DeviceVariantInformation fVariantInformation = null;

   /** Map of variables for all peripherals */
   private final VariableMap fVariables = new VariableMap();

   /** Indicates if the data has changed since being loaded */
   private boolean fIsDirty = false;

   /** Data obtained from the Menu description file */
   private MenuData fMenuData;

   /** Variable provider for project variable (does not include peripherals) */
   private VariableProvider fVariableProvider = null;

   /** File name extension for project file */
   public static final String PROJECT_FILE_EXTENSION = ".usbdmProject";

   /** File name extension for hardware description file */
   public static final String HARDWARE_FILE_EXTENSION = ".usbdmHardware";

   /** File name extension for hardware description file (CSV format) */
   public static final String HARDWARE_CSV_FILE_EXTENSION = ".csv";

   /** Name of default USBDM project file in Eclipse project */
   public static final String USBDM_PROJECT_FILENAME = "Configure";
   
   /** Relative location of ARM peripheral files in USBDM installation */
   public static final String USBDM_ARM_STATIONERY_LOCATION  = "Stationery/Packages/180.ARM_Peripherals";
   
   /** Relative location of hardware files in USBDM installation */
   public static final String USBDM_HARDWARE_LOCATION  = USBDM_ARM_STATIONERY_LOCATION + "/Hardware";
   
   /** Relative location of hardware files in USBDM installation */
   public static final String USBDM_ARM_PERIPHERALS_LOCATION  = USBDM_HARDWARE_LOCATION + "/peripherals";
   
   /** Key for device variant persistence */
   public static final String USBDMPROJECT_VARIANT_SETTING_KEY       = "$$DeviceInfo_Device_Variant";

   /** Old Key for target family persistence */
   public static final String USBDMPROJECT_OLD_SUBFAMILY_SETTING_KEY = "$$DeviceInfo_Target_Device";

   /** Key for target family persistence */
   public static final String USBDMPROJECT_SUBFAMILY_SETTING_KEY     = "$$DeviceInfo_SubFamily";
   
   /** Key for hardware source file persistence */
   public static final String HARDWARE_SOURCE_FILENAME_SETTINGS_KEY  = "$$Hardware_Source_Filename";

   /**
    * Create empty device information
    */
   private DeviceInfo() {
      Variable.setDeviceInfo(this);
   }
   
   public static Path findFile(Path filePath) throws Exception {
      Path resolvedPath = null;

      try {
         do {
            // As is
            if (Files.isReadable(filePath)) {
               resolvedPath = filePath;
               continue;
            }
            resolvedPath = filePath.toAbsolutePath();
            if (Files.isReadable(resolvedPath)) {
               continue;
            }
            if (!filePath.isAbsolute()) {
               // Try default locations
               // Debug location
               resolvedPath = Paths.get("hardware").resolve(filePath);
               if (Files.isReadable(resolvedPath)) {
                  continue;
               }
               // USBDM installation
               resolvedPath = Paths.get(Usbdm.getUsbdmResourcePath()).resolve(USBDM_HARDWARE_LOCATION).resolve(filePath);
               if (Files.isReadable(resolvedPath)) {
                  continue;
               }
               resolvedPath = null;
            }
         } while (false);
      } catch (UsbdmException e) {
         e.printStackTrace();
      }
      return resolvedPath;
   }
   
   /**
    * Create device hardware description from given file<br>
    * 
    * @param filePath            Path to <b>.csv</b> file
    * @param peripheralVersions  Accumulates peripheral versions
    * 
    * @return Created hardware description for device
    * 
    * @throws Exception
    */
   public static DeviceInfo createFromCsvFile(Path filePath, HashMap<String, HashSet<String>> peripheralVersions) throws Exception {
      
      String filename  = filePath.getFileName().toString();
      if (!filename.endsWith(HARDWARE_CSV_FILE_EXTENSION)) {
         throw new Exception("Incorrect file type"+ filePath);
      }
      Path resolvedPath = findFile(filePath);
      if (resolvedPath == null) {
         throw new Exception("Cannot locate file "+ filePath);
      }
      DeviceInfo deviceInfo = new DeviceInfo();
      deviceInfo.loadHardwareDescriptionFromCsv(resolvedPath, peripheralVersions);
      return deviceInfo;
   }

   /**
    * Create device hardware description from given file<br>
    * 
    * @param filePath   Path to <b>.usbdmHardware</b> file
    * 
    * @return Created hardware description for device
    * 
    * @throws Exception
    */
   public static DeviceInfo createFromHardwareFile(Path filePath) throws Exception {
      
      String filename  = filePath.getFileName().toString();
      if (!filename.endsWith(HARDWARE_FILE_EXTENSION)) {
         throw new Exception("Incorrect file type"+ filePath);
      }
      Path resolvedPath = findFile(filePath);
      if (resolvedPath == null) {
         throw new Exception("Cannot locate file "+ filePath);
      }
      DeviceInfo deviceInfo = new DeviceInfo();
      deviceInfo.loadHardwareDescription(resolvedPath);
      return deviceInfo;
   }
   
   /**
    * Create device hardware description from settings file
    * 
    * @param device
    * @param filePath   Path to <b>.usbdmProject</b>  file
    * 
    * @return Create hardware description for device
    * 
    * @throws Exception
    */
   public static DeviceInfo createFromSettingsFile(Device device, Path filePath) throws Exception {
      
      String filename  = filePath.getFileName().toString();
      if (!filename.endsWith(PROJECT_FILE_EXTENSION)) {
         throw new Exception("Incorrect file type"+ filePath);
      }
      
      filePath = filePath.toAbsolutePath();
      if (!Files.isReadable(filePath)) {
         throw new Exception("Cannot locate file "+ filePath);
      }

      DeviceInfo deviceInfo = new DeviceInfo();

      Settings projectSettings = deviceInfo.getSettings(filePath);
      
      Path hardwarePath = Paths.get(projectSettings.get(HARDWARE_SOURCE_FILENAME_SETTINGS_KEY));

      if (!hardwarePath.isAbsolute()) {
         // Try default locations
         do {
            // As is
            Path path = hardwarePath.toAbsolutePath();
            if (Files.isReadable(path)) {
               hardwarePath = path;
               continue;
            }
            // Debug location
            path = Paths.get("Hardware").resolve(hardwarePath);
            if (Files.isReadable(path)) {
               hardwarePath = path;
               continue;
            }
            // USBDM installation
            hardwarePath = Paths.get(Usbdm.getUsbdmResourcePath()).resolve(USBDM_HARDWARE_LOCATION).resolve(hardwarePath);
         } while (false);
      }
      if (!Files.isReadable(hardwarePath)) {
         throw new Exception("Cannot locate file "+ hardwarePath);
      }
      deviceInfo.loadHardwareDescription(hardwarePath);
      deviceInfo.loadSettings(device, projectSettings);
      
      return deviceInfo;
   }

   /**
    * Load hardware description from file (.csv)
    * 
    * @param hardwarePath  Path to load from
    * @param peripheralVersions     Accumulates peripheral versions
    * 
    * @throws Exception
    */
   private void loadHardwareDescriptionFromCsv(Path hardwarePath, HashMap<String, HashSet<String>> peripheralVersions) throws Exception {
      fHardwarePath = hardwarePath;
      ParseFamilyCSV parser = new ParseFamilyCSV(this);
      parser.parseFile(hardwarePath, peripheralVersions);
   }

   /**
    * Load hardware description from file (.usbdmHardware)
    * 
    * @param hardwarePath  Path to load from
    * 
    * @throws Exception
    */
   private void loadHardwareDescription(Path hardwarePath) throws Exception {
      System.out.println("DeviceInfo.parse(" + hardwarePath.getFileName().toString() + ")");
      fHardwarePath = hardwarePath;
      String filename = fHardwarePath.getFileName().toString();
      if ((filename.endsWith("xml"))||(filename.endsWith(HARDWARE_FILE_EXTENSION))) {
         ParseFamilyXML parser = new ParseFamilyXML();
         parser.parseHardwareFile(this, fHardwarePath);

         fVariableProvider = new VariableProvider("Common Settings", this) {
            // Add change lister to mark editor dirty
            @Override
            public void addVariable(Variable variable) {
               super.addVariable(variable);
               variable.addListener(DeviceInfo.this);
            }
         };
         // Add device sub-family as variable
         addOrUpdateStringVariable("_deviceSubFamily", "/_deviceSubFamily", getDeviceSubFamily(), true);
         
         fMenuData = ParseMenuXML.parseMenuFile("common_settings", fVariableProvider);
         
         ArrayList<PeripheralWithState> peripheralWithStateList = new ArrayList<PeripheralWithState>();
         
         // Construct list of all PeripheralWithState
         for (String name:fPeripheralsMap.keySet()) {
            Peripheral p = fPeripheralsMap.get(name);
            if (p instanceof PeripheralWithState) {
               peripheralWithStateList.add((PeripheralWithState) fPeripheralsMap.get(name));
            }
         }
         // Sort in priority order
         Collections.sort(peripheralWithStateList, new Comparator<PeripheralWithState>() {
            @Override
            public int compare(PeripheralWithState o1, PeripheralWithState o2) {
               return o2.getPriority()-o1.getPriority();
            }
         });
         // Construct peripherals
         for (PeripheralWithState p:peripheralWithStateList) {
            p.loadModels();
         }
         repeatedItemMap.clear();
         for (PeripheralWithState p:peripheralWithStateList) {
            p.instantiateAliases();
         }
      }
      else {
         throw new Exception("Unexpected file type for " + hardwarePath);
      }
   }

   /**
    * Set sub-family name
    * 
    * @param deviceSubFamily  Sub-family name e.g. MK20D5, MK22FA12
    */
   public void setDeviceSubFamily(String deviceSubFamily) {
      if ((fDeviceSubFamily != null) && (fDeviceSubFamily.compareTo(deviceSubFamily) == 0)) {
         return;
      }
      fDeviceSubFamily = deviceSubFamily;
      
      if (fDeviceFamily == null) {
         // A bit crude
         if (deviceSubFamily.startsWith("MKE")) {
            fDeviceFamily = DeviceFamily.mke;
         }
         else if (deviceSubFamily.startsWith("MKL")) {
            fDeviceFamily = DeviceFamily.mkl;
         }
         else if (deviceSubFamily.startsWith("MKM")) {
            fDeviceFamily = DeviceFamily.mkm;
         }
         else if (deviceSubFamily.startsWith("MKW")) {
            fDeviceFamily = DeviceFamily.mkw;
         }
         else if (deviceSubFamily.startsWith("S32")) {
            fDeviceFamily = DeviceFamily.s32k;
         }
         else {
            fDeviceFamily = DeviceFamily.mk;
         }
      }
      setDirty(true);
   }

   /**
    * Get device family for this device
    * 
    * @return Target Device SubFamily e.g. MK20D5, MK22FA12
    */
   public String getDeviceSubFamily() {
      return fDeviceSubFamily;
   }

   /**
    * Set device family
    * 
    * @param familyName  Target Device Family e.g. mk, mke, mkl, mkm, mkv
    */
   public void setDeviceFamily(DeviceFamily deviceFamily) {
      if ((fDeviceFamily != null) && (fDeviceFamily.compareTo(deviceFamily) == 0)) {
         return;
      }
      if (fDeviceFamily.compareTo(deviceFamily) == 0) {
         return;
      }
      fDeviceFamily = deviceFamily;
      setDirty(true);
   }

   /**
    * Get device family for this device
    * 
    * @return Device family e.g. mk, mke, mkl, mkm, mkv
    */
   public DeviceFamily getDeviceFamily() {
      return fDeviceFamily;
   }

   /**
    * Get path to hardware file
    * 
    * @return
    */
   public Path getHardwarePath() {
      return fHardwarePath;
   }

   /**
    * Get hardware file name
    * @return
    */
   public String getSourceFilename() {
      return fHardwarePath.getFileName().toString();
   }

   /**
    * Get path to project settings file
    * 
    * @return
    */
   public Path getProjectSettingsPath() {
      return fProjectSettingsPath;
   }

   /*
    * Peripherals =============================================================================================
    */
   /**
    * Map of all peripherals<br>
    * name -> peripheral
    */
   private Map<String, Peripheral> fPeripheralsMap = new TreeMap<String, Peripheral>();

   /**
    * Create peripheral
    * 
    * @param baseName   Base name e.g. FTM3 => FTM
    * @param instance   Instance of peripheral e.g. FTM2 => 2
    * @param template   Template to use to create peripheral
    * @param mode       Failure action
    * 
    * @return
    */
   public Peripheral createPeripheral(String baseName, String instance, SignalTemplate template, Mode mode) {
      String name = baseName+instance;
      Peripheral peripheral = fPeripheralsMap.get(name);
      if (peripheral != null) {
         if (mode != Mode.ignore) {
            throw new RuntimeException("Attempting to re-create peripheral instance " + name);
         }
         return peripheral;
      }
      peripheral = template.createPeripheral(baseName, instance);
      fPeripheralsMap.put(name, peripheral);
      return peripheral;
   }

   /**
    * Create peripheral with given name and instance
    * 
    * @param baseName      Base name of peripheral e.g. FTM3 => FTM
    * @param instance      Instance e.g. FTM3 => 3
    * @param className     Name of class used to represent peripheral<br>(instance of <i>net.sourceforge.usbdm.deviceEditor.information.Peripheral</i>)
    * @param parameters    Parameters pass to constructor
    * 
    * @return Peripheral created
    * 
    * @throws Exception
    */
   public Peripheral createPeripheral(String baseName, String instance, String className, String parameters) throws Exception {
      //      String args[] = null;
      //      if (parameters != null) {
      //         args = parameters.split(",");
      //      }
      Peripheral peripheral = null;
      try {
         // Get peripheral class
         Class<?> clazz = Class.forName(className);
         peripheral = (Peripheral) clazz.getConstructor(String.class, String.class, this.getClass()).newInstance(baseName, instance, this);
      } catch (Exception e) {
         throw new Exception("Failed to instantiate peripheral from class \'"+className+"\'", e);
      }
      if (fPeripheralsMap.put(peripheral.getName(), peripheral) != null) {
         throw new Exception("Peripheral already exists " + baseName + instance);
      }
      return peripheral;
   }

   /**
    * Create peripheral with given name and instance
    * 
    * @param baseName      Base name of peripheral e.g. FTM3 => FTM
    * @param instance      Instance e.g. FTM3 => 3
    * 
    * @return Peripheral created
    */
   public Peripheral createPeripheral(String baseName, String instance) {
      Peripheral peripheral = fPeripheralsMap.get(baseName);
      if (peripheral != null) {
         throw new RuntimeException("Peripheral already exists");
      }
      return findOrCreatePeripheral(baseName+instance);
   }

   /**
    * Find or Create a peripheral<br>
    * e.g. findOrCreatePeripheral("FTM0") => <i>Peripheral</i>(FTM, 0)<br>
    * Checks against all templates.
    * 
    * @return Peripheral if found or name matches an expected pattern
    * 
    * @throws Exception if name does fit expected form
    */
   public Peripheral findOrCreatePeripheral(String name) {
      if (!name.matches("^[a-zA-Z]\\w*$")) {
         throw new RuntimeException("Illegal peripheral name = " + name);
      }
      Peripheral peripheral = fPeripheralsMap.get(name);
      if (peripheral != null) {
         return peripheral;
      }
      for(SignalTemplate template:getSignalTemplateList()) {
         peripheral = template.createPeripheral(name, Mode.ignore);
         if (peripheral != null) {
            break;
         }
      }
      if (peripheral != null) {
         return peripheral;
      }
      throw new RuntimeException("Failed to find pattern that matched peripheral: \'" + name + "\'");
   }

   /**
    * Find an existing peripheral
    * 
    * @param name   Name e.g. FTM3
    * @param mode   How to handle failure
    * 
    * @return
    */
   public Peripheral findPeripheral(String name, Mode mode) {
      Peripheral p = fPeripheralsMap.get(name);
      if ((p == null) && (mode != Mode.ignore)) {
         throw new RuntimeException("No such instance '" + name + "'");
      }
      return p;
   }

   /**
    * Find an existing peripheral
    * 
    * @param baseName   Base name e.g. FTM3 => FTM
    * @param instance   Name e.g. FTM3 => 3
    * @param mode       How to handle failure
    * 
    * @return
    */
   public Peripheral findPeripheral(String baseName, String instance, Mode mode) {
      return findPeripheral(baseName+instance, mode);
   }

   /**
    * Find an existing peripheral
    * 
    * @param baseName   Base name e.g. FTM3 => FTM
    * @param instance   Name e.g. FTM3 => 3
    * 
    * @return
    * @exception If peripheral doesn't exist
    */
   public Peripheral findPeripheral(String baseName, String instance) {
      return findPeripheral(baseName, instance, Mode.fail);
   }

   /**
    * Get map of all peripherals<br>
    * name -> peripheral
    * 
    * @return
    */
   public Map<String, Peripheral> getPeripherals() {
      return fPeripheralsMap;
   }

   /**
    * Get list of all peripheral names
    * 
    * @return Sorted list
    */
   public  ArrayList<String> getPeripheralNames() {
      ArrayList<String> ar = new ArrayList<String>(fPeripheralsMap.keySet());
      Collections.sort(ar);
      return ar;
   }

   /*
    * Signal =============================================================================================
    */
   /**
    * Map of all signals created<br>
    * May be searched by key derived from signal name
    */
   private Map<String, Signal> fSignals = new TreeMap<String, Signal>(Signal.comparator);

   /**
    * Map of signals associated with a baseName<br>
    * May be searched by baseName string
    */
   private Map<String, Map<String, Signal>> fSignalsByBaseName =
         new TreeMap<String, Map<String, Signal>>();

   /**
    * Get map of all signals
    * 
    * @return map
    */
   public Map<String, Signal> getSignals() {
      return fSignals;
   }

   /**
    * Get map of signals associated with the given baseName<br>
    * e.g. "FTM" with return all the FTM signals
    * 
    * @param baseName Base name to search for e.g. FTM, ADC etc
    * 
    * @return  Map or null if none exists for baseName
    */
   public Map<String, Signal> getSignalsByBaseName(String baseName) {
      return fSignalsByBaseName.get(baseName);
   }

   /**
    * Create signal
    * e.g. createSignal(FTM0_CH6,FTM,0,6) = <i>Signal</i>(FTM, 0, 6)
    * 
    * @param name          e.g. FTM0_CH6
    * @param baseName      e.g. FTM0_CH6 = FTM
    * @param instance      e.g. FTM0_CH6 = 0
    * @param signal        e.g. FTM0_CH6 = 6
    * 
    * @return Signal if found or created, null otherwise
    * @throws Exception
    */
   public Signal createSignal(String name, String baseName, String instance, String signalName) {

      Signal signal = fSignals.get(name);
      if (signal != null) {
         throw new RuntimeException("Signal already exists "+ name);
      }

      Peripheral peripheral = findPeripheral(baseName, instance);
      signal = new Signal(name, peripheral, signalName);

      // Add to base name map
      Map<String, Signal> map = fSignalsByBaseName.get(baseName);
      if (map == null) {
         map = new TreeMap<String, Signal>();
         fSignalsByBaseName.put(baseName, map);
      }
      map.put(baseName, signal);

      // Add to map
      fSignals.put(name, signal);
      peripheral.addSignal(signal);

      return signal;
   }

   /**
    * Find or Create signal<br>
    * e.g. findOrCreateSignal("FTM0_CH6") => <i>Signal</i>(FTM, 0, 6)<br>
    * Checks against all templates.
    * 
    * @return Signal if found or matches an expected pattern
    * 
    * @throws Exception if signal does fit expected form
    */
   public Signal findOrCreateSignal(String name) {
      Signal signal = null;
      if (name.equalsIgnoreCase("Disabled")) {
         return Signal.DISABLED_SIGNAL;
      }
      signal = fSignals.get(name);
      if (signal != null) {
         return signal;
      }
      // Try each template
      for(SignalTemplate signalTemplate:getSignalTemplateList()) {
         signal = signalTemplate.createSignal(name);
         if (signal != null) {
            return signal;
         }
      }
      throw new RuntimeException("Failed to find pattern that matched signal: \'" + name + "\'");
   }

   /**
    * Find signal<br>
    * e.g. findSignal("FTM0_CH6") => <i>Signal</i>(FTM, 0, 6)<br>
    * 
    * @return Signal found or null if not found
    */
   public Signal safeFindSignal(String name) {
      if (name.equalsIgnoreCase("Disabled")) {
         return Signal.DISABLED_SIGNAL;
      }
      return fSignals.get(name);
   }

   /**
    * Find signal<br>
    * e.g. findSignal("FTM0_CH6") => <i>Signal</i>(FTM, 0, 6)<br>
    * 
    * @return Signal found
    * 
    * @throws Exception signal nor found
    */
   public Signal findSignal(String name) {
      Signal signal = safeFindSignal(name);
      if (signal != null) {
         return signal;
      }
      throw new RuntimeException("Failed to find signal: \'" + name + "\'");
   }

   /**
    * A string listing all signals
    * 
    * @return
    */
   public String listSignals() {
      StringBuffer buff = new StringBuffer();
      buff.append("(");
      for (String f:fSignals.keySet()) {
         buff.append(f+",");
      }
      buff.append(")");
      return buff.toString();
   }

   /*
    * DevicePackage =============================================================================================
    */
   /** Map of package names to packages */
   Map<String, DevicePackage> fDevicePackages = new TreeMap<String, DevicePackage>();

   /**
    * Find device package information <br>
    * Will create new empty package information if necessary
    * 
    * @param packageName
    * 
    * @return Package information
    */
   public DevicePackage findOrCreateDevicePackage(String packageName) {
      DevicePackage p = findDevicePackage(packageName);
      if (p == null) {
         p = new DevicePackage(packageName);
         fDevicePackages.put(packageName, p);
      }
      return p;
   }

   /**
    * Gets device package information from package name
    * 
    * @param name
    * 
    * @return Package information or null if not found
    */
   public DevicePackage findDevicePackage(String packageName) {
      return fDevicePackages.get(packageName);
   }

   /**
    * Get map of device packages
    * 
    * @return
    */
   public Map<String, DevicePackage> getDevicePackages() {
      return fDevicePackages;
   }

   /*
    * MappingInfo =============================================================================================
    */

   /**
    * Map from Signal to list of Pins
    */
   private Map<Signal, ArrayList<MappingInfo>> fSignalMap = new TreeMap<Signal, ArrayList<MappingInfo>>();

   /**
    * Add info to map by signal
    * 
    * @param info
    */
   void addToSignalMap(Signal signal, MappingInfo info) {
      ArrayList<MappingInfo> list = fSignalMap.get(signal);
      if (list == null) {
         list = new ArrayList<MappingInfo>();
         fSignalMap.put(signal, list);
      }
      list.add(info);
   }

   /**
    * Get list of pin mappings associated with given signal
    * 
    * @param signal
    * 
    * @return
    */
   public ArrayList<MappingInfo> getPins(Signal signal) {
      return fSignalMap.get(signal);
   }

   /**
    * Create new Pin mapping<br>
    * 
    * Mapping is added to pin map<br>
    * Mapping is added to signal map<br>
    * 
    * @param signal          Signal signal being mapped e.g. I2C2_SCL
    * @param pinInformation  Pin being mapped e.g. PTA (pin name not signal!)
    * @param muxValue        Multiplexor setting that maps this signal to the pin
    * @return
    */
   public MappingInfo createMapping(Signal signal, Pin pinInformation, MuxSelection muxValue) {

      MappingInfo mapInfo= pinInformation.addSignal(signal, muxValue);
      addToSignalMap(signal, mapInfo);
      return mapInfo;
   }

   /*
    * PinInformation =============================================================================================
    */
   /**
    * Map of all pins created<br>
    * May be searched by pin name
    */
   private Map<String, Pin> fPins = new TreeMap<String, Pin>(Pin.comparator);

   /**
    * Get Map of all pins
    * 
    * @return
    */
   public Map<String, Pin> getPins() {
      return fPins;
   }

   HashMap<String, Integer> sharedPinMap = new HashMap<String, Integer>();

   /**
    * Create pin from name<br>
    * 
    * @param name Name of pin
    * 
    * @return Created pin
    * 
    * @throws Exception if the pin already exists
    */
   public Pin createPin(String name) throws Exception {

      // Vdd and Vss have suffix added as may be multiple
      if (name.toUpperCase().startsWith("VDD") ||
          name.toUpperCase().startsWith("VSS")) {
         Integer item = sharedPinMap.get(name);
         if (item == null) {
            item = 0;
            sharedPinMap.put(name, item);
         }
//         System.err.print("Mapping '" + name);
         sharedPinMap.put(name, item+1);
         name = name+((item==0)?"":""+item);
//         System.err.println("' => '" + name + "'");
      }
      // Check for repeated pin
      Pin pinInformation = fPins.get(name);
      if (pinInformation != null) {
         throw new Exception("Pin already exists: " + name);
      }
      // Created pin
      pinInformation = new Pin(this, name);
      
      // Add to map
      fPins.put(name, pinInformation);

      return pinInformation;
   }

   /**
    * Finds pin from name<br>
    * 
    * @param name Name of pin
    * 
    * @return Pin found or null if not present
    */
   public Pin findPin(String name) {
      if (name.equalsIgnoreCase(Pin.UNASSIGNED_PIN.getName())) {
         return Pin.UNASSIGNED_PIN;
      }
      return fPins.get(name);
   }
   /*
    * PeripheralTemplateInformation =============================================================================================
    */
   /**
    * List of all templates
    */
   private ArrayList<SignalTemplate> fSignalTemplateList = new ArrayList<SignalTemplate>();

   /**
    * Get list of all templates
    * 
    * @return
    */
   public ArrayList<SignalTemplate> getSignalTemplateList() {
      return fSignalTemplateList;
   }

   /**
    * Gets template that matches this signal
    * 
    * @param signal   Signal to match
    * 
    * @return Matching template or null on none
    */
   public SignalTemplate getTemplate(Signal signal) {
      for (SignalTemplate signalTemplate:fSignalTemplateList) {
         if (signalTemplate.getMatchPattern().matcher(signal.getName()).matches()) {
            return signalTemplate;
         }
      }
      return null;
   }

   /**
    * 
    * @param namePattern            Pattern to extract peripheral base name e.g. FTM2 => FTM
    * @param instancePattern        Pattern to extract instance e.g. FTM2 => "2"
    * @param signalPattern          Pattern to extract signal e.g. FTM2_CH3 => "CH3"
    * @param matchTemplate          Pattern to select use of this template e.g. "FTM\\d+_CH\\d+"
    * @param deviceFamily           Device family
    * @param instanceWriter         InstanceWriter to use
    */
   private SignalTemplate createPeripheralTemplateInformation(
         String        namePattern,
         String        instancePattern,
         String        signalPattern,
         String        matchTemplate,
         DeviceFamily  deviceFamily,
         Class<?>      instanceWriterClass) {

      SignalTemplate template = null;

      try {
         template = new SignalTemplate(this, deviceFamily, namePattern, signalPattern, instancePattern, "^"+matchTemplate+"$", instanceWriterClass);
         fSignalTemplateList.add(template);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
      return template;
   }

   /**
    * Set up templates
    */
   public void initialiseTemplates() throws Exception {
      /*
       *  TODO Where peripheral types are found
       */
      createPeripheralTemplateInformation(
            "GPIO", "$2", "$4",
            "^(GPIO|PORT)([A-I])(_(\\d+))?$",
            getDeviceFamily(),
            WriterForGpio.class);
      
      createPeripheralTemplateInformation(
            "PORT", "", "",
            "^(PORT)$",
            getDeviceFamily(),
            WriterForPort.class);

//      createPeripheralTemplateInformation(
//            "PORT", "$2", "$4",
//            "^(PORT)([A-I])(_(\\d+))?$",
//            getDeviceFamily(),
//            WriterForPort.class);
//
      createPeripheralTemplateInformation(
            "IRQ", "", "INT",
            "IRQ",
            getDeviceFamily(),
            WriterForInterrupt.class);
      
      createPeripheralTemplateInformation(
            "PWT", "", "$1",
            "PWT_(IN\\d+)",
            getDeviceFamily(),
            WriterForPwt.class);
      
      createPeripheralTemplateInformation(
            "POWER", "", "$0",
            "(DCDC.*|USB1_VSS|PSWITCH|VDCDC_IN)|(VOUT33|VBAT|VREFL|VREFH|VSS(A|B|_.*)?|VDD(IO_E|A|B|_.*)?|VREG(IN|_IN|_IN0|_IN1|_OUT|_OUT0))(\\d*(a|b|c)?)",
            getDeviceFamily(),
            WriterForPower.class);
      
      createPeripheralTemplateInformation(
            "OSC", "0", "$2",
            "^(RF_?(XTAL|EXTAL|XTAL_OUT|XTAL_OUT_EN))$",
            getDeviceFamily(),
            WriterForOscRf.class);
      
      createPeripheralTemplateInformation(
            "SCG", "", "$2",
            "^(SCG)_(XTAL|EXTAL)$",
            getDeviceFamily(),
            WriterForScg.class);
      
      createPeripheralTemplateInformation(
            "RADIO", "", "$0",
            "^(RF_NOT_ALLOWED|RF_RESET|ANT|BLE_RF_ACTIVE|BTLL|GANT|DTM_.*|ANT_(a|b)|DIAG(\\d+)|TX_SWITCH|RX_SWITCH|BSM_.*|GEN_FSK|PHYDIG|RSIM|ZIGBEE)$",
            getDeviceFamily(),
            WriterForRadio.class);
      
      createPeripheralTemplateInformation(
            "CONTROL", "", "$0",
            "(JTAG|EZP|SWD|CLKOUT|NMI_b|RESET_b|(noetm_)?TRACE)_?(.*)",
            getDeviceFamily(),
            WriterForControl.class);

      if (getDeviceFamily() != DeviceFamily.mkm) {
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(ADC)([0-3])_((SE|DM|DP)\\d+(a|b)?)",
               getDeviceFamily(),
               WriterForAdc.class);
         createPeripheralTemplateInformation(
               "ADC", "$2", "$3",
               "(PGA)([0-3])(_(DM|DP))?",
               getDeviceFamily(),
               WriterForAdc.class);
         createPeripheralTemplateInformation(
               "CAN", "$2", "$3",
               "(CAN|FLEXCAN)([0-5])_(RX|TX)",
               getDeviceFamily(),
               WriterForFlexCan.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(A?CMP)([0-3])?_(.*)",
               getDeviceFamily(),
               WriterForCmp.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(CMT0?)_()(IRO)",
               getDeviceFamily(),
               WriterForCmt.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(DAC)(\\d+)?_(OUT)",
               getDeviceFamily(),
               WriterForDac.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(DMAMUX)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForDmaMux.class);
         createPeripheralTemplateInformation(
               "ENET", "", "$3",
               "(ENET)(0)?_(.*)",
               getDeviceFamily(),
               WriterForEnet.class);
         createPeripheralTemplateInformation(
               "ENET", "", "$1$3",
               "(R?MII)(0)?(_.*)",
               getDeviceFamily(),
               WriterForEnet.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(EWM)_()(IN|OUT(_b)?)",
               getDeviceFamily(),
               WriterForEwm.class);
         createPeripheralTemplateInformation(
               "FLEXBUS", "", "$2",
               "(FB|FLEXBUS)_?(.*)",
               getDeviceFamily(),
               WriterForFlexBus.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(FTM)([0-3])_(CH\\d+|QD_PH[A|B]|FLT\\d|CLKIN\\d|TRIG\\d|FAULT\\d)",
               getDeviceFamily(),
               WriterForFtm.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(FTM)_()(CLKIN\\d+)",
               getDeviceFamily(),
               WriterForFtmShared.class);
         createPeripheralTemplateInformation(
               "FTM", "", "$2",
               "(TCLK)_?(\\d+)",
               getDeviceFamily(),
               WriterForFtmShared.class);
         createPeripheralTemplateInformation(
               "$1", "$3", "$4",
               "((LP)?I2C)([0-3])_(SCL(S?)|SDA(S?)|4WSCLOUT|4WSDAOUT|HREQ)",
               getDeviceFamily(),
               WriterForI2c.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(I2S)([0-3])_(MCLK|(RXD(\\d+))|RX_BCLK|RX_FS|(TXD(\\d+))|TX_BCLK|TX_FS|xxx|(RW(_b)?)|(TS(_b)?)|(AD\\d+))",
               getDeviceFamily(),
               WriterForI2s.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(LLWU)()_(P\\d+)",
               getDeviceFamily(),
               WriterForLlwu.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(LPTMR)([0-3])_(ALT\\d+)",
               getDeviceFamily(),
               WriterForLptmr.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(LPUART)([0-6])_(TX|RX|CTS(_b)?|RTS(_b)?)",
               getDeviceFamily(),
               WriterForLpuart.class);
         createPeripheralTemplateInformation(
               "$1", "", "",
               "(MCG)",
               getDeviceFamily(),
               WriterForMcg.class);
         createPeripheralTemplateInformation(
               "$1", "", "",
               "(ICS)",
               getDeviceFamily(),
               WriterForIcs.class);
         createPeripheralTemplateInformation(
               "OSC", "0", "$1",
               "(E?XTAL)(0)?",
               getDeviceFamily(),
               WriterForOsc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "((?:L)?PIT)(\\d)?_?(.*)",
               getDeviceFamily(),
               WriterForPit.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(PDB)(\\d?)_?(EXTRG)?",
               getDeviceFamily(),
               WriterForPdb.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "",
               "(PMC)(0?)",
               getDeviceFamily(),
               WriterForPmc.class);
         createPeripheralTemplateInformation(
               "$1", "", "",
               "(PCC)",
               getDeviceFamily(),
               WriterForPcc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "",
               "(SAI)(\\d?)_?(.*)?",
               getDeviceFamily(),
               WriterForPmc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "",
               "(WDOG)(0?)",
               getDeviceFamily(),
               WriterForWdog.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "",
               "(RCM)(0?)",
               getDeviceFamily(),
               WriterForRcm.class);
         createPeripheralTemplateInformation(
               "RTC", "", "$2",
               "(RTC)_?(CLKOUT|CLKIN|WAKEUP_b)",
               getDeviceFamily(),
               WriterForRtc.class);
         createPeripheralTemplateInformation(
               "RTC", "", "$1",
               "(E?XTAL32K?)",
               getDeviceFamily(),
               WriterForRtc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(SDHC)(\\d+)?_(.*)",
               getDeviceFamily(),
               WriterForSdhc.class);
         createPeripheralTemplateInformation(
               "SDRAMC", "$2", "$3",
               "(SDRAMC?)(\\d+)?_(.*)",
               getDeviceFamily(),
               WriterForSdramc.class);
         createPeripheralTemplateInformation(
               "$1", "$4", "$5",
               "((LP)?(SPI))([0-3])_(SCK|SIN|SOUT|MISO|MOSI|SS|SS_b|PCS\\d*)",
               getDeviceFamily(),
               WriterForSpi.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(TPM)([0-3])_(CH\\d+|QD_PH[A|B]|FLT\\d|CLKIN\\d|TRIG\\d|FAULT\\d)",
               getDeviceFamily(),
               WriterForFtm.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(TPM)_()(CLKIN\\d+)",
               getDeviceFamily(),
               WriterForFtmShared.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(TSI)([0-3])_(CH\\d+)",
               getDeviceFamily(),
               WriterForTsi.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(UART)(\\d+)_(TX|RX|CTS_b|RTS_b|COL_b)",
               getDeviceFamily(),
               WriterForUart.class);
         createPeripheralTemplateInformation(
               "USB", "0", "$3",
               "(USB(OTG)?)_(.*)",
               getDeviceFamily(),
               WriterForUsb.class);
         createPeripheralTemplateInformation(
               "USB", "$3", "$4",
               "(USB(OTG)?)(\\d+)?_(.*)",
               getDeviceFamily(),
               WriterForUsb.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(USBHS)(\\d+)_(.*)",
               getDeviceFamily(),
               WriterForUsbhs.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "",
               "(USBPHY)(\\d+)(.*)",
               getDeviceFamily(),
               WriterForUsbPhy.class);
         createPeripheralTemplateInformation(
               "USBPHY", "$2", "",
               "(USB_(ID))",
               getDeviceFamily(),
               WriterForUsbPhy.class);
         createPeripheralTemplateInformation(
               "USBDCD", "$2", "$3",
               "(USBDCD)(\\d+)(.*)",
               getDeviceFamily(),
               WriterForUsbdcd.class);
         createPeripheralTemplateInformation(
               "$1", "", "$3",
               "(VREF)(\\d*)_(OUT)",
               getDeviceFamily(),
               WriterForVref.class);
         createPeripheralTemplateInformation(
               "$1", "", "",
               "(VREF)\\d*",
               getDeviceFamily(),
               WriterForVref.class);

         createPeripheralTemplateInformation(
               "$1", "", "",
               "(SIM)",
               getDeviceFamily(),
               WriterForSim.class);
         
         createPeripheralTemplateInformation(
               "$1", "", "",
               "(SMC)",
               getDeviceFamily(),
               WriterForSmc.class);
         
         createPeripheralTemplateInformation(
               "Console", "", "",
               "CONSOLE",
               getDeviceFamily(),
               WriterForConsole.class);
         
         createPeripheralTemplateInformation(
               "ExternalTrigger", "", "$0",
               "(EXTRG_IN)",
               getDeviceFamily(),
               WriterForShared.class);
         
         createPeripheralTemplateInformation(
               "FLEXIO", "", "$3",
               "(FXIO|FLEXIO)(\\d)?_(.*)",
               getDeviceFamily(),
               WriterForFlexio.class);
         createPeripheralTemplateInformation(
               "$1", "", "$3",
               "S?(LCD)(\\d)?_?(.*)",
               getDeviceFamily(),
               WriterForLcd.class);
         createPeripheralTemplateInformation(
               "LCD_POWER", "", "$0",
               "(VCAP|VLL)\\d+",
               getDeviceFamily(),
               WriterForPower.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(CRC)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForCrc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(DMA)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForDma.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(MPU)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForToDo.class);
         createPeripheralTemplateInformation(
               "$1", "$3", "$4",
               "(FTF(A|C|E|L))(\\d)?(.*)",
               getDeviceFamily(),
               WriterForFlash.class);
         createPeripheralTemplateInformation(
               "$1", "$4", "$5",
               "((T)?RNG(A)?)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForRnga.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(QSPI)(0|1)((A|B).*)",
               getDeviceFamily(),
               WriterForQspi.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(TRGMUX)(\\d)?_(.*)",
               getDeviceFamily(),
               WriterForTrgmux.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(KBI)(\\d)?_(.*)",
               getDeviceFamily(),
               WriterForKbi.class);
         createPeripheralTemplateInformation(
               "$1", "", "",
               "(FMC)",
               getDeviceFamily(),
               WriterForFmc.class);
         createPeripheralTemplateInformation(
               "$1", "", "",
               "(MCM)",
               getDeviceFamily(),
               WriterForMcm.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(EMVSIM)(\\d)?_(.*)",
               getDeviceFamily(),
               WriterForToDo.class);
         createPeripheralTemplateInformation(
               "$1", "$3", "$4",
               "(LTC)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForToDo.class);
      }
      createPeripheralTemplateInformation(
            "$1", "$2", "$3",
            "(.*)()()",
            getDeviceFamily(),
            WriterForNull.class);
   }

   /*
    * DeviceVariantInformation =============================================================================================
    */

   /** Name of device e.g. MK20DN32VLH5, FRDM_K20D50M */
   private String fPreciseName = null;

   /**
    * 
    *
    */
   public enum InitPhase {
      // No variable propagation
      VariablePropagationSuspended,
      // Variable propagation only
      VariablePropagationAllowed,
      // Propagation from variable and GUI changes allowed
      VariableAndGuiPropagationAllowed,;

      /**
       * Indicates if this represents an earlier initialisation phase than argument
       * 
       * @param phase Initialisation phase to compare to
       * 
       * @return  True if earlier
       */
      public boolean isEarlierThan(InitPhase phase) {
         return this.ordinal() < phase.ordinal();
      }
      /**
       * Indicates if this represents an later initialisation phase than argument
       * 
       * @param phase Initialisation phase to compare to
       * 
       * @return  True if later
       */
      public boolean isLaterThan(InitPhase phase) {
         return this.ordinal() > phase.ordinal();
      }
   };
   
   /** Indicates variable update propagation is suspended */
   private InitPhase fInitPhase = InitPhase.VariablePropagationSuspended;
   
   /**
    * Set device variant name (and variant information)
    * 
    * @param preciseName Name of device variant e.g. MK20DN32VLH5, FRDM_K20D50M, MKE04Z8VTG4
    * @throws Exception
    */
   public void setVariantName(String preciseName) throws Exception {
      if ((fPreciseName != null) && (fPreciseName.compareTo(preciseName) == 0)) {
         return;
      }
      fPreciseName = preciseName;
      if (fVariantInformationTable != null) {
         fVariantInformation = fVariantInformationTable.get(fPreciseName);
         if (fVariantInformation == null) {
            throw new UsbdmException("Illegal device variant name "+ preciseName);
         }
      }
      String deviceName = fVariantInformation.getDeviceName();
      if ((deviceName == null) || deviceName.isBlank()) {
         // Use legacy method to obtain device name from variant name
         deviceName = getDeviceName(fPreciseName);
      }
      DeviceLinkerInformation.addLinkerMemoryMap(deviceName, fVariables);
      setDirty(true);
   }
   
   /**
    * Get precise name of device variant
    * 
    * @return Name of device variant e.g. MK20DN32VLH5, FRDM_K20D50M, MKE04Z8VTG4
    */
   public String getPreciseName() {
      return fPreciseName;
   }
   
   /**
    * Get information for current variant
    * 
    * @return Information about variant e.g. MK20DN32VLH5, FRDM_K20D50M
    */
   public DeviceVariantInformation getVariant() {
      return fVariantInformation;
   }

   /**
    * Create device variant information
    * 
    * @param preciseName      Precise name of device variant e.g. MK20DN32VLH5, FRDM_K20D50M, MK28FN2M0CAU15R - Key
    * @param manual           Manual reference e.g. K20P64M50SF0RM
    * @param packageName      Package e.g. LQFP_64, QFN_32  - Identifies package pin information internally
    * @param deviceName       Device name e.g. MK28FN2M0M15 - Used to obtain device information (linker etc)

    * @return class containing given information
    * @throws UsbdmException
    */
   public DeviceVariantInformation createDeviceInformation(String preciseName, String manual, String packageName, String deviceName) throws UsbdmException {
      DeviceVariantInformation deviceInformation =
            new DeviceVariantInformation(preciseName, manual, findOrCreateDevicePackage(packageName), deviceName);
      fVariantInformationTable.put(preciseName, deviceInformation);
      return deviceInformation;
   };

   /**
    * Get map from variant names to device variants
    * 
    * @return Map
    */
   public Map<String, DeviceVariantInformation> getDeviceVariants() {
      return fVariantInformationTable;
   }

   /**
    * Find device variant from variant name
    * 
    * @param variant name
    * 
    * @return variant
    */
   public DeviceVariantInformation findVariant(String deviceName) {
      return fVariantInformationTable.get(deviceName);
   }

   /*
    * DmaInfo =============================================================================================
    */

   /**
    * Create DMA information entry
    * 
    * @param dmaMuxNum
    * @param dmaChannelNumber
    * @param dmaSource
    * @return
    */
   public DmaInfo createDmaInfo(String dmaMuxNum, int dmaChannelNumber, String dmaSource) {
      Peripheral dmaPeripheral = findOrCreatePeripheral("DMAMUX"+dmaMuxNum);
      DmaInfo dmaInfo = dmaPeripheral.addDmaChannel(dmaChannelNumber, dmaSource);
      //      fDmaInfoList.add(dmaInfo);
      return dmaInfo;
   }

   private final static HashMap<String, Integer> exceptions = new  HashMap<String, Integer>();

   /**
    * Used to check if the pin mapping appears sensible
    * 
    * @param deviceFamily  Device family
    * @param pin           Pin to check
    * 
    * @return
    */
   private static boolean checkOkExceptionalMapping(DeviceFamily deviceFamily, Pin pin) {
      
      if (deviceFamily == DeviceFamily.mke) {
         return true;
      }
      if (pin.getResetValue() == MuxSelection.mux0) {
         return true;
      }
      if (pin.getResetValue() == MuxSelection.fixed) {
         return true;
      }
      if (pin.getResetValue() == MuxSelection.unassigned) {
         return true;
      }
      if (exceptions.size() == 0) {
         exceptions.put(DeviceFamily.mk.name()+".PTA0",    1<<MuxSelection.mux7.value);  // JTAG_TCLK/SWD_CLK
         exceptions.put(DeviceFamily.mk.name()+".PTA1",    1<<MuxSelection.mux7.value);  // JTAG_TDI
         exceptions.put(DeviceFamily.mk.name()+".PTA2",    1<<MuxSelection.mux7.value);  // JTAG_TDO/TRACE_SWO
         exceptions.put(DeviceFamily.mk.name()+".PTA3",    1<<MuxSelection.mux7.value);  // JTAG_TMS/SWD_DIO
         exceptions.put(DeviceFamily.mk.name()+".PTA4",    1<<MuxSelection.mux7.value);  // NMI_b
         exceptions.put(DeviceFamily.mk.name()+".PTA5",    1<<MuxSelection.mux7.value);  // JTAG_TRST_b
         // MKL
         exceptions.put(DeviceFamily.mkl.name()+".PTA0",   1<<MuxSelection.mux3.value);  // SWD_CLK
         exceptions.put(DeviceFamily.mkl.name()+".PTA1",   1<<MuxSelection.mux3.value);  // RESET_b
         exceptions.put(DeviceFamily.mkl.name()+".PTA2",   1<<MuxSelection.mux3.value);  // SWD_DIO
         exceptions.put(DeviceFamily.mkl.name()+".PTA3",   1<<MuxSelection.mux7.value);  // SWD_DIO
         exceptions.put(DeviceFamily.mkl.name()+".PTA4",   1<<MuxSelection.mux7.value);  // NMI_b
         exceptions.put(DeviceFamily.mkl.name()+".PTB5",   1<<MuxSelection.mux3.value);  // NMI_b
         exceptions.put(DeviceFamily.mkl.name()+".PTA20",  1<<MuxSelection.mux7.value);  // RESET_b
         // MKM
         exceptions.put(DeviceFamily.mkm.name()+".PTE6",   1<<MuxSelection.mux7.value);  // SWD_DIO
         exceptions.put(DeviceFamily.mkm.name()+".PTE7",   1<<MuxSelection.mux7.value);  // SWD_CLK
         exceptions.put(DeviceFamily.mkm.name()+".PTE1",   1<<MuxSelection.mux7.value);  // RESET_b
         // MKV
         exceptions.put(DeviceFamily.mkv.name()+".PTA0",   1<<MuxSelection.mux7.value);  // JTAG_TCLK/SWD_CLK
         exceptions.put(DeviceFamily.mkv.name()+".PTA1",   1<<MuxSelection.mux7.value);  // JTAG_TDI
         exceptions.put(DeviceFamily.mkv.name()+".PTA2",   1<<MuxSelection.mux7.value);  // JTAG_TDO/TRACE_SWO
         exceptions.put(DeviceFamily.mkv.name()+".PTA3",   1<<MuxSelection.mux7.value);  // JTAG_TMS/SWD_DIO
         exceptions.put(DeviceFamily.mkv.name()+".PTA4",   1<<MuxSelection.mux7.value);  // NMI_b
         // MKW
         exceptions.put(DeviceFamily.mkw.name()+".PTB18",  1<<MuxSelection.mux7.value);  // NMI_b
         exceptions.put(DeviceFamily.mkw.name()+".PTA0",   1<<MuxSelection.mux7.value);  // SWD_DIO
         exceptions.put(DeviceFamily.mkw.name()+".PTA1",   1<<MuxSelection.mux7.value);  // SWD_CLK
         exceptions.put(DeviceFamily.mkw.name()+".PTA2",   1<<MuxSelection.mux7.value);  // RESET_b
         // S32
         exceptions.put(DeviceFamily.s32k.name()+".PTA4",  1<<MuxSelection.mux7.value);  // JTAG_TMS/SWD_DIO
         exceptions.put(DeviceFamily.s32k.name()+".PTA5",  1<<MuxSelection.mux7.value);  // RESET_b
         exceptions.put(DeviceFamily.s32k.name()+".PTA10", 1<<MuxSelection.mux7.value);  // JTAG_TDO/noetm_TRACE_SWO
         exceptions.put(DeviceFamily.s32k.name()+".PTC4",  1<<MuxSelection.mux7.value);  // JTAG_TCLK/SWD_CLK
         exceptions.put(DeviceFamily.s32k.name()+".PTC5",  1<<MuxSelection.mux7.value);  // JTAG_TDI
      }
      String key = deviceFamily.name()+"."+pin.getName();
      Integer exception = exceptions.get(key);
      boolean ok = (exception != null) && ((pin.getResetValue().value & exception) == 0);
      if (!ok) {
         System.err.println("exception = " + exception);
         System.err.println("key       = " + key);
      }
      return ok;
   }
   
   /**
    * Does some basic consistency checks on the data
    */
   public void consistencyCheck() {
      // Every pin should have a reset entry
      for (String pName:getPins().keySet()) {
         Pin pin = getPins().get(pName);
         if (!checkOkExceptionalMapping(this.fDeviceFamily, pin)) {
            // Unusual mapping - report
            System.err.println("Note: Pin "+pin.getName()+" reset mapping is non-zero = "+pin.getResetValue());
         }
      }
      // Every signal should have a reset entry implied by the pin information
      // except for signals with fixed pin mapping
      for (String pName:getSignals().keySet()) {
         Signal signal = getSignals().get(pName);
         if ((signal.getResetMapping() == null) &&
               (signal.getPinMapping().first().getMux() != MuxSelection.fixed)) {
            throw new RuntimeException("No reset value for signal " + signal);
         }
      }
   }

   /*
    * ==============================================================
    */
   /**
    * Get string describing the pin with alias in braces<br>
    * 
    * @return Pin name e.g. PTA1 (p23)
    */
   public String getPinNameWithAlias(Pin pin) {
      String alias = "";
      DeviceVariantInformation deviceInformation = getVariant();
      if (deviceInformation != null) {
         DevicePackage pkg = fDevicePackages.get(deviceInformation.getPackage().getName());
         if (pkg != null) {
            alias = pkg.getLocation(pin);
         }
      }
      if (!alias.isEmpty()) {
         alias = " ("+alias+")";
      }
      return pin.getName() + alias;
   }

   /**
    * Looks in for a file in 'HARDWARE' location
    * 
    * @param name
    * 
    * @return
    */
   Path locateFile(String name) {
      // Try local (debug) directory first
      Path path = Paths.get(DeviceInfo.USBDM_HARDWARE_LOCATION+name);
      if (!Files.isRegularFile(path)) {
         // Look in USBDM installation
         try {
            path = Paths.get(Usbdm.getUsbdmResourcePath()).resolve(path);
         } catch (UsbdmException e) {
            Activator.logError(e.getMessage(), e);
         }
      }
      if (!Files.isRegularFile(path)) {
         System.out.println("Warning: failed to find file "+ name);
         return null;
      }
      return path;
   }
   
   /**
    * Load persistent settings
    * @throws IOException
    */
   public Settings getSettings(Path path) throws Exception {
      Activator.log("Loading settings from" + path.toAbsolutePath() + ")");
      fProjectSettingsPath = path;
      if (path.toFile().isFile()) {
         Settings settings = new Settings("USBDM");
         settings.load(path.toAbsolutePath());
         return settings;
      }
      return null;
   }
   /**
    * Load persistent settings
    * 
    * @param device     Associated device (only used if settings are incomplete)
    * @param settings   Settings to load from
    */
   public void loadSettings(Device device, Settings settings) {
      try {
         String variantName = settings.get(USBDMPROJECT_VARIANT_SETTING_KEY);
         if ((variantName == null) && (device != null)) {
            for (String variant:getDeviceVariants().keySet()) {
               DeviceVariantInformation deviceVarInfo = getDeviceVariants().get(variant);
               String deviceName = deviceVarInfo.getDeviceName();
               if (deviceName.equalsIgnoreCase(device.getName())) {
                  // Found compatible variant
                  variantName = variant;
                  break;
               }
               deviceName = getDeviceName(variant);
               if (deviceName.equalsIgnoreCase(device.getName())) {
                  // Found compatible variant
                  variantName = variant;
                  break;
               }
            }
         }
         setVariantName(variantName);
         Path path = locateFile("/peripherals/symbols/"+variantName+".xml");
         if (path != null) {
            settings.load(path);
         }
         String subFamilyName = settings.get(USBDMPROJECT_SUBFAMILY_SETTING_KEY);
         if (subFamilyName == null) {
            subFamilyName = settings.get(USBDMPROJECT_OLD_SUBFAMILY_SETTING_KEY);
         }
         setDeviceSubFamily(subFamilyName);

         // Create dependencies between variables and peripherals
         for (Entry<String, Peripheral> entry:fPeripheralsMap.entrySet()) {
            Peripheral peripheral =  entry.getValue();
            ArrayList<Validator> validators = peripheral.getValidators();
            for (Validator validator:validators) {
               validator.addDependencies();
            }
         }
         for (String pinName:fPins.keySet()) {
            Pin pin = fPins.get(pinName);
            pin.loadSettings(settings);
         }
         for (String signalName:fSignals.keySet()) {
            Signal signal = fSignals.get(signalName);
            signal.loadSettings(settings);
         }
         for (String peripheralName:fPeripheralsMap.keySet()) {
            Peripheral peripheral =  fPeripheralsMap.get(peripheralName);
            peripheral.loadSettings(settings);
         }
         // Customise peripherals with extra device information
         for (Entry<String, Peripheral> entry:fPeripheralsMap.entrySet()) {
            Peripheral peripheral =  entry.getValue();
            if (peripheral instanceof Customiser) {
               Customiser c = (Customiser)peripheral;
               c.modifyPeripheral();
            }
         }
         for (String key:settings.getKeys()) {
            try {
               Variable var   = fVariables.safeGet(key);
               String   value = settings.get(key);
               if (key.startsWith("/MCG/clock_mode")) {
                  // Old clock names
                  key = key.replace("/MCG/clock_mode","/MCG/mcgClockMode");
               }
               if (var != null) {
                  if (!var.isDerived()) {
                     // Load persistent value associated with variable
                     var.setPersistentValue(value);
                  }
               }
               else if (key.startsWith("$")) {
                  // Ignore these as loaded earlier
                  //               System.err.println("WARNING: Discarding system setting "+key+" to "+value);
               }
               else if (key.startsWith("/")) {
                  // Shouldn't be any unmatched peripheral settings
                  System.err.println("WARNING: Discarding unmatched peripheral settings "+key+"("+value+")");
                  // Indicate state will change on save
                  setDirty(true);
               }
               else {
                  // Load persistent value (parameter)
//               System.err.println("Creating Variable "+key+"("+value+")");
                  var = new StringVariable(key, key);
                  var.setPersistentValue(value);
                  var.setDerived(true);
                  addVariable(var);
               }
            } catch (Exception e) {
               Activator.logError(e.getMessage(),e);
            }
         }
         //         System.err.println("Make sure peripherals have been updated");
         
         refreshConnections();
         
         /**
          * Sanity check - (usually) no persistent variables should change value initially
          */
         for (Entry<String, Variable> entry:fVariables.entrySet()) {
            String value = settings.get(entry.getKey());
            if (value != null) {
               Variable var = fVariables.get(entry.getKey());
               if (!var.isDerived()) {
                  if (!var.getPersistentValue().equals(value)) {
                     System.err.println("WARNING: deviceEditor.information.DeviceInfo.loadSettings - Variable changed " + var.getKey());
                     System.err.println("Loaded value     = " + value);
                     System.err.println("Final value = " + var.getPersistentValue());
                  }
               }
            }
         }
      } catch (Exception e) {
         Activator.logError(e.getMessage(), e);
         e.printStackTrace();
      }
      setDirty(false);
   }

   /**
    * Refreshes dependencies between things!
    */
   public void refreshConnections() {
      // Allow variable change notifications only (not GUI input only variables)
      fInitPhase = InitPhase.VariablePropagationAllowed;
      
      /*
       * Make sure critical peripherals have been updated in order first
       */
      String criticalPeripherals[] = {
            "RTC",
            "OSC",
            "OSC0",
            "OSC_RF0",
            "MCG",
            "ICS",
            "SIM",
      };
      for (String name:criticalPeripherals) {
         Peripheral peripheral =  fPeripheralsMap.get(name);
         if (peripheral instanceof PeripheralWithState) {
            PeripheralWithState p = (PeripheralWithState) peripheral;
            p.variableChanged(null);
         }
      }
      for (Entry<String, Peripheral> entry:fPeripheralsMap.entrySet()) {
         Peripheral peripheral =  entry.getValue();
         if (peripheral instanceof PeripheralWithState) {
            PeripheralWithState p = (PeripheralWithState) peripheral;
            p.variableChanged(null);
         }
      }
      
      /**
       * Add Variable internal listeners (expressions)
       */
      for (Entry<String, Variable> entry:fVariables.entrySet()) {
         Variable var = fVariables.get(entry.getKey());
//         if (var.getName().contains("osc_cr_range")) {
//            System.err.println("Found it "+var.getName());
//         }
         try {
            var.addInternalListeners();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      for (Entry<String, Variable> entry:fVariables.entrySet()) {
         Variable var = fVariables.get(entry.getKey());
//         if (var.getName().equals("kbi_pe_kbipe")) {
//            System.err.println("Found it "+ var.getKey());
//         }
         var.expressionChanged(null);
      }
      //       System.err.println("Notify changes of persistent variables");
      
      /*
       * Notify changes of persistent variables,
       * even on variables that were not loaded
       * Shouldn't be necessary
       */
      for (Entry<String, Variable> entry:fVariables.entrySet()) {
         Variable var = entry.getValue();
//         if (var.getName().contains("osc_input_freq")) {
//            System.err.println("Found it "+var.getName());
//         }
         if (!var.isDerived()) {
            var.notifyListeners();
         }
      }
      // Activate the dynamic signal mappings
      if (dynamicSignalPinMappings != null) {
         for (SignalPinMapping signalPinMapping: dynamicSignalPinMappings) {
            signalPinMapping.activate();
         }
      }
      // Allow all variable change notifications
      fInitPhase = InitPhase.VariableAndGuiPropagationAllowed;

//      System.err.println("Cleaning min-max");
      /**
       * Trigger updates of calculated min and max
       */
      for (Entry<String, Variable> entry:fVariables.entrySet()) {
         Variable var = fVariables.get(entry.getKey());
         if (var instanceof LongVariable) {
            LongVariable lv = (LongVariable) var;
            try {
               lv.getMin();
               lv.getMax();
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
   }
   
   /**
    * Save persistent settings to the current settings path
    */
   public void saveSettings() {
      saveSettingsAs(fProjectSettingsPath);
   }

   static class DeviceNamePattern {
      String fPattern;
      String fSubstitution;
      
      DeviceNamePattern(String pattern, String substitution) {
         fPattern      = pattern;
         fSubstitution = substitution;
      }
   };
   
   /**
    * Save persistent settings to the given path
    */
   public void saveSettingsAs(Path path) {
//      System.err.println("DeviceInfo.saveSettingsAs("+path.toAbsolutePath()+")");
      fProjectSettingsPath = path;
      Settings settings = new Settings("USBDM");
      settings.put(USBDMPROJECT_SUBFAMILY_SETTING_KEY,    fDeviceSubFamily);
      settings.put(USBDMPROJECT_VARIANT_SETTING_KEY,      fPreciseName);
      settings.put(HARDWARE_SOURCE_FILENAME_SETTINGS_KEY, fHardwarePath.getFileName().toString());
      
      // Save settings from pins e.g. pin mapping and descriptions
      for (String pinName:fPins.keySet()) {
         Pin pin = fPins.get(pinName);
         pin.saveSettings(settings);
      }
      // Save settings from signals e.g. signal descriptions and code identifiers
      for (String signalName:fSignals.keySet()) {
         Signal signal = fSignals.get(signalName);
         signal.saveSettings(settings);
      }
      // Save custom items from peripherals
      for (String peripheralName:fPeripheralsMap.keySet()) {
         Peripheral peripheral =  fPeripheralsMap.get(peripheralName);
         peripheral.saveSettings(settings);
      }
      // Save variables
      for (String key:fVariables.keySet()) {
         Variable var = fVariables.get(key);
         if (!var.isDerived() && !var.isDefault()) {
            settings.put(key, var.getPersistentValue());
         }
      }
      try {
         settings.save(fProjectSettingsPath.toAbsolutePath());
      } catch (Exception e) {
         Activator.logError(e.getMessage(), e);
      }
      setDirty(false);
   }

   /**
    * Indicates if the data has changed since being loaded
    * 
    * @return true if changed
    */
   public boolean isDirty() {
      return fIsDirty;
   }
   
   /**
    * Indicates if the data has changed since being loaded
    * 
    * @return true if changed
    */
   public void setDirty(boolean dirty) {
      fIsDirty = dirty;
      notifyListeners();
   }

   /**
    * Create device hardware description from Eclipse project file<br>
    * 
    * @param project    Project to load files from
    * @param device
    * @param monitor
    * 
    * @return Created hardware description for device
    * 
    * @throws Exception
    */
   public static DeviceInfo create(IProject project, Device device, IProgressMonitor monitor) throws Exception {
      SubMonitor subMonitor = SubMonitor.convert(monitor);
      subMonitor.subTask("Opening Project");
      IFile projectFile = project.getFile(USBDM_PROJECT_FILENAME+PROJECT_FILE_EXTENSION);
      if (projectFile.exists()) {
         // Load configuration
         Path filePath = Paths.get(projectFile.getLocation().toPortableString());
         return createFromSettingsFile(device, filePath);
      }
      return null;
   }

   /**
    * Creates vector table information and add to variable map
    * 
    * @param fVariables2
    * @param devicePeripherals
    * @param monitor
    * 
    * @throws Exception
    */
   private void generateVectorTable(DevicePeripherals devicePeripherals, IProgressMonitor monitor) throws Exception {
      // Get description of all peripherals for this device
      SubMonitor progress = SubMonitor.convert(monitor, getPeripherals().size()*100);
      progress.subTask("Modifying vector table");
      
      // Update vector table
      VectorTable vectorTable = devicePeripherals.getVectorTable();
      for (String peripheralName:getPeripherals().keySet()) {
         getPeripherals().get(peripheralName).modifyVectorTable(vectorTable);
         progress.worked(100);
      }
      // Add vector information to variable map
      addOrUpdateStringVariable("Include files needed for vector table", UsbdmConstants.C_VECTOR_TABLE_INCLUDES_KEY, vectorTable.getCIncludeFiles(),       true);
      addOrUpdateStringVariable("Vector table entries",                  UsbdmConstants.C_VECTOR_TABLE_KEY,          vectorTable.getCVectorTableEntries(), true);
   }
   
   /**
    * Generate CPP files (pin_mapping.h, gpio.h)<br>
    * Used for testing (files created relative to executable)
    * 
    * @throws Exception
    */
   public void generateCppFiles() throws Exception {
      
      // Output directory for test files
      Path folder = Paths.get("Testing");

      // Generate device header file
      Path headerfilePath   = folder.resolve(UsbdmConstants.PROJECT_INCLUDE_FOLDER).resolve(getDeviceSubFamily()+".h");
      DevicePeripherals devicePeripherals = getDevicePeripherals();
      devicePeripherals.writeHeaderFile(headerfilePath, new NullProgressMonitor());

      // Generate pinmapping.h etc
      WriteFamilyCpp writer = new WriteFamilyCpp();
      writer.writeCppFiles(folder, "", this);

      // Regenerate vectors.cpp
      generateVectorTable(devicePeripherals, new NullProgressMonitor());
//      FileUtility.refreshFile(folder.resolve(UsbdmConstants.PROJECT_VECTOR_CPP_PATH), variableMap);
      
      StringBuilder actionRecord = new StringBuilder();
      ProcessProjectActions processProjectActions = new ProcessProjectActions();
      regenerateProjectFiles(actionRecord, processProjectActions, null, false, new NullProgressMonitor());
      for (String key:fPeripheralsMap.keySet()) {
         Peripheral p = fPeripheralsMap.get(key);
         if (p instanceof PeripheralWithState) {
            ((PeripheralWithState) p).regenerateProjectFiles(actionRecord, processProjectActions, null, new NullProgressMonitor());
         }
      }
      Activator.log(actionRecord.toString());
   }
   /**
    * Generate CPP files (pin_mapping.h, gpio.h etc) within an Eclipse C++ project
    * 
    * @param project       Destination Eclipse C++ project
    * @param newProject
    * @param monitor
    * @throws Exception
    */
   public synchronized void generateCppFiles(IProject project, boolean isNewProject, IProgressMonitor monitor) throws Exception {
      SubMonitor subMonitor = SubMonitor.convert(monitor, (fPeripheralsMap.size()+5)*100);

      // Generate device header file
      Path projectDirectory = Paths.get(project.getLocation().toPortableString());
//      Path headerfilePath   = projectDirectory.resolve(UsbdmConstants.PROJECT_INCLUDE_FOLDER).resolve(getDeviceSubFamily()+".h");

      subMonitor.subTask("Parsing project action files");
      DevicePeripherals devicePeripherals = getDevicePeripherals();
      Path headerfilePath   = projectDirectory.resolve(UsbdmConstants.PROJECT_INCLUDE_FOLDER).resolve(devicePeripherals.getName()+".h");
      subMonitor.worked(10);
      ModeControl.setUseNamesInFieldMacros(true);
      devicePeripherals.writeHeaderFile(headerfilePath, subMonitor.newChild(10));
      
      // Generate pinmapping.h etc
      WriteFamilyCpp writer = new WriteFamilyCpp();
      writer.writeCppFiles(project, this, subMonitor.newChild(10));
      
      // Regenerate information for vectors.cpp
      generateVectorTable(devicePeripherals, subMonitor.newChild(10));
      
      StringBuilder actionRecord = new StringBuilder();
      actionRecord.append("Actions for regenerating project files\n\n");

      ProcessProjectActions processProjectActions = new ProcessProjectActions();
      regenerateProjectFiles(actionRecord, processProjectActions, project, isNewProject, subMonitor.newChild(10));
      for (String key:fPeripheralsMap.keySet()) {
         Peripheral p = fPeripheralsMap.get(key);
         if (p instanceof PeripheralWithState) {
            PeripheralWithState periph = ((PeripheralWithState) p);
            periph.regenerateProjectFiles(actionRecord, processProjectActions, project, subMonitor.newChild(10));
         }
      }
      Activator.log(actionRecord.toString());
   }

   /**
    * Generate CPP files (pin_mapping.h, gpio.h etc) within an Eclipse C++ project<br>
    * <b>Only used during initial project creation</b>
    * 
    * @param project       Eclipse C++ project
    * @param device
    * @param monitor
    * 
    * @throws Exception
    */
   static public void generateInitialProjectFiles(IProject project, Device device, IProgressMonitor monitor) {
      SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

      // Load hardware project
      DeviceInfo deviceInfo;
      try {
         deviceInfo = DeviceInfo.create(project, device, subMonitor.newChild(10));
         if (deviceInfo != null) {
            // Generate C++ code
            deviceInfo.generateCppFiles(project, true, subMonitor.newChild(90));
            deviceInfo.saveSettings(project);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   /**
    * Save persistent settings to the current settings path
    * 
    * @param project Associated project to refresh
    */
   public void saveSettings(IProject project) {
      saveSettingsAs(fProjectSettingsPath, project);
   }

   /**
    * Save persistent settings to the given path
    * 
    * @param project Associated project to refresh
    */
   public void saveSettingsAs(Path path, IProject project) {
      saveSettingsAs(path);
      if (project != null) {
         org.eclipse.core.runtime.IPath ePath = new org.eclipse.core.runtime.Path(path.toAbsolutePath().toString());
         if (project.getLocation().isPrefixOf(ePath)) {
            ePath = ePath.makeRelativeTo(project.getLocation());
            try {
               project.getFile(ePath).getParent().refreshLocal(IFile.DEPTH_ONE, null);
            } catch (CoreException e) {
               Activator.logError(e.getMessage(), e);
            }

         }
      }
   }
   
   /**
    * Get model for device information
    * 
    * @param parent
    * @return
    */
   public BaseModel[] getModels(DeviceInfo deviceInfo, BaseModel parent) {
      BaseModel[] models = {
            new ConstantModel(parent, "Device", "", deviceInfo.getPreciseName()),
            new ConstantModel(parent, "Hardware File", "", deviceInfo.getSourceFilename()),
            new DeviceVariantModel(parent, deviceInfo),
            new DevicePackageModel(parent, deviceInfo),
      };
      return models;
   }

   /**
    * Adds a variable
    * 
    * @param variable  Variable to add
    * 
    * @throws Exception if variable is already present
    */
   public void addVariable(Variable variable) {
      if (fVariables.put(variable.getKey(), variable) != null) {
         throw new RuntimeException("Variable already present \'"+variable.getKey()+"\'");
      }
      // Listener for variable change (sets dirty)
      variable.addListener(this);
   }
   
   /**
    * Adds or updates a variable based on a string constant<br>
    * If the variable doesn't exist a new StringVariable is created
    * 
    * @param name      Display name for variable if created.
    * @param key       Key used to identify variable.
    * @param value     Value for variable to add/create
    * @param isDerived Indicates whether the variable (if added) is derived (calculated) for user controlled
    */
   public void addOrUpdateStringVariable(String name, String key, String value, boolean isDerived) {
      fVariables.addOrUpdateVariable(name, key, value, isDerived);
   }
   
   /**
    * Removes a variable.<br>
    * If the variable doesn't exist it is ignored.
    * 
    * @param key       Key used to identify variable
    */
   public void removeVariableIfExists(String key) {
      fVariables.remove(key);
   }

   /**
    * Removes a variable
    * 
    * @param key       Key used to identify variable
    * 
    * @throws Exception if variable is not present
    */
   public void removeVariable(String key) {
      if (fVariables.remove(key) == null) {
         throw new RuntimeException("Variable not present \'"+key+"\'");
      }
   }

   @Override
   public Variable safeGetVariable(String key) {
      if (key.endsWith("[]")) {
         String baseKey = key.substring(0,key.length()-2);
         
         // XXX Eventually remove
         if (fVariables.safeGet(baseKey) != null) {
            throw new RuntimeException("Use of non-indexed variable '"+baseKey+"' with []");
         }
         // Use 0 index
         return fVariables.safeGet(baseKey+"[0]");
      }
      else if (key.endsWith("[0]")) {
//         String baseKey = key.substring(0,key.length()-3);
         
         // XXX Eventually remove
//         if (fVariables.safeGet(baseKey) != null) {
//            throw new RuntimeException("Use of non-indexed variable '"+baseKey+"' with [0]");
//         }
         // Use 0 index
         return fVariables.safeGet(key);
      }
      Variable var = fVariables.safeGet(key);
//      if (var == null) {
//         // Try active clock selection as well
//         var = fVariables.safeGet(key+"["+fActiveClockSelection+"]");
//      }
      if ((var == null) && key.endsWith(".")) {
         // No longer supported
         throw new RuntimeException("Dot at end of key");
      }
      return var;
   }

   @Override
   public Variable getVariable(String key) throws Exception {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         throw new Exception("Variable does not exist for key \'"+key+"\'");
      }
      return variable;
   }

   /**
    * Creates a map from variable keys to values
    * 
    * @return Map of key->value pairs
    */
   public ISubstitutionMap getVariablesSymbolMap() {
      return fVariables.getSubstitutionMap();
   }

   /**
    * Get Menu data for Device page
    * @return
    */
   public MenuData getData() {
      return fMenuData;
   }

   @Override
   public BaseModel getModel(BaseModel parent) {
      return new DeviceInformationModel(parent, this);
   }

   @Override
   public void getModels(BaseModel parent) {
      new DeviceInformationModel(parent, this);
   }

   /**
    * Does variable substitution in a string using the device variable map
    * 
    * @param input  String to process
    * 
    * @return Modified string or original if no changes
    */
   private String substitute(String input) {
      ISubstitutionMap map = getVariablesSymbolMap();
      return fVariableProvider.substitute(input, map);
   }
   
   /**
    * Write information to documentUtilities for information declared at USBDM namespace
    * 
    * @param documentUtilities
    * @throws IOException
    */
   public void writeNamespaceInfo(DocumentUtilities documentUtilities) throws IOException {
      if (fMenuData == null) {
         return;
      }
      VariableProvider vp = new VariableProvider("$DeviceInfo", this);
      String template = fMenuData.getTemplate("usbdm", "", vp);
      if ((template != null) && (!template.isEmpty())) {
         documentUtilities.write(substitute(template));
      }
   }

   /**
    * @param processProjectActions
    * @param project
    * @param isNewProject
    * @param monitor
    * 
    * @throws Exception
    */
   public void regenerateProjectFiles(
         StringBuilder         actionRecord,
         ProcessProjectActions processProjectActions,
         IProject              project,
         boolean               isNewProject,
         IProgressMonitor      monitor) throws Exception {
      
      SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
      if (fMenuData == null) {
         return;
      }
      ISubstitutionMap symbolMap = getVariablesSymbolMap();
      
      if (isNewProject) {
         // Actions for new project
         VariableProvider variableProvider = new VariableProvider("Common Settings", this);
         MenuData initialMenuData = ParseMenuXML.parseMenuFile("new_project_actions", variableProvider);
         processProjectActions.process(actionRecord, project, initialMenuData.getProjectActionList(), symbolMap, subMonitor.newChild(100));
      }
      
      processProjectActions.process(actionRecord, project, fMenuData.getProjectActionList(), symbolMap, subMonitor.newChild(100));
   }

   @Override
   public void modelElementChanged(ObservableModel observableModel) {
      setDirty(true);
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
      setDirty(true);
   }

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
      setDirty(true);
   }

   /**
    * Get peripheral information from SVD file<br>
    * This is a time consuming operation.
    * 
    * @return Peripheral information
    * 
    * @throws UsbdmException
    */
   public DevicePeripherals getDevicePeripherals() throws UsbdmException {
      DevicePeripheralsFactory factory           = new DevicePeripheralsFactory();
      DevicePeripherals        devicePeripherals = factory.getDevicePeripherals(fDeviceSubFamily);
//      System.out.println("Device sub family = "+fDeviceSubFamily);
      if (devicePeripherals == null) {
         throw new UsbdmException("Failed to create devicePeripherals from SVD for \'"+ fDeviceSubFamily + "\'");
      }
      return devicePeripherals;
   }
   
   /**
    * Get device name e.g. MK20DN32M5 for use with DeviceDatabase lookup<br>
    * This is deduced from the variant name e.g. MK20DN32VLF5
    * 
    * @return Device name
    */
   public static String getDeviceName(String variantName) {
      final String packageSuffixes = "(CAF|VDC|VFG|VFK|VFT|VFM|VLC|VLF|VLH|VLL|VLQ|VMC|VMD|VMP|VTG|VWJ)";
      DeviceNamePattern patterns[] = {
            new DeviceNamePattern("^(MK(L|E)?\\d+(Z|DN|DX|FN|FX)\\d+(M\\d+)?)"+packageSuffixes+"(\\d+)$", "$1M$6"), // MK20DN32VLF5 -> MK20D5
      };
      
      for (DeviceNamePattern pattern:patterns) {
         Pattern p = Pattern.compile(pattern.fPattern);
         Matcher m = p.matcher(variantName);
         if (m.matches()) {
            String deviceName = m.replaceAll(pattern.fSubstitution);
            return deviceName;
         }
      }
      return variantName;
   }

   /**
    * Create DeviceVariantInformation from XML element
    * 
    * @param element Element to parse e.g. <br>
    * &lt;device variant="FRDM_K20D50M" manual="K20P64M50SF0RM" package="FRDM_K20D50M" deviceName="" />
    * 
    * @return DeviceVariantInformation created.
    * @throws UsbdmException
    * 
    * @note Note DevicePackage may be created but will be empty if it did not previously exist.
    */
   public DeviceVariantInformation parseDeviceInformationXML(Element element) throws UsbdmException {
      String variantName = element.getAttribute("variant");
      String manual      = element.getAttribute("manual");
      String packageName = element.getAttribute("package");
      String deviceName  = element.getAttribute("deviceName");
      return createDeviceInformation(variantName, manual, packageName, deviceName);
   }

   /** Map used to prevent repeated items for iterated enums, templates etc */
   private final HashSet<String> repeatedItemMap = new HashSet<String>();

   private int fActiveClockSelection;

   private ModelFactory fModelFactory;
   
   /**
    * Check if item has already been generated in the C code.
    * It is immediately added to the list of repeated items.
    * 
    * @param key Key used to identify item
    * 
    * @return true if already generated
    * @return false if not already generated
    */
   public boolean addAndCheckIfRepeatedItem(String key) {
      if (repeatedItemMap.contains(key)) {
         return true;
      }
      repeatedItemMap.add(key);
      return false;
   }

   /**
    * Check if item has already been generated in the C code
    * It is not added to the list of repeated items.
    * 
    * @param key Key used to identify item
    * 
    * @return true if already generated
    * @return false if not already generated
    */
   public boolean checkIfRepeatedItem(String key) {
      return repeatedItemMap.contains(key);
   }

   public void setActiveClockSelection(int index) {
      fActiveClockSelection = index;
      if (fModelFactory != null) {
         fModelFactory.refresh();
      }
   }

   public int getActiveClockSelection() {
      return fActiveClockSelection;
   }

   public void setModelFactory(ModelFactory modelFactory) {
      fModelFactory = modelFactory;
   }

   /**
    * Indicates variable update propagation is suspended
    * 
    * @return
    */
   public InitPhase getInitialisationPhase() {
      return fInitPhase;
   }

   /** List of signal to pin mappings dependent on variables */
   ArrayList<SignalPinMapping> dynamicSignalPinMappings;
   
   /**
    * Add dynamic signal->pin mapping dependent on variables
    * 
    * @param signalPinMapping
    */
   public void addDynamicSignalMapping(SignalPinMapping signalPinMapping) {
      
      if (dynamicSignalPinMappings == null) {
         dynamicSignalPinMappings = new ArrayList<SignalPinMapping>();
      }
      dynamicSignalPinMappings.add(signalPinMapping);
   }

//   /**
//    * Test main
//    *
//    * @param args
//    */
//   public static void main(String[] args) {
//      String variantNames[] = {
//            "MK20DN32VLF5",   "MK20DX32VLF5",   "MK20DN64VLF5",    "MK20DX64VLF5",   "MK20DN128VLF5",
//            "MK20DX128VLF5",  "MK20DN32VFT5",   "MK20DX32VFT5",    "MK20DN64VFT5",   "MK20DX64VFT5",
//            "MK20DN128VFT5",  "MK20DX128VFT5",  "MK22FN512VDC12",  "MK22FN512VLL12", "MK22FN512VLH12",
//            "MK22FN512VMP12", "MK22FX512VMC12", "MK22FN1M0VMC12",  "MKL05Z8VFK4",    "MKL05Z16VFK4",
//            "MKL05Z32VFK4",   "MKL05Z8VLC4",    "MKL05Z16VLC4",    "MKL05Z32VLC4",   "MKL05Z8VFM4",
//            "MKL05Z16VFM4",   "MKL05Z32VFM4",   "MKL05Z16VLF4",    "MKL05Z32VLF4",
//      };
//
//      for (String variantName:variantNames) {
//            System.err.println("'" + variantName + "' => '" + getDeviceName(variantName) + "'");
//      }
//
//   }

}
