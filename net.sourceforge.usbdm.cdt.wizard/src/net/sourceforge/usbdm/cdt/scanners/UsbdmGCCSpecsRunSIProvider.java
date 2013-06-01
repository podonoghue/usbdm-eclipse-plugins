package net.sourceforge.usbdm.cdt.scanners;

import org.eclipse.cdt.make.core.scannerconfig.IExternalScannerInfoProvider;
import org.eclipse.cdt.make.internal.core.scannerconfig2.GCCSpecsRunSIProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

public class UsbdmGCCSpecsRunSIProvider extends GCCSpecsRunSIProvider implements
      IExternalScannerInfoProvider {

   /* (non-Javadoc)
    * @see org.eclipse.cdt.make.internal.core.scannerconfig2.GCCSpecsRunSIProvider#initialize()
    */
   @Override
   protected boolean initialize() {
      if (!super.initialize()) {
         return false;
      }
      String command = fCompileCommand.toPortableString();
//      System.err.println("UsbdmGCCSpecsRunSIProvider.initialize() fCompileCommand(B) = \'" + fCompileCommand.toPortableString() + "\'");
      try {
         IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
         command = manager.performStringSubstitution(command);
      } catch (CoreException e1) {
         e1.printStackTrace();
      }         
      fCompileCommand = new Path(command);
//      System.err.println("UsbdmGCCSpecsRunSIProvider.initialize() fCompileCommand(A) = \'" + fCompileCommand.toPortableString() + "\'");
      return true;
   }
}
