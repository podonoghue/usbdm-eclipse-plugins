package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class Validator {

   protected final PeripheralWithState fPeripheral;

   protected Validator(PeripheralWithState peripheral) {
      fPeripheral = peripheral;
   }

   protected abstract void validate();
   
   public abstract void variableChanged(Variable variable); 

}
