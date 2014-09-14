//import java.util.ArrayList;
//import java.util.ListIterator;
//import net.sourceforge.usbdm.jni.Usbdm;
//import net.sourceforge.usbdm.jni.Usbdm.USBDMDeviceInfo;

import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;


public class DeviceDatabaseTest {

   /**
    * @param args
    */
   public static void main(String[] args) {
      try {
//         ArrayList<Usbdm.USBDMDeviceInfo> deviceList = Usbdm.getDeviceList();
//         ListIterator<Usbdm.USBDMDeviceInfo> it = deviceList.listIterator();
//         while (it.hasNext()) {
//            USBDMDeviceInfo di = it.next();
//            System.err.println("Device \n" + di);
//         }
         System.err.println("Application Path : " + Usbdm.getApplicationPath().toPortableString());
         System.err.println("Resource Path    : " + Usbdm.getResourcePath().toPortableString());
         System.err.println("Data Path        : " + Usbdm.getDataPath().toPortableString());
         
         TargetType[] targetTypes = {
               TargetType.T_ARM,  
               TargetType.T_CFV1, 
               TargetType.T_CFVx, 
               };
         for (TargetType targetType:targetTypes) {
            DeviceDatabase database = new DeviceDatabase(targetType);
            database.listDevices(System.err);
//            database.toOptionXML(System.err);
//            database.toXML(System.err);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
