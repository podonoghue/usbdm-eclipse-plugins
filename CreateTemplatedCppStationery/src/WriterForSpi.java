import java.io.BufferedWriter;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
class WriterForSpi extends InstanceWriter {

   static final String ALIAS_BASE_NAME       = "spi_";
   static final String CLASS_BASE_NAME       = "Spi";
   static final String INSTANCE_BASE_NAME    = "spi";

   public WriterForSpi(boolean deviceIsMKE) {
      super(deviceIsMKE);
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
    * @throws Exception 
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      String instance  = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal    = Integer.toString(getFunctionIndex(mappingInfo.functions.get(fnIndex)));
      return "const " + CreatePinDescription.NAME_SPACE + "::" + CLASS_BASE_NAME + instance +"Pin<"+signal+">";
   }

   /* (non-Javadoc)
    * @see InstanceWriter#needPcrTable()
    */
   @Override
   public boolean needPcrTable() {
      return true;
   }

   @Override
   public int getFunctionIndex(PeripheralFunction function) throws Exception {
      String signalNames[] = {"SCK", "SIN,MISO", "SOUT,MOSI", "PCS0,PCS", "PCS1", "PCS2", "PCS3", "PCS4", "PCS5"};
      for (int signal=0; signal<signalNames.length; signal++) {
         for (String s:signalNames[signal].split(",")) {
            if (s.equalsIgnoreCase(function.fSignal)) {
               return signal;
            }
         }
      }
      throw new Exception("Signal does not match expected pattern " + function.fSignal);
   }
   
   static final String TEMPLATE_DOCUMENTATION = 
         "/**\n"+
         " * Convenience templated class representing an SPI pin\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " * using spi0_PCS0 = const USBDM::Spi0Pin<3>;\n"+
         " * @endcode\n"+
         " *\n"+
         " * @tparam spiPinNum    SPI pin number (index into SpiInfo[])\n"+
         " */\n";

   /* (non-Javadoc)
    * @see WriterForDigitalIO#getTemplate(FunctionTemplateInformation)
    */
   @Override
   public String getTemplate(FunctionTemplateInformation pinTemplate) {   
      return TEMPLATE_DOCUMENTATION+String.format(
         "template<uint8_t spiPinNum> using %s =\n" +
         "   Pcr_T<getPortClockMask(spiPinNum,%sInfo), getPcrReg(spiPinNum,%sInfo), PORT_PCR_MUX(getPcrMux(spiPinNum, %sInfo))|DEFAULT_PCR>;\n\n",
         pinTemplate.baseName+"Pin", pinTemplate.baseName, pinTemplate.baseName, pinTemplate.baseName);
   }

   @Override
   public String getAlias(String alias, MappingInfo mappingInfo, int fnIndex) throws Exception {
      return null;
   }

   @Override
   public void writeDefinition(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws Exception {
      cppFile.write(super.getAlias(getInstanceName(mappingInfo, fnIndex), mappingInfo, fnIndex));
   }

   @Override
   public void writeDeclaration(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws Exception {
      cppFile.write("extern ");
      writeDefinition(mappingInfo, fnIndex, cppFile);
   }

   @Override
   public boolean useAliases() {
      return false;
   }

}