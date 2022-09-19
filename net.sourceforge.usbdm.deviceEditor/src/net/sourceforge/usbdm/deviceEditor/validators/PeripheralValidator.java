package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.HashMap;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ClockSelectionVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.ChoiceData;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.parsers.SimpleExpressionParser;
import net.sourceforge.usbdm.deviceEditor.parsers.SimpleExpressionParser.Mode;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class PeripheralValidator extends Validator {

   protected static final Status UNMAPPED_PIN_STATUS = new Status("Not all common signals are mapped to pins", Severity.WARNING);

   /**
    * Peripheral dialogue validator <br>
    * Constructor used by derived classes
    * 
    * @param peripheral
    */
   public PeripheralValidator(PeripheralWithState peripheral, int index) {
      super(peripheral, index);
   }

   /**
    * Peripheral dialogue validator <br>
    * Constructor used by derived classes
    * 
    * @param peripheral
    */
   public PeripheralValidator(PeripheralWithState peripheral, ArrayList<Object> list) {
      super(peripheral);
   }

   /**
    * Peripheral dialogue validator <br>
    * Constructor used by derived classes
    * 
    * @param peripheral
    */
   public PeripheralValidator(PeripheralWithState peripheral) {
      super(peripheral);
   }

   /**
    * Gets peripheral
    * 
    * @return
    */
   @Override
   public PeripheralWithState getPeripheral() {
      return (PeripheralWithState) super.getPeripheral();
   }

   /**
    * Validate peripheral settings dialogue
    * 
    * @param variable   Variable trigger change leading to validation (may be null)
    * 
    * @throws Exception
    */
   @Override
   protected void validate(Variable variable) throws Exception {
      if (variable == null) {
         validateAllClockSelectors();
      }
   }

   private void validateAllClockSelectors() throws Exception {
//      ArrayList<Variable> monitoredVariables = getPeripheral().getMonitoredVariables();
//      if (monitoredVariables == null) {
//         return;
//      }
//      for (Variable monitoredVariable:monitoredVariables) {
//         monitoredVariable.???
//      }
   }

   boolean isClockDependent(String key) {
      return key.endsWith("[]");
   }

   /**
    * Converts a variable key into a clock specific key e.g. varkey[] => varKey[clockIndex]
    * 
    * @param key        Key to convert
    * @param clockIndex Clock index to insert if needed
    * 
    * @return  Converted key or original if not indexed key
    */
   String makeClockSpecificName(String key, int clockIndex) {
      if (isClockDependent(key)) {
         key = key.substring(0,key.length()-2)+"["+clockIndex+"]";
      }
      return key;
   }

   /**
    * Converts a variable key into a clock specific key e.g. varkey[] => varKey[activeClockSelection]
    * 
    * @param key        Key to convert
    * @param clockIndex Clock index to insert as needed
    * 
    * @return  Converted key or original if not indexed key
    */
   String makeClockSpecificName(String key) {
      return makeClockSpecificName(key, getDeviceInfo().getActiveClockSelection());
   }
   
   /**
    * 
    * @param controlVar       Variable controlling outcome
    * @param targetVariable   Target being controlled
    * @param reference        Reference string either "expression" or "primaryVar;expression" or "disabled"
    * 
    * @throws Exception
    */
   void updateTarget(Variable controlVar, Variable targetVariable, String reference) throws Exception {

      if ("disabled".equalsIgnoreCase(reference)) {
         if (targetVariable != null) {
            targetVariable.setValue(0);
            targetVariable.setStatus((Status)null);
            targetVariable.setOrigin("Disabled by "+controlVar.getName());
            targetVariable.enable(false);
         }
         return;
      }
      String data[]      = reference.split(";");
      String expression = data[data.length-1];
      SimpleExpressionParser parser = new SimpleExpressionParser(getPeripheral(), Mode.EvaluateFully);
      Object result = parser.evaluate(expression);
      ArrayList<String>  identifiers = parser.getCollectedIdentifiers();

      String primaryClockSourceName = null;
      String origin = "";
      Status status = null;
      boolean enabled = true;
      if (data.length>1) {
         primaryClockSourceName = data[0].split(",")[0];
      }
      else {
         if (identifiers.size()>0) {
            primaryClockSourceName = identifiers.get(0);
         } else {
            origin = "[Fixed]";
         }
      }
      if (primaryClockSourceName != null) {
         Variable primaryClockSourceVar = safeGetVariable(primaryClockSourceName);
         status   = primaryClockSourceVar.getFilteredStatus();
         enabled  = primaryClockSourceVar.isEnabled();
         origin   = primaryClockSourceVar.getOrigin();
      }
      if (controlVar != targetVariable) {
         if (controlVar instanceof VariableWithChoices) {
            origin = origin + "\n[selected by " + controlVar.getName() +"]";
         }
         else {
            origin = origin + "\n[modified by " + controlVar.getName() +"]";
         }
      }
      if (targetVariable != null) {
         enabled = enabled && targetVariable.evaluateEnable(getPeripheral());
         targetVariable.setValue(result);
         targetVariable.setOrigin(origin);
         targetVariable.setStatus(status);
         targetVariable.enable(enabled);
      }
      if (controlVar instanceof ClockSelectionVariable) {
         ClockSelectionVariable cv = (ClockSelectionVariable) controlVar;
         cv.setDisplayValue(result.toString());
      }
      return;
   }

   protected class StringListener implements IModelChangeListener {

      final StringVariable fStringVariable;

      public StringListener(StringVariable stringVariable) {
         fStringVariable = stringVariable;
      }

      @Override
      public void modelStructureChanged(ObservableModel observableModel) {
      }

      @Override
      public void modelElementChanged(ObservableModel observableModel) {
         try {
            /**
             * Validate a string link variable
             * 
             * The variable value is of form "var1,var2;expression"
             * The target=... specifies the target
             */
            // String variable with value form "varName[,expression]"
            String reference = fStringVariable.getValueAsString();
            
            // Variable being controlled
            Variable targetVariable = safeGetVariable(fStringVariable.getTarget());
            
            updateTarget(fStringVariable, targetVariable, reference);
                     
         } catch (Exception e) {
            System.err.println("Failed to validate "+fStringVariable);
            e.printStackTrace();
         }
      }

      @Override
      public void elementStatusChanged(ObservableModel observableModel) {
      }
   };

   protected class ClockSelectorListener implements IModelChangeListener {

      final VariableWithChoices fClockSelector;

      public ClockSelectorListener(VariableWithChoices clockSelector) {
         fClockSelector = clockSelector;
      }

      @Override
      public void modelStructureChanged(ObservableModel observableModel) {
      }

      @Override
      public void modelElementChanged(ObservableModel observableModel) {
         try {
            /**
             * Validate a clock selector variable
             * 
             * target=... specifies the target variable
             * Each choice has a ref=... to indicate the source reference or equation for that selection
             */
            // Get selected reference (source)
            ChoiceData           choiceData     = fClockSelector.getSelectedItemData();
            
            // Selected source or equation
            String               reference      = choiceData.getReference();

            // Target variable being controlled
            Variable             targetVariable = safeGetVariable(fClockSelector.getTarget());
            
            updateTarget(fClockSelector, targetVariable, reference);
            
         } catch (Exception e) {
            System.err.println("Failed to validate "+fClockSelector);
            e.printStackTrace();
         }
      }

      @Override
      public void elementStatusChanged(ObservableModel observableModel) {
      }
   };

   protected class DependentVariableListener implements IModelChangeListener {

      final Variable fDependentVar;

      public DependentVariableListener(Variable dependentVar) {
         fDependentVar = dependentVar;
      }

      @Override
      public void modelStructureChanged(ObservableModel observableModel) {
      }

      @Override
      public void modelElementChanged(ObservableModel observableModel) {
         try {
            /**
             * Validate a variable referencing another variable
             * 
             * reference=... is the controlling source
             * The variable itself the target
             */
            // Reference of form "[varName];expression"
            String reference = fDependentVar.getReference();
            
            // Variable being controlled is itself
            updateTarget(fDependentVar, fDependentVar, reference);
                     
         } catch (Exception e) {
            System.err.println("Failed to validate "+fDependentVar);
            e.printStackTrace();
         }
      }

      @Override
      public void elementStatusChanged(ObservableModel observableModel) {
      }
   };

   protected class EnabledByListener implements IModelChangeListener {

      final Variable fEnabledByVar;

      /**
       * The variable that is enabled by the expression
       * 
       * @param enabledByVar
       */
      public EnabledByListener(Variable enabledByVar) {
         fEnabledByVar = enabledByVar;
      }

      @Override
      public void modelStructureChanged(ObservableModel observableModel) {
      }

      @Override
      public void modelElementChanged(ObservableModel observableModel) {
         try {
            /**
             * Validate a variable referencing another variable
             * 
             * reference=... is the controlling source
             * The variable itself the target
             */
            // Reference of form "[varName];expression"
            String enabledByExpression = fEnabledByVar.getEnabledBy();
            
            SimpleExpressionParser parser = new SimpleExpressionParser(getPeripheral(), Mode.EvaluateFully);
            Boolean result = (Boolean)parser.evaluate(enabledByExpression);
            fEnabledByVar.enable(result);
         } catch (Exception e) {
            System.err.println("Failed to validate "+fEnabledByVar);
            e.printStackTrace();
         }
      }

      @Override
      public void elementStatusChanged(ObservableModel observableModel) {
      }
   };

   /**
    * Add listeners to all variable in expression and clock as needed
    * 
    * @param referenceString     String of form "expression" or "primaryVar;expression"
    * @param listener            Listener to add to variables
    * @param owningVar           The variable owning the reference (for debug messages)
    * 
    * @throws Exception
    */
   void addExpressionListeners(String referenceString, IModelChangeListener listener, Variable owningVar) throws Exception {
      
      if ("disabled".equalsIgnoreCase(referenceString)) {
         return;
      }
      /*
       * Need to make sensitive to:
       *  - Each referenced variable in expression
       *  - Clock selection if any referenced variable is dependent on active clock selection
       */
      int numberOfClockSettings = (int)getLongVariable("/SIM/numberOfClockSettings").getValueAsLong();
      Variable clockSelectorVar = getVariable("/MCG/currentClockConfig");

      String parts[] = referenceString.split(";");
      // The right-most entry is the expression (may be by itself)
      String expression = parts[parts.length-1];
      SimpleExpressionParser parser = new SimpleExpressionParser(getPeripheral(), Mode.CollectIdentifiers);
      parser.evaluate(expression);
      ArrayList<String> identifiers = parser.getCollectedIdentifiers();
      if (parser.isClockDependent()) {
         // Make sensitive to clock selector
         clockSelectorVar.addListener(listener);
      }
      for (String refName:identifiers) {
         if (isClockDependent(refName)) {
            // Make sensitive to all clock variations of reference
            for (int clockSel=0; clockSel<numberOfClockSettings; clockSel++) {
               Variable referenceVar = safeGetVariable(makeClockSpecificName(refName, clockSel));
               if (referenceVar == null) {
                  throw new Exception("Clock reference variable '"+refName+"' not found for Clock selector var '"+owningVar+"'");
               }
               // Watch references
               referenceVar.addListener(listener);
            }
         }
         else {
            Variable reference = safeGetVariable(refName);
            if (reference == null) {
               throw new Exception("Clock reference variable '"+refName+"' not found for Clock selector var '"+owningVar+"'");
            }
            // Watch references
            reference.addListener(listener);
         }
      }
   }
   
   void addStringListener(StringVariable monitoredVariable, HashMap<String,Object> listeners) throws Exception {

      String targetName = monitoredVariable.getTarget();
      Variable target   = safeGetVariable(targetName);
      if (target == null) {
         throw new Exception("Target '"+targetName+"' not found for String var '"+monitoredVariable);
      }

      // Get/create listener to link references to target
      // Make sure only one is created for each monitored variable (in case it is in an iterated loop)
      // Note that if the variable is actually iterated then it will be a unique variable
      
      String referenceString = monitoredVariable.getValueAsString();
      String duplicateKey = monitoredVariable.getName()+referenceString;
      StringListener listener = (StringListener) listeners.get(duplicateKey);
      if (listener == null) {
         listener = new StringListener(monitoredVariable);
         listeners.put(duplicateKey, listener);
      }
      // Watch clock selector
      monitoredVariable.addListener(listener);

      if ((referenceString == null) || (referenceString.isBlank())) {
         throw new Exception("Clock reference is missing for Clock selector var '"+monitoredVariable+"'");
      }
      if ("disabled".equalsIgnoreCase(referenceString)) {
         return;
      }
      Variable reference = safeGetVariable(referenceString);
      if (reference == null) {
         throw new Exception("Clock reference variable '"+referenceString+"' not found for Clock selector var '"+monitoredVariable+"'");
      }
      // Watch references
      reference.addListener(listener);
   }
   
   /**
    * This handles a variable that depends other variables
    * This variable will have a reference="expression"
    *
    * @param dependentVariable Variable that is dependent (has reference)
    * 
    * @throws Exception
    */
   void addDependentVariableListener(Variable dependentVariable) throws Exception {
      
      // Get/create listener to link references to target
      // No need to check for duplicates as this would require multiple of the same dependent variable
      String referenceString = dependentVariable.getReference();
      DependentVariableListener listener = new DependentVariableListener(dependentVariable);
      if ((referenceString == null) || (referenceString.isBlank())) {
         throw new Exception("Reference is missing for dependent var '"+dependentVariable+"'");
      }
      
      addExpressionListeners(referenceString, listener, dependentVariable);
   }
   
   /**
    * This handles a ChoiceVariable with target="controllerVar" and choices having ref="expression"
    * 
    * @param choiceVariable   Variable controlling a target
    * @param listeners        List of listeners to avoid duplication
    * 
    * @throws Exception
    */
   void addClockSelectorListener(VariableWithChoices choiceVariable, HashMap<String,Object> listeners) throws Exception {

      String targetName = choiceVariable.getTarget();
      if (targetName.isBlank() && (choiceVariable instanceof ClockSelectionVariable)) {
         // Allow to have no target for self updating clock selection
      }
      else {
         // Sanity check target
         Variable target  = safeGetVariable(targetName);
         if (target == null) {
            throw new Exception("Target '"+targetName+"' not found for Clock selector var '"+choiceVariable+"' have target");
         }
      }
      // Get/create listener to link references to target
      // Make sure only one is created for each monitored variable (in case it is in an iterated loop)
      // Note that if the variable is actually iterated then it will be a unique variable
      String duplicateKey = choiceVariable.getName()+targetName;
      ClockSelectorListener listener = (ClockSelectorListener) listeners.get(duplicateKey);
      if (listener == null) {
         listener = new ClockSelectorListener(choiceVariable);
         listeners.put(duplicateKey, listener);
      }
      /* Need to make sensitive to:
       *  - Each referenced source in choices
       *  - Selector itself
       *  - Clock choice if any reference is dependent on active clock selection
       */

      // Watch clock selector
      choiceVariable.addListener(listener);

      // ChoiceVar selecting the clock input
      ChoiceData[] choiceDatas = choiceVariable.getData();
      for (ChoiceData choiceData:choiceDatas) {
         String referenceString = choiceData.getReference();
         if ((referenceString == null) || (referenceString.isBlank())) {
            throw new Exception("Clock reference is missing for Clock selector var '"+choiceVariable+"'");
         }
         addExpressionListeners(referenceString, listener, choiceVariable);
      }
   }
   
   /**
    * This handles a variable that enabled by other variables
    * This variable will have a enabledBy="expression"
    *
    * @param enabledVariable Variable that is dependent (has enabledBy)
    * 
    * @throws Exception
    */
   private void addEnabledByListener(Variable enabledVariable, HashMap<String, Object> addedListeners) throws Exception {
      
      // Get/create listener to link references to target
      // No need to check for duplicates as this would require multiple of the same dependent variable
      String enabledByExpression = enabledVariable.getEnabledBy();
      EnabledByListener listener = new EnabledByListener(enabledVariable);
      addExpressionListeners(enabledByExpression, listener, enabledVariable);
   }

   /**
    * Add clock selectors and their references to watched variables<br>
    * The clock selectors are obtained from the associated peripheral
    * 
    * @throws Exception
    */
   private void addMonitoredVariableListeners() throws Exception {
      ArrayList<Variable> monitoredVariables = getPeripheral().getMonitoredVariables();
      if (monitoredVariables == null) {
         return;
      }
      
      // List to prevent repeated listeners
      HashMap<String,Object> addedListeners  = new HashMap<String,Object>();
      
      for (Variable monitoredVariable:monitoredVariables) {
         boolean actionFound = false;
         if (monitoredVariable.getReference() != null) {
            // This variable depends on another variable
            addDependentVariableListener(monitoredVariable);
            actionFound = true;
         }
         if (monitoredVariable.getTarget() != null) {
            actionFound = true;
            // This variable controllers another variable
            if (monitoredVariable instanceof StringVariable) {
               addStringListener((StringVariable) monitoredVariable, addedListeners);
            }
            else if (monitoredVariable instanceof VariableWithChoices) {
               addClockSelectorListener((VariableWithChoices) monitoredVariable, addedListeners);
            }
            else {
               throw new Exception("Monitored var '"+monitoredVariable+"' is of unsupported type");
            }
         }
         if (monitoredVariable.getEnabledBy() != null) {
            actionFound = true;
            addEnabledByListener(monitoredVariable, addedListeners);
         }
         
         if (!actionFound) {
            throw new Exception("Monitored var '"+monitoredVariable+"' doesn't have target or reference");
         }
      }
   }

   /**
    * Add to watched variables
    * 
    * @param externalVariables Variables to add
    */
   protected void addToWatchedVariables(String name) {
      Variable var = safeGetVariable(name);
      if (var == null) {
         if (fIndex==0) {
            if (fVerbose) {
               System.err.println("Failed to watch variable " + name + " in peripheral " + getClass().getName());
            }
         }
      }
      else {
         var.addListener(getPeripheral());
      }
   }

   /**
    * Add to watched variables
    * 
    * @param variablesToWatch Variables to add
    */
   protected void addToWatchedVariables(String[] variablesToWatch) {
      if (variablesToWatch == null) {
         return;
      }
      for(fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
         for (String name:variablesToWatch) {
            addToWatchedVariables(name);
         }
      }
      fIndex = 0;
   }

   /**
    * Add to watched variables
    * 
    * @param externalVariables Variables to add
    */
   protected void addToWatchedVariables(ArrayList<String> variablesToWatch) {
      if (variablesToWatch == null) {
         return;
      }
      for(fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
         for (String name:variablesToWatch) {
            addToWatchedVariables(name);
         }
      }
      fIndex = 0;
   }

   /**
    * Create reference to a target variable by obtaining its name from given variable <br>
    * The name of the target variable will be added to namesToWatch
    * 
    * @param indirectName  Name of StringVariable containing name of target LongVariable
    * @param namesToWatch  List of names of variables to be watched (may be null)
    * 
    * @return Target variable or null if indirectName variable doesn't exist
    * 
    * @throws Exception if target variable doesn't exist
    */
   protected LongVariable createLongVariableIndirectReference(String indirectName, ArrayList<String> namesToWatch) throws Exception {

      LongVariable reference = null;
      StringVariable nameVar = safeGetStringVariable(indirectName);
      if (nameVar != null) {
         String targetName = nameVar.getValueAsString();
         if (namesToWatch != null) {
            namesToWatch.add(targetName);
         }
         reference = getLongVariable(targetName);
      }
      return reference;
   }

   /**
    * Create reference to a target by name <br>
    * The name of the target variable will be added to namesToWatch
    * 
    * @param targetName    Name of Variable
    * @param namesToWatch  List of names of variables to be watched
    * 
    * @return Target variable
    */
   protected LongVariable safeCreateLongVariableReference(String targetName, ArrayList<String> namesToWatch) {

      LongVariable reference = safeGetLongVariable(targetName);
      if ((reference != null) && (namesToWatch != null)) {
         namesToWatch.add(targetName);
      }
      return reference;
   }

   /**
    * Create reference to a target by name <br>
    * The name of the target variable will be added to namesToWatch
    * 
    * @param targetName    Name of Variable
    * @param namesToWatch  List of names of variables to be watched
    * 
    * @return Target variable
    * 
    * @throws Exception if target variable doesn't exist
    */
   protected LongVariable createLongVariableReference(String targetName, ArrayList<String> namesToWatch) throws Exception {

      LongVariable reference = getLongVariable(targetName);
      namesToWatch.add(targetName);
      return reference;
   }

   /**
    * Create reference to a target by name <br>
    * The name of the target variable will be added to namesToWatch
    * 
    * @param targetName    Name of Variable
    * @param namesToWatch  List of names of variables to be watched
    * 
    * @return Target variable
    */
   protected BooleanVariable safeCreateBooleanVariableReference(String targetName, ArrayList<String> namesToWatch) {

      BooleanVariable reference = safeGetBooleanVariable(targetName);
      if ((reference != null) && (namesToWatch != null)) {
         namesToWatch.add(targetName);
      }
      return reference;
   }

   /**
    * Create reference to a target by name <br>
    * The name of the target variable will be added to namesToWatch
    * 
    * @param targetName    Name of Variable
    * @param namesToWatch  List of names of variables to be watched
    * 
    * @return Target variable
    * 
    * @throws Exception if target variable doesn't exist
    */
   protected BooleanVariable createBooleanVariableReference(String targetName, ArrayList<String> namesToWatch) throws Exception {

      BooleanVariable reference = getBooleanVariable(targetName);
      namesToWatch.add(targetName);
      return reference;
   }

   @Override
   protected void createDependencies() throws Exception {
      addToWatchedVariables(getPeripheral().getDependencies());
      addMonitoredVariableListeners();
   }

   /**
    * Get parameter from peripheral configuration files
    * 
    * @param name          Name of parameter e.g. "/SIM/pdb_input_clock"
    * @param defaultValue  Value to return if parameter is not found e.g. "/SIM/system_bus_clock"
    * 
    * @return Parameter value or default value
    */
   protected String getParameter(String parameterName, String defaultValue) {
      StringVariable   parameterVar = safeGetStringVariable(parameterName);
      if (parameterVar == null) {
         if (fVerbose) {
            System.err.println("Note: Failed to get parameter " + parameterName + ", using default '" + defaultValue + "'");
         }
         return defaultValue;
      }
      return parameterVar.getValueAsString();
   }

   /**
    * Get variable indicated by a parameter from peripheral configuration files
    * 
    * @param parameterVariableName  Name of parameter variable e.g. "/SIM/pdb_input_clock".
    *                               This variable contains the <b>name</b> of another variable to return
    * @param defaultVariableName    Name of variable to use if parameter variable is not found e.g. "/SIM/system_bus_clock"
    * 
    * @return Parameter variable or default variable
    * @throws Exception
    */
   protected Variable getParameterSelectedVariable(String parameterVariableName, String defaultVariableName) throws Exception {
      return getVariable(getParameter(parameterVariableName, defaultVariableName));
   }

}
