package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of VREF
 */
public class WriterForFlexCan extends PeripheralWithState {

   public WriterForFlexCan(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
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