package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.Activator;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;

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
   public StyledString getStyledText(Object element) {
      if (!(element instanceof BaseModel)) {
         return new StyledString("");
      }
      BaseModel baseModel = (BaseModel) element;
      String text = getText(baseModel);
      if ((text == null)||(text.length() == 0)) {
         return new StyledString("");
      }
      if ((element instanceof CategoryModel)) {
         return new StyledString(text, CATEGORY_STYLER);
      }
      else {
         return new StyledString(text, DEFAULT_STYLER);
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
