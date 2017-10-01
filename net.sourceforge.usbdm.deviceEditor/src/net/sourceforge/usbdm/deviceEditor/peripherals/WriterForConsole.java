package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

/**
 * Class encapsulating the code for writing an instance of OSC
 */
public class WriterForConsole extends PeripheralWithState {

   public WriterForConsole(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Console Interface";
   }
   
   public void writeInfoClass(DocumentUtilities pinMappingHeaderFile) throws IOException {
      
   };

//   @Override
//   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
//      pinMappingHeaderFile.write("   // Template:" + getVersion()+"\n\n");
//      String template = fData.fTemplate.get("");
//      if (template != null) {
//         pinMappingHeaderFile.write(substitute(template));
//      }
//   }
}