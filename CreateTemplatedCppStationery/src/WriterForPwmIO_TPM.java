import java.io.IOException;

/**
 * Class encapsulating the code for writing an instance of PwmIO (FTM)
 */
class WriterForPwmIO_TPM extends WriterForDigitalIO {

   static final String ALIAS_BASE_NAME       = "tpm_";
   static final String CLASS_BASE_NAME       = "Tpm";
   static final String INSTANCE_BASE_NAME    = "tpm";
   
   public WriterForPwmIO_TPM(boolean deviceIsMKE) {
      super(deviceIsMKE, true);
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
    * const USBDM::Ftm<b><i>1</b></i>&lt;PORT<b><i>A</i></b>_CLOCK_MASK, PORT<b><i>A</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>0</i></b>]), <i><b>3</i></b>, <i><b>17</i></b>>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * 
    * @throws IOException
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) throws IOException {
      String instance  = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal    = mappingInfo.functions.get(fnIndex).fSignal;
      String pcrInit   = FunctionTemplateInformation.getPCRInitString(mappingInfo.pin);
      
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("const %s::%s%s<", CreatePinDescription.NAME_SPACE, CLASS_BASE_NAME, instance));
      sb.append(String.format("%-44s ", pcrInit));
      sb.append(String.format("%3s  ", mappingInfo.mux.value+","));
      sb.append(String.format("%-4s", signal+">"));
      return sb.toString();
   }
}
