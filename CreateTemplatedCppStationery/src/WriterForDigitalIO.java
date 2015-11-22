import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
class WriterForDigitalIO extends InstanceWriter {

   static final String CLASS_NAME = "digitalIO_";
   
   public WriterForDigitalIO(boolean deviceIsMKE) {
      super(deviceIsMKE, false);
   }

   public WriterForDigitalIO(boolean deviceIsMKE, boolean useGuard) {
      super(deviceIsMKE, useGuard);
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      return CLASS_NAME+mappingInfo.pin.getName(); // e.g. digitalIO_PTA0;
   }

   @Override
   public String getAliasName(String alias) {
      return CLASS_NAME+alias;
   }

   /** 
    * Write DigitalIO template instantiation e.g. 
    * <pre>
    * const DigitalIO&lt;PORT<b><i>A</b></i>_CLOCK_MASK, PORT<b><i>A</b></i>_BasePtr+offsetof(PORT_Type, PCR[<b><i>0</b></i>]), GPIO<b><i>A</b></i>_BasePtr, (1<<<b><i>0</b></i>)> digitalIO_<b><i>PTA0</i></b>; 
    * </pre>
    * or for MKE devices
    * <pre>
    * const DigitalIO digitalIO_<b><i>PTA17</i></b> = {(volatile GPIO_Type*)GPIO<b><i>A</i></b>),(1UL<<<b><i>17</i></b>)};
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * 
    * @throws IOException
    */
   @Override
   public void writeDeclaration(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException {

      String instance         = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal           = mappingInfo.functions.get(fnIndex).fSignal;
      String instanceName     = getInstanceName(mappingInfo, fnIndex);                    // e.g. digitalIO_PTA0
      String gpioBitMask      = String.format("(1UL<<%s)", signal);                       // (1UL<<n)
      
      String pcrInit          = FunctionTemplateInformation.getPCRInitString(mappingInfo.pin);
      
      cppFile.write(String.format("const DigitalIO<%s ", pcrInit));
      cppFile.write(String.format("GPIO%s_BasePtr, ", instance));
      cppFile.write(String.format("%-9s", gpioBitMask));
      cppFile.write(String.format("> %s;\n", instanceName));
   }

   @Override
   public void writeDefinition(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException {
   }
}