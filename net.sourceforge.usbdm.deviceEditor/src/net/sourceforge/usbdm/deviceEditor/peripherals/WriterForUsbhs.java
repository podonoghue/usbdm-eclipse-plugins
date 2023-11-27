package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;

/**
 * Class encapsulating the code for writing an instance of TSI
 */
public class WriterForUsbhs extends PeripheralWithState {

   public WriterForUsbhs(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Instance has internal state
      clearConstType();
}

   @Override
   public String getTitle() {
      return "USB High Speed OTG Controller";
   }
   
   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"DM", "DP", "VBUS", "VSS", "ID", "VDD", "CLKIN", "SOF_OUT", "ID"};
      return getSignalIndex(function, signalNames);
   }

   @Override
   public void extractHardwareInformation(Peripheral dbPortPeripheral) {
      extractAllRegisterFields(dbPortPeripheral);
   }
}