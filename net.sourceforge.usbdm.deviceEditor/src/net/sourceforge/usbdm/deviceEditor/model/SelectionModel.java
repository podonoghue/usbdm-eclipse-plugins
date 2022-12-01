package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.CellEditorProvider;

public abstract class SelectionModel extends EditableModel implements CellEditorProvider {

   /** List of selection values */
   protected String[] fChoices = null;

   /** Current selection index */
   protected int fSelection = 0;
   
   public SelectionModel(BaseModel parent, String name) {
      super(parent, name);
   }

   /**
    * Get array of selection choices
    * 
    * @return The array of choices displayed to user
    */
   public String[] getChoices() {
      return fChoices;
   }

   @Override
   public boolean canEdit() {
      return super.canEdit() && (fChoices.length>1);
   }

   /**
    * Set the selected value from choices
    * 
    * @param selection Index of choice
    */
   void setSelection(int selection) {
      if (selection<fChoices.length) {
         fSelection = selection;
      }
   }
   
   @Override
   public String getValueAsString() {
      try {
         return fChoices[fSelection];
      }
      catch (Exception e) {
         return "Illegal current selection!!";
      }
   }
   
   @Override
   public void setValueAsString(String value) {
      fSelection = findChoiceIndex(value);
      if (fSelection<0) {
         // Invalid - reset to first element
         fSelection = 0;
      }
      return;
   }

   /**
    * Finds the given choice in fChoices
    * 
    * @param fValue Choice to look for
    * 
    * @return Selection index or -1 if not found
    */
   protected int findChoiceIndex(String choice) {
      for (int index=0; index<fChoices.length; index++) {
         if (fChoices[index].equalsIgnoreCase(choice)) {
            return index;
         }
      }
      return -1;
   }

   static class ChoiceCellEditor extends ComboBoxCellEditor {
      public ChoiceCellEditor(Composite tree, String[] choices) {
         super(tree, choices, SWT.READ_ONLY);
         setActivationStyle(
               ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION |
               ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
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
      return new ChoiceCellEditor(tree, getChoices());
   }

}
