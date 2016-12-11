package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;

public class LongVariableModel extends VariableModel {

   static class NumericTextCellEditor extends TextCellEditor {

      class Validator implements ICellEditorValidator {
         LongVariableModel fModel;
         
         Validator(LongVariableModel model) {
            fModel = model;
         }
         
         @Override
         public String isValid(Object value) {
            return fModel.isValid(value.toString());
         }
      }
      
      public NumericTextCellEditor(Tree parent, LongVariableModel model) {
         super(parent, SWT.SINGLE);
         setValueValid(true);
         Validator validator =  new Validator(model);
         setValidator(validator);
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
   public LongVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }
   
   @Override
   public LongVariable getVariable() {
      return (LongVariable)super.getVariable();
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
      return getVariable().getValueAsString(value);
   }

   @Override
   public String getValueAsString() {
      return getVariable().getValueAsString();
   }

   @Override
   Message getMessage() {
      Message msg = super.getMessage();
      if ((msg != null) && msg.greaterThan(Severity.WARNING)) {
         return msg;
      }
      String message = isValid(getValueAsString());
      if (message != null) {
         msg = new Message(message);
      }
      return msg;
   }

   @Override
   public String getToolTip() {
      StringBuffer sb = new StringBuffer();
      sb.append(super.getToolTip());
      boolean newLineNeeded = sb.length()>0;
      
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
      return new NumericTextCellEditor(tree, this);
   }
   
}
