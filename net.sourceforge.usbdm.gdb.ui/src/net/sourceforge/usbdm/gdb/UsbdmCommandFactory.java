package net.sourceforge.usbdm.gdb;

import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.debug.service.command.ICommand;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;

public class UsbdmCommandFactory extends CommandFactory {

   public ICommand<MIInfo> createMIExecReset(IExecutionDMContext dmc) {
      return new MIExecReset(dmc);
   }

}
