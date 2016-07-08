package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;

public class PinInterruptDmaEditingSupport extends EditingSupport {

   public PinInterruptDmaEditingSupport(ColumnViewer viewer) {
      super(viewer);
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

   @Override
   protected Object getValue(Object model) {
      if (!(model instanceof PinModel)) {
         return null;
      }
      PinModel pinModel = (PinModel) model;
      return pinModel.getInterruptDmaSetting().ordinal();
   }

   @Override
   protected void setValue(Object model, Object value) {
      if (!(model instanceof PinModel)) {
         return;
      }
      PinModel pinModel = (PinModel) model;
      pinModel.setInterruptDmaSetting(Pin.PinIntDmaValue.values()[(Integer)value]);
      getViewer().update(model, null);
   }
}
