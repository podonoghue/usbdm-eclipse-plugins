package net.sourceforge.usbdm.not.used.internal;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.constants.ToolInformationData;
import net.sourceforge.usbdm.constants.UsbdmSharedSettings;

import org.eclipse.cdt.make.core.scannerconfig.IExternalScannerInfoProvider;
import org.eclipse.cdt.make.internal.core.scannerconfig2.GCCSpecsRunSIProvider;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.ResourceConfiguration;
import org.eclipse.core.resources.IProject;

@SuppressWarnings("restriction")
public class UsbdmGCCSpecsRunSIProvider extends GCCSpecsRunSIProvider implements
      IExternalScannerInfoProvider {

   protected IToolChain getToolChain(ITool tool) {
      IBuildObject parent = tool.getParent();
      if(parent instanceof ResourceConfiguration) {
         return ((ResourceConfiguration)parent).getBaseToolChain();
      }
      else {
         return (IToolChain)parent;
      }      
   }

   /**
    * @param project       The project to look in for options
    * @return              The prefix for the build tools
    */
   private String getToolPrefix(IProject project) {
      IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
      if (info == null) {
         return "";
      }
      IConfiguration cfg = info.getDefaultConfiguration();
//      System.err.println("UsbdmGCCSpecsRunSIProvider.initialize() Found IConfiguration = " + cfg.getName());

      IToolChain toolChain = cfg.getToolChain();
//      System.err.println("UsbdmGCCSpecsRunSIProvider.initialize() Found toolChain = " + toolChain.getName());
      
      // Find selected build tool (either ARM or Coldfire)
      IOption buildToolOption = toolChain.getOptionBySuperClassId(UsbdmConstants.ARM_BUILDTOOLS_OPTIONS);
      if (buildToolOption == null) {
         buildToolOption = toolChain.getOptionBySuperClassId(UsbdmConstants.COLDFIRE_BUILDTOOLS_OPTIONS);
      }
      if (buildToolOption == null) {
         return "";
      }
      
      // Get build path variable
      ToolInformationData toolData = ToolInformationData.getToolInformationTable().get(buildToolOption.getValue().toString());
      if (toolData == null) {
         return "";
      }
      String toolPrefixVariableId = toolData.getPrefixVariableName();
      if (toolPrefixVariableId == null) {
         return "";
      }
      UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();
      String toolPrefix = null;
      if (settings != null) {
         toolPrefix = settings.get(toolPrefixVariableId);
      }
      if (toolPrefix == null) {
         toolPrefix = "Tool Prefix not set";
         return "";
      }
//      System.err.println("UsbdmGCCSpecsRunSIProvider.initialize() Found tool prefix = " + toolPrefix);
      return toolPrefix;

   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.make.internal.core.scannerconfig2.GCCSpecsRunSIProvider#initialize()
    */
   @Override
   protected boolean initialize() {
      if (!super.initialize()) {
         return false;
      }

      // Add tool prefix to command
      System.err.println("UsbdmGCCSpecsRunSIProvider.initialize() fCompileCommand(Before) = \'" + fCompileCommand.toPortableString() + "\'");
      String command = fCompileCommand.lastSegment();
      if (command != null) {
         command = getToolPrefix(resource.getProject())+command;
         fCompileCommand = fCompileCommand.removeLastSegments(1).append(command);
      }
      System.err.println("UsbdmGCCSpecsRunSIProvider.initialize() fCompileCommand(After) = \'" + fCompileCommand.toPortableString() + "\'");
      return true;
   }
}
