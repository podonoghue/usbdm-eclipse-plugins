package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of OSC
 */
public class WriterForScg extends PeripheralWithState {

   public WriterForScg(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "System Clock Generator";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"^XTAL?$", "^EXTAL?$", };
      return getSignalIndex(function, signalNames);
   }
   
   @Override
   public int getPriority() {
      return 1000;
   }

   @Override
   public String getPcrValue(Signal y) {
      return "USBDM::XTAL_DEFAULT_PCR";
   }

   
}