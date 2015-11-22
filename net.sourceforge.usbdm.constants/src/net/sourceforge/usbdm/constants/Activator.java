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
      System.err.println(String.format("[%s, %s].start()", getBundle().getSymbolicName(), getBundle().getVersion()));
		try {
         loadUsbdmPaths();
      } catch (Exception e) {
         e.printStackTrace();
      }
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
   
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
      System.err.println(String.format("[%s, %s].stop()", getBundle().getSymbolicName(), getBundle().getVersion()));
		super.stop(bundleContext);
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
