package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.util.Map;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;


@SuppressWarnings("restriction")
public class CDTProjectManager {

   // Based on org.eclipse.cdt.managedbuilder.testplugin
   public IProject createProject(String projectName, String directoryPath, IProgressMonitor progressMonitor) throws CoreException {

      System.err.println(String.format("CDTProjectManager.createProject(%s, %s)", projectName, directoryPath));

      progressMonitor.beginTask("Creating project", IProgressMonitor.UNKNOWN);

      IWorkspace          workspace          = ResourcesPlugin.getWorkspace();
      IWorkspaceRoot      wrkSpaceRoot       = workspace.getRoot();
      final IProject      newProjectHandle   = wrkSpaceRoot.getProject(projectName);
      IProject            project            = null;

      if (!newProjectHandle.exists()) {
         progressMonitor.beginTask("Creating project", IProgressMonitor.UNKNOWN);
         IWorkspaceDescription workspaceDesc = workspace.getDescription();
         workspaceDesc.setAutoBuilding(false);
         workspace.setDescription(workspaceDesc);
         IProjectDescription projectDescription = workspace.newProjectDescription(projectName);
         if ((directoryPath != null) && (!directoryPath.isEmpty())) {
            IPath path = new Path(directoryPath).append(projectName);
            projectDescription.setLocation(path);
         }
         project = CCorePlugin.getDefault().createCDTProject(projectDescription, newProjectHandle, progressMonitor);         
      }
      else {
         progressMonitor.beginTask("Refreshing project", IProgressMonitor.UNKNOWN);
         IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
               newProjectHandle.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            }
         };
         workspace.run(runnable, wrkSpaceRoot, IWorkspace.AVOID_UPDATE, progressMonitor);
         project = newProjectHandle;
      }
      Assert.isNotNull(project, "Project not created");
      
      // Open the project if we have to
      if (!project.isOpen()) {
         project.open(progressMonitor);
      }
      return project;
   }
   
   private final String PROJECT_TYPE_ID = "net.sourceforge.usbdm.cdt.projectType.exe";

   private final String WINDOWS_ARM_CONFIGURATION_ID        = "net.sourceforge.usbdm.cdt.arm.windows";
   private final String WINDOWS_COLDFIRE_CONFIGURATION_ID   = "net.sourceforge.usbdm.cdt.coldfire.windows";
   private final String LINUX_ARM_CONFIGURATION_ID          = "net.sourceforge.usbdm.cdt.arm.linux";
   private final String LINUX_COLDFIRE_CONFIGURATION_ID     = "net.sourceforge.usbdm.cdt.coldfire.linux";
   
   void createCDTProj(
         String               projectName, 
         String               directoryPath, 
         InterfaceType        interfaceType, 
         boolean              hasCCNature,
         Device               device,
         Map<String, String>  map, 
         IProgressMonitor     progressMonitor) throws Exception {
      
      map.put("projectName", projectName);

      // Create model project and accompanied descriptions
      IProject project = createProject(projectName, directoryPath, progressMonitor);

      CoreModel coreModel = CoreModel.getDefault();

      // Create project description
      ICProjectDescription projectDescription = coreModel.createProjectDescription(project, false);
      Assert.isNotNull(projectDescription, "createProjectDescription returned null");
      
      // Create one configuration description
      ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
      IProjectType type = ManagedBuildManager.getProjectType(PROJECT_TYPE_ID);
      Assert.isNotNull(type, "project type not found");
      
      ManagedProject mProj = new ManagedProject(project, type);
      info.setManagedProject(mProj);
      
      IConfiguration cfgs[] = type.getConfigurations();
      Assert.isNotNull(cfgs, "configurations not found");
      Assert.isTrue(cfgs.length>0, "no configurations found in the project type");

      String configurationName = null;
      String os = System.getProperty("os.name");
      if ((os != null) && os.toUpperCase().contains("LINUX")) {
         switch (interfaceType) {
         case T_ARM:  configurationName = LINUX_ARM_CONFIGURATION_ID;      break;
         case T_CFV1: configurationName = LINUX_COLDFIRE_CONFIGURATION_ID; break;
         case T_CFVX: configurationName = LINUX_COLDFIRE_CONFIGURATION_ID; break;
         }
      }
      else {
         switch (interfaceType) {
         case T_ARM:  configurationName = WINDOWS_ARM_CONFIGURATION_ID;      break;
         case T_CFV1: configurationName = WINDOWS_COLDFIRE_CONFIGURATION_ID; break;
         case T_CFVX: configurationName = WINDOWS_COLDFIRE_CONFIGURATION_ID; break;
         }
      }
      for (IConfiguration configuration : cfgs) {
         String configId = configuration.getId();
         if (!configId.startsWith(configurationName)) {
            continue;
         }
         String id = ManagedBuildManager.calculateChildId(configuration.getId(), null);
         Configuration config = new Configuration(mProj, (Configuration)configuration, id, false, true, false);
         config.setArtifactName("${ProjName}");
         CConfigurationData data = config.getConfigurationData();
         Assert.isNotNull(data, "data is null for created configuration");
         projectDescription.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
      }
      try {
         if (hasCCNature) {
            // Add cc nature
            IProjectDescription description = project.getDescription();
            String[] natures = description.getNatureIds();
            String[] newNatures = new String[natures.length + 1];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            newNatures[natures.length] = "org.eclipse.cdt.core.ccnature";
            description.setNatureIds(newNatures);
            project.setDescription(description, progressMonitor);
         }
      } catch (CoreException e) {
         throw new Exception("Failed to set CC nature", e);
      }
      Assert.isTrue(projectDescription.getConfigurations().length > 0);

      // Persist project description.
      coreModel.setProjectDescription(project, projectDescription);

      String sourceDirectories[]  = {"Sources", "Startup_Code"};
      String includeFolders[]     = {"Project_Headers"};
      String folders[]            = {"Project_Settings"};
      
      for (String s : sourceDirectories) {
         ProjectUtilities.createSourceFolder(project, s, progressMonitor);
      }
      for (String s : includeFolders) {
         ProjectUtilities.createIncludeFolder(project, s, progressMonitor);
      }
      for (String s : folders) {
         ProjectUtilities.createFolder(project, s, progressMonitor);
      }
      new ProcessProjectActions().process(project, device, map, progressMonitor);
   }


}