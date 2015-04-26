package net.sourceforge.usbdm.cdt.ui.handlers;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.gdb.GdbServerParameters;


public class CfvxGdbServerHandler extends GdbServerHandler {

   public CfvxGdbServerHandler() throws Exception {
      super(GdbServerParameters.getDefaultServerParameters(InterfaceType.T_CFVX));
   }
}
