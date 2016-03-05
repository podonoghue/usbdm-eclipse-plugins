/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
class WriterForLptmr extends InstanceWriter {

   static final String ALIAS_BASE_NAME       = "vref_";
   static final String CLASS_BASE_NAME       = "Vref";
   static final String INSTANCE_BASE_NAME    = "vref";

   public WriterForLptmr(boolean deviceIsMKE) {
      super(deviceIsMKE);
   }

   public WriterForLptmr() {
      super(false);
   }

   /* (non-Javadoc)
    * @see InstanceWriter#getAliasName(java.lang.String)
    */
   @Override
   public String getAliasName(String signalName, String alias) {
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
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal       = getFunctionIndex(mappingInfo.functions.get(fnIndex));
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("const %s::%s<%d>", CreatePinDescription.NAME_SPACE, fOwner.fBaseName, signal));
      return sb.toString();
   }
   /* (non-Javadoc)
    * @see InstanceWriter#needPcrTable()
    */
   @Override
   public boolean needPeripheralInformationClass() {
      return true;
   }

   @Override
   public int getFunctionIndex(PeripheralFunction function) {
      final String signalNames[] = {"ALT0", "ALT1", "ALT2", "ALT3"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.fSignal.matches(signalNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("Signal "+function.fSignal+" does not match expected pattern ");
   }
   
   static final String TEMPLATE_DOCUMENTATION = 
         "/**\n"+
         " * Convenience class representing a VREF\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " * using Vref = const USBDM::Vref<VrefInfo>;\n"+
         " * @endcode\n"+
         " *\n"+
         " */\n";
   
   @Override
   public String getTemplate() {
      return TEMPLATE_DOCUMENTATION + String.format(
            "template<uint8_t channel> using %s = Vref<%sInfo>;\n\n",
            fOwner.fBaseName, fOwner.fBaseName);
   }

   @Override
   public String getAlias(String alias, MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   @Override
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) {
      return super.getAlias(getInstanceName(mappingInfo, fnIndex), mappingInfo, fnIndex);
   }

   @Override
   public String getExternDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      return "extern " + getDefinition(mappingInfo, fnIndex);
   }

   @Override
   public boolean useAliases(PinInformation pinInfo) {
      return false;
   }

   @Override
   String getGroupName() {
      return "LPTMR_Group";
   }

   @Override
   String getGroupTitle() {
      return "Abstraction for Low Power Timer";
   }

   @Override
   String getGroupBriefDescription() {
      return "Pins used for Low Power Timer";
   }

}