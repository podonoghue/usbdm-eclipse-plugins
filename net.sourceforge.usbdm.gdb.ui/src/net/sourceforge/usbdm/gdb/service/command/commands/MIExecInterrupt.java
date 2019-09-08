package net.sourceforge.usbdm.gdb.service.command.commands;

import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.command.commands.MICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;

/**
 * @since 5.2
 */
public class MIExecInterrupt extends MICommand<MIInfo> {

   public MIExecInterrupt(IExecutionDMContext dmc) {
      this(dmc, false);
   }

   public MIExecInterrupt(IExecutionDMContext dmc, boolean allThreads) {
      this(dmc, allThreads, null);
   }

   public MIExecInterrupt(IExecutionDMContext dmc, String groupId) {
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
   private MIExecInterrupt(IExecutionDMContext dmc, boolean allThreads, String groupId) {
	   super(dmc, "-exec-interrupt");
	   if (allThreads) {
		   setParameters(new String[] { "--all" });
	   } else if (groupId != null) {
		   setParameters(new String[] { "--thread-group", groupId });
	   }
   }
}
