import java.io.IOException;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
class WriterForDigitalIO extends InstanceWriter {

   static final String ALIAS_BASE_NAME       = "gpio_";
   static final String CLASS_BASE_NAME       = "Gpio";
   static final String INSTANCE_BASE_NAME    = "gpio";
   
   public WriterForDigitalIO(boolean deviceIsMKE) {
      super(deviceIsMKE, false);
   }

   public WriterForDigitalIO(boolean deviceIsMKE, boolean useGuard) {
      super(deviceIsMKE, useGuard);
   }
   
   /* (non-Javadoc)
    * @see InstanceWriter#getAliasName(java.lang.String)
    */
   @Override
   public String getAliasName(String alias) {
      return ALIAS_BASE_NAME+alias;
   }

   /* (non-Javadoc)
    * @see InstanceWriter#getInstanceName(MappingInfo, int)
    */
   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal   = mappingInfo.functions.get(fnIndex).fSignal;
      return INSTANCE_BASE_NAME+instance+"_"+signal;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * 
    * @throws IOException
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) throws IOException {
      String instance  = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal    = mappingInfo.functions.get(fnIndex).fSignal;
      return "const " + CreatePinDescription.NAME_SPACE + "::" + CLASS_BASE_NAME + instance +"<"+signal+">";
   }
}