package tests.internal;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.gdb.ui.UsbdmDebuggerPanel;

/**
 * @since 5.1
 */
public class TestUsbdmDebuggerPanel {

   /**
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);

      shell.setLayout(new FillLayout());

      UsbdmDebuggerPanel usbdmTab = new UsbdmDebuggerPanel();
      usbdmTab.createContents(shell, true);
      usbdmTab.setInterface(InterfaceType.T_ARM, false);
      try {
         usbdmTab.initializeFrom(null);
      } catch (Exception e) {
         e.printStackTrace();
      }
      usbdmTab.addListener(SWT.CHANGED, new Listener() {
         @Override
         public void handleEvent(Event event) {
            // System.err.println("Changed");
         }
      });
      shell.open();
      shell.setSize(800, 800);
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }

}
