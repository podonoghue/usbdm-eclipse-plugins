package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceVariantInformation;
import net.sourceforge.usbdm.jni.UsbdmException;

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
      super(parent, "Variant", "");

      fDeviceInfo = deviceInfo;

      for (String deviceName:fDeviceInfo.getDeviceVariants().keySet()) {
         DeviceVariantInformation deviceInformation = fDeviceInfo.getDeviceVariants().get(deviceName);
         fDeviceNames.add(deviceInformation.getName());
      }
      fChoices    = fDeviceNames.toArray(new String[fDeviceNames.size()]);
      fSelection  = fDeviceNames.indexOf(fDeviceInfo.getVariantName());
      if (fSelection<0) {
         fSelection = 0;
         try {
            fDeviceInfo.setVariantName(fChoices[0]);
         } catch (UsbdmException e) {
            e.printStackTrace();
         }
      }
      fDeviceInfo.addListener(this);
   }

   public void setValueAsString(String value) {
      int oldSelection = fSelection;
      super.setValueAsString(value);
      if (oldSelection != fSelection) {
         try {
            fDeviceInfo.setVariantName(value);
         } catch (UsbdmException e) {
            e.printStackTrace();
         }
         refresh();
      }
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof DeviceInfo) {
         DeviceInfo deviceInfo = (DeviceInfo) model;
         String variantName = deviceInfo.getVariantName();
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

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
   }

}
