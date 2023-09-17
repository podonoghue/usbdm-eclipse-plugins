package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.CellEditorProvider;

public class BooleanModel extends EditableModel implements CellEditorProvider {

   /** Indicates if signal is mapped to pin */
   boolean fIsMapped;
   
   public BooleanModel(BaseModel parent, String name) {
      super(parent, name);
   }

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
         super.doSetValue(value==null);
      }
   }
   
   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new BooleanCellEditor(tree);
   }

   @Override
   public void setValueAsString(String value) {
      fIsMapped = !value.startsWith("U");
   }

}
