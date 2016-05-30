package net.sourceforge.usbdm.cdt.utilties;
/**
 * 
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;

import net.sourceforge.usbdm.packageParser.DeleteResourceAction;

/**
 * @author pgo
 *
 */
public class DeleteResource {

   /**
    * Process action
    * 
    * @param projectHandle       Handle for access to project
    * @param variableMap         
    * @param resourceInfo
    * @param monitor             Progress monitor
    * 
    * @throws Exception 
    */
   public void process(IProject projectHandle, Map<String,String> variableMap, DeleteResourceAction resourceInfo, IProgressMonitor monitor) 
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
      else {
         IFolder iFolder = projectHandle.getFolder(targetPath.toString());
         if (iFolder.exists()) {
            iFolder.delete(true, monitor);               
         }
      }
   }
   
}
