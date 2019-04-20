package net.sourceforge.usbdm.gdb.launch;

import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ISourceLocator;

/**
 * Empty wrapper class to allow extension
 * @since 5.1
 */
public class UsbdmExtendedLaunch extends GdbLaunch {

   public UsbdmExtendedLaunch(ILaunchConfiguration launchConfiguration, String mode, ISourceLocator locator) {
      super(launchConfiguration, mode, locator);
//      System.err.println("UsbdmExtendedLaunch");
   }
}
