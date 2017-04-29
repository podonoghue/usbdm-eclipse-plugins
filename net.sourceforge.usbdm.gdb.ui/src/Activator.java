import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * @since 4.12
 */
public class Activator extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.sourceforge.usbdm.gdb.ui"; //$NON-NLS-1$

   // The shared instance
   private static Activator plugin;

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

   /**
    * Returns an image descriptor based on an key
    *
    * @param key Key to lookup image 
    * 
    * @return The image descriptor
    */
   public ImageDescriptor getImageDescriptor(String key) {
      return getImageRegistry().getDescriptor(key);
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
