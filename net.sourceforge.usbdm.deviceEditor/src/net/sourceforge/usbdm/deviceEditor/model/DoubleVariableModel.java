package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class DoubleVariableModel extends VariableModel {
   
   static class DoubleTextCellEditor extends TextCellEditor {

      class Validator implements ICellEditorValidator {
         DoubleVariableModel fModel;
         
         Validator(DoubleVariableModel model) {
            fModel = model;
         }
         
         @Override
         public String isValid(Object value) {
            return fModel.isValid(value.toString());
         }
      }
      
      @Override
      protected Object doGetValue() {
         System.err.println("doGetValue() => "+super.doGetValue());
         return super.doGetValue();
      }

      @Override
      protected void doSetValue(Object value) {
//         System.err.println("doSetValue() => "+value);
         String v = (String) value;
         // The value may contain extra text such as a decimal value for a hex radix field e.g. '0x100 (64)'
         if (v != null) {
            String values[] = v.split(" ", 2);
            value = values[0];
         }
         super.doSetValue(value);
      }
      public DoubleTextCellEditor(Tree parent, DoubleVariableModel model) {
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
   public DoubleVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }
   
   @Override
   public DoubleVariable getVariable() {
      return (DoubleVariable)super.getVariable();
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new DoubleTextCellEditor(tree, this);
   }
   
}
