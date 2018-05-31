package net.sourceforge.usbdm.cdt.ui.ksdk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

public class KSDKImporter {

   public static void importKDSLibraries(Shell shell) throws InvocationTargetException, InterruptedException {
      ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(shell);  
      progressDialog.run(true, true, new IRunnableWithProgress() {
         @Override
         public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            if (monitor == null) {
               monitor = new NullProgressMonitor();
            }
            monitor.beginTask("Building libraries...", IProgressMonitor.UNKNOWN);
            try {
               KSDKImporter.importKDSLibrary(monitor);
            } catch (Exception e) {
               e.printStackTrace();
            }
            monitor.done();
         }
      });
   }

   private static void importKDSLibrary(IProgressMonitor monitor) throws Exception {
      URI kdsPath = null; 
      try {
         IWorkspace workspace         = ResourcesPlugin.getWorkspace();
         IPathVariableManager pathMan = workspace.getPathVariableManager();
         kdsPath                      = pathMan.getURIValue(UsbdmSharedConstants.USBDM_KSDK_PATH);
      } catch (Exception e) {
         kdsPath  = new URI("file:/C:/Apps/Freescale/KSDK_1.3.0");
      }
      java.nio.file.Path sourcePath = java.nio.file.Paths.get(kdsPath).resolve("lib/ksdk_platform_lib/kds");
      DirectoryStream<java.nio.file.Path> stream;
      try {
         stream = Files.newDirectoryStream(sourcePath);
         for (java.nio.file.Path entry: stream) {
            importProject(entry, monitor);
         }
      } catch (IOException e) {
         e.printStackTrace();
      } catch (CoreException e) {
         e.printStackTrace();
      }
   }

   private static void importProject(final java.nio.file.Path targetPath, final IProgressMonitor monitor) throws Exception {
      monitor.subTask("Importing project " + targetPath.getFileName());
      
      IPath targetIPath = new Path(targetPath.toAbsolutePath().toString());
      System.err.println(String.format("importProject()  targetIPath = \'%s\'", targetIPath));

      URI                  targetURI = targetIPath.toFile().toURI();
      IWorkspace           workspace = ResourcesPlugin.getWorkspace();
      IPathVariableManager pathMan   = workspace.getPathVariableManager();
      targetURI = pathMan.convertToRelative(targetURI, false, UsbdmSharedConstants.USBDM_KSDK_PATH);

      final IProjectDescription externalProjectDescription = workspace.loadProjectDescription(targetIPath.append(".project"));
      externalProjectDescription.setLocationURI(targetURI);
      final IProject project = workspace.getRoot().getProject(externalProjectDescription.getName());
      try {
         if (!project.exists()) {
            // Only create the project if necessary
            project.create(externalProjectDescription, IProject.NONE, monitor);
         }
         project.open(monitor);
         project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
         project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
         // The KDS project don't have the RefreshPolicy set correctly
         project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
      } catch (CoreException e) {
         e.printStackTrace();
         System.err.println("Unable to process project \'" + targetPath.toAbsolutePath() + "\'");
      }
      monitor.worked(1);
   }

   public static void main(String[] args) throws URISyntaxException {
      try {
         KSDKImporter.importKDSLibraries(null);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
