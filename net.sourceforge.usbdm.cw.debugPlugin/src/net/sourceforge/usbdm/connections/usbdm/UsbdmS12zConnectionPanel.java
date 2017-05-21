package net.sourceforge.usbdm.connections.usbdm;

import net.sourceforge.usbdm.jni.Usbdm.EraseMethod;

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
public class UsbdmS12zConnectionPanel extends UsbdmConnectionPanel {
   
   /**
    * Dummy constructor for WindowBuilder Pro.
    * 
    * @param parent
    * @param style
    */
   public UsbdmS12zConnectionPanel(Composite parent, int style) {
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
   public UsbdmS12zConnectionPanel(ISettingsListener listener, 
         Composite         parent,
         int               swtstyle, 
         String            protocolPlugin,  // "CF - GDI", "HC08 GDI" etc
         String            connectionTypeId) {

      super(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
//    System.err.println("UsbdmHCS12ConnectionPanel::UsbdmHCS12ConnectionPanel(protocolPlugin="+protocolPlugin+", connectionTypeId = "+connectionTypeId+")");
      init();
   }

   private void init() {
      deviceNameId            = UsbdmCommon.S12Z_DeviceNameAttributeKey;
      gdiDllName              = UsbdmCommon.S12Z_GdiWrapperLib;
      gdiDebugDllName         = UsbdmCommon.S12Z_DebugGdiWrapperLib;
      permittedEraseMethods.add(EraseMethod.ERASE_TARGETDEFAULT);
      permittedEraseMethods.add(EraseMethod.ERASE_NONE);
      permittedEraseMethods.add(EraseMethod.ERASE_SELECTIVE);
      permittedEraseMethods.add(EraseMethod.ERASE_ALL);
      permittedEraseMethods.add(EraseMethod.ERASE_MASS);
      
   }

   public void create() {
//      System.err.println("UsbdmHCS12ConnectionPanel::create()");
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
//      System.err.println("UsbdmHCS12ConnectionPanel.loadHCS12Settings()");
      restoreHCS12DefaultSettings();
      try {
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
//      System.err.println("UsbdmS12zConnectionPanel.saveSettings()");

      super.saveSettings(paramILaunchConfigurationWorkingCopy);

      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyAutomaticReconnect),  bdmOptions.autoReconnect);
      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyUseAltBDMClock),      bdmOptions.useAltBDMClock);
   }

   @Override
   protected void addSettingsChangedListeners() {
      super.addSettingsChangedListeners();
      if (fListener != null) {
         comboEraseMethod.addModifyListener(fListener.getModifyListener());
         comboSecurityOption.addModifyListener(fListener.getModifyListener());
         btnAutomaticallyReconnect.addSelectionListener(fListener.getSelectionListener());
         btnBDMClockAlt.addSelectionListener(fListener.getSelectionListener());
         btnBDMClockBus.addSelectionListener(fListener.getSelectionListener());
         btnBDMClockDefault.addSelectionListener(fListener.getSelectionListener());
      }
   }

   /**
    * @param comp
    */
   @Override
   protected void createContents(Composite comp) {
//      System.err.println("createContents::create()");
      super.createContents(comp);

      createConnectionGroup(comp, 0);
      new Label(this, SWT.NONE);
      createBdmClockGroup(comp);
      new Label(this, SWT.NONE);
      createSecurityGroup(comp);
      createEraseGroup(comp);
      createDebugGroup();

      super.appendContents(this);
   }
}
