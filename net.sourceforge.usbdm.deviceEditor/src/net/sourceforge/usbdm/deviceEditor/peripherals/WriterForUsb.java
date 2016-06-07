package net.sourceforge.usbdm.deviceEditor.peripherals;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of TSI
 */
public class WriterForUsb extends PeripheralWithState {

   public WriterForUsb(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "USB OTG Controller";
   }
   
   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"DM", "DP", "CLKIN", "SOF_OUT", "ID", "VBUS", "VDD"};
      return getSignalIndex(function, signalNames);
   }
}