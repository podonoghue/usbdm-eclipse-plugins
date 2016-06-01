package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

/**
 * Class encapsulating the code for writing an instance of UART
 */
public class WriterForSim extends PeripheralWithState {

   public WriterForSim(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "System Integration Module";
   }

   @Override
   public int getPriority() {
      return 1000;
   }

}