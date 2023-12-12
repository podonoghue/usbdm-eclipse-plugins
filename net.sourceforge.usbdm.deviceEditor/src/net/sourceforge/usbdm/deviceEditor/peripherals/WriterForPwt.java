package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of OSC
 */
public class WriterForPwt extends PeripheralWithState {

   public WriterForPwt(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Instance has internal state
      clearConstType();
   }

   @Override
   public String getTitle() {
      return "PWT, Pulse Width Timer";
   }

   @Override
   public int getSignalIndex(Signal signal) {
      Pattern p = Pattern.compile("IN(\\d+)");
      Matcher m = p.matcher(signal.getSignalName());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      throw new RuntimeException("Signal does not match expected pattern " + signal.getSignalName());
   }
}