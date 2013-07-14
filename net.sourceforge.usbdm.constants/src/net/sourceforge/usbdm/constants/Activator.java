package net.sourceforge.usbdm.constants;

import org.osgi.framework.BundleContext;
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
