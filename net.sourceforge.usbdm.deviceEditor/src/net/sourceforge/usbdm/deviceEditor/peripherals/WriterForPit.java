package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of PIT
 */
public class WriterForPit extends PeripheralWithState {

   public WriterForPit(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      loadModels();
   }

   @Override
   public String getTitle() {
      return "Programmable Interrupt Timer";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"OUT"};
      return getSignalIndex(function, signalNames);
   }

   public void loadModels() {
      fData = null;
      switch (fDeviceInfo.getDeviceFamily()) {
      case mk:
         loadModels("Pit");
         break;
      case mkl:
         loadModels("PitSharedIrq");
         break;
      case mke:
      case mkm:
      default:
         return;
      }
   }
   
}