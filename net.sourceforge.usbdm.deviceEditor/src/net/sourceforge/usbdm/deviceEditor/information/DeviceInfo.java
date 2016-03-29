package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.usbdm.deviceEditor.parser.WriterForAnalogueIO;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForCmp;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForDigitalIO;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForDmaMux;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForI2c;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForLlwu;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForLptmr;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForLpuart;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForMisc;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForPit;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForPwmIO_FTM;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForPwmIO_TPM;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForSpi;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForTsi;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForUart;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForVref;

public class DeviceInfo {

   /** Version number */
   public static final String VERSION           = "1.2.0";
   
   /** DTD file to reference in XML */
   public static final String DTD_FILE          = "Pins.dtd";
   
   /** Name space for C files */
   public static final String NAME_SPACE        = "USBDM";
   
   /** MACRO guard for name space */
   public static final String NAMESPACES_GUARD  = "USE_USBDM_NAMESPACE";
   
   /** How to handle existing files etc */
   enum Mode {newInstance, anyInstance, allowNullInstance};
   /** Device families */
   public enum DeviceFamily {mk, mke, mkl, mkm};
   
   /** Name of the device e.g. MKL25Z4 */
   private final String       fDeviceName;
   
   /** Source file containing device description */
   private final String       fSourceFilename;
   
   /** Device family for this device */
   private final DeviceFamily fDeviceFamily;

   /** Device family for this device */
   public DeviceFamily getDeviceFamily() {
      return fDeviceFamily;
   }

   /**
    * Create device information
    * 
    * @param sourceFilename   File name
    * @param deviceName       Device name
    */
   public DeviceInfo(String sourceFilename, String deviceName) {

      this.fSourceFilename = sourceFilename;
      this.fDeviceName     = deviceName;

      if (deviceName.startsWith("MKE")) {
         fDeviceFamily = DeviceFamily.mke;
      }
      else if (deviceName.startsWith("MKL")) {
         fDeviceFamily = DeviceFamily.mkl;
      }
      else if (deviceName.startsWith("MKM")) {
         fDeviceFamily = DeviceFamily.mkm;
      }
      else {
         fDeviceFamily = DeviceFamily.mk;
      }
   }

   /**
    * Get device name
    * 
    * @return
    */
   public String getDeviceName() {
      return fDeviceName;
   }

   /**
    * Get source file name
    * @return
    */
   public String getSourceFilename() {
      return fSourceFilename;
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
    * @param description 
    * 
    * @return
    */
   public Peripheral createPeripheral(String baseName, String instance, String description) {  
      String name = baseName+instance;
      Peripheral p = fPeripheralsMap.get(name);
      if (p!= null) {
         throw new RuntimeException("Attempting to re-create peripheral instance " + name);
      }
      p = new Peripheral(baseName, instance, description);
      fPeripheralsMap.put(name, p);
      return p;
   }

   /**
    * Get existing peripheral
    * 
    * @param baseName   Base name e.g. FTM3 => FTM
    * @param instance   Instance of peripheral e.g. FTM2 => 2 
    * @param mode       Mode.anyInstance to allow null instances  
    * 
    * @return
    */
   public Peripheral findOrCreatePeripheral(String baseName, String instance, String description) {  
      Peripheral p = fPeripheralsMap.get(baseName+instance);
      if (p == null) {
         p = createPeripheral(baseName, instance, description);
      }
      return p;
   }

   /**
    * Get existing peripheral
    * 
    * @param name   Name e.g. FTM3
    * 
    * @return
    */
   public Peripheral findPeripheral(String name) {  
      Peripheral p = fPeripheralsMap.get(name);
      if (p == null) {
         throw new RuntimeException("No such instance " + name);
      }
      return p;
   }

   /**
    * Get existing peripheral
    * 
    * @param name   Name e.g. FTM3
    * 
    * @return
    */
   public Peripheral findPeripheral(String baseName, String instance) {  
      Peripheral p = fPeripheralsMap.get(baseName+instance);
      if (p == null) {
         throw new RuntimeException("No such instance " + baseName+instance);
      }
      return p;
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
   private Map<String, PeripheralFunction> fPeripheralFunctions = new TreeMap<String, PeripheralFunction>(PeripheralFunction.comparator);

   /**
    * Map of peripheral functions associated with a baseName<br>
    * May be searched by baseName string
    */
   private Map<String, Map<String, PeripheralFunction>> fPeripheralFunctionsByBaseName = 
         new TreeMap<String, Map<String, PeripheralFunction>>();

   /**
    * Get map of all peripheral functions
    * 
    * @return map 
    */
   public Map<String, PeripheralFunction> getPeripheralFunctions() {
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
   public Map<String, PeripheralFunction> getPeripheralFunctionsByBaseName(String baseName) {
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
   public PeripheralFunction createPeripheralFunction(String name, String baseName, String instance, String signal) {

      PeripheralFunction peripheralFunction = fPeripheralFunctions.get(name);
      if (peripheralFunction != null) {
         throw new RuntimeException("PeripheralFunction already exists "+ name);
      }

      Peripheral peripheral = findPeripheral(baseName, instance);
      peripheralFunction = new PeripheralFunction(name, peripheral, signal);

      // Add to base name map
      Map<String, PeripheralFunction> map = fPeripheralFunctionsByBaseName.get(baseName);
      if (map == null) {
         map = new TreeMap<String, PeripheralFunction>();
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
   public PeripheralFunction findOrCreatePeripheralFunction(String name) {
      PeripheralFunction peripheralFunction = null;
      if (name.equalsIgnoreCase("Disabled")) {
         return PeripheralFunction.DISABLED;
      }
      peripheralFunction = fPeripheralFunctions.get(name);
      if (peripheralFunction != null) {
         return peripheralFunction;
      }
      for(PeripheralTemplateInformation functionTemplateInformation:getTemplateList()) {
         peripheralFunction = functionTemplateInformation.appliesTo(this, name);
         if (peripheralFunction != null) {
            peripheralFunction.setIncluded(true);
            peripheralFunction.setTemplate(functionTemplateInformation);
            return peripheralFunction;
         }         
      }
      throw new RuntimeException("Failed to find pattern that matched peripheral function: \'" + name + "\'");
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
   public PeripheralFunction findPeripheralFunction(String name) {
      PeripheralFunction peripheralFunction = null;
      if (name.equalsIgnoreCase("Disabled")) {
         return PeripheralFunction.DISABLED;
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
   private Map<PeripheralFunction, ArrayList<MappingInfo>> fPeripheralFunctionMap = new TreeMap<PeripheralFunction, ArrayList<MappingInfo>>();

   /** 
    * Map from base names to Map of pins having that facility 
    */
   private Map<String, HashSet<PinInformation>> fFunctionsByBaseName = new TreeMap<String, HashSet<PinInformation>>();

   /**
    * Add info to map by function
    * 
    * @param info
    */
   void addToFunctionMap(PeripheralFunction function, MappingInfo info) {
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
   public ArrayList<MappingInfo> getPins(PeripheralFunction function) {
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
   public MappingInfo createMapping(PeripheralFunction function, PinInformation pinInformation, MuxSelection functionSelector) {
      Map<MuxSelection, MappingInfo> mappedFunctions = pinInformation.getMappedFunctions();
      MappingInfo mapInfo = mappedFunctions.get(functionSelector);
      if (mapInfo == null) {
         // Create new mapping
         mapInfo = new MappingInfo(pinInformation, functionSelector);
         mappedFunctions.put(functionSelector, mapInfo);
      }
      mapInfo.getFunctions().add(function);
      addToFunctionMap(function, mapInfo);
      if (function.getName().startsWith("ADC0_SE4b")) {
         // XXX Delete me
         System.err.println("Stop here");
      }
      function.addMapping(mapInfo);
      return mapInfo;
   }

   /**
    * Get map of functions for given base name
    * 
    * @param baseName
    * @return
    */
   public HashSet<PinInformation> getFunctionType(String baseName) {
      return fFunctionsByBaseName.get(baseName);
   }

   /**
    * Add pin to 
    * 
    * @param baseName
    * @param pinInfo
    */
   public void addFunctionType(String baseName, PinInformation pinInfo) {
      // Record pin as having this function
      HashSet<PinInformation> set = fFunctionsByBaseName.get(baseName);
      if (set == null) {
         set = new HashSet<PinInformation>();
      }
      set.add(pinInfo);
   }

   /*
    * PinInformation =============================================================================================
    */
   /**
    * Map of all pins created<br>
    * May be searched by pin name
    */
   private Map<String, PinInformation> fPins = new TreeMap<String, PinInformation>(PinInformation.comparator);

   /**
    * Get Map of all pins
    * 
    * @return
    */
   public Map<String, PinInformation> getPins() {
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
   public PinInformation createPin(String name) {
      // Check for repeated pin
      PinInformation pinInformation = fPins.get(name);
      if (pinInformation != null) {
         throw new RuntimeException("Pin already exists: " + name);
      }
      // Created pin
      pinInformation = new PinInformation(name);
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
   public PinInformation findPin(String name) {
      if (name.equalsIgnoreCase(PinInformation.DISABLED_PIN.getName())) {
         return PinInformation.DISABLED_PIN;
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
   public ArrayList<PeripheralTemplateInformation> getTemplateList() {
      return fTemplateList;
   }

   /**
    * Gets template that matches this function
    * 
    * @param function   Function to match
    * 
    * @return Matching template or null on none
    */
   public PeripheralTemplateInformation getTemplate(PeripheralFunction function) {
      for (PeripheralTemplateInformation functionTemplateInformation:fTemplateList) {
         if (functionTemplateInformation.getMatchPattern().matcher(function.getName()).matches()) {
            return functionTemplateInformation;
         }
      }
      return null;
   }

   /**
    * 
    * @param className              Base name of C peripheral class e.g. FTM2 => Ftm
    * @param instance               Instance e.g. FTM2 => "2"
    * @param matchTemplate          Pattern to select use of this template e.g. "FTM\\d+_CH\\d+"
    * @param deviceFamily           Device family
    * @param instanceWriter         InstanceWriter to use
    */
   private PeripheralTemplateInformation createPeripheralTemplateInformation(
         String        classBasename,   
         String        instance, 
         String        matchTemplate, 
         DeviceFamily  deviceFamily, 
         Class<?>      instanceWriterClass) {

      PeripheralTemplateInformation template = null; 

      try {
         template = new PeripheralTemplateInformation(
               this, deviceFamily, classBasename, classBasename.toUpperCase(), instance, matchTemplate, instanceWriterClass);
         fTemplateList.add(template);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
      return template;
   }

   /**
    * 
    * @param classBasename          Base name of C peripheral class e.g. FTM2 => Ftm
    * @param peripheralBasename     Base name of peripheral e.g. FTM2 => FTM
    * @param instance               Instance e.g. FTM2 => "2"
    * @param matchTemplate          Pattern to select use of this template e.g. "FTM\\d+_CH\\d+"
    * @param deviceFamily           Device family
    * @param instanceWriter         InstanceWriter to use
    */
   public PeripheralTemplateInformation createPeripheralTemplateInformation(
         String         classBasename,   
         String         peripheralBasename,   
         String         instance, 
         String         matchTemplate, 
         DeviceFamily   deviceFamily, 
         Class<?>       instanceWriterClass) throws Exception {

      PeripheralTemplateInformation template = null;
      try {
         template = 
               new PeripheralTemplateInformation(
                     this, deviceFamily, classBasename, peripheralBasename, instance, matchTemplate, instanceWriterClass);
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
      for (char instance='A'; instance<='I'; instance++) {
         createPeripheralTemplateInformation(
               "Gpio", "PORT", Character.toString(instance),
               "^\\s*(GPIO)("+instance+")_(\\d+)\\s*$",
               getDeviceFamily(),
               WriterForDigitalIO.class);
      }
      //          for (char suffix='A'; suffix<='I'; suffix++) {
      //             new createPeripheralTemplateInformation(
      //                   "Gpio", "PORT", "Port_Group",  "Port Definitions",               
      //                   "Information required to manipulate PORT PCRs & associated GPIOs", 
      //                   null,
      //                   new WriterForPort(deviceIsMKE));
      //          }

      if (getDeviceFamily() != DeviceFamily.mkm) {
         for (char instance='0'; instance<='3'; instance++) {
            createPeripheralTemplateInformation(
                  "Adc", Character.toString(instance),
                  "(ADC)("+instance+")_((SE|DM|DP)\\d+(a|b)?)",
                  getDeviceFamily(),
                  WriterForAnalogueIO.class);
         }
         for (char instance='0'; instance<='3'; instance++) {
            createPeripheralTemplateInformation(
                  "Cmp", Character.toString(instance),
                  "(CMP)("+instance+")_((IN\\d)|(OUT))",
                  getDeviceFamily(),
                  WriterForCmp.class);
         }
         createPeripheralTemplateInformation(
               "DmaMux", "0",  
               null,
               getDeviceFamily(),
               WriterForDmaMux.class);
         for (char instance='0'; instance<='3'; instance++) {
            createPeripheralTemplateInformation(
                  "Ftm", Character.toString(instance),
                  "(FTM)("+instance+")_(CH\\d+|QD_PH[A|B]|FLT\\d|CLKIN\\d)",
                  getDeviceFamily(),
                  WriterForPwmIO_FTM.class);
         }
         for (char instance='0'; instance<='3'; instance++) {
            createPeripheralTemplateInformation(
                  "I2c", Character.toString(instance),
                  "(I2C)("+instance+")_(SCL|SDA|4WSCLOUT|4WSDAOUT)",
                  getDeviceFamily(),
                  WriterForI2c.class);
         }
         createPeripheralTemplateInformation(
               "Lptmr", "0",
               "(LPTMR)(0)_(ALT\\d+)",
               getDeviceFamily(),
               WriterForLptmr.class);
         createPeripheralTemplateInformation(
               "Pit", "",
               "(PIT)()(\\d+)",
               getDeviceFamily(),
               WriterForPit.class);
         createPeripheralTemplateInformation(
               "Llwu", "",
               "(LLWU)()_(P\\d+)",
               getDeviceFamily(),
               WriterForLlwu.class);
         for (char instance='0'; instance<='3'; instance++) {
            createPeripheralTemplateInformation(
                  "Spi", Character.toString(instance),
                  "(SPI)("+instance+")_(SCK|SIN|SOUT|MISO|MOSI|SS|PCS\\d*)",
                  getDeviceFamily(),
                  WriterForSpi.class);
         }
         for (char instance='0'; instance<='3'; instance++) {
            createPeripheralTemplateInformation(
                  "Tpm", Character.toString(instance),
                  "(TPM)("+instance+")_(CH\\d+|QD_PH[A|B])",
                  getDeviceFamily(),
                  WriterForPwmIO_TPM.class);
         }
         for (char instance='0'; instance<='3'; instance++) {
            createPeripheralTemplateInformation(
                  "Tsi", Character.toString(instance),
                  "(TSI)("+instance+")_(CH\\d+)",
                  getDeviceFamily(),
                  WriterForTsi.class);
         }
         for (char instance='0'; instance<='5'; instance++) {
            createPeripheralTemplateInformation(
                  "Uart", Character.toString(instance),
                  "(UART)("+instance+")_(TX|RX|CTS_b|RTS_b|COL_b)",
                  getDeviceFamily(),
                  WriterForUart.class);
         }
         for (char instance='0'; instance<='5'; instance++) {
            createPeripheralTemplateInformation(
                  "Lpuart", Character.toString(instance),
                  "(LPUART)("+instance+")_(TX|RX|CTS_b|RTS_b)",
                  getDeviceFamily(),
                  WriterForLpuart.class);
         }
         createPeripheralTemplateInformation(
               "Vref", "",
               "(VREF)()_(OUT)",
               getDeviceFamily(),
               WriterForVref.class);
         createPeripheralTemplateInformation(
               "Fb", "",
               "(FB)_()(ALE|(CS(\\d+)(_b)?)|(OE(_b)?)|(RW(_b)?)|(TS(_b)?)|(AD\\d+))",
               getDeviceFamily(),
               WriterForMisc.class);
         for (char instance='0'; instance<='2'; instance++) {
            createPeripheralTemplateInformation(
                  "I2s", Character.toString(instance),
                  "(I2S)("+Character.toString(instance)+")_(MCLK|(RXD(\\d+))|RX_BCLK|RX_FS||(TXD(\\d+))|TX_BCLK|TX_FS|xxx|(RW(_b)?)|(TS(_b)?)|(AD\\d+))",
                  getDeviceFamily(),
                  WriterForMisc.class);
         }
         for (char instance='0'; instance<='2'; instance++) {
            createPeripheralTemplateInformation(
                  "Can", Character.toString(instance),
                  "(CAN)("+Character.toString(instance)+")_(RX|TX)",
                  getDeviceFamily(),
                  WriterForMisc.class);
         }
         createPeripheralTemplateInformation(
               "System", "",
               "(JTAG|EZP|SWD|CLKOUT|NMI_b|RESET_b|TRACE|VOUT33|VREGIN|RTC)_?()(TCLK|TDI|TDO|TMS|TRST_b|CLK|CS_b|DI|DO|DIO|CLKOUT|D3|SWO)?",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "Ewm", "",
               "(EWM)_()(IN|OUT_b)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "Xtal", "",
               "(E?XTAL)()(0|32)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "Usb", "",
               "(USB0?)_()(DM|DP|CLKIN|SOF_OUT)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "Dac", "",
               "(DAC0?)_()(OUT)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "Cmt", "",
               "(CMT0?)_()(IRO)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "Pdb", "",
               "(PDB0?)_()(EXTRG)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "Ftm", "",
               "(FTM)_()(CLKIN\\d+)",
               getDeviceFamily(),
               WriterForMisc.class);
      }
      createPeripheralTemplateInformation(
            "Misc", "",
            "(.*)()()",
            getDeviceFamily(),
            WriterForMisc.class);
   }

   /*
    * DeviceInformation =============================================================================================
    */
   
   /** Devices */
   private Map<String, DeviceInformation> fDevices = new TreeMap<String, DeviceInformation>();

   /**
    * Create device infomation
    * 
    * @param name
    * @param manual
    * @param packageName
    * @return
    */
   public DeviceInformation createDeviceInformation(String name, String manual, String packageName) {
      DeviceInformation deviceInformation = new DeviceInformation(name, findOrCreateDevicePackage(packageName), manual);
      fDevices.put(name, deviceInformation);
      return deviceInformation;
   };

   /**
    * Get map of devices
    * 
    * @return
    */
   public Map<String, DeviceInformation> getDevices() {
      return fDevices;
   }

   /**
    * Find device from device name
    * 
    * @param deviceName
    * @return
    */
   public DeviceInformation findDevice(String deviceName) {
      return fDevices.get(deviceName);
   }

   /*
    * DmaInfo =============================================================================================
    */
   
   /** List of DMA channels */
   private ArrayList<DmaInfo> fDmaInfoList = new ArrayList<DmaInfo>();

   /**
    * Create DMA information entry
    * 
    * @param dmaInstance
    * @param dmaChannelNumber
    * @param dmaSource
    * @return
    */
   public DmaInfo createDmaInfo(int dmaInstance, int dmaChannelNumber, String dmaSource) {
      DmaInfo dmaInfo = new DmaInfo(dmaInstance, dmaChannelNumber, dmaSource);
      fDmaInfoList.add(dmaInfo);
      return dmaInfo;
   }

   /**
    * Get list of DMA entries
    *  
    * @return
    */
   public ArrayList<DmaInfo> getDmaList() {
      return fDmaInfoList;
   }

   /**
    * Does some basic consistency checks on the data
    */
   public void consistencyCheck() {
      // Every pin should have a reset entry
      for (String pName:getPins().keySet()) {
         PinInformation pin = getPins().get(pName);
         if ((pin.getResetValue() != MuxSelection.mux0)) {//) && (pin.getResetValue() != MuxSelection.fixed)) {
            // Unusual mapping - report
            System.err.println("Note: Pin "+pin.getName()+" reset mapping is non-zero = "+pin.getResetValue());
         }
      }
      // Every peripheral function should have a reset entry implied by the pin information
      // except for functions with fixed pin mapping
      for (String pName:getPeripheralFunctions().keySet()) {
         PeripheralFunction function = getPeripheralFunctions().get(pName);
         if ((function.getResetMapping() == null) &&
             (function.getPinMapping().first().getMux() != MuxSelection.fixed)) {
            throw new RuntimeException("No reset value for function " + function);
         }
      }

   }

}
