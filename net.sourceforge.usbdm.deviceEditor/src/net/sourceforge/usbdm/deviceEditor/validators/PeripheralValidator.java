package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class PeripheralValidator extends Validator {

   private boolean fAddedExternalVariables = false;

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
   protected void addToWatchedVariables(String[] externalVariables) {
      if (fAddedExternalVariables) {
         return;
      }
      for(fIndex=0; fIndex<Math.max(1,fDimension); fIndex++) {
         for (String name:externalVariables) {
            Variable var = safeGetVariable(name);
            if (var == null) {
               System.err.println("Failed to watch variable " + name + " in peripheral " + getClass().getName());
            }
            else {
               var.addListener(getPeripheral());
            }
         }
      }
      fAddedExternalVariables = true;
   }
}
