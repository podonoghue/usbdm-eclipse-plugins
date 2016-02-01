import java.io.BufferedWriter;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
class WriterForI2c extends InstanceWriter {

   static final String ALIAS_BASE_NAME       = "i2c_";
   static final String CLASS_BASE_NAME       = "I2c";
   static final String INSTANCE_BASE_NAME    = "i2c";

   public WriterForI2c(boolean deviceIsMKE) {
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
      throw new Exception("Should not be called");
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
   protected String getPcrDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      String instance  = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal    = Integer.toString(getFunctionIndex(mappingInfo.functions.get(fnIndex)));
//      return "const " + CreatePinDescription.NAME_SPACE + "::" + CLASS_BASE_NAME + instance +"Pcr<"+signal+">";
      return "const " + CreatePinDescription.NAME_SPACE + "::PcrTable_T<" + signal + ", " + CLASS_BASE_NAME + instance + "Info>";
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
   protected String getGpioDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      String instance  = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal    = Integer.toString(getFunctionIndex(mappingInfo.functions.get(fnIndex)));
//      return "const " + CreatePinDescription.NAME_SPACE + "::" + CLASS_BASE_NAME + instance +"Gpio<"+signal+">";
      return "const " + CreatePinDescription.NAME_SPACE + "::GpioTable_T<" + signal + ", " + CLASS_BASE_NAME + instance + "Info>";
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
      String signalNames[] = {"SCL", "SDA"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (signalNames[signal].equalsIgnoreCase(function.fSignal)) {
            return signal;
         }
      }
      throw new Exception("Signal does not match expected pattern " + function.fSignal);
   }
   
   static final String PCR_TEMPLATE_DOCUMENTATION = 
         "/**\n"+
         " * Convenience templated class representing PCR associated with a I2C pin\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " * using i2c0_SCLPin = const USBDM::I2c0Pin<3>;\n"+
         " * @endcode\n"+
         " *\n"+
         " * @tparam i2cPinIndex    I2C pin number (index into I2cInfo[])\n"+
         " */\n";
   static final String GPIO_TEMPLATE_DOCUMENTATION = 
         "/**\n"+
         " * Convenience templated class representing a GPIO used as I2C pin\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " * using i2c0_SCLGpio = const USBDM::I2c0Gpio<3>;\n"+
         " * @endcode\n"+
         " *\n"+
         " * @tparam i2cPinIndex    I2C pin number (index into I2cInfo[])\n"+
         " */\n";

//   /* (non-Javadoc)
//    * @see WriterForDigitalIO#getTemplate(FunctionTemplateInformation)
//    */
//   @Override
//   public String getTemplate(FunctionTemplateInformation pinTemplate) {   
//      StringBuffer sb = new StringBuffer();
//      sb.append(PCR_TEMPLATE_DOCUMENTATION);
//      sb.append(String.format(
//         "template<uint8_t i2cPinIndex> using %s =\n" +
//         "   Pcr_T<getPortClockMask(i2cPinIndex,%sInfo), getPcrReg(i2cPinIndex,%sInfo), getGpioBit(i2cPinIndex,%sInfo),\n" +
//         "      PORT_PCR_MUX(getPcrMux(i2cPinIndex, %sInfo))|I2C_DEFAULT_PCR>;\n\n",
//         pinTemplate.baseName+"Pcr", pinTemplate.baseName, pinTemplate.baseName, pinTemplate.baseName, pinTemplate.baseName));
//      sb.append(GPIO_TEMPLATE_DOCUMENTATION);
//      sb.append(String.format(
//            "template<uint8_t i2cPinIndex> using %s =\n" +
//            "   Gpio_T<getPortClockMask(i2cPinIndex,%sInfo), getPcrReg(i2cPinIndex,%sInfo), getGpioBit(i2cPinIndex,%sInfo),\n" +
//            "      getGpioAddress(i2cPinIndex,%sInfo), PORT_PCR_MUX(FIXED_GPIO_FN)|I2C_DEFAULT_PCR>;\n",
//            pinTemplate.baseName+"Gpio", pinTemplate.baseName, pinTemplate.baseName, pinTemplate.baseName, pinTemplate.baseName, pinTemplate.baseName, pinTemplate.baseName));
//      return sb.toString();
//   }

   @Override
   public String getAlias(String alias, MappingInfo mappingInfo, int fnIndex) throws Exception {
      return String.format("using %-20s = %s\n", alias, getDeclaration(mappingInfo, fnIndex)+";");
   }

   @Override
   public void writeDefinition(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws Exception {
      cppFile.write(String.format("using %-14s = %s\n", getInstanceName(mappingInfo, fnIndex)+"Pcr",  getPcrDeclaration(mappingInfo, fnIndex)+";"));
      cppFile.write(String.format("using %-14s = %s\n", getInstanceName(mappingInfo, fnIndex)+"Gpio", getGpioDeclaration(mappingInfo, fnIndex)+";"));
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