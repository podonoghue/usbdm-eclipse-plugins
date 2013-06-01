package net.sourceforge.usbdm.cdt.wizard;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "USBDM CDT Plugin"; //$NON-NLS-1$

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

   /**
    * Returns the default console
    *
    * @return the default console
    */
   public MessageConsole getDefaultConsole() {
      return getConsole("ARM Eclipse Plugin Log");
    }

   public MessageConsole getConsole(String sName)
   {
     IConsoleManager oConMan = ConsolePlugin.getDefault().getConsoleManager();
     IConsole[] aoConsoles = (IConsole[]) oConMan.getConsoles();
     for (IConsole oConsole : aoConsoles) {
       if (oConsole.getName().equals(sName)) {
         return (MessageConsole)oConsole;
       }
     }

     MessageConsole oNewConsole = new MessageConsole(sName, null);
     oConMan.addConsoles(new IConsole[] { oNewConsole });
     return oNewConsole;
   }
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
