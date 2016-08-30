package net.sourceforge.usbdm.gdb.service;

import org.eclipse.cdt.dsf.service.AbstractDsfService;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.osgi.framework.BundleContext;

import net.sourceforge.usbdm.gdb.ui.Activator;

public class UsbdmExtendedFunctions extends AbstractDsfService implements IUsbdmExtendedFunctions {

   public UsbdmExtendedFunctions(DsfSession session) {
      super(session);
   }

   @Override
   protected BundleContext getBundleContext() {
      return Activator.getBundleContext();
   }
   
}
