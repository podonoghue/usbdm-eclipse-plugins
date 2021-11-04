package net.sourceforge.usbdm.annotationEditor;

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
    * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
    */
   public void start(BundleContext context) throws Exception {
      super.start(context);
      plugin = this;
      log(String.format("[%s, %s].start()", getBundle().getSymbolicName(), getBundle().getVersion()));
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
    */
   public void stop(BundleContext context) throws Exception {
      plugin = null;
      log(String.format("[%s, %s].stop()", getBundle().getSymbolicName(), getBundle().getVersion()));
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
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/checkbox-checked.png"), null));
      registry.put(ID_CHECKBOX_CHECKED_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/checkbox-unchecked.png"), null));
      registry.put(ID_CHECKBOX_UNCHECKED_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/error.png"), null));
      registry.put(ID_INVALID_NODE_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/warning.png"), null));
      registry.put(ID_WARNING_NODE_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/padlock.png"), null));
      registry.put(ID_LOCKED_NODE_IMAGE, imageDescriptor);
   }

   public final static String ID_CHECKBOX_CHECKED_IMAGE    = "checkbox-checked";
   public final static String ID_CHECKBOX_UNCHECKED_IMAGE  = "checkbox-unchecked";
   public final static String ID_INVALID_NODE_IMAGE        = "error";
   public final static String ID_WARNING_NODE_IMAGE        = "warning";
   public final static String ID_LOCKED_NODE_IMAGE         = "locked";

   /**
    * Returns an image descriptor based on an key
    *
    * @param key Key to lookup image 
    * 
    * @return The image descriptor
    */
   public static ImageDescriptor getImageDescriptor(String key) {
      return getDefault().getImageRegistry().getDescriptor(key);
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
