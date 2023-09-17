package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
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
    * Get the index of the selected value from choices
    * 
    * @return Index of choice
    */
   public int getSelection() {
      return fSelection;
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
      // value may be from a check-box (true/false) or a selection (string from selection chosen)
      
//      System.err.println("setValueAsString(" + value + ")");

      fSelection = findChoiceIndex(value);
      if (fSelection<0) {
         fSelection = 0;
      }
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

   static class BooleanCellEditor extends CheckboxCellEditor {
      
      // The selection model owning this editor
      final SelectionModel fModel;

      public BooleanCellEditor(Tree tree, SelectionModel model) {
         super(tree);
         setValueValid(true);
         fModel = model;
      }

      @Override
      protected Object doGetValue() {
         // Boolean => choice (String) => setValueAsString()
         Boolean value = (Boolean) super.doGetValue();
//         System.err.println("doGetValue()" + value + "=>" + value);
         return fModel.getChoices()[value?1:0];
      }

      @Override
      protected void doSetValue(Object value) {
         // GUI value (String) => boolean
         int index = fModel.findChoiceIndex(value.toString());
         Boolean bValue = index > 0;
//         System.err.println("doSetValue(" + value + "=>" + bValue + ")");
         super.doSetValue(bValue);
      }
   }

   static class ChoiceCellEditor extends ComboBoxCellEditor {

      // The selection model owning this editor
      final SelectionModel fModel;

      public ChoiceCellEditor(Composite tree, SelectionModel model) {
         super(tree, model.getChoices(), SWT.READ_ONLY);
         fModel = model;
         setActivationStyle(
               ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION |
               ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
         setValueValid(true);
      }

      @Override
      protected Object doGetValue() {
         // Index(int) => choice (String) => setValueAsString()
         int index = (Integer) super.doGetValue();
         String item = getItems()[index];
//         System.err.println("doGetValue()" + index + "=>" + item);
         return item;
      }

      @Override
      protected void doSetValue(Object value) {
         // GUI value (String) => index
         int index = fModel.findChoiceIndex(value.toString());
//         System.err.println("doSetValue(" + value + ")");
         super.doSetValue(index);
      }
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      if (getChoices().length == 2) {
         return new BooleanCellEditor(tree, this);
      }
      return new ChoiceCellEditor(tree, this);
   }

}
