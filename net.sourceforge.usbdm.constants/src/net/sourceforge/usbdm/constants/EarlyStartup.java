package net.sourceforge.usbdm.constants;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.jface.preference.IPreferenceStore;

public class EarlyStartup implements org.eclipse.ui.IStartup {

   @Override
   public void earlyStartup() {
      clearLaunchConfigFilter();
   }

   /**
    * Clears any launch filter applied to USBDM
    */
   private void clearLaunchConfigFilter() {
      System.err.println("net.sourceforge.usbdm.constants.EarlyStartup");
     try
     {
       IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
//       System.err.println("net.sourceforge.usbdm.constants.EarlyStartup store ="+store);
       String filteredLaunchers = store.getString("org.eclipse.debug.ui.PREF_FILTER_TYPE_LIST");
//       System.err.println("net.sourceforge.usbdm.constants.EarlyStartup filteredLaunchers="+filteredLaunchers);
       if (filteredLaunchers != null) {
          final String launchName = "net\\.sourceforge\\.usbdm.*";
//          System.err.println("net.sourceforge.usbdm.constants.EarlyStartup modifying");
          filteredLaunchers = filteredLaunchers.replaceAll(launchName+",", "");
          filteredLaunchers = filteredLaunchers.replaceAll(","+launchName+"$", "");
          store.setValue(IInternalDebugUIConstants.PREF_FILTER_TYPE_LIST, filteredLaunchers);
//          System.err.println("net.sourceforge.usbdm.constants.EarlyStartup filteredLaunchers="+filteredLaunchers);
       }
     }
     catch (Throwable e)
     {
     }
   }

}
