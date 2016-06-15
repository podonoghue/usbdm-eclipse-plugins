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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BooleanVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.EditableModel;
import net.sourceforge.usbdm.deviceEditor.model.FilePathModel;
import net.sourceforge.usbdm.deviceEditor.model.LongVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.SelectionModel;
import net.sourceforge.usbdm.deviceEditor.model.StringVariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.ChoiceVariableModel;

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
      if (element instanceof BooleanVariableModel) {
         return new BooleanCellEditor(viewer.getTree());
      }
      if (element instanceof SelectionModel) {
         SelectionModel model = (SelectionModel)element;
         return new ChoiceCellEditor(viewer.getTree(), model.getChoices());
      }      
      if (element instanceof ChoiceVariableModel) {
         ChoiceVariableModel model = (ChoiceVariableModel)element;
         return new ChoiceCellEditor(viewer.getTree(), model.getChoices());
      }      
      if (element instanceof LongVariableModel) {
         LongVariableModel model = (LongVariableModel)element;
         return new NumericTextCellEditor(viewer.getTree(), model);
      }      
      if (element instanceof StringVariableModel) {
         StringVariableModel model = (StringVariableModel)element;
         return new StringCellEditor(viewer.getTree(), model);
      }      
      if (element instanceof FilePathModel) {
         return new HardwareCellEditor(viewer.getTree());
      }
      return null;
   }

   @Override
   protected Object getValue(Object element) {
      if (element instanceof BooleanVariableModel) {
         return ((BooleanVariableModel)element).getValueAsBoolean();
      }
      if (element instanceof BaseModel) {
         return ((BaseModel)element).getValueAsString();
      }
      return "";
   }

   @Override
   protected void setValue(Object element, Object value) {
      if (element instanceof BooleanVariableModel) {
         ((BooleanVariableModel)element).setBooleanValue((Boolean) value);
      }
      else if (element instanceof EditableModel) {
         ((EditableModel)element).setValueAsString((String) value);
      }
      viewer.update(element, null);
   }

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
         setActivationStyle(
               ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION |
               ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
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

//      class Validator implements ICellEditorValidator {
//         @Override
//         public String isValid(Object value) {
//            return fModel.isValid(value.toString());
//         }
//      }
      
      public StringCellEditor(Tree parent, StringVariableModel model) {
         super(parent, SWT.SINGLE);
         fModel = model;
         setValueValid(true);
//         Validator validator =  new Validator();
//         setValidator(validator);
      }
   }
   
   static class IntegerListEditor extends DialogCellEditor {
      final StringVariableModel fModel;
      
      public IntegerListEditor(Tree tree, StringVariableModel model) {
         super(tree, SWT.NONE);
         fModel = model;
      }

      @Override
      protected Object openDialogBox(Control paramControl) {
         CheckBoxListDialogue dialog = new CheckBoxListDialogue(paramControl.getShell(), 61, fModel.getValueAsString());
         if (dialog.open() == Window.OK) {
            return dialog.getResult();
         };
         return null;
      }
   }

}
