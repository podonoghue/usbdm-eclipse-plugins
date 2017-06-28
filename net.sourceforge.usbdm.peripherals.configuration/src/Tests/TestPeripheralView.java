package Tests;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.peripherals.configuration.views.PeripheralConfigurationEditor;

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
         shell.setText("Peripheral Properties");
         shell.setLayout(new FillLayout());

         Composite composite = new Composite(shell, SWT.NONE);
         composite.setBackground(new Color(display, 255, 0, 0));
         composite.setLayout(new FillLayout());

         PeripheralConfigurationEditor view = new PeripheralConfigurationEditor();
         view.createPartControl(composite);

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
