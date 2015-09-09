package net.sourceforge.usbdm.gdb.ui;

import java.util.HashSet;

import org.eclipse.cdt.launch.ui.CMainTab;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * @since 4.12
 */
public class UsbdmCMainTab extends CMainTab implements ILaunchConfigurationTab {
   
   static final String TAB_ID = "net.sourceforge.usbdm.gdb.ui.usbdmCMainTab";

   /* (non-Javadoc)
    * @see org.eclipse.cdt.launch.ui.CMainTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
    */
   @Override
   public void setDefaults(ILaunchConfigurationWorkingCopy config) {
      super.setDefaults(config);
//      System.err.println("UsbdmCMainTab.setDefaults() Setting PREFERRED_DEBUG_REMOTE_LAUNCH_DELEGATE revised");
      
      HashSet<String> attributes = null;
      attributes = new HashSet<String>();
      attributes.add(org.eclipse.debug.core.ILaunchManager.DEBUG_MODE);
      config.setPreferredLaunchDelegate(attributes, "net.sourceforge.usbdm.gdb.dsfLaunchDelegate");
      attributes = new HashSet<String>();
      attributes.add(org.eclipse.debug.core.ILaunchManager.RUN_MODE);
      config.setPreferredLaunchDelegate(attributes, "net.sourceforge.usbdm.gdb.dsfRunLaunchDelegate");
   }

   @Override
   public String getId() {
      return TAB_ID;
   }   
   
}
