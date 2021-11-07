package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForGpio;

public class BooleanColumnLabelProvider extends BaseLabelProvider {

   String  fToolTip = null;
   String  fTrueText;
   String  fFalseText;
   boolean fValue;
   
   private BooleanColumnLabelProvider() {
      fTrueText  = "1";
      fFalseText = "0";
   }

   private BooleanColumnLabelProvider(String trueText, String falseText) {
      fTrueText  = trueText;
      fFalseText = falseText;
   }

   protected Boolean getValue(BaseModel baseModel) {
      if (baseModel instanceof SignalModel) {
         Signal signal = (Signal)((SignalModel)baseModel).getSignal();
         if (signal.getPeripheral() instanceof WriterForGpio) {
            return signal.isActiveLow();
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
      return value?checkedImage:uncheckedImage;
   }

   @Override
   public String getToolTipText(Object element) {
      if ((element instanceof BaseModel) && (getValue((BaseModel)element) != null)) {
         return fToolTip;
      }
      return super.getToolTipText(element);
   }

   private void setToolTip(String toolTip) {
      fToolTip = toolTip;
   }
   
   public static BooleanColumnLabelProvider getPolarity() {
      BooleanColumnLabelProvider t = new BooleanColumnLabelProvider("ActiveLow","ActiveHigh") {};
      t.setToolTip("Polarity if used as Gpio or part of a GpioField");
      return t;
   }

}
