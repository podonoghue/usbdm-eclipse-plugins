package net.sourceforge.usbdm.gdb;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.usbdm.gdb.GdbServerParameters.GdbServerType;
import net.sourceforge.usbdm.jni.UsbdmException;

import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice;
import org.eclipse.cdt.debug.mi.core.AbstractGDBCDIDebugger;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIPlugin;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.cdi.Session;
import org.eclipse.cdt.debug.mi.core.cdi.model.Target;
import org.eclipse.cdt.debug.mi.core.command.CLICommand;
import org.eclipse.cdt.debug.mi.core.command.Command;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.cdt.debug.mi.core.command.MICommand;
import org.eclipse.cdt.debug.mi.core.command.MIGDBSetNewConsole;
import org.eclipse.cdt.debug.mi.core.output.MIInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

public class UsbdmGdbCdiDebugger extends AbstractGDBCDIDebugger {

   private String    miVersion;
   
   @Override
   public ICDISession createSession(ILaunch launch, File executable,
         IProgressMonitor monitor) throws CoreException {
      return super.createSession(launch, executable, monitor);
   }

   @Override
   public ICDISession createDebuggerSession(ILaunch launch, IBinaryObject exe,
         IProgressMonitor monitor) throws CoreException {
//      System.err.println("UsbdmGdbDebugger.createDebuggerSession()");
      return super.createDebuggerSession(launch, exe, monitor);
   }
   
   @Override
   protected CommandFactory getCommandFactory(ILaunchConfiguration config)
         throws CoreException {
      miVersion = MIPlugin.getMIVersion(config);
      return new UsbdmGdbCommandFactory(miVersion);
   }
   
   private MISession createMISession(Session session, SubMonitor submonitor) throws CoreException {
      
      submonitor.subTask("Creating debug session"); //$NON-NLS-1$
      
      ICDITarget[] targets = session.getTargets();
      if (targets.length == 0 || !(targets[0] instanceof Target)) {
         throw new CoreException(new Status( IStatus.ERROR, Activator.getPluginId(),
               -1, "Error getting debug target.", null));
      }
      MISession miSession = ((Target)targets[0]).getMISession();
      CommandFactory factory = miSession.getCommandFactory();
      if (submonitor.isCanceled()) {
         throw new OperationCanceledException();
      }
      try {
         MIGDBSetNewConsole newConsole = factory.createMIGDBSetNewConsole();
         miSession.postCommand(newConsole);
         MIInfo info = newConsole.getMIInfo();
         if (info == null) {
            throw new MIException(MIPlugin.getResourceString("src.common.No_answer")); //$NON-NLS-1$
         }
      }
      catch( MIException e ) {
         // We ignore this exception, for example
         // on GNU/Linux the new-console is an error.
      }
      if (submonitor.isCanceled()) {
         throw new OperationCanceledException();
      }
      return miSession;
   }
   
   private void sessionLoadSymbols(ILaunchConfiguration config, IGDBJtagDevice gdbJtagDevice, MISession miSession, SubMonitor submonitor) throws CoreException {
      List<String> commands = new ArrayList<String>();

      // execute symbol load
      boolean doLoadSymbols = config.getAttribute(IGDBJtagConstants.ATTR_LOAD_SYMBOLS, IGDBJtagConstants.DEFAULT_LOAD_SYMBOLS);
      if (doLoadSymbols) {
         
         submonitor.subTask("Loading symbols"); //$NON-NLS-1$

         String symbolsFileName = null;
         // New setting in Helios. Default is true. Check for existence
         // in order to support older launch configs
         if (config.hasAttribute(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS) &&
               config.getAttribute(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_SYMBOLS, IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_SYMBOLS)) {
            IPath programFile = CDebugUtils.verifyProgramPath(config);
            if (programFile != null) {
               symbolsFileName = programFile.toOSString();
            }
         }
         else {
            symbolsFileName = config.getAttribute(IGDBJtagConstants.ATTR_SYMBOLS_FILE_NAME, IGDBJtagConstants.DEFAULT_SYMBOLS_FILE_NAME);
            if (symbolsFileName.length() > 0) {
               symbolsFileName = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(symbolsFileName);
            }
         }
         if (symbolsFileName == null) {
            // The launch config GUI should prevent this from happening, but just in case             
            throw new CoreException(new Status( IStatus.ERROR,
                  Activator.getPluginId(),
                  -1, "Symbolics loading was requested but file was not specified or not found.", null));
         }
         // Escape windows path separator characters TWICE, once for Java and once for GDB.
         symbolsFileName = symbolsFileName.replace("\\", "\\\\");

         String symbolsOffset = config.getAttribute(IGDBJtagConstants.ATTR_SYMBOLS_OFFSET, IGDBJtagConstants.DEFAULT_SYMBOLS_OFFSET);
         if (symbolsOffset.length() > 0) {
            symbolsOffset = "0x" + symbolsOffset;              
         }
         commands.clear();
         gdbJtagDevice.doLoadSymbol(symbolsFileName, symbolsOffset, commands);
         submonitor.beginTask("Symbolics loading was requested but file was not specified or not found.", 1); //$NON-NLS-1$            
         executeGDBScript(getGDBScript(commands), miSession, submonitor.newChild(15));
      }
      if (submonitor.isCanceled()) {
         throw new OperationCanceledException();
      }
   }
   
   protected String escapePath(String file) {
      if (file.indexOf('\\') >= 0) {
         return escapeArg(file.replace("\\", "\\\\"));
      }
      return escapeArg(file);
   }

   protected String escapeArg(String arg) {
      if (arg.indexOf(' ') >= 0) { 
         return '"' + arg + '"'; 
      }
      return arg;
   }

   public void startServer(GdbServerParameters gdbServerParameters, SubMonitor submonitor) throws CoreException {
      
//      System.err.println("UsbdmGdbDebugger.startServer()");

      submonitor.subTask("Starting GDB Server"); //$NON-NLS-1$

//      InterfaceType interfaceType = gdbServerParameters.getInterfaceType();
//      System.err.println("UsbdmGdbDebugger.startServer() interfaceType = "+interfaceType.toString());

      GdbServerInterface gdbServerInterface = new GdbServerInterface(gdbServerParameters);  
      try {
         gdbServerInterface.startServer();
      } catch (UsbdmException e1) {
         e1.printStackTrace();
         throw new CoreException(new Status( IStatus.ERROR,
               Activator.getPluginId(),
               -1, "Starting server failed.", null));
      }
   }  

   private void sessionDoRemote(ILaunchConfiguration config, UsbdmGdbInterface usbdmSocketInterface, MISession miSession, SubMonitor submonitor) throws CoreException {
      
      GdbServerParameters gdbServerParameters = GdbServerParameters.getInitializedServerParameters(config);
      if (gdbServerParameters == null) {
         throw new CoreException(new Status( IStatus.ERROR,
               Activator.getPluginId(),
               -1, "GDB Server Parameters not found.", null));
      }
//      InterfaceType interfaceType = gdbServerParameters.getInterfaceType();
//      System.err.println("UsbdmGdbDebugger.startServer() interfaceType = "+interfaceType.toString());
      
      if (gdbServerParameters.getServerType() == GdbServerType.SERVER_PIPE) {
         submonitor.subTask("Connecting to remote"); //$NON-NLS-1$

         ArrayList<String> serverCommandLine = gdbServerParameters.getSerialCommandLine();
         // Create GDB pipe command '| exename args...
         // exename is escaped as a path (i.e. \ are doubled)
         // args are quoted if they contain spaces
         String commandLine = new String();
         commandLine = "| " + escapePath(serverCommandLine.get(0)) + " ";
         for (int index=1; index<serverCommandLine.size(); index++) { 
            commandLine += escapeArg(serverCommandLine.get(index)) + " ";
         }
//         System.err.println("sessionDoRemote() command = \'" + commandLine + "\'");
         List<String> commands = new ArrayList<String>();
         usbdmSocketInterface.doRemote(commandLine, commands);
         executeGDBScript(getGDBScript(commands), miSession, submonitor.newChild(10));
         if (submonitor.isCanceled()) {
            throw new OperationCanceledException();
         }
      }
      
      if (gdbServerParameters.getServerType() == GdbServerType.SERVER_SOCKET) {
         submonitor.subTask("Connecting to remote"); //$NON-NLS-1$

         startServer(gdbServerParameters, submonitor.newChild(50));
         
         String commandLine = new String();
         commandLine = "localhost:" + Integer.toString(gdbServerParameters.getGdbPortNumber());
//         System.err.println("sessionDoRemote() command = \'" + commandLine + "\'");
         List<String> commands = new ArrayList<String>();
         usbdmSocketInterface.doRemote(commandLine, commands);
         executeGDBScript(getGDBScript(commands), miSession, submonitor.newChild(10));
         if (submonitor.isCanceled()) {
            throw new OperationCanceledException();
         }
      }
   }
   
   protected static final String LINESEP = System.getProperty("line.separator"); //$NON-NLS-1$

   private void sessionDoResetandHalt(ILaunchConfiguration config, IGDBJtagDevice gdbJtagDevice, MISession miSession, SubMonitor submonitor) throws CoreException {
      List<String> commands = new ArrayList<String>();

      // execute init script
      submonitor.subTask("Executing initialization commands"); //$NON-NLS-1$

      // Run device-specific code to reset the board
      if (config.getAttribute(IGDBJtagConstants.ATTR_DO_RESET, IGDBJtagConstants.DEFAULT_DO_RESET)) {
         commands.clear();
         gdbJtagDevice.doReset(commands);
         int defaultDelay = gdbJtagDevice.getDefaultDelay();
         gdbJtagDevice.doDelay(config.getAttribute(IGDBJtagConstants.ATTR_DELAY, defaultDelay), commands);
         executeGDBScript(getGDBScript(commands), miSession, submonitor.newChild(15));
      }
      submonitor.setWorkRemaining(65); // compensate for optional work above

      // Run device-specific code to halt the board
      if (config.getAttribute(IGDBJtagConstants.ATTR_DO_HALT, IGDBJtagConstants.DEFAULT_DO_HALT)) {
         commands.clear();
         gdbJtagDevice.doHalt(commands);
         executeGDBScript(getGDBScript(commands), miSession, submonitor.newChild(15));
      }
      // Execute any user defined init command
      executeGDBScript(config, IGDBJtagConstants.ATTR_INIT_COMMANDS, miSession,
            submonitor.newChild(15));
      if (submonitor.isCanceled()) {
         throw new OperationCanceledException();
      }
   }
   
   private void sessionDoLoad(ILaunchConfiguration config, IGDBJtagDevice gdbJtagDevice, MISession miSession, SubMonitor submonitor) throws CoreException {

      // execute load
      boolean doLoad = config.getAttribute(IGDBJtagConstants.ATTR_LOAD_IMAGE, IGDBJtagConstants.DEFAULT_LOAD_IMAGE);
      if (doLoad) {
         submonitor.subTask("Loading file image"); //$NON-NLS-1$

         String imageFileName = null;

         // New setting in Helios. Default is true. Check for existence
         // in order to support older launch configs
         if (config.hasAttribute(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE) &&
               config.getAttribute(IGDBJtagConstants.ATTR_USE_PROJ_BINARY_FOR_IMAGE, IGDBJtagConstants.DEFAULT_USE_PROJ_BINARY_FOR_IMAGE)) {
            IPath programFile = CDebugUtils.verifyProgramPath(config);
            if (programFile != null) {
               imageFileName = programFile.toOSString();
            }
         }
         else {
            imageFileName = config.getAttribute(IGDBJtagConstants.ATTR_IMAGE_FILE_NAME, IGDBJtagConstants.DEFAULT_IMAGE_FILE_NAME);
            if (imageFileName.length() > 0) {
               imageFileName = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(imageFileName);
            }
         }
         if (imageFileName == null) {
            // The launch config GUI should prevent this from happening, but just in case
            throw new CoreException(new Status( IStatus.ERROR,
                  Activator.getPluginId(),
                  -1, "Image loading was requested but file was not specified or not ", null));
         }
         imageFileName = imageFileName.replace("\\", "\\\\");

         String imageOffset = config.getAttribute(IGDBJtagConstants.ATTR_IMAGE_OFFSET, IGDBJtagConstants.DEFAULT_IMAGE_OFFSET);
         if (imageOffset.length() > 0) {
            imageOffset = (imageFileName.endsWith(".elf")) ? "" : "0x" + config.getAttribute(IGDBJtagConstants.ATTR_IMAGE_OFFSET, IGDBJtagConstants.DEFAULT_IMAGE_OFFSET);             
         }

         List<String> commands = new ArrayList<String>();
         commands.clear();
         gdbJtagDevice.doLoadImage(imageFileName, imageOffset, commands);
         executeGDBScript(getGDBScript(commands), miSession, submonitor.newChild(20));
      }
      if (submonitor.isCanceled()) {
         throw new OperationCanceledException();
      }
   }
   
   @Override
   protected void doStartSession(ILaunch launch, Session session, IProgressMonitor monitor) throws CoreException {
      SubMonitor submonitor = SubMonitor.convert(monitor, 100);

      try {

         MISession miSession = createMISession(session, submonitor.newChild(10));
         
         ILaunchConfiguration config = launch.getLaunchConfiguration();

         UsbdmGdbInterface gdbJtagDevice = getUsbdmInterfaceDevice(config);

         sessionLoadSymbols(config, gdbJtagDevice, miSession, submonitor.newChild(10));

         sessionDoRemote(config, gdbJtagDevice, miSession, submonitor.newChild(10));

         sessionDoResetandHalt(config, gdbJtagDevice, miSession, submonitor.newChild(15));
         
         sessionDoLoad(config, gdbJtagDevice, miSession, submonitor.newChild(20));

      } catch (OperationCanceledException e) {
         if (launch != null && launch.canTerminate()) {
            launch.terminate();
         }
      } finally {
         if (monitor != null) {
            monitor.done();
         }
      }
   }

   public void doRunSession(ILaunch launch, ICDISession session, IProgressMonitor monitor) throws CoreException {
//      System.err.println("UsbdmGdbDebugger.doRunSession()");
      
      SubMonitor submonitor = SubMonitor.convert(monitor, 100);
      
      try {
         ILaunchConfiguration config = launch.getLaunchConfiguration();
         ICDITarget[] targets = session.getTargets();
         if ( targets.length == 0 || !(targets[0] instanceof Target) )
            return;
         MISession miSession = ((Target)targets[0]).getMISession();

         IGDBJtagDevice gdbJtagDevice;
         try {
            gdbJtagDevice = getUsbdmInterfaceDevice(config);
         } catch (NullPointerException e) {
            return;
         }

         if (submonitor.isCanceled()) {
            throw new OperationCanceledException();
         }
         submonitor.worked(20);
         List<String> commands = new ArrayList<String>();
         // Set program counter
         boolean setPc = config.getAttribute(IGDBJtagConstants.ATTR_SET_PC_REGISTER, IGDBJtagConstants.DEFAULT_SET_PC_REGISTER);
         if (setPc) {
            String pcRegister = config.getAttribute(IGDBJtagConstants.ATTR_PC_REGISTER, config.getAttribute(IGDBJtagConstants.ATTR_IMAGE_OFFSET, IGDBJtagConstants.DEFAULT_PC_REGISTER)); 
            gdbJtagDevice.doSetPC(pcRegister, commands);
            executeGDBScript(getGDBScript(commands), miSession, submonitor.newChild(20));
         }
         submonitor.setWorkRemaining(60); // compensate for optional work above

         // execute run script
         monitor.beginTask("Executing run commands", 1); //$NON-NLS-1$
         boolean setStopAt = config.getAttribute(IGDBJtagConstants.ATTR_SET_STOP_AT, IGDBJtagConstants.DEFAULT_SET_STOP_AT);
         if (setStopAt) {
            String stopAt = config.getAttribute(IGDBJtagConstants.ATTR_STOP_AT, IGDBJtagConstants.DEFAULT_STOP_AT); 
            commands.clear();
            gdbJtagDevice.doStopAt(stopAt, commands);
            executeGDBScript(getGDBScript(commands), miSession, submonitor.newChild(20));
         }
         submonitor.setWorkRemaining(40); // compensate for optional work above

         boolean setResume = config.getAttribute(IGDBJtagConstants.ATTR_SET_RESUME, IGDBJtagConstants.DEFAULT_SET_RESUME);
         if (setResume) {
            commands.clear();
            gdbJtagDevice.doContinue(commands);
            executeGDBScript(getGDBScript(commands), miSession, submonitor.newChild(20));
         }
         submonitor.setWorkRemaining(20); // compensate for optional work above
         // Run any user defined command
         executeGDBScript(config, IGDBJtagConstants.ATTR_RUN_COMMANDS, miSession, 
               submonitor.newChild(20));
      } catch (OperationCanceledException e) {
         if (launch != null && launch.canTerminate()) {
            launch.terminate();
         }
      }
   }
   
   private void executeGDBScript(String script, MISession miSession, 
         IProgressMonitor monitor) throws CoreException {
      // Try to execute any extra command
      if (script == null || script.length() == 0)
         return;
      script = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(script);
      String[] commands = script.split("\\r?\\n");
      SubMonitor submonitor = SubMonitor.convert(monitor, commands.length);
      for (int j = 0; j < commands.length; ++j) {
         try {
            submonitor.subTask("GDB command:" + commands[j]); //$NON-NLS-1$
            Command cmd = null;
            if (commands[j].startsWith("-")) {
               cmd = new MICommand(miVersion, commands[j]);
            } else {
               cmd = new CLICommand(commands[j]);
            }
            miSession.postCommand(cmd, MISession.FOREVER);
            submonitor.worked(1);
            if (submonitor.isCanceled()) {
               throw new OperationCanceledException();
            }
            MIInfo info = cmd.getMIInfo();
            if (info == null) {
               throw new MIException("Timeout"); //$NON-NLS-1$
            }
         } catch (MIException e) {
            MultiStatus status = new MultiStatus(
                  Activator.PLUGIN_ID,
                  ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR,
                  "Failed command", e); //$NON-NLS-1$
            status
                  .add(new Status(
                        IStatus.ERROR,
                        Activator.PLUGIN_ID,
                        ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR,
                        e == null ? "" : e.getLocalizedMessage(), //$NON-NLS-1$
                        e));
            CDebugCorePlugin.log(status);
         }
      }
   }
   
   private void executeGDBScript(ILaunchConfiguration configuration, String attribute,
         MISession miSession, IProgressMonitor monitor) throws CoreException {
      executeGDBScript(configuration.getAttribute(attribute, ""), miSession, monitor); //$NON-NLS-1$
   }

   private UsbdmGdbInterface getUsbdmInterfaceDevice (ILaunchConfiguration config) 
      throws CoreException {
      try {
         return new UsbdmGdbInterface();
      } catch (NullPointerException e) {
         throw new CoreException(new Status( IStatus.ERROR,
               Activator.getPluginId(),
               -1, "Unable to get device.", null));
      }
   }
   
   private String getGDBScript(List<String> commands) {
      if (commands.isEmpty())
         return null;
      StringBuffer sb = new StringBuffer();
      for (String cmd : commands) {
         sb.append(cmd);
      }
      return sb.toString();
   }
}
