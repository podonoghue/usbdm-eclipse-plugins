package net.sourceforge.usbdm.packageParser;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

public interface CustomAction {

   /**
    * 
    * @param projectHandle    Handle of the project to modify
    * @param symbolMap      Map of variables from project creation
    * @param progressMonitor  Progress monitor
    * @param parameters       Custom parameters
    * 
    * @return                 success/failure
    */ 
   public boolean action(
         IProject              projectHandle, 
         ISubstitutionMap      symbolMap, 
         IProgressMonitor      progressMonitor, 
         Object[]              parameters);
}
