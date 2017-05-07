package net.sourceforge.usbdm.peripherals.usbdm;

import java.util.Vector;

import net.sourceforge.usbdm.peripheralDatabase.DefaultPeripheralDescriptionProvider;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;

public class UsbdmPeripheralDescriptionProvider extends DefaultPeripheralDescriptionProvider {

   static final String LICENSE = 
         "GNU GENERAL PUBLIC LICENSE\n" +
         "Version 2, June 1991";
   
   static final String DESCRIPTION = 
         "USBDM SVD files for ARM devices\n"
         + "Based on Freesale documentation";
   
   static final String NAME =
         "USBDM device files";

   public static final String ID =
         "usbdm.arm.devices";

   /**
    * Constructor for Freescale device peripherals library
    */
   public UsbdmPeripheralDescriptionProvider() {
      super(); // Uses default USBDM directory
      setLicense(LICENSE);
      setName(NAME);
      setDescription(DESCRIPTION);
      setId(ID);
      }
   
   public static void main(String[] args) throws Exception {
      UsbdmPeripheralDescriptionProvider provider = new UsbdmPeripheralDescriptionProvider();
      Vector<String> fileNames = provider.getDeviceNames();
      for (String s : fileNames) {
         System.err.println("Name = " + s);
      }
      DevicePeripherals devicePeripherals = provider.getDevicePeripherals("LPC11Uxx");
      System.err.println(devicePeripherals);
   }

}
