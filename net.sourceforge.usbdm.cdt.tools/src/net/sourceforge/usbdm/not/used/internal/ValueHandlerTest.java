package net.sourceforge.usbdm.not.used.internal;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IManagedOptionValueHandler;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;

public class ValueHandlerTest implements IManagedOptionValueHandler {

   IOption listConfigs(IBuildObject configuration,
                       String       configTypeID) {
      
      System.err.println("ValueHandlerTest.listConfigs(configTypeID="+configTypeID+")");
      
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
         System.err.println("ValueHandlerTest.listConfigs() config not found");
      }
      IOption targetOption = null;
      IToolChain toolChain = config.getToolChain();
      System.err.println("ValueHandlerTest.listConfigs() Checking toolchain: " + toolChain.getId());
      if (toolChain.getOptionBySuperClassId(configTypeID) != null) {
         targetOption = toolChain.getOptionBySuperClassId(configTypeID);
         System.err.println("ValueHandlerTest.listConfigs() Checking toolchain: Found => " + targetOption.getValue().toString());
//         return targetOption;
      }
      ITool[] tools = toolChain.getTools();
      for (int i = 0; i < tools.length; i++) {
         System.err.println("ValueHandlerTest.listConfigs() Checking tool: " + tools[i].getId());
         if (tools[i].getOptionBySuperClassId(configTypeID) != null) {
            targetOption = tools[i].getOptionBySuperClassId(configTypeID);
            System.err.println("ValueHandlerTest.listConfigs() Checking tool: Found => " + targetOption.getValue().toString());
//            return targetOption;
         }
      } 
      if (targetOption == null) {
         System.err.println("ValueHandlerTest.listConfigs() targetOption not found");
      }
      return targetOption;
   }

   @Override
   public boolean handleValue(IBuildObject  configuration,
                              IHoldsOptions holder,
                              IOption       option, 
                              String        extraArgument, 
                              int           event) {
      
      System.err.println("====================================================================================================");
      System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - extraArgument = \'"+extraArgument+"\'");
      switch (event) {
      case EVENT_OPEN:
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - EVENT_OPEN");
         break;
      case EVENT_CLOSE:
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - EVENT_CLOSE");
         break;
      case EVENT_SETDEFAULT:
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - EVENT_SETDEFAULT");
         break;
      case EVENT_APPLY:
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - EVENT_APPLY");
         break;
      case EVENT_LOAD:
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - EVENT_APPLY");
         break;
      default:
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - Unexpected event "+event);
         break;
      }
      listConfigs(configuration, extraArgument);
      IOption option1 = holder.getOptionBySuperClassId(extraArgument);
      if (option1 != null) {
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - holder.getOptionBySuperClassId(extraArgument) => "+option1.getName());
      }
      else {
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - holder.getOptionBySuperClassId(extraArgument) = null");         
      }
      IOption option2 = holder.getOptionById(extraArgument);
      if (option2 != null) {
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - holder.getOptionById(extraArgument) = "+option2.getName());
      }
      else {
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - holder.getOptionById(extraArgument) = null");         
      }

      if (configuration instanceof IConfiguration) {
         IConfiguration theConfiguration = (IConfiguration)configuration;
         System.err.println("theConfiguration = " + theConfiguration.getName());
         IToolChain toolchain = theConfiguration.getToolChain();
         System.err.println("toolchain = " + toolchain.getName());
         IOption mcpuOption = toolchain.getOptionBySuperClassId(extraArgument); //$NON-NLS-1$
         if (mcpuOption != null) {
            System.err.println("cdt.managedbuild.option.gnu.cross.prefix mcpuOption.getName() => " + mcpuOption.getId());
            System.err.println("cdt.managedbuild.option.gnu.cross.prefix mcpuOption.getValue().toString() => " + mcpuOption.getValue().toString());
//            String  mcpuOptionValue = (String)mcpuOption.getValue();
//            try {
//               option.setValue(mcpuOptionValue);
//            } catch (BuildException e) {
//               e.printStackTrace();
//            }
         }
      }
      return false;
   }

   @Override
   public boolean isDefaultValue(IBuildObject configuration,
         IHoldsOptions holder, IOption option, String extraArgument) {
      try {
         return option.getStringValue().isEmpty();
      } catch (BuildException e) {
         e.printStackTrace();
      }
      return false;
   }

   @Override
   public boolean isEnumValueAppropriate(IBuildObject configuration,
         IHoldsOptions holder, IOption option, String extraArgument,
         String enumValue) {
      return false;
   }

}
