package net.sourceforge.usbdm.peripherals.atmel;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static BundleContext context;

   /**
    * Get bundle context for this plug-in
    * 
    * @return Bundle context
    */
	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		context = bundleContext;
      System.err.println(String.format("[%s].start()", "net.sourceforge.usbdm.peripherals.atmel.Activator"));
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		context = null;
      System.err.println(String.format("[%s].stop()", "net.sourceforge.usbdm.peripherals.atmel.Activator"));
	}

}
