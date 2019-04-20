package tests.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.deviceDatabase.ui.DeviceSelectorPanel;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;

public class TestDeviceSelectionPanel {

   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);
      shell.setText("Device Selector");
      shell.setSize(500, 400);
      shell.setBackground(new Color(display, 255,0,0));
      shell.setLayout(new FillLayout());
      
      DeviceSelectorPanel deviceSelectorPanel = new DeviceSelectorPanel(shell, SWT.NONE);
      deviceSelectorPanel.setTargetType(TargetType.T_ARM);
      deviceSelectorPanel.setDevice("MK20DX128M5");
      
      shell.layout();
      
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }

}
