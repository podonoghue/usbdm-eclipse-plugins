package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;

public class PinInterruptDmaEditingSupport extends PinPropertyEditingSupport {

   public PinInterruptDmaEditingSupport(ColumnViewer viewer) {
      super(viewer, MappingInfo.PORT_PCR_IRQC_MASK, MappingInfo.PORT_PCR_IRQC_SHIFT);
   }

   @Override
   protected CellEditor getCellEditor(Object model) {
      ComboBoxCellEditor editor = new ComboBoxCellEditor(
            ((TreeViewer)getViewer()).getTree(), 
            Pin.PinIrqDmaValue.getChoices());
      
      editor.setActivationStyle(
            ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION |
            ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
      return editor;
   }

   @Override
   protected Object getValue(Object model) {
      return Pin.PinIrqDmaValue.valueOf(getValueAsLong(model).intValue()).ordinal();
   }

   @Override
   protected void setValue(Object model, Object value) {
      setValueAsLong(model, (long) Pin.PinIrqDmaValue.values()[(((Integer)value))].getValue());
   }
}
