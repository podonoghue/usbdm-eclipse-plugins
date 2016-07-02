package net.sourceforge.usbdm.gdb.commands;

import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.concurrent.Sequence;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlDMContext;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.mi.service.IMIExecutionDMContext;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.dsf.ui.viewmodel.datamodel.IDMVMContext;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IEnabledStateRequest;
import org.eclipse.debug.core.commands.IRestartHandler;

import net.sourceforge.usbdm.gdb.UsbdmGdbServer;
import net.sourceforge.usbdm.gdb.launch.UsbdmGdbRestartSequence;

@SuppressWarnings("restriction")
public class UsbdmGdbRestartTargetHandler  implements IRestartHandler {
   
   private final DsfSession         fSession;
   private final GdbLaunch          fLaunch;
   
   public UsbdmGdbRestartTargetHandler(DsfSession session, GdbLaunch launch) {
      fSession  = session;
      fLaunch   = launch;
   }

   private IDMVMContext getDataViewContext(final IDebugCommandRequest request) {
      if (request.getElements().length != 1 || !(request.getElements()[0] instanceof IDMVMContext)) {
         return null;
      }
      return (IDMVMContext) request.getElements()[0];
   }

   private ICommandControlDMContext getCommandContext(final IDMVMContext context) {
      if (context == null) {
         return null;
      }
      return DMContexts.getAncestorOfType(context.getDMContext(), ICommandControlDMContext.class);
   }

   private IMIExecutionDMContext getExecutionContext(final IDMVMContext context) {
      if (context == null) {
         return null;
      }
      return DMContexts.getAncestorOfType(context.getDMContext(), IMIExecutionDMContext.class);
   }

   @Override
   public void canExecute(final IEnabledStateRequest request) {
      
      final ICommandControlDMContext context = getCommandContext(getDataViewContext(request));
      if (context == null) {
         request.setEnabled(false);
         request.done();
         return;
      }
      request.setEnabled(true);
      request.done();
   }
   
   @Override
   public boolean execute(final IDebugCommandRequest request) {
      
      final IDMVMContext dataViewContext = getDataViewContext(request);
      if (dataViewContext == null) {
         request.setStatus(new Status(Status.ERROR, UsbdmGdbServer.getPluginId(), "UsbdmGdbRestartTargetHandler.execute() dataViewContext = null"));
         request.done();
         return false;
      }
      final IMIExecutionDMContext executionContext = getExecutionContext(dataViewContext);
      if (executionContext == null) {
         request.setStatus(new Status(Status.ERROR, UsbdmGdbServer.getPluginId(), "UsbdmGdbRestartTargetHandler.execute() executionContext = null"));
         request.done();
         return false;
      }
      Map<String, Object> attributes;
      try {
         attributes = fLaunch.getLaunchConfiguration().getAttributes();
      } catch (Exception e) {
         request.setStatus(new Status(Status.ERROR, UsbdmGdbServer.getPluginId(), "UsbdmGdbRestartTargetHandler.execute() failed to get attributes"));
         request.done();
         return false;
      }
      RequestMonitor rm = new RequestMonitor(fSession.getExecutor(), null) {
         @Override
         protected void handleCompleted() {
            request.setStatus(getStatus());
            request.done();
         }
      };
      Sequence seq = new UsbdmGdbRestartSequence(fSession, executionContext, attributes, rm);
      fSession.getExecutor().execute(seq);
      return false;
   }
 
}