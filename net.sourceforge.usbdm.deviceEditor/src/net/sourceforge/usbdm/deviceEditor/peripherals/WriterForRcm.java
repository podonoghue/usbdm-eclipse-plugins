package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of RCM
 */
/**
 * @author podonoghue
 *
 */
public class WriterForRcm extends PeripheralWithState {

   public WriterForRcm(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

//   @Override
//   public void writeInfoClass(DocumentUtilities writer) throws IOException {
//   }

   @Override
   public String getTitle() {
      return "Reset Control Module";
   }

   @Override
   public int getSignalIndex(Signal function) {
      return -1;
   }

}