package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of TSI
 */
public class WriterForUsbhsdcd extends WriterForUsbdcd {

   public WriterForUsbhsdcd(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }
}