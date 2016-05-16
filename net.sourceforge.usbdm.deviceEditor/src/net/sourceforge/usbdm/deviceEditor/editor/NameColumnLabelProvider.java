package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.Activator;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

class NameColumnLabelProvider extends BaseLabelProvider {
   
   private  Image errorImage     = null;
   private  Image warningImage   = null;

   NameColumnLabelProvider(TreeEditor viewer) {
      if (Activator.getDefault() != null) {
         errorImage   = Activator.getDefault().getImageDescriptor(Activator.ID_ERROR_NODE_IMAGE).createImage();
         warningImage = Activator.getDefault().getImageDescriptor(Activator.ID_WARNING_NODE_IMAGE).createImage();
      }
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
      return null;
   }

   @Override
   public void dispose() {
      super.dispose();
      if (errorImage != null) {
         errorImage.dispose();
         errorImage = null;
      }
      if (warningImage != null) {
         warningImage.dispose();
         warningImage = null;
      }
   }
}
