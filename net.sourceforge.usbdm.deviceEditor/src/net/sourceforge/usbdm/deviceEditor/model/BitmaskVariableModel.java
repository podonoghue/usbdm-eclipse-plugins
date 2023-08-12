package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.BitmaskDialogue;
import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;

public class BitmaskVariableModel extends VariableModel {

   static class BitmaskEditor extends DialogCellEditor {
      final BitmaskVariableModel fModel;

      public BitmaskEditor(Tree tree, BitmaskVariableModel model) {
         super(tree, SWT.NONE);
         fModel = model;
      }

      @Override
      protected Object openDialogBox(Control paramControl) {
         BitmaskVariable var = fModel.getVariable();
         BitmaskDialogue dialog = new BitmaskDialogue(paramControl.getShell(), var.getPermittedBits(), var.getValueAsLong(), (long)var.getDefault());
         dialog.setBitNameList(var.getBitList());
         dialog.setTitle(var.getDescription());
         if (dialog.open() == Window.OK) {
            return Long.toString(dialog.getResult());
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
   public BitmaskVariableModel(BaseModel parent, BitmaskVariable variable) {
      super(parent, variable);
   }

   @Override
   public BitmaskVariable getVariable() {
      return (BitmaskVariable)super.getVariable();
   }

   @Override
   public void setValueAsString(String value) {
      getVariable().setValue(value);
   }

   /**
    * Converts the given string into a form appropriate for this  model
    * 
    * @param value Value to format
    * 
    * @return String in appropriate form e.g. 24.56MHz
    */
   private String getValueAsString(long value) {
      return getVariable().formatValueAsString(value);
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
      boolean newLineNeeded = sb.length()>0;

      try {
         if (getVariable().getMin() != Long.MIN_VALUE) {
            if (newLineNeeded) {
               sb.append("\n");
               newLineNeeded = false;
            }
            sb.append("min="+getValueAsString(getVariable().getMin())+" ");
         }
         if (getVariable().getMax() != Long.MAX_VALUE) {
            if (newLineNeeded) {
               sb.append("\n");
               newLineNeeded = false;
            }
            sb.append("max="+getValueAsString(getVariable().getMax())+" ");
         }
      } catch (Exception e) {
         return "Range evaluation error";
      }
      if (getVariable().getStep() != 1) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("step="+getValueAsString(getVariable().getStep())+" ");
      }
      return (sb.length() == 0)?null:sb.toString();
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new BitmaskEditor(tree, this);
   }

}
