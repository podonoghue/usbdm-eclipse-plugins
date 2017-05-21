package net.sourceforge.usbdm.connections.usbdm;

import java.util.ArrayList;
import java.util.ListIterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.freescale.cdt.debug.cw.core.ui.publicintf.AbstractPhysicalConnectionPanel;
import com.freescale.cdt.debug.cw.core.ui.publicintf.ISettingsListener;
import com.freescale.cdt.debug.cw.core.ui.settings.PrefException;
import com.freescale.cdt.debug.cw.mcu.common.publicintf.ICWGdiInitializationData;
import net.sourceforge.usbdm.jni.JTAGInterfaceData;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.Usbdm.BdmInformation;
import net.sourceforge.usbdm.jni.Usbdm.EraseMethod;
import net.sourceforge.usbdm.jni.Usbdm.SecurityOptions;
import net.sourceforge.usbdm.jni.Usbdm.USBDMDeviceInfo;

public class UsbdmConnectionPanel 
extends AbstractPhysicalConnectionPanel
implements ICWGdiInitializationData {

   final class BdmOptions extends UsbdmCommon.BdmOptions {
      public BdmOptions() {
         autoReconnect            = 1;
         guessSpeed               = 1;
         cycleVddOnConnect        = 0;
         cycleVddOnReset          = 0;
         leaveTargetPowered       = 0;
         connectionSpeed          = 1500000; // Hz
         targetVdd                = UsbdmCommon.BDM_TARGET_VDD_OFF;
         useAltBDMClock           = UsbdmCommon.BDM_CLK_DEFAULT;
         usePSTSignals            = 0;
         useResetSignal           = 0;
         maskinterrupts           = 0;
         derivative_type          = 0;
         clockTrimFrequency       = 0;
         clockTrimNVAddress       = 0;
         doClockTrim              = false;
         powerOffDuration         = 1000;
         powerOnRecoveryInterval  = 1000;
         resetDuration            = 500;
         resetReleaseInterval     = 500;
         resetRecoveryInterval    = 500;
      }
   };

   protected final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

   protected Shell shell;
   protected ILaunchConfiguration launchConfiguration;
   
   // State
   protected final BdmOptions defaultBdmOptions = new BdmOptions();
   protected UsbdmCommon.BdmOptions bdmOptions  = new BdmOptions();
   protected String preferredBdm;
   protected boolean useDebugBuild;
   
   protected ArrayList<USBDMDeviceInfo> deviceList;

   protected EraseMethod            eraseMethod;
   protected ArrayList<EraseMethod> permittedEraseMethods =  new ArrayList<EraseMethod>();
   
   protected SecurityOptions   securityOption;
   
   protected String            gdiDllName;
   protected String            gdiDebugDllName;
   protected String            connectionTypeId;
   protected String            deviceNameId;
   protected String            deviceNote = "";
   protected String            attributeKey = UsbdmCommon.BaseAttributeName+".";

   // GUI Elements
   protected Combo             comboSelectBDM;
   protected Button            btnRefreshBDMs;
   protected Label             lblBDMInformation;
   protected Button            btnDefault;
   protected Button            btnAdvancedOptions;
   protected HexTextAdapter    txtNVTRIMAddressAdapter;
   protected Label             lblNvtrimAddress;
   protected Text              txtTrimFrequency;
   protected DoubleTextAdapter txtTrimFrequencyAdapter;
   protected Text              txtNVTRIMAddress;
   protected Label             lblTrimFrequency;
   protected Label             lblKhz;
   protected Label             lblHex;
   protected Button            btnTrimTargetClock;
   protected Button            btnTargetVddOff;
   protected Button            btnTargetVdd3V3;
   protected Button            btnTargetVdd5V;
   protected Button            btnCycleTargetVddOnReset;
   protected Button            btnCycleTargetVddOnConnect;
   protected Button            btnLeaveTargetPowered;
   protected Button            btnUseDebugBuild;
   protected Label             lblTargetId;
   
   protected Button            btnAutomaticallyReconnect;
   protected Button            btnDriveReset;
   protected Button            btnBDMClockDefault;
   protected Button            btnBDMClockBus;
   protected Button            btnBDMClockAlt;
   protected Combo             comboEraseMethod;
   protected Combo             comboSecurityOption;

   protected Button            btnUsePstSignals;
   protected Combo             comboConnectionSpeed;
   
   protected final int NEEDS_PST   = (1<<0);
   protected final int NEEDS_RESET = (1<<1);
   protected final int NEEDS_SPEED = (1<<2);

   int count = 0;

   /**
    * Dummy listener for WindowBuilder Pro. & debugging
    * 
    * @param parent
    * @param style
    */
   protected static class dummyISettingsListener implements ISettingsListener {
      public ModifyListener getModifyListener() {
         return new ModifyListener() {
            public void modifyText(ModifyEvent e) {
               System.err.println("UsbdmConnectionPanel::dummyISettingsListener.ModifyListener.modifyText");
            }
         };
      }
      public SelectionListener getSelectionListener() {
         return new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
               System.err.println("UsbdmConnectionPanel::dummyISettingsListener.SelectionListener.widgetSelected");
            }
            public void widgetDefaultSelected(SelectionEvent e) {
               System.err.println("UsbdmConnectionPanel::dummyISettingsListener.SelectionListener.widgetDefaultSelected");
            }
         };
      }
      public void settingsChanged() {
      }
   }

   /**
    * Dummy constructor for WindowBuilder Pro. & debugging
    * 
    * @param parent
    * @param style
    */
   public UsbdmConnectionPanel(Composite parent, int style) {
      super(new dummyISettingsListener(), parent, style, "None"); 
//      System.err.println("UsbdmConnectionPanel::UsbdmConnectionPanel()");
      init();
   }

   /**
    * Actual constructor 
    * 
    * @param listener
    * @param parent
    * @param swtstyle
    * @param protocolPlugin    Indicates the connection type e.g. "HCS08 GDI"
    * @param connectionTypeId  A long string indicating the architecture??
    */
   public UsbdmConnectionPanel( 
         ISettingsListener listener, 
         Composite         parent,
         int               swtstyle, 
         String            protocolPlugin,
         String            connectionTypeId) {

      super(listener, parent, swtstyle, connectionTypeId);

//      System.err.println("USBDMConnectionPanel::USBDMConnectionPanel(protocolPlugin="+protocolPlugin+", connectionTypeId = "+connectionTypeId+")");

      this.connectionTypeId = connectionTypeId;
      addDisposeListener(new DisposeListener() {
         public void widgetDisposed(DisposeEvent e) {
            toolkit.dispose();
         };
      });
//      getShell().addListener(SWT.Close, new Listener() {
//         public void handleEvent(Event event) {
//            MessageBox msgBox = new MessageBox(getShell(), SWT.APPLICATION_MODAL|SWT.YES|SWT.NO);
//            msgBox.open();
//            event.doit = event.doit && (count++>4);
//         }
//      });
      init();
   }
   
   private void init() {
//      System.err.println("UsbdmConnectionPanel::init()");
      shell = this.getShell();
   }

   public void create() {
//      System.err.println("USBDMConnectionPanel::create");
      createContents(this);
      addSettingsChangedListeners();
   }

   protected void appendContents(Composite comp) {
   }
   
   protected void createPreferredBdmGroup(Composite comp) {
//      System.err.println("UsbdmConnectionPanel::createPreferredBdmGroup()");

      Group grpSelectBdm = new Group(comp, SWT.NONE);
      grpSelectBdm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      grpSelectBdm.setText("Preferred BDM");
      grpSelectBdm.setLayout(new GridLayout(2, false));
      toolkit.adapt(grpSelectBdm);
      toolkit.paintBordersFor(grpSelectBdm);

      comboSelectBDM = new Combo(grpSelectBdm, SWT.READ_ONLY);
      comboSelectBDM.setToolTipText("Allows selection of  preferred BDM from those currently attached.\r\n" +
                                    "Only used if multiple BDMs are attached when debugging.");
      comboSelectBDM.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            updateBdmDescription();
         }
      });
      GridData gd_comboSelectBDM = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
      gd_comboSelectBDM.minimumWidth = 200;
      comboSelectBDM.setLayoutData(gd_comboSelectBDM);
      toolkit.adapt(comboSelectBDM);
      toolkit.paintBordersFor(comboSelectBDM);

      populateBdmChoices(null, false);

      btnRefreshBDMs = new Button(grpSelectBdm, SWT.NONE);
      btnRefreshBDMs.setToolTipText("Check for connected BDMs");
      btnRefreshBDMs.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            populateBdmChoices(null, true);
         }
      });
      toolkit.adapt(btnRefreshBDMs, true, true);
      btnRefreshBDMs.setText("Refresh");

      lblBDMInformation = new Label(grpSelectBdm, SWT.NONE);
      lblBDMInformation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
      lblBDMInformation.setToolTipText("Description of selected BDM");
      toolkit.adapt(lblBDMInformation, true, true);
      lblBDMInformation.setText("BDM Information");
      new Label(grpSelectBdm, SWT.NONE);
   }
   
   protected void createButtonGroup(Composite comp) {
      
      Composite composite_1 = new Composite(comp, SWT.NONE);
      GridData gd_composite_1 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
      gd_composite_1.verticalIndent = 10;
      gd_composite_1.horizontalIndent = 5;
      composite_1.setLayoutData(gd_composite_1);
      composite_1.setLayout(new GridLayout(1,true));
      toolkit.adapt(composite_1);
      toolkit.paintBordersFor(composite_1);

      btnDefault = new Button(composite_1, SWT.NONE);
      btnDefault.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            restoreDefaultSettings();
            transferToWindow();
         }
      });
      gd_composite_1 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
      btnDefault.setLayoutData(gd_composite_1);
      btnDefault.setToolTipText("Restore dialogue to default values"); //$NON-NLS-1$
      toolkit.adapt(btnDefault, true, true);
      btnDefault.setText("Restore Default"); //$NON-NLS-1$

      btnAdvancedOptions = new Button(composite_1, SWT.NONE);
      btnAdvancedOptions.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            USBDMAdvancedOptionsDialogue advancedDialogue = new USBDMAdvancedOptionsDialogue(shell, 0, bdmOptions);
            advancedDialogue.open();
            bdmOptions = advancedDialogue.getBdmOptions();
         }
      });
      gd_composite_1 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
      btnAdvancedOptions.setLayoutData(gd_composite_1);
      btnAdvancedOptions.setToolTipText("Open Advanced Options dialog"); //$NON-NLS-1$
      toolkit.adapt(btnAdvancedOptions, true, true);
      btnAdvancedOptions.setText("Advanced Options"); //$NON-NLS-1$
   }

   protected void creatTargetVddGroup(Composite comp) {

      Group grpTargetVddSupply = new Group(comp, SWT.NONE);
      grpTargetVddSupply.setText("Target Vdd Supply"); //$NON-NLS-1$
      RowLayout rl_grpTargetVddSupply = new RowLayout(SWT.VERTICAL);
      rl_grpTargetVddSupply.marginHeight = 3;
      rl_grpTargetVddSupply.spacing = 5;
      rl_grpTargetVddSupply.fill = true;
      grpTargetVddSupply.setLayout(rl_grpTargetVddSupply);
      grpTargetVddSupply.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));
      toolkit.adapt(grpTargetVddSupply);
      toolkit.paintBordersFor(grpTargetVddSupply);

      Composite composite = new Composite(grpTargetVddSupply, SWT.NONE);
      toolkit.adapt(composite);
      toolkit.paintBordersFor(composite);
      composite.setLayout(new FillLayout(SWT.HORIZONTAL));

      btnTargetVddOff = new Button(composite, SWT.RADIO);
      btnTargetVddOff.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            setTargetVdd(UsbdmCommon.BDM_TARGET_VDD_OFF);
         }
      });
      btnTargetVddOff.setToolTipText("Do not supply power to the target.  An external target supply is required."); //$NON-NLS-1$
      toolkit.adapt(btnTargetVddOff, true, true);
      btnTargetVddOff.setText("Off"); //$NON-NLS-1$

      btnTargetVdd3V3 = new Button(composite, SWT.RADIO);
      btnTargetVdd3V3.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            setTargetVdd(UsbdmCommon.BDM_TARGET_VDD_3V3);
         }
      });
      btnTargetVdd3V3.setToolTipText("Supply 3.3V to the target through the BDM connection."); //$NON-NLS-1$
      toolkit.adapt(btnTargetVdd3V3, true, true);
      btnTargetVdd3V3.setText("3V3"); //$NON-NLS-1$

      btnTargetVdd5V = new Button(composite, SWT.RADIO);
      btnTargetVdd5V.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            setTargetVdd(UsbdmCommon.BDM_TARGET_VDD_5V);
         }
      });
      btnTargetVdd5V.setToolTipText("Supply 5V to the target through the BDM connection."); //$NON-NLS-1$
      toolkit.adapt(btnTargetVdd5V, true, true);
      btnTargetVdd5V.setText("5V"); //$NON-NLS-1$

      btnCycleTargetVddOnReset = new Button(grpTargetVddSupply, SWT.CHECK);
      btnCycleTargetVddOnReset.setToolTipText("Cycle target supply when resetting."); //$NON-NLS-1$
      toolkit.adapt(btnCycleTargetVddOnReset, true, true);
      btnCycleTargetVddOnReset.setText("Cycle target Vdd on reset"); //$NON-NLS-1$

      btnCycleTargetVddOnConnect = new Button(grpTargetVddSupply, SWT.CHECK);
      btnCycleTargetVddOnConnect.setToolTipText("Cycle target Vdd if having trouble connecting to the target."); //$NON-NLS-1$
      btnCycleTargetVddOnConnect.setText("Cycle target Vdd on connection problems"); //$NON-NLS-1$
      btnCycleTargetVddOnConnect.setBounds(0, 0, 433, 16);
      toolkit.adapt(btnCycleTargetVddOnConnect, true, true);

      btnLeaveTargetPowered = new Button(grpTargetVddSupply, SWT.CHECK);
      btnLeaveTargetPowered.setToolTipText("Leave target powered when leaving the debugger");
      btnLeaveTargetPowered.setText("Leave target powered on exit"); //$NON-NLS-1$
      btnLeaveTargetPowered.setBounds(0, 0, 433, 16);
      toolkit.adapt(btnLeaveTargetPowered, true, true);
   }
   
   protected void createConnectionGroup(Composite comp, int flags) {

      Group grpConnectionControl = new Group(comp, SWT.NONE);
      if ((flags&NEEDS_SPEED) != 0) {
         grpConnectionControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));
      }
      else {
         grpConnectionControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      }
//      rl_grpConnectionControl.marginHeight = 3;
      grpConnectionControl.setLayout(new GridLayout(2, false));
      grpConnectionControl.setText("Connection Control"); //$NON-NLS-1$
      toolkit.adapt(grpConnectionControl);
      toolkit.paintBordersFor(grpConnectionControl);

      btnAutomaticallyReconnect = new Button(grpConnectionControl, SWT.CHECK);
      btnAutomaticallyReconnect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      btnAutomaticallyReconnect.setToolTipText("Automatically re-sync with the target whenever target state is polled."); //$NON-NLS-1$
      toolkit.adapt(btnAutomaticallyReconnect, true, true);
      btnAutomaticallyReconnect.setText("Automatically re-connect"); //$NON-NLS-1$

      if ((flags&NEEDS_RESET) != 0) {
         btnDriveReset = new Button(grpConnectionControl, SWT.CHECK);
         btnDriveReset.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
         btnDriveReset.setToolTipText("Drive target reset pin when resetting the target."); //$NON-NLS-1$
         btnDriveReset.setText("Drive RESET pin"); //$NON-NLS-1$
         btnDriveReset.setBounds(0, 0, 140, 16);
         toolkit.adapt(btnDriveReset, true, true);
      }
      if ((flags&NEEDS_PST) != 0) {
         btnUsePstSignals = new Button(grpConnectionControl, SWT.CHECK);
         btnUsePstSignals.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
         btnUsePstSignals.setToolTipText("Use PST signal to determine execution state of target.");
         btnUsePstSignals.setText("Use PST signals");
      }         
      if ((flags&NEEDS_SPEED) != 0) {
         Label lblConnectionSpeed = new Label(grpConnectionControl, SWT.NONE);
         lblConnectionSpeed.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
         lblConnectionSpeed.setToolTipText("Connection speed to use for BDM communications.\r\nSpeed < Target Clock frequency/5");
         lblConnectionSpeed.setText("Connection\r\nSpeed");
         
         comboConnectionSpeed = new Combo(grpConnectionControl, SWT.READ_ONLY);
         comboConnectionSpeed.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
         comboConnectionSpeed.setItems(JTAGInterfaceData.getConnectionSpeeds());
         comboConnectionSpeed.setToolTipText("");
         comboConnectionSpeed.select(4);
      }
   }
   
   protected void createTrimGroup(Composite comp) {
      
      Group grpClockTrim = new Group(comp, SWT.NONE);
      grpClockTrim.setText("Internal Clock Trim"); //$NON-NLS-1$
      grpClockTrim.setLayout(new GridLayout(2, false));
      grpClockTrim.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));
      toolkit.adapt(grpClockTrim);
      toolkit.paintBordersFor(grpClockTrim);

      btnTrimTargetClock = new Button(grpClockTrim, SWT.CHECK);
      btnTrimTargetClock.setText("Enable Clock Trim");
      btnTrimTargetClock.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            enableTrim(((Button)e.getSource()).getSelection());
         }
      });
      new Label(grpClockTrim, SWT.NONE);

      lblTrimFrequency = new Label(grpClockTrim, SWT.NONE);
      toolkit.adapt(lblTrimFrequency, true, true);
      lblTrimFrequency.setText("Trim Frequency"); //$NON-NLS-1$
      new Label(grpClockTrim, SWT.NONE);

      btnTrimTargetClock.setToolTipText("Enable trimming of target internal clock source."); //$NON-NLS-1$
      toolkit.adapt(btnTrimTargetClock, true, true);

      txtTrimFrequency = new Text(grpClockTrim, SWT.BORDER);
      txtTrimFrequencyAdapter = new DoubleTextAdapter(txtTrimFrequency);
      txtTrimFrequency.setTextLimit(7);
      txtTrimFrequencyAdapter.setDoubleValue(0.0);
      txtTrimFrequency.setToolTipText(""); //$NON-NLS-1$
//      txtTrimFrequency.setBackground(SWTResourceManager.getColor(255, 255, 255));
      GridData gd_txtTrimFrequency = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      gd_txtTrimFrequency.widthHint = 65;
      gd_txtTrimFrequency.minimumWidth = 65;
      txtTrimFrequency.setLayoutData(gd_txtTrimFrequency);
      toolkit.adapt(txtTrimFrequency, true, true);

      lblKhz = new Label(grpClockTrim, SWT.NONE);
      lblKhz.setToolTipText(
            "The frequency to trim the internal clock source to.\r\n" +
            "Note this is NOT the bus clock frequency.\r\n" +
            "Zero indicates use chip default value");
      toolkit.adapt(lblKhz, true, true);
      lblKhz.setText("kHz"); //$NON-NLS-1$

      lblNvtrimAddress = new Label(grpClockTrim, SWT.NONE);
      toolkit.adapt(lblNvtrimAddress, true, true);
      lblNvtrimAddress.setText("NVTRIM Address"); //$NON-NLS-1$
      new Label(grpClockTrim, SWT.NONE);

      txtNVTRIMAddress = new Text(grpClockTrim, SWT.BORDER);
      txtNVTRIMAddressAdapter = new HexTextAdapter("NVTRIM Address", txtNVTRIMAddress, 0);
      txtNVTRIMAddressAdapter.setRange(0, 0xFFFF);
      GridData gd_txtNVTRIMAddress = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      gd_txtNVTRIMAddress.widthHint = 65;
      gd_txtNVTRIMAddress.minimumWidth = 65;
      txtNVTRIMAddress.setLayoutData(gd_txtNVTRIMAddress);
      toolkit.adapt(txtNVTRIMAddress, true, true);

      lblHex = new Label(grpClockTrim, SWT.NONE);
      lblHex.setToolTipText("Address of non-volatile memory location to write the trim value to.\r\n" +
            "Zero indicates use chip default value"); //$NON-NLS-1$
      toolkit.adapt(lblHex, true, true);
      lblHex.setText("hex"); //$NON-NLS-1$
   }

   protected void createBdmClockGroup(Composite comp) {
      
      Group grpBdmClockSelect = new Group(this, SWT.NONE);
      grpBdmClockSelect.setText("BDM Clock Select"); //$NON-NLS-1$
      RowLayout rl_grpBdmClockSelect = new RowLayout(SWT.HORIZONTAL);
      rl_grpBdmClockSelect.marginHeight = 3;
      rl_grpBdmClockSelect.marginBottom = 0;
      grpBdmClockSelect.setLayout(rl_grpBdmClockSelect);
      grpBdmClockSelect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      toolkit.adapt(grpBdmClockSelect);
      toolkit.paintBordersFor(grpBdmClockSelect);

      btnBDMClockDefault = new Button(grpBdmClockSelect, SWT.RADIO);
      btnBDMClockDefault.setToolTipText("Use default BDM clock."); //$NON-NLS-1$
      toolkit.adapt(btnBDMClockDefault, true, true);
      btnBDMClockDefault.setText("Default"); //$NON-NLS-1$

      btnBDMClockBus = new Button(grpBdmClockSelect, SWT.RADIO);
      btnBDMClockBus.setToolTipText("Force use of target Bus Clock as BDM clock."); //$NON-NLS-1$
      toolkit.adapt(btnBDMClockBus, true, true);
      btnBDMClockBus.setText("Bus Clock/2"); //$NON-NLS-1$

      btnBDMClockAlt = new Button(grpBdmClockSelect, SWT.RADIO);
      btnBDMClockAlt.setToolTipText("Force use of alternative  BDM clock (derivative specific source)."); //$NON-NLS-1$
      toolkit.adapt(btnBDMClockAlt, true, true);
      btnBDMClockAlt.setText("Alt"); //$NON-NLS-1$
   }
   
   protected void createEraseGroup(Composite comp) {
      
      Group grpEraseOptions = new Group(this, SWT.NONE);
      grpEraseOptions.setText("Erase Options");
      grpEraseOptions.setLayout(new RowLayout(SWT.HORIZONTAL));
      grpEraseOptions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      toolkit.adapt(grpEraseOptions);
      toolkit.paintBordersFor(grpEraseOptions);

      comboEraseMethod = new Combo(grpEraseOptions, SWT.READ_ONLY);
      comboEraseMethod.setToolTipText("Erase method used before programming");
      toolkit.adapt(comboEraseMethod, true, true);
      for (EraseMethod em : permittedEraseMethods) {
         comboEraseMethod.add(em.toString());
      }
      comboEraseMethod.setText(EraseMethod.ERASE_TARGETDEFAULT.getLegibleName());
   }
   
   protected void createSecurityGroup(Composite comp) {
	      
      Group grpSecurityOptions = new Group(this, SWT.NONE);
      grpSecurityOptions.setText("Security Options");
      grpSecurityOptions.setLayout(new RowLayout(SWT.HORIZONTAL));
      grpSecurityOptions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      toolkit.adapt(grpSecurityOptions);
      toolkit.paintBordersFor(grpSecurityOptions);

      comboSecurityOption = new Combo(grpSecurityOptions, SWT.READ_ONLY);
      comboSecurityOption.setToolTipText("Security options applied to the target when programming");
      toolkit.adapt(comboSecurityOption, true, true);
      
      // Must be added in ordinal order
	  comboSecurityOption.add(SecurityOptions.SECURITY_IMAGE.toString());
	  comboSecurityOption.add(SecurityOptions.SECURITY_UNSECURED.toString());
	  comboSecurityOption.add(SecurityOptions.SECURITY_SMART.toString());
      comboSecurityOption.select(SecurityOptions.SECURITY_SMART.ordinal());
   }
	   
   protected void createDebugGroup() {
      
      Group grpDebuggingOptions = new Group(this, SWT.NONE);
      GridData gd = new GridData();
      gd.horizontalAlignment = GridData.FILL;
      grpDebuggingOptions.setLayoutData(gd);
      grpDebuggingOptions.setText("Debugging Options");
      grpDebuggingOptions.setLayout(new FillLayout(SWT.VERTICAL));
//      grpDebuggingOptions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));
      toolkit.adapt(grpDebuggingOptions);
      toolkit.paintBordersFor(grpDebuggingOptions);
      btnUseDebugBuild = new Button(grpDebuggingOptions, SWT.CHECK);
      btnUseDebugBuild.setToolTipText("Used for debugging USBDM drivers - don't enable");
      toolkit.adapt(btnUseDebugBuild, true, true);
      btnUseDebugBuild.setText("Use debug build");
      btnUseDebugBuild.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            lblTargetId.setText(GetGdiLibrary());
         }
      });
      lblTargetId = new Label(grpDebuggingOptions, SWT.NONE);
      toolkit.adapt(lblTargetId, true, true);
      lblTargetId.setText("Target ID");
   }  

   protected void createContents(Composite comp) {
//      System.err.println("UsbdmConnectionPanel::createContents()");

//      toolkit.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
      toolkit.setBackground(comp.getBackground());
      toolkit.adapt(comp);
      toolkit.paintBordersFor(comp);
      GridLayout gridLayout = new GridLayout(3, false);
      setLayout(gridLayout);

      createPreferredBdmGroup(comp);
      createButtonGroup(comp);
      creatTargetVddGroup(comp);   
   }
   
   /**
    * Transfer internal state to GUI 
    * 
    */
   protected void transferToWindow() {
      System.err.println("UsbdmConnectionPanel.transferToWindow()");
      
      populateBdmChoices(preferredBdm, false);
      btnUseDebugBuild.setSelection(useDebugBuild);
      
      btnCycleTargetVddOnConnect.setSelection(bdmOptions.cycleVddOnConnect != 0);
      btnCycleTargetVddOnReset.setSelection(bdmOptions.cycleVddOnReset != 0);
      btnLeaveTargetPowered.setSelection(bdmOptions.leaveTargetPowered != 0);

      lblTargetId.setText(GetGdiLibrary());

      enableTrim(bdmOptions.doClockTrim);
      setTargetVdd(bdmOptions.targetVdd);

      comboSecurityOption.select(securityOption.ordinal());
      if (comboEraseMethod != null) {
         comboEraseMethod.setText(eraseMethod.toString());
      }
   }

   /**
    * Transfer GUI to internal state 
    * 
    */
   protected void transferFromWindow() {
      preferredBdm = comboSelectBDM.getText();
      useDebugBuild = btnUseDebugBuild.getSelection();
      
      bdmOptions.targetVdd = UsbdmCommon.BDM_TARGET_VDD_OFF;
      if (btnTargetVdd3V3.getSelection()) {
         bdmOptions.targetVdd = UsbdmCommon.BDM_TARGET_VDD_3V3;
      }
      else if (btnTargetVdd5V.getSelection()) {
         bdmOptions.targetVdd = UsbdmCommon.BDM_TARGET_VDD_5V;
      }
      if (bdmOptions.targetVdd == UsbdmCommon.BDM_TARGET_VDD_OFF) {
         bdmOptions.cycleVddOnConnect  = 0;
         bdmOptions.cycleVddOnReset    = 0;
         bdmOptions.leaveTargetPowered = 0;
      }
      else {
         bdmOptions.cycleVddOnConnect  = btnCycleTargetVddOnConnect.getSelection()?1:0;
         bdmOptions.cycleVddOnReset    = btnCycleTargetVddOnReset.getSelection()?1:0;  
         bdmOptions.leaveTargetPowered = btnLeaveTargetPowered.getSelection()?1:0;     
      }
      if (comboSecurityOption == null) {
         securityOption = SecurityOptions.SECURITY_UNSECURED;
      }
      else {
         securityOption = SecurityOptions.values()[comboSecurityOption.getSelectionIndex()];
      }
      if (comboEraseMethod == null) {
         eraseMethod = EraseMethod.ERASE_TARGETDEFAULT;
      }
      else {
         eraseMethod = permittedEraseMethods.get(comboEraseMethod.getSelectionIndex());
      }
   }
   
   /**
    * Set internal state to default values 
    * 
    */
   protected void restoreDefaultSettings() {
//      System.err.println("UsbdmConnectionPanel.restoreDefaultSettings()");
      bdmOptions      = new BdmOptions();
      useDebugBuild   = false;
      preferredBdm    = "Any connected BDM";   
      securityOption  = SecurityOptions.SECURITY_SMART;
      eraseMethod     = EraseMethod.ERASE_TARGETDEFAULT;
   }

   /**
    * Load settings to internal state 
    * 
    * @param iLaunchConfiguration - Settings object to load from
    */
   public void loadSettings(ILaunchConfiguration iLaunchConfiguration) {
//      System.err.println("UsbdmConnectionPanel.loadSettings()");
      restoreDefaultSettings();
      launchConfiguration = iLaunchConfiguration;
      try {
         preferredBdm   = iLaunchConfiguration.getAttribute(attrib(UsbdmCommon.KeyDefaultBdmSerialNumber), preferredBdm);

         useDebugBuild                      = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyUseDebugBuild),           useDebugBuild);

         bdmOptions.targetVdd               = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeySetTargetVdd),            bdmOptions.targetVdd);
         bdmOptions.cycleVddOnConnect       = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyCycleTargetVddonConnect), bdmOptions.cycleVddOnConnect);
         bdmOptions.cycleVddOnReset         = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyCycleTargetVddOnReset),   bdmOptions.cycleVddOnReset);
         bdmOptions.leaveTargetPowered      = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyLeaveTargetPowered),      bdmOptions.leaveTargetPowered);
         
         bdmOptions.powerOffDuration        = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyPowerOffDuration),        bdmOptions.powerOffDuration); 
         bdmOptions.powerOnRecoveryInterval = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyPowerOnRecoveryInterval), bdmOptions.powerOnRecoveryInterval); 
         bdmOptions.resetDuration           = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyResetDuration),           bdmOptions.resetDuration); 
         bdmOptions.resetReleaseInterval    = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyResetReleaseInterval),    bdmOptions.resetReleaseInterval); 
         bdmOptions.resetRecoveryInterval   = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyResetRecoveryInterval),   bdmOptions.resetRecoveryInterval);
         
         int securityOptionMask = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeySecurityOption), SecurityOptions.SECURITY_UNSECURED.getMask());
         securityOption = SecurityOptions.valueOf(securityOptionMask);

         int eraseMethod = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyEraseMethod), EraseMethod.ERASE_TARGETDEFAULT.ordinal());
         EraseMethod em = EraseMethod.values()[eraseMethod];
         if (!permittedEraseMethods.contains(em)) {
            em = EraseMethod.ERASE_TARGETDEFAULT;
         }
         this.eraseMethod = em;
      } catch (CoreException e) {
         e.printStackTrace();
      }
   }

   /**
    * Save settings from internal state/GUI 
    * 
    * @param iLaunchConfigurationWorkingCopy - Settings object to save to
    */
   public void saveSettings(ILaunchConfigurationWorkingCopy iLaunchConfigurationWorkingCopy) throws PrefException {
//      System.err.println("UsbdmConnectionPanel.saveSettings()");
      
      transferFromWindow();

      iLaunchConfigurationWorkingCopy.setAttribute(attrib(UsbdmCommon.KeyDefaultBdmSerialNumber), preferredBdm);
      
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyUseDebugBuild),           useDebugBuild);
      
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeySetTargetVdd),            bdmOptions.targetVdd);
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyCycleTargetVddonConnect), bdmOptions.cycleVddOnConnect  );
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyCycleTargetVddOnReset),   bdmOptions.cycleVddOnReset    );
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyLeaveTargetPowered),      bdmOptions.leaveTargetPowered );
         
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyPowerOffDuration),        bdmOptions.powerOffDuration);
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyPowerOnRecoveryInterval), bdmOptions.powerOnRecoveryInterval);
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyResetDuration),           bdmOptions.resetDuration);
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyResetReleaseInterval),    bdmOptions.resetReleaseInterval);
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyResetRecoveryInterval),   bdmOptions.resetRecoveryInterval);
      
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeySecurityOption),          securityOption.getMask());
      setAttribute(iLaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyEraseMethod),             eraseMethod.getOptionName());
//      try {
//         Map<String, String> allAttributes = iLaunchConfigurationWorkingCopy.getAttributes();
//         for (Map.Entry<String, String> entry : allAttributes.entrySet()) {
//             System.out.println(entry.getKey() + "/" + entry.getValue());
//         }
//      } catch (CoreException e) {
//         e.printStackTrace();
//      }
   }

   protected void addSettingsChangedListeners() {
      if (fListener != null) {
         comboSelectBDM.addModifyListener(fListener.getModifyListener());
         btnDefault.addSelectionListener(fListener.getSelectionListener());

         btnTargetVddOff.addSelectionListener(fListener.getSelectionListener());
         btnTargetVdd3V3.addSelectionListener(fListener.getSelectionListener());
         btnTargetVdd5V.addSelectionListener(fListener.getSelectionListener());
         btnCycleTargetVddOnConnect.addSelectionListener(fListener.getSelectionListener());
         btnCycleTargetVddOnReset.addSelectionListener(fListener.getSelectionListener());
         btnLeaveTargetPowered.addSelectionListener(fListener.getSelectionListener());
         btnUseDebugBuild.addSelectionListener(fListener.getSelectionListener());
         btnAdvancedOptions.addSelectionListener(fListener.getSelectionListener());
      }
   }

   /**
    * Loads an string value from configuration
    * 
    * @param iLaunchConfiguration       Configuration object to load from
    * @param key                        Key to use for retrieval
    * @param defaultValue               Default value if not found
    * @return                           The value found or default    
    * @throws CoreException
    * @note   Always retrieves the attribute as a string as target interface can only access strings
    */
   protected String getAttribute(ILaunchConfiguration iLaunchConfiguration, String key, String defaultValue)
   throws CoreException {
      return iLaunchConfiguration.getAttribute(key,  String.format("%d", defaultValue));
   }

   /**
    * Loads an integer value from configuration
    * 
    * @param iLaunchConfiguration       Configuration object to load from
    * @param key                        Key to use for retrieval
    * @param defaultValue               Default value if not found
    * @return                           The value found or default    
    * @throws CoreException
    * @note   Always retrieves the attribute as a string as target interface can only access strings
    */
   protected int getAttribute(ILaunchConfiguration iLaunchConfiguration, String key, int defaultValue)
   throws CoreException {
      String sValue = iLaunchConfiguration.getAttribute(key,  String.format("%d", defaultValue));
//      System.err.println("getIntAttribute("+key+","+defaultValue+") => "+sValue);
      int value = defaultValue;
      try {
         int iValue = Integer.decode(sValue);
         value = iValue;
      } catch (NumberFormatException e) {
//         System.err.println("getIntAttribute("+key+","+defaultValue+") => Parse failed, using default: "+defaultValue);
      }
      return value;
   }

   /**
    * Loads a boolean value from configuration
    * 
    * @param iLaunchConfiguration       Configuration object to load from
    * @param key                        Key to use for retrieval
    * @param defaultValue               Default value if not found
    * @return                           The value found or default    
    * @throws CoreException
    * @note   Always retrieves the attribute as a string as target interface can only access strings
    */
   protected boolean getAttribute(ILaunchConfiguration iLaunchConfiguration, String key, boolean defaultValue)
   throws CoreException {
      return getAttribute(iLaunchConfiguration, key, defaultValue?1:0) != 0;
   }

   /**
    * Saves an String value to configuration
    * 
    * @param paramILaunchConfiguration  Configuration object to store to
    * @param key                        Key to use
    * @param value                      Value to write
    * @throws CoreException
    */
   protected void setAttribute(ILaunchConfigurationWorkingCopy paramILaunchConfiguration, String key, String value) {
      paramILaunchConfiguration.setAttribute(key, value);
//      System.err.println("setIntAttribute("+key+","+value+")");
   }

   /**
    * Saves an int value to configuration
    * 
    * @param paramILaunchConfiguration  Configuration object to store to
    * @param key                        Key to use
    * @param value                      Value to write
    * @throws CoreException
    * @note   Always stores the attribute as a string as target interface can only access strings
    */
   protected void setAttribute(ILaunchConfigurationWorkingCopy paramILaunchConfiguration, String key, int value) {
      paramILaunchConfiguration.setAttribute(key, String.format("%d",value));
//      System.err.println("setIntAttribute("+key+","+value+")");
   }

   /**
    * Saves a boolean value to configuration
    * 
    * @param paramILaunchConfiguration  Configuration object to store to
    * @param key                        Key to use
    * @param value                      Value to write
    * @throws CoreException
    * @note   Always stores the attribute as a string as target interface can only access strings
    */
   protected void setAttribute(ILaunchConfigurationWorkingCopy paramILaunchConfiguration, String key, boolean value) {
      setAttribute(paramILaunchConfiguration, key, value?1:0);
   }

   /**
    * @param  suffix the last (rightmost) element to use as an attribute key
    * 
    * @return complete attribute key
    */
   protected String attrib(String suffix) {
      return attributeKey+suffix;
   }

   protected void updateBdmDescription() {
      if (lblBDMInformation != null) {
         int index = comboSelectBDM.getSelectionIndex();
         if (index >= 0) {
            USBDMDeviceInfo bdmInterface = deviceList.get(index);
            String deviceDescription = bdmInterface.deviceDescription;
            lblBDMInformation.setText(deviceDescription);
//            btn(bdmInterface.isNullDevice());
            //XXXX
         }
      }
   }

   /**
    * @param previousDevice A String representing the serial number of a previously selected device.
    *                       This will be made the currently selected device (even if not connected).
    * @param scanForBdms    If true a scan is made for currently connected BDMs
    */
   protected void populateBdmChoices(String previousDevice, boolean scanForBdms) {
   //      System.err.println("populateBdmChoices("+previousDevice+") :");
      final USBDMDeviceInfo nullDevice = new USBDMDeviceInfo("Generic BDM", "Any connected USBDM", new BdmInformation());

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
      deviceList.add(0, nullDevice);

      String preferredDevice;
      if ((previousDevice != null) &&
            (!previousDevice.equals(nullDevice.deviceSerialNumber))) {
         // Add dummy device representing previously used device, make preferred
         deviceList.add(1, new USBDMDeviceInfo("Previously selected device", previousDevice, new BdmInformation()));
         preferredDevice = previousDevice;
      }
      else {
         // Use currently selected device (if any) as preferred
         preferredDevice = comboSelectBDM.getText();
      }
      //      System.err.println("populateBdmChoices(), preferred = "+preferredDevice);

      // Add devices to combo
      comboSelectBDM.removeAll();
      ListIterator<Usbdm.USBDMDeviceInfo> it = deviceList.listIterator();
      while (it.hasNext()) {
         USBDMDeviceInfo di = it.next();
         //         System.err.println( " BDM = " + di.toString());
         comboSelectBDM.add(di.deviceSerialNumber);
      }
      int index = comboSelectBDM.indexOf(preferredDevice);
      if (index >= 0)
         comboSelectBDM.select(index);
      else
         comboSelectBDM.select(0);
      updateBdmDescription();
   }

   // Interface: com.freescale.cdt.debug.cw.mcu.common.publicintf.ICWGdiInitializationData.GetConfigFile 
   public String GetConfigFile() { 
      return ""; 
   }

   /**
    * @see com.freescale.cdt.debug.cw.mcu.common.publicintf.ICWGdiInitializationData#GetGdiLibrary()
    * 
    * @return Name of GDI DLL e.g. "USBDM_GDI_HCS08.dll"
    */
   public String GetGdiLibrary() {
      useDebugBuild = btnUseDebugBuild.getSelection();
      if (useDebugBuild) {
//         System.err.println("UsbdmConnectionPanel::GetGdiLibrary() - Using debug DLL : " + gdiDebugDllName);
         return gdiDebugDllName;
      }
      else {
//         System.err.println("UsbdmConnectionPanel::GetGdiLibrary() - Using non-debug DLL : " + gdiDllName);
         return gdiDllName;
      }
   }                                             

   // Interface: com.freescale.cdt.debug.cw.mcu.common.publicintf.ICWGdiInitializationData.GetConfigFile 
   public String[] GetGdiOpenCmdLineArgs() {     
      return new String[0];                       
   }

   protected void enableTrim(boolean enabled) {
      if (btnTrimTargetClock != null) {
         btnTrimTargetClock.setSelection(enabled);
         lblNvtrimAddress.setEnabled(enabled);
         lblHex.setEnabled(enabled);
         lblKhz.setEnabled(enabled);
         lblTrimFrequency.setEnabled(enabled);
         txtNVTRIMAddress.setEnabled(enabled);
         txtTrimFrequency.setEnabled(enabled);
      }
   }

   protected void setTargetVdd(int targetVdd) {
      btnTargetVddOff.setSelection(targetVdd == UsbdmCommon.BDM_TARGET_VDD_OFF);
      btnTargetVdd3V3.setSelection(targetVdd == UsbdmCommon.BDM_TARGET_VDD_3V3);
      btnTargetVdd5V.setSelection( targetVdd == UsbdmCommon.BDM_TARGET_VDD_5V);
      btnCycleTargetVddOnConnect.setEnabled(targetVdd != UsbdmCommon.BDM_TARGET_VDD_OFF);
      btnCycleTargetVddOnReset.setEnabled(  targetVdd != UsbdmCommon.BDM_TARGET_VDD_OFF);
      btnLeaveTargetPowered.setEnabled(     targetVdd != UsbdmCommon.BDM_TARGET_VDD_OFF);
   }
}