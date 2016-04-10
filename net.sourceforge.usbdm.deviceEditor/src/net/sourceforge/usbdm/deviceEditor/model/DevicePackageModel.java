package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

public class DevicePackageModel extends StringModel implements IModelChangeListener {

   private final DeviceInfo fDeviceInfo;
   
   /**
    * Device package
    * 
    * @param parent     Parent node
    * @param deviceInfo Device info used to construct the node
    */
   public DevicePackageModel(BaseModel parent, DeviceInfo deviceInfo) {
      super(parent, "Package", "Device package", deviceInfo.getDeviceVariant().getPackage().getName());
      fDeviceInfo = deviceInfo;
      deviceInfo.addListener(this);
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof DeviceInfo) {
         DeviceInfo deviceInfo = (DeviceInfo) model;
         setValue(deviceInfo.getDeviceVariant().getPackage().getName());
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel model) {
   }

   @Override
   protected void removeMyListeners() {
      fDeviceInfo.addListener(this);
   }
   
}
