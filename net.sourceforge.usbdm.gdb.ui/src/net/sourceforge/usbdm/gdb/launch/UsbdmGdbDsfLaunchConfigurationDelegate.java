package net.sourceforge.usbdm.gdb.launch;

import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.concurrent.Sequence;
import org.eclipse.cdt.dsf.debug.service.IDsfDebugServicesFactory;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate;
import org.eclipse.cdt.dsf.gdb.launching.LaunchUtils;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ISourceLocator;

import net.sourceforge.usbdm.gdb.service.UsbdmGdbDebugServicesFactory;
import net.sourceforge.usbdm.gdb.ui.Activator;

/**
 * @since 5.1
 */
public class UsbdmGdbDsfLaunchConfigurationDelegate extends GdbLaunchDelegate {

   public static final String GDB_DEBUG_MODEL_ID = "org.eclipse.cdt.dsf.gdb"; //$NON-NLS-1$

   public UsbdmGdbDsfLaunchConfigurationDelegate() {
      super();
//      System.err.println("UsbdmGdbDsfLaunchConfigurationDelegate()");
   }

   /**
    * 
    * @param requireCProject
    */
   public UsbdmGdbDsfLaunchConfigurationDelegate(boolean requireCProject) {
      super(requireCProject);
//      System.err.println("UsbdmGdbDsfLaunchConfigurationDelegate(requireCProject)");
   }

   /**
    * @param config   - Configuration to launch
    * @param mode     - Mode in which to launch, one of the mode constants defined by ILaunchManager - RUN_MODE or DEBUG_MODE.
    * @param launch   - Launch object to contribute processes and debug targets to
    * @param monitor  - Progress monitor, or null progress monitor, or null. A cancellable progress monitor is provided by the Job framework. It should be noted that the setCanceled(boolean) method should never be called on the provided monitor or the monitor passed to any delegates from this method; due to a limitation in the progress monitor framework using the setCanceled method can cause entire workspace batch jobs to be canceled, as the canceled flag is propagated up the top-level parent monitor. The provided monitor is not guaranteed to have been started. 
    */
   @Override
   public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
//      System.err.println("UsbdmGdbDsfLaunchConfigurationDelegate.launch(...)");

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
//      System.err.println("UsbdmGdbDsfLaunchConfigurationDelegate.launchDebugger(...)");
      monitor.beginTask("Launching debugger session", 10);  //$NON-NLS-1$
      if ( monitor.isCanceled() ) {
         cleanupLaunch(launch);
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
//      System.err.println("UsbdmGdbDsfLaunchConfigurationDelegate.launchDebugSession(...)");
      super.launchDebugSession(config, l, monitor);
   }

   /**
    * Return the label to be used for the CLI node 
    */
   protected String getCLILabel(ILaunchConfiguration config, String gdbVersion) throws CoreException {
      return LaunchUtils.getGDBPath(config).lastSegment().toString().trim() + " (" + gdbVersion +")"; //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * Method called to create the services factory for this debug session.
    * A subclass can override this method and provide its own ServiceFactory.
    */
   @Override
   protected IDsfDebugServicesFactory newServiceFactory(ILaunchConfiguration config, String version) {
//      System.err.println("UsbdmGdbDsfLaunchConfigurationDelegate.newServiceFactory(...)");
      return new UsbdmGdbDebugServicesFactory(version, config);
   }

   /**
    * Creates an object of GdbLaunch.
    * Subclasses who wish to just replace the GdbLaunch object with a sub-classed GdbLaunch
    * should override this method.
    * Subclasses who wish to replace the GdbLaunch object as well as change the 
    * initialization sequence of the launch, should override getLaunch() as well as this method.
    * Subclasses who wish to create a launch class which does not subclass GdbLaunch, 
    * are advised to override getLaunch() directly.
    * 
    * @param configuration The launch configuration
    * @param mode The launch mode - "run", "debug", "profile"
    * @param locator The source locator.  Can be null.
    * @return The GdbLaunch object, or a sub-classed object
    * @throws CoreException
    * @since 4.1
    */
   protected GdbLaunch createGdbLaunch(ILaunchConfiguration configuration, String mode, ISourceLocator locator) throws CoreException {
//      System.err.println("...gdb.UsbdmGdbDsfLaunchConfigurationDelegate.createGdbLaunch(...)");
      return new UsbdmExtendedLaunch(configuration, mode, locator);
   }

   /**
    * Returns a sequence that will create and initialise the different DSF services.
    * Subclasses that wish to add/remove services can override this method.
    * 
    * @param session The current DSF session
    * @param launch  The current launch
    * @param rm      The progress monitor that is to be used to cancel the sequence if so desired.
    */
   @Override
   protected Sequence getServicesSequence(DsfSession session, ILaunch launch, IProgressMonitor rm) {
//      System.err.println("UsbdmGdbDsfLaunchConfigurationDelegate.getServicesSequence(...)");
      return new UsbdmGdbServicesLaunchSequence(session, (GdbLaunch) launch, rm);
   }

   @Override
   protected String getPluginID() {
      return Activator.getPluginId();
   }
}
