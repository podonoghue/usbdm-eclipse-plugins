package net.sourceforge.usbdm.cdt.tools.scanners;

import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.managedbuilder.language.settings.providers.GCCBuiltinSpecsDetector;
import org.eclipse.core.runtime.CoreException;

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
//      System.out.println("UsbdmBuiltinSpecsDetector.getToolchainId() => id = \'" + toolChainId + "\'");
      return toolChainId;
   }

/* (non-Javadoc)
    * @see org.eclipse.cdt.managedbuilder.language.settings.providers.AbstractBuiltinSpecsDetector#resolveCommand(java.lang.String)
    */
   @Override
   protected String resolveCommand(String languageId) throws CoreException {
      String commandLine = super.resolveCommand(languageId);
//      System.out.println("UsbdmBuiltinSpecsDetector.resolveCommand() => commandLine = \'" + commandLine + "\'");
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
