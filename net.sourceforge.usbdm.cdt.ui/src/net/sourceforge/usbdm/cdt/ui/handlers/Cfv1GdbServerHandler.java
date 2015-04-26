package net.sourceforge.usbdm.cdt.ui.handlers;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.gdb.GdbServerParameters;


public class Cfv1GdbServerHandler extends GdbServerHandler {

   public Cfv1GdbServerHandler() throws Exception {
      super(GdbServerParameters.getDefaultServerParameters(InterfaceType.T_CFV1));
   }
}
