package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class UsbdmProjectNature implements IProjectNature {
   public static final String USBDM_NATURE_ID = "net.sourceforge.usbdm.cdt.usbdmNature";

   private IProject fProject = null;
   
   @Override
   public void configure() throws CoreException {
   }

   @Override
   public void deconfigure() throws CoreException {
   }

   @Override
   public IProject getProject() {
      return fProject;
   }

   @Override
   public void setProject(IProject project) {
     fProject = project;
   }
   
   public static void addNature(IProject project, IProgressMonitor monitor) throws CoreException {
      CProjectNature.addNature(project, UsbdmProjectNature.USBDM_NATURE_ID, monitor);
   }
   
   public static void removeNature(IProject project, IProgressMonitor monitor) throws CoreException {
      CProjectNature.removeNature(project, UsbdmProjectNature.USBDM_NATURE_ID, monitor);
   }
   
}
