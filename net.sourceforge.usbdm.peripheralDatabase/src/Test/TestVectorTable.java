package Test;
import java.io.PrintWriter;
import java.io.StringWriter;

import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripheralsFactory;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

public class TestVectorTable {

   public TestVectorTable() {
   }

   static void testVectorTable(String device) throws Exception {

      // Get description of all peripherals for device
      DevicePeripheralsFactory factory = new DevicePeripheralsFactory();
      DevicePeripherals devicePeripherals = factory.getDevicePeripherals(device);
      if (devicePeripherals == null) {
         // Return empty model
         System.err.println("Failed");
         return;
      }
      VectorTable vt = devicePeripherals.getVectorTable();
      String cVectorTable = vt.getCVectorTableEntries();
      System.err.print(cVectorTable);
   }
   
   static void testHeaderFile(String device) {
      DevicePeripheralsFactory factory = new DevicePeripheralsFactory();
      DevicePeripherals devicePeripherals = factory.getDevicePeripherals(device);

      StringWriter stringWriter = new StringWriter();
      PrintWriter  printWriter  = null;
      try {
         printWriter = new PrintWriter(stringWriter);
         devicePeripherals.writeHeaderFile(printWriter);
      } catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         if (printWriter != null) {
            printWriter.close();
         }
      }
      System.err.print(stringWriter.toString());
   }
   
   public static void main(String[] args) throws Exception {
//      String device="MCF51JF128";
//      String device="MCF52259";
//      String device="MK20D5";
      String device="MKL25Z32M4";
      
      testVectorTable(device);
//      testHeaderFile(device);
   }

}
