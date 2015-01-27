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
public class UsbdmProjectOptionsPage extends WizardPage {

   // These constants are used both for the dialogue persistent storage AND the page data map keys
   private final static String PAGE_ID    = UsbdmConstants.PROJECT_OPTIONS_PAGE_ID;
   private final static String PAGE_NAME  = UsbdmConstants.PROJECT_OPTIONS_PAGE_NAME;

   private UsbdmProjectParametersPage   usbdmProjectPage;
   private UsbdmProjectOptionsPanel     usbdmProjectOptionsPanel;
   private Map<String, String>          paramMap;
   
   public UsbdmProjectOptionsPage(UsbdmProjectParametersPage usbdmProjectPage, Map<String, String> paramMap) {
      super(PAGE_NAME);
      this.usbdmProjectPage         = usbdmProjectPage;
      this.usbdmProjectOptionsPanel = null;
      this.paramMap                 = paramMap;
      
      setTitle("USBDM Project Options");
      setDescription("Select project options");
      setPageComplete(true);
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
         usbdmProjectOptionsPanel = new UsbdmProjectOptionsPanel(parent, SWT.NONE, getDevice(), dialogSettings, paramMap);
         usbdmProjectOptionsPanel.addListener(SWT.CHANGED, new Listener() {
            @Override
            public void handleEvent(Event event) {
               validate();
            }
         });
         setControl(usbdmProjectOptionsPanel);
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
      if (usbdmProjectOptionsPanel != null) {
         message = usbdmProjectOptionsPanel.validate();
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
      if (usbdmProjectPage == null) {
         return getDevice("FRDM-K20D50M");
      }
      return usbdmProjectPage.getDevice();
   }
   
   /**
    *   Gets parameters from options page
    *   
    *   @param paramMap
    */
   public void getPageData(Map<String, String> paramMap) {
      if (usbdmProjectOptionsPanel != null) {
         usbdmProjectOptionsPanel.getPageData(paramMap);
      }
   }

   /**
    *    Save dialog settings
    */
   public void saveSettings() {
      IDialogSettings dialogSettings = getDialogSettings();
      if ((usbdmProjectOptionsPanel != null) && (dialogSettings != null)) {
         usbdmProjectOptionsPanel.saveSettings(dialogSettings);
      }
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

      Map<String, String> paramMap = new HashMap<String, String>();
      paramMap.put("linkerFlashSize", "0x100");
      paramMap.put("linkerRamSize",   "0x100");
      UsbdmProjectOptionsPage page = new UsbdmProjectOptionsPage(null, paramMap);
      page.createControl(composite);

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      Map<String, String> map = new HashMap<String, String>();
      page.getPageData(map);
      display.dispose();
   }

}
