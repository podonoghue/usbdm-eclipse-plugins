package net.sourceforge.usbdm.deviceEditor.editor;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

public class PinInterruptDmaColumnLabelProvider extends PinPropertyColumnLabelProvider {

   public PinInterruptDmaColumnLabelProvider() {
      super(MappingInfo.PORT_PCR_IRQC_MASK, MappingInfo.PORT_PCR_IRQC_SHIFT);
   }

   @Override
   public String getText(BaseModel baseModel) {
      Long value = super.getValue(baseModel);
      if (value == null) {
         return null;
      }
      return Pin.PinIrqDmaValue.valueOf(value.intValue()).getDescription();
   }

   @Override
   public String getToolTipText(Object element) {
      return "Interrupt/DMA option select";
   }

   public static String getColumnToolTipText() {
      return "Pin triggered Interrupt or DMA operation";
   }
   
}
