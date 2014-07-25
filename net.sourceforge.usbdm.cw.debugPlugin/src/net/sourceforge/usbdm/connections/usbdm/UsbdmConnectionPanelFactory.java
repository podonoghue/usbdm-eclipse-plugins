package net.sourceforge.usbdm.connections.usbdm;

import org.eclipse.swt.widgets.Composite;

import com.freescale.cdt.debug.cw.core.ui.publicintf.AbstractPhysicalConnectionPanel;
import com.freescale.cdt.debug.cw.core.ui.publicintf.IPhysicalConnectionPanelFactory;
import com.freescale.cdt.debug.cw.core.ui.publicintf.ISettingsListener;

public class UsbdmConnectionPanelFactory 
implements IPhysicalConnectionPanelFactory {

   public AbstractPhysicalConnectionPanel createComposite( ISettingsListener  listener,
                                                           Composite          parent, 
                                                           int                swtstyle, 
                                                           String             protocolPlugin, 
                                                           String             connectionTypeId)  {
//      System.err.println("UsbdmConnectionPanelFactory::createComposite()");
      UsbdmConnectionPanel panel = null;
      if (connectionTypeId.equalsIgnoreCase(UsbdmCommon.RS08_TypeID)) {
         panel = new UsbdmRS08ConnectionPanel(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
      }
      else if (connectionTypeId.equalsIgnoreCase(UsbdmCommon.HCS08_TypeID)) {
         panel = new UsbdmHCS08ConnectionPanel(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
      }
      else if (connectionTypeId.equalsIgnoreCase(UsbdmCommon.CFV1_TypeID)) {
         panel = new UsbdmCFV1ConnectionPanel(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
      }
      else if (connectionTypeId.equalsIgnoreCase(UsbdmCommon.CFVx_TypeID)) {
         panel = new UsbdmCFVxConnectionPanel(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
      }
      else if (connectionTypeId.equalsIgnoreCase(UsbdmCommon.ARM_TypeID)) {
         panel = new UsbdmARMConnectionPanel(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
      }
      else if (connectionTypeId.equalsIgnoreCase(UsbdmCommon.DSC_TypeID)) {
         panel = new UsbdmDSCConnectionPanel(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
      }
      else if (connectionTypeId.equalsIgnoreCase(UsbdmCommon.S12Z_TypeID)) {
         panel = new UsbdmS12zConnectionPanel(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
      }
      if (panel != null) {
         System.err.println("UsbdmConnectionPanelFactory() - connectionTypeId = "+connectionTypeId);
         panel.create();
      }
      else {
         System.err.println("UsbdmConnectionPanelFactory() - unknown connectionTypeId");
      }
      return panel;
   }
}
