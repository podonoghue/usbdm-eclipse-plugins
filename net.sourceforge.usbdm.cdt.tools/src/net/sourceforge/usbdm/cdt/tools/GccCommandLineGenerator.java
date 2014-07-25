/*
 * Adds shared flags to command line flags
 */

package net.sourceforge.usbdm.cdt.tools;

import java.util.ArrayList;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedSettings;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;

public class GccCommandLineGenerator extends PrefixedCommandLineGenerator {

   // Shared flags that are added to the command line 
   private static final String optionKeys[] = {
      UsbdmConstants.USBDM_GCC_PROC_ARM_MCPU_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_ARM_MTHUMB_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_ARM_MFLOAT_ABI_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_ARM_MFPU_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_ARM_FSHORT_DOUBLE_OPTION_KEY,
      
      UsbdmConstants.USBDM_GCC_PROC_COLDFIRE_MCPU_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_COLDFIRE_MFLOAT_ABI_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_COLDFIRE_MFPU_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_COLDFIRE_FSHORT_DOUBLE_OPTION_KEY,
      
      UsbdmConstants.USBDM_GCC_DEBUG_LEVEL_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_FORMAT_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_OTHER_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_PROF_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_GPROF_OPTION_KEY,
   };

   /**
    * @param codesourceryPathVariableName - environment variable name containing path e.g. codesourcery_arm_prefix
    */
   public GccCommandLineGenerator(String buildToolOptionId) {
      super(buildToolOptionId);
   }

   private String[] addFlags(ITool tool, String[] flags) {
      
      IToolChain toolChain = getToolChain(tool);
      
      // Create list of new flags
      ArrayList<String> newFlags = new ArrayList<String>();
      for(String newFlag:optionKeys) {
         IOption option = toolChain.getOptionBySuperClassId(newFlag); //$NON-NLS-1$
         if (option == null) {
//            System.err.println("GccCommandLineGenerator.addFlags() - option("+newFlag+"): not found");
            continue;
         }
         try {
            String command  = option.getCommand();
            if (command == null) {
               command = "";
            }
            String value = null;
            if (option.getValueType() == IOption.ENUMERATED) {
               String enumId = option.getSelectedEnum();
               value = option.getEnumCommand(enumId);
            }
            else if (option.getValueType() == IOption.STRING) {
               value = option.getStringValue();
            }
            else if (option.getValueType() == IOption.BOOLEAN) {
               boolean booleanValue = option.getBooleanValue();
               if (!booleanValue) {
                  command = option.getCommandFalse();
               }
               option.getDefaultValue();
            }
            else {
               System.err.println("GccCommandLineGenerator.addFlags() - option("+newFlag+"): Unexpected option type");
               continue;
            }
            if (value == null) {
               value = "";
            }
            if ((command.isEmpty()) && (value.isEmpty())) {
               //                  System.err.println("GccCommandLineGenerator.addFlags() - option("+newFlag+"): Command && value ==> null/empty");
               continue;
            }
            String flag = "";
            if (command.contains("{value}")) {
               flag = command.replace("{value}", value);
            }
            else {
               flag = command + value;
            }
            //               System.err.println("GccCommandLineGenerator.addFlags() - option("+newFlag+") ==> \'"+flag+"\'");
            newFlags.add(flag);
         } catch (BuildException e) {
            e.printStackTrace();
         }
      }
      // Create combined list of newFlags + flags as array
      if (flags == null) {
         flags = new String[0];
      }
      String[] allFlags = new String[newFlags.size()+flags.length];
      int index = 0;
      StringBuffer allFlagsBuffer = new StringBuffer();
      boolean firstFlag = false;
      for(String flag:newFlags) {
         allFlags[index] = flag;
         if (!firstFlag) {
            allFlagsBuffer.append(" ");
         }
         allFlagsBuffer.append(flag);
         index++;
      }
      for(String flag:flags) {
         allFlags[index++] = flag; 
      }
      UsbdmSharedSettings.getSharedSettings().put(UsbdmSharedConstants.USBDM_COMPILER_FLAGS_VAR, allFlagsBuffer.toString());
//      String flag = UsbdmSharedSettings.getSharedSettings().get(UsbdmSharedConstants.USBDM_COMPILER_FLAGS_VAR);
      return allFlags;
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

      flags = addFlags(tool, flags);

      return super.generateCommandLineInfo(tool, commandName, flags, outputFlag,
            outputPrefix, outputName, inputResources, commandLinePattern);
   }
}
