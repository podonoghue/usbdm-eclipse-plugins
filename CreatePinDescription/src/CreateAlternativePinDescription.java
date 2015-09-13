import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateAlternativePinDescription extends DocumentUtilities {

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
   static class PinInformation {
      public String name;                          // Name of the pin, usually the port name e.g. PTA1
      public String description;                   // Description of pin functions e.g. // PTA1 = PTA1,FTM0.6
      public Vector<MappingInfo> peripheralNames;  // Peripherals mappable to this pin
      
      public PinInformation(String name) {
         this.name = name;
         peripheralNames = new Vector<MappingInfo>();
      }
   };
   static class MuxValues {
      Vector<String> alternatives;
      int defaultValue;
      MuxValues() {
         defaultValue = 0;
         alternatives = new Vector<String>();
         alternatives.add("Disabled");
      }
      void setDefaultValue(int defaultValue) {
         this.defaultValue = defaultValue;
      }
   };
   Vector<PinInformation> pins = new Vector<CreateAlternativePinDescription.PinInformation>();
   HashMap<String, MuxValues> portMapping = new HashMap<String, MuxValues>();
   //! Mapping of Alias pins to Kinetis pins
   HashMap<String, String> aliasMapping = new HashMap<String, String>(); 
   //! Mapping of Kinetis pins to Alias pins
   HashMap<String, String> reverseAliasMapping = new HashMap<String, String>();
   Vector<String> analogue = new Vector<String>();
   Vector<String> ftm      = new Vector<String>();
   Vector<String> tpm      = new Vector<String>();

   final static String pinMappingBaseFileName   = "PinMapping";
   final static String gpioBaseFileName         = "GPIO";
   String deviceName;

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
         System.err.println("OPPS- looking in " + portMapping);
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
      reverseAliasMapping.put(mappedName, aliasName);
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
    * @param elements      List to add discovered ports to
    * @param pinNames      List to add pin names to add 
    */
   void doPinPeripheral(String[] line, StringBuilder description, String template, Vector<MappingInfo> elements, Vector<String> pinNames) {
      String pinName  = line[pinIndex];
      Pattern pattern = Pattern.compile(template);
      for (int col=altStartIndex; col<line.length; col++) {
         Matcher matcher = pattern.matcher(line[col]);
         if (matcher.matches()) {
            String baseName      = matcher.group(1);
            String peripheralNum = matcher.group(2);
            String signalName    = matcher.group(3);
            int peripheralFn     = col-altStartIndex;
            String peripheralSignalName = String.format("%s%s_%s", baseName, peripheralNum, signalName);
            MuxValues mapEntry = portMapping.get(peripheralSignalName);
            if (mapEntry == null) {
               // Add new entry
               mapEntry = new MuxValues();
               portMapping.put(peripheralSignalName, mapEntry);
            }
            mapEntry.alternatives.add(pinName);
            if (pinNames != null) {
               pinNames.add(pinName);
            }
            elements.addElement(new MappingInfo(baseName, peripheralNum, signalName, peripheralFn));
            if (peripheralSignalName.startsWith("PT")) {
               continue;
            }
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
      StringBuilder description = new StringBuilder();
      PinInformation pinInfo = new PinInformation(pinName);
      pins.add(pinInfo);
      doPinPeripheral(line, description, "^.*(PT)([A-Z])(\\d*)(/.*|$)",     pinInfo.peripheralNames, null);
      doPinPeripheral(line, description, "^.*(ADC)(\\d*)_SE(\\d*)(b.*|/.*|$)", pinInfo.peripheralNames, analogue);
      doPinPeripheral(line, description, "^.*(FTM)(\\d*)_CH(\\d*)(/.*|$)",     pinInfo.peripheralNames, ftm);
      doPinPeripheral(line, description, "^.*(TPM)(\\d*)_CH(\\d*)(/.*|$)",     pinInfo.peripheralNames, tpm);
      doPinPeripheral(line, description, "^.*(SDHC)(\\d*)_((CLKIN)|(D\\d)|(CMD)|(DCLK))(/.*|$)", pinInfo.peripheralNames, null);
      doPinPeripheral(line, description, "^.*(SPI)(\\d*)_((SOUT)|(SIN)|(SCK)|(PCS\\d)|(MOSI)|(MISO))(/.*|$)", pinInfo.peripheralNames, null);
      doPinPeripheral(line, description, "^.*(I2C)(\\d*)_((SDA)|(SCL))(/.*|$)", pinInfo.peripheralNames, null);
      pinInfo.description = pinName;
      if (description.length() > 0) {
         pinInfo.description += " = "+description.toString();
      }
   }
   /**
    * Writes macros describing pin functions
    * 
    * @param pinInfo    Pin to describe
    * @param headerFile Header file to write to
    * 
    * @throws Exception 
    */
   void processPinDefines(PinInformation pinInfo, BufferedWriter headerFile) throws Exception {
      String aliasName = reverseAliasMapping.get(pinInfo.name);
      String description = pinInfo.description;
      if (aliasName != null) {
         description += " (Alias: " + aliasName + ")";
      }
      headerFile.append(description + "\n");
      String pinName  = pinInfo.name;
      String result      = "";
      if (pinInfo.peripheralNames.size()>0) {
         for(MappingInfo peripheralInfo:pinInfo.peripheralNames) {
            if (peripheralInfo.baseName.equals("PT")) {
               // Port description
               String portName = peripheralInfo.name;
               String portNum  = peripheralInfo.channel;
//               int    portFn   = peripheralInfo.mux; // always 1
               result += String.format(
                     "#define %-18s         %-3s   //!< %s Port name\n"+
                           "#define %-18s         %-3s   //!< %s Port number\n",
                           String.format("%s_PORT", pinName), portName, pinName, 
                           String.format("%s_NUM", pinName),  portNum,  pinName
                     );
            }
            else if (peripheralInfo.baseName.equals("ADC")) {
               String adcNum   = peripheralInfo.name;
               String adcChNum = peripheralInfo.channel;
               int    adcFn    = peripheralInfo.mux; // always 0
               String adcName  = peripheralInfo.getName();
               MuxValues muxValues = portMapping.get(adcName);
               if (muxValues == null) {
                  throw new Exception("Failed to find "+adcName);
               }
               Vector<String> mapEntry = muxValues.alternatives;
               if (mapEntry.size()>2) {
                  int selectionIndex = mapEntry.indexOf(pinName);
                  result += String.format(
                        "#if %s_SEL == %d\n",
                        String.format("ADC%s_%s",      adcNum,   adcChNum), selectionIndex
                        );
               }
               result += String.format(
                     "#define %-18s         %-3s   //!< %s ADC number\n"+
                           "#define %-18s         %-3s   //!< %s ADC channel\n",
                           String.format("%s_ADC_NUM", pinName), adcNum,   pinName, 
                           String.format("%s_ADC_CH",  pinName), adcChNum, pinName
                     );
               if (adcFn != 0) {
                  result += String.format(
                        "#define %-18s         %-3d   //!< %s Pin multiplexor for ADC\n",
                        String.format("%s_ADC_FN",  pinName), adcFn,    pinName
                        );
                  throw new Exception("ADC Function must be on MUX=0");
               }
               if (mapEntry.size()>2) {
                  result += "#endif\n\n";
               }
            }
            else if (peripheralInfo.baseName.equals("FTM")) {
               String ftmNum   = peripheralInfo.name;
               String ftmChNum = peripheralInfo.channel;
               int    ftmFn    = peripheralInfo.mux;
               String ftmName  = peripheralInfo.getName();
               Vector<String> mapEntry = portMapping.get(ftmName).alternatives;
               if (mapEntry.size()>1) {
                  int selectionIndex = mapEntry.indexOf(pinName);
                  result += String.format(
                        "#if %s_SEL == %d\n",
                        ftmName, selectionIndex
                        );
               }
               result += String.format(
                     "#define %-18s         %-3s   //!< %s FTM number\n"+
                           "#define %-18s         %-3s   //!< %s FTM channel\n" +
                           "#define %-18s         %-3d   //!< %s Pin multiplexor for FTM\n",
                           String.format("%s_FTM_NUM", pinName), ftmNum,   pinName, 
                           String.format("%s_FTM_CH",  pinName), ftmChNum, pinName, 
                           String.format("%s_FTM_FN",  pinName), ftmFn,    pinName
                     );
               if (mapEntry.size()>1) {
                  result += "#endif\n";
               }
            }
            else if (peripheralInfo.baseName.equals("TPM")) {
               String tpmNum   = peripheralInfo.name;
               String tpmChNum = peripheralInfo.channel;
               int    tpmFn    = peripheralInfo.mux;
               String tpmName  = peripheralInfo.getName();
               Vector<String> mapEntry = portMapping.get(tpmName).alternatives;
               if (mapEntry.size()>1) {
                  int selectionIndex = mapEntry.indexOf(pinName);
                  result += String.format(
                        "#if %s_SEL == %d\n",
                        String.format("TPM%s_%s",      tpmNum,   tpmChNum), selectionIndex
                        );
               }
               result += String.format(
                     "#define %-18s         %-3s   //!< %s TPM number\n"+
                           "#define %-18s         %-3s   //!< %s TPM channel\n" +
                           "#define %-18s         %-3d   //!< %s Pin multiplexor for TPM\n",
                           String.format("%s_TPM_NUM", pinName), tpmNum,   pinName, 
                           String.format("%s_TPM_CH",  pinName), tpmChNum, pinName, 
                           String.format("%s_TPM_FN",  pinName), tpmFn,    pinName
                     );
               if (mapEntry.size()>1) {
                  result += "#endif\n";
               }
            }
            else if (peripheralInfo.baseName.equals("SPI")) {
               String spiNum        = peripheralInfo.name;
               String spiSignalName = peripheralInfo.channel;
               int spiFn            = peripheralInfo.mux;
               String spiName       = peripheralInfo.getName();
               Vector<String> mapEntry = portMapping.get(spiName).alternatives;
               if (mapEntry.size()>1){
                  int selectionIndex = mapEntry.indexOf(pinName);
                  result += String.format(
                        "#if %s_SEL == %d\n",
                        String.format("SPI%s_%s",      spiNum,   spiSignalName), selectionIndex
                        );
               }
               result += String.format(
                     "#define %-18s         %-3d   //!< %s Pin multiplexor for SPI\n"+
                           "#define %-18s         digitalIO_%-2s  //!< %s = SPI\n",
                           String.format("SPI%s_%s_FN",   spiNum,   spiSignalName), spiFn, pinName,
                           String.format("SPI%s_%s_GPIO", spiNum,   spiSignalName), pinName, pinName
                     );
               if (mapEntry.size()>1){
                  result += "#endif\n";
               }
            }
            else if (peripheralInfo.baseName.equals("I2C")) {
               String i2cNum        = peripheralInfo.name;
               String i2cSignalName = peripheralInfo.channel;
               int i2cFn            = peripheralInfo.mux;
               String i2cName       = peripheralInfo.getName();
               Vector<String> mapEntry = portMapping.get(i2cName).alternatives;
               if (mapEntry.size()>1) {
                  int selectionIndex = mapEntry.indexOf(pinName);
                  result += String.format(
                        "#if %s_SEL == %d\n",
                        String.format("I2C%s_%s",      i2cNum,   i2cSignalName), selectionIndex
                        );
               }
               result += String.format(
                     "#define %-18s         %-3d   //!< Pin multiplexor for I2C\n"+
                           "#define %-18s         digitalIO_%-2s  //!< %s = I2C\n",
                           String.format("I2C%s_%s_FN",   i2cNum,   i2cSignalName), i2cFn, 
                           String.format("I2C%s_%s_GPIO", i2cNum,   i2cSignalName), pinName, pinName
                     );
               if (mapEntry.size()>1) {
                  result += "#endif\n";
               }
            }
            else if (peripheralInfo.baseName.equals("SDHC")) {
               String sdhcNum        = peripheralInfo.name;
               String sdhcSignalName = peripheralInfo.channel;
               int sdhcFn            = peripheralInfo.mux;
               String sdhcName       = peripheralInfo.getName();
               Vector<String> mapEntry = portMapping.get(sdhcName).alternatives;
               if (mapEntry.size()>2){
                  int selectionIndex = mapEntry.indexOf(pinName);
                  result += String.format(
                        "#if %s_SEL == %d\n",
                        String.format("SDHC%s_%s",      sdhcNum,   sdhcSignalName), selectionIndex
                        );
               }
               result += String.format(
                     "#define %-18s         %-3d   //!< %s Pin multiplexor for SDHC\n"+
                           "#define %-18s         digitalIO_%-2s  //!< %s = SDHC\n",
                           String.format("SDHC%s_%s_FN",   sdhcNum,   sdhcSignalName), sdhcFn, pinName,
                           String.format("SDHC%s_%s_GPIO", sdhcNum,   sdhcSignalName), pinName, pinName
                     );
               if (mapEntry.size()>2){
                  result += "#endif\n";
               }
            }
            result += "\n";
         }
      }
      headerFile.append(result);
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
      Vector<String> portMap = muxValues.alternatives;
      if ((portMap.size() <= 2) && peripheralPin.startsWith("PT")) {
         return;
      }
//      if ((portMap.size() <= 2) && peripheralPin.startsWith("ADC")) {
//         return;
//      }
      if ((portMap.size() <= 2) && peripheralPin.startsWith("SDHC")) {
         return;
      }
      String description = peripheralPin + " maps to " + portMap;

      StringBuilder sb = new StringBuilder();
      for(String mappedPin:portMap) {
         if (mappedPin.equals("Disabled")) {
            continue;
         }
         if (sb.length()>0) {
            sb.append(", ");
         }
         String aliasName = reverseAliasMapping.get(mappedPin);
         if (aliasName != null) {
            mappedPin += "(" + aliasName + ")";
         }
         sb.append(mappedPin);
      }
      writeWizardOptionSelectionPreamble(writer, 
            description,
            0,
            false,
            String.format("%s Pin Selection [%s]",             peripheralPin, sb),
            String.format("Selects which pin is used for %s", peripheralPin));
      
      int selection = 0;
      int defaultSelection = muxValues.defaultValue;
      for(String mappedPin:portMap) {
         String aliasName = reverseAliasMapping.get(mappedPin);
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
   void processPeripheralPinMappings(BufferedWriter writer) throws IOException {
      tryIt(writer);
      ArrayList<String> peripheralPins = new ArrayList<String>();
      peripheralPins.addAll(portMapping.keySet());
      Collections.sort(peripheralPins);
      for (String s:peripheralPins) {
         processPeripheralPinMapping(s, writer);
      }
   }

   void tryIt(BufferedWriter writer) throws IOException {
      for (PinInformation p:pins) {
         if (p.peripheralNames.size()<2) {
            continue;
         }
         StringBuilder sb = new StringBuilder();
         for (MappingInfo m:p.peripheralNames) {
            if (sb.length()>0) {
               sb.append(", ");
            }
            sb.append(m.getName());
         }
         writeWizardOptionSelectionPreamble(writer, 
               p.description,
               0,
               false,
               String.format("%s Pin Selection [%s]",            p.name, sb.toString()),
               String.format("Selects what function is mapped to %s", p.name));
         int selection = 0;
         int defaultSelection = 0;
         for (MappingInfo m:p.peripheralNames) {
            writeWizardOptionSelectionEnty(writer, Integer.toString(selection++), m.getName());
         }
         writeWizardDefaultSelectionEnty(writer, Integer.toString(defaultSelection));
         writeMacroDefinition(writer, p.name+"_SEL", Integer.toString(defaultSelection));
         writer.write("\n");
      }
   }

   /**
    * Comparator for sorting by port names
    */
   static Comparator<String> portNameComparator = new Comparator<String>() {
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
    * Comparator for lines based on the port names in [1]
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
      Vector<String[]> grid = new Vector<String[]>();
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
         parseDefaultLine(line);
      }
      for(String[] line:grid) {
         if (line.length < 2) {
            continue;
         }
         parseAliasLine(line);
      }
   }

   /**
    * Writes macros describing pin functions for all pins
    * 
    * @param headerFile Header file to write to
    * 
    * @throws Exception 
    */
   void processDefines(BufferedWriter headerFile) throws Exception {
      for(PinInformation p : pins) {
         processPinDefines(p, headerFile);
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
      writeHeaderFilePreamble(headerFile, pinMappingBaseFileName+".h", "", VERSION, "Pin declarations for "+deviceName);
      writeHeaderFileInclude(headerFile, "derivative.h");
      headerFile.write("\n");
      writeWizardMarker(headerFile);
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
         writeMacroDefinition(headerFile, "FTM_SC", "(FTM_SC_CLKS(0x1)|FTM_SC_PS(0x0))");
         headerFile.write("\n");
         writeWizardSectionClose(headerFile);
//         headerFile.write( String.format(optionSectionClose));
      }
      if (tpm.size()>0) {
         writeWizardSectionOpen(headerFile, "TPM Clock settings");
//         headerFile.write( String.format(optionSectionOpenTemplate, "TPM Clock settings"));
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
         writeMacroDefinition(headerFile, "TPM_SC", "(TPM_SC_CMOD(0x9)|TPM_SC_PS(0x0))");
         headerFile.write("\n");
         writeWizardSectionClose(headerFile);
      }
      writeWizardSectionOpen(headerFile, "Pin Peripheral mapping");
      processPeripheralPinMappings(headerFile);
      processDefines(headerFile);
      writeWizardSectionClose(headerFile);
      writeHeaderFilePostamble(headerFile, pinMappingBaseFileName+".h");
   }

   /**
    * Writes aliases
    * 
    * @param writer
    * @param aliasIndex
    * @param analogue2
    * @param prefix
    * @throws IOException
    */
   void writeAliases(BufferedWriter writer, ArrayList<String> aliasIndex, Vector<String> analogue2, String prefix) throws IOException {
      for (String aliasName:aliasIndex) {
         String mappedName = aliasMapping.get(aliasName);
         if ((analogue2==null) || analogue2.contains(mappedName)) {
            writer.write( String.format(
                  "#define %-20s %-20s %s\n", prefix+aliasName, prefix+mappedName, "//!< alias "+aliasName+"=>"+mappedName ));
         }
      }
   }
   
   /**
    * Write GPIO Header file
    * 
    * @param gpioHeaderFile
    * @throws IOException
    */
   private void writeGpioHeaderFile(BufferedWriter gpioHeaderFile) throws IOException {
      writeHeaderFilePreamble(gpioHeaderFile, "GPIO.h", "", VERSION, "Pin declarations for "+deviceName);
      writeHeaderFileInclude(gpioHeaderFile, "derivative.h");
      writeHeaderFileInclude(gpioHeaderFile, "PinMapping.h");
      writeHeaderFileInclude(gpioHeaderFile, "GPIO_defs.h");
      gpioHeaderFile.write("\n");
      writeStartGroup(gpioHeaderFile, "DigitalIO_Group", "Digital Input/Output", "Allows use of port pins as simple digital inputs or outputs");
      for (PinInformation pin:pins) {
         String pinName = pin.name;
         for (MappingInfo map:pin.peripheralNames) {
            if (map.baseName.equals("PT")) {
               gpioHeaderFile.write(String.format("extern const DigitalIO %-24s //!< Digital I/O on %s\n", "digitalIO_"+pinName+";", pinName));
            }
         }
      }
      writeCloseGroup(gpioHeaderFile);

      writeStartGroup(gpioHeaderFile, "AnalogueIO_Group", "Analogue Input", "Allows use of port pins as analogue inputs");
      for (PinInformation pin:pins) {
         String pinName = pin.name;
         for (MappingInfo map:pin.peripheralNames) {
            if (map.baseName.equals("ADC")) {
               String adcNum   = map.name;
               String adcChNum = map.channel;
               String adcName = String.format("ADC%s_%s", adcNum, adcChNum);
               Vector<String> mapEntry = portMapping.get(adcName).alternatives;
               if (mapEntry.size()>2) {
                  int selectionIndex = mapEntry.indexOf(pinName);
                  gpioHeaderFile.write(String.format(
                        "#if %s_SEL == %d\n",
                        String.format("ADC%s_%s",      adcNum,   adcChNum), selectionIndex
                        ));
               }
               gpioHeaderFile.write(String.format("extern const AnalogueIO %-24s //!< %s on %s\n", "analogueIO_"+pinName+";", adcName, pinName));
               if (mapEntry.size()>2) {
                  gpioHeaderFile.write("#endif\n");
               }
            }
         }
      }
      writeCloseGroup(gpioHeaderFile);

      writeStartGroup(gpioHeaderFile, "PwmIO_Group", "PWM, Input capture, Output compare", "Allows use of port pins as PWM outputs");
      for (PinInformation pin:pins) {
         String pinName = pin.name;
         for (MappingInfo map:pin.peripheralNames) {
            if (map.baseName.equals("FTM")) {
               String ftmNum   = map.name;
               String ftmChNum = map.channel;
               String ftmName = String.format("FTM%s_%s", ftmNum, ftmChNum);
               Vector<String> mapEntry = portMapping.get(ftmName).alternatives;
               if (mapEntry.size()>1) {
                  int selectionIndex = mapEntry.indexOf(pinName);
                  gpioHeaderFile.write(String.format(
                        "#if %s_SEL == %d\n",
                        String.format("FTM%s_%s",      ftmNum,   ftmChNum), selectionIndex
                        ));
               }
               gpioHeaderFile.write(String.format("extern const PwmIO  %-24s //!< %s on %s\n", "pwmIO_"+pinName+";", ftmName, pinName));
               if (mapEntry.size()>1) {
                  gpioHeaderFile.write("#endif\n");
               }
            }
         }
      }
      for (PinInformation pin:pins) {
         String pinName = pin.name;
         for (MappingInfo map:pin.peripheralNames) {
            if (map.baseName.equals("TPM")) {
               String tpmNum   = map.name;
               String tpmChNum = map.channel;
               String tpmName = String.format("TPM%s_%s", tpmNum, tpmChNum);
               Vector<String> mapEntry = portMapping.get(tpmName).alternatives;
               if (mapEntry.size()>1) {
                  int selectionIndex = mapEntry.indexOf(pinName);
                  gpioHeaderFile.write(String.format(
                        "#if %s_SEL == %d\n",
                        String.format("TPM%s_%s",      tpmNum,   tpmChNum), selectionIndex
                        ));
               }
               gpioHeaderFile.write(String.format("extern const PwmIO  %-24s //!< %s on %s\n", "pwmIO_"+pinName+";", tpmName, pinName));
               if (mapEntry.size()>1) {
                  gpioHeaderFile.write("#endif\n");
               }
            }
         }
      }
      writeCloseGroup(gpioHeaderFile);

      /*
       * Write aliases
       */
      writeStartGroup(gpioHeaderFile, "alias_pin_mappings_GROUP", "Aliases for pins", "Aliases for port pins for example Arduino based names");
      ArrayList<String> aliasIndex = new ArrayList<String>();
      aliasIndex.addAll(aliasMapping.keySet());
      Collections.sort(aliasIndex, portNameComparator);
      writeAliases(gpioHeaderFile, aliasIndex, null, "digitalIO_");
      writeAliases(gpioHeaderFile, aliasIndex, analogue, "analogueIO_");
      writeAliases(gpioHeaderFile, aliasIndex, ftm, "pwmIO_");
      writeAliases(gpioHeaderFile, aliasIndex, tpm, "pwmIO_");
      writeCloseGroup(gpioHeaderFile);

      /* 
       * XXX - Write debug information
       */
      gpioHeaderFile.write("/*\n");
      ArrayList<String> peripheralPins = new ArrayList<String>();
      peripheralPins.addAll(portMapping.keySet());
      Collections.sort(peripheralPins);
      for (String pinName:peripheralPins) {
         MuxValues muxValues = portMapping.get(pinName);
         Vector<String> mappings = muxValues.alternatives;
         int defaultIndex = muxValues.defaultValue;
         if ((defaultIndex>=mappings.size()) || (defaultIndex<0)) {
            System.err.println(String.format("Illegal default value = %d, for %s",defaultIndex, pinName ));
         }
         String defaultValue = mappings.get(defaultIndex);
         if (mappings.size()<=1) { 
            continue;
         }
         if ((mappings.size()<=2) && pinName.startsWith("PT")) {
            continue;
         }
         if ((mappings.size()<=2) && pinName.startsWith("ADC")) {
            continue;
         }
         if ((mappings.size()<=2) && pinName.startsWith("SDHC")) {
            continue;
         }
         gpioHeaderFile.write(String.format("%s=%s=%s\n", pinName, defaultValue, muxValues.alternatives));
      }
      gpioHeaderFile.write("*/\n");
      writeHeaderFilePostamble(gpioHeaderFile, gpioBaseFileName+".h");
   }

   static final String cppFilePreambleTemplate = 
         " /**\n"+
               "  * @file     %s\n"+
               "  *\n"+
               "  * @brief   Pin declarations for %s\n"+ 
               "  */\n"+
               "\n"+
               "#include \"utilities.h\"\n"+
               "#include \"GPIO.h\"\n"+
               "\n"+
               "#define ADC(num) CONCAT2_(ADC,num)\n"+
               "#define FTM(num) CONCAT2_(FTM,num)\n"+
               "#define TPM(num) CONCAT2_(TPM,num)\n"+
               "\n";

   /**
    * Write CPP file
    * 
    * @param cppFile
    * @throws IOException
    */
   void writeGpioCppFile(BufferedWriter cppFile) throws IOException {
      cppFile.write( String.format(cppFilePreambleTemplate, gpioBaseFileName+".cpp", deviceName ));
      if (pins.size()>0) {
         cppFile.write("\n");
      }
      for (PinInformation pin:pins) {
         String pinName = pin.name;
         for (MappingInfo map:pin.peripheralNames) {
            if (map.baseName.equals("PT")) {
               cppFile.write(String.format("const DigitalIO %-18s = {%-18s%-12s%-18s%-29s%s};\n", 
                     "digitalIO_"+pinName, "&PCR("+pinName+"_PORT,", pinName+"_NUM),", "GPIO("+pinName+"_PORT),", "PORT_CLOCK_MASK("+pinName+"_PORT),", "(1UL<<"+pinName+"_NUM)"));
            }
         }
      }
//      for (PinInformation pin:pins) {
//         String pinName = pin.name;
//         for (MappingInfo map:pin.peripheralNames) {
//            if (map.baseName.equals("ADC")) {
//               
//            }
//         }
//      }
//      for (PinInformation pin:pins) {
//         String pinName = pin.name;
//         for (MappingInfo map:pin.peripheralNames) {
//            if (map.baseName.equals("PT")) {
//            }
//         }
//      }
//      for (PinInformation pin:pins) {
//         String pinName = pin.name;
//         for(MappingInfo adcInfo:pin.adcNames) {
//            String adcNum   = adcInfo.name;
//            String adcChNum = adcInfo.channel;
//            String adcName = String.format("ADC%s_%s", adcNum, adcChNum);
//            Vector<String> mapEntry = portMapping.get(adcName).alternatives;
//            if (mapEntry.size()>2) {
//               int selectionIndex = mapEntry.indexOf(pinName);
//               cppFile.write(String.format(
//                     "#if %s_SEL == %d\n",
//                     String.format("ADC%s_%s",      adcNum,   adcChNum), selectionIndex
//                     ));
//            }
//            if (pin.peripeheralNames.size() == 0) {
//               cppFile.write(String.format("const AnalogueIO %-22s = {%-18s%-20s%s};\n", 
//                     "analogueIO_"+pinName, "0,", "ADC("+pinName+"_ADC_NUM),", pinName+"_ADC_CH"));
//            }
//            else {
//               cppFile.write(String.format("const AnalogueIO %-22s = {%-18s%-20s%s};\n", 
//                     "analogueIO_"+pinName, "&digitalIO_"+pinName+",", "ADC("+pinName+"_ADC_NUM),", pinName+"_ADC_CH"));
//            }
//            if (mapEntry.size()>2) {
//               cppFile.write("#endif\n");
//            }
//         }
//      }
//      for (PinInformation pin:pins) {
//         String pinName = pin.name;
//         for(MappingInfo ftmInfo:pin.ftmNames) {
//            String ftmNum   = ftmInfo.name;
//            String ftmChNum = ftmInfo.channel;
//            String ftmName = String.format("FTM%s_%s", ftmNum, ftmChNum);
//            Vector<String> mapEntry = portMapping.get(ftmName).alternatives;
//            if (mapEntry.size()>1) {
//               int selectionIndex = mapEntry.indexOf(pinName);
//               cppFile.write(String.format(
//                     "#if %s_SEL == %d\n",
//                     String.format("FTM%s_%s",      ftmNum,   ftmChNum), selectionIndex
//                     ));
//            }
//            cppFile.write(String.format("const PwmIO  %-15s = {%-19s%-21s%-15s%s};\n", 
//                  "pwmIO_"+pinName, "&digitalIO_"+pinName+",", "(volatile FTM_Type*)FTM("+pinName+"_FTM_NUM),", pinName+"_FTM_CH,", "PORT_PCR_MUX("+pinName+"_FTM_FN)"));
//            if (mapEntry.size()>1) {
//               cppFile.write("#endif\n");
//            }
//         }
//      }
//      for (PinInformation pin:pins) {
//         String pinName = pin.name;
//         for(MappingInfo tpmInfo:pin.tpmNames) {
//            String tpmNum   = tpmInfo.name;
//            String tpmChNum = tpmInfo.channel;
//            String tpmName = String.format("TPM%s_%s", tpmNum, tpmChNum);
//            Vector<String> mapEntry = portMapping.get(tpmName).alternatives;
//            if (mapEntry.size()>1) {
//               int selectionIndex = mapEntry.indexOf(pinName);
//               cppFile.write(String.format(
//                     "#if %s_SEL == %d\n",
//                     String.format("TPM%s_%s",      tpmNum,   tpmChNum), selectionIndex
//                     ));
//            }
//            cppFile.write(String.format("const PwmIO  %-15s = {%-19s%-21s%-15s%s};\n", 
//                  "pwmIO_"+pinName, "&digitalIO_"+pinName+",", "(volatile TPM_Type*)TPM("+pinName+"_TPM_NUM),", pinName+"_TPM_CH,", "PORT_PCR_MUX("+pinName+"_TPM_FN)"));
//            if (mapEntry.size()>1) {
//               cppFile.write("#endif\n");
//            }
//         }
//      }
   }

   /**
    * Process file
    * 
    * @param filePath
    * @throws Exception
    */
   void processFile(Path filePath) throws Exception {

      deviceName = filePath.getFileName().toString().replace(".csv", "");
      Path sourceDirectory = filePath.getParent().resolve("Sources");
      Path headerDirectory = filePath.getParent().resolve("Project_Headers");
      String pinMappingHeaderFileName = pinMappingBaseFileName+"-"+deviceName+".h";
      String gpioCppFileName          = gpioBaseFileName+"-"+deviceName+".cpp";
      String gpioHeaderFileName       = gpioBaseFileName+"-"+deviceName+".h";

      System.err.println("deviceName = " + deviceName);

      BufferedReader sourceFile = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);

      if (!sourceDirectory.toFile().exists()) {
         Files.createDirectory(sourceDirectory);
      }
      if (!headerDirectory.toFile().exists()) {
         Files.createDirectory(headerDirectory);
      }
      Path pinMappingHeaderPath = headerDirectory.resolve(pinMappingHeaderFileName);
      BufferedWriter pinMappingHeaderFile = Files.newBufferedWriter(pinMappingHeaderPath, StandardCharsets.UTF_8);

      Path gpioCppPath = sourceDirectory.resolve(gpioCppFileName);
      BufferedWriter gpioCppFile    = Files.newBufferedWriter(gpioCppPath, StandardCharsets.UTF_8);

      Path gpioHeaderPath = headerDirectory.resolve(gpioHeaderFileName);
      BufferedWriter gpioHeaderFile    = Files.newBufferedWriter(gpioHeaderPath, StandardCharsets.UTF_8);
      
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
         CreateAlternativePinDescription creater = new CreateAlternativePinDescription();
         creater.processFile(filePath);
      }
   }
}
