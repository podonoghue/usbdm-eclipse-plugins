package net.sourceforge.usbdm.deviceEditor.information;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ConstantModel;
import net.sourceforge.usbdm.deviceEditor.model.DevicePackageModel;
import net.sourceforge.usbdm.deviceEditor.model.DeviceVariantModel;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.ParseFamilyCSV;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForAdc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForCmp;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForCmt;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForConsole;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForControl;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForDac;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForDmaMux;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForEnet;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForEwm;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForFlexBus;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForFlexCan;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForFtfl;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForFtm;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForGpio;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForI2c;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForI2s;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLcd;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLlwu;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLptmr;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLpuart;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForMcg;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForNull;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForOsc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPdb;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPit;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPower;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForRtc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForSdhc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForSdramc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForShared;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForSim;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForSmc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForSpi;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForToDo;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForTsi;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForUart;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForUsb;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForUsbdcd;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForUsbhs;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForVref;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseFamilyXML;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseMenuXML;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripheralsFactory;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

public class DeviceInfo extends ObservableModel {

   /** Version number */
   public static final String VERSION           = "1.2.0";

   /** DTD file to reference in XML */
   public static final String DTD_FILE          = "_Hardware.dtd";

   /** Name space for C files */
   public static final String NAME_SPACE        = "USBDM";

   /** How to handle existing files etc */
   public enum Mode {ignore, fail};

   /** Device families */
   public enum DeviceFamily {mk, mke, mkl, mkm, mkv};

   /** Path of file containing device hardware description */
   private Path fHardwarePath;

   /** Path of file containing project settings */
   private Path fProjectSettingsPath;

   /** Family name of device */
   private String fFamilyName = null;

   /** Device family for this device */
   private DeviceFamily fDeviceFamily = null;

   /** Indicates if the data has changed since being loaded */
   private boolean fIsDirty = false;

   /** File name extension for project file */
   public static final String PROJECT_FILE_EXTENSION = ".usbdmProject";

   /** File name extension for hardware description file */
   public static final String HARDWARE_FILE_EXTENSION = ".usbdmHardware";

   /** File name extension for hardware description file (CSV format) */
   public static final String HARDWARE_CSV_FILE_EXTENSION = ".csv";

   /** Name of default USBDM project file in Eclipse project */
   public static final String USBDM_PROJECT_FILENAME = "Configure";
   
   /** Relative location of hardware files in USBDM installation */
   public static final String USBDM_HARDWARE_LOCATION  = "Stationery/Packages/180.ARM_Peripherals/Hardware";
   
   /** Key for device variant persistence */
   public static final String DEVICE_VARIANT_SETTINGS_KEY   = "$$DeviceInfo_Device_Variant"; 

   /** Key for target device persistence */
   public static final String DEVICE_NAME_SETTINGS_KEY      = "$$DeviceInfo_Target_Device"; 

   /** Key for hardware source file persistence */
   public static final String HARDWARE_SOURCE_FILENAME_SETTINGS_KEY = "$$Hardware_Source_Filename"; 

   /** Map of variables for this peripheral */
   private final HashMap<String, Variable> fVariables = new HashMap<String, Variable>();

   /**
    * Create empty device information
    */
   private DeviceInfo() {
   }

   /**
    * Create DeviceInfo from hardware file, either .csv or .usbdmHardware<br>
    * Several locations will be searched for the file
    * 
    * @param filePath   Path to file
    * 
    * @return DeviceInfo created
    * 
    * @throws Exception
    */
   private static DeviceInfo createFromHardwareFile(Path filePath) throws Exception {
      
      String filename  = filePath.getFileName().toString();
      if (!filename.endsWith(HARDWARE_FILE_EXTENSION) &&
          !filename.endsWith(HARDWARE_CSV_FILE_EXTENSION)) {
         throw new Exception("Incorrect file type"+ filePath);
      }

      if (!filePath.isAbsolute()) {
         // Try default locations
         do {
            // As is
            Path path = filePath.toAbsolutePath();
            if (Files.isReadable(path)) {
               filePath = path;
               continue;
            }
            // Debug location
            path = Paths.get("hardware").resolve(filePath);
            if (Files.isReadable(path)) {
               filePath = path;
               continue;
            }
            // USBDM installation
            filePath = Paths.get(Usbdm.getUsbdmResourcePath()).resolve(USBDM_HARDWARE_LOCATION).resolve(filePath);
         } while (false);
      }
      if (!Files.isReadable(filePath)) {
         throw new Exception("Cannot locate file "+ filePath);
      }
      DeviceInfo deviceInfo = new DeviceInfo();
      deviceInfo.parse(filePath);
      return deviceInfo;
   }
   /**
    * Create device hardware description from given file<br>
    * An associated settings file may be opened if a <b>.usbdmHardware</b> file is provided
    * 
    * @param filePath   Path to <b>.usbdmProject</b> or <b>.usbdmHardware</b> file
    * 
    * @return Create hardware description for device
    * 
    * @throws Exception
    */
   public static DeviceInfo createFromSettingsFile(Path filePath) throws Exception {
      
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
            path = Paths.get("hardware").resolve(hardwarePath);
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
      deviceInfo.parse(hardwarePath);
      deviceInfo.loadSettings(projectSettings);
      return deviceInfo;
   }

   /**
    * Create device hardware description from given file<br>
    * 
    * @param filePath   Path to <b>.usbdmProject</b> or <b>.usbdmHardware</b> file
    * 
    * @return Create hardware description for device
    * 
    * @throws Exception
    */
   public static DeviceInfo create(Path filePath) throws Exception {
      
      String filename  = filePath.getFileName().toString();

      if (filename.endsWith(HARDWARE_FILE_EXTENSION)) {
         return createFromHardwareFile(filePath);
      }
      else if (filename.endsWith(HARDWARE_CSV_FILE_EXTENSION)) {
         return createFromHardwareFile(filePath);
      }
      else if (filename.endsWith(PROJECT_FILE_EXTENSION)) {
         return createFromSettingsFile(filePath);
      }
      else {
         throw new RuntimeException("Unknown file type " + filePath);
      }
   }

   /**
    * Load hardware description from file
    * 
    * @param hardwarePath  Path to load from
    * 
    * @throws Exception
    */
   private void parse(Path hardwarePath) throws Exception {
      System.err.println("DeviceInfo.parse(" + hardwarePath.toAbsolutePath() + ")");
      fHardwarePath = hardwarePath;
      String filename = fHardwarePath.getFileName().toString();
      if (filename.endsWith(HARDWARE_CSV_FILE_EXTENSION)) {
         ParseFamilyCSV parser = new ParseFamilyCSV();
         parser.parseFile(this, fHardwarePath);
      }
      else if ((filename.endsWith("xml"))||(filename.endsWith(HARDWARE_FILE_EXTENSION))) {
         ParseFamilyXML parser = new ParseFamilyXML();
         parser.parseFile(this, fHardwarePath);

         ArrayList<PeripheralWithState> PeripheralWithStateList = new ArrayList<PeripheralWithState>();
         
         // Construct list of all PeripheralWithState
         for (String name:fPeripheralsMap.keySet()) {
            Peripheral p = fPeripheralsMap.get(name);
            if (p instanceof PeripheralWithState) {
               PeripheralWithStateList.add((PeripheralWithState) fPeripheralsMap.get(name));
            }
         }
         // Sort in priority order
         Collections.sort(PeripheralWithStateList, new Comparator<PeripheralWithState>() {
            @Override
            public int compare(PeripheralWithState o1, PeripheralWithState o2) {
               return o2.getPriority()-o1.getPriority();
            }
         });
         // Construct
         for (PeripheralWithState p:PeripheralWithStateList) {
            if (p instanceof PeripheralWithState) {
//               System.err.println("Constructing " + p);
               ((PeripheralWithState) p).loadModels();
            }
         }
      }
      else {
         throw new Exception("Unexpected file type for " + hardwarePath);
      }
   }

   /**
    * Set family name
    * 
    * @param familyName  Family name e.g. MK22FA12
    */
   public void setFamilyName(String familyName) {
      fFamilyName = familyName;

      if (fDeviceFamily == null) {
         // A bit crude
         if (fFamilyName.startsWith("MKE")) {
            fDeviceFamily = DeviceFamily.mke;
         }
         else if (fFamilyName.startsWith("MKL")) {
            fDeviceFamily = DeviceFamily.mkl;
         }
         else if (fFamilyName.startsWith("MKM")) {
            fDeviceFamily = DeviceFamily.mkm;
         }
         else {
            fDeviceFamily = DeviceFamily.mk;
         }
      }
      setDirty(true);
   }

   /**
    * Set device family
    * 
    * @param familyName  Family name e.g. MK22FA12
    */
   public void setFamily(DeviceFamily deviceFamily) {
      fDeviceFamily = deviceFamily;
      setDirty(true);
   }

   /** 
    * Get device family for this device 
    */
   public String getFamilyName() {
      return fFamilyName;
   }

   /** 
    * Get device family for this device 
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
            return peripheral;
         }         
      }
      throw new RuntimeException("Failed to find pattern that matched peripheral: \'" + name + "\'");
   }


   //   /**
   //    * Find or create a peripheral
   //    * 
   //    * @param baseName   Base name e.g. FTM3 => FTM
   //    * @param instance   Instance of peripheral e.g. FTM2 => 2 
   //    * @param mode       Mode.anyInstance to allow null instances  
   //    * 
   //    * @return
   //    */
   //   public Peripheral findOrCreatePeripheral(String baseName, String instance, PeripheralTemplateInformation template) {  
   //      Peripheral p = fPeripheralsMap.get(baseName+instance);
   //      if (p == null) {
   //         p = createPeripheral(baseName, instance, template);
   //      }
   //      return p;
   //   }

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
         throw new RuntimeException("No such instance " + name);
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
    * e.g. createSignal(FTM,0,6) = <i>Signal</i>(FTM, 0, 6)
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
    * @return Signal found
    * 
    * @throws Exception signal nor found
    */
   public Signal findSignal(String name) {
      Signal signal = null;
      if (name.equalsIgnoreCase("Disabled")) {
         return Signal.DISABLED_SIGNAL;
      }
      signal = fSignals.get(name);
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
    * Create device package information
    * Will create new package information if necessary
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

   /**
    * Create pin from name<br>
    * 
    * @param name Name of pin
    *                      
    * @return Created pin
    * 
    * @throws Exception if the pin already exists
    */
   public Pin createPin(String name) {
      // Check for repeated pin
      Pin pinInformation = fPins.get(name);
      if (pinInformation != null) {
         throw new RuntimeException("Pin already exists: " + name);
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
         template = new SignalTemplate(this, deviceFamily, namePattern, signalPattern, instancePattern, matchTemplate, instanceWriterClass);
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
      createPeripheralTemplateInformation(
            "GPIO", "$2", "$4",
            "^(GPIO|PORT)([A-I])(_(\\d+))?$",
            getDeviceFamily(),
            WriterForGpio.class);
      
      createPeripheralTemplateInformation(
            "POWER", "", "$0",
            "(VOUT33|VBAT|VREFL|VREFH|VSS(A|B)?|VDD(A|B)?|VREG(IN|_IN0|_IN1|_OUT))(\\d*(a|b|c)?)",
            getDeviceFamily(),
            WriterForPower.class);
      
      createPeripheralTemplateInformation(
            "CONTROL", "", "$0",
            "(JTAG|EZP|SWD|CLKOUT|NMI_b|RESET_b|TRACE)_?(.*)",
            getDeviceFamily(),
            WriterForControl.class);

      if (getDeviceFamily() != DeviceFamily.mkm) {
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(ADC)([0-3])_((SE|DM|DP)\\d+(a|b)?)",
               getDeviceFamily(),
               WriterForAdc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(CMP)([0-3])?_(.*)",
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
               "FB", "", "$2",
               "(FB|FLEXBUS)_(.*)",
               getDeviceFamily(),
               WriterForFlexBus.class);
         createPeripheralTemplateInformation(
               "CAN", "$2", "$3",
               "(CAN)([0-5])_(RX|TX)",
               getDeviceFamily(),
               WriterForFlexCan.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(FTM)([0-3])_(CH\\d+|QD_PH[A|B]|FLT\\d|CLKIN\\d)",
               getDeviceFamily(),
               WriterForFtm.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(I2C)([0-3])_(SCL|SDA|4WSCLOUT|4WSDAOUT)",
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
               "(LPTMR)(0)_(ALT\\d+)",
               getDeviceFamily(),
               WriterForLptmr.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(LPUART)([0-6])_(TX|RX|CTS_b|RTS_b)",
               getDeviceFamily(),
               WriterForLpuart.class);
         createPeripheralTemplateInformation(
               "$1", "", "",
               "(MCG)",
               getDeviceFamily(),
               WriterForMcg.class);
         createPeripheralTemplateInformation(
               "OSC", "0", "$1",
               "(E?XTAL)(0)?",
               getDeviceFamily(),
               WriterForOsc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(PIT)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForPit.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(PDB)(0?)_(EXTRG)",
               getDeviceFamily(),
               WriterForPdb.class);
         createPeripheralTemplateInformation(
               "RTC", "", "$2",
               "(RTC)_?(CLKOUT|CLKIN|WAKEUP_b)",
               getDeviceFamily(),
               WriterForRtc.class);
         createPeripheralTemplateInformation(
               "RTC", "", "$1",
               "(E?XTAL32)",
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
               "$1", "$2", "$3",
               "(SPI)([0-3])_(SCK|SIN|SOUT|MISO|MOSI|SS|PCS\\d*)",
               getDeviceFamily(),
               WriterForSpi.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(TPM)([0-3])_(CH\\d+|QD_PH[A|B]|CLKIN\\d)",
               getDeviceFamily(),
               WriterForFtm.class);
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
               "USB", "0", "$5",
               "((audio)?USB(OTG)?(0)?)_(.*)",
               getDeviceFamily(),
               WriterForUsb.class);
         createPeripheralTemplateInformation(
               "USBHS", "", "$2",
               "USB1(_)?(.*)",
               getDeviceFamily(),
               WriterForUsbhs.class);
         createPeripheralTemplateInformation(
               "USBDCD", "", "$3",
               "(USBDCD)_?(.*)",
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
               "$1", "$2", "$3",
               "(FTM)_()(CLKIN\\d+)",
               getDeviceFamily(),
               WriterForShared.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(TPM)_()(CLKIN\\d+)",
               getDeviceFamily(),
               WriterForShared.class);
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
               "FLEXIO", "", "",
               "(FXIO|FLEXIO).*",
               getDeviceFamily(),
               WriterForToDo.class);
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
               "$1", "", "$3",
               "(CRC)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForToDo.class);
         createPeripheralTemplateInformation(
               "$1", "0", "$3",
               "(DMA)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForToDo.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(MPU)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForToDo.class);
         createPeripheralTemplateInformation(
               "FTFA", "$2", "$3",
               "(FTFA)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForToDo.class);
         createPeripheralTemplateInformation(
               "FTFE", "$2", "$3",
               "(FTFE)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForToDo.class);
         createPeripheralTemplateInformation(
               "FTFL", "$2", "$3",
               "(FTFL)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForFtfl.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(RNG(A)?)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForToDo.class);
         createPeripheralTemplateInformation(
               "$1", "", "",
               "(USBHS)",
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

   /** Name of device */
   private String fDeviceName = null;

   void setDeviceName(String deviceName) {
      fDeviceName = deviceName;
   }
   
   public String getDeviceName() {
      return fDeviceName;
   }
   
   /** Name of the device variant e.g. MKL25Z4 */
   private String fVariantName = null;

   /** Device variant */
   private DeviceVariantInformation fDeviceVariant = null;

   /**
    * Set device variant
    * 
    * @param device variant name
    * 
    * @return
    */
   public void setDeviceVariant(String variantName) {
      if (fVariantName   != variantName) {
         fVariantName   = variantName;
         fDeviceVariant = fVariants.get(variantName);
         setDirty(true);
      }
   }

   /**
    * Get device variant
    * 
    * @return Name
    */
   public String getDeviceVariantName() {
      return fVariantName;
   }

   /**
    * Get device variant
    * 
    * @return Name
    */
   public DeviceVariantInformation getDeviceVariant() {
      return fDeviceVariant;
   }

   /** Map from variant names to devices */
   private Map<String, DeviceVariantInformation> fVariants = new TreeMap<String, DeviceVariantInformation>();

   /**
    * Create device information
    * 
    * @param name          Variant name
    * @param manual        Manual name
    * @param packageName   Package name
    * @return
    */
   public DeviceVariantInformation createDeviceInformation(String name, String manual, String packageName) {
      DeviceVariantInformation deviceInformation = new DeviceVariantInformation(name, findOrCreateDevicePackage(packageName), manual);
      fVariants.put(name, deviceInformation);

      if (fVariantName == null) {
         fVariantName   = name;
         fDeviceVariant = deviceInformation;
      }
      return deviceInformation;
   };

   /**
    * Get map from variant names to device variants
    * 
    * @return Map
    */
   public Map<String, DeviceVariantInformation> getDeviceVariants() {
      return fVariants;
   }

   /**
    * Find device variant from variant name
    * 
    * @param variant name
    * 
    * @return variant
    */
   public DeviceVariantInformation findVariant(String deviceName) {
      return fVariants.get(deviceName);
   }

   /*
    * DmaInfo =============================================================================================
    */

   //   /** List of DMA channels */
   //   private ArrayList<DmaInfo> fDmaInfoList = new ArrayList<DmaInfo>();

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

   //   /**
   //    * Get list of DMA entries
   //    *  
   //    * @return
   //    */
   //   public ArrayList<DmaInfo> getDmaList() {
   //      return fDmaInfoList;
   //   }

   private final static HashMap<String, MuxSelection> exceptions = new  HashMap<String, MuxSelection>();

   private static boolean checkOkException(Pin pin) {
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
         exceptions.put("PTA0",  MuxSelection.mux7);  // JTAG_TCLK/SWD_CLK
         exceptions.put("PTA1",  MuxSelection.mux7);  // JTAG_TDI
         exceptions.put("PTA2",  MuxSelection.mux7);  // JTAG_TDO/TRACE_SWO
         exceptions.put("PTA3",  MuxSelection.mux7);  // JTAG_TMS/SWD_DIO
         exceptions.put("PTA4",  MuxSelection.mux7);  // NMI_b
         exceptions.put("PTA5",  MuxSelection.mux7);  // JTAG_TRST_b
         // MKL
         exceptions.put("PTA20", MuxSelection.mux7);  // RESET_b
         // MKM
         exceptions.put("PTE6",  MuxSelection.mux7);  // SWD_DIO
         exceptions.put("PTE7",  MuxSelection.mux7);  // SWD_CLK
         exceptions.put("PTE1",  MuxSelection.mux7);  // RESET_b
      }
      MuxSelection exception = exceptions.get(pin.getName());
      return (exceptions != null) && (pin.getResetValue() == exception);
   }
   /**
    * Does some basic consistency checks on the data
    */
   public void consistencyCheck() {
      // Every pin should have a reset entry
      for (String pName:getPins().keySet()) {
         Pin pin = getPins().get(pName);
         if (!checkOkException(pin)) {
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
    * Get name of the pin with alias 
    * 
    * @return Pin name
    */
   public String getPinNameWithAlias(Pin pin) {
      String alias = "";
      DeviceVariantInformation deviceInformation = fVariants.get(fVariantName);
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
    * Load persistent settings
    * @throws IOException 
    */
   public Settings getSettings(Path path) throws Exception {
      System.err.println("DeviceInfo.getSettings(" + path.toAbsolutePath() + ")");
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
    */
   public void loadSettings(Settings settings) {
      try {
         String variantName = settings.get(DEVICE_VARIANT_SETTINGS_KEY);
         if (variantName != null) {
            setDeviceVariant(variantName);
         }
         String deviceName = settings.get(DEVICE_NAME_SETTINGS_KEY);
         if (deviceName != null) {
            setDeviceName(deviceName);
            try {
               ParseMenuXML.parseFile("symbols/"+deviceName, null, new PeripheralWithState("Symbols", "", this) {
                  @Override
                  public String getTitle() {
                     return null;
                  }

                  @Override
                  public void elementStatusChanged(ObservableModel observableModel) {
                  }
               });
            } catch (Exception e) {
            }
         }
         for (String pinName:fPins.keySet()) {
            Pin pin = fPins.get(pinName);
            pin.loadSettings(settings);
         }
         for (String peripheralName:fPeripheralsMap.keySet()) {
            Peripheral peripheral =  fPeripheralsMap.get(peripheralName);
            peripheral.loadSettings(settings);
         }
         for (String key:fVariables.keySet()) {
            String value = settings.get(key);
            if (value != null) {
               Variable var = fVariables.get(key);
               if (!var.isDerived()) {
                  var.setPersistentValue(value);
               }
            }
         }
         for (String peripheralName:fPeripheralsMap.keySet()) {
            Peripheral peripheral =  fPeripheralsMap.get(peripheralName);
            if (peripheral instanceof PeripheralWithState) {
               ((PeripheralWithState)peripheral).variableChanged(null);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      setDirty(false);
   }

   /**
    * Save persistent settings to the current settings path
    */
   public void saveSettings() {
      saveSettingsAs(fProjectSettingsPath);
   }

   /**
    * Save persistent settings to the given path
    */
   public void saveSettingsAs(Path path) {
      System.err.println("DeviceInfo.saveSettingsAs("+path.toAbsolutePath()+")");
      fProjectSettingsPath = path;
      Settings settings = new Settings("USBDM");
      settings.put(DEVICE_NAME_SETTINGS_KEY, fDeviceName);
      settings.put(DEVICE_VARIANT_SETTINGS_KEY, fVariantName);
      settings.put(HARDWARE_SOURCE_FILENAME_SETTINGS_KEY, fHardwarePath.getFileName().toString());
      
      // Save settings from pins e.g. pin mapping and descriptions
      for (String pinName:fPins.keySet()) {
         Pin pin = fPins.get(pinName);
         pin.saveSettings(settings);
      }
      // Save custom items from peripherals
      for (String peripheralName:fPeripheralsMap.keySet()) {
         Peripheral peripheral =  fPeripheralsMap.get(peripheralName);
         peripheral.saveSettings(settings);
      }
      // Save variables
      for (String key:fVariables.keySet()) {
         Variable var = fVariables.get(key);
         if (!var.isDerived()) {
            settings.put(key, var.getPersistentValue());
         }
      }
      try {
         settings.save(fProjectSettingsPath.toAbsolutePath());
      } catch (Exception e) {
         e.printStackTrace();
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
    * Create device hardware description from Eclipse project files<br>
    * 
    * @param project    Project to load files from
    * @param monitor
    * 
    * @return Created hardware description for device
    * 
    * @throws Exception
    */
   public static DeviceInfo create(IProject project, IProgressMonitor monitor) throws Exception {
      monitor.subTask("Opening Project");
      IFile projectFile = project.getFile(USBDM_PROJECT_FILENAME+PROJECT_FILE_EXTENSION);
      if (projectFile.exists()) {
         // Load configuration
         Path filePath = Paths.get(projectFile.getLocation().toPortableString());
         return DeviceInfo.create(filePath);
      }
      return null;
   }

   void doVectors(String deviceName) throws Exception {
      // Get description of all peripherals for device
      DevicePeripheralsFactory factory = new DevicePeripheralsFactory();
      // Get description of all peripherals for this device
      DevicePeripherals devicePeripherals = factory.getDevicePeripherals(deviceName);
      if (devicePeripherals == null) {
         throw new Exception ("Failed to create DevicePeripherals for "+ deviceName);
      }
   }

   /**
    * Creates vector table information and add to variable map
    * 
    * @param variableMap
    * @throws Exception
    */
   private void generateVectorTable(Map<String, String> variableMap) throws Exception {
      // Get description of all peripherals for device
      DevicePeripheralsFactory factory = new DevicePeripheralsFactory();
      String deviceName = fDeviceName;
      if (deviceName.equalsIgnoreCase("$(targetDevice)")) {
         // For testing
         deviceName = "FRDM_K22F";
      }
      // Get description of all peripherals for this device
      DevicePeripherals devicePeripherals = factory.getDevicePeripherals(deviceName);
      if (devicePeripherals == null) {
         throw new Exception ("Failed to create devicePeripherals when writing Vector Table for "+ deviceName);
      }
      // Update vector table
      VectorTable vectorTable = devicePeripherals.getVectorTable();
      for (String peripheralName:getPeripherals().keySet()) {
         getPeripherals().get(peripheralName).modifyVectorTable(vectorTable);
      }
      // Add information to variable map
      variableMap.put(UsbdmConstants.C_VECTOR_TABLE_INCLUDES_KEY, vectorTable.getCIncludeFiles());
      variableMap.put(UsbdmConstants.C_VECTOR_TABLE_KEY, vectorTable.getCVectorTableEntries());
   }
   
   /**
    * Generate CPP files (pin_mapping.h, gpio.h)<br>
    * Used for testing (files created relative to executable)
    * 
    * @throws Exception 
    */
   public void generateCppFiles() throws Exception {
      
      doVectors("FRDM_K22F");
      
      // Output directory for test files
      Path folder = Paths.get("Testing");

      // Generate pinmapping.h etc
      WriteFamilyCpp writer = new WriteFamilyCpp();
      writer.writeCppFiles(folder, "", this);

      // Regenerate vectors.cpp
      Map<String, String> variableMap = getSimpleSymbolMap();
      generateVectorTable(variableMap);
      FileUtility.refreshFile(folder.resolve(UsbdmConstants.PROJECT_VECTOR_CPP_PATH), variableMap);
      
      for (String key:fPeripheralsMap.keySet()) {
         Peripheral p = fPeripheralsMap.get(key);
         if (p instanceof PeripheralWithState) {
            ((PeripheralWithState) p).regenerateProjectFiles(null, new NullProgressMonitor());
         }
      }

   }

   /**
    * Generate CPP files (pin_mapping.h, gpio.h etc) within an Eclipse C++ project
    * 
    * @param project       Destination Eclipse C++ project 
    * @param monitor
    * @throws Exception 
    */
   public void generateCppFiles(IProject project, IProgressMonitor monitor) throws Exception {
      monitor.subTask("Generating CPP files");

      // Generate pinmapping.h etc
      WriteFamilyCpp writer = new WriteFamilyCpp();
      writer.writeCppFiles(project, this, monitor);
      
      // Regenerate vectors.cpp
      Map<String, String> variableMap = new HashMap<String, String>();
      generateVectorTable(variableMap);
      FileUtility.refreshFile(project, UsbdmConstants.PROJECT_VECTOR_CPP_PATH, variableMap, monitor);
      
      for (String key:fPeripheralsMap.keySet()) {
         Peripheral p = fPeripheralsMap.get(key);
         if (p instanceof PeripheralWithState) {
            ((PeripheralWithState) p).regenerateProjectFiles(project, monitor);
         }
      }
   }

   /**
    * Generate CPP files (pin_mapping.h, gpio.h etc) within an Eclipse C++ project<br>
    * Used during initial project creation
    * 
    * @param project       Eclipse C++ project 
    * @param monitor
    * 
    * @throws Exception
    */
   static public void generateFiles(IProject project, IProgressMonitor monitor) {
      SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

      // Load hardware project
      DeviceInfo deviceInfo;
      try {
         deviceInfo = DeviceInfo.create(project, subMonitor);
         if (deviceInfo != null) {
            // Generate C++ code
            deviceInfo.generateCppFiles(project, subMonitor);
            deviceInfo.saveSettings(project);
         }
      } catch (Exception e) {
        // Ignore
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
               project.getFile(ePath).getParent().refreshLocal(1, null);
            } catch (CoreException e) {
               e.printStackTrace();
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
            new ConstantModel(parent, "Device", "", deviceInfo.getDeviceName()),
            new ConstantModel(parent, "Hardware File", "", deviceInfo.getSourceFilename()),
            new DeviceVariantModel(parent, deviceInfo),
            new DevicePackageModel(parent, deviceInfo),
      };
      return models;
   }

   /**
    * Get variable map
    * 
    * @return Map of VariableKey -> Value
    */
   public Map<String, Variable> getVariableMap() {
      return fVariables;
   }
   
   /**
    * Creates a variable
    * 
    * @param key     Key used to identify variable
    * @param value   Value for variable
    * 
    * @throws Exception if variable already exists
    */
   public void addVariable(String key, Variable variable) {
      if (fVariables.put(key, variable) != null) {
         throw new RuntimeException("Variable already exists \'"+key+"\'");
      };
   }
   
   /**
    * Get value of variable
    * 
    * @param key  Key used to identify variable
    * 
    * @throws Exception if variable doesn't exist
    */
   public String getVariableValue(String key) throws Exception {
      return getVariable(key).getValueAsString();
   }

   /**
    * Get variable
    * 
    * @param key  Key used to identify variable
    * 
    * @throws Exception if variable doesn't exist
    */
   public Variable getVariable(String key) throws Exception {
      Variable variable = fVariables.get(key);
      if (variable == null) {
         throw new Exception("Variable does not exist for key \'"+key+"\'");
      }
      return variable;
   }

   /**
    * Set value of variable
    * 
    * @param key  Key used to identify variable
    * 
    * @throws Exception if variable doesn't exist
    */
   public void setVariableValue(String key, String value) {
      Variable variable = fVariables.get(key);
      if (variable == null) {
         System.err.println(String.format("setVariableValue(k=%s, v=%s): Variable not found", key, value));
         throw new RuntimeException(String.format("setVariableValue(k=%s, v=%s): Variable not found", key, value));
      }
      setDirty(variable.setValue(value));
   }

   /**
    * Get Simple map of variables
    * 
    * @return Map of Variables
    */
   public Map<String, String> getSimpleSymbolMap() {
      HashMap<String, String>map = new HashMap<String, String>();
      for (String key:fVariables.keySet()) {
         map.put(key, fVariables.get(key).getSubstitutionValue());
      }
      return map;
   }
}
