package net.sourceforge.usbdm.deviceEditor.parser;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralTemplateInformation;

public class WriterForLpuart extends WriterForUart {

   public WriterForLpuart(String basename, String instance, PeripheralTemplateInformation template, DeviceInfo deviceInfo) {
      super(basename, instance, template, deviceInfo);
   }

   @Override
   public String getGroupTitle() {
      return "LPUART, Low Power Universal Asynchonous Receiver/Transmitter";
   }

}
