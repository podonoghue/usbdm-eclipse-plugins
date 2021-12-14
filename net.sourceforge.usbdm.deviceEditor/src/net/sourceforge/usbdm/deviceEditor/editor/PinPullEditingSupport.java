package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;

public class PinPullEditingSupport extends PinPropertyEditingSupport {

   public PinPullEditingSupport(ColumnViewer viewer) {
      super(viewer, MappingInfo.PORT_PCR_PULL_MASK, MappingInfo.PORT_PCR_PULL_SHIFT);
   }

   @Override
   protected CellEditor getCellEditor(Object model) {
      ComboBoxCellEditor editor = new ComboBoxCellEditor(
            ((TreeViewer)getViewer()).getTree(), 
            Pin.PinPullValue.getChoices());

      editor.setActivationStyle(
            ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION |
            ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
      return editor;
   }

   @Override
   protected Object getValue(Object model) {
      return Pin.PinPullValue.valueOf(getValueAsLong(model).intValue()).ordinal();
      }

   @Override
   protected void setValue(Object model, Object value) {
      setValueAsLong(model, (long) Pin.PinPullValue.values()[(((Integer)value))].getValue());
      }
}
