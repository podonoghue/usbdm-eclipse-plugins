package tests.internal;

import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;

public class DeviceDatabaseTest {

   static class TestClass {
      void doTest(TargetType targetType) {
         try {
//          ArrayList<Usbdm.USBDMDeviceInfo> deviceList = Usbdm.getDeviceList();
//          ListIterator<Usbdm.USBDMDeviceInfo> it = deviceList.listIterator();
//          while (it.hasNext()) {
//             USBDMDeviceInfo di = it.next();
//             System.err.println("Device \n" + di);
//          }
            DeviceDatabase.getDeviceDatabase(targetType).listDevices(System.err, true);
//             database.toOptionXML(System.err);
//             database.toXML(System.err);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }
   
   /**
    * @param args
    */
   public static void main(String[] args) {
      System.err.println("Application Path : " + Usbdm.getApplicationPath().toPortableString());
      System.err.println("Resource Path    : " + Usbdm.getResourcePath().toPortableString());
      System.err.println("Data Path        : " + Usbdm.getDataPath().toPortableString());

      TestClass testClass = new TestClass();

      TargetType[] targetTypes = {
          TargetType.T_ARM,  
//          TargetType.T_CFV1, 
//          TargetType.T_CFVx, 
//            TargetType.T_MC56F80xx,
      };
      for (TargetType targetType:targetTypes) {
         testClass.doTest(targetType);
         try {
            Thread.sleep(5000);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
   }
}
