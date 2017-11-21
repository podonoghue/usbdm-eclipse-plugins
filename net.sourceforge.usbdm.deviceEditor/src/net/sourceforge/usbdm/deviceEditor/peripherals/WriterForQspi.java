package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of FlexIO
 */
public class WriterForQspi extends PeripheralWithState {

   public WriterForQspi(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Quad Serial Peripheral Interface";
   }

   @Override
   public int getSignalIndex(Signal function) {
//      Pattern p;
//      Matcher m;
      
      int offset = 0;
//      p = Pattern.compile("D(\\d+)");
//      m = p.matcher(function.getSignalName());
//      if (m.matches()) {
//         return offset+Integer.parseInt(m.group(1));
//      }
//      offset += 32;

      final String signalNames[] = {
            "A_SS0(_b)?", "A_SS1(_b)?", "A_SCLK", "A_DQS",  
            "A_DATA0", "A_DATA1", "A_DATA2", "A_DATA3",
            "B_SS0(_b)?", "B_SS1(_b)?", "B_SCLK", "B_DQS",  
            "B_DATA0", "B_DATA1", "B_DATA2", "B_DATA3",
            };
      return offset+super.getSignalIndex(function, signalNames);
   }
}