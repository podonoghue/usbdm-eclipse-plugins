package Tests;

import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.deviceEditor.editor.TreeEditor;
import net.sourceforge.usbdm.deviceEditor.model.DeviceModel;
import net.sourceforge.usbdm.deviceEditor.model.ModelFactory;

public class TestDeviceEditor {

   static void createEditor(Composite parent, DeviceModel deviceModel) {
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayout(new FillLayout());
      TreeEditor view = new TreeEditor();
      view.createControls(composite);
      view.setModel(deviceModel);
   }

   static void testEditor(ModelFactory factory) {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Device Editor");
      shell.setLayout(new FillLayout());

      Composite composite = new Composite(shell, SWT.NONE);
      composite.setBackground(new Color(display, 255, 0, 0));
      composite.setLayout(new FillLayout());

      createEditor(composite, factory.getPeripheralModel());
      createEditor(composite, factory.getPinModel());
      
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }

   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      try {
         ModelFactory factory = ModelFactory.createModel(Paths.get("xml/pin_mapping-MK22FA12_64p.hardware"));
//         ModelFactory factory = ModelFactory.createModel(Paths.get("data/MK22FA12_64p.csv"));
         testEditor(factory);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }


}
