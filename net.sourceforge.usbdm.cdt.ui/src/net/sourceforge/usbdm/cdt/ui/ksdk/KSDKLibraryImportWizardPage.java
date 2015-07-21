package net.sourceforge.usbdm.cdt.ui.ksdk;

/*
 Change History
+============================================================================================
| Revision History
+============================================================================================
| 28 Dec 14 | Added requirements                                                  4.10.6.250
| 16 Nov 13 | Fixed path lookup for resource files (e.g. header files) on linux   4.10.6.100
| 16 Nov 13 | Added default files header & vector files based upon subfamily      4.10.6.100
+============================================================================================
*/
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.packageParser.ProjectActionList;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

/**
 *  USBDM KDS Library Import Wizard page
 */
public class KSDKLibraryImportWizardPage extends WizardPage {

   // These constants are used both for the dialogue persistent storage AND the page data map keys
   private final static String PAGE_NAME  = UsbdmConstants.PROJECT_OPTIONS_PAGE_NAME;

   private KSDKLibraryImportOptionsPanel fKSDKLibraryImportOptionsPanel;
   
   public KSDKLibraryImportWizardPage() {
      super(PAGE_NAME);
      this.fKSDKLibraryImportOptionsPanel = null;
      
      setTitle("Create KDS Library Project");
      setDescription("Select options for creation of library project");
      setPageComplete(false);
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
    */
   @Override
   public void createControl(Composite parent) {

      URI kdsPath = null; 
      try {
         IWorkspace workspace         = ResourcesPlugin.getWorkspace();
         IPathVariableManager pathMan = workspace.getPathVariableManager();
         kdsPath                      = pathMan.getURIValue(UsbdmSharedConstants.USBDM_KSDK_PATH);
      } catch (Exception e) {
         try {
            kdsPath = new URI("C:/Apps/Freescale/KSDK_1.1.0");
         } catch (URISyntaxException e1) {
            e1.printStackTrace();
         }
      }
      if (kdsPath == null) {
         Label label = new Label(parent, SWT.NONE);
         label.setText("Path to Kinetis SDK installation is invalid\n"
               + "Please set path on USBDM configuration page");
         setControl(label);
         return;
      }
      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings == null) {
         System.err.println("UsbdmProjectOptionsPage.createControl() dialogSettings == null!");
      }
      try {
         fKSDKLibraryImportOptionsPanel = new KSDKLibraryImportOptionsPanel(parent, SWT.NONE);
         fKSDKLibraryImportOptionsPanel.addListener(SWT.CHANGED, new Listener() {
            @Override
            public void handleEvent(Event event) {
               validate();
            }
         });
         setControl(fKSDKLibraryImportOptionsPanel);
      } catch (Exception e) {
         Label label = new Label(parent, SWT.NONE);
         label.setText("Exception while creating page\n" + "Reason: " + e.getMessage());
         setControl(label);
         e.printStackTrace();
         return;
      }
      validate();
   }
   
   /**
    *  Validates control & returns error message
    *  
    * @return 
    * @return 
    *  
    * @return Error message (null if none)
    */
   public void validate() {
      String message = null;
      if (fKSDKLibraryImportOptionsPanel != null) {
         message = fKSDKLibraryImportOptionsPanel.validate();
      }
      setErrorMessage(message);
      setPageComplete(message == null);
   }

   /**
    *   Gets parameters from options page
    *   
    *   @param paramMap
    * @throws Exception 
    */
   public void getPageData(Map<String, String> paramMap) throws Exception {
      if (fKSDKLibraryImportOptionsPanel != null) {
         fKSDKLibraryImportOptionsPanel.getPageData(paramMap);
      }
   }

   /**
    *    Save dialog settings
    */
   public void saveSettings() {
      IDialogSettings dialogSettings = getDialogSettings();
      if ((fKSDKLibraryImportOptionsPanel != null) && (dialogSettings != null)) {
         fKSDKLibraryImportOptionsPanel.saveSettings();
      }
   }
   
   /**
    * Get the project action list as configured on the Wizard page
    *  
    * @return the fUsbdmKDSImportLibraryOptionsPanel
    */
   public ProjectActionList getProjectActionList() {
      return fKSDKLibraryImportOptionsPanel.getProjectActionList();
   }

}
