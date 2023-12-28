package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;

public class InstanceColumnLabelProvider extends BaseLabelProvider {

   final String  fTrueText  = "Instance";
   final String  fFalseText = "Type";
   
   boolean fValue;

   public InstanceColumnLabelProvider() {}

   /**
    * Provide toolTip for column heading
    * 
    * @return
    */
   static public String getColumnToolTipText() {
      return
            "Whether to create a variable instance or a type declaration in generated code.\n" +
            "Type declarations allow access to static methods but cannot be passed as parameters.\n" +
            "Some peripherals provides instance methods as well which are more flexible but occupy RAM.";
   }

   protected Boolean getValue(BaseModel baseModel) {
      if (baseModel instanceof PeripheralSignalsModel) {
         Peripheral peripheral = ((PeripheralSignalsModel)baseModel).getPeripheral();
         if (peripheral.canCreateInstance() && !peripheral.getCodeIdentifier().isBlank()) {
            return peripheral.getCreateInstance();
         }
      }
      if (baseModel instanceof SignalModel) {
         Signal signal = ((SignalModel)baseModel).getSignal();
         if (signal.getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }
         if (signal.canCreateInstance() && !signal.getCodeIdentifier().isBlank()) {
            return signal.getCreateInstance();
         }
      }
      return null;
   }
   
   @Override
   public String getText(BaseModel baseModel) {
      Boolean value = getValue(baseModel);
      if (value == null) {
         return null;
      }
      return value?fTrueText:fFalseText;
   }

   @Override
   public Image getImage(BaseModel baseModel) {
      Boolean value = getValue(baseModel);
      if (value == null) {
         return null;
      }
//      return warningImage;
      return value?checkedImage:uncheckedImage;
   }

   @Override
   public String getToolTipText(Object element) {
      final String  fToolTip   = "Create instance or type declaration";
      if (element instanceof PeripheralSignalsModel) {
         Peripheral peripheral = ((PeripheralSignalsModel)element).getPeripheral();
         if (peripheral.canCreateInstance() && !peripheral.getCodeIdentifier().isBlank()) {
            return fToolTip;
         }
      }
      if (element instanceof SignalModel) {
         Signal signal = ((SignalModel)element).getSignal();
         if ((signal.getMappedPin() != Pin.UNASSIGNED_PIN) &&
             signal.canCreateInstance() && !signal.getCodeIdentifier().isBlank()) {
            return fToolTip;
         }
      }
      return super.getToolTipText(element);
   }

}
