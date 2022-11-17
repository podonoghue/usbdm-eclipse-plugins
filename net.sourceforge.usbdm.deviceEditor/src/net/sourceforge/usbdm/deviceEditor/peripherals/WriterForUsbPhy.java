package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;

/**
 * Class encapsulating the code for writing an instance of TSI
 */
public class WriterForUsbPhy extends PeripheralWithState {

   public WriterForUsbPhy(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Instance has internal state
      clearConstType();
}

   @Override
   public String getTitle() {
      return "USB PHY Controller";
   }
   

   @Override
   public void extractHardwareInformation(Peripheral dbPortPeripheral) {
      extractAllRegisterFields(dbPortPeripheral);
   }
 }