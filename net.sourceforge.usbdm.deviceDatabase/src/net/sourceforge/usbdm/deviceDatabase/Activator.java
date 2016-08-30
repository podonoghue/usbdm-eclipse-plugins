package net.sourceforge.usbdm.deviceDatabase;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.sourceforge.usbdm.annotationEditor"; //$NON-NLS-1$

   // The shared instance
   private static Activator plugin = null;

   /**
    * The constructor
    */
   public Activator() {
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
    * )
    */
   public void start(BundleContext context) throws Exception {
      super.start(context);
      plugin = this;
      System.err.println(String.format("[%s, %s].start()", getBundle().getSymbolicName(), getBundle().getVersion()));
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
    * )
    */
   public void stop(BundleContext context) throws Exception {
      plugin = null;
      System.err.println(String.format("[%s, %s].stop()", getBundle().getSymbolicName(), getBundle().getVersion()));
      super.stop(context);
   }

   /**
    * Returns the shared instance
    *
    * @return the shared instance
    */
   public static Activator getDefault() {
      return plugin;
   }



   public static String getPluginId() {
      return PLUGIN_ID;
   }

   public static BundleContext getBundleContext() {
      return getDefault().getBundle().getBundleContext();
   }

   /**
    * @since 5.0
    */
   static public void log(String msg) {
      log(msg, null);
   }

   /**
    * @since 5.0
    */
   static public void log(String msg, Exception e) {
      getDefault().getLog().log(new Status(Status.INFO, PLUGIN_ID, Status.OK, msg, e));
   }

   static public void error(String msg, Exception e) {
      getDefault().getLog().log(new Status(Status.ERROR, PLUGIN_ID, Status.ERROR, msg, e));
   }

}
