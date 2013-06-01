import org.eclipse.swt.widgets.Composite;

import com.freescale.cdt.debug.cw.core.ui.publicintf.AbstractPhysicalConnectionPanel;

import net.sourceforge.usbdm.connections.usbdm.*;

public final class USBDMConnectionPanelTestFactory {
   
   public static AbstractPhysicalConnectionPanel createComposite(Composite parent, int swtstyle) {
//      UsbdmConnectionPanel panel = new UsbdmARMConnectionPanel(parent, swtstyle);
//      UsbdmConnectionPanel panel = new UsbdmCFVxConnectionPanel(parent, swtstyle);
//      UsbdmConnectionPanel panel = new UsbdmCFV1ConnectionPanel(parent, swtstyle);
//      UsbdmConnectionPanel panel = new UsbdmRS08ConnectionPanel(parent, swtstyle);
      UsbdmConnectionPanel panel = new UsbdmHCS08ConnectionPanel(parent, swtstyle);
//      UsbdmConnectionPanel panel = new UsbdmDSCConnectionPanel(parent, swtstyle);
//      UsbdmConnectionPanel panel = new UsbdmHCS12ConnectionPanel(parent, swtstyle);
      panel.create();
      return panel;
   }
}
