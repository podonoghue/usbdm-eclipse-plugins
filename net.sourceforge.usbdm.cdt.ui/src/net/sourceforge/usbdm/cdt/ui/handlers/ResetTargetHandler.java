package net.sourceforge.usbdm.cdt.ui.handlers;

import net.sourceforge.usbdm.cdt.ui.Activator;

import org.eclipse.cdt.dsf.concurrent.CountingRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.IMIExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIRunControl;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.mi.service.command.commands.CLICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;

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

   public void resetTarget() {
//      System.err.println("ResetTarget.resetTarget()");
      
      IMIExecutionDMContext executionContext = getExecutionContext();
      if (executionContext == null) {
         return; 
      }
      DsfSession dsfSession = DsfSession.getSession(executionContext.getSessionId());
      if (dsfSession == null) {
         return; 
      }
      DsfServicesTracker tracker = new DsfServicesTracker(Activator.getBundleContext(), dsfSession.getId());

      final IMIRunControl runControl = tracker.getService(IMIRunControl.class);
      if (runControl == null) {
         System.err.println("GDBInterface.privateResetTarget(DSF) runControl = null");
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
      CountingRequestMonitor rm = new CountingRequestMonitor(fGdb.getExecutor(), null) {
         @Override
         protected void handleCompleted() {
//            System.err.println("GDBInterface.privateResetTarget(DSF) - sequence completed");
            super.handleCompleted();
         }
      };
      rm.setDoneCount(2);
      fGdb.queueCommand(new CLICommand<MIInfo>(fGdb.getContext(), "monitor reset"), 
            new DataRequestMonitor<MIInfo>(fGdb.getExecutor(), rm));

      CommandFactory factory = fGdb.getCommandFactory();

      fGdb.queueCommand(factory.createMIExecStep(executionContext),
            new DataRequestMonitor<MIInfo>(fGdb.getExecutor(), rm));
   }
   
   IMIExecutionDMContext getExecutionContext() {
      IMIExecutionDMContext execDmc = null;
      IAdaptable debugContext = DebugUITools.getDebugContext();
      if (debugContext != null) {
         IDMContext idmContext = (IDMContext)debugContext.getAdapter(IDMContext.class);
         if (idmContext != null) {
            execDmc = DMContexts.getAncestorOfType(idmContext, IMIExecutionDMContext.class);
         }
      }
      return execDmc;
   }

   @Override
   public void debugContextChanged(DebugContextEvent event) {
//      System.err.println("ResetTarget.debugContextChanged()");
      IMIExecutionDMContext executionContext = getExecutionContext();
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

   
   //
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
