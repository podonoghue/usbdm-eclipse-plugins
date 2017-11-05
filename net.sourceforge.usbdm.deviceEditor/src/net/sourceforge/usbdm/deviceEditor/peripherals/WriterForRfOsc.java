package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of RF OSC
 */
public class WriterForRfOsc extends PeripheralWithState {

   public WriterForRfOsc(String basename, String instance, DeviceInfo deviceInfo) {
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