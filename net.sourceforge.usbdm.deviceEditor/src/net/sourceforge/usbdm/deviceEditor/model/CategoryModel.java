package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Simple model that only provides a passive holder in the tree
 */
public final class CategoryModel extends BaseModel {

   public CategoryModel(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }

   @Override
   protected void removeMyListeners() {
   }

}
