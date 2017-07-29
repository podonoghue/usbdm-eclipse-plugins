package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BooleanVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.EditableModel;
import net.sourceforge.usbdm.deviceEditor.model.IrqVariableModel;

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
      if (baseModel instanceof IrqVariableModel) {
         switch (((IrqVariableModel)baseModel).getVariable().getMode()) {
         case NotInstalled:
            return uncheckedImage;
         case ClassMethod:
            return greycheckedImage;
         case UserMethod:
            return checkedImage;
         }
      }
      return emptyImage;
   }

}
