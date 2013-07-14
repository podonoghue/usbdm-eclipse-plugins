package net.sourceforge.usbdm.cdt.tools;

import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;

public class ColdfireConfigurationEnvironmentVariableSupplier extends
      ConfigurationEnvironmentVariableSupplier implements IConfigurationEnvironmentVariableSupplier{

   public ColdfireConfigurationEnvironmentVariableSupplier() {
      super(UsbdmConstants.COLDFIRE_BUILDTOOLS_OPTIONS);
   }
}
