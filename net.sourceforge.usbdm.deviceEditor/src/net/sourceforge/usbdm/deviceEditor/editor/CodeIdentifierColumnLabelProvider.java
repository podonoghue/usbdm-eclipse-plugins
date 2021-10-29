package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;

public class CodeIdentifierColumnLabelProvider extends BaseLabelProvider {

   public CodeIdentifierColumnLabelProvider() {
      super();
   }

   @Override
   public String getText(BaseModel baseModel) {
      if (baseModel instanceof SignalModel) {
         Signal signal = ((SignalModel) baseModel).getSignal();
         Pin pin = signal.getMappedPin();
//         if (pin.getName().contains("GPIOA_4")) {
//            // XXXX Delete me!
//            System.err.println("CodeIdentifierColumnLabelProvider.getText(SignalModel("+signal.getName()+"))");
//         }
         return pin.getCodeIdentifier();
      }
      if (baseModel instanceof PinModel) {
         PinModel pinModel = (PinModel)baseModel;
//         if (pinModel.getName().contains("GPIOA_4")) {
//            // XXXX Delete me!
//            System.err.println("CodeIdentifierColumnLabelProvider.getText(PinModel("+pinModel.getName()+"))");
//         }
         return pinModel.getCodeIdentifier();
      }
      return null;
   }

   @Override
   public Image getImage(BaseModel model) {
      return null;
   }
   
}
