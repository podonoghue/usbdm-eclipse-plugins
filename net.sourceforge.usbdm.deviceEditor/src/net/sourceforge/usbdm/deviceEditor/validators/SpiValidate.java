package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to validate SPI settings
 */
public class SpiValidate extends PeripheralValidator {

   public SpiValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to validate SPI settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      
      // Warn if MISO, MOSI and SCK signals not mapped
      validateMappedPins(new int[]{0,1,2}, getPeripheral().getSignalTables().get(0).table);
   }
   
   @Override
   protected void createDependencies() throws Exception {
      // No external dependencies
   }
}