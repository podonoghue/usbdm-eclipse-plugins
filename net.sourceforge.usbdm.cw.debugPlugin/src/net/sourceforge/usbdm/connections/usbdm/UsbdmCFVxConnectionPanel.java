package net.sourceforge.usbdm.connections.usbdm;

import net.sourceforge.usbdm.connections.usbdm.JTAGInterfaceData.ClockSpeed;

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
public class UsbdmCFVxConnectionPanel extends UsbdmConnectionPanel {
   
   /**
    * Dummy constructor for WindowBuilder Pro.
    * 
    * @param parent
    * @param style
    */
   public UsbdmCFVxConnectionPanel(Composite parent, int style) {
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
   public UsbdmCFVxConnectionPanel(ISettingsListener listener, 
         Composite         parent,
         int               swtstyle, 
         String            protocolPlugin,  // "CF - GDI", "HC08 GDI" etc
         String            connectionTypeId) {

      super(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
//      System.err.println("UsbdmCFVxConnectionPanel::UsbdmCFVxConnectionPanel(protocolPlugin="+protocolPlugin+", connectionTypeId = "+connectionTypeId+")");
      init();
   }

   private void init() {
      deviceNameId    = UsbdmCommon.CFVx_DeviceNameAttributeKey;
      gdiDllName      = UsbdmCommon.CFVx_GdiWrapperLib;
      gdiDebugDllName = UsbdmCommon.CFVx_DebugGdiWrapperLib;
      // Override so as not to conflict with CFV1 panel which may be active at the same time
      attributeKey    = UsbdmCommon.BaseAttributeName+".cfvx.";
   }

   public void create() {
      System.err.println("UsbdmCFVxConnectionPanel::create");
      createContents(this);
      addSettingsChangedListeners();
   }
   
   /**
    * Transfer internal state to GUI
    * 
    */
   protected void transferToWindow() {
//      System.err.println("UsbdmCFV1ConnectionPanel.transferToWindow()");
      super.transferToWindow();

      btnAutomaticallyReconnect.setSelection(bdmOptions.autoReconnect != 0);
      btnUsePstSignals.setSelection(bdmOptions.usePSTSignals != 0);
      
      ClockSpeed clockSpeed = JTAGInterfaceData.ClockSpeed.findSuitable(bdmOptions.connectionSpeed);
//      System.err.println("UsbdmCFVxConnectionPanel.loadSettings() - ConnectionSpeed = "+clockSpeed.toFrequency());
      comboConnectionSpeed.select(clockSpeed.ordinal());
   }

   /**
    * Transfer GUI to internal state
    * 
    */
   protected void transferFromWindow() {
      super.transferFromWindow();
      bdmOptions.autoReconnect  = btnAutomaticallyReconnect.getSelection() ? 1:0;
      bdmOptions.usePSTSignals  = btnUsePstSignals.getSelection() ? 1:0;
      int index = comboConnectionSpeed.getSelectionIndex();
      if (index < 0) {
         index = 0;
      }
      bdmOptions.connectionSpeed = JTAGInterfaceData.ClockSpeed.values()[index].frequency;
   }

   /**
    * Set internal state to default values & recurse
    * 
    */
   protected void restoreDefaultSettings() {
//      System.err.println("UsbdmCFV1ConnectionPanel.restoreDefaultSettings()");
      super.restoreDefaultSettings();
      restoreCFVxDefaultSettings();
   }

   /**
    * Set internal state to default values
    * 
    */
   protected void restoreCFVxDefaultSettings() {
//      System.err.println("UsbdmCFV1ConnectionPanel.restoreCFV1DefaultSettings(bool)");
      bdmOptions.autoReconnect   = defaultBdmOptions.autoReconnect;
      bdmOptions.usePSTSignals   = defaultBdmOptions.usePSTSignals;
      bdmOptions.connectionSpeed = defaultBdmOptions.connectionSpeed;
      bdmOptions.connectionSpeed = JTAGInterfaceData.ClockSpeed.findSuitable(bdmOptions.connectionSpeed).frequency;
   }

   /**
    * Load settings to internal state & GUI
    * 
    * @param iLaunchConfiguration
    *           - Settings object to load from
    */
   public void loadSettings(ILaunchConfiguration iLaunchConfiguration) {
//      System.err.println("UsbdmCFVxConnectionPanel.loadSettings()");
      super.loadSettings(iLaunchConfiguration);
      loadCFVxSettings(iLaunchConfiguration);
      transferToWindow();
   }

   /**
    * Load settings to internal state
    * 
    * @param iLaunchConfiguration
    *           - Settings object to load from
    */
   public void loadCFVxSettings(ILaunchConfiguration iLaunchConfiguration) {
//      System.err.println("UsbdmCFVxConnectionPanel.loadCFVxSettings()");
      restoreCFVxDefaultSettings();
      try {
         bdmOptions.autoReconnect = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyAutomaticReconnect), bdmOptions.autoReconnect);
         bdmOptions.usePSTSignals   = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyUsePSTSignals),      bdmOptions.usePSTSignals);
         bdmOptions.connectionSpeed = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyConnectionSpeed),    bdmOptions.connectionSpeed);
         bdmOptions.connectionSpeed = JTAGInterfaceData.ClockSpeed.findSuitable(bdmOptions.connectionSpeed).frequency;
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
//      System.err.println("UsbdmCFVxConnectionPanel.saveSettings()");

      super.saveSettings(paramILaunchConfigurationWorkingCopy);

      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyAutomaticReconnect),  bdmOptions.autoReconnect);
      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyConnectionSpeed),     bdmOptions.connectionSpeed);
      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyUsePSTSignals),       bdmOptions.usePSTSignals);
   }

   @Override
   protected void addSettingsChangedListeners() {
      super.addSettingsChangedListeners();
      if (fListener != null) {
         btnAutomaticallyReconnect.addSelectionListener(fListener.getSelectionListener());
         btnUsePstSignals.addSelectionListener(fListener.getSelectionListener());
//         comboConnectionSpeed.addSelectionListener(fListener.getSelectionListener());
      }
   }

   /**
    * @param comp
    */
   @Override
   protected void createContents(Composite comp) {
      super.createContents(comp);

      createConnectionGroup(comp, NEEDS_SPEED|NEEDS_PST);
      new Label(this, SWT.NONE);
      new Label(this, SWT.NONE);
      new Label(this, SWT.NONE);
      new Label(this, SWT.NONE);
      createDebugGroup();

      super.appendContents(this);
   }
}
