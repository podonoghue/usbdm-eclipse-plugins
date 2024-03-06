package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Cluster;
import net.sourceforge.usbdm.peripheralDatabase.Field;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripheralDatabase.Register;

/**
 * Class encapsulating the code for writing an instance of UART
 */
public class WriterForSim extends PeripheralWithState {

   public WriterForSim(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "System Integration Module";
   }

   @Override
   public int getPriority() {
      return 1100;
   }

   private void extractClock(Cluster cluster) throws Exception {
      if (cluster instanceof Register) {
         if (cluster.getName().startsWith("SCGC")) {
            Register reg = (Register) cluster;
            for (Field f:reg.getFields()) {
//               Pattern p = Pattern.compile("SCGC%");
               String key = makeKey("/"+f.getName()+"/_clockInfo");
               addOrIgnoreParam(key, reg.getName()+","+f.getName());
            }
         }
      }
      else {
         for (Cluster cl : cluster.getRegisters()) {
            extractClock(cl);
         }
      }
   }
   
   @Override
   public void extractHardwareInformation(Peripheral dbPeripheral) {
      for (Cluster cl:dbPeripheral.getRegisters()) {
         try {
            extractClock(cl);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      super.extractHardwareInformation(dbPeripheral);
   }
   
   
}