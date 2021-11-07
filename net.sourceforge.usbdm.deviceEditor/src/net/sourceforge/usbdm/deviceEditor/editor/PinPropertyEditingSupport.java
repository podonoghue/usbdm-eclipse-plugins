package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;

public class PinPropertyEditingSupport extends EditingSupport {

   final long fOffset;
   final long fMask;
   
   public PinPropertyEditingSupport(ColumnViewer viewer, long mask, long offset) {
      super(viewer);
      fOffset = offset;
      fMask   = mask;
   }

   @Override
   protected CellEditor getCellEditor(Object model) {
      if (!(model instanceof PinModel)) {
         return null;
      }
      ComboBoxCellEditor editor = new ComboBoxCellEditor(((TreeViewer)getViewer()).getTree(), Pin.PinIntDmaValue.getChoices());
      editor.setActivationStyle(
            ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION |
            ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
      return editor;
   }

   @Override
   protected boolean canEdit(Object model) {
      if (!(model instanceof BaseModel)) {
         return false;
      }
      BaseModel baseModel = (BaseModel)model;
      return baseModel.canEdit();
   }

   protected Long getValueAsLong(Object model) {
      if (!(model instanceof PinModel)) {
         return null;
      }
      Pin pin = ((PinModel) model).getPin();
      return pin.getProperty(fMask, fOffset);
   }

   @Override
   protected Object getValue(Object model) {
      if (!(model instanceof PinModel)) {
         return null;
      }
      return getValueAsLong(model);
   }

   protected void setValueAsLong(Object model, Long value) {
      if (!(model instanceof PinModel)) {
         return;
      }
      Pin pin = ((PinModel) model).getPin();
      pin.setProperty(fMask, fOffset, value.intValue());
      getViewer().update(model, null);
   }
   
   @Override
   protected void setValue(Object model, Object value) {
      setValueAsLong(model, (Long) value);
   }
}
