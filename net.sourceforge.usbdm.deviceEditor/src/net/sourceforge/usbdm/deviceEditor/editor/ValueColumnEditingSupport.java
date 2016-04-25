package net.sourceforge.usbdm.deviceEditor.editor;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BinaryModel;
import net.sourceforge.usbdm.deviceEditor.model.EditableModel;
import net.sourceforge.usbdm.deviceEditor.model.FilePathModel;
import net.sourceforge.usbdm.deviceEditor.model.NumericModel;
import net.sourceforge.usbdm.deviceEditor.model.SelectionModel;

public class ValueColumnEditingSupport extends EditingSupport {

   private TreeViewer viewer;

   public ValueColumnEditingSupport(TreeViewer viewer) {
      super(viewer);
      this.viewer = viewer;
   }

   @Override
   protected boolean canEdit(Object element) {
      if (!(element instanceof BaseModel)) {
         return false;
      }
      BaseModel model = (BaseModel)element;
      return model.canEdit();
   }

   @Override
   protected CellEditor getCellEditor(Object element) {
      if (element instanceof BinaryModel) {
         return new BooleanCellEditor(viewer.getTree());
      }
      if (element instanceof SelectionModel) {
         SelectionModel model = (SelectionModel)element;

         String[] choices = model.getChoices();
         return new ChoiceCellEditor(viewer.getTree(), choices);
      }      
      if (element instanceof NumericModel) {
         NumericModel model = (NumericModel)element;
         return new NumericTextCellEditor(viewer.getTree(), model.min(), model.max());
      }      
      if (element instanceof FilePathModel) {
         return new HardwareCellEditor(viewer.getTree());
      }
      return null;
   }

   @Override
   protected Object getValue(Object element) {
      if (element instanceof BinaryModel) {
         BinaryModel model = (BinaryModel)element;
         boolean rv = model.getBooleanValue();
         return rv;
      }
      if (element instanceof BaseModel) {
         BaseModel model = (BaseModel)element;
         return model.getValueAsString();
      }
      return "";
   }

   @Override
   protected void setValue(Object element, Object value) {
      if (element instanceof BinaryModel) {
         BinaryModel model = (BinaryModel)element;
         model.setBooleanValue((Boolean) value);
      }
      else if (element instanceof EditableModel) {
         EditableModel model = (EditableModel)element;
         model.setValueAsString((String) value);
      }
      viewer.update(element, null);
   }

   static class NumericTextCellEditor extends TextCellEditor {

      class Validator implements ICellEditorValidator {
         
         long min;
         long max;
         
         Validator(long min, long max) {
            this.min = min;
            this.max = max;
         }
         
         @Override
         public String isValid(Object value) {
            String rv = null;
            long   lValue;
            try {
               String s = value.toString().trim();
               if (s.startsWith("0b")) {
                  lValue = Long.parseLong(s.substring(2, s.length()), 2);
               } else {
                  lValue = Long.decode(s);
               }
               if ((lValue<min) || (lValue>max)) {
                  rv = "Value out of range";
               }
            }
            catch (NumberFormatException e) {
               rv = "Illegal number";
            }
            return rv;
         }
      }
      
      public NumericTextCellEditor(Composite parent, long min, long max) {
         super(parent, SWT.SINGLE);
         setValueValid(true);
         setValidator(new Validator(min, max));
      }

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
         super.doSetValue(value);
      }
   }
   
   static class ChoiceCellEditor extends ComboBoxCellEditor {
      public ChoiceCellEditor(Composite tree, String[] choices) {
         super(tree, choices, SWT.READ_ONLY);
         setValueValid(true);
      }
      
      @Override
      protected Object doGetValue() {
         int index = (Integer) super.doGetValue();
         String[] items = getItems();
         if ((index<0) || (index>=items.length)) {
            index = 0;
         }
         String item = items[index];
         return item;
      }
      
      @Override
      protected void doSetValue(Object value) {
         String[] items = getItems();
         for (int index=0; index<items.length; index++) {
            if (items[index].equalsIgnoreCase(value.toString())) {
               super.doSetValue(index);
               return;
            }
         }
      }
   }

   static class HardwareCellEditor extends DialogCellEditor {

      String currentPath = null;
      
      HardwareCellEditor(Composite parent) {
         super(parent, SWT.NONE);
      }
      
      @Override
      protected void doSetValue(Object value) {
         currentPath = (String) value;
         super.doSetValue(value);
      }

      @Override
      protected Object openDialogBox(Control paramControl) { 
         FileDialog dialog = new FileDialog(paramControl.getShell(), SWT.OPEN);
         dialog.setFilterExtensions(new String [] {"*"+DeviceInfo.HARDWARE_FILE_EXTENSION});
         Path path = Paths.get(currentPath).toAbsolutePath();
         dialog.setFilterPath(path.getParent().toString());
         dialog.setFileName(path.getFileName().toString());
         return dialog.open();
      }
      
   }
}