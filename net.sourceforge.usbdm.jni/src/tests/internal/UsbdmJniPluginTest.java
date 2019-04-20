package tests.internal;
import java.util.ArrayList;

//import org.eclipse.swt.SWT;
//import org.eclipse.swt.widgets.Display;
//import org.eclipse.swt.widgets.MessageBox;
//import org.eclipse.swt.widgets.Shell;

//import org.eclipse.swt.SWT;
//import org.eclipse.swt.widgets.Display;
//import org.eclipse.swt.widgets.MessageBox;
//import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.Usbdm.CustomReg;
import net.sourceforge.usbdm.jni.Usbdm.ExtendedOptions;
import net.sourceforge.usbdm.jni.Usbdm.MemorySpace;
import net.sourceforge.usbdm.jni.Usbdm.Reg;
import net.sourceforge.usbdm.jni.Usbdm.ResetType;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.jni.Usbdm.TargetVddSelect;
import net.sourceforge.usbdm.jni.Usbdm.USBDMDeviceInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

public class UsbdmJniPluginTest {

   public static void listDevices() throws UsbdmException {
      // List USBDM devices
      ArrayList<Usbdm.USBDMDeviceInfo>    deviceList = Usbdm.getDeviceList();
      if (deviceList.size() == 0) {
         System.err.println("No USBDM devices found");
         return;
      }
      for (USBDMDeviceInfo device:deviceList) {
         System.err.println(
               "==========================================\n" +
               "Device \n" + device +
               "\n==========================================\n");
      }
   }
   
   public static void dumpHex(int address, byte data[]) {
      final int width = 16;
      System.err.print("          ");         
      for (int index=0; index<width; index++) {
         System.err.print(String.format("%2X ", index));         
      }
      System.err.print("\n         ");         
      for (int index=0; index<width; index++) {
         System.err.print("===");         
      }
      System.err.println();         
      if ((address%width)!=0) {
         System.err.print(String.format("%08X: ", address&~address));         
         for (int index=0; index<address%width; index++) {
            System.err.print("   ");         
         }
      }
      for (int index=0; index<data.length; index++, address++) {
         if ((address%width)==0) {
            System.err.print(String.format("%08X: ", address));         
         }
         System.err.print(String.format("%02X ", data[index]));         
         if ((address%width)==(width-1)) {
            System.err.println();         
         }
      }
      if ((address%width)!=0) {
         System.err.println();         
      }
   }
   
   public static void testKinetis() throws UsbdmException {
      TargetType target = TargetType.T_ARM;

      ExtendedOptions options = Usbdm.getDefaultExtendedOptions(target);
      System.err.println("Default options: " + options);
      options.targetVdd = TargetVddSelect.BDM_TARGET_VDD_3V3;
      Usbdm.setExtendedOptions(options);
      
      Usbdm.setTargetType(target);
      
      options = Usbdm.getExtendedOptions();
      System.err.println("Current options: " + options);

      Usbdm.targetReset(ResetType.RESET_SPECIAL);
      Usbdm.targetConnect();
      Usbdm.targetHalt();
            
      final int memSize = 80;
      final int flashAdress = 0x00000000;
      final int ramAddress  = 0x20000000;
      byte data[] = new byte[memSize];
      Usbdm.readMemory(MemorySpace.Byte,  memSize, flashAdress, data);
      System.err.println("Flash Read = ");
      dumpHex(flashAdress, data);
      
      Usbdm.writeMemory(MemorySpace.Long, memSize, ramAddress, data);
      Usbdm.readMemory(MemorySpace.Long, memSize, ramAddress, data);
      System.err.println("RAM Write & Read = ");
      dumpHex(ramAddress, data);
      
      Usbdm.writeReg(Usbdm.Reg.ARM_RegR0, 100);
      {
         // Test Custom registers
         CustomReg customReg  = new Usbdm.CustomReg(Reg.ARM_RegR0);
         int registerValue = Usbdm.readReg(customReg);
         System.err.println(String.format("customReg \'%s\' = 0x%08X", customReg.name(), registerValue));
         System.err.println(String.format("customReg \'%s\' = 0x%08X", customReg.toString(), registerValue));
         
      }
      int i = 0;
      for (Reg register = Reg.ARM_RegR0;
                    register.ordinal() <= Reg.ARM_RegR12.ordinal();
                    register = Reg.values()[register.ordinal()+1]) {
         int registerValue;
         Usbdm.writeReg(register, i++);
         registerValue = Usbdm.readReg(register);
         if (registerValue != i-1) {
            System.err.println(String.format("Failed Read/Write to %s = 0x%08X", register.name(), registerValue));
         }
         }
      for (Reg register = Reg.ARM_RegR0;
                    register.ordinal() < Reg.ARM_RegMISC.ordinal();
                    register = Reg.values()[register.ordinal()+1]) {
         int registerValue;
         registerValue = Usbdm.readReg(register);
         System.err.println(String.format("%s = 0x%08X", register.name(), registerValue));
         }
      
//      int registerValue = Usbdm.readReg(Reg.ARM_RegMDM_AP_Status);
      
      Usbdm.controlPins(Usbdm.PIN_RESET_LOW);
      Usbdm.controlPins(Usbdm.PIN_RELEASE);
      
      // Close device
      System.err.println("Closing device: " + Usbdm.getBDMDescription());
      Usbdm.close();
   }
   
   public static void testCFV1() throws UsbdmException {
      TargetType target = TargetType.T_CFV1;
      
      ExtendedOptions options = Usbdm.getDefaultExtendedOptions(target);
      System.err.println("Default options: " + options);
      options.targetVdd = TargetVddSelect.BDM_TARGET_VDD_3V3;
      Usbdm.setExtendedOptions(options);

      // Initially disable target power (for tower boards)
      Usbdm.setTargetVdd(TargetVddSelect.BDM_TARGET_VDD_DISABLE);
      try {
         Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
      // Opens target device
      Usbdm.setTargetType(target);
      
      options = Usbdm.getExtendedOptions();
      System.err.println("Current options: " + options);

      Usbdm.targetConnect();

      Usbdm.targetHalt();
            
      final int memSize = 80;
      final int flashAdress = 0x00000000;
      final int ramAddress  = 0x20000000;
      byte data[] = new byte[memSize];
      Usbdm.readMemory(MemorySpace.Byte,  memSize, flashAdress, data);
      System.err.println("Flash Read = ");
      dumpHex(flashAdress, data);
      
      Usbdm.writeMemory(MemorySpace.Long, memSize, ramAddress, data);
      Usbdm.readMemory(MemorySpace.Long, memSize, ramAddress, data);
      System.err.println("RAM Write & Read = ");
      dumpHex(ramAddress, data);
      
      {
         // Test Custom registers
         CustomReg customReg = new Usbdm.CustomReg(Usbdm.Reg.CFV1_RegPC);
         int registerValue = Usbdm.readReg(customReg);
         System.err.println(String.format("customReg \'%s\' = 0x%08X", customReg.name(), registerValue));
         System.err.println(String.format("customReg \'%s\' = 0x%08X", customReg.toString(), registerValue));
         
      }
      // Test register read & write
      int i = 0;
      for (Reg register = Reg.CFV1_RegD0;
                    register.ordinal() <= Reg.CFV1_RegA7.ordinal();
                    register = Reg.values()[register.ordinal()+1]) {
         int registerValue;
         Usbdm.writeReg(register, i++);
         registerValue = Usbdm.readReg(register);
         if (registerValue != i-1) {
            System.err.println(String.format("Failed Read/Write to %s = 0x%08X", register.name(), registerValue));
         }
         }
      for (Reg register = Reg.CFV1_RegD0;
            register.ordinal() <= Reg.CFV1_RegA7.ordinal();
                    register = Reg.values()[register.ordinal()+1]) {
         int registerValue;
         registerValue = Usbdm.readReg(register);
         System.err.println(String.format("%s = 0x%08X", register.name(), registerValue));
         }
      
      Usbdm.controlPins(Usbdm.PIN_RESET_LOW);
      Usbdm.controlPins(Usbdm.PIN_RELEASE);
      
      // Close device
      System.err.println("Closing device: " + Usbdm.getBDMDescription());
      Usbdm.close();
   }

//   public static void testDatabase() {
//      try {
//         ArrayList<Usbdm.USBDMDeviceInfo> deviceList = Usbdm.getDeviceList();
//         ListIterator<Usbdm.USBDMDeviceInfo> it = deviceList.listIterator();
//         while (it.hasNext()) {
//            USBDMDeviceInfo di = it.next();
//            System.err.println("Device \n" + di);
//         }
//         System.err.println("Application Path : " + Usbdm.getUsbdmApplicationPath());
//         System.err.println("Data Path        : " + Usbdm.getUsbdmDataPath());
//         
//         DeviceDatabase database = new DeviceDatabase(UsbdmJniConstants.ARM_DEVICE_FILE);
//         database.listDevices();
//      } catch (Exception opps) {
//         System.err.println("Opps exception :"+opps.toString());
//      }
//   }

   static void listEnvironment() {
	   String envs[] = {
		   "os.name",
		   "java.vm.specification.version",
		   "java.vm.specification.name",
		   "java.version",
		   "java.vm.version",
		   "java.vm.name",
		   "java.compiler",
		   "java.library.path",
		   "os.arch",
	   };
	   for (String env : envs) {
		   String prop = System.getProperty(env);
		   System.err.println(env + " => " + prop);
	   } 
   }
   
   /**
    * @param args
    * @throws InterruptedException 
    * @throws UsbdmException 
    */
   public static void main(String[] args) throws InterruptedException, UsbdmException {
      String errorMessage = null;
//      testDatabase();
      try {
         listEnvironment();

//         CustomClassLoader ccl = new CustomClassLoader();
//         
//         Class<?> ca = ccl.findClass("net.sourceforge.usbdm.jni.Usbdm");
//         Object a = ca.newInstance();
//         ccl = null;
//         System.gc();

         // Print USBDM paths
         System.err.println("Application Path : " + Usbdm.getApplicationPath().toOSString());
         System.err.println("Resource Path    : " + Usbdm.getResourcePath().toOSString());
         System.err.println("Data Path        : " + Usbdm.getDataPath().toOSString());

//         listDevices();
         
         // Get count of devices (creates internal list)
         int deviceCount = Usbdm.findDevices();
         if (deviceCount == 0) {
            System.err.println("No USBDM devices found");
            return;
         }
         System.err.println("No of USBDM devices found " + deviceCount);
         // Open last device
         Usbdm.open(deviceCount-1);

         System.err.println("Opened device: " + Usbdm.getBDMDescription());

         //         testCFV1();
         testKinetis();
         Usbdm.close();

      } catch (Exception e) {
         e.printStackTrace();
         errorMessage = e.getMessage();
         if (errorMessage == null) {
            errorMessage = e.toString();
         }
         if (errorMessage == null) {
            errorMessage = "Unknown exception";
         }
      }
      finally {
         Usbdm.exit();
       if (errorMessage != null) {
    	   System.console().format("%s", errorMessage);
       }
//         if (errorMessage != null) {
//            Shell shell;
//            // Find the default display and get the active shell
//            final Display disp = Display.getDefault();
//            if (disp == null) {
//               shell = new Shell(new Display());
//            }
//            else {
//               shell = new Shell(disp);
//            }
//            MessageBox msgbox = new MessageBox(shell, SWT.OK);
//            msgbox.setText("USBDM Error");
//            msgbox.setMessage(errorMessage);
//            msgbox.open();
//         }
      }
   }
}
