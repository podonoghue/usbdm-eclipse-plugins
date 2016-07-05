package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of RTC
 */
public class WriterForPdb extends PeripheralWithState {

   public WriterForPdb(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Programmable Dely Block";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"EXTRG"};
      return getSignalIndex(function, signalNames);
   }
}