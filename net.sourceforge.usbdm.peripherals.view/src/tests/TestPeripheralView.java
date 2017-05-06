package tests;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.peripheralDatabase.SVDIdentifier;
import net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel;
import net.sourceforge.usbdm.peripherals.view.UsbdmDevicePeripheralsView;

public class TestPeripheralView {

   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      try {
         Display display = new Display();

         Shell shell = new Shell(display);
         shell.setText("Task List - TableViewer Example");
         shell.setLayout(new FillLayout());

         Composite composite = new Composite(shell, SWT.NONE);
         composite.setBackground(new Color(display, 255, 0, 0));
         composite.setLayout(new FillLayout());

         UsbdmDevicePeripheralsView view = new UsbdmDevicePeripheralsView();

         String os    = System.getProperty("os.name");            
         System.err.println("os.name      => "+os );

         Path path = null;
         if ((os != null) && os.toUpperCase().contains("LINUX")) {
            path = Paths.get("/usr/share/usbdm/DeviceData/Device.SVD/Internal/");
         } else {
            path = Paths.get("C:/Users/podonoghue/Documents/Development/USBDM/usbdm-eclipse-makefiles-build/PackageFiles/DeviceData/Device.SVD/Internal/");
         }
//
//         DeviceFileList fileList = new DeviceFileList(path.resolve("DeviceList.xml"));
//         Path name = fileList.getSvdFilename("LPC11U24_401");
//         System.err.println("Name = " + name);

         view.createPartControl(composite);
         //         SVDIdentifier               svdId = new SVDIdentifier(path.resolve("MKM33Z5.svd.xml"));
         SVDIdentifier               svdId = new SVDIdentifier(path.resolve("LPC13xx.svd.xml"));
         //         SVDIdentifier               svdId = new SVDIdentifier(path.resolve("MK22F51212.svd.xml"));
         UsbdmDevicePeripheralsModel peripheralsModel = UsbdmDevicePeripheralsModel.createModel(null, svdId);

         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MKL25Z4.svd.xml", null);
         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MK20D5.svd.xml", null);
         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MK10D10.svd.xml", null);
         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MK11D5.svd.xml", null);
         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MK64F12.svd.xml", null);
         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MCF5225x.svd.xml", null);
         //      peripheralsModel = new UsbdmDevicePeripheralsModel(path+"MCF51JF.svd.xml", null);
         // Try illegal path/name
         //      peripheralsModel = new UsbdmDevicePeripheralsModel("xxxx", null);

         view.sessionStarted(peripheralsModel);

         shell.open();
         while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
               display.sleep();
         }
         display.dispose();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }


}
