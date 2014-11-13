package net.sourceforge.usbdm.cdt.ui.handlers;

import net.sourceforge.usbdm.cdt.ui.Activator;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;

import org.eclipse.cdt.core.parser.util.DebugUtil;
import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.dsf.concurrent.CountingRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsTargetDMContext;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlDMContext;
import org.eclipse.cdt.dsf.debug.ui.viewmodel.launch.StateChangedEvent;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.IMIExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIProcesses;
import org.eclipse.cdt.dsf.mi.service.IMIRunControl;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.mi.service.command.commands.CLICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIBreakInsertInfo;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.mi.service.command.output.MIThread;
import org.eclipse.cdt.dsf.mi.service.command.output.MIThreadInfoInfo;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.dsf.ui.viewmodel.datamodel.IDMVMContext;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.sourcelookup.ISourceLookupResult;
import org.eclipse.ui.IWorkbenchPage;

@SuppressWarnings({ "restriction" })
public class ResetTargetHandler extends AbstractHandler implements IDebugContextListener {

   public ResetTargetHandler() {
      //      System.err.println("ResetTarget()");
      DebugUITools.getDebugContextManager().addDebugContextListener(this);
   }

   @Override
   public Object execute(ExecutionEvent event) throws ExecutionException {
      resetTarget();
      return null;
   }

   private void resetTarget() {
   //      System.err.println("ResetTarget.resetTarget()");
      final IDMContext dmContext = getIDMContext();
      if (dmContext == null) {
         System.err.println("ResetTargetHandler.resetTarget() dmContext = null");
         return; 
      }
      final IMIExecutionDMContext executionContext = getExecutionContext(dmContext);
      if (executionContext == null) {
         System.err.println("ResetTargetHandler.resetTarget() executionContext = null");
         return; 
      }
      final DsfSession dsfSession = DsfSession.getSession(dmContext.getSessionId());
      if (dsfSession == null) {
         System.err.println("ResetTargetHandler.resetTarget() dsfSession = null");
         return; 
      }
      DsfServicesTracker tracker = new DsfServicesTracker(Activator.getBundleContext(), dsfSession.getId());

      final IMIRunControl runControl = tracker.getService(IMIRunControl.class);
      if (runControl == null) {
         System.err.println("ResetTargetHandler.resetTarget() runControl = null");
         return;
      }
      final IBreakpointsTargetDMContext breakPointContext = getBreakpointContext(executionContext);
      if (breakPointContext == null) {
         System.err.println("ResetTargetHandler.resetTarget() breakPointContext = null");
         return;
      }
      // Check if target running
      DataRequestMonitor<Boolean> brm = new DataRequestMonitor<Boolean>(runControl.getExecutor(), null);
      runControl.canSuspend(executionContext, brm);
      if (brm.getData()) {
         // Suspend target 
         runControl.suspend(executionContext, new RequestMonitor(runControl.getExecutor(), null));
      }

      final IGDBControl fGdb = tracker.getService(IGDBControl.class);
      if (fGdb == null) {
         return;
      }
      
      ILaunch launch = (ILaunch)dsfSession.getModelAdapter(ILaunch.class);
      ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
      
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
//         @Override
//         protected void handleSuccess() {
//            System.err.println("ResetTargetHandler.resetTarget() crm.handleSuccess()");
//            super.handleSuccess();
//         }
      };
      crm.setDoneCount(4);

      CommandFactory factory = fGdb.getCommandFactory();
      
      fGdb.getCommandFactory();

      // Step 1
      fGdb.queueCommand(new CLICommand<MIInfo>(fGdb.getContext(), "monitor reset"), 
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
         fGdb.queueCommand(
               factory.createMIBreakInsert(breakPointContext, true, false, null, 0, stopAtAddress, 0), 
               new DataRequestMonitor<MIBreakInsertInfo>(fGdb.getExecutor(), crm));
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

   IDMContext getIDMContext() {
      IDMContext idmContext = null;
      IAdaptable debugContext = DebugUITools.getDebugContext();
      if (debugContext != null) {
         idmContext = (IDMContext)debugContext.getAdapter(IDMContext.class);
      }
      return idmContext;
   }

   IMIExecutionDMContext getExecutionContext(IDMContext idmContext) {
      return DMContexts.getAncestorOfType(idmContext, IMIExecutionDMContext.class);
   }

   IBreakpointsTargetDMContext getBreakpointContext(IMIExecutionDMContext executionContext) {
      return  DMContexts.getAncestorOfType(executionContext, IBreakpointsTargetDMContext.class);
   }

   ICommandControlDMContext getControlContext(IDMContext idmContext) {
      return  DMContexts.getAncestorOfType(idmContext, ICommandControlDMContext.class);
   }

   @SuppressWarnings({ "unused" })
   private void sendSessionChanged(DsfSession dsfSession) {
      /*
       * Doesn't work - don't know why
       */
//      System.err.println("ResetTarget.sendSessionChanged()");
            DsfServicesTracker tracker = new DsfServicesTracker(Activator.getBundleContext(), dsfSession.getId());
            IMIProcesses procService = tracker.getService(IMIProcesses.class); 
      //      IMIContainerDMContext containerDmc = procService.createContainerContextFromGroupId( 
      //            tracker.getService(IGDBControl.class).getContext(),
      //            MIProcesses.UNIQUE_GROUP_ID);

      //      @SuppressWarnings("restriction")
      //      StateChangedEvent event = new StateChangedEvent(getContext()); 
      //      dsfSession.dispatchEvent(event, null);

      Object context = DebugUITools.getDebugContext();
      if(null != context && context instanceof IDMVMContext) {
         context = ((IDMVMContext)context).getDMContext();
      }
      if(null != context && context instanceof IDMContext) {
         // We create a StateChangedEvend with the current debug context
         StateChangedEvent changeEvent = new StateChangedEvent((IDMContext) context);
         dsfSession.dispatchEvent(changeEvent, null); // <- does not work, content of debug views remains the same
//        // We try to resend latest suspend event
//        ISuspendedDMEvent suspEvent = DSFEventProvider.getSuspendedEvent(dsfSession);
//        dsfSession.dispatchEvent(suspEvent, null); // <- does not refresh current frame, editor... if program counter was changed
      } 
   }

   @Override
   public void debugContextChanged(DebugContextEvent event) {
//      System.err.println("ResetTarget.debugContextChanged()");
      IDMContext executionContext = getIDMContext();
      setBaseEnabled(executionContext != null);
   }

   //   void save() {
   //
   //    IViewReference[] viewReferences = HandlerUtil.getActiveSite(event).getPage().getViewReferences();
   //    for (IViewReference viewReference : viewReferences) {
   //       System.err.println(String.format("id = %s", viewReference.getId()));
   //    }

   //    IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
   //    IWorkbenchSite site = HandlerUtil.getActiveSite(event);
   //    IWorkbenchWindow workbenchWindow = site.getWorkbenchWindow();
   //    ISelection selection = workbenchWindow.getSelectionService().getSelection();

   //    try {
   //       visited = null;
   //       investigate("workbenchWindow.getSelectionService().getSelection()", workbenchWindow.getSelectionService().getSelection());
   //       
   //       if (selection instanceof TreeSelection) {
   //          TreeSelection treeSelection = (org.eclipse.jface.viewers.TreeSelection) selection;
   //          System.err.println(String.format("\ntreeSelection.getClass() = %s", treeSelection.getClass().toString()));
   //          investigate("treeSelection.getFirstElement()", treeSelection.getFirstElement());
   //       }
   //
   //       
   //       investigate("DebugUITools.getDebugContextManager().getContextService(window)", 
   //             DebugUITools.getDebugContextManager().getContextService(window));
   //       
   //       investigate("DebugUITools.getDebugContext()", DebugUITools.getDebugContext());
   //
   //       investigate("DebugUITools.getCurrentProcess()", DebugUITools.getCurrentProcess());
   //    } finally {
   //       visited = null;
   //    }
   //    MessageDialog.openInformation(window.getShell(), "Info", "Info for you");
   //   }

   //   private HashSet<Object> visited = null;
   //
   //   private void investigate(String message, Object object) {
   //      if (visited == null) {
   //         visited = new HashSet<Object>();
   //      }
   //      if (visited.contains(object)) {
   //         return;
   //      }
   //      visited.add(object);
   //      IDMContext idmContext = null;
   //      if (object instanceof IDMContext) {
   //         idmContext = (IDMContext) object;
   //      }
   //      else if (object instanceof IAdaptable) {
   //         idmContext = (IDMContext)((IAdaptable)object).getAdapter(IDMContext.class);
   //      }
   //      if (idmContext != null) {
   //         System.err.println(String.format("DSF session ID = %s", idmContext.getSessionId()));
   //
   //         final IMIExecutionDMContext execDmc = DMContexts.getAncestorOfType(idmContext, IMIExecutionDMContext.class);
   //         if (execDmc != null) {
   //            System.err.println(String.format("execDmc = %s", execDmc.toString()));
   //            return;
   //         }
   //      }
   //      System.err.println("\n=== [ " + message + " ] ==========================================================");
   //      System.err.println(String.format("object.getClass() = %s",  (object==null)?"null":object.getClass().toString()));
   //      if (!(object instanceof IAdaptable)) {
   ////         System.err.println("object is not instanceof IAdaptable");
   //         return;
   //      }
   //
   //      IAdaptable adaptable = (IAdaptable) object;
   //      
   //      Class<?> classes[] = {
   //            IExecutionDMContext.class,
   //            DsfSession.class,
   //            IThread.class,
   //            IDebugTarget.class,
   //            IStep.class,
   //            ICDITarget.class,
   //            GdbLaunch.class,
   //            IMemoryBlockRetrieval.class,
   //            IDMContext.class,
   //      };
   //      for (Class<?> clazz : classes) {
   //         Object instance = clazz.cast(adaptable.getAdapter(clazz));
   //         if ((instance != null)) {
   //            System.err.println(String.format("%40s = %s", clazz.getName(), instance.getClass().toString()));
   //            if (instance != object) {
   //               investigate(String.format("%s.getAdapter(%s)", message, clazz.getSimpleName()), instance);
   //            }
   //         }
   //      }
   //   }

   //   @Override
   //   public void debugContextChanged(DebugContextEvent context) {
   //      IAdaptable adaptable = null;
   //      if(context instanceof IAdaptable) {
   //         adaptable = (IAdaptable) context;
   //         IExecutionDMContext executionContext = (IExecutionDMContext)adaptable.getAdapter(IExecutionDMContext.class);
   //         System.err.println(String.format("executionContext.getClass() = %s",  (executionContext==null)?"null":executionContext.getClass().toString()));
   //      }
   //   }

}
