package net.sourceforge.usbdm.deviceEditor.information;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.jface.dialogs.DialogSettings;

import net.sourceforge.usbdm.deviceEditor.peripherals.DocumentUtilities;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;

/**
 * Represents a peripheral.<br>
 * Includes
 * <li>Name e.g. FTM0
 * <li>Class-name e.g. Ftm0
 * <li>Base-name e.g. FTM0 => FTM
 * <li>Instance e.g. FTM0 => 0
 * <li>Clock register mask e.g. ADC0_CLOCK_REG
 * <li>Clock register e.g. SIM->SCGC6
 */
public abstract class Peripheral {
   
   /** Device information */
   protected final DeviceInfo fDeviceInfo;

   /** Name of peripheral e.g. FTM2 */
   protected final String fName;

   /** Name of C peripheral class e.g. Ftm2 */
   private final String fClassName;
   
   /** Base name of the peripheral e.g. FTM0 = FTM, PTA = PT */
   private final String fBaseName;
   
   /** Instance name/number of the peripheral instance e.g. FTM0 = 0, PTA = A */
   private final String fInstance;
   
   /** Clock register e.g. SIM->SCGC6 */
   private String fClockReg = null;

   /** Clock register mask e.g. ADC0_CLOCK_REG */
   private String fClockMask = null;
   
   /** Hardware interrupt numbers */
   private final ArrayList<String> fIrqNums = new ArrayList<String>();
   
   /** IRQ handler name */
   private String fIrqHandler;
   
   /** List of DMA channels */
   private ArrayList<DmaInfo> fDmaInfoList = new ArrayList<DmaInfo>();

   /** Map of all functions on this peripheral */
   private TreeMap<String, Signal> fFunctions = new TreeMap<String, Signal>(Signal.comparator);
   
   /**
    * Create peripheral
    * 
    * @param baseName      Base name e.g. FTM3 => FTM
    * @param instance      Instance e.g. FTM3 => 3
    * @param writerBase    Description of peripheral
    * @param template      The template associated with this peripheral 
    * @param deviceInfo 
    */
   protected Peripheral(String baseName, String instance, DeviceInfo deviceInfo) {
      fBaseName      = baseName;
      fInstance      = instance;
      fDeviceInfo    = deviceInfo;
      
      fName          = baseName+instance;
      fClassName     = baseName.substring(0, 1).toUpperCase()+baseName.substring(1).toLowerCase()+instance;
   }
   
   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("Peripheral(");
      sb.append("N="+fName+", ");
      sb.append(")");
      return sb.toString();
   }

   /**
    * Get name of peripheral e.g. FTM2 
    * 
    * @return
    */
   public String getName() {
      return fName;
   }

   /**
    * Get base name of the peripheral e.g. FTM0 = FTM, PTA = PT 
    * 
    * @return
    */
   public String getBaseName() {
      return fBaseName;
   }

   /**
    * Get instance name/number of the peripheral instance e.g. FTM0 = 0, PTA = A 
    * 
    * @return
    */
   public String getInstance() {
      return fInstance;
   }

   /**
    * Get name of C peripheral class e.g. Ftm2 
    * 
    * @return
    */
   public String getClassName() {
      return fClassName;
   }

   /**
    * Set clock information
    * 
    * @param clockReg   Clock register name e.g. SCGC5
    * @param clockMask  Clock register mask e.g. SIM_SCGC5_PORTB_MASK
    */
   public void setClockInfo(String clockReg, String clockMask) {
      this.fClockReg  = clockReg;
      this.fClockMask = clockMask;
   }
   
   /**
    * Get clock register e.g. SIM->SCGC6 
    * 
    * @return
    */
   public String getClockReg() {
      return fClockReg;
   }

   /**
    * Get clock register mask e.g. ADC0_CLOCK_REG 
    * 
    * @return
    */
   public String getClockMask() {
      return fClockMask;
   }

   /**
    * Get map of all functions on this peripheral
    * @return
    */
   public TreeMap<String, Signal> getFunctions() {
      return fFunctions;
   }

   /**
    * Add an interrupt number for this peripheral
    * 
    * @param irqNum
    */
   public void addIrqNum(String irqNum) {
      this.fIrqNums.add(irqNum);
   }

   /**
    * Get interrupt numbers for this peripheral
    * 
    * @return List
    */
   public ArrayList<String> getIrqNums() {
      return fIrqNums;
   }

   /**
    * Get number of interrupts for this peripheral
    * @param irqNum
    */
   public int getIrqCount() {
      return fIrqNums.size();
   }

   /**
    * Add an interrupt handler for this peripheral
    * 
    * @param irqNum
    */
   public void setIrqHandler(String irqHandler) {
      this.fIrqHandler  = irqHandler;
   }
   
   /**
    * Get interrupt handler for this peripheral
    * 
    * @param irqNum
    */
   public String getIrqHandler() {
      return fIrqHandler;
   }

   /**
    * Get interrupt numbers for this peripheral as C initialiser
    * e.g. <pre>3,7,9</pre>
    * 
    * @return null if none, otherwise string
    */
   public String getIrqNumsAsInitialiser() {
      if (fIrqNums.isEmpty()) {
         return null;
      }
      StringBuffer buff = new StringBuffer();
      boolean firstElement = true;
      for (String num:fIrqNums) {
         if (!firstElement) {
            buff.append(", ");
         }
         buff.append(num);
         firstElement = false;
      }
      return buff.toString();
   }

   /**
    * Write Peripheral to XML file<br>
    * 
    * <pre>
    *   &lt;peripheral name="PIT"&gt;
    *      &lt;clock clockReg="SCGC6"    clockMask="SIM_SCGC6_PIT_MASK" /&gt;
    *      &lt;irq num="PIT0_IRQn" /&gt;
    *      &lt;irq num="PIT1_IRQn" /&gt;
    *      &lt;irq num="PIT2_IRQn" /&gt;
    *      &lt;irq num="PIT3_IRQn" /&gt;
    *   &lt;/peripheral&gt;
    * </pre>
    * @param documentUtilities Where to write
    * 
    * @throws IOException 
    */
   public void writeXmlInformation(XmlDocumentUtilities documentUtilities) throws IOException {
      documentUtilities.openTag("peripheral");
      documentUtilities.writeAttribute("baseName", fBaseName);
      documentUtilities.writeAttribute("instance", fInstance);

      documentUtilities.openTag("handler");
      documentUtilities.writeAttribute("class", this.getClass().getName());
      documentUtilities.closeTag();

      // Additional, peripheral specific, information
      if ((fClockReg != null) || (fClockMask != null)) {
         documentUtilities.openTag("clock");
         if (fClockReg != null) {
            documentUtilities.writeAttribute("clockReg",  fClockReg);
         }
         if (fClockMask != null) {
            documentUtilities.writeAttribute("clockMask", fClockMask);
         }
         documentUtilities.closeTag();
      }
      if (fIrqHandler != null) {
         documentUtilities.writeAttribute("irqHandler",  fIrqHandler);
      }
      for (String irq:fIrqNums) {
         documentUtilities.openTag("irq");
         documentUtilities.writeAttribute("num", irq);
         documentUtilities.closeTag();
      }
      writeExtraDefinitions(documentUtilities);
      documentUtilities.closeTag();
   }

   /**
    * Add DMA channel to peripheral
    * 
    * @param dmaChannelNumber    Channel number in DMA mux i.e. 0-63
    * @param dmaSource           DMA source e.g. UART0_Receive
    * 
    * @return  Channel created
    */
   public DmaInfo addDmaChannel(int dmaChannelNumber, String dmaSource) {
      DmaInfo dmaInfo = new DmaInfo(this, dmaChannelNumber, dmaSource);
      fDmaInfoList.add(dmaInfo);
      return dmaInfo;
   }
   
   /**
    * Get list of DMA channels
    * @return
    */
   public List<DmaInfo> getDmaInfoList() {
      return fDmaInfoList;
   }

   /** Key for mux selection persistence */
   public static final String MUX_SETTINGS_KEY = "muxSetting"; 
   
   /** Key for description selection persistence */
   public static final String DESCRIPTION_SETTINGS_KEY = "descriptionSetting"; 
   
   /**
    * Load pin settings from settings object
    * 
    * @param settings Settings object
    */
   public void loadSettings(DialogSettings settings) {
   }

   /**
    * Save pin settings to settings object
    * 
    * @param settings Settings object
    */
   public void saveSettings(DialogSettings settings) {
   }

   /** 
    * Class used to hold different classes of peripheral functions 
    */
   public class InfoTable {
      /** Functions that use this writer indexed by function index */
      public  Vector<Signal> table = new Vector<Signal>();
      private String fName;
      
      public InfoTable(String name) {
         fName = name;
      }
      public String getName() {
         return fName;
      }
   }
   
   static final String INFO_TABLE_NAME = "info";
   
   /** Functions that use this writer */
   protected InfoTable fPeripheralFunctions = new InfoTable(INFO_TABLE_NAME);

   /**
    * Get name of documentation group e.g. "DigitalIO_Group"
    * 
    * @return name
    */
   public String getGroupName() {
      return getBaseName().toUpperCase()+"_Group";
      
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
      return getBaseName() + ", " + getTitle();
   }

   /**
    * Get Documentation group brief description <br>e.g. "Allows use of port pins as simple digital inputs or outputs"
    * 
    * @return name
    */
   public String getGroupBriefDescription() {
      return "Pins used for "+getTitle();
   }

   /**
    * Get description of peripheral
    * 
    * @return
    */
   public String getDescription() {
      
      String description = getTitle();
      if (description.length()>0) {
         return getBaseName()+", " + description;
      }
      return getBaseName();
   }

   /**
    * Get instance name for a simple function e.g. <b><i>gpioA_0</b></i>
    * 
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * 
    * @return  String 
    */
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignal();
      return getClassName()+instance+"_"+signal;
   }
   
   /**
    * Get alias name based on the given alias
    * 
    * @param signalName   Function being mapped to alias
    * @param alias        Base for alias name e.g. <b><i>p36</b></i>
    * 
    * @return Alias name e.g. gpio_<b><i>p36</b></i> or <b><i>null</b></i> to suppress alias
    */
   public String getAliasName(String signalName, String alias) {
      return null;
   }
   
   /** 
    * Get alias declaration for a simple function e.g. 
    * <pre>
    * using <b><i>alias</b></i> = const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>;
    * </pre>
    * @param alias          Name of alias e.g. ftm_D8
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * 
    * @return  String 
    */
   public String getAliasDeclaration(String alias, MappingInfo mappingInfo, int fnIndex) {
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
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   /** 
    * Get a definition for a simple function
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
      return getAliasDeclaration(getInstanceName(mappingInfo, fnIndex), mappingInfo, fnIndex);
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
   public String getCTemplate() {
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
   public int getFunctionIndex(Signal function) {
      throw new RuntimeException("Method should not be called");
   }

   /**
    * Returns the PCR constant to use with pins from this peripheral
    * e.g. 
    * <pre>
    *   //! Base value for PCR (excluding MUX value)
    *   static constexpr uint32_t pcrValue  = DEFAULT_PCR;
    * </pre>
    * 
    * @return String
    */
   public String getPcrDefinition() {
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
    * @param pinMappingHeaderFile 
    * 
    * @return Definitions string
    * @throws IOException 
    */
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      StringBuffer sb = new StringBuffer();
      
      // Base address
      sb.append(String.format(
            "   //! Hardware base pointer\n"+
            "   static constexpr uint32_t basePtr   = %s\n\n",
            getName()+"_BasePtr;"
            ));

      sb.append(getPcrDefinition());
      
      if (getClockMask() != null) {
         sb.append(String.format(
               "   //! Clock mask for peripheral\n"+
               "   static constexpr uint32_t clockMask = %s;\n\n",
               getClockMask()));
      }
      if (getClockReg() != null) {
         sb.append(String.format(
               "   //! Address of clock register for peripheral\n"+
               "   static constexpr uint32_t clockReg  = %s;\n\n",
               "SIM_BasePtr+offsetof(SIM_Type,"+getClockReg()+")"));
      }
      sb.append(String.format(
            "   //! Number of IRQs for hardware\n"+
            "   static constexpr uint32_t irqCount  = %s;\n\n",
            getIrqCount()));
      if (getIrqNumsAsInitialiser() != null) {
         sb.append(String.format(
               "   //! IRQ numbers for hardware\n"+
               "   static constexpr IRQn_Type irqNums[]  = {%s};\n\n",
               getIrqNumsAsInitialiser()));
      }
      pinMappingHeaderFile.write(sb.toString());
   }

   public void writeExtraDefinitions(DocumentUtilities pinMappingHeaderFile) throws IOException {
   }
   
   public void writeExtraInfo(DocumentUtilities pinMappingHeaderFile) throws IOException {
   }
   
   public void writeExtraDefinitions(XmlDocumentUtilities documentUtilities) throws IOException {
   }

   /**
    * Add to map of all functions on this peripheral
    * 
    * @param peripheralFunction
    */
   public void addFunction(Signal function) {
      fFunctions.put(function.getName(), function);
      addFunctionToTable(function);
   }
   
   /**
    * Add to table of functions on this peripheral sorted for code generation
    * 
    * @param peripheralFunction
    */
   protected void addFunctionToTable(Signal function) {
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
               "Multiple functions mapped to index\n new = " + function + ",\n old = " + fPeripheralFunctions.table.get(signalIndex));
      }
      fPeripheralFunctions.table.setElementAt(function, signalIndex);
   }
   
   /**
    * Returns Function tables
    * 
    * @return
    */
   public ArrayList<InfoTable> getFunctionTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fPeripheralFunctions);
      return rv;
   }
   
   protected void writeInfoTable(DocumentUtilities pinMappingHeaderFile) throws IOException {      
      final String INVALID_TEMPLATE  = "         /* %-15s = %-30s */  { 0, 0, 0, INVALID_PCR,  0 },\n";
      final String DUMMY_TEMPLATE    = "         /* %-15s = %-30s */  { 0, 0, 0, UNMAPPED_PCR, 0 },\n";
      final String FIXED_TEMPLATE    = "         /* %-15s = %-30s */  { 0, 0, 0, FIXED_NO_PCR, 0 },\n";
      final String USED_TEMPLATE     = "         /* %-15s = %-30s */  { %s %d  },\n";
      final String HEADING_TEMPLATE  = "         // %-15s   %-30s   %s\n";

      if (!needPCRTable()) {
         return;
      }
      ArrayList<InfoTable> functionTables = getFunctionTables();
      for (InfoTable functionTable:functionTables) {
         if (functionTable.table.size() == 0) {
            continue;
         }
         
         String indent = "";
         if (functionTable.getName() != INFO_TABLE_NAME) {
            pinMappingHeaderFile.write(String.format(
                  "   class %s {\n"+
                  "   public:\n",
                        functionTable.getName()
                  ));
            indent = "   ";
         }
         
         pinMappingHeaderFile.write(String.format(
               indent+"   //! Information for each signal of peripheral\n"+
               indent+"   static constexpr PcrInfo  %s[] = {\n"+
               indent+"\n",
                     INFO_TABLE_NAME
               ));
         pinMappingHeaderFile.write(String.format(
               indent+HEADING_TEMPLATE,"Signal","Pin","   clockMask          pcrAddress      gpioAddress     bit  mux"));
         // Signal information table
         for (int signalIndex = 0; signalIndex<functionTable.table.size(); signalIndex++) {
            Signal peripheralFunction = functionTable.table.get(signalIndex);
            if (peripheralFunction == null) {
               pinMappingHeaderFile.write(String.format(indent+INVALID_TEMPLATE, "--", "--"));
               continue;
            }
            ArrayList<MappingInfo> mappedPins = fDeviceInfo.getPins(peripheralFunction);
            boolean valueWritten = false;
            for (MappingInfo mappedPin:mappedPins) {
               if (!mappedPin.getPin().isAvailableInPackage()) {
                  // Discard unmapped functions on this package 
                  continue;
               }
               if (mappedPin.getMux() == MuxSelection.disabled) {
                  // Disabled selection - ignore
                  continue;
               }
               if (mappedPin.getMux() == MuxSelection.reset) {
                  // Reset selection - ignore
                  continue;
               }
               if (mappedPin.getMux() == MuxSelection.fixed) {
                  // Fixed pin mapping
                  pinMappingHeaderFile.write(String.format(indent+FIXED_TEMPLATE, peripheralFunction.getName(), mappedPin.getPin().getNameWithLocation()));
                  valueWritten = true;
                  break;
               }
               if (mappedPin.getPin().getMuxValue() == mappedPin.getMux()) {
                  if (valueWritten) {
                     throw new RuntimeException("Multiple active pin mappings");
                  }
                  valueWritten = true;
                  String pcrInitString = PeripheralTemplateInformation.getPCRInitString(mappedPin.getPin());
                  pinMappingHeaderFile.write(
                        String.format(indent+USED_TEMPLATE, 
                              peripheralFunction.getName(), mappedPin.getPin().getNameWithLocation(), pcrInitString, mappedPin.getMux().value));
               }
            }
            if (!valueWritten) {
               pinMappingHeaderFile.write(String.format(indent+DUMMY_TEMPLATE, peripheralFunction.getName(), "--"));
            }
         }
         pinMappingHeaderFile.write(String.format(indent+"   };\n"));
         
         if (functionTable.getName() != INFO_TABLE_NAME) {
            pinMappingHeaderFile.write(String.format(
                  "   }; \n"
                  ));
         }
      }
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
    *   /*  4 * /  { PORTC_CLOCK_MASK, PORTC_BasePtr,  GPIOC_BasePtr,  2,  0 },
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
   public void writeInfoClass(DocumentUtilities pinMappingHeaderFile) throws IOException {

      pinMappingHeaderFile.writeDocBanner("Peripheral information for " + getGroupTitle());

      // Open class
      pinMappingHeaderFile.write(String.format(
            "class %s {\n"+
                  "public:\n",
                  getClassName()+"Info"
            ));
      
      // Additional, peripheral specific, information
      writeInfoConstants(pinMappingHeaderFile);

      // Write PCR Table
      writeInfoTable(pinMappingHeaderFile);
      
      // Write extra tables
      writeExtraInfo(pinMappingHeaderFile);
      
      // Close class
      pinMappingHeaderFile.write(String.format("};\n\n"));
      
      // Write extra definitions
      writeExtraDefinitions(pinMappingHeaderFile);
   }

   public void writeCSettings(DocumentUtilities headerFile) throws IOException {
   }

   /**
    * Indicate if the peripheral has some pins that <b>may be</b> mapped to a package location<br>
    * Used to suppress peripherals that exist but are unusable in a particular package.
    * 
    * @return
    */
   public boolean hasMappableFunctions() {
      for (String key:fFunctions.keySet()) {
         Signal function = fFunctions.get(key);
         if (function.isAvailableInPackage()) {
            return true;
         }
      }
      return false;
   }

}