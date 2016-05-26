package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class ChoiceVariableModel extends VariableModel {

   /**
    * Constructor - Create model from variable
    * 
    * @param parent     Parent model
    * @param variable   Variable being modelled
    */
   public ChoiceVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
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

   @Override
   protected void removeMyListeners() {
   }

}
