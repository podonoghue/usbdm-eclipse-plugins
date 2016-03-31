package net.sourceforge.usbdm.deviceEditor.parser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInformation;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralTemplateInformation;
import net.sourceforge.usbdm.deviceEditor.information.PinInformation;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;

/**
 * Class encapsulating the code for writing an instance
 */
public abstract class WriterBase {
   
   /** Peripheral associated with this this writer */
   protected final Peripheral fPeripheral;
   
   /** Device information */
   private final DeviceInfo fDeviceInfo;
   
   /** Class used to hold different classes of peripheral functions */
   public class InfoTable {
      /** Functions that use this writer */
      public  Vector<PeripheralFunction> table = new Vector<PeripheralFunction>();
      private String fName;
      
      public InfoTable(String name) {
         fName = name;
      }
      public String getName() {
         return fName;
      }
   }
   
   /** Functions that use this writer */
   protected InfoTable fPeripheralFunctions = new InfoTable("info");

   /**
    * Create InstanceWriter
    * 
    * @param deviceType    Indicates the device family
    * @param useGuard      Indicates that <b><i>#if</b></i> ... <b><i>#endif</b></i> guards should be written
    */
   public WriterBase(DeviceInfo deviceInfo, Peripheral peripheral) {
      fPeripheral        = peripheral;
      fDeviceInfo        = deviceInfo;
   }

   /**
    * Get name of documentation group e.g. "DigitalIO_Group"
    * 
    * @return name
    */
   public String getGroupName() {
      return fPeripheral.getClassName().toUpperCase()+"_Group";
      
   }
   /**
    * Get title e.g. "Analogue Comparator"
    * 
    * @return title
    */
   public abstract String getTitle();

   /**
    * Get documentation group title e.g. "CMP, Analogue Comparator"
    * 
    * @return name
    */
   public String getGroupTitle() {
      return getPeripheralBasename() + ", " + getTitle();
   }

   /**
    * Get Documentation group brief description <br>e.g. "Allows use of port pins as simple digital inputs or outputs"
    * 
    * @return name
    */
   public abstract String getGroupBriefDescription();

   /** 
    * Indicates that <b><i>#if</b></i> ... <b><i>#endif</b></i> guards should be written 
    */
   public boolean useGuard() {
      return false;
   }

   /**
    * Get instance name e.g. <b><i>gpioA_0</b></i>
    * 
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * @param cppFile        Where to write
    * 
    * @return  String 
    */
   public abstract String getInstanceName(MappingInfo mappingInfo, int fnIndex);
   
   /** 
    * Write alias definition e.g. 
    * <pre>
    * using <b><i>alias</b></i> = const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>;
    * </pre>
    * @param alias          Name of alias e.g. ftm_D8
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * @param cppFile        Where to write
    * @throws IOException 
    * 
    * @throws Exception 
    */
   public String getAlias(String alias, MappingInfo mappingInfo, int fnIndex) {
     String declaration = getDeclaration(mappingInfo, fnIndex);
     if (declaration == null) {
        return null;
     }
     return String.format("using %-20s = %s\n", alias, declaration+";");
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i> 
    * const USBDM::Adc<b><i>0</i></b>&lt;<b><i>0</i></b>, <b><i>0</i></b>, <b><i>19</i></b>>
    * const USBDM::Adc<b><i>1</i></b>&lt;PORT<b><i>E</i></b>_CLOCK_MASK, PORT<b><i>E</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>24</i></b>]), <b><i>17</i></b>>
    * const USBDM::Ftm<b><i>1</b></i>&lt;PORT<b><i>A</i></b>_CLOCK_MASK, PORT<b><i>A</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>0</i></b>]), <i><b>3</i></b>, <i><b>17</i></b>>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * @throws IOException 
    */
   protected abstract String getDeclaration(MappingInfo mappingInfo, int fnIndex);

   /** 
    * Get a definition for a simple single-pin device 
    * <pre>
    * using gpio<b><i>A</b></i>_<b><i>0</b></i>   = const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>;
    * using adc<b><i>0</i></b>_se<b><i>19</i></b> = const USBDM::Adc<b><i>0</i></b>&lt;<b><i>0</i></b>, <b><i>0</i></b>, <b><i>19</i></b>>;
    * using adc<b><i>1</i></b>_se<b><i>17</i></b> = const USBDM::Adc<b><i>1</i></b>&lt;PORT<b><i>E</i></b>_CLOCK_MASK, PORT<b><i>E</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>24</i></b>]), <b><i>17</i></b>> ;
    * using ftm<b><i>1</i></b>_ch<b><i>17</i></b> = const USBDM::Ftm<b><i>1</b></i>&lt;PORT<b><i>A</i></b>_CLOCK_MASK, PORT<b><i>A</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>0</i></b>]), <i><b>3</i></b>, <i><b>17</i></b>>;
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * @param cppFile        Where to write
    * @throws IOException 
    */
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) throws IOException {
      return getAlias(getInstanceName(mappingInfo, fnIndex), mappingInfo, fnIndex);
   }
   
   /** 
    * Write component declaration e.g. 
    * <pre>
    * extern const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i> gpio<b><i>A</b></i>_<b><i>0</b></i>;
    * extern const USBDM::Adc<b><i>0</i></b>&lt;<b><i>19</i></b>&gt adc<b><i>A</b></i>_ch<b><i>0</b></i>;
    * extern const USBDM::Ftm<b><i>1</b></i>&lt;<i><b>17</i></b>> ftm<b><i>1</i></b>_ch<b><i>17</i></b>;
    * </pre>
    * @param mappingInfo   Mapping information (pin and peripheral function)
    * @param fnIndex       Index into list of functions mapped to pin
    * @param cppFile       Where to write
    * @throws Exception 
    */
   public String getExternDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      return "extern " +  getDefinition(mappingInfo, fnIndex);
   }

   /**
    * Get alias name based on the given alias
    * @param signalName 
    * 
    * @param signalName   Function being mapped to alias e.g. 
    * @param alias        Base for alias name e.g. <b><i>A5</b></i>
    * 
    * @return Alias name e.g. gpio_<b><i>A5</b></i>
    */
   public abstract String getAliasName(String signalName, String alias);
   
//   /**
//    * Indicates if a Peripheral Information class is required<br>
//    * The default implementation does some sanity checks and returns true if functions are present 
//    *
//    * @return
//    * @throws Exception 
//    */
//   public boolean needPeripheralInformationClass() {
//      // Assume required if functions are present
//      boolean required = fPeripheralFunctions.table.size() > 0;
//      if (!required) {
//         // Shouldn't have clock information for non-existent peripheral 
//         if ((fPeripheral.getClockReg() != null) || (fPeripheral.getClockMask() != null)) {
//            System.err.println("WARNING - Unexpected clock information for peripheral without signal" + getPeripheralName());
//            return false;
////            throw new RuntimeException("Unexpected clock information for non-present peripheral " + fOwner.fPeripheralName);
//         }
//      }
//      return required;
//   }
   
   /**
    * Indicates if a PCR table is required in the Peripheral Information class<br>
    * Default implementation checks the size of the function table
    * 
    * @return
    * @throws Exception 
    */
   public boolean needPCRTable() {
      // Assume required if functions are present
      return fPeripheralFunctions.table.size() > 0;
   }

   /**
    * Provides C template
    * 
    * @return Template
    */
   public String getTemplate() {
      return null;
   }
   
   /**
    * Gets the numeric index of the function for use in PCR tables\n
    * e.g. FTM3_Ch2 => 2 etc.
    * 
    * @param function   Function to look up
    * @return  Index, -1 is returned if function matches template but non-mapped pin
    * 
    * @throws Exception If function doesn't match template
    */
   public abstract int getFunctionIndex(PeripheralFunction function);

   /**
    * Indicates if pin aliases should be written
    * @param pinInfo 
    * 
    * @return true => write aliases
    */
   public boolean useAliases(PinInformation pinInfo) {
      return true;
   }

   /**
    * Returns the PCR constant to use with pins from this peripheral
    * e.g. <b>DEFAULT_PCR</b>
    * 
    * @return
    */
   public String getPcrValue() {
      return String.format(
            "   //! Base value for PCR (excluding MUX value)\n"+
            "   static constexpr uint32_t pcrValue  = DEFAULT_PCR;\n\n"
            );
      }

   /**
    * Returns a string containing definitions to be included in the information class describing the peripheral
    * 
    * <pre>
    * //! Clock mask for peripheral
    * static constexpr uint32_t clockMask = ADC1_CLOCK_MASK;
    * 
    * //! Address of clock register for peripheral
    * static constexpr uint32_t clockReg  = SIM_BasePtr+offsetof(SIM_Type,ADC1_CLOCK_REG);
    * </pre>
    * 
    * @return Definitions string
    */
   public String getInfoConstants() {
      StringBuffer buff = new StringBuffer();
      
      // Base address
      buff.append(String.format(
            "   //! Hardware base pointer\n"+
            "   static constexpr uint32_t basePtr   = %s\n\n",
            getPeripheralName()+"_BasePtr;"
            ));

      buff.append(getPcrValue());
      
      if (fPeripheral.getClockMask() != null) {
         buff.append(String.format(
               "   //! Clock mask for peripheral\n"+
               "   static constexpr uint32_t clockMask = %s;\n\n",
               fPeripheral.getClockMask()));
      }
      if (fPeripheral.getClockReg() != null) {
         buff.append(String.format(
               "   //! Address of clock register for peripheral\n"+
               "   static constexpr uint32_t clockReg  = %s;\n\n",
               "SIM_BasePtr+offsetof(SIM_Type,"+fPeripheral.getClockReg()+")"));
      }
      buff.append(String.format(
            "   //! Number of IRQs for hardware\n"+
            "   static constexpr uint32_t irqCount  = %s;\n\n",
            fPeripheral.getIrqCount()));
      if (fPeripheral.getIrqNumsAsInitialiser() != null) {
         buff.append(String.format(
               "   //! IRQ numbers for hardware\n"+
               "   static constexpr IRQn_Type irqNums[]  = {%s};\n\n",
               fPeripheral.getIrqNumsAsInitialiser()));
      }
      return buff.toString();
   }

   public String getExtraDefinitions() {
      return "";
   }
   
   public void writeInfoConstants(XmlDocumentUtilities documentUtilities) {
   }
   public void writeExtraDefinitions(XmlDocumentUtilities documentUtilities) {
   }
   public void writeWizard(DocumentUtilities writer) throws IOException {
   }

   public String getPcrInfoTableName(PeripheralFunction function) {
      return "info";
   }
   
   /**
    * Add to map of functions on this peripheral to write
    * 
    * @param peripheralFunction
    */
   public void addFunction(PeripheralFunction function) {
      int signalIndex = getFunctionIndex(function);
      if (signalIndex<0) {
         return;
      }
      if (signalIndex>=fPeripheralFunctions.table.size()) {
         fPeripheralFunctions.table.setSize(signalIndex+1);
      }
      if ((fPeripheralFunctions.table.get(signalIndex) != null) && 
            (fPeripheralFunctions.table.get(signalIndex) != function)) {
         throw new RuntimeException(
               "Multiple functions mapped to index new = " + function + ", old = " + fPeripheralFunctions.table.get(signalIndex));
      }
      fPeripheralFunctions.table.setElementAt(function, signalIndex);
   }
   
   public ArrayList<InfoTable> getFunctionTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fPeripheralFunctions);
      return rv;
   }
   
   /**
    * Write Peripheral Information Class<br>
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
    * @param  deviceInformation 
    * @param  pinMappingHeaderFile Where to write
    * 
    * @throws IOException 
    */
   public void writeInfoClass(DeviceInformation deviceInformation, DocumentUtilities pinMappingHeaderFile) throws IOException {
      final String DUMMY_TEMPLATE = "         /* %2d */  { 0, 0, 0, 0, 0 },\n";

      pinMappingHeaderFile.writeDocBanner("Peripheral information for " + getGroupTitle());

      // Open class
      pinMappingHeaderFile.write(String.format(
            "class %s {\n"+
                  "public:\n",
                  fPeripheral.getClassName()+"Info"
            ));
      // Additional, peripheral specific, information
      pinMappingHeaderFile.write(getInfoConstants());
      if (needPCRTable()) {

         ArrayList<InfoTable> functionTables = getFunctionTables();
         for (InfoTable functionTable:functionTables) {
            if (functionTable.table.size() == 0) {
               continue;
            }
            pinMappingHeaderFile.write(String.format(
                  "   //! Information for each pin of peripheral\n"+
                        "   static constexpr PcrInfo  %s[32] = {\n"+
                        "\n",
                  functionTable.getName()
                  ));
            pinMappingHeaderFile.write("         //          clockMask         pcrAddress      gpioAddress gpioBit muxValue\n");
            // Signal information table
            for (int signalIndex = 0; signalIndex<functionTable.table.size(); signalIndex++) {
               PeripheralFunction peripheralFunction = functionTable.table.get(signalIndex);
               if (peripheralFunction == null) {
                  pinMappingHeaderFile.write(String.format(DUMMY_TEMPLATE, signalIndex));
                  continue;
               }
               ArrayList<MappingInfo> mappedPins = fDeviceInfo.getPins(peripheralFunction);
               boolean valueWritten = false;
               int choice = 1;
               for (MappingInfo mappedPin:mappedPins) {
                  if (mappedPin.getMux() == MuxSelection.disabled) {
                     // Disabled selection - ignore
                     continue;
                  }
                  if (mappedPin.getMux() == MuxSelection.reset) {
                     // Reset selection - ignore
                     continue;
                  }
                  if (mappedPin.getMux() == MuxSelection.fixed) {
                     // Fixed pin mapping - handled by default following
                     continue;
                  }
                  if (deviceInformation.getPackage().getLocation(mappedPin.getPin()) == null) {
                     continue;
                  }
                  pinMappingHeaderFile.writeConditional(String.format("%s_PIN_SEL == %d", peripheralFunction.getName(), choice), valueWritten);
                  String pcrInitString = PeripheralTemplateInformation.getPCRInitString(mappedPin.getPin());
                  pinMappingHeaderFile.write(String.format("         /* %2d */  { %s%d },\n", signalIndex, pcrInitString, mappedPin.getMux().value));

                  valueWritten = true;
                  choice++;
               }
               if (valueWritten) {
                  pinMappingHeaderFile.writeConditionalElse();
               }
               pinMappingHeaderFile.write(String.format(DUMMY_TEMPLATE, signalIndex));
               pinMappingHeaderFile.writeConditionalEnd(valueWritten);
            }
            pinMappingHeaderFile.write(String.format("   };\n"));
         }
      }
      pinMappingHeaderFile.write(String.format("};\n\n"));
      pinMappingHeaderFile.write(getExtraDefinitions());
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
   public void writeExternDeclaration(PeripheralTemplateInformation template, MappingInfo mappedFunction, int fnIndex, DocumentUtilities gpioHeaderFile) throws IOException {
      String definition = getDefinition(mappedFunction, fnIndex);
      if (definition == null) {
         return;
      }
      boolean guardWritten = false;

      String signalName = getInstanceName(mappedFunction, fnIndex);
      if (template.useAliases(mappedFunction.getPin())) {
         String locations = mappedFunction.getFunctionList();
         if (locations != null) {
            for (String location:locations.split("/")) {
               if (!location.equalsIgnoreCase(mappedFunction.getPin().getName())) {
                  String aliasName = getAliasName(signalName, location);
                  if (aliasName!= null) {
                     String declaration = getAlias(aliasName, mappedFunction, fnIndex);
                     if (declaration != null) {
//                        if (!guardWritten) {
//                           guardWritten = writeFunctionSelectionGuardMacro(template, mappedFunction, gpioHeaderFile, guardWritten);
//                        }
//                        if (!addMacroAlias(aliasName)) {
//                           // Comment out repeated aliases
//                           gpioHeaderFile.write("//");
//                        }
                        gpioHeaderFile.write(declaration);
                     }
                  }
               }
            }
         }
      }
      gpioHeaderFile.writeConditionalEnd(guardWritten);
   }

   protected String getPeripheralName() {
      return fPeripheral.getName();
   }

   /**
    * Name of C peripheral class e.g. Ftm2 
    * 
    * @return
    */
   protected String getClassName() {
      return fPeripheral.getClassName();
   }

   /**
    * Get base name of the peripheral e.g. FTM0 = FTM, PTA = PT 
    * 
    * @return
    */
   protected String getPeripheralBasename() {
      return fPeripheral.getBaseName();
   }

   /**
    * Get clock register e.g. SIM->SCGC6 
    * 
    * @return
    */
   protected String getClockReg() {
      return fPeripheral.getClockReg();
   }

   /**
    * Get clock register mask e.g. ADC0_CLOCK_REG 
    * 
    * @return
    */
   protected String getClockMask() {
      return fPeripheral.getClockMask();
   }


}