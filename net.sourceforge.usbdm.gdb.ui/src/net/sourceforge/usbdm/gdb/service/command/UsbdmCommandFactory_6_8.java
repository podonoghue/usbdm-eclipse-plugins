package net.sourceforge.usbdm.gdb.service.command;

/*
 * Revised to directly extend CommandFactory_6_8
 */
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.debug.service.command.ICommand;
import org.eclipse.cdt.dsf.gdb.service.command.CommandFactory_6_8;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;

import net.sourceforge.usbdm.gdb.service.command.commands.MIExecInterrupt;
import net.sourceforge.usbdm.gdb.service.command.commands.MIExecReset;

/**
 * @since 5.1
 */
public class UsbdmCommandFactory_6_8 extends CommandFactory_6_8 {
   
   public ICommand<MIInfo> createMIExecReset(IExecutionDMContext dmc) {
      return new MIExecReset(dmc);
   }

   public ICommand<MIInfo> createMIExecReset(IExecutionDMContext dmc, boolean allThreads) {
      return new MIExecReset(dmc, allThreads);
   }

   public ICommand<MIInfo> createMIExecReset(IExecutionDMContext dmc, String groupId) {
      return new MIExecReset(dmc, groupId);
   }

   @Override
   public ICommand<MIInfo> createMIExecInterrupt(IExecutionDMContext dmc) {
      return new MIExecInterrupt(dmc);
   }

   @Override
   public ICommand<MIInfo> createMIExecInterrupt(IExecutionDMContext dmc, boolean allThreads) {
      return new MIExecInterrupt(dmc, allThreads);
   }

   @Override
   public ICommand<MIInfo> createMIExecInterrupt(IExecutionDMContext dmc, String groupId) {
      return new MIExecInterrupt(dmc, groupId);
   }

}
