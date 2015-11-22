import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
class WriterForDigitalIO extends InstanceWriter {

   static final String INSTANCE_TEMPLATE = "extern const DigitalIO %-24s //!< DigitalIO on pin %s\n";
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

   @Override
   public void writeDeclaration(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException {
      cppFile.write(String.format(INSTANCE_TEMPLATE, getInstanceName(mappingInfo, fnIndex)+";", mappingInfo.pin.getName()));
   }

   /** 
    * Write DigitalIO instance e.g. 
    * <pre>
    * const DigitalIO digitalIO_<b><i>PTA0</b></i>  = {{&PORT<b><i>A</b></i>->PCR[<b><i>0</b></i>],   PORT<b><i>A</b></i>_CLOCK_MASK}, GPIO<b><i>A</b></i>,  (1UL<<<b><i>0</b></i>)};
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
   public void writeDefinition(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException {

      String instance         = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal           = mappingInfo.functions.get(fnIndex).fSignal;
      String instanceName     = getInstanceName(mappingInfo, fnIndex); // e.g. digitalIO_PTA0
      String gpioInstance     = String.format("GPIO%s,", instance);                       // GPIOx,
      String gpioInstanceMKE  = String.format("(volatile GPIO_Type*)GPIO%s,", instance);  // (volatile GPIO_Type*)GPIOx,
      String gpioBitMask      = String.format("(1UL<<%s)", signal);                       // (1UL<<n)
      String pcrInit          = FunctionTemplateInformation.getPCRInitString(mappingInfo.pin);

      HashSet<PinInformation> set = MappingInfo.getFunctionType("GPIO");
      boolean noDigitalIO = (set != null) && set.contains(mappingInfo.pin);

      cppFile.write(String.format("const DigitalIO %-18s = ", instanceName));

      if (deviceIsMKE()) {
         if (noDigitalIO) {
            // No PCR register - Only analogue function on pin
            cppFile.write("{0,0}");
         }
         else {
            cppFile.write(String.format("{%-18s%s}", gpioInstanceMKE, gpioBitMask));
         }
      }
      else {
         if (noDigitalIO) {
            // No PCR register - Only analogue function on pin
            cppFile.write("{0,0,0,0}");
         }
         else {
            cppFile.write(String.format("{%s, %-8s%s}", pcrInit, gpioInstance, gpioBitMask));
         }
      }
      cppFile.write(String.format(";\n"));
   }

}