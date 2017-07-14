package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of VREF
 */
public class WriterForFlexCan extends PeripheralWithState {

   public WriterForFlexCan(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Controller Area Network";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"RX", "TX"};
      return getSignalIndex(function, signalNames);
   }
}