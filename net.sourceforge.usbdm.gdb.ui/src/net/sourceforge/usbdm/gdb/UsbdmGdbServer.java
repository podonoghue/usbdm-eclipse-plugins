package net.sourceforge.usbdm.gdb;

import org.eclipse.cdt.utils.Platform;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * @since 4.10
 */
public class UsbdmGdbServer extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.sourceforge.usbdm.gdb.ui"; //$NON-NLS-1$

   // The shared instance
   private static UsbdmGdbServer plugin;

   /**
    * The constructor
    */
   public UsbdmGdbServer() {
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
//      System.err.println("USBDM::Activator::stop()");
      plugin = null;
      super.stop(context);
   }

   /**
    * Returns the shared instance
    * 
    * @return the shared instance
    */
   public static UsbdmGdbServer getDefault() {
      // System.err.println("USBDM::Activator::getDefault()");
      return plugin;
   }


   @Override
   protected void initializeImageRegistry(ImageRegistry registry) {
       super.initializeImageRegistry(registry);
       Bundle bundle = Platform.getBundle(PLUGIN_ID);
       
       ImageDescriptor imageDescriptor;
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/cog.png"), null));
       registry.put(ID_COG_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/bug.png"), null));
       registry.put(ID_BUG_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/arrow.png"), null));
       registry.put(ID_ARROW_IMAGE, imageDescriptor);
   }

   public final static String ID_COG_IMAGE                    = "cog-image";
   public final static String ID_BUG_IMAGE                    = "bug-image";
   public final static String ID_ARROW_IMAGE                  = "arrow-image";
   
   public ImageDescriptor getImageDescriptor(String key) {
      return getImageRegistry().getDescriptor(key);
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

   public static BundleContext getBundleContext() {
      return getDefault().getBundle().getBundleContext();
   }
}
