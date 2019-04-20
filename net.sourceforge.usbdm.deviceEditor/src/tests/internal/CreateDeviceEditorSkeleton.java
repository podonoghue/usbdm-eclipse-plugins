package tests.internal;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.usbdm.peripheralDatabase.Cluster;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripheralDatabase.Register;
import net.sourceforge.usbdm.peripheralDatabase.SVDIdentifier;

public class CreateDeviceEditorSkeleton {
   
   public static Map<String, String> getPeripherals(String name) {
      final Path path = Paths.get("C:/Users/podonoghue/Documents/Development/USBDM/usbdm-eclipse-makefiles-build/PackageFiles/Stationery/Device.SVD/Internal/");
      HashMap<String, String> map = new HashMap<String, String>();
      
      SVDIdentifier svdId = new SVDIdentifier(path.resolve(name));
      
      DevicePeripherals devicePeripherals;
      try {
         devicePeripherals = svdId.getDevicePeripherals();
         for (Peripheral peripheral:devicePeripherals.getPeripherals()) {
            String filename;
            String pName = peripheral.getName();
            while (peripheral.getDerivedFrom() != null) {
               peripheral = peripheral.getDerivedFrom();
            }
            filename = peripheral.getSourceFilename();
//            System.err.println(String.format("Peripheral %-20s %-20s", pName, filename));
            map.put(pName, filename.toLowerCase());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return map;
   }

   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
//      Map<String, String> map = getPeripherals("MK66F18.svd.xml");
      Map<String, String> map = getPeripherals("MK20D5.svd.xml");
      for (String key:map.keySet()) {
         String p = map.get(key);
         System.err.println(String.format("%-20s => %-20s", key, p));
      }
   }

   @SuppressWarnings("unused") // Experiment
   private static void createPeripheralModel(Peripheral peripheral) {
      System.err.println("Peripheral = " + peripheral.getName());
      for (Cluster cluster : peripheral.getRegisters()) {
         if (cluster instanceof Register) {
            createRegisterModels((Register)cluster);
         }
         else {
            createClusterModels(cluster);
         }
      }
}

   private static void createClusterModels(Cluster cluster) {
      System.err.println("Cluster = " + cluster.getName());
      if (cluster.getDimension()>0) {
         // Cluster which is an array
         for (int clusterIndex=0; clusterIndex<cluster.getDimension(); clusterIndex++) {
            Register singleVisibleRegister = (Register) cluster.getSingleVisibleRegister();
            if ((singleVisibleRegister != null) && (singleVisibleRegister.getDimension() == 0)) {
               // Omit cluster wrapper for a single, simple register
               System.err.println("Cluster = " + cluster.getName() + "[]");
               continue;
            }
            for (Cluster register : cluster.getRegisters()) {
               if (register.getDimension()>0) {
                  for (int registerIndex=0; registerIndex<register.getDimension(); registerIndex++) {
                     System.err.println("Cluster = " + cluster.getName() + "[]");
                  }
               }
               else {
                  System.err.println("Cluster = " + cluster.getName() + "[]");
               }
            }
         }
      }
      else {
         // A simple cluster
         for (Cluster register : cluster.getRegisters()) {
            System.err.println("RegisterX = " + register.getName());
         }
      }
   }

   private static void createRegisterModels(Register register) {
      if (register.getDimension() == 0) {
         // Simple register
         System.err.println("Register = " + register.getName());
      }
      else {
         // The register is an array - Create individual registers for each element
         for (int regIndex=0; regIndex<register.getDimension(); regIndex++) {
            System.err.println("Register = " + register.getName() + "["+regIndex+"]");
         }
      }
   
   }


}
