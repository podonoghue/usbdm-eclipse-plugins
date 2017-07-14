package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of VREF
 */
public class WriterForEnet extends PeripheralWithState {

   public WriterForEnet(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "10/100-Mbps Ethernet MAC";
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p;
      Matcher m;
      int offset = 0;
      
      p = Pattern.compile("MII_RXD(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 4;

      p = Pattern.compile("MII_TXD(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 4;
      
      p = Pattern.compile("RMII_RXD(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 4;

      p = Pattern.compile("RMII_TXD(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 4;
      
      p = Pattern.compile("1588_TMR(\\d+)");
      m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return offset+Integer.parseInt(m.group(1));
      }
      offset += 4;

      final String signalNames[] = {"1588_CLKIN", 
            "MII_COL", "MII_CRS", "MII_MDC", "MII_MDIO", "MII_RXCLK", "MII_RXDV", "MII_RXER", "MII_TXCLK", "MII_TXEN", "MII_TXER", 
            "RMII_CRS_DV", "RMII_MDC", "RMII_MDIO", "RMII_RXER", "RMII_TXEN"};
      return offset+super.getSignalIndex(function, signalNames);
   }
}