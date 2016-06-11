package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class Validator {

   protected final PeripheralWithState fPeripheral;

   protected Validator(PeripheralWithState peripheral) {
      fPeripheral = peripheral;
   }

   protected abstract void validate(Variable variable);

   protected String getSimpleClassName() {
      String s = getClass().toString();
      int index = s.lastIndexOf(".");
      return s.substring(index+1, s.length());
   }
   
   /**
    * =============================================================
    */
   private        boolean  busy           = false;
   private        boolean  recursed       = false;
   private  final int      MAX_ITERATION  = 100;

   /**
    * Default handler for variable changed events
    * 
    * @param variable
    * 
    * @return true => Updates pending, false => update completed
    */
   public boolean variableChanged(Variable variable) {
//      System.err.println(getSimpleClassName()+".variableChanged("+variable+")");
      int iterationCount = 0;
//      if (!varModified.add(variable)) {
//         System.err.println(Integer.toString(iterationCount)+getSimpleClassName()+".variableChanged("+variable+") variable already changed " + variable);
//      }
//      System.err.println(getSimpleClassName()+".variableChanged("+variable+")");
      if (busy) {
//         System.err.println(getSimpleClassName()+".variableChanged("+variable+"):Recursed");
         recursed = true;
//         new Throwable().printStackTrace(System.err);
         return true;
      }
      busy = true;
      do {
         recursed = false;
         validate(variable);
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
    * Get Boolean Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found
    */
   BooleanVariable getBooleanVariable(String key) {
      Variable variable = fPeripheral.getVariable(key);
      if (!(variable instanceof BooleanVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to BooleanVariable");
      }
      return (BooleanVariable) variable;
   }

   /**
    * Get Boolean Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found or null
    */
   BooleanVariable safeGetBooleanVariable(String key) {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         return null;
      }
      if (!(variable instanceof BooleanVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to BooleanVariable");
      }
      return (BooleanVariable) variable;
   }

   /**
    * Get Boolean Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return
    */
   LongVariable getLongVariable(String key) {
      Variable variable = fPeripheral.getVariable(fPeripheral.makeKey(key));
      if (!(variable instanceof LongVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to LongVariable");
      }
      return (LongVariable) variable;
   }

   /**
    * Get Boolean Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return
    */
   LongVariable safeGetLongVariable(String key) {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         return null;
      }
      if (!(variable instanceof LongVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to LongVariable");
      }
      return (LongVariable) variable;
   }

   /**
    * Get Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return
    */
   Variable safeGetVariable(String key) {
      try {
         return getVariable(key);
      } catch (Exception e) {
//         System.err.println(e.getMessage());
      }
      return null;
   }

   /**
    * Get Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return
    */
   Variable getVariable(String key) {
      return fPeripheral.getVariable(fPeripheral.makeKey(key));
   }

   /**
    * Add to watched variables
    * 
    * @param externalVariables Variables to add
    */
   protected void addToWatchedVariables(String[] externalVariables) {
      for (String name:externalVariables) {
         Variable var = safeGetVariable(name);
         if (var == null) {
            System.err.println("Failed to watch variable " + name + " in peripheral " + getClass().getName());
         }
         if (var != null) {
            var.addListener(fPeripheral);
         }
      }
   }
}
