package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class BooleanVariableModel extends VariableModel {

   static class BooleanCellEditor extends CheckboxCellEditor {
      public BooleanCellEditor(Tree tree) {
         super(tree);
         setValueValid(true);
      }

      @Override
      protected Object doGetValue() {
         Boolean value = (Boolean) super.doGetValue();
         return value;
      }

      @Override
      protected void doSetValue(Object value) {
         super.doSetValue(value);
      }
   }
   
   /**
    * Constructor - Create model from variable
    * 
    * @param parent     Parent model
    * @param variable   Variable being modelled
    */
   public BooleanVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }

   @Override
   public BooleanVariable getVariable() {
      return (BooleanVariable) super.getVariable();
   }
   
   /**
    * Get value as boolean
    * 
    * @return
    */
   public Boolean getValueAsBoolean() {
      return getVariable().getValueAsBoolean();
   }

   /**
    * Set value as boolean
    * 
    * @param value
    */
   public void setBooleanValue(Boolean bValue) {
      getVariable().setValue(bValue);
      
      // Refresh children in case boolean category
      refreshChildren();
   }
   
   /**
    * Refreshes children
    */
   private void refreshChildren() {
      if (fChildren != null) {
         for (Object obj:fChildren) {
            if (obj instanceof VariableModel) {
               VariableModel child = (VariableModel) obj;
               child.update();
            }
         }
      }
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new BooleanCellEditor(tree);
   }
   
}
