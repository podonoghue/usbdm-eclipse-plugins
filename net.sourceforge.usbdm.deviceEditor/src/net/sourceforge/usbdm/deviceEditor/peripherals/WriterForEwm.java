package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of VREF
 */
public class WriterForEwm extends PeripheralWithState {

   public WriterForEwm(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "External Watchdog Monitor";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"IN", "OUT(_b)?"};
      return getSignalIndex(function, signalNames);
   }
}