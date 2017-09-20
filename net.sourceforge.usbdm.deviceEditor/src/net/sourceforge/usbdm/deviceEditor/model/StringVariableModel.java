package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class StringVariableModel extends VariableModel {

   static class StringCellEditor extends TextCellEditor {
      StringVariableModel fModel;

      // Definitely a hack but I can't find a portable method 
      final static String acceptableChars = "\t\f\n\r\b\0";

      @Override
      protected void keyReleaseOccured(KeyEvent keyEvent) {
         if ((acceptableChars.indexOf(keyEvent.character) < 0) &&
             (fModel.isValidKey(keyEvent.character) != null)) {
            keyEvent.doit = false;
         }
         super.keyReleaseOccured(keyEvent);
      }

      public StringCellEditor(Tree parent, StringVariableModel model) {
         super(parent, SWT.SINGLE);
         fModel = model;
         setValueValid(true);
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
   public StringVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new StringCellEditor(tree, this);
   }
}
