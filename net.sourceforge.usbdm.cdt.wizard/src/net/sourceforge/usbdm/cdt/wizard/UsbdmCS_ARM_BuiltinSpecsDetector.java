package net.sourceforge.usbdm.cdt.wizard;

import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.managedbuilder.language.settings.providers.GCCBuiltinSpecsDetector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;


public class UsbdmCS_ARM_BuiltinSpecsDetector extends GCCBuiltinSpecsDetector implements
      ILanguageSettingsProvider {

   /* (non-Javadoc)
    * @see org.eclipse.cdt.managedbuilder.language.settings.providers.GCCBuiltinSpecsDetector#getToolchainId()
    */
   static final String toolchainId = "net.sourceforge.usbdm.cdt.arm.toolchain";
//   static final String toolchainId = "net.sourceforge.usbdm.cdt.holder";
   @Override
   public String getToolchainId() {
      System.err.println("UsbdmCS_ARM_BuiltinSpecsDetector.getToolchainId() => id = \'" + toolchainId + "\'");
      return toolchainId;
//      return super.getToolchainId();
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.managedbuilder.language.settings.providers.AbstractBuiltinSpecsDetector#resolveCommand(java.lang.String)
    */
   @Override
   protected String resolveCommand(String languageId) throws CoreException {
      String commandLine = super.resolveCommand(languageId);
      System.err.println("UsbdmCS_ARM_BuiltinSpecsDetector.resolveCommand() => (B)commandLine = \'" + commandLine + "\'");
      try {
         // Do variable substitution
         IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
         
//         IValueVariable variable;
//         variable = manager.getValueVariable(UsbdmConstants.CODESOURCERY_ARM_PATH_KEY);
//         if (variable != null) {
//            String codesourceryPath = variable.getValue();
//            codesourceryPath = codesourceryPath.replace('\\', '/');
//            System.err.println("UsbdmCS_ARM_BuiltinSpecsDetector.resolveCommand() => ${"+UsbdmConstants.CODESOURCERY_ARM_PATH_KEY+"} = \'" + codesourceryPath + "\'");
//            commandLine = commandLine.replace("${"+UsbdmConstants.CODESOURCERY_ARM_PATH_KEY+"}", codesourceryPath+"/x/");
//         }
//         variable = manager.getValueVariable(UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY);
//         if (variable != null) {
//            String codesourceryPath = variable.getValue();
//            codesourceryPath = codesourceryPath.replace('\\', '/');
//            System.err.println("UsbdmCS_ARM_BuiltinSpecsDetector.resolveCommand() => ${"+UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY+"} = \'" + codesourceryPath + "\'");
//            commandLine = commandLine.replace("${"+UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY+"}", codesourceryPath+"/x/");
//         }
         commandLine = manager.performStringSubstitution(commandLine);
      } catch (CoreException e1) {
         e1.printStackTrace();
      }
      System.err.println("UsbdmCS_ARM_BuiltinSpecsDetector.resolveCommand() => (A)commandLine = \'" + commandLine + "\'");
      return commandLine;
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.managedbuilder.language.settings.providers.GCCBuiltinSpecsDetector#cloneShallow()
    */
   @Override
   public GCCBuiltinSpecsDetector cloneShallow()
         throws CloneNotSupportedException {
      return (GCCBuiltinSpecsDetector)super.cloneShallow();
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.managedbuilder.language.settings.providers.GCCBuiltinSpecsDetector#clone()
    */
   @Override
   public GCCBuiltinSpecsDetector clone() throws CloneNotSupportedException {
      return (GCCBuiltinSpecsDetector)super.clone();
   }
}
