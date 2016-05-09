package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

/**
 * Class encapsulating the code for writing an instance of MCG
 */
public class WriterForMcg extends PeripheralWithState {

   public WriterForMcg(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      loadModels();
   }

   @Override
   public String getTitle() {
      return "Multipurpose Clock Generator";
   }
   
   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
   }


}