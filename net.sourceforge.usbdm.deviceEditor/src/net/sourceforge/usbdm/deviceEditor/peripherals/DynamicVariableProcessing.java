package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.util.ArrayList;
import java.util.HashMap;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.ClockSelectionVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.ChoiceData;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ModelChangeAdapter;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.parsers.SimpleExpressionParser;
import net.sourceforge.usbdm.deviceEditor.parsers.SimpleExpressionParser.Mode;

public class DynamicVariableProcessing {

   /**
    * Add handlers for dynamic variables belonging to the given peripheral
    * 
    * @param peripheral
    */
   static public void addMonitoredVariableListeners(Peripheral peripheral) {

      if (peripheral instanceof PeripheralWithState) {
         PeripheralWithState pws = (PeripheralWithState) peripheral;
         ArrayList<Variable> monitoredVariables = pws.getMonitoredVariables();
         if (monitoredVariables == null) {
            return;
         }
         DynamicVariableProcessing processing = new DynamicVariableProcessing(pws);
         try {
            processing.addMonitoredVariableListeners(monitoredVariables);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   /// Device info
   private final DeviceInfo          fDeviceInfo;
   
   // Peripheral owning dynamic variables
   private final PeripheralWithState fPeripheral;
   
   // HashMap to merge duplicate handlers
   private HashMap<String, Object>   listenerMap = new HashMap<String, Object>();

   /**
    * Create class to handle dynamic variables belonging to the given peripheral
    * 
    * @param peripheral       Peripheral containing dynamic variables to add handlers to
    */
   private DynamicVariableProcessing(PeripheralWithState peripheral) {
      fPeripheral       = peripheral;
      fDeviceInfo       = fPeripheral.getDeviceInfo();
   }

   /**
    * Adds listeners for all monitored variables
    * 
    * @throws Exception
    */
   public void addMonitoredVariableListeners(ArrayList<Variable> monitoredVariables) throws Exception {

      for (Variable monitoredVariable:monitoredVariables) {
         boolean actionFound = false;
         if (monitoredVariable.getTarget() != null) {
            // This variable controls other variables
            // Has attribute target="variable"
            actionFound = true;
            if (monitoredVariable instanceof StringVariable) {
               // String value specifies relationship (expression)
               addStringListener((StringVariable) monitoredVariable);
            }
            else if (monitoredVariable instanceof VariableWithChoices) {
               // ChoiceVariable
               // Target="variable" specifier controlled variable
               // Each choice has ref="expression" specifying relationship
               addChoiceVariableSelectionListener((VariableWithChoices) monitoredVariable);
            }
            else {
               throw new Exception("Monitored var '"+monitoredVariable+"' is of unsupported type");
            }
         }
         if (monitoredVariable.getReference() != null) {
            // This variable depends on other variables
            // Has attribute ref="expression"
            addDependentVariableListener(monitoredVariable);
            actionFound = true;
         }
         if (addErrorIfListener(monitoredVariable)) {
            actionFound = true;
         }
         if (addEnabledByListener(monitoredVariable)) {
            actionFound = true;
         }
         if (addUnlockedByListener(monitoredVariable)) {
            actionFound = true;
         }
         if (monitoredVariable instanceof LongVariable) {
            LongVariable lv = (LongVariable) monitoredVariable;
            if (addMinMaxChangeListener(lv)) {
               actionFound = true;
            }
         }
         if (monitoredVariable instanceof ChoiceVariable ) {
            ChoiceVariable cv = (ChoiceVariable) monitoredVariable;
            if (addDynamicChangeListener(cv)) {
               actionFound = true;
            }
         }
         if (!actionFound) {
            throw new Exception("Monitored var '"+monitoredVariable+"' doesn't have dynamic parameters or references");
         }
      }
   }

   /**
    * Check for existing listener with the same key.
    * If found it is returned.
    * If not found then the listener provided is recorded.
    * 
    * @param key        Key uniquely identifying listener
    * @param listener   Listener to check
    * 
    * @return Existing listener or provided listener
    */
   public Object getExistingListener(String key, ModelChangeAdapter listener) {
      Object foundListener = listenerMap.get(key);
      if (foundListener == null) {
         
         // Trigger initial update of variable
         listener.modelElementChanged(null);

         listenerMap.put(key, listener);
         foundListener = listener;
      }
//      else {
//         System.err.println("Discarding duplicate handler "+ key);
//      }
      return foundListener;
   }

   /**
    * Class to accumulate variable changes
    */
   private static class VariableUpdateInfo {
      Object  value   = null;
      Status  status  = null;
      String  origin  = null;
      boolean enable  = true;
   };

   /**
    * Determine updates to a variable
    * 
    * @param controlVar       Variable controlling outcome.  Only used in generating origin path.
    * @param targetVariable   Target being controlled. Only used in generating origin path.
    * @param reference        Reference string either "expression" or "primaryVar;expression" or "disabled"
    * 
    * @return Variable update information from evaluating expression etc.
    * 
    * @throws Exception
    */
   private VariableUpdateInfo determineVariableUpdate(Variable controlVar, Variable targetVariable, String reference) throws Exception {

      VariableUpdateInfo info = new VariableUpdateInfo();

      if ("disabled".equalsIgnoreCase(reference)) {
         if (targetVariable != null) {
            info.origin = "Disabled by "+controlVar.getName();
            info.enable = false;
         }
         return info;
      }
      String data[]     = reference.split("#");
      String expression = data[data.length-1];
      SimpleExpressionParser parser = new SimpleExpressionParser(fPeripheral, Mode.EvaluateFully);
      if (expression.isBlank()) {
         // This selection doesn't update target
         if (targetVariable != null) {
            targetVariable.setOrigin(null);
         }
         return null;
      }
      info.value = parser.evaluate(expression);
      ArrayList<String>  identifiers = parser.getCollectedIdentifiers();

      String primaryVariableInExpressionName = null;
      if (data.length>1) {
         primaryVariableInExpressionName = data[0];
      }
      else if (identifiers.size()>0) {
         primaryVariableInExpressionName = identifiers.get(0);
      }
      else {
         // No variables in expression - assume a constant value
         info.origin = "[Fixed]";
      }

      // Assume enabled (may be later disabled by enabledBy expression)
      info.enable = true;
      if (primaryVariableInExpressionName != null) {
         // Get status and enable from primary variable
         Variable primaryVariableInExpression = safeGetVariable(primaryVariableInExpressionName);
         info.status   = primaryVariableInExpression.getStatus();
         info.enable   = primaryVariableInExpression.isEnabled();
         if (primaryVariableInExpression.getIsNamedClock()) {
            info.origin = primaryVariableInExpression.getName();
         }
         else {
            info.origin   = primaryVariableInExpression.getOrigin();
         }
      }
      if (controlVar != targetVariable) {
         if (controlVar instanceof VariableWithChoices) {
            info.origin = info.origin + "\n[selected by " + controlVar.getName() +"]";
         }
         else {
            info.origin = info.origin + "\n[modified by " + parser.getCollectedIdentifiers() +"]";
         }
      }
      else {
         if (identifiers.size()>1) {
            String t = String.join(", ", identifiers).replace(primaryVariableInExpressionName, "");
            info.origin = info.origin + "\n[modified by " + t +"]";
         }
      }
      return info;
   }

   /**
    * Update target value
    * 
    * @param controlVar       Variable controlling outcome
    * @param targetVariable   Target being controlled
    * @param expression       Reference string either "expression" or "primaryVar;expression" or "disabled"
    * 
    * @throws Exception
    */
   private VariableUpdateInfo updateTarget(Variable controlVar, Variable targetVariable, String expression) throws Exception {

      VariableUpdateInfo info = determineVariableUpdate(controlVar, targetVariable, expression);
      
      if ((targetVariable != null) && (info != null)) {
         info.enable = info.enable && targetVariable.evaluateEnable(fPeripheral);
         targetVariable.setStatus(info.status);
         if (info.value != null) {
            targetVariable.setValue(info.value);
         }
         if (info.origin != null) {
            targetVariable.setOrigin(info.origin);
         }
         targetVariable.enable(info.enable);
      }
      if ((controlVar != null) && (controlVar instanceof ClockSelectionVariable)) {
         ClockSelectionVariable cv = (ClockSelectionVariable) controlVar;
         cv.setDisplayValue(info.value.toString());
      }
      return info;
   }

   private DynamicChoiceVariableListener getOrCreateDynamicChoiceVariableListener(ChoiceVariable var) {

      DynamicChoiceVariableListener listener = new DynamicChoiceVariableListener(var);
      listener = (DynamicChoiceVariableListener) getExistingListener("DynamicChoiceVariableListener#"+var.getKey(), listener);

      return listener;
   }

   private class DynamicChoiceVariableListener extends ModelChangeAdapter {

      final ChoiceVariable fChoiceVariable;

      public DynamicChoiceVariableListener(ChoiceVariable choiceVariable) {
         fChoiceVariable = choiceVariable;
      }

      @Override
      public void modelElementChanged(ObservableModel observableModel) {

//         System.err.println(String.format("#%10d: %-70s", hashCode(), "DynamicChoiceVariableListener("+fChoiceVariable.getKey()+")")+"observableModel="+observableModel);

         fChoiceVariable.updateChoices();
      }
   };

   private StringListener getOrCreateStringListener(StringVariable stringVariable) {

      StringListener listener = new StringListener(stringVariable);
      StringListener newListener = (StringListener) getExistingListener("StringListener#"+stringVariable.getKey(), listener);
      if (newListener != listener) {
         System.err.println("Discarded duplicate StringListener for " + stringVariable.getKey());
      }
      return newListener;
   }

   /**
    * Listener to handle relationship specified by a stringVariable
    * <li>The variable value is of form "[var1];expression"
    * <li>The target="variable" specifies the target
    */
   private class StringListener extends ModelChangeAdapter {

      final StringVariable fStringVariable;

      public StringListener(StringVariable stringVariable) {
         fStringVariable = stringVariable;
      }

      @Override
      public void modelElementChanged(ObservableModel observableModel) {
//         System.err.println(String.format("#%10d: %-70s", hashCode(), "ControlledByListener("+fStringVariable.getKey()+")")+"observableModel="+observableModel);
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
   };

   private ChoiceSelectionListener getOrCreateChoiceSelectionListener(VariableWithChoices var) {

      ChoiceSelectionListener listener = new ChoiceSelectionListener(var);
      listener = (ChoiceSelectionListener) getExistingListener("ChoiceSelectionListener#"+var.getKey(), listener);

      return listener;
   }

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
   private class ChoiceSelectionListener extends ModelChangeAdapter {

      final VariableWithChoices fChoiceVariable;

      public ChoiceSelectionListener(VariableWithChoices clockSelector) {
         fChoiceVariable = clockSelector;
      }

      @Override
      public void modelElementChanged(ObservableModel observableModel) {
//         System.err.println(String.format("#%10d: %-70s", hashCode(), "ChoiceSelectionListener("+fChoiceVariable.getKey()+")")+"observableModel="+observableModel);
         try {
            /**
             * Validate a choice variable
             * 
             * target=... specifies the target variable
             * Each choice has a ref=... to indicate the equation for that selection
             */
            // Get selected equation(s) from active choice (ref="")
            String expression    = fChoiceVariable.getSelectedItemData().getReference();
            String expressions[] = expression.split(";");

            // Target variable(s) being controlled
            String target    = fChoiceVariable.getTarget();
            String targets[] = target.split(";");

            if (expressions.length != targets.length) {
               System.err.println(
                     "Targets and Expression do not match in length \n" +
                           "target     = '" + target + "'\n" +
                           "expression = '" + expression + "'" );
            }
            for (int index=0; index<expressions.length; index++) {
               if (targets[index].trim().equalsIgnoreCase("Disabled")) {
                  continue;
               }
               updateTarget(fChoiceVariable, safeGetVariable(targets[index].trim()), expressions[index].trim());
            }

         } catch (Exception e) {
            System.err.println("Failed to validate "+fChoiceVariable);
            e.printStackTrace();
         }
      }
   };

   private ControlledByListener getOrCreateControlledByListener(Variable var) {

      ControlledByListener listener = new ControlledByListener(var);
      listener = (ControlledByListener) getExistingListener("ControlledByListener#"+var.getKey(), listener);

      return listener;
   }

   private class ControlledByListener extends ModelChangeAdapter {

      /** Variable being re-evaluated */
      final Variable fControlledVar;

      /**
       * Creates a listener for variable changes<br>
       * On change, the given variable's expressions are re-evaluated:
       *  <li>ref expression
       *  <li>enabledBy expression
       *  <li>errorIf expression
       *  <li>unlockedBy expression
       *  <li>min and max expressions
       * 
       * @param controlledVar Variable to re-evaluate on change
       */
      public ControlledByListener(Variable controlledVar) {
         fControlledVar = controlledVar;
      }

      @Override
      public void modelElementChanged(ObservableModel observableModel) {
//         System.err.println(String.format("#%10d: %-70s", hashCode(), "ControlledByListener("+fControlledVar.getKey()+")")+"observableModel="+observableModel);
         try {

            VariableUpdateInfo info;

            //            if (fControlledVar.getName().contains("osc_clock")) {
            //               System.err.println("Found, obs = " + observableModel);
            //            }
            // Reference of form "[varName];expression"
            String reference = fControlledVar.getReference();
            if (reference != null) {
               // Variable being controlled is itself
               info = determineVariableUpdate(fControlledVar, fControlledVar, reference);
            }
            else {
               info = new VariableUpdateInfo();
            }

            // Reference of form "[varName];expression"
            String enabledByExpression = fControlledVar.getEnabledBy();
            if (enabledByExpression != null) {
               SimpleExpressionParser parser = new SimpleExpressionParser(fPeripheral, Mode.EvaluateFully);
               info.enable = (Boolean)parser.evaluate(enabledByExpression);
               if (!info.enable) {
                  info.status = new Status(fControlledVar.getEnabledByMessage(parser), Severity.INFO);
               }
            }

            String errorIfExpression = fControlledVar.getErrorIf();
            if (errorIfExpression != null) {
               SimpleExpressionParser parser = new SimpleExpressionParser(fPeripheral, Mode.EvaluateFully);
               if ((Boolean)parser.evaluate(errorIfExpression)) {
                  info.status = new Status(fControlledVar.getErrorIfMessage());
               }
            }
            // Reference of form "[varName],expression"
            String unlockedByExpression = fControlledVar.getUnlockedBy();
            if (unlockedByExpression != null) {
               SimpleExpressionParser parser = new SimpleExpressionParser(fPeripheral, Mode.EvaluateFully);
               Boolean unlocked = (Boolean)parser.evaluate(unlockedByExpression);
               fControlledVar.setLocked(!unlocked);
            }
            //            System.err.println("fControlledVar="+fControlledVar+", observableModel="+observableModel);
            fControlledVar.enable(info.enable);
            fControlledVar.setStatus(info.status);
            if (info.value != null) {
               fControlledVar.setValue(info.value);
            }
            if (info.origin != null) {
               fControlledVar.setOrigin(info.origin);
            }

            if (fControlledVar instanceof LongVariable) {
               LongVariable lv = (LongVariable) fControlledVar;
               lv.updateMin();
               lv.updateMax();
            }
         } catch (Exception e) {
            System.err.println("Failed to validate "+fControlledVar);
            e.printStackTrace();
         }
      }
   };

   /**
    * Check if variable is clock setting dependent i.e. end with "[]"
    * 
    * @param key
    * @return
    */
   private boolean isClockDependent(String key) {
      return key.endsWith("[]");
   }

//   /**
//    * Converts a variable key into a clock specific key e.g. varkey[] => varKey[clockIndex]
//    *
//    * @param key        Key to convert
//    * @param clockIndex Clock index to insert if needed
//    *
//    * @return  Converted key or original if not indexed key
//    */
//   private String makeClockSpecificName(String key, int clockIndex) {
//      if (isClockDependent(key)) {
//         key = key.substring(0,key.length()-2)+"["+clockIndex+"]";
//      }
//      return key;
//   }

   /**
    * Get Variable from associated peripheral.
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable or null if not found
    */
   protected Variable safeGetVariable(String key) {
      if (key.contains("[]")) {
         key = key.replace("[]", Integer.toString(fDeviceInfo.getActiveClockSelection()));
      }
      return fPeripheral.safeGetVariable(fPeripheral.makeKey(key));
   }

   /**
    * Get Variable from associated peripheral.
    * 
    * @param key  Key to lookup variable
    * 
    * @return Variable
    * 
    * @throws Exception if variable not found
    */
   protected Variable getVariable(String key) throws Exception {
      Variable var = safeGetVariable(key);
      if (var == null) {
         throw new Exception("Variable '"+key+"' not found for peripheral '"+fPeripheral.getName());
      }
      return var;
   }

   /**
    * Add listeners to all variables used in expression and clock as needed
    * 
    * @param expressionString    String of form "expression" or "primaryVar#expression"
    * @param listener            Listener to add to variables
    * @param owningVar           The variable owning the reference (for debug messages)
    * 
    * @throws Exception
    */
   private void addExpressionListeners(String expressionString, IModelChangeListener listener, Variable owningVar) throws Exception {

      expressionString = expressionString.trim();
      if (expressionString.isBlank() || ("disabled".equalsIgnoreCase(expressionString))) {
         return;
      }
      /*
       * Need to make sensitive to:
       *  - Each referenced variable in expression
       *  - Clock selection if any referenced variable is dependent on active clock selection
       */
      String parts[] = expressionString.split("#");
      // The right-most entry is the expression (may be by itself)
      String expression = parts[parts.length-1];
      SimpleExpressionParser parser = new SimpleExpressionParser(fPeripheral, Mode.CollectIdentifiers);
      parser.evaluate(expression);
      ArrayList<String> identifiers = parser.getCollectedIdentifiers();
      for (String refName:identifiers) {
         if (isClockDependent(refName)) {
            throw new Exception("Unexpected indexed variable '"+refName+"' in expression for '"+owningVar+"'");
         }
         else {
            Variable reference = safeGetVariable(refName);
            if (reference == null) {
               throw new Exception("Variable '"+refName+"' not found in expression for '"+owningVar+"'");
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
   private void addStringListener(StringVariable stringVariable) throws Exception {

      String targetName = stringVariable.getTarget();
      Variable target   = safeGetVariable(targetName);
      if (target == null) {
         throw new Exception("Target '"+targetName+"' not found for String var '"+stringVariable);
      }

      // Get/create listener to link references to target
      StringListener listener = getOrCreateStringListener(stringVariable);
      //      String duplicateKey = stringVariable.getName()+referenceString;
      //      StringListener listener = (StringListener) listeners.get(duplicateKey);
      //      if (listener == null) {
      //         listener = new StringListener(stringVariable);
      //         listeners.put(duplicateKey, listener);
      //      }
      // Watch clock selector
      stringVariable.addListener(listener);

      String referenceString = stringVariable.getValueAsString();
      if ((referenceString == null) || (referenceString.isBlank())) {
         throw new Exception("Reference is missing for String var '"+stringVariable+"'");
      }
      if ("disabled".equalsIgnoreCase(referenceString)) {
         return;
      }
      Variable reference = safeGetVariable(referenceString);
      if (reference == null) {
         throw new Exception("Reference variable '"+referenceString+"' not found for String var '"+stringVariable+"'");
      }
      // Watch references
      reference.addListener(listener);
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
   private void addChoiceVariableSelectionListener(VariableWithChoices choiceVariable) throws Exception {

      String[] targetNames = choiceVariable.getTarget().split(";");

      //      if (targetNames.length>1) {
      //         System.err.println("Targets = " + targetNames[0] + ", "+targetNames[1]);
      //      }
      // Get/create listener to link references to target
      ChoiceSelectionListener listener = getOrCreateChoiceSelectionListener(choiceVariable);

      for (String targetName:targetNames) {
         if (targetName.isBlank() && (choiceVariable instanceof ClockSelectionVariable)) {
            // Allow to have no target for self updating clock selection
         }
         else {
            // Sanity check target
            Variable target  = safeGetVariable(targetName);
            if (target == null) {
               throw new Exception("Target '"+targetName+"' not found for choice var '"+choiceVariable+"'");
            }
         }

         /* Need to make sensitive to:
          *  - Each referenced source in choices
          *  - Selector itself
          *  - Clock choice if any reference is dependent on active clock selection
          */

         // Watch choice selection
         choiceVariable.addListener(listener);

         // ChoiceVar selecting the input
         ChoiceData[] choiceDatas = choiceVariable.getChoiceData();
         for (ChoiceData choiceData:choiceDatas) {
            String references = choiceData.getReference();
            if ((references == null) || (references.isBlank())) {
               throw new Exception("Reference is missing for choice '"+choiceData.getName()+"' in choice var '"+choiceVariable+"'");
            }
            for (String expression:references.split(";")) {
               addExpressionListeners(expression, listener, choiceVariable);
            }
         }
      }
   }

   /**
    * This handles a variable that depends on other variables
    * This variable will have a reference="expression"
    *
    * @param controlledVariable Variable that is dependent (has reference)
    * 
    * @throws Exception
    */
   private boolean addDependentVariableListener(Variable controlledVariable) throws Exception {

      String enabledByExpression = controlledVariable.getReference();

      if (enabledByExpression == null) {
         return false;
      }

      // Get/create listener to link references to target
      //      // No need to check for duplicates as this would require multiple of the same dependent variable
      //      String referenceString = dependentVariable.getReference();
      //      DependentVariableListener listener = new DependentVariableListener(dependentVariable);
      //      if ((referenceString == null) || (referenceString.isBlank())) {
      //         throw new Exception("Reference is missing for dependent var '"+dependentVariable+"'");
      //      }
      //      addExpressionListeners(referenceString, listener, dependentVariable);

      // Get/create listener to link references to target
      // No need to check for duplicates as this would require multiple of the same dependent variable
      ControlledByListener listener = getOrCreateControlledByListener(controlledVariable);
      addExpressionListeners(enabledByExpression, listener, controlledVariable);
      return true;
   }

   /**
    * This handles a variable that is enabled by other variables
    * <li>Variable will have a enabledBy="expression"
    *
    * @param enabledVariable Variable that is dependent (has enabledBy)
    * 
    * @return true => Listener added otherwise no action
    * 
    * @throws Exception
    */
   private boolean addEnabledByListener(Variable enabledVariable) throws Exception {

      String enabledByExpression = enabledVariable.getEnabledBy();
      if (enabledByExpression == null) {
         return false;
      }
      // Get/create listener to link references to target
      // No need to check for duplicates as this would require multiple of the same dependent variable
      ControlledByListener listener = getOrCreateControlledByListener(enabledVariable);
      addExpressionListeners(enabledByExpression, listener, enabledVariable);
      return true;
   }

   /**
    * This handles a variable that is forced error by other variables
    * <li>Variable will have a errorIf="expression"
    *
    * @param erroredVariable Variable that is dependent (has errrorIf)
    * 
    * @return true => Listener added otherwise no action
    * 
    * @throws Exception
    */
   private boolean addErrorIfListener(Variable erroredVariable) throws Exception {

      String errorIfExpression = erroredVariable.getErrorIf();
      if (errorIfExpression == null) {
         return false;
      }
      // Get/create listener to link references to target
      // No need to check for duplicates as this would require multiple of the same dependent variable
      ControlledByListener listener = getOrCreateControlledByListener(erroredVariable);
      addExpressionListeners(errorIfExpression, listener, erroredVariable);
      return true;
   }

   /**
    * This handles a variable that is enabled by other variables
    * <li>Variable will have a unlockedBy="expression"
    *
    * @param unlockedVariable Variable that is dependent (has enabledBy)
    * 
    * @return true => Listener added otherwise no action
    * 
    * @throws Exception
    */
   private boolean addUnlockedByListener(Variable unlockedVariable) throws Exception {

      String unlockedByExpression = unlockedVariable.getUnlockedBy();
      if (unlockedByExpression == null) {
         return false;
      }
      // Get/create listener to link references to target
      // No need to check for duplicates as this would require multiple of the same dependent variable
      ControlledByListener listener = getOrCreateControlledByListener(unlockedVariable);
      addExpressionListeners(unlockedByExpression, listener, unlockedVariable);
      return true;
   }

   /**
    * This handles a variable that has dynamic min/max expressions
    * 
    * <li>Variable will have a min="expression" and/or max="expression"
    *
    * @param dynamicMinMaxVariable Variable that is dependent (has min/max expressions)
    * 
    * @return true => Listener added otherwise no action
    * 
    * @throws Exception
    */
   private boolean addMinMaxChangeListener(LongVariable dynamicMinMaxVariable) throws Exception {

      String dynamicExpression = dynamicMinMaxVariable.getMaxExpression();
      if (dynamicExpression == null) {
         dynamicExpression = dynamicMinMaxVariable.getMinExpression();
      }
      if (dynamicExpression == null) {
         // No listener added
         return false;
      }
      // Create listener to link references to target
      // No need to check for duplicates as this would require multiple of the same dependent variable
      addExpressionListeners(dynamicExpression, getOrCreateControlledByListener(dynamicMinMaxVariable), dynamicMinMaxVariable);
      return true;
   }

   /**
    * This handles a variable that has dynamic choices
    * 
    * @param dynamicChoiceVariable Variable that is dependent (has min/max expressions)
    * 
    * @return true => Listener added otherwise no action
    * 
    * @throws Exception
    */
   private boolean addDynamicChangeListener(ChoiceVariable dynamicChoiceVariable) throws Exception {

      boolean dynamicValueFound = false;
      ChoiceData choices[] = dynamicChoiceVariable.getChoiceData();
      DynamicChoiceVariableListener listener = getOrCreateDynamicChoiceVariableListener(dynamicChoiceVariable);
      for(ChoiceData choice:choices) {
         String dynamicExpression = choice.getEnabledBy();
         if (dynamicExpression == null) {
            continue;
         }
         // Create listener to changes in choices of target
         addExpressionListeners(dynamicExpression, listener, dynamicChoiceVariable);
         dynamicValueFound = true;
      }
      return dynamicValueFound;
   }

}
