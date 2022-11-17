package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class BaseClockValidator extends PeripheralValidator {

   protected BaseClockValidator(PeripheralWithState peripheral, int dimension) {
      super(peripheral, dimension);
   }

}
