package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings

 * Used for:
 *     osc0_rf
 */
public class OscRfValidate extends PeripheralValidator {

   public OscRfValidate(PeripheralWithState peripheral) {
      super(peripheral);
   }

   /**
    * Class to determine oscillator settings
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {

      super.validate(variable);

      // OSC
      //=================================
//      ChoiceVariable   osc_input_freqVar              =  getChoiceVariable("osc_input_freq");
//      Variable         oscillatorRangeVar             =  getVariable("oscillatorRange");
//      LongVariable     oscclk_clockVar                =  getLongVariable("osc_clock");
//      LongVariable     oscerclk_clockVar              =  getLongVariable("oscer_clock");
//
//      oscillatorRangeVar.enable(true);
//      oscillatorRangeVar.clearStatus();
//
//      long    oscclk_clock_freq    = Integer.parseInt(osc_input_freqVar.getSubstitutionValue());
//
//      oscclk_clockVar.setValue(oscclk_clock_freq);
//      oscclk_clockVar.setOrigin("RF Oscillator");
//      oscclk_clockVar.setStatus((Status)null);
//
//      oscerclk_clockVar.setValue(oscclk_clock_freq);
//      oscerclk_clockVar.setOrigin("RF Oscillator");
//
//      oscillatorRangeVar.setValue(2); // High range for 24/32 MHz
//      oscillatorRangeVar.setOrigin("Determined by RF clock Frequency");
//      oscerclk_clockVar.setStatus((Status)null);
      
   }
   
   @Override
   protected boolean createDependencies() throws Exception {
      // Don't add default dependencies
      return false;
   }

}