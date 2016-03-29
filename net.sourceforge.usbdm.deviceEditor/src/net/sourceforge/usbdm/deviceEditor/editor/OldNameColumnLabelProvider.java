package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.StyledString;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;

class OldNameColumnLabelProvider extends BaseLabelProvider {

   public OldNameColumnLabelProvider(TreeEditor configViewer) {
      super(configViewer);
   }

   @Override
   public StyledString getStyledText(BaseModel element) {
      
      if (element instanceof CategoryModel) {
         BaseModel model = (BaseModel)element;
         StyledString styledString = new StyledString(model.getName(), StyledString.DECORATIONS_STYLER);
         return styledString;
      }
      else if (element instanceof BaseModel) {
         BaseModel model = (BaseModel)element;
         StyledString styledString = new StyledString(model.getName());
         return styledString;
      }
      return null;
   }
}
