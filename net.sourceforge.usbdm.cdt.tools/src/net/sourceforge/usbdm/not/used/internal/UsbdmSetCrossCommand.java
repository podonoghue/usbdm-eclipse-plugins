package net.sourceforge.usbdm.not.used.internal;

//import net.sourceforge.usbdm.cdt.wizard.UsbdmConstants;

//import org.eclipse.cdt.build.core.scannerconfig.ScannerConfigBuilder;
import org.eclipse.cdt.core.templateengine.TemplateCore;
import org.eclipse.cdt.core.templateengine.process.ProcessArgument;
import org.eclipse.cdt.core.templateengine.process.ProcessFailureException;
import org.eclipse.cdt.core.templateengine.process.ProcessRunner;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
//import org.eclipse.cdt.managedbuilder.core.IOption;
//import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.core.runtime.NullProgressMonitor;

public class UsbdmSetCrossCommand extends ProcessRunner {

   @Override
   public void process(TemplateCore template, ProcessArgument[] args,
         String processId, IProgressMonitor monitor)
               throws ProcessFailureException {

      String projectName          = null; 
      String crossCommandPath     = null;   
      String crossCommandPrefix   = null;  

      for (ProcessArgument arg:args) {
         if (arg.getName().equals("projectName")) {
            projectName = arg.getSimpleValue();              // Name of project to get handle
         }
         else if (arg.getName().equals("crossCommandPath")) {
            crossCommandPath  = arg.getSimpleValue();        // Cross Command path
         }
         else if (arg.getName().equals("crossCommandPrefix")) {
            crossCommandPrefix  = arg.getSimpleValue();      // Cross Command prefix
         }
         else {
            throw new ProcessFailureException("UsbdmSetCrossCommand.process() - Unexpected argument \'"+arg.getName()+"\'"); //$NON-NLS-1$
         }
      }
      if ((projectName == null) || (crossCommandPath == null)|| (crossCommandPrefix == null)) {
         throw new ProcessFailureException("UsbdmSetCrossCommand.process() - Missing arguments"); //$NON-NLS-1$
      }     
      IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      if (!project.exists()) {
         return;
      }
      IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
      if (buildInfo == null) {
         return;
      }
//      IConfiguration[] configs = buildInfo.getManagedProject().getConfigurations();
//      for (IConfiguration config : configs) {
//         IToolChain toolchain = config.getToolChain();
//         IOption option;
//         System.err.println("UsbdmSetCrossCommand.process()");
//         option = toolchain.getOptionBySuperClassId(UsbdmConstants.USBDM_GCC_PREFIX_OPTION_KEY); //$NON-NLS-1$
//         System.err.println("UsbdmSetCrossCommand.process() UsbdmConstants.USBDM_GCC_PREFIX_OPTION_KEY => "+option);
//         ManagedBuildManager.setOption(config, toolchain, option, crossCommandPrefix);
//         option = toolchain.getOptionBySuperClassId(UsbdmConstants.USBDM_GCC_PATH_OPTION_KEY); //$NON-NLS-1$
//         System.err.println("UsbdmSetCrossCommand.process() UsbdmConstants.USBDM_GCC_PATH_OPTION_KEY => "+option);
//         ManagedBuildManager.setOption(config, toolchain, option, crossCommandPath);

         //         ICfgScannerConfigBuilderInfo2Set cbi = CfgScannerConfigProfileManager.getCfgScannerConfigBuildInfo(config);
         //         Map<CfgInfoContext, IScannerConfigBuilderInfo2> map = cbi.getInfoMap();
         //         for (CfgInfoContext cfgInfoContext : map.keySet()) {
         //             IScannerConfigBuilderInfo2 bi = map.get(cfgInfoContext);
         //             String providerId = "specsFile"; //$NON-NLS-1$
         //             String runCommand = bi.getProviderRunCommand(providerId);
         //             runCommand = crossCommandPrefix + runCommand;
         //             bi.setProviderRunCommand(providerId, runCommand);
         //             try {
         //                 bi.save();
         //             } catch (CoreException e) {
         //                System.err.println("Exception in SetCrossCommand.process()"+e.toString());
         //             }
         //             
         //             // Clear the path info that was captured at project creation time
         //             
         //             DiscoveredPathInfo pathInfo = new DiscoveredPathInfo(project);
         //             InfoContext infoContext = cfgInfoContext.toInfoContext();
         //             
         //             // 1. Remove scanner info from .metadata/.plugins/org.eclipse.cdt.make.core/Project.sc
         //             DiscoveredScannerInfoStore dsiStore = DiscoveredScannerInfoStore.getInstance();
         //             try {
         //                 dsiStore.saveDiscoveredScannerInfoToState(project, infoContext, pathInfo);
         //             } catch (CoreException e) {
         //                 e.printStackTrace();
         //             }
         //             
         //             // 2. Remove scanner info from CfgDiscoveredPathManager cache and from the Tool
         //             CfgDiscoveredPathManager cdpManager = CfgDiscoveredPathManager.getInstance();
         //             cdpManager.removeDiscoveredInfo(project, cfgInfoContext);
         //
         //             // 3. Remove scanner info from SI collector
         //             IScannerConfigBuilderInfo2 buildInfo2 = map.get(cfgInfoContext);
         //             if (buildInfo2!=null) {
         //                 ScannerConfigProfileManager scpManager = ScannerConfigProfileManager.getInstance();
         //                 String selectedProfileId = buildInfo2.getSelectedProfileId();
         //                 SCProfileInstance profileInstance = scpManager.getSCProfileInstance(project, infoContext, selectedProfileId);
         //                 
         //                 IScannerInfoCollector collector = profileInstance.getScannerInfoCollector();
         //                 if (collector instanceof IScannerInfoCollectorCleaner) {
         //                     ((IScannerInfoCollectorCleaner) collector).deleteAll(project);
         //                 }
         //                 buildInfo2 = null;
         //             }
         //         }
//      }

//      ManagedBuildManager.saveBuildInfo(project, true);
//
//      for (IConfiguration config : configs) {
//         ScannerConfigBuilder.build(config, ScannerConfigBuilder.PERFORM_CORE_UPDATE, new NullProgressMonitor());    
//      }
   }
}
