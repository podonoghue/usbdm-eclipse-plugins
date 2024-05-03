package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of SDHC
 */
public class WriterForSdhc extends PeripheralWithState {

   public WriterForSdhc(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can (usually do) create instances of this class
      fCanCreateInstance = true;
      
      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType = true;
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