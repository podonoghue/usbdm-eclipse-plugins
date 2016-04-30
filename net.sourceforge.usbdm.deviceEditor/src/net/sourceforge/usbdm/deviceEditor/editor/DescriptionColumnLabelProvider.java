package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

class DescriptionColumnLabelProvider extends BaseLabelProvider {

   DescriptionColumnLabelProvider(TreeEditor viewer) {
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
