package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

import org.eclipse.jface.viewers.StructuredViewer;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class ListVariableModel extends StringVariableModel {

   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param variable      Variable associated with this model
    */
   public ListVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }

   @Override
   protected void removeMyListeners() {
   }

   /**
    * Adds the List children as children of parent
    * Note that the list retains the children as well but is no longer their 'parent'
    * 
    * @param parent Parent to adopt children
    */
   public void addChildrenToParent(BaseModel parent) {
      if (fChildren == null) {
         return;
      }
      for (Object child : fChildren) {
         BaseModel ch = (BaseModel) child;
         parent.addChild(ch);
         ch.setParent(parent);
      }
   }
   
   @Override
   public void modelElementChanged(ObservableModelInterface observableModel, String[] properties) {

      ArrayList<BaseModel> children = getChildren();
      if (children != null) {
         boolean isHidden  = isHidden();
         boolean isEnabled = isEnabled();
//         System.err.println("============= hidden = " + isHidden);
         for (BaseModel child:children) {
            if (child instanceof AliasPlaceholderModel) {
               AliasPlaceholderModel aph = (AliasPlaceholderModel) child;
               child = aph.getRealModel();
               if (child == null) {
                  System.err.println("Alias node is empty, "+aph);
                  continue;
               }
            }
            if (child instanceof VariableModel) {
               VariableModel vm = (VariableModel) child;
               Variable var = vm.getVariable();

//               if (vm.getName().contains("ftm_sc_ps")) {
//                  System.err.println("List: vm = "+vm+", hidden (before) = " + vm.isHidden());
//               }
               
               boolean changedToUnknown = false;
               if (isHidden) {
                  // Definitely hidden
                  var.setHidden(true);
               }
               else {
                  // May have become visible
                  changedToUnknown = true;
                  var.setHidden(false);
               }
               if (!isEnabled) {
                  // Definitely disabled
                  var.enable(false);
               }
               else {
                  // May have become enabled
                  changedToUnknown = true;
                  var.enable(true);
               }
               if (changedToUnknown) {
                  // Tell variable to recalculate state
                  var.updateFullyAndNotify();
                  StructuredViewer viewer = vm.getViewer();
                  if (viewer != null) {
                     viewer.refresh(this);
                  }
               }
//               if (vm.getName().contains("ftm_sc_ps")) {
//                  System.err.println("List: vm = "+vm+", hidden (after) = " + vm.isHidden());
//               }
            }
            else {
               child.setHidden(isHidden);
            }
         }
      }
      super.modelElementChanged(observableModel, properties);
   }

   /**
    * Adds the list's children to the parent provided<br>
    * Note the list retains the children as well
    * 
    * @param parent Parent to adopt children
    */
   public void addChildrenTo(BaseModel parent) {
      for (BaseModel child:getChildren()) {
         child.setParent(parent);
      }
   }
   
}
