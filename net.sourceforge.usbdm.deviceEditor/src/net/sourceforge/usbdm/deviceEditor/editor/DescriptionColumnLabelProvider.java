package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;

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

   @Override
   public String getToolTipText(Object element) {
      final String tooltip = "List of C comments separated by '/'";
      
      if (element instanceof SignalModel) {
         Signal signal = ((SignalModel)element).getSignal();
         if (signal.getMappedPin() != Pin.UNASSIGNED_PIN) {
            return tooltip;
         }
         else {
            return "Pins mappable to this signal (*indicates free pins)";
         }
      }
      if (element instanceof PinModel) {
         return tooltip+"\nThese are generated from mapped signals";
      }
      return super.getToolTipText(element);
   }
   
}
