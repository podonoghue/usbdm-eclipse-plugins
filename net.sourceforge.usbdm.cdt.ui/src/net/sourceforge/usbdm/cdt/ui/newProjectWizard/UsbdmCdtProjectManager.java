package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.util.Map;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.index.IndexerSetupParticipant;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.cdt.tools.UsbdmProjectNature;
import net.sourceforge.usbdm.cdt.ui.Activator;
import net.sourceforge.usbdm.cdt.ui.actions.ProcessProjectActions;
import net.sourceforge.usbdm.cdt.utilties.ReplacementParser;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.packageParser.ProjectActionList;

//@SuppressWarnings("restriction")
@SuppressWarnings("restriction")
public class UsbdmCdtProjectManager {

   private static final String ARM_CONFIGURATION_ID        = "net.sourceforge.usbdm.cdt.arm";
   private static final String COLDFIRE_CONFIGURATION_ID   = "net.sourceforge.usbdm.cdt.coldfire";

   /**
    * Used to suppress indexing whole the project is created.
    */
   private static class MyIndexerSetupParticipant extends IndexerSetupParticipant {
      IProject fProject;

      /**
       * Create participant
       * 
       * @param project The project to defer indexing on
       */
      MyIndexerSetupParticipant(IProject project) {
         fProject = project;
      };

      /**
       * Check if indexing is to be postponed on this project
       * 
       * @param cProject Project to check
       */
      public boolean postponeIndexerSetup(ICProject cProject) {
         return ((fProject != null) && (fProject == cProject.getProject()));
      }

      /**
       * Indicate that indexing is to resume
       * 
       * @param cProject Project to modify
       */
      public void notify(ICProject cProject) {
         if(fProject != null && fProject == cProject.getProject()) {
            notifyIndexerSetup(cProject);
            fProject = null;
         }
      }
   };

   /**
    * Create the USBDM project
    * 
    * @param paramMap            Parameters for the project
    * @param projectActionList  Actions to create the project
    * @param progressMonitor     Progress monitor
    * @param device              Device used with the project
    * 
    * @return Created project
    */
   public static IProject createUsbdmProject(
         Map<String, String>  paramMap, 
         ProjectActionList    projectActionList, 
         IProgressMonitor     progressMonitor, 
         Device               device) {
      
      final int WORK_SCALE = 1000;
      SubMonitor monitor   = SubMonitor.convert(progressMonitor, WORK_SCALE);

      monitor.beginTask("Creating USBDM Project", WORK_SCALE*100);

      // Create project
      IProject                  project               = null;
      ICProject                 cProject              = null;
      MyIndexerSetupParticipant indexSetupParticipant = null;
      IIndexManager             indexMgr              = CCorePlugin.getIndexManager();

      // Create model project and accompanied descriptions
      try {
         monitor.beginTask("Create configuration", 100);

         String        projectName       = ReplacementParser.substitute(paramMap.get(UsbdmConstants.PROJECT_NAME_KEY), paramMap); 
         String        directoryPath     = ReplacementParser.substitute(paramMap.get(UsbdmConstants.PROJECT_HOME_PATH_KEY), paramMap); 
         String        projectType       = ReplacementParser.substitute(paramMap.get(UsbdmConstants.PROJECT_OUTPUT_TYPE_KEY), paramMap);
         InterfaceType interfaceType     = InterfaceType.valueOf(paramMap.get(UsbdmConstants.INTERFACE_TYPE_KEY));
         boolean       hasCCNature       = Boolean.valueOf(paramMap.get(UsbdmConstants.HAS_CC_NATURE_KEY));
         String        artifactName      = ReplacementParser.substitute(paramMap.get(UsbdmConstants.PROJECT_ARTIFACT_KEY), paramMap); 
         String        mainlineFilename  = ReplacementParser.substitute(paramMap.get(UsbdmConstants.PROJECT_MAINLINE_FILE), paramMap); 
         
         if ((artifactName == null) || (artifactName.length()==0)) {
            artifactName = "${ProjName}";
         }

         monitor.beginTask("Creating project", IProgressMonitor.UNKNOWN);

         IWorkspace          workspace          = ResourcesPlugin.getWorkspace();
         IWorkspaceRoot      wrkSpaceRoot       = workspace.getRoot();
         
         project = wrkSpaceRoot.getProject(projectName);

         indexSetupParticipant = new MyIndexerSetupParticipant(project);
         indexMgr.addIndexerSetupParticipant(indexSetupParticipant);

         IProjectDescription projectDescription = workspace.newProjectDescription(projectName);
         if ((directoryPath != null) && (!directoryPath.isEmpty())) {
            IPath path = new Path(directoryPath).append(projectName);
            projectDescription.setLocation(path);
         }
         
         monitor.beginTask("Creating C project", IProgressMonitor.UNKNOWN);
         if (!project.exists()) {
            project.create(projectDescription, monitor);
         }
         // Open first
         project.open(monitor);

         // Add C Nature
         CProjectNature.addCNature(project, monitor.newChild(5));
         UsbdmProjectNature.addCNature(project, monitor.newChild(5));

         if (hasCCNature) {
            CCProjectNature.addCCNature(project, monitor.newChild(5));
            UsbdmProjectNature.addCCNature(project, monitor.newChild(5));
         }
         CoreModel coreModel = CoreModel.getDefault();

         // Create project description
         ICProjectDescription icProjectDescription = coreModel.createProjectDescription(project, true);
         Assert.isNotNull(icProjectDescription, "icProjectDescription null");

         IProjectType     type = ManagedBuildManager.getProjectType(projectType);
         Assert.isNotNull(type, "project type not found");

         Assert.isNotNull(project, "project is null");

//         IManagedProject mProj = new ManagedProject(project, type);
         
         // Note: Create build info before create managed project 
         IManagedBuildInfo info  = ManagedBuildManager.createBuildInfo(project);
         Assert.isNotNull(info, "info is null");
         
         IManagedProject   mProj = ManagedBuildManager.createManagedProject(project, type);
         Assert.isNotNull(mProj, "mProj is null");

         info.setManagedProject(mProj);
         
         String configurationName = null;
         switch (interfaceType) {
            case T_ARM:  configurationName = ARM_CONFIGURATION_ID;      break;
            case T_CFV1:
            case T_CFVX: configurationName = COLDFIRE_CONFIGURATION_ID; break;
         }

         System.err.println("isNewStyleProject = " + coreModel.isNewStyleProject(icProjectDescription));
//         
         // Create configurations for project
         IConfiguration cfgs[] = type.getConfigurations();
         for (IConfiguration configuration : cfgs) {
            String configId = configuration.getId();
            if (!configId.startsWith(configurationName)) {
               System.err.println("Skipping configuration '" + configId + "'");
               continue;
            }
            System.err.println("Creating configuration '" + configId + "'");
            
            String id = ManagedBuildManager.calculateChildId(configuration.getId(), null);
            
            Configuration config = new Configuration((ManagedProject) mProj, (Configuration)configuration, id, false, true, false);
            config.setArtifactName(artifactName);
            CConfigurationData data = config.getConfigurationData();
            Assert.isNotNull(data, "data is null for created configuration");
            
            icProjectDescription.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
         }
         
//         // Create configurations for project
//         IConfiguration cfgs[] = type.getConfigurations();
//         for (IConfiguration configuration : cfgs) {
//            String configId = configuration.getId();
//            if (!configId.startsWith(configurationName)) {
//               continue;
//            }
//            String id = ManagedBuildManager.calculateChildId(configuration.getId(), null);
//            
//            Configuration config = new Configuration((ManagedProject) mProj, (Configuration)configuration, id, false, true, false);
//            config.setArtifactName(artifactName);
//            CConfigurationData data = config.getConfigurationData();
//            
//            Assert.isNotNull(data, "data is null for created configuration");
//            
//            icProjectDescription.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
//         }
//         
//         // Create configurations for project
//         IConfiguration cfgs[] = type.getConfigurations();
//         for (IConfiguration configuration : cfgs) {
//            String configId = configuration.getId();
//            if (!configId.startsWith(configurationName)) {
//               continue;
//            }
//            String id = ManagedBuildManager.calculateChildId(configuration.getId(), null);
//            
//            Configuration config = new Configuration((ManagedProject) mProj, (Configuration)configuration, id, false, true, false);
//            config.setArtifactName(artifactName);
//            CConfigurationData data = config.getConfigurationData();
//            
//            Assert.isNotNull(data, "data is null for created configuration");
//            
//            icProjectDescription.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
//         }
//         
//         CCorePlugin.getDefault().createCDTProject(projectDescription, project, null, progressMonitor);

         
         
//         ICConfigurationDescription confDes = icProjectDescription.getDefaultSettingConfiguration();
         
//         // Create configurations for project
//         for (IConfiguration configuration : type.getConfigurations()) {
//            
//            String configId = configuration.getId();
//            if (!configId.startsWith(configurationName)) {
//               System.err.println("Skipping configuration '" + configId + "'");
//               continue;
//            }
//            System.err.println("Creating configuration '" + configId + "'");
//            
//            String id = ManagedBuildManager.calculateChildId(configId, null);
//            
//            CConfigurationData data  = configuration.getConfigurationData();
//            System.err.println("data = " + data);
//            
//            ICConfigurationDescription desc = icProjectDescription.getDefaultSettingConfiguration();
//
//          Configuration config = new Configuration((ManagedProject) mProj, (Configuration)configuration, id, false, true, false);
//
////            ICConfigurationDescription desc = ManagedBuildManager.getDescriptionForConfiguration(configuration);
//            System.err.println("desc = " + desc);
//            
//            icProjectDescription.createConfiguration(id, configuration.getName(), desc);
//            
////            icProjectDescription.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, configuration.getConfigurationData());
//            System.err.println("Created configuration " + configurationName);
//
//            
////            Configuration config = new Configuration((ManagedProject) mProj, (Configuration)configuration, id, false, true, false);
////            config.setArtifactName(artifactName);
////            CConfigurationData data = config.getConfigurationData();
////            Assert.isNotNull(data, "data is null for created configuration");
////            
////            icProjectDescription.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
//            
//         }
         
         Assert.isTrue(icProjectDescription.getConfigurations().length > 0, "No Configurations!");

         coreModel.setProjectDescription(project, icProjectDescription);
         
         CoreModel.getDefault().updateProjectDescriptions(new IProject[]{project}, monitor);

         if (hasCCNature) {
            CCProjectNature.addCCNature(project, monitor.newChild(5));
            UsbdmProjectNature.addCCNature(project, monitor.newChild(5));
         }

         // Apply device project options etc
         ProcessProjectActions.process(project, device, projectActionList, paramMap, monitor.newChild(WORK_SCALE * 20));

         // Generate CPP code as needed
         DeviceInfo.generateFiles(project, device, monitor.newChild(WORK_SCALE * 5));

         project.refreshLocal(IResource.DEPTH_INFINITE, monitor.newChild(WORK_SCALE));

         if (hasCCNature && !project.hasNature(CCProjectNature.CC_NATURE_ID)) {
            CCProjectNature.addCCNature(project, monitor.newChild(WORK_SCALE));
         }
         
         CoreModel.getDefault().updateProjectDescriptions(new IProject[]{project}, monitor.newChild(WORK_SCALE));

         // Allow indexing and re-index
         final IIndexManager indexManager = CCorePlugin.getIndexManager();

         cProject = CoreModel.getDefault().create(project);
         monitor.subTask("Refreshing Index...");
         indexManager.reindex(cProject);
         indexManager.joinIndexer(IIndexManager.FOREVER, monitor.newChild(WORK_SCALE)); 

         // Open main-line file in editor
         final IFile mainlineFile = project.getFile("Sources/"+mainlineFilename);
         if (mainlineFile.exists()) {
            Display.getDefault().syncExec(new Runnable() {
               @Override
               public void run() {
                  try {
                     // Open main-line file
                     IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                     IDE.openEditor( page, mainlineFile );
                  } catch (Exception e) {
                     Activator.logError(e.getMessage(), e);
                  }
               }
            });
         }
      } catch (Exception e) {
         Activator.logError(e.getMessage(), e);
         
      } finally {
         // Disable and remove indexer participant if added
         if (indexSetupParticipant != null) {
            if (cProject != null) {
               indexSetupParticipant.notify(cProject);
            }
            indexMgr.removeIndexerSetupParticipant(indexSetupParticipant);
            indexSetupParticipant = null;
         }
         monitor.done();
      }
      return project;
   }
}
