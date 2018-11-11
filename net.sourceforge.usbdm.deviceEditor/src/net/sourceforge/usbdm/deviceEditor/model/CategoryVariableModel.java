package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Simple model that only provides a passive holder in the tree
 */
public class CategoryModel extends BaseModel {
   
   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param name          Display name
    * @param description   Display description
    * 
    * @note Added as child of parent if not null
    */
   public CategoryModel(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public CategoryModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      return (CategoryModel)super.clone(parentModel, provider, index);
   }

}
