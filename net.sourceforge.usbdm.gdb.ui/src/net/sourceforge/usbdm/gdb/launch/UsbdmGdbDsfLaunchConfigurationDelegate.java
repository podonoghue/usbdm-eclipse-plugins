package net.sourceforge.usbdm.gdb.launch;

import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.concurrent.Sequence;
import org.eclipse.cdt.dsf.debug.service.IDsfDebugServicesFactory;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ISourceLocator;

import net.sourceforge.usbdm.gdb.UsbdmGdbServer;
import net.sourceforge.usbdm.gdb.service.UsbdmGdbDebugServicesFactory;

public class UsbdmGdbDsfLaunchConfigurationDelegate extends GdbLaunchDelegate {

   public UsbdmGdbDsfLaunchConfigurationDelegate() {
      super();
//      System.err.println("...gdb.UsbdmGdbDsfLaunchConfigurationDelegate()");
   }

   /**
    * 
    * @param requireCProject
    */
   public UsbdmGdbDsfLaunchConfigurationDelegate(boolean requireCProject) {
      super(requireCProject);
//      System.err.println("...gdb.UsbdmGdbDsfLaunchConfigurationDelegate(requireCProject)");
   }

   /**
    * @param config   - Configuration to launch
    * @param mode     - Mode in which to launch, one of the mode constants defined by ILaunchManager - RUN_MODE or DEBUG_MODE.
    * @param launch   - Launch object to contribute processes and debug targets to
    * @param monitor  - Progress monitor, or null progress monitor, or null. A cancelable progress monitor is provided by the Job framework. It should be noted that the setCanceled(boolean) method should never be called on the provided monitor or the monitor passed to any delegates from this method; due to a limitation in the progress monitor framework using the setCanceled method can cause entire workspace batch jobs to be canceled, as the canceled flag is propagated up the top-level parent monitor. The provided monitor is not guaranteed to have been started. 
    */
   @Override
   public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
//      System.err.println("...gdb.UsbdmGdbDsfLaunchConfigurationDelegate.launch(...)");

      org.eclipse.cdt.launch.LaunchUtils.enableActivity("org.eclipse.cdt.debug.dsfgdbActivity", true); //$NON-NLS-1$
      if ( monitor == null ) {
         monitor = new NullProgressMonitor();
      }
      if ( mode.equals( ILaunchManager.DEBUG_MODE ) ) {
         launchDebugger( config, launch, monitor );
      }
      else if ( mode.equals( ILaunchManager.RUN_MODE ) ) {
         launchDebugger( config, launch, monitor );
         
         final GdbLaunch gdbLaunch = (GdbLaunch)launch;
         gdbLaunch.disconnect();
         gdbLaunch.shutdownSession(new RequestMonitor(ImmediateExecutor.getInstance(), null));
      }
   }

   private void launchDebugger( ILaunchConfiguration config, ILaunch launch, IProgressMonitor monitor ) throws CoreException {
//      System.err.println("...gdb.UsbdmGdbDsfLaunchConfigurationDelegate.launchDebugger(...)");
      monitor.beginTask("Launching debugger session", 10);  //$NON-NLS-1$
      if ( monitor.isCanceled() ) {
         cleanupLaunch();
         return;
      }
      try {
         launchDebugSession( config, launch, monitor );
      }
      finally {
         monitor.done();
      }     
   }

   @Override
   protected void launchDebugSession(ILaunchConfiguration config, ILaunch l, IProgressMonitor monitor) throws CoreException {
//      System.err.println("...gdb.UsbdmGdbDsfLaunchConfigurationDelegate.launchDebugSession(...)");
      super.launchDebugSession(config, l, monitor);
   }

   /**
    * Method called to create the services factory for this debug session.
    */
   @Override
   protected IDsfDebugServicesFactory newServiceFactory(ILaunchConfiguration config, String version) {
//      System.err.println("...gdb.UsbdmGdbDsfLaunchConfigurationDelegate.newServiceFactory(...)");
      return new UsbdmGdbDebugServicesFactory(config, version);
   }

   @Override
   protected GdbLaunch createGdbLaunch(ILaunchConfiguration configuration, String mode, ISourceLocator locator) throws CoreException {
//      System.err.println("...gdb.UsbdmGdbDsfLaunchConfigurationDelegate.createGdbLaunch(...)");
      return new UsbdmExtendedLaunch(configuration, mode, locator);
   }

   @Override
   protected Sequence getServicesSequence(DsfSession session, ILaunch launch, IProgressMonitor rm) {
//      System.err.println("...gdb.UsbdmGdbDsfLaunchConfigurationDelegate.getServicesSequence(...)");
      return new UsbdmGdbServicesLaunchSequence(session, (GdbLaunch) launch, rm);
   }

   @Override
   protected String getPluginID() {
      return UsbdmGdbServer.getPluginId();
   }

   
}
