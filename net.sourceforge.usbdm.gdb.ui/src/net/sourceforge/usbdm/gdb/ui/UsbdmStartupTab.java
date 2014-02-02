/*******************************************************************************
 * Copyright (c) 2013 Peter O'Donoghue.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
/**
 * @author Peter O'Donoghue
 * 
 */
package net.sourceforge.usbdm.gdb.ui;

import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.debug.gdbjtag.ui.GDBJtagStartupTab;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

public class UsbdmStartupTab extends GDBJtagStartupTab {

   static final String TAB_ID = "net.sourceforge.usbdm.gdb.ui.usbdmStartupTab";

   @Override
   public String getId() {
      return TAB_ID;
   }   

   @Override
   public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	   
	  // Inherit some defaults 
      super.setDefaults(configuration);
      
      // Change defaults for USBDM
      
      // Initialization Commands
      configuration.setAttribute(IGDBJtagConstants.ATTR_DO_RESET, false);
      configuration.setAttribute(IGDBJtagConstants.ATTR_DELAY,    1);
      configuration.setAttribute(IGDBJtagConstants.ATTR_DO_HALT,  false);
      
      // Runtime Options
      configuration.setAttribute(IGDBJtagConstants.ATTR_SET_STOP_AT, true);
      configuration.setAttribute(IGDBJtagConstants.ATTR_STOP_AT,     "main");
      configuration.setAttribute(IGDBJtagConstants.ATTR_SET_RESUME,  true);
   }
}
