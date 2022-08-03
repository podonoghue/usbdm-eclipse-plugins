package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class BaseClockValidator extends PeripheralValidator {

   public enum McgClockMode {
      McgClockMode_None, 
      McgClockMode_FEI,     McgClockMode_FEE,      McgClockMode_FBI,
      McgClockMode_FBE,     McgClockMode_PBE,      McgClockMode_PEE,       McgClockMode_BLPI, McgClockMode_BLPE, 
      McgClockMode_LIRC_8M, McgClockMode_LIRC_2M,  McgClockMode_HIRC_48M,  McgClockMode_EXT,
      McgClockMode_SOSC,    McgClockMode_SIRC,     McgClockMode_FIRC,      McgClockMode_SPLL }

   protected BaseClockValidator(PeripheralWithState peripheral, int dimension) {
      super(peripheral, dimension);
   }

}
