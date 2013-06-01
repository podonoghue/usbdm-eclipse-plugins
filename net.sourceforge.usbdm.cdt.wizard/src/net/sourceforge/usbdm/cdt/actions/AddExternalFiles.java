package net.sourceforge.usbdm.cdt.actions;
/**
 * 
 */


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.cdt.core.templateengine.TemplateCore;
import org.eclipse.cdt.core.templateengine.TemplateEngineHelper;
import org.eclipse.cdt.core.templateengine.process.ProcessArgument;
import org.eclipse.cdt.core.templateengine.process.ProcessFailureException;
import org.eclipse.cdt.core.templateengine.process.ProcessHelper;
import org.eclipse.cdt.core.templateengine.process.ProcessRunner;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author pgo
 *
 */
public class AddExternalFiles extends ProcessRunner {

   /**
    * This method takes a URL as parameter to read the contents, and to add
    * into a string buffer.
    * Remove file existence check since it was broken!! - pgo
    * 
    * @param source URL to read the contents.
    * @return string contents of a file specified in the URL source path.
    * @since 4.0
    */
   public static String readFromFile(URL source) throws IOException {
      char[] chars = new char[4092];
      InputStreamReader contentsReader = null;
      StringBuilder buffer = new StringBuilder();
      contentsReader = new InputStreamReader(source.openStream());
      int c;
      do {
         c = contentsReader.read(chars);
         if (c == -1)
            break;
         buffer.append(chars, 0, c);
      } while (c != -1);
      contentsReader.close();
      return buffer.toString();
   }

   /**
    * @param path                       path of source file
    * @param target                     path of target file
    * @param isReplaceable              whether to macro substitution
    * @param template                   template for macro values
    * @param projectHandle              handle for access to project
    * 
    * @throws ProcessFailureException
    */
   private void processFile(URL path, String target, Boolean isReplaceable, TemplateCore template, IProject projectHandle) throws ProcessFailureException {
//      System.err.println("AddExternalFiles.processFile(): ");
//      System.err.println("\tsource: \'"+ path.toString() + "\'");
//      System.err.println("\ttarget: \'" + target + "\'");
      InputStream contents = null;
      if (isReplaceable) {
         String fileContents;
         try {
            fileContents = readFromFile(path);
         } catch (IOException e) {
            throw new ProcessFailureException("\"" + path + "\" failed read"); //$NON-NLS-1$ //$NON-NLS-2$
         }
         fileContents = ProcessHelper.getValueAfterExpandingMacros(
               fileContents, ProcessHelper.getReplaceKeys(fileContents),
               template.getValueStore());
         contents = new ByteArrayInputStream(fileContents.getBytes());
      } else {
         try {
            contents = path.openStream();
         } catch (IOException e) {
            throw new ProcessFailureException("\"" + path + "\" failed open"); //$NON-NLS-1$  //$NON-NLS-2$
         }
      }
      try {
         IFile iFile = projectHandle.getFile(target);
         if (!iFile.getParent().exists()) {
            ProcessHelper.mkdirs(projectHandle, projectHandle.getFolder(iFile.getParent().getProjectRelativePath()));
         }
         iFile.create(contents, true, null);
         iFile.refreshLocal(IResource.DEPTH_ONE, null);
         projectHandle.refreshLocal(IResource.DEPTH_INFINITE, null);
      } catch (CoreException e) {
         throw new ProcessFailureException("Failed" + e.getMessage(), e); //$NON-NLS-1$
      }
   }

   /**
    * @param path                       path of source file or directory
    * @param target                     path of target file (if source is a directory then parent is used)
    * @param isReplaceable              whether to macro substitution
    * @param template                   template for macro values
    * @param projectHandle              handle for access to project
    * 
    * @throws ProcessFailureException
    */
   private void processItem(URL path, String target, Boolean isReplaceable, TemplateCore template, IProject projectHandle) throws ProcessFailureException {
      // Check if directory
//      System.err.println("AddExternalFiles.processItem() path.toString() = " + path.toString());
//      System.err.println("AddExternalFiles.processItem() path.getFile()  = " + path.getFile());

      File file;
      try {
        file = new File(path.toURI());
      } catch(URISyntaxException e) {
        file = new File(path.getPath());
      }
      if (file.isDirectory()) {
         // Directory copy
//         System.err.println("AddExternalFiles.processItem() folder  = " + file.toString());
         String contents[] = file.list();
         for (String filename:contents) {
            try {
               URL sourceURL = new URL(path.getProtocol(), path.getHost(), file.getAbsolutePath()+File.separator+filename);
//               System.err.println("AddExternalFiles.processItem() file URL = " + dirFile.toString());
               IContainer iPath = projectHandle.getFile(target).getParent();
               String newTarget = iPath.getProjectRelativePath().toOSString()+File.separator+filename;
//               System.err.println("AddExternalFiles.processItem() target = " + newTarget);
               processFile(sourceURL, newTarget, isReplaceable, template, projectHandle);               
            } catch (MalformedURLException e) {
               e.printStackTrace();
               return;
            }
         }
      }
      else {
         // Simple file copy
         processFile(path, target, isReplaceable, template, projectHandle);
      }
//      ProcessHelper.mkdirs(projectHandle, parentFolder)
   }
   
   @Override
   public void process(
         TemplateCore template, 
         ProcessArgument[] args,
         String processId, 
         IProgressMonitor monitor)
               throws ProcessFailureException {

      String              projectName   = null;
      ProcessArgument[][] copyList      = null; 
      for (ProcessArgument arg:args) {
//         System.err.println("AddExternalFiles.process() - arg = "+arg.getName());
         if (arg.getName().equals("projectName")) {
//            projectName   = args[0].getSimpleValue();        // name of project to get handle
            projectName   = arg.getSimpleValue();        // name of project to get handle
         }
         else if (arg.getName().equals("copyList")) {
//            copyList      = args[1].getComplexArrayValue();  // list of files/directories to process
            copyList      = arg.getComplexArrayValue();  // list of files/directories to process
         }
         else {
            throw new ProcessFailureException("AddExternalFiles.process() - Unexpected argument \'"+arg.getName()+"\'"); //$NON-NLS-1$
         }
      }
      if ((projectName == null) || (copyList == null)) {
         throw new ProcessFailureException("AddExternalFiles.process() - Missing arguments"); //$NON-NLS-1$
      }     
      IProject projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      if (projectHandle == null) {
         throw new ProcessFailureException("AddExternalFiles.process() - Unable to get projectHandle"); //$NON-NLS-1$
      }
      for (ProcessArgument[] file:copyList) {
         String externalFile = null;
         String internalFile = null;
         String target       = null;
         String replaceable  = null;
         for (ProcessArgument arg:file) {
            if (arg.getName().equals("externalSource")) {
               externalFile = file[0].getSimpleValue(); // externally provided file (full path)
            }
            else if (arg.getName().equals("internalSource")) {
               internalFile = file[1].getSimpleValue(); // internally provided file (default if no external)
            }
            else if (arg.getName().equals("target")) {
               target       = file[2].getSimpleValue(); // target resource
            }
            else if (arg.getName().equals("replaceable")) {
               replaceable  = file[3].getSimpleValue(); // do macro expansion on source
            }
         }
         if (target == null) {
            throw new ProcessFailureException("AddExternalFiles.process() - Missing arguments - target"); //$NON-NLS-1$
         }     
         boolean isReplaceable = (replaceable == null) || Boolean.valueOf(replaceable).booleanValue();
//         System.err.println("AddExternalFiles.ProcessRunner(" + projectName
//               + ", (" + externalFile + ", " + internalFile
//               + ") => (" + target + ", " + isReplaceable + "))");
         try {
            URL path = null;
            if ((externalFile != null) && !externalFile.isEmpty()) {
               // Use external file
//               System.err.println("External file found: \'" + externalFile + "\'");
               path = new URL(externalFile);
//               System.err.println("External file URL  : \'" + path.toString() + "\'");
//               // Use external file
//               IPath externalFilePath = new Path(externalFile);
//               if (!externalFilePath.toFile().exists()) {
//                  throw new UsbdmException("Exernal file not found: " + externalFilePath.toString());
//               }
//               System.err.println("Exernal file found: " + externalFilePath.toString());
//               path = new URL(externalFilePath.toFile().getPath());
            } else if (internalFile != null) {
               // Use internal file
               path = TemplateEngineHelper.getTemplateResourceURLRelativeToTemplate(template, internalFile);
            }else {
               throw new ProcessFailureException("AddExternalFiles.process() - Missing arguments - externalFile/internalFile"); //$NON-NLS-1$
            }
            processItem(path, target, isReplaceable, template, projectHandle);
         } catch (MalformedURLException e) {
            throw new ProcessFailureException("\"" + internalFile + "\" failed open, reason "+ e.getMessage()); //$NON-NLS-1$
         } catch (IOException e) {
            throw new ProcessFailureException("\"" + internalFile + "\" failed open"); //$NON-NLS-1$
         }
      }
   }
}
