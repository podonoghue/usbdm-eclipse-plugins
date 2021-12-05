package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
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
      if (element instanceof SignalModel) {
         Signal signal = ((SignalModel)element).getSignal();
         if (signal.getMappedPin() == Pin.UNASSIGNED_PIN) {
            return false;
         }
         return signal.canCreateType() || signal.canCreateInstance();
      }
      if (element instanceof PeripheralSignalsModel) { 
         Peripheral p = ((PeripheralSignalsModel) element).getPeripheral();
         return p.canCreateType() || p.canCreateInstance();
      }
      return false;
   }

   @Override
   protected CellEditor getCellEditor(Object element) {
      if ((element instanceof SignalModel) ||
          (element instanceof PeripheralSignalsModel)) {
         return new StringCellEditor(viewer.getTree());
      }
      return null;
   }

   @Override
   protected Object getValue(Object element) {
      if (element instanceof SignalModel) {
         Signal signal = ((SignalModel) element).getSignal();
         return signal.getCodeIdentifier();
      }
      if (element instanceof PeripheralSignalsModel) {
         Peripheral peripheral = ((PeripheralSignalsModel)element).getPeripheral();
         return peripheral.getCodeIdentifier();
      }
      return null;
   }

   @Override
   protected void setValue(Object element, Object value) {
      if (element instanceof SignalModel) {
         Signal signal = ((SignalModel) element).getSignal();
         signal.setCodeIdentifier((String) value);
         viewer.update(element, null);
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
