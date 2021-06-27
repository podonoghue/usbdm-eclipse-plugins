package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.io.File;
import java.util.Map;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
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

public class UsbdmNewProjectPage_1 extends WizardPage {

   static final String PAGE_NAME  = "NewUsbdmProjectWizardPage";
   static final String PAGE_TITLE = "New USBDM Project";
   
   private Button          fProjectLocationBrowseButton;
   private Text            fProjectNameText;
   private Label           fLocationText;
   private Button          fUseDefaultLocationButton;
   private Button          fUseSemiHosting;
   private Button          fUseFloatingPointInScanf;
   private Button          fUseFloatingPointInPrintf;
   private Boolean         fHasCCNature         = false;
   private InterfaceType   fInterfaceType       = null;
   private Boolean         fHasChanged          = true;
   private Boolean         fCreateStaticLibrary = false;

   protected UsbdmNewProjectPage_1(UsbdmNewProjectWizard usbdmNewProjectWizard) {
      super(PAGE_NAME);
      setTitle(PAGE_TITLE);
      setDescription("Creates a new USBDM C/C++ Project");
      setWizard(usbdmNewProjectWizard);
   }

   private Control createProjectNameControl(Composite parent) {
      GridData gd;

      Composite group = new Composite(parent, SWT.NONE);
      group.setLayout(new GridLayout(3, false));

      Label name = new Label(group,SWT.NONE);
      name.setText("Project name:");

      fProjectNameText = new Text(group,SWT.BOLD | SWT.BORDER);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      fProjectNameText.setLayoutData(gd);
      fProjectNameText.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            validate();
         }
      });

      fProjectNameText.setFocus();
      return group;
   }
   
   private void updateLocation() {
    if (fUseDefaultLocationButton.getSelection()) {
       fLocationText.setEnabled(false);
       fProjectLocationBrowseButton.setEnabled(false);
       try {
          fLocationText.setText(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
       }
       catch (IllegalStateException s) {
          fLocationText.setText(".");
       }
    }
    else {
       fLocationText.setEnabled(true);
       fProjectLocationBrowseButton.setEnabled(true);
    }
    validate();
 }

   private Control createProjectLocationControl(final Composite parent) {
      GridLayout layout;

      Group group = new Group(parent, SWT.NONE);
      group.setText("Project Location");
      //
      layout = new GridLayout();
      group.setLayout(layout);

      //
      // Custom project location checkbox
      //
      fUseDefaultLocationButton = new Button(group, SWT.CHECK);
      fUseDefaultLocationButton.setText(" Use default");
      fUseDefaultLocationButton.setToolTipText("Creat project in default workspace location");
      fUseDefaultLocationButton.addSelectionListener(new SelectionListener() {
         public void widgetSelected(SelectionEvent e) {
            updateLocation();
         }
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });
      //
      // Project location file browse
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

      fLocationText = new Label(composite, SWT.BORDER);
      gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
      fLocationText.setLayoutData(gd);
      fLocationText.setToolTipText("This is used as the parent directory for the project");
      
      fProjectLocationBrowseButton = new Button(composite, SWT.PUSH);
      gd = new GridData(SWT.FILL, SWT.FILL, false, false);
      fProjectLocationBrowseButton.setLayoutData(gd);
      fProjectLocationBrowseButton.setText("Browse...");      
      fProjectLocationBrowseButton.setToolTipText("Browse for project location");
      fProjectLocationBrowseButton.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            DirectoryDialog directoryDialog = new org.eclipse.swt.widgets.DirectoryDialog(parent.getShell(), SWT.OPEN);
            directoryDialog.setFilterPath(fLocationText.getText());

            directoryDialog.setText("USBDM - Select the parent directory for project");
            String directoryName = directoryDialog.open();
            if (directoryName != null) {
               fLocationText.setText(directoryName);
            }
            updateLocation();
         }

         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }}
            );

      fUseDefaultLocationButton.setSelection(true);
      fLocationText.setText(""); //$NON-NLS-1$
      return group;
   }
      
   static class ButtonValues {
      String        fDescription;
      InterfaceType fInterfaceType;
      boolean       fHasCcNature;
      public ButtonValues(InterfaceType interfaceType, boolean hasCcNature, String description) {
         this.fInterfaceType   = interfaceType;
         this.fHasCcNature     = hasCcNature;
         this.fDescription     = description;
      }
   }
   
   ButtonValues buttonValues[] = {
         new ButtonValues(InterfaceType.T_ARM,  false, "ARM Project"),                 new ButtonValues(InterfaceType.T_ARM,  true, null), 
         new ButtonValues(InterfaceType.T_CFV1, false, "Coldfire V1 Project"),         new ButtonValues(InterfaceType.T_CFV1, true, null), 
         new ButtonValues(InterfaceType.T_CFVX, false, "Coldfire V2,3,4 Project  "),   new ButtonValues(InterfaceType.T_CFVX, true, null), 
   };
   
   private Control createProjectTypeControl(final Composite parent) {
	      GridLayout layout;

	      Group group = new Group(parent, SWT.NONE);
	      group.setText("Project Type");
	      //
	      layout = new GridLayout(3, false);
	      group.setLayout(layout);
	      
	      IDialogSettings dialogSettings = getDialogSettings();

	      fInterfaceType = InterfaceType.T_ARM;
	      fHasCCNature  = true;
	      if (dialogSettings != null) {
	         try {
	            String sInterfaceType = dialogSettings.get(UsbdmConstants.INTERFACE_TYPE_KEY);
	            if (sInterfaceType != null) {
	               fInterfaceType = InterfaceType.values()[Integer.parseInt(sInterfaceType)];
	            }
	            String sHasCCNature = dialogSettings.get(UsbdmConstants.HAS_CC_NATURE_KEY);
	            if (sHasCCNature != null) {
	               fHasCCNature = Boolean.valueOf(sHasCCNature);
	            }
	         } catch (Exception e) {
	         }
	      }
	      for (final ButtonValues b : buttonValues) {
	         if (b.fDescription != null) {
	            Label label = new Label(group, SWT.NONE);
	            label.setText(b.fDescription);
	         }
	         Button button = new Button(group, SWT.RADIO);
	         if (b.fHasCcNature) {
	            button.setText(" C++");
	         }
	         else {
	            button.setText(" C  ");
	         }
	         if ((b.fInterfaceType == fInterfaceType) && (b.fHasCcNature == fHasCCNature)) {
	            button.setSelection(true);
	         }
	         button.addSelectionListener(new SelectionListener() {
	            @Override
	            public void widgetSelected(SelectionEvent e) {
	               fInterfaceType = b.fInterfaceType;           
	               fHasCCNature   = b.fHasCcNature;
	               validate();
	            }
	            @Override
	            public void widgetDefaultSelected(SelectionEvent e) {
	            }
	         });
	      }
	      return group;
	   }
	   

   private Control createDebugOptionsControl(final Composite parent) {
      GridLayout layout;

      Group group = new Group(parent, SWT.NONE);
      group.setText("Debug Options (Debug target only)");
      //
      layout = new GridLayout(3, false);
      group.setLayout(layout);
      
      IDialogSettings dialogSettings = getDialogSettings();

      fUseSemiHosting = new Button(group, SWT.CHECK);
      fUseSemiHosting.setText("Semi-hosting");
      fUseSemiHosting.setToolTipText("Add semi-hosting option to debug target");
      fUseSemiHosting.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
        	validate();
         }
         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });
      if (dialogSettings != null) {
          try {
             String sSemiHosting = dialogSettings.get(UsbdmConstants.SEMI_HOSTING_TYPE_KEY);
             if (sSemiHosting != null) {
             	fUseSemiHosting.setSelection(Boolean.valueOf(sSemiHosting));
             }
          } catch (Exception e) {
          }
       }
      return group;
   }
   
   private Control createLibraryOptionsControl(final Composite parent) {
      GridLayout layout;

      Group group = new Group(parent, SWT.NONE);
      group.setText("Library Options");
      //
      layout = new GridLayout(1, false);
      group.setLayout(layout);
      
      fUseFloatingPointInScanf = new Button(group, SWT.CHECK);
      fUseFloatingPointInScanf.setText("Floating point in scanf()");
      fUseFloatingPointInScanf.setToolTipText("Allow floating point (%f format) in scanf() - increases CODE size and RAM usage");
      fUseFloatingPointInScanf.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
         validate();
         }
         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });

      fUseFloatingPointInPrintf = new Button(group, SWT.CHECK);
      fUseFloatingPointInPrintf.setText("Floating point in printf()");
      fUseFloatingPointInPrintf.setToolTipText("Allow floating point (%f format) in printf() - increases CODE size and RAM usage");
      fUseFloatingPointInPrintf.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
         validate();
         }
         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });

      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings != null) {
          try {
             String useFloatingPointInScanf = dialogSettings.get(UsbdmConstants.USE_FLOATINGPOINT_IN_SCANF_KEY);
             if (useFloatingPointInScanf != null) {
               fUseFloatingPointInScanf.setSelection(Boolean.valueOf(useFloatingPointInScanf));
             }
             String useFloatingPointInPrintf = dialogSettings.get(UsbdmConstants.USE_FLOATINGPOINT_IN_PRINTF_KEY);
             if (useFloatingPointInPrintf != null) {
               fUseFloatingPointInPrintf.setSelection(Boolean.valueOf(useFloatingPointInPrintf));
             }
          } catch (Exception e) {
          }
       }
      return group;
   }
   
   private Control createProjectOutputControl(final Composite parent) {
      GridLayout layout;

      Group group = new Group(parent, SWT.NONE);
      group.setText("Project Output");
      layout = new GridLayout(3, false);
      group.setLayout(layout);
      
      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings != null) {
         String sOutputType = dialogSettings.get(UsbdmConstants.PROJECT_OUTPUT_TYPE_KEY);
         if (sOutputType != null) {
            fCreateStaticLibrary = Boolean.valueOf(sOutputType);
         }
      }
      Composite composite = new Composite(group, SWT.NONE);
      layout = new GridLayout(2, false);
      composite.setLayout(layout);
      Button executableButton = new Button(composite, SWT.RADIO);
      executableButton.setText("Executable");
      executableButton.setSelection(!fCreateStaticLibrary);
      executableButton.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            fCreateStaticLibrary = false;
            validate();
         }
         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });
      Button libraryButton = new Button(composite, SWT.RADIO);
      libraryButton.setText("Static Library");
      libraryButton.setSelection(fCreateStaticLibrary);
      libraryButton.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            fCreateStaticLibrary = true;
            validate();
         }
         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });
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

      control = createProjectLocationControl(composite);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);

      control = createProjectTypeControl(composite);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);

      control = createDebugOptionsControl(composite);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);

      control = createLibraryOptionsControl(composite);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);
      
      control = createProjectOutputControl(composite);
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
      IPath path = new Path(fLocationText.getText().trim());
      File file = path.toFile();
      if (fProjectNameText.getText().trim().isEmpty()) {
         message = "Project name is required";
      }
      else if (!fProjectNameText.getText().matches("^\\s*[a-zA-Z0-9_][a-zA-Z0-9_\\-\\.]*\\s*")) {
         message = "Project name contains illegal characters, use only 'a-z A-Z 0-9 - _ .' and not start with '-' or '.'";
      }
      else if (!file.isDirectory() || !file.canRead()) {
         message = "Project location is invalid or inaccessible"; 
      }
      else {
         path = path.append(fProjectNameText.getText().trim());
         file = path.toFile();
         if (file.exists()) {
            message = "Project already exists at that location"; 
         }
      }
      setErrorMessage(message);
      setPageComplete(message == null);
      fHasChanged    = true;
   }

   public void saveSettings() {
      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings != null) {
         dialogSettings.put(UsbdmConstants.INTERFACE_TYPE_KEY,              fInterfaceType.ordinal());
         dialogSettings.put(UsbdmConstants.HAS_CC_NATURE_KEY,               fHasCCNature);
         dialogSettings.put(UsbdmConstants.PROJECT_OUTPUT_TYPE_KEY,         fCreateStaticLibrary);
         dialogSettings.put(UsbdmConstants.SEMI_HOSTING_TYPE_KEY,           fUseSemiHosting.getSelection());
         dialogSettings.put(UsbdmConstants.USE_FLOATINGPOINT_IN_SCANF_KEY,  fUseFloatingPointInScanf.getSelection());
         dialogSettings.put(UsbdmConstants.USE_FLOATINGPOINT_IN_PRINTF_KEY, fUseFloatingPointInPrintf.getSelection());
      }
   }

   /**
    * Indicates if the page has changed since last checked
    * 
    * @return
    */
   public Boolean hasChanged() {
      Boolean hasChanged = fHasChanged;
      fHasChanged = false;
      return hasChanged;
   }

   private String getProjectLocation() {
      if (fUseDefaultLocationButton.getSelection()) {
         return "";
      }
      return fLocationText.getText().trim();
   }

   private String getProjectOutputType() {
      String outputType = UsbdmConstants.EXE_PROJECT_TYPE_ID;
      if (fCreateStaticLibrary) {
         outputType = UsbdmConstants.STATIC_LIB_PROJECT_TYPE_ID;
      }
      return outputType;
   }
   
   /**
    * Updates paramMap from dialogue contents
    * 
    * @param paramMap
    * 
    * projectName      Name of project being created 
    * projectHomePath  Home path of project (empty indicates default)
    * interfaceType    ARM/Coldfire interface type e.g. T_ARM
    * hasCCNature      Indicates C++ nature (true/false)
    * outputType       net.sourceforge.usbdm.cdt.newProjectType.exe/net.sourceforge.usbdm.cdt.newProjectType.staticLib
    * semiHosting      Indicates semi-hosting is desired (true/false)
    */
   public void getPageData(Map<String, String> paramMap) {
      paramMap.put(UsbdmConstants.PROJECT_NAME_KEY,                 fProjectNameText.getText().trim());
      paramMap.put(UsbdmConstants.PROJECT_HOME_PATH_KEY,            getProjectLocation());
      paramMap.put(UsbdmConstants.INTERFACE_TYPE_KEY,               fInterfaceType.name());
      paramMap.put(UsbdmConstants.HAS_CC_NATURE_KEY,                fHasCCNature.toString());
      paramMap.put(UsbdmConstants.PROJECT_OUTPUT_TYPE_KEY,          getProjectOutputType());
      paramMap.put(UsbdmConstants.SEMI_HOSTING_TYPE_KEY,            Boolean.valueOf(fUseSemiHosting.getSelection()).toString());
      paramMap.put(UsbdmConstants.USE_FLOATINGPOINT_IN_SCANF_KEY,   Boolean.valueOf(fUseFloatingPointInScanf.getSelection()).toString());
      paramMap.put(UsbdmConstants.USE_FLOATINGPOINT_IN_PRINTF_KEY,  Boolean.valueOf(fUseFloatingPointInPrintf.getSelection()).toString());
   }

}