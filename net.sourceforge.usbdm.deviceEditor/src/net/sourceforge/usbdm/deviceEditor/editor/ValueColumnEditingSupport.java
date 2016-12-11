package net.sourceforge.usbdm.deviceEditor.editor;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BooleanVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.EditableModel;
import net.sourceforge.usbdm.deviceEditor.model.FilePathModel;

public class ValueColumnEditingSupport extends EditingSupport {

   private TreeViewer fViewer;

   public ValueColumnEditingSupport(TreeViewer viewer) {
      super(viewer);
      fViewer = viewer;
   }

   @Override
   protected boolean canEdit(Object model) {
      if (!(model instanceof BaseModel)) {
         return false;
      }
      BaseModel baseModel = (BaseModel)model;
      return baseModel.canEdit();
   }

   @Override
   protected CellEditor getCellEditor(Object element) {
      if (element instanceof CellEditorProvider) {
         CellEditorProvider model = (CellEditorProvider) element;
         return model.createCellEditor(fViewer.getTree());
      }
      if (element instanceof FilePathModel) {
         return new HardwareCellEditor(fViewer.getTree());
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
      fViewer.update(element, null);
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
