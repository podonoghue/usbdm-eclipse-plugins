package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;

public class PinPropertyColumnLabelProvider extends BaseLabelProvider {

   final long fOffset;
   final long fMask;

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
         return pinModel.getProperty(fMask,fOffset);
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
