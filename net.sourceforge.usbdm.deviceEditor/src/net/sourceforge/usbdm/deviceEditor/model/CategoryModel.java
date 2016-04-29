package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Simple model that only provides a passive holder in the tree
 */
public final class CategoryModel extends BaseModel {
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

}
