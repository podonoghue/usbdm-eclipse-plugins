package net.sourceforge.usbdm.constants;

import net.sourceforge.usbdm.jni.Usbdm;

import org.osgi.framework.BundleContext;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class Activator extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.sourceforge.usbdm.constants"; //$NON-NLS-1$

	public static String getPluginId() {
      return PLUGIN_ID;
   }

   private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		try {
         loadUsbdmPaths();
      } catch (Exception e) {
         e.printStackTrace();
      }
	}

   protected void loadUsbdmPaths() {
      String appPathName = UsbdmSharedConstants.USBDM_APPLICATION_PATH_VAR;
      String resPathName = UsbdmSharedConstants.USBDM_RESOURCE_PATH_VAR;

      System.err.println("loadUsbdmPaths()");
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
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
      if (pathMan.validateValue(usbdmApplicationPath).isOK() && 
          pathMan.validateValue(usbdmResourcePath).isOK()) {
         try {
            pathMan.setURIValue(appPathName, usbdmApplicationPath.toFile().toURI());
            pathMan.setURIValue(resPathName, usbdmResourcePath.toFile().toURI());
//            System.err.println("loadUsbdmPath() - paths loaded");
         } catch (Exception e) {
            System.err.println("loadUsbdmPath() - Failed to set USBDM path variables, Exception = "+e.getMessage());
         }
      } else {
         System.err.println("loadUsbdmPath() - Failed to set USBDM path variables");
      }   
   }
   
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

	/**
    * Returns an image descriptor for the image file at the given
    * plug-in relative path
    *
    * @param path the path
    * @return the image descriptor
    */
   public static ImageDescriptor getImageDescriptor(String path) {
      ImageDescriptor imageDescriptor = null;
      if (getContext() != null) {
         imageDescriptor = Activator.imageDescriptorFromPlugin(getPluginId(), path);
      }
      else {
         imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
      }
      return imageDescriptor;
   }

	
}
