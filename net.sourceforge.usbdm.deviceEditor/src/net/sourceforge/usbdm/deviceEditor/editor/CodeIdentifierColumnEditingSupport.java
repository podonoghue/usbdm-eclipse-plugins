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
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;

public class CodeIdentifierColumnEditingSupport extends EditingSupport {

   private TreeViewer viewer;

   public CodeIdentifierColumnEditingSupport(TreeViewer viewer) {
      super(viewer);
      this.viewer = viewer;
   }

   /**
    * Get editable C code identifier
    * 
    * @param model
    * 
    * @return Text to edit (may be blank) or null if not possible to edit
    */
   public static String getEditableCodeIdentifier(Object model) {
      if (model instanceof PinModel) {
         Signal signal = ((PinModel)model).getPin().getUniqueMappedSignal();
         if (signal == null) {
            // Not editable as multiple sources for code identifier
            return null;
         }
         if (!signal.canCreateType() && !signal.canCreateInstance()) {
            // Cannot create C identifier for this signal
            return null;
         }
         return signal.getCodeIdentifier();
      }
      if (model instanceof SignalModel) {
         Signal signal = ((SignalModel) model).getSignal();
         if (!signal.canCreateType() && !signal.canCreateInstance()) {
            // Cannot create C identifier for this signal
            return null;
         }
         return signal.getCodeIdentifier();
      }
      if (model instanceof PeripheralSignalsModel) {
         Peripheral peripheral = ((PeripheralSignalsModel)model).getPeripheral();
         if (!peripheral.canCreateType() && !peripheral.canCreateInstance()) {
            // Cannot create C identifier for this peripheral
            return null;
         }
         return peripheral.getCodeIdentifier();
      }
      if (model instanceof VariableModel) {
         VariableModel vm = (VariableModel) model;
         Signal signal = vm.getAssociatedSignal();
         if (signal != null) {
            if (!signal.canCreateType() && !signal.canCreateInstance()) {
               // Cannot create C identifier for this signal
               return null;
            }
            return signal.getCodeIdentifier();
         }
      }
      return null;
   }
   
   @Override
   protected boolean canEdit(Object model) {
      return getEditableCodeIdentifier(model) != null;
   }

   @Override
   protected CellEditor getCellEditor(Object model) {
      return new StringCellEditor(viewer.getTree());
   }

   @Override
   protected Object getValue(Object model) {
      return getEditableCodeIdentifier(model);
   }

   @Override
   protected void setValue(Object model, Object value) {
      if (model instanceof PinModel) {
         Signal signal = ((PinModel)model).getPin().getUniqueMappedSignal();
         if (signal == null) {
            return;
         }
         signal.setCodeIdentifier((String)value);
         viewer.update(model, null);
      }
      if (model instanceof SignalModel) {
         Signal signal = ((SignalModel) model).getSignal();
         signal.setCodeIdentifier((String) value);
         viewer.update(model, null);
      }
      if (model instanceof PeripheralSignalsModel) {
         Peripheral peripheral = ((PeripheralSignalsModel)model).getPeripheral();
         peripheral.setCodeIdentifier((String) value);
         viewer.update(model, null);
      }
      if (model instanceof VariableModel) {
         VariableModel vm = (VariableModel) model;
         Signal signal = vm.getAssociatedSignal();
         if (signal != null) {
            signal.setCodeIdentifier((String) value);
            viewer.update(model, null);
         }
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
