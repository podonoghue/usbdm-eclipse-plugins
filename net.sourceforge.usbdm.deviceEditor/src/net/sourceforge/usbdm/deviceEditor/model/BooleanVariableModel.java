package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class BooleanVariableModel extends VariableModel {

   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param provider      Provider that owns the variable
    * @param key           Key used to access the variable
    * @param description   Description for the display
    */
   public BooleanVariableModel(BaseModel parent, Variable variable, String key) {
      super(parent, variable, key);
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
