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
import net.sourceforge.usbdm.deviceDatabase.FileAction;
import net.sourceforge.usbdm.jni.Usbdm;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;

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
    * @param path                path of source file
    * @param targetPath          path of target file
    * @param doMacroReplacement  whether to macro substitution
    * @param variableMap         macro values
    * @param projectHandle       handle for access to project
    * 
    * @throws Exception 
    */
   private void processFile(Path path, Path targetPath, boolean doMacroReplacement, boolean doReplacement, Map<String, String> variableMap, IProject projectHandle, IProgressMonitor monitor) throws Exception {
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
      if (doMacroReplacement) {
         fileContents = MacroSubstitute.substitute(fileContents, variableMap);
      }
      contents = new ByteArrayInputStream(fileContents.getBytes());
      try {
         IFile iFile = projectHandle.getFile(targetPath.toString());
         if (!iFile.getParent().exists()) {
            ProjectUtilities.createFolder(projectHandle, iFile.getParent().getProjectRelativePath().toString(), monitor);
         }
         // Replace existing, more specific, file
         if (iFile.exists()) {
            if (doReplacement) {
//             System.err.println("AddTargetFiles.processFile() - replacing " + iFile.toString());
               iFile.delete(true, monitor);               
            }
            else {
               throw new Exception("\"" + iFile.toString() + "\" already exists"); //$NON-NLS-1$ //$NON-NLS-2$
            }
         }
         iFile.create(contents, true, null);
         iFile.refreshLocal(IResource.DEPTH_ONE, null);
         projectHandle.refreshLocal(IResource.DEPTH_INFINITE, null);
      } catch (CoreException e) {
         throw new Exception("Failed" + e.getMessage(), e); //$NON-NLS-1$
      }
   }

   /**
    * 
    * @param path                path of source file or directory
    * @param targetPath          path of target file (if source is a directory then parent is used)
    * @param doMacroReplacement  whether to macro substitution
    * @param variableMap         template for macro values
    * @param projectHandle       handle for access to project
    * @param monitor
    * 
    * @throws Exception
    */
   private void processItem(Path path, Path targetPath, boolean doMacroReplacement, boolean doReplacement, Map<String, String> variableMap, IProject projectHandle, IProgressMonitor monitor) throws Exception {
//      System.err.println("AddTargetFiles.processItem() file  = " + path.toString());
//      System.err.println("AddTargetFiles.processItem() exists?  = " + Files.exists(path));
//      System.err.println("AddTargetFiles.processItem() directory?  = " + Files.isDirectory(path));
//      System.err.println("AddTargetFiles.processItem() targetPath = " + targetPath.toString());
//      System.err.println("AddTargetFiles.processItem() targetPath.getFileName = " + targetPath.getFileName().toString());
      // Check if directory
      if (Files.isDirectory(path)) {
         // Directory copy
         // Hack for when we expected a header file but were given entire folder of header files
         String filename = targetPath.getFileName().toString();
         if (filename.endsWith(".h") || filename.endsWith(".H")) {
            targetPath = targetPath.getParent();
//            System.err.println("AddTargetFiles.processItem() changed target to = " + targetPath.toAbsolutePath());
         }
//         System.err.println("AddTargetFiles.processItem() folder  = " + path.toString());
         DirectoryStream<Path> stream = Files.newDirectoryStream(path);
         for (java.nio.file.Path entry: stream) {
            processItem(entry, targetPath.resolve(entry.getFileName()), doMacroReplacement, doReplacement, variableMap, projectHandle, monitor);
         }
      }
      else {
         // Simple file copy
         processFile(path, targetPath, doMacroReplacement, doReplacement, variableMap, projectHandle, monitor);
      }
   }
//   
//   private URI resolveURI(URI uri) throws URISyntaxException {
//      
//      IWorkspace workspace = ResourcesPlugin.getWorkspace();
//      IPathVariableManager pathMan = workspace.getPathVariableManager();
//      return pathMan.resolveURI(uri);
//   }
   
   public void process(IProject projectHandle, Device device, Map<String,String> variableMap, FileAction fileInfo, IProgressMonitor monitor) throws Exception {
      String root   = MacroSubstitute.substitute(fileInfo.getRoot(),   variableMap);
      String source = MacroSubstitute.substitute(fileInfo.getSource(), variableMap);
      String target = MacroSubstitute.substitute(fileInfo.getTarget(), variableMap);
//      System.err.println("root   = \'" + root.toString() + "\'");
//      System.err.println("source = \'" + source.toString() + "\'");
//      System.err.println("target = \'" + target.toString() + "\'");
      if (source.isEmpty()) {
//         System.err.println("AddTargetFiles.process() - source is empty, fileInfo.getSource() = " + fileInfo.getSource());
         return;
      }
      root   = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(root);
      source = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(source);
      target = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(target);
      
      Path sourcePath = Paths.get(source);
      if (!sourcePath.isAbsolute()) {
         sourcePath = Paths.get(root, source);
      }
      Path targetPath = Paths.get(target);
//      System.err.println(String.format("AddTargetFiles.process() \'%s\' => \'%s\'", sourcePath, targetPath));

      
      switch (fileInfo.getFileType()) {
      case LINK :
         createLink(sourcePath, target, projectHandle, monitor);
         break;
      case NORMAL :
         processItem(sourcePath, targetPath, fileInfo.isDoMacroReplacement(), fileInfo.isDoReplace(), variableMap, projectHandle, monitor);
         break;
      }
   }

   /**
    * Create link to file.  Attempts to make it relative to 'usbdm_resource_path'
    * 
    * @param sourcePath
    * @param target
    * @param projectHandle
    * @param monitor
    * @throws Exception
    */
   private void createLink(Path sourcePath, String target, IProject projectHandle, IProgressMonitor monitor) throws Exception {
      Path usbdmResourcePath = Paths.get(Usbdm.getResourcePath().toString());

//      System.err.println("sourcePath = \'" + sourcePath.toString() + "\'");
//      System.err.println("usbdmResourcePath = \'" + usbdmResourcePath.toString() + "\'");

      // Convert to relative path using ${usbdm_resource_path} if possible
      if (sourcePath.startsWith(usbdmResourcePath)) {
         try {
            sourcePath = usbdmResourcePath.relativize(sourcePath);
//            System.err.println("relative sourcePath = \'" + sourcePath.toString() + "\'");
            sourcePath = Paths.get("usbdm_resource_path").resolve(sourcePath);
         } catch (IllegalArgumentException e) {
         }
      }
//      System.err.println("macroed sourcePath' = \'" + sourcePath.toString() + "\'");
      URI sourceURI = null;
      try {
         sourceURI = new URI(sourcePath.toString().replaceAll("\\\\", "/"));
      } catch (URISyntaxException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
//      System.err.println("relative sourceURI' = \'" + sourceURI.toString() + "\'");
      
      //      System.err.println(String.format("AddTargetFiles.createLink() \'%s\' => \'%s\'", sourceURI.toString(), target));
      IFile iFile = projectHandle.getFile(target);
      if (!iFile.getParent().exists()) {
         ProjectUtilities.createFolder(projectHandle, iFile.getParent().getProjectRelativePath().toString(), monitor);
      }
      iFile.createLink(sourceURI, IResource.ALLOW_MISSING_LOCAL, monitor);
   }

   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      Path sourcePath        = Paths.get("c:/Program File(x86)/pgo/USBDM 4.10.230/a/b/c/d/e/f/g");
      Path usbdmResourcePath = Paths.get("c:/Program File(x86)/pgo/USBDM 4.10.230");
      
      System.err.println("sourcePath = \'" + sourcePath.toString() + "\'");
      System.err.println("usbdmResourcePath = \'" + usbdmResourcePath.toString() + "\'");

      // Convert to relative path using ${usbdm_resource_path} if possible
      if (sourcePath.startsWith(usbdmResourcePath)) {
         try {
            sourcePath = usbdmResourcePath.relativize(sourcePath);
            System.err.println("relative sourcePath = \'" + sourcePath.toString() + "\'");
            sourcePath = Paths.get("usbdm_resource_path").resolve(sourcePath);
         } catch (IllegalArgumentException e) {
         }
      }
      System.err.println("macroed sourcePath' = \'" + sourcePath.toString() + "\'");
      URI sourceURI = null;
      try {
         sourceURI = new URI(sourcePath.toString().replaceAll("\\\\", "/"));
      } catch (URISyntaxException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      System.err.println("relative sourceURI' = \'" + sourceURI.toString() + "\'");
   }
   
}
