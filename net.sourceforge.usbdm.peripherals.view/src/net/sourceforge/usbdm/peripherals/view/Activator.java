package net.sourceforge.usbdm.peripherals.view;

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
	public static final String PLUGIN_ID = "net.sourceforge.usbdm.peripherals.view"; //$NON-NLS-1$

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
      System.err.println("net.sourceforge.usbdm.peripherals.view.Activator.stop()");
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
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/peripheral.png"), null));
       registry.put(ID_PERIPHERAL_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/registerReadOnly.png"), null));
       registry.put(ID_REGISTER_READ_ONLY_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/registerReadWrite.png"), null));
       registry.put(ID_REGISTER_READ_WRITE_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/fieldReadOnly.png"), null));
       registry.put(ID_FIELD_READ_ONLY_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/fieldReadWrite.png"), null));
       registry.put(ID_FIELD_READ_WRITE_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/cog.png"), null));
       registry.put(ID_COG_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/funnel.png"), null));
       registry.put(ID_FILTER_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/refresh.png"), null));
       registry.put(ID_REFRESH_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/refreshSelection.png"), null));
       registry.put(ID_REFRESH_SELECTION_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/reset.png"), null));
       registry.put(ID_RESET_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/showColumn.png"), null));
       registry.put(ID_SHOW_COLUMN_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/usbdm.png"), null));
       registry.put(ID_USBDM_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/bug.png"), null));
       registry.put(ID_BUG_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/checkbox-checked.png"), null));
       registry.put(ID_CHECKBOX_CHECKED_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/checkbox-unchecked.png"), null));
       registry.put(ID_CHECKBOX_UNCHECKED_IMAGE, imageDescriptor);
       imageDescriptor = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/exception.png"), null));
       registry.put(ID_EXCEPTION_IMAGE, imageDescriptor);
   }

   public final static String ID_PERIPHERAL_IMAGE             = "peripheral-image";
   public final static String ID_REGISTER_READ_ONLY_IMAGE     = "register-read-only-image";
   public final static String ID_REGISTER_READ_WRITE_IMAGE    = "register-read-write-image";
   public final static String ID_FIELD_READ_ONLY_IMAGE        = "read-only-image";
   public final static String ID_FIELD_READ_WRITE_IMAGE       = "read-write-image";
   public final static String ID_COG_IMAGE                    = "cog-image";
   public final static String ID_FILTER_IMAGE                 = "funnel-image";
   public final static String ID_REFRESH_IMAGE                = "refresh-image";
   public final static String ID_REFRESH_SELECTION_IMAGE      = "refresh-selection-image";
   public final static String ID_RESET_IMAGE                  = "reset-image";
   public final static String ID_SHOW_COLUMN_IMAGE            = "show-column-image";
   public final static String ID_USBDM_IMAGE                  = "usbdm-image";
   public final static String ID_BUG_IMAGE                    = "bug-image";
   public final static String ID_CHECKBOX_CHECKED_IMAGE       = "checkbox-checked";
   public final static String ID_CHECKBOX_UNCHECKED_IMAGE     = "checkbox-unchecked";
   public final static String ID_EXCEPTION_IMAGE              = "exception";
	
   public ImageDescriptor getImageDescriptor(String key) {
	   return getImageRegistry().getDescriptor(key);
	}
   
   public static BundleContext getBundleContext() {
      return getDefault().getBundle().getBundleContext();
   }

}
