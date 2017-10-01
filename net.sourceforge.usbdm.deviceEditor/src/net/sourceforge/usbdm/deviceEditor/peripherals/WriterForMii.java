package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of VREF
 */
public class WriterForMii extends PeripheralWithState {

   public WriterForMii(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Media Independent Interface";
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p;
      Matcher m;
      int offset = 0;
      
      p = Pattern.compile("RXD(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 4;

      p = Pattern.compile("TXD(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 4;
      
      final String signalNames[] = {"COL", "CRS", "MDC", "MDIO", "RXCLK", "RXDV", "RXER", "TXCLK", "TXEN", "TXER", "CRS_DV", };
      return offset+super.getSignalIndex(function, signalNames);
   }
}