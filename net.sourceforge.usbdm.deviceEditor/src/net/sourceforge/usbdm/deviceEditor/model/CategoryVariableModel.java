package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

/**
 * Simple model that only provides a passive holder in the tree
 */
public class CategoryVariableModel extends StringVariableModel {
   
   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param variable      Variable associated with this model
    */
   public CategoryVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public void modelElementChanged(ObservableModelInterface observableModel, int properties) {

      ArrayList<BaseModel> children = getChildren();
      if (children != null) {
         for (BaseModel x:children) {
            if (x instanceof VariableModel) {
               VariableModel vm = (VariableModel) x;
               Variable v = vm.getVariable();
               if (isHidden()) {
                  v.enable(false);
               }
               else {
                  v.expressionChanged(null);
               }
            }
         }
      }
      super.modelElementChanged(observableModel, properties);
   }

   @Override
   public boolean showAsLocked() {
//      return false;
      return !canEdit() && (fVariable.getValueAsString() != null);
   }

}
