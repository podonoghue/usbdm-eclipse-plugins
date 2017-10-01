package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of RTC
 */
public class WriterForSdhc extends PeripheralWithState {

   public WriterForSdhc(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Secure Digital High Capacity Interface";
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
      offset += 8;

      final String signalNames[] = {"CLKIN", "CMD", "DCLK"};
      return offset+getSignalIndex(function, signalNames);
   }
}