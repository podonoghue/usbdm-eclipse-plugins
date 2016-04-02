package net.sourceforge.usbdm.deviceEditor.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInformation;

public class DevicePackageModel extends SelectionModel {

   private DeviceInfo fDeviceInfo;
   ArrayList<String>  fDeviceNames = new ArrayList<String>();
   
   public DevicePackageModel(BaseModel parent, DeviceInfo deviceInfo) {
      super(parent, "Device", "");

      fDeviceInfo = deviceInfo;
      
      for (String deviceName:fDeviceInfo.getDevices().keySet()) {
         DeviceInformation deviceInformation = fDeviceInfo.getDevices().get(deviceName);
         fDeviceNames.add(deviceInformation.getName());
      }
      
      fValues     = fDeviceNames.toArray(new String[fDeviceNames.size()]);
      fSelection  = 0;
   }

   @Override
   public void setValue(String value) {
      super.setValue(value);
      fDeviceInfo.setDeviceName(value);
      refresh();
   }

}
