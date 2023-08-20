package net.sourceforge.usbdm.deviceEditor;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
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
   public static final String PLUGIN_ID = "net.sourceforge.usbdm.deviceEditor"; //$NON-NLS-1$

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
   @Override
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
   @Override
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
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/empty.png"), null));
      registry.put(ID_EMPTY_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/generateFiles.png"), null));
      registry.put(ID_GEN_FILES_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/generateFiles-disabled.png"), null));
      registry.put(ID_GEN_FILES_DISABLED_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/disabled.png"), null));
      registry.put(ID_DISABLED_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/cog.png"), null));
      registry.put(ID_COG_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/checkbox-checked.png"), null));
      registry.put(ID_CHECKBOX_CHECKED_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/checkbox-unchecked.png"), null));
      registry.put(ID_CHECKBOX_UNCHECKED_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/checkbox-greyed.png"), null));
      registry.put(ID_CHECKBOX_GREYED_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/error.png"), null));
      registry.put(ID_ERROR_NODE_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/warning.png"), null));
      registry.put(ID_WARNING_NODE_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/padlock.png"), null));
      registry.put(ID_LOCKED_NODE_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/peripheral.png"), null));
      registry.put(ID_PERIPHERAL_IMAGE, imageDescriptor);

      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/Arrow_Up.png"), null));
      registry.put(ID_UP_ARROW_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/Arrow_Down.png"), null));
      registry.put(ID_DOWN_ARROW_IMAGE, imageDescriptor);
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/Arrow_UpDown.png"), null));
      registry.put(ID_UP_DOWN_ARROW_IMAGE, imageDescriptor);

   }

   public final static String ID_EMPTY_IMAGE               = "empty";
   public final static String ID_GEN_FILES_IMAGE           = "generate-files";
   public final static String ID_GEN_FILES_DISABLED_IMAGE  = "generate-files-disabled";
   public final static String ID_COG_IMAGE                 = "cog";
   public final static String ID_DISABLED_IMAGE            = "disabled";
   public final static String ID_CHECKBOX_CHECKED_IMAGE    = "checkbox-checked";
   public final static String ID_CHECKBOX_UNCHECKED_IMAGE  = "checkbox-unchecked";
   public final static String ID_CHECKBOX_GREYED_IMAGE     = "checkbox-greyed";
   public final static String ID_ERROR_NODE_IMAGE          = "error";
   public final static String ID_WARNING_NODE_IMAGE        = "warning";
   public final static String ID_LOCKED_NODE_IMAGE         = "locked";
   public final static String ID_PERIPHERAL_IMAGE          = "peripheral-image";
   public final static String ID_UP_ARROW_IMAGE            = "up-arrow";
   public final static String ID_DOWN_ARROW_IMAGE          = "down-arrow";
   public final static String ID_UP_DOWN_ARROW_IMAGE       = "up-down-arrow";

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

   /**
    * Get Bundle context
    * 
    * @return The bundle context or null if unavailable
    */
   public static BundleContext getBundleContext() {
      if (getDefault() == null) {
         return null;
      }
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
         getDefault().getLog().error(msg, e);
         return;
      }
      getDefault().getLog().info(msg);
   }

   static public void logError(String msg, Exception e) {
      if (getDefault() == null) {
         if (e != null) {
            e.printStackTrace();
         }
         getDefault().getLog().error(msg, e);
         return;
      }
      getDefault().getLog().error(msg);
   }

}
