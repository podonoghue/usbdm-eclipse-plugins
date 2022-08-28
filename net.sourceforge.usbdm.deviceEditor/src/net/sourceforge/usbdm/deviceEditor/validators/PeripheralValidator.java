package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.ChoiceData;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class PeripheralValidator extends Validator {

   protected static final Status UNMAPPED_PIN_STATUS = new Status("Not all common signals are mapped to pins", Severity.WARNING);
   
   /**
    * Peripheral dialogue validator <br>
    * Constructor used by derived classes
    * 
    * @param peripheral
    */
   public PeripheralValidator(PeripheralWithState peripheral, int index) {
      super((VariableProvider)peripheral, index);
   }

   /**
    * Peripheral dialogue validator <br>
    * Constructor used by derived classes
    * 
    * @param peripheral
    */
   public PeripheralValidator(PeripheralWithState peripheral, ArrayList<Object> list) {
      super((VariableProvider)peripheral);
   }

   /**
    * Peripheral dialogue validator <br>
    * Constructor used by derived classes
    * 
    * @param peripheral
    */
   public PeripheralValidator(PeripheralWithState peripheral) {
      super((VariableProvider)peripheral);
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
      ArrayList<Variable> clockSelectors = getPeripheral().getClockSelectors();
      if (clockSelectors == null) {
         return;
      }
      for (Variable clockSelector:clockSelectors) {
         validateClockSelectorVariable(clockSelector);
      }
   }
      
   /**
    * Validate a clock selector variable
    * 
    * @param selectorVar This variable is controlling a target
    * 
    * @throws Exception
    */
   private void validateClockSelectorVariable(Variable selectorVar) throws Exception {

      // Get clock source (selected input)
      String reference = null;
      if (selectorVar instanceof StringVariable) {
         // String variable with name of reference being used.
         reference = selectorVar.getValueAsString();
      }
      else if (selectorVar instanceof VariableWithChoices) {
         VariableWithChoices cv = (VariableWithChoices)selectorVar;
         ChoiceData choiceData = cv.getSelectedItemData();
         reference = choiceData.getReference();
      }
      else {
         throw new Exception("Selector control variable not of correct type" + selectorVar);
      }
      Variable targetVariable = safeGetVariable(selectorVar.getTarget());
      if (targetVariable instanceof LongVariable) {
         LongVariable targetClockVar = (LongVariable)targetVariable;
         if ("disabled".equalsIgnoreCase(reference)) {
            targetClockVar.setValue(0);
            targetClockVar.setStatus((Status)null);
            targetClockVar.setOrigin("Disabled by "+selectorVar.getName());
            targetClockVar.enable(false);
            return;
         }
         String data[] = reference.split(",");

         // Simple math operations
         Variable     clockSourceVar = safeGetVariable(data[0]);     
         long         value          = clockSourceVar.getValueAsLong();
         String       origin         = clockSourceVar.getOrigin();

         if (data.length>1) {
            origin = "("+origin+")";
            Pattern p = Pattern.compile("(/|\\*)(\\d+)");
            for (int index=1; index<data.length; index++) {
               Matcher m = p.matcher(data[index]);  
               if (!m.matches()) {
                  throw new Exception("Clock source factor '" + data[1] + "' does not match expected pattern");
               }
               origin = origin+data[index];
               long factor = Long.parseLong(m.group(2));
               switch(m.group(1).charAt(0)) {
               case '/' : value = value / factor; break;
               case '*' : value = value * factor; break;
               }
            }
            origin = origin + " [controlled by "+selectorVar.getName() +"]";
         }
         targetClockVar.setValue(value);
         targetClockVar.setStatus(clockSourceVar.getStatus());
         targetClockVar.setOrigin(origin);
         targetClockVar.enable(clockSourceVar.isEnabled());
         return;
      }
      if (targetVariable instanceof StringVariable) {
         StringVariable targetStringVariable = (StringVariable)targetVariable;
         if ("disabled".equalsIgnoreCase(reference)) {
            targetStringVariable.setValue(0);
            targetStringVariable.setStatus((Status)null);
            targetStringVariable.setOrigin("Disabled by "+selectorVar.getName());
            targetStringVariable.enable(false);
            return;
         }
         Variable sourceVar = safeGetVariable(reference);     
         String data = sourceVar.getValueAsString();
         targetVariable.setValue(data);
      }
   }
   
   protected class ClockSelectorListener implements IModelChangeListener {
      
      final Variable fClockSelector;
      
      public ClockSelectorListener(Variable clockSelector) {
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
   
  /**
   * Add clock selectors and their references to watched variables<br>
   * The clock selectors are obtained from the associated peripheral
   * 
   * @throws Exception
   */
   private void addClockSelectors() throws Exception {
      ArrayList<Variable> clockSelectors = getPeripheral().getClockSelectors();
      if (clockSelectors == null) {
         return;
      }
      HashMap<Variable,ClockSelectorListener> addedClockSelectorListeners = null;
      for (Variable clockSelector:clockSelectors) {
         String targetName = clockSelector.getTarget();
         if (targetName == null) {
            throw new Exception("Clock selector var '"+clockSelector+"' doesn't have target");
         }
         Variable target = safeGetVariable(targetName);
         if (target == null) {
            throw new Exception("Target '"+targetName+"' not found for Clock selector var '"+clockSelector+"' have target");
         }

         if (addedClockSelectorListeners == null) {
            addedClockSelectorListeners = new HashMap<Variable, ClockSelectorListener>();
         }
         
         ClockSelectorListener listener = addedClockSelectorListeners.get(clockSelector);
         if (listener == null) {
            listener = new ClockSelectorListener(clockSelector);
            addedClockSelectorListeners.put(clockSelector, listener);
         }
         
         // Watch clock selector
         clockSelector.addListener(listener);
         if (clockSelector instanceof StringVariable) {
            StringVariable sv = (StringVariable) clockSelector;
            String referenceString = sv.getValueAsString();
            if ((referenceString == null) || (referenceString.isBlank())) {
               throw new Exception("Clock reference is missing for Clock selector var '"+clockSelector+"'");
            }
            if ("disabled".equalsIgnoreCase(referenceString)) {
               continue;
            }
            Variable reference = safeGetVariable(referenceString);
            if (reference == null) {
               throw new Exception("Clock reference variable '"+referenceString+"' not found for Clock selector var '"+clockSelector+"'");
            }
            // Watch references
            reference.addListener(listener);
         }
         else if (clockSelector instanceof VariableWithChoices) {
            // ChoiceVar selecting the clock input
            VariableWithChoices cv = (VariableWithChoices)clockSelector;
            ChoiceData[] choiceDatas = cv.getData();
            for (ChoiceData choiceData:choiceDatas) {
               String referenceString = choiceData.getReference();
               if ((referenceString == null) || (referenceString.isBlank())) {
                  throw new Exception("Clock reference is missing for Clock selector var '"+clockSelector+"'");
               }
               if ("disabled".equalsIgnoreCase(referenceString)) {
                  continue;
               }
               String parts[] = referenceString.split(",");
               Variable reference = safeGetVariable(parts[0]);
               if (reference == null) {
                  throw new Exception("Clock reference variable '"+choiceData.getReference()+"' not found for Clock selector var '"+clockSelector+"' have target");
               }
               // Watch references
               reference.addListener(listener);
            }
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
