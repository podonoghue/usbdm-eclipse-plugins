package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings
 
 * Used for:
 *     lpuart
 */
public class LpuartValidate extends Validator {
   
   public LpuartValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine LPUART settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      Variable     clockSource = getVariable("clockSource");
      clockSource.setStatus((Message)null);
      clockSource.setOrigin("");
      clockSource.enable(false);
   }
}