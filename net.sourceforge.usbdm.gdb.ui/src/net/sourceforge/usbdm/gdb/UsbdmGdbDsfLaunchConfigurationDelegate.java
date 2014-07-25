package net.sourceforge.usbdm.gdb;

import org.eclipse.cdt.dsf.debug.service.IDsfDebugServicesFactory;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

/**
 * @since 4.10
 */
public class UsbdmGdbDsfLaunchConfigurationDelegate extends GdbLaunchDelegate
      implements ILaunchConfigurationDelegate {

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
//      System.err.println("net.sourceforge.usbdm.gdb.newServiceFactory()");
      return new UsbdmGdbJtagDebugServicesFactory(version);
   }

}
