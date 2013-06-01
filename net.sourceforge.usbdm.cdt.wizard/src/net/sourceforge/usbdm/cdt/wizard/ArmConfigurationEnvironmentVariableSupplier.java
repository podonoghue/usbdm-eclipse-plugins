package net.sourceforge.usbdm.cdt.wizard;

import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;

public class ArmConfigurationEnvironmentVariableSupplier extends
      ConfigurationEnvironmentVariableSupplier implements IConfigurationEnvironmentVariableSupplier{

   public ArmConfigurationEnvironmentVariableSupplier() {
      
      super(UsbdmConstants.CODESOURCERY_ARM_PATH_KEY);
   }
}
