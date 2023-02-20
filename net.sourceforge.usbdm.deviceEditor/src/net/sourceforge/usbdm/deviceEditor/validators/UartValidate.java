package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to validate UART settings
 */
public class UartValidate extends PeripheralValidator {

   public UartValidate(PeripheralWithState peripheral) {
      super(peripheral);
   }

   /**
    * Class to validate UART settings
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);

      // Variables
      //=================================
      BooleanVariable   uartClassVar           =  getBooleanVariable("uartClass");
      LongVariable      receiveBufferSizeVar   =  getLongVariable("receiveBufferSize");
      LongVariable      transmitBufferSizeVar  =  getLongVariable("transmitBufferSize");

      IrqVariable       txrxHandlerVar         =  getIrqVariable("irqHandlingMethod");
      
      // Enable/disable parameters that depend on mode
      boolean uartClass = uartClassVar.getValueAsBoolean();
      
      if (variable == uartClassVar) {
//         System.err.println("uartClassVar = "+uartClassVar.getValueAsBoolean());
         // Changed buffering
         receiveBufferSizeVar.enable(uartClass);
         transmitBufferSizeVar.enable(uartClass);
         txrxHandlerVar.setLocked(uartClass);
         if (uartClass) {
            txrxHandlerVar.setValue(true);
         }
      }
   }
   
   @Override
   protected boolean createDependencies() throws Exception {
      // Don't add default dependencies
      return false;
   }
}