package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;

public class PinPropertyColumnLabelProvider extends BaseLabelProvider {

   final long fOffset;
   final long fMask;

   /**
    * Display property (field from getProperties())
    * 
    * @param mask    Mask to extract field 
    * @param offset  Offset to shift after extraction
    */
   public PinPropertyColumnLabelProvider(long mask, long offset) {
      super();
      fOffset = offset;
      fMask   = mask;
   }

   Long getValue(BaseModel baseModel) {
      if (baseModel instanceof PinModel) {
         PinModel pinModel = (PinModel)baseModel;
         if (!pinModel.canEdit()) {
            return null;
         }
         Pin pin = pinModel.getPin();
         return pin.getProperty(fMask,fOffset);
      }
      return null;
   }

   @Override
   public String getText(BaseModel baseModel) {
      return Long.toString(getValue(baseModel));
   }

   @Override
   public Image getImage(BaseModel model) {      
      return null;
   }

}
