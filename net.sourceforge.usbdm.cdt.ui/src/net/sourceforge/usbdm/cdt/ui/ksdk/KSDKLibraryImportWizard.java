package net.sourceforge.usbdm.cdt.ui.ksdk;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiConsumer;

import org.eclipse.cdt.build.core.scannerconfig.ScannerConfigBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.cdt.ui.Activator;
import net.sourceforge.usbdm.cdt.ui.actions.ProcessProjectActions;
import net.sourceforge.usbdm.cdt.ui.newProjectWizard.UsbdmCdtProjectManager;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;
import net.sourceforge.usbdm.packageParser.ProjectActionList;
import net.sourceforge.usbdm.packageParser.SubstitutionMap;

/**
 * @author pgo
 *
 */
public class KSDKLibraryImportWizard extends Wizard implements INewWizard, IRunnableWithProgress {

   KSDKLibraryImportWizardPage kdsLibraryImportWizardPage = null;

   @Override
   public void init(IWorkbench workbench, IStructuredSelection selection) {
      setNeedsProgressMonitor(true);
      setDefaultPageImageDescriptor(UsbdmSharedConstants.getUsbdmIcon());
      
      IDialogSettings settings = null;
      Activator activator = Activator.getDefault();
      if (activator == null) {
         System.err.println("*************************** plugin is null *********************");
      }
      if (activator != null) {
         settings = activator.getDialogSettings();
         if (settings == null) {
            System.err.println("*************************** settings is null *********************");
         }
      }
      setDialogSettings(settings);
   }

   @Override
   public boolean performFinish() {
      try {
         kdsLibraryImportWizardPage.saveSettings();
         getContainer().run(false, true, this);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return true;
   }

   @Override
   public void addPages() {
      kdsLibraryImportWizardPage = new KSDKLibraryImportWizardPage();
         addPage(kdsLibraryImportWizardPage);
   }

   @Override
   public String getWindowTitle() {
      return "KDS Library Importer";
   }

   /**
    * Get device from name
    *    @param deviceName
    *    @return
    */
   private static Device getDevice(String deviceName) {
      DeviceDatabase deviceDatabase = DeviceDatabase.getDeviceDatabase(TargetType.T_ARM);
      if (!deviceDatabase.isValid()) {
         return null;
      }
      return deviceDatabase.getDevice(deviceName);
   }

   private void listParamMap(String title, final ISubstitutionMap paramMap) {
      System.err.println(title);
      paramMap.forEach(new BiConsumer<String, String>() {
         @Override
         public void accept(String key, String value) {
            if (key.equals("linkerInformation")) {
               return;
            }
            if (key.equals("cVectorTable")) {
               return;
            }
            System.err.println(String.format("%-50s => %-20s", key, value));
         }
      });
      System.err.println("================================================");
   }
   
   public void updateConfigurations(IProject project, IProgressMonitor monitor) {
      final int WORK_SCALE = 1000;
      ManagedBuildManager.saveBuildInfo(project, true);
      IConfiguration[] projectConfigs = ManagedBuildManager.getBuildInfo(project).getManagedProject().getConfigurations();
     
      try {
         monitor.beginTask("Update Configurations", WORK_SCALE*projectConfigs.length);
         for (IConfiguration config : projectConfigs) {
            ScannerConfigBuilder.build(config, ScannerConfigBuilder.PERFORM_CORE_UPDATE, monitor);
            monitor.worked(WORK_SCALE);    
         }
      } finally {
         monitor.done();
      }
   }

   @Override
   public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
      System.err.println("KSDKLibraryImportWizard.run()");
      
      SubMonitor monitor = SubMonitor.convert(progressMonitor);
      monitor.beginTask("Importing KDS Library", 100);

      ISubstitutionMap paramMap = new SubstitutionMap();
            
      try {
         kdsLibraryImportWizardPage.getPageData(paramMap);
         
         Device device = getDevice(paramMap.getSubstitutionValue(UsbdmConstants.TARGET_DEVICE_KEY));
         
         if (device == null) {
            throw new Exception("Failed to obtain device description for " + paramMap.getSubstitutionValue(UsbdmConstants.TARGET_DEVICE_KEY));
         }
         // Add device options
         ProjectActionList deviceActionList = device.getProjectActionList(paramMap);
//         UsbdmOptionsPanel.getPageData(paramMap, deviceActionLists);
         listParamMap("KSDKLibraryImportWizard.run() - paramMap =================================", paramMap);

         // Create project
         System.err.println("KSDKLibraryImportWizard.run() - Creating project");
         IProject project = UsbdmCdtProjectManager.createUsbdmProject(paramMap, deviceActionList, progressMonitor, device);
         
//         IProject project = new CDTProjectManager().createUSBDMProject(paramMap, monitor.newChild(30));
         
         // Apply default device project options
//         System.err.println("KSDKLibraryImportWizard.run() - Applying deviceActionLists");
//         ProcessProjectActions.process(project, device, deviceActionList, paramMap, monitor.newChild(30));
         
         // Apply Library options
         System.err.println("KSDKLibraryImportWizard.run() - Getting libraryActionList");
         ProjectActionList libraryActionList = kdsLibraryImportWizardPage.getProjectActionList();
         
         System.err.println("KSDKLibraryImportWizard.run() - Applying libraryActionList");
         ProcessProjectActions.process(project, device, libraryActionList, (ISubstitutionMap) paramMap, monitor.newChild(30));

         updateConfigurations(project, monitor.newChild(10));
         
      } catch (Exception e) {
         e.printStackTrace();
         throw new InvocationTargetException(e);
      } finally {
         monitor.done();
      }
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);

      // Instantiates and initialises the wizard
      KSDKLibraryImportWizard wizard = new KSDKLibraryImportWizard();
      wizard.init(null,null);
      
      // Instantiates the wizard container with the wizard and opens it
      WizardDialog dialog = new WizardDialog(shell, wizard);
      dialog.create();
      dialog.open();
   }
   
}
