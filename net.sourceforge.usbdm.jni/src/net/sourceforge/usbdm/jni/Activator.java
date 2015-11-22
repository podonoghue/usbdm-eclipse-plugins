package net.sourceforge.usbdm.jni;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * @since 4.12
 */
public class Activator extends AbstractUIPlugin {

   private static BundleContext context;

   static BundleContext getContext() {
      return context;
   }

   /*
    * (non-Javadoc)
    * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
    */
   @Override
   public void start(BundleContext bundleContext) throws Exception {
      super.start(bundleContext);
      System.err.println(String.format("[%s, %s].start()", getBundle().getSymbolicName(), getBundle().getVersion()));
      context = bundleContext;
   }

   /*
    * (non-Javadoc)
    * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
    */
   @Override
   public void stop(BundleContext bundleContext) throws Exception {
      context = null;
      Usbdm.exit();
      System.err.println(String.format("[%s, %s].stop()", getBundle().getSymbolicName(), getBundle().getVersion()));
      super.stop(bundleContext);
   }
}
