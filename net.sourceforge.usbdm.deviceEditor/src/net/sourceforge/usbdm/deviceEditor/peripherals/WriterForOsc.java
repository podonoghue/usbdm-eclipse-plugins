package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of OSC
 */
public class WriterForOsc extends PeripheralWithState {

   public WriterForOsc(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Crystal Oscillator";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"^XTAL(32K?)?$", "^EXTAL(32K?)?$", };
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