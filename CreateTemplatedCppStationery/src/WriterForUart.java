/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
class WriterForUart extends InstanceWriter {

   static final String ALIAS_BASE_NAME       = "uart_";
   static final String CLASS_BASE_NAME       = "Uart";
   static final String INSTANCE_BASE_NAME    = "uart";

   public WriterForUart(boolean deviceIsMKE) {
      super(deviceIsMKE);
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
      final String signalNames[] = {"TX", "RX", "RTS_b|RTS", "CTS_b|CTS"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.fSignal.matches(signalNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("Signal does not match expected pattern " + function.fSignal);
   }
   
   static final String TEMPLATE_DOCUMENTATION = 
         "/**\n"+
         " * Convenience templated class representing an SPI pin\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " * using uart0_PCS0 = const USBDM::Uart0Pin<3>;\n"+
         " * @endcode\n"+
         " *\n"+
         " * @tparam uartPinNum    SPI pin number (index into UartInfo[])\n"+
         " */\n";

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
      return "Uart_Group";
   }

   @Override
   String getGroupTitle() {
      return "UART, Universal Asynchonous Receiver/Transmitter";
   }

   @Override
   String getGroupBriefDescription() {
      return "Pins used for UART functions";
   }
}