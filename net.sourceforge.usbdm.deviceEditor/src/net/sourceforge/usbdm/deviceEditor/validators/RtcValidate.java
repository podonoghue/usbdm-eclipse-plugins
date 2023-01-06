package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings
 *
 * Used for:
 *     osc0
 *     osc0_div
 */
/**
 * @author peter
 *
 */
public class RtcValidate extends PeripheralValidator {

   /**
    * @param peripheral Associated peripheral
    * @param values     Not used
    */
   public RtcValidate(PeripheralWithState peripheral) {
      super(peripheral);
   }

   /**
    * Class to determine RTC oscillator settings
    * 
    * Outputs rtcclk_clock, rtcclk_gated_clock,
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {
   }
   
   @Override
   protected boolean createDependencies() throws Exception {
      super.createDependencies();
      return false;
   }
}
