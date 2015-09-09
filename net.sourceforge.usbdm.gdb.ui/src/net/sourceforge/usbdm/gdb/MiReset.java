package net.sourceforge.usbdm.gdb;

import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.mi.service.command.commands.MICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;

/**
 * @since 4.12
 */
public class MiReset extends MICommand<MIInfo> {

   public MiReset(IDMContext ctx, String operation) {
      super(ctx, "monitor remote");
   }

}
