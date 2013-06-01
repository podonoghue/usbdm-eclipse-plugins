/*
 * Basic ManagedCommandLineGenerator
 * 
 *  Adds GCC derivative prefix e.g. arm-eabi-none- to commandName
 *  
 */
package net.sourceforge.usbdm.cdt.wizard;

import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.internal.core.ResourceConfiguration;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;

public class PrefixedCommandLineGenerator extends ManagedCommandLineGenerator {

   private String codesourceryPrefixVariableName;

   /**
    * @param codesourceryPathVariableName - environment variable name containing path e.g. codesourcery_arm_prefix
    */
   public PrefixedCommandLineGenerator(String codesourceryPrefixVariableName) {
      this.codesourceryPrefixVariableName = codesourceryPrefixVariableName;
   }

   /**
    * @param command command name e.g. gcc
    * @return prefixed command name e.g. arm-none-eabi-gcc
    */
   protected String addPrefix(String command) {

      // Get prefix variable value
      IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();

      IValueVariable prefixVariable = null;
      if (manager != null) {
         prefixVariable = manager.getValueVariable(codesourceryPrefixVariableName);
      }
      String codesourceryPrefix = null;
      if (prefixVariable != null) {
         codesourceryPrefix = prefixVariable.getValue();
      }
      if (codesourceryPrefix == null) {
         System.err.println("PrefixedCommandLineGenerator.addPrefix() - codesourceryPrefix = null");
         return command;
      }
      return codesourceryPrefix.trim()+command.trim();
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

      return super.generateCommandLineInfo(tool, addPrefix(commandName), flags, outputFlag,
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

//      Object parent = tool.getParent();
//      while ((parent != null) && (!(parent instanceof IToolChain)))
//      {
//         Object oSuper = tool.getSuperClass();
//         if ((oSuper != null) && ((oSuper instanceof ITool)))
//            parent = ((ITool)oSuper).getParent();
//         else {
//            parent = null;
//         }
//      }      
   }
   //private String addPrefix(ITool tool, String command) {
   //
   // IToolChain toolchain = getToolChain(tool);
   //
   // IOption option = toolchain.getOptionBySuperClassId(UsbdmConstants.USBDM_GCC_PREFIX_OPTION_KEY); //$NON-NLS-1$
   // String commandPrefix = "";
   // if (option != null) {
   //    try {
   //       commandPrefix = option.getStringValue();
   //    } catch (BuildException e) {
   //       // Ignore
   //       //             e.printStackTrace();
   //    }
   // }
   // return commandPrefix.trim()+command.trim();
   //}   
}
