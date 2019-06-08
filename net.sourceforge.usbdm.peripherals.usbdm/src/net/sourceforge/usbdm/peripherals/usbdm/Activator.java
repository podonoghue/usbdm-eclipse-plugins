package net.sourceforge.usbdm.peripherals.usbdm;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.sourceforge.usbdm.peripherals.usbdm"; //$NON-NLS-1$

	private static BundleContext context;

   // The shared instance
   private static Activator plugin = null;

   /**
    * Get bundle context for this plug-in
    * 
    * @return Bundle context
    */
	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
      super.start(bundleContext);
      plugin  = this;
      context = bundleContext;
      System.err.println(String.format("[%s, %s].start()", getBundle().getSymbolicName(), getBundle().getVersion()));
   }

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
	   super.stop(bundleContext);
      System.err.println(String.format("[%s].start()", "net.sourceforge.usbdm.peripherals.usbdm.Activator"));
		context = null;
	}
	
   public static Activator getDefault() {
      return plugin;
   }

   static public void log(String msg) {
      log(msg, null);
   }

   static public void log(String msg, Exception e) {
      if (getDefault() == null) {
         if (e != null) {
            e.printStackTrace();
         }
         System.out.println(msg + ((e!=null)?e.getMessage():""));
         return;
      }
      getDefault().getLog().log(new Status(Status.INFO, PLUGIN_ID, Status.OK, msg, e));
   }

   static public void logError(String msg, Exception e) {
      if (getDefault() == null) {
         if (e != null) {
            e.printStackTrace();
         }
         System.err.println(msg + ((e!=null)?e.getMessage():""));
         return;
      }
      getDefault().getLog().log(new Status(Status.ERROR, PLUGIN_ID, Status.ERROR, msg, e));
   }

}
