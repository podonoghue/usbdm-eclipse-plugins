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
      super(parent, "Device", "");
      
      fDeviceInfo = deviceInfo;
      fDeviceInfo.addListener(this);
      
      for (String deviceName:fDeviceInfo.getDeviceVariants().keySet()) {
         DeviceVariantInformation deviceInformation = fDeviceInfo.getDeviceVariants().get(deviceName);
         fDeviceNames.add(deviceInformation.getName());
      }
      fChoices     = fDeviceNames.toArray(new String[fDeviceNames.size()]);
      fSelection  = fDeviceNames.indexOf(fDeviceInfo.getDeviceVariantName());
      if (fSelection<0) {
         fSelection = 0;
         fDeviceInfo.setDeviceVariant(fChoices[0]);
      }
   }

   public void setValueAsString(String value) {
      super.setValueAsString(value);
      fDeviceInfo.setDeviceVariant(value);
      refresh();
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof DeviceInfo) {
         DeviceInfo deviceInfo = (DeviceInfo) model;
         String variantName = deviceInfo.getDeviceVariantName();
         setValueAsString(variantName);
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
   }

   @Override
   protected void removeMyListeners() {
      fDeviceInfo.removeListener(this);
   }

}
