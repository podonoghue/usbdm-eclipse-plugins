package net.sourceforge.usbdm.cdt.ui.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import net.sourceforge.usbdm.packageParser.ISubstitutionMap;

public interface CustomAction {

   /**
    * 
    * @param wizard 
    * @param projectHandle    Handle of the project to modify
    * @param variableMap      Map of variables from project creation
    * @param progressMonitor  Progress monitor
    * @param parameters       Custom parameters
    * @return                 success/failure
    */ 
   public boolean action(
         IProject              projectHandle, 
         ISubstitutionMap   variableMap, 
         IProgressMonitor      progressMonitor, 
         String[]              parameters);
}
