package net.sourceforge.usbdm.constants;

import net.sourceforge.usbdm.jni.Usbdm;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.sourceforge.usbdm.constants"; //$NON-NLS-1$

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
      try {
         loadUsbdmPaths();
      } catch (Exception e) {
         e.printStackTrace();
      }
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

   protected void loadUsbdmPaths() {
      String appPathName = UsbdmSharedConstants.USBDM_APPLICATION_PATH_VAR;
      String resPathName = UsbdmSharedConstants.USBDM_RESOURCE_PATH_VAR;
      String kdsPathName = UsbdmSharedConstants.USBDM_KSDK_PATH;

      System.err.println("loadUsbdmPaths()");
      IWorkspace workspace = ResourcesPlugin.getWorkspace();

      UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();
      IPath kdsPath                = new Path(settings.get(kdsPathName, "Not Set"));

      IPath usbdmApplicationPath = Usbdm.getApplicationPath();
      IPath usbdmResourcePath    = Usbdm.getResourcePath();

      IPathVariableManager pathMan = workspace.getPathVariableManager();

      if (!pathMan.validateValue(usbdmApplicationPath).isOK()) {
         usbdmApplicationPath = new Path("USBDM APPLICATION PATH NOT FOUND");
         System.err.println("loadUsbdmPath() - setting USBDM Application path variable");
      }
      if (!pathMan.validateValue(usbdmResourcePath).isOK()) {
         usbdmResourcePath = new Path("USBDM RESOURCE PATH NOT FOUND");
         System.err.println("loadUsbdmPath() - setting USBDM Resource path variable");
      }
      if (!pathMan.validateValue(kdsPath).isOK()) {
         kdsPath = new Path("");
         System.err.println("loadUsbdmPath() - KDS path is invalid or not set = " + kdsPath);
      }
      if (pathMan.validateValue(usbdmApplicationPath).isOK()) {
         try {
            pathMan.setURIValue(appPathName, usbdmApplicationPath.toFile().toURI());
         } catch (Exception e) {
            System.err.println("loadUsbdmPath() - Failed to set USBDM path variables, Exception = "+e.getMessage());
         }
      } else {
         System.err.println("loadUsbdmPath() - Failed to set USBDM path variables");
      }   
      if (pathMan.validateValue(usbdmResourcePath).isOK()) {
         try {
            pathMan.setURIValue(resPathName, usbdmResourcePath.toFile().toURI());
         } catch (Exception e) {
            System.err.println("loadUsbdmPath() - Failed to set USBDM path variables, Exception = "+e.getMessage());
         }
      } else {
         System.err.println("loadUsbdmPath() - Failed to set USBDM path variables");
      }   
      if (pathMan.validateValue(kdsPath).isOK()) {
         try {
            pathMan.setURIValue(kdsPathName, kdsPath.toFile().toURI());
         } catch (Exception e) {
            System.err.println("loadUsbdmPath() - Failed to set kdsPath path variables, Exception = "+e.getMessage());
         }
      } else {
         System.err.println("loadUsbdmPath() - Failed to set kdsPath path variables");
      }   
      //      System.err.println("loadUsbdmPath() - Path names =================================");
      //      String[] names = pathMan.getPathVariableNames();
      //      for (String s:names) {
      //         System.err.println(String.format("loadUsbdmPath() \'%s\' => \'%s\'", s, pathMan.getURIValue(s)));
      //      }
      //      System.err.println("loadUsbdmPath() - ============================================");
   }
   @Override
   protected void initializeImageRegistry(ImageRegistry registry) {
      super.initializeImageRegistry(registry);
      Bundle bundle = Platform.getBundle(PLUGIN_ID);

      ImageDescriptor imageDescriptor;
      imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/usbdm.png"), null));
      registry.put(ID_USB_ICON_IMAGE, imageDescriptor);
   }

   public final static String ID_USB_ICON_IMAGE    = "usbdm-icon";

   /**
    * Returns an image descriptor based on an key
    *
    * @param key Key to lookup image 
    * 
    * @return The image descriptor or null if unavailable
    */
   public static ImageDescriptor getImageDescriptor(String key) {
      if (getDefault() == null) {
         return null;
      }
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

   /**
    * @since 5.0
    */
   static public void log(String msg, Exception e) {
      if (getDefault() == null) {
         System.out.println(msg + ((e!=null)?e.getMessage():""));
         return;
      }
      getDefault().getLog().log(new Status(Status.INFO, PLUGIN_ID, Status.OK, msg, e));
   }

   static public void error(String msg, Exception e) {
      if (getDefault() == null) {
         System.err.println(msg + ((e!=null)?e.getMessage():""));
         return;
      }
      getDefault().getLog().log(new Status(Status.ERROR, PLUGIN_ID, Status.ERROR, msg, e));
   }

}
