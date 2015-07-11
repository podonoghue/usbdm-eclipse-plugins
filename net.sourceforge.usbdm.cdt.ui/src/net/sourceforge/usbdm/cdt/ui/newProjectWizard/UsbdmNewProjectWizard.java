package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.usbdm.cdt.ui.Activator;
import net.sourceforge.usbdm.cdt.ui.actions.ProcessProjectActions;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.packageParser.ProjectAction;
import net.sourceforge.usbdm.packageParser.ProjectActionList;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Value;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor.Result;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor.Result.Status;
import net.sourceforge.usbdm.packageParser.ProjectConstant;
import net.sourceforge.usbdm.packageParser.WizardPageInformation;

import org.eclipse.cdt.build.core.scannerconfig.ScannerConfigBuilder;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
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
 */
public class UsbdmNewProjectWizard extends Wizard implements INewWizard, IRunnableWithProgress {
   
   private UsbdmNewProjectPage_1                fUsbdmNewProjectPage_1 = null;
   private UsbdmProjectParametersPage_2         fUsbdmProjectParametersPage_2 = null;
   private ArrayList<UsbdmProjectOptionsPage_3> fWizardPages  = null;
   private ArrayList<WizardPageInformation>     fWizardPageInformation = new ArrayList<WizardPageInformation>();
   private ProjectActionList                    fProjectActionList = null;
   private Map<String, String>                  fBaseParamMap = null;
   private Map<String, String>                  fParamMap = null;
   private Device                               fDevice = null;

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
      if ((fUsbdmNewProjectPage_1 == null)        || !fUsbdmNewProjectPage_1.isPageComplete() ||
          (fUsbdmProjectParametersPage_2 == null) || !fUsbdmProjectParametersPage_2.isPageComplete() ||
          fWizardPages == null) {
         return false;
      }
      if (fWizardPages.size() != fWizardPageInformation.size()) {
         // Haven't visited all pages
         return false;
      }
      for (UsbdmProjectOptionsPage_3 page:fWizardPages) {
         if (!page.isPageComplete()) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean needsPreviousAndNextButtons() {
      return true;
   }

   /**
    * Updates fBaseParamMap from 1st two pages, and <br>
    * fParamMap from fBaseParamMap and buttons on dynamic pages
    */
   void updateParamMap() {
//      System.err.println("updateParamMap()");
      if (fBaseParamMap == null) {
         fBaseParamMap = new HashMap<String, String>();
         fUsbdmNewProjectPage_1.getPageData(fBaseParamMap);
         fUsbdmProjectParametersPage_2.getPageData(fBaseParamMap);
         fDevice = fUsbdmProjectParametersPage_2.getDevice();
         fProjectActionList = fDevice.getProjectActionList(fBaseParamMap);
      }
      fParamMap = new HashMap<String, String>(fBaseParamMap);
      if (fWizardPages != null) {
         for (UsbdmProjectOptionsPage_3 p:fWizardPages) {
            p.getButtonData(fParamMap);
         }
      }
      updateMapConstants(fParamMap, fProjectActionList);
//      listParamMap("fParamMap\n===========================================", fParamMap);
   }

   /**
    * Collects information about dynamic wizard pages
    */
   void updateDynamicWizardPageInformation() {
//      System.err.println("updateDynamicWizardPageInformation()");
      fWizardPageInformation = new ArrayList<WizardPageInformation>();
      Visitor visitor = new ProjectActionList.Visitor() {
         @Override
         public Result applyTo(ProjectAction action, ProjectActionList.Value result, IProgressMonitor monitor) {
            if (action instanceof WizardPageInformation) {
               WizardPageInformation page = (WizardPageInformation) action;
               fWizardPageInformation.add(page);
            }
            return CONTINUE;
         }
      };
      fProjectActionList.visit(visitor, null);
   }
   
   boolean hasChanged(IWizardPage currentPage) {
      boolean changed = false;
      if (fUsbdmNewProjectPage_1 != null) {
         changed = fUsbdmNewProjectPage_1.hasChanged();
      }
      if ((currentPage == fUsbdmNewProjectPage_1) || changed) {
         return changed;           
      }
      if (fUsbdmProjectParametersPage_2 != null) {
         changed = fUsbdmProjectParametersPage_2.hasChanged();
      }
      if ((currentPage == fUsbdmProjectParametersPage_2) || changed) {
         return changed;           
      }
      if (fWizardPages == null) {
         return false;
      }
      for (UsbdmProjectOptionsPage_3 page:fWizardPages) {
         if (page.hasChanged()) {
            return true;
         }
         if (page == currentPage) {
            return false;
         }
      }
      return false;
   }
   
   /**
    * Visits the nodes of the projectActionList and add constants found 
    * 
    * @param paramMap            Map to add constants to
    * @param projectActionList   Action list to visit
    */
   void updateMapConstants(final Map<String, String> paramMap, ProjectActionList projectActionList) {
      Visitor visitor = new Visitor() {
         @Override
         public Result applyTo(ProjectAction action, Value result, IProgressMonitor monitor) {
            try {
               if (action instanceof ProjectActionList) {
                  ProjectActionList projectActionList = (ProjectActionList) action;
                  return projectActionList.appliesTo(fUsbdmProjectParametersPage_2.getDevice(), paramMap)?CONTINUE:PRUNE;
               }
               else if (action instanceof ProjectConstant) {
                  ProjectConstant projectConstant = (ProjectConstant) action;
//                  System.err.println(String.format("updateMapConstants(): Found constant %s => %s",  projectConstant.getId(), projectConstant.getValue()));
                  String value = paramMap.get(projectConstant.getId());
                  if (value != null) {
                     if (projectConstant.isWeak()) {
                        // Ignore - assume constant is a default that has been overwritten
                        return CONTINUE;
                     }
                     if (!projectConstant.doReplace() && !value.equals(projectConstant.getValue())) {
                        return new Result(new Exception("paramMap already contains constant " + projectConstant.getId()));
                     }
                  }
                  paramMap.put(projectConstant.getId(), projectConstant.getValue());
               }
               return CONTINUE;
            } catch (Exception e) {
               return new Result(e);
            }
         }
      };
      // Visit all enabled actions and collect constants
      Result result = fProjectActionList.visit(visitor, null);
      if (result.getStatus() == Status.EXCEPTION) {
         result.getException().printStackTrace();
      }
   }
 
   @Override
   public IWizardPage getNextPage(IWizardPage currentPage) {
      System.err.println("getNextPage() " + currentPage.getTitle());
      if (currentPage == fUsbdmNewProjectPage_1) {
         // Create new project page if none or interface has changed
         if (hasChanged(currentPage) || (fUsbdmProjectParametersPage_2 == null)) {
            fBaseParamMap = new HashMap<String, String>();
            fUsbdmNewProjectPage_1.getPageData(fBaseParamMap);
            fUsbdmProjectParametersPage_2 = new UsbdmProjectParametersPage_2(fBaseParamMap);
            fUsbdmProjectParametersPage_2.setWizard(this);
         }
         return fUsbdmProjectParametersPage_2;
      }
      if (currentPage == fUsbdmProjectParametersPage_2) {
         // Create new option page if none or device has changed
         if (hasChanged(currentPage) || (fWizardPages == null)) {
//            System.err.println("creating fUsbdmProjectParametersPage_2");
            fWizardPages = null;
            fBaseParamMap = null;
            updateParamMap();
            updateDynamicWizardPageInformation();
            if (fWizardPageInformation.size()==0) {
               return null;
            }
            UsbdmProjectOptionsPage_3 newPage = new UsbdmProjectOptionsPage_3(fDevice, fProjectActionList, fBaseParamMap, fWizardPageInformation.get(0));
            newPage.setWizard(this);
            fWizardPages = new ArrayList<UsbdmProjectOptionsPage_3>();
            fWizardPages.add(newPage);
         }
         return fWizardPages.get(0);
      }
      // Check if current page is a dynamic page
      int dynamicPageIndex = -1;
      if (fWizardPages != null) {
         dynamicPageIndex = fWizardPages.indexOf(currentPage);
      }
      if (dynamicPageIndex < 0) {
         // Non-existent page!
         // Should be impossible
         return null;
      }
      int newDynamixPageIndex = dynamicPageIndex+1;
      if (hasChanged(currentPage)) {
         if (newDynamixPageIndex<fWizardPages.size()) {
            // Remove invalidated pages
            fWizardPages.subList(newDynamixPageIndex, fWizardPages.size()).clear();
         }
      }
      if (newDynamixPageIndex < fWizardPages.size()) {
         // Return existing page
         return fWizardPages.get(newDynamixPageIndex);
      }
      if (newDynamixPageIndex >= fWizardPageInformation.size()) {
         // No more dynamic pages to create
         return null;
      }
      updateParamMap();
      // Create dynamic page
      UsbdmProjectOptionsPage_3 newPage = new UsbdmProjectOptionsPage_3(fUsbdmProjectParametersPage_2.getDevice(), fProjectActionList, fParamMap, fWizardPageInformation.get(newDynamixPageIndex));
      newPage.setWizard(this);
      fWizardPages.add(newPage);
      return newPage;
   }

   @Override
   public IWizardPage getPreviousPage(IWizardPage page) {
      if (page == fUsbdmProjectParametersPage_2) {
         return fUsbdmNewProjectPage_1;
      }
      if (fWizardPages != null) {
         int index = fWizardPages.indexOf(page);
         if (index == 0) {
            // Discard all dynamic pages
            return fUsbdmProjectParametersPage_2;
         }
         if (index > 0) {
            return fWizardPages.get(index-1);
         }
      }
      return null;
   }

   private void listParamMap(String title, final Map<String, String> paramMap) {
      System.err.println(title);
      ArrayList<String> keySet = new ArrayList<String>(paramMap.keySet());
      Collections.sort(keySet);
      for (String key:keySet) {
         if (key.equals("linkerInformation")) {
            continue;
         }
         if (key.equals("cVectorTable")) {
            continue;
         }
         if (key.startsWith("demo.KSDK")) {
            continue;
         }
         System.err.println(String.format("%-60s => %s", key, paramMap.get(key)));
      }
      System.err.println("================================================");
   }
   
   public void buildConfigurations(IProject project, IProgressMonitor monitor) {
      final int WORK_SCALE = 1000;
      try {
         ManagedBuildManager.saveBuildInfo(project, true);
         IConfiguration[] projectConfigs = ManagedBuildManager.getBuildInfo(project).getManagedProject().getConfigurations();
         monitor.beginTask("Build project", WORK_SCALE);
         try {
            ManagedBuildManager.buildConfigurations(projectConfigs, monitor);
         } catch (CoreException e) {
            e.printStackTrace();
         }
      }
      finally {
         monitor.done();
      }
   }

   public void updateConfigurations(IProject project, IProgressMonitor monitor) {
      final int WORK_SCALE = 100;
      try {
         IConfiguration[] projectConfigs = ManagedBuildManager.getBuildInfo(project).getManagedProject().getConfigurations();
         monitor.beginTask("Update configuration", WORK_SCALE*projectConfigs.length);
         for (IConfiguration config : projectConfigs) {
            ScannerConfigBuilder.build(config, ScannerConfigBuilder.PERFORM_CORE_UPDATE, monitor);
            monitor.worked(WORK_SCALE);
         }
         CCorePlugin.getIndexManager().reindex(CoreModel.getDefault().create(project));
      }
      finally {
         monitor.done();
      }
   }

   @Override
   public boolean performFinish() {
      try {
         fUsbdmNewProjectPage_1.saveSettings();
         fUsbdmProjectParametersPage_2.saveSettings();
         for (UsbdmProjectOptionsPage_3 page:fWizardPages) {
            page.saveSettings();
         }
         updateParamMap();
         listParamMap("fParamMap\n===========================================", fParamMap);
         
         getContainer().run(false, true, this);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return true;
   }

   @Override
   public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
      final int WORK_SCALE = 1000;
      System.err.println("UsbdmNewProjectWizard.run()");

      try {
         monitor.beginTask("Creating USBDM Project", WORK_SCALE*100);
         
         // Create project
         IProject project = new CDTProjectManager().createCDTProj(fParamMap, fDevice, new SubProgressMonitor(monitor, WORK_SCALE*20));
      
         // Apply device project options
         ProcessProjectActions.process(project, fDevice, fProjectActionList, fParamMap, new SubProgressMonitor(monitor, WORK_SCALE*20));
//         buildConfigurations(project,  new SubProgressMonitor(monitor, WORK_SCALE*40));
         updateConfigurations(project, new SubProgressMonitor(monitor, WORK_SCALE*20));
      } catch (Exception e) {
         e.printStackTrace();
         throw new InvocationTargetException(e);
      } finally {
         monitor.done();
      }
   }
   
   @Override
   public IWizardPage getPage(String name) {
      if (fUsbdmNewProjectPage_1.getName().equals(name)) {
         return fUsbdmNewProjectPage_1;
      }
      if (fUsbdmProjectParametersPage_2.getName().equals(name)) {
         return fUsbdmProjectParametersPage_2;
      }
      for (UsbdmProjectOptionsPage_3 page:fWizardPages) {
         if (page.getName().equals(name)) {
            return page;
         }
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