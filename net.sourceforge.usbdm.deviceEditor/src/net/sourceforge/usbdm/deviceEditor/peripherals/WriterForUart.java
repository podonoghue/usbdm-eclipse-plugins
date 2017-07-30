package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of UART
 */
public class WriterForUart extends PeripheralWithState {

   public WriterForUart(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Universal Asynchronous Receiver/Transmitter";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"TX", "RX", "RTS(_b)?", "CTS(_b)?", "COL(_b)?"};
      return getSignalIndex(function, signalNames);
   }
}