/*******************************************************************************
 * Copyright (c) 2013 Peter O'Donoghue and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
/**
 * @author Peter O'Donoghue
 *         based upon work by Doug Schaefer, Adrian Petrescu
 * 
 */
package net.sourceforge.usbdm.gdb.ui;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ListIterator;

import net.sourceforge.usbdm.constants.ToolInformationData;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.Device.ClockTypes;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.gdb.GdbServerParameters;
import net.sourceforge.usbdm.gdb.UsbdmGdbServer;
import net.sourceforge.usbdm.gdb.GdbServerParameters.GdbServerType;
import net.sourceforge.usbdm.jni.JTAGInterfaceData;
import net.sourceforge.usbdm.jni.JTAGInterfaceData.ClockSpeed;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.Usbdm.AutoConnect;
import net.sourceforge.usbdm.jni.Usbdm.BdmInformation;
import net.sourceforge.usbdm.jni.Usbdm.EraseMethod;
import net.sourceforge.usbdm.jni.Usbdm.SecurityOptions;
import net.sourceforge.usbdm.jni.Usbdm.TargetVddSelect;
import net.sourceforge.usbdm.jni.Usbdm.USBDMDeviceInfo;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.debug.gdbjtag.core.Activator;
import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.debug.mi.core.IMILaunchConfigurationConstants;
import org.eclipse.cdt.debug.mi.core.MIPlugin;
import org.eclipse.cdt.debug.mi.core.command.factories.CommandFactoryDescriptor;
import org.eclipse.cdt.debug.mi.core.command.factories.CommandFactoryManager;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class UsbdmDebuggerTab extends AbstractLaunchConfigurationTab {

   private static final String         TAB_NAME = "Debugger";
   private static final String         TAB_ID   = "net.sourceforge.usbdm.gdb.ui.usbdmDebuggerTab";

   // Keys used in GDB Launch configurations 
   public  final static String USBDM_LAUNCH_ATTRIBUTE_KEY   = "net.sourceforge.usbdm.gdb.";               //$NON-NLS-1$
   public  final static String USBDM_GDB_INTERFACE_TYPE_KEY = USBDM_LAUNCH_ATTRIBUTE_KEY+"interfaceType";
   private final static String USBDM_GDB_BIN_PATH_KEY       = USBDM_LAUNCH_ATTRIBUTE_KEY+"gdbBinPath";    //$NON-NLS-1$
   private final static String USBDM_GDB_COMMAND_KEY        = USBDM_LAUNCH_ATTRIBUTE_KEY+"gdbCommand";    //$NON-NLS-1$
   private final static String USBDM_BUILD_TOOL_KEY         = USBDM_LAUNCH_ATTRIBUTE_KEY+"buildToolId";   //$NON-NLS-1$

   private static final String         JTAG_DEVICE_NAME = UsbdmSharedConstants.USBDM_INTERFACE_NAME;

   private CommandFactoryDescriptor[]  cfDescs;

   private GdbServerParameters         gdbServerParameters;
   private DeviceDatabase              deviceDatabase;
   private ArrayList<USBDMDeviceInfo>  deviceList;

   private InterfaceType[]             interfaceTypes;
   private Combo                       comboInterfaceType;
   private ModifyListener              comboInterfaceTypeListener;

   private Combo                       comboTargetDeviceName;
   private ModifyListener              comboTargetDeviceNameListener;

   private Combo                       comboBuildTool;
   private String                      buildToolsIds[] = null;

   private Text                        txtGdbBinPath;
   private Button                      btnGdbBinPathBrowse;
   private Button                      btnGdbBinPathVariables;

   private Text                        txtGdbCommand;
   private Button                      btnGdbCommandVariables;

   private Combo                       comboCommandFactory;
   private Combo                       comboMiProtocol;
   private Button                      btnVerboseMode;

   private Combo                       comboSelectBDM;
   private Button                      btnRefreshBDMs;
   private Label                       lblBDMInformation;
   private Button                      btnRequireExactBdm;

   private Text                        txtGdbPort;
   private NumberTextAdapter           txtGdbPortAdapter;
   private Button                      btnUseDebug;
   private Button                      btnExitOnClose;
   private Button                      btnPipeServer;
   private Button                      btnSocketServer;

   private Combo                       comboInterfaceSpeed;
   private Button                      btnAutomaticallyReconnect;
   private Button                      btnDriveReset;
   private Button                      btnUsePstSignals;

   private Combo                       comboSecurityOption;

   private EraseMethod[]               eraseMethods;
   private Combo                       comboEraseMethod;

   private TargetVddSelect[]           targetVdds;
   private Combo                       comboTargetVdd;

   private ClockTypes                  clockType;
   private Button                      btnTrimTargetClock;
   private Text                        txtTrimFrequency;
   private DoubleTextAdapter           txtTrimFrequencyAdapter;
   private Label                       lblKhz;
   private Label                       lblNvtrimAddress;
   private Text                        txtNVTRIMAddress;
   private HexTextAdapter              txtNVTRIMAddressAdapter;
   private Label                       lblHex;

   public UsbdmDebuggerTab() {
      super();
   }

   @Override
   public String getName() {
      return TAB_NAME;
   }

   /* (non-Javadoc)
    * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
    */
   @Override
   public Image getImage() {
      ImageDescriptor imageDescriptor = UsbdmGdbServer.getDefault().getImageDescriptor(UsbdmGdbServer.ID_BUG_IMAGE);
      return imageDescriptor.createImage();
   }

   @Override
   public String getId() {
      return TAB_ID;
   }

   /**   Add interface names to Interface combo
    * 
    *    @param interfaceTypeToSelect Used as initial selection
    */
   private void populateInterfaceTypes(InterfaceType interfaceTypeToSelect) {
//      System.err.println("populateInterfaceTypes() "+ interfaceTypeToSelect);
      int index = 0;
      comboInterfaceType.removeAll();
      if (comboInterfaceTypeListener != null) {
         comboInterfaceType.removeModifyListener(comboInterfaceTypeListener);
      }
      for (InterfaceType interfaceType : InterfaceType.values()) {
         comboInterfaceType.add(interfaceType.toString());
         interfaceTypes[index++] = interfaceType;
      }
      if (comboInterfaceTypeListener == null) {
         comboInterfaceTypeListener = new ModifyListener() { 
            @Override
            public void modifyText(ModifyEvent e) {
//               System.err.println("populateInterfaceTypes().modifyText()");
               String value = comboInterfaceType.getText();
               if (!value.equals(comboInterfaceType.getData())) {
                  comboInterfaceType.setData(value);
                  if (value.length()>0) {
                     populateGdbServerControls();
                     String deviceName = gdbServerParameters.getDeviceName();
                     populateTargetDevices(deviceName);
                     populateBuildTools();
                     doUpdate();
                  }
               }
            }
         };
      }
      // Add watchers for user data entry
      comboInterfaceType.addModifyListener(comboInterfaceTypeListener);

      if (interfaceTypeToSelect != null) {
         // Select the required interface
         comboInterfaceType.setText(interfaceTypeToSelect.toString());
      }
   }

   /**  Sets Interface type
    *   
    * @param interfaceType
    */
   private void setInterface(InterfaceType interfaceType) {
    //  System.err.println("setInterface() "+ interfaceType.toString());

      populateInterfaceTypes(interfaceType);

      comboInterfaceType.setText(interfaceType.toString());
   }

   @SuppressWarnings("unused")
   private void setInterface(String interfaceTypeName) {
      InterfaceType interfaceType = InterfaceType.valueOf(interfaceTypeName);
      setInterface(interfaceType);
   }

   private InterfaceType getInterfaceType() {
      int deviceIndex = comboInterfaceType.getSelectionIndex();
      if (deviceIndex<0) {
         return null;
      }
      return interfaceTypes[deviceIndex];
   }

   /** Populate multiple fields from the gdbServerParameters
    * 
    *  It will update gdbServerParameters to default if incompatible with current interface type
    */
   private void populateGdbServerControls() {
      InterfaceType interfaceType = getInterfaceType();
      if ((gdbServerParameters == null) || (gdbServerParameters.getInterfaceType() != interfaceType)) {
         // Make GDB server parameters consistent with interface
       //  System.err.println("populateGdbServerControls() - gdbServerParameters changed");
         gdbServerParameters = GdbServerParameters.getDefaultServerParameters(interfaceType);
      }
      loadGdbServerParameters();
   }

   /**
    * Populate the Target Device combo (if necessary)
    * 
    * Device list depends on currently selected Interface
    */
   private void populateTargetDevices(String deviceNametoSelect) {
    //  System.err.println("populateTargetDevices() "+deviceNametoSelect);
      InterfaceType interfaceType = getInterfaceType();
      if ((deviceDatabase == null) || (deviceDatabase.getTargetType() != interfaceType.targetType)) {
         // Change device database to be consistent with interface
         comboTargetDeviceName.removeAll();
         if (comboTargetDeviceNameListener != null) {
            comboTargetDeviceName.removeModifyListener(comboTargetDeviceNameListener);
         }
         deviceDatabase = new DeviceDatabase(interfaceType.targetType);
         if ((deviceDatabase == null) || !deviceDatabase.isValid()) {
            // Add dummy message
            comboTargetDeviceName.add("Device database not found");
         }
         else {
            for (Device device : deviceDatabase.getDeviceList()) {
               if (!device.isHidden()) {
                  comboTargetDeviceName.add(device.getName());
               }
            }
         }
         if (comboTargetDeviceNameListener == null) {
            comboTargetDeviceNameListener = new ModifyListener() {
               @Override
               public void modifyText(ModifyEvent e) {
                  String value = comboTargetDeviceName.getText();
                  if (!value.equals(comboTargetDeviceName.getData())) {
                     comboTargetDeviceName.setData(value);
                     if (value.length()>0) {
                        Device device = deviceDatabase.getDevice(value);
                        if (value != null) {
                           clockType = device.getClockType();
                           gdbServerParameters.setClockTrimFrequency(device.getDefaultClockTrimFreq());
                           gdbServerParameters.setNvmClockTrimLocation(device.getDefaultClockTrimNVAddress());
                           populateTrim();
                           doUpdate();
                        }
                     }
                  }
               }
            };
         }
         // Add watchers for user data entry
         comboTargetDeviceName.addModifyListener(comboTargetDeviceNameListener);
      }
      if ((deviceNametoSelect != null) && !deviceNametoSelect.isEmpty()) {
         comboTargetDeviceName.setText(deviceNametoSelect);
      }
      else {
         comboTargetDeviceName.select(0);
      }
   }

   //   private GDBJtagDeviceContribution findJtagDeviceByName(String name) {
   //      GDBJtagDeviceContributionFactory deviceContributionFactory = GDBJtagDeviceContributionFactory.getInstance();
   //      if (deviceContributionFactory == null) {
   //         System.err.println("deviceContributionFactory = null!");
   //         return null;
   //      }
   //      GDBJtagDeviceContribution[] availableDevices = deviceContributionFactory.getGDBJtagDeviceContribution();
   //      if (availableDevices == null) {
   //         System.err.println("availableDevices = null");
   //         return null; 
   //      }
   //      for (GDBJtagDeviceContribution device : availableDevices) {
   //         if (device.getDeviceName().equals(name)) {
   //            return device;
   //         }
   //      }
   //      return null;
   //   }

   protected void setTargetDevice(String deviceName) {
    //  System.err.println("setTargetDevice() "+deviceName);
      //      populateTargetDevices();
      //      Device device = deviceDatabase.getDevice(deviceName);
      //      if (device == null) {
      //         // Use 1st device in list
      //         comboTargetDeviceName.select(0);
      //         deviceName = comboTargetDeviceName.getText();
      //         device = deviceDatabase.getDevice(deviceName);
      //      }
      //      clockType = device.getClockType();
      //      if (btnTrimTargetClock != null) {
      //         enableTrim(btnTrimTargetClock.getSelection());
      //      }
      gdbServerParameters.setDeviceName(deviceName);
      comboTargetDeviceName.setText(deviceName);
   }

   /**
    *  Populate the Build Tool combo
    *  Build tool list depends on current interface
    *  Tries to maintain currently selected build tool if possible
    */
   private void populateBuildTools() {

      if (comboBuildTool == null) {
         return;
      }
      String currentBuildTool = comboBuildTool.getText();
      Hashtable<String, ToolInformationData> toolData = ToolInformationData.getToolInformationTable();

      if (buildToolsIds == null) {
         buildToolsIds = new String[toolData.size()+1];
      }
      comboBuildTool.removeAll();
      final String customSettings = "Custom Settings";
      // Add Custom Settings entry
      int index = 0;
      comboBuildTool.add(customSettings);
      buildToolsIds[index++] = "";

      String defaultBuildTool = customSettings;
      for (ToolInformationData toolInfo:toolData.values()) {
         if (!toolInfo.applicableTo(getInterfaceType())) {
            continue;
         }
         comboBuildTool.add(toolInfo.getDescription());
         if (defaultBuildTool.equals(customSettings)) {
            // First added tool becomes default
            defaultBuildTool = toolInfo.getDescription();
         }
         buildToolsIds[index++] = toolInfo.getBuildToolId();
      }
      // Set to default
      comboBuildTool.setText(defaultBuildTool);
      // try to set to last selected
      comboBuildTool.setText(currentBuildTool);
   }

   private String getBuildToolId() {
      int index = comboBuildTool.getSelectionIndex();
      if ((index<0) || (index > buildToolsIds.length)) {
         return "";
      }
      return buildToolsIds[index];
   }

   /** Populates the BDM choice control
    * 
    * @param previousDevice A String representing the serial number of a previously selected device.
    *                       This will be made the currently selected device (even if not connected).
    * @param scanForBdms    If true a scan is made for currently connected BDMs
    */
   private void populateBdmChoices(String previousDevice, boolean scanForBdms) {
    //  System.err.println("populateBdmChoices(\'"+previousDevice+"\', "+scanForBdms+")\n");

      if (scanForBdms) {
         // scan for connected BDMs
         //         System.err.println("populateBdmChoices() - looking for BDMs...");
         deviceList = Usbdm.getDeviceList();
      }
      else {
         // Don't scan for BDMs - use an empty list
         deviceList = new ArrayList<USBDMDeviceInfo>(); 
      }
      // Always add a null device
      //      System.err.println("populateBdmChoices() - Adding nullDevice");
      deviceList.add(0, USBDMDeviceInfo.nullDevice);

      String preferredDevice;
      // Check if non-default preferred device
      if ((previousDevice != null) && (!previousDevice.trim().isEmpty()) && 
            (!previousDevice.equals(USBDMDeviceInfo.nullDevice.deviceSerialNumber))) {
         // Set as preferred device
         preferredDevice = previousDevice;
         //         System.err.println("populateBdmChoices() preferredDevice = previousDevice = \'"+preferredDevice+"\'\n");
      }
      else {
         // Use currently selected device (if any) as preferred
         preferredDevice = comboSelectBDM.getText();
         if (preferredDevice.isEmpty()) {
            // Use dummy device
            preferredDevice = USBDMDeviceInfo.nullDevice.deviceSerialNumber;
         }
         //         System.err.println("populateBdmChoices() preferredDevice = currentDevice = \'"+preferredDevice+"\'\n");
      }
      // Add devices to combo
      comboSelectBDM.removeAll();
      ListIterator<Usbdm.USBDMDeviceInfo> it = deviceList.listIterator();
      while (it.hasNext()) {
         USBDMDeviceInfo di = it.next();
         //         System.err.println( "populateBdmChoices() Adding BDM = " + di.deviceSerialNumber);
         comboSelectBDM.add(di.deviceSerialNumber);
      }
      int index = comboSelectBDM.indexOf(preferredDevice);
      if (index >= 0) {
         // Preferred device is present
         comboSelectBDM.select(index);
         //         System.err.println("populateBdmChoices() selecting device by index = \'"+comboSelectBDM.getText()+"\'\n");
      }
      else {
         // Preferred device is not present
         if ((previousDevice == null) || (previousDevice.trim().isEmpty())) {
            // If no previous device just select first BDM
            comboSelectBDM.select(0);
            //            System.err.println("populateBdmChoices() Selecting 1st device = \'"+comboSelectBDM.getText()+"\'\n");
         }
         else {
            // Add dummy device representing previously used device, make preferred
            deviceList.add(new USBDMDeviceInfo("Previously selected device (not connected)", previousDevice, new BdmInformation()));
            comboSelectBDM.add(previousDevice);
            comboSelectBDM.setText(previousDevice);
          //  System.err.println("populateBdmChoices() Adding preferredDevice = \'"+comboSelectBDM.getText()+"\'\n");
         }
      }
      updateBdmDescription();
   }

   /**  Populate the erase methods
    * 
    *   These are filtered by gdbServerParameters
    */
   private void populateEraseMethods() {
      String eraseMethod = comboEraseMethod.getText();
      eraseMethods = new EraseMethod[EraseMethod.values().length];
      int index = 0;
      comboEraseMethod.removeAll();
      for (EraseMethod method :EraseMethod.values()) {
         if (gdbServerParameters.isAllowedEraseMethod(method)) {
            comboEraseMethod.add(method.toString());
            eraseMethods[index++] = method;
         }
      }
      comboEraseMethod.select(comboEraseMethod.getItemCount()-1);
      comboEraseMethod.setText(eraseMethod);      
   }

   private void setEraseMethod(EraseMethod eraseMethod) {
    //  System.err.println("setEraseMethod() "+ eraseMethod.toString());
      comboEraseMethod.setText(eraseMethod.toString());
   }

   @SuppressWarnings("unused")
   private void setEraseMethod(String eraseMethodName) {
    //  System.err.println("setEraseMethod() "+ eraseMethodName);
      EraseMethod eraseMethod = EraseMethod.valueOf(eraseMethodName);
      setEraseMethod(eraseMethod);
   }

   public EraseMethod getEraseMethod() {
      int index = comboEraseMethod.getSelectionIndex();
      if (index < 0) {
         return gdbServerParameters.getPreferredEraseMethod();
      }
      else {
         return eraseMethods[index];
      }
   }

   private void setTargetVdd(TargetVddSelect targetVdd) {
    //  System.err.println("TargetVddSelect() "+ targetVdd.toString());
      comboTargetVdd.setText(targetVdd.toString());
   }

   @SuppressWarnings("unused")
   private void setTargetVdd(String targetVddName) {
    //  System.err.println("TargetVddSelect() "+ targetVddName);
      TargetVddSelect targetVdd = TargetVddSelect.valueOf(targetVddName);
      setTargetVdd(targetVdd);
   }

   public TargetVddSelect getTargetVdd() {
      int index = comboTargetVdd.getSelectionIndex();
      if (index < 0) {
         return TargetVddSelect.BDM_TARGET_VDD_OFF;
      }
      else {
         return targetVdds[index];
      }
   }

   private void setSecurityOption(SecurityOptions securityOption) {
    //  System.err.println("setSecurityOption() "+ securityOption.toString());
      comboSecurityOption.setText(securityOption.toString());
   }

   @SuppressWarnings("unused")
   private void setSecurityOption(String securityOptionName) {
    //  System.err.println("getSecurityOption() "+ securityOptionName);
      SecurityOptions securityOption = SecurityOptions.valueOf(securityOptionName);
      setSecurityOption(securityOption);
   }

   public SecurityOptions getSecurityOption() {
      int index = comboSecurityOption.getSelectionIndex();
      if (index < 0) {
         return SecurityOptions.SECURITY_SMART;
      }
      else {
         return SecurityOptions.values()[index];
      }
   }

   /** Update Trim control group
    * 
    * @param enabled Whether trimming is enabled
    * 
    * @note The entire group will be disabled if the clock is not valid
    */
   private void enableTrim(boolean enabled) {
      boolean groupEnabled = (clockType != ClockTypes.INVALID) && (clockType != ClockTypes.EXTERNAL);

      btnTrimTargetClock.setEnabled(groupEnabled);
      txtTrimFrequency.setEnabled(groupEnabled&enabled);
      lblKhz.setEnabled(groupEnabled&enabled);
      lblNvtrimAddress.setEnabled(groupEnabled&enabled);
      txtNVTRIMAddress.setEnabled(groupEnabled&enabled);
      lblHex.setEnabled(groupEnabled&enabled);
      btnTrimTargetClock.setSelection(enabled);
   }

   private void populateTrim() {
      enableTrim(                                gdbServerParameters.isTrimClock());
      txtTrimFrequencyAdapter.setDoubleValue(    gdbServerParameters.getClockTrimFrequency()/1000.0);
      txtNVTRIMAddressAdapter.setValue(          gdbServerParameters.getNvmClockTrimLocation());
   }

   //   private GDBJtagDeviceContribution findJtagDeviceByName(String name) {
   //      GDBJtagDeviceContributionFactory deviceContributionFactory = GDBJtagDeviceContributionFactory.getInstance();
   //      if (deviceContributionFactory == null) {
   //         System.err.println("deviceContributionFactory = null!");
   //         return null;
   //      }
   //      GDBJtagDeviceContribution[] availableDevices = deviceContributionFactory.getGDBJtagDeviceContribution();
   //      if (availableDevices == null) {
   //         System.err.println("availableDevices = null");
   //         return null;
   //      }
   //      for (GDBJtagDeviceContribution device : availableDevices) {
   //         if (device.getDeviceName().equals(name)) {
   //            return device;
   //         }
   //      }
   //      return null;
   //   }

   public void setGdbServerParameters(GdbServerParameters gdbServerParameters) {
      this.gdbServerParameters = gdbServerParameters;
   }

   public GdbServerParameters getGdbServerParameters() {
      return gdbServerParameters;
   }

   private void populateGdbCommandFactoryControls() {

      comboCommandFactory.removeAll();

      // Get the command sets
      MIPlugin miPlugin = MIPlugin.getDefault();
      if (miPlugin == null) {
       //  System.err.println("miPlugin == NULL");
         comboCommandFactory.add("miPlugin == NULL");
         comboCommandFactory.select(0);
         return;
      }
      CommandFactoryManager cfManager = miPlugin.getCommandFactoryManager();
      if (cfManager == null) {
       //  System.err.println("cfManager == NULL");
         comboCommandFactory.add("cfManager == NULL");
         comboCommandFactory.select(0);
         return;
      }
      cfDescs = cfManager.getDescriptors(IGDBJtagConstants.DEBUGGER_ID);
      for (int i = 0; i < cfDescs.length; ++i) {
         comboCommandFactory.add(cfDescs[i].getName());
      }
      comboCommandFactory.select(0);
   }

   private int getInterfaceSpeed() {
      return JTAGInterfaceData.ClockSpeed.parse(comboInterfaceSpeed.getText()).getFrequency();
   }

   private void doUpdate() {
      try {
         scheduleUpdateJob();
         //         updateLaunchConfigurationDialog();
      } catch (Exception e) {
         e.printStackTrace();
      }      
   }

   protected void createUsbdmParametersGroup(Composite parent) {
      //      System.err.println("createUsbdmControl()");

      Group group = new Group(parent, SWT.NONE);
      group.setText("USBDM Parameters");
      group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

      RowLayout layout = new RowLayout();
      layout.center = true;
      layout.spacing = 10;
      layout.wrap = false;
      group.setLayout(layout);

      //         GDBJtagDeviceContribution probedDevice = findJtagDeviceByName(jtagDeviceName);
      //         if ((probedDevice == null) || !(probedDevice.getDevice() instanceof UsbdmInterface)) {
      //            System.err.println("createUsbdmControl() : USBDM device not found!\'" + jtagDeviceName + "\'");
      //         }

      //
      // Create Combo for interface
      //
      Label label = new Label(group, SWT.NONE);
      label.setText("Interface:"); //$NON-NLS-1$
      comboInterfaceType = new Combo(group, SWT.BORDER|SWT.READ_ONLY);
      interfaceTypes = new InterfaceType[InterfaceType.values().length];

      //         populateInterfaceTypes(null);
      comboInterfaceType.select(0);

      //         comboInterfaceType.addSelectionListener(new SelectionListener() {
      //            @Override
      //            public void widgetSelected(SelectionEvent e) {
      //               if (comboInterfaceType.getText().length()>0) {
      //                  populateGdbServerControls();
      //                  populateTargetDevices();
      //                  populateBuildTools();
      //                  doUpdate();
      //               }
      //            }            
      //            @Override
      //            public void widgetDefaultSelected(SelectionEvent e) {
      //            }
      //         });

      //
      // Create Combo for device selection
      //
      label = new Label(group, SWT.NONE);
      label.setText("Target Device:");
      comboTargetDeviceName = new Combo(group, SWT.BORDER|SWT.READ_ONLY);
      //         gd = new GridData();
      //         gd.widthHint = 150;
      //         targetDeviceName.setLayoutData(gd);
      //         populateTargetDevices();
      comboTargetDeviceName.select(0);
   }

   protected void createGdbSetupGroup(Composite parent) {
      Group group = new Group(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      group.setLayout(layout);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      group.setLayoutData(gd);
      group.setText("GDB Setup");

      createGdbCommandControls(group);
      createGdbCommandFactoryControls(group);
   }

   protected void createGdbCommandControls(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      comp.setLayoutData(gd);

      GridLayout layout = new GridLayout();
      layout.numColumns = 4;
      layout.marginTop  = 0;
      comp.setLayout(layout);
      Label label;

      //====================================================================
      label = new Label(comp, SWT.NONE);
      label.setText("Build Tool:");
      comboBuildTool = new Combo(comp, SWT.READ_ONLY | SWT.DROP_DOWN);
      gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
      gd.widthHint = 200;
      comboBuildTool.setLayoutData(gd);
      comboBuildTool.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            buildToolSelectionChanged();
            doUpdate();
         }
      });

      new Label(comp, SWT.NONE);
      new Label(comp, SWT.NONE);

      //====================================================================
      label = new Label(comp, SWT.NONE);
      label.setText("GDB bin Path: ");

      txtGdbBinPath = new Text(comp, SWT.SINGLE | SWT.BORDER);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      txtGdbBinPath.setLayoutData(gd);
      txtGdbBinPath.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      btnGdbBinPathBrowse = new Button(comp, SWT.NONE);
      btnGdbBinPathBrowse.setText("Browse");
      btnGdbBinPathBrowse.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            DirectoryDialog fd = new org.eclipse.swt.widgets.DirectoryDialog(getShell(), SWT.OPEN);
            fd.setText("GCC Path - Select Directory");
            fd.setMessage("Locate GCC bin directory");
            fd.setFilterPath(txtGdbBinPath.getText());
            String directoryPath = fd.open();
            if (directoryPath != null) {
               txtGdbBinPath.setText(directoryPath);
            }
         }
      });

      btnGdbBinPathVariables = new Button(comp, SWT.NONE);
      btnGdbBinPathVariables.setText("Variables");
      btnGdbBinPathVariables.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            variablesButtonSelected(txtGdbBinPath);
         }
      });

      //====================================================================
      label = new Label(comp, SWT.NONE);
      label.setText("GDB Command:");

      txtGdbCommand = new Text(comp, SWT.SINGLE | SWT.BORDER);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      //      gd.horizontalSpan = 1;
      txtGdbCommand.setLayoutData(gd);
      txtGdbCommand.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      label = new Label(comp, SWT.NONE);

      btnGdbCommandVariables = new Button(comp, SWT.NONE);
      btnGdbCommandVariables.setText("Variables");
      btnGdbCommandVariables.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            variablesButtonSelected(txtGdbCommand);
         }
      });

      //         buildToolSelectionChanged();
   }

   protected void createGdbCommandFactoryControls(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      RowLayout layout = new RowLayout();
      layout.center = true;
      layout.spacing = 10;
      layout.wrap = false;
      comp.setLayout(layout);

      //=====================================================
      Label label = new Label(comp, SWT.NONE);
      label.setText("Command Set:");

      comboCommandFactory = new Combo(comp, SWT.READ_ONLY | SWT.DROP_DOWN);
      comboCommandFactory.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            populateMiProtocol();
            doUpdate();
         }
      });

      //=====================================================
      label = new Label(comp, SWT.NONE);
      label.setText("Protocol:");

      comboMiProtocol = new Combo(comp, SWT.READ_ONLY | SWT.DROP_DOWN);
      comboMiProtocol.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      //=====================================================
      btnVerboseMode = new Button(comp, SWT.CHECK);
      btnVerboseMode.setText("Verbose console");
      btnVerboseMode.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });
   }

   /** Create BDM Selection Group
    * 
    * @param parent parent of group
    */
   protected void createPreferredBdmGroup(Composite parent) {
      //    System.err.println("UsbdmConnectionPanel::createPreferredBdmGroup()");

      Group grpSelectBdm = new Group(parent, SWT.NONE);
      grpSelectBdm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
      grpSelectBdm.setText("BDM Selection");
      grpSelectBdm.setLayout(new GridLayout(2, false));

      comboSelectBDM = new Combo(grpSelectBdm, SWT.READ_ONLY);
      comboSelectBDM.setToolTipText("Allows selection of preferred or required BDM\nfrom those currently attached.");
      comboSelectBDM.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            updateBdmDescription();
            doUpdate();
         }
      });
      GridData gd_comboSelectBDM = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
      gd_comboSelectBDM.minimumWidth = 200;
      comboSelectBDM.setLayoutData(gd_comboSelectBDM);
      //      populateBdmChoices(null, false);
      btnRefreshBDMs = new Button(grpSelectBdm, SWT.NONE);
      btnRefreshBDMs.setToolTipText("Check for connected BDMs");
      btnRefreshBDMs.setText("Refresh");
      btnRefreshBDMs.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            populateBdmChoices(null, true);
            doUpdate();
         }
      });

      lblBDMInformation = new Label(grpSelectBdm, SWT.NONE);
      lblBDMInformation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
      lblBDMInformation.setToolTipText("Description of selected BDM");
      lblBDMInformation.setText("BDM Information");

      btnRequireExactBdm = new Button(grpSelectBdm, SWT.CHECK);
      btnRequireExactBdm.setToolTipText("Use only the selected BDM.\nOtherwise selection is preferred BDM.");
      btnRequireExactBdm.setText("Exact");
      btnRequireExactBdm.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

   }

   /** Create GDB Port selection Group
    * 
    * @param parent parent of group
    */
   protected void createGdbServerGroup(Composite parent) {
      //    System.err.println("UsbdmConnectionPanel::createPreferredBdmGroup()");

      Group grpGdbControl = new Group(parent, SWT.NONE);
      grpGdbControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));
      grpGdbControl.setText("GDB Server Control");
      GridLayout gridLayout = new GridLayout(2,false);
      grpGdbControl.setLayout(gridLayout);

      Label lbl = new Label(grpGdbControl, SWT.NONE);
      lbl.setText("Port ");
      txtGdbPort = new Text(grpGdbControl, SWT.BORDER);
      txtGdbPortAdapter = new NumberTextAdapter("GDB Server port", txtGdbPort,  1234);
      txtGdbPort.setLayoutData(new GridData(60, 16));
      txtGdbPort.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      btnUseDebug = new Button(grpGdbControl, SWT.CHECK);
      btnUseDebug.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      btnUseDebug.setToolTipText("Use debug version of server.");
      btnUseDebug.setText("Debug");
      btnUseDebug.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

      btnExitOnClose = new Button(grpGdbControl, SWT.CHECK);
      btnExitOnClose.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      btnExitOnClose.setToolTipText("Exit the server when the client connection is closed.");
      btnExitOnClose.setText("Exit on Close");
      btnExitOnClose.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });
      btnPipeServer = new Button(grpGdbControl, SWT.RADIO);
      btnPipeServer.setText("Pipe");
      btnPipeServer.setToolTipText("Run server as Pipe e.g. target remote | server");
      btnPipeServer.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });
      btnSocketServer = new Button(grpGdbControl, SWT.RADIO);
      btnSocketServer.setText("Socket");
      btnSocketServer.setToolTipText("Run server as separate GUI process using sockets");
      btnSocketServer.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });      
   }

   protected void disableUnusedControls() {
      boolean enableThese;
      enableThese = gdbServerParameters.isRequiredDialogueComponents(GdbServerParameters.NEEDS_SPEED);
      comboInterfaceSpeed.setEnabled(enableThese);
      comboInterfaceSpeed.setVisible(enableThese);
      enableThese = gdbServerParameters.isRequiredDialogueComponents(GdbServerParameters.NEEDS_RESET);
      btnDriveReset.setEnabled(enableThese);
      btnDriveReset.setVisible(enableThese);
      enableThese = gdbServerParameters.isRequiredDialogueComponents(GdbServerParameters.NEEDS_PST);
      btnUsePstSignals.setEnabled(enableThese);         
      btnUsePstSignals.setVisible(enableThese);
   }

   protected void createConnectionGroup(Composite comp) {

      Group grpConnectionControl = new Group(comp, SWT.NONE);
      grpConnectionControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));

      grpConnectionControl.setLayout(new GridLayout(1, false));
      grpConnectionControl.setText("Connection Control"); //$NON-NLS-1$

      comboInterfaceSpeed = new Combo(grpConnectionControl, SWT.READ_ONLY);
      comboInterfaceSpeed.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      comboInterfaceSpeed.setItems(JTAGInterfaceData.getConnectionSpeeds());
      comboInterfaceSpeed.setToolTipText("Connection speed to use for BDM communications");
      comboInterfaceSpeed.select(4);
      comboInterfaceSpeed.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      btnAutomaticallyReconnect = new Button(grpConnectionControl, SWT.CHECK);
      btnAutomaticallyReconnect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      btnAutomaticallyReconnect.setToolTipText("Automatically re-sync with the target whenever target state is polled."); //$NON-NLS-1$
      btnAutomaticallyReconnect.setText("Auto reconnect"); //$NON-NLS-1$
      btnAutomaticallyReconnect.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

      btnDriveReset = new Button(grpConnectionControl, SWT.CHECK);
      btnDriveReset.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      btnDriveReset.setToolTipText("Drive target reset pin when resetting the target."); //$NON-NLS-1$
      btnDriveReset.setText("Drive RESET pin"); //$NON-NLS-1$
      btnDriveReset.setBounds(0, 0, 140, 16);
      btnDriveReset.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

      btnUsePstSignals = new Button(grpConnectionControl, SWT.CHECK);
      btnUsePstSignals.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      btnUsePstSignals.setToolTipText("Use PST signal to determine execution state of target.");
      btnUsePstSignals.setText("Use PST signals");
      btnUsePstSignals.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });
   }

   protected void createEraseGroup(Composite comp) {

      Group grpEraseOptions = new Group(comp, SWT.NONE);
      grpEraseOptions.setText("Erase Options");
      grpEraseOptions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));

      RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
      rowLayout.pack = false;
      rowLayout.justify = true;

      grpEraseOptions.setLayout(rowLayout);
      comboEraseMethod = new Combo(grpEraseOptions, SWT.READ_ONLY);
      comboEraseMethod.setToolTipText("Erase method used before programming");
      comboEraseMethod.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });
   }

   /** Create Security Selection Group
    * 
    * @param parent parent of group
    */
   protected void createSecurityGroup(Composite parent) {
      //    System.err.println("UsbdmConnectionPanel::createSecurityGroup()");

      Group grpSelectsecurity = new Group(parent, SWT.NONE);
      grpSelectsecurity.setText("Security Options");
      grpSelectsecurity.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));

      RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
      rowLayout.pack = false;
      rowLayout.justify = true;
      grpSelectsecurity.setLayout(rowLayout);

      comboSecurityOption = new Combo(grpSelectsecurity, SWT.READ_ONLY);
      comboSecurityOption.setToolTipText("Security options applied to the target when programming ");

      // Must be added in ordinal order
      comboSecurityOption.add(SecurityOptions.SECURITY_IMAGE.toString());
      comboSecurityOption.add(SecurityOptions.SECURITY_UNSECURED.toString());
      comboSecurityOption.add(SecurityOptions.SECURITY_SMART.toString());
      comboSecurityOption.select(SecurityOptions.SECURITY_SMART.ordinal());

      comboSecurityOption.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            updateBdmDescription();
            doUpdate();
         }
      });   
   }


   protected void createTargetVddGroup(Composite comp) {

      Group grpTargetVddSupply = new Group(comp, SWT.NONE);
      grpTargetVddSupply.setText("Target Vdd"); //$NON-NLS-1$
      grpTargetVddSupply.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));

      RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
      rowLayout.pack = false;
      rowLayout.justify = false;
      rowLayout.fill = true;

      grpTargetVddSupply.setLayout(rowLayout);

      targetVdds = new TargetVddSelect[TargetVddSelect.values().length];
      comboTargetVdd = new Combo(grpTargetVddSupply, SWT.READ_ONLY);
      comboTargetVdd.setToolTipText("Target Vdd supplied from BDM to target");
      comboTargetVdd.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

      int index = 0;
      for (TargetVddSelect targetVdd :TargetVddSelect.values()) {
         if (targetVdd.ordinal() > TargetVddSelect.BDM_TARGET_VDD_5V.ordinal()) {
            continue;
         }
         comboTargetVdd.add(targetVdd.toString());
         targetVdds[index++] = targetVdd;
         comboTargetVdd.select(comboTargetVdd.getItemCount()-1);
      }
   }

   protected void createTrimGroup(Composite comp) {

      Group grpClockTrim = new Group(comp, SWT.NONE);
      grpClockTrim.setText("Internal Clock Trim"); //$NON-NLS-1$
      grpClockTrim.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 2));

      grpClockTrim.setLayout(new GridLayout(3, false));

      btnTrimTargetClock = new Button(grpClockTrim, SWT.CHECK);
      btnTrimTargetClock.setText("Frequency"); //$NON-NLS-1$
      btnTrimTargetClock.setToolTipText("Enable trimming of target internal clock source\r\nto given frequency."); //$NON-NLS-1$
      btnTrimTargetClock.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            enableTrim(((Button)e.getSource()).getSelection());
            doUpdate();
         }
      });

      txtTrimFrequency = new Text(grpClockTrim, SWT.BORDER);
      txtTrimFrequencyAdapter = new DoubleTextAdapter(txtTrimFrequency);
      txtTrimFrequency.setTextLimit(7);
      txtTrimFrequencyAdapter.setDoubleValue(0.0);
      txtTrimFrequency.setToolTipText(""); //$NON-NLS-1$
      GridData gd_txtTrimFrequency = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      gd_txtTrimFrequency.widthHint = 65;
      gd_txtTrimFrequency.minimumWidth = 65;
      txtTrimFrequency.setLayoutData(gd_txtTrimFrequency);
      txtTrimFrequency.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      lblKhz = new Label(grpClockTrim, SWT.NONE);
      lblKhz.setToolTipText(
            "The frequency to trim the internal clock source to.\r\n" +
            "Note this is NOT the bus clock frequency.");
      lblKhz.setText("kHz"); //$NON-NLS-1$

      lblNvtrimAddress = new Label(grpClockTrim, SWT.NONE);
      GridData gd_lblNvtrimAddress = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
      lblNvtrimAddress.setLayoutData(gd_lblNvtrimAddress);
      lblNvtrimAddress.setText("NVTRIM "); //$NON-NLS-1$
      lblNvtrimAddress.setToolTipText("Address of non-volatile memory location to write the trim value to."); //$NON-NLS-1$
      //      new Label(grpClockTrim, SWT.NONE);

      txtNVTRIMAddress = new Text(grpClockTrim, SWT.BORDER);
      txtNVTRIMAddressAdapter = new HexTextAdapter("NVTRIM Address", txtNVTRIMAddress, 0);
      txtNVTRIMAddressAdapter.setRange(0, 0xFFFF);
      GridData gd_txtNVTRIMAddress = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      gd_txtNVTRIMAddress.widthHint = 65;
      gd_txtNVTRIMAddress.minimumWidth = 65;
      txtNVTRIMAddress.setLayoutData(gd_txtNVTRIMAddress);
      txtNVTRIMAddress.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      lblHex = new Label(grpClockTrim, SWT.NONE);
      lblHex.setToolTipText("Address of non-volatile memory location to write the trim value to."); //$NON-NLS-1$
      lblHex.setText("hex"); //$NON-NLS-1$
   }

   @Override
   public void createControl(Composite parent) {
    //  System.err.println("createControl()");
      ScrolledComposite sc = new ScrolledComposite(parent, SWT.V_SCROLL|SWT.H_SCROLL);
      sc.setExpandHorizontal(true);
      sc.setExpandVertical(true);
      setControl(sc);

      Composite comp = new Composite(sc, SWT.NONE);
      sc.setContent(comp);
      GridLayout layout = new GridLayout();
      comp.setLayout(layout);

      createUsbdmParametersGroup(comp);
      createGdbSetupGroup(comp);
      createPreferredBdmGroup(comp);

      Composite holder = new Composite(comp, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(holder);

      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(holder);
      createGdbServerGroup(holder);
      createConnectionGroup(holder);
      createEraseGroup(holder);
      createSecurityGroup(holder);
      createTrimGroup(holder);
      createTargetVddGroup(holder);
   }

   private void variablesButtonSelected(Text text) {
      StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
      if (dialog.open() == StringVariableSelectionDialog.OK) {
         text.insert(dialog.getVariableExpression());
      }
   }

   private void buildToolSelectionChanged() {
      ToolInformationData toolInfo = ToolInformationData.getToolInformationTable().get(getBuildToolId());
      if (toolInfo != null) {
         txtGdbCommand.setText("${"+toolInfo.getPrefixVariableName()+"}"+UsbdmSharedConstants.GDB_NAME);
         txtGdbBinPath.setText("${"+toolInfo.getPathVariableName()+"}");
      }
      txtGdbCommand.setEnabled(toolInfo == null);
      btnGdbCommandVariables.setEnabled(toolInfo == null);

      txtGdbBinPath.setEnabled(toolInfo == null);
      btnGdbBinPathBrowse.setEnabled(toolInfo == null);
      btnGdbBinPathVariables.setEnabled(toolInfo == null);
   }

   /**  Populates the Mi Protocol Combo
    *   Values depends on Command set chosen 
    */
   private void populateMiProtocol() {
      if (cfDescs == null) {
         comboMiProtocol.add("cfDescs == null");
         comboMiProtocol.select(0);
         return;
      }
      String currProt = comboMiProtocol.getText();
      comboMiProtocol.removeAll();
      int cfsel = comboCommandFactory.getSelectionIndex();
      if (cfsel >= 0) {
         String[] protocols = cfDescs[cfsel].getMIVersions();
         for (int i = 0; i < protocols.length; ++i) {
            comboMiProtocol.add(protocols[i]);
            if (currProt != null && protocols[i].equals(currProt))
               comboMiProtocol.select(i);
         }
      }
      comboMiProtocol.select(0);
      comboMiProtocol.setText(currProt);
   }

   /**
    *   Updates the description of the selected BDM
    */
   private void updateBdmDescription() {
      if (lblBDMInformation != null) {
         int index = comboSelectBDM.getSelectionIndex();
         if (index >= 0) {
            String deviceDescription = deviceList.get(index).deviceDescription;
            lblBDMInformation.setText(deviceDescription);
         }
      }
   }

   //   private GDBJtagDeviceContribution findJtagDeviceByName(String name) {
   //      GDBJtagDeviceContributionFactory deviceContributionFactory = GDBJtagDeviceContributionFactory.getInstance();
   //      if (deviceContributionFactory == null) {
   //         System.err.println("deviceContributionFactory = null!");
   //         return null;
   //      }
   //      GDBJtagDeviceContribution[] availableDevices = deviceContributionFactory.getGDBJtagDeviceContribution();
   //      if (availableDevices == null) {
   //         System.err.println("availableDevices = null");
   //         return null;
   //      }
   //      for (GDBJtagDeviceContribution device : availableDevices) {
   //         if (device.getDeviceName().equals(name)) {
   //            return device;
   //         }
   //      }
   //      return null;
   //   }

   /**  Try to get interface type from project via ILaunchConfiguration
    * 
    * @param configuration
    * @return
    */
   private ToolInformationData getToolInformationDataFromConfig(ILaunchConfiguration configuration) {

      String buildToolsId = null;
      try {

         //ToDo   Consider using -      ICProject projectHandle = CDebugUtils.verifyCProject(configuration);

         String projectName    = configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME,            (String)null);
         String projectBuildId = configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_BUILD_CONFIG_ID, (String)null);

       //  System.err.println("projectName    = "+projectName);
       //  System.err.println("projectBuildId = "+projectBuildId);

         if ((projectName != null) && (projectBuildId != null)) {
            ICProject projectHandle = CoreModel.getDefault().getCModel().getCProject(projectName);
            IConfiguration buildConfig = ManagedBuildManager.getBuildInfo(projectHandle.getProject()).getManagedProject().getConfiguration(projectBuildId);
            IToolChain toolChain = buildConfig.getToolChain();
            IOption option = null;
            option = toolChain.getOptionBySuperClassId("net.sourceforge.usbdm.cdt.arm.toolchain.buildtools");
            if (option == null) {
               option = toolChain.getOptionBySuperClassId("net.sourceforge.usbdm.cdt.coldfire.toolchain.buildtools");
            }
            if (option != null) {
             //  System.err.println("option(net.sourceforge.usbdm.cdt.arm.toolchain.buildtools).getId() = "+option.getId());
             //  System.err.println("option(net.sourceforge.usbdm.cdt.arm.toolchain.buildtools).getName() = "+option.getName());
               buildToolsId = option.getStringValue();
            }
         }
      } catch (BuildException e) {
         //         e.printStackTrace();
      } catch (Exception e) {
         //         e.printStackTrace();
      }
    //  System.err.println("Selected Build Tools ID = " + buildToolsId);

      ToolInformationData toolInformationData = null;
      if (buildToolsId != null) {
         toolInformationData = ToolInformationData.get(buildToolsId);
      }
    //  System.err.println("Selected Build Tools = " + toolInformationData);
      return toolInformationData;
   }
   /*
    *       InterfaceType interfaceType = null;
      if (toolInformationData != null) {
         interfaceType = toolInformationData.getPreferredInterfaceType();
      }
      if (interfaceType == null) {
         interfaceType = InterfaceType.T_ARM;
      }
      return interfaceType;      

    */

   /** Loads multiple fields from the current gdbServerParameters
    * 
    */
   private void loadGdbServerParameters() {
    //  System.err.println("loadGdbServerParameters()");
    //  System.err.print(gdbServerParameters.toString());

      //      setTargetDevice(                           gdbServerParameters.getDeviceName());
      populateBdmChoices(                        gdbServerParameters.getBdmSerialNumber(), true);
      btnRequireExactBdm.setSelection(           gdbServerParameters.isBdmSerialNumberMatchRequired());
      txtGdbPortAdapter.setDecimalValue(         gdbServerParameters.getGdbPortNumber());
      btnUseDebug.setSelection(                  gdbServerParameters.isUseDebugVersion());
      btnExitOnClose.setSelection(               gdbServerParameters.isExitOnClose());
      btnPipeServer.setSelection(                gdbServerParameters.getServerType() == GdbServerType.SERVER_PIPE);
      btnSocketServer.setSelection(              gdbServerParameters.getServerType() == GdbServerType.SERVER_SOCKET);
      comboInterfaceSpeed.setText(              
            ClockSpeed.findSuitable(             gdbServerParameters.getInterfaceFrequency()).toString());
      btnAutomaticallyReconnect.setSelection(    gdbServerParameters.getAutoReconnect().name() != AutoConnect.AUTOCONNECT_NEVER.name());
      btnDriveReset.setSelection(                gdbServerParameters.isUseReset());
      btnUsePstSignals.setSelection(             gdbServerParameters.isUsePstSignals());
      // Update list to match gdbServerParameters
      populateEraseMethods();
      setEraseMethod(                            gdbServerParameters.getEraseMethod());
      setTargetVdd(                              gdbServerParameters.getTargetVdd());
      setSecurityOption(                         gdbServerParameters.getSecurityOption());
      populateTrim();
      disableUnusedControls();
   }

   /** Loads multiple fields from the current gdbServerParameters
    * 
    */
   private void saveGdbServerParameters() {
      //      System.err.println("saveGdbServerParameters()");

      gdbServerParameters.setDeviceName(                       comboTargetDeviceName.getText());
      gdbServerParameters.setBdmSerialNumber(                  comboSelectBDM.getText());
      gdbServerParameters.enableBdmSerialNumberMatchRequired(  btnRequireExactBdm.getSelection());
      gdbServerParameters.setGdbPortNumber(                    txtGdbPortAdapter.getDecimalValue());
      gdbServerParameters.enableUseDebugVersion(               btnUseDebug.getSelection());
      gdbServerParameters.enableExitOnClose(                   btnExitOnClose.getSelection());
      gdbServerParameters.setServerType(                       btnPipeServer.getSelection()?GdbServerType.SERVER_PIPE:GdbServerType.SERVER_SOCKET);
      gdbServerParameters.setInterfaceFrequency(               getInterfaceSpeed());
      gdbServerParameters.setAutoReconnect(                    btnAutomaticallyReconnect.getSelection()?AutoConnect.AUTOCONNECT_ALWAYS:AutoConnect.AUTOCONNECT_NEVER);
      gdbServerParameters.enableUseReset(                      btnDriveReset.getSelection());
      gdbServerParameters.enableUsePstSignals(                 btnUsePstSignals.getSelection());
      gdbServerParameters.setEraseMethod(                      getEraseMethod());
      gdbServerParameters.setSecurityOption(                   getSecurityOption());
      gdbServerParameters.setTargetVdd(                        getTargetVdd());
      gdbServerParameters.enableTrimClock(                     btnTrimTargetClock.getSelection());
      gdbServerParameters.setClockTrimFrequency(          (int)Math.round(txtTrimFrequencyAdapter.getDoubleValue()*1000));
      gdbServerParameters.setNvmClockTrimLocation(             txtNVTRIMAddressAdapter.getHexValue());
      //      System.err.print(gdbServerParameters.toString());
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
    */
   @Override
   public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {

      CommandFactoryManager cfManager = MIPlugin.getDefault().getCommandFactoryManager();
      CommandFactoryDescriptor defDesc = cfManager.getDefaultDescriptor(IGDBJtagConstants.DEBUGGER_ID);
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_COMMAND_FACTORY, defDesc.getName());
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_PROTOCOL,        defDesc.getMIVersions()[0]);
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_VERBOSE_MODE,    
            IMILaunchConfigurationConstants.DEBUGGER_VERBOSE_MODE_DEFAULT);
      configuration.setAttribute(IGDBJtagConstants.ATTR_USE_REMOTE_TARGET,                         true);
   }

   @Override
   public void initializeFrom(ILaunchConfiguration configuration) {
    //  System.err.println("initializeFrom()");

      try {
         // Get interface type from settings
         InterfaceType       interfaceType = null;
         ToolInformationData toolInformationData = null;

         if (configuration != null) {
            String interfaceTypeName = configuration.getAttribute(USBDM_GDB_INTERFACE_TYPE_KEY, (String)null);
            if (interfaceTypeName != null) {
               interfaceType = InterfaceType.valueOf(interfaceTypeName);
             //  System.err.println("Setting interface to launch saved value = "+interfaceType.toString());
            }
            // Try to get tool information from project via configuration
            toolInformationData = getToolInformationDataFromConfig(configuration);
         }

         Boolean discardSettings = (configuration == null);
         if ((interfaceType == null) || 
               ((toolInformationData != null) && !toolInformationData.applicableTo(interfaceType))) {
            // Interface type not set or incompatible - reset
            discardSettings = true;
          //  System.err.println("Interface type is missing or incompatible");
            // Interface type not set or incompatible - reset
            if (toolInformationData == null) {
               // Use a default
               interfaceType = InterfaceType.T_ARM;
             //  System.err.println("Setting interface to default = "+interfaceType.toString());
            }
            else {
               // Use tool default
               interfaceType = toolInformationData.getPreferredInterfaceType();
             //  System.err.println("Setting interface to tool default = "+interfaceType.toString());
            }
         }
       //  System.err.println("Interface = "+interfaceType.toString());

         // Load clean settings
         gdbServerParameters = GdbServerParameters.getDefaultServerParameters(interfaceType);

         // Update from configuration (if appropriate)
         if (!discardSettings) {
            // Only load if appropriate to current interface
          //  System.err.println("Loading gdbServerParameters from settings");
            gdbServerParameters.initializeFrom(configuration);
         }
         // Populate & set the initial interface
         populateInterfaceTypes(interfaceType);
         populateGdbCommandFactoryControls();

         // Load into controls
       //  System.err.println("Loading gdbServerParameters into controls");
         populateGdbServerControls();

         if (!discardSettings) {
            // Only load settings if appropriate to interface

            txtGdbBinPath.setText(configuration.getAttribute(USBDM_GDB_BIN_PATH_KEY, ""));
            txtGdbCommand.setText(configuration.getAttribute(USBDM_GDB_COMMAND_KEY, ""));

            String buildToolId = configuration.getAttribute(USBDM_BUILD_TOOL_KEY, ""); 
            ToolInformationData toolInfo = ToolInformationData.get(buildToolId);
            if (toolInfo != null) {
               comboBuildTool.setText(ToolInformationData.get(buildToolId).getDescription());
            }
            else {
               comboBuildTool.select(0);
            }
            buildToolSelectionChanged();

            CommandFactoryManager cfManager = MIPlugin.getDefault().getCommandFactoryManager();
            CommandFactoryDescriptor defDesc = cfManager.getDefaultDescriptor(IGDBJtagConstants.DEBUGGER_ID);
            String commandFactoryAttr = configuration.getAttribute(
                  IMILaunchConfigurationConstants.ATTR_DEBUGGER_COMMAND_FACTORY,
                  defDesc.getName());
            comboCommandFactory.setText(commandFactoryAttr);

            String miProtocolAttr = configuration.getAttribute(
                  IMILaunchConfigurationConstants.ATTR_DEBUGGER_PROTOCOL,
                  defDesc.getMIVersions()[0]);
            comboMiProtocol.setText(miProtocolAttr);

            boolean verboseModeAttr = configuration.getAttribute(
                  IMILaunchConfigurationConstants.ATTR_DEBUGGER_VERBOSE_MODE,
                  IMILaunchConfigurationConstants.DEBUGGER_VERBOSE_MODE_DEFAULT);
            btnVerboseMode.setSelection(verboseModeAttr);

            String currentJtagDeviceName = configuration.getAttribute(IGDBJtagConstants.ATTR_JTAG_DEVICE, "");
            if (currentJtagDeviceName != JTAG_DEVICE_NAME) {
               //             System.err.println("jtagDeviceNameNeedsUpdate required");
               doUpdate();
            }
         }
      } catch (CoreException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void performApply(ILaunchConfigurationWorkingCopy configuration) {
      InterfaceType interfaceType = getInterfaceType();

      // Save GDB settings
      saveGdbServerParameters();
      gdbServerParameters.performApply(configuration, USBDM_LAUNCH_ATTRIBUTE_KEY);

      configuration.setAttribute(USBDM_GDB_INTERFACE_TYPE_KEY, getInterfaceType().name());

      String gdbSpritePath = btnUseDebug.getSelection()?interfaceType.gdbDebugSprite:interfaceType.gdbSprite;
      String connectionString = gdbSpritePath + " " + comboTargetDeviceName.getText().trim();
      //      System.err.println("UsbdmDebuggerTab.performApply() connectionString = \'" + connectionString + "\'");
      try {
         URI uri = new URI("gdb", connectionString, "");                                        //$NON-NLS-1$ //$NON-NLS-2$
         configuration.setAttribute(IGDBJtagConstants.ATTR_CONNECTION, uri.toString());
         //         System.err.println("UsbdmDebuggerTab.performApply() uri = \'" + uri.toString() + "\'");
      } catch (URISyntaxException e) {
         Activator.log(e);
      }
      configuration.setAttribute(USBDM_BUILD_TOOL_KEY,      getBuildToolId());
      configuration.setAttribute(USBDM_GDB_COMMAND_KEY,     txtGdbCommand.getText().trim());
      configuration.setAttribute(USBDM_GDB_BIN_PATH_KEY,    txtGdbBinPath.getText().trim());

      // Compatibility, still needed ?
      String gdbPath = txtGdbBinPath.getText().trim();
      if (gdbPath.length() != 0) {
         gdbPath += File.separator;
      }
      gdbPath += txtGdbCommand.getText().trim();
//      System.err.println("UsbdmDebuggerTab.performApply() gdbPath = \'" + gdbPath + "\'");
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME,                gdbPath);
      configuration.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME,               gdbPath); // DSF

      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_COMMAND_FACTORY,  comboCommandFactory.getText());
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_PROTOCOL,         comboMiProtocol.getText());
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_VERBOSE_MODE,     btnVerboseMode.getSelection());

      configuration.setAttribute(IGDBJtagConstants.ATTR_JTAG_DEVICE, JTAG_DEVICE_NAME);
      configuration.setAttribute(IGDBJtagConstants.ATTR_USE_REMOTE_TARGET, true);
   }

   /**
    * @param args
    * @since 4.10
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);

      shell.setLayout(new FillLayout());

      shell.setSize(600, 450);

      UsbdmDebuggerTab usbdmTab = new UsbdmDebuggerTab();

      usbdmTab.createControl(shell);
      usbdmTab.initializeFrom(null);
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose(); 
   }
}
