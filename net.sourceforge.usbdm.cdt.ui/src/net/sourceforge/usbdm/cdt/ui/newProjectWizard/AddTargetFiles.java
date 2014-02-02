package net.sourceforge.usbdm.cdt.ui.newProjectWizard;
/**
 * 
 */


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.Device.FileInfo;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase.FileType;
import net.sourceforge.usbdm.jni.Usbdm;

import org.eclipse.cdt.core.templateengine.process.ProcessFailureException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 * @author pgo
 *
 */
public class AddTargetFiles {

   static String readFile(String path) throws IOException {
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
   }

   /**
    * @param path                       path of source file
    * @param target                     path of target file
    * @param isReplaceable              whether to macro substitution
    * @param variableMap                macro values
    * @param projectHandle              handle for access to project
    * 
    * @throws Exception 
    */
   private void processFile(String path, String target, Boolean isReplaceable, Map<String, String> variableMap, IProject projectHandle, IProgressMonitor monitor) throws Exception {
//      System.err.println("AddTargetFiles.processFile(): ");
//      System.err.println("\tsource: \'"+ path.toString() + "\'");
//      System.err.println("\ttarget: \'" + target + "\'");
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
         IFile iFile = projectHandle.getFile(target);
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
    * @param path                       path of source file or directory
    * @param target                     path of target file (if source is a directory then parent is used)
    * @param isReplaceable              whether to macro substitution
    * @param variableMap                template for macro values
    * @param projectHandle              handle for access to project
    * 
    * @throws Exception 
    * 
    * @throws ProcessFailureException
    */
   private void processItem(IPath path, String target, Boolean isReplaceable, Map<String, String> variableMap, IProject projectHandle, IProgressMonitor monitor) throws Exception {
      // Check if directory
      File file = path.toFile();
      if (file.isDirectory()) {
         // Directory copy
         // Remove last segment of path if it is
         IPath targetPath = new Path(target);
         if ((targetPath.getFileExtension() != null) && (targetPath.getFileExtension().equalsIgnoreCase("h"))) {
            target = targetPath.removeLastSegments(1).toPortableString();
         }
         System.err.println("AddTargetFiles.processItem() folder  = " + file.toString());
         String contents[] = file.list();
         for (String filename:contents) {
            IPath filePath = path.append(filename);
            String newTarget = target + File.separator + filename;
            processItem(filePath, newTarget, isReplaceable, variableMap, projectHandle, monitor);
         }
      }
      else {
         // Simple file copy
         processFile(path.toPortableString(), target, isReplaceable, variableMap, projectHandle, monitor);
      }
   }
   
   public void process(IProject projectHandle, Device device, Map<String,String> variableMap, FileInfo fileInfo, IProgressMonitor monitor) throws Exception {
      IPath usbdmDataPath = Usbdm.getResourcePath();

      String root   = MacroSubstitute.substitute(fileInfo.getRoot(),   variableMap);
      String source = MacroSubstitute.substitute(fileInfo.getSource(), variableMap);
      String target = MacroSubstitute.substitute(fileInfo.getTarget(), variableMap);

      FileType fileType = fileInfo.getFileType();
      
      if (source.isEmpty()) {
         return;
      }
      IPath path = new Path(source);
      if (!path.isAbsolute()) {
         if (root != null) {
            path = new Path(root).append(path);
         }
         if (!path.isAbsolute()) {
            path = usbdmDataPath.append(path);
         }
      }
      System.err.println(String.format("AddTargetFiles.process() \'%s\' => \'%s\'", source, target));
      switch (fileType) {
      case LINK :
         createLink(path, target, projectHandle, monitor);
         break;
      case NORMAL :
         processItem(path, target, fileInfo.isReplaceable(), variableMap, projectHandle, monitor);
         break;
      }
   }

   private void createLink(IPath path, String target, IProject projectHandle, IProgressMonitor monitor) throws Exception {
      IFile iFile = projectHandle.getFile(target);
      if (!iFile.getParent().exists()) {
         ProjectUtilities.createFolder(projectHandle, iFile.getParent().getProjectRelativePath().toString(), monitor);
      }
      iFile.createLink(path, IResource.NONE, monitor);
   }

}
