package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

/**
 * Class encapsulating the code for writing an instance of MCG
 */
public class WriterForMcg extends PeripheralWithState {

   public WriterForMcg(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Multipurpose Clock Generator";
   }
   
   @Override
   public int getPriority() {
      return 900;
   }

   @Override
   public String getVersion() {
      return super.getVersion();
   }

}