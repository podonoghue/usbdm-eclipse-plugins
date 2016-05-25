package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class ChoiceVariableModel extends VariableModel {

   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param provider      Provider that owns the variable
    * @param key           Key used to access the variable
    * @param description   Description for the display
    */
   public ChoiceVariableModel(BaseModel parent, Variable variable, String key) {
      super(parent, variable, key);
   }

   @Override
   public ChoiceVariable getVariable() {
      return (ChoiceVariable) super.getVariable();
   }

   /**
    * Get array of selection choices
    * 
    * @return The array of choices displayed to user
    */
   public String[] getChoices() {
      return getVariable().getChoices();
   }

//   /**
//    * Finds the given choice in fChoices
//    * 
//    * @param value Choice to look for
//    * 
//    * @return Selection index or -1 if not found
//    */
//   protected int findChoice(String choice) {
//      for (int index=0; index<fChoices.length; index++) {
//         if (fChoices[index].equalsIgnoreCase(choice)) {
//            return index;
//         }
//      }
//      return -1;
//   }
//   /**
//    * Finds the given value in fValues
//    * 
//    * @param value Value to look for
//    * 
//    * @return Selection index or -1 if not found
//    */
//   protected int findValue(String value) {
//      for (int index=0; index<fValues.length; index++) {
//         if (fValues[index].equalsIgnoreCase(value)) {
//            return index;
//         }
//      }
//      return -1;
//   }

   @Override
   protected void removeMyListeners() {
   }

}
