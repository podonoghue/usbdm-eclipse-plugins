package net.sourceforge.usbdm.gdb;

import java.util.Map;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;

import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.CDIDebugModel;
import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.launch.AbstractCLaunchDelegate;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.IProcess;

/**
 * @since 4.12
 */
public class UsbdmGdbLaunchConfigurationDelegate extends AbstractCLaunchDelegate implements
      ILaunchConfigurationDelegate {

   @Override
   protected String getPluginID() {
      return UsbdmGdbServer.getPluginId();
   };

   @Override
   public void launch(ILaunchConfiguration configuration, String mode,
         ILaunch launch, IProgressMonitor monitor) throws CoreException {
//      System.err.println("UsbdmGdbLaunchConfigurationDelegate.launch()");
      SubMonitor submonitor = SubMonitor.convert(monitor, 2);
      // set the default source locator if required
      setDefaultSourceLocator(launch, configuration);

      if (mode.equals(ILaunchManager.DEBUG_MODE)) {
         UsbdmGdbCdiDebugger debugger = new UsbdmGdbCdiDebugger();
         ICProject project = CDebugUtils.verifyCProject(configuration);
         IPath exePath = CDebugUtils.verifyProgramPath(configuration);
         ICDISession session = debugger.createSession(launch, null, submonitor.newChild(1));
         // Attache device name to session
         String deviceName = configuration.getAttribute(UsbdmSharedConstants.LAUNCH_DEVICE_NAME_KEY, (String)null);
         session.setAttribute(UsbdmSharedConstants.LAUNCH_DEVICE_NAME_KEY, deviceName);
         IBinaryObject exeBinary = null;
         if ( exePath != null ) {
            exeBinary = verifyBinary(project, exePath);
         }
         
         try {
            // create the Launch targets/processes for eclipse.
            ICDITarget[] targets = session.getTargets();
            for( int i = 0; i < targets.length; i++ ) {
               Process process = targets[i].getProcess();
               IProcess iprocess = null;
//               System.err.println("UsbdmGdbLaunchConfigurationDelegate.launch() process = " + process);
               if ( process != null ) {
                  @SuppressWarnings("unchecked")
                  Map<String,String> map = getDefaultProcessMap();
                  iprocess = DebugPlugin.newProcess(launch, process, renderProcessLabel(exePath != null ? exePath.toOSString() : "???"), map);
//                  System.err.println("UsbdmGdbLaunchConfigurationDelegate.launch() iprocess = " + iprocess);
               }
               CDIDebugModel.newDebugTarget(launch, project.getProject(), targets[i],
                     renderProcessLabel("USBDM GDB Hardware Debugger"), iprocess, exeBinary, true, false, false);
            }
            debugger.doRunSession(launch, session, submonitor.newChild(1));
         } catch (CoreException e) {
            try {
               session.terminate();
            } catch (CDIException e1) {
               // ignore
            }
            throw e;
         }
      } else {
         cancel("TargetConfiguration not supported",
               ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
      }
   }

}
