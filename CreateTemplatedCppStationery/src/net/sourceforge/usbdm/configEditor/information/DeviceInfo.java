package net.sourceforge.usbdm.configEditor.information;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.usbdm.configEditor.parser.MiscellaneousPeripheralTemplateInformation;
import net.sourceforge.usbdm.configEditor.parser.WriterBase;
import net.sourceforge.usbdm.configEditor.parser.WriterForAnalogueIO;
import net.sourceforge.usbdm.configEditor.parser.WriterForCmp;
import net.sourceforge.usbdm.configEditor.parser.WriterForDigitalIO;
import net.sourceforge.usbdm.configEditor.parser.WriterForDmaMux;
import net.sourceforge.usbdm.configEditor.parser.WriterForI2c;
import net.sourceforge.usbdm.configEditor.parser.WriterForLlwu;
import net.sourceforge.usbdm.configEditor.parser.WriterForLptmr;
import net.sourceforge.usbdm.configEditor.parser.WriterForLpuart;
import net.sourceforge.usbdm.configEditor.parser.WriterForPit;
import net.sourceforge.usbdm.configEditor.parser.WriterForPwmIO_FTM;
import net.sourceforge.usbdm.configEditor.parser.WriterForPwmIO_TPM;
import net.sourceforge.usbdm.configEditor.parser.WriterForSpi;
import net.sourceforge.usbdm.configEditor.parser.WriterForTsi;
import net.sourceforge.usbdm.configEditor.parser.WriterForUart;
import net.sourceforge.usbdm.configEditor.parser.WriterForVref;

public class DeviceInfo {

   public static final String VERSION           = "1.2.0";
   public static final String DTD_FILE          = "Pins.dtd";
   public static final String NAME_SPACE        = "USBDM";
   public static final String NAMESPACES_GUARD  = "USE_USBDM_NAMESPACE";

   enum Mode {newInstance, anyInstance, allowNullInstance};

   public enum DeviceFamily {mk, mke, mkl, mkm};

   public DeviceFamily getDeviceFamily() {
      return fDeviceFamily;
   }

   /** Name of the device e.g. MKL25Z4 */
   private final String       fDeviceName;
   private final String       fSourceFilename;
   private final DeviceFamily fDeviceFamily;

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

   public String getDeviceName() {
      return fDeviceName;
   }

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
    * 
    * @return
    */
   public Peripheral createPeripheral(String baseName, String instance) {  
      String name = baseName+instance;
      Peripheral p = fPeripheralsMap.get(name);

      if (p!= null) {
         throw new RuntimeException("Attempting to re-create instance " + name);
      }
      p = new Peripheral(baseName, instance);
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
   public Peripheral findOrCreatePeripheral(String baseName, String instance) {  
      Peripheral p = fPeripheralsMap.get(baseName+instance);
      if (p == null) {
         p = createPeripheral(baseName, instance);
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
    * e.g. findPeripheralFunction(FTM,0,6) = <i>PeripheralFunction</i>(FTM, 0, 6)
    * 
    * @param baseName      e.g. FTM0_CH6 = FTM
    * @param instanceNum   e.g. FTM0_CH6 = 0
    * @param signalName    e.g. FTM0_CH6 = 6
    * @param create        If true then the peripheral function will be created if it does not already exist
    *                      
    * @return Peripheral function if found or created, null otherwise
    * @throws Exception 
    */
   public PeripheralFunction createPeripheralFunction(String name, String baseName, String instance, String signal) {

      PeripheralFunction peripheralFunction = fPeripheralFunctions.get(name);
      if (peripheralFunction != null) {
         throw new RuntimeException("PeripheralFunction already exists "+ name);
      }

      peripheralFunction = new PeripheralFunction(name, findOrCreatePeripheral(baseName, instance), signal);

      // Add to base name map
      Map<String, PeripheralFunction> map = fPeripheralFunctionsByBaseName.get(baseName);
      if (map == null) {
         map = new TreeMap<String, PeripheralFunction>();
         fPeripheralFunctionsByBaseName.put(baseName, map);
      }
      map.put(baseName, peripheralFunction);

      // Add to map
      fPeripheralFunctions.put(name, peripheralFunction);

      findOrCreatePeripheral(baseName, instance);

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

   public Map<String, DevicePackage> getDevicePackages() {
      return fDevicePackages;
   }

   /*
    * MappingInfo =============================================================================================
    */

   /**
    * Map from Pin to list of Functions
    */
   private Map<PinInformation, Map<MuxSelection, MappingInfo>> fPinInformationMap = new TreeMap<PinInformation, Map<MuxSelection, MappingInfo>>();

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
    * Get list of pin mappings associated with given pin
    * 
    * @param pin 
    * 
    * @return
    */
   public Map<MuxSelection, MappingInfo> getFunctions(PinInformation pin) {
      return fPinInformationMap.get(pin);
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
      //      System.err.println(String.format("S:%s, P:%s, M:%s", function.toString(), pinInformation.toString(), functionSelector.toString()));
      Map<MuxSelection, MappingInfo> list = fPinInformationMap.get(pinInformation);
      if (list == null) {
         list = new TreeMap<MuxSelection, MappingInfo>();
         fPinInformationMap.put(pinInformation, list);
      }
      MappingInfo mapInfo = list.get(functionSelector);
      if (mapInfo == null) {
         mapInfo = new MappingInfo(pinInformation, functionSelector);
         list.put(functionSelector, mapInfo);
      }
      mapInfo.functions.add(function);
      addToFunctionMap(function, mapInfo);

      return mapInfo;
   }

   /**
    * Get functions for given base name
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
      //      System.err.println("Matches");
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

   public PeripheralTemplateInformation createPeripheralTemplateInformation(
         String baseName,   
         String peripheralName, 
         String matchTemplate, 
         WriterBase instanceWriter) {
      PeripheralTemplateInformation template = 
            new PeripheralTemplateInformation(this, baseName, peripheralName, matchTemplate, instanceWriter);
      fTemplateList.add(template);
      return template;
   }

   public void createMiscellaneousPeripheralTemplateInformation(DeviceFamily deviceFamily) {
      PeripheralTemplateInformation template = new MiscellaneousPeripheralTemplateInformation(this, deviceFamily);
      fTemplateList.add(template);
   }

   public void initialiseTemplates() {

      /*
       * Set up templates
       */
      for (char suffix='A'; suffix<='I'; suffix++) {
         createPeripheralTemplateInformation(
               "Gpio"+suffix, "PORT"+suffix, 
               "^\\s*(GPIO)("+suffix+")_(\\d+)\\s*$",
               new WriterForDigitalIO(getDeviceFamily()));
      }
      //    for (char suffix='A'; suffix<='I'; suffix++) {
      //       new FunctionTemplateInformation(
      //             "Gpio"+suffix, "PORT"+suffix, "Port_Group",  "Port Definitions",               
      //             "Information required to manipulate PORT PCRs & associated GPIOs", 
      //             null,
      //             new WriterForPort(deviceIsMKE));
      //    }

      if (getDeviceFamily() != DeviceFamily.mkm) {
         for (char suffix='0'; suffix<='3'; suffix++) {
            createPeripheralTemplateInformation(
                  "Adc"+suffix, "ADC"+suffix,
                  "(ADC)("+suffix+")_(SE\\d+)b?",
                  new WriterForAnalogueIO(getDeviceFamily()));
            createPeripheralTemplateInformation(
                  "Adc"+suffix+"a", "ADC"+suffix,
                  "(ADC)("+suffix+")_(SE\\d+)a",
                  new WriterForAnalogueIO(getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            createPeripheralTemplateInformation(
                  "Cmp"+suffix, "CMP"+suffix,  
                  "(CMP)("+suffix+")_(IN\\d)",
                  new WriterForCmp(getDeviceFamily()));
         }
         createPeripheralTemplateInformation(
               "DmaMux0", "DMAMUX0",  
               null,
               new WriterForDmaMux(getDeviceFamily()));
         for (char suffix='0'; suffix<='3'; suffix++) {
            createPeripheralTemplateInformation(
                  "Ftm"+suffix, "FTM"+suffix, 
                  "(FTM)("+suffix+")_(CH\\d+|QD_PH[A|B]|FLT\\d|CLKIN\\d)",
                  new WriterForPwmIO_FTM(getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            createPeripheralTemplateInformation(
                  "I2c"+suffix, "I2C"+suffix,  
                  "(I2C)("+suffix+")_(SCL|SDA|4WSCLOUT|4WSDAOUT)",
                  new WriterForI2c(getDeviceFamily()));
         }
         createPeripheralTemplateInformation(
               "Lptmr0", "LPTMR0",  
               "(LPTMR)(0)_(ALT\\d+)",
               new WriterForLptmr(getDeviceFamily()));
         createPeripheralTemplateInformation(
               "Pit", "PIT",  
               "(PIT)()(\\d+)",
               new WriterForPit(getDeviceFamily()));
         createPeripheralTemplateInformation(
               "Llwu", "LLWU",  
               "(LLWU)()_(P\\d+)",
               new WriterForLlwu(getDeviceFamily()));
         for (char suffix='0'; suffix<='3'; suffix++) {
            createPeripheralTemplateInformation(
                  "Spi"+suffix, "SPI"+suffix,  
                  "(SPI)("+suffix+")_(SCK|SIN|SOUT|MISO|MOSI|SS|PCS\\d*)",
                  new WriterForSpi(getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            createPeripheralTemplateInformation(
                  "Tpm"+suffix, "TPM"+suffix,  
                  "(TPM)("+suffix+")_(CH\\d+|QD_PH[A|B])",
                  new WriterForPwmIO_TPM(getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            createPeripheralTemplateInformation(
                  "Tsi"+suffix, "TSI"+suffix,  
                  "(TSI)("+suffix+")_(CH\\d+)",
                  new WriterForTsi(getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='5'; suffix++) {
            createPeripheralTemplateInformation(
                  "Uart"+suffix, "UART"+suffix,  
                  "(UART)("+suffix+")_(TX|RX|CTS_b|RTS_b|COL_b)",
                  new WriterForUart(getDeviceFamily()));
         }
         for (char suffix='0'; suffix<='5'; suffix++) {
            createPeripheralTemplateInformation(
                  "Lpuart"+suffix, "LPUART"+suffix,  
                  "(LPUART)("+suffix+")_(TX|RX|CTS_b|RTS_b)",
                  new WriterForLpuart(getDeviceFamily()));
         }
         createPeripheralTemplateInformation(
               "Vref", "VREF",  
               "(VREF)()_(OUT)",
               new WriterForVref(getDeviceFamily()));
      }
      createMiscellaneousPeripheralTemplateInformation(getDeviceFamily());
   }
   
   /*
    * DeviceInformation =============================================================================================
    */
   /** Devices */
   private Map<String, DeviceInformation> fDevices = new TreeMap<String, DeviceInformation>();

   public DeviceInformation createDeviceInformation(String name, String manual, String packageName) {
      DeviceInformation deviceInformation = new DeviceInformation(name, findOrCreateDevicePackage(packageName), manual);
      fDevices.put(name, deviceInformation);
      return deviceInformation;
   };

   public Map<String, DeviceInformation> getDevices() {
      return fDevices;
   }

   public DeviceInformation findDevice(String deviceName) {
      return fDevices.get(deviceName);
   }

   /*
    * DmaInfo =============================================================================================
    */
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
   
}
