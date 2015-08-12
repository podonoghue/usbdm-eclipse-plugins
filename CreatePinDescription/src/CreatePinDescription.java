import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreatePinDescription extends DocumentUtilities {

   static final String VERSION = "1.0.0";

   /*
    * Indicates what function may be mapped to a pin
    */
   static class MappingInfo {
      public String baseName;    // Base name of the peripheral e.g. FTM0_CH6 => FTM, PTA3 => PT
      public String name;        // Name/Number of the peripheral e.g. FTM0_CH6 => 0, PTA3 => A
      public String channel;     // Channel or pin operation e.g. FTM0_CH6 => 6, PTA3 => 3, SPI0_SCK => SCK
      public int    mux;         // Pin multiplexor setting
      
      public MappingInfo(String baseName, String name, String channel, int mux) {
         this.baseName = baseName;
         this.name     = name;
         this.channel  = channel;
         this.mux      = mux;
      }
      String getName() {
         return baseName+name+"_"+channel;
      }
   };
   HashSet<String> baseNames = new HashSet<String>();
   
   static class PinInformation {
      public String name;                     // Name of the pin, usually the port name e.g. PTA1
      public String description;              // Description of pin functions e.g. // PTA1 = PTA1,FTM0.6
      public HashMap<String, ArrayList<MappingInfo>> mappedPins = new HashMap<String, ArrayList<MappingInfo>>();

      public PinInformation(String name) {
         this.name = name;
      }
      ArrayList<MappingInfo> getMappingList(String baseName) {
         ArrayList<MappingInfo> list = mappedPins.get(baseName);
         if (list == null) {
            list = new ArrayList<CreatePinDescription.MappingInfo>();
            mappedPins.put(baseName, list);
         }
         return list;
      }
   };
   static class MuxValues {
      ArrayList<String> alternatives;
      int defaultValue;
      MuxValues() {
         defaultValue = 0;
         alternatives = new ArrayList<String>();
         alternatives.add("Disabled");
      }
      void setDefaultValue(int defaultValue) {
         this.defaultValue = defaultValue;
      }
   };
   ArrayList<PinInformation> pins = new ArrayList<CreatePinDescription.PinInformation>();
   HashMap<String, MuxValues> portMapping = new HashMap<String, MuxValues>();
   
   //! Mapping of Alias pins to Kinetis pins
   //! Many-to-1
   HashMap<String, String> aliasMapping = new HashMap<String, String>();
   
   //! Mapping of Kinetis pins to Alias pins
   //! 1-to-Many
   HashMap<String, ArrayList<String>> reverseAliasMapping = new HashMap<String, ArrayList<String>>();
   
   ArrayList<String> gpio     = new ArrayList<String>(); // Pins with ADC feature
   ArrayList<String> analogue = new ArrayList<String>(); // Pins with ADC feature
   ArrayList<String> ftm      = new ArrayList<String>(); // Pins with FTM feature
   ArrayList<String> tpm      = new ArrayList<String>(); // Pins with TPM feature
   HashMap<String, HashSet<String>> peripheralInstances = new HashMap<String, HashSet<String>>();
   HashMap<String, ClockInfo>       clockInfo           = new HashMap<String, ClockInfo>();
   
   final static String pinMappingBaseFileName   = "pin_mapping";
   final static String gpioBaseFileName         = "gpio";
   String deviceName;

   private String pinMappingHeaderFileName;  // Name of pin-mapping-XX.h header file
   private String gpioCppFileName;           // Name of gpio-XX.cpp source file
   private String gpioHeaderFileName;        // Name of gpio-XX.h header file

   int      gpioFunctionMuxValue          = 1; // Ports default to mux setting 1
   boolean  gpioFunctionMuxValueChanged   = false;
   int      adcFunctionMuxValue           = 0; // ADCs default to mux setting 0
   boolean  adcFunctionMuxValueChanged    = false;
   String   portClockRegisterValue        = null;
   boolean  portClockRegisterValueChanged = false;

   class ClockInfo {
      String clockReg;
      String clockMask;
      
      ClockInfo(String clockReg, String clockMask) {
         this.clockReg = clockReg;
         this.clockMask = clockMask;
      }
   };
   
   void addPeripheralCount(String peripheral, String instanceNumber) {
      HashSet<String> instances = peripheralInstances.get(peripheral);
      if (instances == null) {
         instances = new HashSet<String>();
         peripheralInstances.put(peripheral, instances);
      }
      instances.add(instanceNumber);
   }
   /**
    * Parse line containing Default value
    * 
    * @param line
    * @throws Exception
    */
   void parseDefaultLine(String[] line) throws Exception {
      if (!line[0].equals("Default")) {
         return;
      }
      if (line.length < 3) {
         throw new Exception("Illegal Default Mapping line");
      }
      String portName         = line[1];
      String preferredMapping = line[2];
      MuxValues muxValues = portMapping.get(portName);
      if (muxValues == null) {
         System.err.println("OPPS- looking for " + portName);
         System.err.println("OPPS- looking in " + portMapping.keySet());
      }
      else {
         int index = muxValues.alternatives.indexOf(preferredMapping);
         if (index<0) {
            throw new Exception(String.format("Illegal default value = %s, for %s", preferredMapping, portName ));
         }
         muxValues.setDefaultValue(index);
      }
   }
   /**
    * Parse line containing Alias value
    * 
    * @param line
    * @throws Exception
    */
   void parseAliasLine(String[] line) throws Exception {
      if (!line[0].equals("Alias")) {
         return;
      }
      if (line.length < 3) {
         throw new Exception("Illegal Alias Mapping line");
      }
      String aliasName    = line[1];
      String mappedName   = line[2];
      aliasMapping.put(aliasName, mappedName);
      ArrayList<String> aliasList = reverseAliasMapping.get(mappedName);
      if (aliasList == null) {
         aliasList = new ArrayList<String>();
         reverseAliasMapping.put(mappedName, aliasList);
      }
      aliasList.add(aliasName);
   }
   /**
    * Parse line containing ClockReg value
    * 
    * @param line
    * @throws Exception
    */
   void parseClockInfoLine(String[] line) throws Exception {
      if (!line[0].equals("ClockInfo")) {
         return;
      }
      if (line.length < 3) {
         throw new Exception("Illegal ClockInfo Mapping line");
      }
      String peripheralName       = line[1];
      String peripheralClockReg   = line[2];
      String peripheralClockMask;
      if (line.length < 4) {
         peripheralClockMask = peripheralClockReg.replace("->", "_")+"_"+peripheralName+"_MASK";
      }
      else {
         peripheralClockMask = line[3];
      }
      clockInfo.put(peripheralName, new ClockInfo(peripheralClockReg, peripheralClockMask));
   }

   final int pinIndex      = 1;
   final int altStartIndex = 2;
   final int portIndex     = 3;
   /**
    * 
    * @param line          Line to process
    * @param description   Description of peripheral functions that may be mapped to this pin
    * @param template      Template for matching port names e.g. "^.*(FTM)(\\d*)_CH(\\d*).*$"<br>
    *                      <li>group(1)=baseName e.g. ADC, 
    *                      <li>group(2)=peripheral number/name e.g. PT,ADC, 
    *                      <li>group(3)=peripheral pin number/name e.g. 3,SIN
    * @param pinInfo 
    * @param elements      List to add discovered ports to
    * @param pinNames      List to add pin names to add 
    */
   void doPinPeripheral(String[] line, StringBuilder description, String baseName, String template, PinInformation pinInfo, ArrayList<String> pinNames) {
      String pinName  = line[pinIndex];
      Pattern pattern = Pattern.compile(template);
      for (int col=altStartIndex; col<line.length; col++) {
         Matcher matcher = pattern.matcher(line[col]);
         if (matcher.matches()) {
//            String pn = matcher.group(1);
            String peripheralNum = matcher.group(2);
            String signalName    = matcher.group(3);
//            System.err.println(String.format("pn = %s, p#=%s, sn=%s", pn, peripheralNum, signalName));
            baseNames.add(baseName);
            
            int       peripheralFn         = col-altStartIndex;
            String    peripheralSignalName = String.format("%s%s_%s", baseName, peripheralNum, signalName);
            MuxValues mapEntry             = portMapping.get(peripheralSignalName);
            if (mapEntry == null) {
               // Add new entry
               mapEntry = new MuxValues();
               portMapping.put(peripheralSignalName, mapEntry);
            }
            mapEntry.alternatives.add(pinName);
            if (pinNames != null) {
               pinNames.add(pinName);
            }
            ArrayList<MappingInfo> elements = pinInfo.getMappingList(baseName);
            elements.add(new MappingInfo(baseName, peripheralNum, signalName, peripheralFn));
            if (peripheralSignalName.startsWith("PT")) {
               continue;
            }
            addPeripheralCount(baseName, peripheralNum);
            
            if (description.length() != 0) {
               description.append(",");
            }
            description.append(peripheralSignalName);
         }
      }
   }
   /**
    * Parse line containing Pin information
    *  
    * @param line
    * @throws Exception
    */
   void parsePinLine(String[] line) throws IOException {
      if (!line[0].equals("Pin")) {
         return;
      }
      String pinName  = line[pinIndex];
      if ((pinName == null) || (pinName.isEmpty())) {
         System.err.println("Line discarded");
         return;
      }
      StringBuilder  description = new StringBuilder();
      PinInformation pinInfo     = new PinInformation(pinName);
      pins.add(pinInfo); 
      doPinPeripheral(line, description, "GPIO",  "^.*(GPIO)([A-Z])_(\\d*)(/.*|$)",                                    pinInfo, gpio);
      if (!deviceIsMKE) {
         doPinPeripheral(line, description, "ADC",   "^.*(ADC)(\\d*)(?:_SE)?(\\d*)(b.*|/.*|$)",                           pinInfo, analogue);
         doPinPeripheral(line, description, "FTM",   "^.*(FTM)(\\d*)_CH(\\d*)(/.*|$)",                                    pinInfo, ftm);
         doPinPeripheral(line, description, "TPM",   "^.*(TPM)(\\d*)_CH(\\d*)(/.*|$)",                                    pinInfo, tpm);
         doPinPeripheral(line, description, "LPTMR", "^.*(LPTMR)(\\d*)_ALT(\\d*)(b.*|/.*|$)",                             pinInfo, null);
         doPinPeripheral(line, description, "SDHC",  "^.*(SDHC)(\\d*)_((CLKIN)|(D\\d)|(CMD)|(DCLK))(/.*|$)",              pinInfo, null);
         doPinPeripheral(line, description, "SPI",   "^.*(SPI)(\\d*)_((SOUT)|(SIN)|(SCK)|(PCS\\d)|(MOSI)|(MISO))(/.*|$)", pinInfo, null);
         doPinPeripheral(line, description, "I2C",   "^.*(I2C)(\\d*)_((SDA)|(SCL))(/.*|$)",                               pinInfo, null);
      }
      pinInfo.description = pinName;
      if (description.length() > 0) {
         pinInfo.description += " = "+description.toString();
      }
   }
   /**
    * Gets list of alias for this pin as a comma separated string.
    * 
    * @param pinName
    * @return String of form alias1,alias2 ... or null if no aliases defined
    */
   String getAliasList(String pinName) {
      ArrayList<String> aliasList = reverseAliasMapping.get(pinName);
      if (aliasList == null) {
         return null;
      }
      StringBuilder b = new StringBuilder();
      boolean firstTime = true;
      for(String s:aliasList) {
         if (!firstTime) {
            b.append(",");
         }
         firstTime = false;
         b.append(s);
      }
      return b.toString();
   }
   /**
    * Writes macros describing pin functions
    * 
    * @param pinInfo    Pin to describe
    * @param headerFile Header file to write to
    * 
    * @throws Exception 
    */
   void writePinDefines(PinInformation pinInfo, BufferedWriter headerFile) throws Exception {
      String aliasName = getAliasList(pinInfo.name);
      String description = pinInfo.description;
      if (aliasName != null) {
         description += " (Alias: " + aliasName + ")";
      }
      headerFile.append("// "+description + "\n");
      String pinName  = pinInfo.name;
      
      /*
       * Do port functions
       */
      ArrayList<MappingInfo> mappingList = pinInfo.getMappingList("GPIO");
      if (mappingList.size()>0) {
         if (mappingList.size() > 1) {
            throw new Exception("Multiple ports mapped!");
         }
         MappingInfo info = mappingList.get(0);
         String baseName  = info.baseName;
         String name      = info.name;
         String chNum     = info.channel;
         int    pinMux    = info.mux; 
         if (info.mux != gpioFunctionMuxValue) {
            if (gpioFunctionMuxValueChanged) {
               throw new Exception(
                     String.format("GPIO pin mux value non-constant, pin = %s, mux = %d", pinName, info.mux));
            }
            gpioFunctionMuxValueChanged = true;
            gpioFunctionMuxValue = info.mux;
            System.err.println(String.format("Changing GPIO pin mapping, pin = %s, mux = %d", pinName, info.mux));
         }
         headerFile.append(String.format(
               "#define %-18s         %-3s   //!< %s %s name\n"+
               "#define %-18s         %-3s   //!< %s %s bit number\n",
               pinInfo.name+"_"+baseName+"_NAME", name,  pinName, baseName,
               pinInfo.name+"_"+baseName+"_BIT",  chNum, pinName, baseName
               ));
//         headerFile.append(String.format(
//                     "#define %-18s         %-3s   //!< %s Port name\n"+
//                     "#define %-18s         %-3s   //!< %s Port number\n",
//                     pinName+"_PORT", name,   pinName, 
//                     pinName+"_NUM",  chNum,  pinName
//                     ));
         headerFile.append(String.format(
               "#define %-18s         %-3d   //!< %s Pin multiplexor for %s\n",
               pinInfo.name+"_"+baseName+"_FN", pinMux,    pinName, baseName
               ));
      }
      /*
       * Do ADC functions
       */
      mappingList = pinInfo.getMappingList("ADC");
      if (mappingList.size() >0 ) {
         MappingInfo info = mappingList.get(0);
         String baseName  = info.baseName;
         String name      = info.name;
         String chNum     = info.channel;
         int    pinMux    = info.mux; 
         String fullName  = info.getName();
         MuxValues muxValues = portMapping.get(fullName);
         if (mappingList.size() > 1) {
            throw new Exception(String.format("ADC multiple ports mapped!, pin = %s, mux = %d", pinName, pinMux));
         }
         if (pinMux != adcFunctionMuxValue) {
            if (adcFunctionMuxValueChanged) {
               System.err.println(String.format("ADC pin mux value non-constant, pin = %s, mux = %d", pinName, pinMux));
            }
            adcFunctionMuxValueChanged = true;
            adcFunctionMuxValue = pinMux;
            System.err.println(String.format("Changing ADC pin mapping, pin = %s, mux = %d", pinName, pinMux));
         }
         if (muxValues == null) {
            throw new Exception("Failed to find "+fullName);
         }
         ArrayList<String> mapEntry = muxValues.alternatives;
         if (mapEntry.size()>2) {
            int selectionIndex = mapEntry.indexOf(pinName);
            headerFile.append(String.format(
               "#if %s_SEL == %d\n",
               baseName+name+"_"+chNum, selectionIndex
               ));
         }
         headerFile.append(String.format(
               "#define %-18s         %-3s   //!< %s %s number\n"+
               "#define %-18s         %-3s   //!< %s %s channel\n",
               pinInfo.name+"_"+baseName+"_NUM", name,  pinName, baseName,
               pinInfo.name+"_"+baseName+"_CH",  chNum, pinName, baseName
               ));
         headerFile.append(String.format(
               "#define %-18s         %-3d   //!< %s Pin multiplexor for %s\n",
               pinInfo.name+"_"+baseName+"_FN", pinMux,    pinName, baseName
               ));
         if (mapEntry.size()>2) {
            headerFile.append("#endif\n");
         }
      }
   
      mappingList = pinInfo.getMappingList("FTM");
      for(MappingInfo info:mappingList) {
         String baseName  = info.baseName;
         String name      = info.name;
         String chNum     = info.channel;
         int    pinMux    = info.mux; 
         String fullName  = info.getName();
         MuxValues muxValues = portMapping.get(fullName);
         if (muxValues == null) {
            throw new Exception("Failed to find "+fullName);
         }
         ArrayList<String> mapEntry = muxValues.alternatives;
         if (mapEntry.size()>2) {
            int selectionIndex = mapEntry.indexOf(pinName);
            headerFile.append(String.format(
                  "#if %s_SEL == %d\n",
                  baseName+name+"_"+chNum, selectionIndex
                  ));
         }
         headerFile.append(String.format(
               "#define %-18s         %-3s   //!< %s %s number\n"+
               "#define %-18s         %-3s   //!< %s %s channel\n",
               pinInfo.name+"_"+baseName+"_NUM", name,  pinName, baseName,
               pinInfo.name+"_"+baseName+"_CH",  chNum, pinName, baseName
               ));
         headerFile.append(String.format(
               "#define %-18s         %-3d   //!< %s Pin multiplexor for %s\n",
               pinInfo.name+"_"+baseName+"_FN", pinMux,    pinName, baseName
               ));
         if (mapEntry.size()>2) {
            headerFile.append("#endif\n");
         }
      }
      mappingList = pinInfo.getMappingList("TPM");
      for(MappingInfo info:mappingList) {
         String baseName  = info.baseName;
         String name      = info.name;
         String chNum     = info.channel;
         int    pinMux    = info.mux;
         String fullName  = info.getName();
         MuxValues muxValues = portMapping.get(fullName);
         if (muxValues == null) {
            throw new Exception("Failed to find "+fullName);
         }
         ArrayList<String> mapEntry = muxValues.alternatives;
         if (mapEntry.size()>2) {
            int selectionIndex = mapEntry.indexOf(pinName);
            headerFile.append(String.format(
                  "#if %s_SEL == %d\n",
                  baseName+name+"_"+chNum, selectionIndex
                  ));
         }
         headerFile.append(String.format(
               "#define %-18s         %-3s   //!< %s %s number\n"+
               "#define %-18s         %-3s   //!< %s %s channel\n",
               pinInfo.name+"_"+baseName+"_NUM", name,  pinName, baseName,
               pinInfo.name+"_"+baseName+"_CH",  chNum, pinName, baseName
               ));
         headerFile.append(String.format(
               "#define %-18s         %-3d   //!< %s Pin multiplexor for %s\n",
               pinInfo.name+"_"+baseName+"_FN", pinMux,    pinName, baseName
               ));
         if (mapEntry.size()>2) {
            headerFile.append("#endif\n");
         }
      }
      for (PinFunctionDescription pinFunctionDescription:pinFunctionDescriptions) {
         mappingList = pinInfo.getMappingList(pinFunctionDescription.baseName);
         for(MappingInfo info:mappingList) {
            String baseName  = info.baseName;
            String name      = info.name;
            String chNum     = info.channel;
            int    pinMux    = info.mux;
            String fullName  = info.getName();
            MuxValues muxValues = portMapping.get(fullName);
            if (muxValues == null) {
               throw new Exception("Failed to find "+fullName);
            }
            ArrayList<String> mapEntry = muxValues.alternatives;
            if (mapEntry.size()>1) {
               int selectionIndex = mapEntry.indexOf(pinName);
               headerFile.append(String.format(
                  "#if %s_SEL == %d\n",
                  baseName+name+"_"+chNum, selectionIndex
                  ));
            }
            headerFile.append(String.format(
                  "#define %-18s         %-3d   //!< %s Pin multiplexor for %s\n",
                  fullName+"_FN", pinMux,    pinName, baseName
                  ));
            headerFile.append(String.format(
               "#define %-18s         digitalIO_%-3s   //!< %s %s GPIO\n",
               fullName+"_GPIO",  pinName, pinName, baseName
               ));
            if (mapEntry.size()>1) {
               headerFile.append("#endif\n");
            }
         }
      }
      headerFile.append("\n");
   }

   /**
    * Writes pin-mapping selection code to header file
    *  
    * @param peripheralPin Pin to process
    * @param writer        File to write result
    * 
    * @throws IOException
    */
   void processPeripheralPinMapping(String peripheralPin, BufferedWriter writer) throws IOException {
      MuxValues muxValues = portMapping.get(peripheralPin);
      ArrayList<String> portMap = muxValues.alternatives;
      boolean isConstant = false;
      if ((portMap.size() <= 2) && peripheralPin.startsWith("GPIO")) {
         return;
      }
      if ((portMap.size() <= 2) && peripheralPin.startsWith("ADC")) {
         muxValues.setDefaultValue(1);
         isConstant = true;
      }
      if ((portMap.size() <= 2) && peripheralPin.startsWith("FTM")) {
         muxValues.setDefaultValue(1);
         isConstant = true;
      }
      if ((portMap.size() <= 2) && peripheralPin.startsWith("TPM")) {
         muxValues.setDefaultValue(1);
         isConstant = true;
      }
//      if ((portMap.size() <= 2) && peripheralPin.startsWith("SDHC")) {
//         return;
//      }
      String description = peripheralPin + " maps to " + portMap;

      StringBuilder sb = new StringBuilder();
      for(String mappedPin:portMap) {
         if (mappedPin.equals("Disabled")) {
            continue;
         }
         if (sb.length()>0) {
            sb.append(", ");
         }
         String aliasName = getAliasList(mappedPin);
//         System.err.println(mappedPin + " => " + aliasName);
         if (aliasName != null) {
            mappedPin += "(" + aliasName + ")";
         }
         sb.append(mappedPin);
      }
      writeWizardOptionSelectionPreamble(writer, 
            description,
            0,
            isConstant,
            String.format("%s Pin Selection [%s]",            peripheralPin, sb),
            String.format("Selects which pin is used for %s", peripheralPin));
      
      int selection = 0;
      int defaultSelection = muxValues.defaultValue;
      for(String mappedPin:portMap) {
         String aliasName = getAliasList(mappedPin);
         if (aliasName != null) {
            mappedPin += " (Alias: " + aliasName + ")";
         }
         writeWizardOptionSelectionEnty(writer, Integer.toString(selection++), mappedPin);
      }
      writeWizardDefaultSelectionEnty(writer, Integer.toString(defaultSelection));
      writeMacroDefinition(writer, peripheralPin+"_SEL", Integer.toString(defaultSelection));
      writer.write("\n");
   }

   /**
    * Writes pin-mapping selection code for all mappable pins to header file
    *  
    * @param writer        File to write result
    * 
    * @throws IOException
    */
   void writePeripheralPinMappings(BufferedWriter writer) throws IOException {
      ArrayList<String> peripheralPins = new ArrayList<String>(portMapping.keySet());
      Collections.sort(peripheralPins, portNameComparator);
      for (String s:peripheralPins) {
         processPeripheralPinMapping(s, writer);
      }
//      for (String e:reverseAliasMapping.keySet()) {
//         System.err.println(e + " => " + reverseAliasMapping.get(e));
//      }
   }

   /**
    * Comparator for peripheral names e.g. FTM0_CH3 c.f. SPI_SCK
    * Treats the number separately as a number.
    */
   static Comparator<String> peripheralNameComparator = new Comparator<String>() {
      @Override
      public int compare(String arg0, String arg1) {
         Pattern p = Pattern.compile("([^\\d]*)(\\d*)");
         Matcher m0 = p.matcher(arg0);
         Matcher m1 = p.matcher(arg1);
         if (m0.matches() && m1.matches()) {
            String t0 = m0.group(1);
            String n0 = m0.group(2);
            String t1 = m1.group(1);
            String n1 = m1.group(2);
            int r = t0.compareTo(t1);
            if (r == 0) {
               int no0 = -1, no1 = -1;
               if (n0.length() > 0) {
                  no0 = Integer.parseInt(n0);
               }
               if (n1.length() > 0) {
                  no1 = Integer.parseInt(n1);
               }
               r = -no1 + no0;
            }
            return r;
         }
         return arg0.compareTo(arg1);
      }
   };

   /**
    * Comparator for port names e.g. PTA13 c.f. PTB12
    * Treats the number separately as a number.
    */
   static Comparator<String> portNameComparator = new Comparator<String>() {
      @Override
      public int compare(String arg0, String arg1) {
         Pattern p = Pattern.compile("([^\\d]*)(\\d*)(.*)");
         Matcher m0 = p.matcher(arg0);
         Matcher m1 = p.matcher(arg1);
         if (m0.matches() && m1.matches()) {
            String t0 = m0.group(1);
            String n0 = m0.group(2);
            String s0 = m0.group(3);
            String t1 = m1.group(1);
            String n1 = m1.group(2);
            String s1 = m1.group(3);
            int r = t0.compareTo(t1);
            if (r == 0) {
               int no0 = -1, no1 = -1;
               if (n0.length() > 0) {
                  no0 = Integer.parseInt(n0);
               }
               if (n1.length() > 0) {
                  no1 = Integer.parseInt(n1);
               }
               r = -no1 + no0;
            }
            if (r == 0) {
               Pattern pp = Pattern.compile("([^\\d]*)(\\d*)(.*)");
               Matcher mm0 = pp.matcher(s0);
               Matcher mm1 = pp.matcher(s1);
               if (mm0.matches() && mm1.matches()) {
                  String tt0 = mm0.group(1);
                  String nn0 = mm0.group(2);
                  String tt1 = mm1.group(1);
                  String nn1 = mm1.group(2);
                  r = tt0.compareTo(tt1);
                  if (r == 0) {
                     int no0 = -1, no1 = -1;
                     if (nn0.length() > 0) {
                        no0 = Integer.parseInt(nn0);
                     }
                     if (nn1.length() > 0) {
                        no1 = Integer.parseInt(nn1);
                     }
                     r = -no1 + no0;
                  }
               }
               else {
                  r = s0.compareTo(s1);
               }
            }
            return r;
         }
         return arg0.compareTo(arg1);
      }
   };

   /**
    * Comparator two lines based upon the Port name in line[0]
    */
   static Comparator<String[]> LineComparitor = new Comparator<String[]>() {
      @Override
      public int compare(String[] arg0, String[] arg1) {
         if (arg0.length < 2) {
            return (arg1.length<2)?0:-1;
         }
         if (arg1.length < 2) {
            return 1;
         }
         return portNameComparator.compare(arg0[1], arg1[1]);
      }
   };

   /**
    * Parse file
    * 
    * @param reader
    * 
    * @throws Exception
    */
   void parseFile(BufferedReader reader) throws Exception {
      ArrayList<String[]> grid = new ArrayList<String[]>();
      // Discard title line
      reader.readLine();
      do {
         String line = reader.readLine();
         if (line == null) {
            break;
         }
         grid.add(line.split(","));
         //         System.err.println(line);
      } while (true);
      Collections.sort(grid, LineComparitor);
      for(String[] line:grid) {
         if (line.length < 2) {
            continue;
         }
         parsePinLine(line);
      }
      for(String[] line:grid) {
         if (line.length < 2) {
            continue;
         }
         parseAliasLine(line);
      }
      for(String[] line:grid) {
         if (line.length < 2) {
            continue;
         }
         parseDefaultLine(line);
      }
      for(String[] line:grid) {
         if (line.length < 2) {
            continue;
         }
         parseClockInfoLine(line);
      }
   }

   /**
    * Writes macros describing pin functions for all pins
    * 
    * @param headerFile Header file to write to
    * 
    * @throws Exception 
    */
   void writePeripheralDefines(BufferedWriter headerFile) throws Exception {
      for(PinInformation p : pins) {
         writePinDefines(p, headerFile);
      }
      if (adcFunctionMuxValueChanged) {
         headerFile.append(String.format("#define ADC_FN_CHANGES      //!< Indicates ADC Multiplexing varies with pin\n"));
      }
      else {
         headerFile.append(String.format("#define DEFAULT_ADC_FN   %d //!< Fixed ADC Multiplexing value for pins\n", adcFunctionMuxValue));
      }
      if (gpioFunctionMuxValueChanged) {
         headerFile.append(String.format("#define GPIO_FN_CHANGES     //!< Indicates GPIO Multiplexing varies with pin\n"));
      }
      else {
         headerFile.append(String.format("#define DEFAULT_GPIO_FN  %d //!< Fixed GPIO Multiplexing value for pins\n", gpioFunctionMuxValue));
      }
   }

   void writeTimerControls(BufferedWriter headerFile) throws IOException {      
      if (ftm.size()>0) {
      writeWizardSectionOpen(headerFile, "FTM Clock settings");
      writeWizardOptionSelectionPreamble(headerFile, 
            String.format("FTM%s_SC.CLKS ================================\n//", ""), 
            0,
            false,
            String.format("FTM%s_SC.CLKS Clock source", ""),
            String.format("Selects the clock source for the FTM%s module. [FTM%s_SC.CLKS]", "", ""));
      writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
      writeWizardOptionSelectionEnty(headerFile, "1", "System clock");
      writeWizardOptionSelectionEnty(headerFile, "2", "Fixed frequency clock");
      writeWizardOptionSelectionEnty(headerFile, "3", "External clock");
      writeWizardDefaultSelectionEnty(headerFile, "1");
      writeWizardOptionSelectionPreamble(headerFile, 
            String.format("FTM%s_SC.PS ================================\n//", ""),
            1,
            false,
            String.format("FTM%s_SC.PS Clock prescaler", ""),
            String.format("Selects the prescaler for the FTM%s module. [FTM%s_SC.PS]", "", ""));
      writeWizardOptionSelectionEnty(headerFile, "0", "Divide by 1");
      writeWizardOptionSelectionEnty(headerFile, "1", "Divide by 2");
      writeWizardOptionSelectionEnty(headerFile, "2", "Divide by 4");
      writeWizardOptionSelectionEnty(headerFile, "3", "Divide by 8");
      writeWizardOptionSelectionEnty(headerFile, "4", "Divide by 16");
      writeWizardOptionSelectionEnty(headerFile, "5", "Divide by 32");
      writeWizardOptionSelectionEnty(headerFile, "6", "Divide by 64");
      writeWizardOptionSelectionEnty(headerFile, "7", "Divide by 128");
      writeWizardDefaultSelectionEnty(headerFile, "0");
      writeMacroDefinition(headerFile, "FTM_SC", "(FTM_SC_CLKS(0x1)|FTM_SC_PS(0x0))");
      headerFile.write("\n");
      writeWizardSectionClose(headerFile);
//      headerFile.write( String.format(optionSectionClose));
   }
   if (tpm.size()>0) {
      writeWizardSectionOpen(headerFile, "TPM Clock settings");
//      headerFile.write( String.format(optionSectionOpenTemplate, "TPM Clock settings"));
      writeWizardOptionSelectionPreamble(headerFile, 
            String.format("TPM%s_SC.CMOD ================================\n//", ""),
            0,
            false,
            String.format("TPM%s_SC.CMOD Clock source", ""),
            String.format("Selects the clock source for the TPM%s module. [TPM%s_SC.CMOD]", "", ""));
      writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
      writeWizardOptionSelectionEnty(headerFile, "1", "Internal clock");
      writeWizardOptionSelectionEnty(headerFile, "2", "External clock");
      writeWizardOptionSelectionEnty(headerFile, "3", "Reserved");
      writeWizardDefaultSelectionEnty(headerFile, "1");
      writeWizardOptionSelectionPreamble(headerFile, 
            String.format("TPM%s_SC.PS ================================\n//", ""),
            1,
            false,
            String.format("TPM%s_SC.PS Clock prescaler", ""),
            String.format("Selects the prescaler for the TPM%s module. [TPM%s_SC.PS]", "", ""));
      writeWizardOptionSelectionEnty(headerFile, "0", "Divide by 1");
      writeWizardOptionSelectionEnty(headerFile, "1", "Divide by 2");
      writeWizardOptionSelectionEnty(headerFile, "2", "Divide by 4");
      writeWizardOptionSelectionEnty(headerFile, "3", "Divide by 8");
      writeWizardOptionSelectionEnty(headerFile, "4", "Divide by 16");
      writeWizardOptionSelectionEnty(headerFile, "5", "Divide by 32");
      writeWizardOptionSelectionEnty(headerFile, "6", "Divide by 64");
      writeWizardOptionSelectionEnty(headerFile, "7", "Divide by 128");
      writeWizardDefaultSelectionEnty(headerFile, "0");
      writeMacroDefinition(headerFile, "TPM_SC", "(TPM_SC_CMOD(0x1)|TPM_SC_PS(0x0))");
      headerFile.write("\n");
      writeWizardSectionClose(headerFile);
   }
}
   /**
    * Writes pin mapping header file
    * 
    * @param headerFile Header file to write to
    * 
    * @throws Exception
    */
   void writePinMappingHeaderFile(BufferedWriter headerFile) throws Exception {
      writeHeaderFilePreamble(headerFile, pinMappingBaseFileName+".h", pinMappingHeaderFileName, VERSION, "Pin declarations for "+deviceName);
      writeHeaderFileInclude(headerFile, "derivative.h");
      headerFile.write("\n");
      writeWizardMarker(headerFile);
      writeTimerControls(headerFile);
      writeClockMacros(headerFile);
      writeWizardSectionOpen(headerFile, "Pin Peripheral mapping");
      writePeripheralPinMappings(headerFile);
      writeWizardSectionClose(headerFile);
      writePeripheralDefines(headerFile);
      writeHeaderFilePostamble(headerFile, pinMappingBaseFileName+".h");
   }

   /**
    * Writes aliases
    * 
    * @param writer
    * @param aliasIndex
    * @param peripheralList
    * @param prefix
    * @throws IOException
    */
   void writeAliases(BufferedWriter writer, ArrayList<String> aliasIndex, ArrayList<String> peripheralList, String prefix) throws IOException {
      for (String aliasName:aliasIndex) {
         String mappedName = aliasMapping.get(aliasName);
         if ((peripheralList==null) || peripheralList.contains(mappedName)) {
            writer.write( String.format(
                  "#define %-25s %-20s %s\n", prefix+aliasName, prefix+mappedName, "//!< alias "+aliasName+"=>"+mappedName ));
         }
      }
   }
   
   ArrayList<PeripheralTypeDescription> deviceDescriptions      = new ArrayList<PeripheralTypeDescription>();
   ArrayList<PinFunctionDescription>    pinFunctionDescriptions = new ArrayList<PinFunctionDescription>();
   private boolean deviceIsMKE;
   @SuppressWarnings("unused")
   private boolean deviceIsMKL;
   @SuppressWarnings("unused")
   private boolean deviceIsMKM;

   /**
    * Write GPIO Header file
    * 
    * @param gpioHeaderFile
    * @throws IOException
    */
   private void writeGpioHeaderFile(BufferedWriter gpioHeaderFile) throws IOException {
      writeHeaderFilePreamble(gpioHeaderFile, "gpio.h", gpioHeaderFileName, VERSION, "Pin declarations for "+deviceName);
      writeHeaderFileInclude(gpioHeaderFile, "derivative.h");
      writeHeaderFileInclude(gpioHeaderFile, "pin_mapping.h");
      writeHeaderFileInclude(gpioHeaderFile, "gpio_defs.h");
      gpioHeaderFile.write("\n");

      for (PeripheralTypeDescription deviceDescription:deviceDescriptions) {
         boolean groupDone = false;
         for (PinInformation pin:pins) {
            String pinName = pin.name;
            ArrayList<MappingInfo> listOfMappings = pin.getMappingList(deviceDescription.baseName);
            for (MappingInfo info:listOfMappings) {
               if (!groupDone) {
                  groupDone = true;
                  writeStartGroup(gpioHeaderFile, deviceDescription);
               }
               String baseName = info.baseName;
               String fullName = info.getName();
               ArrayList<String> mapEntry = portMapping.get(fullName).alternatives;
               boolean macroDone = false;
               if ((mapEntry.size()>2) || (!baseName.equals("GPIO") && (mapEntry.size()>1))) {
                  macroDone = true;
                  int selectionIndex = mapEntry.indexOf(pinName);
                  gpioHeaderFile.write(String.format(
                        "#if %s_SEL == %d\n",
                        fullName, selectionIndex
                        ));
               }
               gpioHeaderFile.write(String.format(deviceDescription.outputTemplate, deviceDescription.className+pinName+";", pinName));
               if (macroDone) {
                  gpioHeaderFile.write("#endif\n");
               }
            }
         }
         if (groupDone) {
            writeCloseGroup(gpioHeaderFile);
         }
      }
      
      /*
       * Write aliases
       */
      writeStartGroup(gpioHeaderFile, "alias_pin_mappings_GROUP", "Aliases for pins", "Aliases for port pins for example Arduino based names");
      ArrayList<String> aliasIndex = new ArrayList<String>();
      aliasIndex.addAll(aliasMapping.keySet());
      Collections.sort(aliasIndex, portNameComparator);
      writeAliases(gpioHeaderFile, aliasIndex, gpio,     "digitalIO_");
      writeAliases(gpioHeaderFile, aliasIndex, analogue, "analogueIO_");
      writeAliases(gpioHeaderFile, aliasIndex, ftm,      "pwmIO_");
      writeAliases(gpioHeaderFile, aliasIndex, tpm,      "pwmIO_");
      writeCloseGroup(gpioHeaderFile);

      /* 
       * TODO - Write debug information
       */
      gpioHeaderFile.write("/*\n");
      ArrayList<String> peripheralPins = new ArrayList<String>();
      peripheralPins.addAll(portMapping.keySet());
      Collections.sort(peripheralPins, portNameComparator);
      for (String pinName:peripheralPins) {
         MuxValues muxValues = portMapping.get(pinName);
         ArrayList<String> mappings = muxValues.alternatives;
         int defaultIndex = muxValues.defaultValue;
         if ((defaultIndex>=mappings.size()) || (defaultIndex<0)) {
            System.err.println(String.format("Illegal default value = %d, for %s",defaultIndex, pinName ));
         }
         String defaultValue = mappings.get(defaultIndex);
         if (mappings.size()<=1) { 
            continue;
         }
         if ((mappings.size()<=2) && pinName.startsWith("GPIO")) {
            continue;
         }
//         if ((mappings.size()<=2) && pinName.startsWith("ADC")) {
//            continue;
//         }
         if ((mappings.size()<=2) && pinName.startsWith("SDHC")) {
            continue;
         }
         gpioHeaderFile.write(String.format("%s=%s=%s\n", pinName, defaultValue, muxValues.alternatives));
      }
      gpioHeaderFile.write("\n");

      for (String peripheral:peripheralInstances.keySet()) {
         if (peripheral.startsWith("GPIO")) {
            // GPIO don't have a clock
            // The clock controls the PORT not the GPIO!
            continue;
         }
         HashSet<String> instances = peripheralInstances.get(peripheral);
         for (String instance : instances) {
            boolean incomplete = false;
            ClockInfo cInfo  = clockInfo.get(peripheral+instance);
            if (cInfo == null) {
               incomplete = true;
               cInfo = new ClockInfo(null, null);
               clockInfo.put(peripheral+instance, cInfo);
            }
            if (cInfo.clockReg == null) {
               incomplete = true;
               cInfo.clockReg = "SIM->SCGC6";
            }
            if (cInfo.clockMask == null) {
               incomplete = true;
               cInfo.clockMask = "SIM_SCGC6_"+peripheral+instance+"_MASK";
            }
            gpioHeaderFile.write(String.format("%s=%s=%s%s\n", peripheral+instance,  cInfo.clockReg, cInfo.clockMask, incomplete?"=default":""));
         }
      }
      gpioHeaderFile.write("*/\n");
      writeHeaderFilePostamble(gpioHeaderFile, gpioBaseFileName+".h");
   }
   
   void writeMacroDefinition(BufferedWriter writer, String briefDescription, String paramDescription, String name, String value) throws IOException {
      writer.write( String.format(
      "/**\n"+
      " * @brief %s\n"+
      " *\n"+
      " * @param %s\n"+
      " */\n"+
      "#define %-20s  %s\n"+
      "\n",
      briefDescription, paramDescription, name, value
      ));
      
   }
  /**                    
   * Write CPP file      
   *                     
   * @param cppFile      
   * @throws IOException 
   */                    
   void writeGpioCppFile(BufferedWriter cppFile) throws IOException {
      String description = "Pin declarations for " + deviceName;
      writeCppFilePreable(cppFile, gpioBaseFileName+".cpp", gpioCppFileName, description);
      writeHeaderFileInclude(cppFile, "utilities.h");
      writeHeaderFileInclude(cppFile, "gpio.h");
      writeHeaderFileInclude(cppFile, "pin_mapping.h");
      cppFile.write("\n");
      if (!ftm.isEmpty()) {
         writeMacroDefinition(cppFile, 
               "Create Timer Clock register name from timer number", 
               "number Timer number e.g. 1 => FTM1_CLOCK_REG", 
               "FTM_CLOCK_REG(number)", 
               "CONCAT3_(FTM,number,_CLOCK_REG)");
         writeMacroDefinition(cppFile, 
               "Create Timer Clock register mask from timer number", 
               "number Timer number e.g. 1 => FTM1_CLOCK_MASK", 
               "FTM_CLOCK_MASK(number)", 
               "CONCAT3_(FTM,number,_CLOCK_MASK)");
      }
      if (!tpm.isEmpty()) {
         writeMacroDefinition(cppFile, 
               "Create Timer Clock register name from timer number", 
               "number Timer number e.g. 1 => TPM1_CLOCK_REG", 
               "TPM_CLOCK_REG(number)", 
               "CONCAT3_(TPM,number,_CLOCK_REG)");
         writeMacroDefinition(cppFile, 
               "Create Timer Clock register mask from timer number", 
               "number Timer number e.g. 1 => TPM1_CLOCK_MASK", 
               "TPM_CLOCK_MASK(number)", 
               "CONCAT3_(TPM,number,_CLOCK_MASK)");
      }
      if (!analogue.isEmpty()) {
         writeMacroDefinition(cppFile, 
               "Create ADC Clock register name from ADC number", 
               "number Timer number e.g. 1 => ADC1_CLOCK_REG", 
               "ADC_CLOCK_REG(number)", 
               "CONCAT3_(ADC,number,_CLOCK_REG)");
         writeMacroDefinition(cppFile, 
               "Create ADC Clock register mask from ADC number", 
               "number Timer number e.g. 1 => ADC1_CLOCK_MASK", 
               "ADC_CLOCK_MASK(number)", 
               "CONCAT3_(ADC,number,_CLOCK_MASK)");
      }
      writeMacroDefinition(cppFile, "ADC(num)", "CONCAT2_(ADC,num)");
      writeMacroDefinition(cppFile, "FTM(num)", "CONCAT2_(FTM,num)");
      writeMacroDefinition(cppFile, "TPM(num)", "CONCAT2_(TPM,num)");
      cppFile.write("\n");
      
      for (PinInformation pin:pins) {
         String pinName = pin.name;
         if (pin.getMappingList("GPIO").size() > 0) {
            if (deviceIsMKE) {
               cppFile.write(String.format("const DigitalIO %-18s = {%-18s%s};\n", 
                     "digitalIO_"+pinName, "(volatile GPIO_Type*)GPIO("+pinName+"_GPIO_NAME),", "(1UL<<"+pinName+"_GPIO_BIT)"));
            }
            else {
               cppFile.write(String.format("const DigitalIO %-18s = {%-18s%-12s%-18s%-29s%s};\n", 
                     "digitalIO_"+pinName, "&PCR("+pinName+"_GPIO_NAME,", pinName+"_GPIO_BIT),", "GPIO("+pinName+"_GPIO_NAME),", "PORT_CLOCK_MASK("+pinName+"_GPIO_NAME),", "(1UL<<"+pinName+"_GPIO_BIT)"));
            }
         }
      }
      for (PinInformation pin:pins) {
         String pinName = pin.name;
         ArrayList<MappingInfo> info = pin.getMappingList("ADC");
         for(MappingInfo adcInfo:info) {
            String adcNum   = adcInfo.name;
            String adcChNum = adcInfo.channel;
            String adcName = String.format("ADC%s_%s", adcNum, adcChNum);
            ArrayList<String> mapEntry = portMapping.get(adcName).alternatives;
            if (mapEntry.size()>2) {
               int selectionIndex = mapEntry.indexOf(pinName);
               cppFile.write(String.format(
                     "#if %s_SEL == %d\n",
                     adcInfo.getName(), selectionIndex
                     ));
            }
            if (pin.getMappingList("GPIO").size() == 0) {
               cppFile.write(String.format("const AnalogueIO %-22s = {%-18s%-20s%-31s%-31s%s};\n", 
                     "analogueIO_"+pinName, "0,", "ADC("+pinName+"_ADC_NUM),", "&ADC_CLOCK_REG("+pinName+"_ADC_NUM),", "ADC_CLOCK_MASK("+pinName+"_ADC_NUM),", pinName+"_ADC_CH"));
            }
            else {
               cppFile.write(String.format("const AnalogueIO %-22s = {%-18s%-20s%-31s%-31s%s};\n", 
                     "analogueIO_"+pinName, "&digitalIO_"+pinName+",", "ADC("+pinName+"_ADC_NUM),", "&ADC_CLOCK_REG("+pinName+"_ADC_NUM),", "ADC_CLOCK_MASK("+pinName+"_ADC_NUM),", pinName+"_ADC_CH"));
            }
            if (mapEntry.size()>2) {
               cppFile.write("#endif\n");
            }
         }
      }
      for (PinInformation pin:pins) {
         String pinName = pin.name;
         ArrayList<MappingInfo> info = pin.getMappingList("FTM");
         for(MappingInfo ftmInfo:info) {
            String ftmNum   = ftmInfo.name;
            String ftmChNum = ftmInfo.channel;
            String ftmName = String.format("FTM%s_%s", ftmNum, ftmChNum);
            ArrayList<String> mapEntry = portMapping.get(ftmName).alternatives;
            if (mapEntry.size()>2) {
               int selectionIndex = mapEntry.indexOf(pinName);
               cppFile.write(String.format(
                     "#if %s_SEL == %d\n",
                     String.format("FTM%s_%s",      ftmNum,   ftmChNum), selectionIndex
                     ));
            }
            cppFile.write(String.format("const PwmIO  %-15s = {%-19s%-40s%-15s%-28s%-31s%s};\n", 
                  "pwmIO_"+pinName, "&digitalIO_"+pinName+",", "(volatile FTM_Type*)FTM("+pinName+"_FTM_NUM),",
                  pinName+"_FTM_CH,", "PORT_PCR_MUX("+pinName+"_FTM_FN),", "&FTM_CLOCK_REG("+pinName+"_FTM_NUM),", "FTM_CLOCK_MASK("+pinName+"_FTM_NUM)"));
            if (mapEntry.size()>2) {
               cppFile.write("#endif\n");
            }
         }
      }
      for (PinInformation pin:pins) {
         String pinName = pin.name;
         ArrayList<MappingInfo> info = pin.getMappingList("TPM");
         for(MappingInfo tpmInfo:info) {
            String tpmNum   = tpmInfo.name;
            String tpmChNum = tpmInfo.channel;
            String tpmName = String.format("TPM%s_%s", tpmNum, tpmChNum);
            ArrayList<String> mapEntry = portMapping.get(tpmName).alternatives;
            if (mapEntry.size()>2) {
               int selectionIndex = mapEntry.indexOf(pinName);
               cppFile.write(String.format(
                     "#if %s_SEL == %d\n",
                     String.format("TPM%s_%s",      tpmNum,   tpmChNum), selectionIndex
                     ));
            }
            cppFile.write(String.format("const PwmIO  %-15s = {%-19s%-40s%-15s%-28s%-31s%s};\n", 
                  "pwmIO_"+pinName, "&digitalIO_"+pinName+",", "(volatile TPM_Type*)TPM("+pinName+"_TPM_NUM),",
                  pinName+"_TPM_CH,", "PORT_PCR_MUX("+pinName+"_TPM_FN),", "&TPM_CLOCK_REG("+pinName+"_TPM_NUM),", "TPM_CLOCK_MASK("+pinName+"_TPM_NUM)"));
            if (mapEntry.size()>2) {
               cppFile.write("#endif\n");
            }
         }
      }
      writeCppFilePostAmple();
   }

   void writeClockMacros(BufferedWriter writer) throws Exception {
      ArrayList<String> ar = new ArrayList<String>(clockInfo.keySet());
      Collections.sort(ar, portNameComparator);
      for (String peripheral:ar) {
         ClockInfo cInfo = clockInfo.get(peripheral);
         if (peripheral.matches("PORT[A-Z]")) {
            if (portClockRegisterValue == null) {
               portClockRegisterValue = cInfo.clockReg;
            }
            else if (!portClockRegisterValue.equals(cInfo.clockReg)) {
               throw new Exception(
                  String.format("Multiple port clock registers existing=%s, new=%s", portClockRegisterValue, cInfo.clockReg));
            }
         }
         writeMacroDefinition(writer, peripheral+"_CLOCK_REG",  cInfo.clockReg);
         writeMacroDefinition(writer, peripheral+"_CLOCK_MASK", cInfo.clockMask);
      }
//      for (String peripheral:peripheralInstances.keySet()) {
//         HashSet<String> instances = peripheralInstances.get(peripheral);
//         for (String instance : instances) {
//            ClockInfo cInfo = clockInfo.get(peripheral+instance);
//            String clockReg  = cInfo.clockReg;
//            String clockMask = cInfo.clockMask;
//            writeMacroDefinition(writer, peripheral+instance+"_CLOCK_REG",  clockReg);
//            writeMacroDefinition(writer, peripheral+instance+"_CLOCK_MASK", clockMask);
//         }
//      }
      writeMacroDefinition(writer, "PORT_CLOCK_REG", portClockRegisterValue);
      writer.write("\n");
   }

   /**
    * Process file
    * 
    * @param filePath
    * @throws Exception
    */
   void processFile(Path filePath) throws Exception {

      deviceName = filePath.getFileName().toString().replace(".csv", "");
      deviceIsMKE = deviceName.startsWith("MKE");
      deviceIsMKL = deviceName.startsWith("MKL");
      deviceIsMKM = deviceName.startsWith("MKL");

      deviceDescriptions.add(new PeripheralTypeDescription("GPIO", "GPIO", "DigitalIO_Group",  "Digital Input/Output",               "Allows use of port pins as simple digital inputs or outputs", "digitalIO_",  "extern const DigitalIO %-24s //!< DigitalIO on %s\n"));
      if (!deviceIsMKE) {
         deviceDescriptions.add(new PeripheralTypeDescription("ADC",  "ADC",  "AnalogueIO_Group", "Analogue Input",                     "Allows use of port pins as analogue inputs",                  "analogueIO_", "extern const AnalogueIO %-24s //!< AnalogueIO on %s\n"));
         deviceDescriptions.add(new PeripheralTypeDescription("FTM",  "FTM",  "PwmIO_Group",      "PWM, Input capture, Output compare", "Allows use of port pins as PWM outputs",                      "pwmIO_",      "extern const PwmIO  %-24s //!< PwmIO on %s\n"));
         deviceDescriptions.add(new PeripheralTypeDescription("TPM",  "TPM",  "PwmIO_Group",      "PWM, Input capture, Output compare", "Allows use of port pins as PWM outputs",                      "pwmIO_",      "extern const PwmIO  %-24s //!< PwmIO on %s\n"));
      }
      pinFunctionDescriptions.add(new PinFunctionDescription("LPTMR", "", ""));
      pinFunctionDescriptions.add(new PinFunctionDescription("SPI",   "", ""));
      pinFunctionDescriptions.add(new PinFunctionDescription("I2C",   "", ""));
      pinFunctionDescriptions.add(new PinFunctionDescription("SDHC",   "", ""));
      
      Path sourceDirectory = filePath.getParent().resolve("Sources");
      Path headerDirectory = filePath.getParent().resolve("Project_Headers");
      pinMappingHeaderFileName = pinMappingBaseFileName+"-"+deviceName+".h";
      gpioCppFileName          = gpioBaseFileName+"-"+deviceName+".cpp";
      gpioHeaderFileName       = gpioBaseFileName+"-"+deviceName+".h";

      System.err.println("deviceName = " + deviceName);

      BufferedReader sourceFile = Files.newBufferedReader(filePath);

      if (!sourceDirectory.toFile().exists()) {
         Files.createDirectory(sourceDirectory);
      }
      if (!headerDirectory.toFile().exists()) {
         Files.createDirectory(headerDirectory);
      }
      Path pinMappingHeaderPath = headerDirectory.resolve(pinMappingHeaderFileName);
      BufferedWriter pinMappingHeaderFile = Files.newBufferedWriter(pinMappingHeaderPath);

      Path gpioCppPath = sourceDirectory.resolve(gpioCppFileName);
      BufferedWriter gpioCppFile    = Files.newBufferedWriter(gpioCppPath);

      Path gpioHeaderPath = headerDirectory.resolve(gpioHeaderFileName);
      BufferedWriter gpioHeaderFile    = Files.newBufferedWriter(gpioHeaderPath);
      
      parseFile(sourceFile);
      writePinMappingHeaderFile(pinMappingHeaderFile);
      writeGpioHeaderFile(gpioHeaderFile);
      writeGpioCppFile(gpioCppFile);

      sourceFile.close();
      pinMappingHeaderFile.close();
      gpioCppFile.close();
      gpioHeaderFile.close();
   }

   public static void main(String[] args) throws Exception {
      DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
         @Override
         public boolean accept(Path path) throws IOException {
            return path.getFileName().toString().matches(".*\\.csv$");
         }
      };
      Path directory = Paths.get("");
      DirectoryStream<Path> folderStream = Files.newDirectoryStream(directory.toAbsolutePath(), filter);
      for (Path filePath : folderStream) {
         if (!Files.isRegularFile(filePath)) {
            continue;
         }
         CreatePinDescription creater = new CreatePinDescription();
         creater.processFile(filePath);
      }
   }
}
