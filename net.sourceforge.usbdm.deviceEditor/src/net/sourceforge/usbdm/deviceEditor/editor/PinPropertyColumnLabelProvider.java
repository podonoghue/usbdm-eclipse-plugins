package net.sourceforge.usbdm.deviceEditor.editor;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.PinModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;

public abstract class PinPropertyColumnLabelProvider extends BaseLabelProvider {

   final long fOffset;
   final long fMask;

   /**
    * Display property (field extracted from getProperties())
    * 
    * @param mask    Mask to extract field 
    * @param offset  Offset to shift after extraction
    */
   public PinPropertyColumnLabelProvider(long mask, long offset) {
      super();
      fOffset = offset;
      fMask   = mask;
   }

   /**
    * Get pin property value of object associated with model
    * 
    * @param baseModel
    * 
    * @return Value as Long or null if not available or suitable model type
    */
   Long getValue(BaseModel baseModel) {
      if (baseModel.isError()) {
         return null;
      }
      if (baseModel instanceof SignalModel) {
         SignalModel signalModel = (SignalModel)baseModel;
         Signal signal = signalModel.getSignal();
         
         Pin pin = signal.getMappedPin();
         if (pin == Pin.UNASSIGNED_PIN) {
            return null;
         }
         if (signal.hasDigitalFeatures()) {
            return signal.getProperty(fMask,fOffset);
         }
      }
      if (baseModel instanceof PinModel) {
         Pin pin = ((PinModel)baseModel).getPin();
         
         ArrayList<MappingInfo> mappedSignals = pin.getActiveMappings();
         if ((mappedSignals.size()==0) || (mappedSignals.size()>1)) {
            // Unmapped or multiply mapped signals
            return null;
         }
         MappingInfo mappingInfo = mappedSignals.get(0);
         if (!mappingInfo.getMux().isMappedValue()) {
            // No PCR for this mapping
            return null;
         }
         if (!mappingInfo.getSignals().get(0).hasDigitalFeatures()) {
            return null;
         }
         return mappingInfo.getProperty(fMask,fOffset);
      }
      return null;
   }

   @Override
   public Image getImage(BaseModel model) {      
      return null;
   }

}
