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

   public static final String VERSION = "1.0.0";

   /** Base name for pin mapping file */
   private final static String pinMappingBaseFileName   = "pin_mapping";
   
   /** Base name for gpio files */
   private final static String gpioBaseFileName         = "gpio";

   /** Name of the device e.g. MKL25Z4 */
   private String deviceName;
   
   /** Name of pin-mapping-XX.h header file */
   private String pinMappingHeaderFileName;
   
   /** Name of gpio-XX.cpp source file */
   private String gpioCppFileName;
   
   /** Name of gpio-XX.h header file */
   private String gpioHeaderFileName;

   /** Fixed GPIO mux function */
   private int      gpioFunctionMuxValue          = 1; 

   /** GPIO mux function varies with port */
   private boolean  gpioFunctionMuxValueChanged   = false;
   
   /** Fixed ADC mux function - default to mux setting 0*/
   private int      adcFunctionMuxValue           = 0;

   /** GPIO ADC function varies with port */
   private boolean  adcFunctionMuxValueChanged    = false;
   
   /** Fixed PORT clock enable register */
   private String   portClockRegisterValue        = "SIM->SCGC5";

   /** PORT clock enable register varies with port */
   private boolean  portClockRegisterChanged      = false;

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

   /**
    * Create a list of peripheral functions described by a string
    * 
    * @param pinText Text of function names e.g. <b><i>PTA4/LLWU_P3</b></i>
    * 
    * @return List of functions created
    * 
    * @throws Exception
    */
   ArrayList<PeripheralFunction> createFunctionsFromString(String pinText) throws Exception {
      ArrayList<PeripheralFunction> peripheralFunctionList = new ArrayList<PeripheralFunction>();
      pinText = pinText.trim();
      if (pinText.isEmpty()) {
         return peripheralFunctionList;
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
   
   /**
    * Parse line containing Pin information
    *  
    * @param line
    * @throws Exception
    */
   private void parsePinLine(String[] line) throws Exception {
      /** Index of Pin name in CSV file */
      final int PIN_INDEX       = 1;
      /** Index of aliases in CSV file */
      final int ALIAS_INDEX     = 2;
      /** Index of reset functions in CSV file */
      final int RESET_INDEX     = 3;
      /** Index of default functions in CSV file */
      final int DEFAULT_INDEX   = 4;
      /** Start index of multiplexor functions in CSV file */
      final int ALT_START_INDEX = 5;
      /** Last index of multiplexor functions in CSV file */
      final int ALT_END_INDEX   = ALT_START_INDEX+7;

      StringBuffer sb = new StringBuffer();
      
      if (!line[0].equals("Pin")) {
         return;
      }
      String pinName  = line[PIN_INDEX];
      if ((pinName == null) || (pinName.isEmpty())) {
         System.err.println("Line discarded");
         return;
      }

      final PinInformation pinInformation = PinInformation.createPin(pinName);

      sb.append(String.format("%-10s => ", pinInformation.getName()));
//      boolean debug = false;
//      if (pinName.equalsIgnoreCase("PTC1")) {
//         System.err.println("pin = " + pinName);
//         debug = true;
//      }
      boolean pinIsMapped = false;
      for (int col=ALT_START_INDEX; col<=ALT_END_INDEX; col++) {
         if (col>=line.length) {
            break;
         }
         ArrayList<PeripheralFunction> peripheralFunctions = createFunctionsFromString(line[col]);
         for (PeripheralFunction peripheralFunction:peripheralFunctions) {
            sb.append(peripheralFunction.getName()+", ");
            if ((peripheralFunction != null) && (peripheralFunction != PeripheralFunction.DISABLED)) {
               MuxSelection functionSelector = MuxSelection.valueOf(col-ALT_START_INDEX);
               MappingInfo.createMapping(peripheralFunction, pinInformation, functionSelector);
//               System.err.println(mappingInfo.toString());
               pinIsMapped = true;
            }
         }
      }
      if (line.length>ALIAS_INDEX) {
         String aliases  = line[ALIAS_INDEX];
         parseAlias(pinInformation, aliases);
      }
      if ((line.length>RESET_INDEX) && (line[RESET_INDEX] != null) && (!line[RESET_INDEX].isEmpty())) {
         String resetName  = line[RESET_INDEX];
//       if (!pinIsMapped) {
         // Must be a unmapped pin - add as only mapping
         ArrayList<PeripheralFunction> resetFunctions = createFunctionsFromString(resetName);
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
      if (line.length>DEFAULT_INDEX) {
         String defaultName  = line[DEFAULT_INDEX];
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
   private void parseClockInfoLine(String[] line) throws Exception {
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
//      System.err.println(String.format("p=%s, cr=%s, cm=%s",peripheralName, peripheralClockReg, peripheralClockMask));
      ;
      Peripheral p = Peripheral.getPeripheral(peripheralName);
      if (p == null) {
//         System.err.println("Adding peripheral for clock " + peripheralName);
         p = Peripheral.addPeripheral(peripheralName);
      }
      p.setClockInfo(peripheralClockReg, peripheralClockMask);
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
         parseClockInfoLine(line);
      }
   }

   /**
    * Writes macros describing common pin functions for all pins
    * e.g.<pre>
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
      if (adcFunctionMuxValueChanged) {
         writeMacroDefinition(headerFile, "ADC_FN_CHANGES", "", "Indicates ADC Multiplexing varies with pin");
      }
      else {
         writeMacroDefinition(headerFile, "FIXED_ADC_FN", Integer.toString(adcFunctionMuxValue), "Fixed ADC Multiplexing value");
      }
      if (gpioFunctionMuxValueChanged) {
         writeMacroDefinition(headerFile, "GPIO_FN_CHANGES", "", "Indicates GPIO Multiplexing varies with pin");
      }
      else {
         writeMacroDefinition(headerFile, "FIXED_GPIO_FN", Integer.toString(gpioFunctionMuxValue), "Fixed GPIO Multiplexing value");
      }
      if (portClockRegisterChanged) {
         writeMacroDefinition(headerFile, "PORT_CLOCK_REG_CHANGES", "", "Indicates PORT Clock varies with pin");
      }
      else {
         writeMacroDefinition(headerFile, "FIXED_PORT_CLOCK_REG", portClockRegisterValue, "Fixed PORT Clock");
      }
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
            String.format("%s%s [%s]", pinInformation.getName(), aliases, alternativeHint),
            hint );
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
      if (choices == null) {
         choices = "";
      }
      else {
         choices = " [" + choices + "]";
      }

      WizardAttribute[] attributes = {new NameAttribute(function.getName()+"_PIN_SEL"), noChoices?constantAttribute:null};
      writeWizardOptionSelectionPreamble(writer, 
            "Pin Mapping for " + function.getName() + " signal",
            0,
            attributes,
            String.format("%s", function.getName() + choices),
            String.format("Shows which pin %s is mapped to", function.getName()));

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
         
         boolean firstIf = true;
         selection = 0;
         if (!noChoices) {
            selection++;         
         }
         for (MappingInfo mappingInfo:mappingInfos) {
            if (mappingInfo.mux == MuxSelection.Disabled) {
               continue;
            }
            if (mappingInfo.mux == MuxSelection.Reset) {
               selection++;
               continue;
            }
            if (!noChoices) {
               if (firstIf) {
                  writeConditionalStart(writer, String.format("%s == %d", function.getName()+"_PIN_SEL", selection));
                  firstIf = false;
               }
               else {
                  writeConditionalElse(writer, String.format("%s == %d", function.getName()+"_PIN_SEL", selection));
               }
            }
            if (mappingInfo.mux.value < 0) {
               writeMacroDefinition(writer, function.getName()+"_GPIO", "0");
               writeMacroDefinition(writer, function.getName()+"_FN",   "0");
            }
            else {
               writeMacroDefinition(writer, function.getName()+"_GPIO", "digitalIO_"+mappingInfo.pin.getName());
               writeMacroDefinition(writer, function.getName()+"_FN", Integer.toString(mappingInfo.mux.value));
            }
            selection++;
         }
         if (!firstIf) {
            writeConditionalEnd(writer);
         }
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
    * #define FTM0_SC (FTM_SC_CLKS(0x1)|FTM_SC_PS(0x0))
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
            writeMacroDefinition(headerFile, "FTM"+ftm+"_SC", "(FTM_SC_CLKS(0x1)|FTM_SC_PS(0x0))");
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
            writeMacroDefinition(headerFile, "TPM"+ftm+"_SC", "(TPM_SC_CMOD(0x1)|TPM_SC_PS(0x0))");
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
      writeWizardBinaryOptionSelectionPreamble(headerFile, 
            String.format("Inline port functions\n//"), 
            0,
            false,
            String.format("Force inline port functions"),
            String.format("Forces some small GPIO functions to be inlined\n"+
                          "This increases speed but may also increase code size"));
      writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
      writeWizardOptionSelectionEnty(headerFile, "1", "Enabled");
      writeMacroDefinition(headerFile, "DO_INLINE_GPIO", "0");
      headerFile.write("\n");
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
                          "peripherals are configured or during reset initialisation.\n" +
                          "This will also cause unselected peripherals to be unavailable."));
      writeWizardOptionSelectionEnty(headerFile, "0", "Pins mapped on demand");
      writeWizardOptionSelectionEnty(headerFile, "1", "Pin mapping on reset  - unselected peripherals unavailable");
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
   }

   /**
    * Writes pin mapping header file
    * 
    * @param headerFile Header file to write to
    * 
    * @throws Exception
    */
   private void writePinMappingHeaderFile(BufferedWriter headerFile) throws Exception {
      writeHeaderFilePreamble(headerFile, pinMappingBaseFileName+".h", pinMappingHeaderFileName, VERSION, "Pin declarations for "+deviceName);
      writeHeaderFileInclude(headerFile, "derivative.h");
      headerFile.write("\n");
      writeWizardMarker(headerFile);
      ValidatorAttribute[] attributes = 
         {new ValidatorAttribute("net.sourceforge.usbdm.annotationEditor.validators.PinMappingValidator")};
      writeValidators(headerFile, attributes);
      writeTimerWizard(headerFile);
      writeGpioWizard(headerFile);
      writePinMappingOptions(headerFile);
      writePinMappings(headerFile);
      writePeripheralSignalMappings(headerFile);
      writeEndWizardMarker(headerFile);
      writePinDefines(headerFile);
      writeClockMacros(headerFile);
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

   private ArrayList<PinFunctionDescription>    pinFunctionDescriptions = new ArrayList<PinFunctionDescription>();
   private boolean deviceIsMKE;
   @SuppressWarnings("unused")
   private boolean deviceIsMKL;
   @SuppressWarnings("unused")
   private boolean deviceIsMKM;

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
    * #if <b><i>PTD0</b></i>_SEL == 1
    * extern const DigitalIO digitalIO_<b><i>PTD0</b></i> //!< DigitalIO on <b><i>PTD0</b></i>
    * #define digitalIO_D5 digitalIO_<b><i>PTD0</b></i>
    * #endif
    * </pre>
    * 
    * @param template         Template information
    * @param mappedFunction   Information about the pin and function being declared
    * @param fnIndex    Instance number e.g. PTD0 => 0
    * @param gpioHeaderFile   Where to write
    * 
    * @throws IOException
    */
   void writeExternDeclaration(FunctionTemplateInformation template, MappingInfo mappedFunction, int fnIndex, BufferedWriter gpioHeaderFile) throws IOException {

      boolean guardWritten = writeFunctionSelectionGuardMacro(template, mappedFunction, gpioHeaderFile);

      template.instanceWriter.writeDeclaration(mappedFunction, fnIndex, gpioHeaderFile);
      
      Aliases aliasList = Aliases.getAlias(mappedFunction.pin);
      if (aliasList != null) {
         String instanceName = template.instanceWriter.getInstanceName(mappedFunction, fnIndex);
         for (String alias:aliasList.aliasList) {
            writeMacroDefinition(gpioHeaderFile, template.instanceWriter.getAliasName(alias), instanceName);
         }
      }
      if (guardWritten) {
         writeConditionalEnd(gpioHeaderFile);
      }
   }
   
   /**
    * Process pins
    */
   void processPins() {
      for (FunctionTemplateInformation pinTemplate:FunctionTemplateInformation.getList()) {
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
//                  System.err.println(String.format("processPins() - N:%s, P:%s", function.getName(), pinTemplate.matchPattern.toString()));
                  if (pinTemplate.matchPattern.matcher(function.getName()).matches()) {
                     MappingInfo.addFunctionType(pinTemplate.baseName, pinInfo);
                  }
               }
            }
         }
      }
   }
   
   /**
    * Write GPIO Header file.<br>
    * This mostly contains the extern declarations for peripherals
    * 
    * <pre>
    * #if <b><i>PTA1</b></i>_SEL == 1
    * extern const DigitalIO digitalIO_<b><i>PTA1</b></i>;  //!< DigitalIO on <b><i>PTA1</b></i>
    * #define digitalIO_D5 digitalIO_<b><i>PTA1</b></i>
    * #endif
    * </pre>
    * @param gpioHeaderFile
    * @throws Exception 
    */
   private void writeGpioHeaderFile(BufferedWriter gpioHeaderFile) throws Exception {
      writeHeaderFilePreamble(gpioHeaderFile, "gpio.h", gpioHeaderFileName, VERSION, "Pin declarations for "+deviceName);
      writeHeaderFileInclude(gpioHeaderFile, "derivative.h");
      writeHeaderFileInclude(gpioHeaderFile, "pin_mapping.h");
      writeHeaderFileInclude(gpioHeaderFile, "gpio_defs.h");
      gpioHeaderFile.write("\n");
      for (FunctionTemplateInformation pinTemplate:FunctionTemplateInformation.getList()) {
         boolean groupDone = false;
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
               for (int fnIndex=0; fnIndex<mappedFunction.functions.size(); fnIndex++) {
                  PeripheralFunction function = mappedFunction.functions.get(fnIndex);
                  if (pinTemplate.matchPattern.matcher(function.getName()).matches()) {
                     if (!groupDone) {
                        writeStartGroup(gpioHeaderFile, pinTemplate);
                        groupDone = true;
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

      gpioHeaderFile.write(
            "#if defined(DO_MAP_PINS_ON_RESET) && (DO_MAP_PINS_ON_RESET>0)\n" +
            "/**\n" + 
            " * Used to configure pin-mapping before 1st use of peripherals\n" + 
            " */\n" + 
            "extern void usbdm_PinMapping();\n" +
            "#endif\n\n"
         );

      /* 
       * XXX - Write debug information
       */
      gpioHeaderFile.write("/*\nClock Information \n");
      for (String name:Peripheral.getList()) {
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
         gpioHeaderFile.write(String.format("%-10s %-12s %-10s\n", peripheral.fName,  peripheral.fClockReg, peripheral.fClockMask));
      }
      gpioHeaderFile.write("*/\n\n");
      writeHeaderFilePostamble(gpioHeaderFile, gpioBaseFileName+".h");
   }

   /**
    * Write Pin Mapping function to CPP file
    * 
    * @param cppFile    File to write to
    * 
    * @throws IOException
    */
   private void writePinMappingFunction(BufferedWriter cppFile) throws IOException {
      
      cppFile.write(
         "\n" +
         "#if defined(DO_MAP_PINS_ON_RESET) && (DO_MAP_PINS_ON_RESET>0)\n" +
         "struct PinInit {\n"+
         "   uint32_t pcrValue;\n"+
         "   uint32_t volatile *pcr;\n"+
         "};\n\n"+
         "static const PinInit pinInit[] = {\n"
      );

      for (String pinName:PinInformation.getPinNames()) {
         Pattern p = Pattern.compile("PT([A-Z]+)([0-9]+)");
         Matcher m = p.matcher(pinName);
         if (m.matches()) {
            String instance = m.replaceAll("$1");
            String signal   = m.replaceAll("$2");
            cppFile.write(String.format("#if defined(%s_SIG_SEL) && (%s_SIG_SEL>=0)\n", pinName, pinName));
            cppFile.write(String.format("   { PORT_PCR_MUX(%s_SIG_SEL)|DigitalIO::DEFAULT_PCR, &PORT%s->PCR[%s]},\n", pinName, instance, signal));
            cppFile.write(String.format("#endif\n"));
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
      for (String pinName:PinInformation.getPinNames()) {
         Pattern p = Pattern.compile("(PT([A-Z]))[0-9]+");
         Matcher m = p.matcher(pinName);
         if (m.matches()) {
            String basename = m.replaceAll("$1");
            if (!basename.equals(currentBasename)) {
               if (!firstExpression) {
                  cppFile.write(String.format("\n\n   FIXED_PORT_CLOCK_REG |= SIM_SCGC5_PORT%s_MASK;\n", instance));
                  cppFile.write("#endif\n\n");
               }
               currentBasename = basename;
               cppFile.write("#if ");
               firstExpression = false;
               instance = m.replaceAll("$2");
            }
            else {
               cppFile.write(" || \\\n    ");
            }
            cppFile.write(String.format("(defined(%s_SIG_SEL) && (%s_SIG_SEL>=0))", pinName, pinName));
         }
      }
      if (!firstExpression) {
         cppFile.write(String.format("\n\n   FIXED_PORT_CLOCK_REG |= SIM_SCGC5_PORT%s_MASK;\n", instance));
         cppFile.write("\n#endif\n");
      }
  
      cppFile.write(
         "\n"+
         "   for (const PinInit *p=pinInit; p<(pinInit+(sizeof(pinInit)/sizeof(pinInit[0]))); p++) {\n"+   
         "      *(p->pcr) = p->pcrValue;\n"+ 
         "   }\n"
      );
      
      cppFile.write(
         "}\n" +
         "#endif\n\n"
      );
   }
   
   /**
    * Write conditional macro guard for function declaration or definition
    * <pre>
    * e.g. #if (PTD5_SIG_SEL == 0)
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
   private boolean writeFunctionSelectionGuardMacro(FunctionTemplateInformation pinTemplate, MappingInfo mappedFunction, BufferedWriter file) throws IOException {
//      final String format = "(DO_MAP_PINS_ON_RESET==0) || (%s == %s)";
      final String format = "(%s == %s)";
      String pinName = mappedFunction.pin.getName();

      if (mappedFunction.mux == MuxSelection.Fixed) {
         // Don't guard fixed selections
         return false;
      }
      if (!pinTemplate.instanceWriter.useGuard()) {
         // Don't use guard
         return false;
      }
      writeConditionalStart(file, String.format(format, pinName+"_SIG_SEL", Integer.toString(mappedFunction.mux.value)));
      return true;
   }
   
   /**                    
    * Write CPP file      
    *                     
    * @param cppFile      
    * @throws Exception 
    */                    
   private void writeGpioCppFile(BufferedWriter cppFile) throws Exception {
      String description = "Pin declarations for " + deviceName;
      writeCppFilePreable(cppFile, gpioBaseFileName+".cpp", gpioCppFileName, description);
      writeHeaderFileInclude(cppFile, "utilities.h");
      writeHeaderFileInclude(cppFile, "gpio.h");
      writeHeaderFileInclude(cppFile, "pin_mapping.h");
      cppFile.write("\n");

      for (FunctionTemplateInformation pinTemplate:FunctionTemplateInformation.getList()) {
         for (String pinName:PinInformation.getPinNames()) {
            PinInformation                     pinInfo         = PinInformation.find(pinName);
            HashMap<MuxSelection, MappingInfo> mappedFunctions = MappingInfo.getFunctions(pinInfo);
            if (mappedFunctions == null) {
               continue;
            }
            for (MuxSelection muxSelection:mappedFunctions.keySet()) {
               if (muxSelection == MuxSelection.Reset) {
                  continue;
               }
               MappingInfo mappedFunction = mappedFunctions.get(muxSelection);
               for (int fnIndex=0; fnIndex<mappedFunction.functions.size(); fnIndex++) {
                  PeripheralFunction function = mappedFunction.functions.get(fnIndex);
                  if (pinTemplate.matchPattern.matcher(function.getName()).matches()) {
                     boolean guardWritten = writeFunctionSelectionGuardMacro(pinTemplate, mappedFunction, cppFile);
                     pinTemplate.instanceWriter.writeDefinition(mappedFunction, fnIndex, cppFile);
                     if (guardWritten) {
                        writeConditionalEnd(cppFile);
                     }
//                     System.err.println(String.format("N:%s, P:%s", x.getName(), pinTemplate.matchPattern.toString()));
//                     System.err.println("Matches");
                  }
               }
            }
         }
      }
      writePinMappingFunction(cppFile);
      writeCppFilePostAmple();
   }

   /**
    * Process file
    * 
    * @param filePath
    * @throws Exception
    */
   private void processFile(Path filePath) throws Exception {
      PinInformation.reset();
      PeripheralFunction.reset();
      MappingInfo.reset();
      Aliases.reset();
      Peripheral.reset();
      
      deviceName = filePath.getFileName().toString().replace(".csv", "");
      deviceIsMKE = deviceName.startsWith("MKE");
      deviceIsMKL = deviceName.startsWith("MKL");
      deviceIsMKM = deviceName.startsWith("MKL");
      FunctionTemplateInformation.reset();
      new FunctionTemplateInformation(
            "GPIO", "DigitalIO_Group",  "Digital Input/Output",               
            "Allows use of port pins as simple digital inputs or outputs", 
            "GPIO.*",
            new WriterForDigitalIO(deviceIsMKE));
      if (!deviceIsMKE) {
         new FunctionTemplateInformation(
               "ADC",  "AnalogueIO_Group", "Analogue Input",
               "Allows use of port pins as analogue inputs",
               "ADC[0-9]+_(SE).*",
               new WriterForAnalogueIO(deviceIsMKE));
         new FunctionTemplateInformation(
               "FTM",  "PwmIO_Group",      "PWM, Input capture, Output compare",
               "Allows use of port pins as PWM outputs",
               "FTM\\d+_CH\\d+",
               new WriterForPwmIO_FTM(deviceIsMKE));
         new FunctionTemplateInformation(
               "TPM",  "PwmIO_Group",      "PWM, Input capture, Output compare",
               "Allows use of port pins as PWM outputs",
               "TPM\\d+_CH\\d+",
               new WriterForPwmIO_TPM(deviceIsMKE));
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
      
      processPins();
      
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
