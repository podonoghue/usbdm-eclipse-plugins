package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.editor.ModifierEditorInterface;
import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.DeviceFamily;
import net.sourceforge.usbdm.deviceEditor.information.DmaInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.PcrInitialiser;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Settings;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.SignalTemplate;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModelInterface;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.parsers.XmlDocumentUtilities;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp.HardwareDeclarationInfo;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp.WriterInformation;
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

   /** Base name of C peripheral class e.g. Ftm */
   private final String fClassBaseName;
   
   /** Base name of the peripheral e.g. FTM0 = FTM, PTA = PT */
   private final String fBaseName;
   
   /** Instance name/number of the peripheral instance e.g. FTM0 = 0, PTA = A */
   private final String fInstance;
   
   /** Hardware interrupt numbers */
   private final ArrayList<String> fIrqNums = new ArrayList<String>();

   /** Key used to save/restore identifier used for code generation */
   private final String CODE_IDENTIFIER_KEY  = "$peripheral$"+getName()+"_codeIdentifier";
   
   /** Key used to save/restore identifier used for code generation */
   private final String CREATE_INSTANCE_KEY  = "$peripheral$"+getName()+"_createInstance";
   
   /** Key used to save/restore user description */
   private final String USER_DESCRIPTION_KEY = "$peripheral$"+getName()+"_userDescription";

   /** User description of peripheral use */
   private String fUserDescription = "";

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
   
   /** String showing instances of this peripheral class e.g. "0,1" or "A,B,C" */
   private String fInstanceList;
   
   /**
    * Indicates the class representing this peripheral is const - default true
    * - May be placed in ROM
    * - Has only static methods (or are overloaded by same)
    * - This means that an instance need not be created
    */
   protected boolean fIsConstType = true;
   
   /** Indicates the peripheral as synthetic i.e. no hardware is associated - default false */
   protected boolean fIsSynthetic = false;

   /** Indicates that code for a user instance of the peripheral class should be created */
   protected boolean fCreateInstance = false;
   
   /** Can create type declarations for this peripheral - most can */
   protected boolean fCanCreateType           = true;
   
   /** Can create instances of this peripheral - most can't */
   protected boolean fCanCreateInstance       = false;
   
   /** Can create type declarations for signals belonging to this peripheral - Most can't */
   protected boolean fcanCreateSignalType     = false;
   
   /** Can create instances for signals belonging to this peripheral - Most can't */
   protected boolean fCanCreateSignalInstance = false;

//   /** Name of C struct associated with this peripheral */
//   private String fheaderStructName = null;

   /**
    * Sets the peripheral as a non-const variable i.e. cannot be placed in ROM
    */
   protected void clearConstType() {
      fIsConstType = false;
   }
   
   /**
    * Indicates the class representing this peripheral is const
    *  - May be placed in ROM - Has only static methods (or are overloaded by same)
    *  - This means that an instance need not be created
    * 
    * @return true is const
    */
   public boolean isConstType() {
      return fIsConstType;
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
    * Get base name of the peripheral e.g. FTM0 => FTM, PTA => PT
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
    * Get base name of C peripheral class e.g. FTM0 => Ftm
    * 
    * @return
    */
   public String getClassBaseName() {
      return fClassBaseName;
   }

   /**
    * Get name of C peripheral class e.g. FTM2 => Ftm2
    * 
    * @return
    */
   public String getClassName() {
      return getClassBaseName()+fInstance;
   }

   /**
    * Set version name for this peripheral e.g. <b>flexio_4sh_4tmr_0x4005f000</b> <br>
    * This is used to access the XML file for the device editor.
    * 
    * @param version Version name to set
    */
   public void setPeripheralVersion(String version) {
      if (version.isBlank()) {
         System.err.println("setPeripheralVersion");
      }
      fVersion = version;
    }

   /**
    * Return version name of peripheral e.g. <b>flexio_4sh_4tmr_0x4005f000</b> <br>
    * 
    * @return null if not set
    */
   public String getOverriddenPeripheralVersionName() {
      return fVersion;
   }

   /**
    * Return version name of peripheral e.g. <b>flexio_4sh_4tmr_0x4005f000</b> <br>
    * Defaults to name based on peripheral e.g. Ftm
    */
   public String getPeripheralVersionName() {
      return (fVersion!=null)?fVersion:getClassBaseName().toLowerCase();
//      return fVersion;
   }

   /**
    * Set clock information
    * 
    * @param clockEnable      Clock enable C code e.g. <b><i>SIM->SCGC6 = SIM->SCGC6 | SIM_SCGC6_FTM0_MASK;</b></i>
    * @param clockDisable     Clock disable C code e.g. <b><i>SIM->SCGC6 = SIM->SCGC6 & ~SIM_SCGC6_FTM0_MASK;</b></i>
    * 
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
         Variable var = new StringVariable(this, "clockEnable", makeKey("clockEnable"), fClockEnable.replaceAll("\\\\n", "\n      "));
         addVariable(var);
         fClockDisable = clockDisable;
         addVariable(new StringVariable(this, "clockDisable", makeKey("clockDisable"), fClockDisable.replaceAll("\\\\n", "\n      ")));
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
    * Get signal belonging to peripheral by name
    * 
    * @param signalName Name of signal to locate
    * 
    * @return Signal or null if signal doesn't belong to peripheral (or doesn't exist at all)
    */
   public Signal getSignal(String signalName) {
      return fSignals.get(signalName);
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
      documentUtilities.writeAttribute("baseName",       fBaseName);
      documentUtilities.writeAttribute("instance",       fInstance);
      if (!isSynthetic()) {
         documentUtilities.writeAttribute("instanceList",   fInstanceList);
      }
      else {
         documentUtilities.writeAttribute("instanceList",   "");
      }
      documentUtilities.writeAttribute("version",        fVersion);
//      documentUtilities.writeAttribute("_structName",    fheaderStructName);

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
      documentUtilities.writeParam("/"+getName()+"/_instanceList", "StringVariable", fInstanceList);

      if (getIrqCount()>0) {
         documentUtilities.writeParam("/"+getName()+"/_hardwareIrqCount", "LongVariable", Integer.toString(getIrqCount()));
      }
      if (getIrqNums() != null) {
         StringBuilder sb = new StringBuilder();
         boolean needSeparator = false;
         for (String irqNum:getIrqNums()) {
            if (needSeparator) {
               sb.append(";");
            }
            sb.append(irqNum);
            needSeparator = true;
         }
         documentUtilities.writeParam("/"+getName()+"/_hardwareIrqNums", "StringVariable", sb.toString());
      }
      if (this instanceof PeripheralWithState) {
         PeripheralWithState pws = (PeripheralWithState) this;
         if (pws.isPcrTableNeeded()) {
            documentUtilities.writeParam("/"+getName()+"/_hasPcrTable", "BooleanVariable", Boolean.TRUE.toString());
         }
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
      if (settings.get(CREATE_INSTANCE_KEY) != null) {
         setCreateInstance(true);
      }
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
      if (getCreateInstance()) {
         settings.put(CREATE_INSTANCE_KEY, "true");
      }
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
      
      /**
       * Get index of signal in table
       * 
       * @param signal To check
       * 
       * @return Index or -1 if not found
       */
      public int indexOf(Signal signal) {
         return table.indexOf(signal);
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
      if ((identifier == null) || identifier.isBlank()) {
         return "";
      }
      identifier = identifier.trim();
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
    * @return Valid C identifier or empty string
    */
   String makeCTypeIdentifier(String identifier) {
      if ((identifier == null) || identifier.isBlank()) {
         return "";
      }
      identifier = identifier.trim();
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
    * @return Valid C identifier or empty string
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
    * Indicates that an <b>#include</b> for this peripheral is needed for hardware.h
    * 
    * Used during code generation
    */
   void setHardwareIncludeFile() {
      fCreatedUserDeclarations = true;
   }
   
   /**
    *  Write variable declaration to hardwareDeclarationInfo.hardwareDeclarations
    * 
    *  <pre> {} = optional
    *  {/// <i><b>description</b></i>}
    *  {// <i><b>error</b></i>}
    *  {<i><b>extern</b></i>}<i><b> type identifier</b></i>( <i><b>args</b></i> ); {// <i><b>trailingComment</b></i>}
    *  </pre>
    * @param error            Error message to precede declaration (empty to suppress)
    * @param description      Comment describing declaration
    * @param cIdentifier      C identifier to use
    * @param cType            C Type to use
    * @param args             Arguments for declaration
    * @param trailingComment  Trailing comment
    */
   protected void writeVariableDeclaration(HardwareDeclarationInfo hardwareDeclarationInfo, String error, String description, String cIdentifier, String cType, String args, String trailingComment) {
      cIdentifier = makeCVariableIdentifier(cIdentifier);
      boolean isRepeated = !hardwareDeclarationInfo.usedIdentifiers.add(cIdentifier);
      hardwareDeclarationInfo.hardwareDeclarations.append("\n");
      if (!description.isBlank()) {
         hardwareDeclarationInfo.hardwareDeclarations.append("/// " + description + "\n");
         if (error.isBlank()) {
            hardwareDeclarationInfo.hardwareDeclarations.append("/// " + description + "\n");
         }
      }
      
      if (!error.isBlank()) {
         hardwareDeclarationInfo.hardwareDeclarations.append("#error \"" + error + "\"\n");
      }
      
      if (!trailingComment.isBlank()) {
         trailingComment = "// " + trailingComment;
      }
      if (!args.isBlank()) {
         args = "{"+args+"}";
      }
      setHardwareIncludeFile();
      hardwareDeclarationInfo.hardwareDeclarations.append(String.format("%-60s %-45s %s\n", (isRepeated?"// ":"")+"extern " + cType, cIdentifier+";", trailingComment));
      if (error.isBlank()) {
         hardwareDeclarationInfo.hardwareDefinitions.append(String.format("%-60s %-45s %s\n", (isRepeated?"// ":"")+cType, cIdentifier+args+";", trailingComment));
      }
   }
   
   /**
    *  Write variable declaration to hardwareDeclarationInfo.hardwareDeclarations
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
   protected void writeVariableDeclaration(HardwareDeclarationInfo hardwareDeclarationInfo, String error, String description, String cIdentifier, String cType, String trailingComment) {
      writeVariableDeclaration(hardwareDeclarationInfo, error, description, cIdentifier, cType, "",  trailingComment);
   }
   
   /**
    *  Write type declaration to hardwareDeclarationInfo.hardwareDeclarations
    * 
    *  <pre> {} = optional
    *  {/// <i><b>description</b></i>}
    *  {// <i><b>error</b></i>}
    *  typedef <i><b>type</b></i> <i><b>identifier</b></i>; {// <i><b>trailingComment</b></i>}
    *  </pre>
    * @param error            Error message to precede declaration (empty to suppress)
    * @param description      Comment describing declaration
    * @param cIdentifier      C identifier to use
    * @param cType            C Type to use
    * @param trailingComment  Trailing comment
    */
   protected void writeTypeDeclaration(HardwareDeclarationInfo hardwareDeclarationInfo, String error, String description, String cIdentifier, String cType, String trailingComment) {
      cIdentifier = makeCTypeIdentifier(cIdentifier);
      boolean isRepeated = !hardwareDeclarationInfo.usedIdentifiers.add(cIdentifier);
      
      hardwareDeclarationInfo.hardwareDeclarations.append("\n");
      if (!description.isBlank()) {
         hardwareDeclarationInfo.hardwareDeclarations.append("/// " + description + "\n");
      }
      
      if (!error.isBlank()) {
         hardwareDeclarationInfo.hardwareDeclarations.append("#error \"" + error + "\"\n");
      }
      
      if (!trailingComment.isBlank()) {
         trailingComment = "// " + trailingComment;
      }
      setHardwareIncludeFile();
      hardwareDeclarationInfo.hardwareDeclarations.append(String.format("%-60s %-45s %s\n", (isRepeated?"// ":"")+"typedef "+cType, cIdentifier+";", trailingComment));
   }
   
   /**
    * Write declarations to hardwareDeclarationInfo.hardwareDeclarations for this peripheral e.g.
    * <pre>
    * // UserDescription
    * extern const <i><b>className</b></i> codeIdentifier;
    * 
    * // UserDescription
    * using CodeIdentifier = <i><b>className</b></i>;
    * </pre>
    * 
    * @param className  Class name to use in creating declarations and definitions
    * @param hardwareDeclarationInfo
    * 
    * @return true if a declaration or definition was created
    */
   protected void writeDefaultPeripheralDeclaration(HardwareDeclarationInfo hardwareDeclarationInfo, String className) {
      // Default action is to create a declaration for the device itself
      if (getCodeIdentifier().isBlank()) {
         return;
      }
      String cIdentifier = getCodeIdentifier();
      String cConstType  = (fIsConstType?"const ":"") + className;
      String cType       = className;
      
      String description = getUserDescription();
      if (description.isBlank()) {
         description = getDescription();
      }
      String desc[] = description.split("/");
      int index = 0;
      for (String cId:cIdentifier.split("/")) {
         if (desc.length==1) {
            // Use single description provided
            description = desc[0];
         }
         else if (index>=desc.length) {
            // Run out of descriptions
            description = "";
         }
         else {
            // Description provided
            description = desc[index];
         }
         if (getCreateInstance()) {
            writeVariableDeclaration(hardwareDeclarationInfo, "", description, cId, cConstType, "");
         }
         else {
            writeTypeDeclaration(hardwareDeclarationInfo, "", description, cId, cType, "");
         }
         index++;
      }
   }

   protected void writeConstexprValue(HardwareDeclarationInfo hardwareDeclarationInfo, String description, String cIdentifier, String cType, String cValue, String trailingComment) {
      cIdentifier = makeCTypeIdentifier(cIdentifier);
      boolean isRepeated = !hardwareDeclarationInfo.usedIdentifiers.add(cIdentifier);
      
      hardwareDeclarationInfo.hardwareDeclarations.append("\n");
      if (!description.isBlank()) {
         hardwareDeclarationInfo.hardwareDeclarations.append("/// " + description + "\n");
      }
      
      if (!trailingComment.isBlank()) {
         trailingComment = "// " + trailingComment;
      }
      setHardwareIncludeFile();
      hardwareDeclarationInfo.hardwareDeclarations.append(String.format("%-60s %-45s %s\n", (isRepeated?"// ":"")+"constexpr "+cType, cIdentifier+" = "+cValue+";", trailingComment));
   }
   
   /**
    * Patterns available <br>
    *  <li> "%i" => pin.getPortInstance());     => port instance e.g."A"
    *  <li> "%n" => pin.getGpioBitNum());       => bit number within associated GPIO
    *  <li> "%p" => polarity);                  => polarity of GPIO
    *  <li> "%c" => getClassName());            => class name e.g.  FTM2 => Ftm2
    *  <li> "%b" => getClassBaseName());        => base class name e.g. FTM2 => Ftm
    *  <li> "%t" => infoTableIndex.toString()); => index in infotable
    * 
    * @param pattern          Path to do substitutions in
    * @param pin              pin
    * @param infoTableIndex   index in infotable
    * @param polarity         polarity of GPIO
    * 
    * @return Expanded pattern
    */
   public String expandTypePattern(String pattern, Pin pin, Integer infoTableIndex, String polarity) {
      String type = pattern;
      String[] patterns = {"%i","%n","%p","%c","%b","%t",};
      String[] replacements = {
            pin.getPortInstance(),     // port instance e.g."A"
            pin.getGpioBitNum(),       // bit number within associated GPIO
            polarity,                  // polarity polarity
            getClassName(),            // class name e.g.  FTM2 => Ftm2
            getClassBaseName(),        // base class name e.g. FTM2 => Ftm
            infoTableIndex.toString(), // index in infotable
      };
      for (int index=0; index<patterns.length; index++) {
         String from = patterns[index];
         String to   = replacements[index];
         if (to == null) {
            to = from+" not available";
         }
         type = type.replace(from,to);
      }
      return type;
   }
   
   /**
    * Write PCR style type declarations for all named and mapped signals in peripheral<br>
    * 
    * <pre>
    * /// Clkout
    * using Clkout  = PcrTable_T&lt;ControlInfo,10&gt;;  // PTC3 ()
    * </pre>
    */
   protected void writeSignalPcrDeclarations(HardwareDeclarationInfo hardwareDeclarationInfo) {
      String pattern = null;

      // Check for explicit pattern from peripheral XML
      StringVariable patternVar = (StringVariable) safeGetVariable(makeKey("$pcrPattern"));
      if (patternVar == null) {
         // Check for general pattern from peripheral XML
         patternVar = (StringVariable) safeGetVariable("/SYSTEM/$pcrPattern");
      }
      if (patternVar != null) {
         pattern = patternVar.getValueAsString();
         // Suppressed output
         if (pattern.isBlank()) {
            return;
         }
      }
      ArrayList<InfoTable> InfoTables = getSignalTables();
      for (InfoTable infoTable:InfoTables) {
         for (int infoTableIndex=0; infoTableIndex<infoTable.table.size(); infoTableIndex++) {

            Signal signal = infoTable.table.get(infoTableIndex);
            if (signal == null) {
               continue;
            }
            MappingInfo pinMapping = signal.getFirstMappedPinInformation();
            if (pinMapping == MappingInfo.UNASSIGNED_MAPPING) {
               continue;
            }
            Pin pin = pinMapping.getPin();
            String trailingComment  = pin.getNameWithLocation();
            String cIdentifier = makeCTypeIdentifier(signal.getCodeIdentifier());
            if (!cIdentifier.isBlank()) {
               String type = expandTypePattern(pattern, pin, infoTableIndex, "ActiveHigh");
               writeTypeDeclaration(hardwareDeclarationInfo, "", signal.getUserDescription(), cIdentifier, type, trailingComment);
            }
         }
      }
   }

   /**
    * Write declarations to hardwareDeclarationInfo.hardwareDeclarations for variables and types associated with this peripheral e.g.
    * <pre>
    * // An example peripheral
    * using MyAdc = <i><b>Adc1</b></i>;
    * // An example peripheral
    * extern const <i><b>Adc1</b></i> myAdc;
    * 
    * extern const <i><b>Adc1</b></i>::Channel&lt;<i><b>3</b></i>&gt;    myAdcChannel1; // p9
    * extern const <i><b>Adc1</b></i>::Channel&lt;<i><b>6</b></i>&gt;    myAdcChannel2; // p11
    * </pre>
    * Default action is to create a declaration for the device itself.<br>
    * Overridden in some peripherals to add declarations for signals
    * @param hardwareDeclarationInfo
    * @throws Exception
    */
   protected void writeDeclarations(HardwareDeclarationInfo hardwareDeclarationInfo) {
      // Default action is to create a declaration for the device itself
      writeDefaultPeripheralDeclaration(hardwareDeclarationInfo, getClassName());
   }

//   // Used by by peripheral to record shared information when generating user objects
//   // Prevent re-use of C identifiers
//   Set<String>   hardwareDeclarationInfo.usedIdentifiers            = null;
//
//   // Collects definitions of user objects for hardware.cpp
//   StringBuilder hardwareDeclarationInfo.hardwareDeclarations  = null;
//
//   // Collects declarations of user objects for hardware.cpp
//   StringBuilder hardwareDeclarationInfo.hardwareDeclarations = null;
//
//   // Indicates that user objects were created for this peripheral
   boolean fCreatedUserDeclarations = false;
   
   /**
    * Create declarations in hardware.h file for variables and types associated with this peripheral e.g.
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
    * @param hardwareIncludeFiles   Collects include files need for user declared objects (->hardware.h)
    * @param hardwareDeclarations   Collects declarations of user objects (->hardware.h)
    * @param hardwareDefinitions    Collects definitions of user objects (->hardware.cpp)
    * @throws Exception
    */
   final synchronized void createDeclarations(
         HardwareDeclarationInfo hardwareDeclarationInfo) {
      
      // Used by by peripheral to record shared information for hardware file
//      hardwareDeclarationInfo.usedIdentifiers                 = hardwareDeclarationInfo.usedIdentifiers;
//      hardwareDeclarationInfo.hardwareDeclarations      = hardwareDeclarationInfo.hardwareDeclarations;
//      hardwareDeclarationInfo.hardwareDeclarations       = hardwareDeclarationInfo.hardwareDefinitions;
      fCreatedUserDeclarations   = false;
      
      Variable signalVar = safeGetVariable(makeKey("_signals"));
      if ((signalVar != null) &&  !signalVar.isEnabled()) {
         return;
      }
      writeDeclarations(hardwareDeclarationInfo);
      
      if (fCreatedUserDeclarations) {
         // Need include file in hardware.cpp since peripheral is referenced in generated code
         hardwareDeclarationInfo.hardwareIncludeFiles.add("#include \"" + getBaseName().toLowerCase()+".h\"");
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
    *    //! IRQ numbers for hardware
    *    static constexpr IRQn_Type irqNums[]  = ADC0_IRQS;
    *
    *    //! Number of IRQs for hardware
    *    static constexpr uint32_t irqCount  = sizeofArray(irqNums);
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
   
   /**
    * Get signal by index form main signal info table i.e. signals for peripheral
    * 
    * @param index Index into table
    * 
    * @return Signal or null if not found
    */
   public Signal getSignalFromIndex(int index) {
      if (index>=fInfoTable.table.size()) {
         return null;
      }
      return fInfoTable.table.get(index);
   }
   
   /**
    * Get a list of signals that belong to this peripheral ordered by index.
    * Empty indices are indicated by a null entry.
    * 
    * @return
    */
   public Signal[] getMappedSignals() {
      ArrayList<Signal> signals = new ArrayList<Signal>();
      for (int index = 0; index<fInfoTable.table.size(); index++) {
         Signal signal = fInfoTable.table.elementAt(index);
         if ((signal == null) || (signal == Signal.DISABLED_SIGNAL)) {
            signals.add(null);
         }
         else {
            signals.add(signal);
         }
      }
      return signals.toArray(new Signal[signals.size()]);
   }
   
   /**
    * Get a list of pins for mapped signals that belong to this peripheral ordered by index.
    * Empty indices are indicated by a null entry.
    * 
    * @return
    */
   public Pin[] getMappedPins() {
      ArrayList<Pin> pins = new ArrayList<Pin>();
      for (int index = 0; index<fInfoTable.table.size(); index++) {
         Signal signal = fInfoTable.table.elementAt(index);
         if ((signal == null) || (signal == Signal.DISABLED_SIGNAL)) {
            pins.add(null);
            continue;
         }
         Pin pin = signal.getMappedPin();
         if (pin == Pin.UNASSIGNED_PIN) {
            pins.add(null);
            continue;
         }
         pins.add(pin);
      }
      return pins.toArray(new Pin[pins.size()]);
   }
   
   /**
    * Returns the single signal table <br>
    * 
    * @return The signal table
    */
   public InfoTable getUniqueSignalTable() {
      return fInfoTable;
   }
   
   private static final String INVALID_TEMPLATE  = "         /* %3d: %-20s = %-30s */  { PinIndex::INVALID_PCR,  PcrValue(0)         },\n";
   private static final String DUMMY_TEMPLATE    = "         /* %3d: %-20s = %-30s */  { PinIndex::UNMAPPED_PCR, PcrValue(0)         },\n";
   private static final String FIXED_TEMPLATE    = "         /* %3d: %-20s = %-30s */  { PinIndex::FIXED_NO_PCR, PcrValue(0)         },\n";
   private static final String USED_TEMPLATE     = "         /* %3d: %-20s = %-30s */  { PinIndex::%-13s PcrValue(%s) },\n";
   private static final String HEADING_TEMPLATE  = "         //      %-20s   %-30s   %s\n";

   protected void writeInfoTable(DocumentUtilities pinMappingHeaderFile, InfoTable signalTable) throws IOException {
      if (signalTable.table.size() == 0) {
         return;
      }
      if (getDeviceInfo().getDeviceFamily() == DeviceFamily.mke) {
         return;
      }
      String indent = "";
      if (signalTable.getName() != INFO_TABLE_NAME) {
         // Write MACRO indicated extra Pin Table information
//         pinMappingHeaderFile.writeMacroDefinition("USBDM_"+(getClassName()+"_"+signalTable.getName()).toUpperCase()+"_IS_DEFINED");

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
            indent+HEADING_TEMPLATE, "Signal", "Pin","    PinIndex                PCR value"));
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
                     String.format(indent+USED_TEMPLATE, index, signal.getName(),
                           mappingInfo.getPin().getNameWithLocation(), pinInfoInitString, PcrInitialiser.longTo5Hex(mappingInfo.getPcr())));
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
    * //! Information for each signal of peripheral
    * static constexpr PinInfo  info[] = {
    *   //      Signal         Pin                     PinIndex                PCR value
    *   /*   0: UART0_TX     = PTB17 (ConTx) &#42;/  { PinIndex::PTB17,        PcrValue(0x00000UL)  },
    *   /*   1: UART0_RX     = PTB16 (ConRx) &#42;/  { PinIndex::UNMAPPED_PCR, PcrValue(0)          },
    *   /*   2: UART0_RTS_b  = --            &#42;/  { PinIndex::FIXED_NO_PCR, PcrValue(0)          },
    *   ...
    * };
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
    * Check flag to generate this peripherals definitions in peripheral header file rather than in pin_mapping.h
    * 
    * @return  true => Generate in peripheral header file
    */
   public boolean generateDefinitionsInHeaderFile() {
      
      BooleanVariable bv = (BooleanVariable) safeGetVariable("definitionsInHeader");
      return (bv != null) && bv.getValueAsBoolean();
   }
   
   /**
    * Write Peripheral Information Class<br>
    * along with related classes and definitions
    * 
    * <pre>
    *  class Lpuart0Info {
    *  public:
    *   ...
    *   };
    * </pre>
    * 
    * @param  deviceInformation
    * @param  writerInformation Where to write
    * 
    * @throws IOException
    */
   public void writeInfoClass(WriterInformation writerInformation) throws IOException {
      
      writerInformation.writer.writeDocBanner(
            "Peripheral information for " + getGroupTitle() + ".\n\n" +
            "This may include pin information, constants, register addresses, and default register values,\n" +
            "along with simple accessor functions.");

      writeNamespaceInfo(writerInformation.writer);
      
      String className = getClassName()+"Info";
      
      String classDecl = className;
      Variable classDeclaration = safeGetVariable(makeKey("_class_declaration"));
      if (classDeclaration != null) {
         // Custom Open class
         classDecl = classDeclaration.getValueAsString();
      }
      // Open class
      writerInformation.writer.write(String.format(
            "class %s {\n\n" +
            "public:\n",
               classDecl
            ));
      // Write PCR Table
      writeInfoTables(writerInformation.writer);
      
      // Write extra tables
      writeExtraInfo(writerInformation.writer);
      
      // Additional, peripheral specific, information
      writeInfoConstants(writerInformation.writer);
      
      writeClassTemplate(writerInformation.writer);
      
      // Close class
      writerInformation.writer.write(String.format("}; // class %s\n\n", className));
   }

   /**
    * Writes the main template describing the class to pin_mapping.h<br>
    * 
    * <b>Example:</b>
    * <pre>
    * 
    *   //! Map all allocated pins on a peripheral when enabled
    *   static constexpr bool mapPinsOnEnable = true;
    *    ...
    * </pre>
    * 
    * @throws IOException
    * @throws Exception
    */
   public void writeClassTemplate(DocumentUtilities pinMappingHeaderFile) throws IOException {
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
    * Write initPCRs() functions
    * 
    * @param pinMappingHeaderFile
    * @param indent
    * @param signalTable
    * 
    * @throws IOException
    */
   public void writeInitPCR(DocumentUtilities pinMappingHeaderFile, String indent, InfoTable signalTable) throws IOException {
      indent += "   ";
      final String INIT_PCR_FUNCTION_TEMPLATE =
            indent+"/**\n"+
            indent+" * Initialise pins used by peripheral\n"+
            indent+" *\n"+
            indent+" * @note Only the lower 16-bits of the PCR registers are affected\n" +
            indent+" */\n"+
            indent+"static void initPCRs() {\n";

      final String CLEAR_PCR_FUNCTION_TEMPLATE =
            indent+"/**\n"+
            indent+" * Release pins used by peripheral\n"+
            indent+" *\n"+
            indent+" * @note Only the lower 16-bits of the PCR registers are affected\n" +
            indent+" */\n"+
            indent+"static void clearPCRs() {\n";

      PcrInitialiser pcrInitialiser = new PcrInitialiser();
      
      for (int index=0; index<signalTable.table.size(); index++) {
         Signal signal = signalTable.table.get(index);
         if (signal == null) {
            continue;
         }
         pcrInitialiser.addSignal(signal);
      }
      
      String initClocksBuffer = pcrInitialiser.getEnablePortClocksStatement(indent+"   ");

      pinMappingHeaderFile.write(INIT_PCR_FUNCTION_TEMPLATE);
      pinMappingHeaderFile.write(initClocksBuffer);
      String pcrInitStatements = pcrInitialiser.getPcrInitStatements(indent+"   ");
      pinMappingHeaderFile.write(pcrInitStatements);
      pinMappingHeaderFile.write(indent+"}\n\n");
      
      pinMappingHeaderFile.write(CLEAR_PCR_FUNCTION_TEMPLATE);
      pinMappingHeaderFile.write(initClocksBuffer);
      pinMappingHeaderFile.write(pcrInitialiser.getPcrClearStatements(indent+"   "));
      pinMappingHeaderFile.write(indent+"}\n\n");
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

   public String getPcrValue(Signal y) {
      return "USBDM::DEFAULT_PCR";
   }

   // Indicates the peripheral has associated signals
   boolean fHasSignal = false;

   /**
    * Class to hold peripheral to get signals from and a filter to select them
    */
   class PeripheralSignals {
      private final Peripheral fPeripheral;
      private final String     fSignalFilter;
      
      public PeripheralSignals(Peripheral peripheral, String signalFilter) {
         fPeripheral   = peripheral;
         fSignalFilter = signalFilter;
      }

      /**
       * Get peripheral
       * 
       * @return
       */
      public Peripheral getPeripheral() {
         return fPeripheral;
      }

      /**
       * get Filter selecting signals (may be null)
       * 
       * @return
       */
      public String getSignalFilter() {
         return fSignalFilter;
      }
      
   };
   
   /**
    * Array of (peripheral+filter) to obtain signals associated with this peripheral
    * from other peripherals.
    */
   ArrayList<PeripheralSignals> fSignalPeripherals;

   /**
    * Create models representing the signals directly associated with this
    * peripheral filtered by a regex filter.
    * 
    * @param parent   Created signal models are added to this model.
    */
   private void createMySignalModels(BaseModel parent, String filter) {
      Pattern pattern = null;
      if (filter != null) {
         pattern = Pattern.compile(filter);
      }
      TreeMap<String, Signal> signals = getSignals();
      if (signals == null) {
         // No signals
         return;
      }
      // Add signals from this peripheral filtered by filter
      for (String signalName:signals.keySet()) {
         Signal signal = fSignals.get(signalName);
         if (!signal.isAvailableInPackage()) {
            continue;
         }
         if ((pattern != null) && !pattern.matcher(signal.getName()).matches()) {
            continue;
         }
         new SignalModel(parent, signal);
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

      // Add all signals from this peripheral
      createMySignalModels(parent, null);

      if (fSignalPeripherals != null) {
         
         // Add signals from referenced peripherals with filer
         for (PeripheralSignals signalPeripheral:fSignalPeripherals) {
            signalPeripheral.getPeripheral().createMySignalModels(parent, signalPeripheral.getSignalFilter());
         }
      }
   }

   /**
    * Validate the signal to pin mapping
    * 
    * Updates fStatus
    */
   public void validateMappedPins() {
      return;
   }

   /**
    * Sets the peripheral as synthetic i.e. no hardware is associated
    */
   protected void setSynthetic() {
      fIsSynthetic = true;
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
    * Set user identifier used for this peripheral in C code
    * 
    * @param codeIdentifier User name
    */
   public void setCodeIdentifier(String codeIdentifier) {
      if ((fCodeIdentifier != null) && (fCodeIdentifier.compareTo(codeIdentifier) == 0)) {
         return;
      }
      fCodeIdentifier = codeIdentifier;
      setDirty();
      fProxy.notifyListeners();
   }

   /**
    * Get status of peripheral
    * 
    * @return
    */
   public Status getStatus() {
      return null;
   }

   /**
    * Set description of pin use
    */
   public void setUserDescription(String userDescription) {
      if ((getUserDescription().compareTo(userDescription) == 0)) {
         return;
      }
      fUserDescription = userDescription;
      setDirty();
      fProxy.notifyListeners();
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

   /**
    * Sets whether code for a user instance of the peripheral class should be created
    * 
    * @param value true to create instance
    */
   public void setCreateInstance(boolean value) {
      if (fCreateInstance != value) {
         fCreateInstance = value;
         setDirty();
         fProxy.notifyListeners();
      }
   }

   /**
    * Indicates whether code for a instance or type declaration should be created in user code
    * 
    * @return true => Instance, false => Type
    */
   public boolean getCreateInstance() {
      return fCreateInstance;
   }
   
  /**
    * Indicates whether code for a user declaration of the peripheral type can be generated.
    * This also includes enums or constant values e.g. CMP input selectors
    * 
    * @return true to indicate type declaration can be created
    */
   public final boolean canCreateType() {
      return !isSynthetic() && fCanCreateType;
   }

   /**
    * Indicates whether code for a user declaration of the signal type can be generated
    * This also includes enums or constant values e.g. CMP input selectors
    * 
    * @return true to indicate type declaration can be created
    */
   public final boolean canCreateType(Signal signal) {
      return fcanCreateSignalType;
   }

   /**
    * Indicates whether code for a user instance of the peripheral can be created
    * 
    * @return true to indicate an instance can be created
    */
   public final boolean canCreateInstance() {
      // Most devices can't create a useful instance
      Variable var =  safeGetVariable("_can_create_instance");
      if (var != null) {
         return var.getValueAsBoolean();
      }
      return fCanCreateInstance;
   }

   /**
    * Indicates whether code for a user instance of the signal related class can be created
    * 
    * @return true to indicate an instance can be created
    */
   public final boolean canCreateInstance(Signal signal) {
      // Most signals can't create a useful instance
      return fCanCreateSignalInstance;
   }

   public void addLinkedSignals() {
   }

   /**
    * Get mask indicating forced bits in PCR value for the given signal
    * 
    * @param signal Signal to check
    * 
    * @return mask
    */
   public long getPcrForcedBitsMask(Signal signal) {
      return 0;
   }

   /**
    * Get mask indicating the value of forced bits in PCR value for the given signal
    * 
    * @param signal Signal to check
    * 
    * @return mask
    */
   public long getPcrForcedBitsValueMask(Signal signal) {
      return 0;
   }

   public String getModifierHint(Signal signal) {
      return null;
   }

   public ModifierEditorInterface getModifierEditor() {
      return null;
   }

   /**
    * Set string showing instances of this peripheral class e.g. "0,1" or "A,B,C"
    * 
    * @param instanceList
    */
   public void setInstanceList(String instanceList) {
      fInstanceList = instanceList;
   }
   
   /**
    * Get string showing instances of this peripheral class e.g. "0,1" or "A,B,C"
    */
   public String getInstanceCount() {
      return fInstanceList;
   }
   
   // ================= IModelChangeListener ==========================

   /** Proxy used to support ObservableModel interface */
   private final ObservableModel fProxy;
   
   @Override
   public boolean addListener(IModelChangeListener listener) {
      return fProxy.addListener(listener);
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
   public void notifyBaseModelListeners() {
      fProxy.notifyBaseModelListeners();
   }

   @Override
   public void notifyStructureChangeListeners() {
      fProxy.notifyStructureChangeListeners();
   }

   public void notifyListeners() {
      fProxy.notifyListeners();
   }
   
   @Override
   public void notifyListeners(int properties) {
      fProxy.notifyListeners(properties);
   }


}