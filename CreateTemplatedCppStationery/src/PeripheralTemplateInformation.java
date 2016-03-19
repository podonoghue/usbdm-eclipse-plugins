import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing information about a function available on a pin
 */
class PeripheralTemplateInformation {

   /**
    * List of templates
    */
   static private ArrayList<PeripheralTemplateInformation> list = new ArrayList<PeripheralTemplateInformation>();

   /**
    * Clear list of templates
    */
   static void reset() {
      list = new ArrayList<PeripheralTemplateInformation>();
   }

//   /**
//    * Get template for given basename
//    * 
//    * @return Template or null if none match
//    */
//   static FunctionTemplateInformation getTemplate(String baseName) {
//      for (FunctionTemplateInformation item:list) {
//         if (item.baseName == baseName) {
//            return item;
//         }
//      }
//      return null;
//   }

   /**
    * Get list of all templates
    * 
    * @return
    */
   static ArrayList<PeripheralTemplateInformation> getList() {
      return list;
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
   static String getPCRInitString(PinInformation pin) throws Exception {
      if (pin == null) {
         throw new Exception("Pin may not be null");
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

   /** Name to use as base-name of peripheral e.g. Ftm2 */
   final String fBaseName;
   /** Name of peripheral e.g. FTM2 */
   final String fPeripheralName;
   /** */
   final InstanceWriter fInstanceWriter;
   /** */
   private final Pattern fMatchPattern;
   /*
    * Functions that match this template ordered by signal 
    */
   private Vector<PeripheralFunction> fPeripheralFunctions;
   private String fClockReg;
   private String fClockMask;
   private ArrayList<String> fIrqNums;
   private String fIrqHandler;

   /**
    * Create function template
    * 
    * @param baseName               Name to use as base-name of peripheral e.g. Ftm2
    * @param peripheralName         Name of peripheral e.g. FTM2
    * @param matchTemplate          Pattern to select use of this template e.g. "FTM\\d+_CH\\d+"
    * @param instanceWriter         Detailed instanceWriter to use
    */
   public PeripheralTemplateInformation(
         String baseName, String peripheralName, 
         String matchTemplate, 
         InstanceWriter instanceWriter) {
      this.fBaseName              = baseName;
      this.fPeripheralName        = peripheralName;
      if (matchTemplate != null) {
         this.fMatchPattern          = Pattern.compile(matchTemplate);
      }
      else {
         this.fMatchPattern          = null;
      }
      this.fInstanceWriter        = instanceWriter;
      this.fPeripheralFunctions   = new Vector<PeripheralFunction>();
      this.fClockMask             = null;
      this.fClockReg              = null;
      this.fIrqNums               = new ArrayList<String>();

      instanceWriter.setOwner(this);
      list.add(this);
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
   public int getFunctionIndex(PeripheralFunction function) throws Exception {
      if (!fMatchPattern.matcher(function.getName()).matches()) {
         return -1;
      }
      return fInstanceWriter.getFunctionIndex(function);
   }
   
   /**
    * Gets template that matches this function
    * 
    * @param function   Function to match
    * 
    * @return Matching template or null on none
    */
   public static PeripheralTemplateInformation getTemplate(PeripheralFunction function) {
      for (PeripheralTemplateInformation functionTemplateInformation:list) {
         if (functionTemplateInformation.fMatchPattern.matcher(function.getName()).matches()) {
            return functionTemplateInformation;
         }
      }
      return null;
   }

   public boolean useAliases(PinInformation pinInfo) {
      return fInstanceWriter.useAliases(pinInfo);
   }

   /**
    * Gets the template match function
    * 
    * @param function PeripheralFunction to match
    * 
    * @return Non-null if the Matcher exists
    */
   public Matcher matcher(String name) {
      if (fMatchPattern == null) {
         return null;
      }
      return fMatchPattern.matcher(name);
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
   
   /**
    * Add peripheral function
    * 
    * @param function Peripheral function to add
    * @throws Exception 
    */
   public void addFunction(PeripheralFunction function) throws Exception {
      int signalIndex = getFunctionIndex(function);
      if (signalIndex<0) {
         throw new Exception("Function doesn't match this template");
      }
      if (signalIndex>=fPeripheralFunctions.size()) {
         fPeripheralFunctions.setSize(signalIndex+1);
      }
      if ((fPeripheralFunctions.get(signalIndex) != null) && 
          (fPeripheralFunctions.get(signalIndex) != function)) {
         throw new Exception("Multiple functions mapped to index new = " + function + ", old = " + fPeripheralFunctions.get(signalIndex));
      }
      fPeripheralFunctions.setElementAt(function, signalIndex);
   }
   public Vector<PeripheralFunction> getFunctions() {
      return fPeripheralFunctions;
   }

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
   public boolean classIsUsed() throws Exception {
      return (fClockMask != null) || (fClockReg != null) || needPcrInfoTable();
   }

   /**
    * Indicates that it is necessary to create a PcrInfo table in the Peripheral Information class
    *  
    * @return true if Information class is needed
    * @throws Exception 
    */
   public boolean needPcrInfoTable() {
      return fInstanceWriter.needPeripheralInformationClass() && (getFunctions().size() > 0);
   }

   public String getGroupTitle() {
      return fInstanceWriter.getGroupTitle();
   }

   public String getGroupName() {
      return fInstanceWriter.getGroupName();
      }

   public String getGroupBriefDescription() {
      return fInstanceWriter.getGroupBriefDescription();
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
    * @param pinMappingHeaderFile Where to write
    * 
    * @throws Exception 
    */
   public void writeInfoClass(BufferedWriter pinMappingHeaderFile) throws Exception {
      final String DUMMY_TEMPLATE = "         /* %2d */  { 0, 0, 0, 0, 0 },\n";
      
      if (!classIsUsed()) {
         return;
      }
      DocumentUtilities.writeDocBanner(pinMappingHeaderFile, "Peripheral information for "+getGroupTitle());
      
      // Open class
      pinMappingHeaderFile.write(String.format(
            "class %s {\n"+
            "public:\n",
            fBaseName+"Info"
            ));
      // Additional, peripheral specific, information
      pinMappingHeaderFile.write(fInstanceWriter.getInfoConstants());
      if (needPcrInfoTable()) {
         // Signal information table
         pinMappingHeaderFile.write(String.format(
               "   //! Information for each pin of peripheral\n"+
                     "   static constexpr PcrInfo  info[32] = {\n"+
                     "\n"
               ));
         pinMappingHeaderFile.write("         //          clockMask         pcrAddress      gpioAddress gpioBit muxValue\n");
         for (int signalIndex = 0; signalIndex<getFunctions().size(); signalIndex++) {
            PeripheralFunction peripheralFunction = getFunctions().get(signalIndex);
            if (peripheralFunction == null) {
               pinMappingHeaderFile.write(String.format(DUMMY_TEMPLATE, signalIndex));
               continue;
            }
            ArrayList<MappingInfo> mappedPins = MappingInfo.getPins(peripheralFunction);
            boolean valueWritten = false;
            int choice = 1;
            for (MappingInfo mappedPin:mappedPins) {
               if (mappedPin.mux == MuxSelection.Disabled) {
                  // Disabled selection - ignore
                  continue;
               }
               if (mappedPin.mux == MuxSelection.Reset) {
                  // Reset selection - ignore
                  continue;
               }
               if (mappedPin.mux == MuxSelection.Fixed) {
                  // Fixed pin mapping - handled by default following
                  continue;
               }
               DocumentUtilities.writeConditional(pinMappingHeaderFile, String.format("%s_PIN_SEL == %d", peripheralFunction.getName(), choice), valueWritten);
               String pcrInitString = PeripheralTemplateInformation.getPCRInitString(mappedPin.pin);
               pinMappingHeaderFile.write(String.format("         /* %2d */  { %s%d },\n", signalIndex, pcrInitString, mappedPin.mux.value));

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
      pinMappingHeaderFile.write(String.format("};\n\n"));
      pinMappingHeaderFile.write(fInstanceWriter.getExtraDefinitions());
   }

}