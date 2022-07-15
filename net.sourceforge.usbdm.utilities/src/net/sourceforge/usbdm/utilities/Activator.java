package net.sourceforge.usbdm.utilities;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin  {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.sourceforge.usbdm.utilities"; //$NON-NLS-1$

   // The shared instance
   private static Activator plugin = null;

	public void start(BundleContext bundleContext) throws Exception {
      super.start(bundleContext);
      plugin = this;
      System.err.println(String.format("[%s, %s].start()", getBundle().getSymbolicName(), getBundle().getVersion()));
   	}

	public void stop(BundleContext bundleContext) throws Exception {
      plugin = null;
      System.err.println(String.format("[%s, %s].stop()", getBundle().getSymbolicName(), getBundle().getVersion()));
      super.stop(bundleContext);
	}
	
   /**
    * Returns the shared instance
    *
    * @return The shared instance
    */
   public static Activator getDefault() {
      return plugin;
   }

   public static BundleContext getBundleContext() {
      if (getDefault() == null) {
         return null;
      }
      return getDefault().getBundle().getBundleContext();
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
