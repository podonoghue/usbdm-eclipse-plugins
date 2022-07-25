package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.Vector;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public abstract class Validator {

   protected final VariableProvider fProvider;
   protected final int              fDimension;
   protected int                    fIndex=0;
   protected boolean                fVerbose = false;
   
   /**
    * Create validator
    * 
    * @param provider  Associated variable provider
    * @param dimension Dimension of index variables
    */
   public Validator(VariableProvider provider, int dimension) {
      fProvider  = provider;
      fDimension = dimension;
   }

   /**
    * Create validator
    * 
    * @param provider  Associated variable provider
    * @param dimension Dimension of index variables
    */
   public Validator(VariableProvider provider) {
      fProvider  = provider;
      fDimension = 0;
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
   protected boolean isValidCIdentifier(String id) {
      
      return ((id != null) && id.matches("[_a-zA-Z][_a-zA-z0-9]*"));
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
   private        boolean  busy              = false;
   private  final int      MAX_ITERATION     = 100;
   private        Variable recursedVariable  = null;
   
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
         recursedVariable = variable;
//         new Throwable().printStackTrace(System.err);
         return true;
      }
      busy = true;
      do {
         recursedVariable = null;
         try {
            validate(variable);
         } catch (Exception e) {
            e.printStackTrace();
            return false;
         }
//         System.err.println(getSimpleClassName()+".variableChanged("+variable+") Iterating " + iterationCount);
         if (iterationCount++>(MAX_ITERATION-10)) {
            System.err.println(getSimpleClassName()+".variableChanged("+recursedVariable+") Near iteration limit");
         }
         if (iterationCount++>MAX_ITERATION) {
            System.err.println(getSimpleClassName()+".variableChanged("+variable+") Iteration limit reached");
            break;
         }
      } while (recursedVariable != null);
      busy = false;
      return false;
   }

   /**
    * Add a variable to provider
    * 
    * @param variable
    */
   protected void addVariable(Variable variable) {
      fProvider.addVariable(variable);
   }
   
   /**
    * Get Variable from associated peripheral. <br> 
    * Tries to obtain an indexed variable or failing that an unindexed one.
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable or null if not found
    */
   protected Variable safeGetVariable(String key) {
      Variable variable = fProvider.safeGetVariable(fProvider.makeKey(key)+"["+fIndex+"]");
      if (variable == null) {
         variable = fProvider.safeGetVariable(fProvider.makeKey(key));
      }
      return variable;
   }
   
   /**
    * Get Variable from associated peripheral. <br>
    * Tries to obtain an indexed variable or failing that an unindexed one. 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable requested
    * 
    * @throws Exception if variable not found 
    */
   protected Variable getVariable(String key) throws Exception {

      Variable variable = safeGetVariable(key);
      if (variable == null) {
         throw new Exception("Variable not  found '"+ key + "'");
      }
      return variable;
   }
   
   /**
    * Get Irq Variable from associated peripheral <br>
    * Tries to obtain an indexed variable or failing that an unindexed one.  
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found
    * @throws Exception if variable not found 
    */
   protected IrqVariable getIrqVariable(String key) throws Exception {
      Variable variable = getVariable(key);
      if (!(variable instanceof IrqVariable)) {
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to IrqVariable");
      }
      return (IrqVariable) variable;
   }

   /**
    * Get Irq Variable from associated peripheral. <br>
    * Tries to obtain an indexed variable or failing that an unindexed one.  
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
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to IrqVariable");
      }
      return (IrqVariable) variable;
   }

   /**
    * Get Boolean Variable from associated peripheral. <br>
    * Tries to obtain an indexed variable or failing that an unindexed one.   
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found
    * 
    * @throws Exception if variable not found 
    */
   protected BooleanVariable getBooleanVariable(String key) throws Exception {
      Variable variable = getVariable(key);
      if (!(variable instanceof BooleanVariable)) {
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to BooleanVariable");
      }
      return (BooleanVariable) variable;
   }

   /**
    * Get Boolean Variable from associated peripheral. <br>
    * Tries to obtain an indexed variable or failing that an unindexed one.   
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
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to BooleanVariable");
      }
      return (BooleanVariable) variable;
   }

   /**
    * Get Choice Variable from associated peripheral. <br>
    * Tries to obtain an indexed variable or failing that an unindexed one.  
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found
    * 
    * @throws Exception if variable not found 
    */
   protected ChoiceVariable getChoiceVariable(String key) throws Exception {
      Variable variable = getVariable(key);
      if (!(variable instanceof ChoiceVariable)) {
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to ChoiceVariable");
      }
      return (ChoiceVariable) variable;
   }

   /**
    * Get Choice Variable from associated peripheral. <br>
    * Tries to obtain an indexed variable or failing that an unindexed one.  
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found or null
    */
   protected ChoiceVariable safeGetChoiceVariable(String key) {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         return null;
      }
      if (!(variable instanceof ChoiceVariable)) {
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to ChoiceVariable");
      }
      return (ChoiceVariable) variable;
   }

   /**
    * Get Long Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found
    * 
    * @throws Exception if variable not found 
    */
   protected LongVariable getLongVariable(String key) throws Exception {
      Variable variable = getVariable(key);
      if (!(variable instanceof LongVariable)) {
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to LongVariable");
      }
      return (LongVariable) variable;
   }

   /**
    * Get Long Variable from associated peripheral. <br>
    * Tries to obtain an indexed variable or failing that an unindexed one.
    * 
    * @return Variable found or null
    */
   protected LongVariable safeGetLongVariable(String key) {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         return null;
      }
      if (!(variable instanceof LongVariable)) {
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to LongVariable");
      }
      return (LongVariable) variable;
   }

   /**
    * Get String Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return
    * 
    * @return Variable found
    * 
    * @throws Exception if variable not found 
    */
   protected StringVariable getStringVariable(String key) throws Exception {
      Variable variable = getVariable(key);
      if (!(variable instanceof StringVariable)) {
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to StringVariable");
      }
      return (StringVariable) variable;
   }

   /**
    * Get String Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found or null
    */
   protected StringVariable safeGetStringVariable(String key) {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         return null;
      }
      if (!(variable instanceof StringVariable)) {
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to StringVariable");
      }
      return (StringVariable) variable;
   }

   /**
    * Get Boolean Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found
    * 
    * @throws Exception if variable not found 
    */
   protected DoubleVariable getDoubleVariable(String key) throws Exception {
      Variable variable = getVariable(key);
      if (!(variable instanceof DoubleVariable)) {
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to DoubleVariable");
      }
      return (DoubleVariable) variable;
   }

   /**
    * Get Boolean Variable from associated peripheral 
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable found or null
    */
   protected DoubleVariable safeGetDoubleVariable(String key) {
      Variable variable = safeGetVariable(key);
      if (variable == null) {
         return null;
      }
      if (!(variable instanceof DoubleVariable)) {
         throw new ClassCastException("Variable " + variable + "(" + variable.getClass().getSimpleName()+") cannot be cast to DoubleVariable");
      }
      return (DoubleVariable) variable;
   }

   /**
    * Create dependencies between variable providers i.e. connect validators to external variables
    * @throws Exception 
    */
   protected abstract void createDependencies() throws Exception;
   
   class adapter implements IModelChangeListener {

      @Override
      public void modelElementChanged(ObservableModel observableModel) {
         try {
            validate(null);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      @Override
      public void modelStructureChanged(ObservableModel observableModel) {
      }

      @Override
      public void elementStatusChanged(ObservableModel observableModel) {
      }
      
   }
   
   /**
    * Adds dependencies for validators:<br>
    * <li> Connect validators to external variable changes
    * <li> Connect validators to signal changes
    * 
    * @throws Exception 
    */
   public void addDependencies() throws Exception {
      try {
         createDependencies();
      } catch (Exception e) {
         e.printStackTrace();
      }
      if (fProvider instanceof Peripheral) {
       Vector<Signal> table = ((Peripheral)fProvider).getSignalTables().get(0).table;
       for(Signal signal:table) {
          if (signal != null) {
             signal.addListener(new adapter());
          }
       }
      }
   }

}
