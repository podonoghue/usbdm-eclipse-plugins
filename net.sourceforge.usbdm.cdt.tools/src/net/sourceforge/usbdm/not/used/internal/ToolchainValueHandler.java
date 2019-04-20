/*
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!! NOT USED !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */


package net.sourceforge.usbdm.not.used.internal;

import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IManagedOptionValueHandler;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;

public class ToolchainValueHandler implements IManagedOptionValueHandler {

   @Override
   public boolean handleValue(IBuildObject configuration, IHoldsOptions holder,
         IOption option, String extraArgument, int event) {
      System.err.println("ToolchainValueHandler.handleValue");
      return false;
   }

   @Override
   public boolean isDefaultValue(IBuildObject configuration,
         IHoldsOptions holder, IOption option, String extraArgument) {
      System.err.println("ToolchainValueHandler.isDefaultValue");
      return  option.getId().toString().equals("net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.gnuToolsForARM");
   }

   void listConfigs(IBuildObject configuration) {
      String configTypeID = "net.sourceforge.usbdm.cdt.toolchain.processor.mcpu";
      
      System.err.println("ToolchainValueHandler.isEnumValueAppropriate()");
      // first we check the preference for this project, if it exists
      IConfiguration config = null;
      if (configuration instanceof IConfiguration) {
         config = (IConfiguration) configuration;
      }
      else if(configuration instanceof IFolderInfo) {
         IFolderInfo folderInfo = (IFolderInfo) configuration;
         config = folderInfo.getParent();
      }  
      if (config == null) {
         System.err.println("ToolchainValueHandler.isEnumValueAppropriate() config not found");
      }
      IToolChain toolChain = config.getToolChain();
      ITool[] tools = toolChain.getTools();
      IOption CPUListOption = null;
      for (int i = 0; i < tools.length; i++) {
         System.err.println("ToolchainValueHandler.isEnumValueAppropriate() Checking tool: " + tools[i].getId());
         if (tools[i].getOptionBySuperClassId(configTypeID) != null) {
            CPUListOption = tools[i].getOptionBySuperClassId(configTypeID);
         }
      } 
      if (CPUListOption == null) {
         System.err.println("ToolchainValueHandler.isEnumValueAppropriate() CPUListOption not found");
      }
   }

   @Override
   public boolean isEnumValueAppropriate(IBuildObject configuration,
         IHoldsOptions holder, IOption option, String extraArgument,
         String enumValue) {
      String configTypeID = "net.sourceforge.usbdm.cdt.toolchain.processor.mcpu";
      
      System.err.println("ToolchainValueHandler.isEnumValueAppropriate()");
      // first we check the preference for this project, if it exists
      IConfiguration config = null;
      if (configuration instanceof IConfiguration) {
         config = (IConfiguration) configuration;
      }
      else if(configuration instanceof IFolderInfo) {
         IFolderInfo folderInfo = (IFolderInfo) configuration;
         config = folderInfo.getParent();
      }  
      if (config == null) {
         System.err.println("ToolchainValueHandler.isEnumValueAppropriate() config not found");
         return true;
      }
      IToolChain toolChain = config.getToolChain();
      ITool[] tools = toolChain.getTools();
      IOption CPUListOption = null;
      for (int i = 0; i < tools.length; i++) {
         System.err.println("ToolchainValueHandler.isEnumValueAppropriate() Checking tool: " + tools[i].getId());
         if (tools[i].getOptionBySuperClassId(configTypeID) != null) {
            CPUListOption = tools[i].getOptionBySuperClassId(configTypeID);
         }
      } 
      if (CPUListOption == null) {
         System.err.println("ToolchainValueHandler.isEnumValueAppropriate() CPUListOption not found");
      }
      return true;
      
   }
}
