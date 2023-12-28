package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceVariantInformation;

public class DeviceVariantModel extends SelectionModel implements IModelChangeListener {

   private final DeviceInfo fDeviceInfo;
   ArrayList<String>  fDeviceNames = new ArrayList<String>();
   
   /**
    * Create Model
    * 
    * @param parent        Parent model
    * @param deviceInfo    Device information to construct node
    */
   public DeviceVariantModel(BaseModel parent, DeviceInfo deviceInfo) {
      super(parent, "Variant");
      setSimpleDescription("Selected variant from this sub-family");

      fDeviceInfo = deviceInfo;

      for (String deviceName:fDeviceInfo.getDeviceVariants().keySet()) {
         DeviceVariantInformation deviceInformation = fDeviceInfo.getDeviceVariants().get(deviceName);
         fDeviceNames.add(deviceInformation.getPreciseName());
      }
      fChoices    = fDeviceNames.toArray(new String[fDeviceNames.size()]);
      fSelection  = fDeviceNames.indexOf(fDeviceInfo.getPreciseName());
      if (fSelection<0) {
         fSelection = 0;
         try {
            fDeviceInfo.setVariantName(fChoices[0]);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      fDeviceInfo.addListener(this);
   }

   @Override
   public void setValueAsString(String value) {
      int oldSelection = fSelection;
      super.setValueAsString(value);
      if (oldSelection != fSelection) {
         try {
            fDeviceInfo.setVariantName(value);
         } catch (Exception e) {
            e.printStackTrace();
         }
         refresh();
      }
   }

   @Override
   public void modelElementChanged(ObservableModelInterface model, String[] properties) {
      if (model instanceof DeviceInfo) {
         DeviceInfo deviceInfo = (DeviceInfo) model;
         String variantName = deviceInfo.getPreciseName();
         setValueAsString(variantName);
      }
   }

   @Override
   protected void removeMyListeners() {
      fDeviceInfo.removeListener(this);
   }

}
