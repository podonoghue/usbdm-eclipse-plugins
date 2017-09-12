package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class BaseClockValidator extends PeripheralValidator {

   public enum ClockMode {
      ClockMode_None, 
      ClockMode_FEI, ClockMode_FEE, ClockMode_FBI,
      ClockMode_FBE, ClockMode_PBE,  ClockMode_PEE, ClockMode_BLPI, ClockMode_BLPE, 
      ClockMode_LIRC_8M, ClockMode_LIRC_2M, ClockMode_HIRC_48M, ClockMode_EXT,}

   protected BaseClockValidator(PeripheralWithState peripheral) {
      super(peripheral);
   }

}
