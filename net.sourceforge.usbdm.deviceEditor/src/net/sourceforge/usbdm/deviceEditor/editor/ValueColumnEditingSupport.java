package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BooleanVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.EditableModel;
import net.sourceforge.usbdm.deviceEditor.model.RtcTimeModel;

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
      return null;
   }

   @Override
   protected Object getValue(Object element) {
      if (element instanceof BooleanVariableModel) {
         return ((BooleanVariableModel)element).getValueAsBoolean();
      }
      if (element instanceof BaseModel) {
         return ((BaseModel)element).getEditValueAsString();
      }
      return "";
   }

   @Override
   protected void setValue(Object element, Object value) {
      if (element instanceof BooleanVariableModel) {
         ((BooleanVariableModel)element).setBooleanValue((Boolean) value);
      }
      else if (element instanceof RtcTimeModel) {
         if (value instanceof Long) {
            ((RtcTimeModel)element).setTime((Long)value);
         }
      }
      else if (element instanceof EditableModel) {
         ((EditableModel)element).setValueAsString(value.toString());
      }
      fViewer.update(element, null);
   }

}
