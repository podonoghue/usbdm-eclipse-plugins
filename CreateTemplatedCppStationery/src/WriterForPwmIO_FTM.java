import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class encapsulating the code for writing an instance of PwmIO (FTM)
 */
class WriterForPwmIO_FTM extends InstanceWriter {

   static final String ALIAS_BASE_NAME       = "ftm_";
   static final String CLASS_BASE_NAME       = "Ftm";
   static final String INSTANCE_BASE_NAME    = "ftm";
   
   public WriterForPwmIO_FTM(boolean deviceIsMKE) {
      super(deviceIsMKE);
   }
   
   /* (non-Javadoc)
    * @see WriterForDigitalIO#getAliasName(java.lang.String)
    */
   @Override
   public String getAliasName(String signalName, String alias) {
//      System.err.println(String.format("getAliasName(%s,%s)", signalName, alias));
      if (signalName.matches(".*ch\\d+")) {
         return ALIAS_BASE_NAME+alias;
      }
      return null;
   }

   /* (non-Javadoc)
    * @see WriterForDigitalIO#getInstanceName(MappingInfo, int)
    */
   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal   = mappingInfo.functions.get(fnIndex).fSignal.replaceAll("CH", "ch");
      return INSTANCE_BASE_NAME+instance+"_"+signal;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Ftm<b><i>1</b></i>&lt;<i><b>17</i></b>>
    * </pre>
    * @param  mappingInfo    Mapping information (pin and peripheral function)
    * @param  fnIndex        Index into list of functions mapped to pin
    * @throws Exception 
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      String instance  = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      int    signal    = getFunctionIndex(mappingInfo.functions.get(fnIndex));
//      System.err.println(String.format("getDeclaration(i=%s,s=%s)", instance, signal));
      if (signal<8) {
         // FTM channel
         return String.format("const %s::%s%s<%d>", CreatePinDescription.NAME_SPACE, CLASS_BASE_NAME, instance, signal);
      }
      if (signal==8) {
      // FTM channel
      return String.format("const %s::%s%s<%d>", CreatePinDescription.NAME_SPACE, CLASS_BASE_NAME, instance, signal);
      }
      return null;
   }
   @Override
   public boolean needPeripheralInformationClass() {
      return true;
   };

   @Override
   public boolean useGuard() {
      return true;
   }

   @Override
   public int getFunctionIndex(PeripheralFunction function) {
      Pattern p = Pattern.compile("CH(\\d+)");
      Matcher m = p.matcher(function.fSignal);
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      final String signalNames[] = {"QD_PHA", "QD_PHB", "CLKIN0", "CLKIN1", "FLT0", "FLT1", "FLT2", "FLT3"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.fSignal.matches(signalNames[signal])) {
            return 8+signal;
         }
      }
      throw new RuntimeException("function '" + function.fSignal + "' does not match expected pattern");
   }

   static final String TEMPLATE_DOCUMENTATION = 
   "/**\n"+
   " * Convenience templated class representing a FTM\n"+
   " *\n"+
   " * Example\n"+
   " * @code\n"+
   " * // Instantiate the ftm channel (for FTM0 CH6)\n"+
   " * const USBDM::Ftm0<6>   ftm0_ch6;\n"+
   " *\n"+
   " * // Initialise PWM with initial period and alignment\n"+
   " * ftm0_ch6.setPwmOutput(200, USBDM::ftm_leftAlign);\n"+
   " *\n"+
   " * // Change period (in ticks)\n"+
   " * ftm0_ch6.setPeriod(500);\n"+
   " *\n"+
   " * // Change duty cycle (in percent)\n"+
   " * ftm0_ch6.setDutyCycle(45);\n"+
   " * @endcode\n"+
   " *\n"+
   " * @tparam channel    Timer channel\n"+
   " */\n";
   @Override
   public String getTemplate() {
      return TEMPLATE_DOCUMENTATION + String.format(
            "template<uint8_t channel> using %s = TmrBase_T<%sInfo, channel>;\n\n",
            fOwner.fBaseName, fOwner.fBaseName, fOwner.fPeripheralName);
   }

   @Override
   public String getInfoConstants() {
      return super.getInfoConstants()+
         String.format(
         "   //! Base value for tmr->SC register\n"+
         "   static constexpr uint32_t scValue  = %s;\n\n",
         fOwner.fPeripheralName+"_SC");
   }

   @Override
   String getGroupName() {
      return "PwmIO_Group";
   }

   @Override
   String getGroupTitle() {
      return "PWM, Input capture, Output compare";
   }

   @Override
   String getGroupBriefDescription() {
      return "Allows use of port pins as PWM outputs";
   }
}
