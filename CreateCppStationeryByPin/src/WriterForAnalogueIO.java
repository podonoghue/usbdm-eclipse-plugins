import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Class encapsulating the code for writing an instance of AnalogueIO
 */
class WriterForAnalogueIO extends WriterForDigitalIO {      

   static final String INSTANCE_TEMPLATE = "extern const AnalogueIO %-24s //!< AnalogueIO on pin %s\n";
   static final String CLASS_NAME        = "analogueIO_";

   public WriterForAnalogueIO(boolean deviceIsMKE) {
      super(deviceIsMKE, true);
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      //         return CLASS_NAME+mappingInfo.pin.getName(); // e.g. analogueIO_PTA0;
      return CLASS_NAME+mappingInfo.functions.get(fnIndex).getName(); // e.g. analogueIO_PTA0
   }

   @Override
   public String getAliasName(String alias) {
      return CLASS_NAME+alias;
   }
   @Override
   public void writeDeclaration(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException {
      cppFile.write(String.format(INSTANCE_TEMPLATE, getInstanceName(mappingInfo, fnIndex)+";", mappingInfo.pin.getName()));
   }

   /** 
    * Write AnalogueIO instance e.g. 
    * <pre>
    * const AnalogueIO analogueIO_<b><i>PTA17</i></b> = {&digitalIO_<b><i>PTA17</i></b>, ADC(<b><i>PTA17</i></b>_ADC_NUM), &ADC_CLOCK_REG(<b><i>PTA17</i></b>_ADC_NUM), ADC_CLOCK_MASK(<b><i>PTA17</i></b>_ADC_NUM), <b><i>PTA17</i></b>_ADC_CH};
    * </pre>
    * or, if no PCR
    * <pre>
    * const AnalogueIO analogueIO_<b><i>PTA17</i></b> = {0, ADC(<b><i>PTA17</i></b>_ADC_NUM), &ADC_CLOCK_REG(<b><i>PTA17</i></b>_ADC_NUM), ADC_CLOCK_MASK(<b><i>PTA17</i></b>_ADC_NUM), <b><i>PTA17</i></b>_ADC_CH};
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param suffix         Used to create a unique name when multiple ADC are mappable to the same pin
    * @param cppFile        Where to write
    * 
    * @throws IOException
    */
   @Override
   public void writeDefinition(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException {

      String instance = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal   = mappingInfo.functions.get(fnIndex).fSignal;

      String instanceName = getInstanceName(mappingInfo, fnIndex);                // analogueIO_PTE1
      String adcInstance  = String.format("ADC%s,", instance);                    // ADC(PTE1_ADC_NUM),
      String adcClockReg  = String.format("&ADC%s_CLOCK_REG,", instance);         // &ADCx_CLOCK_REG,
      String adcClockMask = String.format("ADC%s_CLOCK_MASK,", instance);         // ADC_CLOCK_MASK(PTE1_ADC_NUM),
      String adcChannel   = signal;                                               // N
      String pcrInit      = FunctionTemplateInformation.getPCRInitString(mappingInfo.pin);

      //         cppFile.write(String.format("#if %s == %s\n", pinName+"_SEL", Integer.toString(mappingInfo.mux)));
      cppFile.write(String.format("const AnalogueIO %-25s = {", instanceName));
      cppFile.write(String.format("%s, %-10s%-20s%-20s%s};\n", pcrInit, adcInstance, adcClockReg, adcClockMask, adcChannel));
      //         cppFile.write(String.format("#endif\n"));
   }
}