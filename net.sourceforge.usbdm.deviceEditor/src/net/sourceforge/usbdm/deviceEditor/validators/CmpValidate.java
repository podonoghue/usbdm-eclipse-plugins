package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings

 * Used for:
 *     adc0_diff
 */
public class CmpValidate extends PeripheralValidator {

   private ChoiceVariable        cmp_filterVar             = null;
   
   public CmpValidate(PeripheralWithState peripheral) {
      super(peripheral);
   }

   /**
    * Class to determine LPTMR settings
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {

      super.validate(variable);
      cmp_filterVar.getValueAsLong();

   }

   @Override
   protected boolean createDependencies() throws Exception {
      super.createDependencies();
      
//      // Variable to watch
//      ArrayList<String> variablesToWatch = new ArrayList<String>();
//
      cmp_filterVar       = getChoiceVariable("cmp_filter");
//
//      addToWatchedVariables(variablesToWatch);
      
      return false;
   }

}