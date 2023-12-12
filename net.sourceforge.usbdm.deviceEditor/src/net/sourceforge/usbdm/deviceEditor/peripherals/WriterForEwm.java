package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of VREF
 */
public class WriterForEwm extends PeripheralWithState {

   public WriterForEwm(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
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