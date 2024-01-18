package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModelInterface;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;

public abstract class Validator implements IModelChangeListener {

   private   final Peripheral       fPeripheral;
   protected final DeviceInfo       fDeviceInfo;

   protected boolean                fVerbose = false;
   
   /**
    * Create validator
    * 
    * @param peripheral  Associated peripheral
    */
   public Validator(Peripheral peripheral) {
      fPeripheral  = peripheral;
      fDeviceInfo = fPeripheral.getDeviceInfo();
   }

   /**
    * Validate settings dialogue
    * 
    * @param variable   Variable trigger change leading to validation (may be null)
    * @param properties
    * 
    * @throws Exception
    */
   protected abstract void validate(Variable variable, int properties) throws Exception;

   /**
    * Checks is identifier is a valid C name
    * 
    * @param id
    * 
    * @return Valid => null<br>
    *         Invalid => Error string
    */
   protected static boolean isValidCIdentifier(String id) {
      
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
    * @param properties
    * 
    * @return true => Updates pending, false => update completed
    */
   public boolean variableChanged(Variable variable, int properties) {
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
            validate(variable, properties);
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
    * Add a variable to associated peripheral
    * 
    * @param variable
    */
   protected void addVariable(Variable variable) {
      fPeripheral.addVariable(variable);
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

      // Make absolute relative to peripheral
      key = fPeripheral.makeKey(key);

      // XXX Remove eventually
      if (getPeripheral().safeGetVariable(key+"[1]") != null) {
         throw new RuntimeException("Use of indexed var '"+key+"' without index");
      }

      return fPeripheral.safeGetVariable(key);
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

   private static class ValidateInfo {
      final public Validator validator;
      final public Variable  variable;
      final public int       properties;
      
      public ValidateInfo(Validator validator, Variable variable, int properties) {
         this.validator  = validator;
         this.variable   = variable;
         this.properties = properties;
      }
   }

   static List<ValidateInfo> validationQueue = null;

   static synchronized List<ValidateInfo> getQueue() {
      if (validationQueue == null) {
         validationQueue = new LinkedList<ValidateInfo>();
      }
      return validationQueue;
   }

   {
      validationQueue = getQueue();
   }
   
   /**
    * Add validator to queue.<br>
    * If the queue is initially empty, then the queue is returned and it is
    * the responsibility of the caller to execute all validators in the queue.<br>
    * This has the effect of serialising validator execution.<br>
    * 
    * @param validator  Validator to add
    * 
    * @param var  Variable triggering validation (may be null)
    * 
    * @return Queue or null
    */
   static synchronized List<ValidateInfo> addValidation(Validator validator, Variable var, int properties) {

      synchronized (validationQueue) {
         if (validationQueue.isEmpty()) {
            validationQueue.add(new ValidateInfo(validator, var, properties));
            return validationQueue;
         }
         else {
            validationQueue.add(new ValidateInfo(validator, var, properties));
            return null;
         }

      }
   }
   
   /**
    * Adds dependencies for validators:<br>
    * 
    * <li> Connect validators to external variable changes
    * <li> Connect validators to signal changes
    */
   public void addDependencies() {

      boolean needSignalDependencies = true;

      try {
         needSignalDependencies = createDependencies();
      } catch (Exception e) {
         e.printStackTrace();
      }
      if (needSignalDependencies) {
         Vector<Signal> table = fPeripheral.getSignalTables().get(0).table;
         for(Signal signal:table) {
            if (signal != null) {
               signal.addListener(this);
            }
         }
      }
   }
   
   /**
    * Add explicit dependencies on variables i.e. connect validators to external variables
    * 
    * @return True  => Indicates default (peripheral) dependencies should be added
    * @return False => All needed dependencies have already been added.
    */
   protected abstract boolean createDependencies() throws Exception;
   
   /**
    * Gets peripheral
    * 
    * @return
    */
   protected Peripheral getPeripheral() {
      return fPeripheral;
   }

   /**
    * Gets device info
    * 
    * @return
    */
   protected DeviceInfo getDeviceInfo() {
      return fDeviceInfo;
   }
   
   @Override
   public void modelElementChanged(ObservableModelInterface observableModel, int properties) {
      
      try {
         Variable var = null;
         if (observableModel instanceof Variable) {
            var = (Variable) observableModel;
         }
         List<ValidateInfo> queue = addValidation(this, var, properties);
         if (queue == null) {
            // Not the only active validator - leave execution to later
//            System.err.println("Deferring validation");
            return;
         }
         // First validator in queue - execute validators in order until empty
         ValidateInfo item;
//         int count=0;
         do {
            item = null;
            synchronized (validationQueue) {
               if (!queue.isEmpty()) {
                  item = queue.get(0);
                  queue.remove(0);
//                  System.err.println("Executing validator("+getSimpleClassName()+":"+count+") this="+fPeripheral.getName()+", p=" + item.validator.fPeripheral.getName() +", v="+item.variable.getName());
                  item.validator.validate(item.variable, item.properties);
               }
            }
//            count++;
         } while (item != null);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
