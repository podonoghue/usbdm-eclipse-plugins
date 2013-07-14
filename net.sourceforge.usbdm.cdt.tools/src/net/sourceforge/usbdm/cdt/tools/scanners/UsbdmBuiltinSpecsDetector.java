package net.sourceforge.usbdm.cdt.tools.scanners;

import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.managedbuilder.language.settings.providers.GCCBuiltinSpecsDetector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

public class UsbdmBuiltinSpecsDetector extends GCCBuiltinSpecsDetector implements
      ILanguageSettingsProvider {

   private final String toolChainId;
   
   public UsbdmBuiltinSpecsDetector(String toolChainId) {
      this.toolChainId = toolChainId;
   }
        
   /* (non-Javadoc)
    * @see org.eclipse.cdt.managedbuilder.language.settings.providers.GCCBuiltinSpecsDetector#getToolchainId()
    */
   public String getToolchainId() {
      System.err.println("UsbdmBuiltinSpecsDetector.getToolchainId() => id = \'" + toolChainId + "\'");
      return toolChainId;
   }

/* (non-Javadoc)
    * @see org.eclipse.cdt.managedbuilder.language.settings.providers.AbstractBuiltinSpecsDetector#resolveCommand(java.lang.String)
    */
   @Override
   protected String resolveCommand(String languageId) throws CoreException {
      String commandLine = super.resolveCommand(languageId);
      System.err.println("UsbdmBuiltinSpecsDetector.resolveCommand() => (Before)commandLine = \'" + commandLine + "\'");
      try {
         // Do variable substitution
         IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
         
//         IValueVariable variable;
//         variable = manager.getValueVariable(UsbdmConstants.CODESOURCERY_ARM_PATH_KEY);
//         if (variable != null) {
//            String codesourceryPath = variable.getValue();
//            codesourceryPath = codesourceryPath.replace('\\', '/');
//            System.err.println("UsbdmBuiltinSpecsDetector.resolveCommand() => ${"+UsbdmConstants.CODESOURCERY_ARM_PATH_KEY+"} = \'" + codesourceryPath + "\'");
//            commandLine = commandLine.replace("${"+UsbdmConstants.CODESOURCERY_ARM_PATH_KEY+"}", codesourceryPath+"/x/");
//         }
//         variable = manager.getValueVariable(UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY);
//         if (variable != null) {
//            String codesourceryPath = variable.getValue();
//            codesourceryPath = codesourceryPath.replace('\\', '/');
//            System.err.println("UsbdmBuiltinSpecsDetector.resolveCommand() => ${"+UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY+"} = \'" + codesourceryPath + "\'");
//            commandLine = commandLine.replace("${"+UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY+"}", codesourceryPath+"/x/");
//         }
         commandLine = manager.performStringSubstitution(commandLine);
      } catch (CoreException e1) {
         e1.printStackTrace();
      }
      System.err.println("UsbdmBuiltinSpecsDetector.resolveCommand() => (After)commandLine = \'" + commandLine + "\'");
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
