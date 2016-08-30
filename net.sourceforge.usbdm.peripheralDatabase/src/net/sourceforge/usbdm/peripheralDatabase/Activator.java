package net.sourceforge.usbdm.peripheralDatabase;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

   private static BundleContext context;

   /*
    * (non-Javadoc)
    *
    * @see 
    * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
    */
   public void start(BundleContext bundleContext) throws Exception {
      context = bundleContext;
      System.err.println(String.format("[%s].start()", "net.sourceforge.usbdm.peripheralDatabase"));
   }

   /*
    * (non-Javadoc)
    *
    * @see 
	* org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
    */
   public void stop(BundleContext bundleContext) throws Exception {
      context = null;
      System.err.println(String.format("[%s].stop()", "net.sourceforge.usbdm.peripheralDatabase"));
   }

   /**
    * Get bundle context for this plug-in
    * 
    * @return Bundle context
    */
   static BundleContext getContext() {
      return context;
   }

}
