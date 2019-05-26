package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.util.Map;

import org.eclipse.cdt.core.CCProjectNature;
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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.cdt.utilties.ReplacementParser;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;

@SuppressWarnings({ "restriction", "unused" })
public class CDTProjectManager {

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
   public IProject createProject(String projectName, String directoryPath, boolean hasCCNature, IProgressMonitor progressMonitor) throws Exception {
//    System.err.println(String.format("CDTProjectManager.createProject(%s, %s)", projectName, directoryPath));
      SubMonitor monitor = SubMonitor.convert(progressMonitor, 100);

      IProject project = null;
      try {
         monitor.beginTask("Creating project", IProgressMonitor.UNKNOWN);

         IWorkspace          workspace          = ResourcesPlugin.getWorkspace();
         IWorkspaceRoot      wrkSpaceRoot       = workspace.getRoot();
         final IProject      newProjectHandle   = wrkSpaceRoot.getProject(projectName);

         IProjectDescription projectDescription = workspace.newProjectDescription(projectName);
         if ((directoryPath != null) && (!directoryPath.isEmpty())) {
            IPath path = new Path(directoryPath).append(projectName);
            projectDescription.setLocation(path);
         }
         project = CCorePlugin.getDefault().createCDTProject(projectDescription, newProjectHandle, monitor.newChild(80));     
         Assert.isNotNull(project, "Project not created");
         UsbdmProjectNature.addNature(project, monitor.newChild(5));

         if (hasCCNature) {
            CCProjectNature.addCCNature(project, monitor.newChild(5));
         }
         if (!(hasCCNature && !project.hasNature(CCProjectNature.CC_NATURE_ID))) {
            CCProjectNature.addCCNature(project, monitor.newChild(5));
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
   public IProject createUSBDMProject( Map<String, String> paramMap, IProgressMonitor progressMonitor) throws Exception {

      SubMonitor monitor = SubMonitor.convert(progressMonitor);

      // Create model project and accompanied descriptions
      IProject project;

      try {
         monitor.beginTask("Create configuration",100);

         String        projectName   = ReplacementParser.substitute(paramMap.get(UsbdmConstants.PROJECT_NAME_KEY), paramMap); 
         String        directoryPath = ReplacementParser.substitute(paramMap.get(UsbdmConstants.PROJECT_HOME_PATH_KEY), paramMap); 
         String        projectType   = ReplacementParser.substitute(paramMap.get(UsbdmConstants.PROJECT_OUTPUT_TYPE_KEY), paramMap);
         InterfaceType interfaceType = InterfaceType.valueOf(paramMap.get(UsbdmConstants.INTERFACE_TYPE_KEY));
         boolean       hasCCNature   = Boolean.valueOf(paramMap.get(UsbdmConstants.HAS_CC_NATURE_KEY));
         String        artifactName  = ReplacementParser.substitute(paramMap.get(UsbdmConstants.PROJECT_ARTIFACT_KEY), paramMap); 

         if ((artifactName == null) || (artifactName.length()==0)) {
            artifactName = "${ProjName}";
         }
         project = createProject(projectName, directoryPath, hasCCNature, monitor.newChild(70));

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
         if (!(hasCCNature && !project.hasNature(CCProjectNature.CC_NATURE_ID))) {
            CCProjectNature.addCCNature(project, monitor.newChild(5));
         }
         CoreModel.getDefault().updateProjectDescriptions(new IProject[]{project}, monitor);
         if (!(hasCCNature && !project.hasNature(CCProjectNature.CC_NATURE_ID))) {
            CCProjectNature.addCCNature(project, monitor.newChild(5));
         }
      } finally {
         monitor.done();
      }
      return project;
   }

}
