/*
 * !!!!!!!!!!!!!!!!!!!!!!!!! NOT USED !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */
package net.sourceforge.usbdm.not.used;

import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IManagedOptionValueHandler;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;

public class UsbdmOptionValueHandler implements IManagedOptionValueHandler {

   // Shared Variable manager
   private static IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
   
   private IValueVariable variable = null;

   public UsbdmOptionValueHandler() {
      if (manager == null) {
         manager = VariablesPlugin.getDefault().getStringVariableManager();
      }
   }
   
   private IValueVariable openVariable(String variableName) {
      variable = manager.getValueVariable(variableName);
      if (variable == null) {
         variable = manager.newValueVariable(variableName, "No description");
      }
      return variable;
   }
   private String getValue() {
      if (variable != null) {
         return variable.getValue();
      }
      return "";
   }
   private void setValue(String value) {
//      System.err.println("UsbdmOptionValueHandler.setValue()");
      if (variable != null) {
         variable.setValue(value);
//         System.err.println("UsbdmOptionValueHandler.setValue() - done");
      }
   }
   
   @Override
   public boolean handleValue(IBuildObject configuration, 
                              IHoldsOptions holder,
                              IOption option, 
                              String extraArgument, 
                              int event) {

      if (manager == null) {
         return false;
      }
      switch (event) {
      case EVENT_OPEN: // Load initial value
         openVariable(extraArgument);
         try {
            option.setValue(getValue());
         } catch (BuildException e) {
            e.printStackTrace();
         }
         break;
      case EVENT_CLOSE: // Config/resource deleted - no action
      case EVENT_SETDEFAULT: // option::defaultValue has been set
         break;
      case EVENT_APPLY: // Transfer the value of the option to store
         setValue(option.getValue().toString());
         break;
      case EVENT_LOAD:
         break;
      }
      return false;
   }

   @Override
   public boolean isDefaultValue(IBuildObject configuration,
         IHoldsOptions holder, IOption option, String extraArgument) {
      return false;
   }

   @Override
   public boolean isEnumValueAppropriate(IBuildObject configuration,
         IHoldsOptions holder, IOption option, String extraArgument,
         String enumValue) {
      return false;
   }

}
