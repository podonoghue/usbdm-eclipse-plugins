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
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreatePinDescription extends DocumentUtilities {

   public static final String VERSION = "1.0.0";

   /**
    *  Mapping of Aliases to device pins<br>
    *  Many-to-1 i.e. more than one entry may map to a particular pin
    */
   private HashMap<String,PinInformation> aliasToPin = new HashMap<String, PinInformation>();
   
   /**
    * Mapping of device pins to Aliases<br>
    * 1-to-Many i.e. a given pin may have multiple aliases
    */
   private HashMap<PinInformation, ArrayList<String>> pinToAliases = new HashMap<PinInformation, ArrayList<String>>();
   
   /** Map from peripheral base name to set of instances<br>
    *  e.g. FTM = map(0,1), PT = map(A,B)  
    */
   private HashMap<String, HashSet<String>> peripheralInstances = new HashMap<String, HashSet<String>>();
   
   /** Map from peripheral to associated clock information<br> 
    * e.g. DMAMUX0 = ClockInfo(SIM->SCGC6, SIM_SCGC6_DMAMUX0_MASK) 
    */
   private HashMap<String, ClockInfo>       clockInfo           = new HashMap<String, ClockInfo>();
   
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
   
   /** Fixed ADC mux function */
   private int      adcFunctionMuxValue           = 0; // ADCs default to mux setting 0

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
    * Associates a clock register, clock mask pair<br>
    * e.g. ClockInfo(SIM->SCGC6, SIM_SCGC6_DMAMUX0_MASK)
    */
   private class ClockInfo {
      String clockReg;
      String clockMask;
      
      /**
       * Creates a clock register, clock mask pair
       * 
       * @param clockReg   Clock register e.g. SIM->SCGC6
       * @param clockMask  Clock mask e.g. SIM_SCGC6_DMAMUX0_MASK
       */
      ClockInfo(String clockReg, String clockMask) {
         this.clockReg  = clockReg;
         this.clockMask = clockMask;
      }
   };
   
   /**
    * Record an instance of a peripheral
    * 
    * @param peripheral       Base name of peripheral e.g. FTM
    * @param instanceNumber   Instance e.g. 2
    */
   private void addPeripheralInstance(PeripheralFunction peripheralFunction) {
//      System.err.println(String.format("p=%s, i=%s",peripheral, instanceNumber));

      HashSet<String> instances = peripheralInstances.get(peripheralFunction.fBaseName);
      if (instances == null) {
         instances = new HashSet<String>();
         peripheralInstances.put(peripheralFunction.fBaseName, instances);
      }
      instances.add(peripheralFunction.fInstance);
   }
   
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
         throw new Exception("Unable to find preferredPin: " + preferredPinName);
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
               pinInformation.addPeripheralFunction(peripheralFunction, functionSelector);
               peripheralFunction.addPinMapping(pinInformation);
               addPeripheralInstance(peripheralFunction);
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

      clockInfo.put(peripheralName, new ClockInfo(peripheralClockReg, peripheralClockMask));
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
      
      ArrayList<MappingInfo> peripherals = MappingInfo.getFunctions(pinInformation);
      
      String[] alternatives = new String[16];
      StringBuffer aliases = new StringBuffer();
      int selectionCount = 0;
      if (peripherals == null) {
         return;
      }
      for (MappingInfo mappedPeripheral:peripherals) {
         int selection = mappedPeripheral.mux;
         if (alternatives[selection] == null) {
            alternatives[selection] = new String();
            if (selectionCount>0) {
               aliases.append(", ");
            }
            aliases.append(mappedPeripheral.function.getName());
            selectionCount++;
         }
         else {
            alternatives[selection] += "/";
            aliases.append("/");
            aliases.append(mappedPeripheral.function.getName());
         }
         alternatives[selection] += mappedPeripheral.function.getName();
      }

      boolean isConstant = selectionCount < 2;
      
      writeWizardOptionSelectionPreamble(writer, 
            pinInformation.getName()+" Pin Mapping",
            0,
            isConstant,
            String.format("%s Pin Selection [%s]",                                 pinInformation.getName(), aliases),
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
   private void writePeripheralPinMappings(BufferedWriter writer) throws Exception {
      ArrayList<String> pinNames = PinInformation.getPinNames();
      for (String name:pinNames) {
         PinInformation pinInformation = PinInformation.find(name);
         writePeripheralPinMapping(pinInformation, writer);
      }
   }

   /**
    * Write timer configuration wizard
    * <pre>
    * // &lt> Clock settings for FTM0
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
            instances.add(map.get(function).fInstance);
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
            instances.add(map.get(function).fInstance);
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
      writeWizardSectionOpen(headerFile, "Pin Peripheral mapping");
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

   /**
    * Write an external declaration for a simple peripheral (GPIO,ADC,PWM)
    * 
    * <pre>
    * #if PTD0_SEL == 1
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
      String pinName = mappedFunction.pin.getName();
      gpioHeaderFile.write(String.format("#if %s == %s\n", pinName+"_SEL", Integer.toString(mappedFunction.mux)));
      String instanceName = pinName;
      if (instanceCount>0) {
         instanceName += "_" + Integer.toString(instanceCount);
      }
      gpioHeaderFile.write(String.format(template.externTemplate, instanceName+";", pinName));
      Aliases x = Aliases.getAlias(mappedFunction.pin);
      if (x != null) {
         for (String alias:x.aliasList) {
            gpioHeaderFile.write(String.format("#define %s %s\n", template.className+alias, template.className+pinName));
         }
      }
      gpioHeaderFile.write(String.format("#endif\n"));
   }
   
   /**
    * Write GPIO Header file
    * 
    * @param gpioHeaderFile
    * @throws Exception 
    */
   private void writeGpioHeaderFile(BufferedWriter gpioHeaderFile) throws Exception {
      writeHeaderFilePreamble(gpioHeaderFile, "gpio.h", gpioHeaderFileName, VERSION, "Pin declarations for "+deviceName);
      writeHeaderFileInclude(gpioHeaderFile, "derivative.h");
      writeHeaderFileInclude(gpioHeaderFile, "pin_mapping.h");
      writeHeaderFileInclude(gpioHeaderFile, "gpio_defs.h");
      gpioHeaderFile.write("\n");

      for (PinTemplateInformation pinTemplate:PinTemplateInformation.getList()) {
         boolean groupDone = false;
         for (String pinName:PinInformation.getPinNames()) {
            PinInformation pinInfo = PinInformation.find(pinName);
            ArrayList<MappingInfo> mappedFunctions = MappingInfo.getFunctions(pinInfo);
            if (mappedFunctions == null) {
               continue;
            }
            int instanceCount = 0;
            for (MappingInfo mappedFunction:mappedFunctions) {
               if (pinTemplate.baseName.equals(mappedFunction.function.fBaseName)) {
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
      
//      /*
//       * Write aliases
//       */
//      writeStartGroup(gpioHeaderFile, "alias_pin_mappings_GROUP", "Aliases for pins", "Aliases for pins for example Arduino based names");
//      HashMap<PinInformation, ArrayList<String>> x = pinToAliases;
//      
//      ArrayList<String> aliasIndex = new ArrayList<String>();
//      aliasIndex.addAll(aliasToPin.keySet());
//      Collections.sort(aliasIndex, portNameComparator);
//      writeAliases(gpioHeaderFile, aliasIndex, gpio,     "digitalIO_");
//      writeAliases(gpioHeaderFile, aliasIndex, analogue, "analogueIO_");
//      writeAliases(gpioHeaderFile, aliasIndex, ftm,      "pwmIO_");
//      writeAliases(gpioHeaderFile, aliasIndex, tpm,      "pwmIO_");
//      writeCloseGroup(gpioHeaderFile);

//      /* 
//       * XXX - Write debug information
//       */
//      gpioHeaderFile.write("/*\n");
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
//      for (String peripheral:peripheralInstances.keySet()) {
//         if (peripheral.startsWith("GPIO")) {
//            // GPIO don't have a clock
//            // The clock controls the PORT not the GPIO!
//            continue;
//         }
//         HashSet<String> instances = peripheralInstances.get(peripheral);
//         for (String instance : instances) {
//            boolean incomplete = false;
//            ClockInfo cInfo  = clockInfo.get(peripheral+instance);
//            if (cInfo == null) {
//               incomplete = true;
//               cInfo = new ClockInfo(null, null);
//               clockInfo.put(peripheral+instance, cInfo);
//            }
//            if (cInfo.clockReg == null) {
//               incomplete = true;
//               cInfo.clockReg = "SIM->SCGC6";
//            }
//            if (cInfo.clockMask == null) {
//               incomplete = true;
//               cInfo.clockMask = "SIM_SCGC6_"+peripheral+instance+"_MASK";
//            }
//            gpioHeaderFile.write(String.format("%s,%s,%s%s\n", peripheral+instance,  cInfo.clockReg, cInfo.clockMask, incomplete?"=default":""));
//         }
//      }
//      gpioHeaderFile.write("*/\n");
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
               if (pinTemplate.baseName.equals(mappedFunction.function.fBaseName)) {
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
               new PinTemplateInformation.analogueIO_Writer());
         new PinTemplateInformation(
               "FTM",  "PwmIO_Group",      "PWM, Input capture, Output compare",
               "Allows use of port pins as PWM outputs",
               "pwmIO_",      "extern const PwmIO  pwmIO_%-24s //!< PwmIO on %s\n",
               new PinTemplateInformation.pwmIO_FTM_Writer());
         new PinTemplateInformation(
               "TPM",  "PwmIO_Group",      "PWM, Input capture, Output compare",
               "Allows use of port pins as PWM outputs",
               "pwmIO_",      "extern const PwmIO  pwmIO_%-24s //!< PwmIO on %s\n",
               new PinTemplateInformation.pwmIO_TPM_Writer());
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
