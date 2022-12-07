package net.sourceforge.usbdm.deviceEditor.information;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;
import net.sourceforge.usbdm.jni.UsbdmException;

public class WriterForPort extends PeripheralWithState {

   public WriterForPort(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);

      // Can't create instances of this peripheral
      fCanCreateInstance = false;

      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType = true;

      // Can create instances for signals belonging to this peripheral
      fCanCreateSignalInstance = true;
   }

   @Override
   public String getTitle() {
      return "PORT";
   }

}
