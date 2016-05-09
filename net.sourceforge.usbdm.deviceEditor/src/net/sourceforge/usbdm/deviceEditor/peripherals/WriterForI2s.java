package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
public class WriterForI2s extends Peripheral {

   public WriterForI2s(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Inter-Integrated-Circuit Interface";
   }

   @Override
   public int getSignalIndex(Signal function) {
      String signalNames[] = {"MCLK", "RX_BCLK", "RX_FS", "TX_BCLK", "TX_FS", "TXD0", "TXD1", "RXD0", "RXD1"};
      return getSignalIndex(function, signalNames);
   }
}