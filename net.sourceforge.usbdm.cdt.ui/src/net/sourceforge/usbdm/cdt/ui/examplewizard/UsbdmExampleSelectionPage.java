package net.sourceforge.usbdm.cdt.ui.examplewizard;

import java.io.File;
import java.util.ArrayList;

import net.sourceforge.usbdm.cdt.ui.examplewizard.ExampleList.ProjectInformation;
import net.sourceforge.usbdm.cdt.ui.newProjectWizard.IUsbdmProjectTypeSelection;
import net.sourceforge.usbdm.constants.ToolInformationData;

import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

public class UsbdmExampleSelectionPage extends WizardPage  implements IWizardPage, IUsbdmProjectTypeSelection {
   
   private Composite                   container;

   private ProjectInformation          projectInformation = null;
   private Label                       projectDescription; 
   
   public UsbdmExampleSelectionPage() {
      super("USBDM Example Selection");
      setTitle("USBDM Example Selection");
      setDescription("Select Example Project");

      setPageComplete(false);
   }

   public void saveSetting() {
   }

   public ProjectInformation getProjectInformation() {
      return projectInformation;
   }

   private void setProjectInformation(ProjectInformation projectInformation) {
      this.projectInformation = projectInformation;
   }

   /**
    *  Validates control & sets error message
    *  
    * @param message error message (null if none)
    * 
    * @return true => dialogue values are valid
    */
   public boolean validate() {
      String message = null;

      String projectDesc   = null;
      String buildtoolId = null;
      if (projectInformation != null) {
         projectDesc = projectInformation.getLongDescription();
         buildtoolId = projectInformation.getBuildTool();

         File file = projectInformation.getPath().toFile();
         if (!file.isFile() || !file.canRead()) {
            message = "Project file not found";
         }         
      }
      if (buildtoolId == null) {
         buildtoolId = "";
      }
      ToolInformationData toolInfo = ToolInformationData.getToolInformationTable().get(buildtoolId);
      if (toolInfo == null) {
         buildToolLabel.setText("None");
      }
      else {
         buildToolLabel.setText(toolInfo.getDescription());
      }
      if (projectDesc == null) {
         projectDesc = "";
      }
      projectDescription.setText(projectDesc);
      if (projectInformation == null) {
         message = "Project not selected";
      }
      setErrorMessage(message);
      setPageComplete(message == null);
      return message == null;
   }

   private class ProjectButtonListener implements SelectionListener {
      ProjectInformation projectInfo;

      ProjectButtonListener(ProjectInformation projectInfo) {
         super();
         this.projectInfo = projectInfo;
      }
      @Override
      public void widgetSelected(SelectionEvent e) {
         setProjectInformation(projectInfo);
         validate();
      }
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
      }
   };

   private void createExampleListControl(Composite parent) {

      Composite container = new Composite(parent, SWT.NULL);
      container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      container.setLayout(new GridLayout(2, true));

      ExampleList exampleList = new ExampleList(); 
      ArrayList<ProjectInformation> examples = exampleList.getProjectList();
      for (ProjectInformation projectInfo:examples) {
         Button button = new Button(container, SWT.RADIO);
         button.setSelection(false);
         button.setText(projectInfo.getDescription());
         button.addSelectionListener(new ProjectButtonListener(projectInfo));
      }      
      
      Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
      group.setText("Description");
      group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      group.setLayout(new GridLayout(1, true));

      projectDescription = new Label(group, NONE);

      GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
      gridData.heightHint = 100;
      projectDescription.setLayoutData(gridData);
   }
   
//   Combo buildToolCombo;
//   
//   private String buildToolIds[] = null;
//   
//   private void loadBuildtoolNames(InterfaceType deviceType) {
//      String currentTool = buildToolCombo.getText();
//      buildToolCombo.removeAll();
//      Hashtable<String, ToolInformationData> toolInformationData = ToolInformationData.getToolInformationTable();
//      buildToolIds = new String[toolInformationData.size()];
//      int index = 0;
//      for (ToolInformationData toolInformation:toolInformationData.values()) {
//         if (toolInformation.applicableTo(deviceType)) {
//            buildToolCombo.add(toolInformation.getDescription());
//            buildToolIds[index++] = toolInformation.getBuildToolId();
//         }
//      }
//      // Try to restore current selection
//      buildToolCombo.setText(currentTool);
//      int buildToolIndex = buildToolCombo.getSelectionIndex();
//      if (buildToolIndex<0) {
//         buildToolCombo.select(0);
//      }
//   }
//   
   @Override
   public String getSelectedBuildToolsId() {

      if (projectInformation == null) {
         return "";
      }
      return projectInformation.getBuildTool();
      
//      int index = buildToolCombo.getSelectionIndex();
//      if ((index<0) || (index > buildToolIds.length)) {
//         return "";
//      }
//      return buildToolIds[index];
   }

   protected IDialogSettings getDialogSettings() {
      return super.getDialogSettings();
   }
 
   Label buildToolLabel;
   
   private Composite createUsbdmControl(Composite parent) {

      Group group = new Group(parent, SWT.NONE);
      group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

      group.setLayout(new GridLayout(2, false));
      group.setText("USBDM Parameters");

      Label label;
      GridData gd;
      
      //
      // Create & Populate Combo for Build tool selection
      //
      label = new Label(group, SWT.NONE);
      label.setText("Build tools:"); //$NON-NLS-1$

      buildToolLabel = new Label(group, SWT.NONE);
      gd = new GridData();
      gd.widthHint = 250;
      buildToolLabel.setLayoutData(gd);
      
//      buildToolCombo = new Combo(group, SWT.BORDER|SWT.READ_ONLY);
//      gd = new GridData();
//      gd.widthHint = 250;
//      buildToolCombo.setLayoutData(gd);
//      buildToolCombo.select(0);
//      loadBuildtoolNames(InterfaceType.T_ARM);
           
      return group;
   }

   @Override
   public void createControl(Composite parent) {

      container = new Composite(parent, SWT.NULL);
      container.setLayout(new GridLayout(1, true));

      createExampleListControl(container);     
      createUsbdmControl(container);
      
      setControl(container);

//      // Get values from Variable manager
//      IValueVariable  coldfirePathVariable = null;
//      IValueVariable  armPathVariable      = null;

      VariablesPlugin variablesPlugin      = VariablesPlugin.getDefault();
      if (variablesPlugin != null) {
//         IStringVariableManager manager = variablesPlugin.getStringVariableManager();
//         if (manager != null) {
//            coldfirePathVariable = manager.getValueVariable(UsbdmConstants.USBDM_COLDFIRE_PATH_VAR);
//            if (coldfirePathVariable == null) {
//               coldfirePathVariable = manager.newValueVariable(UsbdmConstants.USBDM_COLDFIRE_PATH_VAR, "Path to Coldfire Tools directory");
//            }
//            armPathVariable = manager.getValueVariable(UsbdmConstants.USBDM_ARM_PATH_VAR);
//            if (armPathVariable == null) {
//               armPathVariable = manager.newValueVariable(UsbdmConstants.USBDM_ARM_PATH_VAR, "Path to ARM Tools directory");
//            }
//         }
      }
//      if (coldfirePathVariable != null) {
//         coldfirePathText.setText(coldfirePathVariable.getValue());
//      }
//      if (armPathVariable != null) {
//         armPathText.setText(armPathVariable.getValue());
//      }
   }
} 
