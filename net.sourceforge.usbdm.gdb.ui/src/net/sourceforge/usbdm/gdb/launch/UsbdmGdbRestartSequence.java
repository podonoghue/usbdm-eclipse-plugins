/*******************************************************************************
 * Modified Peter O'Donoghue for USBDM
 *******************************************************************************/

package net.sourceforge.usbdm.gdb.launch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.dsf.concurrent.CountingRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.concurrent.Sequence;
import org.eclipse.cdt.dsf.gdb.service.IGDBBackend;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.IMIExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIProcesses;
import org.eclipse.cdt.dsf.mi.service.IMIRunControl;
import org.eclipse.cdt.dsf.mi.service.command.commands.CLICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.gdb.server.GdbServerParameters;
import net.sourceforge.usbdm.gdb.ui.Activator;

/**
 * Runs sequence to restart target
 */
public class UsbdmGdbRestartSequence extends Sequence {

   private final Map<String, Object>   fAttributes;
   private final DsfSession            fSession;
   private final IMIExecutionDMContext fExecutionContext;
   private boolean                     fDoSyncTarget = false;

   private GdbServerParameters         fGdbServerParameters;
   private IGDBControl                 fCommandControl;
   private IGDBBackend	               fGDBBackend;
   private IMIProcesses                fProcService;
   private UsbdmGdbInterface           fUsbdmGdbInterface;
   private DsfServicesTracker          fTracker;
   private IMIRunControl               fRunControl;
   
   boolean fDoSetPc            = UsbdmSharedConstants.DEFAULT_SET_PC_REGISTER;
   String  fPcValue            = UsbdmSharedConstants.DEFAULT_PC_REGISTER_VALUE;
   boolean fDoStopAt           = UsbdmSharedConstants.DEFAULT_DO_STOP_AT_MAIN;
   String  fStopAtAddress      = UsbdmSharedConstants.DEFAULT_RESTART_STOP_AT_MAIN_ADDRESS;
   boolean fDoResume           = UsbdmSharedConstants.DEFAULT_DO_RESUME;

   public UsbdmGdbRestartSequence(
         DsfSession session, 
         IMIExecutionDMContext executionContext, 
         Map<String, Object> attributes, 
         RequestMonitor rm) {
      
      super(session.getExecutor(), rm);
      
      fAttributes          = attributes;
      fSession             = session;
      fExecutionContext    = executionContext;
   }
   
   @Override
   public Step[] getSteps() {
      return fSteps;
   }

   /** Utility method; cuts down on clutter */
   /**
    * Queue commands to  CLICommand
    *    
    * @param commands Commands to queue
    * @param rm       RM to report to
    */
   private void queueCommands(Collection<String> commands, RequestMonitor rm) {
      /*
       * I prefer the commands separated
       */
      if (!commands.isEmpty()) { 
         CountingRequestMonitor crm = new CountingRequestMonitor(getExecutor(), rm);
         crm.setDoneCount(commands.size());
         for (String command:commands) {
            fCommandControl.queueCommand(
                  new CLICommand<MIInfo>(fCommandControl.getContext(), command),
                  new DataRequestMonitor<MIInfo>(getExecutor(), crm));
         }
      }
      else {
         rm.done();
      }
   }

   /**
    * The steps to execute
    */
   Step[] fSteps = new Step[] {

         new Step() {
            /**
             * Roll-back is first step to ensure it occurs 
             */
            @Override
            public void rollBack(RequestMonitor rm) {
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.rollBack()");
               if (fTracker != null) {
                  fTracker.dispose();
                  fTracker = null;
               }
               rm.done();
            }
         },
         new Step() {
            /** 
             * Initialize the members of the class.
             * This step is mandatory for the rest of the sequence to complete.
             */
            @Override
            public void execute(RequestMonitor rm) {
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepInitRestartSequence()");

               fTracker    = new DsfServicesTracker(Activator.getBundleContext(), fSession.getId());
               fGDBBackend = fTracker.getService(IGDBBackend.class);
               if (fGDBBackend == null) {
//                  System.err.println("Cannot obtain GDBBackend service");
                  rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Cannot obtain GDBBackend service")); //$NON-NLS-1$
                  return;
               }
               fUsbdmGdbInterface = new UsbdmGdbInterface();

               fCommandControl = fTracker.getService(IGDBControl.class);
               if (fCommandControl == null) {
//                  System.err.println("Cannot obtain control service");
                  rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Cannot obtain control service")); //$NON-NLS-1$
                  return;
               }
               fProcService = fTracker.getService(IMIProcesses.class);
               if (fProcService == null) {
//                  System.err.println("Cannot obtain process service");
                  rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Cannot obtain process service")); //$NON-NLS-1$
                  return;
               }
               fGdbServerParameters = GdbServerParameters.getInitializedServerParameters(fAttributes);
               if (fGdbServerParameters == null) {
//                  System.err.println("Unable to obtain server parameters");
                  rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Unable to obtain server parameters")); //$NON-NLS-1$
                  return;
               }
               fRunControl = fTracker.getService(IMIRunControl.class);
               if (fRunControl == null) {
//                  System.err.println("Unable to obtain run control ");
                  rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Unable to obtain run control ")); //$NON-NLS-1$
                  return;
               }
               boolean restartUsesStartup = CDebugUtils.getAttribute(fAttributes, 
                     UsbdmSharedConstants.ATTR_RESTART_USES_STARTUP, 
                     UsbdmSharedConstants.DEFAULT_RESTART_USES_STARTUP);

               if (restartUsesStartup) {
//                  System.err.println("Using Startup sequence");
                  fDoSetPc = CDebugUtils.getAttribute(fAttributes, 
                        UsbdmSharedConstants.ATTR_SET_PC_REGISTER, 
                        UsbdmSharedConstants.DEFAULT_SET_PC_REGISTER);
                  fPcValue = CDebugUtils.getAttribute(fAttributes, 
                        UsbdmSharedConstants.ATTR_PC_REGISTER_VALUE,     
                        UsbdmSharedConstants.DEFAULT_PC_REGISTER_VALUE);
                  fDoStopAt= CDebugUtils.getAttribute(fAttributes, 
                        UsbdmSharedConstants.ATTR_DO_STOP_AT_MAIN,     
                        UsbdmSharedConstants.DEFAULT_DO_STOP_AT_MAIN);
                  fStopAtAddress = CDebugUtils.getAttribute(fAttributes, 
                        UsbdmSharedConstants.ATTR_STOP_AT_MAIN_ADDRESS,         
                        UsbdmSharedConstants.DEFAULT_STOP_AT_MAIN_ADDRESS);
                  fDoResume= CDebugUtils.getAttribute(fAttributes, 
                        UsbdmSharedConstants.ATTR_DO_RESUME,      
                        UsbdmSharedConstants.DEFAULT_DO_RESUME);
               }
               else {
//                  System.err.println("Using Restart sequence");
                  fDoSetPc = CDebugUtils.getAttribute(fAttributes, 
                        UsbdmSharedConstants.ATTR_RESTART_SET_PC_REGISTER, 
                        UsbdmSharedConstants.DEFAULT_RESTART_SET_PC_REGISTER);
                  fPcValue = CDebugUtils.getAttribute(fAttributes, 
                        UsbdmSharedConstants.ATTR_RESTART_PC_REGISTER_VALUE,     
                        UsbdmSharedConstants.DEFAULT_RESTART_PC_REGISTER_VALUE);
                  fDoStopAt= CDebugUtils.getAttribute(fAttributes, 
                        UsbdmSharedConstants.ATTR_RESTART_DO_STOP_AT_MAIN,     
                        UsbdmSharedConstants.DEFAULT_RESTART_DO_STOP_AT_MAIN);
                  fStopAtAddress = CDebugUtils.getAttribute(fAttributes, 
                        UsbdmSharedConstants.ATTR_RESTART_STOP_AT_MAIN_ADDRESS,         
                        UsbdmSharedConstants.DEFAULT_RESTART_STOP_AT_MAIN_ADDRESS);
                  fDoResume= CDebugUtils.getAttribute(fAttributes, 
                        UsbdmSharedConstants.ATTR_RESTART_DO_RESUME,      
                        UsbdmSharedConstants.DEFAULT_RESTART_DO_RESUME);
               }
               rm.done();
            }
         },
         new Step() {
            /**
             * Halt the target (if running)
             */
            @Override
            public void execute(RequestMonitor rm) {
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepHaltTarget()");

               if (fExecutionContext == null) {
//                  System.err.println("Cannot obtain executionContext");
                  rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "No executionContext")); //$NON-NLS-1$
                  return;
               }
               // Check if target running
               DataRequestMonitor<Boolean> brm = new DataRequestMonitor<Boolean>(fRunControl.getExecutor(), null);
               fRunControl.canSuspend(fExecutionContext, brm);
               if (brm.getData()) {
                  // Suspend target 
//                  System.err.println("Suspending target");
                  fRunControl.suspend(fExecutionContext, new RequestMonitor(fRunControl.getExecutor(), null) {
                     protected void handleSuccess(){
//                        System.err.println("Halted target OK");
                     }
                     @Override
                     protected void handleError() {
//                        System.err.println("Halting target Failed");
                        fGDBBackend.interruptAndWait(100, this);
                     }
                  });
               }
               rm.done();
            }
         },
         new Step() {
            /**
             * Run device-specific code to reset the board
             */
            @Override
            public void execute(RequestMonitor rm) {
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepResetTarget()");

               // Always reset if Run mode or loading an image
               List<String> commands = new ArrayList<String>();
               fUsbdmGdbInterface.doReset(commands);
               fDoSyncTarget = true;
               queueCommands(commands, rm);
            }
         },
         new Step() {
            /**
             * Flush registers
             */
            @Override
            public void execute(RequestMonitor rm) {
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepFlushRegs()");

               // Always flush regs if Run mode or Loading an image
               List<String> commands = new ArrayList<String>();
               commands.add("flushregs");
               queueCommands(commands, rm);
               fDoSyncTarget = true;
            
               rm.done();
            }
         },
         new Step() {
            /**
             * Set the program counter
             */
            @Override
            public void execute(RequestMonitor rm) {
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepSetProgramCounter()");
               if (fDoSetPc) {
//                  System.err.println("Setting PC");
                  List<String> commands = new ArrayList<String>();
                  fUsbdmGdbInterface.doSetPC(fPcValue, commands);
                  queueCommands(commands, rm);                       
                  fDoSyncTarget = true;
               } else {
                  rm.done();
               }
            }
         },
         new Step() {
            /**
             * Stop at initial breakpoint
             */
            @Override
            public void execute(RequestMonitor rm) {
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepSetInitialBreakpoint()");
               if (fDoStopAt) {
//                  System.err.println("Setting breakpoint");
                  List<String> commands = new ArrayList<String>();
                  fUsbdmGdbInterface.doStopAt(fStopAtAddress, commands);
                  queueCommands(commands, rm);
               } else {
                  rm.done();
               }
            
            }
         },
         new Step() {
            /**
             * Resume execution after load/connect
             */
            @Override
            public void execute(RequestMonitor rm) {
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepResumeTarget()");
               if (fDoResume) {
//                  System.err.println("Doing continue");
                  List<String> commands = new ArrayList<String>();
                  fUsbdmGdbInterface.doContinue(commands);
                  queueCommands(commands, rm);   
                  fDoSyncTarget = false;
               } else {
                  rm.done();
               }
            
            }
         },
         new Step() {
            /**
             * If necessary, steps the target once to synchronize GDB
             * This helps 
             * @param rm
             */
            @Override
            public void execute(RequestMonitor rm) {
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepSyncTarget()");
               if (fDoSyncTarget) {
//                  System.err.println("Doing step");
                  List<String> commands = new ArrayList<String>();
                  fUsbdmGdbInterface.doStep(commands);
                  queueCommands(commands, rm);  
               }
               rm.done();
               return;
            
            }
         },
         new Step() {
            /**
             * Cleanup now that the sequence has been completed.
             */
            @Override
            public void execute(final RequestMonitor rm) {
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepUsbdmCleanup()");
               fTracker.dispose();
               fTracker = null;
               rm.done();
            }
         }
   };

   /**
    * Cleanup now that the sequence has been run.
    */
   public void stepUsbdmCleanup(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepUsbdmCleanup()");
      fTracker.dispose();
      fTracker = null;
      rm.done();
   }

}

