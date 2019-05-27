package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.cdt.ui.Activator;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.packageParser.ProjectAction;
import net.sourceforge.usbdm.packageParser.ProjectActionList;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Value;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor.Result;
import net.sourceforge.usbdm.packageParser.ProjectConstant;
import net.sourceforge.usbdm.packageParser.WizardPageInformation;

/**
 * @author pgo
 */
public class UsbdmNewProjectWizard extends Wizard implements INewWizard, IRunnableWithProgress {

   private UsbdmNewProjectPage_1                fUsbdmNewProjectPage_1 = null;
   private UsbdmDeviceSelectionPage_2           fUsbdmProjectParametersPage_2 = null;
   private ArrayList<UsbdmDynamicOptionPage_N>  fDynamicWizardPages  = null;
   private ArrayList<WizardPageInformation>     fWizardPageInformation = new ArrayList<WizardPageInformation>();
   private ProjectActionList                    fProjectActionList = null;
   /** Base parameters from pages 1 & 2 */
   private Map<String, String>                  fBaseParamMap = null;
   /** Parameters from all Pages */
   private Map<String, String>                  fParamMap = null;
   private Device                               fDevice = null;

   @Override
   public void init(IWorkbench workbench, IStructuredSelection selection) {
      setNeedsProgressMonitor(true);
      setDefaultPageImageDescriptor(UsbdmSharedConstants.getUsbdmIcon());

      IDialogSettings settings = null;
      Activator activator = Activator.getDefault();
      if (activator == null) {
         Activator.log("*************************** plugin is null *********************");
      }
      if (activator != null) {
         settings = activator.getDialogSettings();
         if (settings == null) {
            Activator.log("*************************** settings is null *********************");
         }
      }
      setDialogSettings(settings);
   }

   @Override
   public void addPages() {
      fUsbdmNewProjectPage_1  = new UsbdmNewProjectPage_1(this);
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
            fDynamicWizardPages == null) {
         return false;
      }
      if (fDynamicWizardPages.size() != fWizardPageInformation.size()) {
         // Haven't visited all pages
         return false;
      }
      for (UsbdmDynamicOptionPage_N page:fDynamicWizardPages) {
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
    * Visits the nodes of the projectActionList and add constants found 
    * 
    * @param paramMap            Map to add constants to
    * @param projectActionList   Action list to visit
    */
   void updateMapConstants(final Map<String, String> paramMap, ProjectActionList projectActionList, final Device device) {
      Visitor visitor = new Visitor() {
         @Override
         public Result applyTo(ProjectAction action, Value result, IProgressMonitor monitor) {
            try {
               if (action instanceof ProjectActionList) {
                  ProjectActionList projectActionList = (ProjectActionList) action;
                  return projectActionList.appliesTo(device, paramMap)?CONTINUE:PRUNE;
               }
               else if (action instanceof ProjectConstant) {
                  ProjectConstant projectConstant = (ProjectConstant) action;
                  //                  Activator.log(String.format("updateMapConstants(): Found constant %s => %s",  projectConstant.getId(), projectConstant.getValue()));
                  String value = paramMap.get(projectConstant.getId());
                  if (value != null) {
                     if (projectConstant.isWeak()) {
                        // Ignore - assume constant is a default that has been overwritten
                        return CONTINUE;
                     }
                     if (!projectConstant.doOverwrite() && !value.equals(projectConstant.getValue())) {
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
      Result result = projectActionList.visit(visitor, null);
      if (result.getStatus() == Result.Status.EXCEPTION) {
         result.getException().printStackTrace();
      }
   }

   /**
    * Collects information about dynamic wizard pages
    */
   void updateDynamicWizardPageInformation() {
      //      Activator.log("updateDynamicWizardPageInformation()");
      fWizardPageInformation = new ArrayList<WizardPageInformation>();
      Visitor visitor = new ProjectActionList.Visitor() {
         @Override
         public Result applyTo(ProjectAction action, ProjectActionList.Value result, IProgressMonitor monitor) {
            if (action instanceof WizardPageInformation) {
               WizardPageInformation page = (WizardPageInformation) action;
               //               Activator.log("Adding dynamic page info " + page.getName());
               fWizardPageInformation.add(page);
            }
            return CONTINUE;
         }
      };
      if (fProjectActionList != null) {
         fProjectActionList.visit(visitor, null);
      }
   }

   /**
    * Set device
    * 
    * @param device
    */
   void setDevice(Device device) {
      fDevice = device;
   }
   
   /**
    * Updates fParamMap and pages
    */
   void updateParamMap(WizardPage currentPage) {
//      Activator.log("updateParamMap()");
      fParamMap = new HashMap<String, String>(fBaseParamMap);
      final UsbdmNewProjectWizard wizard = this;
      if (fUsbdmProjectParametersPage_2 != null) {
         Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
               if (fUsbdmProjectParametersPage_2 != null) {
                  fUsbdmProjectParametersPage_2.getPageData(fParamMap);
               }
            }
         });
         fProjectActionList = fDevice.getProjectActionList(fParamMap);
         if (currentPage == fUsbdmProjectParametersPage_2) {
            // Create 1st dynamic page
//            Activator.log("updateParamMap() - Create 1st dynamic page");

            updateDynamicWizardPageInformation();

            fDynamicWizardPages = new ArrayList<UsbdmDynamicOptionPage_N>();
            UsbdmDynamicOptionPage_N newPage = new UsbdmDynamicOptionPage_N(fDevice, fParamMap, fProjectActionList, fWizardPageInformation.get(0));
            newPage.setWizard(wizard);
            fDynamicWizardPages.add(newPage);
         }
         if (fDynamicWizardPages != null) {
            for (final UsbdmDynamicOptionPage_N page:fDynamicWizardPages) {
               Display.getDefault().syncExec(new Runnable() {
                  @Override
                  public void run() {
                     page.getPageData(fParamMap);
                  }
               });
               updateMapConstants(fParamMap, fProjectActionList, fDevice);
               if (page == currentPage) {
                  break;
               }
            }
         }
         int dynamicIndex = fDynamicWizardPages.indexOf(currentPage);
         if (dynamicIndex>=0) {
            int newPageIndex = dynamicIndex + 1;
            // Remove pages after current page
            fDynamicWizardPages.removeAll(fDynamicWizardPages.subList(newPageIndex, fDynamicWizardPages.size()));
            if (fWizardPageInformation.size()>newPageIndex) {
               // Regenerate page after current page
               UsbdmDynamicOptionPage_N newPage = new UsbdmDynamicOptionPage_N(fDevice, fParamMap, fProjectActionList, fWizardPageInformation.get(newPageIndex));
               newPage.setWizard(wizard);
               fDynamicWizardPages.add(newPage);
            }
         }
      }
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
      if (fDynamicWizardPages == null) {
         return false;
      }
      for (UsbdmDynamicOptionPage_N page:fDynamicWizardPages) {
         if (page.hasChanged()) {
            return true;
         }
         if (page == currentPage) {
            return false;
         }
      }
      return false;
   }

   @Override
   public IWizardPage getNextPage(IWizardPage currentPage) {
//      Activator.log("getNextPage(" + currentPage.getTitle() + ")");
      if (currentPage == fUsbdmNewProjectPage_1) {
         // Create new project page if none or interface has changed
         if (hasChanged(currentPage) || (fUsbdmProjectParametersPage_2 == null)) {
            fBaseParamMap = new HashMap<String, String>();
            fUsbdmNewProjectPage_1.getPageData(fBaseParamMap);
            InterfaceType interfaceType = InterfaceType.valueOf(fBaseParamMap.get(UsbdmConstants.INTERFACE_TYPE_KEY));
            fUsbdmProjectParametersPage_2 = new UsbdmDeviceSelectionPage_2(interfaceType, this);
            fDynamicWizardPages = null;
         }
         return fUsbdmProjectParametersPage_2;
      }
      int newDynamicPageIndex = 0;
      if (currentPage == fUsbdmProjectParametersPage_2) {
         if ((fDynamicWizardPages == null) || (fDynamicWizardPages.size() == 0)) {
//            Activator.log("getNextPage(" + currentPage.getTitle() + ") => null");
            return null;
         }
//         Activator.log("getNextPage(" + currentPage.getTitle() + ") => " + fDynamicWizardPages.get(0));
         newDynamicPageIndex = 0;
      }
      else {
         // Check if current page is a dynamic page
         int dynamicPageIndex = -1;
         if (fDynamicWizardPages != null) {
            dynamicPageIndex = fDynamicWizardPages.indexOf(currentPage);
         }
         if (dynamicPageIndex < 0) {
            // Non-existent page!
            // Should be impossible
            return null;
         }
         newDynamicPageIndex = dynamicPageIndex+1;
      }
      if (newDynamicPageIndex < fDynamicWizardPages.size()) {
         // Return existing page
         UsbdmDynamicOptionPage_N page = fDynamicWizardPages.get(newDynamicPageIndex);
//         page.setProjectInformation(fProjectActionList, fParamMap);
         return page;
      }
      else {
         // No more dynamic pages to create
         return null;
      }
   }

   @Override
   public IWizardPage getPreviousPage(IWizardPage page) {
      if (page == fUsbdmProjectParametersPage_2) {
         return fUsbdmNewProjectPage_1;
      }
      if (fDynamicWizardPages != null) {
         int index = fDynamicWizardPages.indexOf(page);
         if (index == 0) {
            // Discard all dynamic pages
            return fUsbdmProjectParametersPage_2;
         }
         if (index > 0) {
            return fDynamicWizardPages.get(index-1);
         }
      }
      return null;
   }

   /**
    * Write parameter map to log.
    * Done as job as time consuming
    * 
    * @param title
    * @param paramMap
    */
   private void listParamMap(final String title, final Map<String, String> paramMap) {
      Job job = Job.create("Log paramater map", new IJobFunction() {
         @Override
         public IStatus run(IProgressMonitor pm) {

            StringBuffer sb = new StringBuffer();
            sb.append(title+"\n");
            sb.append("================================================\n");
            ArrayList<String> keySet = new ArrayList<String>(paramMap.keySet());
            Collections.sort(keySet);
            for (String key:keySet) {
               if (key.equals("linkerExtraRegions")) {
                  continue;
               }
               if (key.equals("linkerInformation")) {
                  continue;
               }
               if (key.equals("cVectorTable")) {
                  continue;
               }
               if (key.startsWith("demo.KSDK")) {
                  continue;
               }
               sb.append(String.format("%-60s => %s\n", key, paramMap.get(key)));
            }
            sb.append("================================================\n");
            Activator.log(sb.toString());
            return Status.OK_STATUS;
         }
      });
      // Start the Job
      job.schedule();
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
            Activator.logError("buildConfigurations()", e);
         }
      }
      finally {
         monitor.done();
      }
   }

   /**
    * Re-index the C project
    * 
    * @param project Project to index
    * @param monitor Progress monitor
    */
   public void reindexProject(IProject project, IProgressMonitor monitor) {
      try {
         monitor.beginTask("Update configuration", IProgressMonitor.UNKNOWN);
         ICProject cproject = CoreModel.getDefault().getCModel().getCProject(project.getName());
         CCorePlugin.getIndexManager().reindex(cproject);
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
         for (UsbdmDynamicOptionPage_N page:fDynamicWizardPages) {
            page.saveSettings();
         }
         listParamMap("fParamMap", fParamMap);
         getContainer().run(true, false, this);
      } catch (Exception e) {
         Activator.log("performFinish()", e);
      }
      return true;
   }

   /**
    *  Turn off Auto-build in workspace
    *  TODO - should restore to original after project construction?
    */
   void disableAutoBuild() {
      try {
         IWorkspace            workspace = ResourcesPlugin.getWorkspace();
         IWorkspaceDescription workspaceDesc = workspace.getDescription();
         workspaceDesc.setAutoBuilding(false);
         workspace.setDescription(workspaceDesc);
      } catch (CoreException e) {
         // Ignore - For debug without workspace
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
      for (UsbdmDynamicOptionPage_N page:fDynamicWizardPages) {
         if (page.getName().equals(name)) {
            return page;
         }
      }
      return null;
   }

   public Map<String, String> getparamMap() {
      return fParamMap;
   }

 @Override
 public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
    disableAutoBuild();
    Job job = Job.create("Project]", new IJobFunction() {
      
      @Override
      public IStatus run(IProgressMonitor progressMonitor) {
         UsbdmCdtProjectManager.createUsbdmProject(fParamMap, fProjectActionList, progressMonitor, fDevice);
         return Status.OK_STATUS;
      }
   });
    job.setUser(true);
    job.schedule();
    job.join();
 }

   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Project Parameters");
      shell.setLayout(new FillLayout());
      shell.setSize(500, 350);

      UsbdmNewProjectWizard wizard = new UsbdmNewProjectWizard();
      new WizardDialog(shell, wizard).open();
      display.dispose();
   }

   
}