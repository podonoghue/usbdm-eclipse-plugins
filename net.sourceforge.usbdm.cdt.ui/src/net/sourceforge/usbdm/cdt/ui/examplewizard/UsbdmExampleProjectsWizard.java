package net.sourceforge.usbdm.cdt.ui.examplewizard;

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

import net.sourceforge.usbdm.cdt.ui.Activator;
import net.sourceforge.usbdm.cdt.ui.examplewizard.ExampleList.Attribute;
import net.sourceforge.usbdm.cdt.ui.examplewizard.ExampleList.ProjectInformation;
import net.sourceforge.usbdm.cdt.ui.wizards.UsbdmConfigurationPage;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.jni.UsbdmException;

import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.ui.templateengine.IWizardDataPage;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * @author pgo
 *
 */
public class UsbdmExampleProjectsWizard extends Wizard implements INewWizard {

   private UsbdmExampleSelectionPage usbdmSelectProjectWizardPage;
 
   public UsbdmExampleProjectsWizard() {
      super();
   }

   @Override
   public void init(IWorkbench workbench, IStructuredSelection selection) {
//     System.err.println("ExampleProjectsWizard.init()");
     setNeedsProgressMonitor(true);
     Activator activator = Activator.getDefault();
     if (activator != null) {
        setDialogSettings(activator.getDialogSettings());
     }
     setDefaultPageImageDescriptor(UsbdmSharedConstants.getUsbdmIcon());
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
   
   static final void convertTextStream(InputStream in, OutputStream out, ArrayList<Attribute> attributes ) throws IOException {
      
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
               //             System.err.println("performFinish() Creating file:                " + filePath.toString());
               String fileExtension = filePath.getFileExtension();
               String textFiles[] = {"project","cproject","h","hpp","c","cpp","s","asm","ld","launch","mk","rc","d"};
               boolean doConversion = false;
               if ((fileExtension!= null) && (fileExtension.length()>0)) {
                  for (String textExtension : textFiles) {
                     if (fileExtension.equalsIgnoreCase(textExtension)) {
                        doConversion = true;
                        break;
                     }
                  }
               }
               else {
                  String filename = filePath.lastSegment();
                  String specialFiles[] = {"makefile","timestamp"};
                  if ((filename!= null) && (filename.length()>0)) {
                     for (String specialFile : specialFiles) {
                        if (filename.equalsIgnoreCase(specialFile)) {
                           doConversion = true;
                           break;
                        }
                     }
                  }
               }
               if (doConversion) {
//                  System.err.println("performFinish() Converting file:                " + filePath.toString());
                  convertTextStream(
                        zipFile.getInputStream(entry),
                        new FileOutputStream(filePath.toFile()),
                        attributes);
               }
               else {
//                  System.err.println("performFinish() Copying file:                   " + filePath.toString());
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

         String additionalProjectNature = null;
         if ((usbdmSelectProjectWizardPage.getSelectedBuildToolId().equals(UsbdmSharedConstants.CODESOURCERY_ARM_BUILD_ID)) ||
               (usbdmSelectProjectWizardPage.getSelectedBuildToolId().equals(UsbdmSharedConstants.ARMLTD_ARM_BUILD_ID))) {
            additionalProjectNature = "net.sourceforge.usbdm.cdt.tools.ArmProjectNature";
         }
         else if (usbdmSelectProjectWizardPage.getSelectedBuildToolId().equals(UsbdmSharedConstants.CODESOURCERY_COLDFIRE_BUILD_ID)) {
            additionalProjectNature = "net.sourceforge.usbdm.cdt.tools.ColdfireProjectNature";            
         }
         if (additionalProjectNature != null) {
            IProjectDescription projectDescription = newProject.getDescription();
            String[] currentIds = projectDescription.getNatureIds();
            String[] newIds = new String[currentIds.length+1];
            int index;
            for (index = 0; index < currentIds.length; index++) {
               newIds[index] = currentIds[index];
//               System.err.println("AddNature.process() Copying nature : " + newIds[index]);
            }
            newIds[index] = additionalProjectNature;
//            System.err.println("AddNature.process() Adding nature : " + newIds[index]);
            projectDescription.setNatureIds(newIds);
            newProject.setDescription(projectDescription, null);
            ManagedBuildManager.saveBuildInfo(newProject, true);
         }
      } catch (Exception e) {
         e.printStackTrace();
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
      
      usbdmSelectProjectWizardPage = new UsbdmExampleSelectionPage();
      IWizardDataPage[] pages = {
         usbdmSelectProjectWizardPage,
         new UsbdmConfigurationPage(usbdmSelectProjectWizardPage),
      };
      for (int index=1; index<pages.length; index++) {
         pages[index-1].setNextPage(pages[index]);
         pages[index].setPreviousPage(pages[index-1]);
      }
      for (IWizardDataPage page:pages) {
         addPage(page);
      }
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);

      // Instantiates and initializes the wizard
      UsbdmExampleProjectsWizard wizard = new UsbdmExampleProjectsWizard();
      wizard.init(null, //part.getSite().getWorkbenchWindow().getWorkbench(),
                  null   //(IStructuredSelection)selection
                  );
      // Instantiates the wizard container with the wizard and opens it
      WizardDialog dialog = new WizardDialog(shell, wizard);
      dialog.create();
      dialog.open();
   }

}
