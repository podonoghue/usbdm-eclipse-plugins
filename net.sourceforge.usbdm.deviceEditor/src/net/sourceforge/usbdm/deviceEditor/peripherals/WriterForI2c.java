package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of I2C
 */
public class WriterForI2c extends PeripheralWithState {

   public WriterForI2c(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Inter-Integrated-Circuit Interface";
   }

   @Override
   public int getSignalIndex(Signal signal) {
      String signalNames[] = {"SCL", "SDA", "4WSCLOUT", "4WSDAOUT"};
      for (int signalName=0; signalName<signalNames.length; signalName++) {
         if (signal.getSignalName().matches(signalNames[signalName])) {
            return signalName;
         }
      }
      throw new RuntimeException("Signal does not match expected pattern " + signal.getSignalName());
   }

   @Override
   public String getPcrDefinition() {
      return String.format(
            "   //! Base value for PCR (excluding MUX value)\n"+
            "   static constexpr uint32_t %s  = I2C_DEFAULT_PCR;\n\n", DEFAULT_PCR_VALUE_NAME
            );
   }

   @Override
   public String getPcrValue(Signal y) {
      return "USBDM::I2C_DEFAULT_PCR";
   }
   
   
}