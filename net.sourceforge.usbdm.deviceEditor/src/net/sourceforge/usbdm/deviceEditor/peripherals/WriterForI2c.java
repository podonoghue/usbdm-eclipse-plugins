package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of I2C
 */
public class WriterForI2c extends PeripheralWithState {

   public WriterForI2c(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can (usually do) create instances of this class 
      super.setCanCreateInstance(true);
      
      // Instance has internal state
      clearConstType();
   }

   @Override
   public String getTitle() {
      return "Inter-Integrated-Circuit Interface";
   }

   @Override
   public int getSignalIndex(Signal signal) {
      String signalNames[] = {"SCL", "SDA", "4WSCLOUT", "4WSDAOUT", "SCLS", "SDAS", "HREQ"};
      return getSignalIndex(signal, signalNames);
   }

//   @Override
//   public String getPcrDefinition() {
//      return String.format(
//            "   //! Base value for PCR (excluding MUX value)\n"+
//            "   static constexpr uint32_t %s  = I2C_DEFAULT_PCR;\n\n", DEFAULT_PCR_VALUE_NAME
//            );
//   }

   @Override
   public String getPcrValue(Signal y) {
      return "USBDM::I2C_DEFAULT_PCR";
   }
   
   @Override
   public void validateMappedPins() {
      super.validateMappedPins();
      if (fStatus != null) {
         return;
      }
      // Warn if SCL and SDA signals not mapped
      validateMappedPins(new int[]{0,1}, getSignalTables().get(0).table);
   }
}