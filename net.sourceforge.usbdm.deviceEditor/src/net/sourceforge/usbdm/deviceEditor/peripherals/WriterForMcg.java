package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.validators.ClockValidator;
import net.sourceforge.usbdm.deviceEditor.validators.PllClockValidater;

/**
 * Class encapsulating the code for writing an instance of MCG
 */
public class WriterForMcg extends PeripheralWithState {

   public WriterForMcg(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      loadModels();
      //                                    maxCoreClockfrequency, maxBusClockFrequency, maxFlashClockFrequency
      addValidator(new ClockValidator(this, 120000000,             60000000,             30000000));
      //                                       pllOutMin, pllOutMax  pllInMin pllInMax prDivMin prDivMax vDivMin vDivMax pllPostDiv
      addValidator(new PllClockValidater(this, 48000000,  120000000, 2000000, 4000000, 1,       25,      24,     55,     1));
//      addValidator(new FLLValidator(this));
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
   public void loadModels() {
      super.loadModels();
   }

   @Override
   protected void variableChanged(Variable variable) {
      super.variableChanged(variable);
   }
}