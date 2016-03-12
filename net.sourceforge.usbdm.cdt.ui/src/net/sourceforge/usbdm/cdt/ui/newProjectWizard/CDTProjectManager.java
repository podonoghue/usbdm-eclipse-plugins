package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.util.Arrays;
import java.util.Map;
import java.util.Vector;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IndexerSetupParticipant;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
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
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;

@SuppressWarnings({ "restriction", "unused" })
public class CDTProjectManager {

   static final String ccNature     = org.eclipse.cdt.core.CCProjectNature.CC_NATURE_ID;

   /**
    * Create basic CDT project
    * Based on org.eclipse.cdt.managedbuilder.testplugin
    * 
    * @param projectName      Name of project
    * @param directoryPath    Where to create in file system
    * @param hasCCNature      Adds C++ nature if true
    * @param monitor          Where to report progress
    * 
    * @return CDT Project created
    * 
    * @throws Exception
    */
   public IProject createProject(String projectName, String directoryPath, boolean hasCCNature, IProgressMonitor monitor) throws Exception {
//    System.err.println(String.format("CDTProjectManager.createProject(%s, %s)", projectName, directoryPath));
      final int WORK_SCALE = 1000;
      
      IProject project = null;
      try {
         monitor.beginTask("Creating project", IProgressMonitor.UNKNOWN);

         IWorkspace          workspace          = ResourcesPlugin.getWorkspace();
         IWorkspaceRoot      wrkSpaceRoot       = workspace.getRoot();
         final IProject      newProjectHandle   = wrkSpaceRoot.getProject(projectName);

         // Turn off Auto-build in workspace
         // TODO - should restore to original after project construction?
         IWorkspaceDescription workspaceDesc = workspace.getDescription();
         workspaceDesc.setAutoBuilding(false);
         workspace.setDescription(workspaceDesc);

         IProjectDescription projectDescription = workspace.newProjectDescription(projectName);
         if ((directoryPath != null) && (!directoryPath.isEmpty())) {
            IPath path = new Path(directoryPath).append(projectName);
            projectDescription.setLocation(path);
         }
         project = CCorePlugin.getDefault().createCDTProject(projectDescription, newProjectHandle, new SubProgressMonitor(monitor, WORK_SCALE*30));     
         Assert.isNotNull(project, "Project not created");
         if (hasCCNature) {
            CCProjectNature.addCCNature(project, monitor);
         }
         UsbdmProjectNature.addNature(project, monitor);
         if (hasCCNature && !project.hasNature(ccNature)) {
            System.err.println("C++ Nature is missing!");
         }
         // Open the project if we have to
         if (!project.isOpen()) {
            project.open(new SubProgressMonitor(monitor, WORK_SCALE*30));
         }
      } finally {
         monitor.done();
      }
      return project;
   }

   private final String ARM_CONFIGURATION_ID        = "net.sourceforge.usbdm.cdt.arm";
   private final String COLDFIRE_CONFIGURATION_ID   = "net.sourceforge.usbdm.cdt.coldfire";

   /**
    * Create USBDM CDT project
    * 
    * @param paramMap      Parameters for project (from Wizard dialogue)
    * @param monitor       Progress monitor
    * 
    * @return  Created project
    * 
    * @throws Exception
    */
   public IProject createCDTProj(
         Map<String, String>  paramMap, 
         IProgressMonitor     monitor) throws Exception {

      final int WORK_SCALE = 1000;

      // Create model project and accompanied descriptions
      IProject project;

      try {
         monitor.beginTask("Create configuration", WORK_SCALE*100);

         String        projectName   = MacroSubstitute.substitute(paramMap.get(UsbdmConstants.PROJECT_NAME_KEY), paramMap); 
         String        directoryPath = MacroSubstitute.substitute(paramMap.get(UsbdmConstants.PROJECT_HOME_PATH_KEY), paramMap); 
         String        projectType   = MacroSubstitute.substitute(paramMap.get(UsbdmConstants.PROJECT_OUTPUT_TYPE_KEY), paramMap);
         InterfaceType interfaceType = InterfaceType.valueOf(paramMap.get(UsbdmConstants.INTERFACE_TYPE_KEY));
         boolean       hasCCNature   = Boolean.valueOf(paramMap.get(UsbdmConstants.HAS_CC_NATURE_KEY));
         String        artifactName  = MacroSubstitute.substitute(paramMap.get(UsbdmConstants.PROJECT_ARTIFACT_KEY), paramMap); 

         if ((artifactName == null) || (artifactName.length()==0)) {
            artifactName = "${ProjName}";
         }
         project = createProject(projectName, directoryPath, hasCCNature, new SubProgressMonitor(monitor, WORK_SCALE*70));

         CoreModel coreModel = CoreModel.getDefault();

         // Create project description
         ICProjectDescription projectDescription = coreModel.createProjectDescription(project, false);
         Assert.isNotNull(projectDescription, "createProjectDescription returned null");

         // Create one configuration description
         ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
         IProjectType     type = ManagedBuildManager.getProjectType(projectType);
         Assert.isNotNull(type, "project type not found");

         ManagedProject mProj = new ManagedProject(project, type);
         info.setManagedProject(mProj);

         IConfiguration cfgs[] = type.getConfigurations();
         Assert.isNotNull(cfgs, "configurations not found");
         Assert.isTrue(cfgs.length>0, "no configurations found in the project type");

         String configurationName = null;
         String os = System.getProperty("os.name");
         switch (interfaceType) {
         case T_ARM:  configurationName = ARM_CONFIGURATION_ID;      break;
         case T_CFV1:
         case T_CFVX: configurationName = COLDFIRE_CONFIGURATION_ID; break;
         }
         for (IConfiguration configuration : cfgs) {
            String configId = configuration.getId();
            if (!configId.startsWith(configurationName)) {
               continue;
            }
            String id = ManagedBuildManager.calculateChildId(configuration.getId(), null);
            Configuration config = new Configuration(mProj, (Configuration)configuration, id, false, true, false);
            config.setArtifactName(artifactName);
            CConfigurationData data = config.getConfigurationData();
            Assert.isNotNull(data, "data is null for created configuration");
            projectDescription.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
         }
         Assert.isTrue(projectDescription.getConfigurations().length > 0, "No Configurations!");
         coreModel.setProjectDescription(project, projectDescription);
      } finally {
         monitor.done();
      }
      return project;
   }

}
