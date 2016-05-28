package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of LPTMR
 */
public class WriterForLptmr extends PeripheralWithState {

   public WriterForLptmr(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Low Power Timer";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"ALT0", "ALT1", "ALT2", "ALT3"};
      return getSignalIndex(function, signalNames);
   }
}