package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class FLLValidator extends BaseClockValidator {

   public FLLValidator(PeripheralWithState peripheral) {
      super(peripheral);
   }

   @Override
   protected void validate() {

      // Warning level to use (depend on whether FLL is enabled)
      boolean  fll_enabled                     = getVariable("fll_enabled").getValueAsBoolean();
      Severity warningLevel                    = fll_enabled?Severity.WARNING:Severity.OK;

      Variable mcg_c4_dmx32Node               =  getVariable("mcg_c4_dmx32");
      Variable mcg_c4_drst_drsNode            =  getVariable("mcg_c4_drst_drs");

      Variable fllTargetFrequencyNode         =  getVariable("fllTargetFrequency");

      Variable     fllInputFrequencyNode      =  getVariable("fllInputFrequency");

      long fllInputFrequency = fllInputFrequencyNode.getValueAsLong();

      // Default values
      long mcg_c4_drst_drs = 0;

//      boolean fllOutputValid = true;
      Message fllTargetFrequencyMessage = null;

      //=================
      // Determine possible output frequencies & check against desired value
      //
      long fllTargetFrequency = fllTargetFrequencyNode.getValueAsLong();

      long fllOutFrequency;
      if (mcg_c4_dmx32Node.getValueAsBoolean()) {
         fllOutFrequency = Math.round(fllInputFrequency * 732.0);
      }
      else {
         fllOutFrequency = Math.round(fllInputFrequency * 640.0);
      }
      mcg_c4_drst_drs = -1;
      long probe = 0;
      ArrayList<Long> fllFrequencies = new ArrayList<Long>(); 
      for (probe=1; probe<=4; probe++) {
         fllFrequencies.add(fllOutFrequency*probe);
         // Accept value within ~10% of desired
         if (Math.abs((fllOutFrequency*probe) - fllTargetFrequency) < (fllTargetFrequency/10)) {
            mcg_c4_drst_drs = probe-1;
         }         
      }

      StringBuilder sb = new StringBuilder();
      Severity severity = Severity.OK;
      if (mcg_c4_drst_drs >= 0) {
         if (fllTargetFrequency != (fllOutFrequency*(mcg_c4_drst_drs+1))) {
            // Adjust rounded value
            fllTargetFrequency = (fllOutFrequency*(mcg_c4_drst_drs+1));
            fllTargetFrequencyNode.setValue(fllTargetFrequency);
         }
      }
      else {
//         fllOutputValid = false;
         mcg_c4_drst_drs = 0;
         sb.append("Not possible to generate desired FLL frequency from input clock. \n");
         severity = warningLevel;
      }
      boolean needComma = false;
      for (Long freq : fllFrequencies) {
         if (needComma) {
            sb.append(", ");
         }
         else {
            sb.append("Possible values (Hz) = ");
         }
         needComma = true;
         sb.append(EngineeringNotation.convert(freq, 5)+"Hz");
      }
      fllTargetFrequencyMessage = new Message(sb.toString(), severity);
      //         System.err.println("FllClockValidate.validate() fllOutFrequency = " + fllOutFrequency);
      //      mcg_c2_rangeNode.setValue(mcg_c2_range);
      mcg_c4_drst_drsNode.setValue(mcg_c4_drst_drs);
      mcg_c4_drst_drsNode.enable(fll_enabled);
      fllTargetFrequencyNode.setMessage(fllTargetFrequencyMessage);
   }
}
