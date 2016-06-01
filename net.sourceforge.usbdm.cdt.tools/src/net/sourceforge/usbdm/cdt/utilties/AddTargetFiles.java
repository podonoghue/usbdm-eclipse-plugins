package net.sourceforge.usbdm.cdt.utilties;
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.variables.VariablesPlugin;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.packageParser.FileAction;
import net.sourceforge.usbdm.packageParser.FileAction.PathType;

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
    * @param sourcePath          path of source file
    * @param targetPath          path of target file
    * @param doMacroReplacement  whether to macro substitution
    * @param variableMap         macro values
    * @param projectHandle       handle for access to project
    * 
    * @throws Exception 
    */
   private void copyFile(Path sourcePath, Path targetPath, FileAction fileAction, Map<String, String> variableMap, IProject projectHandle, IProgressMonitor monitor) throws Exception {
//      System.err.println(String.format("AddTargetFiles.copyFile() \'%s\' \n\t=> \'%s\'", sourcePath, targetPath));
      InputStream contents = null;
      String fileContents;
      try {
         fileContents = readFile(sourcePath);
      } catch (IOException e) {
         throw new Exception("\"" + sourcePath + "\" failed read"); //$NON-NLS-1$ //$NON-NLS-2$
      }
      if (fileAction.isDoMacroReplacement()) {
         fileContents = MacroSubstitute.substitute(fileContents, variableMap);
      }
      contents = new ByteArrayInputStream(fileContents.getBytes());
      try {
         monitor.beginTask("Copy File", 100);
         IFile iFile = projectHandle.getFile(targetPath.toString());
         int remainder = 100;
         if (!iFile.getParent().exists()) {
            ProjectUtilities.createFolder(projectHandle, iFile.getParent().getProjectRelativePath().toString(), new SubProgressMonitor(monitor, 50));
            remainder -= 50;
         }
         // Replace existing, more specific, file
         if (iFile.exists()) {
            if (fileAction.isDoMacroReplacement()) {
//             System.err.println("AddTargetFiles.processFile() - replacing " + iFile.toString());
               iFile.delete(true, new SubProgressMonitor(monitor, remainder));               
            }
            else {
               throw new Exception("\"" + iFile.toString() + "\" already exists"); //$NON-NLS-1$ //$NON-NLS-2$
            }
         }
         iFile.create(contents, true, null);
         iFile.setDerived(fileAction.isDerived(), monitor);
         iFile.refreshLocal(IResource.DEPTH_ONE, null);
         projectHandle.refreshLocal(IResource.DEPTH_INFINITE, null);
      } catch (CoreException e) {
         throw new Exception("Failed" + e.getMessage(), e); //$NON-NLS-1$
      } finally {
         monitor.done();
      }
   }

   /**
    * Resolves a path containing Path variables
    * 
    * @param sourcePath
    * 
    * @return Modified path
    * 
    * @throws URISyntaxException
    */
   Path resolvePath(Path sourcePath) throws URISyntaxException {
      
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      IPathVariableManager pathMan = workspace.getPathVariableManager();
      URI sourceURI = URIUtil.fromString("file:"+sourcePath.toString());
//      System.err.println(String.format("AddTargetFiles.resolvePath() sourceURI.a = \'%s\'",   sourceURI.toASCIIString()));
//      System.err.println(String.format("AddTargetFiles.resolvePath() sourceURI.p = \'%s\'",   sourceURI.getPath()));
//      System.err.println(String.format("AddTargetFiles.resolvePath() Absolute?   = \'%s\'\n", sourceURI.isAbsolute()?"True":"False"));
      sourceURI = pathMan.resolveURI(sourceURI);
//      System.err.println(String.format("AddTargetFiles.resolvePath() resolved URI.a = \'%s\'", sourceURI.toASCIIString()));
//      System.err.println(String.format("AddTargetFiles.resolvePath() resolved URI.p = \'%s\'", sourceURI.getPath()));
//      System.err.println(String.format("AddTargetFiles.resolvePath() Absolute?      = \'%s\'\n", sourceURI.isAbsolute()?"True":"False"));
      String source = sourceURI.getPath().replaceFirst("/([a-zA-Z]:.*)", "$1");
      sourcePath = Paths.get(source);
//      System.err.println(String.format("AddTargetFiles.resolvePath() sourcePath     = \'%s\'", sourcePath));
      
      return sourcePath;
   }
   /**
    * 
    * @param sourcePath          path of source file or directory
    * @param targetPath          path of target file (if source is a directory then parent is used)
    * @param doMacroReplacement  whether to macro substitution
    * @param variableMap         template for macro values
    * @param projectHandle       handle for access to project
    * @param monitor
    * 
    * @throws Exception
    */
   private void copyFiles(Path sourcePath, Path targetPath, FileAction fileInfo, Map<String, String> variableMap, IProject projectHandle, IProgressMonitor monitor) throws Exception {
      //      System.err.println("AddTargetFiles.processItem() file  = " + path.toString());
      //      System.err.println("AddTargetFiles.processItem() exists?  = " + Files.exists(path));
      //      System.err.println("AddTargetFiles.processItem() directory?  = " + Files.isDirectory(path));
      //      System.err.println("AddTargetFiles.processItem() targetPath = " + targetPath.toString());
      //      System.err.println("AddTargetFiles.processItem() targetPath.getFileName = " + targetPath.getFileName().toString());

      sourcePath = resolvePath(sourcePath);
      //      System.err.println(String.format("AddTargetFiles.process() sourcePath     = \'%s\'", sourcePath));

      // Check if directory
      if (Files.isDirectory(sourcePath)) {
         // Directory copy
         // Hack for when we expected a header file but were given entire folder of header files
         String filename = targetPath.getFileName().toString();
         if (filename.endsWith(".h") || filename.endsWith(".H")) {
            targetPath = targetPath.getParent();
//          System.err.println("AddTargetFiles.processItem() changed target to = " + targetPath.toAbsolutePath());
         }
//       System.err.println("AddTargetFiles.processItem() folder  = " + path.toString());
         DirectoryStream<Path> stream = Files.newDirectoryStream(sourcePath);
         
         try {
            monitor.beginTask("Copy Files", 100);
            for (java.nio.file.Path entry: stream) {
               copyFiles(entry, targetPath.resolve(entry.getFileName()), fileInfo, variableMap, projectHandle, new SubProgressMonitor(monitor, 1));
            }
         } finally {
            monitor.done();
         }
      }
      else {
         // Simple file copy
         copyFile(sourcePath, targetPath, fileInfo, variableMap, projectHandle, monitor);
      }
   }

   /**
    * Create link to file.  
    * Attempts to make it relative to usbdm_resource_path or usbdm_kds_path path variables
    * 
    * @param sourcePath    Path of source. May start with path variable
    * @param target
    * @param projectHandle
    * @param monitor
    * @throws Exception
    */
   private void createLink(Path sourcePath, String target, IProject projectHandle, IProgressMonitor monitor) throws Exception {
      URI                  sourceURI = URIUtil.fromString("file:"+sourcePath.toString());
      IWorkspace           workspace = ResourcesPlugin.getWorkspace();
      IPathVariableManager pathMan   = workspace.getPathVariableManager();
      sourceURI = pathMan.convertToRelative(sourceURI, false, UsbdmSharedConstants.USBDM_APPLICATION_PATH_VAR);
      sourceURI = pathMan.convertToRelative(sourceURI, false, UsbdmSharedConstants.USBDM_KSDK_PATH);
      sourcePath = resolvePath(sourcePath);
//      System.err.println(String.format("AddTargetFiles.createLink() \'%s\' => \'%s\'", sourceURI.toString(), target));
      
      try {
         monitor.beginTask("Create Link", 100);
         
         if (sourcePath.toFile().isDirectory()) {
            IFolder iFolder = projectHandle.getFolder(target);
            if (!iFolder.getParent().exists()) {
               // Create parent directories if necessary
               ProjectUtilities.createFolder(projectHandle, iFolder.getParent().getProjectRelativePath().toString(), new SubProgressMonitor(monitor, 50));
            }
            iFolder.createLink(sourceURI, IResource.ALLOW_MISSING_LOCAL, new SubProgressMonitor(monitor, 50));
         }
         else {
            IFile iFile = projectHandle.getFile(target);
            if (!iFile.getParent().exists()) {
               // Create parent directories if necessary
               ProjectUtilities.createFolder(projectHandle, iFile.getParent().getProjectRelativePath().toString(), new SubProgressMonitor(monitor, 50));
            }
            iFile.createLink(sourceURI, IResource.ALLOW_MISSING_LOCAL, new SubProgressMonitor(monitor, 50));
         }
      } finally {
         monitor.done();
      }
   }

   public void process(IProject projectHandle, Map<String,String> variableMap, FileAction fileInfo, IProgressMonitor monitor) throws Exception {
      /*
       * Do macro substitution on path using project wizard variables
       */
      if (projectHandle == null) {
         // For debug
         System.err.println("Debug: "+fileInfo);
         return;
      }
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
      /*
       * Do macro substitution on path using Eclipse Variables
       */
      VariablesPlugin varPlugin = VariablesPlugin.getDefault();
      if (varPlugin != null) {
         root   = varPlugin.getStringVariableManager().performStringSubstitution(root,   false);
         source = varPlugin.getStringVariableManager().performStringSubstitution(source, false);
         target = varPlugin.getStringVariableManager().performStringSubstitution(target, false);
      }
      /*
       * Make source path absolute if necessary 
       */
      Path sourcePath = Paths.get(source);
      if (fileInfo.getSourcePathType() == PathType.RELATIVE) {
         sourcePath = Paths.get(root, source);
      }
      else if (fileInfo.getSourcePathType() == PathType.UNKNOWN) {
         if (!sourcePath.isAbsolute()) {
            sourcePath = Paths.get(root, source);
         }
      }

      Path targetPath = Paths.get(target);
//      System.err.println(String.format("AddTargetFiles.process() ============================================="));
//      System.err.println(String.format("AddTargetFiles.process() \'%s\' => \'%s\'", sourcePath, targetPath));

//      System.err.println(String.format("AddTargetFiles.process() source     = \'%s\'", source));
//      System.err.println(String.format("AddTargetFiles.process() sourcePath = \'%s\'", sourcePath));

      switch (fileInfo.getFileType()) {
         case LINK : {
            createLink(sourcePath, target, projectHandle, monitor);
            break;
         }
         case NORMAL : {
            copyFiles(sourcePath, targetPath, fileInfo, variableMap, projectHandle, monitor);
            break;
         }
      }
   }

static void tryIt(String s) throws URISyntaxException {
   System.err.println(String.format("AddTargetFiles.process() ========================================"));
   System.err.println(String.format("AddTargetFiles.process() s              = \'%s\'", s));
      URI sourceURI = URIUtil.fromString("file:"+s);
      System.err.println(String.format("AddTargetFiles.process() sourceURI      = \'%s\'", sourceURI));
      System.err.println(String.format("AddTargetFiles.process() sourceURI.a = \'%s\'", sourceURI.toASCIIString()));
      System.err.println(String.format("AddTargetFiles.process() sourceURI.h = \'%s\'", sourceURI.getHost()));
      System.err.println(String.format("AddTargetFiles.process() sourceURI.s = \'%s\'", sourceURI.getScheme()));
      System.err.println(String.format("AddTargetFiles.process() sourceURI.a = \'%s\'", sourceURI.getAuthority()));
      System.err.println(String.format("AddTargetFiles.process() sourceURI.p = \'%s\'", sourceURI.getPath()));
      System.err.println(String.format("AddTargetFiles.process() sourceURI.q = \'%s\'", sourceURI.getQuery()));
      System.err.println(String.format("AddTargetFiles.process() sourceURI.f = \'%s\'", sourceURI.getFragment()));
      System.err.println(String.format("AddTargetFiles.process() Absolute?   = \'%s\'\n", sourceURI.isAbsolute()?"True":"False"));
      String ss = sourceURI.getPath();
      ss = ss.replaceFirst("/([a-zA-Z]:.*)", "$1");
      System.err.println(String.format("AddTargetFiles.process() ss   = \'%s\'\n", ss));
   }
   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      
      try {
         tryIt("C:\\Program Files (x86)\\pgo\\USBDM 4.10.6.260\\Stationery\\Packages\\100.ARM_DeviceOptions\\Startup_Code\\system-mkxxx.c");
         tryIt("jskajska/a/b/c/d.c");
      } catch (URISyntaxException e1) {
         e1.printStackTrace();
      }
   }
}
