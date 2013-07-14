package net.sourceforge.usbdm.cdt.tools;

import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;

public class ArmConfigurationEnvironmentVariableSupplier extends
      ConfigurationEnvironmentVariableSupplier implements IConfigurationEnvironmentVariableSupplier{

   public ArmConfigurationEnvironmentVariableSupplier() {
      
      super(UsbdmConstants.ARM_BUILDTOOLS_OPTIONS);
   }
}
