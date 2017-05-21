package net.sourceforge.usbdm.connections.usbdm;

public class UsbdmCommon {

   public static final String UsbdmJniLibraryName = "UsbdmJniWrapper";

   
   // Keys used to obtain the device name e.g. "MC9S08JM16"
   public static final String RS08_DeviceNameAttributeKey  = "com.freescale.cdt.debug.cw.CW_SHADOWED_PREF.RS08 Debugger.processor";
   public static final String HCS08_DeviceNameAttributeKey = "com.freescale.cdt.debug.cw.CW_SHADOWED_PREF.HC08 Debugger.processor";
   public static final String CFV1_DeviceNameAttributeKey  = "com.freescale.cdt.debug.cw.CW_SHADOWED_PREF.CF Debugger.processor";
   public static final String CFVx_DeviceNameAttributeKey  = "com.freescale.cdt.debug.cw.CW_SHADOWED_PREF.CF Debugger.processor";
   public static final String ARM_DeviceNameAttributeKey   = "com.freescale.cdt.debug.cw.CW_SHADOWED_PREF.ARM Debugger.processor";
   public static final String DSC_DeviceNameAttributeKey   = "com.freescale.cdt.debug.cw.CW_SHADOWED_PREF.DSC Debugger.processor";
   public static final String S12Z_DeviceNameAttributeKey  = "com.freescale.cdt.debug.cw.CW_SHADOWED_PREF.S12Z Debugger.processor";
   public static final String CW_DeviceNameAttributeKey    = "com.freescale.cdt.debug.cw.processor";
   
   // Key used to access persistent attributes of the USBDM plug-in
   public static final String BaseAttributeName    = "net.sourceforge.usbdm.connections.usbdm"; //$NON-NLS-1$
   
   public static final String RS08_TypeID  = "net.sourceforge.usbdm.connections.usbdm.rs08";   //$NON-NLS-1$
   public static final String HCS08_TypeID = "net.sourceforge.usbdm.connections.usbdm.hcs08";  //$NON-NLS-1$
   public static final String CFV1_TypeID  = "net.sourceforge.usbdm.connections.usbdm.cfv1";   //$NON-NLS-1$
   public static final String CFVx_TypeID  = "net.sourceforge.usbdm.connections.usbdm.cfv234"; //$NON-NLS-1$
   public static final String ARM_TypeID   = "net.sourceforge.usbdm.connections.usbdm.arm";    //$NON-NLS-1$
   public static final String DSC_TypeID   = "net.sourceforge.usbdm.connections.usbdm.dsc";    //$NON-NLS-1$
   public static final String S12Z_TypeID  = "net.sourceforge.usbdm.connections.usbdm.s12z";   //$NON-NLS-1$
   
   // Names of the Device information files
   public static final String RS08_DEVICE_FILE  = "rs08_devices.xml";
   public static final String HCS08_DEVICE_FILE = "hcs08_devices.xml";
   public static final String CFV1_DEVICE_FILE  = "cfv1_devices.xml";
   
   // Names of the GDI DLLs for each target architecture
   public static final String RS08_GdiWrapperLib        = "usbdm-gdi-rs08.4";  //$NON-NLS-1$
   public static final String HCS08_GdiWrapperLib       = "usbdm-gdi-hcs08.4"; //$NON-NLS-1$
   public static final String CFV1_GdiWrapperLib        = "usbdm-gdi-cfv1.4";  //$NON-NLS-1$
   public static final String CFVx_GdiWrapperLib        = "usbdm-gdi-cfvx.4";  //$NON-NLS-1$
   public static final String ARM_GdiWrapperLib         = "usbdm-gdi-arm.4";  //$NON-NLS-1$
   public static final String DSC_GdiWrapperLib         = "usbdm-gdi-dsc.4";  //$NON-NLS-1$
   public static final String S12Z_GdiWrapperLib        = "usbdm-gdi-s12z.4";  //$NON-NLS-1$
   public static final String RS08_DebugGdiWrapperLib   = "usbdm-gdi-rs08-debug.4";  //$NON-NLS-1$
   public static final String HCS08_DebugGdiWrapperLib  = "usbdm-gdi-hcs08-debug.4"; //$NON-NLS-1$
   public static final String CFV1_DebugGdiWrapperLib   = "usbdm-gdi-cfv1-debug.4";  //$NON-NLS-1$
   public static final String CFVx_DebugGdiWrapperLib   = "usbdm-gdi-cfvx-debug.4";  //$NON-NLS-1$
   public static final String ARM_DebugGdiWrapperLib    = "usbdm-gdi-arm-debug.4";  //$NON-NLS-1$
   public static final String DSC_DebugGdiWrapperLib    = "usbdm-gdi-dsc-debug.4";  //$NON-NLS-1$
   public static final String S12Z_DebugGdiWrapperLib   = "usbdm-gdi-s12z-debug.4";  //$NON-NLS-1$

   public static final int BDM_TARGET_VDD_OFF = 0;   //!< - Target Vdd internal 3.3V
   public static final int BDM_TARGET_VDD_3V3 = 1;   //!< - Target Vdd internal 3.3V
   public static final int BDM_TARGET_VDD_5V  = 2;   //!< - Target Vdd internal 5.0V

   public static final int BDM_CLK_DEFAULT     = 0xFF;  //!< - Use default clock selection (don't modify target's reset default)
   public static final int BDM_CLK_ALT         = 0;  //!< - Force ALT clock (CLKSW = 0)
   public static final int BDM_CLK_NORMAL      = 1;  //!< - Force Normal clock (CLKSW = 1)

 //! BDM interface options
 //!
   public static class BdmOptions {
	   
    // Options passed to the BDM
    public int      targetVdd;                //!< - Target Vdd (off, 3.3V or 5V)
    public int      cycleVddOnReset;          //!< - Cycle target Power  when resetting
    public int      cycleVddOnConnect;        //!< - Cycle target Power if connection problems)
    public int      leaveTargetPowered;       //!< - Leave target power on exit
    public int      autoReconnect;            //!< - Automatically re-connect to target (for speed change)
    public int      guessSpeed;               //!< - Guess speed for target w/o ACKN
    public int      useAltBDMClock;           //!< - Use alternative BDM clock source in target
    public int      useResetSignal;           //!< - Whether to use RESET signal on BDM public int erface
    public int      maskinterrupts;           //!< - Whether to mask public int errupts when  stepping

    // Options used internally by DLL
    public int      manuallyCycleVdd;         //!< - Prompt user to manually cycle Vdd on connection problems
    public int      derivative_type;          //!< - RS08 Derivative
    public int      connectionSpeed;          //!< - CFVx/ARM/DSC - Interface frequency. \n
                                              //!< - Other targets automatically determine frequency.
    public int      clockTrimNVAddress;       //!< - Address in Flash to store clock trim values (0 => default)
    public int      clockTrimFrequency;       //!< - RS08/HCS08/CFV1 - Frequency to trim internal clock to (0 => default).
    public boolean  doClockTrim;              //!< - Trim target internal clock
    public int      usePSTSignals;            //!< - CFVx, PST Signal monitors
    
    int             powerOffDuration;         //!< - How long to remove power (ms)
    int             powerOnRecoveryInterval;  //!< - How long to wait after power enabled (ms)
    int             resetDuration;            //!< - How long to assert reset (ms)
    int             resetReleaseInterval;     //!< - How long to wait after reset release to release other signals (ms)
    int             resetRecoveryInterval;    //!< - How long to wait after reset sequence completes (ms)
    
   public int getPowerOffDuration() {
      return powerOffDuration;
   }
   public void setPowerOffDuration(int powerOffDuration) {
      this.powerOffDuration = powerOffDuration;
   }
   public int getPowerOnRecoveryInterval() {
      return powerOnRecoveryInterval;
   }
   public void setPowerOnRecoveryInterval(int powerOnRecoveryInterval) {
      this.powerOnRecoveryInterval = powerOnRecoveryInterval;
   }
   public int getResetDuration() {
      return resetDuration;
   }
   public void setResetDuration(int resetDuration) {
      this.resetDuration = resetDuration;
   }
   public int getResetReleaseInterval() {
      return resetReleaseInterval;
   }
   public void setResetReleaseInterval(int resetReleaseInterval) {
      this.resetReleaseInterval = resetReleaseInterval;
   }
   public int getResetRecoveryInterval() {
      return resetRecoveryInterval;
   }
   public void setResetRecoveryInterval(int resetRecoveryInterval) {
      this.resetRecoveryInterval = resetRecoveryInterval;
   }
 };

 // BDM options
    public static final String KeyDefaultBdmSerialNumber    = "defaultBdmSerialNumber";
    public static final String KeySetTargetVdd              = "setTargetVdd";
    public static final String KeyCycleTargetVddOnReset     = "cycleTargetVddOnReset";
    public static final String KeyCycleTargetVddonConnect   = "cycleTargetVddOnConnect";
    public static final String KeyLeaveTargetPowered        = "leaveTargetPowered";
    public static final String KeyAutomaticReconnect        = "automaticReconnect";
    public static final String KeyUseAltBDMClock            = "useAltBDMClock";
    public static final String KeyUseResetSignal            = "useResetSignal";
    public static final String KeyMaskInterrupt             = "maskInterrupt";
    public static final String KeyConnectionSpeed           = "connectionSpeed";
    public static final String KeyUsePSTSignals             = "usePSTSignals";
    
    public static final String KeyPowerOffDuration          = "powerOffDuration";
    public static final String KeyPowerOnRecoveryInterval   = "powerOnRecoveryInterval";
    public static final String KeyResetDuration             = "resetDuration";          
    public static final String KeyResetReleaseInterval      = "resetReleaseInterval";   
    public static final String KeyResetRecoveryInterval     = "resetRecoveryInterval";  

// Target options
    public static final String KeyUseDebugBuild             = "useDebugBuild";
    public static final String KeyClockTrimNVAddress        = "clockTrimNVAddress";
    public static final String KeyClockTrimFrequency        = "clockTrimFrequency";
    public static final String KeyTrimTargetClock           = "trimTargetClock";
    public static final String KeyEraseMethod               = "eraseMethod";
    public static final String KeySecurityOption            = "securityOption";
}
