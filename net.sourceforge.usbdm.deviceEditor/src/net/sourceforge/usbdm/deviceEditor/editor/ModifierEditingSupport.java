package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForGpio;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLlwu;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLlwu.LlwuPinMode;

public class ModifierEditingSupport extends EditingSupport {

   final String[] llwuPinModes = {
         LlwuPinMode.LlwuPinMode_Disabled.getName(),
         LlwuPinMode.LlwuPinMode_RisingEdge.getName(),
         LlwuPinMode.LlwuPinMode_FallingEdge.getName(),
         LlwuPinMode.LlwuPinMode_EitherEdge.getName(),
   };
   
   static class ChoiceCellEditor extends ComboBoxCellEditor {
      
      public ChoiceCellEditor(Composite tree, String[] choices) {
         super(tree, choices, SWT.READ_ONLY);
         setActivationStyle(
               ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION |
               ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);
         setValueValid(true);
      }
   }

   public ModifierEditingSupport(ColumnViewer viewer) {
      super(viewer);
   }

   @Override
   protected CellEditor getCellEditor(Object model) {
      
      if (model instanceof SignalModel) {
         
         Signal signal = (Signal)((SignalModel)model).getSignal();
         if (signal.getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }

         Peripheral peripheral = signal.getPeripheral();
         if (peripheral instanceof WriterForGpio) {
            return new CheckboxCellEditor(((TreeViewer)getViewer()).getTree());
         }
         if (peripheral instanceof WriterForLlwu) {
            return new ChoiceCellEditor(((TreeViewer)getViewer()).getTree(), llwuPinModes );
         }
      }
      return null;
   }

   @Override
   protected boolean canEdit(Object model) {
      
      if (model instanceof SignalModel) {
         
         Signal signal = (Signal)((SignalModel)model).getSignal();
         if (signal.getMappedPin() == Pin.UNASSIGNED_PIN) {
            return false;
         }

         Peripheral peripheral = signal.getPeripheral();
         if (peripheral instanceof WriterForGpio) {
            return true;
         }
         if (peripheral instanceof WriterForLlwu) {
            return signal.getCreateInstance();
         }
      }
      return false;
   }

   @Override
   protected Object getValue(Object model) {
      
      if (model instanceof SignalModel) {
         
         Signal signal = (Signal)((SignalModel)model).getSignal();
         if (signal.getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }

         Peripheral peripheral = signal.getPeripheral();
         if (peripheral instanceof WriterForGpio) {
            WriterForGpio gpio = (WriterForGpio) peripheral;
            return gpio.isActiveLow(signal);
         }
         if (peripheral instanceof WriterForLlwu) {
            WriterForLlwu llwu = (WriterForLlwu) peripheral;
            return llwu.getPinMode(signal).getValue();
         }
      }
      return false;
   }

   @Override
   protected void setValue(Object model, Object value) {
      
      if (model instanceof SignalModel) {
         
         Signal signal = (Signal)((SignalModel)model).getSignal();
         Peripheral peripheral = signal.getPeripheral();

         if (peripheral instanceof WriterForGpio) {
            WriterForGpio gpio = (WriterForGpio) peripheral;
            gpio.setActiveLow(signal, (Boolean)value);
            getViewer().update(model, null);
         }
         if (peripheral instanceof WriterForLlwu) {
            WriterForLlwu llwu = (WriterForLlwu) peripheral;
            llwu.setPinMode(signal, LlwuPinMode.convertFromInt((int)value));
            getViewer().update(model, null);
         }
      }
   }

   public static String getColumnToolTipText() {
      return "Modifies the Instance or Type";
   }
}
