package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForGpio;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForLlwu;

public class ModifierColumnLabelProvider extends BaseLabelProvider {

   String  fToolTip = null;

   @Override
   public String getText(BaseModel baseModel) {

      if (baseModel instanceof SignalModel) {

         Signal signal = ((SignalModel)baseModel).getSignal();
         if (signal.getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }
         if (signal.getCodeIdentifier().isBlank()) {
            return null;
         }

         Peripheral peripheral = signal.getPeripheral();
         if (peripheral instanceof WriterForGpio) {
            WriterForGpio gpio = (WriterForGpio) peripheral;
            return gpio.isActiveLow(signal)?"ActiveLow":"ActiveHigh";
         }
         if (peripheral instanceof WriterForLlwu) {
            if (signal.getCreateInstance()) {
               WriterForLlwu llwu = (WriterForLlwu) peripheral;
               return llwu.getPinMode(signal).getName();
            }
         }
      }
      return null;

   }

   @Override
   public Image getImage(BaseModel baseModel) {

      if (baseModel instanceof SignalModel) {

         Signal signal = ((SignalModel)baseModel).getSignal();
         if (signal.getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }
         if (signal.getCodeIdentifier().isBlank()) {
            return null;
         }

         Peripheral peripheral = signal.getPeripheral();
         if (peripheral instanceof WriterForGpio) {
            WriterForGpio gpio = (WriterForGpio) peripheral;
            return gpio.isActiveLow(signal)?checkedImage:uncheckedImage;
         }
         if (peripheral instanceof WriterForLlwu) {
            if (signal.getCreateInstance()) {
               WriterForLlwu llwu = (WriterForLlwu) peripheral;
               switch(llwu.getPinMode(signal)) {
               case LlwuPinMode_Disabled:    return disabledImage;
               case LlwuPinMode_EitherEdge:  return upDownArrowImage;
               case LlwuPinMode_FallingEdge: return downArrowImage;
               case LlwuPinMode_RisingEdge:  return upArrowImage;
               }
            }
            return null;
         }
      }
      return null;
   }

   @Override
   public String getToolTipText(Object element) {

      if (element instanceof SignalModel) {
         
         Signal signal = ((SignalModel)element).getSignal();
         
         if ((signal.getMappedPin() != Pin.UNASSIGNED_PIN) &&
               !signal.getCodeIdentifier().isBlank()) {
            
            Peripheral peripheral = signal.getPeripheral();
            if (peripheral instanceof WriterForGpio) {
               return "Polarity of Gpio or bit within GpioField";
            }
            if (peripheral instanceof WriterForLlwu) {
               if (signal.getCreateInstance()) {
                  return "Sensitivity of LLWU input";
               }
            }
         }
      }
      return super.getToolTipText(element);
   }

   /**
    * Provide toolTip for column heading
    * 
    * @return
    */
   public String getColumnToolTipText() {
      return "Modifies the instance or type";
   }

}
