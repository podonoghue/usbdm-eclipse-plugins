package net.sourceforge.usbdm.cdt.wizard;

import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;

public class ColdfireConfigurationEnvironmentVariableSupplier extends
      ConfigurationEnvironmentVariableSupplier implements IConfigurationEnvironmentVariableSupplier{

   public ColdfireConfigurationEnvironmentVariableSupplier() {
      super(UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY);
   }
}
