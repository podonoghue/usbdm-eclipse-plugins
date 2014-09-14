package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.io.File;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class UsbdmNewProjectPage extends WizardPage {

   static final String PAGE_NAME  = "USBDMProjectPage";
   static final String PAGE_TITLE = "New USBDM Project";
   private Button    projectLocationBrowseButton;
   private Text      projectNameText;
   private Label     locationText;
   private Button    useDefaultLocationButton;
   private boolean   hasCCNature;
   
   protected UsbdmNewProjectPage() {
      super(PAGE_NAME);
      setTitle(PAGE_TITLE);
      setDescription("Creates a new USBDM C/C++ Project");
   }

   private Control createProjectNameControl(Composite parent) {
      GridData gd;

      Composite group = new Composite(parent, SWT.NONE);
      group.setLayout(new GridLayout(3, false));

      Label name = new Label(group,SWT.NONE);
      name.setText("Project name:");

      projectNameText = new Text(group,SWT.BOLD | SWT.BORDER);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      projectNameText.setLayoutData(gd);
      projectNameText.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            validate();
         }
      });

      projectNameText.setFocus();
      
      return group;
   }
   
   private void updateLocation() {
    if (useDefaultLocationButton.getSelection()) {
       locationText.setEnabled(false);
       projectLocationBrowseButton.setEnabled(false);
       try {
          locationText.setText(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
       }
       catch (IllegalStateException s) {
       }
    }
    else {
       locationText.setEnabled(true);
       projectLocationBrowseButton.setEnabled(true);
    }
    validate();
 }

   private Control createLocationControl(final Composite parent) {
      GridLayout layout;

      Group group = new Group(parent, SWT.NONE);
      group.setText("Project Location");
      //
      layout = new GridLayout();
      group.setLayout(layout);

      //
      // Custom linker file checkbox
      //
      useDefaultLocationButton = new Button(group, SWT.CHECK);
      useDefaultLocationButton.setText(" Use default");
      useDefaultLocationButton.setToolTipText("Creat project in default workspace location");
      useDefaultLocationButton.addSelectionListener(new SelectionListener() {
         public void widgetSelected(SelectionEvent e) {
            updateLocation();
         }
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });
      //
      // Linker file browse
      //
      Composite composite = new Composite(group, SWT.NO_TRIM | SWT.NO_FOCUS);
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      composite.setLayoutData(gd);
      layout = new GridLayout(3, false);
      composite.setLayout(layout);
      composite.setBackground(group.getParent().getBackground());

      Label label = new Label(composite, SWT.NONE);
      gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
      label.setLayoutData(gd);
      label.setText("Location:"); 

      locationText = new Label(composite, SWT.BORDER);
      gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
      locationText.setLayoutData(gd);
      locationText.setToolTipText("This is used as the parent directory for the project");
      
      projectLocationBrowseButton = new Button(composite, SWT.PUSH);
      gd = new GridData(SWT.FILL, SWT.FILL, false, false);
      projectLocationBrowseButton.setLayoutData(gd);
      projectLocationBrowseButton.setText("Browse...");      
      projectLocationBrowseButton.setToolTipText("Browse for linker script");
      projectLocationBrowseButton.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            DirectoryDialog directoryDialog = new org.eclipse.swt.widgets.DirectoryDialog(parent.getShell(), SWT.OPEN);
            directoryDialog.setFilterPath(locationText.getText());

            directoryDialog.setText("USBDM - Select the parent directory for project");
            String directoryName = directoryDialog.open();
            if (directoryName != null) {
               locationText.setText(directoryName);
            }
            updateLocation();
         }

         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }}
            );

      useDefaultLocationButton.setSelection(true);
      locationText.setText(""); //$NON-NLS-1$
      return group;
   }
   
   InterfaceType interfaceType = null;
   
   InterfaceType getInterfaceType() {
      return interfaceType;
   }
   
   static class ButtonValues {
      String        description;
      InterfaceType interfaceType;
      boolean       hasCcNature;
      public ButtonValues(InterfaceType interfaceType, boolean hasCcNature, String description) {
         this.interfaceType   = interfaceType;
         this.hasCcNature     = hasCcNature;
         this.description     = description;
      }
   }
   
   ButtonValues buttonValues[] = {
         new ButtonValues(InterfaceType.T_ARM,  false, "ARM Project"),                 new ButtonValues(InterfaceType.T_ARM,  true, null), 
         new ButtonValues(InterfaceType.T_CFV1, false, "Coldfire V1 Project"),         new ButtonValues(InterfaceType.T_CFV1, true, null), 
         new ButtonValues(InterfaceType.T_CFVX, false, "Coldfire V2,3,4 Project  "),   new ButtonValues(InterfaceType.T_CFVX, true, null), 
   };
   
   static final private String INTERFACE_TYPE_KEY     = "projectSelectionWizard.interfaceType";
   static final private String HAS_CCNATURE_TYPE_KEY  = "projectSelectionWizard.hasCcNature";
   
   private Control createProjectChoiceControl(final Composite parent) {
      GridLayout layout;

      Group group = new Group(parent, SWT.NONE);
      group.setText("Project Type");
      //
      layout = new GridLayout(3, false);
      group.setLayout(layout);

      
      IDialogSettings dialogSettings = getDialogSettings();

      interfaceType = InterfaceType.T_ARM;
      hasCCNature  = true;
      if (dialogSettings != null) {
         try {
            String sInterfaceType = dialogSettings.get(INTERFACE_TYPE_KEY);
            if (sInterfaceType != null) {
               interfaceType = InterfaceType.values()[Integer.parseInt(sInterfaceType)];
            }
            String sHasCCNature = dialogSettings.get(HAS_CCNATURE_TYPE_KEY);
            if (sHasCCNature != null) {
               hasCCNature = Boolean.valueOf(sHasCCNature);
            }
         } catch (Exception e) {
         }
      }
      for (final ButtonValues b : buttonValues) {
         if (b.description != null) {
            Label label = new Label(group, SWT.NONE);
            label.setText(b.description);
         }
         Button button = new Button(group, SWT.RADIO);
         if (b.hasCcNature) {
            button.setText(" C++");
         }
         else {
            button.setText(" C  ");
         }
         if ((b.interfaceType == interfaceType) && (b.hasCcNature == hasCCNature)) {
            button.setSelection(true);
         }
         button.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               interfaceType = b.interfaceType;           
               hasCCNature   = b.hasCcNature;
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
         });
      }
      return group;
   }
   
   @Override
   public void createControl(Composite parent) {
      GridData gd;
      Control control;
      
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayout(new GridLayout());

      control = createProjectNameControl(composite);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);

      control = createLocationControl(composite);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);

      control = createProjectChoiceControl(composite);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);

      setControl(composite);
      
      updateLocation();
   }

   /**
    *  Validates control & sets error message
    *  
    * @param message error message (null if none)
    */
   private void validate() {
      String message = null;
      IPath path = new Path(locationText.getText().trim());
      File file = path.toFile();
      if (projectNameText.getText().trim().isEmpty()) {
         message = "Project name is required";
      }
      else if (!file.isDirectory() || !file.canRead()) {
         message = "Project location is invalid or inaccessible"; 
      }
      else {
         path = path.append(projectNameText.getText().trim());
         file = path.toFile();
         if (file.exists()) {
            message = "Project already exists at that location"; 
         }
      }
      setErrorMessage(message);
      setPageComplete(message == null);
   }


   public String getProjectName() {
      return projectNameText.getText().trim();
   }

   public String getProjectLocation() {
      if (useDefaultLocationButton.getSelection()) {
         return null;
      }
      return locationText.getText().trim();
   }

   public void saveSettings() {
      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings != null) {
         dialogSettings.put(INTERFACE_TYPE_KEY,    interfaceType.ordinal());
         dialogSettings.put(HAS_CCNATURE_TYPE_KEY, hasCCNature);
      }
   }
   
   public boolean isCCNature() {
      return hasCCNature;
   }

}