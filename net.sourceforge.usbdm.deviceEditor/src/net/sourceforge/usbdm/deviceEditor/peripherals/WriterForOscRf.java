package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of RF OSC
 */
public class WriterForRfOsc extends PeripheralWithState {

   public WriterForRfOsc(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "RF Crystal Oscillator";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"^XTAL$", "^EXTAL$", "^XTAL_OUT$", "^XTAL_OUT_EN$", };
      return getSignalIndex(function, signalNames);
   }

   @Override
   public String getPcrDefinition() {
      return null;
   }

   @Override
   public int getPriority() {
      return 1000;
   }

   @Override
   public String getPcrValue(Signal y) {
      return null;
   }


}