package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

public class DevicePackageModel extends StringModel implements IModelChangeListener {

   /**
    * Device package
    * 
    * @param parent     Parent node
    * @param deviceInfo Device info used to construct the node
    */
   public DevicePackageModel(BaseModel parent, DeviceInfo deviceInfo) {
      super(parent, "Package", "Device package", deviceInfo.getVariant().getPackage().getName());
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
