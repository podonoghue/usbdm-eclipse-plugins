package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class ChoiceVariableModel extends VariableModel {

   /**
    * Constructor - Create model from variable
    * 
    * @param parent     Parent model
    * @param variable   Variable being modelled
    */
   public ChoiceVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }

   @Override
   public ChoiceVariable getVariable() {
      return (ChoiceVariable) super.getVariable();
   }

   /**
    * Get array of selection choices
    * 
    * @return The array of choices displayed to user
    */
   public String[] getChoices() {
      return getVariable().getVisibleChoiceNames();
   }

   @Override
   protected void removeMyListeners() {
   }

   static class ChoiceCellEditor extends ComboBoxCellEditor {
      ChoiceVariable fVariable;
      
      public ChoiceCellEditor(Composite tree, String[] choices, ChoiceVariable variable) {
         super(tree, choices, SWT.READ_ONLY);
         fVariable = variable;
         setActivationStyle(
               ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION |
               ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
         setValue(variable.getValueAsString());
         setValueValid(true);
      }

      @Override
      protected Object doGetValue() {
         int index = (Integer) super.doGetValue();
         String[] items = getItems();
         if ((index<0) || (index>=items.length)) {
            index = 0;
         }
         String item = items[index];
         return item;
      }

      @Override
      protected void doSetValue(Object value) {
         String[] items = getItems();
         for (int index=0; index<items.length; index++) {
            if (items[index].equalsIgnoreCase(value.toString())) {
               super.doSetValue(index);
               return;
            }
         }
      }
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new ChoiceCellEditor(tree, getChoices(), getVariable());
   }
   
   @Override
   public String getValueAsString() {
      String displayValue = getVariable().getDisplayValue();
      if (displayValue != null) {
         return displayValue;
      }
      return super.getValueAsString();
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      // TODO Auto-generated method stub
      return super.clone();
   }

}
