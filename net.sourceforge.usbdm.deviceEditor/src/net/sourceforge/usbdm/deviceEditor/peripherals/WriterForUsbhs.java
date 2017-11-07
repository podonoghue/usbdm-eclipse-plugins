package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of TSI
 */
public class WriterForUsbhs extends PeripheralWithState {

   public WriterForUsbhs(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "USB High Speed OTG Controller";
   }
   
   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"DM", "DP", "VBUS", "VSS", "ID", "VDD", "CLKIN", "SOF_OUT"};
      return getSignalIndex(function, signalNames);
   }
}