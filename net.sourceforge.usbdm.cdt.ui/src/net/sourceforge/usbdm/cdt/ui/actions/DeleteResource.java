package net.sourceforge.usbdm.cdt.ui.actions;
/**
 * 
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import net.sourceforge.usbdm.cdt.ui.newProjectWizard.MacroSubstitute;
import net.sourceforge.usbdm.deviceDatabase.DeleteResourceAction;
import net.sourceforge.usbdm.deviceDatabase.Device;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;

/**
 * @author pgo
 *
 */
public class DeleteResource {

   /**
    * Process action
    * 
    * @param projectHandle       Handle for access to project
    * @param device              Device
    * @param variableMap         
    * @param resourceInfo
    * @param monitor             Progress monitor
    * 
    * @throws Exception 
    */
   public void process(IProject projectHandle, Device device, Map<String,String> variableMap, DeleteResourceAction resourceInfo, IProgressMonitor monitor) 
      throws Exception {
      
      String root   = MacroSubstitute.substitute(resourceInfo.getRoot(),   variableMap);
      String target = MacroSubstitute.substitute(resourceInfo.getTarget(), variableMap);
      root   = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(root);
      target = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(target);
      Path targetPath = Paths.get(target);

//      System.err.println("DeleteResource.processFile(): target: \'" + targetPath.toString() + "\'");
      IFile iFile = projectHandle.getFile(targetPath.toString());
      if (iFile.exists()) {
         iFile.delete(true, monitor);               
      }
   }
   
}
