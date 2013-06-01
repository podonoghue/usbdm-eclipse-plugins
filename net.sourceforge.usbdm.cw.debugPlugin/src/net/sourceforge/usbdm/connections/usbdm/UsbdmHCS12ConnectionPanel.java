package net.sourceforge.usbdm.connections.usbdm;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.freescale.cdt.debug.cw.core.ui.publicintf.ISettingsListener;
import com.freescale.cdt.debug.cw.core.ui.settings.PrefException;

/**
 * @author podonoghue
 * 
 */
public class UsbdmHCS12ConnectionPanel extends UsbdmConnectionPanel {

   /**
    * Dummy constructor for WindowBuilder Pro.
    * 
    * @param parent
    * @param style
    */
   public UsbdmHCS12ConnectionPanel(Composite parent, int style) {
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
   public UsbdmHCS12ConnectionPanel(ISettingsListener listener, 
         Composite         parent,
         int               swtstyle, 
         String            protocolPlugin,  // "CF - GDI", "HC08 GDI" etc
         String            connectionTypeId) {

      super(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
//    System.err.println("UsbdmHCS12ConnectionPanel::UsbdmHCS12ConnectionPanel(protocolPlugin="+protocolPlugin+", connectionTypeId = "+connectionTypeId+")");
      init();
   }

   private void init() {
      deviceNameId       = UsbdmCommon.S12Z_DeviceNameAttributeKey;
      gdiDllName         = UsbdmCommon.S12Z_GdiWrapperLib;
      gdiDebugDllName    = UsbdmCommon.S12Z_DebugGdiWrapperLib;
      defaultEraseMethod = EraseMethod.E_ALL; 
      lastEraseMethod    = EraseMethod.E_SELECTIVE; 
   }

   public void create() {
      createContents(this);
      addSettingsChangedListeners();
   }
   
   /**
    * Transfer internal state to GUI
    * 
    */
   protected void transferToWindow() {
//      System.err.println("UsbdmHCS12ConnectionPanel.transferToWindow()");
      super.transferToWindow();

      btnAutomaticallyReconnect.setSelection(bdmOptions.autoReconnect != 0);

      int bdmClockSelect = bdmOptions.useAltBDMClock;
      btnBDMClockDefault.setSelection(bdmClockSelect == UsbdmCommon.BDM_CLK_DEFAULT);
      btnBDMClockBus.setSelection(    bdmClockSelect == UsbdmCommon.BDM_CLK_NORMAL);
      btnBDMClockAlt.setSelection(    bdmClockSelect == UsbdmCommon.BDM_CLK_ALT);

      comboEraseMethod.select(eraseMethod.ordinal());
   }

   /**
    * Transfer GUI to internal state
    * 
    */
   protected void transferFromWindow() {
      super.transferFromWindow();
      bdmOptions.autoReconnect  = btnAutomaticallyReconnect.getSelection() ? 1:0;

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
      eraseMethod = EraseMethod.values()[comboEraseMethod.getSelectionIndex()];
   }

   /**
    * Set internal state to default values & recurse
    * 
    */
   protected void restoreDefaultSettings() {
//      System.err.println("UsbdmHCS12ConnectionPanel.restoreDefaultSettings()");
      super.restoreDefaultSettings();
      restoreHCS12DefaultSettings();
   }

   /**
    * Set internal state to default values
    * 
    */
   protected void restoreHCS12DefaultSettings() {
//      System.err.println("UsbdmHCS12ConnectionPanel.restoreHCS12DefaultSettings(bool)");
      eraseMethod = defaultEraseMethod;
      bdmOptions.autoReconnect      = defaultBdmOptions.autoReconnect;
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
//      System.err.println("UsbdmHCS12ConnectionPanel.loadSettings()");
      super.loadSettings(iLaunchConfiguration);
      loadHCS12Settings(iLaunchConfiguration);
      transferToWindow();
   }

   /**
    * Load settings to internal state
    * 
    * @param iLaunchConfiguration
    *           - Settings object to load from
    */
   public void loadHCS12Settings(ILaunchConfiguration iLaunchConfiguration) {
//      System.err.println("UsbdmHCS08ConnectionPanel.loadHCS08Settings()");
      restoreHCS12DefaultSettings();
      try {
         int eraseMethod = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyEraseMethod), defaultEraseMethod.ordinal());
         if (eraseMethod > lastEraseMethod.ordinal()) {
            eraseMethod = defaultEraseMethod.ordinal();
         }
         this.eraseMethod = EraseMethod.values()[eraseMethod];

         bdmOptions.autoReconnect      = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyAutomaticReconnect),  bdmOptions.autoReconnect);
         bdmOptions.useAltBDMClock     = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyUseAltBDMClock),      bdmOptions.useAltBDMClock);
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
//      System.err.println("UsbdmHCS12ConnectionPanel.saveSettings()");

      super.saveSettings(paramILaunchConfigurationWorkingCopy);

      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyEraseMethod),         eraseMethod.ordinal());

      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyAutomaticReconnect),  bdmOptions.autoReconnect);
      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyUseAltBDMClock),      bdmOptions.useAltBDMClock);
   }

   @Override
   protected void addSettingsChangedListeners() {
      super.addSettingsChangedListeners();
      if (fListener != null) {
         comboEraseMethod.addModifyListener(fListener.getModifyListener());

         btnAutomaticallyReconnect.addSelectionListener(fListener.getSelectionListener());
         btnBDMClockAlt.addSelectionListener(fListener.getSelectionListener());
         btnBDMClockBus.addSelectionListener(fListener.getSelectionListener());
         btnBDMClockDefault.addSelectionListener(fListener.getSelectionListener());
      }
   }

   /**
    * @param comp
    */
   protected void createContents(Composite comp) {
      super.createContents(comp);

      createConnectionGroup(comp, 0);
      new Label(this, SWT.NONE);
      createBdmClockGroup(comp);
      new Label(this, SWT.NONE);
      new Label(this, SWT.NONE);
      createEraseGroup(comp);
      createDebugGroup();

      super.appendContents(this);
   }
}
