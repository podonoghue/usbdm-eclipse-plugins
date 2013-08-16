package net.sourceforge.usbdm.cdt.ui.handlers;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.gdb.GdbServerParameters;


public class ArmGdbServerHandler extends GdbServerHandler {

   public ArmGdbServerHandler() {
      super(GdbServerParameters.getDefaultServerParameters(InterfaceType.T_ARM));
   }
}
