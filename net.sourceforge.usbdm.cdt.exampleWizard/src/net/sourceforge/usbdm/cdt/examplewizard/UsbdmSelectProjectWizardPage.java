package net.sourceforge.usbdm.cdt.examplewizard;

import java.io.File;
import java.util.ArrayList;

import net.sourceforge.usbdm.cdt.examplewizard.ExampleList.ProjectInformation;
import net.sourceforge.usbdm.cdt.wizard.UsbdmConstants;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class UsbdmSelectProjectWizardPage extends WizardPage {
   private Composite container;

   private ProjectInformation projectInformation = null;
   private Text     codesourceryArmPathText;
   private Button   codesourceryArmPathBrowseButton;
   private Text     codesourceryColdfirePathText;
   private Button   codesourceryColdfirePathBrowseButton;
   private Label    projectDescription; 

   public UsbdmSelectProjectWizardPage() {
      super("USBDM Example Selection");
      setTitle("USBDM Example Selection");
      setDescription("Select Example Project");
      setPageComplete(false);
   }

   public void saveSetting() {
      if (validate()) {
         //         System.err.println("saveSetting() saving settings");
         IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
         IValueVariable pathVariable;

         pathVariable= manager.getValueVariable(UsbdmConstants.CODESOURCERY_ARM_PATH_KEY);
         pathVariable.setValue(codesourceryArmPathText.getText());

         pathVariable = manager.getValueVariable(UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY);
         pathVariable.setValue(codesourceryColdfirePathText.getText());
      }
      //      else {
      //         System.err.println("saveSetting() - validate() failed, not saving settings");
      //      }
   }

   public String getCodeSourceryArmPath() {      
      return codesourceryArmPathText.getText();
   }

   public String getCodeSourceryColdfirePath() {      
      return codesourceryColdfirePathText.getText();
   }

   public ProjectInformation getProjectInformation() {
      return projectInformation;
   }

   public void setProjectInformation(ProjectInformation projectInformation) {
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

      String projectDesc = null;
      if (projectInformation != null) {
         projectDesc = projectInformation.getLongDescription();

         File file = projectInformation.getPath().toFile();
         if (!file.isFile() || !file.canRead()) {
            message = "Project file not found";
         }         
      }
      if (projectDesc == null) {
         projectDesc = "";
      }
      projectDescription.setText(projectDesc);

      if (projectInformation == null) {
         message = "Project not selected";
         codesourceryArmPathText.setEnabled(false);
         codesourceryColdfirePathText.setEnabled(false);
         codesourceryArmPathBrowseButton.setEnabled(false);
         codesourceryColdfirePathBrowseButton.setEnabled(false);
      }
      else if (projectInformation.getFamily().equalsIgnoreCase("ARM")) {
         IPath path = new Path(codesourceryArmPathText.getText()).append("bin");
         if (!path.toFile().isDirectory()) {
            message = "ARM directory invalid";
         }
         codesourceryArmPathText.setEnabled(true);
         codesourceryColdfirePathText.setEnabled(false);
         codesourceryArmPathBrowseButton.setEnabled(true);
         codesourceryColdfirePathBrowseButton.setEnabled(false);
      }
      else if (projectInformation.getFamily().equalsIgnoreCase("Coldfire")) {
         IPath path = new Path(codesourceryColdfirePathText.getText()).append("bin");
         if (!path.toFile().isDirectory()) {
            message = "Coldfire directory invalid";
         }
         codesourceryArmPathText.setEnabled(false);
         codesourceryColdfirePathText.setEnabled(true);
         codesourceryArmPathBrowseButton.setEnabled(false);
         codesourceryColdfirePathBrowseButton.setEnabled(true);
      }
      else {
         codesourceryArmPathText.setEnabled(false);
         codesourceryColdfirePathText.setEnabled(false);
         codesourceryArmPathBrowseButton.setEnabled(false);
         codesourceryColdfirePathBrowseButton.setEnabled(false);
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

   private void createCodesourceryPathControl(Composite parent) {

      Composite comp = new Composite(parent, SWT.NONE);
      comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      
      comp.setLayout(new GridLayout(3, false));

      //==================
      Label label = new Label(comp, SWT.NONE);
      label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      label.setText("Codesourcery ARM Path: ");

      codesourceryArmPathText = new Text(comp, SWT.SINGLE | SWT.BORDER);
      codesourceryArmPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      codesourceryArmPathText.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            validate();
         }
      });

      codesourceryArmPathBrowseButton = new Button(comp, SWT.NONE);
      codesourceryArmPathBrowseButton.setText("Browse");
      codesourceryArmPathBrowseButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            DirectoryDialog fd = new org.eclipse.swt.widgets.DirectoryDialog(getShell(), SWT.OPEN);
            fd.setText("Codesourcery Path - Select Directory");
            fd.setMessage("Locate Codesourcery directory");
            fd.setFilterPath(codesourceryArmPathText.getText());
            String directoryPath = fd.open();
            if (directoryPath != null) {
               codesourceryArmPathText.setText(directoryPath);
            }
            validate();
         }
      });

      //==================
      label = new Label(comp, SWT.NONE);
      label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      label.setText("Codesourcery Coldfire Path: ");

      codesourceryColdfirePathText = new Text(comp, SWT.SINGLE | SWT.BORDER);
      codesourceryColdfirePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      codesourceryColdfirePathText.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            validate();
         }
      });

      codesourceryColdfirePathBrowseButton = new Button(comp, SWT.NONE);
      codesourceryColdfirePathBrowseButton.setText("Browse");
      codesourceryColdfirePathBrowseButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            DirectoryDialog fd = new org.eclipse.swt.widgets.DirectoryDialog(getShell(), SWT.OPEN);
            fd.setText("Codesourcery Path - Select Directory");
            fd.setMessage("Locate Codesourcery directory");
            fd.setFilterPath(codesourceryColdfirePathText.getText());
            String directoryPath = fd.open();
            if (directoryPath != null) {
               codesourceryColdfirePathText.setText(directoryPath);
            }
            validate();
         }
      });
   }

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

//    Display display = Display.getCurrent();
//    Color blue = display.getSystemColor(SWT.COLOR_BLUE);
//    projectDescription.setBackground(blue);
}

   @Override
   public void createControl(Composite parent) {

      container = new Composite(parent, SWT.NULL);
      container.setLayout(new GridLayout(1, true));

      createExampleListControl(container);     
      createCodesourceryPathControl(container);
      
      setControl(container);

      // Get values from Variable manager
      IValueVariable  coldfirePathVariable = null;
      IValueVariable  armPathVariable      = null;

      VariablesPlugin variablesPlugin      = VariablesPlugin.getDefault();
      if (variablesPlugin != null) {
         IStringVariableManager manager = variablesPlugin.getStringVariableManager();
         if (manager != null) {
            coldfirePathVariable = manager.getValueVariable(UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY);
            if (coldfirePathVariable == null) {
               coldfirePathVariable = manager.newValueVariable(UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY, "Path to Codesourcery Coldfire directory");
            }
            armPathVariable = manager.getValueVariable(UsbdmConstants.CODESOURCERY_ARM_PATH_KEY);
            if (armPathVariable == null) {
               armPathVariable = manager.newValueVariable(UsbdmConstants.CODESOURCERY_ARM_PATH_KEY, "Path to Codesourcery ARM directory");
            }
         }
      }
      if (coldfirePathVariable != null) {
         codesourceryColdfirePathText.setText(coldfirePathVariable.getValue());
      }
      if (armPathVariable != null) {
         codesourceryArmPathText.setText(armPathVariable.getValue());
      }
   }
} 
