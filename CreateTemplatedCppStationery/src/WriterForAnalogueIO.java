import java.io.IOException;

/**
 * Class encapsulating the code for writing an instance of AnalogueIO
 */
class WriterForAnalogueIO extends WriterForDigitalIO {      

   static final String ALIAS_BASE_NAME       = "adc_";
   static final String CLASS_BASE_NAME       = "Adc";
   static final String INSTANCE_BASE_NAME    = "adc";
   
   public WriterForAnalogueIO(boolean deviceIsMKE) {
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
      return INSTANCE_BASE_NAME+instance+"_se"+signal;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Adc<b><i>1</i></b>&lt;PORT<b><i>E</i></b>_CLOCK_MASK, PORT<b><i>E</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>24</i></b>]), <b><i>17</i></b>>          
    * </pre>
    * or, if no PCR
    * <pre>
    * const USBDM::Adc<b><i>0</i></b>&lt;<b><i>0</i></b>, <b><i>0</i></b>, <b><i>19</i></b>>
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
      sb.append(String.format("%3s", signal+">"));
      return sb.toString();
   }
}