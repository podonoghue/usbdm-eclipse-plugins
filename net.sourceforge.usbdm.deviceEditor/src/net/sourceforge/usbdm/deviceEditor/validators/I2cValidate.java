package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to validate I2C settings
 */
public class I2cValidate extends PeripheralValidator {

   public I2cValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to validate I2C settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      
      // Warn if SCL and SDA signals not mapped
      validateMappedPins(new int[]{0,1}, getPeripheral().getSignalTables().get(0).table);
   }
   
   @Override
   protected void createDependencies() throws Exception {
      // No external dependencies
   }
}