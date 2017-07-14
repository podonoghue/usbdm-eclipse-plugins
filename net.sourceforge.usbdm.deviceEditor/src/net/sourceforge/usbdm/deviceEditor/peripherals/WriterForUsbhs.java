package net.sourceforge.usbdm.deviceEditor.peripherals;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of TSI
 */
public class WriterForUsbhs extends PeripheralWithState {

   public WriterForUsbhs(String basename, String instance, DeviceInfo deviceInfo) {
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