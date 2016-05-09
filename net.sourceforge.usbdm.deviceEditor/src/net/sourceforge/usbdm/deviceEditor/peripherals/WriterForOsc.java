package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of OSC
 */
public class WriterForOsc extends PeripheralWithState {

   public WriterForOsc(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      loadModels();
   }

   @Override
   public String getTitle() {
      return "Crystal Oscillator";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"^XTAL(32)?$", "^EXTAL(32)?$", };
      return getSignalIndex(function, signalNames);
   }
}