package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class BaseClockValidator extends Validator {

   public enum ClockMode {ClockMode_None, ClockMode_FEI, ClockMode_FEE, ClockMode_FBI, ClockMode_FBE, ClockMode_PBE,  ClockMode_PEE, ClockMode_BLPI, ClockMode_BLPE}

   private static final int MAX_ITERATION = 100;

   protected BaseClockValidator(PeripheralWithState peripheral) {
      super(peripheral);
   }

   static boolean busy      = false;
   static boolean recursed  = false;
   
   @Override
   public void variableChanged(Variable variable) {
      
      if (busy) {
         recursed = true;
         return;
      }
      busy = true;
      int iterationCount = 0;
      do {
         recursed = false;
         validate();
         if (iterationCount++>MAX_ITERATION) {
            System.err.println("BaseClockValidator() Iteration limit reached");
            break;
         }
      } while (recursed);
      busy = false;
   }

}
