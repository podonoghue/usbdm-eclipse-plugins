package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

public class DescriptionColumnLabelProvider extends BaseLabelProvider {

   public DescriptionColumnLabelProvider() {
      super();
   }

   @Override
   public String getText(BaseModel baseModel) {
      return baseModel.getDescription();
   }

   @Override
   public Image getImage(BaseModel model) {
      return null;
   }
   
}
