/**
 * Class encapsulating the code for writing an instance of PwmIO (T)
 */
class WriterForPwmIO_TPM extends WriterForDigitalIO {

   static final String ALIAS_BASE_NAME       = "tpm_";
   static final String CLASS_BASE_NAME       = "Tpm";
   static final String INSTANCE_BASE_NAME    = "tpm";
   
   public WriterForPwmIO_TPM(boolean deviceIsMKE) {
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
    * const USBDM::Tpm<b><i>1</b></i>&lt;<i><b>17</i></b>>
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
   " * Convenience templated class representing a TPM\n"+
   " *\n"+
   " * Example\n"+
   " * @code\n"+
   " * // Instantiate the tpm channel (for TPM0 CH6)\n"+
   " * const USBDM::Tpm0<6>   tpm0_ch6;\n"+
   " *\n"+
   " * // Initialise PWM with initial period and alignment\n"+
   " * tpm0_ch6.setPwmOutput(200, USBDM::ftm_leftAlign);\n"+
   " *\n"+
   " * // Change period (in ticks)\n"+
   " * tpm0_ch6.setPeriod(500);\n"+
   " *\n"+
   " * // Change duty cycle (in percent)\n"+
   " * tpm0_ch6.setDutyCycle(45);\n"+
   " * @endcode\n"+
   " *\n"+
   " * @tparam channel    TPM channel\n"+
   " */\n";
   @Override
   public String getTemplate(FunctionTemplateInformation pinTemplate) {               
      return TEMPLATE_DOCUMENTATION + String.format(
            "template<uint8_t channel> using %s =\n" +
            "      Tmr_T<%sInfo, %s_BasePtr, SIM_BasePtr+offsetof(SIM_Type, %s_CLOCK_REG), %s_CLOCK_MASK, %s_SC, channel>;\n\n",
            pinTemplate.baseName, pinTemplate.baseName, pinTemplate.peripheralName, pinTemplate.peripheralName, pinTemplate.peripheralName, pinTemplate.peripheralName);
   }
}
