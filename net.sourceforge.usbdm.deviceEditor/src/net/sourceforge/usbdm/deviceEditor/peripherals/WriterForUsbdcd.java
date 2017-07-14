package net.sourceforge.usbdm.deviceEditor.peripherals;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of TSI
 */
public class WriterForUsbdcd extends PeripheralWithState {

   public WriterForUsbdcd(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "USB Device Charger Detection";
   }
   
   @Override
   public int getSignalIndex(Signal function) {
      return -1;
//      final String signalNames[] = {"CLKIN", "DM", "DP", "SOF_OUT", "ID",};
//      return getSignalIndex(function, signalNames);
   }
}