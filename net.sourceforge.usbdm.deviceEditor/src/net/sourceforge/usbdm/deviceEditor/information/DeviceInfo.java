package net.sourceforge.usbdm.deviceEditor.information;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.DialogSettings;

import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForAnalogueIO;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForCmp;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForDigitalIO;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForDmaMux;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForI2c;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForI2s;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLlwu;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLptmr;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLpuart;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForMisc;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForNull;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPit;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPwmIO_FTM;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForPwmIO_TPM;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForSpi;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForTsi;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForUart;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForVref;

public class DeviceInfo extends ObservableModel {

   /** Version number */
   public static final String VERSION           = "1.2.0";

   /** DTD file to reference in XML */
   public static final String DTD_FILE          = "Pins.dtd";

   /** Name space for C files */
   public static final String NAME_SPACE        = "USBDM";

   /** How to handle existing files etc */
   public enum Mode {ignore, fail};

   /** Device families */
   public enum DeviceFamily {mk, mke, mkl, mkm};

   /** Path of source file containing device description */
   private final Path fSourcePath;

   /** Name of device configuration file e.g. MK22FA12_64p.hardware */
   private String fDeviceFilename;

   /** Name of project file e.g. MK22FA12_64p.UsbdmProject */
   private String fProjectFilename;

   /** Family name of device */
   private String fFamilyName = null;

   /** Device family for this device */
   private DeviceFamily fDeviceFamily = null;

   /** Indicates if the data has changed since being loaded */
   private boolean fIsDirty = false;;
   
   /**
    * Create device information
    * 
    * @param filePath   File name
    * @param deviceName       Device name
    */
   public DeviceInfo(Path filePath) {

      fSourcePath            = filePath.toAbsolutePath();
      fDeviceFilename  = fSourcePath.getFileName().toString();
      fProjectFilename = fDeviceFilename.replace(".hardware", ".UsbdmProject");
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
         else if (fDeviceFilename.startsWith("MKL")) {
            fDeviceFamily = DeviceFamily.mkl;
         }
         else if (fDeviceFilename.startsWith("MKM")) {
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
    * Get source file name
    * @return
    */
   public Path getSourcePath() {
      return fSourcePath;
   }

   /**
    * Get source file name
    * @return
    */
   public String getSourceFilename() {
      return fSourcePath.getFileName().toString();
   }

   /*
    * Peripherals =============================================================================================
    */
   /**
    * Map of all Peripherals
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
   public Peripheral createPeripheral(String baseName, String instance, PeripheralTemplateInformation template, Mode mode) {  
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
//         System.err.println(String.format("Creating peripheral: %s(%s,%s)", className, baseName, instance));
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
    * e.g. findPeripheralFunction("FTM0") => <i>Peripheral</i>(FTM, 0)<br>
    * Checks against all templates.
    * 
    * @return Peripheral if found or name matches an expected pattern
    * 
    * @throws Exception if name does fit expected form
    */
   public Peripheral findOrCreatePeripheral(String name) {
      Peripheral peripheral = fPeripheralsMap.get(name);
      if (peripheral != null) {
         return peripheral;
      }
      for(PeripheralTemplateInformation template:getFunctionTemplateList()) {
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
    * Get map of all peripherals
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
    * PeripheralFunction =============================================================================================
    */
   /**
    * Map of all peripheral functions created<br>
    * May be searched by key derived from function name
    */
   private Map<String, Signal> fPeripheralFunctions = new TreeMap<String, Signal>(Signal.comparator);

   /**
    * Map of peripheral functions associated with a baseName<br>
    * May be searched by baseName string
    */
   private Map<String, Map<String, Signal>> fPeripheralFunctionsByBaseName = 
         new TreeMap<String, Map<String, Signal>>();

   /**
    * Get map of all peripheral functions
    * 
    * @return map 
    */
   public Map<String, Signal> getPeripheralFunctions() {
      return fPeripheralFunctions;
   }

   /**
    * Get map of peripheral functions associated with the given baseName<br>
    * e.g. "FTM" with return all the FTM peripheral functions
    * 
    * @param baseName Base name to search for e.g. FTM, ADC etc
    * 
    * @return  Map or null if none exists for baseName
    */
   public Map<String, Signal> getPeripheralFunctionsByBaseName(String baseName) {
      return fPeripheralFunctionsByBaseName.get(baseName);
   }

   /**
    * Create peripheral function
    * e.g. createPeripheralFunction(FTM,0,6) = <i>PeripheralFunction</i>(FTM, 0, 6)
    * 
    * @param name          e.g. FTM0_CH6
    * @param baseName      e.g. FTM0_CH6 = FTM
    * @param instance      e.g. FTM0_CH6 = 0
    * @param signal        e.g. FTM0_CH6 = 6
    *                      
    * @return Peripheral function if found or created, null otherwise
    * @throws Exception 
    */
   public Signal createPeripheralFunction(String name, String baseName, String instance, String signal) {

      Signal peripheralFunction = fPeripheralFunctions.get(name);
      if (peripheralFunction != null) {
         throw new RuntimeException("PeripheralFunction already exists "+ name);
      }

      Peripheral peripheral = findPeripheral(baseName, instance);
      peripheralFunction = new Signal(name, peripheral, signal);

      // Add to base name map
      Map<String, Signal> map = fPeripheralFunctionsByBaseName.get(baseName);
      if (map == null) {
         map = new TreeMap<String, Signal>();
         fPeripheralFunctionsByBaseName.put(baseName, map);
      }
      map.put(baseName, peripheralFunction);

      // Add to map
      fPeripheralFunctions.put(name, peripheralFunction);
      peripheral.addFunction(peripheralFunction);

      return peripheralFunction;
   }

   /**
    * Find or Create peripheral function<br>
    * e.g. findPeripheralFunction("FTM0_CH6") => <i>PeripheralFunction</i>(FTM, 0, 6)<br>
    * Checks against all templates.
    * 
    * @return Function if found or matches an expected pattern
    * 
    * @throws Exception if function does fit expected form
    */
   public Signal findOrCreatePeripheralFunction(String name) {
      Signal peripheralFunction = null;
      if (name.equalsIgnoreCase("Disabled")) {
         return Signal.DISABLED_SIGNAL;
      }
      peripheralFunction = fPeripheralFunctions.get(name);
      if (peripheralFunction != null) {
         return peripheralFunction;
      }
      for(PeripheralTemplateInformation functionTemplateInformation:getFunctionTemplateList()) {
         peripheralFunction = functionTemplateInformation.createFunction(name);
         if (peripheralFunction != null) {
            peripheralFunction.setIncluded(true);
//            peripheralFunction.setTemplate(functionTemplateInformation);
            return peripheralFunction;
         }         
      }
      throw new RuntimeException("Failed to find pattern that matched peripheral function: \'" + name + "\'");
   }

   /**
    * Find peripheral function<br>
    * e.g. findPeripheralFunction("FTM0_CH6") => <i>PeripheralFunction</i>(FTM, 0, 6)<br>
    * 
    * @return Function found
    * 
    * @throws Exception function nor found
    */
   public Signal findPeripheralFunction(String name) {
      Signal peripheralFunction = null;
      if (name.equalsIgnoreCase("Disabled")) {
         return Signal.DISABLED_SIGNAL;
      }
      peripheralFunction = fPeripheralFunctions.get(name);
      if (peripheralFunction != null) {
         return peripheralFunction;
      }
      throw new RuntimeException("Failed to find pattern that matched peripheral function: \'" + name + "\'");
   }

   /**
    * A string listing all peripheral functions
    *  
    * @return
    */
   public String listPeripheralFunctions() {
      StringBuffer buff = new StringBuffer();
      buff.append("(");
      for (String f:fPeripheralFunctions.keySet()) {
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
    * Map from Function to list of Pins
    */
   private Map<Signal, ArrayList<MappingInfo>> fPeripheralFunctionMap = new TreeMap<Signal, ArrayList<MappingInfo>>();

   /**
    * Add info to map by function
    * 
    * @param info
    */
   void addToFunctionMap(Signal function, MappingInfo info) {
      //      System.err.println(String.format("addToFunctionMap() - F:%s, info:%s", function.toString(), info.toString()));
      ArrayList<MappingInfo> list = fPeripheralFunctionMap.get(function);
      if (list == null) {
         list = new ArrayList<MappingInfo>();
         fPeripheralFunctionMap.put(function, list);
      }
      list.add(info);
   }

   /**
    * Get list of pin mappings associated with given function
    * 
    * @param function 
    * 
    * @return
    */
   public ArrayList<MappingInfo> getPins(Signal function) {
      return fPeripheralFunctionMap.get(function);
   }

   /**
    * Create new Pin mapping<br>
    * 
    * Mapping is added to pin map<br>
    * Mapping is added to function map<br>
    * 
    * @param function            Function signal being mapped e.g. I2C2_SCL
    * @param pinInformation      Pin being mapped e.g. PTA (pin name not signal!)
    * @param functionSelector    Multiplexor setting that maps this signal to the pin
    * @return
    */
   public MappingInfo createMapping(Signal function, Pin pinInformation, MuxSelection functionSelector) {
      
      MappingInfo mapInfo= pinInformation.addSignal(function, functionSelector);
      addToFunctionMap(function, mapInfo);
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
      if (name.equalsIgnoreCase(Pin.DISABLED_PIN.getName())) {
         return Pin.DISABLED_PIN;
      }
      return fPins.get(name);
   }
   /*
    * PeripheralTemplateInformation =============================================================================================
    */
   /**
    * List of all templates
    */
   private ArrayList<PeripheralTemplateInformation> fTemplateList = new ArrayList<PeripheralTemplateInformation>();

   /**
    * Get list of all templates
    * 
    * @return
    */
   public ArrayList<PeripheralTemplateInformation> getFunctionTemplateList() {
      return fTemplateList;
   }

   /**
    * Gets template that matches this function
    * 
    * @param function   Function to match
    * 
    * @return Matching template or null on none
    */
   public PeripheralTemplateInformation getTemplate(Signal function) {
      for (PeripheralTemplateInformation functionTemplateInformation:fTemplateList) {
         if (functionTemplateInformation.getMatchPattern().matcher(function.getName()).matches()) {
            return functionTemplateInformation;
         }
      }
      return null;
   }

   /**
    * 
    * @param namePattern            Pattern to extract peripheral base name e.g. FTM2 => Ftm
    * @param instancePattern        Pattern to extract instance e.g. FTM2 => "2"
    * @param matchTemplate          Pattern to select use of this template e.g. "FTM\\d+_CH\\d+"
    * @param deviceFamily           Device family
    * @param instanceWriter         InstanceWriter to use
    */
   private PeripheralTemplateInformation createPeripheralTemplateInformation(
         String        namePattern,   
         String        instancePattern, 
         String        matchTemplate, 
         DeviceFamily  deviceFamily, 
         Class<?>      instanceWriterClass) {
      return createPeripheralTemplateInformation(namePattern, instancePattern, "$3", matchTemplate, deviceFamily, instanceWriterClass);
   }

   /**
    * 
    * @param namePattern            Pattern to extract peripheral base name e.g. FTM2 => Ftm
    * @param instancePattern        Pattern to extract instance e.g. FTM2 => "2"
    * @param matchTemplate          Pattern to select use of this template e.g. "FTM\\d+_CH\\d+"
    * @param deviceFamily           Device family
    * @param instanceWriter         InstanceWriter to use
    */
   private PeripheralTemplateInformation createPeripheralTemplateInformation(
         String        namePattern,   
         String        instancePattern, 
         String        signalPattern, 
         String        matchTemplate, 
         DeviceFamily  deviceFamily, 
         Class<?>      instanceWriterClass) {

      PeripheralTemplateInformation template = null; 

      try {
         template = new PeripheralTemplateInformation(this, deviceFamily, namePattern, signalPattern, instancePattern, matchTemplate, instanceWriterClass);
         fTemplateList.add(template);
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
            WriterForDigitalIO.class);
      if (getDeviceFamily() != DeviceFamily.mkm) {
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(ADC)([0-3])_((SE|DM|DP)\\d+(a|b)?)",
               getDeviceFamily(),
               WriterForAnalogueIO.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(CMP)([0-3])?_(.*)",
               getDeviceFamily(),
               WriterForCmp.class);
         createPeripheralTemplateInformation(
               "DmaMux", "0",  
               null,
               getDeviceFamily(),
               WriterForDmaMux.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(FTM)([0-3])_(CH\\d+|QD_PH[A|B]|FLT\\d|CLKIN\\d)",
               getDeviceFamily(),
               WriterForPwmIO_FTM.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(I2C)([0-3])_(SCL|SDA|4WSCLOUT|4WSDAOUT)",
               getDeviceFamily(),
               WriterForI2c.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(I2S)([0-3])_(MCLK|(RXD(\\d+))|RX_BCLK|RX_FS|(TXD(\\d+))|TX_BCLK|TX_FS|xxx|(RW(_b)?)|(TS(_b)?)|(AD\\d+))",
               getDeviceFamily(),
               WriterForI2s.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(LPTMR)(0)_(ALT\\d+)",
               getDeviceFamily(),
               WriterForLptmr.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(PIT)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForPit.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(LLWU)()_(P\\d+)",
               getDeviceFamily(),
               WriterForLlwu.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(SPI)([0-3])_(SCK|SIN|SOUT|MISO|MOSI|SS|PCS\\d*)",
               getDeviceFamily(),
               WriterForSpi.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(TPM)([0-3])_(CH\\d+|QD_PH[A|B]|CLKIN\\d)",
               getDeviceFamily(),
               WriterForPwmIO_TPM.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(TSI)([0-3])_(CH\\d+)",
               getDeviceFamily(),
               WriterForTsi.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(UART)(\\d+)_(TX|RX|CTS_b|RTS_b|COL_b)",
               getDeviceFamily(),
               WriterForUart.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(LPUART)([0-6])_(TX|RX|CTS_b|RTS_b)",
               getDeviceFamily(),
               WriterForLpuart.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(VREF)()_(OUT)",
               getDeviceFamily(),
               WriterForVref.class);
         createPeripheralTemplateInformation(
               "FB", "", "",
               "(FB|FLEXBUS|FXIO|FLEXIO).*",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(CAN)([0-3])_(RX|TX)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(CAN)([0-5])_(RX|TX)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "", "$3",
               "(RTC)_?()(CLKOUT|CLKIN|WAKEUP_b)?",
               getDeviceFamily(),
               WriterForNull.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(JTAG|EZP|SWD|CLKOUT|NMI_b|RESET_b|TRACE|VOUT33|VREGIN|EXTRG)_?()(.*)",
               getDeviceFamily(),
               WriterForNull.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(EWM)_()(IN|OUT_b)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "OSC$2", "", "$1",
               "(E?XTAL)(0|32)()",
               getDeviceFamily(),
               WriterForNull.class);
         // Note USBOTG is used for clock name
         createPeripheralTemplateInformation(
               "USBDCD", "",
               "(USBDCD)_?(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         // Note USBOTG is used for clock name
         createPeripheralTemplateInformation(
               "USB", "0",
               "((audio)?(USB)(OTG)?0?)_?(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(DAC)(\\d+)?_(OUT)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(CMT0?)_()(IRO)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(PDB)(0?)_(EXTRG)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2",
               "(FTM)_()(CLKIN\\d+)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "", "",
               "(ENET)(0_)?(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "",
               "(SDHC)(\\d+)_(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "R?(MII)([0-3])_(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "", "$3",
               "S?(LCD)(\\d)?_?(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "", "$3",
               "(CRC)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(DMAMUX)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForDmaMux.class);
         createPeripheralTemplateInformation(
               "$1", "0", "$3",
               "(DMA)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(MPU)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "FTFE", "$2", "$3",
               "(FTF)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(RNGA)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
      }
      createPeripheralTemplateInformation(
            "$1", "", "$2",
            "(VDD|VSS)(\\d+)",
            getDeviceFamily(),
            WriterForMisc.class);
      createPeripheralTemplateInformation(
            "$1", "$2",
            "(.*)()()",
            getDeviceFamily(),
            WriterForNull.class);
   }

   /*
    * DeviceVariantInformation =============================================================================================
    */

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
    * @param dmaMux
    * @param dmaChannelNumber
    * @param dmaSource
    * @return
    */
   public DmaInfo createDmaInfo(String dmaMux, int dmaChannelNumber, String dmaSource) {
      Peripheral dmaPeripheral = findOrCreatePeripheral(dmaMux);
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
      // Every peripheral function should have a reset entry implied by the pin information
      // except for functions with fixed pin mapping
      for (String pName:getPeripheralFunctions().keySet()) {
         Signal function = getPeripheralFunctions().get(pName);
         if ((function.getResetMapping() == null) &&
               (function.getPinMapping().first().getMux() != MuxSelection.fixed)) {
            throw new RuntimeException("No reset value for function " + function);
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

   /** Key for device variant persistence */
   public static final String DEVICE_VARIANT_SETTINGS_KEY = "DeviceInfo_deviceVariant"; 
   
   /**
    * Load persistent settings
    */
   public void loadSettings() {
      System.err.println("DeviceInfo.loadSettings("+fProjectFilename+")");
      Path path = fSourcePath.getParent().resolve(fProjectFilename);
      if (path.toFile().isFile()) {
         try {
         DialogSettings settings = new DialogSettings("USBDM");
         settings.load(path.toAbsolutePath().toString());
         String variantName = settings.get(DEVICE_VARIANT_SETTINGS_KEY);
         if (variantName != null) {
            setDeviceVariant(variantName);
         }
         for (String pinName:fPins.keySet()) {
            Pin pin = fPins.get(pinName);
            pin.loadSettings(settings);
         }
         for (String deviceName:fPeripheralsMap.keySet()) {
            Peripheral peripheral =  fPeripheralsMap.get(deviceName);
            peripheral.loadSettings(settings);
         }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      setDirty(false);
   }
   
   /**
    * Save persistent settings
    */
   public void saveSettings() {
      System.err.println("DeviceInfo.saveSettings("+fProjectFilename+")");
      Path path = fSourcePath.getParent().resolve(fProjectFilename);
      DialogSettings settings = new DialogSettings("USBDM");
      settings.put(DEVICE_VARIANT_SETTINGS_KEY, fVariantName);
      for (String pinName:fPins.keySet()) {
         Pin pin = fPins.get(pinName);
         pin.saveSettings(settings);
      }
      for (String deviceName:fPeripheralsMap.keySet()) {
         Peripheral peripheral =  fPeripheralsMap.get(deviceName);
         peripheral.saveSettings(settings);
      }
      try {
         settings.save(path.toAbsolutePath().toString());
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

}
