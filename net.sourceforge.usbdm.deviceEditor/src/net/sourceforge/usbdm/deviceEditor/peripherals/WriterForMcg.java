package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

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
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
   }

   @Override
   public void variableChanged(Variable variable) {
      super.variableChanged(variable);
   }

   @Override
   public int getPriority() {
      return 900;
   }

}