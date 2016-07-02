package net.sourceforge.usbdm.gdb.ui;

import org.eclipse.cdt.launch.ui.CMainTab;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * @since 4.12
 */
public class UsbdmCMainTab extends CMainTab implements ILaunchConfigurationTab {
   
   static final String TAB_ID = "net.sourceforge.usbdm.gdb.ui.usbdmCMainTab";

   @Override
   public void setDefaults(ILaunchConfigurationWorkingCopy config) {
      super.setDefaults(config);
//      System.err.println("UsbdmCMainTab.setDefaults() Setting PREFERRED_DEBUG_REMOTE_LAUNCH_DELEGATE revised");
   }

   @Override
   public String getId() {
      return TAB_ID;
   }   
   
}
