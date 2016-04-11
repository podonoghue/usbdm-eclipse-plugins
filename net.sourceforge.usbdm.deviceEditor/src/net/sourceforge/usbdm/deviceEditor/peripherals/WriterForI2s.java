package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForI2s extends Peripheral {
   static final String CLASS_BASE_NAME       = "I2s";
   static final String INSTANCE_BASE_NAME    = "i2s";


   public WriterForI2s(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Inter-Integrated-Circuit Interface";
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignalName();
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
   protected String getPcrDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      String instance  = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal    = Integer.toString(getSignalIndex(mappingInfo.getSignals().get(fnIndex)));
//      return "const " + CreatePinDescription.NAME_SPACE + "::" + CLASS_BASE_NAME + instance +"Pcr<"+signal+">";
      return "const " + DeviceInfo.NAME_SPACE + "::PcrTable_T<" + CLASS_BASE_NAME + instance + "Info, " + signal + ">" ;
//      return "const " + CreatePinDescription.NAME_SPACE + "::PcrTable_T<" + signal + ", " + CLASS_BASE_NAME + instance + "Info>";
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
      String instance  = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal    = Integer.toString(getSignalIndex(mappingInfo.getSignals().get(fnIndex)));
//      return "const " + CreatePinDescription.NAME_SPACE + "::" + CLASS_BASE_NAME + instance +"Gpio<"+signal+">";
//      return "const " + CreatePinDescription.NAME_SPACE + "::GpioTable_T<" + signal + ", " + CLASS_BASE_NAME + instance + "Info>";
      return "const " + DeviceInfo.NAME_SPACE + "::GpioTable_T<" + CLASS_BASE_NAME + instance + "Info, " + signal + ">" ;
   }

   @Override
   public int getSignalIndex(Signal function) {
//   (RXD(\\d+))|(TXD(\\d+))|(RW(_b)?)|(TS(_b)?)|(AD\\d+))
      String signalNames[] = {"MCLK", "RX_BCLK", "RX_FS", "TX_BCLK", "TX_FS"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.getSignalName().matches(signalNames[signal])) {
            return signal;
         }
      }
      //TODO other signals
      return -1;
//      throw new RuntimeException("Signal does not match expected pattern " + function.getSignal());
   }
   
   @Override
   public String getAliasDeclaration(String alias, MappingInfo mappingInfo, int fnIndex) {
      return String.format("using %-20s = %s\n", alias, getDeclaration(mappingInfo, fnIndex)+";");
   }

   @Override
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) {
      return null;
//      return String.format("using %-14s = %s\n", getInstanceName(mappingInfo, fnIndex)+"Pcr",  getPcrDeclaration(mappingInfo, fnIndex)+";") +
//             String.format("using %-14s = %s\n", getInstanceName(mappingInfo, fnIndex)+"Gpio", getGpioDeclaration(mappingInfo, fnIndex)+";");
   }

   @Override
   public String getExternDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      return "extern " + getDefinition(mappingInfo, fnIndex);
   }

}