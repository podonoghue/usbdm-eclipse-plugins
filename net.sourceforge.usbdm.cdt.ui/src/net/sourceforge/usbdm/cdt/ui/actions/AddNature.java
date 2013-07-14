package net.sourceforge.usbdm.cdt.ui.actions;

import org.eclipse.cdt.core.templateengine.TemplateCore;
import org.eclipse.cdt.core.templateengine.process.ProcessArgument;
import org.eclipse.cdt.core.templateengine.process.ProcessFailureException;
import org.eclipse.cdt.core.templateengine.process.ProcessRunner;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Adds project settings from USBDM device database
 * 
 * @author PODonoghue
 *
 */
public class AddNature extends ProcessRunner {

   /* 
    * (non-Javadoc)
    * @see org.eclipse.cdt.core.templateengine.process.ProcessRunner#process(org.eclipse.cdt.core.templateengine.TemplateCore, org.eclipse.cdt.core.templateengine.process.ProcessArgument[], java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
    */
   @Override
   public void process(
         TemplateCore template, 
         ProcessArgument[] args,
         String processId, IProgressMonitor monitor)
         throws ProcessFailureException {

//      System.err.println("ProcessUsbdmSettings.process()"); //$NON-NLS-1$
      
      String projectName          = null;
      String nature               = null;

      for (ProcessArgument arg:args) {
         if (arg.getName().equals("projectName")) {
            projectName = arg.getSimpleValue();              // Name of project to get handle
         }
         else if (arg.getName().equals("nature")) {
            nature  = arg.getSimpleValue();                  // nature to add
         }
         else {
            throw new ProcessFailureException("AddNature.process() - Unexpected argument \'"+arg.getName()+"\'"); //$NON-NLS-1$
         }
      }
      if ((projectName == null) ) {
         throw new ProcessFailureException("AddNature.process() - Missing projectName argument"); //$NON-NLS-1$
      }     
      if ( (nature == null)) {
         throw new ProcessFailureException("AddNature.process() - Missing nature argument"); //$NON-NLS-1$
      }     
      IProject projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      
      try {
         IProjectDescription projectDescription = projectHandle.getDescription();
         String[] currentIds = projectDescription.getNatureIds();
         String[] newIds = new String[currentIds.length+1];
         int index;
         for (index = 0; index < currentIds.length; index++) {
            newIds[index] = currentIds[index];
//            System.err.println("AddNature.process() Copying nature : " + newIds[index]);
         }
         newIds[index] = nature;
//         System.err.println("AddNature.process() Adding nature : " + newIds[index]);

         projectDescription.setNatureIds(newIds);
         projectHandle.setDescription(projectDescription, monitor);
         ManagedBuildManager.saveBuildInfo(projectHandle, true);

      } catch (CoreException e) {
         e.printStackTrace();
      }
   }
}
