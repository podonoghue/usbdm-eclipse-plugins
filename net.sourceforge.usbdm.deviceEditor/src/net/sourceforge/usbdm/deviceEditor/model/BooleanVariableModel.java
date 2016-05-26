package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class BooleanVariableModel extends VariableModel {

   /**
    * Constructor - Create model from variable
    * 
    * @param parent     Parent model
    * @param variable   Variable being modelled
    */
   public BooleanVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }

   @Override
   public BooleanVariable getVariable() {
      return (BooleanVariable) super.getVariable();
   }
   
   /**
    * Get value as boolean
    * 
    * @return
    */
   public Boolean getValueAsBoolean() {
      return getVariable().getValueAsBoolean();
   }

   /**
    * Set value as boolean
    * 
    * @param value
    */
   public void setBooleanValue(Boolean bValue) {
      getVariable().setValue(bValue);
      
      // Refresh children in case boolean category
      refreshChildren();
   }
   
   /**
    * Refreshes children
    */
   private void refreshChildren() {
      if (fChildren != null) {
         for (Object obj:fChildren) {
            if (obj instanceof VariableModel) {
               VariableModel child = (VariableModel) obj;
               child.update();
            }
         }
      }
   }

}
