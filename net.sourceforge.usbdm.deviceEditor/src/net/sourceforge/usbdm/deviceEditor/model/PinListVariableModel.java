package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.PinListDialogue;
import net.sourceforge.usbdm.deviceEditor.information.PinListVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class PinListVariableModel extends StringVariableModel {

   public PinListVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }

   static class PinListEditor extends DialogCellEditor {
      final PinListVariableModel fModel;
      
      public PinListEditor(Tree tree, PinListVariableModel numericListVariableModel) {
         super(tree, SWT.NONE);
         fModel = numericListVariableModel;
      }

      @Override
      protected Object openDialogBox(Control paramControl) {
         PinListVariable var  = (PinListVariable) fModel.getVariable();
         PinListDialogue dialog = new PinListDialogue(paramControl.getShell(), var, fModel.getValueAsString());
         if (dialog.open() == Window.OK) {
            return dialog.getResult();
         };
         return null;
      }
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new PinListEditor(tree, this);
   }

}
