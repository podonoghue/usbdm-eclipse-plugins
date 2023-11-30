package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
public class WriterForI2s extends PeripheralWithState {

   public WriterForI2s(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Instance has internal state
      clearConstType();
   }

   @Override
   public String getTitle() {
      return "Synchronous Audio Interface";
   }

   @Override
   public int getSignalIndex(Signal function) {
      
      String signalNames[] = {
            "MCLK", "RX_BCLK", "RX_FS", "TX_BCLK", "TX_FS", "TXD0", "TXD1", "RXD0", "RXD1"};
      return getSignalIndex(function, signalNames);
   }

   
   @Override
   public void extractHardwareInformation(Peripheral dbPortPeripheral) {
      extractAllRegisterFields(dbPortPeripheral);
   }

}