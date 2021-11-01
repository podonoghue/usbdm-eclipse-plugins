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

public class CodeIdentifierColumnEditingSupport extends EditingSupport {

   private TreeViewer viewer;

   public CodeIdentifierColumnEditingSupport(TreeViewer viewer) {
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
            (element instanceof SignalModel) ||
            (element instanceof PeripheralSignalsModel)) {
         return new StringCellEditor(viewer.getTree());
      }
      return null;
   }

   @Override
   protected Object getValue(Object element) {
      if (element instanceof PinModel) {
         Pin pin = ((PinModel)element).getPin();
         return pin.getCodeIdentifier();
      }
      if (element instanceof SignalModel) {
         Pin pin = ((SignalModel)element).getSignal().getMappedPin();
         return pin.getCodeIdentifier();
      }
      if (element instanceof PeripheralSignalsModel) {
         Peripheral peripheral = ((PeripheralSignalsModel)element).getPeripheral();
         return peripheral.getCodeIdentifier();
      }
      return null;
   }

   @Override
   protected void setValue(Object element, Object value) {
      if (element instanceof PinModel) {
         Pin pin = ((PinModel)element).getPin();
         pin.setCodeIdentifier((String) value);
         viewer.update(element, null);
      }
      if (element instanceof SignalModel) {
         Pin pin = ((SignalModel)element).getSignal().getMappedPin();
         if (pin != Pin.UNASSIGNED_PIN) {
            pin.setCodeIdentifier((String) value);
            viewer.update(element, null);
         }
      }
      if (element instanceof PeripheralSignalsModel) {
         Peripheral peripheral = ((PeripheralSignalsModel)element).getPeripheral();
         peripheral.setCodeIdentifier((String) value);
         viewer.update(element, null);
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
