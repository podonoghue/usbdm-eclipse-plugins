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
public class WriterForLcd extends PeripheralWithState {

   public WriterForLcd(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Instance has internal state
      clearConstType();
   }

   @Override
   public String getTitle() {
      return "LCD, Segment LCD";
   }

   @Override
   public int getSignalIndex(Signal signal) {
      Pattern p = Pattern.compile("P(\\d+)");
      Matcher m = p.matcher(signal.getSignalName());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      throw new RuntimeException("Signal does not match expected pattern " + signal.getSignalName());
   }
   
//   @Override
//   public String getPcrDefinition() {
//      return String.format(
//            "   //! Base value for PCR (excluding MUX value)\n"+
//            "   static constexpr uint32_t %s  = 0;\n\n", DEFAULT_PCR_VALUE_NAME
//            );
//   }

}