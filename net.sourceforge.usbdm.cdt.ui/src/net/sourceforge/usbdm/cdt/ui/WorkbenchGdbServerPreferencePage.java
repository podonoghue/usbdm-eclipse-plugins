package net.sourceforge.usbdm.cdt.ui;

import java.util.ArrayList;
import java.util.ListIterator;

import net.sourceforge.usbdm.cdt.ui.handlers.GdbServerParameters;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.jni.JTAGInterfaceData;
import net.sourceforge.usbdm.jni.JTAGInterfaceData.ClockSpeed;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.Usbdm.AutoConnect;
import net.sourceforge.usbdm.jni.Usbdm.BdmInformation;
import net.sourceforge.usbdm.jni.Usbdm.EraseMethod;
import net.sourceforge.usbdm.jni.Usbdm.TargetVddSelect;
import net.sourceforge.usbdm.jni.Usbdm.USBDMDeviceInfo;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public abstract class WorkbenchGdbServerPreferencePage extends PreferencePage {

   private static final int   NEEDS_SPEED = 1<<0;
   private static final int   NEEDS_RESET = 1<<1;
   private static final int   NEEDS_PST   = 1<<2;

   private GdbServerParameters   gdbServerParameters = null;

   protected ArrayList<USBDMDeviceInfo> deviceList;
   private Label              lblTrimFrequency;

   private Combo              comboSelectBDM;
   private Button             btnRefreshBDMs;
   private Label              lblBDMInformation;
   private Text               txtTrimFrequency;
   private Label              lblKhz;
   private Label              lblNvtrimAddress;
   private Text               txtNVTRIMAddress;
   private Label              lblHex;
   private Button             btnTrimTargetClock;
   private Combo              comboEraseMethod;
   private Composite          targetVddComposite;
   private Button[]           targetVddButtons;
   private Button             btnAutomaticallyReconnect;
   private Button             btnDriveReset;
   private Button             btnUsePstSignals;
   private Combo              comboConnectionSpeed;
   private InterfaceType      interfaceType;
   private Text               gdbPortText;
   private NumberTextAdapter  gdbPortAdapter;

   private EraseMethod[]      eraseMethods;
   private Button btnUseDebug;

   static public class WorkbenchPreferenceArmPage extends WorkbenchGdbServerPreferencePage 
   implements IWorkbenchPreferencePage {
      public WorkbenchPreferenceArmPage() {
         super("GDB Server settings for ARM");
         setGdbServerParameters(new GdbServerParameters.ArmGdbServerParameters());
      }

      @Override
      protected Control createContents(Composite parent) {

         Composite composite = new Composite(parent, SWT.NONE);
         composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
         composite.setLayout(new GridLayout(4, false));

         //-----
         createPreferredBdmGroup(composite);
         new Label(composite, SWT.FILL).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
         //-----
         createGdbPortGroup(composite);
         createConnectionGroup(composite, NEEDS_SPEED);
         createEraseGroup(composite);
         new Label(composite, SWT.NONE);
         //-----
         creatTargetVddGroup(composite);
         new Label(composite, SWT.NONE);
         new Label(composite, SWT.NONE);
         //-----

         loadSettings();

         //         setControl(composite); 
         return composite;
      }

      @Override
      public void init(IWorkbench workbench) {
         super.init(workbench);
         GdbServerParameters params = new GdbServerParameters.ArmGdbServerParameters();
         params.loadDefaultSettings();
         super.setGdbServerParameters(params);
      }
   }

   static public class WorkbenchPreferenceCfv1Page extends WorkbenchGdbServerPreferencePage
   implements IWorkbenchPreferencePage {
      public WorkbenchPreferenceCfv1Page() {
         super("GDB Server settings for Coldfire V1");
         setGdbServerParameters(new GdbServerParameters.Cfv1GdbServerParameters());
      }

      @Override
      protected Control createContents(Composite parent) {
         Composite composite = new Composite(parent, SWT.NONE);
         composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
         composite.setLayout(new GridLayout(4, false));

         //-----
         createPreferredBdmGroup(composite);
         new Label(composite, SWT.NONE);
         //-----
         createGdbPortGroup(composite);
         createConnectionGroup(composite, NEEDS_RESET);
         createEraseGroup(composite);
         new Label(composite, SWT.NONE);
         //-----
         creatTargetVddGroup(composite);
         createTrimGroup(composite);
         new Label(composite, SWT.NONE);
         //-----

         loadSettings();

         return composite;
      }

      @Override
      public void init(IWorkbench workbench) {
         super.init(workbench);
         GdbServerParameters params = new GdbServerParameters.Cfv1GdbServerParameters();
         params.loadDefaultSettings();
         super.setGdbServerParameters(params);
      }
   }

   static public class WorkbenchPreferenceCfvxPage extends WorkbenchGdbServerPreferencePage 
   implements IWorkbenchPreferencePage {
      public WorkbenchPreferenceCfvxPage() {
         super("GDB Server settings for Coldfire V2,3 & 4");
         setGdbServerParameters(new GdbServerParameters.CfvxGdbServerParameters());
      }

      @Override
      public Control createContents(Composite parent) {

         Composite composite = new Composite(parent, SWT.NONE);
         composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
         composite.setLayout(new GridLayout(4, false));

         createPreferredBdmGroup(composite);
         new Label(composite, SWT.NONE);
         //-----
         createGdbPortGroup(composite);
         createConnectionGroup(composite, NEEDS_SPEED|NEEDS_PST);
         createEraseGroup(composite);
         new Label(composite, SWT.NONE);
         //-----
         creatTargetVddGroup(composite);
         new Label(composite, SWT.NONE);
         new Label(composite, SWT.NONE);
         //-----

         loadSettings();

         return composite;      
      }

      @Override
      public void init(IWorkbench workbench) {
         super.init(workbench);
         GdbServerParameters params = new GdbServerParameters.CfvxGdbServerParameters();
         params.loadDefaultSettings();
         super.setGdbServerParameters(params);
      }
   }

   protected abstract Control createContents(Composite parent);

   public WorkbenchGdbServerPreferencePage(String title) {
      super(title);
      setDescription("Select default settings for GDB Socket Server");
      setInterfaceType(interfaceType);
   }

   //==========================================================

   /** Create BDM Selection Group
    * 
    * @param parent parent of group
    */
   protected void createPreferredBdmGroup(Composite parent) {
      //    System.err.println("UsbdmConnectionPanel::createPreferredBdmGroup()");

      Group grpSelectBdm = new Group(parent, SWT.NONE);
      grpSelectBdm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
      grpSelectBdm.setText("Preferred BDM");
      grpSelectBdm.setLayout(new GridLayout(2, false));

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

      populateBdmChoices(null, false);

      btnRefreshBDMs = new Button(grpSelectBdm, SWT.NONE);
      btnRefreshBDMs.setToolTipText("Check for connected BDMs");
      btnRefreshBDMs.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            populateBdmChoices(null, true);
         }
      });
      btnRefreshBDMs.setText("Refresh");

      lblBDMInformation = new Label(grpSelectBdm, SWT.NONE);
      lblBDMInformation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
      lblBDMInformation.setToolTipText("Description of selected BDM");
      lblBDMInformation.setText("BDM Information");
      new Label(grpSelectBdm, SWT.NONE);
   }

   /** Populates the BDM choice control
    * 
    * @param previousDevice A String representing the serial number of a previously selected device.
    *                       This will be made the currently selected device (even if not connected).
    * @param scanForBdms    If true a scan is made for currently connected BDMs
    */
   /**
    * @param previousDevice
    * @param scanForBdms
    */
   protected void populateBdmChoices(String previousDevice, boolean scanForBdms) {
//      System.err.println("populateBdmChoices(\'"+previousDevice+"\', "+scanForBdms+")\n");

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
//            System.err.println("populateBdmChoices() Adding preferredDevice = \'"+comboSelectBDM.getText()+"\'\n");
         }
      }
      updateBdmDescription();
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

   /** Create GDB Port selection Group
    * 
    * @param parent parent of group
    */
   protected void createGdbPortGroup(Composite parent) {
      //    System.err.println("UsbdmConnectionPanel::createPreferredBdmGroup()");

      Group grpGdbControl = new Group(parent, SWT.NONE);
      grpGdbControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      grpGdbControl.setText("GDB Server Control");
      GridLayout gridLayout = new GridLayout(2,false);
      grpGdbControl.setLayout(gridLayout);

      Label lbl = new Label(grpGdbControl, SWT.NONE);
      lbl.setText("Port ");
      gdbPortText = new Text(grpGdbControl, SWT.BORDER);
      gdbPortAdapter = new NumberTextAdapter("GDB Server port", gdbPortText,  1234);
      gdbPortText.setLayoutData(new GridData(60, 20));
      btnUseDebug = new Button(grpGdbControl, SWT.CHECK);
      btnUseDebug.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
      btnUseDebug.setToolTipText("Use debug version of server.");
      btnUseDebug.setText("Debug");
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

      targetVddComposite = new Composite(grpTargetVddSupply, SWT.NONE);
      targetVddComposite.setLayout(new FillLayout(SWT.HORIZONTAL));

      targetVddButtons = new Button[TargetVddSelect.values().length];
      for(final TargetVddSelect targetVdd :TargetVddSelect.values()) {
         if (targetVdd.ordinal() > TargetVddSelect.BDM_TARGET_VDD_5V.ordinal()) {
            continue;
         }
         Button button = new Button(targetVddComposite, SWT.RADIO);
         targetVddButtons[targetVdd.ordinal()] = button;
         button.setData(targetVdd);
         button.setText(targetVdd.getName());
         button.setToolTipText(targetVdd.getHint());
         button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
               gdbServerParameters.setTargetVdd(targetVdd);
            }
         });
      }
   }

   protected void createTrimGroup(Composite comp) {

      Group grpClockTrim = new Group(comp, SWT.NONE);
      grpClockTrim.setText("Internal Clock Trim"); //$NON-NLS-1$
      grpClockTrim.setLayout(new GridLayout(2, false));
      grpClockTrim.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));

      btnTrimTargetClock = new Button(grpClockTrim, SWT.CHECK);
      btnTrimTargetClock.setText("Enable Clock Trim");
      btnTrimTargetClock.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            enableTrim(((Button)e.getSource()).getSelection());
         }
      });
      new Label(grpClockTrim, SWT.NONE);

      lblTrimFrequency = new Label(grpClockTrim, SWT.NONE);
      lblTrimFrequency.setText("Trim Frequency"); //$NON-NLS-1$
      new Label(grpClockTrim, SWT.NONE);

      btnTrimTargetClock.setToolTipText("Enable trimming of target internal clock source."); //$NON-NLS-1$

      txtTrimFrequency = new Text(grpClockTrim, SWT.BORDER);
      DoubleTextAdapter txtTrimFrequencyAdapter = new DoubleTextAdapter(txtTrimFrequency);
      txtTrimFrequency.setTextLimit(7);
      txtTrimFrequencyAdapter.setDoubleValue(0.0);
      txtTrimFrequency.setToolTipText(""); //$NON-NLS-1$
      GridData gd_txtTrimFrequency = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      gd_txtTrimFrequency.widthHint = 65;
      gd_txtTrimFrequency.minimumWidth = 65;
      txtTrimFrequency.setLayoutData(gd_txtTrimFrequency);

      lblKhz = new Label(grpClockTrim, SWT.NONE);
      lblKhz.setToolTipText(
            "The frequency to trim the internal clock source to.\r\n" +
                  "Note this is NOT the bus clock frequency.\r\n" +
            "Zero indicates use chip default value");
      lblKhz.setText("kHz"); //$NON-NLS-1$

      lblNvtrimAddress = new Label(grpClockTrim, SWT.NONE);
      lblNvtrimAddress.setText("NVTRIM Address"); //$NON-NLS-1$
      new Label(grpClockTrim, SWT.NONE);

      txtNVTRIMAddress = new Text(grpClockTrim, SWT.BORDER);
      HexTextAdapter txtNVTRIMAddressAdapter = new HexTextAdapter("NVTRIM Address", txtNVTRIMAddress, 0);
      txtNVTRIMAddressAdapter.setRange(0, 0xFFFF);
      GridData gd_txtNVTRIMAddress = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
      gd_txtNVTRIMAddress.widthHint = 65;
      gd_txtNVTRIMAddress.minimumWidth = 65;
      txtNVTRIMAddress.setLayoutData(gd_txtNVTRIMAddress);

      lblHex = new Label(grpClockTrim, SWT.NONE);
      lblHex.setToolTipText("Address of non-volatile memory location to write the trim value to.\r\n" +
            "Zero indicates use chip default value"); //$NON-NLS-1$
      lblHex.setText("hex"); //$NON-NLS-1$
   }

   protected void createEraseGroup(Composite comp) {

      Group grpEraseOptions = new Group(comp, SWT.NONE);
      grpEraseOptions.setText("Erase Options");
      grpEraseOptions.setLayout(new RowLayout(SWT.HORIZONTAL));
      grpEraseOptions.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

      eraseMethods = new EraseMethod[EraseMethod.values().length];
      comboEraseMethod = new Combo(grpEraseOptions, SWT.READ_ONLY);
      comboEraseMethod.setToolTipText("Erase method used before programming");
      int index = 0;
      for (EraseMethod method :EraseMethod.values()) {
         if (gdbServerParameters.isAllowedEraseMethod(method)) {
            comboEraseMethod.add(method.getName());
            eraseMethods[index++] = method;
         }
      }
      comboEraseMethod.select(comboEraseMethod.getItemCount()-1);
   }

   protected void createConnectionGroup(Composite comp, int flags) {

      Group grpConnectionControl = new Group(comp, SWT.NONE);
      grpConnectionControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

      grpConnectionControl.setLayout(new GridLayout(1, false));
      grpConnectionControl.setText("Connection Control"); //$NON-NLS-1$

      if ((flags&NEEDS_SPEED) != 0) {
         comboConnectionSpeed = new Combo(grpConnectionControl, SWT.READ_ONLY);
         comboConnectionSpeed.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
         comboConnectionSpeed.setItems(JTAGInterfaceData.getConnectionSpeeds());
         comboConnectionSpeed.setToolTipText("Connection speed to use for BDM communications");
         comboConnectionSpeed.select(4);
      }
      btnAutomaticallyReconnect = new Button(grpConnectionControl, SWT.CHECK);
      btnAutomaticallyReconnect.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
      btnAutomaticallyReconnect.setToolTipText("Automatically re-sync with the target whenever target state is polled."); //$NON-NLS-1$
      btnAutomaticallyReconnect.setText("Automatically re-connect"); //$NON-NLS-1$

      if ((flags&NEEDS_RESET) != 0) {
         btnDriveReset = new Button(grpConnectionControl, SWT.CHECK);
         btnDriveReset.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
         btnDriveReset.setToolTipText("Drive target reset pin when resetting the target."); //$NON-NLS-1$
         btnDriveReset.setText("Drive RESET pin"); //$NON-NLS-1$
         btnDriveReset.setBounds(0, 0, 140, 16);
      }
      if ((flags&NEEDS_PST) != 0) {
         btnUsePstSignals = new Button(grpConnectionControl, SWT.CHECK);
         btnUsePstSignals.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
         btnUsePstSignals.setToolTipText("Use PST signal to determine execution state of target.");
         btnUsePstSignals.setText("Use PST signals");
      }         
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
    */
   public boolean isPageComplete() {
      // Allow early completion without displaying page (after display contents are checked)
      return !isControlCreated();// || super.isPageComplete();
   }

   public GdbServerParameters getGdbServerParameters() {
      return gdbServerParameters;
   }

   public void setGdbServerParameters(GdbServerParameters gdbServerParameters) {
      this.gdbServerParameters = gdbServerParameters;
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

   protected void setTargetVdd(TargetVddSelect targetVdd) {
      if (targetVddButtons[targetVdd.ordinal()] != null) {
         targetVddButtons[targetVdd.ordinal()].setSelection(true);
      }
   }

   protected TargetVddSelect getTargetVdd() {
      for (Button button :targetVddButtons) {
         if ((button != null) && button.getSelection())  {
            return (TargetVddSelect)button.getData();
         }
      }
      return TargetVddSelect.BDM_TARGET_VDD_OFF;
   }

   public EraseMethod getEraseMethod() {
      if (comboEraseMethod.getSelectionIndex() < 0) {
         return gdbServerParameters.getPreferredEraseMethod();
      }
      else {
         return eraseMethods[comboEraseMethod.getSelectionIndex()];
      }
   }
   
   public InterfaceType getInterfaceType() {
      return interfaceType;
   }

   public void setInterfaceType(InterfaceType interfaceType) {
      this.interfaceType = interfaceType;
   }

   public void init(IWorkbench workbench) {
      noDefaultAndApplyButton();
   }

   /**
    *  Validates control & sets error message
    *  
    * @param message error message (null if none)
    * 
    * @return true => dialogue values are valid
    */
   public boolean validate() {
      String message = null;

      if (!isControlCreated()) {
         return true;
      }
      setErrorMessage(message);
      //      System.err.println("UsbdmConfigurationWizardPage.validate() => " + (message == null));
      return message == null;
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
    */
   @Override
   public void setVisible(boolean visible) {
      super.setVisible(visible);
      if (visible) {
         validate();
      }
   }

   protected void loadSettings() {
      System.err.println("loadSettings() loading settings");
      
      populateBdmChoices(gdbServerParameters.getBdmSerialNumber(), true);
      gdbPortAdapter.setDecimalValue(gdbServerParameters.getGdbPortNumber());
      btnUseDebug.setSelection(gdbServerParameters.isUseDebugServer());
      comboEraseMethod.setText(gdbServerParameters.getEraseMethod().getName());
      if (comboConnectionSpeed != null) {
         comboConnectionSpeed.setText(ClockSpeed.findSuitable(gdbServerParameters.getInterfaceFrequency()).toString());
      }
      if (btnTrimTargetClock != null) {
         enableTrim(gdbServerParameters.isTrimclock());
         txtTrimFrequency.setText(Integer.toString(gdbServerParameters.getClockTrimFrequency()));
         txtNVTRIMAddress.setText(Integer.toHexString(gdbServerParameters.getNvmClockTrimLocation()));
      }
      setTargetVdd(gdbServerParameters.getTargetVdd());
      btnAutomaticallyReconnect.setSelection(gdbServerParameters.getAutoReconnect() != AutoConnect.AUTOCONNECT_NEVER);
      if (btnUsePstSignals != null) {
         btnUsePstSignals.setSelection(gdbServerParameters.isUsePstSignals());
      }
      if (btnDriveReset != null) {
         btnDriveReset.setSelection(gdbServerParameters.isUseReset());
      }      
      validate();
   }

   public boolean saveSettings() {
      System.err.println("saveSetting() saving settings");
//      if (!validate()) {
//         return false;
//      }
//      if (!isControlCreated()) {
//         return true;
//      }
      gdbServerParameters.setBdmSerialNumber(comboSelectBDM.getText());
      gdbServerParameters.setGdbPortNumber(gdbPortAdapter.getDecimalValue());
      gdbServerParameters.enableUseDebugServer(btnUseDebug.getSelection());
      gdbServerParameters.setEraseMethod(getEraseMethod());
      if (comboConnectionSpeed != null) {
         gdbServerParameters.setInterfaceFrequency(ClockSpeed.lookup(comboConnectionSpeed.getText()).getFrequency());
      }
      if (btnTrimTargetClock != null) {
         gdbServerParameters.enableTrimClock(btnTrimTargetClock.getSelection());
         gdbServerParameters.setClockTrimFrequency(Integer.parseInt(txtTrimFrequency.getText()));
         gdbServerParameters.setNvmClockTrimLocation(Integer.parseInt(txtNVTRIMAddress.getText(),16));
      }
      gdbServerParameters.setTargetVdd(getTargetVdd());
      gdbServerParameters.setAutoReconnect(btnAutomaticallyReconnect.getSelection()?AutoConnect.AUTOCONNECT_ALWAYS:AutoConnect.AUTOCONNECT_NEVER);
      if (btnUsePstSignals != null) {
         gdbServerParameters.enableUsePstSignals(btnUsePstSignals.getSelection());
      }
      if (btnDriveReset != null) {
         gdbServerParameters.enableUseReset(btnDriveReset.getSelection());
      }
      
      return gdbServerParameters.saveSettings();
   }

   @Override
   public boolean performOk() {
      return super.performOk() && saveSettings();
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);
      final WorkbenchGdbServerPreferencePage topPage = new WorkbenchPreferenceArmPage();
      shell.setLayout(new FillLayout());
      topPage.init(null);
      topPage.createContents(shell);
      Button btn = new Button(shell, SWT.NONE);
      btn.setText("Save");
      btn.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            topPage.saveSettings();
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
