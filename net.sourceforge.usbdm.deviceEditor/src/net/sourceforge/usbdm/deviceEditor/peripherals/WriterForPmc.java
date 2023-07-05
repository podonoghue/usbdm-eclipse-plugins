package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;

/**
 * Class encapsulating the code for writing an instance of PMC
 */
public class WriterForPmc extends PeripheralWithState {

   public WriterForPmc(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Power Management Controller";
   }

   @Override
   public void extractHardwareInformation(Peripheral dbPeripheral) {
      extractAllRegisterFields(dbPeripheral);
   }
   
}