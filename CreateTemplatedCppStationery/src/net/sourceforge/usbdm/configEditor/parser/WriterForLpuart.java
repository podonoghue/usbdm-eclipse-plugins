package net.sourceforge.usbdm.configEditor.parser;

import net.sourceforge.usbdm.configEditor.information.DeviceInfo.DeviceFamily;

public class WriterForLpuart extends WriterForUart {

   public WriterForLpuart(DeviceFamily deviceFamily) {
      super(deviceFamily);
   }

   @Override
   public String getGroupName() {
      return "Uart_Group";
   }

   @Override
   public String getGroupTitle() {
      return "LPUART, Low Power Universal Asynchonous Receiver/Transmitter";
   }

   @Override
   public String getGroupBriefDescription() {
      return "Pins used for LPUART functions";
   }
}
