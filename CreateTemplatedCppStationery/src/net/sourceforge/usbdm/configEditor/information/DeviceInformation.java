package net.sourceforge.usbdm.configEditor.information;

public class DeviceInformation {
   String            fName;
   DevicePackage     fPackage;
   String            fManual;
   
   public DeviceInformation(String name, DevicePackage devicePackage, String manual) {
      fName    = name;
      fPackage = devicePackage;
      fManual  = manual;
   }
   
   public String getName() {
      return fName;
   }
   
   public String getManual() {
      return fManual;
   }
   
   public DevicePackage getPackage() {
      return fPackage;
   }
   
   @Override
   public String toString() {
      return "Device("+fName+", "+fPackage+", "+fManual+")";
   }
}
