package net.sourceforge.usbdm.connections.usbdm;

//import net.sourceforge.usbdm.jni.JTAGInterfaceData;
//import net.sourceforge.usbdm.jni.JTAGInterfaceData.ClockSpeed;
//import net.sourceforge.usbdm.jni.Usbdm.EraseMethod;
//
//import org.eclipse.debug.core.ILaunchConfiguration;
//import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.widgets.Composite;
//import org.eclipse.swt.widgets.Label;

//import com.freescale.cdt.debug.cw.core.ui.publicintf.ISettingsListener;
//import com.freescale.cdt.debug.cw.core.ui.settings.PrefException;

/**
 * @author podonoghue
 * 
 */
public class UsbdmARMConnectionPanel extends UsbdmConnectionPanel {
   
   /**
    * Dummy constructor for WindowBuilder Pro.
    * 
    * @param parent
    * @param style
    */
   public UsbdmARMConnectionPanel(Composite parent, int style) {
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
   public UsbdmARMConnectionPanel(ISettingsListener listener, 
         Composite         parent,
         int               swtstyle, 
         String            protocolPlugin,  // "CF - GDI", "HC08 GDI" etc
         String            connectionTypeId) {

      super(listener, parent, swtstyle, protocolPlugin, connectionTypeId);
//      System.err.println("UsbdmARMConnectionPanel::UsbdmARMConnectionPanel(protocolPlugin="+protocolPlugin+", connectionTypeId = "+connectionTypeId+")");
      init();
   }
 
   private void init() {
      deviceNameId            = UsbdmCommon.ARM_DeviceNameAttributeKey;
      gdiDllName              = UsbdmCommon.ARM_GdiWrapperLib;
      gdiDebugDllName         = UsbdmCommon.ARM_DebugGdiWrapperLib;
	  
      permittedEraseMethods.add(EraseMethod.ERASE_TARGETDEFAULT);
      permittedEraseMethods.add(EraseMethod.ERASE_NONE);
      permittedEraseMethods.add(EraseMethod.ERASE_SELECTIVE);
      permittedEraseMethods.add(EraseMethod.ERASE_ALL);
      permittedEraseMethods.add(EraseMethod.ERASE_MASS);
   }

   public void create() {
//      System.err.println("UsbdmARMConnectionPanel::create()");
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
      
      ClockSpeed clockSpeed = JTAGInterfaceData.ClockSpeed.findSuitable(bdmOptions.connectionSpeed);
      comboConnectionSpeed.select(clockSpeed.ordinal());

   }

   /**
    * Transfer GUI to internal state
    * 
    */
   protected void transferFromWindow() {
      super.transferFromWindow();
      bdmOptions.autoReconnect  = btnAutomaticallyReconnect.getSelection() ? 1:0;
      int index = comboConnectionSpeed.getSelectionIndex();
      if (index < 0) {
         index = 0;
      }
      bdmOptions.connectionSpeed = JTAGInterfaceData.ClockSpeed.values()[index].getFrequency();
   }

   /**
    * Set internal state to default values & recurse
    * 
    */
   protected void restoreDefaultSettings() {
//      System.err.println("UsbdmARMConnectionPanel.restoreDefaultSettings()");
      super.restoreDefaultSettings();
      restoreARMDefaultSettings();
   }

   /**
    * Set internal state to default values
    * 
    */
   protected void restoreARMDefaultSettings() {
//      System.err.println("UsbdmARMConnectionPanel.restoreARMDefaultSettings(bool)");
      eraseMethod                   = EraseMethod.ERASE_TARGETDEFAULT;
      bdmOptions.autoReconnect      = defaultBdmOptions.autoReconnect;
      bdmOptions.connectionSpeed    = defaultBdmOptions.connectionSpeed;
      bdmOptions.connectionSpeed    = JTAGInterfaceData.ClockSpeed.findSuitable(bdmOptions.connectionSpeed).getFrequency();
   }

   /**
    * Load settings to internal state & GUI
    * 
    * @param iLaunchConfiguration
    *           - Settings object to load from
    */
   public void loadSettings(ILaunchConfiguration iLaunchConfiguration) {
//      System.err.println("UsbdmARMConnectionPanel.loadSettings()");
      super.loadSettings(iLaunchConfiguration);
      loadARMSettings(iLaunchConfiguration);
      transferToWindow();
   }

   /**
    * Load settings to internal state
    * 
    * @param iLaunchConfiguration
    *           - Settings object to load from
    */
   public void loadARMSettings(ILaunchConfiguration iLaunchConfiguration) {
//      System.err.println("UsbdmARMConnectionPanel.loadARMSettings()");
      restoreARMDefaultSettings();
      try {
         bdmOptions.autoReconnect       = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyAutomaticReconnect),  bdmOptions.autoReconnect);
         bdmOptions.connectionSpeed     = getAttribute(iLaunchConfiguration, attrib(UsbdmCommon.KeyConnectionSpeed),    bdmOptions.connectionSpeed);
         // Round off to available value
         bdmOptions.connectionSpeed     = JTAGInterfaceData.ClockSpeed.findSuitable(bdmOptions.connectionSpeed).getFrequency();
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
//      System.err.println("UsbdmARMConnectionPanel.saveSettings()");

      super.saveSettings(paramILaunchConfigurationWorkingCopy);

      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyAutomaticReconnect),  bdmOptions.autoReconnect);
      setAttribute(paramILaunchConfigurationWorkingCopy, attrib(UsbdmCommon.KeyConnectionSpeed),     bdmOptions.connectionSpeed);
   }

   @Override
   protected void addSettingsChangedListeners() {
      super.addSettingsChangedListeners();
      if (fListener != null) {
         comboEraseMethod.addModifyListener(fListener.getModifyListener());
         comboSecurityOption.addModifyListener(fListener.getModifyListener());
         btnAutomaticallyReconnect.addSelectionListener(fListener.getSelectionListener());
         comboConnectionSpeed.addModifyListener(fListener.getModifyListener());
      }
   }

   /**
    * @param comp
    */
   @Override
   protected void createContents(Composite comp) {
//      System.err.println("createContents::create()");
      super.createContents(comp);

      createConnectionGroup(comp, NEEDS_SPEED);
      new Label(this, SWT.NONE);
      new Label(this, SWT.NONE);
      createSecurityGroup(comp);
      createEraseGroup(comp);
      createDebugGroup();

      super.appendContents(this);
   }
}
