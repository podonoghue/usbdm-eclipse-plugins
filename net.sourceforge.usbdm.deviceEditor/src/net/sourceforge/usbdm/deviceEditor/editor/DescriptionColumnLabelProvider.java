package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.StyledString;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;

class DescriptionColumnLabelProvider extends BaseLabelProvider {

   public DescriptionColumnLabelProvider(TreeEditor configViewer) {
      super(configViewer);
   }

   @Override
   public StyledString getStyledText(BaseModel element) {
      StyledString styledString = new StyledString("");
      if ((element instanceof PinModel)) {
         PinModel model = (PinModel)element;
         if (model.isError()) {
            if (model.getDescription() != null) {
               styledString = new StyledString(model.getDescription(), StyledString.QUALIFIER_STYLER);
            }
         }
         else {
            if (model.getPinUseDescription() != null) {
               styledString = new StyledString(model.getPinUseDescription(), StyledString.DECORATIONS_STYLER);
            }
         }
      }
      else if (element instanceof CategoryModel) {
         BaseModel model = (BaseModel)element;
         if (model.getDescription() != null) {
            styledString = new StyledString(model.getDescription(), StyledString.QUALIFIER_STYLER);
         }
      }
      else if (element instanceof SignalModel) {
         SignalModel model = (SignalModel)element;
         if (model.getDescription() != null) {
            styledString = new StyledString(model.getDescription(), StyledString.QUALIFIER_STYLER);
         }
      }
      else if (element instanceof BaseModel) {
         BaseModel model = (BaseModel)element;
         if (model.getDescription() != null) {
            styledString = new StyledString(model.getDescription());
         }
      }
      return styledString;
   }
}
