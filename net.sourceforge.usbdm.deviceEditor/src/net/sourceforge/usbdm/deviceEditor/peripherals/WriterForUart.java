package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of UART
 */
public class WriterForUart extends PeripheralWithState {

   public WriterForUart(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can (usually do) create instances of this class
      fCanCreateInstance = true;
      
      // Instance has internal state
      clearConstType();
   }

   @Override
   public String getTitle() {
      return "Universal Asynchronous Receiver/Transmitter";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"TX", "RX", "RTS(_b)?", "CTS(_b)?", "COL(_b)?"};
      return getSignalIndex(function, signalNames);
   }
   
   @Override
   public void validateMappedPins() {
      super.validateMappedPins();
      if (fStatus != null) {
         return;
      }
      // Warn if Rx and Tx signals not mapped
      validateMappedPins(new int[]{0,1}, getSignalTables().get(0).table);
   }
}