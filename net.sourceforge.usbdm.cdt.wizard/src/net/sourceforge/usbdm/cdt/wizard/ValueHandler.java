package net.sourceforge.usbdm.cdt.wizard;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IManagedOptionValueHandler;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IToolChain;

public class ValueHandler implements IManagedOptionValueHandler {

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
      IOption option1 = holder.getOptionBySuperClassId(extraArgument);
      if (option1 != null) {
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - holder.getOptionBySuperClassId(extraArgument) = "+option1.toString());
      }
      else {
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - holder.getOptionBySuperClassId(extraArgument) = null");         
      }
      IOption option2 = holder.getOptionById(extraArgument);
      if (option2 != null) {
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - holder.getOptionById(extraArgument) = "+option2.toString());
      }
      else {
         System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - holder.getOptionById(extraArgument) = null");         
      }

      IToolChain toolchain;
      if (configuration instanceof IConfiguration) {
         IConfiguration theConfiguration = (IConfiguration)configuration;
         System.err.println("theConfiguration = " + theConfiguration.getName());
         toolchain = theConfiguration.getToolChain();
         IOption mcpuOption = toolchain.getOptionBySuperClassId(extraArgument); //$NON-NLS-1$
         if (mcpuOption != null) {
            String  mcpuOptionValue = (String)mcpuOption.getValue();
            System.err.println("cdt.managedbuild.option.gnu.cross.prefix = " + mcpuOption);
            try {
               option.setValue(mcpuOptionValue);
            } catch (BuildException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            }
         }
      }
      System.err.println("net.sourceforge.usbdm.cdt.wizard.ValueHandler() - value = "+option.getValue().toString());
      return false;
   }

   @Override
   public boolean isDefaultValue(IBuildObject configuration,
         IHoldsOptions holder, IOption option, String extraArgument) {
      try {
         return option.getStringValue().isEmpty();
      } catch (BuildException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      return false;
   }

   @Override
   public boolean isEnumValueAppropriate(IBuildObject configuration,
         IHoldsOptions holder, IOption option, String extraArgument,
         String enumValue) {
      // TODO Auto-generated method stub
      return false;
   }

}
