package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of RTC
 */
public class WriterForPdb extends PeripheralWithState {

   public WriterForPdb(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Programmable Delay Block";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"EXTRG"};
      return getSignalIndex(function, signalNames);
   }
}