package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.*;

public class CodeIdentifierColumnLabelProvider extends BaseLabelProvider {

   public CodeIdentifierColumnLabelProvider() {
      super();
   }

   @Override
   public String getText(BaseModel baseModel) {
//      System.err.println("CodeIdentifierColumnLabelProvider::getText(" + baseModel.getName() + baseModel+")");
      if (baseModel instanceof SignalModel) {
         Signal signal = ((SignalModel) baseModel).getSignal();
         Pin pin = signal.getMappedPin();
         return pin.getCodeIdentifier();
      }
      if (baseModel instanceof PinModel) {
         Pin pin = ((PinModel)baseModel).getPin();
         return pin.getCodeIdentifier();
      }
      if (baseModel instanceof PeripheralSignalsModel) {
         Peripheral peripheral = ((PeripheralSignalsModel)baseModel).getPeripheral();
         return peripheral.getCodeIdentifier();
      }
      return null;// baseModel.toString();
   }

   @Override
   public Image getImage(BaseModel model) {
      return null;
   }
   
}
