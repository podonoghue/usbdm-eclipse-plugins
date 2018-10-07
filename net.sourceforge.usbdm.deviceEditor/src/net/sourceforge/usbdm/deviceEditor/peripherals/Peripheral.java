package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DmaInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.PcrInitialiser;
import net.sourceforge.usbdm.deviceEditor.information.Settings;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.SignalTemplate;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

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
public abstract class Peripheral extends VariableProvider {
   /** Name for default PCR value uses in Info classes */
   public static final String DEFAULT_PCR_VALUE_NAME = "defaultPcrValue";

   /** Device information */
   protected final DeviceInfo fDeviceInfo;
   
   /** Base name of C peripheral class e.g. Ftm */
   private final String fClassBaseName;
   
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

   /** Map of all signals on this peripheral */
   private TreeMap<String, Signal> fSignals = new TreeMap<String, Signal>(Signal.comparator);
   
   /** Information for signals that use this writer */
   protected InfoTable fInfoTable = new InfoTable(INFO_TABLE_NAME);

   /** Version of the peripheral e.g. adc0_diff_a */
   private String fVersion;

   /**
    * Create peripheral
    * 
    * @param baseName      Base name e.g. FTM3 => FTM
    * @param instance      Instance e.g. FTM3 => 3
    * @param writerBase    Description of peripheral
    * @param template      The template associated with this peripheral 
    * @param deviceInfo 
    * @throws UsbdmException 
    * @throws IOException 
    */
   protected Peripheral(String baseName, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(baseName+instance, deviceInfo);
      
      fBaseName       = baseName;
      fInstance       = instance;
      fDeviceInfo     = deviceInfo;

      fClassBaseName = baseName.substring(0, 1).toUpperCase()+baseName.substring(1).toLowerCase();
   }
   
   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("Peripheral(");
      sb.append("N="+getName()+", ");
      sb.append(")");
      return sb.toString();
   }

   /**
    * Get base name of the peripheral e.g. FTM0 => FTM, PTA =. PT 
    * 
    * @return
    */
   public String getBaseName() {
      return fBaseName;
   }

   /**
    * Get instance name/number of the peripheral instance e.g. FTM0 => 0, PTA => A 
    * 
    * @return
    */
   public String getInstance() {
      return fInstance;
   }

   /**
    * Get name of C peripheral class e.g. Ftm
    * 
    * @return
    */
   public String getClassBaseName() {
      return fClassBaseName;
   }

   /**
    * Get name of C peripheral class e.g. Ftm2 
    * 
    * @return
    */
   public String getClassName() {
      return getClassBaseName()+fInstance;
   }

   /**
    * Return version name of peripheral<br>
    * Defaults to name based on peripheral e.g. Ftm
    */
   public String getPeripheralVersionName() {
      return ((fVersion!=null) && !fVersion.isEmpty())?fVersion:getClassBaseName().toLowerCase();
   }

   /**
    * Set clock information
    * 
    * @param clockReg   Clock register name e.g. SCGC5
    * @param clockMask  Clock register mask e.g. SIM_SCGC5_PORTB_MASK
    */
   public void setClockInfo(String clockReg, String clockMask) {
      if ((clockReg.length()==0) || (clockMask.length()==0)) {
         throw new RuntimeException("Illegale clock info = " + clockReg + ", " + clockMask);
      }
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
    * Get map of all signals on this peripheral
    * @return
    */
   public TreeMap<String, Signal> getSignals() {
      return fSignals;
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
    * e.g. <pre>I2C0_Rx_IRQn, I2C0_Tx_IRQn,</pre>
    * 
    * @return null if none, otherwise string
    */
   public String getIrqNumsAsInitialiser() {
      if (fIrqNums.isEmpty()) {
         return null;
      }
      StringBuffer buff = new StringBuffer();
      final String indent = "\n      ";
      int elementCount = 0;
      for (String num:fIrqNums) {
         if ((elementCount++ %4) == 0) {
            buff.append(indent);
         }
         buff.append(num);
         buff.append(", ");
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
    * @throws UsbdmException 
    */
   public void writeXmlInformation(XmlDocumentUtilities documentUtilities) throws IOException, UsbdmException {
      documentUtilities.openTag("peripheral");
      documentUtilities.writeAttribute("baseName", fBaseName);
      documentUtilities.writeAttribute("instance", fInstance);
      documentUtilities.writeAttribute("version",  fVersion);

      documentUtilities.openTag("handler");
      documentUtilities.writeAttribute("class", this.getClass().getName());
      documentUtilities.closeTag();
      
      // Additional, peripheral specific, information
      if ((fClockReg != null) || (fClockMask != null)) {
         documentUtilities.openTag("clock");
         if (fClockReg != null) {
            documentUtilities.writeAttribute("reg",  fClockReg);
         }
         if (fClockMask != null) {
            documentUtilities.writeAttribute("mask", fClockMask);
         }
         documentUtilities.closeTag();
      }
      if (fIrqHandler != null) {
         documentUtilities.writeAttribute("irqHandler",  fIrqHandler);
      }
      for (String irqNum:getIrqNums()) {
         documentUtilities.openTag("irq");
         documentUtilities.writeAttribute("num", irqNum);
         documentUtilities.closeTag();
      }
      writeExtraXMLDefinitions(documentUtilities);
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

   /**
    * Load settings from settings object
    * 
    * @param settings Settings object
    */
   public void loadSettings(Settings settings) {
   }

   /**
    * Save settings to settings object
    * 
    * @param settings Settings object
    */
   public void saveSettings(Settings settings) {
   }

   /** 
    * Class used to hold different classes of peripheral signals 
    */
   public class InfoTable {
      /** Signals that use this writer indexed by signal index */
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
      return "Abstraction for "+getTitle();
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
    * Get instance name for a simple signal e.g. <b><i>gpioA_0</b></i>
    * 
    * @param mappingInfo    Mapping information (pin and signal)
    * @param fnIndex        Index into list of signals mapped to pin
    * 
    * @return  String 
    */
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignalName();
      return getClassName().toLowerCase()+"_"+signal;
   }
   
   /**
    * Get alias name based on the given alias
    * 
    * @param signalName   Signal being mapped to alias
    * @param alias        Base for alias name e.g. <b><i>p36</b></i>
    * 
    * @return Alias name e.g. Gpio_<b><i>p36</b></i> or <b><i>null</b></i> to suppress alias
    */
   public String getAliasName(String signalName, String alias) {
      String temp = getBaseName().toLowerCase();
      return Character.toUpperCase(temp.charAt(0))+ temp.substring(1)+"_"+alias;
   }
   
   /** 
    * Get alias declaration for a simple signal e.g. 
    * <pre>
    * using <b><i>alias</b></i> = const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>;
    * </pre>
    * @param alias          Name of alias e.g. ftm_D8
    * @param mappingInfo    Mapping information (pin and signal)
    * @param fnIndex        Index into list of signals mapped to pin
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
    * const USBDM::Adc<b><i>0</i></b>Channel&lt;<b><i>19</i></b>>
    * const USBDM::Ftm<b><i>1</b></i>&lt;PORT<b><i>A</i></b>_CLOCK_MASK, PORT<b><i>A</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>0</i></b>]), <i><b>3</i></b>, <i><b>17</i></b>>
    * </pre>
    * @param mappingInfo    Mapping information (pin and signal)
    * @param cppFile        Where to write
    * @throws IOException 
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   /** 
    * Get a definition for a simple signal
    * <pre>
    * using gpio<b><i>A</b></i>_<b><i>0</b></i>   = const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>;
    * using adc<b><i>0</i></b>_se<b><i>19</i></b> = const USBDM::Adc<b><i>0</i></b>&lt;<b><i>0</i></b>, <b><i>0</i></b>, <b><i>19</i></b>>;
    * using adc<b><i>1</i></b>_se<b><i>17</i></b> = const USBDM::Adc<b><i>1</i></b>&lt;PORT<b><i>E</i></b>_CLOCK_MASK, PORT<b><i>E</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>24</i></b>]), <b><i>17</i></b>> ;
    * using ftm<b><i>1</i></b>_ch<b><i>17</i></b> = const USBDM::Ftm<b><i>1</b></i>&lt;PORT<b><i>A</i></b>_CLOCK_MASK, PORT<b><i>A</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>0</i></b>]), <i><b>3</i></b>, <i><b>17</i></b>>;
    * </pre>
    * @param mappingInfo    Mapping information (pin and signal)
    * @param fnIndex        Index into list of signals mapped to pin
    * 
    * @return Definition as string
    * 
    * @throws IOException 
    */
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) throws IOException {
      return getAliasDeclaration(getInstanceName(mappingInfo, fnIndex), mappingInfo, fnIndex);
   }
   
   /**
    * Indicates if a PCR table is required in the Peripheral Information class<br>
    * Default implementation checks the size of the signal table
    * 
    * @return
    * @throws Exception 
    */
   public boolean needPCRTable() {
      // Assume required if signals are present
      return fInfoTable.table.size() > 0;
   }

   /**
    * Gets the numeric index of the signal for use in PCR tables\n
    * e.g. FTM3_Ch2 => 2 etc.
    * 
    * @param signal   Signal to look up
    * 
    * @return  Index, -1 is returned if signal matches template but non-mapped pin
    * 
    * @throws Exception if signal doesn't match template
    */
   public int getSignalIndex(Signal signal) {
      if (signal.getResetMapping().getMux() == MuxSelection.fixed) {
         return  -1;
      }
      throw new RuntimeException("Method should not be called, signal = " + signal);
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
//   public String getPcrDefinition() {
//      return String.format(
//            "   //! Base value for PCR (excluding MUX value)\n"+
//            "   static constexpr uint32_t %s  = DEFAULT_PCR;\n\n", DEFAULT_PCR_VALUE_NAME);
//   }

   /**
    * Writes definitions to be included in the information class describing the peripheral
    * 
    * <pre>
    *    //! Hardware base pointer
    *    static __attribute__((always_inline)) static volatile ADC_Type &adc() {...}
    * 
    *    //! Clock mask for peripheral
    *    static constexpr uint32_t clockMask = SIM_SCGC6_ADC0_MASK;
    * 
    *    //! Address of clock register for peripheral
    *    static __attribute__((always_inline)) static volatile uint32_t &clockReg() {...}
    * 
    *    //! Number of IRQs for hardware
    *    static constexpr uint32_t irqCount  = 1;
    * 
    *    //! IRQ numbers for hardware
    *    static constexpr IRQn_Type irqNums[]  = {
    *       ADC0_IRQn, };
    * </pre>
    * 
    * @param pinMappingHeaderFile   Where to write definitions
    * 
    * @throws IOException 
    * @throws Exception 
    */
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      StringBuffer sb = new StringBuffer();
      
      // Base address as uint32_t
      sb.append(String.format(
            "   //! Hardware base address as uint32_t \n"+
            "   static constexpr uint32_t baseAddress = %s;\n\n",
            getName()+"_BasePtr"
            ));
      // Base address as pointer to struct
      sb.append(String.format(
            "   //! Hardware base pointer\n"+
            "   __attribute__((always_inline)) static volatile %s_Type &%s() {\n"+
            "      return *(%s_Type *)baseAddress;\n"+
            "   }\n\n",
            getBaseName(), getBaseName().toLowerCase(), getBaseName()
            ));
      // Clock mask
      if (getClockMask() != null) {
         sb.append(String.format(
               "   //! Clock mask for peripheral\n"+
               "   static constexpr uint32_t clockMask = %s;\n\n",
               getClockMask()));
      }
      // Clock register
      if (getClockReg() != null) {
         sb.append(String.format(
               "   //! Address of clock register for peripheral\n"+
               "   __attribute__((always_inline)) static volatile uint32_t &clockReg() {\n"+
               "      return *%s;\n"+
               "   }\n\n",
               "(uint32_t *)(SIM_BasePtr+offsetof(SIM_Type,"+getClockReg()+"))"));
      }
      // Number of IRQs
      sb.append(String.format(
            "   //! Number of IRQs for hardware\n"+
            "   static constexpr uint32_t irqCount  = %s;\n\n",
            getIrqCount()));
      
      // Explicit IRQ numbers
      if (getIrqNumsAsInitialiser() != null) {
         sb.append(String.format(
               "   //! IRQ numbers for hardware\n"+
               "   static constexpr IRQn_Type irqNums[]  = {%s};\n\n",
               getIrqNumsAsInitialiser()));
      }
      pinMappingHeaderFile.write(sb.toString());
   }

   /**
    * Write extra information within the class
    *  
    * @param documentUtilities
    * @throws IOException
    */
   public void writeExtraInfo(DocumentUtilities pinMappingHeaderFile) throws IOException {
   }
   
   /**
    * Write USBDM namespace level information associated with a peripheral
    * 
    * @param documentUtilities
    * @throws IOException
    */
   public void writeNamespaceInfo(DocumentUtilities documentUtilities) throws IOException {
   }

   /**
    * Writes extra definitions to the device XML file
    * 
    * @param documentUtilities
    * @throws IOException
    */
   protected void writeExtraXMLDefinitions(XmlDocumentUtilities documentUtilities) throws IOException {
   }

   /**
    * Add to map of all signal on this peripheral
    * 
    * @param signal
    */
   public void addSignal(Signal signal) {
      fSignals.put(signal.getName(), signal);
      addSignalToTable(signal);
   }
   
   /**
    * Add to table of signals on this peripheral sorted for code generation
    * 
    * @param signal
    */
   protected void addSignalToTable(Signal signal) {
      int signalIndex = getSignalIndex(signal);
      if (signalIndex<0) {
         return;
      }
      if (signalIndex>=fInfoTable.table.size()) {
         fInfoTable.table.setSize(signalIndex+1);
      }
      if ((fInfoTable.table.get(signalIndex) != null) && 
            (fInfoTable.table.get(signalIndex) != signal)) {
         throw new RuntimeException(
               "Multiple signals mapped to index\n new = " + signal + ",\n old = " + fInfoTable.table.get(signalIndex));
      }
      fInfoTable.table.setElementAt(signal, signalIndex);
   }
   
   /**
    * Returns signal tables
    * 
    * @return
    */
   public ArrayList<InfoTable> getSignalTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fInfoTable);
      return rv;
   }
   
   private static final String INVALID_TEMPLATE  = "         /* %3d: %-20s = %-30s */  { NoPortInfo, 0,         INVALID_PCR,  0 },\n";
   private static final String DUMMY_TEMPLATE    = "         /* %3d: %-20s = %-30s */  { NoPortInfo, 0,         UNMAPPED_PCR, 0 },\n";
   private static final String FIXED_TEMPLATE    = "         /* %3d: %-20s = %-30s */  { NoPortInfo, 0,         FIXED_NO_PCR, 0 },\n";
   private static final String USED_TEMPLATE     = "         /* %3d: %-20s = %-30s */  { %s     PORT_PCR_MUX(%d)|"+DEFAULT_PCR_VALUE_NAME+"  },\n";
   private static final String HEADING_TEMPLATE  = "         //      %-20s   %-30s   %s\n";

   protected void writeInfoTable(DocumentUtilities pinMappingHeaderFile, InfoTable signalTable) throws IOException {
      if (signalTable.table.size() == 0) {
         return;
      }
      
      String indent = "";
      if (signalTable.getName() != INFO_TABLE_NAME) {
         // Write MACRO indicated extra Pin Table information
         pinMappingHeaderFile.writeMacroDefinition("USBDM_"+(getClassName()+"_"+signalTable.getName()).toUpperCase()+"_IS_DEFINED");

         // Open static class wrapper
         pinMappingHeaderFile.write(String.format(
               "   class %s {\n"+
               "   public:\n",
                     signalTable.getName()
               ));
         indent = "   ";
      }
      pinMappingHeaderFile.write(String.format(
            indent+"   //! Number of signals available in info table\n"+
            indent+"   static constexpr int numSignals  = %d;\n" +
            "\n",
            signalTable.table.size()));
      pinMappingHeaderFile.write(String.format(
            indent+"   //! Information for each signal of peripheral\n"+
            indent+"   static constexpr PinInfo  %s[] = {\n"+
            indent+"\n",
                  INFO_TABLE_NAME
            ));
      pinMappingHeaderFile.write(String.format(
            indent+HEADING_TEMPLATE, "Signal", "Pin","    portInfo    gpioAddress     gpioBit  PCR value"));
      // Signal information table
      int index = -1;
      for (Signal signal:signalTable.table) {
         index++;
         if (signal == null) {
            pinMappingHeaderFile.write(String.format(indent+INVALID_TEMPLATE, index, "--", "--"));
            continue;
         }
         MappingInfo mappingInfo = signal.getMappedPin();
         MappingInfo mappedPin = null;
         do {
            if (!mappingInfo.getPin().isAvailableInPackage()) {
               // Discard unmapped signals on this package 
               continue;
            }
            if (mappingInfo.getMux() == MuxSelection.unassigned) {
               // Reset selection - ignore
               continue;
            }
            if (mappingInfo.getMux() == MuxSelection.fixed) {
               // Fixed pin mapping
               pinMappingHeaderFile.write(String.format(indent+FIXED_TEMPLATE, index, signal.getName(), mappingInfo.getPin().getNameWithLocation()));
               mappedPin = mappingInfo;
               continue;
            }
            if (mappingInfo.isSelected()) {
               mappedPin = mappingInfo;
               String pinInfoInitString = SignalTemplate.getPinInfoInitString(mappingInfo.getPin());
               pinMappingHeaderFile.write(
                     String.format(indent+USED_TEMPLATE, index, 
                           signal.getName(), mappingInfo.getPin().getNameWithLocation(), pinInfoInitString, mappingInfo.getMux().value));

            }
         } while(false);
         if (mappedPin == null) {
            pinMappingHeaderFile.write(String.format(indent+DUMMY_TEMPLATE, index, signal.getName(), "--"));
         }
      }
      pinMappingHeaderFile.write(String.format(indent+"   };\n\n"));
      
      writeInitPCR(pinMappingHeaderFile, indent, signalTable);
      
      if (signalTable.getName() != INFO_TABLE_NAME) {
         // Close static class wrapper
         pinMappingHeaderFile.write(String.format(
               "   }; \n\n"
               ));
      }
      
   }

   /**
    *  Writes pin information tables
    *  
    * <pre>
    *      //! Information for each signal of peripheral
    *      static constexpr PinInfo  info[] = {
    *            //      Signal         Pin                 portInfo    gpioAddress     gpioBit       PCR value
    *            /*   0: UART0_TX     = PTB17 (ConTx) &#42;/  { PortBInfo,  GPIOB_BasePtr,  17,           PORT_PCR_MUX(3)|defaultPcrValue  },
    *            /*   1: UART0_RX     = PTB16 (ConRx) &#42;/  { PortBInfo,  GPIOB_BasePtr,  16,           PORT_PCR_MUX(3)|defaultPcrValue  },
    *            /*   2: UART0_RTS_b  = --            &#42;/  { NoPortInfo, 0,              UNMAPPED_PCR, 0 },
    *      ...
    *      };
    * </pre>

    * @param pinMappingHeaderFile
    * @throws IOException
    */
   protected void writeInfoTables(DocumentUtilities pinMappingHeaderFile) throws IOException {      

      if (!needPCRTable()) {
         return;
      }
      ArrayList<InfoTable> signalTables = getSignalTables();
      for (InfoTable signalTable:signalTables) {
         writeInfoTable(pinMappingHeaderFile, signalTable);
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
    *   ...
    *   };
    * </pre>
    * @param  deviceInformation 
    * @param  pinMappingHeaderFile Where to write
    * 
    * @throws IOException 
    */
   public void writeInfoClass(DocumentUtilities pinMappingHeaderFile) throws IOException {

      // Macro indicating presence of peripheral
      pinMappingHeaderFile.writeMacroDefinition("USBDM_"+getClassName().toUpperCase()+"_IS_DEFINED");

      pinMappingHeaderFile.writeDocBanner(
            "Peripheral information for " + getGroupTitle() + ".\n\n" + 
            "This may include pin information, constants, register addresses, and default register values,\n" + 
            "along with simple accessor functions.");

      writeNamespaceInfo(pinMappingHeaderFile);
      
      // Open class
      pinMappingHeaderFile.write(String.format(
            "class %s {\n"+
                  "public:\n",
                  getClassName()+"Info"
            ));
      // Additional, peripheral specific, information
      writeInfoConstants(pinMappingHeaderFile);

      // Write PCR Table
      writeInfoTables(pinMappingHeaderFile);
      
      // Write extra tables
      writeExtraInfo(pinMappingHeaderFile);
      
      // Close class
      pinMappingHeaderFile.write(String.format("};\n\n"));
   }

   public void writeCSettings(DocumentUtilities headerFile) throws IOException {
   }

   /**
    * Indicate if the peripheral has some pins that <b>may be</b> mapped to a package location<br>
    * Used to suppress peripherals that exist but are unusable in a particular package.
    * 
    * @return
    */
   public boolean hasMappableSignals() {
      for (String key:fSignals.keySet()) {
         Signal signal = fSignals.get(key);
         if (signal.isAvailableInPackage()) {
            return true;
         }
      }
      return false;
   }
   
   /**
    * Write initPCRs() function
    * 
    * @param pinMappingHeaderFile
    * @param indent 
    * @param signalTable
    * 
    * @throws IOException
    */
   public void writeInitPCR(DocumentUtilities pinMappingHeaderFile, String indent, InfoTable signalTable) throws IOException {

      final String INIT_PCR_FUNCTION_TEMPLATE = 
            indent+"   /**\n"+
            indent+"    * Initialise pins used by peripheral\n"+
            indent+"    * \n"+
            indent+"    * @param pcrValue PCR value controlling pin options\n"+
            indent+"    */\n"+
            indent+"   static void initPCRs(uint32_t pcrValue="+DEFAULT_PCR_VALUE_NAME+") {\n";

      final String CLEAR_PCR_FUNCTION_TEMPLATE = 
            indent+"   /**\n"+
            indent+"    * Resets pins used by peripheral\n"+
            indent+"    */\n"+
            indent+"   static void clearPCRs() {\n";

      PcrInitialiser pcrInitialiser = new PcrInitialiser();
      
      for (int index=0; index<signalTable.table.size(); index++) {
         Signal signal = signalTable.table.get(index);
         if (signal == null) {
            continue;
         }
         pcrInitialiser.addSignal(signal, "pcrValue");
      }
      
      String initClocksBuffer = pcrInitialiser.getInitPortClocksStatement(indent);

      pinMappingHeaderFile.write(INIT_PCR_FUNCTION_TEMPLATE);
      pinMappingHeaderFile.write(initClocksBuffer);
      String pcrInitStatements = pcrInitialiser.getPcrInitStatements(indent);
      if (pcrInitStatements.isEmpty()) {
         pinMappingHeaderFile.write(indent+"      (void)pcrValue;\n");
      }
      else {
         pinMappingHeaderFile.write(pcrInitStatements);
      }
      pinMappingHeaderFile.write(indent+"   }\n\n");
      
      pinMappingHeaderFile.write(CLEAR_PCR_FUNCTION_TEMPLATE);
      pinMappingHeaderFile.write(initClocksBuffer);
      pinMappingHeaderFile.write(pcrInitialiser.getPcrClearStatements(indent));
      pinMappingHeaderFile.write(indent+"   }\n\n");
   }
     
   /**
    * Search vector table for handler and replace with class static method name or user function name.<br>
    * By default, matches any handler starting with the peripheral name e.g. FTM0<br> 
    * and replaces with class name e.g. <b>FTM0_IRQHandler</b> => <b>USBDM::Ftm0::irqHandler</b><br>
    * Uses class name to create handler name<br>
    * Overridden to do special replacement
    * 
    * @param vectorTable  Vector table to search
    */
   public void modifyVectorTable(VectorTable vectorTable) {
   }
   
   /**
    * Searches array for match for signal
    * 
    * @param signal        Signal to search for
    * @param signalNames   Array of regular expressions to match against
    * 
    * @return              Index of match
    * @throws RuntimeException if not found
    */
   protected static int getSignalIndex(Signal signal, String[] signalNames) {
      for (int signalIndex=0; signalIndex<signalNames.length; signalIndex++) {
         if (signal.getSignalName().matches(signalNames[signalIndex])) {
            return signalIndex;
         }
      }
      throw new RuntimeException("Signal does not match expected pattern \'" + signal.getSignalName() + "\'");
   }

   public void setVersion(String version) {
     fVersion = version;
   }

   /**
    * @return the DeviceInfo
    */
   public DeviceInfo getDeviceInfo() {
      return fDeviceInfo;
   }

   public String getPcrValue(Signal y) {
      return "USBDM::DEFAULT_PCR";
   }

   /**
    * Model representing the pins for this peripheral<br>
    * @note may contain related pins e.g. RTC may contains OSC pins 
    */
   CategoryModel fPinModel = null;
   
   /**
    * Array of peripherals to obtain signals from
    */
   ArrayList<Peripheral> fSignalPeripherals;
   
   /**
    * Create models representing the signals for this peripheral
    * 
    * @param parent     Parent model to contain pins created
    * @param peripheral Peripheral to obtain signals from
    */
   void createSignalModels(BaseModel parent, Peripheral peripheral) {
      // Add signals from this peripheral
      TreeMap<String, Signal> signals = peripheral.getSignals();
      if (signals == null) {
         return;
      }
      for (String signalName:signals.keySet()) {
         Signal signal = peripheral.fSignals.get(signalName);
         if (signal.isAvailableInPackage()) {
            new SignalModel(parent, signal);
         }
      }
   }

   /**
    * Create models representing the signals for this peripheral
    * 
    * @param parent     Parent model to contain pins created
    * 
    * @note May add related pins e.g. RTC may contains OSC pins 
    */
   public void createSignalModels(BaseModel parent) {
      // Add signals from this peripheral
      createSignalModels(parent, this);
      if (fSignalPeripherals == null) {
         return;
      }
      for (Peripheral peripheral:fSignalPeripherals) {
         // Add signals from referenced peripherals
         createSignalModels(parent, peripheral);
      }
   }

   /**
    * Create models representing the signals for this peripheral
    * 
    * @param parentModel 
    * 
    * @note May add related pins e.g. RTC may contains OSC pins 
    * 
    * @return Category model holding signals
    */
   public void createSignalModel() {
      if (fPinModel == null) {
         return;
      }
      fPinModel.removeChildren();
      createSignalModels(fPinModel);
   }

   /**
    * Add peripheral as source for signals for this peripheral
    * 
    * @param parentModel   Model to contain signal category
    * @param peripheral    Peripheral to obtain signals from
    * 
    * @return Category model to hold signals when created
    */
   public CategoryModel addSignals(BaseModel parentModel, Peripheral peripheral) {
      if (fPinModel == null) {
         fPinModel = new CategoryModel(parentModel, "Signals", "Signals for this peripheral");
      }
      if (peripheral != this) {
         if (fSignalPeripherals == null) {
            fSignalPeripherals = new ArrayList<Peripheral>();
         }
         fSignalPeripherals.add(peripheral);
      }
      return fPinModel; 
   }

   /**
    * Get model representing the pins for this peripheral<br>
    * @note may contain related pins e.g. RTC may contains OSC pins 
    * 
    * @return Category model
    */
   public CategoryModel getPinModel() {
      return fPinModel;
   }

   /**
    * Set model representing the pins for this peripheral<br>
    * 
    * @param pinModel Category model
    */
   public void setPinModel(CategoryModel pinModel) {
      fPinModel = pinModel;
   }

   /**
    * Indicates if the peripheral represents real hardware
    * 
    * @return true if not real hardware
    */
   public boolean isSynthetic() {
      return false;
   }

}