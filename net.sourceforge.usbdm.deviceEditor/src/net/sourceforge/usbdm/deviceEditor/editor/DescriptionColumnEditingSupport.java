package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;

public class DescriptionColumnEditingSupport extends EditingSupport {

   private TreeViewer viewer;

   public DescriptionColumnEditingSupport(TreeViewer viewer) {
      super(viewer);
      this.viewer = viewer;
   }

   /**
    * Get editable description text
    * 
    * @param model
    * 
    * @return Text to edit (may be blank) or null if not possible to edit
    */
   public static String getEditableDescription(Object model) {
      if (model instanceof PinModel) {
         Signal signal = ((PinModel)model).getPin().getUniqueMappedSignal();
         if (signal == null) {
            // Not editable as multiple sources for description
            return null;
         }
         return signal.getUserDescription();
      }
      else if (model instanceof SignalModel) {
         SignalModel signalModel = (SignalModel)model;
         return signalModel.getSignal().getUserDescription();
      }
      else if (model instanceof PeripheralSignalsModel) {
         PeripheralSignalsModel peripheralSignalsModel = ((PeripheralSignalsModel)model);
         return peripheralSignalsModel.getPeripheral().getUserDescription();
      }
      // No editable description for this type
      return null;
   }
   
   @Override
   protected boolean canEdit(Object model) {
      return getEditableDescription(model) != null;
   }
   
   @Override
   protected CellEditor getCellEditor(Object model) {
      return new StringCellEditor(viewer.getTree());
   }

   @Override
   protected Object getValue(Object model) {
      return getEditableDescription(model);
   }

   @Override
   protected void setValue(Object model, Object value) {
      if (model instanceof PinModel) {
         Signal signal = ((PinModel)model).getPin().getUniqueMappedSignal();
         if (signal == null) {
            return;
         }
         signal.setUserDescription((String)value);
         viewer.update(model, null);
      }
      if (model instanceof SignalModel) {
         SignalModel signalModel = (SignalModel)model;
         signalModel.getSignal().setUserDescription((String) value);
         //         signalModel.getSignal().getMappedPin().setUserDescription((String) value);
         viewer.update(model, null);
      }
      if (model instanceof PeripheralSignalsModel) {
         Peripheral peripheral = ((PeripheralSignalsModel)model).getPeripheral();
         peripheral.setUserDescription((String) value);
         viewer.update(model, null);
      }
   }

   public class StringCellEditor extends TextCellEditor {

      public StringCellEditor(Composite parent, int style) {
         super(parent, style);
         super.setValueValid(true);
      }

      public StringCellEditor(Composite parent) {
         this(parent, SWT.SINGLE);
      }
   }
}
