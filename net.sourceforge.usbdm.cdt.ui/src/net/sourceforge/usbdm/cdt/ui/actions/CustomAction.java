package net.sourceforge.usbdm.cdt.ui.actions;

import java.util.Map;

import net.sourceforge.usbdm.deviceDatabase.Device;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

public interface CustomAction {

   /**
    * 
    * @param projectHandle    Handle of the project to modify
    * @param device           Name of the device (usbdm style)
    * @param variableMap      Map of variables from project creation
    * @param progressMonitor  Progress monitor
    * @param parameters       Custom parameters
    * @return                 success/failure
    */
   public boolean action(
         IProject             projectHandle, 
         Device               device, 
         Map<String, String>  variableMap, 
         IProgressMonitor     progressMonitor, 
         String[]             parameters);
}
