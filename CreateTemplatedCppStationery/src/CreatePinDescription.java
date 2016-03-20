import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreatePinDescription extends DocumentUtilities {

   public static final String VERSION = "1.2.0";

   /** Base name for pin mapping file */
   private final static String pinMappingBaseFileName   = "pin_mapping";
   
   /** Base name for gpio files */
   private final static String gpioBaseFileName         = "gpio";
   
   /** Name of the source file e.g. MKL25Z4.csv */
   private String sourceName;
   
   /** Name of the device e.g. MKL25Z4 */
   private String deviceName;
   
   /** Name of pin-mapping-XX.h header file */
   private String pinMappingHeaderFileName;

   /** Name of gpio-XX.cpp source file */
   private String gpioCppFileName;

   /** Path to 'Source' output directory */
   private Path sourceDirectory;
   
   /** Path to 'Project_Headers' output directory */
   private Path headerDirectory;

   /** Fixed GPIO mux function */
   private int      gpioFunctionMuxValue          = 1; 

   /** GPIO mux function varies with port */
   private boolean  gpioFunctionMuxValueChanged   = false;
   
   /** Fixed ADC mux function - default to mux setting 0*/
   private int      adcFunctionMuxValue           = 0;

   /** GPIO ADC function varies with port */
   private boolean  adcFunctionMuxValueChanged    = false;
   
   /** Fixed PORT clock enable register */
   private String   portClockRegisterValue        = "SCGC5";

   /** PORT clock enable register varies with port */
   private boolean  portClockRegisterChanged      = false;

   /** Name for namespace to use */
   public static final String NAME_SPACE = "USBDM";

   /** Name used to protect Namespace usage */
   public static final String NAMESPACES_GUARD_STRING = "USBDM_USE_NAMESPACES";

   HashSet<String> macroAliases;
   ArrayList<DmaInfo> dmaInfoList;
   
   public static class NameAttribute implements WizardAttribute {
      private String fName;
      
      NameAttribute(String name) {
         fName = name;
      }
      
      @Override
      public String getAttributeString() {
         return "<name=" + fName + ">";
      }
   }

   public static class ValidatorAttribute implements WizardAttribute {
      private String fValidatorId;
      
      ValidatorAttribute(String validatorId) {
         fValidatorId = validatorId;
      }
      
      @Override
      public String getAttributeString() {
         return "<validate=" + fValidatorId + ">";
      }
   }

   public static class SelectionAttribute implements WizardAttribute {
      private String fName;
      private String fSelection;
      
      SelectionAttribute(String name, String selection) {
         fName  = name;
         fSelection = selection;
      }
      
      @Override
      public String getAttributeString() {
         return "<selection=" + fName + "," + fSelection + ">";
      }
   }

   /**
    * Comparator for port names e.g. PTA13 c.f. PTB12<br>
    * Treats the number separately as a number.
    */
   private static Comparator<String> portNameComparator = new Comparator<String>() {
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
    * Compares two lines based upon the Port name in line[0]
    */
   private static Comparator<String[]> LineComparitor = new Comparator<String[]>() {
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

   String convertName(String pinText) {
      pinText = pinText.replaceAll("PTA", "GPIOA_");
      pinText = pinText.replaceAll("PTB", "GPIOB_");
      pinText = pinText.replaceAll("PTC", "GPIOC_");
      pinText = pinText.replaceAll("PTD", "GPIOD_");
      pinText = pinText.replaceAll("PTE", "GPIOE_");
      return pinText;
   }
   
   /**
    * Create a list of peripheral functions described by a string
    * 
    * @param pinText Text of function names e.g. <b><i>PTA4/LLWU_P3</b></i>
    * 
    * @return List of functions created
    * 
    * @throws Exception
    */
   ArrayList<PeripheralFunction> createFunctionsFromString(String pinText, Boolean convert) throws Exception {
      ArrayList<PeripheralFunction> peripheralFunctionList = new ArrayList<PeripheralFunction>();
      pinText = pinText.trim();
      if (pinText.isEmpty()) {
         return peripheralFunctionList;
      }
      if (convert) {
         pinText = convertName(pinText);
      }
      String[] functions = pinText.split("\\s*/\\s*");
      for (String function:functions) {
         function = function.trim();
         if (function.isEmpty()) {
            continue;
         }
         PeripheralFunction peripheralFunction = PeripheralFunction.createPeripheralFunction(function);
         if (peripheralFunction != null) {
            peripheralFunctionList.add(peripheralFunction);
         }
      }
      return peripheralFunctionList;
   }

   /**
    * Parses aliases information and adds to given pin
    * 
    * @param pinInformation   Pin to add to
    * @param aliases          Aliases (names separated by '/')
    */
   private void parseAlias(PinInformation pinInformation, String aliases) throws Exception {
      if ((aliases == null) || (aliases.length() == 0)) {
         return;
      }
      for (String aliasName:aliases.split("/")) {
         Aliases.addAlias(pinInformation, aliasName);
      }
   }
   
   class AliasInfo {
      final public String  name;
      final public int     index;
      
      public AliasInfo(String name, int index) {
         this.name         = name;
         this.index        = index;
      }
   }
   /** Index of Pin name column in CSV file */
   int pinIndex       = 1;
   /** List of all indices of alias columns in CSV file */
   ArrayList<AliasInfo> aliasIndexes = new ArrayList<AliasInfo>();
   /** Index of current alias column in CSV file */
   int aliasIndex     = 2;
   /** Index of reset function column in CSV file */
   int resetIndex     = 3;
   /** Index of default function column in CSV file */
   int defaultIndex   = 4;
   /** Start index of multiplexor function columns in CSV file */
   int altStartIndex  = 5;
   /** Last index of multiplexor function columns in CSV file */
   int altEndIndex    = altStartIndex+7;
   /** Names of reference manual for the device(s) */
   ArrayList<String> referenceManual;
   /** Names of device(s) */
   ArrayList<String> deviceNames;
   
   /**
    * Parse line containing Pin information
    *  
    * @param line
    * 
    * @return true - line is valid
    * 
    * @throws Exception
    */
   private boolean parseKeyLine(String[] line, String deviceName) throws Exception {
      
      // Set default values for column indices
      pinIndex       = 1;
      aliasIndex     = 2;
      resetIndex     = 3;
      defaultIndex   = 4;
      altStartIndex  = 5;
      altEndIndex    = altStartIndex+7;
      aliasIndexes = new ArrayList<AliasInfo>();

      // Add base device without aliases
//      aliasIndexes.add(new AliasInfo(deviceName, -1, true));

      final Pattern p = Pattern.compile("Alias\\s*(.*)\\s*");

      for (int col=0; col<line.length; col++) {
         if (line[col].equalsIgnoreCase("Pin")) {
            pinIndex = col;
//            System.err.println("pinIndex index = " + pinIndex);
         }
         Matcher m = p.matcher(line[col]);
         if (m.matches()) {
            aliasIndexes.add(new AliasInfo(m.group(1), col));
//            System.err.println("Found Alias: \'" + m.group(1) + "\'" + ", Col: " + col);
         }
         if (line[col].equalsIgnoreCase("Reset")) {
            resetIndex = col;
//            System.err.println("resetIndex index = " + resetIndex);
         }
         if (line[col].equalsIgnoreCase("Default")) {
            defaultIndex = col;
//            System.err.println("defaultIndex index = " + defaultIndex);
         }
         if (line[col].equalsIgnoreCase("ALT0")) {
            altStartIndex = col;
            altEndIndex = col;
//            System.err.println("altStartIndex index = " + altStartIndex);
         }
         if (line[col].toUpperCase().startsWith("ALT")) {
            if (altEndIndex<col) {
               altEndIndex = col;
//               System.err.println("altEndIndex index = " + altEndIndex);
            }
         }
      }
      return true;
   }
   
   /**
    * Parse line containing Pin information
    *  
    * @param line
    * @throws Exception
    */
   private void parsePinLine(String[] line) throws Exception {

      StringBuffer sb = new StringBuffer();
      
      if (!line[0].equals("Pin")) {
         return;
      }
      String pinName  = line[pinIndex];
      if ((pinName == null) || (pinName.isEmpty())) {
//         System.err.println("No pin name - Line discarded");
         return;
      }
      // Use first name on pin as Pin name e.g. PTC4/LLWU_P8 => PTC4
      Pattern p = Pattern.compile("(.+?)/.*");
      Matcher m = p.matcher(pinName);
      if (m.matches()) {
         pinName = m.group(1);
      }
      String aliases = null;
      if ((aliasIndex>=0) && (line.length>aliasIndex)) {
         aliases  = line[aliasIndex];
         if (aliases.equals("*")) {
//            System.err.println("Alias Line suppressed");
            return;
         }
      }

      final PinInformation pinInformation = PinInformation.createPin(pinName);

      sb.append(String.format("%-10s => ", pinInformation.getName()));
//      boolean debug = false;
//      if (pinName.equalsIgnoreCase("PTC1")) {
//         System.err.println("pin = " + pinName);
//         debug = true;
//      }
      boolean pinIsMapped = false;
      for (int col=altStartIndex; col<=altEndIndex; col++) {
         if (col>=line.length) {
            break;
         }
         ArrayList<PeripheralFunction> peripheralFunctions = createFunctionsFromString(line[col], true);
         for (PeripheralFunction peripheralFunction:peripheralFunctions) {
            sb.append(peripheralFunction.getName()+", ");
            if ((peripheralFunction != null) && (peripheralFunction != PeripheralFunction.DISABLED)) {
               MuxSelection functionSelector = MuxSelection.valueOf(col-altStartIndex);
               MappingInfo.createMapping(peripheralFunction, pinInformation, functionSelector);
//               System.err.println(mappingInfo.toString());
               pinIsMapped = true;
            }
         }
      }

      parseAlias(pinInformation, aliases);
      if ((line.length>resetIndex) && (line[resetIndex] != null) && (!line[resetIndex].isEmpty())) {
         String resetName  = line[resetIndex];
//       if (!pinIsMapped) {
         // Must be a unmapped pin - add as only mapping
         ArrayList<PeripheralFunction> resetFunctions = createFunctionsFromString(resetName, true);
         for (PeripheralFunction peripheralFunction:resetFunctions) {
            sb.append("R:" + peripheralFunction.getName() + ", ");
            // Pin is not mapped to this function in the ALT columns - must be a non-mappable pin
            MappingInfo.createMapping(peripheralFunction, pinInformation, pinIsMapped?MuxSelection.Reset:MuxSelection.Fixed);
//          System.err.println(mappingInfo.toString());
         }
         pinInformation.setResetPeripheralFunctions(resetName);
//       }
      }
      else {
         sb.append("R:" + PeripheralFunction.DISABLED.getName() + ", ");
         MappingInfo.createMapping(PeripheralFunction.DISABLED, pinInformation, MuxSelection.Reset);
         pinInformation.setResetPeripheralFunctions(PeripheralFunction.DISABLED.getName());
      }
      if (line.length>defaultIndex) {
         String defaultName  = convertName(line[defaultIndex]);
         
         if ((defaultName != null) && (!defaultName.isEmpty())) {
            pinInformation.setDefaultPeripheralFunctions(defaultName);
            for (PeripheralFunction fn:pinInformation.getDefaultValue()) {
               sb.append("D:" + fn + ", ");
            }
         }
      }
//      if (debug) {
//         System.err.println(sb.toString());
//      }
   }

   /**
    * Parse line containing ClockReg value
    * 
    * @param line
    * @throws Exception
    */
   private void parsePeripheralInfoLine(String[] line) throws Exception {
      if (!line[0].equals("Peripheral")) {
         return;
      }
      if (line.length < 3) {
         throw new Exception("Illegal ClockInfo Mapping line");
      }
      String peripheralName       = line[1];
      String peripheralClockReg   = line[2];
      
      String peripheralClockMask = null;
      if (line.length >= 4) {
         peripheralClockMask = line[3];
      }
      if ((peripheralClockMask==null) || (peripheralClockMask.isEmpty())) {
         peripheralClockMask = peripheralClockReg.replace("->", "_")+"_"+peripheralName+"_MASK";
      }

      String[] irqNums = new String[10]; 
      for (int index=0; index<irqNums.length; index++) {
         if (line.length >= index+5) {
            irqNums[index] = line[index+4];
         }
      }
      
      Pattern pattern = Pattern.compile("SIM->(SCGC\\d?)");
      Matcher matcher = pattern.matcher(peripheralClockReg);
      if (!matcher.matches()) {
         throw new Exception("Unexpected Peripheral Clock Register " + peripheralClockReg + " for " + peripheralName);
      }
      peripheralClockReg = matcher.group(1);
      if (!peripheralClockMask.contains(peripheralClockReg)) {
         throw new Exception("Clock Mask "+peripheralClockMask+" doesn't match Clock Register " + peripheralClockReg);
      }
      for(PeripheralTemplateInformation template:PeripheralTemplateInformation.getList()) {
         if (template.fPeripheralName.equalsIgnoreCase(peripheralName)) {
            template.setClockInfo(peripheralClockReg, peripheralClockMask);
            for (int index=0; index<irqNums.length; index++) {
               if (irqNums[index] != null) {
                  template.addIrqNum(irqNums[index]);
               }
            }
         }
      }
   }

   /**
    * Parse file
    * 
    * @param reader
    * 
    * @throws Exception
    */
   private void parseFile(BufferedReader reader) throws Exception {
      
      ArrayList<String[]> grid = new ArrayList<String[]>();
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
         for (int index=0; index<line.length; index++) {
            line[index] = line[index].trim();
         }
      }
      for(String[] line:grid) {
         if (line.length < 2) {
            continue;
         }
         try {
            parsePinLine(line);
         } catch (Exception e) {
            System.err.println("Exception @line " + line[1].toString());
            throw e;
         }
      }
      for(String[] line:grid) {
         if (line.length < 2) {
            continue;
         }
         parsePeripheralInfoLine(line);
      }
      for(String[] line:grid) {
         if (line.length < 2) {
            continue;
         }
         parseDmaMuxInfoLine(line);
      }
   }

   /**
    * Parse file
    * 
    * @param reader
    * 
    * @throws Exception
    */
   private void parsePreliminaryInformation(BufferedReader reader) throws Exception {
      
      // Set default values for column indices
      pinIndex        = 1;
      aliasIndex      = 2;
      resetIndex      = 3;
      defaultIndex    = 4;
      altStartIndex   = 5;
      altEndIndex     = altStartIndex+7;
      aliasIndexes    = new ArrayList<AliasInfo>();
      referenceManual = new ArrayList<String>();
      deviceNames     = new ArrayList<String>();
            
      ArrayList<String[]> grid = new ArrayList<String[]>();
      do {
         String line = reader.readLine();
         if (line == null) {
            break;
         }
         grid.add(line.split(","));
//         System.err.println(line);
      } while (true);
      for(String[] line:grid) {
         if (line.length<1) {
            continue;
         }
         for (int index=0; index<line.length; index++) {
            line[index] = line[index].trim();
         }
         if (line[0].equalsIgnoreCase("Key")) {
            // Process title line
            parseKeyLine(line, sourceName.replace(".csv", ""));
         }
         if (line[0].equalsIgnoreCase("Manual")) {
            for (int sub=1; sub<line.length; sub++) {
               referenceManual.add(line[sub]);
            }
         }
         if (line[0].equalsIgnoreCase("Devices")) {
            for (int sub=1; sub<line.length; sub++) {
               deviceNames.add(line[sub]);
            }
         }
      }
      if (aliasIndexes.size() == 0) {
         // For compatibility assume column 2 is a single device pinout
         aliasIndexes.add(new AliasInfo(sourceName.replace(".csv", ""), 2));
      }
//      if (deviceNames.size() == 0) {
//         deviceNames.add(sourceName.replace(".csv", ""));
//      }
      if (referenceManual.size() == 0) {
         referenceManual.add(sourceName.replace(".csv", ""));
      }
   }

   class DmaInfo {
      public final int    dmaInstance;
      public final int    dmaChannelNumber;
      public final String dmaSource;
      public DmaInfo(int dmaInstance, int dmaChannelNumber, String dmaSource) {
         this.dmaInstance      = dmaInstance;
         this.dmaChannelNumber = dmaChannelNumber;
         this.dmaSource        = dmaSource;
      }
   };
   
   /**
    * Parse DMA info line
    * 
    * @param line
    * @throws Exception
    */
   private void parseDmaMuxInfoLine(String[] line) throws Exception {
      if (!line[0].equals("DmaMux")) {
         return;
      }
      if (line.length < 4) {
         throw new Exception("Illegal DmaMux Mapping line");
      }
      DmaInfo dmaInfo = new DmaInfo(Integer.parseInt(line[1]), Integer.parseInt(line[2]), line[3]);
      dmaInfoList.add(dmaInfo);
   }

   /**
    * Writes enumeration describing DMA slot use
    * 
    * e.g.<pre>
    * 
    * </pre>
    * @param writer
    * @throws IOException
    */
   private void writeDmaMuxInfo(BufferedWriter writer) throws IOException {
      if (dmaInfoList.size() == 0) {
         return;
      }
      writer.write("\n");
      writeOpenNamespace(writer, NAME_SPACE);
      writeStartGroup(writer, "DMA_Group", "Direct Memory Access (DMA)", "Support for DMA operations");
      for (int instance=0; instance<4; instance++) {
         boolean noneWritten = true;
         for (DmaInfo item:dmaInfoList) {
            if (item.dmaInstance == instance) {
               if (noneWritten) {
                  writer.write("enum {\n");
                  noneWritten = false;
               }
               writer.write(String.format("   %-35s  = %d,\n", "DMA"+item.dmaInstance+"_SLOT_"+item.dmaSource, item.dmaChannelNumber));
            }
         }
         if (!noneWritten) {
            writer.write("};\n");
         }
      }
      writeCloseGroup(writer);
      writeCloseNamespace(writer, NAME_SPACE);
   }
   
   /**
    * Writes macros describing common pin functions for all pins
    * e.g.<pre>
    * #undef FIXED_ADC_FN
    * #undef FIXED_GPIO_FN
    * #undef FIXED_PORT_CLOCK_REG
    * 
    * #define FIXED_ADC_FN         0                    // Fixed ADC Multiplexing value
    * #define FIXED_GPIO_FN        1                    // Fixed GPIO Multiplexing value
    * #define FIXED_PORT_CLOCK_REG SIM->SCGC5           // Fixed PORT Clock
    * </pre>
    * 
    * @param headerFile Header file to write to
    * 
    * @throws Exception 
    */
   private void writePinDefines(BufferedWriter headerFile) throws Exception {
      writeBanner(headerFile, "Common Mux settings for PCR");
      writeMacroUnDefinition(headerFile, "FIXED_ADC_FN");
      writeMacroUnDefinition(headerFile, "FIXED_GPIO_FN");
      writeMacroUnDefinition(headerFile, "FIXED_PORT_CLOCK_REG");
      if (adcFunctionMuxValueChanged) {
         writeMacroDefinition(headerFile, "ADC_FN_CHANGES", "", " Indicates ADC Multiplexing varies with pin");
      }
      else {
         writeMacroDefinition(headerFile, "FIXED_ADC_FN", Integer.toString(adcFunctionMuxValue), " Fixed ADC Multiplexing value");
      }
      if (gpioFunctionMuxValueChanged) {
         writeMacroDefinition(headerFile, "GPIO_FN_CHANGES", "", " Indicates GPIO Multiplexing varies with pin");
      }
      else {
         writeMacroDefinition(headerFile, "FIXED_GPIO_FN", Integer.toString(gpioFunctionMuxValue), " Fixed GPIO Multiplexing value");
      }
      if (portClockRegisterChanged) {
         writeMacroDefinition(headerFile, "PORT_CLOCK_REG_CHANGES", "", " Indicates PORT Clock varies with pin");
      }
      else {
         writeMacroDefinition(headerFile, "FIXED_PORT_CLOCK_REG", portClockRegisterValue, " Fixed PORT Clock");
      }
      headerFile.write("\n");
   }

   /**
    * Writes code to select which peripheral signal is mapped to a pin
    * e.g.<pre>
    * // <b><i>PTD1</b></i> Pin Mapping
    * //   &lt;o&gt; <b><i>PTD1</b></i> (Alias:<b><i>D13</b></i>) [<b><i>ADC0_SE5b, GPIOD_1, SPI0_SCK</b></i>] &lt;name=<b><i>PTD1_SIG_SEL</b></i>&gt;
    * //   &lt;i&gt; Selects which peripheral signal is mapped to <b><i>PTD1</b></i> pin
    * //     &lt;0=&gt; <b><i>ADC0_SE5b</b></i>&lt;selection=<b><i>ADC0_SE5b_PIN_SEL,PTD1</b></i>&gt;
    * //     &lt;1=&gt; <b><i>GPIOD_1</b></i>&lt;selection=<b><i>GPIOD_1_PIN_SEL,PTD1</b></i>&gt;
    * //     &lt;2=&gt; <b><i>SPI0_SCK</b></i>&lt;selection=<b><i>SPI0_SCK_PIN_SEL,PTD1</b></i>&gt;
    * //     &lt;0=&gt; <b><i>Default</b></i>
    * #define <b><i>PTD1_SIG_SEL</b></i> 0                   
    * </pre>
    *  
    * @param pinInformation  Peripheral function to write definitions for
    * @param writer          Where to write
    * @throws Exception 
    */
   private void writePinMapping(PinInformation pinInformation, BufferedWriter writer) throws Exception {

      HashMap<MuxSelection, MappingInfo>  mappingInfo  = MappingInfo.getFunctions(pinInformation);

      MuxSelection[] sortedSelectionIndexes = mappingInfo.keySet().toArray(new MuxSelection[mappingInfo.keySet().size()]);
      Arrays.sort(sortedSelectionIndexes);

//      System.err.println("Pin " + pinInformation.getName()+": sortedSelectionIndexes = " + mappingInfo.keySet());
      
      MuxSelection defaultSelection = MuxSelection.Reset;

//      boolean debug = false;
//      if (pinInformation.getName().startsWith("PTA0")) {
//         debug = true;
//         System.err.println(String.format("writePinMapping() P:%s => %s", pinInformation.getName(), pinInformation.getResetValue()));
//         System.err.println(String.format("writePinMapping() P:%s => %s", pinInformation.getName(), pinInformation.getDefaultValue()));
//      }
      // Construct list of alternatives
      StringBuffer alternativeHint = new StringBuffer();
      for (MuxSelection selection:sortedSelectionIndexes) {
         if (selection == MuxSelection.Disabled) {
            continue;
         }
         if ((selection == MuxSelection.Reset) && (sortedSelectionIndexes.length>1)) {
            continue;
         }
         if (selection == MuxSelection.Fixed) {
            defaultSelection = MuxSelection.Fixed;
         }
         MappingInfo mInfo = mappingInfo.get(selection);
         StringBuffer name = new StringBuffer();
//         ArrayList<SelectionAttribute> selectionAttribute = new ArrayList<SelectionAttribute>();
         name.append(mInfo.getFunctionList());
//         if (debug) {
//            System.err.println("Checking " + mInfo.functions);
//         }
         if (mInfo.functions == pinInformation.getDefaultValue()) {
            defaultSelection = selection;
//            System.err.println("Found " + mInfo.functions);
         }
//         for (PeripheralFunction function:mInfo.functions) {
//            selectionAttribute.add(new SelectionAttribute(function.getName()+"_PIN_SEL", getPinNameWithAlias(pinInformation)));
//         }
         if (alternativeHint.length() != 0) {
            alternativeHint.append(", ");
         }
         alternativeHint.append(name);
      }
      WizardAttribute[] attributes = {new NameAttribute(pinInformation.getName()+"_SIG_SEL"), (sortedSelectionIndexes.length <= 1)?constantAttribute:null};
      String aliases = Aliases.getAliasList(pinInformation);
      if (aliases != null) {
         aliases = " (Alias:"+aliases+")";
      }
      else {
         aliases = "";
      }
      String hint;
      if (sortedSelectionIndexes.length <= 1) {
         hint = String.format("%s has no pin-mapping hardware", pinInformation.getName());
      }
      else {
         hint = String.format("Selects which peripheral signal is mapped to %s pin", pinInformation.getName());
      }
      writeWizardOptionSelectionPreamble(writer, 
            "Signal mapping for " + pinInformation.getName() + " pin",
            0,
            attributes,
            String.format("%s%s", pinInformation.getName(), aliases),
            hint,
            alternativeHint.toString());
      for (MuxSelection selection:sortedSelectionIndexes) {
         MappingInfo mInfo = mappingInfo.get(selection);
         StringBuffer name = new StringBuffer();
         ArrayList<SelectionAttribute> selectionAttribute = new ArrayList<SelectionAttribute>();
         name.append(mInfo.getFunctionList());
         for (PeripheralFunction fn:mInfo.functions) {
            String targetName = getPinNameWithAlias(pinInformation);
            if (selection == MuxSelection.Reset) {
               targetName += " (reset default)";
            }
            if (fn.isIncluded()) {
               selectionAttribute.add(new SelectionAttribute(fn.getName()+"_PIN_SEL", targetName));
            }
         }
         if (sortedSelectionIndexes.length <= 1) {
            name.append(" (fixed)");
         }
         else if (selection == MuxSelection.Reset) {
            name.append(" (reset default)");
         }
         writeWizardOptionSelectionEnty(writer, Integer.toString(selection.value), name.toString(), selectionAttribute.toArray(new SelectionAttribute[selectionAttribute.size()]));
      }
      if (sortedSelectionIndexes.length >= 2) {
         writeWizardDefaultSelectionEnty(writer, Integer.toString(defaultSelection.value));
      }
      writeMacroDefinition(writer, pinInformation.getName()+"_SIG_SEL", Integer.toString(defaultSelection.value));
      writer.write("\n");
   }

   /**
    * Writes pin-mapping selection code for all peripheral functions
    *  
    * @param writer  Header file to write result
    * 
    * @throws Exception 
    */
   private void writePinMappings(BufferedWriter headerFile) throws Exception {
      WizardAttribute[] attributes = {new NameAttribute("MAP_BY_PIN")};

      writeWizardConditionalSectionOpen(headerFile, 
            "Pin peripheral signal mapping", 
            0, 
            attributes, 
            "Mapping by Pin", 
            "This allows the mapping of peripheral functions to pins\n"+
            "to be controlled by individual pin");
      writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
      writeWizardOptionSelectionEnty(headerFile, "1", "Enabled");
      writeMacroDefinition(headerFile, "MAP_BY_PIN_ENABLED", "1");
      headerFile.write("\n");
      HashMap<String,ArrayList<PinInformation>> categories = new HashMap<String,ArrayList<PinInformation>>();
      class Pair {
         public final String namePattern;
         public final String titlePattern;
         
         Pair(String n, String t) {
            namePattern    = n;
            titlePattern   = t;
         }
      };
      final String UNMATCHED_NAME = "Miscellaneous Pins";
      Pair[] pinPatterns = {
            new Pair("XXXX",          UNMATCHED_NAME), 
            new Pair("PT([A-Z]).*",   "Port $1 Pins"), 
      };
      ArrayList<String> categoryTitles = new ArrayList<String>();
      ArrayList<String> pinNames = PinInformation.getPinNames();
      for (String name:pinNames) {
         
         String categoryTitle = UNMATCHED_NAME;
         for (Pair pair:pinPatterns) {
            Pattern p = Pattern.compile(pair.namePattern);
            Matcher m = p.matcher(name);
            if (m.matches()) {
               categoryTitle = m.replaceAll(pair.titlePattern);
               break;
            }
         }
         ArrayList<PinInformation> category = categories.get(categoryTitle);
         if (category == null) {
            category = new ArrayList<PinInformation>();
            categories.put(categoryTitle, category);
            categoryTitles.add(categoryTitle);
         }
         category.add(PinInformation.find(name));
      }
      for (String p:categoryTitles) {
         ArrayList<PinInformation> category = categories.get(p);
         if (category != null) {
            writeWizardSectionOpen(headerFile, p);
            for (PinInformation pinInformation:category) {
               writePinMapping(pinInformation, headerFile);
            }
            writeWizardSectionClose(headerFile);
         }
      }
      writeWizardConditionalSectionClose(headerFile);
   }

   static class ConstantAttribute implements WizardAttribute {

      @Override
      public String getAttributeString() {
         return "<constant>";
      }
      
   };
   
   /** A constant attribute for convenience */
   static final ConstantAttribute   constantAttribute      = new ConstantAttribute();
//   static final ConstantAttribute[] constantAttributeArray = {constantAttribute};
   
   /**
    * Gets pin name with appended list of aliases
    * 
    * @param pinInformation
    * @return name with aliases e.g. <b><i>PTE0 (Alias:D14)</b></i>
    */
   private String getPinNameWithAlias(PinInformation pinInformation) {
      String pinName = pinInformation.getName();
      String aliases = Aliases.getAliasList(pinInformation);
      if (aliases != null) {
         pinName += " (Alias:"+aliases+")";
      }
      return pinName;
   }
   
   /**
    * Writes code to select which pin a peripheral function is mapped to
    *  
    * @param writer     Header file to write result
    * @param function   The function to process
    * 
    * @throws Exception
    */
   private void writePeripheralSignalMapping(BufferedWriter writer, PeripheralFunction function) throws Exception {
      if (!function.isIncluded()) {
         return;
      }
      ArrayList<MappingInfo> mappingInfos = MappingInfo.getPins(function);
      Collections.sort(mappingInfos, new Comparator<MappingInfo>() {

         @Override
         public int compare(MappingInfo o1, MappingInfo o2) {
            return o1.mux.value - o2.mux.value;
         }
      });
      boolean noChoices =  ((mappingInfos == null) || (mappingInfos.size() == 0) ||
            ((mappingInfos.size() == 1) && (mappingInfos.get(0).mux == MuxSelection.Fixed)));

      //      boolean debug = false;
      //      if (function.getName().startsWith("JTAG_TDI")) {
      //         System.err.println("writePeripheralSignalMapping(): " + function.getName());
      //         debug = true;
      //      }
      // Create list of choices as string and determine default selection (if any)
      int defaultSelection = 0;
      //      int resetSelection = -1;
      String choices = null;
      if (mappingInfos != null) {
         //         Collections.sort(mappingInfos, new Comparator<MappingInfo>() {
         //            @Override
         //            public int compare(MappingInfo o1, MappingInfo o2) {
         //               return PinInformation.portNameComparator.compare(o1.pin.getName(), o2.pin.getName());
         //            }
         //         });
         int selection = 0;
         if (!noChoices) {
            selection++;
         }
         for (MappingInfo mappingInfo:mappingInfos) {
            if (mappingInfo.mux == MuxSelection.Disabled) {
               continue;
            }
            if ((mappingInfo.mux == MuxSelection.Reset) && (mappingInfo.pin.getDefaultValue() == null)) {
               if (defaultSelection == 0) {
                  defaultSelection = selection;
               }
               //               continue;
            }
            if (mappingInfo.mux == MuxSelection.Fixed) {
               defaultSelection = selection;
            }
            if (mappingInfo.pin.getDefaultValue() != null) {
               if (mappingInfo.pin.getDefaultValue().indexOf(function) >= 0) {
                  defaultSelection = selection;
               }
            }
            if (mappingInfo.mux != MuxSelection.Reset) {
               if (choices == null) {
                  choices = mappingInfo.pin.getName();
               }
               else {
                  choices += ", " + mappingInfo.pin.getName();
               }
            }
            selection++;
         }
      }
      if (choices != null) {
         choices = " [" + choices + "]";
      }

      WizardAttribute[] attributes = {new NameAttribute(function.getName()+"_PIN_SEL"), noChoices?constantAttribute:null};
      writeWizardOptionSelectionPreamble(writer, 
            "Pin Mapping for " + function.getName() + " signal",
            0,
            attributes,
            String.format("%s", function.getName()),
            String.format("Shows which pin %s is mapped to", function.getName()),
            choices);

      int selection = 0;
      if (!noChoices) {
         writeWizardOptionSelectionEnty(writer, Integer.toString(selection++), "Disabled");
      }
      if ((mappingInfos == null) || (mappingInfos.size() == 0)) {
         writeWizardOptionSelectionEnty(writer, Integer.toString(-1), function.getName());
         writeMacroDefinition(writer, function.getName()+"_PIN_SEL", Integer.toString(-1));
      }
      else {
         for (MappingInfo mappingInfo:mappingInfos) {
            if (mappingInfo.mux == MuxSelection.Disabled) {
               continue;
            }
            String pinName = getPinNameWithAlias(mappingInfo.pin);
            if (mappingInfo.mux == MuxSelection.Reset) {
               pinName += " (reset default)";
               //            continue;
            }
            String seletionTag = mappingInfo.getFunctionList();
            if (mappingInfo.mux == MuxSelection.Reset) {
               seletionTag += " (reset default)";
            }
            WizardAttribute[] functionAttributes = {new SelectionAttribute(mappingInfo.pin.getName()+"_SIG_SEL", seletionTag)};
            writeWizardOptionSelectionEnty(writer, Integer.toString(selection++), pinName, functionAttributes);
         }

         writeWizardOptionSelectionEnty(writer, Integer.toString(defaultSelection), "Default", null);
         writeMacroDefinition(writer, function.getName()+"_PIN_SEL", Integer.toString(defaultSelection));

//         FunctionTemplateInformation functionTemplateInformation = FunctionTemplateInformation.getTemplate(function);
//         if ((functionTemplateInformation == null) || !functionTemplateInformation.instanceWriter.needPcrTable()) {
//            selection = 0;
//            if (!noChoices) {
//               selection++;         
//            }
//            boolean conditionWritten = false;
//            for (MappingInfo mappingInfo:mappingInfos) {
//               if (mappingInfo.mux == MuxSelection.Disabled) {
//                  continue;
//               }
//               if (mappingInfo.mux == MuxSelection.Reset) {
//                  selection++;
//                  continue;
//               }
//               if (!noChoices) {
//                  writeConditional(writer, String.format("%s == %d", function.getName()+"_PIN_SEL", selection), conditionWritten);
//                  conditionWritten = true;
//               }
//               if (mappingInfo.mux.value < 0) {
////                  writeMacroDefinition(writer, function.getName()+"_GPIO", "0");
//                  writeMacroDefinition(writer, function.getName()+"_FN",   "0");
//               }
//               else {
////                  writeMacroDefinition(writer, function.getName()+"_GPIO", NAME_SPACE+"::"+mappingInfo.pin.getGpioClass());
//                  writeMacroDefinition(writer, function.getName()+"_FN", Integer.toString(mappingInfo.mux.value));
//               }
//               selection++;
//            }
//            writeConditionalEnd(writer, conditionWritten);
//         }
      }
      writer.write("\n");
   }

   /**
    * Writes code to control what pin peripheral functions are mapped to
    *  
    * @param writer  Header file to write result
    * 
    * @throws Exception 
    */
   private void writePeripheralSignalMappings(BufferedWriter headerFile) throws Exception {
      WizardAttribute[] attributes = {new NameAttribute("MAP_BY_FUNCTION"), new ConstantAttribute()};

      writeWizardConditionalSectionOpen(headerFile, 
            "Pin peripheral signal mapping", 
            0, 
            attributes, 
            "Mapping by Peripheral Function", 
            "This allows the mapping of peripheral functions to pins\n"+
            "to be controlled by peripheral function.\n" +
            "This option is active when Mapping by Pin is disabled");
      writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
      writeWizardOptionSelectionEnty(headerFile, "1", "Enabled");
      writeMacroDefinition(headerFile, "MAP_BY_FUNCTION_ENABLED", "0");
      headerFile.write("\n");
      
      HashMap<String,ArrayList<PeripheralFunction>> categories = new HashMap<String,ArrayList<PeripheralFunction>>();
      class Pair {
         public final String namePattern;
         public final String titlePattern;
         
         Pair(String p, String t) {
            namePattern = p;
            titlePattern   = t;
         }
      };
      final String UNMATCHED_NAME = "Miscellaneous";
      Pair[] functionPatterns = {
            new Pair("(ADC\\d+).*",             "Analogue to Digital ($1)"), 
            new Pair("(VREF\\d*).*",            "Voltage Reference ($1)"), 
            new Pair("(A?CMP\\d+).*",           "Analogue Comparator ($1)"), 
            new Pair("(FTM\\d+).*",             "FlexTimer ($1)"), 
            new Pair("(TPM\\d+).*",             "Timer ($1)"), 
            new Pair("(LCD_P)?(\\d+).*",        "Liquid Crystal Display"), 
            new Pair("(GPIO[A-Z]+).*",          "General Purpose I/O ($1)"), 
            new Pair("(I2C\\d+).*",             "Inter-Integrated Circuit ($1)"), 
            new Pair("(I2S\\d+).*",             "Integrated Interchip Sound ($1)"), 
            new Pair("(LLWU\\d*).*",            "Low-Leakage Wake-up Unit ($1)"), 
            new Pair("(SPI\\d+).*",             "Serial Peripheral Interface ($1)"), 
            new Pair("(TSI\\d+).*",             "Touch Sense Interface ($1)"), 
            new Pair("(LPTMR|LPTIM)(\\d+)*.*",  "Low Power Timer ($1)"), 
            new Pair("(UART\\d+).*",            "Universal Asynchronous Rx/Tx ($1)"), 
            new Pair("(PXBAR).*",               "($1)"), 
            new Pair("(QT).*",                  "($1)"), 
            new Pair("(SCI\\d+).*",             "Serial Communication Interface ($1)"), 
            new Pair("(SDAD)(M|P)\\d+.*",       "Sigma-delta ADC ($1)"), 
            new Pair("(LPUART\\d+).*",          "Low Power UART ($1)"), 
            new Pair("(DAC\\d*).*",             "Digital to Analogue ($1)"), 
            new Pair("(PDB\\d*).*",             "Programmable Delay Block ($1)"), 
            new Pair("(CAN\\d*).*",             "CAN Bus ($1)"), 
            new Pair("(ENET\\d*).*",            "Ethernet ($1)"), 
            new Pair("(MII\\d*).*",             "Ethernet ($1)"), 
            new Pair("(RMII\\d*).*",            "Ethernet ($1)"), 
            new Pair("(SDHC\\d*).*",            "Secured Digital Host Controller ($1)"), 
            new Pair("(CMT\\d*).*",             "Carrier Modulator Transmitter ($1)"), 
            new Pair("(EWM).*",                 "External Watchdog Monitor ($1)"), 
            new Pair("E?XTAL.*",                "Clock and Timing"),
            new Pair("(JTAG|SWD|NMI|TRACE|RESET).*",  "Debug and Control"),
            new Pair("(FB_).*",                 "Flexbus"),
            new Pair("(FXIO\\d+).*",            "Flexbus ($1)"),
            new Pair(".*(USB).*",               "Universal Serial Bus"), 
            new Pair(".*(CLK|EXTRG).*",         "Clock and Timing"),
      };
      
      ArrayList<String> categoryTitles = new ArrayList<String>();

      // Add catch-all "Miscellaneous" category
      categoryTitles.add(UNMATCHED_NAME);
      categories.put(UNMATCHED_NAME, new ArrayList<PeripheralFunction>());
      
      ArrayList<String> peripheralNames = PeripheralFunction.getPeripheralFunctionsAsList();
      for (String name:peripheralNames) {
         PeripheralFunction peripheralFunction = PeripheralFunction.find(name);
         if (!peripheralFunction.isIncluded()) {
            continue;
         }
         String categoryTitle = UNMATCHED_NAME;
         for (Pair pair:functionPatterns) {
            Pattern p = Pattern.compile(pair.namePattern);
            Matcher m = p.matcher(name);
            if (m.matches()) {
               categoryTitle = m.replaceAll(pair.titlePattern);
               break;
            }
         }
         ArrayList<PeripheralFunction> category = categories.get(categoryTitle);
         if (category == null) {
            categoryTitles.add(categoryTitle);
            category = new ArrayList<PeripheralFunction>();
            categories.put(categoryTitle, category);
         }
         category.add(peripheralFunction);
      }
      for (String categoryTitle:categoryTitles) {
         ArrayList<PeripheralFunction> category = categories.get(categoryTitle);
         if (category.size()>0) {
            writeWizardSectionOpen(headerFile, categoryTitle);
            for (PeripheralFunction peripheralFunction:category) {
               writePeripheralSignalMapping(headerFile, peripheralFunction);
            }
            writeWizardSectionClose(headerFile);
         }
      }
      writeWizardConditionalSectionClose(headerFile);
   }

   /**
    * Write timer configuration wizard information e.g.
    * <pre>
    * // &lth> Clock settings for FTM0
    * //
    * // FTM0_SC.CLKS ================================
    * //   &lt;o> FTM0_SC.CLKS Clock source 
    * //   &lt;i> Selects the clock source for the FTM0 module. [FTM0_SC.CLKS]
    * //     &lt;0=> Disabled
    * //     &lt;1=> System clock
    * //     &lt;2=> Fixed frequency clock
    * //     &lt;3=> External clock
    * //     &lt;1=> Default
    * 
    * // FTM0_SC.PS ================================
    * //   &lt;o1> FTM0_SC.PS Clock prescaler 
    * //   &lt;i> Selects the prescaler for the FTM0 module. [FTM0_SC.PS]
    * //     &lt;0=> Divide by 1
    * //     &lt;1=> Divide by 2
    * //     &lt;2=> Divide by 4
    * //     &lt;3=> Divide by 8
    * //     &lt;4=> Divide by 16
    * //     &lt;5=> Divide by 32
    * //     &lt;6=> Divide by 64
    * //     &lt;7=> Divide by 128
    * //     &lt;0=> Default
    * namespace USBDM {
    * constexpr uint32_t FTM0_SC = (FTM_SC_CLKS(0x1)|FTM_SC_PS(0x6));
    * }
    * </pre>
    * @param headerFile    Where to write
    * 
    * @throws IOException
    */
   private void writeTimerWizard(BufferedWriter headerFile) throws IOException {
      HashMap<String, PeripheralFunction> map;
      map = PeripheralFunction.getFunctionsByBaseName("FTM");
      if (map != null) {
         HashSet<String> instances = new HashSet<String>();
         for (String function:map.keySet()) {
            instances.add(map.get(function).fPeripheral.fInstance);
         }
         String[] sortedInstances = instances.toArray(new String[instances.size()]);
         Arrays.sort(sortedInstances);
         for (String ftm:sortedInstances) {
            if (!ftm.matches("\\d+")) {
               continue;
            }
            writeWizardSectionOpen(headerFile, "Clock settings for FTM" + ftm);
            writeWizardOptionSelectionPreamble(headerFile, 
                  String.format("FTM%s_SC.CLKS ================================\n//", ftm), 
                  0,
                  null,
                  String.format("FTM%s_SC.CLKS Clock source", ftm),
                  String.format("Selects the clock source for the FTM%s module. [FTM%s_SC.CLKS]", ftm, ftm));
            writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
            writeWizardOptionSelectionEnty(headerFile, "1", "System clock");
            writeWizardOptionSelectionEnty(headerFile, "2", "Fixed frequency clock");
            writeWizardOptionSelectionEnty(headerFile, "3", "External clock");
            writeWizardDefaultSelectionEnty(headerFile, "1");
            writeWizardOptionSelectionPreamble(headerFile, 
                  String.format("FTM%s_SC.PS ================================\n//",ftm),
                  1,
                  null,
                  String.format("FTM%s_SC.PS Clock prescaler", ftm),
                  String.format("Selects the prescaler for the FTM%s module. [FTM%s_SC.PS]", ftm, ftm));
            writeWizardOptionSelectionEnty(headerFile, "0", "Divide by 1");
            writeWizardOptionSelectionEnty(headerFile, "1", "Divide by 2");
            writeWizardOptionSelectionEnty(headerFile, "2", "Divide by 4");
            writeWizardOptionSelectionEnty(headerFile, "3", "Divide by 8");
            writeWizardOptionSelectionEnty(headerFile, "4", "Divide by 16");
            writeWizardOptionSelectionEnty(headerFile, "5", "Divide by 32");
            writeWizardOptionSelectionEnty(headerFile, "6", "Divide by 64");
            writeWizardOptionSelectionEnty(headerFile, "7", "Divide by 128");
            writeWizardDefaultSelectionEnty(headerFile, "0");
            writeOpenNamespace(headerFile, NAME_SPACE);
            writeConstexpr(headerFile, 16, "FTM"+ftm+"_SC", "(FTM_SC_CLKS(0x1)|FTM_SC_PS(0x0))");
            writeCloseNamespace(headerFile);
            headerFile.write("\n");
            writeWizardSectionClose(headerFile);
            //      headerFile.write( String.format(optionSectionClose));
         }
      }
      map = PeripheralFunction.getFunctionsByBaseName("TPM");
      if (map != null) {
         HashSet<String> instances = new HashSet<String>();
         for (String function:map.keySet()) {
            instances.add(map.get(function).fPeripheral.fInstance);
         }
         String[] sortedInstances = instances.toArray(new String[instances.size()]);
         Arrays.sort(sortedInstances);
         for (String ftm:sortedInstances) {
            if (!ftm.matches("\\d+")) {
               continue;
            }
            writeWizardSectionOpen(headerFile, "Clock settings for TPM" + ftm);
            writeWizardOptionSelectionPreamble(headerFile, 
                  String.format("TPM%s_SC.CMOD ================================\n//", ftm),
                  0,
                  null,
                  String.format("TPM%s_SC.CMOD Clock source",ftm),
                  String.format("Selects the clock source for the TPM%s module. [TPM%s_SC.CMOD]", ftm, ftm));
            writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
            writeWizardOptionSelectionEnty(headerFile, "1", "Internal clock");
            writeWizardOptionSelectionEnty(headerFile, "2", "External clock");
            writeWizardOptionSelectionEnty(headerFile, "3", "Reserved");
            writeWizardDefaultSelectionEnty(headerFile, "1");
            writeWizardOptionSelectionPreamble(headerFile, 
                  String.format("TPM%s_SC.PS ================================\n//", ftm),
                  1,
                  null,
                  String.format("TPM%s_SC.PS Clock prescaler", ftm),
                  String.format("Selects the prescaler for the TPM%s module. [TPM%s_SC.PS]", ftm, ftm));
            writeWizardOptionSelectionEnty(headerFile, "0", "Divide by 1");
            writeWizardOptionSelectionEnty(headerFile, "1", "Divide by 2");
            writeWizardOptionSelectionEnty(headerFile, "2", "Divide by 4");
            writeWizardOptionSelectionEnty(headerFile, "3", "Divide by 8");
            writeWizardOptionSelectionEnty(headerFile, "4", "Divide by 16");
            writeWizardOptionSelectionEnty(headerFile, "5", "Divide by 32");
            writeWizardOptionSelectionEnty(headerFile, "6", "Divide by 64");
            writeWizardOptionSelectionEnty(headerFile, "7", "Divide by 128");
            writeWizardDefaultSelectionEnty(headerFile, "0");
            writeOpenNamespace(headerFile, NAME_SPACE);
            writeConstexpr(headerFile, 16, "TPM"+ftm+"_SC", "(TPM_SC_CMOD(0x1)|TPM_SC_PS(0x0))");
            writeCloseNamespace(headerFile);
            headerFile.write("\n");
            writeWizardSectionClose(headerFile);
         }
      }
   }

   /**
    * Writes GPIO options e.g.
    * 
    * <pre>
    * // Inline port functions
    * //   &lt;q> Force inline port functions
    * //   &lt;i> This option forces some small GPIO functions to be inlined
    * //   &lt;i> This increases speed but may also increase code size
    * //     &lt;0=> Disabled
    * //     &lt;1=> Enabled
    * 
    * #define DO_INLINE_GPIO   0
    * 
    * </pre>
    * 
    * @param headerFile    Where to write
    * 
    * @throws IOException
    */
   private void writeGpioWizard(BufferedWriter headerFile) throws IOException {
//      writeWizardSectionOpen(headerFile, "GPIO Options");
//      writeWizardBinaryOptionSelectionPreamble(headerFile, 
//            String.format("Inline port functions\n//"), 
//            0,
//            false,
//            String.format("Force inline port functions"),
//            String.format("Forces some small GPIO functions to be inlined\n"+
//                          "This increases speed but may also increase code size"));
//      writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
//      writeWizardOptionSelectionEnty(headerFile, "1", "Enabled");
//      writeMacroDefinition(headerFile, "DO_INLINE_GPIO", "0");
      
//      writeWizardBinaryOptionSelectionPreamble(headerFile, 
//            String.format("Use USBDM namespace\n//"), 
//            0,
//            false,
//            String.format("Place CPP objects in the USBDM namespace"),
//            String.format("This will require us of \"using namespace USBDM\" directive"));
//      writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
//      writeWizardOptionSelectionEnty(headerFile, "1", "Enabled");
//      writeMacroDefinition(headerFile, NAMESPACES_GUARD_STRING, "0");
//      headerFile.write("\n");
//      writeWizardSectionClose(headerFile);
   }

   /**
    * Writes Pin Function options e.g.
    * 
    * <pre>
    * // Map 
    * //   &lt;q> Force inline port functions
    * //   &lt;i> This option forces some small GPIO functions to be inlined
    * //   &lt;i> This increases speed but may also increase code size
    * //     &lt;0=> Disabled
    * //     &lt;1=> Enabled
    * 
    * #define DO_INLINE_GPIO   0
    * 
    * </pre>
    * 
    * @param headerFile    Where to write
    * 
    * @throws IOException
    */
   private void writePinMappingOptions(BufferedWriter headerFile) throws IOException {
      writeWizardBinaryOptionSelectionPreamble(headerFile, 
            String.format("Pin mapping Options\n//"), 
            0,
            false,
            String.format("Map pins"),
            String.format("Selects whether pin mappings are done when individual\n" +
                          "peripherals are configured or during reset initialisation." ));
      writeWizardOptionSelectionEnty(headerFile, "0", "Pins mapped on demand");
      writeWizardOptionSelectionEnty(headerFile, "1", "Pin mapping on reset");
      writeMacroDefinition(headerFile, "DO_MAP_PINS_ON_RESET", "0");
      headerFile.write("\n");
   }

   /**
    * Writes all clock macros e.g.
    * <pre>
    * #define ADC0_CLOCK_REG       SIM->SCGC6          
    * #define ADC0_CLOCK_MASK      SIM_SCGC6_ADC0_MASK 
    * </pre>
    * 
    * @param writer  Where to write
    * 
    * @throws Exception
    */
   private void writeClockMacros(BufferedWriter writer) throws Exception {
      writeBanner(writer, "Peripheral clock macros");
      ArrayList<String> peripheralNames = Peripheral.getList();
      for (String name:peripheralNames) {
         Peripheral peripheral = Peripheral.getPeripheral(name);
         if (peripheral.fClockReg == null) {
            continue;
         }
         if (peripheral.fName.matches("PORT[A-Z]")) {
            if (portClockRegisterValue == null) {
               portClockRegisterValue = peripheral.fClockReg;
            }
            else if (!portClockRegisterValue.equals(peripheral.fClockReg)) {
               throw new Exception(
                  String.format("Multiple port clock registers existing=%s, new=%s", portClockRegisterValue, peripheral.fClockReg));
            }
         }
         writeMacroDefinition(writer, peripheral.fName+"_CLOCK_REG",  peripheral.fClockReg);
         writeMacroDefinition(writer, peripheral.fName+"_CLOCK_MASK", peripheral.fClockMask);
      }
      writeMacroDefinition(writer, "PORT_CLOCK_REG", portClockRegisterValue);
      writer.write("\n");
      
//      /* 
//       * XXX - Write debug information
//       */
//      writer.write("/*\n * Clock Information Summary\n");
//      for (String name:Peripheral.getList()) {
//         Peripheral peripheral = Peripheral.getPeripheral(name);
//         if (peripheral.fClockReg == null) {
//            continue;
//         }
//         if (peripheral.fName.matches("PORT[A-Z]")) {
//            if (portClockRegisterValue == null) {
//               portClockRegisterValue = peripheral.fClockReg;
//            }
//            else if (!portClockRegisterValue.equals(peripheral.fClockReg)) {
//               throw new Exception(
//                     String.format("Multiple port clock registers existing=%s, new=%s", portClockRegisterValue, peripheral.fClockReg));
//            }
//         }
//         writer.write(String.format(" * %-10s %-12s %-10s\n", peripheral.fName,  peripheral.fClockReg, peripheral.fClockMask));
//      }
//      writer.write(" */\n\n");

   }

   /**
    * Writes pin mapping header file
    * 
    * @param headerFile Header file to write to
    * 
    * @throws Exception
    */
   private void writePinMappingHeaderFile(BufferedWriter headerFile) throws Exception {
      writeHeaderFilePreamble(
            headerFile, 
            pinMappingBaseFileName+".h", pinMappingHeaderFileName, 
            VERSION, 
            "Pin declarations for "+deviceName+", generated from "+sourceName+"\n" +
            "Devices   " + deviceNames.toString() + "\n" +
            "Reference " + referenceManual.toString());
      writeSystemHeaderFileInclude(headerFile, "stddef.h");
      writeHeaderFileInclude(headerFile, "derivative.h");
      headerFile.write("\n");

      writeWizardMarker(headerFile);
      ValidatorAttribute[] attributes = {
            new ValidatorAttribute("net.sourceforge.usbdm.annotationEditor.validators.PinMappingValidator")
      };
      writeValidators(headerFile, attributes);

      writeTimerWizard(headerFile);
      writeGpioWizard(headerFile);
      writePinMappingOptions(headerFile);
      writePinMappings(headerFile);
      writePeripheralSignalMappings(headerFile);
      writeEndWizardMarker(headerFile);

      writePinDefines(headerFile);
      writeClockMacros(headerFile);
      writePeripheralInformationTables(headerFile);

      writeHeaderFileInclude(headerFile, "gpio_defs.h");
      
      writeDeclarations(headerFile);
      
      writeDmaMuxInfo(headerFile);
      
      writeHeaderFilePostamble(headerFile, pinMappingBaseFileName+".h");
   }

   /**
    * Write file validators e.g.
    * 
    * <pre>
    *    //================================
    *    // Validators
    *    // &lt;validate=<b><i>net.sourceforge.usbdm.annotationEditor.validators.PinMappingValidator</b></i>&gt;
    * </pre>
    * 
    * @param writer        Where to write
    * @param validators    Validators to write
    * 
    * @throws IOException
    */
   private void writeValidators(BufferedWriter writer, ValidatorAttribute[] validators) throws IOException {
      final String format = 
         "//================================\n"
         + "// Validators\n";
      
      writer.write(format);
      
      for (ValidatorAttribute validatorAttribute: validators) {
         if (validatorAttribute != null) {
            writer.write("// " + validatorAttribute.getAttributeString() + "\n");
         }
      }
      writer.write("\n");
   }

   private boolean deviceIsMKE;
   @SuppressWarnings("unused")
   private boolean deviceIsMKL;
   @SuppressWarnings("unused")
   private boolean deviceIsMKM;

   @SuppressWarnings("unused")
   private String makeOrExpression(String value, boolean[] values) {
      StringBuffer sb = new StringBuffer();
      boolean firstValue = true;
      for (int index=0; index<values.length; index++) {
         if (!values[index]) {
            continue;
         }
         if (!firstValue) {
            sb.append("||");
         }
         sb.append("("+value+"=="+Integer.toString(index)+")");
         firstValue = false;
      }
      return sb.toString();
   }
   
   /**
    * Write an external declaration for a simple peripheral (GPIO,ADC,PWM) e.g.
    * 
    * <pre>
    * <b>#if</b> <i>PTC18_SEL</i> == 1
    * using <i>gpio_A5</i>  = const USBDM::<i>GpioC&lt;18&gt;</i>;
    * <b>#endif</b>
    * </pre>
    * 
    * @param template         Template information
    * @param mappedFunction   Information about the pin and function being declared
    * @param fnIndex          Index into list of functions mapped to pin
    * @param gpioHeaderFile   Where to write
    * 
    * @throws Exception 
    */
   void writeExternDeclaration(PeripheralTemplateInformation template, MappingInfo mappedFunction, int fnIndex, BufferedWriter gpioHeaderFile) throws Exception {

      String definition = template.fInstanceWriter.getDefinition(mappedFunction, fnIndex);
      if (definition == null) {
         return;
      }
      boolean guardWritten = false;

      String signalName = template.fInstanceWriter.getInstanceName(mappedFunction, fnIndex);
//      if (guardWritten || macroAliases.add(signalName)) {
//         gpioHeaderFile.write(definition);
//      }
      if (template.useAliases(null)) {
         Aliases aliasList = Aliases.getAlias(mappedFunction.pin);
         if (aliasList != null) {
            for (String alias:aliasList.aliasList) {
               String aliasName = template.fInstanceWriter.getAliasName(signalName, alias);
               if (aliasName!= null) {
                  String declaration = template.fInstanceWriter.getAlias(aliasName, mappedFunction, fnIndex);
                  if (declaration != null) {
                     if (!guardWritten) {
                        guardWritten = writeFunctionSelectionGuardMacro(template, mappedFunction, gpioHeaderFile);
                     }
                     if (!macroAliases.add(aliasName)) {
                        // Comment out repeated aliases
                        gpioHeaderFile.write("//");
                     }
                     gpioHeaderFile.write(declaration);
                  }
               }
            }
         }
      }
      writeConditionalEnd(gpioHeaderFile, guardWritten);
   }
   
   /**
    * Process pins
    */
   void processPins() {
      for (PeripheralTemplateInformation pinTemplate:PeripheralTemplateInformation.getList()) {
         for (String pinName:PinInformation.getPinNames()) {
            PinInformation pinInfo = PinInformation.find(pinName);
            HashMap<MuxSelection, MappingInfo> mappedFunctions = MappingInfo.getFunctions(pinInfo);
            if (mappedFunctions == null) {
               continue;
            }
            for (MuxSelection index:mappedFunctions.keySet()) {
               if (index == MuxSelection.Reset) {
                  continue;
               }
               MappingInfo mappedFunction = mappedFunctions.get(index);
               for (PeripheralFunction function:mappedFunction.functions) {
                  if (pinTemplate.matches(function)) {
                     MappingInfo.addFunctionType(pinTemplate.fPeripheralName, pinInfo);
                  }
               }
            }
         }
      }
   }
      
   /**
    * Write alls Peripheral Information Classes<br>
    * 
    * <pre>
    *  class Adc0Info {
    *     public:
    *        //! Hardware base pointer
    *        static constexpr uint32_t basePtr   = ADC0_BasePtr;
    * 
    *        //! Base value for PCR (excluding MUX value)
    *        static constexpr uint32_t pcrValue  = DEFAULT_PCR;
    * 
    *        //! Information for each pin of peripheral
    *        static constexpr PcrInfo  info[32] = {
    * 
    *   //         clockMask         pcrAddress      gpioAddress gpioBit muxValue
    *   /*  0 * /  { 0 },
    *   ...
    *   #if (ADC0_SE4b_PIN_SEL == 1)
    *    /*  4 * /  { PORTC_CLOCK_MASK, PORTC_BasePtr,  GPIOC_BasePtr,  2,  0 },
    *   #else
    *    /*  4 * /  { 0 },
    *   #endif
    *   ...
    *   };
    *   };
    * </pre>
    * @param pinMappingHeaderFile Where to write
    * 
    * @throws Exception 
    */
   private void writePeripheralInformationTables(BufferedWriter pinMappingHeaderFile) throws Exception {
      writeOpenNamespace(pinMappingHeaderFile, NAME_SPACE);
      writeBanner(pinMappingHeaderFile, "Peripheral Pin Tables");

      writeStartGroup(pinMappingHeaderFile, 
            "PeripheralPinTables", 
            "Peripheral Information Classes", 
            "Provides instance specific information about a peripheral");

      for (PeripheralTemplateInformation pinTemplate:PeripheralTemplateInformation.getList()) {
         pinTemplate.writeInfoClass(pinMappingHeaderFile);
      }
      writeCloseGroup(pinMappingHeaderFile, "PeripheralPinTables");
      writeCloseNamespace(pinMappingHeaderFile, NAME_SPACE);
      pinMappingHeaderFile.write("\n");
   }

   /**
    * Write GPIO Header file.<br>
    * This mostly contains the extern declarations for peripherals
    * 
    * <pre>
    * <b>#if</b> <i>PTC18_SEL</i> == 1
    * using <i>gpio_A5</i>  = const USBDM::<i>GpioC&lt;18&gt;</i>;
    * <b>#endif</b>
    * </pre>
    * @param gpioHeaderFile
    * 
    * @throws Exception 
    */
   private void writeDeclarations(BufferedWriter gpioHeaderFile) throws Exception {
      
      gpioHeaderFile.write("\n");
      writeOpenNamespace(gpioHeaderFile, NAME_SPACE);
      for (PeripheralTemplateInformation pinTemplate:PeripheralTemplateInformation.getList()) {
         if (!pinTemplate.classIsUsed()) {
            continue;
         }
         boolean groupDone = false;
         for (String pinName:PinInformation.getPinNames()) {
            PinInformation pinInfo = PinInformation.find(pinName);
            HashMap<MuxSelection, MappingInfo> mappedFunctions = MappingInfo.getFunctions(pinInfo);
            if (mappedFunctions == null) {
               continue;
            }
            if (!pinTemplate.useAliases(pinInfo)) {
               continue;
            }
            for (MuxSelection index:mappedFunctions.keySet()) {
               if (index == MuxSelection.Reset) {
                  continue;
               }
               MappingInfo mappedFunction = mappedFunctions.get(index);
               for (int fnIndex=0; fnIndex<mappedFunction.functions.size(); fnIndex++) {
                  PeripheralFunction function = mappedFunction.functions.get(fnIndex);
                  if (pinTemplate.matches(function)) {
                     if (!groupDone) {
                        writeStartGroup(gpioHeaderFile, pinTemplate);
                        groupDone = true;
                        String t = pinTemplate.fInstanceWriter.getTemplate();
                        if (t != null) {
                           gpioHeaderFile.write(t);
                        }
                     }
                     writeExternDeclaration(pinTemplate, mappedFunction, fnIndex, gpioHeaderFile);
                  }
               }
            }
         }
         if (groupDone) {
            writeCloseGroup(gpioHeaderFile);
         }
      }
      writeConditionalStart(gpioHeaderFile, "DO_MAP_PINS_ON_RESET>0");
      writeDocBanner(gpioHeaderFile, "Used to configure pin-mapping before 1st use of peripherals");
      gpioHeaderFile.write("extern void usbdm_PinMapping();\n");
      writeConditionalEnd(gpioHeaderFile);
      writeCloseNamespace(gpioHeaderFile, NAME_SPACE);
//      writeHeaderFilePostamble(gpioHeaderFile, gpioBaseFileName+".h");
   }

   /**
    * Write Pin Mapping function to CPP file
    * 
    * @param cppFile    File to write to
    * 
    * @throws IOException
    */
   private void writePinMappingFunction(BufferedWriter cppFile) throws IOException {
      
      writeConditionalStart(cppFile, "DO_MAP_PINS_ON_RESET>0");
      cppFile.write(
         "struct PinInit {\n"+
         "   uint32_t pcrValue;\n"+
         "   uint32_t volatile *pcr;\n"+
         "};\n\n"+
         "static constexpr PinInit pinInit[] = {\n"
      );

      for (String pinName:PinInformation.getPinNames()) {
         Pattern p = Pattern.compile("PT([A-Z]+)([0-9]+)");
         Matcher m = p.matcher(pinName);
         if (m.matches()) {
            String instance = m.replaceAll("$1");
            String signal   = m.replaceAll("$2");
            writeConditionalStart(cppFile, String.format("%s_SIG_SEL>=0", pinName, pinName));
            cppFile.write(String.format("   { PORT_PCR_MUX(%s_SIG_SEL)|%s::DEFAULT_PCR, &PORT%s->PCR[%s]},\n", pinName, NAME_SPACE, instance, signal));
            writeConditionalEnd(cppFile);
         }
      }
      cppFile.write("};\n\n");
      
      cppFile.write(
         "/**\n" + 
         " * Used to configure pin-mapping before 1st use of peripherals\n" + 
         " */\n" + 
         "void usbdm_PinMapping() {\n"
      );
      
      boolean firstExpression = true;
      String currentBasename = null;
      String  instance = "X";
      int conditionCounter = 0;
      for (String pinName:PinInformation.getPinNames()) {
         Pattern p = Pattern.compile("(PT([A-Z]))[0-9]+");
         Matcher m = p.matcher(pinName);
         if (m.matches()) {
            String basename = m.replaceAll("$1");
            if (!basename.equals(currentBasename)) {
               if (!firstExpression) {
                  cppFile.write(String.format("\n\n   SIM->FIXED_PORT_CLOCK_REG |= PORT%s_CLOCK_MASK;\n", instance));
                  writeConditionalEnd(cppFile);
               }
               currentBasename = basename;
               cppFile.write("#if ");
               firstExpression = false;
               instance = m.replaceAll("$2");
            }
            else {
               cppFile.write(" || ");
               if (++conditionCounter>=4) {
                  cppFile.write("\\\n    ");
                  conditionCounter = 0;
               }
            }
            cppFile.write(String.format("(%s_SIG_SEL>=0)", pinName));
         }
      }
      if (!firstExpression) {
         cppFile.write(String.format("\n   SIM->FIXED_PORT_CLOCK_REG |= PORT%s_CLOCK_MASK;\n", instance));
         writeConditionalEnd(cppFile);
      }
  
      cppFile.write(
         "\n"+
         "   for (const PinInit *p=pinInit; p<(pinInit+(sizeof(pinInit)/sizeof(pinInit[0]))); p++) {\n"+   
         "      *(p->pcr) = p->pcrValue;\n"+ 
         "   }\n"
      );
      cppFile.write("}\n");
      writeConditionalEnd(cppFile);
   }
   
   /**
    * Write conditional macro guard for function declaration or definition
    * <pre>
    * e.g. #<b>if</b> (PTD5_SIG_SEL == 0)
    * or   #<b>elif</b> (PTD5_SIG_SEL == 0)
    * </pre>
    * 
    * @param pinTemplate
    * @param mappedFunction
    * @param file
    * @param guardWritten     If true, indicates that an elif clause should be written
    * 
    * @return Indicates if guard was written (and hence closing macro needs to be written)
    * 
    * @throws IOException
    */
   private boolean writeFunctionSelectionGuardMacro(PeripheralTemplateInformation pinTemplate, MappingInfo mappedFunction, BufferedWriter file, boolean guardWritten) throws IOException {
      final String format = "%s == %s";
      String pinName = mappedFunction.pin.getName();

      if (mappedFunction.mux == MuxSelection.Fixed) {
         // Don't guard fixed selections
         return false;
      }
      if (!pinTemplate.fInstanceWriter.useGuard()) {
         // Don't use guard
         return false;
      }
      writeConditional(file, String.format(format, pinName+"_SIG_SEL", Integer.toString(mappedFunction.mux.value)), guardWritten);
      return true;
   }
   
   /**
    * Write conditional macro guard for function declaration or definition
    * <pre>
    * e.g. #<b>if</b> (PTD5_SIG_SEL == 0)
    * </pre>
    * 
    * @param pinTemplate
    * @param mappedFunction
    * @param file
    * 
    * @return Indicates if guard was written (and hence closing macro needs to be written)
    * 
    * @throws IOException
    */
   private boolean writeFunctionSelectionGuardMacro(PeripheralTemplateInformation pinTemplate, MappingInfo mappedFunction, BufferedWriter file) throws IOException {
      return writeFunctionSelectionGuardMacro(pinTemplate, mappedFunction, file, false);
   }
   /**                    
    * Write CPP file      
    *                     
    * @param cppFile      
    * @throws Exception 
    */                    
   private void writeGpioCppFile(BufferedWriter cppFile) throws Exception {
    writeCppFilePreamble(
            cppFile, 
            gpioBaseFileName+".cpp", 
            gpioCppFileName, 
            "Pin declarations for "+deviceName+", generated from "+sourceName+"\n" +
            "Devices   " + deviceNames.toString() + "\n" +
            "Reference " + referenceManual.toString());
      writeHeaderFileInclude(cppFile, "gpio.h");
      writeHeaderFileInclude(cppFile, "pin_mapping.h");
      cppFile.write("\n");

      writeOpenNamespace(cppFile, NAME_SPACE);
      writePinMappingFunction(cppFile);
      writeCppFilePostAmple();
      writeCloseNamespace(cppFile, NAME_SPACE);
   }

   /**
    * Process file
    * 
    * @param filePath
    * @throws Exception
    */
   private void processFile(BufferedReader sourceFile) throws Exception {
      PinInformation.reset();
      PeripheralFunction.reset();
      MappingInfo.reset();
      Aliases.reset();
      Peripheral.reset();
      dmaInfoList = new ArrayList<DmaInfo>();

      macroAliases = new HashSet<String>();
      deviceIsMKE = deviceName.startsWith("MKE");
      deviceIsMKL = deviceName.startsWith("MKL");
      deviceIsMKM = deviceName.startsWith("MKL");
      
      PeripheralTemplateInformation.reset();
      for (char suffix='A'; suffix<='G'; suffix++) {
         new PeripheralTemplateInformation(
               "Gpio"+suffix, "PORT"+suffix, 
               "^\\s*(GPIO)("+suffix+")_(\\d+)\\s*$",
               new WriterForDigitalIO(deviceIsMKE));
      }
//      for (char suffix='A'; suffix<='G'; suffix++) {
//         new FunctionTemplateInformation(
//               "Gpio"+suffix, "PORT"+suffix, "Port_Group",  "Port Definitions",               
//               "Information required to manipulate PORT PCRs & associated GPIOs", 
//               null,
//               new WriterForPort(deviceIsMKE));
//      }
      if (!deviceIsMKE) {
         for (char suffix='0'; suffix<='3'; suffix++) {
            new PeripheralTemplateInformation(
                  "Adc"+suffix, "ADC"+suffix,
                  "(ADC)("+suffix+")_(SE\\d+)b?",
                  new WriterForAnalogueIO(false));
            new PeripheralTemplateInformation(
                  "Adc"+suffix+"a", "ADC"+suffix,
                  "(ADC)("+suffix+")_(SE\\d+)a",
                  new WriterForAnalogueIO(false));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            new PeripheralTemplateInformation(
                  "Cmp"+suffix, "CMP"+suffix,  
                  "(CMP)("+suffix+")_(IN\\d)",
                  new WriterForCmp(false));
         }
         new PeripheralTemplateInformation(
               "DmaMux0", "DMAMUX0",  
               null,
               new WriterForDmaMux());
         for (char suffix='0'; suffix<='3'; suffix++) {
            new PeripheralTemplateInformation(
                  "Ftm"+suffix, "FTM"+suffix, 
                  "(FTM)("+suffix+")_(CH\\d+|QD_PH[A|B]|FLT\\d|CLKIN\\d)",
                  new WriterForPwmIO_FTM(false));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            new PeripheralTemplateInformation(
                  "I2c"+suffix, "I2C"+suffix,  
                  "(I2C)("+suffix+")_(SCL|SDA)",
                  new WriterForI2c(false));
         }
         new PeripheralTemplateInformation(
               "Lptmr0", "LPTMR0",  
               "(LPTMR)(0)_(ALT\\d+)",
               new WriterForLptmr());
         new PeripheralTemplateInformation(
               "Pit", "PIT",  
               "(PIT)()(\\d+)",
               new WriterForPit());
         new PeripheralTemplateInformation(
               "Llwu", "LLWU",  
               "(LLWU)()_(P\\d+)",
               new WriterForLlwu(false));
         for (char suffix='0'; suffix<='3'; suffix++) {
            new PeripheralTemplateInformation(
                  "Spi"+suffix, "SPI"+suffix,  
                  "(SPI)("+suffix+")_(SCK|SIN|SOUT|MISO|MOSI|PCS\\d+)",
                  new WriterForSpi(false));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            new PeripheralTemplateInformation(
                  "Tpm"+suffix, "TPM"+suffix,  
                  "(TPM)("+suffix+")_(CH\\d+|QD_PH[A|B])",
                  new WriterForPwmIO_TPM(false));
         }
         for (char suffix='0'; suffix<='3'; suffix++) {
            new PeripheralTemplateInformation(
                  "Tsi"+suffix, "TSI"+suffix,  
                  "(TSI)("+suffix+")_(CH\\d+)",
                  new WriterForTsi(false));
         }
         for (char suffix='0'; suffix<='5'; suffix++) {
            new PeripheralTemplateInformation(
                  "Uart"+suffix, "UART"+suffix,  
                  "(UART)("+suffix+")_(TX|RX|CTS_b|RTS_b)",
                  new WriterForUart(false));
         }
         for (char suffix='0'; suffix<='5'; suffix++) {
            new PeripheralTemplateInformation(
                  "Lpuart"+suffix, "LPUART"+suffix,  
                  "(LPUART)("+suffix+")_(TX|RX|CTS_b|RTS_b)",
                  new WriterForLpuart(false));
         }
         new PeripheralTemplateInformation(
               "Vref", "VREF",  
               "(VREF)()_(OUT)",
               new WriterForVref());
      }
      Path pinMappingHeaderPath = headerDirectory.resolve(pinMappingHeaderFileName);
      BufferedWriter pinMappingHeaderFile = Files.newBufferedWriter(pinMappingHeaderPath, StandardCharsets.UTF_8);

      parseFile(sourceFile);
      processPins();
      writePinMappingHeaderFile(pinMappingHeaderFile);
      pinMappingHeaderFile.close();

      // Write single gpio-xxxx.cpp file
      Path gpioCppPath = sourceDirectory.resolve(gpioCppFileName);
      BufferedWriter gpioCppFile    = Files.newBufferedWriter(gpioCppPath, StandardCharsets.UTF_8);
      writeGpioCppFile(gpioCppFile);
      gpioCppFile.close();
   }

   /**
    * Process file
    * Rather crude - it processes the file multiple times to process each alias
    * 
    * @param filePath
    * @throws Exception
    */
   void processFile(Path filePath) throws Exception {
      
      System.err.println("Processing ============= " + filePath.getFileName());
      sourceName = filePath.getFileName().toString();

      // Locate output directories  
      sourceDirectory = filePath.getParent().resolve("Sources");
      headerDirectory = filePath.getParent().resolve("Project_Headers");
      
      // Create output directories if needed  
      if (!sourceDirectory.toFile().exists()) {
         Files.createDirectory(sourceDirectory);
      }
      if (!headerDirectory.toFile().exists()) {
         Files.createDirectory(headerDirectory);
      }

      // Open source file
      BufferedReader sourceFile = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);

      parsePreliminaryInformation(sourceFile);
      
      sourceFile.close();
      
      for (AliasInfo aliasInfo:aliasIndexes) {
         deviceName        = aliasInfo.name;
         aliasIndex        = aliasInfo.index;
         gpioCppFileName   = gpioBaseFileName+"-"+deviceName+".cpp";
         
         pinMappingHeaderFileName = pinMappingBaseFileName+"-"+deviceName+".h";
//         packageChoiceFileName    = pinMappingBaseFileName+"-"+deviceName+".xml";
         
         System.err.println("deviceName = " + deviceName);

         // Re-open source file
         sourceFile = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
         processFile(sourceFile);
         sourceFile.close();
         // Only write CPP file once for first alias
      }
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
