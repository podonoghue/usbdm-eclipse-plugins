package net.sourceforge.usbdm.gdb.launch;

import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.RequestMonitorWithProgress;
import org.eclipse.cdt.dsf.service.DsfSession;

import net.sourceforge.usbdm.gdb.server.GdbServerParameters;

/**
 * Version for GDB 7.2 and higher.
 * @since 4.12
 */
public class UsbdmGdbDsfFinalLaunchSequence_7_2 extends UsbdmGdbDsfFinalLaunchSequence {

   public UsbdmGdbDsfFinalLaunchSequence_7_2(DsfSession session, Map<String, Object> attributes, RequestMonitorWithProgress rm, GdbServerParameters gdbServerParameters) {
      super(session, attributes, rm, gdbServerParameters);
   }
}
