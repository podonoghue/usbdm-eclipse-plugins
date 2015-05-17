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

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.packageParser.ProjectActionList;

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
public class UsbdmProjectOptionsPage_3 extends WizardPage {

   // These constants are used both for the dialogue persistent storage AND the page data map keys
   private final static String PAGE_ID    = UsbdmConstants.PROJECT_OPTIONS_PAGE_ID;
   private final static String PAGE_NAME  = UsbdmConstants.PROJECT_OPTIONS_PAGE_NAME;

   private UsbdmProjectParametersPage_2   fUsbdmProjectPage = null;
   private UsbdmNewProjectOptionsPanel    fUsbdmNewProjectOptionsPanel = null;
   private Map<String, String>            fParamMap = null;
   private String                         fDeviceName = null;              
   
   public UsbdmProjectOptionsPage_3(UsbdmProjectParametersPage_2 usbdmProjectPage, Map<String, String> paramMap) {
      super(PAGE_NAME);
      fUsbdmProjectPage             = usbdmProjectPage;
      fUsbdmNewProjectOptionsPanel  = null;
      fParamMap                     = paramMap;
      fDeviceName                   = null;
      
      setTitle("USBDM Project Options");
      setDescription("Select project options");
      setPageComplete(false);
   }

   private UsbdmProjectOptionsPage_3(String deviceName, Map<String, String> paramMap) {
      super(PAGE_NAME);
      fUsbdmProjectPage             = null;
      fUsbdmNewProjectOptionsPanel  = null;
      fParamMap                     = paramMap;
      fDeviceName                   = deviceName;
      
      setTitle("USBDM Project Options");
      setDescription("Select project options");
      setPageComplete(false);
   }

   public String getPageID() {
      return PAGE_ID;
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
    */
   @Override
   public void createControl(Composite parent) {

      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings == null) {
         System.err.println("UsbdmProjectOptionsPage.createControl() dialogSettings == null!");
      }
      try {
         fUsbdmNewProjectOptionsPanel = new UsbdmNewProjectOptionsPanel(parent, SWT.NONE, getDevice(), dialogSettings, fParamMap);
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
    *    For debug only
    * 
    *    @param deviceName
    *    @return
    */
   private static Device getDevice(String deviceName) {
      DeviceDatabase deviceDatabase = new DeviceDatabase(TargetType.T_ARM);
      if (!deviceDatabase.isValid()) {
         return null;
      }
      return deviceDatabase.getDevice(deviceName);
   }

   private Device getDevice() {
      if (fUsbdmProjectPage == null) {
         return getDevice(fDeviceName);
      }
      return fUsbdmProjectPage.getDevice();
   }
   
   /**
    *   Gets parameters from options page
    *   
    *   @param paramMap
    * @throws Exception 
    */
   public void getPageData(Map<String, String> paramMap) throws Exception {
      if (fUsbdmNewProjectOptionsPanel != null) {
         fUsbdmNewProjectOptionsPanel.getPageData(paramMap);
      }
   }

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
   
   /**
    * Test main
    * 
    * @param args
    */
   public static void main(String[] args) {
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
      UsbdmProjectOptionsPage_3 page = new UsbdmProjectOptionsPage_3(deviceName, paramMap);
      page.createControl(composite);

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      try {
         page.getPageData(paramMap);
      } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      display.dispose();
   }

}
