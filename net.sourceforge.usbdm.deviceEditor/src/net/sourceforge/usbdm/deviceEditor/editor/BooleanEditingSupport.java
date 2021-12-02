package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForGpio;

public abstract class BooleanEditingSupport extends EditingSupport {

   public BooleanEditingSupport(ColumnViewer viewer) {
      super(viewer);
   }

   @Override
   protected CellEditor getCellEditor(Object model) {
      return new CheckboxCellEditor(((TreeViewer)getViewer()).getTree());
   }

   public static EditingSupport getPolarity(TreeViewer viewer) {
      BooleanEditingSupport t = new BooleanEditingSupport(viewer) {
         
         @Override
         protected boolean canEdit(Object model) {
            if (!(model instanceof SignalModel)) {
               return false;
            }
            SignalModel signalModel = (SignalModel)model;
            return signalModel.getSignal().getPeripheral() instanceof WriterForGpio;
         }
         
         @Override
         protected Object getValue(Object model) {
            if (!(model instanceof SignalModel)) {
               return null;
            }
            Signal signal = ((SignalModel)model).getSignal();
            return signal.isActiveLow();
         }

         @Override
         protected void setValue(Object model, Object value) {
            if (!(model instanceof SignalModel)) {
               return;
            }
            Signal signal = ((SignalModel)model).getSignal();
            signal.setActiveLow((Boolean)value);
            getViewer().update(model, null);
         }

      };
      return t;
   }

   public static BooleanEditingSupport getInstance(TreeViewer viewer) {
      BooleanEditingSupport t = new BooleanEditingSupport(viewer) {

         @Override
         protected boolean canEdit(Object model) {
            if (model instanceof PeripheralSignalsModel) {
               Peripheral p = ((PeripheralSignalsModel)model).getPeripheral();
               return p.canCreateInstance() && !p.getCodeIdentifier().isBlank();
            }
            if (model instanceof SignalModel) {
               Signal s = ((SignalModel)model).getSignal();
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
         
      };
      return t;
   }

}
