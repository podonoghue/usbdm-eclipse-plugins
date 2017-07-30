package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of RTC
 */
public class WriterForRtc extends PeripheralWithState {

   public WriterForRtc(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Real Time Clock";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"XTAL32", "EXTAL32", "CLKOUT", "CLKIN", "WAKEUP_b"};
      return getSignalIndex(function, signalNames);
   }

   @Override
   public int getPriority() {
      return 900;
   }
}