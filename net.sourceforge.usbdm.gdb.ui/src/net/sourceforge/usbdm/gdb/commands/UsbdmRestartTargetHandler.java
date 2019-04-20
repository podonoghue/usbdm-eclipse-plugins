package net.sourceforge.usbdm.gdb.commands;

import org.eclipse.debug.core.commands.IRestartHandler;
import org.eclipse.debug.ui.actions.DebugCommandHandler;

/**
 * @since 5.1
 */
public class UsbdmRestartTargetHandler extends DebugCommandHandler {

   
   @Override
   protected Class<IRestartHandler> getCommandType() {
//      System.err.println("UsbdmRestartTargetHandler.getCommandType()");
      return IRestartHandler.class;
   }

}