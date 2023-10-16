package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BooleanVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.SelectionModel;

public class ValueColumnLabelProvider extends BaseLabelProvider {
   
   public ValueColumnLabelProvider() {
      super();
   }

   @Override
   public String getText(BaseModel baseModel) {
      if (baseModel instanceof CategoryModel) {
         return baseModel.getDescription();
      }
      return baseModel.getValueAsString();
   }

   @Override
   public Image getImage(BaseModel baseModel) {
      if (!baseModel.isEnabled()) {
         return disabledImage;
      }
      if (baseModel.showAsLocked()) {
         return lockedImage;
      }
      if (baseModel instanceof BooleanVariableModel) {
         return (((BooleanVariableModel)baseModel).getVariable().getValueAsBoolean())?checkedImage:uncheckedImage;
      }
      if (baseModel instanceof SelectionModel) {
         SelectionModel model = (SelectionModel)baseModel;
         if (model.getChoices().length == 2) {
            return (model.getSelection() !=  0)?checkedImage:uncheckedImage;
         }
      }
      
      return emptyImage;
   }

}
