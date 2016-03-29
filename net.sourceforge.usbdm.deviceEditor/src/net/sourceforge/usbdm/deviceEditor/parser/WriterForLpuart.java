package net.sourceforge.usbdm.deviceEditor.parser;

import net.sourceforge.usbdm.deviceEditor.information.PeripheralTemplateInformation;

public class WriterForLpuart extends WriterForUart {

   public WriterForLpuart(PeripheralTemplateInformation owner) {
      super(owner);
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
