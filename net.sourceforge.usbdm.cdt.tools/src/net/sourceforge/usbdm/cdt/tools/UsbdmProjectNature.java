package net.sourceforge.usbdm.cdt.tools;


import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class UsbdmProjectNature implements IProjectNature {

   public static String C_NATURE_ID  = Activator.PLUGIN_ID + ".cnature";
   public static String CC_NATURE_ID = Activator.PLUGIN_ID + ".ccnature";

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

   /**
    * Add USBDM C nature to project
    * 
    * @param project          Project to be modified
    * @param mon
    * 
    * @throws CoreException
    */
   public static void addCNature(IProject project, IProgressMonitor mon) throws CoreException {
      CProjectNature.addNature(project, C_NATURE_ID, mon);
   }

   /**
    * Remove USBDM C nature to project
    * 
    * @param project          Project to be modified
    * @param mon
    * 
    * @throws CoreException
    */
   public static void removeCNature(IProject project, IProgressMonitor mon) throws CoreException {
      CProjectNature.removeNature(project, C_NATURE_ID, mon);
   }

   /**
    * Add USBDM C++ nature to project
    * 
    * @param project          Project to be modified
    * @param mon
    * 
    * @throws CoreException
    */
   public static void addCCNature(IProject project, IProgressMonitor mon) throws CoreException {
      CProjectNature.addNature(project, CC_NATURE_ID, mon);
   }

   /**
    * Remove USBDM C++ nature to project
    * 
    * @param project          Project to be modified
    * @param mon
    * 
    * @throws CoreException
    */
   public static void removeCCNature(IProject project, IProgressMonitor mon) throws CoreException {
      CProjectNature.removeNature(project, CC_NATURE_ID, mon);
   }

}
