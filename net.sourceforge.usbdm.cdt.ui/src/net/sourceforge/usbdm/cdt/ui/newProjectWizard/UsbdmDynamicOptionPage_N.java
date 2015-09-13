package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

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
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.packageParser.ProjectActionList;
import net.sourceforge.usbdm.packageParser.WizardPageInformation;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 *  USBDM New Project Wizard page "USBDM Project"
 */
public class UsbdmDynamicOptionPage_N extends WizardPage {

   private UsbdmNewProjectOptionsPanel    fUsbdmNewProjectOptionsPanel = null;
   private Device                         fDevice = null;
   private ProjectActionList              fProjectActionList = null;
   private Map<String, String>            fParamMap = null;
   private WizardPageInformation          fWizardPageInfo = null;

   public UsbdmDynamicOptionPage_N (
         Device                  device, 
         ProjectActionList       projectActionList,
         WizardPageInformation   wizardPageInfo) {
      
      super(wizardPageInfo.getName());
      fDevice              = device;
      fProjectActionList   = projectActionList;
      fParamMap            = null;
      fWizardPageInfo      = wizardPageInfo;
      
      setTitle(wizardPageInfo.getName());
      setDescription(wizardPageInfo.getDescription());
      setPageComplete(false);
//      System.err.println("UsbdmDynamicOptionPage_N() name = " + fWizardPageInfo.getName());
   }

   public String getPageID() {
      return fWizardPageInfo.getId();
   }

   
   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
    */
   @Override
   public void createControl(Composite parent) {

      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings == null) {
         System.err.println("UsbdmDynamicOptionPage_N.createControl() dialogSettings == null!");
      }
      try {
         UsbdmNewProjectWizard wizard = (UsbdmNewProjectWizard) getWizard();
         fParamMap = wizard.getparamMap();
         fUsbdmNewProjectOptionsPanel = new UsbdmNewProjectOptionsPanel(parent, SWT.NONE, dialogSettings, fDevice, fProjectActionList, fParamMap, fWizardPageInfo);
         fUsbdmNewProjectOptionsPanel.addListener(SWT.CHANGED, new Listener() {
            @Override
            public void handleEvent(Event event) {
               validate();
            }
         });
         setControl(fUsbdmNewProjectOptionsPanel);
      } catch (Exception e) {
         setControl(null);
         e.printStackTrace();
      }
      validate();
   }
   
   /**
    *  Validates control & returns error message
    * @return 
    * @return 
    *  
    * @return Error message (null if none)
    */
   public void validate() {
      String message = null;
      if (fUsbdmNewProjectOptionsPanel != null) {
         message = fUsbdmNewProjectOptionsPanel.validate();
      }
//      System.err.println("UsbdmProjectOptionsPage.validate() - " + message);
      setErrorMessage(message);
      setPageComplete(message == null);
   }
   
   /**
    *   Gets parameters from options page
    *   
    *   @param paramMap
    */
   public void getPageData(Map<String, String> paramMap) {
      if (fUsbdmNewProjectOptionsPanel != null) {
         fUsbdmNewProjectOptionsPanel.getButtonData(paramMap);
      }
   }   
   
//   /**
//    *   Gets parameters from options page
//    *   
//    *   @param paramMap
//    */
//   public void getPageData(Map<String, String> paramMap) {
//      if (fUsbdmNewProjectOptionsPanel != null) {
//         fUsbdmNewProjectOptionsPanel.getPageData(paramMap);
//      }
//   }

   /**
    *    Save dialog settings
    */
   public void saveSettings() {
      if (fUsbdmNewProjectOptionsPanel != null) {
         fUsbdmNewProjectOptionsPanel.saveSettings();
      }
   }
   
   /**
    * Get Project action lists
    * 
    * @return
    */
   public ProjectActionList getProjectActionLists() {
      return fUsbdmNewProjectOptionsPanel.getProjectActionList();
   }
   
//   @Override
//   public boolean canFlipToNextPage() {
//      return isPageComplete();
//   }

   /**
    * Test main
    * 
    * @param args
    * @throws Exception 
    */
   public static void main(String[] args) throws Exception {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Packages Available");
      shell.setLayout(new FillLayout());

      Composite composite = new Composite(shell, SWT.NONE);
      composite.setLayout(new FillLayout());

      String deviceName = "FRDM_K22F";
//      String deviceName = "FRDM_KL27Z";
//      String deviceName = "MKL27Z64M4";

      Map<String, String> paramMap = new HashMap<String, String>();

      paramMap.put("linkerFlashSize", "0x100");
      paramMap.put("linkerRamSize",   "0x100");
      paramMap.put("outputType",      "xxxxxProjectType.exe");
      paramMap.put("targetDevice",    deviceName);
//      UsbdmDynamicOptionPage_N page = new UsbdmDynamicOptionPage_N(deviceName, paramMap, "usbdm-project-options-page");
//      UsbdmDynamicOptionPage_N page = new UsbdmDynamicOptionPage_N(deviceName, paramMap, "kinetis-CPP-abstraction-options-page");
      WizardPageInformation wizardPageInfo = new WizardPageInformation("kinetis-sdk-options-page", "Kinetis", "Kinetis description");
      DeviceDatabase deviceDatabase = new DeviceDatabase(TargetType.T_ARM);
      Device device = deviceDatabase.getDevice(deviceName);
      ProjectActionList projectActionList = device.getProjectActionList(paramMap);
      UsbdmDynamicOptionPage_N page = new UsbdmDynamicOptionPage_N(device, projectActionList, wizardPageInfo);
      page.createControl(composite);

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      page.getPageData(paramMap);
      display.dispose();
   }
   
   public boolean hasChanged() {
      return fUsbdmNewProjectOptionsPanel.hasChanged();
   }

}
