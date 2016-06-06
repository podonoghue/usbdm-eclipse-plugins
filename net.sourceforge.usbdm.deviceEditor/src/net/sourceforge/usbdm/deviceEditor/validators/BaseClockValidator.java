package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class BaseClockValidator extends Validator {

   public enum ClockMode {ClockMode_None, ClockMode_FEI, ClockMode_FEE, ClockMode_FBI,
      ClockMode_FBE, ClockMode_PBE,  ClockMode_PEE, ClockMode_BLPI, ClockMode_BLPE}

   protected BaseClockValidator(PeripheralWithState peripheral) {
      super(peripheral);
   }

}
