package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

/**
 * Class encapsulating the code for writing an instance of PMC
 */
public class WriterForPmc extends PeripheralWithState {

   public WriterForPmc(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Power Management Controller";
   }
}