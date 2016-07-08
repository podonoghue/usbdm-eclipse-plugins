package net.sourceforge.usbdm.deviceEditor.editor;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

public class PinInterruptDmaColumnLabelProvider extends PinPropertyColumnLabelProvider {

   public PinInterruptDmaColumnLabelProvider() {
      super(Pin.PORT_PCR_IRQC_MASK, Pin.PORT_PCR_IRQC_SHIFT);
   }

   @Override
   public String getText(BaseModel baseModel) {
      Long value = super.getValue(baseModel);
      if (value == null) {
         return null;
      }
      return Pin.PinIntDmaValue.valueOf(value.intValue()).getName();
   }
   
}
