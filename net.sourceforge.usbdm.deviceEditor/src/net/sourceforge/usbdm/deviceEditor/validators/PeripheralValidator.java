package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.ChoiceData;
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
    * @param clockSelectorVar This variable is controlling a clock multiplexor
    * 
    * @throws Exception
    */
   private void validateClockSelectorVariable(Variable clockSelectorVar) throws Exception {

      if (clockSelectorVar.getName().contains("PFD clock")) {
         System.err.println("Validating cs '"+clockSelectorVar+"'");
      }
      LongVariable targetClockVar = safeGetLongVariable(clockSelectorVar.getTarget());

      // Get clock source (selected input)
      String reference = null;
      if (clockSelectorVar instanceof StringVariable) {
         // String variable with name of clock being used.
         reference = clockSelectorVar.getValueAsString();
      }
      else if (clockSelectorVar instanceof ChoiceVariable) {
         // ChoiceVar selecting the clock input
         ChoiceVariable cv = (ChoiceVariable)clockSelectorVar;
         ChoiceData[] choiceData = cv.getData();
         int index = (int)cv.getValueAsLong();
         if (index<0) {
            cv.getValueAsLong();
            index = 0;
         }
         reference = choiceData[index].getReference();
      }
      else {
         throw new Exception("Clock source control variable not of correct type" + clockSelectorVar);
      }
      if ("disabled".equalsIgnoreCase(reference)) {
         targetClockVar.setValue(0);
         targetClockVar.setStatus((Status)null);
         targetClockVar.setOrigin("Disabled");
         return;
      }
      String data[] = reference.split(",");
      
      Variable     clockSourceVar = safeGetVariable(data[0]);     
      long         value          = clockSourceVar.getValueAsLong();
      String       origin         = clockSourceVar.getOrigin();
      
      if (data.length>1) {
         Pattern p = Pattern.compile("\\=([a-z]\\w+)");
         Matcher m = p.matcher(data[1]);  
         if (m.matches()) {
            if (m.group(1).equalsIgnoreCase("clkdiv2")) {
               clkdiv2(clockSourceVar, targetClockVar);
               return;
            }
            if (m.group(1).equalsIgnoreCase("clkdiv3")) {
               clkdiv2(clockSourceVar, targetClockVar);
               return;
            }
            throw new Exception("Clock source factor '" + data[1] + "' does not match expected pattern");
         }
         origin = "("+origin+")";
         p = Pattern.compile("(/|\\*)(\\d+)");
         for (int index=1; index<data.length; index++) {
            m = p.matcher(data[index]);  
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
      }
      targetClockVar.setValue(value);
      targetClockVar.setStatus(clockSourceVar.getStatus());
      targetClockVar.setOrigin(origin);
   }
   
   void clkdiv2(Variable clockSourceVar, LongVariable targetClockVar) {

      final Variable sim_clkdiv2_usbVar = safeGetVariable("sim_clkdiv2_usb");

      // Peripheral Clock / CLKDIV2
      int  fracDiv = Long.decode(sim_clkdiv2_usbVar.getSubstitutionValue()).intValue();
      int  frac    = fracDiv&0x1;
      int  div     = (fracDiv>>1)&0x7;

      long   frequency = clockSourceVar.getValueAsLong()*(frac+1)/(div+1);
      String origin    = clockSourceVar.getOrigin() + " after /CLKDIV2";
      
      Status status = clockSourceVar.getStatus();
      if (frequency != 48000000) {
         status = new Status("Illegal clock frequecy for USB", Severity.WARNING);
      }
      targetClockVar.setValue(frequency);
      targetClockVar.setStatus(status);
      targetClockVar.setOrigin(origin);
   }
   
   /**
    * Update system_peripheral_postdivider_clock
    * 
    * @param clockVar Active peripheral clock
    * 
    * @return system_peripheral_postdivider_clock if present or clockVar unchanged if not
    * 
    * @throws Exception
    */
   void clkdiv3(Variable clockSourceVar, LongVariable targetClockVar) throws Exception {

      final  Variable sim_clkdiv3_pllfllVar = getVariable("sim_clkdiv3_pllfll");
      
      int    fracDiv = Long.decode(sim_clkdiv3_pllfllVar.getSubstitutionValue()).intValue();
      int    frac    = fracDiv&0x1;
      int    div     = (fracDiv>>1)&0x7;
      Long   value   = (clockSourceVar.getValueAsLong()*(frac+1))/(div+1);
      String origin  = clockSourceVar.getOrigin() + " after /CLKDIV3";
      Status status = clockSourceVar.getStatus();

      targetClockVar.setValue(value);
      targetClockVar.setOrigin(origin);
      targetClockVar.setStatus(status);
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
//          System.err.println("Validating "+fClockSelector);
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

         if (clockSelector instanceof ChoiceVariable) {
            // ChoiceVar selecting the clock input
            ChoiceVariable cv = (ChoiceVariable)clockSelector;
            ChoiceData[] choiceDatas = cv.getData();
            for (ChoiceData choiceData:choiceDatas) {
               String referenceString = choiceData.getReference();
               if (referenceString == null) {
                  throw new Exception("Clock reference is missing for Clock selector var '"+clockSelector+"' have target");
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
      addToWatchedVariables(getPeripheral().getDepenedencies());
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
