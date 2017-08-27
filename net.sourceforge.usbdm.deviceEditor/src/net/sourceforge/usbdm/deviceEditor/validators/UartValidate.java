package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings

 * Used for:
 *     uart_mk
 */
public class UartValidate extends Validator {

   private boolean addedExternalVariables = false;
   private final static String[] externalVariables = {
   };

   public UartValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to validate UART settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);

//      System.err.println("Uart.validate("+variable+")");
      if (!addedExternalVariables) {
         addToWatchedVariables(externalVariables);
         addedExternalVariables = true;
      }
      // Variables
      //=================================
      BooleanVariable   uartClassVar           =  getBooleanVariable("uartClass");
      LongVariable      receiveBufferSizeVar     =  getLongVariable("receiveBufferSize");
      LongVariable      transmitBufferSizeVar    =  getLongVariable("transmitBufferSize");

      IrqVariable       txrxHandlerVar         =  getIrqVariable("txrxHandler");
      IrqVariable       errorHandler           =  safeGetIrqVariable("errorHandler");
      IrqVariable       lonHandler             =  safeGetIrqVariable("lonHandler");
      
      // Enable/disable parameters that depend on mode
      boolean uartClass = uartClassVar.getValueAsBoolean();
      
      if (variable == uartClassVar) {
//         System.err.println("uartClassVar = "+uartClassVar.getValueAsBoolean());
         // Changed buffering
         receiveBufferSizeVar.enable(uartClass);
         transmitBufferSizeVar.enable(uartClass);
         txrxHandlerVar.setLocked(uartClass);
         if (uartClass) {
            txrxHandlerVar.setValue(IrqVariable.CLASS_VALUE);
         }
         if (errorHandler != null) {
            errorHandler.setLocked(uartClass);
            if (uartClass) {
               errorHandler.setValue(IrqVariable.CLASS_VALUE);
            }
         }
         if (lonHandler != null) {
            lonHandler.setLocked(uartClass);
            if (uartClass) {
               lonHandler.setValue(IrqVariable.CLASS_VALUE);
            }
         }
      }
   }
   
}