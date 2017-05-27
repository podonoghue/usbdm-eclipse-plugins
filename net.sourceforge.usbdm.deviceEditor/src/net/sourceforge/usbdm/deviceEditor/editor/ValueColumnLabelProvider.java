package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BooleanVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.EditableModel;

public class ValueColumnLabelProvider extends BaseLabelProvider {
   
   public ValueColumnLabelProvider() {
      super();
   }

   @Override
   public String getText(BaseModel baseModel) {
      return baseModel.getValueAsString();
   }

   @Override
   public Image getImage(BaseModel baseModel) {
      if (!baseModel.isEnabled()) {
         return disabledImage;
      }
      if ((baseModel instanceof EditableModel) && !baseModel.canEdit()) {
         return lockedImage;
      }
      if (baseModel instanceof BooleanVariableModel) {
         return ((Boolean)((BooleanVariableModel)baseModel).getVariable().getValueAsBoolean())?checkedImage:uncheckedImage;
      }
      return emptyImage;
   }

   @Override
   public String getToolTipText(Object element) {
      return super.getToolTipText(element);
   }

}
