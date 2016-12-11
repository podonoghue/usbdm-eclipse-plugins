package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.NumericListDialogue;
import net.sourceforge.usbdm.deviceEditor.information.NumericListVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class NumericListVariableModel extends StringVariableModel {

   public NumericListVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }

   static class IntegerListEditor extends DialogCellEditor {
      final NumericListVariableModel fModel;
      
      public IntegerListEditor(Tree tree, NumericListVariableModel numericListVariableModel) {
         super(tree, SWT.NONE);
         fModel = numericListVariableModel;
      }

      @Override
      protected Object openDialogBox(Control paramControl) {
         NumericListVariable var  = (NumericListVariable) fModel.getVariable();
         NumericListDialogue dialog = new NumericListDialogue(paramControl.getShell(), var.getMaxListSize(), (int)var.getMax(), fModel.getValueAsString());
//         CheckBoxListDialogue dialog = new CheckBoxListDialogue(paramControl.getShell(), 61, fModel.getValueAsString());
         if (dialog.open() == Window.OK) {
            return dialog.getResult();
         };
         return null;
      }
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new IntegerListEditor(tree, this);
   }

}
