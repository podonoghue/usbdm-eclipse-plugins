package net.sourceforge.usbdm.configEditor.information;

public class DmaInfo {
   public final int    dmaInstance;
   public final int    dmaChannelNumber;
   public final String dmaSource;
   public DmaInfo(int dmaInstance, int dmaChannelNumber, String dmaSource) {
      this.dmaInstance      = dmaInstance;
      this.dmaChannelNumber = dmaChannelNumber;
      this.dmaSource        = dmaSource;
   }
};

