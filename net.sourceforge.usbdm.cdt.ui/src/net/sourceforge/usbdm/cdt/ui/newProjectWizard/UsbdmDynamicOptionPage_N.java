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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 *  USBDM New Project Wizard page "USBDM Project"
 */
public class UsbdmDynamicOptionPage_N extends WizardPage {

   private UsbdmNewProjectOptionsPanel    fUsbdmNewProjectOptionsPanel = null;
   private Device                         fDevice = null;
   private ProjectActionList              fProjectActionList = null;
   private Map<String, String>            fParamMap = null;
   private WizardPageInformation          fWizardPageInfo = null;
   private Composite                      fParent = null;

   public UsbdmDynamicOptionPage_N (
         Device                  device,
         Map<String, String>     paramMap,
         ProjectActionList       projectActionList,
         WizardPageInformation   wizardPageInfo) {
      
      super(wizardPageInfo.getName());
      fDevice              = device;
      fParamMap            = paramMap;
      fProjectActionList   = projectActionList;
      fWizardPageInfo      = wizardPageInfo;
      
      setTitle(wizardPageInfo.getName());
      setDescription(wizardPageInfo.getDescription());
      setPageComplete(false);
//      System.err.println("UsbdmDynamicOptionPage_N() name = " + fWizardPageInfo.getName());
   }

   public void updateControl() {
      Control c = getControl();
      if (c != null) {
         c.dispose();
      }
      fUsbdmNewProjectOptionsPanel = null;

      if ((fProjectActionList == null) || (fParamMap == null)) {
         Text t = new Text(fParent,  SWT.FILL);
         t.setText("Building configuration");
         setControl(t);
         return;
      }

      //      Text t = new Text(fParent,  SWT.FILL);
      //      t.setText("Building configuration #2");
      //      setControl(t);
      //      return;
      try {
         IDialogSettings dialogSettings = getDialogSettings();
         if (dialogSettings == null) {
            System.err.println("UsbdmDynamicOptionPage_N.createControl() dialogSettings == null!");
         }
         fUsbdmNewProjectOptionsPanel = new UsbdmNewProjectOptionsPanel(fParent, SWT.FILL, dialogSettings, fDevice, fProjectActionList, fParamMap, fWizardPageInfo);
         fUsbdmNewProjectOptionsPanel.addListener(SWT.CHANGED, new Listener() {
            @Override
            public void handleEvent(Event event) {
               validate();
            }
         });
         setControl(fUsbdmNewProjectOptionsPanel);
         fParent.layout(true);
      } catch (Exception e) {
         setControl(null);
         e.printStackTrace();
      }
   }
   
   public void setProjectInformation(ProjectActionList projectActionList, Map<String, String> paramMap) {
      fProjectActionList = projectActionList;
      fParamMap          = paramMap;
   }
   
   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
    */
   @Override
   public void createControl(Composite parent) {
      fParent = parent;
      updateControl();
      validate();
   }
   
   /**
    * Updates the internal state
    * This is done on a worker thread as it is time consuming
    * After completion it calls page.setPageComplete() to notify wizard of changes
    */
    void updateState() {
//      System.err.println("UsbdmDynamicOptionPage_N.updateState()");
      final UsbdmNewProjectWizard wizard = (UsbdmNewProjectWizard) getWizard();
      final UsbdmDynamicOptionPage_N page = this;
      
      if (wizard != null) {
         Job job = new Job("Updating configuration") {
            protected IStatus run(IProgressMonitor monitor) {
               monitor.beginTask("Updating Pages...", 10);
               wizard.updateParamMap(page);
               Display.getDefault().syncExec(new Runnable() {
                  @Override
                  public void run() {
//                     System.err.println("UsbdmDynamicOptionPage_N.page.setPageComplete()");
                     page.setPageComplete(true);
                  }
               });
               monitor.done();
               return Status.OK_STATUS;
            }
         };
         job.setUser(true);
         job.schedule();
      }      
   }

   /**
    *  Validates control & returns error message
    * @return 
    * @return 
    *  
    * @return Error message (null if none)
    */
   public void validate() {
//      System.err.println("UsbdmDynamicOptionPage_N.validate()");
      String message = null;
      setPageComplete(false);
      if (fProjectActionList == null) {
         message = "Building configuration";
      }
      else if (fUsbdmNewProjectOptionsPanel != null) {
         message = fUsbdmNewProjectOptionsPanel.validate();
      }
//      System.err.println("UsbdmProjectOptionsPage.validate() - " + message);
      setErrorMessage(message);
      if (message == null) {
         updateState();
      }
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

   static public Map<String, String> populateMap() {
      
      Map<String, String> paramMap = new HashMap<String, String>();
      paramMap.put("bdmTargetVdd",                             "BDM_TARGET_VDD_3V3");
      paramMap.put("buildToolsBinPath",                        "${usbdm_armLtd_arm_path}");
      paramMap.put("buildToolsId",                             "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.armLtdGnuToolsForARM");
      paramMap.put("cDeviceParameters",                        "");
      paramMap.put("clockTrimFrequency",                       "0");
      paramMap.put("cpp.peripheral.external.max30100",         "true");
      paramMap.put("cpp.peripheral.internal.adc",              "true");
      paramMap.put("cpp.peripheral.internal.ftm",              "true");
      paramMap.put("cpp.peripheral.internal.i2c",              "true");
      paramMap.put("cpp.peripheral.internal.lptmr",            "true");
      paramMap.put("cpp.peripheral.internal.pit",              "true");
      paramMap.put("cpp.peripheral.internal.spi",              "true");
      paramMap.put("demo.cpp.console",                         "USBDM::Uart##0");
      paramMap.put("demo.cpp.led",                             "GpioA<2,ActiveLow>");
      paramMap.put("demo.lcd.elecfreaks.backlight",            "GpioC<2>");
      paramMap.put("demo.lcd.elecfreaks.backlight.ftm",        "Ftm0<1>");
      paramMap.put("demo.lcd.elecfreaks.cs",                   "GpioA<2>");
      paramMap.put("demo.lcd.elecfreaks.reset",                "GpioA<12>");
      paramMap.put("eraseMethod",                              "TargetDefault");
      paramMap.put("externalHeaderFile",                       "C:/Program Files (x86)/pgo/USBDM 4.12.1.261/Stationery/Project_Headers/MK20D5.h");
      paramMap.put("externalHeaderFilename",                   "MK20D5.h");
      paramMap.put("externalVectorTable",                      "");
      paramMap.put("gdbCommand",                               "${usbdm_armLtd_arm_prefix}gdb");
      paramMap.put("hasCCNature",                              "true");
      paramMap.put("interfaceType",                            "T_ARM");
      paramMap.put("linkerExtraRegions",                       "  ");
      paramMap.put("linkerFlashSize",                          "0x20000");
      paramMap.put("linkerHeapSize",                           "0x1000");
      paramMap.put("linkerRamSize",                            "0x2000");
      paramMap.put("linkerStackSize",                          "0x1000");
      paramMap.put("nvmClockTrimLocation",                     "0");
      paramMap.put("outputType",                               "net.sourceforge.usbdm.cdt.newProjectType.exe");
      paramMap.put("pathSeparator",                            "\\");
      paramMap.put("projectHomePath",                          "");
      paramMap.put("projectName",                              "xx");
      paramMap.put("projectOptionValue.CMSIS-RTOS",            "false");
      paramMap.put("projectOptionValue.CPP-abstraction",       "true");
      paramMap.put("projectOptionValue.CPP-abstraction-LCD",   "false");
      paramMap.put("projectOptionValue.FRDM-Blinky",           "false");
      paramMap.put("projectOptionValue.FreeRTOS",              "false");
      paramMap.put("projectOptionValue.Kinetis-PE",            "false");
      paramMap.put("resetMethod",                              "TargetDefault");
      paramMap.put("semiHosting",                              "false");
      paramMap.put("targetDeviceFamily",                       "CortexM4");
      paramMap.put("useFloatingpointInPrintf",                 "false");
      paramMap.put("useFloatingpointInScanf",                  "false");
      
      return paramMap;
   }
   
   static void delayedLoad(final Map<String, String> paramMap, final Device device, final UsbdmDynamicOptionPage_N page) {
      Job job = new Job("Updating configuration") {
         protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask("Updating Pages...", 10);
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
               return Status.CANCEL_STATUS;
            }
            final ProjectActionList projectActionList = device.getProjectActionList(paramMap);
            Display.getDefault().syncExec(new Runnable() {

               @Override
               public void run() {
                  page.setProjectInformation(projectActionList, paramMap);
               }
            });
            monitor.done();
            return Status.OK_STATUS;
         }
      };
      job.setUser(true);
      job.schedule();
   }
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
      shell.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
      
      FillLayout fillLayout = new FillLayout();
      fillLayout.type = SWT.HORIZONTAL;
      shell.setLayout(fillLayout);
      
      Composite composite = new Composite(shell, SWT.NONE);
      fillLayout = new FillLayout();
      fillLayout.type = SWT.HORIZONTAL;
      composite.setBackground(display.getSystemColor(SWT.COLOR_RED));
      composite.setLayout(fillLayout);

      String targetDevice = "FRDM_K20D50M";
      
      Map<String, String> paramMap = populateMap();
      paramMap.put("targetDevice",                  targetDevice);
      paramMap.put("cpp.peripheral.subfamily",      "MK20D5");
      paramMap.put("targetDeviceName",              "frdm_k20d50m");
      paramMap.put("targetDevice",                  "FRDM_K20D50M");
      paramMap.put("targetDeviceSubFamily",         "MK20D5");
      
////      UsbdmDynamicOptionPage_N page = new UsbdmDynamicOptionPage_N(targetDevice, paramMap, "usbdm-project-options-page");
////      UsbdmDynamicOptionPage_N page = new UsbdmDynamicOptionPage_N(targetDevice, paramMap, "kinetis-CPP-abstraction-options-page");
//      WizardPageInformation wizardPageInfo = new WizardPageInformation("kinetis-sdk-options-page", "Kinetis", "Kinetis description");
      WizardPageInformation wizardPageInfo = new WizardPageInformation("usbdm-project-options-page", "USBDM Project Options", "Select Project Options");
      DeviceDatabase deviceDatabase = DeviceDatabase.getDeviceDatabase(TargetType.T_ARM);
      Device device = deviceDatabase.getDevice(targetDevice);

      UsbdmDynamicOptionPage_N page = new UsbdmDynamicOptionPage_N(device, paramMap, null, wizardPageInfo);
      page.createControl(composite);
//      composite.layout();
      
      delayedLoad(paramMap, device, page);

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
