package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of VREF
 */
public class WriterForFlexBus extends PeripheralWithState {

   public WriterForFlexBus(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Flexbus";
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p;
      Matcher m;
      int offset = 0;
      
      p = Pattern.compile("CS(\\d+)(_b)?");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 6;

      p = Pattern.compile("AD(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 32;
      
      p = Pattern.compile("A(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 32;
      
      final String signalNames[] = {"TSIZ0", "TSIZ1", "ALE", "OE(_b)?", "RW(_b)?", "TA(_b)?", "TBST(_b)?", "TS(_b)?", 
            "BE7_0_BLS31_24(_b)?", "BE15_8_BLS23_16(_b)?", "BE23_16_BLS15_8(_b)?", "BE31_24_BLS7_0(_b)?", };
      return offset+super.getSignalIndex(function, signalNames);
   }
}