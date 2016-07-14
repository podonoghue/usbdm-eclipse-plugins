package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings

 * Used for:
 *     osc0
 *     osc0_div
 */
public class PitValidate extends Validator {

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

      // OSC
      //=================================
      LongVariable     pit_ldvalVar               = getLongVariable("pit_ldval");
      DoubleVariable   pit_periodVar              = getDoubleVariable("pit_period");
      DoubleVariable   pit_frequencyVar           = getDoubleVariable("pit_frequency");
      LongVariable     system_bus_clockVar        = getLongVariable("/SIM/system_bus_clock");

      long busFrequency = system_bus_clockVar.getValueAsLong();
      long pit_ldval    = pit_ldvalVar.getValueAsLong();
      
      if ((variable != null) && variable.equals(pit_periodVar)) {
         // Default period ->  ldval, frequency
         System.err.println("pit_period");
         double pit_period = pit_periodVar.getValueAsDouble();
         if (pit_period == 0) {
            pit_ldval = 0;
         }
         else {
            pit_ldval = Math.round(pit_period*busFrequency);
         }
      }
      else if ((variable != null) && variable.equals(pit_frequencyVar)) {
         // Default frequency ->  period, ldval
         System.err.println("pit_frequency");
         double pit_frequency = pit_frequencyVar.getValueAsDouble();
         if (pit_frequency == 0) {
            pit_ldval = 0;
         }
         else {
            pit_ldval = Math.round(busFrequency/pit_frequency);
         }
      }
      pit_ldvalVar.setValue(pit_ldval);
      if (pit_ldval == 0) {
         pit_periodVar.setValue(0);
         pit_frequencyVar.setValue(0);
      }
      else {
         pit_periodVar.setValue((double)pit_ldval/busFrequency);
         pit_frequencyVar.setValue(busFrequency/(double)pit_ldval);
      }
   }
}
