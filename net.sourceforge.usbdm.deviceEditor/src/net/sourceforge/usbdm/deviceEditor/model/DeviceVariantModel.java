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
      
      deviceInfo.addListener(this);

      fDeviceInfo = deviceInfo;
      
      for (String deviceName:fDeviceInfo.getDeviceVariants().keySet()) {
         DeviceVariantInformation deviceInformation = fDeviceInfo.getDeviceVariants().get(deviceName);
         fDeviceNames.add(deviceInformation.getName());
      }
      fValues     = fDeviceNames.toArray(new String[fDeviceNames.size()]);
      fSelection  = fDeviceNames.indexOf(deviceInfo.getDeviceVariantName());
      if (fSelection<0) {
         fSelection = 0;
         deviceInfo.setDeviceVariant(fValues[0]);
      }
   }

   @Override
   public void setValue(String value) {
      super.setValue(value);
      fDeviceInfo.setDeviceVariant(value);
      refresh();
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof DeviceInfo) {
         DeviceInfo deviceInfo = (DeviceInfo) model;
         String variantName = deviceInfo.getDeviceVariantName();
         if (variantName  != fValues[fSelection]) {
            setValue(variantName);
            refresh();
         }
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
   }

}
