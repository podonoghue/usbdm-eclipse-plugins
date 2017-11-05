package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

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

   public PitValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
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
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      
      // Clocks
      //=================================
      LongVariable     clockVar                   = getLongVariable("/SIM/system_bus_clock");
      LongVariable     pit_ldvalVar               = getLongVariable("pit_ldval");
      DoubleVariable   pit_periodVar              = getDoubleVariable("pit_period");
      DoubleVariable   pit_frequencyVar           = getDoubleVariable("pit_frequency");

      double busFrequency = clockVar.getValueAsDouble();
      long   pit_ldval    = pit_ldvalVar.getValueAsLong();
      
      if (variable != null) {
         if (variable.equals(pit_periodVar)) {
            // Default period ->  ldval, frequency
            //         System.err.println("pit_period");
            double pit_period = pit_periodVar.getValueAsDouble();
            if (pit_period == 0) {
               pit_ldval = 0;
            }
            else {
               pit_ldval = Math.max(0, Math.round((pit_period*busFrequency)-1));
            }
         }
         else if (variable.equals(pit_frequencyVar)) {
            // Default frequency ->  period, ldval
            //         System.err.println("pit_frequency");
            double pit_frequency = pit_frequencyVar.getValueAsDouble();
            if (pit_frequency == 0) {
               pit_ldval = 0;
            }
            else {
               pit_ldval = Math.max(0, Math.round(busFrequency/pit_frequency-1));
            }
         }
      }
      pit_periodVar.setMax((pit_ldvalVar.getMax()+1)/busFrequency);
      pit_ldvalVar.setValue(pit_ldval);
      if (pit_ldval == 0) {
         pit_periodVar.setValue(0);
         pit_frequencyVar.setValue(0);
      }
      else {
         pit_periodVar.setValue((pit_ldval+1)/busFrequency);
         pit_frequencyVar.setValue(busFrequency/(pit_ldval+1));
      }
   }
   
   @Override
   protected void createDependencies() throws Exception {
      final String[] externalVariables = {
            "/SIM/system_bus_clock",
      };
      addToWatchedVariables(externalVariables);
   }
}
