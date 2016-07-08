package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;

public class PinBooleanColumnLabelProvider extends PinPropertyColumnLabelProvider {

   private PinBooleanColumnLabelProvider(long mask, long offset) {
      super(mask, offset);
   }

   @Override
   public String getText(BaseModel baseModel) {
      Long value = super.getValue(baseModel);
      if (value == null) {
         return null;
      }
      return (getValue(baseModel)!=0)?"1":"0";
   }

   @Override
   public Image getImage(BaseModel baseModel) {      
      if (!(baseModel instanceof PinModel)) {
         return null;
      }
      return (getValue(baseModel)!=0)?checkedImage:uncheckedImage;
   }

   public static PinBooleanColumnLabelProvider getLk() {
      return new PinBooleanColumnLabelProvider(Pin.PORT_PCR_LK_MASK, Pin.PORT_PCR_LK_SHIFT);
   }
   
   public static PinBooleanColumnLabelProvider getDse() {
      return new PinBooleanColumnLabelProvider(Pin.PORT_PCR_DSE_MASK, Pin.PORT_PCR_DSE_SHIFT);
   }
   
   public static PinBooleanColumnLabelProvider getOde() {
      return new PinBooleanColumnLabelProvider(Pin.PORT_PCR_ODE_MASK, Pin.PORT_PCR_ODE_SHIFT);
   }
   
   public static PinBooleanColumnLabelProvider getPfe() {
      return new PinBooleanColumnLabelProvider(Pin.PORT_PCR_PFE_MASK, Pin.PORT_PCR_PFE_SHIFT);
   }
   
   public static PinBooleanColumnLabelProvider getSre() {
      return new PinBooleanColumnLabelProvider(Pin.PORT_PCR_SRE_MASK, Pin.PORT_PCR_SRE_SHIFT);
   }
   
}
