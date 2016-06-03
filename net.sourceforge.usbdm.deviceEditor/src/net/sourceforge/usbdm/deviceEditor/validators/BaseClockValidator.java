package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class BaseClockValidator extends Validator {

   public enum ClockMode {ClockMode_None, ClockMode_FEI, ClockMode_FEE, ClockMode_FBI,
      ClockMode_FBE, ClockMode_PBE,  ClockMode_PEE, ClockMode_BLPI, ClockMode_BLPE}

   protected BaseClockValidator(PeripheralWithState peripheral) {
      super(peripheral);
   }

   private String getSimpleClassName() {
      String s = getClass().toString();
      int index = s.lastIndexOf(".");
      return s.substring(index+1, s.length());
   }
   
//   HashSet<Variable> varModified = new HashSet<Variable>();
   
   /**
    * =============================================================
    */
   private        boolean  busy           = false;
   private        boolean  recursed       = false;
   private  final int      MAX_ITERATION  = 100;

   @Override
   public boolean variableChanged(Variable variable) {
      int iterationCount = 0;
//      if (!varModified.add(variable)) {
//         System.err.println(Integer.toString(iterationCount)+getSimpleClassName()+".variableChanged("+variable+") variable already changed " + variable);
//      }
//      System.err.println(getSimpleClassName()+".variableChanged("+variable+")");
      if (busy) {
         recursed = true;
//         System.err.println(getSimpleClassName()+".variableChanged("+variable+"):Recursed");
//         new Throwable().printStackTrace(System.err);
         return true;
      }
      busy = true;
      do {
         recursed = false;
         validate();
//         System.err.println(getSimpleClassName()+".variableChanged("+variable+") Iterating " + iterationCount);
         if (iterationCount++>MAX_ITERATION) {
            System.err.println(getSimpleClassName()+".variableChanged("+variable+") Iteration limit reached");
            break;
         }
      } while (recursed);
      busy = false;
      return false;
   }
   
   /**
    * Check if a value is within a range
    * 
    * @param value   Value to check
    * @param min     Smallest allowed value
    * @param max     Largest allowed value
    * 
    * @return        true is value in [min,max]
    */
   boolean checkRange(long value, long min, long max) {
      return (value>=min) && (value<=max);
   }

}
