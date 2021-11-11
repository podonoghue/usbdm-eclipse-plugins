package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModelInterface;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
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
public abstract class Peripheral extends VariableProvider implements ObservableModelInterface {

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
   
   /** Hardware interrupt numbers */
   private final ArrayList<String> fIrqNums = new ArrayList<String>();

   /** Key used for peripheral declarations in C code */
   private final String PERIPHERAL_DECLARATIONS_KEY = makeKey("Declarations");

   /** Key used to save/restore identifier used for code generation */
   private final String CODE_IDENTIFIER_KEY  = "$peripheral$"+getName()+"_codeIdentifier";
   
   /** Key used to save/restore user description */
   private final String USER_DESCRIPTION_KEY = "$peripheral$"+getName()+"_userDescription";

   /** User description of peripheral use */
   private String fUserDescription;

   /** IRQ handler name */
   private String fIrqHandler;
   
   /** List of DMA channels */
   private ArrayList<DmaInfo> fDmaInfoList = new ArrayList<DmaInfo>();

   /** Map of all signals on this peripheral */
   private TreeMap<String, Signal> fSignals = new TreeMap<String, Signal>(Signal.comparator);
   
   /** Name for information table in generated code */
   private static final String INFO_TABLE_NAME = "info";

   /** Information for signals that use this writer */
   protected InfoTable fInfoTable = new InfoTable(INFO_TABLE_NAME);

   /** Version of the peripheral e.g. adc0_diff_a */
   private String fVersion;

   /** Command to enable peripheral clock */
   private String fClockEnable;

   /** Command to disable peripheral clock */
   private String fClockDisable;

   /** C identifier used for this peripheral */
   private String fCodeIdentifier = "";
   
   /** Proxy used to support ObservableModel interface */
   private final ObservableModel fProxy;
   
   /** Indicates the class representing this peripheral is const (May be placed in ROM) - default true */
   private boolean fIsConstType = true;
   
   /** Indicates the peripheral as synthetic i.e. no hardware is associated - default false */
   private boolean fIsSynthetic = false;
   
   /**
    * Sets the peripheral as synthetic i.e. no hardware is associated
    */
   protected void setSynthetic() {
      fIsSynthetic = true;
   }
   
   /**
    * Sets the peripheral as a non-const variable i.e. cannot be placed in ROM 
    */
   protected void clearConstType() {
      fIsConstType = false;
   }
   
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
      fIsConstType    = true;
      
      fClassBaseName = baseName.substring(0, 1).toUpperCase()+baseName.substring(1).toLowerCase();
      fProxy = new ObservableModel();
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
    * Get base name of C peripheral class e.g. Ftm
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
    * @throws Exception 
    */
   public void setClockControlInfo(String clockEnable, String clockDisable) throws Exception {
      if ((clockEnable.length()==0) || (clockDisable.length()==0)) {
         throw new RuntimeException("Illegal clock control info = " + clockEnable + ", " + clockDisable);
      }
      if (fClockEnable != null) {
         fClockEnable  += "\\n" + clockEnable;
         Variable var = getVariable(makeKey("clockEnable"));
         var.setValue(fClockEnable.replaceAll("\\n", "\n"));
         fClockDisable  += "\\n" + fClockDisable;
         var = getVariable(makeKey("clockDisable"));
         var.setValue(fClockDisable.replaceAll("\\n", "\n"));
      }
      else {
         fClockEnable  = clockEnable;
         Variable var = new StringVariable("clockEnable", makeKey("clockEnable"), fClockEnable.replaceAll("\\\\n", "\n      "));
         addVariable(var);
         fClockDisable = clockDisable;
         addVariable(new StringVariable("clockDisable", makeKey("clockDisable"), fClockDisable.replaceAll("\\\\n", "\n      ")));
      }
   }
   
   /**
    * Get clock enable C statement e.g. SIM->SCGC5 |= SIM_SCGC5_UART_MASK
    * 
    * @return
    */
   public String getClockEnable() {
      return fClockEnable;
   }

   /**
    * Get clock disable C statement e.g. SIM->SCGC5 &= ~SIM_SCGC5_UART_MASK
    * 
    * @return
    */
   public String getClockDisable() {
      return fClockDisable;
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
   void writeXmlInformation(XmlDocumentUtilities documentUtilities) throws IOException, UsbdmException {
      documentUtilities.openTag("peripheral");
      documentUtilities.writeAttribute("baseName", fBaseName);
      documentUtilities.writeAttribute("instance", fInstance);
      documentUtilities.writeAttribute("version",  fVersion);

      documentUtilities.openTag("handler");
      documentUtilities.writeAttribute("class", this.getClass().getName());
      documentUtilities.closeTag();
      
      // Additional, peripheral specific, information
      if (fClockEnable != null) {
         documentUtilities.openTag("clock");
         documentUtilities.writeAttribute("clockEnable",  XmlDocumentUtilities.escapeXml(fClockEnable));
         documentUtilities.writeAttribute("clockDisable", XmlDocumentUtilities.escapeXml(fClockDisable));
         documentUtilities.closeTag();
      }
      if (fIrqHandler != null) {
         documentUtilities.writeAttribute("irqHandler",  XmlDocumentUtilities.escapeXml(fIrqHandler));
      }
      for (String irqNum:getIrqNums()) {
         documentUtilities.openTag("irq");
         documentUtilities.writeAttribute("num", XmlDocumentUtilities.escapeXml(irqNum));
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
      String value = settings.get(CODE_IDENTIFIER_KEY);
      if (value != null) {
         setCodeIdentifier(value);
      }
      value = settings.get(USER_DESCRIPTION_KEY);
      if (value != null) {
         setUserDescription(value);
      }
   }

   /**
    * Save settings to settings object
    * 
    * @param settings Settings object
    */
   public void saveSettings(Settings settings) {
      String value = getCodeIdentifier();
      if ((value != null) && !value.isBlank()) {
         settings.put(CODE_IDENTIFIER_KEY, value);
      }
      value = getUserDescription();
      if ((value != null) && !value.isBlank() && (!value.equals(getDescription()))) {
         settings.put(USER_DESCRIPTION_KEY, value);
      }
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
    * Get description of peripheral e.g. <i><b>CMP Comparator</b></i>
    * 
    * @return
    */
   public String getDescription() {
      
      String description = getTitle();
      if ((description != null) && !description.isBlank()) {
         return getBaseName()+", " + description;
      }
      return getBaseName();
   }

   /**
    * Get instance name for a simple signal e.g. <i><b>gpioA_0</b></i>
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
    * Convert string to valid C identifier<br>
    * If the string starts with a non-character it is prefixed with X_<br>
    * Other invalid characters are converted to '_'.<br>
    * 
    * @param identifier String to convert
    * 
    * @return Valid C identifier
    */
   String makeCIdentifier(String identifier) {
      if (identifier.isBlank()) {
         return "";
      }
      identifier = identifier.replaceAll("[^a-zA-Z0-9]", "_");
      if (!identifier.matches("^[a-zA-Z].*")) {
         identifier = "X_" + identifier;
      }
      return identifier;
   }
   
   /**
    * Convert string to valid C identifier<br>
    * If the string starts with a non-character it is prefixed with X_<br>
    * Other invalid characters are converted to '_'.<br>
    * The 1st character is made uppercase.<br>
    * 
    * @param identifier String to convert
    * 
    * @return Valid C identifier
    */
   String makeCTypeIdentifier(String identifier) {
      if (identifier.isBlank()) {
         return "";
      }
      identifier = makeCIdentifier(identifier);
      identifier = Character.toUpperCase(identifier.charAt(0))+identifier.substring(1);
      return identifier;
   }
   
   /**
    * Convert string to valid C identifier<br>
    * If the string starts with a non-character it is prefixed with X_<br>
    * Other invalid characters are converted to '_'.<br>
    * The 1st character is made lowercase.<br>
    * 
    * @param identifier String to convert
    * 
    * @return Valid C identifier
    */
   String makeCVariableIdentifier(String identifier) {
      if (identifier.isBlank()) {
         return "";
      }
      identifier = makeCIdentifier(identifier);
      identifier = Character.toLowerCase(identifier.charAt(0))+identifier.substring(1);
      return identifier;
   }
   
   /**
    *  Write variable declaration
    *  
    *  <pre> {} = optional
    *  {/// <i><b>description</b></i>}
    *  {// <i><b>error</b></i>}
    *  {<i><b>extern</b></i>}<i><b> type identifier</b></i>; {// <i><b>trailingComment</b></i>}
    *  </pre>
    * @param error            Error message to precede declaration (empty to suppress) 
    * @param description      Comment describing declaration
    * @param cIdentifier      C identifier to use
    * @param cType            C Type to use
    * @param trailingComment  Trailing comment
    */
   protected void writeVariableDeclaration(String error, String description, String cIdentifier, String cType, String trailingComment) {
      cIdentifier = makeCVariableIdentifier(cIdentifier);
      boolean isRepeated = !fUsedNames.add(cIdentifier);
      
      fDeclarations.append("\n");
      if (!description.isBlank()) {
         fDeclarations.append("/// " + description + "\n");
         if (error.isBlank()) {
            fHardwareDefinitions.append("/// " + description + "\n");
         }
      }
      
      if (!error.isBlank()) {
         fDeclarations.append("#error \"" + error + "\"\n");
      }
      
      if (!trailingComment.isBlank()) {
         trailingComment = "// " + trailingComment;
      }
      fDeclarations.append(String.format("%s%-50s %-30s  %s\n", isRepeated?"// ":"", "extern " + cType, cIdentifier+";", trailingComment));
      if (error.isBlank()) {
         fHardwareDefinitions.append(String.format("%s%-50s %-30s  %s\n", isRepeated?"// ":"", cType, cIdentifier+";", trailingComment));
      }
   }
   
   /**
    *  Write type declaration
    *  
    *  <pre> {} = optional
    *  {/// <i><b>description</b></i>} 
    *  {// <i><b>error</b></i>}
    *  using <i><b>identifier</b></i> = <i><b>type</b></i>; {// <i><b>trailingComment</b></i>}
    *  </pre>
    * @param error            Error message to precede declaration (empty to suppress) 
    * @param description      Comment describing declaration
    * @param cIdentifier      C identifier to use
    * @param cType            C Type to use
    * @param trailingComment  Trailing comment
    */
   protected void writeTypeDeclaration(String error, String description, String cIdentifier, String cType, String trailingComment) {
      cIdentifier = makeCTypeIdentifier(cIdentifier);
      boolean isRepeated = !fUsedNames.add(cIdentifier);
      
      fDeclarations.append("\n");
      if (!description.isBlank()) {
         fDeclarations.append("/// " + description + "\n");
      }
      
      if (!error.isBlank()) {
         fDeclarations.append("#error \"" + error + "\"\n");
      }
      
      if (!trailingComment.isBlank()) {
         trailingComment = "// " + trailingComment;
      }
      fDeclarations.append(String.format("%susing %-30s = %-50s %s\n", isRepeated?"// ":"", cIdentifier, cType+";", trailingComment));
   }
   
   /**
    * Write declarations for this peripheral e.g.
    * <pre>
    * // UserDescription
    * extern const <i><b>className</b></i> codeIdentifier;
    * 
    * // UserDescription
    * using CodeIdentifier = const <i><b>className</b></i>;
    * </pre>
    * @param sb               Where to write
    * @param className
    */
   protected void writeDefaultPeripheralDeclaration(String className) {
      // Default action is to create a declaration for the device itself 
      if (getCodeIdentifier().isBlank()) {
         return;
      }
      String cIdentifier = getCodeIdentifier();
      String cType       = (fIsConstType?"const ":"") + className;
      
      String description = getUserDescription();
      if (description.isBlank()) {
         description = getDescription();
      }
      writeTypeDeclaration("", description, cIdentifier, cType, "");
      writeVariableDeclaration("", description, cIdentifier, cType, "");
   }

   /**
    * Write declarations for variables and types associated with this peripheral e.g.
    * <pre>
    * // An example peripheral
    * using MyAdc = const <i><b>Adc1</b></i>;
    * // An example peripheral
    * extern const <i><b>Adc1</b></i> myAdc;
    * 
    * extern const <i><b>Adc1</b></i>::Channel&lt;<i><b>3</b></i>&gt;    myAdcChannel1; // p9   
    * extern const <i><b>Adc1</b></i>::Channel&lt;<i><b>6</b></i>&gt;    myAdcChannel2; // p11 
    * </pre>
    * Default action is to create a declaration for the device itself.<br>
    * Overridden in some peripherals to add declarations for signals
    * 
    * @param writer        Where to write declarations
    */
   protected void writeDeclarations() {
      // Default action is to create a declaration for the device itself 
      writeDefaultPeripheralDeclaration(getClassName());
   }

   // Used by by peripheral to record shared information when generating code
   // Prevent re-use of C identifiers
   Set<String>   fUsedNames            = null;
   
   // Collects definitions of user objects for hardware file
   StringBuilder fHardwareDefinitions  = null;
   
   // Collects declarations of user objects for the include file for this peripheral
   StringBuilder fDeclarations          = null;
   
   /**
    * Creat declarations for variables and types associated with this peripheral e.g.
    * <pre>
    * // An example peripheral
    * using MyAdc = const <i><b>Adc1</b></i>;
    * // An example peripheral
    * extern const <i><b>Adc1</b></i> myAdc;
    * 
    * extern const <i><b>Adc1</b></i>::Channel&lt;<i><b>3</b></i>&gt;    myAdcChannel1; // p9   
    * extern const <i><b>Adc1</b></i>::Channel&lt;<i><b>6</b></i>&gt;    myAdcChannel2; // p11 
    * </pre>
    * 
    * @param usedIdentifiers        Set used to prevent repeated C identifiers
    * @param hardwareIncludeFiles   Collects include files need for user declared objects
    * @param hardwareDefinitions    Collects definitions of user objects
    * @return 
    */
   final synchronized void createDeclarations(Set<String> usedIdentifiers, HashSet<String> hardwareIncludeFiles, StringBuilder hardwareDefinitions) {
      
      // Used by by peripheral to record shared information for hardware file
      fUsedNames            = usedIdentifiers;
      fHardwareDefinitions  = hardwareDefinitions;

      // Collects declarations of user objects for the include file for this peripheral
      fDeclarations         = new StringBuilder();
            
      writeDeclarations();
      
      if (fDeclarations.toString().isBlank()) {
         // No declarations for this peripheral
         fDeviceInfo.removeVariableIfExists(PERIPHERAL_DECLARATIONS_KEY);
      }
      else {
         // Need include file in hardware.cpp since created instance of type
         hardwareIncludeFiles.add("#include \"" + getBaseName().toLowerCase()+".h\"");

         // Create variable containing declarations for this peripheral
         StringVariable declarationsVar = new StringVariable("Declarations", PERIPHERAL_DECLARATIONS_KEY, fDeclarations.toString());
         declarationsVar.setDerived(true);
         fDeviceInfo.addOrReplaceVariable(PERIPHERAL_DECLARATIONS_KEY, declarationsVar);
      }
   }
   
   /**
    * Indicates if a PCR table is required in the Peripheral Information class<br>
    * Default implementation checks the size of the signal table.
    * 
    * @return
    * @throws Exception 
    */
   public boolean isPcrTableNeeded() {
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
    * Write USBDM namespace level information associated with a peripheral to <i><b>pin_mapping.h</b></i><br>
    * This appears before the peripheral class at USBDM namespace level.
    * 
    * @param documentUtilities
    * @throws IOException
    * @throws Exception 
    */
   void writeNamespaceInfo(DocumentUtilities documentUtilities) throws IOException {
   }

   /**
    * Writes definitions to be included in the information class describing the peripheral to <i><b>pin_mapping.h</b></i><br>
    * 
    * <i><b>Example:</b></i>
    * <pre>
    *
    *    //! Number of IRQs for hardware
    *    static constexpr uint32_t irqCount  = 1;
    * 
    *    //! IRQ numbers for hardware
    *    static constexpr IRQn_Type irqNums[]  = {
    *       USB0_IRQn, };
    * </pre>
    * 
    * @param pinMappingHeaderFile   Where to write definitions
    * 
    * @throws IOException 
    * @throws Exception 
    */
   abstract void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException;

   /**
    * Write extra information within the class
    *  
    * @param documentUtilities
    * @throws IOException
    */
   void writeExtraInfo(DocumentUtilities pinMappingHeaderFile) throws IOException {
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
   
   private static final String INVALID_TEMPLATE  = "         /* %3d: %-20s = %-30s */  { NoPortInfo, INVALID_PCR,  0                                },\n";
   private static final String DUMMY_TEMPLATE    = "         /* %3d: %-20s = %-30s */  { NoPortInfo, UNMAPPED_PCR, 0                                },\n";
   private static final String FIXED_TEMPLATE    = "         /* %3d: %-20s = %-30s */  { NoPortInfo, FIXED_NO_PCR, 0                                },\n";
   private static final String USED_TEMPLATE     = "         /* %3d: %-20s = %-30s */  { %-25s PORT_PCR_MUX(%d)|"+ DEFAULT_PCR_VALUE_NAME +"  },\n";
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
            indent+HEADING_TEMPLATE, "Signal", "Pin","    portInfo    gpioBit       PCR value"));
      // Signal information table
      int index = -1;
      for (Signal signal:signalTable.table) {
         index++;
         if (signal == null) {
            pinMappingHeaderFile.write(String.format(indent+INVALID_TEMPLATE, index, "--", "--"));
            continue;
         }
         MappingInfo mappingInfo = signal.getFirstMappedPinInformation();
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

      if (!isPcrTableNeeded()) {
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
    *  class Lpuart0Info {
    *  public:
    *     //! Hardware base address as uint32_t 
    *     static constexpr uint32_t baseAddress = LPUART0_BasePtr;
    *
    *     //! Hardware base pointer
    *     static volatile LPUART_Type &lpuart() {
    *        return *(LPUART_Type *)baseAddress;
    *     }
    *   ...
    *   };
    * </pre>
    * 
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

   /**
    * Indicate if the peripheral has some pins that <i><b>may be</b></i> mapped to a package location<br>
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
    * and replaces with class name e.g. <i><b>FTM0_IRQHandler</b></i> => <i><b>Ftm0::irqHandler</b></i><br>
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
    * 
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

   // Indicates the peripheral has associated signals
   boolean hasSignal = false;
   
   /**
    * Array of peripherals to obtain signals from
    */
   ArrayList<Peripheral> fSignalPeripherals;
   
   /**
    * Create models representing the signals directly associated with this peripheral
    * 
    * @param parent     Parent model to contain pins created
    */
   private void createMySignalModels(BaseModel parent) {
      
      TreeMap<String, Signal> signals = getSignals();
      if (signals == null) {
         // No signals
         return;
      }
      // Add signals from this peripheral
      for (String signalName:signals.keySet()) {
         Signal signal = fSignals.get(signalName);
         if (signal.isAvailableInPackage()) {
            new SignalModel(parent, signal);
         }
      }
   }

   /**
    * Create models representing the signals for this peripheral
    * 
    * @param parent Parent model to contain pins created
    * 
    * @note May add related pins e.g. RTC may contains OSC pins 
    */
   public void createSignalModels(BaseModel parent) {

      // Add signals from this peripheral
      createMySignalModels(parent);
      
      if (fSignalPeripherals != null) {
         
         // Add signals from referenced peripherals
         for (Peripheral peripheral:fSignalPeripherals) {
            peripheral.createMySignalModels(parent);
         }
      }
   }

   /**
    * Create models representing the signals for this peripheral.<br>
    * <i><b>May add related pins e.g. RTC may contains OSC pins</b></i> 
    * 
    * @param parent Model to attach PeripheralSignalsModel to
    * 
    * @return PeripheralSignalsModel containing signals or null if no signals are associated with this peripheral
    */
   public BaseModel createPeripheralSignalsModel(BaseModel parent) {
      if (!hasSignal) {
         return null;
      }
      return new PeripheralSignalsModel(parent, this);
   }

   /**
    * Add peripheral as source for signals for this peripheral.
    * Actual signal models are created later.
    * 
    * @param parentModel   Model to contain signal category
    * @param peripheral    Peripheral to obtain signals from (may be this peripheral)
    */
   public void addSignalsFromPeripheral(BaseModel parentModel, Peripheral peripheral) {
      hasSignal = true;
      if (peripheral == this) {
         // Don't add me!
         return;
      }
      if (fSignalPeripherals == null) {
         fSignalPeripherals = new ArrayList<Peripheral>();
      }
      fSignalPeripherals.add(peripheral);
   }

   /**
    * Validate the signal to pin mapping
    * 
    * @return Status Status if error or null if none
    */
   public void validateMappedPins() {
      return;
   }

   /**
    * Indicates if the peripheral represents real hardware
    * 
    * @return true if not real hardware
    */
   public final boolean isSynthetic() {
      return fIsSynthetic;
   }

   /**
    * Get user identifier used for this peripheral in C code
    * 
    * @return User name
    */
   public String getCodeIdentifier() {
      return fCodeIdentifier;
   }

   /**
    * Set editor dirty via deviceInfo
    */
   public void setDirty(boolean dirty) {
      if (fDeviceInfo != null) {
         fDeviceInfo.setDirty(dirty);
      }
   }
   
   /**
    * Set user identifier used for this peripheral in C code
    * 
    * @param codeIdentifier User name
    */
   public void setCodeIdentifier(String codeIdentifier) {
      if ((fCodeIdentifier != null) && (fCodeIdentifier.compareTo(codeIdentifier) == 0)) {
         return;
      }
      fCodeIdentifier = codeIdentifier;
      setDirty(true);
      notifyListeners();
   }

   /**
    * Get status of peripheral
    * 
    * @return
    */
   public Status getStatus() {
      return null;
   }

   @Override
   public void addListener(IModelChangeListener listener) {
      fProxy.addListener(listener);      
   }

   @Override
   public void removeAllListeners() {
      fProxy.removeAllListeners();      
   }

   @Override
   public void removeListener(IModelChangeListener listener) {
      fProxy.removeListener(listener);
   }

   @Override
   public void notifyListeners() {
      fProxy.notifyListeners();
   }

   @Override
   public void notifyStatusListeners() {
      fProxy.notifyStatusListeners();
   }

   @Override
   public void notifyStructureChangeListeners() {
      fProxy.notifyStructureChangeListeners();
   }

   @Override
   public boolean isRefreshPending() {
      return fProxy.isRefreshPending();
   }

   @Override
   public void setRefreshPending(boolean refreshPending) {
      fProxy.setRefreshPending(refreshPending);
   }

   /** 
    * Set description of pin use 
    */
   public void setUserDescription(String userDescription) {
      if ((getUserDescription().compareTo(userDescription) == 0)) {
         return;
      }
      fUserDescription = userDescription;
      setDirty(true);
      notifyListeners();
   }

   /** 
    * Get user description of peripheral
    */
   public String getUserDescription() {
      if (fUserDescription == null) {
         return "";
      }
      return fUserDescription;
   }

}