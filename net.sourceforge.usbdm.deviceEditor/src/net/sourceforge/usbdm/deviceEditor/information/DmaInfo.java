package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;

public class DmaInfo {
   public final Peripheral dmaPeripheral;
   public final int        dmaChannelNumber;
   public final String     dmaSource;
   
   public DmaInfo(Peripheral dmaPeripheral, int dmaChannelNumber, String dmaSource) {
      this.dmaPeripheral    = dmaPeripheral;
      this.dmaChannelNumber = dmaChannelNumber;
      this.dmaSource        = dmaSource;
   }
   
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("DmaInfo(");
      sb.append("P("+dmaPeripheral.getName()+")");
      sb.append("C("+dmaChannelNumber+")");
      sb.append("S("+dmaSource+")");
      sb.append("DmaInfo)");
      return sb.toString();
   }
};

