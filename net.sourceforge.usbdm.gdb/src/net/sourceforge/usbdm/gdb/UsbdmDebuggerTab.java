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
package net.sourceforge.usbdm.gdb;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Iterator;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.constants.*;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;

import org.eclipse.cdt.debug.gdbjtag.core.Activator;
import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.GDBJtagDeviceContribution;
import org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.GDBJtagDeviceContributionFactory;
import org.eclipse.cdt.debug.mi.core.IMILaunchConfigurationConstants;
import org.eclipse.cdt.debug.mi.core.MIPlugin;
import org.eclipse.cdt.debug.mi.core.command.factories.CommandFactoryDescriptor;
import org.eclipse.cdt.debug.mi.core.command.factories.CommandFactoryManager;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
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

   private static final String        TAB_NAME = "Debugger";
   private static final String        TAB_ID   = "net.sourceforge.usbdm.gdb.debuggerTab";

   private CommandFactoryDescriptor[] cfDescs;

   private Combo                      interfaceType;
   private Combo                      targetDeviceName;
   private Button                     debugMode;

   private Combo                      buildTool;
   private Text                       gdbCommand;
   private Text                       gdbBinPath;
   
   private Combo                      commandFactory;
   private Combo                      miProtocol;
   private Button                     verboseMode;

   private final String               jtagDeviceName = UsbdmSharedConstants.USBDM_INTERFACE_NAME;

   private Button                     gdbCommandVariablesButton;
   private Button                     gdbBinPathVariablesButton;
   private Button                     gdbBinPathBrowseButton;

   
   private String buildToolIds[] = null;

   public UsbdmDebuggerTab() {
      super();
   }
   
   @Override
   public String getName() {
      return TAB_NAME;
   }

   @Override
   public Image getImage() {
      return null;
   }

   private InterfaceType getInterfaceType() {
      int deviceIndex = interfaceType.getSelectionIndex();
      if ((deviceIndex<0)||(deviceIndex>=InterfaceType.values().length)) {
         deviceIndex = 0;
      }
      return InterfaceType.values()[deviceIndex];
   }

   /*
    * Populate the Target Device combo
    * Device list depends on currently selected Interface
    */
   private void populateTargetDevices() {
      InterfaceType deviceType = getInterfaceType();
      String currentDevice = null;
      if (targetDeviceName != null) {
         currentDevice = targetDeviceName.getText();
      }
      targetDeviceName.removeAll();
      DeviceDatabase deviceDatabase = new DeviceDatabase(deviceType.deviceFile);
      if (!deviceDatabase.isValid()) {
         targetDeviceName.add("Device database not found");
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
      if (currentDevice != null) {
         targetDeviceName.setText(currentDevice);
      }
      if (targetDeviceName.getSelectionIndex() < 0) {
         targetDeviceName.select(0);
      }
   }

   private void createUsbdmControl(Composite parent) {
   //      System.err.println("createUsbdmControl()");
         
         Group group = new Group(parent, SWT.NONE);
         group.setText("USBDM Parameters");
         group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

         GridLayout layout = new GridLayout(2, false);
         group.setLayout(layout);
   
         GDBJtagDeviceContribution probedDevice = findJtagDeviceByName(jtagDeviceName);
         if ((probedDevice == null) || !(probedDevice.getDevice() instanceof UsbdmInterface)) {
            System.err.println("createUsbdmControl() : USBDM device not found!\'" + jtagDeviceName + "\'");
         }

         //
         // Create & Populate Combo for interface
         //
         Label label = new Label(group, SWT.NONE);
         label.setText("Interface:"); //$NON-NLS-1$
         interfaceType = new Combo(group, SWT.BORDER|SWT.READ_ONLY);
         GridData gd = new GridData();
         gd.widthHint = 200;
         interfaceType.setLayoutData(gd);
         interfaceType.add(InterfaceType.T_ARM.toString());
         interfaceType.add(InterfaceType.T_CFV1.toString());
         interfaceType.add(InterfaceType.T_CFVX.toString());
         interfaceType.select(0);

         // Add watchers for user data entry
         interfaceType.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
               populateTargetDevices();
               populateBuildTools();
               scheduleUpdateJob();
            }
         });
         
         //
         // Create & Populate Combo for device selection
         //
         label = new Label(group, SWT.NONE);
         label.setText("Target Device:"); //$NON-NLS-1$
         targetDeviceName = new Combo(group, SWT.BORDER|SWT.READ_ONLY);
         gd = new GridData();
         gd.widthHint = 200;
         targetDeviceName.setLayoutData(gd);
         populateTargetDevices();
         targetDeviceName.select(0);

         // Add watchers for user data entry
         targetDeviceName.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
               scheduleUpdateJob();
            }
         });
         
         //
         // Debug checkbox
         //
         debugMode = new Button(group, SWT.CHECK);
         debugMode.setText("Debug");
         debugMode.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               updateLaunchConfigurationDialog();
            }
         });
         
      }

   private void createGdbControl(Composite parent) {
      Group group = new Group(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      group.setLayout(layout);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      group.setLayoutData(gd);
      group.setText("GDB Setup");

      createCommandControl(group);
      createCommandFactoryControl(group);
      createProtocolControl(group);
      createVerboseModeControl(group);
   }
   
   private void variablesButtonSelected(Text text) {
      StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
      if (dialog.open() == StringVariableSelectionDialog.OK) {
         text.insert(dialog.getVariableExpression());
      }
   }

   /*
    *  Populate the Build Tool combo
    *  Build tool list depends on current interface
    *  Tries to maintain currently selected build tool if possible
    */
   private void populateBuildTools() {
   
      if (buildTool == null) {
         return;
      }
      Hashtable<String, ToolInformationData> toolData = ToolInformationData.getToolInformationTable();
      
      String currentBuildToolId = "";
      if (buildToolIds == null) {
         buildToolIds = new String[toolData.size()+1];
      }
      else {
         currentBuildToolId = getBuildToolId();
      }
      buildTool.removeAll();
      
      // Add Custom Settings entry
      int index = 0;
      buildTool.add("Custom Settings");
      buildToolIds[index++] = null;

      int defaultBuildToolIndex = 0;
      for (ToolInformationData toolInfo:toolData.values()) {
         if (!toolInfo.applicableTo(getInterfaceType())) {
            continue;
         }
         buildTool.add(toolInfo.getDescription());
         if (toolInfo.getBuildToolId().equals(currentBuildToolId)) {
            defaultBuildToolIndex = index;
         }
         buildToolIds[index++] = toolInfo.getBuildToolId();
      }
      buildTool.select(defaultBuildToolIndex);
   }
   
   private String getBuildToolId() {
      int index = buildTool.getSelectionIndex();
      if ((index<0) || (index > buildToolIds.length)) {
         index = 0;
      }
      if (buildToolIds[index] == null) {
         return "";
      }
      else {
         return buildToolIds[index];
      }
   }
   
   private void buildToolSelectionChanged() {
      ToolInformationData toolInfo = ToolInformationData.getToolInformationTable().get(getBuildToolId());
      if (toolInfo != null) {
         gdbCommand.setText("${"+toolInfo.getPrefixVariableName()+"}"+UsbdmSharedConstants.GDB_NAME);
         gdbBinPath.setText("${"+toolInfo.getPathVariableName()+"}");
      }
      gdbCommand.setEnabled(toolInfo == null);
      gdbCommandVariablesButton.setEnabled(toolInfo == null);

      gdbBinPath.setEnabled(toolInfo == null);
      gdbBinPathBrowseButton.setEnabled(toolInfo == null);
      gdbBinPathVariablesButton.setEnabled(toolInfo == null);
   }
   
   private void createCommandControl(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.numColumns = 4;
      comp.setLayout(layout);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.horizontalSpan = 1;
      comp.setLayoutData(gd);
      Label label;
      
      label = new Label(comp, SWT.NONE);
      label.setText("Build Tool:");
      buildTool = new Combo(comp, SWT.READ_ONLY | SWT.DROP_DOWN);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.horizontalSpan = 1;
      buildTool.setLayoutData(gd);
      
      populateBuildTools();

      buildTool.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            buildToolSelectionChanged();
            scheduleUpdateJob();
         }
      });
      
      new Label(comp, SWT.NONE);
      new Label(comp, SWT.NONE);
      label = new Label(comp, SWT.NONE);
      label.setText("GDB Command:");
      gd = new GridData();
      gd.horizontalSpan = 1;
      label.setLayoutData(gd);

      gdbCommand = new Text(comp, SWT.SINGLE | SWT.BORDER);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.horizontalSpan = 1;
      gdbCommand.setLayoutData(gd);
      gdbCommand.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            scheduleUpdateJob();
         }
      });

      label = new Label(comp, SWT.NONE);
      gd = new GridData();
      gd.horizontalSpan = 1;
      label.setLayoutData(gd);

      gdbCommandVariablesButton = new Button(comp, SWT.NONE);
      gdbCommandVariablesButton.setText("Variables");
      gdbCommandVariablesButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            variablesButtonSelected(gdbCommand);
         }
      });

      label = new Label(comp, SWT.NONE);
      label.setText("GDB bin Path: ");
      gd = new GridData();
      gd.horizontalSpan = 1;
      label.setLayoutData(gd);

      gdbBinPath = new Text(comp, SWT.SINGLE | SWT.BORDER);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      gdbBinPath.setLayoutData(gd);
      gdbBinPath.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            scheduleUpdateJob();
         }
      });

      gdbBinPathBrowseButton = new Button(comp, SWT.NONE);
      gdbBinPathBrowseButton.setText("Browse");
      gdbBinPathBrowseButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            DirectoryDialog fd = new org.eclipse.swt.widgets.DirectoryDialog(getShell(), SWT.OPEN);
            fd.setText("GCC Path - Select Directory");
            fd.setMessage("Locate GCC bin directory");
            fd.setFilterPath(gdbBinPath.getText());
            String directoryPath = fd.open();
            if (directoryPath != null) {
               gdbBinPath.setText(directoryPath);
            }
         }
      });

      gdbBinPathVariablesButton = new Button(comp, SWT.NONE);
      gdbBinPathVariablesButton.setText("Variables");
      gdbBinPathVariablesButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            variablesButtonSelected(gdbBinPath);
         }
      });
      
      buildToolSelectionChanged();
   }

   private void createCommandFactoryControl(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout layout = new GridLayout(2, false);
      comp.setLayout(layout);
      Label label = new Label(comp, SWT.NONE);
      label.setText("Command Set:");

      commandFactory = new Combo(comp, SWT.READ_ONLY | SWT.DROP_DOWN);
      if (commandFactory == null) {
         System.err.println("commandFactory == NULL");
         return;
      }
      // Get the command sets
      MIPlugin miPlugin = MIPlugin.getDefault();
      if (miPlugin == null) {
         System.err.println("miPlugin == NULL");
         return;
      }
      CommandFactoryManager cfManager = miPlugin.getCommandFactoryManager();
      if (cfManager == null) {
         System.err.println("cfManager == NULL");
         return;
      }
      cfDescs = cfManager.getDescriptors(IGDBJtagConstants.DEBUGGER_ID);
      for (int i = 0; i < cfDescs.length; ++i) {
         commandFactory.add(cfDescs[i].getName());
      }
      commandFactory.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            commandFactoryChanged();
            scheduleUpdateJob();
         }
      });
   }

   private void createProtocolControl(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout layout = new GridLayout(2, false);
      comp.setLayout(layout);
      Label label = new Label(comp, SWT.NONE);
      label.setText("Protocol Version:");

      miProtocol = new Combo(comp, SWT.READ_ONLY | SWT.DROP_DOWN);
      if ((miProtocol.getSelectionIndex() < 0) && (miProtocol.getItemCount() > 0)) {
         miProtocol.select(0);
      }
      miProtocol.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            scheduleUpdateJob();
         }
      });
   }

   private void commandFactoryChanged() {
      int currsel = miProtocol.getSelectionIndex();
      String currProt = null;
      if (currsel >= 0) {
         currProt = miProtocol.getItem(currsel);
      }
      miProtocol.removeAll();
      int cfsel = commandFactory.getSelectionIndex();
      if (cfsel >= 0) {
         String[] protocols = cfDescs[cfsel].getMIVersions();
         for (int i = 0; i < protocols.length; ++i) {
            miProtocol.add(protocols[i]);
            if (currProt != null && protocols[i].equals(currProt))
               miProtocol.select(i);
         }
      }
      if (miProtocol.getSelectionIndex() < 0 && miProtocol.getItemCount() > 0) {
         miProtocol.select(0);
      }
   }

   private void createVerboseModeControl(Composite parent) {
      verboseMode = new Button(parent, SWT.CHECK);
      verboseMode.setText("Verbose console mode");
      verboseMode.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            updateLaunchConfigurationDialog();
         }
      });
   }

   @Override
   public void createControl(Composite parent) {
//      System.err.println("createControl()");
      ScrolledComposite sc = new ScrolledComposite(parent, SWT.V_SCROLL|SWT.H_SCROLL);
      sc.setExpandHorizontal(true);
      sc.setExpandVertical(true);
      setControl(sc);

      Composite comp = new Composite(sc, SWT.NONE);
      sc.setContent(comp);
      GridLayout layout = new GridLayout();
      comp.setLayout(layout);

      createUsbdmControl(comp);
      createGdbControl(comp);
   }

   private GDBJtagDeviceContribution findJtagDeviceByName(String name) {
      GDBJtagDeviceContributionFactory deviceContributionFactory = GDBJtagDeviceContributionFactory.getInstance();
      if (deviceContributionFactory == null) {
         System.err.println("deviceContributionFactory = null!");
         return null;
      }
      GDBJtagDeviceContribution[] availableDevices = deviceContributionFactory.getGDBJtagDeviceContribution();
      if (availableDevices == null) {
         System.err.println("availableDevices = null");
         return null;
      }
      for (GDBJtagDeviceContribution device : availableDevices) {
         if (device.getDeviceName().equals(name)) {
            return device;
         }
      }
      return null;
   }

   @Override
   public void initializeFrom(ILaunchConfiguration configuration) {
//      System.err.println("initializeFrom()");
      try {
         String interfaceTypeName = configuration.getAttribute(UsbdmSharedConstants.attributeKey_Family, "");
         try {
            interfaceType.setText(InterfaceType.valueOf(interfaceTypeName).toString());
         } catch (java.lang.IllegalArgumentException e) {
            // For backwards compatibility try using name directly to select item
            interfaceType.setText(interfaceTypeName);
            if (interfaceType.getSelectionIndex() < 0) {
               interfaceType.select(0);
            }
         }         
         populateTargetDevices(); // As depends on interface
         targetDeviceName.setText(configuration.getAttribute(UsbdmSharedConstants.attributeKey_Device, ""));
         debugMode.setSelection(configuration.getAttribute(UsbdmSharedConstants.attributeKey_DebugMode,false));
         
         gdbBinPath.setText(configuration.getAttribute(UsbdmSharedConstants.attributeKey_GdbBinPath, ""));
         gdbCommand.setText(configuration.getAttribute(UsbdmSharedConstants.attributeKey_GdbCommand, ""));
         
         populateBuildTools(); // As depends on interface
         String buildToolId = configuration.getAttribute(UsbdmSharedConstants.attributeKey_BuildToolId, ""); 
         ToolInformationData toolInfo = ToolInformationData.get(buildToolId);
         if (toolInfo != null) {
            buildTool.setText(ToolInformationData.get(buildToolId).getDescription());
         }
         else {
            buildTool.select(0);
         }
         buildToolSelectionChanged();

         CommandFactoryManager cfManager = MIPlugin.getDefault().getCommandFactoryManager();
         CommandFactoryDescriptor defDesc = cfManager.getDefaultDescriptor(IGDBJtagConstants.DEBUGGER_ID);
         String commandFactoryAttr = configuration.getAttribute(
               IMILaunchConfigurationConstants.ATTR_DEBUGGER_COMMAND_FACTORY,
               defDesc.getName());
         int cfid = 0;
         for (int i = 0; i < cfDescs.length; ++i) {
            if (cfDescs[i].getName().equals(commandFactoryAttr)) {
               cfid = i;
               break;
            }
         }
         commandFactory.select(cfid); // populates protocol list too

         String miProtocolAttr = configuration.getAttribute(
               IMILaunchConfigurationConstants.ATTR_DEBUGGER_PROTOCOL,
               defDesc.getMIVersions()[0]);
         int n = miProtocol.getItemCount();
         for (int i = 0; i < n; ++i) {
            if (miProtocol.getItem(i).equals(miProtocolAttr)) {
               miProtocol.select(i);
            }
         }
         boolean verboseModeAttr = configuration.getAttribute(
               IMILaunchConfigurationConstants.ATTR_DEBUGGER_VERBOSE_MODE,
               IMILaunchConfigurationConstants.DEBUGGER_VERBOSE_MODE_DEFAULT);
         verboseMode.setSelection(verboseModeAttr);

         String currentJtagDeviceName = configuration.getAttribute(IGDBJtagConstants.ATTR_JTAG_DEVICE, "");
         if (currentJtagDeviceName != jtagDeviceName) {
//            System.err.println("jtagDeviceNameNeedsUpdate required");
            scheduleUpdateJob();
         }
      } catch (CoreException e) {
         Activator.getDefault().getLog().log(e.getStatus());
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
    */
   @Override
   public String getId() {
      return TAB_ID;
   }

   @Override
   public void performApply(ILaunchConfigurationWorkingCopy configuration) {
      InterfaceType deviceType = getInterfaceType();
      
      String gdbSpritePath;
      if (debugMode.getSelection()) {
         gdbSpritePath = deviceType.gdbDebugSprite;
      }
      else {
         gdbSpritePath = deviceType.gdbSprite;
      }
      String connectionString = gdbSpritePath + " " + targetDeviceName.getText().trim();
      System.err.println("UsbdmDebuggerTab.performApply() connectionString = \'" + connectionString + "\'");
      try {
         URI uri = new URI("gdb", connectionString, "");                                        //$NON-NLS-1$ //$NON-NLS-2$
         configuration.setAttribute(IGDBJtagConstants.ATTR_CONNECTION, uri.toString());
         System.err.println("UsbdmDebuggerTab.performApply() uri = \'" + uri.toString() + "\'");
      } catch (URISyntaxException e) {
         Activator.log(e);
      }
      configuration.setAttribute(UsbdmSharedConstants.attributeKey_Family,    deviceType.name());
      configuration.setAttribute(UsbdmSharedConstants.attributeKey_Device,    targetDeviceName.getText());
      configuration.setAttribute(UsbdmSharedConstants.attributeKey_DebugMode, debugMode.getSelection());      

      configuration.setAttribute(UsbdmSharedConstants.attributeKey_BuildToolId, getBuildToolId());
      configuration.setAttribute(UsbdmSharedConstants.attributeKey_GdbCommand, gdbCommand.getText().trim());
      configuration.setAttribute(UsbdmSharedConstants.attributeKey_GdbBinPath, gdbBinPath.getText().trim());
      String gdbPath = gdbBinPath.getText().trim();
      if (gdbPath.length() != 0) {
         gdbPath += File.separator;
      }
      gdbPath += gdbCommand.getText().trim();
      System.err.println("UsbdmDebuggerTab.performApply() gdbPath = \'" + gdbPath + "\'");
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME,                gdbPath);
      configuration.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME,               gdbPath); // DSF
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_COMMAND_FACTORY,  commandFactory.getText());
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_PROTOCOL,         miProtocol.getText());
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_VERBOSE_MODE,     verboseMode.getSelection());
      
      configuration.setAttribute(IGDBJtagConstants.ATTR_JTAG_DEVICE, jtagDeviceName);
      configuration.setAttribute(IGDBJtagConstants.ATTR_USE_REMOTE_TARGET, true);
   }

   @Override
   public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
      configuration.setAttribute(UsbdmSharedConstants.attributeKey_GdbBinPath,                     "");
      configuration.setAttribute(UsbdmSharedConstants.attributeKey_GdbCommand,                     "");
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME,                  "");
      configuration.setAttribute(UsbdmSharedConstants.attributeKey_GdbBinPath,                     "");
      configuration.setAttribute(IGDBJtagConstants.ATTR_JTAG_DEVICE,                               jtagDeviceName);
      configuration.setAttribute(UsbdmSharedConstants.attributeKey_Device,                         "");
      configuration.setAttribute(UsbdmSharedConstants.attributeKey_Family,                         InterfaceType.T_ARM.name());
            CommandFactoryManager cfManager = MIPlugin.getDefault().getCommandFactoryManager();
            CommandFactoryDescriptor defDesc = cfManager.getDefaultDescriptor(IGDBJtagConstants.DEBUGGER_ID);
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_COMMAND_FACTORY, defDesc.getName());
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_PROTOCOL,        defDesc.getMIVersions()[0]);
      configuration.setAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_VERBOSE_MODE,    
            IMILaunchConfigurationConstants.DEBUGGER_VERBOSE_MODE_DEFAULT);
      configuration.setAttribute(IGDBJtagConstants.ATTR_USE_REMOTE_TARGET,                         true);
      configuration.setAttribute(UsbdmSharedConstants.attributeKey_DebugMode,                      false);
   }
   
   /**
    * @param args
    * @since 4.10
    */
   public static void main(String[] args) {
      Display display = new Display();
      
      Shell shell = new Shell(display);
  
      shell.setLayout(new FillLayout());

      UsbdmDebuggerTab usbdmTab = new UsbdmDebuggerTab();
      usbdmTab.createControl(shell);
      
      shell.open();
      while (!shell.isDisposed()) {
        if (!display.readAndDispatch())
          display.sleep();
      }
      display.dispose(); 

      // Instantiates and initialises the wizard

   }
}
