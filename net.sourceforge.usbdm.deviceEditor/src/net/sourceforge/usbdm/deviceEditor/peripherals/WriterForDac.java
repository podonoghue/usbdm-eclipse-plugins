package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of VREF
 */
public class WriterForDac extends Peripheral {

   public WriterForDac(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Digital-to-Analogue Converter";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"OUT"};
      return getSignalIndex(function, signalNames);
   }
}