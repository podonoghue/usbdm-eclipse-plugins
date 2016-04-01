package net.sourceforge.usbdm.deviceEditor.information;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.usbdm.deviceEditor.parser.WriterBase;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForAnalogueIO;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForCmp;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForDigitalIO;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForDmaMux;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForI2c;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForI2s;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForLlwu;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForLptmr;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForLpuart;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForMisc;
import net.sourceforge.usbdm.deviceEditor.parser.WriterForNull;
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

   /** How to handle existing files etc */
   enum Mode {newInstance, anyInstance, allowNullInstance};
   /** Device families */
   public enum DeviceFamily {mk, mke, mkl, mkm};

   /** Name of the device e.g. MKL25Z4 */
   private final String       fDeviceName;

   /** Source file containing device description */
   private final Path       fPath;

   /** Device family for this device */
   private final DeviceFamily fDeviceFamily;

   /** Device family for this device */
   public DeviceFamily getDeviceFamily() {
      return fDeviceFamily;
   }

   /**
    * Create device information
    * 
    * @param filePath   File name
    * @param deviceName       Device name
    */
   public DeviceInfo(Path filePath, String deviceName) {

      this.fPath = filePath.toAbsolutePath();
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
   public Path getSourcePath() {
      return fPath;
   }

   /**
    * Get source file name
    * @return
    */
   public String getSourceFilename() {
      return fPath.getFileName().toString();
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
    * @param writerBase 
    * 
    * @return
    */
   public Peripheral createPeripheral(String baseName, String instance, WriterBase writerBase, PeripheralTemplateInformation template) {  
      String name = baseName+instance;
      Peripheral p = fPeripheralsMap.get(name);
      if (p!= null) {
         throw new RuntimeException("Attempting to re-create peripheral instance " + name);
      }
      p = new Peripheral(baseName, instance, template);
      fPeripheralsMap.put(name, p);
      return p;
   }

   /**
    * Find or create a peripheral
    * 
    * @param baseName   Base name e.g. FTM3 => FTM
    * @param instance   Instance of peripheral e.g. FTM2 => 2 
    * @param mode       Mode.anyInstance to allow null instances  
    * 
    * @return
    */
   public Peripheral findOrCreatePeripheral(String baseName, String instance, WriterBase writerBase, PeripheralTemplateInformation template) {  
      Peripheral p = fPeripheralsMap.get(baseName+instance);
      if (p == null) {
         p = createPeripheral(baseName, instance, writerBase, template);
      }
      return p;
   }

   /**
    * Find an existing peripheral
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
    * Find an existing peripheral
    * 
    * @param baseName   Base name e.g. FTM3 => FTM
    * @param instance   Name e.g. FTM3 => 3
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
      for(PeripheralTemplateInformation functionTemplateInformation:getFunctionTemplateList()) {
         peripheralFunction = functionTemplateInformation.createFunction(this, name);
         if (peripheralFunction != null) {
            peripheralFunction.setIncluded(true);
            peripheralFunction.setTemplate(functionTemplateInformation);
            return peripheralFunction;
         }         
      }
      throw new RuntimeException("Failed to find pattern that matched peripheral function: \'" + name + "\'");
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
      for(PeripheralTemplateInformation functionTemplateInformation:getFunctionTemplateList()) {
         peripheral = functionTemplateInformation.createPeripheral(this, name);
         if (peripheral != null) {
            return peripheral;
         }         
      }
      throw new RuntimeException("Failed to find pattern that matched peripheral: \'" + name + "\'");
   }

   /**
    * Find or Create peripheral function<br>
    * e.g. findPeripheralFunction("FTM0_CH6") => <i>PeripheralFunction</i>(FTM, 0, 6)<br>
    * Checks against all templates.
    * 
    * @return Function if found or name matches an expected pattern
    * 
    * @throws Exception if name does fit expected form
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
      function.addMapping(mapInfo);
      return mapInfo;
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

   //   /**
   //    * 
   //    * @param namePattern          Base name of C peripheral class e.g. FTM2 => Ftm
   //    * @param peripheralBasename     Base name of peripheral e.g. FTM2 => FTM
   //    * @param instancePattern               Instance e.g. FTM2 => "2"
   //    * @param matchTemplate          Pattern to select use of this template e.g. "FTM\\d+_CH\\d+"
   //    * @param deviceFamily           Device family
   //    * @param instanceWriter         InstanceWriter to use
   //    */
   //   public PeripheralTemplateInformation createPeripheralTemplateInformation(
   //         String         namePattern,   
   //         String         peripheralBasename,   
   //         String         instancePattern, 
   //         String         matchTemplate, 
   //         DeviceFamily   deviceFamily, 
   //         Class<?>       instanceWriterClass) throws Exception {
   //
   //      PeripheralTemplateInformation template = null;
   //      try {
   //         template = 
   //               new PeripheralTemplateInformation(
   //                     this, deviceFamily, namePattern, peripheralBasename, instancePattern, matchTemplate, instanceWriterClass);
   //         fTemplateList.add(template);
   //      }
   //      catch (Exception e) {
   //         throw new RuntimeException(e);
   //      }
   //      return template;
   //   }

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
               "$1", "$2",
               "(PIT)()(\\d+)",
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
               "USB", "",
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
               "(PIT)(\\d)?(.*)",
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
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(DMA)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
               "(MPU)(\\d)?(.*)",
               getDeviceFamily(),
               WriterForMisc.class);
         createPeripheralTemplateInformation(
               "$1", "$2", "$3",
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
            "$1", "$2",
            "(.*)()()",
            getDeviceFamily(),
            WriterForNull.class);
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

   private final static HashMap<String, MuxSelection> exceptions = new  HashMap<String, MuxSelection>();

   private static boolean checkOkException(PinInformation pin) {
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
         PinInformation pin = getPins().get(pName);
         if (!checkOkException(pin)) {
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
