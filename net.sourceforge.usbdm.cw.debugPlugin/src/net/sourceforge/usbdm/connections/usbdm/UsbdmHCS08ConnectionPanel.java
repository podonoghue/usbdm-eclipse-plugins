package net.sourceforge.usbdm.connections.usbdm;

import net.sourceforge.usbdm.jni.Usbdm.EraseMethod;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Composite;

import com.freescale.cdt.debug.cw.core.ui.publicintf.ISettingsListener;
import com.freescale.cdt.debug.cw.core.ui.settings.PrefException;

/**
 * @author podonoghue
 * 
 */
public class UsbdmHCS08ConnectionPanel extends UsbdmConnectionPanel {
   
   /**
    * Dummy constructor for WindowBuilder Pro.
    * 
    * @param parent
    * @param style
    */
   public UsbdmHCS08ConnectionPanel(Composite parent, int style) {
      super(parent, style);
      init();
   }

   /**
    * Actual constructor used by factory
    * 
    * @param listener
    * @param parent
    * @param swtstyle
    * @param protocolPlugin    Indicates the connection type e.g. "HCS08 GDI"
    * @param connectionTypeId  A long string indicating the architecture??
    */
   public UsbdmHCS08ConnectionPanel(ISettingsListener listener,
         Composite         parent,
         int               swtstyle, 
         String            protocolPlugin,  // "CF - GDI", "HC08 GDI" etc
         String            connectionTypeId) {

      super(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
//    System.err.println("UsbdmHCS08ConnectionPanel::UsbdmHCS08ConnectionPanel(protocolPlugin="+protocolPlugin+", connectionTypeId = "+connectionTypeId+")");
      init();
   }

   private void init() {
      deviceNameId            = UsbdmCommon.HCS08_DeviceNameAttributeKey;
      gdiDllName              = UsbdmCommon.HCS08_GdiWrapperLib;
      gdiDebugDllName         = UsbdmCommon.HCS08_DebugGdiWrapperLib;
	  
      permittedEraseMethods.add(EraseMethod.ERASE_TARGETDEFAULT);
      permittedEraseMethods.add(EraseMethod.ERASE_NONE);
      permittedEraseMethods.add(EraseMethod.ERASE_SELECTIVE);
      permittedEraseMethods.add(EraseMethod.ERASE_ALL);
      permittedEraseMethods.add(EraseMethod.ERASE_MASS);
   }

   public void create() {
//      System.err.println("UsbdmHCS08ConnectionPanel::create()");
      createContents(this);
      addSettingsChangedListeners();
   }
   
   /**
    * Transfer internal state to GUI
    * 
    */
   protected void transferToWindow() {
//      System.err.println("UsbdmHCS08ConnectionPanel.transferToWindow()");
      super.transferToWindow();

      btnAutomaticallyReconnect.setSelection(bdmOptions.autoReconnect != 0);
      btnDriveReset.setSelection(bdmOptions.useResetSignal != 0);

      int bdmClockSelect = bdmOptions.useAltBDMClock;
      btnBDMClockDefault.setSelection(bdmClockSelect == UsbdmCommon.BDM_CLK_DEFAULT);
      btnBDMClockBus.setSelection(    bdmClockSelect == UsbdmCommon.BDM_CLK_NORMAL);
      btnBDMClockAlt.setSelection(    bdmClockSelect == UsbdmCommon.BDM_CLK_ALT);

      txtTrimFrequencyAdapter.setDoubleValue(bdmOptions.clockTrimFrequency / 1000.0);
      txtNVTRIMAddressAdapter.setHexValue(bdmOptions.clockTrimNVAddress);
      enableTrim(bdmOptions.doClockTrim);
   }

   /**
    * Transfer GUI to internal state
    * 
    */
   protected void transferFromWindow() {
      super.transferFromWindow();
      bdmOptions.autoReconnect  = btnAutomaticallyReconnect.getSelection() ? 1:0;
      bdmOptions.useResetSignal = btnDriveReset.getSelection() ? 1:0;

      bdmOptions.useAltBDMClock = UsbdmCommon.BDM_CLK_DEFAULT;
      if (btnBDMClockBus.getSelection()) {
         bdmOptions.useAltBDMClock = UsbdmCommon.BDM_CLK_NORMAL;
      } else if (btnBDMClockAlt.getSelection()) {
         bdmOptions.useAltBDMClock = UsbdmCommon.BDM_CLK_ALT;
      }
      bdmOptions.doClockTrim = btnTrimTargetClock.getSelection();
      if (bdmOptions.doClockTrim) {
         // Trimming
         bdmOptions.clockTrimFrequency = (int) (txtTrimFrequencyAdapter.getDoubleValue() * 1000);
         bdmOptions.clockTrimNVAddress = txtNVTRIMAddressAdapter.getHexValue();
      } else {
         // Not Trimming
         bdmOptions.clockTrimFrequency = 0;
         bdmOptions.clockTrimNVAddress = 0;
      }
   }

   /**
    * Set internal state to default values & recurse
    * 
    */
   protected void restoreDefaultSettings() {
//      System.err.println("UsbdmHCS08ConnectionPanel.restoreDefaultSettings()");
      super.restoreDefaultSettings();
      restoreHCS08DefaultSettings();
   }

   /**
    * Set internal state to default values
    * 
    */
   protected void restoreHCS08DefaultSettings() {
//      System.err.println("UsbdmHCS08ConnectionPanel.restoreHCS08DefaultSettings(bool)");
      bdmOptions.autoReconnect      = defaultBdmOptions.autoReconnect;
      bdmOptions.useResetSignal     = defaultBdmOptions.useResetSignal;    
      bdmOptions.useAltBDMClock     = defaultBdmOptions.useAltBDMClock;    
      bdmOptions.clockTrimFrequency = defaultBdmOptions.clockTrimFrequency;
      bdmOptions.clockTrimNVAddress = defaultBdmOptions.clockTrimNVAddress;
   }

   /**
    * Load settings to internal state & GUI
    * 
    * @param iLaunchConfiguration
    *           - Settings object to load from
    */
   public void loadSettings(ILaunchConfiguration iLaunchConfiguration) {
//      System.err.println("UsbdmHCS08ConnectionPanel.loadSettings()");
      super.loadSettings(iLaunchConfiguration);
      loadHCS08Settings(iLaunchConfiguration);
      transferToWindow();
   }

   /**
    * Load settings to internal state
    * 
    * @param iLaunchConfiguration
    *           - Settings object to load from
    */
   public void loadHCS08Settings(ILaunchConfiguration iLaunchConfiguration) {
//      System.err.println("UsbdmHCS08ConnectionPanel.loadHCS08Settings()");
      restoreHCS08DefaultSettings();
      try {
         bdmOptions.autoReconnect       = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyAutomaticReconnect),  bdmOptions.autoReconnect);
         bdmOptions.useResetSignal      = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyUseResetSignal),      bdmOptions.useResetSignal);
         bdmOptions.useAltBDMClock      = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyUseAltBDMClock),      bdmOptions.useAltBDMClock);
         bdmOptions.doClockTrim         = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyTrimTargetClock),     bdmOptions.doClockTrim);
         bdmOptions.clockTrimFrequency  = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyClockTrimFrequency),  bdmOptions.clockTrimFrequency);
         bdmOptions.clockTrimNVAddress  = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyClockTrimNVAddress),  bdmOptions.clockTrimNVAddress);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Save settings from internal state/GUI
    * 
    * @param paramILaunchConfigurationWorkingCopy
    *           - Settings object to save to
    */
   @Override
   public void saveSettings( ILaunchConfigurationWorkingCopy paramILaunchConfigurationWorkingCopy)throws PrefException {
//      System.err.println("UsbdmHCS08ConnectionPanel.saveSettings()");

      super.saveSettings(paramILaunchConfigurationWorkingCopy);

      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyAutomaticReconnect),  bdmOptions.autoReconnect);
      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyUseResetSignal),      bdmOptions.useResetSignal);
      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyUseAltBDMClock),      bdmOptions.useAltBDMClock);
      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyTrimTargetClock),     bdmOptions.doClockTrim);
      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyClockTrimFrequency),  bdmOptions.clockTrimFrequency);
      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyClockTrimNVAddress),  bdmOptions.clockTrimNVAddress);
   }

   @Override
   protected void addSettingsChangedListeners() {
      super.addSettingsChangedListeners();
      if (fListener != null) {
         comboEraseMethod.addModifyListener(fListener.getModifyListener());
         comboSecurityOption.addModifyListener(fListener.getModifyListener());
         btnAutomaticallyReconnect.addSelectionListener(fListener.getSelectionListener());
         btnDriveReset.addSelectionListener(fListener.getSelectionListener());
         btnBDMClockAlt.addSelectionListener(fListener.getSelectionListener());
         btnBDMClockBus.addSelectionListener(fListener.getSelectionListener());
         btnBDMClockDefault.addSelectionListener(fListener.getSelectionListener());
         btnTrimTargetClock.addSelectionListener(fListener.getSelectionListener());
         txtTrimFrequency.addModifyListener(fListener.getModifyListener());
         txtNVTRIMAddress.addModifyListener(fListener.getModifyListener());
      }
   }

   /**
    * @param comp
    */
   @Override
   protected void createContents(Composite comp) {
//      System.err.println("createContents::create()");
      super.createContents(comp);

      createConnectionGroup(comp, NEEDS_RESET);
      createTrimGroup(comp);
      createBdmClockGroup(comp);
      createSecurityGroup(comp);
      createEraseGroup(comp);
      createDebugGroup();

      super.appendContents(this);
   }
}
