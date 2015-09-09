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


package net.sourceforge.usbdm.gdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.usbdm.gdb.GdbServerParameters.GdbServerType;
import net.sourceforge.usbdm.gdb.ttyConsole.MyConsoleInterface;
import net.sourceforge.usbdm.jni.UsbdmException;

import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.debug.core.model.IConnectHandler;
import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.dsf.concurrent.CountingRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DsfExecutor;
import org.eclipse.cdt.dsf.concurrent.ImmediateDataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitorWithProgress;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IMemory.IMemoryDMContext;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.actions.IConnect;
import org.eclipse.cdt.dsf.gdb.launching.FinalLaunchSequence;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.gdb.service.IGDBBackend;
import org.eclipse.cdt.dsf.gdb.service.IGDBMemory;
import org.eclipse.cdt.dsf.gdb.service.SessionType;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.IMIContainerDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIProcesses;
import org.eclipse.cdt.dsf.mi.service.MIBreakpointsManager;
import org.eclipse.cdt.dsf.mi.service.MIProcesses;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
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

   // The launchConfiguration attributes
   private Map<String, Object>   fAttributes;
   private GdbServerParameters   fGdbServerParameters;

   private DsfSession            fSession;

   private IGDBControl           fCommandControl;
   private IGDBBackend	         fGDBBackend;
   private IMIProcesses          fProcService;
   private UsbdmGdbInterface     fUsbdmGdbInterface;

   private DsfServicesTracker    fTracker;
   private IMIContainerDMContext fContainerCtx;
   private CommandFactory        fCommandFactory;
   private String                fLaunchMode;
   private int                   fTtyPortNum;

   private boolean               fDoSyncTarget;
   
   public UsbdmGdbDsfFinalLaunchSequence(DsfSession session, Map<String, Object> attributes, RequestMonitorWithProgress rm, GdbServerParameters gdbServerParameters) {
      super(session, attributes, rm);
      fSession             = session;
      fAttributes          = attributes;
      fGdbServerParameters = gdbServerParameters;		
      fDoSyncTarget        = false;
      ILaunch launch       = (ILaunch)fSession.getModelAdapter(ILaunch.class);
      fTtyPortNum          = -1;
      boolean ttyOpen = getBooleanLaunchSetting(launch, net.sourceforge.usbdm.gdb.GdbServerParameters.USE_SEMI_HOSTING_KEY);
      if (ttyOpen) {
         fTtyPortNum = getIntegerSetting(launch, net.sourceforge.usbdm.gdb.GdbServerParameters.TTY_PORT_KEY);
      }
      fLaunchMode = launch.getLaunchMode();
   }

   int getIntegerSetting(ILaunch launch, String key) {
      int value = 0;
      try {
         value = launch.getLaunchConfiguration().getAttribute(
               net.sourceforge.usbdm.gdb.ui.UsbdmDebuggerPanel.USBDM_LAUNCH_ATTRIBUTE_KEY+key, 0);
      } catch (CoreException e) {
         e.printStackTrace();
      }
//      System.err.println("getLaunchSetting() "+ key + " => " + value);
      return value;
   }
   
   String getStringLaunchSetting(ILaunch launch, String key) {
      String value = null;
      try {
         value = launch.getLaunchConfiguration().getAttribute(
               net.sourceforge.usbdm.gdb.ui.UsbdmDebuggerPanel.USBDM_LAUNCH_ATTRIBUTE_KEY+key, "");
      } catch (CoreException e) {
         e.printStackTrace();
      }
//      System.err.println("getLaunchSetting() "+ key + " => " + value);
      return value;
   }
   
   boolean getBooleanLaunchSetting(ILaunch launch, String key) {
      boolean value = false;
      try {
         value = launch.getLaunchConfiguration().getAttribute(
               net.sourceforge.usbdm.gdb.ui.UsbdmDebuggerPanel.USBDM_LAUNCH_ATTRIBUTE_KEY+key, false);
      } catch (CoreException e) {
         e.printStackTrace();
      }
//      System.err.println("getLaunchSetting() "+ key + " => " + value);
      return value;
   }
   
   public UsbdmGdbDsfFinalLaunchSequence(DsfExecutor executor, GdbLaunch launch, SessionType sessionType, boolean attach, RequestMonitorWithProgress rm, GdbServerParameters gdbServerParameters) {
      this(launch.getSession(), getAttributes(launch), rm, gdbServerParameters);
   }

   private static Map<String, Object> getAttributes(GdbLaunch launch) {
      try {
         return launch.getLaunchConfiguration().getAttributes();
      } catch (CoreException e) {
      }
      return new HashMap<String, Object>();
   }

   protected IMIContainerDMContext getContainerContext() {
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
                  "stepCreateUsbdmInterface",               //$NON-NLS-1$
                  "stepConnectToTarget",                    //$NON-NLS-1$
                  "stepResetTarget",                        //$NON-NLS-1$
                  "stepUserInitCommands",                   //$NON-NLS-1$
                  "stepLoadImage",                          //$NON-NLS-1$
//                "stepUpdateContainer",                    //$NON-NLS-1$
                  "stepSetArguments",                       //$NON-NLS-1$
                  "stepSetEnvironmentVariables",            //$NON-NLS-1$
                  "stepRunTarget",                          //$NON-NLS-1$
                  "stepRunUserCommands",                    //$NON-NLS-1$
//                  "stepUsbdmConnection",                    //$NON-NLS-1$
//                  "stepUsbdmAttachToProcess",               //$NON-NLS-1$
                  "stepDetachTarget",                       //$NON-NLS-1$
                  "stepUsbdmCleanup",                       //$NON-NLS-1$
            };
         }
         else { // Assume DEBUG_MODE
            steps = new String[] {
                  "stepInitUsbdmGdbDsfFinalLaunchSequence", //$NON-NLS-1$
                  "stepOpenUsbdmTtyConsole",                //$NON-NLS-1$
                  "stepLaunchUsbdmGdbServer",               //$NON-NLS-1$
                  "stepCreateUsbdmInterface",               //$NON-NLS-1$ 
                  // -- x
                  "stepLoadSymbols",                        //$NON-NLS-1$
                  "stepConnectToTarget",                    //$NON-NLS-1$
                  "stepResetTarget",                        //$NON-NLS-1$
                  "stepHaltTarget",                         //$NON-NLS-1$
                  "stepUserInitCommands",                   //$NON-NLS-1$
                  "stepLoadImage",                          //$NON-NLS-1$
                  "stepUpdateContainer",                    //$NON-NLS-1$
                  "stepInitializeMemory",                   //$NON-NLS-1$ //TODO check
                  "stepSetArguments",                       //$NON-NLS-1$
                  "stepSetEnvironmentVariables",            //$NON-NLS-1$
                  "stepStartTrackingBreakpoints",           //$NON-NLS-1$

                  "stepSetProgramCounter",                  //$NON-NLS-1$
                  "stepSetInitialBreakpoint",               //$NON-NLS-1$
                  "stepResumeTarget",                       //$NON-NLS-1$
                  "stepRunUserCommands",                    //$NON-NLS-1$
//                  "stepUsbdmConnection",                    //$NON-NLS-1$
//                  "stepUsbdmAttachToProcess",               //$NON-NLS-1$
//                  "stepSyncTarget",                         //$NON-NLS-1$
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
      fTracker = new DsfServicesTracker(UsbdmGdbServer.getBundleContext(), fSession.getId());
      fGDBBackend = fTracker.getService(IGDBBackend.class);
      if (fGDBBackend == null) {
         rm.done(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Cannot obtain GDBBackend service", null)); //$NON-NLS-1$
         return;
      }
      fCommandControl = fTracker.getService(IGDBControl.class);
      if (fCommandControl == null) {
         rm.done(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Cannot obtain control service", null)); //$NON-NLS-1$
         return;
      }
      fProcService = fTracker.getService(IMIProcesses.class);
      if (fProcService == null) {
         rm.done(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Cannot obtain process service", null)); //$NON-NLS-1$
         return;
      }
      fCommandFactory = fCommandControl.getCommandFactory();
      if (fCommandFactory == null) {
         rm.done(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Cannot obtain command factory", null)); //$NON-NLS-1$
         return;
      }
      // When we are starting to debug a new process, the container is the default process used by GDB.
      // We don't have a pid yet, so we can simply create the container with the UNIQUE_GROUP_ID
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
    * Create Local USBDM GDB Server (if needed)
    * 
    */
   @Execute
   public void stepOpenUsbdmTtyConsole(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepOpenUsbdmTtyConsole()");
      try {
         if (fTtyPortNum>0) {
            MyConsoleInterface.startServer(fTtyPortNum);
         }
      } catch (Exception e) {
         e.printStackTrace();
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

      if (fGdbServerParameters == null) {
         rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1,"Unable to obtain server parameters", null)); //$NON-NLS-1$
         rm.done();
         return;
      }
      // Launch GDB server if using socket based server
      if (fGdbServerParameters.getServerType() == GdbServerType.SERVER_SOCKET) {
         GdbServerInterface gdbServerInterface = new GdbServerInterface(fGdbServerParameters);  
         if (gdbServerInterface != null) {
            try {
               gdbServerInterface.startServer();
            } catch (UsbdmException e1) {
               e1.printStackTrace();
               rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, e1.getMessage(), null)); //$NON-NLS-1$
               rm.done();
               return;
            }
         }
      }
      rm.done();
   }

   /**
    * Create the UsbdmGdbInterface instance
    * 
    */
   @Execute
   public void stepCreateUsbdmInterface(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepCreateUsbdmInterface()");
      fUsbdmGdbInterface = new UsbdmGdbInterface();
      rm.done();
   }

   /*
    * Execute symbol loading
    */
   @Execute
   public void stepLoadSymbols(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepLoadSymbols()");

      if (!CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_LOAD_SYMBOLS, IGDBJtagConstants.DEFAULT_LOAD_SYMBOLS)) {
         rm.done();
         return;
      }
      try {
         String symbolsFileName = null;

         if (CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS, IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_SYMBOLS)) {
            IPath programFile = fGDBBackend.getProgramPath();
            if (programFile != null) {
               symbolsFileName = programFile.toOSString();
            }
         }
         else {
            symbolsFileName = CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_SYMBOLS_FILE_NAME, IGDBJtagConstants.DEFAULT_SYMBOLS_FILE_NAME);
            if (symbolsFileName.length() > 0) {
               symbolsFileName = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(symbolsFileName);
            } else {
               symbolsFileName = null;
            }
         }
         if (symbolsFileName == null) {
            rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Symbol file not found", null)); //$NON-NLS-1$
            rm.done();
            return;
         }
         // Escape windows path separator characters TWICE, once for Java and once for GDB.						
         symbolsFileName = symbolsFileName.replace("\\", "\\\\"); //$NON-NLS-1$ //$NON-NLS-2$

         String symbolsOffset = CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_SYMBOLS_OFFSET, IGDBJtagConstants.DEFAULT_SYMBOLS_OFFSET);
         if (symbolsOffset.length() > 0) {
            symbolsOffset = "0x" + symbolsOffset;					
         }
         List<String> commands = new ArrayList<String>();
         fUsbdmGdbInterface.doLoadSymbol(symbolsFileName, symbolsOffset, commands);
         queueCommands(commands, rm);									
      } catch (CoreException e) {
         rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Cannot load symbols file", e)); //$NON-NLS-1$
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
         rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1,"Unable to obtain server parameters", null)); //$NON-NLS-1$
         rm.done();
         return;
      }
      // Get command line with options from server parameters
      ArrayList<String> serverCommandLine = fGdbServerParameters.getCommandLine();
      if (serverCommandLine == null) {
         rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1,"Unable to obtain server command line", null)); //$NON-NLS-1$
         rm.done();
         return;
      }
      StringBuffer commandLine = new StringBuffer();
      for (String commandArg : serverCommandLine) {
         commandLine.append(commandArg);
         commandLine.append(" ");
      }
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepConnectToTarget() command = \'" + commandLine + "\'");
      List<String> commands = new ArrayList<String>();
      fUsbdmGdbInterface.doRemote(commandLine.toString(), commands);
      queueCommands(commands, rm);
      rm.done();
   }

   /*
    * Run device-specific code to reset the board
    */
   @Execute
   public void stepResetTarget(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepResetTarget()");
      // Always reset if Run mode or Loading an image
      if (fLaunchMode.equals(ILaunchManager.RUN_MODE) || 
          CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_LOAD_IMAGE, IGDBJtagConstants.DEFAULT_LOAD_IMAGE) ||
          CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_DO_RESET,   IGDBJtagConstants.DEFAULT_DO_RESET)) {
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
      if (!CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_LOAD_IMAGE, IGDBJtagConstants.DEFAULT_LOAD_IMAGE) &&
          !CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_DO_RESET,   IGDBJtagConstants.DEFAULT_DO_RESET) &&
           CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_DO_HALT,    IGDBJtagConstants.DEFAULT_DO_HALT)) {
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
         String userCmd = CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_INIT_COMMANDS, IGDBJtagConstants.DEFAULT_INIT_COMMANDS);
         userCmd = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(userCmd);
         queueCommands(Arrays.asList(userCmd.split("\\r?\\n")), rm); //$NON-NLS-1$
      } catch (CoreException e) {
         rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Cannot run user defined init commands", e)); //$NON-NLS-1$
         rm.done();
      }
   }

   /*
    * Execute image loading
    */
   @Execute
   public void stepLoadImage(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepLoadImage()");

      if (fLaunchMode.equals(ILaunchManager.DEBUG_MODE) &&
            !CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_LOAD_IMAGE, IGDBJtagConstants.DEFAULT_LOAD_IMAGE)) {
         rm.done();
         return;
      }
      try {
         String imageFileName = null;
         if (CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE, IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_IMAGE)) {
            IPath programFile = fGDBBackend.getProgramPath();
            if (programFile != null) {
               imageFileName = programFile.toOSString();
            }
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepLoadImage() PROJ_BINARY, imageFileName = " + imageFileName);
         }
         else {
            imageFileName = CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_IMAGE_FILE_NAME, IGDBJtagConstants.DEFAULT_IMAGE_FILE_NAME); 
            if (imageFileName.length() > 0) {
               imageFileName = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(imageFileName);
            } else {
               imageFileName = null;
            }
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepLoadImage() IMAGE_FILE, imageFileName = " + imageFileName);
         }
         if (imageFileName == null) {
//               System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepLoadImage() imageFileName = null");
            rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Error - No image file found", null)); //$NON-NLS-1$
            rm.done();
            return;
         }
         // Escape windows path separator characters TWICE, once for Java and once for GDB.						
         imageFileName = imageFileName.replace("\\", "\\\\"); //$NON-NLS-1$ //$NON-NLS-2$

         String imageOffset = CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_IMAGE_OFFSET, IGDBJtagConstants.DEFAULT_IMAGE_OFFSET);
         if (imageOffset.length() > 0) {
            imageOffset = (imageFileName.endsWith(".elf")) ? "" : 
               "0x" + CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_IMAGE_OFFSET, IGDBJtagConstants.DEFAULT_IMAGE_OFFSET); //$NON-NLS-2$ 
         }
//            System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepLoadImage() IMAGE_FILE, imageFileName = " + imageFileName);
         List<String> commands = new ArrayList<String>();
         fUsbdmGdbInterface.doLoadImage(imageFileName, imageOffset, commands);
         queueCommands(commands, rm);									
      } catch (CoreException e) {
//         System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepLoadImage() CoreException = " + e.getMessage());
         rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Cannot load image", e)); //$NON-NLS-1$
         rm.done();
      }
   }

   /**
    * Now that we are connected to the target, we should update
    * our container to properly fill in its pid.
    */
   @Execute
   public void stepUpdateContainer(RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepUpdateContainer()");
      String groupId = getContainerContext().getGroupId();
      setContainerContext(fProcService.createContainerContextFromGroupId(fCommandControl.getContext(), groupId));
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
         rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Cannot get inferior arguments", e)); //$NON-NLS-1$
         rm.done();
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
         rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Cannot get environment information", e)); //$NON-NLS-1$
         rm.done();
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
      if (CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_SET_PC_REGISTER, IGDBJtagConstants.DEFAULT_SET_PC_REGISTER)) {
         String pcRegister = CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_PC_REGISTER, 
               CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_IMAGE_OFFSET, IGDBJtagConstants.DEFAULT_PC_REGISTER)); 
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
      if (CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_SET_STOP_AT, IGDBJtagConstants.DEFAULT_SET_STOP_AT)) {
         String stopAt = CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_STOP_AT, IGDBJtagConstants.DEFAULT_STOP_AT); 
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
            (CDebugUtils.getAttribute(fAttributes,  IGDBJtagConstants.ATTR_LOAD_IMAGE, IGDBJtagConstants.DEFAULT_LOAD_IMAGE) &&
             CDebugUtils.getAttribute(fAttributes,  IGDBJtagConstants.ATTR_SET_RESUME, IGDBJtagConstants.DEFAULT_SET_RESUME)) ||
            (!CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_LOAD_IMAGE, IGDBJtagConstants.DEFAULT_LOAD_IMAGE) &&
              CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_DO_RESET,   IGDBJtagConstants.DEFAULT_DO_RESET) &&
             !CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_DO_HALT,    IGDBJtagConstants.DEFAULT_DO_HALT))) {
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
            (CDebugUtils.getAttribute(fAttributes,  IGDBJtagConstants.ATTR_LOAD_IMAGE, IGDBJtagConstants.DEFAULT_LOAD_IMAGE) &&
             CDebugUtils.getAttribute(fAttributes,  IGDBJtagConstants.ATTR_SET_RESUME, IGDBJtagConstants.DEFAULT_SET_RESUME)) ||
            (!CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_LOAD_IMAGE, IGDBJtagConstants.DEFAULT_LOAD_IMAGE) &&
              CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_DO_RESET,   IGDBJtagConstants.DEFAULT_DO_RESET) &&
             !CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_DO_HALT,    IGDBJtagConstants.DEFAULT_DO_HALT))) {
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
         String userCmd = CDebugUtils.getAttribute(fAttributes, IGDBJtagConstants.ATTR_RUN_COMMANDS, IGDBJtagConstants.DEFAULT_RUN_COMMANDS); 
         userCmd = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(userCmd);
         queueCommands(Arrays.asList(userCmd.split("\\r?\\n")), rm); //$NON-NLS-1$
      } catch (CoreException e) {
         rm.setStatus(new Status(IStatus.ERROR, UsbdmGdbServer.PLUGIN_ID, -1, "Cannot run user defined run commands", e)); //$NON-NLS-1$
         rm.done();
      }
   }

   private final static String INVALID = "invalid";   //$NON-NLS-1$

   /** 
    * If we are dealing with a remote-attach debugging session, connect to the target.
    * @since 4.0
    */
   @Execute
   public void stepUsbdmConnection(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepUsbdmConnection()");
      if (fGDBBackend.getSessionType() == SessionType.REMOTE && fGDBBackend.getIsAttachSession()) {
         boolean isTcpConnection = CDebugUtils.getAttribute(fAttributes, IGDBLaunchConfigurationConstants.ATTR_REMOTE_TCP, false);
         if (isTcpConnection) {
            String remoteTcpHost = CDebugUtils.getAttribute(fAttributes, IGDBLaunchConfigurationConstants.ATTR_HOST, INVALID);
            String remoteTcpPort = CDebugUtils.getAttribute(fAttributes, IGDBLaunchConfigurationConstants.ATTR_PORT, INVALID);
            fCommandControl.queueCommand(
                  fCommandFactory.createMITargetSelect(fCommandControl.getContext(), 
                        remoteTcpHost, remoteTcpPort, true), 
                        new ImmediateDataRequestMonitor<MIInfo>(rm));
         } else {
            String serialDevice = CDebugUtils.getAttribute(fAttributes, IGDBLaunchConfigurationConstants.ATTR_DEV, INVALID);
            fCommandControl.queueCommand(
                  fCommandFactory.createMITargetSelect(fCommandControl.getContext(), 
                        serialDevice, true), 
                        new ImmediateDataRequestMonitor<MIInfo>(rm));
         }
      } else {
         rm.done();
      }
   }

   /**
    * If we are dealing with an local attach session, perform the attach.
     * For a remote attach session, we don't attach during the launch; instead
     * we wait for the user to manually do the attach.
    * @since 4.0 
    */
   @Execute
   public void stepUsbdmAttachToProcess(final RequestMonitor rm) {
//      System.err.println("UsbdmGdbDsfFinalLaunchSequence.stepUsbdmAttachToProcess()");
      if (fGDBBackend.getIsAttachSession() && fGDBBackend.getSessionType() != SessionType.REMOTE) {
         // Is the process id already stored in the launch?
         int pid = CDebugUtils.getAttribute(fAttributes, ICDTLaunchConfigurationConstants.ATTR_ATTACH_PROCESS_ID, -1);
         if (pid != -1) {
            fProcService.attachDebuggerToProcess(
                  fProcService.createProcessContext(fCommandControl.getContext(), Integer.toString(pid)),
                  new DataRequestMonitor<IDMContext>(getExecutor(), rm));
         } else {
            IConnectHandler connectCommand = (IConnectHandler)fSession.getModelAdapter(IConnectHandler.class);
            if (connectCommand instanceof IConnect) {
               ((IConnect)connectCommand).connect(rm);
            } else {
               rm.done();
            }
         }
      } else {
         rm.done();
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
		IGDBMemory memory = fTracker.getService(IGDBMemory.class);
		IMemoryDMContext memContext = DMContexts.getAncestorOfType(getContainerContext(), IMemoryDMContext.class);
		if (memory == null || memContext == null) {
			rm.done();
			return;
		}
		memory.initializeMemoryData(memContext, rm);
	}
	
	public DsfSession getSession() {
      return fSession;
   }
}

