package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;

public class InstanceEditingSupport extends EditingSupport {

   public InstanceEditingSupport(ColumnViewer viewer) {
      super(viewer);
   }

   @Override
   protected CellEditor getCellEditor(Object model) {
      return new CheckboxCellEditor(((TreeViewer)getViewer()).getTree());
   }

   @Override
   protected boolean canEdit(Object model) {
      if (model instanceof PeripheralSignalsModel) {
         Peripheral p = ((PeripheralSignalsModel)model).getPeripheral();
         return p.canCreateInstance() && !p.getCodeIdentifier().isBlank();
      }
      if (model instanceof SignalModel) {
         Signal s = ((SignalModel)model).getSignal();
         if (s.getMappedPin() == Pin.UNASSIGNED_PIN) {
            return false;
         }
         return s.canCreateInstance() && !s.getCodeIdentifier().isBlank();
      }
      return false;
   }

   @Override
   protected Object getValue(Object model) {
      if (model instanceof PeripheralSignalsModel) {
         Peripheral p = ((PeripheralSignalsModel)model).getPeripheral();
         if (p.canCreateInstance()) {
            return p.getCreateInstance(); 
         }
      }
      if (model instanceof SignalModel) {
         Signal s = ((SignalModel)model).getSignal();
         if (s.canCreateInstance()) {
            return s.getCreateInstance(); 
         }
      }
      return null;
   }

   @Override
   protected void setValue(Object model, Object value) {
      if (model instanceof PeripheralSignalsModel) {
         Peripheral p = ((PeripheralSignalsModel)model).getPeripheral();
         p.setCreateInstance((Boolean)value);
         getViewer().update(model, null);
      }
      if (model instanceof SignalModel) {
         Signal s = ((SignalModel)model).getSignal();
         s.setCreateInstance((Boolean)value);
         getViewer().update(model, null);
      }
   }

}
