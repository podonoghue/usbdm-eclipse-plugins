/**
 * Class encapsulating the code for writing an instance of PwmIO (FTM)
 */
class WriterForPwmIO_FTM extends WriterForDigitalIO {

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
   public String getAliasName(String alias) {
      return ALIAS_BASE_NAME+alias;
   }

   /* (non-Javadoc)
    * @see WriterForDigitalIO#getInstanceName(MappingInfo, int)
    */
   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal   = mappingInfo.functions.get(fnIndex).fSignal;
      return INSTANCE_BASE_NAME+instance+"_ch"+signal;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Ftm<b><i>1</b></i>&lt;<i><b>17</i></b>>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * @throws Exception 
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      String instance  = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      int    signal    = getFunctionIndex(mappingInfo.functions.get(fnIndex));
      return String.format("const %s::%s%s<%d>", CreatePinDescription.NAME_SPACE, CLASS_BASE_NAME, instance, signal);
   }
   @Override
   public boolean needPcrTable() {
      return true;
   };

   @Override
   public boolean useGuard() {
      return true;
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
   public String getTemplate(FunctionTemplateInformation pinTemplate) {               
      return TEMPLATE_DOCUMENTATION + String.format(
            "template<uint8_t channel> using %s =\n" +
            "      Tmr_T<%sInfo, %s_BasePtr, SIM_BasePtr+offsetof(SIM_Type, %s_CLOCK_REG), %s_CLOCK_MASK, %s_SC, channel>;\n\n",
            pinTemplate.baseName, pinTemplate.baseName, pinTemplate.peripheralName, pinTemplate.peripheralName, pinTemplate.peripheralName, pinTemplate.peripheralName);
   }
}
