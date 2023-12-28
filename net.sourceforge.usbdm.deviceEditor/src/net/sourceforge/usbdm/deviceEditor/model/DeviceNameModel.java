package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceVariantInformation;

public class DeviceNameModel extends StringModel implements IModelChangeListener {

   /**
    * Device package
    * 
    * @param parent     Parent node
    * @param deviceInfo Device info used to construct the node
    */
   public DeviceNameModel(BaseModel parent, DeviceInfo deviceInfo) {
      super(parent, "Generic Device Name", "Device name for this variant", deviceInfo.getVariant().getDeviceName());
      deviceInfo.addListener(this);
   }

   @Override
   public void modelElementChanged(ObservableModelInterface model, String[] properties) {
      if (model instanceof DeviceInfo) {
         DeviceInfo deviceInfo = (DeviceInfo) model;
         DeviceVariantInformation deviceVarianrInfo = deviceInfo.getDeviceVariants().get(deviceInfo.getPreciseName());
         String deviceName = deviceVarianrInfo.getDeviceName();
         if (getValueAsString() != deviceName) {
            setValue(deviceName);
         }
      }
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public boolean showAsLocked() {
      return true;
   }
   
}
