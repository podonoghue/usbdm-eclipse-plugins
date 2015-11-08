package net.sourceforge.usbdm.cdt.ui.actions;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;

import net.sourceforge.usbdm.deviceDatabase.Device;

public interface CustomAction {

   /**
    * 
    * @param wizard 
    * @param projectHandle    Handle of the project to modify
    * @param device           Name of the device (usbdm style)
    * @param variableMap      Map of variables from project creation
    * @param progressMonitor  Progress monitor
    * @param parameters       Custom parameters
    * @return                 success/failure
    */ 
   public boolean action(
         Wizard                wizard, 
         IProject              projectHandle, 
         Device                device, 
         Map<String, String>   variableMap, 
         IProgressMonitor      progressMonitor, 
         String[]              parameters);
}
