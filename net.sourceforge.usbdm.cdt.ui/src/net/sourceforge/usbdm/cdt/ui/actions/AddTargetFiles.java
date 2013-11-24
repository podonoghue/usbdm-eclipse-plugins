package net.sourceforge.usbdm.cdt.ui.actions;
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
import java.util.Iterator;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.Device.FileInfo;
import net.sourceforge.usbdm.deviceDatabase.Device.FileList;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;

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
public class AddTargetFiles extends ProcessRunner {

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
//      System.err.println("AddTargetFiles.processFile(): ");
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
         // Don't replace existing, more specific, file
         if (!iFile.exists()) {
            iFile.create(contents, true, null);
            iFile.refreshLocal(IResource.DEPTH_ONE, null);
            projectHandle.refreshLocal(IResource.DEPTH_INFINITE, null);
         }
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
//      System.err.println("AddTargetFiles.processItem() path.toString() = " + path.toString());
//      System.err.println("AddTargetFiles.processItem() path.getFile()  = " + path.getFile());

      File file;
      try {
        file = new File(path.toURI());
      } catch(URISyntaxException e) {
        file = new File(path.getPath());
      }
      if (file.isDirectory()) {
         // Directory copy
//         System.err.println("AddTargetFiles.processItem() folder  = " + file.toString());
         String contents[] = file.list();
         for (String filename:contents) {
            try {
               URL sourceURL = new URL(path.getProtocol(), path.getHost(), file.getAbsolutePath()+File.separator+filename);
//               System.err.println("AddTargetFiles.processItem() file URL = " + dirFile.toString());
               IContainer iPath = projectHandle.getFile(target).getParent();
               String newTarget = iPath.getProjectRelativePath().toOSString()+File.separator+filename;
//               System.err.println("AddTargetFiles.processItem() target = " + newTarget);
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
         String processId, IProgressMonitor monitor)
         throws ProcessFailureException {

      System.err.println("AddTargetFiles.process()"); //$NON-NLS-1$
      
      String projectName          = null;
      String targetDeviceFamily   = null;
      String targetDevice         = null;

      for (ProcessArgument arg:args) {
         if (arg.getName().equals("projectName")) {
            projectName = arg.getSimpleValue();              // Name of project to get handle
         }
         else if (arg.getName().equals("targetDeviceFamily")) {
            targetDeviceFamily  = arg.getSimpleValue();      // Target device family e.g. CFV1 etc
         }
         else if (arg.getName().equals("targetDevice")) {
            targetDevice  = arg.getSimpleValue();            // Target device name
         }
         else {
            throw new ProcessFailureException("AddTargetFiles.process() - Unexpected argument \'"+arg.getName()+"\'"); //$NON-NLS-1$
         }
      }
      if ((projectName == null) || (targetDeviceFamily == null) || (targetDevice == null)) {
         throw new ProcessFailureException("AddTargetFiles.process() - Missing arguments"); //$NON-NLS-1$
      }     
      IProject projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      if (projectHandle == null) {
         throw new ProcessFailureException("AddTargetFiles.process() - Unable to get projectHandle"); //$NON-NLS-1$
      }
      InterfaceType deviceType = InterfaceType.valueOf(targetDeviceFamily);
      DeviceDatabase deviceDatabase = new DeviceDatabase(deviceType.targetType);
      if (!deviceDatabase.isValid()) {
         throw new ProcessFailureException("Device database failed to load"); //$NON-NLS-1$ //$NON-NLS-2$
      }
      Device device = deviceDatabase.getDevice(targetDevice);
      if (device == null) {
         throw new ProcessFailureException("Device \""+targetDevice+"\" not found in database"); //$NON-NLS-1$ //$NON-NLS-2$
      }
      System.err.println("AddTargetFiles.process("+device.getName()+")");
      FileList fileList = device.getFileListMap();
      if (fileList != null) {
         System.err.println("AddTargetFiles.process() - has map");
         Iterator<FileInfo> it = fileList.iterator();
         while(it.hasNext()) {
            FileInfo fileInfo = it.next();
            System.err.println("AddTargetFiles.process() - "+fileInfo.toString());
            try {
               URL path = TemplateEngineHelper.getTemplateResourceURLRelativeToTemplate(template, fileInfo.getSource());
               if (path != null) {
                  processItem(path, fileInfo.getTarget(), fileInfo.isReplaceable(), template, projectHandle);
               }
            } catch (MalformedURLException e) {
               throw new ProcessFailureException("\"" + fileInfo.getSource() + "\" failed open, reason "+ e.getMessage()); //$NON-NLS-1$
            } catch (IOException e) {
               throw new ProcessFailureException("\"" + fileInfo.getSource() + "\" failed open"); //$NON-NLS-1$
            }
            
         }
      }     
   }
   
}
