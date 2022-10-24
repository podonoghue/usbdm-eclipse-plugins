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
    * Update target value
    * 
    * @param controlVar       Variable controlling outcome
    * @param targetVariable   Target being controlled
    * @param reference        Reference string either "expression" or "primaryVar;expression" or "disabled"
    * 
    * @throws Exception
    */
   void updateTarget(Variable controlVar, Variable targetVariable, String reference) throws Exception {

//      if (controlVar.getName().equals("cmp_cr0_filter")) {
//         System.err.println("Found " + controlVar.getName());
//      }
      if ("disabled".equalsIgnoreCase(reference)) {
         if (targetVariable != null) {
            targetVariable.setValue(0);
            targetVariable.setStatus((Status)null);
            targetVariable.setOrigin("Disabled by "+controlVar.getName());
            targetVariable.enable(false);
         }
         return;
      }
//      if (reference.contains("SIM/system_bus_clock[0]")) {
//         System.err.println("Found " + reference);
//      }
      String data[]      = reference.split("#");
      String expression = data[data.length-1];
      SimpleExpressionParser parser = new SimpleExpressionParser(getPeripheral(), Mode.EvaluateFully);
      if (expression.isBlank()) {
         // This selection doesn't update target
         if (targetVariable != null) {
            targetVariable.setOrigin(null);
         }
         return;
      }
      Object result = parser.evaluate(expression);
      ArrayList<String>  identifiers = parser.getCollectedIdentifiers();

      String primaryClockSourceName = null;
      String origin = "";
      Status status = null;
      boolean enabled = true;
      if (data.length>1) {
         primaryClockSourceName = data[0];
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
         if (primaryClockSourceVar.getIsNamedClock()) {
            origin = primaryClockSourceVar.getName();
         }
         else {
            origin   = primaryClockSourceVar.getOrigin();
         }
      }
      if (controlVar != targetVariable) {
         if (controlVar instanceof VariableWithChoices) {
            origin = origin + "\n[selected by " + controlVar.getName() +"]";
         }
         else {
            origin = origin + "\n[modified by " + parser.getCollectedIdentifiers() +"]";
         }
      }
      else {
         if (identifiers.size()>1) {
            String t = String.join(", ", identifiers).replace(primaryClockSourceName, "");
            origin = origin + "\n[modified by " + t +"]";
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

   /**
    * Listener to handle relationship specified by a stringVariable
    * <li>The variable value is of form "[var1];expression"
    * <li>The target="variable" specifies the target
    */
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
             * The variable value is of form "[var];expression"
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

   /**
    * Listener for
    * <li> ChoiceVariable selection changes
    * <li> Changes in referenced variables in the Choices<br><br>
    * 
    * This listener is attached to a ChoiceVariable with:
    * <li> Target="targetVariable", the targets to be kept up to date
    * <li> Each choices with Ref="expression", the expressions used to update the target.
    * 
    */
   protected class ChoiceSelectionListener implements IModelChangeListener {

      final VariableWithChoices fClockSelector;

      public ChoiceSelectionListener(VariableWithChoices clockSelector) {
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
             * Each choice has a ref=... to indicate the equation for that selection
             */
            // Get selected equation(s) from active choice (ref="")
            String expression    = fClockSelector.getSelectedItemData().getReference();
            String expressions[] = expression.split(";");

            // Target variable(s) being controlled
            String target    = fClockSelector.getTarget();
            String targets[] = target.split(";");

            if (expressions.length != targets.length) {
               System.err.println(
                     "Targets and Expression do not match in length \n" +
                     "target     = '" + target + "'\n" +
                     "expression = '" + expression + "'" );
            }
            for (int index=0; index<expressions.length; index++) {
               updateTarget(fClockSelector, safeGetVariable(targets[index].trim()), expressions[index].trim());
            }
            
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

   protected class ControlledByListener implements IModelChangeListener {

      final Variable fControlledVar;

      /**
       * The variable that is enabled by the expression
       * 
       * @param controlledVar
       */
      public ControlledByListener(Variable controlledVar) {
         fControlledVar = controlledVar;
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
            String enabledByExpression = fControlledVar.getEnabledBy();
            if (enabledByExpression != null) {
               SimpleExpressionParser parser = new SimpleExpressionParser(getPeripheral(), Mode.EvaluateFully);
               Boolean result = (Boolean)parser.evaluate(enabledByExpression);
               fControlledVar.enable(result);
            }
            // Reference of form "[varName];expression"
            String unlockedByExpression = fControlledVar.getUnlockedBy();
            if (unlockedByExpression != null) {
               SimpleExpressionParser parser = new SimpleExpressionParser(getPeripheral(), Mode.EvaluateFully);
               Boolean unlocked = (Boolean)parser.evaluate(unlockedByExpression);
               fControlledVar.setLocked(!unlocked);
            }
         } catch (Exception e) {
            System.err.println("Failed to validate "+fControlledVar);
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
    * @param expressionString    String of form "expression" or "primaryVar#expression"
    * @param listener            Listener to add to variables
    * @param owningVar           The variable owning the reference (for debug messages)
    * 
    * @throws Exception
    */
   void addExpressionListeners(String expressionString, IModelChangeListener listener, Variable owningVar) throws Exception {
      
      expressionString = expressionString.trim();
      if (expressionString.isBlank() || ("disabled".equalsIgnoreCase(expressionString))) {
         return;
      }
      /*
       * Need to make sensitive to:
       *  - Each referenced variable in expression
       *  - Clock selection if any referenced variable is dependent on active clock selection
       */
      int numberOfClockSettings = (int)getLongVariable("/SIM/numberOfClockSettings").getValueAsLong();
      Variable clockSelectorVar = getVariable("/MCG/currentClockConfig");

      String parts[] = expressionString.split("#");
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
   
   /**
    * Create listener to handle relationship specified by a stringVariable
    * 
    * <li>The variable value is of form "[var1];expression"
    * <li>The target="variable" specifies the target
    *
    * @param stringVariable  The variable specifying the relationship
    * @param listeners       Listeners already added (and is updated)
    * 
    * @throws Exception
    */
   void addStringListener(StringVariable stringVariable, HashMap<String,Object> listeners) throws Exception {

      String targetName = stringVariable.getTarget();
      Variable target   = safeGetVariable(targetName);
      if (target == null) {
         throw new Exception("Target '"+targetName+"' not found for String var '"+stringVariable);
      }

      // Get/create listener to link references to target
      // Make sure only one is created for each monitored variable (in case it is simple variable in an iterated loop)
      // Note that if the variable is actually iterated then it will be a unique variable
      
      String referenceString = stringVariable.getValueAsString();
      String duplicateKey = stringVariable.getName()+referenceString;
      StringListener listener = (StringListener) listeners.get(duplicateKey);
      if (listener == null) {
         listener = new StringListener(stringVariable);
         listeners.put(duplicateKey, listener);
      }
      // Watch clock selector
      stringVariable.addListener(listener);

      if ((referenceString == null) || (referenceString.isBlank())) {
         throw new Exception("Clock reference is missing for Clock selector var '"+stringVariable+"'");
      }
      if ("disabled".equalsIgnoreCase(referenceString)) {
         return;
      }
      Variable reference = safeGetVariable(referenceString);
      if (reference == null) {
         throw new Exception("Clock reference variable '"+referenceString+"' not found for Clock selector var '"+stringVariable+"'");
      }
      // Watch references
      reference.addListener(listener);
   }
   
   /**
    * This handles a variable that depends on other variables
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
    * This handles a ChoiceVariable
    * <li> target="variable"  specifier controlled variable
    * <li> Each choice has ref="expression" specifying relationship
    * 
    * @param choiceVariable   Variable controlling a target
    * @param listeners        List of listeners to avoid duplication
    * 
    * @throws Exception
    */
   void addChoiceVariableSelectionListener(VariableWithChoices choiceVariable, HashMap<String,Object> listeners) throws Exception {

      String[] targetNames = choiceVariable.getTarget().split(";");

//      if (targetNames.length>1) {
//         System.err.println("Targets = " + targetNames[0] + ", "+targetNames[1]);
//      }
      for (String targetName:targetNames) {
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
         ChoiceSelectionListener listener = (ChoiceSelectionListener) listeners.get(duplicateKey);
         if (listener == null) {
            listener = new ChoiceSelectionListener(choiceVariable);
            listeners.put(duplicateKey, listener);
         }
         /* Need to make sensitive to:
          *  - Each referenced source in choices
          *  - Selector itself
          *  - Clock choice if any reference is dependent on active clock selection
          */

         // Watch choice selection
         choiceVariable.addListener(listener);

         // ChoiceVar selecting the input
         ChoiceData[] choiceDatas = choiceVariable.getData();
         for (ChoiceData choiceData:choiceDatas) {
            String references = choiceData.getReference();
            if ((references == null) || (references.isBlank())) {
               throw new Exception("Choice varu=iable reference is missing for selector var '"+choiceVariable+"'");
            }
            for (String expression:references.split(";")) {
               addExpressionListeners(expression, listener, choiceVariable);
            }
         }
      }
   }
   
   /**
    * This handles a variable that is enabled by other variables
    * <li>Variable will have a enabledBy="expression"
    *
    * @param enabledVariable Variable that is dependent (has enabledBy)
    * 
    * @throws Exception
    */
   private void addEnabldedByListener(Variable enabledVariable, HashMap<String, Object> addedListeners) throws Exception {
      
      // Get/create listener to link references to target
      // No need to check for duplicates as this would require multiple of the same dependent variable
      String enabledByExpression = enabledVariable.getEnabledBy();
      ControlledByListener listener = new ControlledByListener(enabledVariable);
      addExpressionListeners(enabledByExpression, listener, enabledVariable);
   }

   /**
    * This handles a variable that is enabled by other variables
    * <li>Variable will have a unlockedBy="expression"
    *
    * @param unlockedVariable Variable that is dependent (has enabledBy)
    * 
    * @throws Exception
    */
   private void addUnlockedByListener(Variable unlockedVariable, HashMap<String, Object> addedListeners) throws Exception {
      
      // Get/create listener to link references to target
      // No need to check for duplicates as this would require multiple of the same dependent variable
      String unlockedByExpression = unlockedVariable.getUnlockedBy();
      ControlledByListener listener = new ControlledByListener(unlockedVariable);
      addExpressionListeners(unlockedByExpression, listener, unlockedVariable);
   }

   /**
    * Add monitored variable and their references to watched variables<br>
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
            // This variable depends on other variables
            // Has attribute ref="expression"
            addDependentVariableListener(monitoredVariable);
            actionFound = true;
         }
         if (monitoredVariable.getTarget() != null) {
            // This variable controls other variables
            // Has attribute target="variable"
            actionFound = true;
            if (monitoredVariable instanceof StringVariable) {
               // String value specifies relationship (expression)
               addStringListener((StringVariable) monitoredVariable, addedListeners);
            }
            else if (monitoredVariable instanceof VariableWithChoices) {
               // ChoiceVariable
               // Target="variable" specifier controlled variable
               // Each choice has ref="expression" specifying relationship
               addChoiceVariableSelectionListener((VariableWithChoices) monitoredVariable, addedListeners);
            }
            else {
               throw new Exception("Monitored var '"+monitoredVariable+"' is of unsupported type");
            }
         }
         if (monitoredVariable.getEnabledBy() != null) {
            actionFound = true;
            addEnabldedByListener(monitoredVariable, addedListeners);
         }
         if (monitoredVariable.getUnlockedBy() != null) {
            actionFound = true;
            addUnlockedByListener(monitoredVariable, addedListeners);
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
