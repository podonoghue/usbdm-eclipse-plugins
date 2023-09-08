package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class PeripheralValidator extends Validator {

   protected static final Status UNMAPPED_PIN_STATUS = new Status("Not all common signals are mapped to pins", Severity.WARNING);

   public enum McgClockMode {
      McgClockMode_FEI,       McgClockMode_FEE,       McgClockMode_FBI,
      McgClockMode_FBE,       McgClockMode_PBE,       McgClockMode_PEE,         McgClockMode_BLPI, McgClockMode_BLPE,
      McgClockMode_LIRC_8MHz, McgClockMode_LIRC_2MHz, McgClockMode_HIRC_48MHz,  McgClockMode_EXT,
      McgClockMode_SOSC,      McgClockMode_SIRC,      McgClockMode_FIRC,        McgClockMode_SPLL }

   public enum SmcRunMode {
      SmcRunMode_Normal, SmcRunMode_VeryLowPower, SmcRunMode_HighSpeed,  }

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
   protected PeripheralWithState getPeripheral() {
      return (PeripheralWithState) super.getPeripheral();
   }

   @Override
   protected void validate(Variable variable) throws Exception {
   }

   /**
    * Add to watched variables
    * 
    * @param externalVariables Variables to add
    */
   protected void addToWatchedVariables(String name) {
      
      Variable var = safeGetVariable(name);
      if (var == null) {
//         System.err.println("Failed to watch variable " + name + " in validator " + getClass().getSimpleName());
      }
      else {
         var.addListener(this);
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
      for (String name:variablesToWatch) {
         addToWatchedVariables(name);
      }
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
      for (String name:variablesToWatch) {
         addToWatchedVariables(name);
      }
   }

   /**
    * Add this validator as a listener on each variable in list
    * 
    * @param variablesToWatch
    */
   protected void addSpecificWatchedVariables(String[] variablesToWatch) {

      for (String varName:variablesToWatch) {
         Variable var = safeGetVariable(varName);
         if (var != null) {
            var.addListener(this);
         }
      }
   }
   
   /**
    * Add this validator as a listener on each variable in list
    * 
    * @param variablesToWatch
    */
   protected void addSpecificWatchedVariables(ArrayList<String> variablesToWatch) {

      for (String varName:variablesToWatch) {
         Variable var = safeGetVariable(varName);
         if (var != null) {
            var.addListener(this);
         }
      }
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
   protected LongVariable getLongVariableIndirect(String indirectName, ArrayList<String> namesToWatch) throws Exception {

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
   protected LongVariable getLongVariable(String targetName, ArrayList<String> namesToWatch) throws Exception {

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
    * 
    * @throws Exception if target variable doesn't exist
    */
   protected DoubleVariable getDoubleVariable(String targetName, ArrayList<String> namesToWatch) throws Exception {

      DoubleVariable reference = getDoubleVariable(targetName);
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
    * 
    * @throws Exception if target variable doesn't exist
    */
   protected ChoiceVariable getChoiceVariable(String targetName, ArrayList<String> namesToWatch) throws Exception {

      ChoiceVariable reference = getChoiceVariable(targetName);
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
   protected BooleanVariable safeGetBooleanVariable(String targetName, ArrayList<String> namesToWatch) {

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
   protected BooleanVariable getBooleanVariable(String targetName, ArrayList<String> namesToWatch) throws Exception {

      BooleanVariable reference = getBooleanVariable(targetName);
      namesToWatch.add(targetName);
      return reference;
   }

   @Override
   protected boolean createDependencies() throws Exception {
      return false;
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
