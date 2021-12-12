package net.sourceforge.usbdm.deviceEditor.editor;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

public class PinPullColumnLabelProvider extends PinPropertyColumnLabelProvider {

   public PinPullColumnLabelProvider() {
      super(Pin.PORT_PCR_PULL_MASK, Pin.PORT_PCR_PULL_SHIFT);
   }

   @Override
   public String getText(BaseModel baseModel) {
      Long value = super.getValue(baseModel);
      if (value == null) {
         return null;
      }
      return Pin.PinPullValue.valueOf(value.intValue()).getDecription();
   }

   @Override
   public String getToolTipText(Object element) {
      return "Pin pull-up/down select";
   }
   
   public static String getColumnToolTipText() {
      return "Enables pullup/down resistor on pin ";
   }
   
}
