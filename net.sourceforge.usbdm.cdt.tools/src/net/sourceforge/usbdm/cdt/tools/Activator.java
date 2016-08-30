package net.sourceforge.usbdm.cdt.tools;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.sourceforge.usbdm.cdt.tools"; //$NON-NLS-1$

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

   /**
    * @since 5.0
    */
   static public void log(String msg, Exception e) {
      getDefault().getLog().log(new Status(Status.INFO, PLUGIN_ID, Status.OK, msg, e));
   }

   static public void error(String msg, Exception e) {
      getDefault().getLog().log(new Status(Status.ERROR, PLUGIN_ID, Status.ERROR, msg, e));
   }

}
