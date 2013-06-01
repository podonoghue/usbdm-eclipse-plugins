package net.sourceforge.usbdm.cdt.examplewizard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//import net.sourceforge.usbdm.cdt.UsbdmCdtConstants;
import net.sourceforge.usbdm.cdt.examplewizard.ExampleList.Attribute;
import net.sourceforge.usbdm.cdt.examplewizard.ExampleList.ProjectInformation;
import net.sourceforge.usbdm.jni.UsbdmException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * @author pgo
 *
 */
public class UsbdmExampleProjectsWizard extends Wizard implements INewWizard {

   private UsbdmSelectProjectWizardPage usbdmSelectProjectWizardPage;
 
   public UsbdmExampleProjectsWizard() {
      super();
   }

   @Override
   public void init(IWorkbench workbench, IStructuredSelection selection) {
//     System.err.println("ExampleProjectsWizard.init()");
     setNeedsProgressMonitor(true);
     setDialogSettings(Activator.getDefault().getDialogSettings());
   }

   @Override
   public boolean canFinish() {
      return super.canFinish();
   }

   private final static String MACRO_PREFIX = "${";
   private final static String MACRO_SUFFIX = "}";

   /**
    * @param   file   File to read from
    * @return         String with file contents
    * @throws         IOException
    */
   private static String readStream(InputStream in) throws IOException {
      
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      StringBuilder  buffer = new StringBuilder();
      char[] charBuffer = new char[4096];
      int numChars;
      
      while ((numChars = reader.read(charBuffer))>0) {
         buffer.append(charBuffer, 0, numChars);
      }
      return buffer.toString();
   }
   
   private static String expandMacros(String string, ArrayList<Attribute> attributes) {
      
      String buffer = string;
      for(Attribute macro:attributes) {
         String key = MACRO_PREFIX + macro.key + MACRO_SUFFIX;
//         System.err.println("expandMacros() - " + key + "=>" + macro.value);
         if (buffer.contains(key)) {
            buffer = buffer.replace(key, macro.value);
         }
      }
      return buffer;
   }
   
   static final void copyProfileStream(InputStream in, OutputStream out, ArrayList<Attribute> attributes ) throws IOException {
      
      String buffer   = readStream(in);
      String expanded = expandMacros(buffer, attributes);
      
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
      writer.write(expanded);
      
      in.close();
      writer.close();
   }

   static final void copyBinaryStream(InputStream in, OutputStream out) throws IOException {
      
      byte[] buffer = new byte[1024];
      int len = in.read(buffer);
      while (len != -1) {
          out.write(buffer, 0, len);
          len = in.read(buffer);
      }
      in.close();
      out.close();
   }

   private void addCommonAttributes(ArrayList<Attribute> attributes) {
//      System.err.println("addCommonAttributes(" + attributes.toString() + ")");
//      String os = System.getProperty("os.name");
      
//      if ((os != null) && os.toUpperCase().contains("LINUX")) {
//         attributes.add(new Attribute(UsbdmCdtConstants.CODESOURCERY_MAKE_COMMAND_KEY, "make"));
//      }
//      else {
//         attributes.add(new Attribute(UsbdmCdtConstants.CODESOURCERY_MAKE_COMMAND_KEY, "cs-make"));
//      }
   }
   
   @Override
   public boolean performFinish() {
      IWorkspace workspace = ResourcesPlugin.getWorkspace();

      ZipFile zipFile   = null;
      String internalProjectDirectoryName = null;
      if (!usbdmSelectProjectWizardPage.validate()) {
         return false;
      }
      // Unzip example into workspace
      try {
         usbdmSelectProjectWizardPage.saveSetting();
         
         ProjectInformation projectInformation = usbdmSelectProjectWizardPage.getProjectInformation();
         zipFile = new ZipFile(projectInformation.getPath().toFile());
         ArrayList<Attribute> attributes = projectInformation.getAttributes();
         attributes.add(new Attribute("projectName",     projectInformation.getDescription()));         
         addCommonAttributes(attributes);
         Enumeration<? extends ZipEntry> entries = zipFile.entries();
         if (!entries.hasMoreElements()) {
            throw new UsbdmException("Example Zip file is empty");
         }
         IPath workspaceRootPath = workspace.getRoot().getLocation();
         IPath projectPath       = null;
         // Do pass through zip file creating directories & files
         entries = zipFile.entries();
         while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry)entries.nextElement();
//            System.err.println("performFinish() entry = " + entry.getName());
            IPath internalPath = new Path(entry.getName());
            if (internalPath.segmentCount() < 1) {
               throw new UsbdmException("Example project has invalid structure, \'" + entry.toString() + "\' is missing project directory)");
            }
            if (internalProjectDirectoryName == null) {
               // Haven't got project directory yet - determine from root of first entry
               internalProjectDirectoryName = internalPath.segment(0);
               projectPath = workspaceRootPath.append(internalProjectDirectoryName);
//               System.err.println("performFinish() Project Path: " + projectPath.toString());
               File directory = projectPath.toFile();
               if (directory.exists()) {
                  // Project directory should not already exist
                  throw new UsbdmException("Project Directory already exists (" + internalProjectDirectoryName + ")");
               }
//               System.err.println("performFinish() Creating project directory:   " + directory.toString());
               directory.mkdir();
            }
            if (!internalProjectDirectoryName.equals(internalPath.segment(0))) {
               // Entry is not a child of project directory
               throw new UsbdmException("Example project has invalid structure, \'" + entry.toString() + "\' is outside project directory)");
            }
            if ((internalPath.segmentCount() == 1) && !entry.isDirectory()) {
               // Only 1 segment => must be project directory
               throw new UsbdmException("Example project has invalid structure, \'" + entry.toString() + "\' is outside project directory)");
            }
            
            IPath filePath = workspaceRootPath.append(internalPath);
            
            // Create each directory in path (excluding last segment which may be a file or directory)
            File directory = filePath.uptoSegment(filePath.segmentCount()-1).toFile();
            if (!directory.exists()) {
//               System.err.println("performFinish() Creating directories in path: " + directory.toString());
               directory.mkdirs();
            }
            if (entry.isDirectory()) {
               // Make the directory
               directory = filePath.toFile();
               if (!directory.exists()) {
//                  System.err.println("performFinish() Creating directory:           " + filePath.toString());
                  directory.mkdir();
               }
            }
            else {
//               System.err.println("performFinish() Creating file:                " + filePath.toString());
               if (filePath.lastSegment().equals(".project")) {
                  copyProfileStream(
                        zipFile.getInputStream(entry),
                        new FileOutputStream(filePath.toFile()),
                        attributes);
               }
               else {
                  copyBinaryStream(
                        zipFile.getInputStream(entry),
                        new FileOutputStream(filePath.toFile()));
               }
            }
         };
         if (projectPath == null) {
            throw new UsbdmException("No project Folder found");
         }
         IProjectDescription description   = workspace.loadProjectDescription(projectPath.append(".project"));
         IProject            newProject    = workspace.getRoot().getProject(description.getName());
         newProject.create(description, null);
         newProject.open(null);    
      } catch (Exception e) {
//         e.printStackTrace();
         MessageBox errBox = new MessageBox(getShell(), SWT.ERROR);
         errBox.setText("Error Creating Project");
         errBox.setMessage("Reason:\n\n    "+ e.getMessage());
         errBox.open();
      } finally {
         if (zipFile != null) {
            try {
               zipFile.close();
            } catch (IOException e) {
            }
         }
      }
      return true;
   }

   @Override
   public void addPages() {
      usbdmSelectProjectWizardPage = new UsbdmSelectProjectWizardPage(); 
      addPage(usbdmSelectProjectWizardPage);   
   }

}
