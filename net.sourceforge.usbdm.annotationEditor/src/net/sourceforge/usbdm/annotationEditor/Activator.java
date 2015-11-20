package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.core.runtime.CoreException;
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
	public static final String PLUGIN_ID = "net.sourceforge.usbdm.annotationEditor"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		System.err.println("net.sourceforge.usbdm.annotationEditor.Activator.stop()");
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
   
   public ImageDescriptor getImageDescriptor(String key) {
      return getImageRegistry().getDescriptor(key);
   }

}
