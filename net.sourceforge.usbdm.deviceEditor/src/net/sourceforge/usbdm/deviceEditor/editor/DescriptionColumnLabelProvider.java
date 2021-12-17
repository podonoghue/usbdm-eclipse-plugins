package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;

public class DescriptionColumnLabelProvider extends BaseLabelProvider {

   public DescriptionColumnLabelProvider() {
      super();
   }

   @Override
   public String getText(BaseModel model) {
      String text = DescriptionColumnEditingSupport.getEditableDescription(model);
      if ((text != null) && !text.isBlank()) {
         // Something useful to display
         return text;
      }
      // Display alternative text
      if (model instanceof SignalModel) {
         SignalModel signalModel = (SignalModel)model;
         return signalModel.getAvailablePins();
      }
      else if (model instanceof PinModel) {
         PinModel pinModel = (PinModel)model;
         Pin pin = pinModel.getPin();
         if (pin.getActiveMappings().size() > 0) {
            // Some mapped signals - use description from them 
            text = pinModel.getPin().getMappedSignalsUserDescriptions();
         }
         else {
            // Nothing mapped to pin - display list of available signals
            text = pinModel.getAvailableSignals();
         }
         return text;
      }
      return model.getDescription();
   }

   @Override
   public Image getImage(BaseModel model) {
      return null;
   }

   @Override
   public String getToolTipText(Object model) {
      final String tooltip = "List of C comments separated by '/'";
      
      if (model instanceof SignalModel) {
         Signal signal = ((SignalModel)model).getSignal();
         if (signal.getUserDescription().isBlank()) {
            return "Pins mappable to this signal (*indicates free pins)";
         }
         return tooltip;
      }
      if (model instanceof PeripheralSignalsModel) {
         return "Description  of peripheral\n";
      }
      if (model instanceof PinModel) {
         PinModel pinModel = (PinModel)model;
         Pin pin = pinModel.getPin();
         if (pin.getActiveMappings().size() > 0) {
            // Some mapped signals - use description from them 
            return tooltip+
                  "\n" +
                  "These are generated from mapped signals\n" +
                  "Only editable here if a single signal is mapped";
         }
         else {
            // Nothing mapped to pin - display list of available signals
            return "Signals mappable to this pin" +
                   "(*indicates unallocated signals)";
         }
      }
      return super.getToolTipText(model);
   }

   public static String getColumnToolTipText() {
      return 
            "Property Description or\n" +
            "Description of use (Peripherals and signals only)\n"+
            "Appears as comment in user code";
   }
   
}
