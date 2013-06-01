//import java.util.ArrayList;
//import java.util.ListIterator;
//import net.sourceforge.usbdm.jni.Usbdm;
//import net.sourceforge.usbdm.jni.Usbdm.USBDMDeviceInfo;

import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.UsbdmJniConstants;


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
//         System.err.println("Application Path : " + Usbdm.getUsbdmApplicationPath());
//         System.err.println("Data Path        : " + Usbdm.getUsbdmDataPath());
         
//         DeviceDatabase database = new DeviceDatabase(UsbdmJniConstants.CFVX_DEVICE_FILE);
//         database.listDevices();
//         database.toXML(System.err);
         String[] targets = {UsbdmJniConstants.ARM_DEVICE_FILE, UsbdmJniConstants.CFV1_DEVICE_FILE, UsbdmJniConstants.CFVX_DEVICE_FILE};
         for (String target:targets) {
            DeviceDatabase database = new DeviceDatabase(target);
            database.toOptionXML(System.err);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
