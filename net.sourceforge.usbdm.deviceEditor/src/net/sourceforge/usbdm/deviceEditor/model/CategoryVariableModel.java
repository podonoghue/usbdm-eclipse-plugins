package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Simple model that only provides a passive holder in the tree
 */
public class CategoryVariableModel extends StringVariableModel {
   
   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param name          Display name
    * @param description   Display description
    * 
    * @note Added as child of parent if not null
    */
   public CategoryVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public CategoryVariableModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      return (CategoryVariableModel)super.clone(parentModel, provider, index);
   }

}
