package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

public class NameColumnLabelProvider extends BaseLabelProvider {
   
   public NameColumnLabelProvider() {
      super();
   }

   @Override
   public String getText(BaseModel baseModel) {
      return baseModel.getName();
   }

   @Override
   public Image getImage(BaseModel model) {
      if (model.isError()) {
         return errorImage;
      }
      else if (model.isWarning()) {
         return warningImage;
      }
      // Space to prevent movement
      return emptyImage;
   }

}
