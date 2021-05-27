package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.Vector;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
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
      validateInterrupt(variable);
   }

   /**
    * Validates the interrupt portion of the dialogue
    * 
    * @param variable   Variable trigger change leading to validation (may be null)
    * 
    * @throws Exception
    */
   protected void validateInterrupt(Variable variable) throws Exception {
      
//      ChoiceVariable irqHandlingMethodVar         = safeGetChoiceVariable("irqHandlingMethod");
//      StringVariable namedInterruptHandlerVar     = safeGetStringVariable("namedInterruptHandler");
//      LongVariable   irqLevelVar                  = safeGetLongVariable("irqLevel");
//
//     if (irqHandlingMethodVar == null) {
//        return;
//     }
//     
//     switch((int)irqHandlingMethodVar.getValueAsLong()) {
//     default:
//        irqHandlingMethodVar.setValue(0);
//     case 0: // No handler
//        namedInterruptHandlerVar.enable(false);
//        namedInterruptHandlerVar.setOrigin("Disabled by irqHandlingMethod");
//        irqLevelVar.enable(false);
//        irqLevelVar.setOrigin("Disabled by irqHandlingMethod");
//        break;
//     case 1: // Software (Use setCallback() or class method)
//        namedInterruptHandlerVar.enable(false);
//        namedInterruptHandlerVar.setOrigin("Disabled by irqHandlingMethod");
//        irqLevelVar.enable(true);
//        irqLevelVar.setOrigin(null);
//        break;
//     case 2: // Named function
//        namedInterruptHandlerVar.enable(true);
//        namedInterruptHandlerVar.setOrigin(null);
//        namedInterruptHandlerVar.setStatus(isValidCIdentifier(namedInterruptHandlerVar.getValueAsString()));
//        irqLevelVar.enable(true);
//        irqLevelVar.setOrigin(null);
//        break;
//    }
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
    * @param externalVariables Variables to add
    */
   protected void addToWatchedVariables(String[] externalVariables) {
      for(fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
         for (String name:externalVariables) {
            addToWatchedVariables(name);
         }
      }
      fIndex = 0;
   }

   @Override
   protected void createDependencies() throws Exception {
      // Assume no external dependencies
   }

   /**
    * Checks if signals are mapped to pins<br>
    * If not then a warning is attached to pins category
    * 
    * @param  requiredSignals Array of required signals as index into the signal table
    * @param  table           Peripheral signal table to use
    * 
    * @throws Exception
    */
   protected void validateMappedPins(int requiredSignals[], Vector<Signal> table) throws Exception {

      CategoryModel pinModel = getPeripheral().getPinModel();
      Status unmappedSignals = null;
      for (int pinNum:requiredSignals) {
         Signal signal = table.get(pinNum);
         if ((signal == null) || (signal.getMappedPin().getPin() == Pin.UNASSIGNED_PIN)) {
            unmappedSignals = UNMAPPED_PIN_STATUS;
            break;
         }
      }
      pinModel.setStatus(unmappedSignals);
      pinModel.update();
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
