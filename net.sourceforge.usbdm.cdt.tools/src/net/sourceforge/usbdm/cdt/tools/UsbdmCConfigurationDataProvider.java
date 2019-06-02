package net.sourceforge.usbdm.cdt.tools;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class UsbdmCConfigurationDataProvider
      extends org.eclipse.cdt.core.settings.model.extension.impl.CDefaultConfigurationDataProvider {
//      org.eclipse.cdt.core.settings.model.extension.CConfigurationDataProvider {

   public UsbdmCConfigurationDataProvider() {
   }

   @Override
   public CConfigurationData loadConfiguration(
         ICConfigurationDescription cfgDescription, 
         IProgressMonitor           monitor) throws CoreException {
      System.err.println("UsbdmCConfigurationDataProvider.loadConfiguration()");
      return super.loadConfiguration(cfgDescription, monitor);
   }

   @Override
   public CConfigurationData createConfiguration(
         ICConfigurationDescription    cfgDescription,
         ICConfigurationDescription    baseCfgDescription, 
         CConfigurationData            baseData, 
         boolean                       clone,
         IProgressMonitor monitor) throws CoreException {
      System.err.println("UsbdmCConfigurationDataProvider.createConfiguration()");
      return super.createConfiguration(cfgDescription, baseCfgDescription, baseData, clone, monitor);
   }

   @Override
   public void removeConfiguration(
         ICConfigurationDescription cfgDescription, 
         CConfigurationData         data,
         IProgressMonitor           monitor) {
      
      // TODO Auto-generated method stub
      System.err.println("UsbdmCConfigurationDataProvider.removeConfiguration()");
      super.removeConfiguration(cfgDescription, data, monitor);
   }

}
