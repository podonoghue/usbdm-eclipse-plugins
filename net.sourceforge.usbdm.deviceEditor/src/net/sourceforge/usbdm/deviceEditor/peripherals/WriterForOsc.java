package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of OSC
 */
public class WriterForOsc extends PeripheralWithState {

   public WriterForOsc(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
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
   
   @Override
   public String getPcrDefinition() {
      return String.format(
            "   //! Base value for PCR (excluding MUX value)\n"+
            "   static constexpr uint32_t %s  = 0;\n\n", DEFAULT_PCR_VALUE_NAME
            );
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