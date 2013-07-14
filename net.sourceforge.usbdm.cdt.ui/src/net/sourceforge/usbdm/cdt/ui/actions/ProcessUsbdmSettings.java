package net.sourceforge.usbdm.cdt.ui.actions;

import java.util.Iterator;

import net.sourceforge.usbdm.cdt.tools.UsbdmBuildException;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.Device.GnuInfo;
import net.sourceforge.usbdm.deviceDatabase.Device.GnuInfoList;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;

import org.eclipse.cdt.build.core.scannerconfig.ScannerConfigBuilder;
import org.eclipse.cdt.core.templateengine.TemplateCore;
import org.eclipse.cdt.core.templateengine.process.ProcessArgument;
import org.eclipse.cdt.core.templateengine.process.ProcessFailureException;
import org.eclipse.cdt.core.templateengine.process.ProcessRunner;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IResourceConfiguration;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Adds project settings from USBDM device database
 * 
 * @author PODonoghue
 *
 */
public class ProcessUsbdmSettings extends ProcessRunner {

   /* 
    * (non-Javadoc)
    * @see org.eclipse.cdt.core.templateengine.process.ProcessRunner#process(org.eclipse.cdt.core.templateengine.TemplateCore, org.eclipse.cdt.core.templateengine.process.ProcessArgument[], java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
    */
   @Override
   public void process(
         TemplateCore template, 
         ProcessArgument[] args,
         String processId, IProgressMonitor monitor)
         throws ProcessFailureException {

//      System.err.println("ProcessUsbdmSettings.process()"); //$NON-NLS-1$
      
      String projectName          = null;
      String targetDeviceFamily   = null;
      String targetDevice         = null;

      for (ProcessArgument arg:args) {
         if (arg.getName().equals("projectName")) {
            projectName = arg.getSimpleValue();              // Name of project to get handle
         }
         else if (arg.getName().equals("targetDeviceFamily")) {
            targetDeviceFamily  = arg.getSimpleValue();      // Target device family e.g. CFV1 etc
         }
         else if (arg.getName().equals("targetDevice")) {
            targetDevice  = arg.getSimpleValue();            // Target device name
         }
         else {
            throw new ProcessFailureException("ProcessUsbdmSettings.process() - Unexpected argument \'"+arg.getName()+"\'"); //$NON-NLS-1$
         }
      }
      if ((projectName == null) || (targetDeviceFamily == null) || (targetDevice == null)) {
         throw new ProcessFailureException("ProcessUsbdmSettings.process() - Missing arguments"); //$NON-NLS-1$
      }     
      InterfaceType deviceType = InterfaceType.valueOf(targetDeviceFamily);
      DeviceDatabase deviceDatabase = new DeviceDatabase(deviceType.deviceFile);
      if (!deviceDatabase.isValid()) {
         throw new ProcessFailureException("Device database failed to load"); //$NON-NLS-1$ //$NON-NLS-2$
      }
      Device device = deviceDatabase.getDevice(targetDevice);
      if (device == null) {
         throw new ProcessFailureException("Device \""+targetDevice+"\" not found in database"); //$NON-NLS-1$ //$NON-NLS-2$
      }
      IProject projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
//      System.err.println("ProcessUsbdmSettings.process("+device.getName()+")");
      GnuInfoList gnuInfoMap = device.getGnuInfoMap();
      if (gnuInfoMap != null) {
//         System.err.println("ProcessUsbdmSettings.process() - has map");
         Iterator<GnuInfo> it = gnuInfoMap.iterator();
         while(it.hasNext()) {
            GnuInfo gnuInfo = it.next();
//            System.err.println("ProcessUsbdmSettings.process() - "+gnuInfo.toString());
            try {
               setOptionValue(projectHandle, gnuInfo.getId(), gnuInfo.getValue(), gnuInfo.getPath());
            } catch (BuildException e) {
               e.printStackTrace();
            }
         }
      }     
   }
   
   private boolean setOptionValue(IProject projectHandle, String id, String value, String path) throws BuildException, UsbdmBuildException, ProcessFailureException {
      IConfiguration[] projectConfigs = ManagedBuildManager.getBuildInfo(projectHandle).getManagedProject().getConfigurations();

      boolean resource = !(path == null || path.equals("") || path.equals("/")); //$NON-NLS-1$ //$NON-NLS-2$
      boolean modified = false;

//      System.err.println("ProcessUsbdmSettings.setOptionValue(\n\t\t\t\t id="+id+",\n\t\t\t\t value="+value+",\n\t\t\t\t path="+path);

      for (IConfiguration config : projectConfigs) {
         IResourceConfiguration resourceConfig = null;
         if (resource) {
            resourceConfig = config.getResourceConfiguration(path);
            if (resourceConfig == null) {
               IFile file = projectHandle.getFile(path);
               if (file == null) {
                  throw new UsbdmBuildException("" + path); //$NON-NLS-1$
               }
               resourceConfig = config.createResourceConfiguration(file);
            }
            ITool[] tools = resourceConfig.getTools();
            for (ITool tool : tools) {
               modified |= addToOptionForResourceConfig(id, value, resourceConfig, tool.getOptions(), tool);
            }
         } else {
            IToolChain toolChain = config.getToolChain();
            modified |= addToOptionForConfig(id, value, config, toolChain.getOptions(), toolChain);

            ITool[] tools = config.getTools();
            for (ITool tool : tools) {
               modified |= addToOptionForConfig(id, value, config, tool.getOptions(), tool);
            }
         }
      }
      ManagedBuildManager.saveBuildInfo(projectHandle, true);
      
      for (IConfiguration config : projectConfigs) {
          ScannerConfigBuilder.build(config, ScannerConfigBuilder.PERFORM_CORE_UPDATE, new NullProgressMonitor());    
      }
      return modified;
   }

//   private boolean setOptionForResourceConfig(String id, String value, IResourceConfiguration resourceConfig, IOption[] options, IHoldsOptions optionHolder) throws BuildException {
//      boolean modified = false;
//      String lowerId = id.toLowerCase();
//      for (IOption option : options) {
//         if (option.getBaseId().toLowerCase().matches(lowerId)) {
//            int optionType = option.getValueType();
//            if ((optionType == IOption.STRING) || (optionType == IOption.ENUMERATED) || (optionType == IOption.TREE)) {
//               ManagedBuildManager.setOption(resourceConfig, optionHolder, option, value);
//               modified = true;
//            }
//         }
//      }
//      return modified;
//   }
//
//   private boolean setOptionForConfig(String id, String value, IConfiguration config, IOption[] options, IHoldsOptions optionHolder) throws BuildException {
//      boolean modified = false;
//      String lowerId = id.toLowerCase();
//      for (IOption option : options) {
//         if (option.getBaseId().toLowerCase().matches(lowerId)) {
//            int optionType = option.getValueType();
//            if ((optionType == IOption.STRING) || (optionType == IOption.ENUMERATED) || (optionType == IOption.TREE)) {
//               ManagedBuildManager.setOption(config, optionHolder, option, value);
//               modified = true;
//            }
//         }
//      }
//      return modified;
//   }
   
   private boolean addToOptionForResourceConfig(String id, String value, IResourceConfiguration resourceConfig, IOption[] options, IHoldsOptions optionHolder) 
         throws BuildException, ProcessFailureException {
      boolean modified = false;
      String lowerId = id.toLowerCase();
      for (IOption option : options) {
         if (option.getBaseId().toLowerCase().matches(lowerId)) {
            int optionType = option.getValueType();
            if ((optionType == IOption.STRING)) {
               String oldValue = option.getStringValue();
               String newValue = oldValue + value;
               ManagedBuildManager.setOption(resourceConfig, optionHolder, option, newValue);
               modified = true;
            }
            else if ((optionType == IOption.ENUMERATED)) {
               ManagedBuildManager.setOption(resourceConfig, optionHolder, option, value);
               modified = true;
            }
            else if ((optionType == IOption.BOOLEAN)) {
               ManagedBuildManager.setOption(resourceConfig, optionHolder, option, value.equals("true"));
               modified = true;
            }
            else {
               throw new ProcessFailureException("Unexpected option type"); //$NON-NLS-1$ //$NON-NLS-2$
            }
         }
      }
      return modified;
   }

   private boolean addToOptionForConfig(String id, String value, IConfiguration config, IOption[] options, IHoldsOptions optionHolder) 
         throws BuildException, ProcessFailureException {
      boolean modified = false;
      String lowerId = id.toLowerCase();
      for (IOption option : options) {
         if (option.getBaseId().toLowerCase().matches(lowerId)) {
            int optionType = option.getValueType();
            if ((optionType == IOption.STRING)) {
               String oldValue = option.getStringValue();
               String newValue = oldValue + value;
               ManagedBuildManager.setOption(config, optionHolder, option, newValue);
               modified = true;
            
            }
            else if ((optionType == IOption.ENUMERATED)) {
               ManagedBuildManager.setOption(config, optionHolder, option, value);
               modified = true;
            }
            else if ((optionType == IOption.BOOLEAN)) {
               ManagedBuildManager.setOption(config, optionHolder, option, value.equals("true"));
               modified = true;
            }
            else {
               throw new ProcessFailureException("Unexpected option type"); //$NON-NLS-1$ //$NON-NLS-2$
            }
         }
      }
      return modified;
   }
}
