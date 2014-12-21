package net.sourceforge.usbdm.gdb;

import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.debug.service.IDsfDebugServicesFactory;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * @since 4.10
 */
public class UsbdmGdbDsfLaunchConfigurationDelegate extends GdbLaunchDelegate
      implements ILaunchConfigurationDelegate {

   /* (non-Javadoc)
    * @see org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
    */
   @Override
   public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {

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

   /* (non-Javadoc)
    * @see org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate#launchDebugSession(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
    */
   @Override
   protected void launchDebugSession(ILaunchConfiguration config, ILaunch l, IProgressMonitor monitor) throws CoreException {
      super.launchDebugSession(config, l, monitor);
   }

   /**
    * 
    */
   public UsbdmGdbDsfLaunchConfigurationDelegate() {
      super();
//      System.err.println("net.sourceforge.usbdm.gdb.UsbdmGdbDsfLaunchConfigurationDelegate()");
   }

   /**
    * 
    * @param requireCProject
    */
   public UsbdmGdbDsfLaunchConfigurationDelegate(boolean requireCProject) {
      super(requireCProject);
//      System.err.println("net.sourceforge.usbdm.gdb.UsbdmGdbDsfLaunchConfigurationDelegate(requireCProject)");
   }

   /**
    * Method called to create the services factory for this debug session.
    * A subclass can override this method and provide its own ServiceFactory.
    * @since 4.1
    */
   protected IDsfDebugServicesFactory newServiceFactory(ILaunchConfiguration config, String version) {
//      System.err.println("UsbdmGdbDsfLaunchConfigurationDelegate.newServiceFactory()");
      return new UsbdmGdbDebugServicesFactory(version);
   }

}
