package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class Validator {

   protected final PeripheralWithState fPeripheral;

   /**
    * Base validator
    * 
    * @param peripheral
    * @param values
    */
   public Validator(PeripheralWithState peripheral, ArrayList<Object> values) {
      fPeripheral = peripheral;
   }

   /**
    * Constructor used by derived classes
    * 
    * @param peripheral
    */
   protected Validator(PeripheralWithState peripheral) {
      fPeripheral = peripheral;
   }

   /**
    * Validate peripheral settings dialogue
    * 
    * @param variable   Variable trigger change leading to validation (may be null)
    * 
    * @throws Exception
    */
   protected void validate(Variable variable) throws Exception {
      validateInterrupt(variable);
   }

   /**
    * Checks is identifier is a valid C name
    * 
    * @param id
    * 
    * @return Valid => null<br>
    *         Invalid => Error string
    */
   String checkValidCIdentifier(String id) {
      if ((id == null) || id.isEmpty()) {
         return "Illegal name for C identifier";
      }
      return null;
   }
   /**
    * Validates the interrupt portion of the dialogue
    * 
    * @param variable   Variable trigger change leading to validation (may be null)
    * 
    * @throws Exception
    */
   protected void validateInterrupt(Variable variable) throws Exception {
      
      ChoiceVariable irqHandlingMethodVar         = safeGetChoiceVariable("irqHandlingMethod");
      StringVariable namedInterruptHandlerVar     = safeGetStringVariable("namedInterruptHandler");
      LongVariable   irqLevelVar                  = safeGetLongVariable("irqLevel");

     if (irqHandlingMethodVar == null) {
        return;
     }
     
     switch((int)irqHandlingMethodVar.getValueAsLong()) {
     default:
        irqHandlingMethodVar.setValue(0);
     case 0: // No handler
        namedInterruptHandlerVar.enable(false);
        namedInterruptHandlerVar.setOrigin("Disabled by irqHandlingMethod");
        irqLevelVar.enable(false);
        irqLevelVar.setOrigin("Disabled by irqHandlingMethod");
        break;
     case 1: // Software (Use setCallback() or class method)
        namedInterruptHandlerVar.enable(false);
        namedInterruptHandlerVar.setOrigin("Disabled by irqHandlingMethod");
        irqLevelVar.enable(true);
        irqLevelVar.setOrigin(null);
        break;
     case 2: // Named function
        namedInterruptHandlerVar.enable(true);
        namedInterruptHandlerVar.setOrigin(null);
        namedInterruptHandlerVar.setStatus(checkValidCIdentifier(namedInterruptHandlerVar.getValueAsString()));
        irqLevelVar.enable(true);
        irqLevelVar.setOrigin(null);
        break;
    }
   }
   
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
         try {
            validate(variable);
         } catch (Exception e) {
            e.printStackTrace();
            return false;
         }
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
    * @throws Exception 
    */
   BooleanVariable getBooleanVariable(String key) throws Exception {
      Variable variable = fPeripheral.getVariable(fPeripheral.makeKey(key));
      if (!(variable instanceof BooleanVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to BooleanVariable");
      }
      return (BooleanVariable) variable;
   }

   /**
    * Get String Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found or null
    */
   StringVariable safeGetStringVariable(String key) {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         return null;
      }
      if (!(variable instanceof StringVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to StringVariable");
      }
      return (StringVariable) variable;
   }

   /**
    * Get Choice Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found or null
    */
   ChoiceVariable safeGetChoiceVariable(String key) {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         return null;
      }
      if (!(variable instanceof ChoiceVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to ChoiceVariable");
      }
      return (ChoiceVariable) variable;
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
    * @return Variable found
    * @throws Exception 
    */
   ChoiceVariable getChoiceVariable(String key) throws Exception {
      Variable variable = fPeripheral.getVariable(fPeripheral.makeKey(key));
      if (!(variable instanceof ChoiceVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to BooleanVariable");
      }
      return (ChoiceVariable) variable;
   }

   /**
    * Get Boolean Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found or null
    */
   ChoiceVariable safeGetChoiceVariableVariable(String key) {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         return null;
      }
      if (!(variable instanceof ChoiceVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to BooleanVariable");
      }
      return (ChoiceVariable) variable;
   }

   /**
    * Get Boolean Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return
    * @throws Exception 
    */
   LongVariable getLongVariable(String key) throws Exception {
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
    * Get Boolean Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return
    * @throws Exception 
    */
   DoubleVariable getDoubleVariable(String key) throws Exception {
      Variable variable = fPeripheral.getVariable(fPeripheral.makeKey(key));
      if (!(variable instanceof DoubleVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to DoubleVariable");
      }
      return (DoubleVariable) variable;
   }

   /**
    * Get Boolean Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return
    */
   DoubleVariable safeGetDoubleVariable(String key) {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         return null;
      }
      if (!(variable instanceof DoubleVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to DoubleVariable");
      }
      return (DoubleVariable) variable;
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
    * @throws Exception 
    */
   Variable getVariable(String key) throws Exception {
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
         else {
            var.addListener(fPeripheral);
         }
      }
   }
}
