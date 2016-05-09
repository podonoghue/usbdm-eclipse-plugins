package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of SPI
 */
public class WriterForSpi extends PeripheralWithState {
   
   public WriterForSpi(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      loadModels();
   }

   @Override
   public String getTitle() {
      return "Serial Peripheral Interface";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"SCK", "SIN|MISO", "SOUT|MOSI", "PCS0|PCS|SS", "PCS1", "PCS2", "PCS3", "PCS4", "PCS5"};
      return getSignalIndex(function, signalNames);
   }
}