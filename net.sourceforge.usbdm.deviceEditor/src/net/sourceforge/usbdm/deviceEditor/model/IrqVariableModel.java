package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.IrqDialogue;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;

public class IrqVariableModel extends VariableModel {

//   OwnerDrawLabelProvider x;
   
   static class IrqEditor extends DialogCellEditor {
      final IrqVariableModel fModel;

      public IrqEditor(Tree tree, IrqVariableModel model) {
         super(tree, SWT.NONE);
         fModel = model;
      }

      @Override
      protected Object openDialogBox(Control paramControl) {
         IrqVariable var = fModel.getVariable();
         IrqDialogue dialog = new IrqDialogue(paramControl.getShell(), var.getPersistentValue());
         dialog.setTitle(var.getDescription());
         if (dialog.open() == Window.OK) {
            return dialog.getResult();
         };
         return null;
      }
   }

   /**
    * 
    * @param parent        Parent model
    * @param provider      Associated variable provider
    * @param key           Variable key
    * @param description   Description for model
    * 
    * @note Added as child of parent if not null
    */
   public IrqVariableModel(BaseModel parent, IrqVariable variable) {
      super(parent, variable);
   }
   
   @Override
   public IrqVariable getVariable() {
      return (IrqVariable)super.getVariable();
   }

   @Override
   public void setValueAsString(String value) {
      getVariable().setValue(value);
   }

   @Override
   public String getValueAsString() {
      return getVariable().getValueAsString();
   }

   @Override
   Status getStatus() {
      Status msg = super.getStatus();
      if ((msg != null) && msg.greaterThan(Severity.WARNING)) {
         return msg;
      }
      String message = isValid(getValueAsString());
      if (message != null) {
         msg = new Status(message);
      }
      return msg;
   }

   @Override
   public String getToolTip() {
      StringBuffer sb = new StringBuffer();
      sb.append(super.getToolTip());
      return (sb.length() == 0)?null:sb.toString();
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new IrqEditor(tree, this);
   }

   
}
