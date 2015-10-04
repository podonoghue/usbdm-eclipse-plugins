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
    * Parse line containing Default value for peripheral pin mapping
    * 
    * @param line
    * @throws Exception
    */
   private void parseDefaultLine(String[] line) throws Exception {
      if (!line[0].equals("Default")) {
         return;
      }
      if (line.length < 3) {
         throw new Exception("Illegal Default Mapping line");
      }
      String peripheralFunctionName = line[1];
      String preferredPinName       = line[2];
      
      PeripheralFunction peripheralFunction = PeripheralFunction.find(peripheralFunctionName);
      PinInformation     pinInformation     = PinInformation.find(preferredPinName);

      if (peripheralFunction == null) {
         System.err.println(PeripheralFunction.listFunctions());
         throw new Exception("Unable to find peripheralFunction: " + peripheralFunctionName);
      }
      if (preferredPinName == null) {
         throw new Exception("Unable to find pin: " + preferredPinName);
      }
      ArrayList<PinInformation> mappablePins = peripheralFunction.getMappablePins();
      if (pinInformation != PinInformation.DISABLED_PIN) {
         int index = mappablePins.indexOf(pinInformation);
         if (index <0) {
            System.err.println("OPPS- looking for " + pinInformation);
            System.err.println("OPPS- looking in " + peripheralFunction);
            throw new Exception();
         }
      }
      peripheralFunction.setPreferredPin(pinInformation);
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
      /** Start index of multiplexor functions in CSV file */
      final int ALT_START_INDEX = 2;
      
      if (!line[0].equals("Pin")) {
         return;
      }
      String pinName  = line[PIN_INDEX];
      if ((pinName == null) || (pinName.isEmpty())) {
         System.err.println("Line discarded");
         return;
      }
      PinInformation pinInformation = PinInformation.createPin(pinName);

      for (int col=ALT_START_INDEX; col<line.length; col++) {
         String[] functions = line[col].split("/");
         for (String function:functions) {
            function = function.trim();
            if (function.isEmpty()) {
               continue;
            }
            PeripheralFunction peripheralFunction = PeripheralFunction.createPeripheralFunction(function);
            if (peripheralFunction != null) {
               int functionSelector = col-ALT_START_INDEX;
               MappingInfo mappingInfo = new MappingInfo(peripheralFunction, pinInformation, functionSelector);
               pinInformation.addPeripheralFunctionMapping(mappingInfo);
               peripheralFunction.addPinMapping(mappingInfo);
            }
         }
      }
   }
   
   /**
    * Parse line containing Alias value
    * 
    * @param line
    * @throws Exception 
    */
   private void parseAliasLine(String[] line) throws Exception {
      if (!line[0].equals("Alias")) {
         return;
      }
      if (line.length < 3) {
         throw new Exception("Illegal Alias Mapping line");
      }
      String aliasName = line[1];
      String pinName   = line[2];
      PinInformation pinInformation = PinInformation.find(pinName);
      if (pinInformation == null) {
         throw new Exception("Illegal alias, Pin not found: " + pinName);
      }
      Aliases.addAlias(pinInformation, aliasName);
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
    * Writes macros describing common pin functions for all pins
    * e.g.<pre>
    * #define FIXED_ADC_FN   0 //!< Fixed ADC Multiplexing value for pins
    * #define FIXED_GPIO_FN  1 //!< Fixed GPIO Multiplexing value for pins
    * #define FIXED_PORT_CLOCK_REG   SIM->SCGC5 //!< Fixed PORT Clock varies with port
    * </pre>
    * 
    * @param headerFile Header file to write to
    * 
    * @throws Exception 
    */
   private void writePinDefines(BufferedWriter headerFile) throws Exception {
      if (adcFunctionMuxValueChanged) {
         headerFile.append(String.format("#define ADC_FN_CHANGES      //!< Indicates ADC Multiplexing varies with pin\n"));
      }
      else {
         headerFile.append(String.format("#define FIXED_ADC_FN   %d //!< Fixed ADC Multiplexing value for pins\n", adcFunctionMuxValue));
      }
      if (gpioFunctionMuxValueChanged) {
         headerFile.append(String.format("#define GPIO_FN_CHANGES     //!< Indicates GPIO Multiplexing varies with pin\n"));
      }
      else {
         headerFile.append(String.format("#define FIXED_GPIO_FN  %d //!< Fixed GPIO Multiplexing value for pins\n", gpioFunctionMuxValue));
      }
      if (portClockRegisterChanged) {
         headerFile.append(String.format("#define PORT_CLOCK_REG_CHANGES     //!< Indicates GPIO Clock varies with pin\n"));
      }
      else {
         headerFile.append(String.format("#define FIXED_PORT_CLOCK_REG  %s //!< Fixed PORT Clock varies with port\n", portClockRegisterValue));
      }
   }

   
   /**
    * Writes pin-mapping selection code to header file
    * e.g.<pre>
    * // <b><i>PTD1</b></i> Pin Mapping
    * //   <o> <b><i>PTD1</b></i> Pin Selection [<b/><i/>ADC0_SE5b, GPIOD_1, SPI0_SCK, FTM3_CH1</b></i>] 
    * //   <i> Selects which peripheral function is mapped to <b><i>PTD1</b></i> pin
    * //     <0=> <b><i>ADC0_SE5b</b></i>
    * //     <1=> <b><i>GPIOD_1</b></i>
    * //     <2=> <b><i>SPI0_SCK</b></i>
    * //     <4=> <b><i>FTM3_CH1</b></i>
    * //     <0=> Default
    * #define <b><i>PTD1_SEL</b></i>             0                   
    * </pre>
    *  
    * @param pinInformation  Peripheral function to write definitions for
    * @param writer          Where to write
    * @throws Exception 
    */
   private void writePeripheralPinMapping(PinInformation pinInformation, BufferedWriter writer) throws Exception {
      
      ArrayList<MappingInfo> mappingInfo = MappingInfo.getFunctions(pinInformation);
      
      String[] alternatives = new String[16];
      StringBuffer alternativeHint = new StringBuffer();
      int selectionCount = 0;
      if (mappingInfo == null) {
         return;
      }
      for (MappingInfo mapInfo:mappingInfo) {
         String alternative = mapInfo.function.getName();
         int selection = mapInfo.mux;
         if (alternatives[selection] == null) {
            alternatives[selection] = new String();
            if (selectionCount>0) {
               alternativeHint.append(", ");
            }
            alternativeHint.append(alternative);
            selectionCount++;
         }
         else {
            alternatives[selection] += "/";
            alternativeHint.append("/");
            alternativeHint.append(alternative);
         }
         alternatives[selection] += alternative;
      }

      boolean isConstant = selectionCount < 2;
      
      String aliases = Aliases.getAliasList(pinInformation);
      if (aliases != null) {
         aliases = " (Alias:"+aliases+") ";
      }
      else {
         aliases = "";
      }
      writeWizardOptionSelectionPreamble(writer, 
            pinInformation.getName()+" Pin Mapping",
            0,
            isConstant,
            String.format("%s %s [%s]", pinInformation.getName(), aliases, alternativeHint),
            String.format("Selects which peripheral function is mapped to %s pin", pinInformation.getName()));
      
      int defaultSelection = -1; //pinInformation.getpreferredPinIndex();

      for (int selection=0; selection<alternatives.length; selection++) {
         if (alternatives[selection] == null) {
            continue;
         }
         if (defaultSelection <0) {
            defaultSelection = selection;
         }
//         String name = mappedPeripheral.function.getName() + " (" + Integer.toString(selection) + ")";
         writeWizardOptionSelectionEnty(writer, Integer.toString(selection), alternatives[selection]);
      }
      writeWizardDefaultSelectionEnty(writer, Integer.toString(defaultSelection));
      writeMacroDefinition(writer, pinInformation.getName()+"_SEL", Integer.toString(defaultSelection));
      writer.write("\n");
   }

   /**
    * Writes pin-mapping selection code for all peripheral functions
    *  
    * @param writer  Header file to write result
    * 
    * @throws Exception 
    */
   private void writePeripheralPinMappings(BufferedWriter headerFile) throws Exception {
      writeWizardSectionOpen(headerFile, "Pin Peripheral mapping");
      ArrayList<String> pinNames = PinInformation.getPinNames();
      for (String name:pinNames) {
         PinInformation pinInformation = PinInformation.find(name);
         writePeripheralPinMapping(pinInformation, headerFile);
      }
   }

   /**
    * Write timer configuration wizard information e.g.
    * <pre>
    * // &lth> Clock settings for FTM0
    * //
    * // FTM0_SC.CLKS ================================
    * //
    * //   &lt;o> FTM0_SC.CLKS Clock source 
    * //   &lt;i> Selects the clock source for the FTM0 module. [FTM0_SC.CLKS]
    * //     &lt;0=> Disabled
    * //     &lt;1=> System clock
    * //     &lt;2=> Fixed frequency clock
    * //     &lt;3=> External clock
    * //     &lt;1=> Default
    * // FTM0_SC.PS ================================
    * //
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
         for (String ftm:instances) {
            writeWizardSectionOpen(headerFile, "Clock settings for FTM" + ftm);
            writeWizardOptionSelectionPreamble(headerFile, 
                  String.format("FTM%s_SC.CLKS ================================\n//", ftm), 
                  0,
                  false,
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
                  false,
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
         for (String ftm:instances) {
            writeWizardSectionOpen(headerFile, "Clock settings for TPM" + ftm);
            writeWizardOptionSelectionPreamble(headerFile, 
                  String.format("TPM%s_SC.CMOD ================================\n//", ftm),
                  0,
                  false,
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
                  false,
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
    * Writes some GPIO options e.g.
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
      writeWizardSectionOpen(headerFile, "GPIO Options");
      writeWizardBinaryOptionSelectionPreamble(headerFile, 
            String.format("Inline port functions\n//"), 
            0,
            false,
            String.format("Force inline port functions"),
            String.format("This option forces some small GPIO functions to be inlined\n"+
                          "This increases speed but may also increase code size"));
      writeWizardOptionSelectionEnty(headerFile, "0", "Disabled");
      writeWizardOptionSelectionEnty(headerFile, "1", "Enabled");
      headerFile.write("#define DO_INLINE_GPIO   0\n");
      headerFile.write("\n");
      writeWizardSectionClose(headerFile);
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
      writeTimerWizard(headerFile);
      writeGpioWizard(headerFile);
      writePeripheralPinMappings(headerFile);
      writeWizardSectionClose(headerFile);
      writeEndWizardMarker(headerFile);
      writePinDefines(headerFile);
      writeHeaderFilePostamble(headerFile, pinMappingBaseFileName+".h");
   }

   private ArrayList<PinFunctionDescription>    pinFunctionDescriptions = new ArrayList<PinFunctionDescription>();
   private boolean deviceIsMKE;
   @SuppressWarnings("unused")
   private boolean deviceIsMKL;
   @SuppressWarnings("unused")
   private boolean deviceIsMKM;

   String makeAndExpression(String value, boolean[] values) {
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
    * #endif
    * </pre>
    * 
    * @param template         Template information
    * @param mappedFunction   Information about the pin and function being declared
    * @param instanceCount 
    * @param gpioHeaderFile   Where to write
    * 
    * @throws IOException
    */
   void writeExternDeclaration(PinTemplateInformation template, MappingInfo mappedFunction, int instanceCount, BufferedWriter gpioHeaderFile) throws IOException {
      boolean[] selectionValue = new boolean[16];
      Arrays.fill(selectionValue, false);
      selectionValue[mappedFunction.mux] = true;
      
      String pinName = mappedFunction.pin.getName();
//      gpioHeaderFile.write(String.format("#if %s == %s\n", pinName+"_SEL", Integer.toString(mappedFunction.mux)));
      String instanceName = pinName;
      if (instanceCount>0) {
         instanceName += "_" + Integer.toString(instanceCount);
      }
      gpioHeaderFile.write(String.format(template.externTemplate, instanceName+";", pinName));
      Aliases aliasList = Aliases.getAlias(mappedFunction.pin);
      if (aliasList != null) {
         for (String alias:aliasList.aliasList) {
            gpioHeaderFile.write(String.format("#define %s %s\n", template.className+alias, template.className+pinName));
         }
      }
//      gpioHeaderFile.write(String.format("#endif\n"));
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
      ArrayList<PinTemplateInformation> x = PinTemplateInformation.getList();
      for (PinTemplateInformation pinTemplate:x) {
         boolean groupDone = false;
         for (String pinName:PinInformation.getPinNames()) {
            PinInformation pinInfo = PinInformation.find(pinName);
            ArrayList<MappingInfo> mappedFunctions = MappingInfo.getFunctions(pinInfo);
            if (mappedFunctions == null) {
               continue;
            }
            int instanceCount = 0;
            for (MappingInfo mappedFunction:mappedFunctions) {
               if (pinTemplate.baseName.equals(mappedFunction.function.fPeripheral.fBaseName)) {
                  if (!groupDone) {
                     writeStartGroup(gpioHeaderFile, pinTemplate);
                     groupDone = true;
                  }
                  writeExternDeclaration(pinTemplate, mappedFunction, instanceCount, gpioHeaderFile);
                  instanceCount++;
               }
            }
         }
         if (groupDone) {
            writeCloseGroup(gpioHeaderFile);
         }
      }

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
    
      gpioHeaderFile.write("/*\nDefault pin mappings \n");
      for (String name:PinInformation.getPinNames()) {
         PinInformation pin = PinInformation.find(name);
//         gpioHeaderFile.write(String.format("%-10s %-12s %-10s\n", peripheral.fName,  peripheral.fClockReg, peripheral.fClockMask));
      }
      gpioHeaderFile.write("*/\n");

      
//      ArrayList<String> ar = PeripheralFunction.getPeripheralFunctionsAsList();
//      for (String functionName:ar) {
//         PeripheralFunction peripheralFunction = PeripheralFunction.lookup(functionName);
//         if (peripheralFunction.getName().startsWith("GPIO")) {
//            continue;
//         }
//         ArrayList<PinInformation> mappablePins = peripheralFunction.getMappablePins();
//         if ((mappablePins == null) || (mappablePins.size() < 2)) {
//            continue;
//         }
//         String defaultMapping = mappablePins.get(peripheralFunction.getpreferredPinIndex()).getName();
//         gpioHeaderFile.write(String.format("%s,%s,[,", peripheralFunction.getName(), defaultMapping));
//         boolean firstOne = true;
//         for (PinInformation p:mappablePins) {
//            gpioHeaderFile.write(String.format((firstOne?"%s":", %s"), p.getName()));
//            firstOne = false;
//         }      
//         gpioHeaderFile.write(",]\n");
//      }
//      gpioHeaderFile.write("\n");
//
      writeHeaderFilePostamble(gpioHeaderFile, gpioBaseFileName+".h");
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
      
      writeClockMacros(cppFile);

      for (PinTemplateInformation pinTemplate:PinTemplateInformation.getList()) {
         for (String pinName:PinInformation.getPinNames()) {
            PinInformation pinInfo = PinInformation.find(pinName);
            ArrayList<MappingInfo> mappedFunctions = MappingInfo.getFunctions(pinInfo);
            int instanceCount = 0;
            if (mappedFunctions == null) {
               continue;
            }
            for (MappingInfo mappedFunction:mappedFunctions) {
               if (pinTemplate.baseName.equals(mappedFunction.function.fPeripheral.fBaseName)) {
                  pinTemplate.instanceWriter.writeInstance(mappedFunction, instanceCount, cppFile);
                  instanceCount++;
               }
            }
         }
      }
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
      PinTemplateInformation.reset();
      new PinTemplateInformation(
            "GPIO", "DigitalIO_Group",  "Digital Input/Output",               
            "Allows use of port pins as simple digital inputs or outputs", 
            "digitalIO_",  "extern const DigitalIO digitalIO_%-24s //!< DigitalIO on %s\n",
            new PinTemplateInformation.digitalIO_Writer(deviceIsMKE));
      if (!deviceIsMKE) {
         new PinTemplateInformation(
               "ADC",  "AnalogueIO_Group", "Analogue Input",
               "Allows use of port pins as analogue inputs",
               "analogueIO_", "extern const AnalogueIO analogueIO_%-24s //!< AnalogueIO on %s\n",
               new PinTemplateInformation.analogueIO_Writer(deviceIsMKE));
         new PinTemplateInformation(
               "FTM",  "PwmIO_Group",      "PWM, Input capture, Output compare",
               "Allows use of port pins as PWM outputs",
               "pwmIO_",      "extern const PwmIO  pwmIO_%-24s //!< PwmIO on %s\n",
               new PinTemplateInformation.pwmIO_FTM_Writer(deviceIsMKE));
         new PinTemplateInformation(
               "TPM",  "PwmIO_Group",      "PWM, Input capture, Output compare",
               "Allows use of port pins as PWM outputs",
               "pwmIO_",      "extern const PwmIO  pwmIO_%-24s //!< PwmIO on %s\n",
               new PinTemplateInformation.pwmIO_TPM_Writer(deviceIsMKE));
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
