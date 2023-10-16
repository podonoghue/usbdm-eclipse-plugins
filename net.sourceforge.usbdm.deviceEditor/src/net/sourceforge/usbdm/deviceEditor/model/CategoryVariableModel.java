package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

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

   @Override
   public void modelElementChanged(ObservableModel observableModel) {

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
      super.modelElementChanged(observableModel);
   }

}
