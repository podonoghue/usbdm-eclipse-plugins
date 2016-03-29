package net.sourceforge.usbdm.deviceEditor.information;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.DeviceFamily;
import net.sourceforge.usbdm.deviceEditor.parser.DocumentUtilities;
import net.sourceforge.usbdm.deviceEditor.parser.WriterBase;
import net.sourceforge.usbdm.deviceEditor.parser.WriterBase.InfoTable;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;

/**
 * Represents a peripheral.<br>
 * Includes
 * <li>name e.g. FTM0
 * <li>base-name e.g. FTM0 => FTM
 * <li>instance e.g. FTM0 => 0
 * <li>clock mask
 * <li>clock register
 */
public class PeripheralTemplateInformation {

   /** Device information */
   private final DeviceInfo fDeviceInfo;
   
   /** Writer used for managing and formatting information */
   private final WriterBase fInstanceWriter;
   
   /** Template that is used to select functions applicable to this template */
   private final Pattern fMatchPattern;
   
   /** Clock register for hardware */
   private String fClockReg;
   
   /** Clock mask for hardware */
   private String fClockMask;
   
   /** Hardware interrupt numbers */
   private ArrayList<String> fIrqNums;
   
   /** IRQ handler name */
   private String fIrqHandler;
   
   /** Name of C peripheral class e.g. Ftm2 */
   private final String fClassName;
   
   /** Name of peripheral e.g. FTM2 */
   private final String fPeripheralName;
   
   /** Base name or C peripheral class e.g. Adc */
   private final String fClassBasename;
   
   /** Base name of C peripheral instance e.g. adc */
   private final String fInstanceBaseName;
   
   /** Base name of C peripheral alias e.g. adc_ */
   private final String fAliasBaseName;

   /** Device family */
   private final DeviceFamily fDeviceFamily;

   /** Base name of peripheral e.g. FTM2 => FTM */
   private final String fPeripheralBasename;
   
   /**
    * Create function template
    * 
    * @param deviceInfo             Device information handle
    * @param deviceFamily           Device family
    * @param classBasename          Base name of C peripheral class e.g. FTM2 => Ftm
    * @param peripheralBasename     Base name of peripheral e.g. FTM2 => FTM
    * @param instance               Instance name e.g. FTM2 => "2"
    * @param matchTemplate          Pattern to select use of this template e.g. "FTM\\d+_CH\\d+"
    * @param instanceWriter         Detailed instanceWriter to use
    * 
    * @throws Exception 
    */
   public PeripheralTemplateInformation(
         DeviceInfo     deviceInfo, 
         DeviceFamily   deviceFamily,
         String         classBasename, 
         String         peripheralBasename, 
         String         instance, 
         String         matchTemplate, 
         Class<?>       instanceWriterClass) throws Exception {
      
      Constructor<?> constructor = instanceWriterClass.getConstructor(PeripheralTemplateInformation.class);
      WriterBase instanceWriter = (WriterBase) constructor.newInstance(this);

      fDeviceFamily           = deviceFamily;
      fClassName              = classBasename+instance;
      fPeripheralBasename     = peripheralBasename;
      fPeripheralName         = peripheralBasename+instance;
      fClassBasename          = classBasename;
      fInstanceBaseName       = classBasename+instance;
      fAliasBaseName          = classBasename+instance+"_";
      
      fDeviceInfo             = deviceInfo;
      if (matchTemplate != null) {
         fMatchPattern       = Pattern.compile(matchTemplate);
      }
      else {
         fMatchPattern       = null;
      }
      fInstanceWriter        = instanceWriter;
//      fPeripheralFunctions   = new Vector<PeripheralFunction>();
      fClockMask             = null;
      fClockReg              = null;
      fIrqNums               = new ArrayList<String>();
   }

   /**
    * Checks if the template matches this name
    * 
    * @param factory
    * @param name    Name of function e.g. FTM3_CH2
    * 
    * @return PeripheralFunction or null if not applicable
    */
   public PeripheralFunction appliesTo(DeviceInfo factory, String name) {         
      Matcher matcher = matcher(name);
      if ((matcher == null) || !matcher.matches()) {
         return null;
      }
//      String basename = matcher.group(1);
      String basename = fClassBasename.toUpperCase();
      String instance = matcher.group(2); 
      String signal   = matcher.group(3);
//    String basename = matcher.replaceAll("$1");
      factory.findOrCreatePeripheral(basename, instance, fInstanceWriter.getTitle());
      PeripheralFunction peripheralFunction = factory.createPeripheralFunction(name, basename, instance, signal);
      fInstanceWriter.addFunction(peripheralFunction);
      return peripheralFunction;
   }

   /**
    * Get PCR initialisation string for given pin e.g. for <b><i>PTB4</b></i>
    * <pre>
    * "PORTB_CLOCK_MASK, PORTB_BasePtr,  GPIOB_BasePtr,  4, "
    * OR
    * "0, 0, 0, 0, "
    * </pre>
    * 
    * @param pin The pin being configured
    * 
    * @return
    * @throws Exception 
    */
   static String getPCRInitString(PinInformation pin) {
      if (pin == null) {
         throw new RuntimeException("Pin may not be null");
      }
      String portClockMask = pin.getClockMask();
      if (portClockMask == null) {
         // No PCR - probably an analogue pin
         return "0, 0, 0, 0, ";
      }
      String pcrRegister      = pin.getPORTBasePtr();
      String gpioRegister     = pin.getGpioReg();
      String gpioBitNum       = pin.getGpioBitNum();
      
      return String.format("%-17s %-15s %-15s %-4s", portClockMask+",", pcrRegister+",", gpioRegister+",", gpioBitNum+",");
   }

   /**
    * Gets the numeric index of the function\n
    * e.g. FTM3_Ch2 => 2 etc.
    * 
    * @param function   Function to look up
    * @return  Index, -1 is returned if template doesn't match
    * 
    * @throws Exception If template matches peripheral but unexpected function 
    */
   public int getFunctionIndex(PeripheralFunction function) {
      if (!getMatchPattern().matcher(function.getName()).matches()) {
         return -1;
      }
      return getInstanceWriter().getFunctionIndex(function);
   }

   public boolean useAliases(PinInformation pinInfo) {
      return getInstanceWriter().useAliases(pinInfo);
   }

   /**
    * Gets the template match function
    * 
    * @param function PeripheralFunction to match
    * 
    * @return Non-null if the Matcher exists
    */
   private Matcher matcher(String name) {
      if (getMatchPattern() == null) {
         return null;
      }
      return getMatchPattern().matcher(name);
   }
   /**
    * Checks if the template matches the function
    * 
    * @param function PeripheralFunction to match
    * 
    * @return True if the template is applicable to this function 
    */
   public boolean matches(PeripheralFunction function) {
      Matcher m = matcher(function.getName());
      return (m!=null) && m.matches();
   }

//   /**
//    * Add peripheral function
//    * 
//    * @param function Peripheral function to add
//    * @throws Exception 
//    */
//   public void addFunction(PeripheralFunction function) {
//      int signalIndex = getFunctionIndex(function);
//      if (signalIndex<0) {
//         return;
//      }
//      if (signalIndex>=fPeripheralFunctions.size()) {
//         fPeripheralFunctions.setSize(signalIndex+1);
//      }
//      if ((fPeripheralFunctions.get(signalIndex) != null) && 
//            (fPeripheralFunctions.get(signalIndex) != function)) {
//         throw new RuntimeException("Multiple functions mapped to index new = " + function + ", old = " + fPeripheralFunctions.get(signalIndex));
//      }
//      fPeripheralFunctions.setElementAt(function, signalIndex);
//   }
//   public Vector<PeripheralFunction> getFunctions() {
//      return fPeripheralFunctions;
//   }

   public void setClockInfo(String clockReg, String clockMask) {
      this.fClockReg  = clockReg;
      this.fClockMask = clockMask;
   }

   public String getClockReg() {
      return fClockReg;
   }

   public String getClockMask() {
      return fClockMask;
   }

   public void addIrqNum(String irqNum) {
      this.fIrqNums.add(irqNum);
   }

   public ArrayList<String> getIrqNums() {
      return fIrqNums;
   }

   public int getIrqCount() {
      return fIrqNums.size();
   }
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

   public void setIrqHandler(String irqHandler) {
      this.fIrqHandler  = irqHandler;
   }
   
   public String getIrqHandler() {
      return fIrqHandler;
   }

   /**
    * Indicates that it is necessary to create a Peripheral Information class
    *  
    * @return true if Information class is needed
    * @throws Exception 
    */
   public boolean classIsUsed() {
      boolean needed = (fClockMask != null) || (fClockReg != null) || needPeripheralInformationClass();
//      System.err.println("classIsUsed: " + fBaseName + needed);
      return needed;
   }

   /**
    * Indicates that it is necessary to create a PcrInfo table in the Peripheral Information class
    *  
    * @return true if Information class is needed
    * @throws Exception 
    */
   public boolean needPeripheralInformationClass() {
      return getInstanceWriter().needPeripheralInformationClass();
   }

   /**
    * Indicates that it is necessary to create a PcrInfo table in the Peripheral Information class
    *  
    * @return true if Information class is needed
    * @throws Exception 
    */
   public boolean needPCRTable() {
      return getInstanceWriter().needPCRTable();
   }

   /**
    * Get documentation group title e.g. "Digital Input/Output"
    * 
    * @return name
    */
   public String getGroupTitle() {
      return getInstanceWriter().getGroupTitle();
   }

   /**
    * Get name of documentation group e.g. "DigitalIO_Group"
    * 
    * @return name
    */
   public String getGroupName() {
      return getInstanceWriter().getGroupName();
   }

   /**
    * Get Documentation group brief description <br>e.g. "Allows use of port pins as simple digital inputs or outputs"
    * 
    * @return name
    */
   public String getGroupBriefDescription() {
      return getInstanceWriter().getGroupBriefDescription();
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
   public void writeInfoClass(DeviceInformation deviceInformation, BufferedWriter pinMappingHeaderFile) throws IOException {
      final String DUMMY_TEMPLATE = "         /* %2d */  { 0, 0, 0, 0, 0 },\n";

      if (!classIsUsed()) {
         return;
      }
      DocumentUtilities.writeDocBanner(pinMappingHeaderFile, "Peripheral information for " + fInstanceWriter.getGroupTitle());

      // Open class
      pinMappingHeaderFile.write(String.format(
            "class %s {\n"+
                  "public:\n",
                  fClassName+"Info"
            ));
      // Additional, peripheral specific, information
      pinMappingHeaderFile.write(getInstanceWriter().getInfoConstants());
      if (needPeripheralInformationClass()) {

         ArrayList<InfoTable> functionTables = fInstanceWriter.getFunctionTables();
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
                  DocumentUtilities.writeConditional(pinMappingHeaderFile, String.format("%s_PIN_SEL == %d", peripheralFunction.getName(), choice), valueWritten);
                  String pcrInitString = PeripheralTemplateInformation.getPCRInitString(mappedPin.getPin());
                  pinMappingHeaderFile.write(String.format("         /* %2d */  { %s%d },\n", signalIndex, pcrInitString, mappedPin.getMux().value));

                  valueWritten = true;
                  choice++;
               }
               if (valueWritten) {
                  DocumentUtilities.writeConditionalElse(pinMappingHeaderFile);
               }
               pinMappingHeaderFile.write(String.format(DUMMY_TEMPLATE, signalIndex));
               DocumentUtilities.writeConditionalEnd(pinMappingHeaderFile, valueWritten);
            }
            pinMappingHeaderFile.write(String.format("   };\n"));
         }
      }
      pinMappingHeaderFile.write(String.format("};\n\n"));
      pinMappingHeaderFile.write(getInstanceWriter().getExtraDefinitions());
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
    * @param documentUtilities Where to write
    * @throws IOException 
    * 
    * @throws Exception 
    */
   public void writePeripheralInformation(XmlDocumentUtilities documentUtilities) throws IOException {
      if (!classIsUsed()) {
         return;
      }
      documentUtilities.openTag("peripheral");
      documentUtilities.writeAttribute("name", getClassName());

      // Additional, peripheral specific, information
      getInstanceWriter().writeInfoConstants(documentUtilities);

      if (needPCRTable()) {
         documentUtilities.openTag("pcrs");
         ArrayList<InfoTable> functionTables = fInstanceWriter.getFunctionTables();
         for (InfoTable functionTable:functionTables) {

            for (int signalIndex = 0; signalIndex<functionTable.table.size(); signalIndex++) {
               PeripheralFunction peripheralFunction = functionTable.table.get(signalIndex);
               if (peripheralFunction == null) {
                  continue;
               }
               documentUtilities.openTag("pcr");
               documentUtilities.writeAttribute("index", signalIndex);
               documentUtilities.writeAttribute("function", peripheralFunction.getName());

//            ArrayList<MappingInfo> mappedPins = MappingInfo.getPins(peripheralFunction);
//            boolean valueWritten = false;
//            int choice = 1;
//            for (MappingInfo mappedPin:mappedPins) {
//               if (mappedPin.mux == MuxSelection.Disabled) {
//                  // Disabled selection - ignore
//                  continue;
//               }
//               if (mappedPin.mux == MuxSelection.Reset) {
//                  // Reset selection - ignore
//                  continue;
//               }
//               if (mappedPin.mux == MuxSelection.Fixed) {
//                  // Fixed pin mapping - handled by default following
//                  continue;
//               }
//               documentUtilities.openTag("pcr");
////               DocumentUtilities.writeConditional(documentUtilities, String.format("%s_PIN_SEL == %d", peripheralFunction.getName(), choice), valueWritten);
//               String pcrInitString = PeripheralTemplateInformation.getPCRInitString(mappedPin.pin);
////               documentUtilities.write(String.format("         /* %2d */  { %s%d },\n", signalIndex, pcrInitString, mappedPin.mux.value));
//               documentUtilities.writeAttribute("name", peripheralFunction.getName());
//               documentUtilities.writeAttribute("index", signalIndex);
//               documentUtilities.writeAttribute("mux", mappedPin.mux.value);
//               valueWritten = true;
//               choice++;
documentUtilities.closeTag();
//            }
            }
         }
         documentUtilities.closeTag();
      }
      getInstanceWriter().writeExtraDefinitions(documentUtilities);
      documentUtilities.closeTag();
   }

   @Override
   public String toString() {
      return "Template("+getClassName() + ")";
   }

   public void writeWizard(BufferedWriter headerFile) throws IOException {
      if (classIsUsed()) {
         getInstanceWriter().writeWizard(headerFile);
      }
   }

   public Pattern getMatchPattern() {
      return fMatchPattern;
   }

   public WriterBase getInstanceWriter() {
      return fInstanceWriter;
   }

   public DeviceFamily getDeviceFamily() {
      return fDeviceFamily;
   }

   /**
    * @return Name of C peripheral e.g. FTM2 
    */
   public String getPeripheralName() {
      return fPeripheralName;
   }

   /**
    * @return Name of C peripheral class e.g. Ftm2 
    */
   public String getClassName() {
      return fClassName;
   }

   /**
    * @return Base name of C peripheral class e.g. Ftm
    */
   public String getClassBasename() {
      return fClassBasename;
   }

   /**
    * @return Base name of C peripheral alias e.g. adc_ 
    */
   public String getAliasBaseName() {
      return fAliasBaseName;
   }

   /**
    * @return Base name of C peripheral instance e.g. adc 
    */
   public String getInstanceBaseName() {
      return fInstanceBaseName;
   }

   /**
    * @return Base name of peripheral e.g. FTM2 => FTM 
    */
   public String getPeripheralBasename() {
      return fPeripheralBasename;
   }
   
}