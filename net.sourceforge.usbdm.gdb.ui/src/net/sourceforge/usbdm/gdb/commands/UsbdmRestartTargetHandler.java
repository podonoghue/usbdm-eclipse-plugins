package net.sourceforge.usbdm.gdb.commands;

import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.dsf.concurrent.CountingRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DsfExecutor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsTargetDMContext;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlDMContext;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.gdb.service.GDBBackend;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.IMIExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIRunControl;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.mi.service.command.commands.CLICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.dsf.ui.viewmodel.datamodel.IDMVMContext;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IEnabledStateRequest;
import org.eclipse.debug.core.commands.IRestartHandler;
import org.eclipse.debug.ui.DebugUITools;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.gdb.UsbdmGdbServer;

@SuppressWarnings({ "restriction" })
public class UsbdmRestartTargetHandler implements IRestartHandler, IHandler {
   
   @SuppressWarnings("unused")
   private final DsfExecutor        fExecutor;
   private final DsfServicesTracker fTracker;
   private final DsfSession         fSession;

   public UsbdmRestartTargetHandler(DsfSession session, GdbLaunch launch) {
      System.err.println("UsbdmRestartTargetHandler(DsfSession)");
      fSession  = session;
      fExecutor = session.getExecutor();
      fTracker  = new DsfServicesTracker(UsbdmGdbServer.getBundleContext(), session.getId());
   }

   public void dispose() {
      fTracker.dispose();
   }
   
   private ICommandControlDMContext getContext(final IDebugCommandRequest request) {
      if (request.getElements().length != 1 || !(request.getElements()[0] instanceof IDMVMContext)) {
         return null;
      }

      final IDMVMContext       context    = (IDMVMContext) request.getElements()[0];
      ICommandControlDMContext controlDmc = DMContexts.getAncestorOfType(context.getDMContext(), ICommandControlDMContext.class);
      
      return controlDmc;
   }

   @Override
   public void canExecute(final IEnabledStateRequest request) {
      System.err.println("UsbdmRestartTargetHandler.canExecute()");
      
      final ICommandControlDMContext context = getContext(request);
      if (context == null) {
         request.setEnabled(false);
         request.done();
         return;
      }

      request.setEnabled(true);
      request.done();
      
//      fExecutor.execute(new DsfRunnable() {
//         @Override
//         public void run() {
//            final TT funcService = fTracker.getService(TT.class);
//            if (funcService == null) {
//               request.setEnabled(false);
//               request.done();
//            } else {
//               funcService.canGetVersion(context, new DataRequestMonitor<Boolean>(fExecutor, null) {
//                  @Override
//                  protected void handleCompleted() {
//                     if (!isSuccess()) {
//                        request.setEnabled(false);
//                     } else {
//                        request.setEnabled(getData());
//                     }
//                     request.done();
//                  }
//               });
//            }
//         }
//      });

   }

   @Override
   public boolean execute(final IDebugCommandRequest request) {
      System.err.println("UsbdmRestartTargetHandler.execute()");
      Runnable r = new Runnable() {
         @Override
         public void run() {
            resetTarget(request);
         }
      };
      new Thread(r).start();
      return false;
   }
   
   private void resetTarget(final IDebugCommandRequest request) {
      
      final ICommandControlDMContext context = getContext(request);

      if (context == null) {
         System.err.println("RestartTargetHandler.resetTarget() context = null");
         request.setStatus(new Status(Status.ERROR, UsbdmGdbServer.getPluginId(), "RestartTargetHandler.resetTarget() dmContext = null"));
         request.done();
         return;
      }
      
//      System.err.println("RestartTargetHandler.resetTarget()");
      final IDMContext dmContext = getIDMContext();
      if (dmContext == null) {
         System.err.println("RestartTargetHandler.resetTarget() dmContext = null");
         request.setStatus(new Status(Status.ERROR, UsbdmGdbServer.getPluginId(), "RestartTargetHandler.resetTarget() dmContext = null"));
         request.done();
         return;
      }
      final IMIExecutionDMContext executionContext = getExecutionContext(dmContext);
      if (executionContext == null) {
         System.err.println("RestartTargetHandler.resetTarget() executionContext = null");
         request.setStatus(new Status(Status.ERROR, UsbdmGdbServer.getPluginId(), "RestartTargetHandler.resetTarget() executionContext = null"));
         request.done();
         return;
      }

      final IMIRunControl runControl = fTracker.getService(IMIRunControl.class);
      if (runControl == null) {
         System.err.println("RestartTargetHandler.resetTarget() runControl = null");
         request.setStatus(new Status(Status.ERROR, UsbdmGdbServer.getPluginId(), "RestartTargetHandler.resetTarget() runControl = null"));
         request.done();
         return;
      }
      final IBreakpointsTargetDMContext breakPointContext = getBreakpointContext(executionContext);
      if (breakPointContext == null) {
         System.err.println("RestartTargetHandler.resetTarget() breakPointContext = null");
         request.setStatus(new Status(Status.ERROR, UsbdmGdbServer.getPluginId(), "RestartTargetHandler.resetTarget() breakPointContext = null"));
         request.done();
         return;
      }

      ILaunch launch = (ILaunch)fSession.getModelAdapter(ILaunch.class);
      if (launch == null) {
         System.err.println("Failed to get launch configuration");
         request.setStatus(new Status(Status.ERROR, UsbdmGdbServer.getPluginId(), "Failed to get launch configuration"));
         request.done();
         return;
      }
      final ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
      
      // Check if target running
      DataRequestMonitor<Boolean> brm = new DataRequestMonitor<Boolean>(runControl.getExecutor(), null);
      runControl.canSuspend(executionContext, brm);
      if (brm.getData()) {
         // Suspend target 
//         System.err.println("RestartTargetHandler.resetTarget() suspending target");
         runControl.suspend(executionContext, new RequestMonitor(runControl.getExecutor(), null) {
            protected void handleSuccess(){
//               System.err.println("RestartTargetHandler.resetTarget() suspending target OK");
            }

            @Override
            protected void handleError() {
               System.err.println("RestartTargetHandler.resetTarget() suspending target Failed");
               GDBBackend gdbBackend = new GDBBackend(fSession, launchConfiguration);
               gdbBackend.interruptAndWait(100, this);
            }
         });
      }
      else {
//         System.err.println("RestartTargetHandler.resetTarget() not suspending target");
      }
      
      final IGDBControl fGdb = fTracker.getService(IGDBControl.class);
      if (fGdb == null) {
         System.err.println("RestartTargetHandler.resetTarget() IGDBControl = null");
         return;
      }
//      System.err.println("RestartTargetHandler.resetTarget() IGDBControl created");
      
      boolean restartUsesStartup = UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_USES_STARTUP;
      boolean doSetPc            = IGDBJtagConstants.DEFAULT_SET_PC_REGISTER;
      String  pcValue            = IGDBJtagConstants.DEFAULT_PC_REGISTER;
      boolean doStopAt           = IGDBJtagConstants.DEFAULT_SET_STOP_AT;
      String  stopAtAddress      = IGDBJtagConstants.DEFAULT_STOP_AT;
      boolean doResume           = IGDBJtagConstants.DEFAULT_SET_STOP_AT;
      try {
         restartUsesStartup = 
               launchConfiguration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_USES_STARTUP, restartUsesStartup);
         if (restartUsesStartup) {
            doSetPc       = launchConfiguration.getAttribute(IGDBJtagConstants.ATTR_SET_PC_REGISTER, doSetPc);
            pcValue       = launchConfiguration.getAttribute(IGDBJtagConstants.ATTR_PC_REGISTER,     pcValue);
            doStopAt      = launchConfiguration.getAttribute(IGDBJtagConstants.ATTR_SET_STOP_AT,     doStopAt);
            stopAtAddress = launchConfiguration.getAttribute(IGDBJtagConstants.ATTR_STOP_AT,         stopAtAddress);
            doResume      = launchConfiguration.getAttribute(IGDBJtagConstants.ATTR_SET_RESUME,      doResume);
         }
         else {
            doSetPc       = launchConfiguration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_PC_REGISTER,               
                  UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_SET_PC_REGISTER);
            pcValue       = launchConfiguration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_PC_REGISTER,                   
                  UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_PC_REGISTER);
            doStopAt      = launchConfiguration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_STOP_AT,                   
                  UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_SET_STOP_AT);
            stopAtAddress = launchConfiguration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_STOP_AT,                       
                  UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_STOP_AT);
            doResume      = launchConfiguration.getAttribute(UsbdmSharedConstants.LAUNCH_ATTR_RESTART_SET_RESUME,  
                  UsbdmSharedConstants.LAUNCH_DEFAULT_RESTART_SET_RESUME);
         }
      } catch (CoreException e) {
         e.printStackTrace();
      }
      CountingRequestMonitor crm = new CountingRequestMonitor(fGdb.getExecutor(), null) {
         @Override
         protected void handleError() {
            System.err.println("RestartTargetHandler.resetTarget() executionContext = null");
            request.setStatus(new Status(Status.ERROR, UsbdmGdbServer.getPluginId(), getStatus().getMessage()));
            request.done();
            return;
         }
      };
      crm.setDoneCount(4);

      CommandFactory factory = fGdb.getCommandFactory();

      fGdb.getCommandFactory();

      // Step 1
      fGdb.queueCommand(new CLICommand<MIInfo>(fGdb.getContext(), "monitor reset"), 
            new DataRequestMonitor<MIInfo>(fGdb.getExecutor(), crm));

      // Step 1a
      fGdb.queueCommand(new CLICommand<MIInfo>(fGdb.getContext(), "flushregs"), 
            new DataRequestMonitor<MIInfo>(fGdb.getExecutor(), crm));

      // Step 2
      if (doSetPc) {
         fGdb.queueCommand(
               factory.createMIGDBSet(getIDMContext(), new String[] {"$pc=" + pcValue}), 
               new DataRequestMonitor<MIInfo>(fGdb.getExecutor(), crm));
      }
      else {
         crm.done();
      }
      // Step 3
      if (doStopAt) {
         // Set temporary restart breakpoint 
         fGdb.queueCommand(new CLICommand<MIInfo>(fGdb.getContext(), "tb " + stopAtAddress), 
               new DataRequestMonitor<MIInfo>(fGdb.getExecutor(), crm));
//         fGdb.queueCommand(
//               factory.createMIBreakInsert(breakPointContext, true, false, null, 0, stopAtAddress, null), 
//               new DataRequestMonitor<MIBreakInsertInfo>(fGdb.getExecutor(), crm));
      }
      else {
         crm.done();
      }
      // Step 4
      if (doResume) {
         // Continue from reset
         fGdb.queueCommand(factory.createMIExecContinue(executionContext),
               new DataRequestMonitor<MIInfo>(fGdb.getExecutor(), crm));
      }
      else {
         // Single step at reset location to synchronize GDB
         fGdb.queueCommand(factory.createMIExecStep(executionContext),
               new DataRequestMonitor<MIInfo>(fGdb.getExecutor(), crm));
      }
   }

   private IDMContext getIDMContext() {
      IDMContext idmContext = null;
      IAdaptable debugContext = DebugUITools.getDebugContext();
      if (debugContext != null) {
         idmContext = (IDMContext)debugContext.getAdapter(IDMContext.class);
      }
      return idmContext;
   }

   private IMIExecutionDMContext getExecutionContext(IDMContext idmContext) {
      return DMContexts.getAncestorOfType(idmContext, IMIExecutionDMContext.class);
   }

   private IBreakpointsTargetDMContext getBreakpointContext(IMIExecutionDMContext executionContext) {
      return  DMContexts.getAncestorOfType(executionContext, IBreakpointsTargetDMContext.class);
   }

   @Override
   public void addHandlerListener(IHandlerListener arg0) {
      // TODO Auto-generated method stub
      
   }

   @Override
   public Object execute(ExecutionEvent arg0) throws ExecutionException {
      System.err.println("UsbdmRestartTargetHandler.execute()");
      return null;
   }

   @Override
   public boolean isEnabled() {
      System.err.println("UsbdmRestartTargetHandler.isEnabled()");
      return true;
   }

   @Override
   public boolean isHandled() {
      System.err.println("UsbdmRestartTargetHandler.isHandled()");
      return false;
   }

   @Override
   public void removeHandlerListener(IHandlerListener arg0) {
      System.err.println("UsbdmRestartTargetHandler.removeHandlerListener()");
   }

//   private ICommandControlDMContext getControlContext(IDMContext idmContext) {
//      return  DMContexts.getAncestorOfType(idmContext, ICommandControlDMContext.class);
//   }

}