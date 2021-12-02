package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForGpio;

public abstract class BooleanColumnLabelProvider extends BaseLabelProvider {

   String  fToolTip = null;
   String  fTrueText;
   String  fFalseText;
   boolean fValue;
   
   /**
    * Provide toolTip for column heading
    * 
    * @return
    */
   public abstract String getColumnToolTipText();

   private BooleanColumnLabelProvider() {
      fTrueText  = "1";
      fFalseText = "0";
   }

   private BooleanColumnLabelProvider(String trueText, String falseText) {
      fTrueText  = trueText;
      fFalseText = falseText;
   }

   protected abstract Boolean getValue(BaseModel baseModel);
   
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
//      if ((element instanceof BaseModel) && (getValue((BaseModel)element) != null)) {
         return fToolTip;
//      }
//      return super.getToolTipText(element);
   }

   private void setToolTip(String toolTip) {
      fToolTip = toolTip;
   }
   
   public static BooleanColumnLabelProvider getPolarity() {
      BooleanColumnLabelProvider t = new BooleanColumnLabelProvider("ActiveLow","ActiveHigh") {

         @Override
         public String getColumnToolTipText() {
            return "Polarity if used as GPIO";
         }

         @Override
         protected Boolean getValue(BaseModel baseModel) {
            if (baseModel instanceof SignalModel) {
               Signal signal = (Signal)((SignalModel)baseModel).getSignal();
               if (signal.getPeripheral() instanceof WriterForGpio) {
                  return signal.isActiveLow();
               }
            }
            return null;
         }
         
      };
      t.setToolTip("Polarity if used as Gpio or part of a GpioField");
      return t;
   }

   public static BooleanColumnLabelProvider getInstance() {
      BooleanColumnLabelProvider t = new BooleanColumnLabelProvider("Instance", "Type") {

//         @Override
//         public StyledString getStyledText(Object element) {
//            if (element instanceof PeripheralSignalsModel) {
//               Peripheral psm = ((PeripheralSignalsModel)element).getPeripheral();
//               if (psm.getCodeIdentifier().isBlank()) {
//                  String text = getText((BaseModel)element);
//                  return new StyledString(text, DISABLED_STYLER);
//               }
//            }
//            if (element instanceof SignalModel) {
//               Signal signal = (Signal)((SignalModel)element).getSignal();
//               if (signal.getCodeIdentifier().isBlank()) {
//                  String text = getText((BaseModel)element);
//                  return new StyledString(text, DISABLED_STYLER);
//               }
//            }
//            return super.getStyledText(element);
//         }

         /**
          * Provide toolTip for column heading
          * 
          * @return
          */
         @Override
         public String getColumnToolTipText() {
            return
                  "Whether to create a variable instance or a type declaration in generated code.\n" + 
                  "Type declarations allow access to static methods but cannot be passed as parameters.\n" +
                  "Some peripherals provides instance methods as well which are more flexible but occupy RAM.";
         }

         @Override
         protected Boolean getValue(BaseModel baseModel) {
            if (baseModel instanceof PeripheralSignalsModel) {
               Peripheral peripheral = ((PeripheralSignalsModel)baseModel).getPeripheral();
               if (peripheral.canCreateInstance() && !peripheral.getCodeIdentifier().isBlank()) {
                  return peripheral.getCreateInstance(); 
               }
            }
            if (baseModel instanceof SignalModel) {
               Signal signal = (Signal)((SignalModel)baseModel).getSignal();
               if (signal.canCreateInstance() && !signal.getCodeIdentifier().isBlank()) {
                  return signal.getCreateInstance(); 
               }
            }
            return null;
         }
         
      };
      t.setToolTip("Create instance or type declaration");
      return t;
   }

}
