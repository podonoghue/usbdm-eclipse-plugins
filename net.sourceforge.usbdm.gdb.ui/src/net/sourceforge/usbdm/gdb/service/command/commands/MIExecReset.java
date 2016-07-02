package net.sourceforge.usbdm.gdb.service.command.commands;

import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.command.commands.MICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;

public class MIExecReset extends MICommand<MIInfo> {

   public MIExecReset(IExecutionDMContext dmc) {
      this(dmc, false);
   }

   public MIExecReset(IExecutionDMContext dmc, boolean allThreads) {
      this(dmc, allThreads, null);
   }

   public MIExecReset(IExecutionDMContext dmc, String groupId) {
      this(dmc, false, groupId);
   }

   /**
    * Convenience function
    * allThreads & groupId exclusive
    * 
    * @param dmc           Execution context
    * @param allThreads    Apply to all threads or not
    * @param groupId       Apply to group
    * 
    */
   private MIExecReset(IExecutionDMContext dmc, boolean allThreads, String groupId) {
      super(dmc, "mon reset"); //$NON-NLS-1$
   }
}
