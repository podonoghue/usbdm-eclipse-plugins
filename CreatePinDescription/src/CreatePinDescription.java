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
    * Comparator for port names e.g. PTA13 c.f. PTB12
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
    * Comparator two lines based upon the Port name in line[0]
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
    * Adds an alias for a pin
    * This is a one to many relationship
    * 
    * @param pinInformation Pin to add alias for
    * @param aliasName      Alias name
    */
   private void addAlias(PinInformation pinInformation, String aliasName) {
      aliasToPin.put(aliasName, pinInformation);
      
      /* Aliases for this pin */
      ArrayList<String> aliases = pinToAliases.get(pinInformation);
      if (aliases == null) {
         aliases = new ArrayList<String>();
         pinToAliases.put(pinInformation, aliases);
      }
      aliases.add(aliasName);
   }
   
   /**
    * Gets list of alias for this pin as a comma separated string.
    * 
    * @param pinName
    * @return String of form alias1,alias2 ... or null if no aliases defined
    */
   private String getAliasList(PinInformation pinInformation) {
      ArrayList<String> aliasList = pinToAliases.get(pinInformation);
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
   
//   /**
//    * Gets the aliases for a pin
//    * 
//    * @param pinInformation
//    * 
//    * @return Set of aliases associated with this pin
//    */
//   private ArrayList<String> getAliases(PinInformation pinInformation) {
//      return pinToAliases.get(pinInformation);
//   }
   
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
      addAlias(pinInformation, aliasName);
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
    * Writes macros describing pin functions
    * e.g.<pre>
    * // PTB2 = ADC0_SE12,GPIOB_2,I2C0_SCL (Alias: D15)
    * #define PTB2_GPIO_NAME   B                //!< PTB2 GPIO name
    * #define PTB2_GPIO_BIT    2                //!< PTB2 GPIO bit number
    * #define PTB2_GPIO_FN     1                //!< PTB2 Pin multiplexor for GPIO
    * #define PTB2_ADC_NUM     0                //!< PTB2 ADC number
    * #define PTB2_ADC_CH      12               //!< PTB2 ADC channel
    * #define PTB2_ADC_FN      0                //!< PTB2 Pin multiplexor for ADC
    * #if I2C0_SCL_SEL == 2                     
    * #define I2C0_SCL_FN      2                //!< PTB2 Pin multiplexor for I2C
    * #define I2C0_SCL_GPIO    digitalIO_PTB2   //!< PTB2 I2C GPIO
    * #endif
    * </pre>
    * 
    * @param pinInfo    Pin to describe
    * @param headerFile Header file to write to
    * 
    * @throws Exception 
    */
   private void writeDefinesForPin(PinInformation pinInfo, BufferedWriter headerFile) throws Exception {
      
      String aliasName   = getAliasList(pinInfo);
      String description = pinInfo.getDescription();
      if (aliasName != null) {
         description += " (Alias: " + aliasName + ")";
      }
      headerFile.append("// "+description + "\n");
      String pinName  = pinInfo.getName();
      
      /*
       * Do port functions
       */
      ArrayList<MappingInfo> mappingList = pinInfo.getMappingList("GPIO");
      if (mappingList.size()>0) {
         if (mappingList.size() > 1) {
            throw new Exception("Multiple ports mapped!");
         }
         MappingInfo info = mappingList.get(0);
         String baseName  = info.function.fBaseName;
         String name      = info.function.fInstance;
         String chNum     = info.function.fSignal;
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
               pinInfo.getName()+"_"+baseName+"_NAME", name,  pinName, baseName,
               pinInfo.getName()+"_"+baseName+"_BIT",  chNum, pinName, baseName
               ));
         headerFile.append(String.format(
               "#define %-18s         %-3d   //!< %s Pin multiplexor for %s\n",
               pinInfo.getName()+"_"+baseName+"_FN", pinMux,    pinName, baseName
               ));
      }

      /*
       * Do ADC functions
       */
      mappingList = pinInfo.getMappingList("ADC");
      int suffix = 0;
      for (MappingInfo info:mappingList) {
         String modifier = "";
         if (suffix>0) {
            modifier = "_"+Integer.toString(suffix);
         }
         suffix++;
         String baseName  = info.function.fBaseName;
         String name      = info.function.fInstance;
         String chNum     = info.function.fSignal;
         int    pinMux    = info.mux; 
         // ToDo - Handle multiple ADC mapping
//         if (mappingList.size() > 1) {
//            throw new Exception(String.format("ADC multiple ports mapped!, pin = %s, mux = %d", pinName, pinMux));
//         }
         if (pinMux != adcFunctionMuxValue) {
            if (adcFunctionMuxValueChanged) {
               System.err.println(String.format("ADC pin mux value non-constant, pin = %s, mux = %d", pinName, pinMux));
            }
            adcFunctionMuxValueChanged = true;
            adcFunctionMuxValue = pinMux;
            System.err.println(String.format("Changing ADC pin mapping, pin = %s, mux = %d", pinName, pinMux));
         }
         PeripheralFunction peripheralFunction = info.function;
         ArrayList<PinInformation> pinMappings = peripheralFunction.getMappablePins();
         if (pinMappings.size()>2) {
            int selectionIndex = mappingList.indexOf(pinName);
            headerFile.append(String.format(
               "#if %s_SEL == %d\n",
               info.function.getName(), selectionIndex
               ));
         }
         headerFile.append(String.format(
               "#define %-18s         %-3s   //!< %s %s number\n"+
               "#define %-18s         %-3s   //!< %s %s channel\n",
               pinInfo.getName()+"_"+baseName+"_NUM"+modifier, name,  pinName, baseName,
               pinInfo.getName()+"_"+baseName+"_CH"+modifier,  chNum, pinName, baseName
               ));
         headerFile.append(String.format(
               "#define %-18s         %-3d   //!< %s Pin multiplexor for %s\n",
               pinInfo.getName()+"_"+baseName+"_FN"+modifier, pinMux,    pinName, baseName
               ));
         if (pinMappings.size()>2) {
            headerFile.append("#endif\n");
         }
      }
   
      /*
       * Do FTM functions
       */
      mappingList = pinInfo.getMappingList("FTM");
      for(MappingInfo info:mappingList) {
         String baseName  = info.function.fBaseName;
         String name      = info.function.fInstance;
         String chNum     = info.function.fSignal;
         int    pinMux    = info.mux; 
         PeripheralFunction peripheralFunction = info.function;
         ArrayList<PinInformation> pinMappings = peripheralFunction.getMappablePins();
         if (pinMappings.size()>2) {
            // Function can be mapped to more than one pin
            int selectionIndex = pinMappings.indexOf(pinInfo);
            headerFile.append(String.format(
               "#if %s_SEL == %d\n",
               info.function.getName(), selectionIndex
               ));
         }
         headerFile.append(String.format(
               "#define %-18s         %-3s   //!< %s %s number\n"+
               "#define %-18s         %-3s   //!< %s %s channel\n",
               pinInfo.getName()+"_"+baseName+"_NUM", name,  pinName, baseName,
               pinInfo.getName()+"_"+baseName+"_CH",  chNum, pinName, baseName
               ));
         headerFile.append(String.format(
               "#define %-18s         %-3d   //!< %s Pin multiplexor for %s\n",
               pinInfo.getName()+"_"+baseName+"_FN", pinMux,    pinName, baseName
               ));
         if (pinMappings.size()>2) {
            headerFile.append("#endif\n");
         }
      }
      /*
       * Do TPM functions
       */
      mappingList = pinInfo.getMappingList("TPM");
      for(MappingInfo info:mappingList) {
         String baseName  = info.function.fBaseName;
         String name      = info.function.fInstance;
         String chNum     = info.function.fSignal;
         int    pinMux    = info.mux;
         PeripheralFunction peripheralFunction = info.function;
         ArrayList<PinInformation> pinMappings = peripheralFunction.getMappablePins();
         if (pinMappings.size()>2) {
            // Function can be mapped to more than one pin
            int selectionIndex = pinMappings.indexOf(pinInfo);
            headerFile.append(String.format(
                  "#if %s_SEL == %d\n",
                  info.function.getName(), selectionIndex
                  ));
         }
         headerFile.append(String.format(
               "#define %-18s         %-3s   //!< %s %s number\n"+
               "#define %-18s         %-3s   //!< %s %s channel\n",
               pinInfo.getName()+"_"+baseName+"_NUM", name,  pinName, baseName,
               pinInfo.getName()+"_"+baseName+"_CH",  chNum, pinName, baseName
               ));
         headerFile.append(String.format(
               "#define %-18s         %-3d   //!< %s Pin multiplexor for %s\n",
               pinInfo.getName()+"_"+baseName+"_FN", pinMux,    pinName, baseName
               ));
         if (pinMappings.size()>2) {
            headerFile.append("#endif\n");
         }
      }
      for (PinFunctionDescription pinFunctionDescription:pinFunctionDescriptions) {
         mappingList = pinInfo.getMappingList(pinFunctionDescription.baseName);
         for(MappingInfo info:mappingList) {
            String baseName  = info.function.fBaseName;
            int    pinMux    = info.mux;
            PeripheralFunction peripheralFunction = info.function;
            ArrayList<PinInformation> pinMappings = peripheralFunction.getMappablePins();
            if (pinMappings.size()>1) {
               int selectionIndex = pinMappings.indexOf(pinInfo);
               if (selectionIndex < 0) {
                  System.err.println(pinMappings);
                  throw new Exception("Cannot find index of " + pinName);
               }
               headerFile.append(String.format(
                  "#if %s_SEL == %d\n",
                  info.function.getName(), selectionIndex
                  ));
            }
            headerFile.append(String.format(
                  "#define %-18s         %-3d   //!< %s Pin multiplexor for %s\n",
                  peripheralFunction.getName()+"_FN", pinMux,    pinName, baseName
                  ));
            headerFile.append(String.format(
               "#define %-18s         digitalIO_%-3s   //!< %s %s GPIO\n",
               peripheralFunction.getName()+"_GPIO",  pinName, pinName, baseName
               ));
            if (pinMappings.size()>1) {
               headerFile.append("#endif\n");
            }
         }
      }
      headerFile.append("\n");
   }

   /**
    * Writes macros describing pin functions for all pins
    * e.g.<pre>
    * // PTB2 = ADC0_SE12,GPIOB_2,I2C0_SCL (Alias: D15)
    * #define PTB2_GPIO_NAME   B                //!< PTB2 GPIO name
    * #define PTB2_GPIO_BIT    2                //!< PTB2 GPIO bit number
    * #define PTB2_GPIO_FN     1                //!< PTB2 Pin multiplexor for GPIO
    * #define PTB2_ADC_NUM     0                //!< PTB2 ADC number
    * #define PTB2_ADC_CH      12               //!< PTB2 ADC channel
    * #define PTB2_ADC_FN      0                //!< PTB2 Pin multiplexor for ADC
    * #if I2C0_SCL_SEL == 2                     
    * #define I2C0_SCL_FN      2                //!< PTB2 Pin multiplexor for I2C
    * #define I2C0_SCL_GPIO    digitalIO_PTB2   //!< PTB2 I2C GPIO
    * #endif
    * </pre>
    * 
    * @param headerFile Header file to write to
    * 
    * @throws Exception 
    */
   private void writePinDefines(BufferedWriter headerFile) throws Exception {
      for(String pinName : PinInformation.getPinNames()) {
         writeDefinesForPin(PinInformation.find(pinName), headerFile);
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
      if (portClockRegisterChanged) {
         headerFile.append(String.format("#define PORT_CLOCK_REG_CHANGES     //!< Indicates GPIO Clock varies with pin\n"));
      }
      else {
         headerFile.append(String.format("#define DEFAULT_PORT_CLOCK_REG  %s //!< Fixed PORT Clock varies with port\n", portClockRegisterValue));
      }
   }

   /**
    * Writes pin-mapping selection code to header file
    * e.g.<pre>
    * // SPI0_PCS2 Pin Mapping
    * //   &lt;o> SPI0_PCS2 Pin Selection [PTC2(D10), PTD5(A3)] 
    * //   &lt;i> Selects which pin is used for SPI0_PCS2
    * //     &lt;0=> Disabled
    * //     &lt;1=> PTC2 (Alias: D10)
    * //     &lt;2=> PTD5 (Alias: A3)
    * //     &lt;0=> Default
    * </pre>
    *  
    * @param peripheralFunction  Peripheral function to write definitions for
    * @param writer              Where to write
    * 
    * @throws IOException
    */
   private void writePeripheralPinMapping(PeripheralFunction peripheralFunction, BufferedWriter writer) throws IOException {
      ArrayList<PinInformation> portMap = peripheralFunction.getMappablePins();
      boolean isConstant = false;
      if ((portMap.size() <= 2) && peripheralFunction.getName().startsWith("GPIO")) {
         return;
      }
      if ((portMap.size() <= 2) && peripheralFunction.getName().startsWith("SDHC")) {
         return;
      }
      if ((portMap.size() <= 2) && peripheralFunction.getName().startsWith("ADC")) {
         peripheralFunction.setPreferredPin(1);
         isConstant = true;
      }
      if ((portMap.size() <= 2) && peripheralFunction.getName().startsWith("FTM")) {
         peripheralFunction.setPreferredPin(1);
         isConstant = true;
      }
      if ((portMap.size() <= 2) && peripheralFunction.getName().startsWith("TPM")) {
         peripheralFunction.setPreferredPin(1);
         isConstant = true;
      }
      StringBuilder sb = new StringBuilder();
      for(PinInformation mappedPin:portMap) {
         if (mappedPin == PinInformation.DISABLED_PIN) {
            continue;
         }
         if (sb.length()>0) {
            sb.append(", ");
         }
         String name = mappedPin.getName();
         String aliasName = getAliasList(mappedPin);
//         System.err.println(mappedPin + " => " + aliasName);
         if (aliasName != null) {
            name += "(" + aliasName + ")";
         }
         sb.append(name);
      }
      writeWizardOptionSelectionPreamble(writer, 
            peripheralFunction.getName()+" Pin Mapping",
            0,
            isConstant,
            String.format("%s Pin Selection [%s]",            peripheralFunction.getName(), sb),
            String.format("Selects which pin is used for %s", peripheralFunction.getName()));
      
      int selection = 0;
      int defaultSelection = peripheralFunction.getpreferredPinIndex();
      for(PinInformation mappedPin:portMap) {
         String name = mappedPin.getName();
         String aliasName = getAliasList(mappedPin);
         if (aliasName != null) {
            name += " (Alias: " + aliasName + ")";
         }
         writeWizardOptionSelectionEnty(writer, Integer.toString(selection++), name);
      }
      writeWizardDefaultSelectionEnty(writer, Integer.toString(defaultSelection));
      writeMacroDefinition(writer, peripheralFunction.getName()+"_SEL", Integer.toString(defaultSelection));
      writer.write("\n");
   }

   /**
    * Writes pin-mapping selection code for all peripherals
    *  
    * @param writer  Header file to write result
    * 
    * @throws Exception 
    */
   private void writePeripheralPinMappings(BufferedWriter writer) throws Exception {
      ArrayList<String> peripheralFunctions = new ArrayList<String>(PeripheralFunction.getFunctions().keySet());
      Collections.sort(peripheralFunctions, portNameComparator);
      for (String key:peripheralFunctions) {
         PeripheralFunction peripheralFunction = PeripheralFunction.lookup(key);
         writePeripheralPinMapping(peripheralFunction, writer);
      }
   }

//   /**
//    * Comparator for peripheral names e.g. FTM0_CH3 c.f. SPI_SCK
//    * Treats the number separately as a number.
//    */
//   private static Comparator<String> peripheralNameComparator = new Comparator<String>() {
//      @Override
//      public int compare(String arg0, String arg1) {
//         Pattern p = Pattern.compile("([^\\d]*)(\\d*)");
//         Matcher m0 = p.matcher(arg0);
//         Matcher m1 = p.matcher(arg1);
//         if (m0.matches() && m1.matches()) {
//            String t0 = m0.group(1);
//            String n0 = m0.group(2);
//            String t1 = m1.group(1);
//            String n1 = m1.group(2);
//            int r = t0.compareTo(t1);
//            if (r == 0) {
//               int no0 = -1, no1 = -1;
//               if (n0.length() > 0) {
//                  no0 = Integer.parseInt(n0);
//               }
//               if (n1.length() > 0) {
//                  no1 = Integer.parseInt(n1);
//               }
//               r = -no1 + no0;
//            }
//            return r;
//         }
//         return arg0.compareTo(arg1);
//      }
//   };

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
      writeClockMacros(headerFile);
      writeWizardSectionOpen(headerFile, "Pin Peripheral mapping");
      writePeripheralPinMappings(headerFile);
      writeWizardSectionClose(headerFile);
      writeEndWizardMarker(headerFile);
      writePinDefines(headerFile);
      writeHeaderFilePostamble(headerFile, pinMappingBaseFileName+".h");
   }

//   /**
//    * Writes aliases
//    * 
//    * @param writer
//    * @param aliasIndex
//    * @param peripheralList
//    * @param prefix
//    * @throws IOException
//    */
//   void writeAliases(BufferedWriter writer, ArrayList<String> aliasIndex, ArrayList<String> peripheralList, String prefix) throws IOException {
//      for (String aliasName:aliasIndex) {
//         PinInformation mappedName = aliasToPin.get(aliasName);
//         if ((peripheralList==null) || peripheralList.contains(mappedName)) {
//            writer.write( String.format(
//                  "#define %-25s %-20s %s\n", prefix+aliasName, prefix+mappedName, "//!< alias "+aliasName+"=>"+mappedName ));
//         }
//      }
//   }

   /**
    * Write alias for a particular function on a pin
    * 
    * @param writer           Where to write
    * @param aliasName        Alias name         e.g. "D3"
    * @param pinName          Name of pin        e.g. "PTC2"
    * @param prefix           Prefix of function e.g. "DigitalIO_"
    * 
    * @throws IOException
    */
   private void writeAlias(BufferedWriter writer, String aliasName, String pinName, String prefix) throws IOException {
      writer.write( String.format(
            "#define %-25s %-20s %s\n", prefix+aliasName, prefix+pinName, "//!< alias "+aliasName+"=>"+pinName ));
   }

   private ArrayList<PeripheralTypeDescription> deviceDescriptions      = new ArrayList<PeripheralTypeDescription>();
   private ArrayList<PinFunctionDescription>    pinFunctionDescriptions = new ArrayList<PinFunctionDescription>();
   private boolean deviceIsMKE;
   @SuppressWarnings("unused")
   private boolean deviceIsMKL;
   @SuppressWarnings("unused")
   private boolean deviceIsMKM;

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

      for (PeripheralTypeDescription deviceDescription:deviceDescriptions) {
         boolean groupDone = false;
         ArrayList<String> ar = PinInformation.getPinNames();
         for (String pinName:ar) {
            PinInformation pin = PinInformation.find(pinName);
            ArrayList<MappingInfo> listOfMappings = pin.getMappingList(deviceDescription.baseName);
            for (MappingInfo mapInfo:listOfMappings) {
               if (!groupDone) {
                  groupDone = true;
                  writeStartGroup(gpioHeaderFile, deviceDescription);
               }
               PeripheralFunction peripheralFunction = mapInfo.function;
               String baseName = peripheralFunction.fBaseName;
               String fullName = peripheralFunction.getName();
               boolean macroDone = false;
               ArrayList<PinInformation> mapEntry = peripheralFunction.getMappablePins();
               if ((mapEntry.size()>2) || (!baseName.equals("GPIO") && (mapEntry.size()>1))) {
                  macroDone = true;
                  int selectionIndex = mapEntry.indexOf(pin);
                  gpioHeaderFile.write(String.format(
                        "#if %s_SEL == %d\n",
                        fullName, selectionIndex
                        ));
               }
               gpioHeaderFile.write(String.format(deviceDescription.outputTemplate, deviceDescription.className+pinName+";", pinName));
               ArrayList<String> pinAliases = pinToAliases.get(pin);
               if (pinAliases != null) {
                  for (String pinAlias:pinAliases) {
                     writeAlias(gpioHeaderFile, pinAlias, pinName, deviceDescription.className);
                  }
               }
               if (macroDone) {
                  gpioHeaderFile.write("#endif\n");
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

      /* 
       * XXX - Write debug information
       */
      gpioHeaderFile.write("/*\n");
      ArrayList<String> ar = PeripheralFunction.getPeripheralFunctionsAsList();
      for (String functionName:ar) {
         PeripheralFunction peripheralFunction = PeripheralFunction.lookup(functionName);
         if (peripheralFunction.getName().startsWith("GPIO")) {
            continue;
         }
         ArrayList<PinInformation> mappablePins = peripheralFunction.getMappablePins();
         if ((mappablePins == null) || (mappablePins.size() < 2)) {
            continue;
         }
         String defaultMapping = mappablePins.get(peripheralFunction.getpreferredPinIndex()).getName();
         gpioHeaderFile.write(String.format("%s,%s,[,", peripheralFunction.getName(), defaultMapping));
         boolean firstOne = true;
         for (PinInformation p:mappablePins) {
            gpioHeaderFile.write(String.format((firstOne?"%s":", %s"), p.getName()));
            firstOne = false;
         }      
         gpioHeaderFile.write(",]\n");
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
            gpioHeaderFile.write(String.format("%s,%s,%s%s\n", peripheral+instance,  cInfo.clockReg, cInfo.clockMask, incomplete?"=default":""));
         }
      }
      gpioHeaderFile.write("*/\n");
      writeHeaderFilePostamble(gpioHeaderFile, gpioBaseFileName+".h");
   }

   /** 
    * Write DigitalIO instance e.g. 
    * <pre>
    * const DigitalIO digitalIO_<b><i>PTA17</i></b> = {&PCR(<b><i>PTA17</i></b>_GPIO_NAME,<b><i>PTA17</i></b>_GPIO_BIT),GPIO(<b><i>PTA17</i></b>_GPIO_NAME),PORT_CLOCK_MASK(<b><i>PTA17</i></b>_GPIO_NAME),(1UL<<<b><i>PTA17</i></b>_GPIO_BIT)};
    * </pre>
    * or for MKE devices
    * <pre>
    * const DigitalIO digitalIO_<b><i>PTA17</i></b> = {(volatile GPIO_Type*)GPIO(<b><i>PTA17</i></b>_GPIO_NAME),(1UL<<<b><i>PTA17</i></b>_GPIO_BIT)};
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * 
    * @throws IOException
    */
   void writeDigitalInstance(MappingInfo mappingInfo, BufferedWriter cppFile) throws IOException {

      String pinName          = mappingInfo.pin.getName();
      String instanceName     = "digitalIO_"+pinName;                                  // digitalIO_PTA0
      String pcrInstance      = "&PCR("+pinName+"_GPIO_NAME,"+pinName+"_GPIO_BIT),";   // &PCR(PTA0_GPIO_NAME,PTA0_GPIO_BIT)
      String gpioInstance     = "GPIO("+pinName+"_GPIO_NAME),";                        // GPIO(PTA0_GPIO_NAME),
      String gpioInstanceMKE  = "(volatile GPIO_Type*)GPIO("+pinName+"_GPIO_NAME),";   // (volatile GPIO_Type*)GPIO(PTA0_GPIO_NAME),
      String gpioClockMask    =  "PORT_CLOCK_MASK("+pinName+"_GPIO_NAME),";            // PORT_CLOCK_MASK(PTA0_GPIO_NAME),
      String gpioBitMask      = "(1UL<<"+pinName+"_GPIO_BIT)";                         // (1UL<<PTA0_GPIO_BIT)
      
      if (deviceIsMKE) {
         cppFile.write(String.format("const DigitalIO %-18s = {%-18s%s};\n", 
               instanceName, gpioInstanceMKE, gpioBitMask));
      }
      else {
         cppFile.write(String.format("const DigitalIO %-18s = {%-30s%-18s%-29s%s};\n", 
               instanceName, pcrInstance, gpioInstance, gpioClockMask, gpioBitMask));
      }
   }

   /** 
    * Write AnalogueIO instance e.g. 
    * <pre>
    * const AnalogueIO analogueIO_<b><i>PTA17</i></b> = {&digitalIO_<b><i>PTA17</i></b>, ADC(<b><i>PTA17</i></b>_ADC_NUM), &ADC_CLOCK_REG(<b><i>PTA17</i></b>_ADC_NUM), ADC_CLOCK_MASK(<b><i>PTA17</i></b>_ADC_NUM), <b><i>PTA17</i></b>_ADC_CH};
    * </pre>
    * or, if no PCR
    * <pre>
    * const AnalogueIO analogueIO_<b><i>PTA17</i></b> = {0, ADC(<b><i>PTA17</i></b>_ADC_NUM), &ADC_CLOCK_REG(<b><i>PTA17</i></b>_ADC_NUM), ADC_CLOCK_MASK(<b><i>PTA17</i></b>_ADC_NUM), <b><i>PTA17</i></b>_ADC_CH};
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param suffix         Used to create a unique name when multiple ADC are mappable to the same pin
    * @param cppFile        Where to write
    * 
    * @throws IOException
    */
   void writeAnalogueInstance(MappingInfo mappingInfo, int suffix, BufferedWriter cppFile) throws IOException {
      String modifier = "";
      if (suffix>0) {
         modifier += "_" + Integer.toString(suffix);
      }
      String pinName      = mappingInfo.pin.getName();
      String instanceName = "analogueIO_"+pinName+modifier;                      // analogueIO_PTE1
      String gpioName     = "&digitalIO_"+pinName+",";                           // &digitalIO_PTE1,
      String adcInstance  = "ADC("+pinName+"_ADC_NUM"+modifier+"),";             // ADC(PTE1_ADC_NUM),
      String adcClockReg  = "&ADC_CLOCK_REG("+pinName+"_ADC_NUM"+modifier+"),";  // &ADC_CLOCK_REG(PTE1_ADC_NUM),
      String adcClockMask = "ADC_CLOCK_MASK("+pinName+"_ADC_NUM"+modifier+"),";  // ADC_CLOCK_MASK(PTE1_ADC_NUM),
      String adcChannel   = pinName+"_ADC_CH"+modifier+"";                       // PTE1_ADC_CH
      
      if (mappingInfo.pin.getMappingList("GPIO").size() == 0) {
         // No PCR register - Only analogue function on pin
         gpioName = "0,"; // NULL indicates no PCR
      }
      cppFile.write(String.format("const AnalogueIO %-28s = {%-18s%-20s%-31s%-31s%s};\n", 
            instanceName, gpioName, adcInstance, adcClockReg, adcClockMask, adcChannel));
   }

   /** 
    * Write PwmIO instance for a FTM e.g. 
    * <pre>
    * const PwmIO  pwmIO_<b><i>PTA17</i></b> = {&digitalIO_<b><i>PTA17</i></b>, (volatile FTM_Type*)FTM(<b><i>PTA17</i></b>_FTM_NUM), <b><i>PTA17</i></b>_FTM_CH, PORT_PCR_MUX(<b><i>PTA17</i></b>_FTM_FN), &FTM_CLOCK_REG(<b><i>PTA17</i></b>_FTM_NUM), FTM_CLOCK_MASK(<b><i>PTA17</i></b>_FTM_NUM), <b><i>FTM0</b></i>_SC};
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * 
    * @note It is not allowable to simultaneously map multiple FTMs to the same pin so no suffix is used.
    * 
    * @throws IOException
    */
   void writePwmIOInstanceFromFTM(PinInformation pin, MappingInfo mappedPeripheral, BufferedWriter cppFile) throws IOException {
      String ftmNum       = mappedPeripheral.function.fInstance;
      String pinName      = mappedPeripheral.pin.getName();
      
      String instanceName = "pwmIO_"+pinName;                                 // pwmIO_PTA0
      String gpioName     = "&digitalIO_"+pinName+",";                        // &digitalIO_PTA0,
      String ftmInstance  = "(volatile FTM_Type*)FTM("+pinName+"_FTM_NUM),";  // (volatile FTM_Type*)FTM(PTA0_FTM_NUM),
      String ftmChannel   = pinName+"_FTM_CH,";                               // PTA0_FTM_CH
      String ftmMuxValue  = "PORT_PCR_MUX("+pinName+"_FTM_FN),";              // PORT_PCR_MUX(PTA0_FTM_FN);
      String ftmClockReg  = "&FTM_CLOCK_REG("+pinName+"_FTM_NUM),";           // &FTM_CLOCK_REG(PTA0_FTM_NUM),
      String ftmClockMask = "FTM_CLOCK_MASK("+pinName+"_FTM_NUM),";           // FTM_CLOCK_MASK(PTA0_FTM_NUM),
      String ftmSCValue   = "FTM"+ftmNum+"_SC";                               // FTM0_SC
      
      cppFile.write(String.format("const PwmIO  %-15s = {%-19s%-40s%-15s%-28s%-31s%s %s};\n", 
            instanceName, gpioName, ftmInstance, ftmChannel, ftmMuxValue, ftmClockReg, ftmClockMask,ftmSCValue) );
   }

   /** 
    * Write PwmIO instance for a TPM e.g. 
    * <pre>
    * const PwmIO  pwmIO_<b><i>PTA17</i></b> = {&digitalIO_<b><i>PTA17</i></b>, (volatile TPM_Type*)TPM(<b><i>PTA17</i></b>_TPM_NUM), <b><i>PTA17</i></b>_TPM_CH, PORT_PCR_MUX(<b><i>PTA17</i></b>_TPM_FN), &TPM_CLOCK_REG(<b><i>PTA17</i></b>_TPM_NUM), TPM_CLOCK_MASK(<b><i>PTA17</i></b>_TPM_NUM), <b><i>TPM0</b></i>_SC};
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * 
    * @note It is not allowable to simultaneously map multiple TPMs to the same pin so no suffix is used.
    * 
    * @throws IOException
    */
   void writePwmIOInstanceFromTPM(PinInformation pin, MappingInfo mappedPeripheral, BufferedWriter cppFile) throws IOException {
      String ftmNum       = mappedPeripheral.function.fInstance;
      String pinName      = mappedPeripheral.pin.getName();
      
      String instanceName = "pwmIO_"+pinName;                                 // pwmIO_PTA0
      String gpioName     = "&digitalIO_"+pinName+",";                        // &digitalIO_PTA0,
      String ftmInstance  = "(volatile TPM_Type*)TPM("+pinName+"_TPM_NUM),";  // (volatile TPM_Type*)TPM(PTA0_TPM_NUM),
      String ftmChannel   = pinName+"_TPM_CH,";                               // PTA0_TPM_CH
      String ftmMuxValue  = "PORT_PCR_MUX("+pinName+"_TPM_FN),";              // PORT_PCR_MUX(PTA0_TPM_FN);
      String ftmClockReg  = "&TPM_CLOCK_REG("+pinName+"_TPM_NUM),";           // &TPM_CLOCK_REG(PTA0_TPM_NUM),
      String ftmClockMask = "TPM_CLOCK_MASK("+pinName+"_TPM_NUM),";           // TPM_CLOCK_MASK(PTA0_TPM_NUM),
      String ftmSCValue   = "TPM"+ftmNum+"_SC";                               // TPM0_SC
      
      cppFile.write(String.format("const PwmIO  %-15s = {%-19s%-40s%-15s%-28s%-31s%s %s};\n", 
            instanceName, gpioName, ftmInstance, ftmChannel, ftmMuxValue, ftmClockReg, ftmClockMask,ftmSCValue) );
   }

   /**                    
   * Write CPP file      
   *                     
   * @param cppFile      
   * @throws IOException 
   */                    
   private void writeGpioCppFile(BufferedWriter cppFile) throws IOException {
      String description = "Pin declarations for " + deviceName;
      writeCppFilePreable(cppFile, gpioBaseFileName+".cpp", gpioCppFileName, description);
      writeHeaderFileInclude(cppFile, "utilities.h");
      writeHeaderFileInclude(cppFile, "gpio.h");
      writeHeaderFileInclude(cppFile, "pin_mapping.h");
      cppFile.write("\n");
      if (PeripheralFunction.getFunctionsByBaseName("FTM") != null) {
         writeMacroDefinition(cppFile, 
               "Create Timer Clock register name from timer number", 
               "number Timer number e.g. 1 = FTM1_CLOCK_REG", 
               "FTM_CLOCK_REG(number)", 
               "CONCAT3_(FTM,number,_CLOCK_REG)");
         writeMacroDefinition(cppFile, 
               "Create Timer Clock register mask from timer number", 
               "number Timer number e.g. 1 = FTM1_CLOCK_MASK", 
               "FTM_CLOCK_MASK(number)", 
               "CONCAT3_(FTM,number,_CLOCK_MASK)");
      }
      if (PeripheralFunction.getFunctionsByBaseName("TPM") != null) {
         writeMacroDefinition(cppFile, 
               "Create Timer Clock register name from timer number", 
               "number Timer number e.g. 1 = TPM1_CLOCK_REG", 
               "TPM_CLOCK_REG(number)", 
               "CONCAT3_(TPM,number,_CLOCK_REG)");
         writeMacroDefinition(cppFile, 
               "Create Timer Clock register mask from timer number", 
               "number Timer number e.g. 1 = TPM1_CLOCK_MASK", 
               "TPM_CLOCK_MASK(number)", 
               "CONCAT3_(TPM,number,_CLOCK_MASK)");
      }
      if (PeripheralFunction.getFunctionsByBaseName("ADC") != null) {
         writeMacroDefinition(cppFile, 
               "Create ADC Clock register name from ADC number", 
               "number Timer number e.g. 1 = ADC1_CLOCK_REG", 
               "ADC_CLOCK_REG(number)", 
               "CONCAT3_(ADC,number,_CLOCK_REG)");
         writeMacroDefinition(cppFile, 
               "Create ADC Clock register mask from ADC number", 
               "number Timer number e.g. 1 = ADC1_CLOCK_MASK", 
               "ADC_CLOCK_MASK(number)", 
               "CONCAT3_(ADC,number,_CLOCK_MASK)");
      }
      writeMacroDefinition(cppFile, "ADC(num)", "CONCAT2_(ADC,num)");
      writeMacroDefinition(cppFile, "FTM(num)", "CONCAT2_(FTM,num)");
      writeMacroDefinition(cppFile, "TPM(num)", "CONCAT2_(TPM,num)");
      cppFile.write("\n");

      /*
       * GPIOs
       */
      for (String pinName:PinInformation.getPinNames()) {
         PinInformation pin = PinInformation.find(pinName);
         ArrayList<MappingInfo> mappingInfo = pin.getMappingList("GPIO");
         for(MappingInfo mappedPeripheral:mappingInfo) {
            writeDigitalInstance(mappedPeripheral, cppFile);
         }
      }
      /*
       * ADCs
       */
      for (String pinName:PinInformation.getPinNames()) {
         PinInformation pin = PinInformation.find(pinName);
         ArrayList<MappingInfo> mappingInfo = pin.getMappingList("ADC");
         int suffix = 0;
         for(MappingInfo mappedPeripheral:mappingInfo) {
            PeripheralFunction peripheralFunction = mappedPeripheral.function;
            ArrayList<PinInformation> mapEntry = peripheralFunction.getMappablePins();
            if (mapEntry.size()>2) {
               cppFile.write(String.format(
                     "#if %s_SEL == %d\n",
                     mappedPeripheral.function.getName(), mapEntry.indexOf(pin)
                     ));
            }
            writeAnalogueInstance(mappedPeripheral, suffix, cppFile);
            suffix++;
            if (mapEntry.size()>2) {
               cppFile.write("#endif\n");
            }
         }
      }
      /*
       * FTMs
       */
      for (String pinName:PinInformation.getPinNames()) {
         PinInformation pin = PinInformation.find(pinName);
         ArrayList<MappingInfo> mappingInfo = pin.getMappingList("FTM");
         for(MappingInfo mappedPeripheral:mappingInfo) {
            PeripheralFunction peripheralFunction = mappedPeripheral.function;
            ArrayList<PinInformation> mapEntry = peripheralFunction.getMappablePins();
            if (mapEntry.size()>2) {
               cppFile.write(String.format(
                     "#if %s_SEL == %d\n",
                     mappedPeripheral.function.getName(), mapEntry.indexOf(pin)
                     ));
            }
            writePwmIOInstanceFromFTM(pin, mappedPeripheral, cppFile);
            if (mapEntry.size()>2) {
               cppFile.write("#endif\n");
            }
         }
      }
      /*
       * TPMs
       */
      for (String pinName:PinInformation.getPinNames()) {
         PinInformation pin = PinInformation.find(pinName);
         ArrayList<MappingInfo> mappingInfo = pin.getMappingList("TPM");
         for(MappingInfo mappedPeripheral:mappingInfo) {
            PeripheralFunction peripheralFunction = mappedPeripheral.function;
            ArrayList<PinInformation> mapEntry = peripheralFunction.getMappablePins();
            if (mapEntry.size()>2) {
               int selectionIndex = mapEntry.indexOf(pin);
               cppFile.write(String.format(
                     "#if %s_SEL == %d\n",
                     mappedPeripheral.function.getName(), selectionIndex
                     ));
            }
            writePwmIOInstanceFromTPM(pin, mappedPeripheral, cppFile);
            if (mapEntry.size()>2) {
               cppFile.write("#endif\n");
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
      
      deviceName = filePath.getFileName().toString().replace(".csv", "");
      deviceIsMKE = deviceName.startsWith("MKE");
      deviceIsMKL = deviceName.startsWith("MKL");
      deviceIsMKM = deviceName.startsWith("MKL");

      deviceDescriptions.add(new PeripheralTypeDescription("GPIO", "DigitalIO_Group",  "Digital Input/Output",               "Allows use of port pins as simple digital inputs or outputs", "digitalIO_",  "extern const DigitalIO %-24s //!< DigitalIO on %s\n"));
      if (!deviceIsMKE) {
         deviceDescriptions.add(new PeripheralTypeDescription("ADC",  "AnalogueIO_Group", "Analogue Input",                     "Allows use of port pins as analogue inputs",                  "analogueIO_", "extern const AnalogueIO %-24s //!< AnalogueIO on %s\n"));
         deviceDescriptions.add(new PeripheralTypeDescription("FTM",  "PwmIO_Group",      "PWM, Input capture, Output compare", "Allows use of port pins as PWM outputs",                      "pwmIO_",      "extern const PwmIO  %-24s //!< PwmIO on %s\n"));
         deviceDescriptions.add(new PeripheralTypeDescription("TPM",  "PwmIO_Group",      "PWM, Input capture, Output compare", "Allows use of port pins as PWM outputs",                      "pwmIO_",      "extern const PwmIO  %-24s //!< PwmIO on %s\n"));
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
