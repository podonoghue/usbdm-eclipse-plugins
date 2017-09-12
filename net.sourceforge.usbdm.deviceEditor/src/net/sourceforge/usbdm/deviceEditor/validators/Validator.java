package net.sourceforge.usbdm.deviceEditor.validators;

import java.lang.ClassCastException;
import java.lang.Exception;
import java.lang.String;
import java.lang.System;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public abstract class Validator {

   protected final VariableProvider fProvider;
   
   public Validator(VariableProvider provider) {
      fProvider = provider;
   }

   /**
    * Validate settings dialogue
    * 
    * @param variable   Variable trigger change leading to validation (may be null)
    * 
    * @throws Exception
    */
   protected abstract void validate(Variable variable) throws Exception;

   /**
    * Checks is identifier is a valid C name
    * 
    * @param id
    * 
    * @return Valid => null<br>
    *         Invalid => Error string
    */
   String isValidCIdentifier(String id) {
      if (id != null) {
         id = id.replaceAll("%", "");
         if (id.matches("[_a-zA-Z][_a-zA-z0-9]*")) {
            return null;
         }
      }
      return "Illegal name for C identifier";
   }
   
   /**
    * 
    * @return
    */
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
   protected IrqVariable getIrqVariable(String key) throws Exception {
      Variable variable = fProvider.getVariable(fProvider.makeKey(key));
      if (!(variable instanceof IrqVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to IrqVariable");
      }
      return (IrqVariable) variable;
   }

   /**
    * Get String Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found or null
    */
   protected IrqVariable safeGetIrqVariable(String key) {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         return null;
      }
      if (!(variable instanceof IrqVariable)) {
         throw new ClassCastException("Variable " + variable + "cannot be cast to IrqVariable");
      }
      return (IrqVariable) variable;
   }

   /**
    * Get Boolean Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found
    * @throws Exception 
    */
   protected BooleanVariable getBooleanVariable(String key) throws Exception {
      Variable variable = fProvider.getVariable(fProvider.makeKey(key));
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
   protected BooleanVariable safeGetBooleanVariable(String key) {
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
    * @return Variable found
    * @throws Exception 
    */
   protected ChoiceVariable getChoiceVariable(String key) throws Exception {
      Variable variable = fProvider.getVariable(fProvider.makeKey(key));
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
   protected LongVariable getLongVariable(String key) throws Exception {
      Variable variable = fProvider.getVariable(fProvider.makeKey(key));
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
   protected LongVariable safeGetLongVariable(String key) {
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
   protected DoubleVariable getDoubleVariable(String key) throws Exception {
      Variable variable = fProvider.getVariable(fProvider.makeKey(key));
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
    * @throws Exception 
    */
   protected Variable getVariable(String key) throws Exception {
      return fProvider.getVariable(fProvider.makeKey(key));
   }

   /**
    * Get Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return
    */
   protected Variable safeGetVariable(String key) {
      return fProvider.safeGetVariable(fProvider.makeKey(key));
   }

}
