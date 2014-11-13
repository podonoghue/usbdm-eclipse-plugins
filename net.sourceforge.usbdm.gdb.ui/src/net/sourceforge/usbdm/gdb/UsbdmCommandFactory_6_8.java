package net.sourceforge.usbdm.gdb;

import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsTargetDMContext;
import org.eclipse.cdt.dsf.debug.service.command.ICommand;
import org.eclipse.cdt.dsf.mi.service.command.commands.MIBreakInsert;
import org.eclipse.cdt.dsf.mi.service.command.output.MIBreakInsertInfo;

public class UsbdmCommandFactory_6_8 extends UsbdmCommandFactory {

   @Override 
   public ICommand<MIBreakInsertInfo> createMIBreakInsert(IBreakpointsTargetDMContext ctx, String func) {
      return new MIBreakInsert(ctx, func, true);
   }

   @Override
   public ICommand<MIBreakInsertInfo> createMIBreakInsert(IBreakpointsTargetDMContext ctx, boolean isTemporary, 
         boolean isHardware, String condition, int ignoreCount, String line, int tid) {
      return new MIBreakInsert(ctx, isTemporary, isHardware, condition, ignoreCount, line, tid, true);
   }

   @Override
   public ICommand<MIBreakInsertInfo> createMIBreakInsert(IBreakpointsTargetDMContext ctx, boolean isTemporary,
         boolean isHardware, String condition, int ignoreCount, String location, int tid, boolean disabled, boolean isTracepoint) {
      return new MIBreakInsert(ctx, isTemporary, isHardware, condition, ignoreCount, location, tid, disabled, isTracepoint, true);
   }
}
