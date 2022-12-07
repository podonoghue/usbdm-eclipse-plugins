package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to validate LLWU settings
 */
public class LlwuValidate extends PeripheralValidator {
   boolean donePinNames = false;
   
   public LlwuValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Converts a string to Capital-case by removing spaces and capitalising the next character
    * 
    * @param name Name to convert
    * 
    * @return Converted name e.g. "HELLO   there" => "HelloThere"
    */
   private String capitalCase(String name) {
      if (name == null) {
         return null;
      }
      StringBuilder sb = new StringBuilder();
      boolean skippingSpace = true;
      for (int index=0; index<name.length(); index++) {
         char ch = name.charAt(index);
         if (!Character.isAlphabetic(ch) && !Character.isDigit(ch) ){
            ch = ' ';
         }
         if (Character.isWhitespace(ch)) {
            skippingSpace = true;
            continue;
         }
         if (skippingSpace) {
            ch = Character.toUpperCase(ch);
         }
         else {
            ch = Character.toLowerCase(ch);
         }
         sb.append(ch);
         skippingSpace = false;
      }
      return sb.toString();
   }
   
   /**
    * Create LLWU peripheral C enum table values<br>
    * 
    * Tables are added to the following Peripheral Variables:
    *  <li>InputPeripherals
    */
   private void doPeripheralNames() {
      if (donePinNames) {
         return;
      }
      donePinNames = true;
      StringBuilder sb = new StringBuilder();

      sb = new StringBuilder();
      for (int index=0; index<=7; index++) {
         String         choiceName = "llwu_me_wume"+index;
         BooleanVariable choiceVar  = safeGetBooleanVariable(choiceName);
         if (choiceVar != null) {
            String llwuPeripheralName = capitalCase(choiceVar.getDescription());
            String llwuPeripheralLine = String.format(
                  "   LlwuPeripheral_%-15s = LlwuPeripheral_%d, //!< %s wake-up\n",
                  llwuPeripheralName, index, choiceVar.getDescription()
                  );
            sb.append(llwuPeripheralLine);
         }
      }
      StringVariable llwuPeripheralsVar = new StringVariable("InputPeripherals", getPeripheral().makeKey("InputPeripherals"));
      llwuPeripheralsVar.setValue(sb.toString());
      llwuPeripheralsVar.setDerived(true);
      getPeripheral().addVariable(llwuPeripheralsVar);
   }
   
   /**
    * Class to validate LLWU settings
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      doPeripheralNames();
      
      for (int index=0; index<8; index++) {
         // Filter variables
         ChoiceVariable   llwu_filt_filteVar     = safeGetChoiceVariable("llwu_filt"+index+"_filte");
         ChoiceVariable   llwu_filt_filtselVar   = safeGetChoiceVariable("llwu_filt"+index+"_filtsel");
         if ((llwu_filt_filteVar != null) && ((variable == null) || (variable == llwu_filt_filteVar))) {
            llwu_filt_filtselVar.enable(llwu_filt_filteVar.getValueAsLong() != 0);
         }
      }
   }
   
   @Override
   protected boolean createDependencies() throws Exception {
      return super.createDependencies();
      // No external dependencies
   }
}