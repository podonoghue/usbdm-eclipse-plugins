package net.sourceforge.usbdm.deviceEditor.parser;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;

/**
 * Class encapsulating the code for writing an instance of AnalogueIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForAnalogueIO extends WriterBase {      

   static private final String ALIAS_PREFIX = "adc_";
   
   /** Functions that use this writer */
   protected InfoTable fSeFunctions = new InfoTable("infoSE");
         
   /** Functions that use this writer */
   protected InfoTable fDmFunctions = new InfoTable("infoDM");
         
   /** Functions that use this writer */
   protected InfoTable fDpFunctions = new InfoTable("infoDP");
         
   public WriterForAnalogueIO(DeviceInfo deviceInfo, Peripheral peripheral) {
      super(deviceInfo, peripheral);
      // TODO Auto-generated constructor stub
   }

   /* (non-Javadoc)
    * @see WriterForDigitalIO#getAliasName(java.lang.String)
    */
   @Override
   public String getAliasName(String signalName, String alias) {
      Pattern p = Pattern.compile(".*(SE|DM|DP)(\\d+)(a|b)?");
      Matcher m = p.matcher(signalName);
      if (!m.matches()) {
         throw new RuntimeException("Function " + signalName +" does not match expected pattern");
      }
      String signalType = m.group(1);
      if (signalType.equalsIgnoreCase("SE")) {
         return ALIAS_PREFIX+alias;
      }
      else if (signalType.equalsIgnoreCase("DM")) {
         return null;
      }
      else if (signalType.equalsIgnoreCase("DP")) {
         return null;
      }
      return null;
   }

   /* (non-Javadoc)
    * @see WriterForDigitalIO#getInstanceName(MappingInfo, int)
    */
   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getFunctions().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getFunctions().get(fnIndex).getSignal();
      return getClassName()+instance+"_se"+signal;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Adc<b><i>0</i></b>&lt;<b><i>19</i></b>>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    */
   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal       = getFunctionIndex(mappingInfo.getFunctions().get(fnIndex));
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName(), signal));
      return sb.toString();
   }
   
   @Override
   public int getFunctionIndex(PeripheralFunction function) {
      Pattern p = Pattern.compile("(SE|DM|DP)(\\d+)(a|b)?");
      Matcher m = p.matcher(function.getSignal());
      if (!m.matches()) {
         throw new RuntimeException("Function "+function+", Signal " + function.getSignal() + " does not match expected pattern");
      }
      int index = Integer.parseInt(m.group(2));
      if ((m.group(3) != null) && m.group(3).equalsIgnoreCase("a")) {
         index += 32;
      }
      return index;
   }

//   /* (non-Javadoc)
//    * @see WriterForDigitalIO#needPcrTable()
//    */
//   @Override
//   public boolean needPeripheralInformationClass() {
//      // Assume required if functions are present
//      boolean required = needPCRTable();
//      if (!required) {
//         // Shouldn't have clock information for non-existent peripheral 
//         if ((getClockReg() != null) || (getClockMask() != null)) {
//            System.err.println("WARNING - Unexpected clock information for peripheral without signal" + getPeripheralName());
//            return false;
////            throw new RuntimeException("Unexpected clock information for non-present peripheral " + fOwner.fPeripheralName);
//         }
//      }
//      return required;
//   };

   @Override
   public boolean needPCRTable() {
      boolean required = 
            (fPeripheralFunctions.table.size() +
                  fSeFunctions.table.size() + 
                  fDpFunctions.table.size() + 
                  fDmFunctions.table.size()) > 0;
      return required;
   }

    @Override
   public boolean useGuard() {
      return true;
   }

   static final String TEMPLATE_DOCUMENTATION = 
         "/**\n"+
         " * Convenience templated class representing an ADC\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " *  // Instantiate ADC0 single-ended channel #8\n"+
         " *  const adc0<8> adc0_se8;\n"+
         " *\n"+
         " *  // Initialise ADC\n"+
         " *  adc0_se8.initialiseADC(USBDM::resolution_12bit_se);\n"+
         " *\n"+
         " *  // Set as analogue input\n"+
         " *  adc0_se8.setAnalogueInput();\n"+
         " *\n"+
         " *  // Read input\n"+
         " *  uint16_t value = adc0_se8.readAnalogue();\n"+
         " *  @endcode\n"+
         " *\n"+
         " * @tparam adcChannel    ADC channel\n"+
         " */\n";

   /* (non-Javadoc)
    * @see WriterForDigitalIO#getTemplate(FunctionTemplateInformation)
    */
   @Override
   public String getTemplate() {   
      return TEMPLATE_DOCUMENTATION+String.format(
            "template<uint8_t channel> using %s = Adc_T<%sInfo, channel>;\n\n",
            getClassName(), getClassName());
   }

   @Override
   public String getGroupName() {
      return "AnalogueIO_Group";
   }

   @Override
   public String getTitle() {
      return "Analogue Input";
   }

   @Override
   public String getGroupBriefDescription() {
      return "Pins used for analogue inputs";
   }

   @Override
   public String getPcrInfoTableName(PeripheralFunction function) {
      Pattern p = Pattern.compile("(SE|DM|DP)(\\d+)(a|b)?");
      Matcher m = p.matcher(function.getSignal());
      if (!m.matches()) {
         throw new RuntimeException("Function "+function+", Signal " + function.getSignal() + " does not match expected pattern");
      }
      String tableName = "info"+m.group(1);
      String suffix     = m.group(3);
      if ((suffix != null) && suffix.equals("a")) {
         tableName += "Alt";
      }
      return tableName;
   }

   @Override
   public void addFunction(PeripheralFunction function) {
      InfoTable fFunctions = super.fPeripheralFunctions;
      
      Pattern p = Pattern.compile("(SE|DM|DP)(\\d+)(a|b)?");
      Matcher m = p.matcher(function.getSignal());
      if (!m.matches()) {
         throw new RuntimeException("Function "+function+", Signal " + function.getSignal() + " does not match expected pattern");
      }
      int signalIndex = getFunctionIndex(function);
      String signalType = m.group(1);
      if (signalType.equalsIgnoreCase("SE")) {
         fFunctions = fSeFunctions;
      }
      else if (signalType.equalsIgnoreCase("DM")) {
         fFunctions = fDmFunctions;
      }
      else if (signalType.equalsIgnoreCase("DP")) {
         fFunctions = fDpFunctions;
      }
      if (signalIndex>=fFunctions.table.size()) {
         fFunctions.table.setSize(signalIndex+1);
      }
      if ((fFunctions.table.get(signalIndex) != null) && 
            (fFunctions.table.get(signalIndex) != function)) {
         throw new RuntimeException("Multiple functions mapped to index new = " + function + ", old = " + fFunctions.table.get(signalIndex));
      }
      fFunctions.table.setElementAt(function, signalIndex);
   }

   @Override
   public ArrayList<InfoTable> getFunctionTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fPeripheralFunctions);
      rv.add(fSeFunctions);
      rv.add(fDpFunctions);
      rv.add(fDmFunctions);
      return rv;
   }
   
   
}