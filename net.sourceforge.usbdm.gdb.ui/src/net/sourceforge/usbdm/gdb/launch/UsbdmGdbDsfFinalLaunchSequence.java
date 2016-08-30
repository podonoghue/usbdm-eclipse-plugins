/*******************************************************************************
 * Copyright (c) 2007 - 2011 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Modified Peter O'Donoghue for USBDM
 *  
 * Based on GDBJtagDSFFinalLaunchSequence developed by:
 *     Ericsson - initial API and implementation this class is based on
 *     QNX Software Systems - Initial implementation for Jtag debugging
 *     Sage Electronic Engineering, LLC - bug 305943
 *              - API generalization to become transport-independent (allow
 *                connections via serial ports and pipes).
 *     John Dallaway - Wrong groupId during initialization (Bug 349736)    
 *     Marc Khouzam (Ericsson) - Updated to extend FinalLaunchSequence instead of copying it (bug 324101)
 *     Andy Jin
 *******************************************************************************/
package net.sourceforge.usbdm.gdb.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.concurrent.CountingRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.ImmediateDataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitorWithProgress;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.debug.service.IMemory.IMemoryDMContext;
import org.eclipse.cdt.dsf.gdb.launching.FinalLaunchSequence;
import org.eclipse.cdt.dsf.gdb.service.IGDBBackend;
import org.eclipse.cdt.dsf.gdb.service.IGDBMemory;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.IMIContainerDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIProcesses;
import org.eclipse.cdt.dsf.mi.service.MIBreakpointsManager;
import org.eclipse.cdt.dsf.mi.service.MIProcesses;
import org.eclipse.cdt.dsf.mi.service.command.commands.CLICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.utils.CommandLineUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.gdb.server.GdbServerInterface;
import net.sourceforge.usbdm.gdb.server.GdbServerParameters;
import net.sourceforge.usbdm.gdb.server.GdbServerParameters.GdbServerType;
import net.sourceforge.usbdm.gdb.ttyConsole.MyConsoleInterface;
import net.sourceforge.usbdm.gdb.ui.Activator;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * The final launch sequence for the hardware debugging using the
 * DSF/GDB debugger framework.
 * <p>
 * This class is based on the implementation of the standard DSF/GDB debugging
 * <code>org.eclipse.cdt.dsf.gdb.launching.FinalLaunchSequence</code>
 * <p>
 * It adds hardware debugging specific steps to initialize a USBDM interface
 * @since 4.12
 * 
 */
public class UsbdmGdbDsfFinalLaunchSequence extends FinalLaunchSequence {

   private final Map<String, Object>   fAttributes;
   private final DsfSession            fSession;
   private final String                fLaunchMode;

   private GdbServerParameters         fGdbServerParameters;
   private IGDBControl                 fCommandControl;
   private IGDBBackend	               fGDBBackend;
   private IMIProcesses                fProcService;
   private UsbdmGdbInterface           fUsbdmGdbInterface;
   private DsfServicesTracker          fTracker;
   private IMIContainerDMContext       fContainerCtx;
   private boolean                     fDoSyncTarget;
   
   public UsbdmGdbDsfFinalLaunchSequence(DsfSession session, Map<String, Object> attributes, RequestMonitorWithProgress rm) {
      super(session, attributes, rm);
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence(...)");
      fSession             = session;
      fAttributes          = attributes;
      fDoSyncTarget        = false;
      ILaunch launch       = (ILaunch)fSession.getModelAdapter(ILaunch.class);
      fLaunchMode          = launch.getLaunchMode();
   }

   protected IMIContainerDMContext getContainerContext() {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.getContainerContext() context = "+ fContainerCtx);
//      String groupId = fContainerCtx.getGroupId();
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.getContainerContext() groupId = "+ groupId);
      return fContainerCtx;
   }

   protected void setContainerContext(IMIContainerDMContext ctx) {
      fContainerCtx = ctx;
   }

   protected static final String GROUP_USBDM = "GROUP_USBDM";

   @Override
   protected String[] getExecutionOrder(String group) {
      if (GROUP_TOP_LEVEL.equals(group)) {
         // Initialize the list with the base class' steps
         // We need to create a list that we can modify, which is why we create our own ArrayList.
         List<String> orderList = new ArrayList<String>(Arrays.asList(super.getExecutionOrder(GROUP_TOP_LEVEL)));
//         System.err.println("============ GROUP_TOP_LEVEL original =========== \n");
//         for (String s : orderList) {
//            System.err.println(s);
//         }
//         System.err.println("============ GROUP_TOP_LEVEL original =========== \n");
         // First, remove all steps of the base class that we don't want to use.
         orderList.removeAll(Arrays.asList(new String[] { 
               "stepRemoteConnection",    //$NON-NLS-1$
               "stepNewProcess",          //$NON-NLS-1$
               "stepAttachToProcess",     //$NON-NLS-1$
         }));
         // Now insert our steps before the data model initialized event is sent
         orderList.add(orderList.indexOf("stepDataModelInitializationComplete"), GROUP_USBDM);
//         System.err.println("============ GROUP_TOP_LEVEL modified =========== \n");
//         for (String s : orderList) {
//            System.err.println(s);
//         }
//         System.err.println("============ GROUP_TOP_LEVEL modified =========== \n");
         return orderList.toArray(new String[orderList.size()]);
      }

      // Finally, deal with our groups and their steps.
      if (GROUP_USBDM.equals(group)) {
         String[] steps = null;
         if (fLaunchMode.equals(ILaunchManager.RUN_MODE)) {
            steps = new String[] {
                  "stepInitUsbdmGdbDsfFinalLaunchSequence", //$NON-NLS-1$
                  "stepLaunchUsbdmGdbServer",               //$NON-NLS-1$
                  "stepConnectToTarget",                    //$NON-NLS-1$
                  "stepResetTarget",                        //$NON-NLS-1$
                  "stepUserInitCommands",                   //$NON-NLS-1$
                  "stepLoadImage",                          //$NON-NLS-1$
                  "stepSetArguments",                       //$NON-NLS-1$
                  "stepSetEnvironmentVariables",            //$NON-NLS-1$
                  "stepRunTarget",                          //$NON-NLS-1$
                  "stepRunUserCommands",                    //$NON-NLS-1$
                  "stepDetachTarget",                       //$NON-NLS-1$
                  "stepUsbdmCleanup",                       //$NON-NLS-1$
            };
         }
         else { // Assume DEBUG_MODE
            steps = new String[] {
                  "stepInitUsbdmGdbDsfFinalLaunchSequence", //$NON-NLS-1$
                  "stepOpenUsbdmTtyConsole",                //$NON-NLS-1$
                  "stepLaunchUsbdmGdbServer",               //$NON-NLS-1$
                  // -- x
                  "stepLoadSymbols",                        //$NON-NLS-1$
                  "stepConnectToTarget",                    //$NON-NLS-1$
                  "stepResetTarget",                        //$NON-NLS-1$
                  "stepHaltTarget",                         //$NON-NLS-1$
                  "stepUserInitCommands",                   //$NON-NLS-1$
                  "stepLoadImage",                          //$NON-NLS-1$
                  "stepUpdateContainer",                    //$NON-NLS-1$
                  "stepInitializeMemory",                   //$NON-NLS-1$
                  "stepSetArguments",                       //$NON-NLS-1$
                  "stepSetEnvironmentVariables",            //$NON-NLS-1$
                  "stepStartTrackingBreakpoints",           //$NON-NLS-1$

                  "stepSetProgramCounter",                  //$NON-NLS-1$
                  "stepSetInitialBreakpoint",               //$NON-NLS-1$
                  "stepResumeTarget",                       //$NON-NLS-1$
                  "stepRunUserCommands",                    //$NON-NLS-1$
                  "stepUsbdmCleanup",                       //$NON-NLS-1$
            };
         }
         return steps;
      }
      // For any subgroups of the base class
      return super.getExecutionOrder(group);
   }

   /** utility method; cuts down on clutter */
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
    * Initialize the members of the class.
    * This step is mandatory for the rest of the sequence to complete.
    */
   @Execute
   public void stepInitUsbdmGdbDsfFinalLaunchSequence(RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepInitUsbdmGdbDsfFinalLaunchSequence()");
      
      fTracker    = new DsfServicesTracker(Activator.getBundleContext(), fSession.getId());
      fGDBBackend = fTracker.getService(IGDBBackend.class);
      if (fGDBBackend == null) {
         rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Cannot obtain GDBBackend service")); //$NON-NLS-1$
         return;
      }
      fUsbdmGdbInterface = new UsbdmGdbInterface();
      fCommandControl = fTracker.getService(IGDBControl.class);
      if (fCommandControl == null) {
         rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Cannot obtain control service")); //$NON-NLS-1$
         return;
      }
      fProcService = fTracker.getService(IMIProcesses.class);
      if (fProcService == null) {
         rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Cannot obtain process service")); //$NON-NLS-1$
         return;
      }
      fGdbServerParameters = GdbServerParameters.getInitializedServerParameters(fAttributes);
      if (fGdbServerParameters == null) {
         rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Unable to obtain server parameters")); //$NON-NLS-1$
         return;
      }
      // When we are starting to debug a new process, the container is the default process used by GDB.
      // We don't have a PID yet, so we can simply create the container with the MIProcesses.UNIQUE_GROUP_ID
      setContainerContext(fProcService.createContainerContextFromGroupId(fCommandControl.getContext(), MIProcesses.UNIQUE_GROUP_ID));
      rm.done();
   }

   /** 
    * Roll-back method for {@link #stepInitUsbdmGdbDsfFinalLaunchSequence()}
    */
   @RollBack("stepInitUsbdmGdbDsfFinalLaunchSequence")
   public void rollBackInitializeFinalLaunchSequence(RequestMonitor rm) {
      if (fTracker != null) {
         fTracker.dispose();
      }
      fTracker = null;
      rm.done();
   }

   /**
    * Open TTY Console server (if needed)
    */
   @Execute
   public void stepOpenUsbdmTtyConsole(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepOpenUsbdmTtyConsole()");

      try {
         if (fGdbServerParameters.isUseSemihosting()) {
            fGdbServerParameters.getGdbTtyPortNumber();
            int ttyPortNum = fGdbServerParameters.getGdbTtyPortNumber();
//            System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepOpenUsbdmTtyConsole() - Starting server @" + ttyPortNum);
            MyConsoleInterface.startServer(ttyPortNum);
         }
      } catch (Exception e) {
         e.printStackTrace();
         rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Failed to open TTY", e)); //$NON-NLS-1$
         return;
      }
      rm.done();
   }

   /**
    * Create Local USBDM GDB Server (if needed)
    * 
    */
   @Execute
   public void stepLaunchUsbdmGdbServer(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepLaunchUsbdmGdbServer()");

      // Launch GDB server if using socket based server
      if (fGdbServerParameters.getServerType() == GdbServerType.SERVER_SOCKET) {
         GdbServerInterface gdbServerInterface = new GdbServerInterface(fGdbServerParameters);  
         if (gdbServerInterface != null) {
            try {
               gdbServerInterface.startServer();
            } catch (UsbdmException e) {
               e.printStackTrace();
               rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), e.getMessage(), e)); //$NON-NLS-1$
               return;
            }
         }
      }
      rm.done();
   }

   /*
    * Execute symbol loading
    */
   @Execute
   public void stepLoadSymbols(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepLoadSymbols()");

      try {
         String symbolsFileName = null;

         if (CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_PROGRAM_TARGET, UsbdmSharedConstants.DEFAULT_PROGRAM_TARGET)) {
            // Programming target - using binary or external file
            if (CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_USE_EXTERNAL_FILE, UsbdmSharedConstants.DEFAULT_USE_EXTERNAL_FILE)) {
               // External file
               symbolsFileName = CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_EXTERNAL_FILE_NAME, UsbdmSharedConstants.DEFAULT_EXTERNAL_FILE_NAME);
            }
            else {
               // Associated binary file
               IPath programFile = fGDBBackend.getProgramPath();
               if ((programFile != null) && !programFile.isEmpty()) {
                  symbolsFileName = programFile.toOSString();
               }
            }
         }
         else {
            // No programming target - using binary or external symbol file
            if (CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_USE_EXTERNAL_SYMBOL_FILE, UsbdmSharedConstants.DEFAULT_USE_EXTERNAL_SYMBOL_FILE)) {
               // External file
               symbolsFileName = CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_EXTERNAL_SYMBOL_FILE_NAME, UsbdmSharedConstants.DEFAULT_EXTERNAL_SYMBOL_FILE_NAME);
            }
            else {
               // Associated binary file
               IPath programFile = fGDBBackend.getProgramPath();
               if ((programFile != null) && !programFile.isEmpty()) {
                  symbolsFileName = programFile.toOSString();
               }
            }
         }
         if (symbolsFileName == null) {
            rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Symbol file not found")); //$NON-NLS-1$
            return;
         }
         // Escape windows path separator characters TWICE, once for Java and once for GDB.						
         symbolsFileName = symbolsFileName.replace("\\", "\\\\"); //$NON-NLS-1$ //$NON-NLS-2$

         String symbolsOffset = "";
         if (CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_USE_SYMBOLS_OFFSET, UsbdmSharedConstants.DEFAULT_USE_SYMBOLS_OFFSET)) {
            symbolsOffset = CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_SYMBOLS_OFFSET_VALUE, UsbdmSharedConstants.DEFAULT_SYMBOLS_OFFSET_VALUE);
            if (symbolsOffset.length() > 0) {
               symbolsOffset = "0x" + symbolsOffset;              
            }
         }
         List<String> commands = new ArrayList<String>();
         fUsbdmGdbInterface.doLoadSymbol(symbolsFileName, symbolsOffset, commands);
         queueCommands(commands, rm);									
      } finally {
         rm.done();
      }
   }

   /*
    * Hook up to remote target
    */
   @Execute
   public void stepConnectToTarget(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepConnectToTarget()");

      if (fGdbServerParameters == null) {
         rm.done(new Status(IStatus.ERROR, Activator.getPluginId(),"Unable to obtain server parameters")); //$NON-NLS-1$
         return;
      }
      // Get command line with options from server parameters
      String serverCommandLine = fGdbServerParameters.getCommandLine();
      if (serverCommandLine == null) {
         rm.done(new Status(IStatus.ERROR, Activator.getPluginId(),"Unable to obtain server command line")); //$NON-NLS-1$
         return;
      }
      List<String> commands = new ArrayList<String>();
      fUsbdmGdbInterface.doRemote(serverCommandLine, commands);
      queueCommands(commands, rm);
   }

   /*
    * Run device-specific code to reset the board
    */
   @Execute
   public void stepResetTarget(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepResetTarget()");
      
      // Always reset if Run mode or Loading an image
      if (fLaunchMode.equals(ILaunchManager.RUN_MODE) || 
          CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_PROGRAM_TARGET, UsbdmSharedConstants.DEFAULT_PROGRAM_TARGET) ||
          CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_DO_RESET,       UsbdmSharedConstants.DEFAULT_DO_RESET)) {
         
         List<String> commands = new ArrayList<String>();
         fUsbdmGdbInterface.doReset(commands);
         queueCommands(commands, rm);
         fDoSyncTarget = true;
      } else {
         rm.done();
      }
   }

   /*
    * Run device-specific code to halt the target
    */
   @Execute
   public void stepHaltTarget(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepHaltTarget()");
      
      // No need to halt if Loading an image as already reset
      if (!CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_PROGRAM_TARGET, UsbdmSharedConstants.DEFAULT_PROGRAM_TARGET) &&
          !CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_DO_RESET,       UsbdmSharedConstants.DEFAULT_DO_RESET) &&
           CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_DO_HALT,        UsbdmSharedConstants.DEFAULT_DO_HALT)) {
         
         List<String> commands = new ArrayList<String>();
         fUsbdmGdbInterface.doHalt(commands);
         queueCommands(commands, rm);								
         fDoSyncTarget = true;
      } else {
         rm.done();
      }
   }

   /*
    * Execute any user defined initialization commands
    */
   @Execute
   public void stepUserInitCommands(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepUserInitCommands()");
      
      try {
         String userCmd = CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_INIT_COMMANDS, UsbdmSharedConstants.DEFAULT_INIT_COMMANDS);
         userCmd = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(userCmd);
         queueCommands(Arrays.asList(userCmd.split("\\r?\\n")), rm); //$NON-NLS-1$
      } catch (CoreException e) {
         rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Cannot run user defined init commands", e)); //$NON-NLS-1$
      }
   }

   /*
    * Execute image loading
    */
   @Execute
   public void stepLoadImage(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepLoadImage()");

      if (fLaunchMode.equals(ILaunchManager.DEBUG_MODE) &&
            !CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_PROGRAM_TARGET, UsbdmSharedConstants.DEFAULT_PROGRAM_TARGET)) {
         rm.done();
         return;
      }
      try {
         String imageFileName = null;
         
         // Programming target - using binary or external file
         if (CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_USE_EXTERNAL_FILE, UsbdmSharedConstants.DEFAULT_USE_EXTERNAL_FILE)) {
            // External file
            imageFileName = CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_EXTERNAL_FILE_NAME, UsbdmSharedConstants.DEFAULT_EXTERNAL_FILE_NAME);
         }
         else {
            // Associated binary file
            IPath programFile = fGDBBackend.getProgramPath();
            if ((programFile != null) && !programFile.isEmpty()) {
               imageFileName = programFile.toOSString();
            }
         }

         if (imageFileName == null) {
            rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Binary file not found")); //$NON-NLS-1$
            return;
         }

         // Escape windows path separator characters TWICE, once for Java and once for GDB.						
         imageFileName = imageFileName.replace("\\", "\\\\"); //$NON-NLS-1$ //$NON-NLS-2$

         String imageOffset = "";
         if (CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_USE_BINARY_OFFSET, UsbdmSharedConstants.DEFAULT_USE_BINARY_OFFSET)) {
            imageOffset = CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_BINARY_OFFSET_VALUE, UsbdmSharedConstants.DEFAULT_BINARY_OFFSET_VALUE);
            if (imageOffset.length() > 0) {
               imageOffset = (imageFileName.endsWith(".elf")) ? "" : ("0x" + imageOffset); //$NON-NLS-2$ 
            }
         }
//            System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepLoadImage() IMAGE_FILE, imageFileName = " + imageFileName);
         List<String> commands = new ArrayList<String>();
         fUsbdmGdbInterface.doLoadImage(imageFileName, imageOffset, commands);
         queueCommands(commands, rm);									
      } finally {
         rm.done();
      }
   }

   /**
    * Now that we are connected to the target, we should update
    * our container to properly fill in its pid.
    */
   @Execute
   public void stepUpdateContainer(RequestMonitor rm) {
      IMIContainerDMContext context = getContainerContext();
//    String groupId = "i1";
      String groupId = context.getGroupId();
      IMIContainerDMContext newContext = fProcService.createContainerContextFromGroupId(fCommandControl.getContext(), groupId);
      setContainerContext(newContext);
      rm.done();
   }

   /**
    * Specify the arguments to the program that will be run.
    */
   @Execute
   public void stepSetArguments(RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepSetArguments()");
      try {
         String args = CDebugUtils.getAttribute(fAttributes, ICDTLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, ""); //$NON-NLS-1$
         if (args.length() != 0) {
            args = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(args);
            String[] argArray = CommandLineUtil.argumentsToArray(args);
            fCommandControl.queueCommand(
                  fCommandControl.getCommandFactory().createMIGDBSetArgs(getContainerContext(), argArray), 
                  new ImmediateDataRequestMonitor<MIInfo>(rm));
         } else {
            rm.done();
         }
      } catch (CoreException e) {
         rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Cannot get inferior arguments", e)); //$NON-NLS-1$
      }    		
   }

   /**
    * Specify environment variables if needed
    */
   @Execute
   public void stepSetEnvironmentVariables(RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepSetEnvironmentVariables()");
      boolean    clear      = false;
      Properties properties = new Properties();
      try {
         // Here we need to pass the proper container context
         clear = fGDBBackend.getClearEnvironment();
         properties = fGDBBackend.getEnvironmentVariables();
      } catch (CoreException e) {
         rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Cannot get environment information", e)); //$NON-NLS-1$
         return;
      }
      if (clear == true || properties.size() > 0) {
         fCommandControl.setEnvironment(properties, clear, rm);
      } else {
         rm.done();
      }
   }

   /* 
    * Start tracking the breakpoints once we know we are connected to the target (necessary for remote debugging) 
    */
   @Execute
   public void stepStartTrackingBreakpoints(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepStartTrackingBreakpoints()");
      MIBreakpointsManager bpmService = fTracker.getService(MIBreakpointsManager.class);
      bpmService.startTrackingBpForProcess(getContainerContext(), rm);
   }

   /*
    * Set the program counter
    */
   @Execute
   public void stepSetProgramCounter(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepSetProgramCounter()");
      if (CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_SET_PC_REGISTER, UsbdmSharedConstants.DEFAULT_SET_PC_REGISTER)) {
         String pcRegister = CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_PC_REGISTER_VALUE, UsbdmSharedConstants.DEFAULT_PC_REGISTER_VALUE); 
         List<String> commands = new ArrayList<String>();
         fUsbdmGdbInterface.doSetPC(pcRegister, commands);
         queueCommands(commands, rm);								
         fDoSyncTarget = true;
      } else {
         rm.done();
      }
   }

   /*
    * Stop at initial breakpoint
    */
   @Execute
   public void stepSetInitialBreakpoint(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepSetInitialBreakpoint()");
      if (CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_DO_STOP_AT_MAIN, UsbdmSharedConstants.DEFAULT_DO_STOP_AT_MAIN)) {
         String stopAt = CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_STOP_AT_MAIN_ADDRESS, UsbdmSharedConstants.DEFAULT_STOP_AT_MAIN_ADDRESS); 
         List<String> commands = new ArrayList<String>();
         fUsbdmGdbInterface.doStopAt(stopAt, commands);
         queueCommands(commands, rm);
      } else {
         rm.done();
      }
   }

   /*
    * Resume execution after load/connect
    */
   @Execute
   public void stepResumeTarget(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepResumeTarget()");
      // Resume target if
      // - Run mode or
      // - Loading image AND selected Resume OR
      // - Connecting to target AND Resetting and not Halting
      if (fLaunchMode.equals(ILaunchManager.RUN_MODE) ||
            (CDebugUtils.getAttribute(fAttributes,  UsbdmSharedConstants.ATTR_PROGRAM_TARGET, UsbdmSharedConstants.DEFAULT_PROGRAM_TARGET) &&
             CDebugUtils.getAttribute(fAttributes,  UsbdmSharedConstants.ATTR_DO_RESUME,      UsbdmSharedConstants.DEFAULT_DO_RESUME)) ||
            (!CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_PROGRAM_TARGET, UsbdmSharedConstants.DEFAULT_PROGRAM_TARGET) &&
              CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_DO_RESET,       UsbdmSharedConstants.DEFAULT_DO_RESET) &&
             !CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_DO_HALT,        UsbdmSharedConstants.DEFAULT_DO_HALT))) {
         List<String> commands = new ArrayList<String>();
         fUsbdmGdbInterface.doContinue(commands);
         queueCommands(commands, rm);   
         fDoSyncTarget = false;
      } else {
         rm.done();
      }
   }

   /*
    * Resume execution after load/connect
    */
   @Execute
   public void stepRunTarget(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepRunTarget()");
      // Resume target if
      // - Run mode or
      // - Loading image AND selected Resume OR
      // - Connecting to target AND Resetting and not Halting
      if (fLaunchMode.equals(ILaunchManager.RUN_MODE) ||
            (CDebugUtils.getAttribute(fAttributes,  UsbdmSharedConstants.ATTR_PROGRAM_TARGET, UsbdmSharedConstants.DEFAULT_PROGRAM_TARGET) &&
             CDebugUtils.getAttribute(fAttributes,  UsbdmSharedConstants.ATTR_DO_RESUME,      UsbdmSharedConstants.DEFAULT_DO_RESUME)) ||
            (!CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_PROGRAM_TARGET, UsbdmSharedConstants.DEFAULT_PROGRAM_TARGET) &&
              CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_DO_RESET,       UsbdmSharedConstants.DEFAULT_DO_RESET) &&
             !CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_DO_HALT,        UsbdmSharedConstants.DEFAULT_DO_HALT))) {
         List<String> commands = new ArrayList<String>();
         fUsbdmGdbInterface.doRun(commands);
         queueCommands(commands, rm);   
         fDoSyncTarget = false;
      } else {
         rm.done();
      }
   }

   /*
    * Run any user defined commands to start debugging
    */
   @Execute
   public void stepRunUserCommands(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepRunUserCommands()");
      try {
         String userCmd = CDebugUtils.getAttribute(fAttributes, UsbdmSharedConstants.ATTR_RUN_COMMANDS, UsbdmSharedConstants.DEFAULT_RUN_COMMANDS); 
         userCmd = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(userCmd);
         queueCommands(Arrays.asList(userCmd.split("\\r?\\n")), rm); //$NON-NLS-1$
      } catch (CoreException e) {
         rm.done(new Status(IStatus.ERROR, Activator.getPluginId(), "Cannot run user defined run commands", e)); //$NON-NLS-1$
      }
   }

   /**
    * If necessary, steps the target once to synchronize GDB
    * This helps 
    * @param rm
    */
   @Execute
   public void stepDetachTarget(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepDetachTarget()");
      List<String> commands = new ArrayList<String>();
      fUsbdmGdbInterface.doDetach(commands);
      queueCommands(commands, rm);  
   }

   /**
    * If necessary, steps the target once to synchronize GDB
    * This helps 
    * @param rm
    */
   @Execute
   public void stepSyncTarget(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepSyncTarget()");
      if (!fDoSyncTarget) {
         rm.done();
         return;
      }
      List<String> commands = new ArrayList<String>();
      fUsbdmGdbInterface.doStep(commands);
      queueCommands(commands, rm);  
   }

   /**
    * Cleanup now that the sequence has been run.
    */
   @Execute
   public void stepUsbdmCleanup(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepUsbdmCleanup()");
      fTracker.dispose();
      fTracker = null;
      rm.done();
   }
   
   /**
    * Initialize the memory service with the data for given process.
    * @since 8.3
    */
   @Execute
   public void stepInitializeMemory(final RequestMonitor rm) {
//    System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepInitializeMemory()");
      IGDBMemory       memory     = fTracker.getService(IGDBMemory.class);
      IMemoryDMContext memContext = DMContexts.getAncestorOfType(getContainerContext(), IMemoryDMContext.class);
      if (memory == null || memContext == null) {
         rm.done();
         return;
      }
      memory.initializeMemoryData(memContext, rm);
   }
   
   @Override
   public DsfSession getSession() {
      return fSession;
   }
}

