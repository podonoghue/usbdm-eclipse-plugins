package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of RTC
 */
public class WriterForSdramc extends PeripheralWithState {

   public WriterForSdramc(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Instance has internal state
      clearConstType();
   }

   @Override
   public String getTitle() {
      return "Synchronous DRAM Controller";
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p;
      Matcher m;
      int offset = 0;
      
      p = Pattern.compile("A(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 32;

      p = Pattern.compile("D(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 32;

      p = Pattern.compile("DQM(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 4;

      final String signalNames[] = {
            "BE7_0_BLS31_24(_b)?",
            "BE15_8_BLS23_16(_b)?",
            "BE23_16_BLS15_8(_b)?",
            "BE31_24_BLS7_0(_b)?", 
            "CLKOUT", 
            "RAS(_b)?", "CAS(_b)?", 
            "WE(_b)?", "CKE", 
            "CS0(_b)?", "CS1(_b)?"};
      return offset+getSignalIndex(function, signalNames);
   }
   
   @Override
   public void addLinkedSignals() {
      final String extraSignals[] = {"FB_BE7_0_BLS31_24_b", "FB_BE15_8_BLS23_16_b", "FB_BE23_16_BLS15_8_b", "FB_BE31_24_BLS7_0_b", "CLKOUT", };
      for (String extraSignal:extraSignals) {
         Signal signal = fDeviceInfo.getSignals().get(extraSignal);
         if (signal != null) {
            addSignal(signal);
         }
      }
   }

}