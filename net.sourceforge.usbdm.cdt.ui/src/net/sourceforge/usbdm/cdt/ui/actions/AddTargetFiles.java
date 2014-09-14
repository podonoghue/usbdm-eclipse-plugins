package net.sourceforge.usbdm.cdt.ui.actions;
/**
 * 
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import net.sourceforge.usbdm.cdt.ui.newProjectWizard.MacroSubstitute;
import net.sourceforge.usbdm.cdt.ui.newProjectWizard.ProjectUtilities;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.Device.FileAction;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author pgo
 *
 */
public class AddTargetFiles {

   static String readFile(java.nio.file.Path path) throws IOException {
      byte[] encoded = Files.readAllBytes((java.nio.file.Path) path);
      return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
   }

   /**
    * @param path           path of source file
    * @param targetPath     path of target file
    * @param isReplaceable  whether to macro substitution
    * @param variableMap    macro values
    * @param projectHandle  handle for access to project
    * 
    * @throws Exception 
    */
   private void processFile(Path path, Path targetPath, Boolean isReplaceable, Map<String, String> variableMap, IProject projectHandle, IProgressMonitor monitor) throws Exception {
//      System.err.println("AddTargetFiles.processFile(): ");
//      System.err.println("\tsource: \'"+ path.toString() + "\'");
//      System.err.println("\ttarget: \'" + targetPath.toString() + "\'");

      InputStream contents = null;
      String fileContents;
      try {
         fileContents = readFile(path);
      } catch (IOException e) {
         throw new Exception("\"" + path + "\" failed read"); //$NON-NLS-1$ //$NON-NLS-2$
      }
      if (isReplaceable) {
         fileContents = MacroSubstitute.substitute(fileContents, variableMap);
      }
      contents = new ByteArrayInputStream(fileContents.getBytes());
      try {
         IFile iFile = projectHandle.getFile(targetPath.toString());
         if (!iFile.getParent().exists()) {
            ProjectUtilities.createFolder(projectHandle, iFile.getParent().getProjectRelativePath().toString(), monitor);
         }
         // Don't replace existing, more specific, file
         if (!iFile.exists()) {
            iFile.create(contents, true, null);
            iFile.refreshLocal(IResource.DEPTH_ONE, null);
            projectHandle.refreshLocal(IResource.DEPTH_INFINITE, null);
         }
      } catch (CoreException e) {
         throw new Exception("Failed" + e.getMessage(), e); //$NON-NLS-1$
      }
   }

   /**
    * 
    * @param path           path of source file or directory
    * @param targetPath     path of target file (if source is a directory then parent is used)
    * @param isReplaceable  whether to macro substitution
    * @param variableMap    template for macro values
    * @param projectHandle  handle for access to project
    * @param monitor
    * 
    * @throws Exception
    */
   private void processItem(Path path, Path targetPath, Boolean isReplaceable, Map<String, String> variableMap, IProject projectHandle, IProgressMonitor monitor) throws Exception {
      // Check if directory
//      System.err.println("AddTargetFiles.processItem() file  = " + path.toString());
//      System.err.println("AddTargetFiles.processItem() exists?  = " + Files.exists(path));
//      System.err.println("AddTargetFiles.processItem() directory?  = " + Files.isDirectory(path));
      if (Files.isDirectory(path)) {
         // Directory copy
         // Hack for case we expected a header file but given entire folder of header files
         if (targetPath.getFileName().endsWith(".h") || targetPath.getFileName().endsWith(".H")) {
            targetPath = targetPath.getParent();
         }
//         System.err.println("AddTargetFiles.processItem() folder  = " + path.toString());
         DirectoryStream<Path> stream = Files.newDirectoryStream(path);
         for (java.nio.file.Path entry: stream) {
            processItem(entry, targetPath.resolve(entry.getFileName()), isReplaceable, variableMap, projectHandle, monitor);
         }
      }
      else {
         // Simple file copy
         processFile(path, targetPath, isReplaceable, variableMap, projectHandle, monitor);
      }
   }
   
   private URI resolveURI(URI uri) throws URISyntaxException {
      
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IPathVariableManager pathMan = workspace.getPathVariableManager();
      return pathMan.resolveURI(uri);
   }
   
   public void process(IProject projectHandle, Device device, Map<String,String> variableMap, FileAction fileInfo, IProgressMonitor monitor) throws Exception {
      String root   = MacroSubstitute.substitute(fileInfo.getRoot(),   variableMap);
      String source = MacroSubstitute.substitute(fileInfo.getSource(), variableMap);
      String target = MacroSubstitute.substitute(fileInfo.getTarget(), variableMap);

      if (source.isEmpty()) {
         return;
      }
      URI sourceURI = null;
      if (root != null) {
         sourceURI = new URI(null, root, null).resolve(source);
      }
      else {
         sourceURI = new URI(null, source, null);
      }
//      System.err.println(String.format("process()\t sourceURI => \'%s\'", sourceURI));
//      System.err.println(String.format("process()\t sourceURI => \'%s\'", sourceURI));
      
//      System.err.println(String.format("AddTargetFiles.process() \'%s\' => \'%s\'", sourceURI.toString(), target));
      switch (fileInfo.getFileType()) {
      case LINK :
         createLink(/*Paths.get(sourceURI)*/sourceURI, target, projectHandle, monitor);
         break;
      case NORMAL :
         processItem(Paths.get(resolveURI(sourceURI)), Paths.get(target), fileInfo.isReplaceable(), variableMap, projectHandle, monitor);
         break;
      }
   }

   private void createLink(URI sourceURI, String target, IProject projectHandle, IProgressMonitor monitor) throws Exception {
      System.err.println(String.format("AddTargetFiles.createLink() \'%s\' => \'%s\'", sourceURI.toString(), target));
      IFile iFile = projectHandle.getFile(target);
      if (!iFile.getParent().exists()) {
         ProjectUtilities.createFolder(projectHandle, iFile.getParent().getProjectRelativePath().toString(), monitor);
      }
      iFile.createLink(sourceURI, IResource.ALLOW_MISSING_LOCAL, monitor);
   }

}
