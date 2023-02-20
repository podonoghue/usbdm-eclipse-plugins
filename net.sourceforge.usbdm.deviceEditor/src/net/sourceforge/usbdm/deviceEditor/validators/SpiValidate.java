package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to validate SPI settings
 */
public class SpiValidate extends PeripheralValidator {

   public SpiValidate(PeripheralWithState peripheral) {
      super(peripheral);
   }

   /**
    * Class to validate SPI settings
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
   }
   
   @Override
   protected boolean createDependencies() throws Exception {
      // Don't add default dependencies
      return false;
   }
}