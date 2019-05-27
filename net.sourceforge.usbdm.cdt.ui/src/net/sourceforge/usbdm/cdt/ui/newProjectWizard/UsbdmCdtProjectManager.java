package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.util.Map;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.index.IndexerSetupParticipant;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.cdt.ui.Activator;
import net.sourceforge.usbdm.cdt.ui.actions.ProcessProjectActions;
import net.sourceforge.usbdm.cdt.utilties.ReplacementParser;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.packageParser.ProjectActionList;

@SuppressWarnings({ "restriction", "unused" })
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
//         Activator.log("MyIndexerSetupParticipant(fProject = "+project+")");
         fProject = project;
      };

      /**
       * Check if indexing is to be postponed on this project
       * 
       * cProject Project to check
       */
      public boolean postponeIndexerSetup(ICProject cProject) {
         IProject project = cProject.getProject();
         return (fProject != null && fProject == cProject.getProject());
      }

      /**
       * Indicate that indexing is to resume
       * 
       * @param cProject Project to modify
       */
      public void notify(ICProject cProject) {
//         Activator.log("MyIndexerSetupParticipant.notify(fProject = "+cProject+")");
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

      IndexerSetupParticipant indexerParticipant = null;

      monitor.beginTask("Creating USBDM Project", WORK_SCALE*100);

      // Create project
      IProject                  project               = null;
      ICProject                 cProject              = null;
      MyIndexerSetupParticipant indexSetupParticipant = null;
      IIndexManager             indexMgr              = CCorePlugin.getIndexManager();

      // Create model project and accompanied descriptions
      try {
         //==================== Start - createUSBDMProject =========================

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

         // ================== Start - createProject =======================
         Activator.log(String.format("CDTProjectManager.createProject(%s, %s)", projectName, directoryPath));

         monitor.beginTask("Creating project", IProgressMonitor.UNKNOWN);

         IWorkspace          workspace          = ResourcesPlugin.getWorkspace();
         IWorkspaceRoot      wrkSpaceRoot       = workspace.getRoot();
         final IProject      newProjectHandle   = wrkSpaceRoot.getProject(projectName);

         indexSetupParticipant = new MyIndexerSetupParticipant(newProjectHandle);
         indexMgr.addIndexerSetupParticipant(indexSetupParticipant);

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
         // ================== End - createProject =======================

         CoreModel coreModel = CoreModel.getDefault();

         // Create project description
         ICProjectDescription icProjectDescription = coreModel.createProjectDescription(project, false);
         Assert.isNotNull(icProjectDescription, "createProjectDescription returned null");

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
            icProjectDescription.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
         }
         Assert.isTrue(icProjectDescription.getConfigurations().length > 0, "No Configurations!");

         coreModel.setProjectDescription(project, icProjectDescription);
         if (!(hasCCNature && !project.hasNature(CCProjectNature.CC_NATURE_ID))) {
            CCProjectNature.addCCNature(project, monitor.newChild(5));
         }
         CoreModel.getDefault().updateProjectDescriptions(new IProject[]{project}, monitor);
         if (!(hasCCNature && !project.hasNature(CCProjectNature.CC_NATURE_ID))) {
            CCProjectNature.addCCNature(project, monitor.newChild(5));
         }
         //==================== End - createUSBDMProject =========================

         // Apply device project options etc
         ProcessProjectActions.process(project, device, projectActionList, paramMap, monitor.newChild(WORK_SCALE * 20));

         // Generate CPP code as needed
         DeviceInfo.generateFiles(project, monitor.newChild(WORK_SCALE * 5));

         project.refreshLocal(IResource.DEPTH_INFINITE, monitor.newChild(WORK_SCALE));

         //         reindexProject(project, monitor.newChild(WORK_SCALE * 20));

         if (hasCCNature) {
            Activator.log("Last ditch adding CC nature");
            CCProjectNature.addCCNature(project, monitor.newChild(WORK_SCALE));
         }
         
         CoreModel.getDefault().updateProjectDescriptions(new IProject[]{project}, monitor.newChild(WORK_SCALE));

         // Allow indexing and re-index
         final IIndexManager indexManager = CCorePlugin.getIndexManager();

         cProject = CoreModel.getDefault().create(project);
         monitor.subTask("Refreshing Index...");
         indexManager.reindex(cProject);
         indexManager.joinIndexer(IIndexManager.FOREVER, monitor.newChild(WORK_SCALE)); 

         final IFile mainlineFile = project.getFile("Sources/"+mainlineFilename);
         
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
         
      } catch (Exception e) {
         Activator.logError(e.getMessage(), e);
         
      } finally {
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
