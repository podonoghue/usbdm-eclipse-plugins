package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of FlexIO
 */
public class WriterForFlexio extends PeripheralWithState {

   public WriterForFlexio(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Flexible I/O";
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p;
      Matcher m;
      
      int offset = 0;
      p = Pattern.compile("D(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 32;

      final String signalNames[] = {};
      return offset+super.getSignalIndex(function, signalNames);
   }
}