package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForInterrupt extends PeripheralWithState {

   public WriterForInterrupt(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Direct Memory Access (DMA)";
   }


   @Override
   public void extractHardwareInformation(Peripheral dbPeripheral) {
      extractAllRegisterFields(dbPeripheral);
   }

   @Override
   public int getSignalIndex(Signal signal) {
      return -1;
   }

}