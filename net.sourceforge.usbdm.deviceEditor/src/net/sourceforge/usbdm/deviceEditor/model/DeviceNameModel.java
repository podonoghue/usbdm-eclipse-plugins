package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

public class DeviceNameModel extends StringModel implements IModelChangeListener {

   /**
    * Device package
    * 
    * @param parent     Parent node
    * @param deviceInfo Device info used to construct the node
    */
   public DeviceNameModel(BaseModel parent, DeviceInfo deviceInfo) {
      super(parent, "Generic device Name", "Device name for this variant", deviceInfo.getVariant().getDeviceName());
      deviceInfo.addListener(this);
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof DeviceInfo) {
         DeviceInfo deviceInfo = (DeviceInfo) model;
         String packageName = deviceInfo.getVariant().getPackage().getName();
         if (getValueAsString() != packageName) {
            setValue(packageName);
         }
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel model) {
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
   }

   @Override
   public boolean showAsLocked() {
      return true;
   }
   
}
