package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.*;

public class CodeIdentifierColumnLabelProvider extends BaseLabelProvider {

   public CodeIdentifierColumnLabelProvider() {
      super();
   }

   @Override
   public String getText(BaseModel baseModel) {
      if (baseModel instanceof SignalModel) {
         Signal signal = ((SignalModel) baseModel).getSignal();
         if (signal.getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }
         return signal.getCodeIdentifier();
      }
      if (baseModel instanceof PeripheralSignalsModel) {
         Peripheral peripheral = ((PeripheralSignalsModel)baseModel).getPeripheral();
         return peripheral.getCodeIdentifier();
      }
      if (baseModel instanceof PinModel) {
         Pin pin = ((PinModel)baseModel).getPin();
         return pin.getMappedSignalsCodeIdentifiers();
      }
      return null;
   }

   @Override
   public Image getImage(BaseModel model) {
      return null;
   }

   @Override
   public String getToolTipText(Object element) {
      final String tooltip = "List of C identifiers separated by '/'";
      if (element instanceof PeripheralSignalsModel) {
         return tooltip;
      }
      if (element instanceof SignalModel) {
         Signal signal = ((SignalModel)element).getSignal();
         if (signal.getMappedPin() != Pin.UNASSIGNED_PIN) {
            return tooltip;
         }
      }
      if (element instanceof PinModel) {
         return tooltip+"\nThese are generated from mapped signals";
      }
      return super.getToolTipText(element);
   }
   
}
