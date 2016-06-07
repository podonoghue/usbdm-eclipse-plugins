package Tests;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.deviceEditor.editor.DeviceEditor;

public class TestDeviceEditor {

   static void testEditor(Path path) {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Device Editor");
      shell.setLayout(new FillLayout());

      Composite composite = new Composite(shell, SWT.NONE);
      composite.setBackground(new Color(display, 255, 0, 0));
      composite.setLayout(new FillLayout());

      DeviceEditor editor = new DeviceEditor();
      editor.init(path);
      editor.createPartControl(composite);
      
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      editor.doSave(null);
      display.dispose();
   }

   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      try {
//       testEditor(Paths.get("projectFiles/Tests/FRDM_K20D50M.usbdmProject"));
//       testEditor(Paths.get("projectFiles/Tests/FRDM_K22F.usbdmProject"));
//       testEditor(Paths.get("projectFiles/Tests/FRDM_K22F1M0.usbdmProject"));
//       testEditor(Paths.get("projectFiles/Tests/FRDM_K64F.usbdmProject"));
       testEditor(Paths.get("projectFiles/Tests/FRDM_K66F.usbdmProject"));
//       testEditor(Paths.get("projectFiles/Tests/MK22F12810.usbdmProject"));
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
