package net.sourceforge.usbdm.cdt.tools;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class ProjectNature implements IProjectNature {

   private IProject project;

   public void configure() throws CoreException {
      // Add nature-specific information
      // for the project, such as adding a builder
      // to a project's build specification.
   }
   
   public void deconfigure() throws CoreException {
      // Remove the nature-specific information here.
   }
   
   public IProject getProject() {
      return project;
   }
   
   public void setProject(IProject value) {
      project = value;
   }
}
