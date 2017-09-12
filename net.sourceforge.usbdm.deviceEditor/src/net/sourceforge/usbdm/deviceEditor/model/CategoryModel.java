package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Simple model that only provides a passive holder in the tree
 */
public class CategoryModel extends BaseModel {
   private final  int fDimension;
   
   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param name          Display name
    * @param description   Display description
    * 
    * @note Added as child of parent if not null
    */
   public CategoryModel(BaseModel parent, String name, String description, int dimension) {
      super(parent, name, description);
      fDimension = dimension;
   }

   public CategoryModel(BaseModel parent, String name, String description) {
      super(parent, name, description);
      fDimension = 1;
   }

   @Override
   protected void removeMyListeners() {
   }

   public int getDimension() {
      return fDimension;
   }
}
