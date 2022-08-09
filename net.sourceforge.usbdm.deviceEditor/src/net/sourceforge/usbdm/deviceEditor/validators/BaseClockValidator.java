package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class BaseClockValidator extends PeripheralValidator {

   public enum McgClockMode {
      McgClockMode_FEI,       McgClockMode_FEE,       McgClockMode_FBI,
      McgClockMode_FBE,       McgClockMode_PBE,       McgClockMode_PEE,         McgClockMode_BLPI, McgClockMode_BLPE, 
      McgClockMode_LIRC_8MHz, McgClockMode_LIRC_2MHz, McgClockMode_HIRC_48MHz,  McgClockMode_EXT,
      McgClockMode_SOSC,      McgClockMode_SIRC,      McgClockMode_FIRC,        McgClockMode_SPLL }

   protected BaseClockValidator(PeripheralWithState peripheral, int dimension) {
      super(peripheral, dimension);
   }

}
