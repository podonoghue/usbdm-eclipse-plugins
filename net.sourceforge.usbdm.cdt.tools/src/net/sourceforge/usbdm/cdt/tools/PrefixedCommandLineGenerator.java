/*
 * Basic ManagedCommandLineGenerator
 * 
 *  Adds GCC derivative prefix e.g. arm-eabi-none- to commandName
 * 
 */
package net.sourceforge.usbdm.cdt.tools;

import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.internal.core.ResourceConfiguration;
import net.sourceforge.usbdm.constants.ToolInformationData;
import net.sourceforge.usbdm.constants.UsbdmSharedSettings;

@SuppressWarnings("restriction")
public class PrefixedCommandLineGenerator extends ManagedCommandLineGenerator {

   /**
    * The id of the build tool option that identifies the build tool e.g. Codesourcery etc
    */
   String buildToolOptionId = null;

   /**
    * @param buildToolOptionId - option id
    */
   public PrefixedCommandLineGenerator(String buildToolOptionId) {
      this.buildToolOptionId = buildToolOptionId;
   }

   /**
    * @param tool       The tool to look in for options
    * @return           The tool prefix e.g.arm-none-eabi-
    */
   private String getToolPrefix(ITool tool) {

      if (buildToolOptionId == null) {
//         System.err.println("PrefixedCmdLineGenerator.getToolPrefix() buildToolOptionId null");
         return "";
      }
      IToolChain toolChain = getToolChain(tool);
      if (toolChain == null) {
//         System.err.println("PrefixedCmdLineGenerator.getToolPrefix() Toolchain null");
         return "";
      }
//      System.err.println("PrefixedCmdLineGenerator.getToolPrefix() buildToolOptionId = " +buildToolOptionId);
//      System.err.println("PrefixedCmdLineGenerator.getToolPrefix() Checking toolchain: " + toolChain.getId());

      // Find selected build tool
      IOption buildToolOption = toolChain.getOptionBySuperClassId(buildToolOptionId);
//      if (buildToolOption != null) {
//         System.err.println("PrefixedCmdLineGenerator.getToolPrefix() Checking toolchain: Found name =  " + buildToolOption.getName());
//         System.err.println("PrefixedCmdLineGenerator.getToolPrefix() Checking toolchain: Found value = " + buildToolOption.getValue().toString());
//      }
      if (buildToolOption == null) {
         return "";
      }

      // Get build path variable
      ToolInformationData toolData = ToolInformationData.getToolInformationTable().get(buildToolOption.getValue().toString());
      if (toolData == null) {
         return null;
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
      }
//      System.err.println("PrefixedCmdLineGenerator.getToolPrefix() Found tool prefix = " + toolPrefix);
      
//
//      // Do variable substitution
//      IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
//
//      IValueVariable prefixVariable = null;
//      if (manager != null) {
//         prefixVariable = manager.getValueVariable(toolPrefixVariableId);
//      }
//      String toolPrefix = "";
//      if (prefixVariable != null) {
//         toolPrefix = prefixVariable.getValue();
////         System.err.println("PrefixedCmdLineGenerator.getToolPrefix() Found tool prefix = " + toolPrefix);
//      }
//      else {
//
//      }
      return toolPrefix;
   }

   /**
    * @param command command name e.g. gcc
    * @return prefixed command name e.g. arm-none-eabi-gcc
    */
   protected String addPrefix(ITool tool, String command) {

      String prefix = getToolPrefix(tool);
      return prefix+command.trim();
   }

   @Override
   public IManagedCommandLineInfo generateCommandLineInfo(
         ITool      tool,
         String     commandName,
         String[]   flags,
         String     outputFlag,
         String     outputPrefix,
         String     outputName,
         String[]   inputResources,
         String     commandLinePattern) {

      return super.generateCommandLineInfo(tool, addPrefix(tool, commandName), flags, outputFlag,
            outputPrefix, outputName, inputResources, commandLinePattern);
   }

   protected IToolChain getToolChain(ITool tool) {
      IBuildObject parent = tool.getParent();
      if(parent instanceof ResourceConfiguration) {
         return ((ResourceConfiguration)parent).getBaseToolChain();
      }
      else {
         return (IToolChain)parent;
      }
   }
}
