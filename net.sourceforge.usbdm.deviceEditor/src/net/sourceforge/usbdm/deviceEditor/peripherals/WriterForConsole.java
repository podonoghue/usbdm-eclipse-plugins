package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp.WriterInformation;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of OSC
 */
public class WriterForConsole extends PeripheralWithState {

   public WriterForConsole(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      setSynthetic();
   }

   @Override
   public String getTitle() {
      return "Console Interface";
   }
   
   @Override
   public void writeInfoClass(WriterInformation writerInformation) throws IOException {
      super.writeInfoConstants(writerInformation.writer);
   }
}