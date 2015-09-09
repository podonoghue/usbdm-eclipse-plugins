package net.sourceforge.usbdm.gdb;

import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.command.commands.MICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;

/**
 * @since 4.12
 */
public class MIExecReset extends MICommand<MIInfo> {
   public MIExecReset(IExecutionDMContext dmc) {
      this(dmc, false);
  }

  /**
   * @since 1.1
   */
  public MIExecReset(IExecutionDMContext dmc, boolean allThreads) {
    this(dmc, allThreads, null);
  }

  /**
  * @since 3.0
  */
  public MIExecReset(IExecutionDMContext dmc, String groupId) {
    this(dmc, false, groupId);
  }
  
  /*
   * The parameters allThreads and groupId are mutually exclusive.  allThreads must be false
   * if we are to use groupId.  The value of this method is to only have one place
   * where we use the hard-coded strings.
   */
  private MIExecReset(IExecutionDMContext dmc, boolean allThreads, String groupId) {
      super(dmc, "mon reset"); //$NON-NLS-1$
  }
}
