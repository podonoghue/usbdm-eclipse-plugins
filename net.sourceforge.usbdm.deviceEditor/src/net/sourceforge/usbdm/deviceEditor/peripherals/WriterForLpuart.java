package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

public class WriterForLpuart extends WriterForUart {

   public WriterForLpuart(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getGroupTitle() {
      return "LPUART, Low Power Universal Asynchonous Receiver/Transmitter";
   }

}
