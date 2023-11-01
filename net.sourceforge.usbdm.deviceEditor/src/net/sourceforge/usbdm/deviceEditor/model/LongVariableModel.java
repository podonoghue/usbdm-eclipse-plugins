package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class LongVariableModel extends VariableModel {
   
   static class NumericTextCellEditor extends TextCellEditor {
      final LongVariable fVar;

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
         fVar = model.getVariable();
         setValueValid(true);
         Validator validator =  new Validator(model);
         setValidator(validator);
      }

      @Override
      protected Object doGetValue() {
//         System.err.println("doGetValue() => "+super.doGetValue());
         return super.doGetValue();
      }

      @Override
      protected void doSetValue(Object value) {
//         System.err.println("doSetValue() => "+value);
         super.doSetValue(value);
      }
   }
   
   /**
    * Constructor - Create model from variable
    * 
    * @param parent     Parent model
    * @param variable   Variable being modelled
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
   public CellEditor createCellEditor(Tree tree) {
      return new NumericTextCellEditor(tree, this);
   }
   
}
