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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ListIterator;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.usbdm.constants.ToolInformationData;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.deviceDatabase.ClockTypes;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.ui.DeviceSelector;
import net.sourceforge.usbdm.gdb.server.GdbServerParameters;
import net.sourceforge.usbdm.jni.JTAGInterfaceData;
import net.sourceforge.usbdm.jni.JTAGInterfaceData.ClockSpeed;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.Usbdm.AutoConnect;
import net.sourceforge.usbdm.jni.Usbdm.BdmInformation;
import net.sourceforge.usbdm.jni.Usbdm.EraseMethod;
import net.sourceforge.usbdm.jni.Usbdm.SecurityOptions;
import net.sourceforge.usbdm.jni.Usbdm.TargetVddSelect;
import net.sourceforge.usbdm.jni.Usbdm.USBDMDeviceInfo;

/**
 * @since 4.12
 */
public class UsbdmDebuggerPanel {

   // Keys used in GDB Launch configurations
   public  final static String USBDM_LAUNCH_ATTRIBUTE_KEY = "net.sourceforge.usbdm.gdb."; //$NON-NLS-1$
   public  final static String USBDM_GDB_INTERFACE_TYPE_KEY = USBDM_LAUNCH_ATTRIBUTE_KEY + "interfaceType";
   
   private final static String USBDM_GDB_BIN_PATH_KEY = USBDM_LAUNCH_ATTRIBUTE_KEY + "gdbBinPath"; //$NON-NLS-1$
   private final static String USBDM_GDB_COMMAND_KEY  = USBDM_LAUNCH_ATTRIBUTE_KEY + "gdbCommand"; //$NON-NLS-1$
   private final static String USBDM_BUILD_TOOL_KEY   = USBDM_LAUNCH_ATTRIBUTE_KEY + "buildToolId"; //$NON-NLS-1$

   private GdbServerParameters         fGdbServerParameters;
   private ArrayList<USBDMDeviceInfo>  fDeviceList;

   private InterfaceType[]   fInterfaceTypes;
   private Combo             fComboInterfaceType;
   private ModifyListener    fComboInterfaceTypeListener;

   private Text              fTextTargetDeviceName;
   private Button            fButtonTargetDeviceSelect;
                             
   private Combo             fComboBuildTool;
   private String            fBuildToolsIds[] = null;
                             
   private Text              fTextGdbBinPath;
   private Button            fButtonGdbBinPathBrowse;
   private Button            fButtonGdbBinPathVariables;
                             
   private Text              fTextGdbCommand;
   private Button            fButtonGdbCommandVariables;

   private Combo             fComboSelectBDM;
   private Button            fButtonRefreshBDMs;
   private Label             fLabelBDMInformation;
   private Button            fButtonRequireExactBdm;

   private Text              fTextGdbServerPort;
   private NumberTextAdapter fTextGdbServerPortAdapter;
   private Button            fButtonUseSemihosting;
   private Text              fTextGdbTtyPort;
   private NumberTextAdapter fTextGdbTtyPortAdapter;
   private Button            fButtonUseDebug;
   private Button            fButtonExitOnClose;

   private Combo             fComboInterfaceSpeed;
   private Button            fButtonAutomaticallyReconnect;
   private Button            fButtonDriveReset;
   private Button            fButtonUsePstSignals;
   private Button            fButtonCatchVLLSsEvents;
   private Button            fButtonMaskInterrupts;
   private NumberTextAdapter fConnectionTimeoutTextAdapter;
   private Combo             fComboSecurityOption;

   private EraseMethod[]     fEraseMethods;
   private Combo             fComboEraseMethod;

   private TargetVddSelect[] fTargetVdds;
   private Combo             fComboTargetVdd;

   private ClockTypes        fClockType;
   private Button            fButtonTrimTargetClock;
   private Text              fTextTrimFrequency;
   private DoubleTextAdapter fTextTrimFrequencyAdapter;
   private Label             fLabelKhz;
   private Label             fLabelNvtrimAddress;
   private Text              fTextNVTRIMAddress;
   private HexTextAdapter    fTextNVTRIMAddressAdapter;
   private Label             fLabelHex;
   private Listener          fListener;
   private Group             fGroupClockTrim;
   private Composite         fComposite;

   private Label             fLabelcommandLine;

   private int               fSuspendUpdate;

   public UsbdmDebuggerPanel() {
   }

   Shell getShell() {
      return fComposite.getShell();
   }

   /**
    * Create contents of the panel
    * 
    * @param createGDBGroup Whether to include the GDB setup group
    * 
    * @return The control representing the contents of the panel
    */
   public Control createContents(Composite parent, boolean createGDBGroup) {
      fComposite = new Composite(parent, SWT.NONE);
      GridLayout gl = new GridLayout(1, false);
      gl.marginBottom = 0;
      gl.marginTop = 0;
      gl.marginLeft = 0;
      gl.marginRight = 0;
      gl.marginHeight = 0;
      gl.marginWidth = 0;
      fComposite.setLayout(gl);

      // System.err.println("createControl()");
      ScrolledComposite sc = new ScrolledComposite(fComposite, SWT.V_SCROLL | SWT.H_SCROLL);
      sc.setExpandHorizontal(true);
      sc.setExpandVertical(true);

      Composite comp = new Composite(sc, SWT.NONE);
      sc.setContent(comp);
      GridLayout layout = new GridLayout();
      comp.setLayout(layout);

      createUsbdmParametersGroup(comp);
      if (createGDBGroup) {
         createGdbSetupGroup(comp);
      }
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

      createCommandLineGroup(comp);

      return fComposite;
   }

   /**
    * Create USBDM Parameters selection Group
    * 
    * @param parent Parent of group
    */
   protected void createUsbdmParametersGroup(Composite parent) {
      // System.err.println("createUsbdmControl()");

      Group group = new Group(parent, SWT.NONE);
      group.setText("USBDM Parameters");
      group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

      RowLayout layout = new RowLayout();
      layout.center = true;
      layout.spacing = 10;
      layout.wrap = false;
      group.setLayout(layout);

      //
      // Create Combo for interface
      //
      Label label = new Label(group, SWT.NONE);
      label.setText("Interface:"); //$NON-NLS-1$
      fComboInterfaceType = new Combo(group, SWT.BORDER | SWT.READ_ONLY);
      fInterfaceTypes = new InterfaceType[InterfaceType.values().length];

      fComboInterfaceType.select(0);

      //
      // Create Device selection group
      //
      label = new Label(group, SWT.NONE);
      label.setText("Target Device:");
      fTextTargetDeviceName = new Text(group, SWT.BORDER | SWT.READ_ONLY | SWT.CENTER);
      fTextTargetDeviceName.setLayoutData(new RowData(200, SWT.DEFAULT));
      fButtonTargetDeviceSelect = new Button(group, SWT.NONE);
      fButtonTargetDeviceSelect.setText("Device...");
      fButtonTargetDeviceSelect.addSelectionListener(new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            InterfaceType interfaceType = getInterfaceType();
            DeviceSelector ds = new DeviceSelector(getShell(), interfaceType.targetType, fTextTargetDeviceName.getText());
            if (ds.open() == Window.OK) {
               fTextTargetDeviceName.setText(ds.getText());
               Device device = ds.getDevice();
               if (device != null) {
                  fSuspendUpdate++;
                  fClockType = device.getClockType();
                  fGdbServerParameters.setClockTrimFrequency(device.getDefaultClockTrimFreq());
                  fGdbServerParameters.setNvmClockTrimLocation(device.getDefaultClockTrimNVAddress());
                  populateTrim();
                  fSuspendUpdate--;
                  doUpdate();
               }
            }
         }

         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      });
   }

   /**
    * Create GDB Parameters selection Group
    * 
    * @param parent Parent of group
    */
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
      layout.marginTop = 0;
      comp.setLayout(layout);
      Label label;

      // ====================================================================
      label = new Label(comp, SWT.NONE);
      label.setText("Build Tool:");
      fComboBuildTool = new Combo(comp, SWT.READ_ONLY | SWT.DROP_DOWN);
      gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
      gd.widthHint = 200;
      fComboBuildTool.setLayoutData(gd);
      fComboBuildTool.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            fSuspendUpdate++;
            buildToolSelectionChanged();
            fSuspendUpdate--;
            doUpdate();
         }
      });

      new Label(comp, SWT.NONE);
      new Label(comp, SWT.NONE);

      // ====================================================================
      label = new Label(comp, SWT.NONE);
      label.setText("GDB bin Path: ");

      fTextGdbBinPath = new Text(comp, SWT.SINGLE | SWT.BORDER);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      fTextGdbBinPath.setLayoutData(gd);
      fTextGdbBinPath.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      fButtonGdbBinPathBrowse = new Button(comp, SWT.NONE);
      fButtonGdbBinPathBrowse.setText("Browse");
      fButtonGdbBinPathBrowse.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            DirectoryDialog fd = new org.eclipse.swt.widgets.DirectoryDialog(getShell(), SWT.OPEN);
            fd.setText("GCC Path - Select Directory");
            fd.setMessage("Locate GCC bin directory");
            fd.setFilterPath(fTextGdbBinPath.getText());
            String directoryPath = fd.open();
            if (directoryPath != null) {
               fTextGdbBinPath.setText(directoryPath);
            }
         }
      });

      fButtonGdbBinPathVariables = new Button(comp, SWT.NONE);
      fButtonGdbBinPathVariables.setText("Variables");
      fButtonGdbBinPathVariables.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            variablesButtonSelected(fTextGdbBinPath);
         }
      });

      // ====================================================================
      label = new Label(comp, SWT.NONE);
      label.setText("GDB Command:");

      fTextGdbCommand = new Text(comp, SWT.SINGLE | SWT.BORDER);
      gd = new GridData(GridData.FILL_HORIZONTAL);
      // gd.horizontalSpan = 1;
      fTextGdbCommand.setLayoutData(gd);
      fTextGdbCommand.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      label = new Label(comp, SWT.NONE);

      fButtonGdbCommandVariables = new Button(comp, SWT.NONE);
      fButtonGdbCommandVariables.setText("Variables");
      fButtonGdbCommandVariables.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            variablesButtonSelected(fTextGdbCommand);
         }
      });
   }

   protected void createGdbCommandFactoryControls(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);
      RowLayout layout = new RowLayout();
      layout.center = true;
      layout.spacing = 10;
      layout.wrap = false;
      comp.setLayout(layout);

      // =====================================================
//      btnVerboseMode = new Button(comp, SWT.CHECK);
//      btnVerboseMode.setText("Verbose console");
//      btnVerboseMode.addSelectionListener(new SelectionAdapter() {
//         @Override
//         public void widgetSelected(SelectionEvent e) {
//            doUpdate();
//         }
//      });
   }

   /**
    * Create BDM Selection Group
    * 
    * @param parent Parent of group
    */
   protected void createPreferredBdmGroup(Composite parent) {
      // System.err.println("UsbdmConnectionPanel::createPreferredBdmGroup()");

      Group grpSelectBdm = new Group(parent, SWT.NONE);
      grpSelectBdm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
      grpSelectBdm.setText("BDM Selection");
      grpSelectBdm.setLayout(new GridLayout(2, false));

      fComboSelectBDM = new Combo(grpSelectBdm, SWT.READ_ONLY);
      fComboSelectBDM.setToolTipText("Allows selection of preferred or required BDM\nfrom those currently attached.");
      fComboSelectBDM.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            fSuspendUpdate++;
            updateBdmDescription();
            fSuspendUpdate--;
            doUpdate();
         }
      });
      GridData gd_comboSelectBDM = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
      gd_comboSelectBDM.minimumWidth = 200;
      fComboSelectBDM.setLayoutData(gd_comboSelectBDM);
      // populateBdmChoices(null, false);
      fButtonRefreshBDMs = new Button(grpSelectBdm, SWT.NONE);
      fButtonRefreshBDMs.setToolTipText("Check for connected BDMs");
      fButtonRefreshBDMs.setText("Refresh");
      fButtonRefreshBDMs.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            fSuspendUpdate++;
            populateBdmChoices(null, true);
            fSuspendUpdate--;
            doUpdate();
         }
      });

      fLabelBDMInformation = new Label(grpSelectBdm, SWT.NONE);
      fLabelBDMInformation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
      fLabelBDMInformation.setToolTipText("Description of selected BDM");
      fLabelBDMInformation.setText("BDM Information");

      fButtonRequireExactBdm = new Button(grpSelectBdm, SWT.CHECK);
      fButtonRequireExactBdm.setToolTipText("Use only the selected BDM.\nOtherwise selection is preferred BDM.");
      fButtonRequireExactBdm.setText("Exact");
      fButtonRequireExactBdm.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

   }

   /**
    * Create GDB Server parameters Group
    * 
    * @param parent Parent of group
    */
   protected void createGdbServerGroup(Composite parent) {
      // System.err.println("UsbdmConnectionPanel::createPreferredBdmGroup()");

      Group grpGdbControl = new Group(parent, SWT.NONE);
      grpGdbControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));
      grpGdbControl.setText("GDB Server Control");
      GridLayout gridLayout = new GridLayout(2, false);
      grpGdbControl.setLayout(gridLayout);

      Label lbl = new Label(grpGdbControl, SWT.NONE);
      lbl.setText("Server Port ");
      fTextGdbServerPort = new Text(grpGdbControl, SWT.BORDER | SWT.FILL);
      fTextGdbServerPort.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      fTextGdbServerPort.setToolTipText("Port number used for GDB Server connection");
      fTextGdbServerPortAdapter = new NumberTextAdapter("GDB Server port", fTextGdbServerPort, 1234);
      fTextGdbServerPort.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });
      fButtonUseSemihosting = new Button(grpGdbControl, SWT.CHECK);
      fButtonUseSemihosting.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      fButtonUseSemihosting.setText("Open semi-hosting TTY");
      fButtonUseSemihosting.setToolTipText("Open a TTY console for GDB to connect to");
      fButtonUseSemihosting.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent arg0) {
            doUpdate();
         }
      });

      lbl = new Label(grpGdbControl, SWT.NONE);
      lbl.setText("TTY Port ");
      fTextGdbTtyPort = new Text(grpGdbControl, SWT.BORDER);
      fTextGdbTtyPort.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      fTextGdbTtyPort.setToolTipText("Port number used for GDB TTY connection");
      fTextGdbTtyPortAdapter = new NumberTextAdapter("GDB TTY port", fTextGdbTtyPort, 4321);
      fTextGdbTtyPort.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      fButtonUseDebug = new Button(grpGdbControl, SWT.CHECK);
      fButtonUseDebug.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      fButtonUseDebug.setToolTipText("Use debug version of server.");
      fButtonUseDebug.setText("Debug");
      fButtonUseDebug.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

      fButtonExitOnClose = new Button(grpGdbControl, SWT.CHECK);
      fButtonExitOnClose.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      fButtonExitOnClose.setToolTipText("Exit the server when the client connection is closed.");
      fButtonExitOnClose.setText("Exit on Close");
      fButtonExitOnClose.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });
      // btnPipeServer = new Button(grpGdbControl, SWT.RADIO);
      // btnPipeServer.setText("Pipe");
      // btnPipeServer.setToolTipText("Run server as Pipe e.g. target remote |
      // server");
      // btnPipeServer.addSelectionListener(new SelectionAdapter() {
      // @Override
      // public void widgetSelected(SelectionEvent e) {
      // doUpdate();
      // }
      // });
      // btnSocketServer = new Button(grpGdbControl, SWT.RADIO);
      // btnSocketServer.setText("Socket");
      // btnSocketServer.setToolTipText("Run server as separate GUI process
      // using sockets");
      // btnSocketServer.addSelectionListener(new SelectionAdapter() {
      // @Override
      // public void widgetSelected(SelectionEvent e) {
      // doUpdate();
      // }
      // });
   }

   /**
    * Create Connection parameters Group
    * 
    * @param parent Parent of group
    */
   protected void createConnectionGroup(Composite comp) {

      Group grpConnectionControl = new Group(comp, SWT.NONE);
      grpConnectionControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));
      grpConnectionControl.setLayout(new GridLayout(2, false));
      grpConnectionControl.setText("Connection Control"); //$NON-NLS-1$

      fComboInterfaceSpeed = new Combo(grpConnectionControl, SWT.READ_ONLY);
      fComboInterfaceSpeed.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      fComboInterfaceSpeed.setItems(JTAGInterfaceData.getConnectionSpeeds());
      fComboInterfaceSpeed.setToolTipText("Connection speed to use for BDM communications");
      fComboInterfaceSpeed.select(4);
      fComboInterfaceSpeed.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      Label lbl = new Label(grpConnectionControl, SWT.NONE);
      lbl.setText("Timeout");
      Text txtConnectionTimeout = new Text(grpConnectionControl, SWT.BORDER);
      txtConnectionTimeout.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      txtConnectionTimeout.setToolTipText("How long to wait for an unresponsive target\n" + "(e.g. target may be in VLLSx mode)\n" + "0 indicates indefinite wait");

      fConnectionTimeoutTextAdapter = new NumberTextAdapter("Timeout", txtConnectionTimeout, 10);
      // txtConnectionTimeout.setLayoutData(new GridData(30, 16));
      txtConnectionTimeout.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      fButtonAutomaticallyReconnect = new Button(grpConnectionControl, SWT.CHECK);
      fButtonAutomaticallyReconnect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      fButtonAutomaticallyReconnect.setToolTipText("Automatically re-sync with the target whenever target state is polled."); //$NON-NLS-1$
      fButtonAutomaticallyReconnect.setText("Auto reconnect"); //$NON-NLS-1$
      fButtonAutomaticallyReconnect.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

      fButtonCatchVLLSsEvents = new Button(grpConnectionControl, SWT.CHECK);
      fButtonCatchVLLSsEvents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      fButtonCatchVLLSsEvents.setToolTipText("Halt on resets due to VLLSx wakeup"); //$NON-NLS-1$
      fButtonCatchVLLSsEvents.setText("Catch VLLS events"); //$NON-NLS-1$
      fButtonCatchVLLSsEvents.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

      fButtonMaskInterrupts = new Button(grpConnectionControl, SWT.CHECK);
      fButtonMaskInterrupts.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      fButtonMaskInterrupts.setToolTipText("Mask interrupts when stepping"); //$NON-NLS-1$
      fButtonMaskInterrupts.setText("Mask Interrupts"); //$NON-NLS-1$
      fButtonMaskInterrupts.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

      fButtonDriveReset = new Button(grpConnectionControl, SWT.CHECK);
      fButtonDriveReset.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      fButtonDriveReset.setToolTipText("Drive target reset pin to reset the target."); //$NON-NLS-1$
      fButtonDriveReset.setText("Hardware RESET"); //$NON-NLS-1$
      fButtonDriveReset.setBounds(0, 0, 140, 16);
      fButtonDriveReset.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

      fButtonUsePstSignals = new Button(grpConnectionControl, SWT.CHECK);
      fButtonUsePstSignals.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      fButtonUsePstSignals.setToolTipText("Use PST signal to determine execution state of target.");
      fButtonUsePstSignals.setText("Use PST signals");
      fButtonUsePstSignals.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });
   }

   /**
    * Create Erase parameters Group
    * 
    * @param parent Parent of group
    */
   protected void createEraseGroup(Composite comp) {

      Group grpEraseOptions = new Group(comp, SWT.NONE);
      grpEraseOptions.setText("Erase Options");
      grpEraseOptions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));

      RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
      rowLayout.pack = false;
      rowLayout.justify = true;

      grpEraseOptions.setLayout(rowLayout);
      fComboEraseMethod = new Combo(grpEraseOptions, SWT.READ_ONLY);
      fComboEraseMethod.setToolTipText("Erase method used before programming");
      fComboEraseMethod.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });
   }

   /**
    * Create Security Selection Group
    * 
    * @param parent Parent of group
    */
   protected void createSecurityGroup(Composite parent) {
      // System.err.println("UsbdmConnectionPanel::createSecurityGroup()");

      Group grpSelectsecurity = new Group(parent, SWT.NONE);
      grpSelectsecurity.setText("Security Options");
      grpSelectsecurity.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));

      RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
      rowLayout.pack = false;
      rowLayout.justify = true;
      grpSelectsecurity.setLayout(rowLayout);

      fComboSecurityOption = new Combo(grpSelectsecurity, SWT.READ_ONLY);
      fComboSecurityOption.setToolTipText("Security options applied to the target when programming ");

      // Must be added in ordinal order
      fComboSecurityOption.add(SecurityOptions.SECURITY_IMAGE.toString());
      fComboSecurityOption.add(SecurityOptions.SECURITY_UNSECURED.toString());
      fComboSecurityOption.add(SecurityOptions.SECURITY_SMART.toString());
      fComboSecurityOption.select(SecurityOptions.SECURITY_SMART.ordinal());

      fComboSecurityOption.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            fSuspendUpdate++;
            updateBdmDescription();
            fSuspendUpdate--;
            doUpdate();
         }
      });
   }

   /**
    * Create Target Vdd parameters Group
    * 
    * @param parent Parent of group
    */
   protected void createTargetVddGroup(Composite comp) {

      Group grpTargetVddSupply = new Group(comp, SWT.NONE);
      grpTargetVddSupply.setText("Target Vdd"); //$NON-NLS-1$
      grpTargetVddSupply.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));

      RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
      rowLayout.pack = false;
      rowLayout.justify = false;
      rowLayout.fill = true;

      grpTargetVddSupply.setLayout(rowLayout);

      fTargetVdds = new TargetVddSelect[TargetVddSelect.values().length];
      fComboTargetVdd = new Combo(grpTargetVddSupply, SWT.READ_ONLY);
      fComboTargetVdd.setToolTipText("Target Vdd supplied from BDM to target");
      fComboTargetVdd.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            doUpdate();
         }
      });

      int index = 0;
      for (TargetVddSelect targetVdd : TargetVddSelect.values()) {
         if (targetVdd.ordinal() > TargetVddSelect.BDM_TARGET_VDD_5V.ordinal()) {
            continue;
         }
         fComboTargetVdd.add(targetVdd.toString());
         fTargetVdds[index++] = targetVdd;
         fComboTargetVdd.select(fComboTargetVdd.getItemCount() - 1);
      }
   }

   /**
    * Create Clock Trim parameters Group
    * 
    * @param parent Parent of group
    */
   protected void createTrimGroup(Composite comp) {

      fGroupClockTrim = new Group(comp, SWT.NONE);
      fGroupClockTrim.setText("Internal Clock Trim"); //$NON-NLS-1$
      fGroupClockTrim.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 2));

      fGroupClockTrim.setLayout(new GridLayout(3, false));

      fButtonTrimTargetClock = new Button(fGroupClockTrim, SWT.CHECK);
      fButtonTrimTargetClock.setText("Frequency"); //$NON-NLS-1$
      fButtonTrimTargetClock.setToolTipText("Enable trimming of target internal clock source\r\nto given frequency."); //$NON-NLS-1$
      fButtonTrimTargetClock.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            fSuspendUpdate++;
            enableTrim(((Button) e.getSource()).getSelection());
            fSuspendUpdate--;
            doUpdate();
         }
      });

      fTextTrimFrequency = new Text(fGroupClockTrim, SWT.BORDER);
      fTextTrimFrequencyAdapter = new DoubleTextAdapter(fTextTrimFrequency);
      fTextTrimFrequency.setTextLimit(7);
      fTextTrimFrequencyAdapter.setDoubleValue(0.0);
      fTextTrimFrequency.setToolTipText(""); //$NON-NLS-1$
      GridData gd_txtTrimFrequency = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      gd_txtTrimFrequency.widthHint = 65;
      gd_txtTrimFrequency.minimumWidth = 65;
      fTextTrimFrequency.setLayoutData(gd_txtTrimFrequency);
      fTextTrimFrequency.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      fLabelKhz = new Label(fGroupClockTrim, SWT.NONE);
      fLabelKhz.setToolTipText("The frequency to trim the internal clock source to.\r\n" + "Note this is NOT the bus clock frequency.");
      fLabelKhz.setText("kHz"); //$NON-NLS-1$

      fLabelNvtrimAddress = new Label(fGroupClockTrim, SWT.NONE);
      GridData gd_lblNvtrimAddress = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
      fLabelNvtrimAddress.setLayoutData(gd_lblNvtrimAddress);
      fLabelNvtrimAddress.setText("NVTRIM "); //$NON-NLS-1$
      fLabelNvtrimAddress.setToolTipText("Address of non-volatile memory location to write the trim value to."); //$NON-NLS-1$
      // new Label(fGroupClockTrim, SWT.NONE);

      fTextNVTRIMAddress = new Text(fGroupClockTrim, SWT.BORDER);
      fTextNVTRIMAddressAdapter = new HexTextAdapter("NVTRIM Address", fTextNVTRIMAddress, 0);
      fTextNVTRIMAddressAdapter.setRange(0, 0xFFFF);
      GridData gd_txtNVTRIMAddress = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      gd_txtNVTRIMAddress.widthHint = 65;
      gd_txtNVTRIMAddress.minimumWidth = 65;
      fTextNVTRIMAddress.setLayoutData(gd_txtNVTRIMAddress);
      fTextNVTRIMAddress.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            doUpdate();
         }
      });

      fLabelHex = new Label(fGroupClockTrim, SWT.NONE);
      fLabelHex.setToolTipText("Address of non-volatile memory location to write the trim value to."); //$NON-NLS-1$
      fLabelHex.setText("hex"); //$NON-NLS-1$
   }

   /**
    * Create Command Line display Group
    * 
    * @param parent Parent of group
    */
   protected void createCommandLineGroup(Composite parent) {
      Group group = new Group(parent, SWT.NONE);
      group.setText("Command line");
      group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      GridLayout layout = new GridLayout(1, false);
      group.setLayout(layout);
      fLabelcommandLine = new Label(group, SWT.FILL | SWT.WRAP);
      fLabelcommandLine.setToolTipText("Actual command line used to start GDB Server");
      fLabelcommandLine.setText("No Command");
      final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      gd.widthHint = 100;
      gd.heightHint = 80;
      fLabelcommandLine.setLayoutData(gd);
   }

   /**
    * Add interface names to Interface combo
    * 
    * @param interfaceTypeToSelect
    *           Used as initial selection
    */
   private void populateInterfaceTypes(InterfaceType interfaceTypeToSelect) {
      // System.err.println("populateInterfaceTypes() "+ interfaceTypeToSelect);
      int index = 0;
      if (fComboInterfaceTypeListener != null) {
         fComboInterfaceType.removeModifyListener(fComboInterfaceTypeListener);
      }
      fComboInterfaceType.removeAll();
      for (InterfaceType interfaceType : InterfaceType.values()) {
         fComboInterfaceType.add(interfaceType.toString());
         fInterfaceTypes[index++] = interfaceType;
      }
      if (fComboInterfaceTypeListener == null) {
         fComboInterfaceTypeListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
               // System.err.println("populateInterfaceTypes().modifyText()");
               String value = fComboInterfaceType.getText();
               if (!value.equals(fComboInterfaceType.getData())) {
                  fComboInterfaceType.setData(value);
                  if (value.length() > 0) {
                     fSuspendUpdate++;
                     try {
                        populateGdbServerControls();
                     } catch (Exception e1) {
                        e1.printStackTrace();
                     }
                     String deviceName = fGdbServerParameters.getDeviceName();
                     fTextTargetDeviceName.setText(deviceName);
                     populateBuildTools();
                     fSuspendUpdate--;
                     doUpdate();
                  }
               }
            }
         };
      }
      // Add watchers for user data entry
      fComboInterfaceType.addModifyListener(fComboInterfaceTypeListener);

      if (interfaceTypeToSelect != null) {
         // Select the required interface
         fComboInterfaceType.setText(interfaceTypeToSelect.toString());
      }
   }

   /**
    * Sets Interface type
    * 
    * @param interfaceType
    */
   public void setInterface(InterfaceType interfaceType, boolean fixed) {
      populateInterfaceTypes(interfaceType);
      fComboInterfaceType.setText(interfaceType.toString());
      fComboInterfaceType.setEnabled(!fixed);
   }

   private InterfaceType getInterfaceType() {
      int deviceIndex = fComboInterfaceType.getSelectionIndex();
      if (deviceIndex < 0) {
         deviceIndex = 0;
      }
      return fInterfaceTypes[deviceIndex];
   }

   /**
    * Populate multiple fields from the fGdbServerParameters<br>
    * 
    * It will update fGdbServerParameters to default if incompatible with current
    * interface type
    * 
    * @throws Exception
    */
   private void populateGdbServerControls() throws Exception {
      InterfaceType interfaceType = getInterfaceType();
      if ((fGdbServerParameters == null) || (fGdbServerParameters.getInterfaceType() != interfaceType)) {
         // Make GDB server parameters consistent with interface
         setGdbServerParameters(GdbServerParameters.getDefaultServerParameters(interfaceType));
      }
      loadGdbServerParameters();
   }

   protected void setTargetDevice(String deviceName) {
      fGdbServerParameters.setDeviceName(deviceName);
      fTextTargetDeviceName.setText(deviceName);
   }

   /**
    * Populate the Build Tool combo Build tool list depends on current interface
    * Tries to maintain currently selected build tool if possible
    */
   private void populateBuildTools() {

      if (fComboBuildTool == null) {
         return;
      }
      String currentBuildTool = fComboBuildTool.getText();
      Hashtable<String, ToolInformationData> toolData = ToolInformationData.getToolInformationTable();

      if (fBuildToolsIds == null) {
         fBuildToolsIds = new String[toolData.size() + 1];
      }
      fComboBuildTool.removeAll();
      final String customSettings = "Custom Settings";
      // Add Custom Settings entry
      int index = 0;
      fComboBuildTool.add(customSettings);
      fBuildToolsIds[index++] = "";

      String defaultBuildTool = customSettings;
      for (ToolInformationData toolInfo : toolData.values()) {
         if (!toolInfo.applicableTo(getInterfaceType())) {
            continue;
         }
         fComboBuildTool.add(toolInfo.getDescription());
         if (defaultBuildTool.equals(customSettings)) {
            // First added tool becomes default
            defaultBuildTool = toolInfo.getDescription();
         }
         fBuildToolsIds[index++] = toolInfo.getBuildToolId();
      }
      // Set to default
      fComboBuildTool.setText(defaultBuildTool);
      // try to set to last selected
      fComboBuildTool.setText(currentBuildTool);
   }

   private String getBuildToolId() {
      int index = fComboBuildTool.getSelectionIndex();
      if ((index < 0) || (index > fBuildToolsIds.length)) {
         return "";
      }
      return fBuildToolsIds[index];
   }

   /**
    * Populates the BDM choice control
    * 
    * @param preferredDevice
    *           A String representing the serial number of a previously selected
    *           device. This will be made the currently selected device (even if
    *           not connected).
    * @param scanForBdms
    *           If true a scan is made for currently connected BDMs
    */
   private void populateBdmChoices(String preferredDevice, boolean scanForBdms) {
      // System.err.println("populateBdmChoices(\'"+previousDevice+"\',
      // "+scanForBdms+")\n");

      if ((preferredDevice == null) || preferredDevice.trim().isEmpty()) {
         // Treat null or empty as null device (= ANY device)
         preferredDevice =  USBDMDeviceInfo.nullDevice.deviceSerialNumber;
      }
      if (scanForBdms) {
         // scan for connected BDMs
         // System.err.println("populateBdmChoices() - looking for BDMs...");
         fDeviceList = Usbdm.getDeviceList();
      } else {
         // Don't scan for BDMs - use an empty list
         fDeviceList = new ArrayList<USBDMDeviceInfo>();
      }
      // Always add a null device as 1st entry
      // System.err.println("populateBdmChoices() - Adding nullDevice");
      fDeviceList.add(0, USBDMDeviceInfo.nullDevice);

      // Add all devices to combo
      fComboSelectBDM.removeAll();
      ListIterator<Usbdm.USBDMDeviceInfo> it = fDeviceList.listIterator();
      while (it.hasNext()) {
         USBDMDeviceInfo di = it.next();
         // System.err.println( "populateBdmChoices() Adding BDM = " +
         // di.deviceSerialNumber);
         fComboSelectBDM.add(di.deviceSerialNumber);
      }
      int index = fComboSelectBDM.indexOf(preferredDevice);
      if (index >= 0) {
         // Preferred device is present
         fComboSelectBDM.select(index);
         // System.err.println("populateBdmChoices() selecting device by index =
         // \'"+fComboSelectBDM.getText()+"\'\n");
      } else {
         // Preferred device is not present.
         // This must be a previously selected device that is now not present.
         // Add dummy device representing previously used device and make preferred
         fDeviceList.add(new USBDMDeviceInfo("Previously selected device (not connected)", preferredDevice, new BdmInformation()));
         fComboSelectBDM.add(preferredDevice);
         fComboSelectBDM.setText(preferredDevice);
         // System.err.println("populateBdmChoices() Adding preferredDevice =
         // \'"+fComboSelectBDM.getText()+"\'\n");
      }
      updateBdmDescription();
   }

   /**
    * Populate the erase methods
    * 
    * These are filtered by fGdbServerParameters
    */
   private void populateEraseMethods() {
      String eraseMethod = fComboEraseMethod.getText();
      fEraseMethods = new EraseMethod[EraseMethod.values().length];
      int index = 0;
      fComboEraseMethod.removeAll();
      for (EraseMethod method : EraseMethod.values()) {
         if (fGdbServerParameters.isAllowedEraseMethod(method)) {
            fComboEraseMethod.add(method.toString());
            fEraseMethods[index++] = method;
         }
      }
      fComboEraseMethod.select(fComboEraseMethod.getItemCount() - 1);
      fComboEraseMethod.setText(eraseMethod);
   }

   private void setEraseMethod(EraseMethod eraseMethod) {
      // System.err.println("setEraseMethod() "+ eraseMethod.toString());
      fComboEraseMethod.setText(eraseMethod.toString());
   }

   @SuppressWarnings("unused")
   private void setEraseMethod(String eraseMethodName) {
      // System.err.println("setEraseMethod() "+ eraseMethodName);
      EraseMethod eraseMethod = EraseMethod.valueOf(eraseMethodName);
      setEraseMethod(eraseMethod);
   }

   private EraseMethod getEraseMethod() {
      int index = fComboEraseMethod.getSelectionIndex();
      if (index < 0) {
         return fGdbServerParameters.getPreferredEraseMethod();
      } else {
         return fEraseMethods[index];
      }
   }

   private void setTargetVdd(TargetVddSelect targetVdd) {
      // System.err.println("TargetVddSelect() "+ targetVdd.toString());
      fComboTargetVdd.setText(targetVdd.toString());
   }

   @SuppressWarnings("unused")
   private void setTargetVdd(String targetVddName) {
      // System.err.println("TargetVddSelect() "+ targetVddName);
      TargetVddSelect targetVdd = TargetVddSelect.valueOf(targetVddName);
      setTargetVdd(targetVdd);
   }

   private TargetVddSelect getTargetVdd() {
      int index = fComboTargetVdd.getSelectionIndex();
      if (index < 0) {
         return TargetVddSelect.BDM_TARGET_VDD_OFF;
      } else {
         return fTargetVdds[index];
      }
   }

   private void setTimeout(int delayInSeconds) {
      // System.err.println("setTimeout() "+ delayInSeconds);
      fConnectionTimeoutTextAdapter.setDecimalValue(delayInSeconds);
   }

   private int getTimeout() {
      return fConnectionTimeoutTextAdapter.getDecimalValue();
   }

   private void setSecurityOption(SecurityOptions securityOption) {
      // System.err.println("setSecurityOption() "+ securityOption.toString());
      fComboSecurityOption.setText(securityOption.toString());
   }

   @SuppressWarnings("unused")
   private void setSecurityOption(String securityOptionName) {
      // System.err.println("getSecurityOption() "+ securityOptionName);
      SecurityOptions securityOption = SecurityOptions.valueOf(securityOptionName);
      setSecurityOption(securityOption);
   }

   private SecurityOptions getSecurityOption() {
      int index = fComboSecurityOption.getSelectionIndex();
      if (index < 0) {
         return SecurityOptions.SECURITY_SMART;
      } else {
         return SecurityOptions.values()[index];
      }
   }

   /**
    * Update Trim control group
    * 
    * @param enabled
    *           Whether trimming is enabled
    * 
    * @note The entire group will be disabled if the clock is not valid
    */
   private void enableTrim(boolean enabled) {
      boolean groupEnabled = (fClockType != ClockTypes.INVALID) && (fClockType != ClockTypes.EXTERNAL);

      fButtonTrimTargetClock.setEnabled(groupEnabled);
      fTextTrimFrequency.setEnabled(groupEnabled & enabled);
      fLabelKhz.setEnabled(groupEnabled & enabled);
      fLabelNvtrimAddress.setEnabled(groupEnabled & enabled);
      fTextNVTRIMAddress.setEnabled(groupEnabled & enabled);
      fLabelHex.setEnabled(groupEnabled & enabled);
      fButtonTrimTargetClock.setSelection(enabled);
   }

   private void populateTrim() {
      enableTrim(fGdbServerParameters.isTrimClock());
      fTextTrimFrequencyAdapter.setDoubleValue(fGdbServerParameters.getClockTrimFrequency() / 1000.0);
      fTextNVTRIMAddressAdapter.setValue(fGdbServerParameters.getNvmClockTrimLocation());
   }

   private int getInterfaceSpeed() {
      return JTAGInterfaceData.ClockSpeed.parse(fComboInterfaceSpeed.getText()).getFrequency();
   }

   protected void disableUnusedControls() {
      boolean enableThese;
      enableThese = fGdbServerParameters.isRequiredDialogueComponents(GdbServerParameters.NEEDS_SPEED);
      fComboInterfaceSpeed.setEnabled(enableThese);
      fComboInterfaceSpeed.setVisible(enableThese);
      enableThese = fGdbServerParameters.isRequiredDialogueComponents(GdbServerParameters.NEEDS_RESET);
      fButtonDriveReset.setEnabled(enableThese);
      fButtonDriveReset.setVisible(enableThese);
      enableThese = fGdbServerParameters.isRequiredDialogueComponents(GdbServerParameters.NEEDS_PST);
      fButtonUsePstSignals.setEnabled(enableThese);
      fButtonUsePstSignals.setVisible(enableThese);
      enableThese = fGdbServerParameters.isRequiredDialogueComponents(GdbServerParameters.NEEDS_VLLSCATCH);
      fButtonCatchVLLSsEvents.setEnabled(enableThese);
      fButtonCatchVLLSsEvents.setVisible(enableThese);
      enableThese = fGdbServerParameters.isRequiredDialogueComponents(GdbServerParameters.NEEDS_MASKINTS);
      fButtonMaskInterrupts.setEnabled(enableThese);
      fButtonMaskInterrupts.setVisible(enableThese);
      enableThese = fGdbServerParameters.isRequiredDialogueComponents(GdbServerParameters.NEEDS_CLKTRIM);
      fGroupClockTrim.setVisible(enableThese);
      fButtonTrimTargetClock.setEnabled(enableThese);
      fButtonTrimTargetClock.setVisible(enableThese);
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
         fTextGdbCommand.setText("${" + toolInfo.getPrefixVariableName() + "}" + UsbdmSharedConstants.GDB_NAME);
         fTextGdbBinPath.setText("${" + toolInfo.getPathVariableName() + "}");
      }
      fTextGdbCommand.setEnabled(toolInfo == null);
      fButtonGdbCommandVariables.setEnabled(toolInfo == null);

      fTextGdbBinPath.setEnabled(toolInfo == null);
      fButtonGdbBinPathBrowse.setEnabled(toolInfo == null);
      fButtonGdbBinPathVariables.setEnabled(toolInfo == null);
   }

   /**
    * Updates the description of the selected BDM
    */
   private void updateBdmDescription() {
      if (fLabelBDMInformation != null) {
         int index = fComboSelectBDM.getSelectionIndex();
         if (index >= 0) {
            String deviceDescription = fDeviceList.get(index).deviceDescription;
            fLabelBDMInformation.setText(deviceDescription);
         }
      }
   }

   /**
    * Try to get interface type from project via ILaunchConfiguration
    * 
    * @param configuration
    * @return
    */
   private ToolInformationData getToolInformationDataFromConfig(ILaunchConfiguration configuration) {

      String buildToolsId = null;
      try {
         // ToDo Consider using - ICProject projectHandle =
         // CDebugUtils.verifyCProject(configuration);
         String projectName = configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
         String projectBuildId = configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_BUILD_CONFIG_ID, (String) null);

         // System.err.println("projectName = "+projectName);
         // System.err.println("projectBuildId = "+projectBuildId);

         if ((projectName != null) && (projectBuildId != null)) {
            ICProject projectHandle = CoreModel.getDefault().getCModel().getCProject(projectName);
            IConfiguration buildConfig = ManagedBuildManager.getBuildInfo(projectHandle.getProject()).getManagedProject().getConfiguration(projectBuildId);
            if (buildConfig != null) {
               IToolChain toolChain = buildConfig.getToolChain();
               if (toolChain != null) {
                  IOption option = toolChain.getOptionBySuperClassId("net.sourceforge.usbdm.cdt.arm.toolchain.buildtools");
                  if (option == null) {
                     option = toolChain.getOptionBySuperClassId("net.sourceforge.usbdm.cdt.coldfire.toolchain.buildtools");
                  }
                  if (option != null) {
                     // System.err.println("option(net.sourceforge.usbdm.cdt.arm.toolchain.buildtools).getId()
                     // = "+option.getId());
                     // System.err.println("option(net.sourceforge.usbdm.cdt.arm.toolchain.buildtools).getName()
                     // = "+option.getName());
                     buildToolsId = option.getStringValue();
                  }
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      // System.err.println("Selected Build Tools ID = " + buildToolsId);

      ToolInformationData toolInformationData = null;
      if (buildToolsId != null) {
         toolInformationData = ToolInformationData.get(buildToolsId);
      }
      // System.err.println("Selected Build Tools = " + toolInformationData);
      return toolInformationData;
   }

   public void setGdbServerParameters(GdbServerParameters gdbServerParameters) {
//      System.err.println("UsbdmDebuggerPanel.setGdbServerParameters()");
      fGdbServerParameters = gdbServerParameters;
   }

   public GdbServerParameters getGdbServerParameters() {
      return fGdbServerParameters;
   }

   public boolean saveGdbServerParametersAsDefault() throws Exception {
      saveGdbServerParameters();
      return fGdbServerParameters.saveSettingsAsDefault();
   }

   /**
    * Loads multiple dialogue fields from the current fGdbServerParameters
    */
   private void loadGdbServerParameters() {
//       System.err.println("loadGdbServerParameters()");
      // System.err.print(fGdbServerParameters.toString());
      if (fGdbServerParameters == null) {
         System.err.println("loadGdbServerParameters() fGdbServerParameters == null");
         return;
      }
      fSuspendUpdate++;
//      System.err.println("UsbdmDebuggerPanel.loadGdbServerParameters() VLLS = " + fGdbServerParameters.isCatchVLLSxEvents());
      fTextTargetDeviceName.setText(fGdbServerParameters.getDeviceName());
      populateBdmChoices(fGdbServerParameters.getBdmSerialNumber(), true);
      fButtonRequireExactBdm.setSelection(fGdbServerParameters.isBdmSerialNumberMatchRequired());
      fTextGdbServerPortAdapter.setDecimalValue(fGdbServerParameters.getGdbServerPortNumber());
      fTextGdbTtyPortAdapter.setDecimalValue(fGdbServerParameters.getGdbTtyPortNumber());
      fButtonUseSemihosting.setSelection(fGdbServerParameters.isUseSemihosting());
      fButtonUseDebug.setSelection(fGdbServerParameters.isUseDebugVersion());
      fButtonExitOnClose.setSelection(fGdbServerParameters.isExitOnClose());
      fComboInterfaceSpeed.setText(ClockSpeed.findSuitable(fGdbServerParameters.getInterfaceFrequency()).toString());
      fButtonAutomaticallyReconnect.setSelection(fGdbServerParameters.getAutoReconnect().name() != AutoConnect.AUTOCONNECT_NEVER.name());
      fButtonDriveReset.setSelection(fGdbServerParameters.isUseReset());
      fButtonUsePstSignals.setSelection(fGdbServerParameters.isUsePstSignals());
      fButtonCatchVLLSsEvents.setSelection(fGdbServerParameters.isCatchVLLSxEvents());
//      System.err.println("loadGdbServerParameters() VLLS = " + fGdbServerParameters.isCatchVLLSxEvents());

      fButtonMaskInterrupts.setSelection(fGdbServerParameters.isMaskInterrupts());
      // Update list to match fGdbServerParameters
      populateEraseMethods();
      setEraseMethod(fGdbServerParameters.getEraseMethod());
      setTargetVdd(fGdbServerParameters.getTargetVdd());
      setSecurityOption(fGdbServerParameters.getSecurityOption());
      setTimeout(fGdbServerParameters.getConnectionTimeout());
      populateTrim();
      disableUnusedControls();
      fSuspendUpdate--;
   }

   /**
    * Save multiple dialogue fields to the current fGdbServerParameters
    */
   private void saveGdbServerParameters() {
//      System.err.println("saveGdbServerParameters()");

      fGdbServerParameters.setDeviceName(fTextTargetDeviceName.getText());
      fGdbServerParameters.setBdmSerialNumber(fComboSelectBDM.getText());
      fGdbServerParameters.enableBdmSerialNumberMatchRequired(fButtonRequireExactBdm.getSelection());
      fGdbServerParameters.setGdbServerPortNumber(fTextGdbServerPortAdapter.getDecimalValue());
      fGdbServerParameters.setGdbTtyPortNumber(fTextGdbTtyPortAdapter.getDecimalValue());
      fGdbServerParameters.enableUseSemiHosting(fButtonUseSemihosting.getSelection());
      fGdbServerParameters.enableUseDebugVersion(fButtonUseDebug.getSelection());
      fGdbServerParameters.enableExitOnClose(fButtonExitOnClose.getSelection());
      fGdbServerParameters.setInterfaceFrequency(getInterfaceSpeed());
      fGdbServerParameters.setAutoReconnect(fButtonAutomaticallyReconnect.getSelection() ? AutoConnect.AUTOCONNECT_ALWAYS : AutoConnect.AUTOCONNECT_NEVER);
      fGdbServerParameters.enableUseReset(fButtonDriveReset.getSelection());
      fGdbServerParameters.enableUsePstSignals(fButtonUsePstSignals.getSelection());
      fGdbServerParameters.enableCatchVLLSxEvents(fButtonCatchVLLSsEvents.getSelection());
      fGdbServerParameters.enableMaskInterrupts(fButtonMaskInterrupts.getSelection());
      fGdbServerParameters.setEraseMethod(getEraseMethod());
      fGdbServerParameters.setSecurityOption(getSecurityOption());
      fGdbServerParameters.setTargetVdd(getTargetVdd());
      fGdbServerParameters.enableTrimClock(fButtonTrimTargetClock.getSelection());
      fGdbServerParameters.setClockTrimFrequency((int) Math.round(fTextTrimFrequencyAdapter.getDoubleValue() * 1000));
      fGdbServerParameters.setNvmClockTrimLocation(fTextNVTRIMAddressAdapter.getHexValue());
      fGdbServerParameters.setConnectionTimeout(getTimeout());
   }

   /**
    * Initialises the given launch configuration with default values for this
    * panel. This method is called when a new launch configuration is created
    * such that the configuration can be initialized with meaningful values.
    * This method may be called before this tab's control is created.
    * 
    * @note No meaningful defaults as too dependent on device type
    * 
    * @param configuration launch configuration
    */
   public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
//      System.err.println("UsbdmDebuggerPanel.setDefaults()");
   }

   /**
    * Initialises this panel's controls with values from the given launch
    * configuration. This method is called when a configuration is selected to
    * view or edit, after this tab's control has been created.
    * 
    * @param configuration
    *           launch configuration
    * @throws Exception
    */
   public void initializeFrom(ILaunchConfiguration configuration) throws Exception {
//      System.err.println("UsbdmDebuggerPanel.initializeFrom() "+configuration);

      try {
         // Get interface type from settings
         InterfaceType       interfaceType       = null;
         ToolInformationData toolInformationData = null;

         if (configuration != null) {
//            System.err.println("UsbdmDebuggerPanel.initializeFrom() #1 "+configuration);
            String interfaceTypeName = configuration.getAttribute(USBDM_GDB_INTERFACE_TYPE_KEY, (String) null);
            if (interfaceTypeName != null) {
               interfaceType = InterfaceType.valueOf(interfaceTypeName);
//               System.err.println("Setting interface to launch saved value = "+interfaceType.toString());
            }
            // Try to get tool information from project via configuration
            toolInformationData = getToolInformationDataFromConfig(configuration);
         }

         Boolean discardSettings = (configuration == null);
         if ((interfaceType == null) || ((toolInformationData != null) && !toolInformationData.applicableTo(interfaceType))) {
            // Interface type not set or incompatible - reset settings
//            System.err.println("UsbdmDebuggerPanel.initializeFrom() #2 "+configuration);
            discardSettings = true;
            // System.err.println("Interface type is missing or incompatible");
            // Interface type not set or incompatible - reset
            if (toolInformationData == null) {
               // Use ARM as default
               interfaceType = InterfaceType.T_ARM;
               // System.err.println("Setting interface to default =
               // "+interfaceType.toString());
            } else {
               // Use tool default
               interfaceType = toolInformationData.getPreferredInterfaceType();
               // System.err.println("Setting interface to tool default =
               // "+interfaceType.toString());
            }
         }
         // System.err.println("Interface = "+interfaceType.toString());

         // Populate & set the initial interface
         populateInterfaceTypes(interfaceType);

         // Load default settings for this target
//         System.err.println("UsbdmDebuggerPanel.initializeFrom() new fGdbServerParameters");
         setGdbServerParameters(GdbServerParameters.getDefaultServerParameters(interfaceType));

         // Update from configuration (if appropriate)
         if (!discardSettings) {
            // Only load if appropriate to current interface
//            System.err.println("Loading fGdbServerParameters from settings");
            fGdbServerParameters.initializeFrom(configuration, USBDM_LAUNCH_ATTRIBUTE_KEY);

         }
         // Load GDB Server parameters into controls
         // System.err.println("Loading fGdbServerParameters into controls");
         loadGdbServerParameters();
         
         if (!discardSettings) {
            // Only load settings if appropriate to interface
            fTextGdbBinPath.setText(configuration.getAttribute(USBDM_GDB_BIN_PATH_KEY, ""));
            fTextGdbCommand.setText(configuration.getAttribute(USBDM_GDB_COMMAND_KEY, ""));

            String buildToolId = configuration.getAttribute(USBDM_BUILD_TOOL_KEY, "");
            ToolInformationData toolInfo = ToolInformationData.get(buildToolId);
            if (toolInfo != null) {
               fComboBuildTool.setText(ToolInformationData.get(buildToolId).getDescription());
            } else {
               fComboBuildTool.select(0);
            }
            buildToolSelectionChanged();

//            boolean verboseModeAttr = false; // configuration.getAttribute(
//                                             // ATTR_DEBUGGER_VERBOSE_MODE,
//                                             // false);
//            btnVerboseMode.setSelection(verboseModeAttr);
         }
      } catch (CoreException e) {
         e.printStackTrace();
      }
   }

   /**
    * Copies values from this Panel into the given launch configuration.
    * 
    * @param configuration
    *           launch configuration
    */
   public void performApply(ILaunchConfigurationWorkingCopy configuration) {
//      System.err.println("UsbdmDebuggerPanel.performApply()");

      // Save GDB settings
      saveGdbServerParameters();
      fGdbServerParameters.performApply(configuration, USBDM_LAUNCH_ATTRIBUTE_KEY);
      
      configuration.setAttribute(USBDM_GDB_INTERFACE_TYPE_KEY, getInterfaceType().name());

      configuration.setAttribute(USBDM_BUILD_TOOL_KEY, getBuildToolId());
      configuration.setAttribute(USBDM_GDB_COMMAND_KEY, fTextGdbCommand.getText().trim());
      configuration.setAttribute(USBDM_GDB_BIN_PATH_KEY, fTextGdbBinPath.getText().trim());

      // DSF GDB Launcher needs this
      String gdbPath = fTextGdbBinPath.getText().trim();
      if (gdbPath.length() != 0) {
         gdbPath += File.separator;
      }
      gdbPath += fTextGdbCommand.getText().trim();
      configuration.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, gdbPath);
   }

   private void doUpdate() {
      if (fSuspendUpdate > 0) {
         return;
      }
      saveGdbServerParameters();
      if (fListener != null) {
         fListener.handleEvent(new Event());
      }
      fTextGdbTtyPort.setEnabled(fButtonUseSemihosting.getSelection());
      fLabelcommandLine.setText(fGdbServerParameters.getServerCommandLineAsString());
   }

   public void addListener(int eventType, Listener listener) {
      if (eventType == SWT.CHANGED) {
         this.fListener = listener;
      }
   }

   /**
    * ========================================================================
    */

   /**
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);

      shell.setLayout(new FillLayout());

      shell.setSize(600, 450);

      UsbdmDebuggerPanel usbdmTab = new UsbdmDebuggerPanel();
      usbdmTab.createContents(shell, true);
      try {
         usbdmTab.initializeFrom(null);
      } catch (Exception e) {
         e.printStackTrace();
      }
      usbdmTab.setInterface(InterfaceType.T_ARM, false);
      usbdmTab.addListener(SWT.CHANGED, new Listener() {
         @Override
         public void handleEvent(Event event) {
            // System.err.println("Changed");
         }
      });
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }
}
