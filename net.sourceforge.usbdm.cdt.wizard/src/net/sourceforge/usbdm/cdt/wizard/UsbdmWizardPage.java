package net.sourceforge.usbdm.cdt.wizard;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.usbdm.cdt.UsbdmCdtConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryRegion;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryRegion.MemoryRange;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.UsbdmException;

import org.eclipse.cdt.ui.templateengine.IWizardDataPage;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author pgo
 *
 */
/**
 *  USBDM Wizard page
 *
 */
/**
 * @author pgo
 *
 */
public class UsbdmWizardPage extends WizardPage implements IWizardDataPage {

   public String getPageID() {
//      System.err.println("WizardPage1.getPageID() => " + pageID);
      return pageID;
   }

   private String             pageID;
   private InterfaceType      deviceType;

   private Combo              targetDeviceName;

   private Button             autoGenerateLinkerScript;
   private Label              externalLinkerScript;
   private Button             browseButton;

   private Text               crossCompilerPrefix;
   private Text               crossCompilerPath;
   private Button             crossCompilerPathBrowseButton;
   
   private IWizardPage        nextPage;
   
   private final String       gccCommand; // gcc or gcc.exe
   private final String       crossCommandPathVariableKey; 
   private final String       crossCommandPrefixVariableKey;
   
   private IStringVariableManager manager = null;

   public UsbdmWizardPage(InterfaceType deviceType) throws UsbdmException {
      super(Messages.USBDM_PARAMETERS);
      pageID          = UsbdmConstants.PAGE_ID + deviceType.toString();
      this.deviceType = deviceType;
      setTitle(Messages.USBDM_INTERFACE);
      setDescription(Messages.USBDM_PARAMETERS);
      setPageComplete(false);
      String os = System.getProperty("os.name");
      if ((os != null) && os.toUpperCase().contains("LINUX")) {
         gccCommand = UsbdmConstants.GCC_COMMAND_LINUX;
      }
      else {
         gccCommand = UsbdmConstants.GCC_COMMAND_WINDOWS;
      }
      switch (deviceType) {
      case T_ARM  : 
         crossCommandPathVariableKey   = UsbdmConstants.CODESOURCERY_ARM_PATH_KEY;      
         crossCommandPrefixVariableKey = UsbdmConstants.CODESOURCERY_ARM_PREFIX_KEY; 
         break;
      case T_CFV1 : 
         crossCommandPathVariableKey   = UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY; 
         crossCommandPrefixVariableKey = UsbdmConstants.CODESOURCERY_COLDFIRE_PREFIX_KEY; 
         break;
      case T_CFVX : 
         crossCommandPathVariableKey   = UsbdmConstants.CODESOURCERY_COLDFIRE_PATH_KEY; 
         crossCommandPrefixVariableKey = UsbdmConstants.CODESOURCERY_COLDFIRE_PREFIX_KEY; 
         break;
      default: throw new UsbdmException("Unexpected device type");
      }
      VariablesPlugin variablesPlugin = VariablesPlugin.getDefault();
      if (variablesPlugin != null) {
         manager = variablesPlugin.getStringVariableManager();
      }
   }

   /**
    *  Validates control & sets error message
    *  
    * @param message error message (null if none)
    */
   private void validate() {
//      IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();

      String message = null;
      String crossPath = crossCompilerPath.getText();
//      try {
//       System.err.println("UsbdmInterface.doRemote() Before substitution spritePath = \'" + crossPath + "\"");
//         crossPath = manager.performStringSubstitution(crossPath);
//         System.err.println("UsbdmInterface.doRemote() After substitution spritePath = \'" + crossPath + "\"");
//      } catch (CoreException e) {
//         e.printStackTrace();
//      }
      IPath gccPath = new Path(crossPath).append("bin").append(crossCompilerPrefix.getText()+gccCommand);
      File gccFile = gccPath.toFile();
      if (!gccFile.isFile() || !gccFile.canExecute()) {
         message = Messages.ERR_GCC_PATH_OR_PREFIX_INVALID;
      }
      if (!autoGenerateLinkerScript.getSelection()) {
         IPath linkerPath = new Path(externalLinkerScript.getText());
         File linkerFile = linkerPath.toFile();
         if (!linkerFile.isFile() || !linkerFile.canRead()) {
            message = Messages.ERR_LINKER_SCRIPT_PATH_INVALID; 
         }
      }
      if (!targetDeviceName.isEnabled()) {
         message = Messages.ERR_DEVICE_DATABASE_INVALID;
      }
      setErrorMessage(message);
      setPageComplete(message == null);
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
    */
   @Override
   public void createControl(Composite parent) {
      //      System.err.println("createControl");
      Composite control = new Composite(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      control.setLayout(layout);
      createUsbdmControl(control);
      setControl(control);
      updateLinker();
      updateDevice();
   }

   private void updateDevice() {
      String currentDevice = targetDeviceName.getText();
      targetDeviceName.removeAll();
      DeviceDatabase deviceDatabase = new DeviceDatabase(deviceType.deviceFile);
      if (!deviceDatabase.isValid()) {
         targetDeviceName.add(Messages.ERR_DEVICE_DATABASE_NOT_FOUND);
         targetDeviceName.setEnabled(false);
      }
      else {
         Iterator<Device> it = deviceDatabase.iterator();
         while(it.hasNext()) {
            Device device = it.next();
            if (!device.isAlias()) {
               targetDeviceName.add(device.getName());
            }
         }
      }
      // Try to restore original device
      targetDeviceName.setText(currentDevice);
      int targetDeviceIndex = targetDeviceName.getSelectionIndex();
      if (targetDeviceIndex<0) {
         targetDeviceName.select(0);
      }
   }

   private void updateLinker() {
//      System.err.println("updateLinker()"); //$NON-NLS-1$
      if (autoGenerateLinkerScript.getSelection()) {
         externalLinkerScript.setEnabled(false);
         browseButton.setEnabled(false);
      }
      else {
         externalLinkerScript.setEnabled(true);
         browseButton.setEnabled(true);
      }
      validate();
   }

   private void createUsbdmControl(Composite parent) {
      GridData gd;
      Control  control;
      parent.setLayout(new GridLayout());

      control = createUsbdmParametersControl(parent);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);

      control = createLinkerParametersControl(parent);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);

      control = createCrossGccControl(parent);
      gd = new GridData(SWT.FILL, SWT.NONE, true, false);
      control.setLayoutData(gd);
   }

   private Control createUsbdmParametersControl(Composite parent) {
      GridLayout layout;

      Group group = new Group(parent, SWT.NONE);
      GridData gd = new GridData(SWT.FILL, SWT.NONE, true, true);
      group.setLayoutData(gd);
      layout = new GridLayout();
      group.setLayout(layout);
      group.setText(Messages.NAME_USBDM_PARAMETERS);

      //
      // Create & Populate Combo for USBDM devices
      //
      Composite composite = new Composite(group, SWT.NO_TRIM | SWT.NO_FOCUS);
      layout = new GridLayout(2, false);
      composite.setLayout(layout);
      composite.setBackground(group.getParent().getBackground());

      Label label = new Label(composite, SWT.NONE);
      label.setText("Target Device:"); //$NON-NLS-1$
      targetDeviceName = new Combo(composite, SWT.BORDER|SWT.READ_ONLY);
      gd = new GridData();
      gd.widthHint = 200;
      targetDeviceName.setLayoutData(gd);
      targetDeviceName.select(0);
      updateDevice();

      IDialogSettings dialogSettings = getDialogSettings();
//      System.err.println("getDialogSettings() => " + dialogSettings);             //$NON-NLS-1$
      if (dialogSettings != null) {
         String attrValue = dialogSettings.get(deviceType.name()+UsbdmConstants.TARGET_DEVICE_KEY);
         if (attrValue != null) {
            targetDeviceName.setText(attrValue);
         }
      }
      return group;
   }

   private Control createLinkerParametersControl(final Composite parent) {
      GridLayout layout;

      Group group = new Group(parent, SWT.NONE);
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
      group.setLayoutData(gd);
      group.setText(Messages.NAME_LINKER_PARAMETERS);
      //
      layout = new GridLayout();
      group.setLayout(layout);

      //
      // Custom linker file checkbox
      //
      autoGenerateLinkerScript = new Button(group, SWT.CHECK);
      autoGenerateLinkerScript.setText(Messages.NAME_AUTO_LINKER_SCRIPT);
      autoGenerateLinkerScript.setToolTipText(Messages.TOOL_TIP_AUTO_LINKER_SCRIPT);
      autoGenerateLinkerScript.addSelectionListener(new SelectionListener() {
         public void widgetSelected(SelectionEvent e) {
            updateLinker();
         }
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });
      //
      // Linker file browse
      //
      Composite composite = new Composite(group, SWT.NO_TRIM | SWT.NO_FOCUS);
      gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      composite.setLayoutData(gd);
      layout = new GridLayout(3, false);
      composite.setLayout(layout);
      composite.setBackground(group.getParent().getBackground());

      Label label = new Label(composite, SWT.NONE);
      gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
      label.setLayoutData(gd);
      label.setText("Linker script:"); 

      externalLinkerScript = new Label(composite, SWT.BORDER);
      gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
      externalLinkerScript.setLayoutData(gd);
      externalLinkerScript.setToolTipText(Messages.TOOL_TIP_EXTERNAL_LINKER_SCRIPT);
      //      linkerScript.setEditable(false);
      browseButton = new Button(composite, SWT.PUSH);
      gd = new GridData(SWT.FILL, SWT.FILL, false, false);
      browseButton.setLayoutData(gd);
      browseButton.setText(Messages.NAME_BROWSE);      
      browseButton.setToolTipText(Messages.TOOL_TIP_BROWSE_LINKER_SCRIPT);
      browseButton.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            String[] filterExts = {"*.ld"}; //$NON-NLS-1$
            FileDialog fd = new org.eclipse.swt.widgets.FileDialog(parent.getShell(), SWT.OPEN);
            fd.setFileName(externalLinkerScript.getText());

            fd.setText("USBDM - Select Linker File");
            fd.setFilterExtensions(filterExts);
            String fileName = fd.open();
            if (fileName != null) {
               externalLinkerScript.setText(fileName);
            }
            updateLinker();
         }

         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
//            System.err.println("widgetDefaultSelected()"); //$NON-NLS-1$
         }}
            );

      autoGenerateLinkerScript.setSelection(true);
      externalLinkerScript.setText(""); //$NON-NLS-1$
      IDialogSettings dialogSettings = getDialogSettings();
//      System.err.println("getDialogSettings() => " + dialogSettings);             //$NON-NLS-1$
      if (dialogSettings != null) {
         String stringAttrValue = dialogSettings.get(deviceType.name()+UsbdmConstants.EXTERNAL_LINKER_SCRIPT_KEY);
         if (stringAttrValue != null) {
            externalLinkerScript.setText(stringAttrValue);
         }
         autoGenerateLinkerScript.setSelection(!dialogSettings.getBoolean(deviceType.name()+UsbdmConstants.DONT_GENERATE_LINKER_SCRIPT_KEY));
      }
      return group;
   }

   private Control createCrossGccControl(final Composite parent) {
      GridLayout layout;

      Group group = new Group(parent, SWT.NONE);
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
      group.setLayoutData(gd);
      group.setText("GCC Cross Compiler Parameters");
      //
      layout = new GridLayout();
      group.setLayout(layout);

      //
      // Cross compiler prefix
      //
      Composite composite = new Composite(group, SWT.NO_TRIM | SWT.NO_FOCUS);
      gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      composite.setLayoutData(gd);
      layout = new GridLayout(2, false);
      composite.setLayout(layout);
      composite.setBackground(group.getParent().getBackground());

      Label label = new Label(composite, SWT.NONE);
      gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
      label.setLayoutData(gd);
      label.setText("Prefix:");

      crossCompilerPrefix = new Text(composite, SWT.BORDER);
      gd = new GridData(SWT.NONE, SWT.LEFT, true, false);
      gd.widthHint = 150;
      crossCompilerPrefix.setLayoutData(gd);
      crossCompilerPrefix.setToolTipText("Prefix for Cross Compiler commands e.g. arm-none-eabi-");
      crossCompilerPrefix.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            validate();
         }
      });
      //
      // Cross compiler path
      //
      composite = new Composite(group, SWT.NO_TRIM | SWT.NO_FOCUS);
      gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      composite.setLayoutData(gd);
      layout = new GridLayout(3, false);
      composite.setLayout(layout);
      composite.setBackground(group.getParent().getBackground());

      label = new Label(composite, SWT.NONE);
      gd = new GridData(SWT.NONE, SWT.CENTER, false, false);
      label.setLayoutData(gd);
      label.setText("Path:");

      crossCompilerPath = new Text(composite, SWT.BORDER);
      gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
      crossCompilerPath.setLayoutData(gd);
      crossCompilerPath.setToolTipText("Path to Codesourcery Directory");
      crossCompilerPath.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            validate();
         }
      });
      //      linkerScript.setEditable(false);
      crossCompilerPathBrowseButton = new Button(composite, SWT.PUSH);
      gd = new GridData(SWT.FILL, SWT.FILL, false, false);
      crossCompilerPathBrowseButton.setLayoutData(gd);
      crossCompilerPathBrowseButton.setText(Messages.NAME_BROWSE);      
      crossCompilerPathBrowseButton.setToolTipText("Browse for GCC directory");
      crossCompilerPathBrowseButton.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            DirectoryDialog fd = new org.eclipse.swt.widgets.DirectoryDialog(parent.getShell(), SWT.OPEN);
            fd.setText("Codesourcery - Select Directory");
            fd.setMessage("Locate Codesourcery installation directory (" + deviceType.toString() + ")");
            fd.setFilterPath(crossCompilerPath.getText());
            String directoryPath = fd.open();
            if (directoryPath != null) {
               crossCompilerPath.setText(directoryPath);
            }
            validate();
         }
         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
//            System.err.println("widgetDefaultSelected()"); //$NON-NLS-1$
         }}
            );
//      crossCompilerPath.setText("");//$NON-NLS-1$
//      crossCompilerPrefix.setText(deviceType.prefix);
      IDialogSettings dialogSettings = getDialogSettings();
      if (dialogSettings != null) {
//         String stringAttrValue = dialogSettings.get(deviceType.name()+Constants.CROSS_COMMAND_PATH_KEY);
//         if (stringAttrValue != null) {
//            crossCompilerPath.setText(stringAttrValue);
//         }
         String stringAttrValue = dialogSettings.get(deviceType.name()+UsbdmConstants.CROSS_COMMAND_PREFIX_KEY);
         if (stringAttrValue != null) {
            crossCompilerPrefix.setText(stringAttrValue);
         }
      }
      // Get initial value from Variable manager
      crossCompilerPath.setText(getVariableValue(crossCommandPathVariableKey,     "",                "Path to Codesourcery directory"));
      crossCompilerPrefix.setText(getVariableValue(crossCommandPrefixVariableKey, deviceType.prefix, "Codesourcery command prefix"));

      validate();
      return group;
   }

   String getVariableValue(String key, String defaultValue, String description) {
      // Get initial value from Variable manager
      if (manager != null) {
         IValueVariable variable = manager.getValueVariable(key);
         if (variable == null) {
            variable = manager.newValueVariable(key, description);
            variable.setValue(defaultValue);
         }
         return variable.getValue();
      }
      return "";
   }
   
   private void setVariableValue(String key, String value) {
      // Get initial value from Variable manager
      if (manager != null) {
         IValueVariable variable = manager.getValueVariable(key);
         if (variable != null) {
            variable.setValue(value);
         }
      }
   }
   
   static String getRangeSuffix(int count) {
      if (count == 0) {
         return "   "; //$NON-NLS-1$
      }
      else if (count <= 9) {
         return "_" + Integer.toString(count) + " "; //$NON-NLS-1$ //$NON-NLS-2$
      }
      else {
         return "_" + Integer.toString(count); //$NON-NLS-1$
      }
   }

   private String createLinkerMemoryMap(Device device) {
      int flashRangeCount = 0;
      int ramRangeCount   = 0;
      int ioRangeCount    = 0;
      int flexNVMCount    = 0;
      int flexRamCount    = 0;

      String memoryMap = "  /* Map generated by USBDM New Project Wizard */\n"; //$NON-NLS-1$
      for (Iterator<MemoryRegion> it = device.getMemoryRegionIterator();
            it.hasNext();) {
         MemoryRegion memoryRegion = it.next();
         String template = ""; //$NON-NLS-1$
         for ( Iterator<MemoryRange> it1 = memoryRegion.iterator();
               it1.hasNext();) {
            MemoryRange memoryRange = it1.next();
            switch (memoryRegion.getMemoryType()) {
            case MemFLASH : 
               template = "  rom%s     (rx)  : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap += String.format(template, getRangeSuffix(flashRangeCount++), memoryRange.start, memoryRange.end-memoryRange.start+1);
               break;
            case MemRAM   :
               template = "  ram%s     (rwx) : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap += String.format(template, getRangeSuffix(ramRangeCount++), memoryRange.start, memoryRange.end-memoryRange.start+1);
               break;
            case MemIO    : 
               template = "  io%s      (rwx) : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap += String.format(template, getRangeSuffix(ioRangeCount++), memoryRange.start, memoryRange.end-memoryRange.start+1);
               break;
            case MemFlexNVM : 
               template = "  flexNVM%s (rx)  : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap += String.format(template, getRangeSuffix(flexNVMCount++), memoryRange.start, memoryRange.end-memoryRange.start+1);
               break;
            case MemFlexRAM : 
               template = "  flexRAM%s (rx)  : ORIGIN = 0x%08X, LENGTH = 0x%08X\n"; //$NON-NLS-1$
               memoryMap += String.format(template, getRangeSuffix(flexRamCount++), memoryRange.start, memoryRange.end-memoryRange.start+1);
               break;
            default: break;            
            }
         }
      }
      return memoryMap;
   }

   private String getExternalProjectHeaderFile(String deviceName) {
      
      String applicationPath;
      try {
         applicationPath = Usbdm.getUsbdmApplicationPath();
      } catch (UsbdmException e) {
         return "";
      }
//    System.err.println("DeviceDatabase(): Usbdm.getUsbdmApplicationPath() => " + applicationPath);
//    String dataPath = Usbdm.getUsbdmDataPath();
//    System.err.println("DeviceDatabase(): Usbdm.getUsbdmDataPath()        => " + dataPath);
      String deviceHeaderRoot = applicationPath + UsbdmConstants.PROJECT_HEADER_PATH + deviceName;
      
      File f = new File(deviceHeaderRoot+".h");
      if(f.isFile()) {
//         System.err.println("UsbdmWizardPage.getExternalProjectHeaderFile() looking for FILE \'"+deviceHeaderRoot+".h\' => found");
         return f.toURI().toString();
      }
//      System.err.println("UsbdmWizardPage.getExternalProjectHeaderFile() looking for \'"+deviceHeaderRoot+".h\' => not found");
      f = new File(deviceHeaderRoot);
      if(f.isDirectory()) {
//         System.err.println("UsbdmWizardPage.getExternalProjectHeaderFile() looking for DIR \'"+deviceHeaderRoot+"\' => found");
         return f.toURI().toString();
      }
//      System.err.println("UsbdmWizardPage.getExternalProjectHeaderFile() looking for \'"+deviceHeaderRoot+"\' => not found");
      return "";
   }
   
   private String getExternalVectorTable(String deviceName) {
      String applicationPath;
      try {
         applicationPath = Usbdm.getUsbdmApplicationPath();
      } catch (UsbdmException e) {
         return "";
      }
//    System.err.println("DeviceDatabase(): Usbdm.getUsbdmApplicationPath() => " + applicationPath);
//    String dataPath = Usbdm.getUsbdmDataPath();
//    System.err.println("DeviceDatabase(): Usbdm.getUsbdmDataPath()        => " + dataPath);
      String vectorTable = applicationPath + UsbdmConstants.VECTOR_TABLE_PATH + deviceName + ".c";
      
      File f = new File(vectorTable);
      if(f.exists()) {
//         System.err.println("getExternalVectorTable() looking for \'"+vectorTable+"\' => found");
//         System.err.println("   return: \'" + f.toURI().toString() + "\'");
         return f.toURI().toString();
      }
      else {
//         System.err.println("getExternalVectorTable() looking for \'"+vectorTable+"\' => not found");
         return "";
      }
   }
   
   /**
    * Adds C Device attributes
    * 
    * @param paramMap  Map to add attributes to
    * @param device    Device being used
    * @param deviceSubFamily 
    */
   private void addDatabaseValues(Map<String, String> paramMap, Device device, String deviceSubFamily) {
      String parameters = "";
      long soptAddress = device.getSoptAddress();
      if (soptAddress != 0) {
         switch(deviceType) {
         case T_ARM: 
            break;
         case T_CFV1:
            if (deviceSubFamily.equals(UsbdmConstants.SUB_FAMILY_CFV1)) {
               parameters +=   
                     "#ifndef SOPT1\n" +
                     String.format("#define SOPT1 (*(uint8_t*) 0x%X)\n",soptAddress) +
                     "#endif\n";
            }
            else if (deviceSubFamily.equals(UsbdmConstants.SUB_FAMILY_CFV1_PLUS)) {
               parameters +=   
                     "#ifndef SIM_COPC\n" +
                     String.format("#define SIM_COPC (*(uint8_t*) (0x%X+0x0A))\n",soptAddress) +
                     "#endif\n";
            }
            break;
         case T_CFVX:
         default:
            break;      
         }         
      }
      paramMap.put(UsbdmConstants.C_DEVICE_PARAMETERS,parameters);
   }
   
   /**
    * Adds device specific attributes to map
    * 
    * @param paramMap Map to add attributes to
    */
   private void addDeviceAttributes(Map<String, String> paramMap) {
      String deviceName = targetDeviceName.getText();
      String linkerMemoryMap;
      String deviceSubFamily;
      switch(deviceType) {
      case T_ARM: 
         linkerMemoryMap = UsbdmConstants.LINKER_MEMORY_MAP_COLDFIRE_KINETIS;
         deviceSubFamily = UsbdmConstants.SUB_FAMILY_CORTEX_M4;
         break;
      case T_CFV1:
         linkerMemoryMap = UsbdmConstants.LINKER_MEMORY_MAP_COLDFIRE_V1;
         deviceSubFamily = UsbdmConstants.SUB_FAMILY_CFV1;
         break;
      case T_CFVX:
      default:
         linkerMemoryMap = UsbdmConstants.LINKER_MEMORY_MAP_COLDFIRE_Vx;
         deviceSubFamily = UsbdmConstants.SUB_FAMILY_CFV2;
         break;      
      }
      DeviceDatabase deviceDatabase = new DeviceDatabase(deviceType.deviceFile);
      if (!deviceDatabase.isValid()) {
         System.err.println("Device database not loaded - using default device information"); //$NON-NLS-1$
      }
      else {
         Device device = deviceDatabase.getDevice(deviceName);
         if (device == null) {
            System.err.println("Device \""+deviceName+"\" not found - using default memory map");             //$NON-NLS-1$ //$NON-NLS-2$
         }
         else {
            linkerMemoryMap = createLinkerMemoryMap(device);
            deviceSubFamily = device.getFamily();
            addDatabaseValues(paramMap, device, deviceSubFamily);
         }
      }
      paramMap.put(UsbdmConstants.LINKER_MEMORY_MAP_KEY,       linkerMemoryMap);
      paramMap.put(UsbdmConstants.TARGET_DEVICE_SUBFAMILY_KEY, "DEVICE_SUBFAMILY_"+deviceSubFamily);
   }
   
   @Override
   public Map<String, String> getPageData() {
//      System.err.println("getPageData()");
      String deviceName       = targetDeviceName.getText();
      
      Map<String, String> paramMap = new HashMap<String, String>();
      paramMap.put(UsbdmConstants.TARGET_DEVICE_KEY,          deviceName);
      paramMap.put(UsbdmConstants.TARGET_DEVICE_NAME_KEY,     deviceName.toLowerCase());
      paramMap.put(UsbdmConstants.TARGET_DEVICE_FAMILY_KEY,   deviceType.name());
      paramMap.put(UsbdmConstants.GDB_COMMAND_KEY,            deviceType.gdbCommand);
      paramMap.put(UsbdmConstants.USBDM_GDB_SPRITE_KEY,       deviceType.gdbSprite);
      paramMap.put(UsbdmConstants.EXTERNAL_HEADER_FILE_KEY,   getExternalProjectHeaderFile(deviceName));
      paramMap.put(UsbdmConstants.EXTERNAL_VECTOR_TABLE_KEY,  getExternalVectorTable(deviceName));
           
      // Substitute for Template
      paramMap.put(UsbdmConstants.CROSS_COMMAND_PATH_KEY,    "${"+crossCommandPathVariableKey+"}");
      paramMap.put(UsbdmConstants.CROSS_COMMAND_PREFIX_KEY,  "${"+crossCommandPrefixVariableKey+"}");

      // Save cross command path & prefix to variable manager
      setVariableValue(crossCommandPathVariableKey,   crossCompilerPath.getText());
      setVariableValue(crossCommandPrefixVariableKey, crossCompilerPrefix.getText());
//    System.err.println("getPageData() - crossCommandPath   = " + crossCompilerPath.getText());
//    System.err.println("getPageData() - crossCommandPrefix = " + crossCompilerPrefix.getText());

      if (autoGenerateLinkerScript.getSelection()) {
//         System.err.println("getPageData() => "+"externalLinkerScript = \"\""); //$NON-NLS-1$ //$NON-NLS-2$
         paramMap.put(UsbdmConstants.EXTERNAL_LINKER_SCRIPT_KEY,  ""); //$NON-NLS-1$
      }
      else {
         // Copy specified script
//         System.err.println("getPageData() => "+"externalLinkerScript = \""+externalLinkerScript.getText()+"\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         URL url;
         try {
            File filepath = new File(externalLinkerScript.getText());
            url = filepath.toURI().toURL();
            paramMap.put(UsbdmConstants.EXTERNAL_LINKER_SCRIPT_KEY,  url.toString());
         } catch (MalformedURLException e) {
            e.printStackTrace();
         }
      }
      addDeviceAttributes(paramMap);

      IDialogSettings dialogSettings = getDialogSettings();
//      System.err.println("getDialogSettings() => " + dialogSettings);             //$NON-NLS-1$
      if (dialogSettings != null) {
         dialogSettings.put(deviceType.name()+UsbdmConstants.TARGET_DEVICE_KEY,                 targetDeviceName.getText());
         dialogSettings.put(deviceType.name()+UsbdmConstants.EXTERNAL_LINKER_SCRIPT_KEY,        externalLinkerScript.getText());
         dialogSettings.put(deviceType.name()+UsbdmConstants.DONT_GENERATE_LINKER_SCRIPT_KEY,   !autoGenerateLinkerScript.getSelection());
      }
      return paramMap;
   }

   @Override
   public void setNextPage(IWizardPage next) {
      nextPage = next;
   }

   @Override
   public IWizardPage getNextPage() {
      return nextPage;
   }
}
