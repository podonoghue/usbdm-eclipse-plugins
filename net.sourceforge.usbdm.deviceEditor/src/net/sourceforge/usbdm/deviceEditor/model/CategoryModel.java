package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Simple model representing a category in a tree view
 */
public class CategoryModel extends BaseModel {

   /**
    * Construct a simple model representing a category in a tree view
    * 
    * @param parent        Owning model
    * @param name          Name of category
    * @param description   Description of category
    */
   public CategoryModel(BaseModel parent, String name) {
      super(parent, name);
   }
   
   @Override
   protected void removeMyListeners() {
   }

}
