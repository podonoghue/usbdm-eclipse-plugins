package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
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

   @Override
   protected boolean canEdit(Object element) {
      if ((element instanceof PinModel) ||
          (element instanceof SignalModel)) {
         return true;
      }
      if ((element instanceof PeripheralSignalsModel) && 
            !((PeripheralSignalsModel) element).getPeripheral().isSynthetic()) {
           return true;
        }
      return false;
   }

   @Override
   protected CellEditor getCellEditor(Object element) {
      if ((element instanceof PinModel) ||
            (element instanceof SignalModel)||
            (element instanceof PeripheralSignalsModel)) {
         return new StringCellEditor(viewer.getTree());
      }
      return null;
   }

   @Override
   protected Object getValue(Object element) {
      if (element instanceof PinModel) {
         Pin pin = ((PinModel)element).getPin();
         return pin.getUserDescription();
      }
      if (element instanceof SignalModel) {
         SignalModel signalModel = (SignalModel)element;
         return signalModel.getSignal().getMappedPin().getUserDescription();
      }
      if (element instanceof PeripheralSignalsModel) {
         PeripheralSignalsModel peripheralSignalsModel = ((PeripheralSignalsModel)element);
         return peripheralSignalsModel.getPeripheral().getUserDescription();
      }
      return null;
   }

   @Override
   protected void setValue(Object element, Object value) {
      if (element instanceof PinModel) {
         PinModel pinModel = (PinModel)element;
         pinModel.getPin().setUserDescription((String) value);
      }
      if (element instanceof SignalModel) {
         SignalModel signalModel = (SignalModel)element;
         signalModel.getSignal().getMappedPin().setUserDescription((String) value);
      }
      if (element instanceof PeripheralSignalsModel) {
         Peripheral peripheral = ((PeripheralSignalsModel)element).getPeripheral();
         peripheral.setUserDescription((String) value);
      }
      viewer.update(element, null);
      }

   public class StringCellEditor extends TextCellEditor {

      public StringCellEditor(Composite parent, int style) {
         super(parent, style);
         super.setValueValid(true);
      }

      public StringCellEditor(Composite parent) {
         this(parent, SWT.SINGLE);
      }

      @Override
      protected Object doGetValue() {
         Object item = super.doGetValue();
//         System.err.println("StringCellEditor.doGetValue value = " + item + ", " + item.getClass());
         return item;
      }

      @Override
      protected void doSetValue(Object value) {
//         System.err.println("StringCellEditor.doSetValue value = " + value + ", " + value.getClass());
         super.doSetValue(value);
      }
   }
  

}
