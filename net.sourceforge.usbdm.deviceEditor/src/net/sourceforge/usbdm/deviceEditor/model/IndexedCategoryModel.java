package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.IndexedCategoryVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class IndexedCategoryModel extends StringVariableModel {

   /** Dimension of the group */
   private int fDimension;

   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param variable      Associated variable
    * @param dimension     Dimension
    * 
    * @note Added as child of parent if not null
    */
   public IndexedCategoryModel(BaseModel parent, Variable variable) {
      super(parent, variable);
      fDimension = 0;
   }

   /**
    * Set dimension of this GroupModel
    * 
    * @param dimension The dminesion to set
    */
   public void setDimension(int dimension) {
      fDimension = dimension;
   }

   /**
    * Get dimension of this GroupModel
    * @return
    */
   public int getDimension() {
      return fDimension;
   }

   @Override
   public IndexedCategoryVariable getVariable() {
      return (IndexedCategoryVariable) super.getVariable();
   }

   @Override
   public IndexedCategoryModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      return (IndexedCategoryModel) super.clone(parentModel, provider, index);
   }
   
}
