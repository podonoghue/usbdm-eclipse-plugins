package net.sourceforge.usbdm.gdb;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.sourceforge.usbdm.gdb"; //$NON-NLS-1$

   // The shared instance
   private static Activator plugin;

   /**
    * The constructor
    */
   public Activator() {
//      System.err.println("USBDM::Activator() - Ver 4.10.0");
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
//      System.err.println("USBDM::Activator::start() - "
//            + getDefault().getBundle().getSymbolicName() + " : "
//            + getDefault().getBundle().getVersion());
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
    * )
    */
   public void stop(BundleContext context) throws Exception {
      System.err.println("USBDM::Activator::stop()");
      plugin = null;
      super.stop(context);
   }

   /**
    * Returns the shared instance
    * 
    * @return the shared instance
    */
   public static Activator getDefault() {
      // System.err.println("USBDM::Activator::getDefault()");
      return plugin;
   }

   /**
    * Returns an image descriptor for the image file at the given plug-in
    * relative path
    * 
    * @param path
    *           the path
    * @return the image descriptor
    */
   public static ImageDescriptor getImageDescriptor(String path) {
//      System.err.println("USBDM::Activator::getImageDescriptor(" + path + ")");
      return imageDescriptorFromPlugin(PLUGIN_ID, path);
   }
   
   public static String getPluginId() {
      return PLUGIN_ID;
   }
   
   /**
    * Logs the specified status with this plug-in's log.
    * 
    * @param status status to log
    */
   public static void log(IStatus status) {
      getDefault().getLog().log(status);
   }
   /**
    * @since 7.0
    */
   public static void log(Throwable t) {
      getDefault().getLog().log(new Status(Status.ERROR, PLUGIN_ID, t.getMessage(), t));
   }


}
