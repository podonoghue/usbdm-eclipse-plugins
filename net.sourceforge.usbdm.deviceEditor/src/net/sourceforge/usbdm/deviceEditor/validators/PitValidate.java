package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings

 * Used for:
 *     pit0
 */
public class PitValidate extends PeripheralValidator {

   public PitValidate(PeripheralWithState peripheral) {
      super(peripheral);
   }

   /**
    * Class to determine PIT settings
    * 
    * Outputs pit_ldval
    * 
    * @throws Exception
    */
   @Override
   public void validate(Variable variable, int properties) throws Exception {
      
      super.validate(variable, properties);
      
      // Clocks
      //=================================
      LongVariable   pitInputClockVar           = getLongVariable("pitInputClock");
      LongVariable   numChannelsVar             = getLongVariable("NumChannels");

      double pitInputClockFrequency = pitInputClockVar.getValueAsDouble();
      int numberOfChannels          = (int) numChannelsVar.getValueAsLong();
      
      for (int ch=0; ch<numberOfChannels; ch++) {
         LongVariable    pit_ldvalVar      = getLongVariable("pit_ldval_tsv["+ch+"]");
         DoubleVariable  pit_periodVar     = getDoubleVariable("pit_period["+ch+"]");

         long pit_ldval = pit_ldvalVar.getValueAsLong();
         
         // These updates involves a loop so suppress initially
         if (getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed) {
            if ((variable != null) && variable.isEnabled()) {
               if (variable.equals(pit_periodVar)) {
                  // period ->  ldval
                  //         System.err.println("pit_period");
                  double pit_period = pit_periodVar.getValueAsDouble();
                  if (pit_period == 0) {
                     pit_ldval = 0;
                  }
                  else {
                     pit_ldval = Math.max(0, Math.round((pit_period*pitInputClockFrequency)-1));
                  }
               }
            }
         }
         if (pit_ldvalVar.isEnabled()) {
            pit_periodVar.setMax((pit_ldvalVar.getMax()+1)/pitInputClockFrequency);
            pit_ldvalVar.setValue(pit_ldval);
            if (pit_ldval == 0) {
               pit_periodVar.setValue(0);
            }
            else {
               pit_periodVar.setValue((pit_ldval+1)/pitInputClockFrequency);
            }
         }
      }
   }
   
   @Override
   protected boolean createDependencies() throws Exception {
 
      // Variable to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

      variablesToWatch.add("pitInputClock");
      LongVariable   numChannelsVar = getLongVariable("NumChannels");
      for (int num=(int)numChannelsVar.getValueAsLong(); num>=0; num--) {
         variablesToWatch.add("pit_ldval_tsv["+num+"]");
         variablesToWatch.add("pit_period["+num+"]");
      }

      addSpecificWatchedVariables(variablesToWatch);

      // Don't add default dependencies
      return false;
   }
}
