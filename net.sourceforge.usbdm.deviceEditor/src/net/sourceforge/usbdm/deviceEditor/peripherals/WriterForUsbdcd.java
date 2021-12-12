package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of TSI
 */
public class WriterForUsbdcd extends PeripheralWithState {

   public WriterForUsbdcd(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "USB Device Charger Detection";
   }
   
   @Override
   public int getSignalIndex(Signal function) {
      return -1;
   }

   @Override
   public boolean hasDigitalFeatures(Signal signal) {
      return false;
   }
}