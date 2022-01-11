package net.sourceforge.usbdm.cdt.utilties;
/**
 * 
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;

import net.sourceforge.usbdm.packageParser.DeleteResourceAction;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;

/**
 * @author pgo
 *
 */
public class DeleteResource {

   /**
    * Process action
    * 
    * @param projectHandle       Handle for access to project
    * @param symbolMap         
    * @param deleteAction
    * @param monitor             Progress monitor
    * 
    * @throws Exception 
    */
   public void process(IProject projectHandle, ISubstitutionMap symbolMap, DeleteResourceAction deleteAction, IProgressMonitor monitor) 
      throws Exception {
      if (projectHandle == null) {
         // For debug
         System.err.println("Debug: "+deleteAction);
         return;
      }
      String root   = symbolMap.substitute(deleteAction.getRoot());
      String target = symbolMap.substitute(deleteAction.getTarget());
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
         int retryCounter = 4;
         while (--retryCounter>=0) {
            if (!iFolder.exists()) {
               // Ignore if folder no longer exists
               break;
            }
            try {
               iFolder.delete(true, monitor);
               break;
            } catch (Exception e) {
               final String errorMsg = "Failed to delete resource "+targetPath.toAbsolutePath();
               if (retryCounter == 0) {
                  throw(new Exception(errorMsg,e));
               }
               System.err.println(errorMsg+", retrying");
               Thread.sleep(100 /*ms*/);
            }               
         }
      }
   }
   
}
