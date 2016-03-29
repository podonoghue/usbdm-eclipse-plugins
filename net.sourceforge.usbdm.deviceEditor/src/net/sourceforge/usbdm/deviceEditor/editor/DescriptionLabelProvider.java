package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.StyledString;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralModel;

class DescriptionLabelProvider extends BaseLabelProvider {

   public DescriptionLabelProvider(TreeEditor configViewer) {
      super(configViewer);
   }

   @Override
   public StyledString getStyledText(BaseModel element) {
      if ((element instanceof CategoryModel)||(element instanceof PeripheralModel)) {
         BaseModel model = (BaseModel)element;
         StyledString styledString = new StyledString(model.getDescription(), StyledString.DECORATIONS_STYLER);
         return styledString;
      }
      else if (element instanceof BaseModel) {
         BaseModel model = (BaseModel)element;
         StyledString styledString = new StyledString(model.getDescription());
         return styledString;
      }
      return null;
   }
}
