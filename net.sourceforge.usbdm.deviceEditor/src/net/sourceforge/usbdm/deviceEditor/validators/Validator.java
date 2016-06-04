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

   protected abstract void validate();

   public abstract boolean variableChanged(Variable variable); 

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
