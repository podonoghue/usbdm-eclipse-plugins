package tests;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.SVDIdentifier;
import net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel;
import net.sourceforge.usbdm.peripherals.view.DevicePeripheralSelectionDialogue;

public class TestDevicePeripheralSelectionDialogue {

   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);
      shell.setText("Selection");
      shell.setLayout(new FillLayout());

      DevicePeripheralSelectionDialogue dialogue;
      try {
         //      SVDIdentifier svdIdentifier = new SVDIdentifier(Paths.get("C:/Program Files (x86)/pgo/USBDM 4.11.1.70/DeviceData/Device.SVD/Freescale/MK10D7.svd.xml"));
//         SVDIdentifier svdIdentifier = new SVDIdentifier("[SVDIdentifier:usbdm.arm.devices:FRDM_K64F]");
         SVDIdentifier svdIdentifier = new SVDIdentifier("[SVDIdentifier:path=C:/Users/podonoghue/Documents/Development/USBDM/usbdm-eclipse-plugins/net.sourceforge.usbdm.peripherals.stmicro/data/STM32F40x.svd.xml]");
         //      SVDIdentifier svdIdentifier = new SVDIdentifier("[SVDIdentifier:usbdm.arm.devices:S9KEAZN8]");
         svdIdentifier = new SVDIdentifier(svdIdentifier.toString());
         dialogue = new DevicePeripheralSelectionDialogue(shell, svdIdentifier);
      } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
         return;
      }
      //      DeviceSelectDialogue dialogue = new DeviceSelectDialogue(shell, "C:/Users/podonoghue/Development/USBDM/ARM_Devices/Generated/STMicro/STM32F40x.svd.xml");
      int result = dialogue.open();
      if (result != Window.OK) {
         // Cancelled etc
         System.err.println("**** Cancelled ****");
         return;
      }
      SVDIdentifier svdID = dialogue.getSVDId();
      System.err.println("svdID = " + svdID);
      try {
         System.err.println("svdID.getDeviceName() = " + svdID.getDeviceName());
         DevicePeripherals devicePeripherals = svdID.getDevicePeripherals();
         System.err.println("svdID.getDevicePeripherals() = " + devicePeripherals);

         System.err.println("devicePeripherals.getName() = " + devicePeripherals.getName());
         UsbdmDevicePeripheralsModel peripheralModel = UsbdmDevicePeripheralsModel.createModel(null, svdID);
         System.err.println("peripheralModel = " + peripheralModel);
      } catch (Exception e) {
         e.printStackTrace();
      }

      display.dispose();
   }

}
