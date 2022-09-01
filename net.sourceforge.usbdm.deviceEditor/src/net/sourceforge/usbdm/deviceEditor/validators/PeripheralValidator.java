package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.HashMap;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
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
   public PeripheralWithState getPeripheral() {
      return (PeripheralWithState) fProvider;
   }

   /**
    * Validate peripheral settings dialogue
    * 
    * @param variable   Variable trigger change leading to validation (may be null)
    * 
    * @throws Exception
    */
   protected void validate(Variable variable) throws Exception {
      if (variable == null) {
         validateAllClockSelectors();
      }
   }

   private void validateAllClockSelectors() throws Exception {
//      ArrayList<Variable> clockSelectors = getPeripheral().getMonitoredVariables();
//      if (clockSelectors == null) {
//         return;
//      }
//      for (Variable clockSelector:clockSelectors) {
//         validateClockSelectorVariable(clockSelector);
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
    * @return  COnverted key or original if not indexed key
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
    * @param clockIndex Clock index to insert if needed
    * 
    * @return  COnverted key or original if not indexed key
    */
   String makeClockSpecificName(String key) {
      return makeClockSpecificName(key, fProvider.getDeviceInfo().getActiveClockSelection());
   }

   /**
    * Validate a string link variable
    * The variable value is name of referenced variable
    * 
    * @param stringLinkVar This variable is controlling a target
    * 
    * @throws Exception
    */
   private void validateStringLinkVariable(Variable stringLinkVar) throws Exception {

      // String variable with name of reference being used (or "disabled").
      String reference = stringLinkVar.getValueAsString();
      
      // Variable being controlled
      Variable targetVariable = safeGetVariable(stringLinkVar.getTarget());

      if (targetVariable instanceof StringVariable) {
         Variable sourceVar = safeGetVariable(makeClockSpecificName(reference));     
         String data = sourceVar.getValueAsString();
         targetVariable.setValue(data);
      }
      else if ((targetVariable instanceof LongVariable) || (targetVariable instanceof DoubleVariable)) {
         String data[] = reference.split(",");
         
         // Simple math operations
         Variable     clockSourceVar = safeGetVariable(makeClockSpecificName(data[0]));     
         long         value          = clockSourceVar.getValueAsLong();
         String       origin         = clockSourceVar.getOrigin();
         Object       result = null;

         if (data.length>1) {
            origin = "("+origin+")";
            SimpleExpressionParser parser = new SimpleExpressionParser(data[1], Long.toString(value));
            result = parser.evaluate().longValue();
         }
         else {
            result = value;
         }
         targetVariable.setValue(result);
         targetVariable.setStatus(clockSourceVar.getFilteredStatus());
         targetVariable.setOrigin(origin);
         targetVariable.enable(clockSourceVar.isEnabled());
         return;
      }
      else {
         throw new Exception("Unexpected variable type for '" + targetVariable + "'");
      }
   }

   /**
    * Validate a clock selector variable
    * 
    * @param selectorVar This variable is controlling a target by linking to multiple sources
    * 
    * @throws Exception
    */
   private void validateClockSelectorVariable(Variable selectorVar) throws Exception {

      // Get selected reference (source)
      VariableWithChoices  choiceVar      = (VariableWithChoices)selectorVar;
      ChoiceData           choiceData     = choiceVar.getSelectedItemData();
      String               reference      = choiceData.getReference();

      // Target variable being controlled
      Variable             targetVariable = safeGetVariable(selectorVar.getTarget());
      
      if ("disabled".equalsIgnoreCase(reference)) {
         targetVariable.setValue(0);
         targetVariable.setStatus((Status)null);
         targetVariable.setOrigin("Disabled by "+selectorVar.getName());
         targetVariable.enable(false);
         return;
      }

      if (targetVariable instanceof StringVariable) {
         Variable sourceVar = safeGetVariable(makeClockSpecificName(reference));     
         String data = sourceVar.getValueAsString();
         targetVariable.setValue(data);
      }
      else {
         // Of form "target[,expression]"
         String data[] = reference.split(",");

         // Simple math operations
         Variable     clockSourceVar = safeGetVariable(makeClockSpecificName(data[0]));     
         long         value          = clockSourceVar.getValueAsLong();
         String       origin         = clockSourceVar.getOrigin()+"\n";
         Object       result = null;

         if (data.length>1) {
            origin = origin + data[1].replace("%%", "X");
            SimpleExpressionParser parser = new SimpleExpressionParser(data[1], Long.toString(value));
            result = parser.evaluate().longValue();
         }
         else {
            result = value;
         }
         origin = origin + "[selected by "+selectorVar.getName() +"]";
         targetVariable.setValue(result);
         targetVariable.setStatus(clockSourceVar.getFilteredStatus());
         targetVariable.setOrigin(origin);
         targetVariable.enable(clockSourceVar.isEnabled());
         return;
      }
   }

   /**
    * Validate a string link variable
    * The variable value is name of referenced variable
    * 
    * @param dependentVar This variable is controlling a target
    * 
    * @throws Exception
    */
   private void validateDependentVariable(Variable dependentVar) throws Exception {

      // Reference of for "varName[,expression]"
      String reference = dependentVar.getReference();
      
      // Variable being controlled is itself
      Variable targetVariable = dependentVar;

      if (targetVariable instanceof StringVariable) {
         Variable sourceVar = safeGetVariable(makeClockSpecificName(reference));     
         String data = sourceVar.getValueAsString();
         targetVariable.setValue(data);
      }
      else if ((targetVariable instanceof LongVariable) || (targetVariable instanceof DoubleVariable)) {
         String data[] = reference.split(",");
         
         // Simple math operations
         Variable     clockSourceVar = safeGetVariable(makeClockSpecificName(data[0]));     
         long         value          = clockSourceVar.getValueAsLong();
         String       origin         = clockSourceVar.getOrigin();
         Object       result = null;

         if (data.length>1) {
            origin = "("+origin+")";
            SimpleExpressionParser parser = new SimpleExpressionParser(data[1], Long.toString(value));
            result = parser.evaluate().longValue();
         }
         else {
            result = value;
         }
         targetVariable.setValue(result);
         targetVariable.setStatus(clockSourceVar.getFilteredStatus());
         targetVariable.setOrigin(origin);
         targetVariable.enable(clockSourceVar.isEnabled());
         return;
      }
      else {
         throw new Exception("Unexpected variable type for '" + targetVariable + "'");
      }
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
            // System.err.println("Validating "+fClockSelector);
            validateStringLinkVariable(fStringVariable);
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
            //            System.err.println("Validating "+fClockSelector);
            validateClockSelectorVariable(fClockSelector);
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
            //            System.err.println("Validating "+fClockSelector);
            validateDependentVariable(fDependentVar);
         } catch (Exception e) {
            System.err.println("Failed to validate "+fDependentVar);
            e.printStackTrace();
         }
      }

      @Override
      public void elementStatusChanged(ObservableModel observableModel) {
      }
   };

   void addStringListener(StringVariable monitoredVariable, HashMap<Variable,Object> listeners) throws Exception {

      String targetName = monitoredVariable.getTarget();
      Variable target   = safeGetVariable(targetName);
      if (target == null) {
         throw new Exception("Target '"+targetName+"' not found for String var '"+monitoredVariable);
      }

      // Get/create listener to link references to target
      // Make sure only one is created for each monitored variable (in case it is in an iterated loop)
      // Note that if the variable is actually iterated then it will be a unique variable
      StringListener listener = (StringListener) listeners.get(monitoredVariable);
      if (listener == null) {
         listener = new StringListener(monitoredVariable);
         listeners.put(monitoredVariable, listener);
      }
      // Watch clock selector
      monitoredVariable.addListener(listener);

      String referenceString = monitoredVariable.getValueAsString();
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
   
   void addClockSelectorListener(VariableWithChoices monitoredVariable, HashMap<Variable,Object> listeners) throws Exception {

      int numberOfClockSettings = (int)getLongVariable("/SIM/numberOfClockSettings").getValueAsLong();
      Variable clockSelectorVar = getVariable("/MCG/currentClockConfig");

      String targetName = monitoredVariable.getTarget();
      Variable target   = safeGetVariable(targetName);
      if (target == null) {
         throw new Exception("Target '"+targetName+"' not found for Clock selector var '"+monitoredVariable+"' have target");
      }

      // Get/create listener to link references to target
      // Make sure only one is created for each monitored variable (in case it is in an iterated loop)
      // Note that if the variable is actually iterated then it will be a unique variable
      ClockSelectorListener listener = (ClockSelectorListener) listeners.get(monitoredVariable);
      if (listener == null) {
         listener = new ClockSelectorListener(monitoredVariable);
         listeners.put(monitoredVariable, listener);
      }
      // Watch clock selector
      monitoredVariable.addListener(listener);

      /* Need to make sensitive to:
       *  - Each referenced source in choices
       *  - Selector itself
       *  - Clock choice if any reference is dependent on active clock selection 
       */
      // ChoiceVar selecting the clock input
      VariableWithChoices cv = (VariableWithChoices)monitoredVariable;
      ChoiceData[] choiceDatas = cv.getData();
      for (ChoiceData choiceData:choiceDatas) {
         String referenceString = choiceData.getReference();
         if ((referenceString == null) || (referenceString.isBlank())) {
            throw new Exception("Clock reference is missing for Clock selector var '"+monitoredVariable+"'");
         }
         if ("disabled".equalsIgnoreCase(referenceString)) {
            continue;
         }
         String parts[] = referenceString.split(",");

         if (isClockDependent(parts[0])) {

            // Make sensitive to clock selector
            clockSelectorVar.addListener(listener);
            // Make sensitive to all clock variations of variable
            for (int clockSel=0; clockSel<numberOfClockSettings; clockSel++) {
               Variable reference = safeGetVariable(makeClockSpecificName(parts[0], clockSel));
               if (reference == null) {
                  throw new Exception("Clock reference variable '"+choiceData.getReference()+"' not found for Clock selector var '"+monitoredVariable+"' have target");
               }
               // Watch references
               reference.addListener(listener);
            }
         }
         else {
            Variable reference = safeGetVariable(parts[0]);
            if (reference == null) {
               throw new Exception("Clock reference variable '"+choiceData.getReference()+"' not found for Clock selector var '"+monitoredVariable+"' have target");
            }
            // Watch references
            reference.addListener(listener);
         }
      }
   }
   
   void addDependentVariableListener(Variable dependentVariable, HashMap<Variable,Object> listeners) throws Exception {
      
      int numberOfClockSettings = (int)getLongVariable("/SIM/numberOfClockSettings").getValueAsLong();
      Variable clockSelectorVar = getVariable("/MCG/currentClockConfig");

      // Get/create listener to link references to target
      // Make sure only one is created for each monitored variable (in case it is in an iterated loop)
      // Note that if the variable is actually iterated then it will be a unique variable
      DependentVariableListener listener = (DependentVariableListener) listeners.get(dependentVariable);
      if (listener == null) {
         listener = new DependentVariableListener(dependentVariable);
         listeners.put(dependentVariable, listener);
      }

      String referenceString = dependentVariable.getReference();
      if ((referenceString == null) || (referenceString.isBlank())) {
         throw new Exception("Reference is missing for dependent var '"+dependentVariable+"'");
      }
      if ("disabled".equalsIgnoreCase(referenceString)) {
         return;
      }
      String parts[] = referenceString.split(",");

      if (isClockDependent(parts[0])) {

         // Make sensitive to clock selector
         clockSelectorVar.addListener(listener);
         
         // Make sensitive to all clock variations of reference
         for (int clockSel=0; clockSel<numberOfClockSettings; clockSel++) {
            Variable reference = safeGetVariable(makeClockSpecificName(parts[0], clockSel));
            if (reference == null) {
               throw new Exception("Clock reference variable '"+referenceString+"' not found for Clock selector var '"+dependentVariable+"' have target");
            }
            // Watch references
            reference.addListener(listener);
         }
      }
      else {
         Variable reference = safeGetVariable(parts[0]);
         if (reference == null) {
            throw new Exception("Clock reference variable '"+referenceString+"' not found for Clock selector var '"+dependentVariable+"' have target");
         }
         // Watch references
         reference.addListener(listener);
      }
   }
   
   /**
    * Add clock selectors and their references to watched variables<br>
    * The clock selectors are obtained from the associated peripheral
    * 
    * @throws Exception
    */
   private void addClockSelectors() throws Exception {
      ArrayList<Variable> monitoredVariables = getPeripheral().getMonitoredVariables();
      if (monitoredVariables == null) {
         return;
      }
      
      // List to prevent repeated listeners
      HashMap<Variable,Object> addedListeners  = new HashMap<Variable,Object>();
      
      for (Variable monitoredVariable:monitoredVariables) {

         if (monitoredVariable.getReference() != null) {
            // This variable depends on another variable
            addDependentVariableListener(monitoredVariable, addedListeners);
         }
         else if (monitoredVariable.getTarget() != null) {
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
         else {
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
      addClockSelectors();
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
