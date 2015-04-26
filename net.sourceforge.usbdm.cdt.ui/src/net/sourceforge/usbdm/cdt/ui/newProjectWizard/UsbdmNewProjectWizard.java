package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.usbdm.cdt.ui.Activator;
import net.sourceforge.usbdm.cdt.ui.actions.ProcessProjectActions;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.packageParser.ProjectActionList;

import org.eclipse.cdt.build.core.scannerconfig.ScannerConfigBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * @author pgo
 *
 */
public class UsbdmNewProjectWizard extends Wizard implements INewWizard, IRunnableWithProgress {
   
   private UsbdmNewProjectPage_1              fUsbdmNewProjectPage_1        = null;
   private UsbdmProjectParametersPage_2       fUsbdmProjectParametersPage_2 = null;
   private UsbdmProjectOptionsPage_3          fUsbdmProjectOptionsPage_3    = null;

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
         fUsbdmNewProjectPage_1.saveSettings();
         fUsbdmProjectParametersPage_2.saveSettings();
         fUsbdmProjectOptionsPage_3.saveSettings();
         getContainer().run(false, true, this);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return true;
   }

   @Override
   public void addPages() {
      fUsbdmNewProjectPage_1  = new UsbdmNewProjectPage_1();
      addPage(fUsbdmNewProjectPage_1);
   }

   @Override
   public IWizardPage getStartingPage() {
      return fUsbdmNewProjectPage_1;
   }

   @Override
   public boolean canFinish() {
//      super.canFinish();
      return ((fUsbdmNewProjectPage_1 != null)        && fUsbdmNewProjectPage_1.isPageComplete()) &&
             ((fUsbdmProjectParametersPage_2 != null) && fUsbdmProjectParametersPage_2.isPageComplete()) &&
             ((fUsbdmProjectOptionsPage_3 != null)    && fUsbdmProjectOptionsPage_3.isPageComplete()) &&
             (getContainer() != null) && 
             ((getContainer().getCurrentPage() == fUsbdmProjectOptionsPage_3));
   }

   @Override
   public boolean needsPreviousAndNextButtons() {
      return true;
   }

   @Override
   public IWizardPage getNextPage(IWizardPage page) {
      if (page == fUsbdmNewProjectPage_1) {
         // Create new project page if none or interface has changed
         if (fUsbdmNewProjectPage_1.hasChanged() || (fUsbdmProjectParametersPage_2 == null)) {
            Map<String, String> paramMap = new HashMap<String, String>();
            fUsbdmNewProjectPage_1.getPageData(paramMap);
//            listParamMap("fUsbdmNewProjectPage_1 map\n===========================================", paramMap);
            fUsbdmProjectParametersPage_2 = new UsbdmProjectParametersPage_2(paramMap);
            fUsbdmProjectParametersPage_2.setWizard(this);
            fUsbdmProjectOptionsPage_3 = null;
         }
         return fUsbdmProjectParametersPage_2;
      }
      if (page == fUsbdmProjectParametersPage_2) {
         // Create new option page if none or device has changed
         if (fUsbdmNewProjectPage_1.hasChanged() || fUsbdmProjectParametersPage_2.hasChanged() ||
             (fUsbdmProjectOptionsPage_3 == null)) {
            Map<String, String> paramMap = new HashMap<String, String>();
            fUsbdmNewProjectPage_1.getPageData(paramMap);
            fUsbdmProjectParametersPage_2.getPageData(paramMap);
            
            
            
//            listParamMap("fUsbdmNewProjectPage_1&2 map\n===========================================", paramMap);
            fUsbdmProjectOptionsPage_3 = new UsbdmProjectOptionsPage_3(fUsbdmProjectParametersPage_2, paramMap);
            fUsbdmProjectOptionsPage_3.setWizard(this);
         }
         return fUsbdmProjectOptionsPage_3;
      }
      return null;
   }

   @Override
   public IWizardPage getPreviousPage(IWizardPage page) {
      if (page == fUsbdmProjectOptionsPage_3) {
         return fUsbdmProjectParametersPage_2;
      }
      if (page == fUsbdmProjectParametersPage_2) {
         return fUsbdmNewProjectPage_1;
      }
      return null;
   }

   private void listParamMap(String title, final Map<String, String> paramMap) {
      System.err.println(title);
      for (Entry<String, String> x:paramMap.entrySet()) {
         if (x.getKey().equals("linkerInformation")) {
            continue;
         }
         if (x.getKey().equals("cVectorTable")) {
            continue;
         }
         System.err.println(String.format("%-50s => %-20s", x.getKey(), x.getValue()));
      }
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
   public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
      final int WORK_SCALE = 1000;
      System.err.println("UsbdmNewProjectWizard.run()");

      Map<String, String> paramMap = new HashMap<String, String>(); 
            
      try {
         monitor.beginTask("Creating USBDM Project", WORK_SCALE*100);
	  
	  
         Device device = fUsbdmProjectParametersPage_2.getDevice();

         // Get project parameters from dialogue pages
         fUsbdmNewProjectPage_1.getPageData(paramMap);
         fUsbdmProjectParametersPage_2.getPageData(paramMap);
         fUsbdmProjectOptionsPage_3.getPageData(paramMap);
         
         listParamMap("UsbdmNewProjectWizard.run() - paramMap =================================", paramMap);
         
         // Create project
         IProject project = new CDTProjectManager().createCDTProj(paramMap, device, new SubProgressMonitor(monitor, WORK_SCALE*45));
      
         // Apply device project options
         ProjectActionList actionLists = fUsbdmProjectOptionsPage_3.getProjectActionLists();
         ProcessProjectActions.process(project, device, actionLists, paramMap, new SubProgressMonitor(monitor, WORK_SCALE*45));

         updateConfigurations(project, new SubProgressMonitor(monitor, WORK_SCALE*10));
         
      } catch (Exception e) {
         e.printStackTrace();
         throw new InvocationTargetException(e);
      } finally {
         monitor.done();
      }
   }
   
   @Override
   public IWizardPage getPage(String name) {
      if ((fUsbdmNewProjectPage_1 != null) && fUsbdmNewProjectPage_1.isPageComplete()) {
         return fUsbdmNewProjectPage_1;
      }
      if ((fUsbdmProjectParametersPage_2 != null) && fUsbdmProjectParametersPage_2.isPageComplete()) {
         return fUsbdmProjectParametersPage_2;
      }
      if ((fUsbdmProjectOptionsPage_3 != null) && fUsbdmProjectOptionsPage_3.isPageComplete()) {
         return fUsbdmProjectOptionsPage_3;
      }
      return null;
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);

      // Instantiates and initialises the wizard
      UsbdmNewProjectWizard wizard = new UsbdmNewProjectWizard();
      wizard.init(null,null);
      
      // Instantiates the wizard container with the wizard and opens it
      WizardDialog dialog = new WizardDialog(shell, wizard);
      dialog.create();
      dialog.open();
   }
   
}