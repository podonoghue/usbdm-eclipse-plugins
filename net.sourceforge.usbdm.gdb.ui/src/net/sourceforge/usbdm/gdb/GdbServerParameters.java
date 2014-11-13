/*
 * Used to package USBDM GDB server parameters.
 * 
 */
package net.sourceforge.usbdm.gdb;

import java.util.ArrayList;

import net.sourceforge.usbdm.constants.UsbdmSharedSettings;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.gdb.ui.UsbdmDebuggerTab;
import net.sourceforge.usbdm.jni.JTAGInterfaceData.ClockSpeed;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.Usbdm.AutoConnect;
import net.sourceforge.usbdm.jni.Usbdm.BdmInformation;
import net.sourceforge.usbdm.jni.Usbdm.EraseMethod;
import net.sourceforge.usbdm.jni.Usbdm.ExtendedOptions;
import net.sourceforge.usbdm.jni.Usbdm.ResetType;
import net.sourceforge.usbdm.jni.Usbdm.SecurityOptions;
import net.sourceforge.usbdm.jni.Usbdm.TargetVddSelect;
import net.sourceforge.usbdm.jni.Usbdm.USBDMDeviceInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.IStatus;
//import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * @author Peter
 * @since 4.10
 *
 */
public class GdbServerParameters {

   private InterfaceType         interfaceType;
   private String                deviceName;
   private String                bdmSerialNumber;
   private boolean               bdmSerialNumberMatchRequired;
   private int                   gdbPortNumber;
   private boolean               useDebugVersion;
   private boolean               exitOnClose;
   private GdbServerType         serverType;
   
   private EraseMethod           eraseMethod;
   private boolean               trimClock;
   private int                   clockTrimFrequency;
   private long                  nvmClockTrimLocation;
                                 
   private ExtendedOptions       extendedOptions;
                                 
   private EraseMethod           preferredEraseMethod;
   private ResetType             preferredResetType;
   private int                   allowedEraseMethods;
   private int                   requiredDialogueComponents;
   
   private SecurityOptions		 securityOption;
   
   private static final String   deviceNameKey                   = "deviceName";
   private static final String   bdmSerialNumberKey              = "bdmSerialNumber";
   private static final String   bdmSerialNumberMatchRequiredKey = "bdmSerialNumberMatchRequired";
   private static final String   portKey                         = "port";
   private static final String   useDebugVersionKey              = "useDebugVersion";
   private static final String   exitOnCloseKey                  = "exitOnClose";
   private static final String   serverTypeKey                   = "serverType";
   private static final String   interfaceFrequencyKey           = "interfaceFrequency";
   private static final String   autoReconnectKey                = "autoReconnect";
   private static final String   useResetKey                     = "driveReset";
   private static final String   usePstSignalsKey                = "usePstSignals";
   private static final String   eraseMethodKey                  = "eraseMethod";
   private static final String   securityOptionKey               = "securityOption";
   private static final String   targetVddKey                    = "targetVdd";
   private static final String   trimClockKey                    = "trimClock";
   private static final String   clockTrimFrequencyKey           = "clockTrimFrequency";
   private static final String   nvmClockTrimLocationKey         = "nvmClockTrimLocation";
   
   private static final int      E_SELECTIVE_MASK  = (1<<EraseMethod.ERASE_SELECTIVE.ordinal());
   private static final int      E_ALL_MASK        = (1<<EraseMethod.ERASE_ALL.ordinal());
   private static final int      E_NONE_MASK       = (1<<EraseMethod.ERASE_NONE.ordinal());
   private static final int      E_MASS_MASK       = (1<<EraseMethod.ERASE_MASS.ordinal());
   private static final int      armMethods        = E_SELECTIVE_MASK|E_ALL_MASK|E_NONE_MASK|E_MASS_MASK;
   private static final int      cfv1Methods       = E_SELECTIVE_MASK|E_ALL_MASK|E_NONE_MASK|E_MASS_MASK;
   private static final int      cfvxMethods       = E_SELECTIVE_MASK|E_ALL_MASK|E_NONE_MASK;
                                 
   /**  Mask indicating Interface requires Speed selection                                 
    */
   public  static final int      NEEDS_SPEED = 1<<0;
   
   /**  Mask indicating Interface requires Reset selection                                 
    */
   public  static final int      NEEDS_RESET = 1<<1;
   
   /**  Mask indicating Interface requires PST selection                                 
    */
   public  static final int      NEEDS_PST   = 1<<2;
   
   /**  Mask indicating Interface requires Clock selection                                 
    */
   public  static final int      NEEDS_CLOCK = 1<<3;
   private static final int      armDialogueNeeds  = NEEDS_SPEED;
   private static final int      cfv1DialogueNeeds = NEEDS_RESET|NEEDS_CLOCK;
   private static final int      cfvxDialogueNeeds = NEEDS_SPEED|NEEDS_PST;
           
   private static final  USBDMDeviceInfo nullDevice = new USBDMDeviceInfo("Generic BDM", "Any connected USBDM", new BdmInformation());

   public static enum GdbServerType {
      SERVER_PIPE       ("Pipe based server"),
      SERVER_SOCKET     ("Socket based server"),
      ;
      private final String legibleName;
      private GdbServerType(String legibleName) {
         this.legibleName = legibleName;
      }
      public String getLegibleName() {
         return legibleName;
      }
   }
   
   /**
    *   Get default GdbServerParameters for Interface
    *   
    * @param interfaceType  Interface to select defaults
    * @return Appropriate GdbServerParameters
    */
   public static GdbServerParameters getDefaultServerParameters(InterfaceType interfaceType) {
      switch (interfaceType) {
      case T_ARM:
         return new ArmGdbServerParameters();
      case T_CFV1:
         return new Cfv1GdbServerParameters();
      case T_CFVX:
         return new CfvxGdbServerParameters();
      default:
         return null;
      }
   }
   
   public static GdbServerParameters getInitializedServerParameters(ILaunchConfiguration config) {
      if (config == null) {
         return null;
      }
      String interfaceName = null;
      try {
         interfaceName = config.getAttribute(UsbdmDebuggerTab.USBDM_GDB_INTERFACE_TYPE_KEY, (String)null);
      } catch (CoreException e) {
         return null;
      }
      if (interfaceName == null) {
         return null;
      }
      InterfaceType interfaceType = null;
      try {
         interfaceType = InterfaceType.valueOf(interfaceName);
      } catch (Exception e2) {
         return null;
      }
      if (interfaceType == null) {
         return null;
      }
      GdbServerParameters gdbServerParameters = GdbServerParameters.getDefaultServerParameters(interfaceType);
      if (gdbServerParameters == null) {
         return null;
      }
      try {
         gdbServerParameters.initializeFrom(config);
      } catch (CoreException e) {
         return null;
      }
      return gdbServerParameters;
   }

   protected static class ArmGdbServerParameters extends GdbServerParameters {
      public ArmGdbServerParameters() {
         super(InterfaceType.T_ARM);
         setPreferredEraseMethod(EraseMethod.ERASE_MASS);
         setAllowedEraseMethods(armMethods);
         setRequiredDialogueComponents(armDialogueNeeds);
         loadDefaultSettings();
      }
   }
   
   protected static class Cfv1GdbServerParameters extends GdbServerParameters {
      public Cfv1GdbServerParameters() {
         super(InterfaceType.T_CFV1);
         setPreferredEraseMethod(EraseMethod.ERASE_MASS);
         setAllowedEraseMethods(cfv1Methods);
         setRequiredDialogueComponents(cfv1DialogueNeeds);
         loadDefaultSettings();
      }
   }
   
   protected static class CfvxGdbServerParameters extends GdbServerParameters {
      public CfvxGdbServerParameters() {
         super(InterfaceType.T_CFVX);
         setPreferredEraseMethod(EraseMethod.ERASE_ALL);
         setAllowedEraseMethods(cfvxMethods);
         setRequiredDialogueComponents(cfvxDialogueNeeds);
         loadDefaultSettings();
      }
   }
   
   /**
    * @param interfaceType             Type of interface (T_ARM, T_CFV1 etc)              
    * @param serialNumber              Serial number of (preferred) bdm 
    * @param serialNumberMatchRequired Only use bdm with serial number
    * 
    * @note If serialNumberMatchRequired is false then serialNumber is viewed as a preference only
    */
   protected GdbServerParameters(InterfaceType  interfaceType) {

      setBdmSerialNumber(null, false);
      setInterfaceType(interfaceType);
      setGdbPortNumber(1234);
      setDeviceName(null);
      enableExitOnClose(false);
      setNvmClockTrimLocation(-1);
      setClockTrimFrequency(0);
      setEraseMethod(EraseMethod.ERASE_MASS);
      try {
         extendedOptions = Usbdm.getDefaultExtendedOptions(interfaceType.toTargetType());
      } catch (UsbdmException e) {
         // Ignore if we can't load defaults
         e.printStackTrace();
      }
      setInterfaceType(interfaceType);
   }

   public String getBdmSerialNumber() {
      return bdmSerialNumber;
   }

   public void setBdmSerialNumber(String serialNumber, boolean serialNumberMatchRequired ) {
      this.bdmSerialNumber = null;
      if (serialNumber != null) {
         serialNumber = serialNumber.trim();
         if (!serialNumber.isEmpty()) {
            this.bdmSerialNumber = serialNumber;
         }
      }
      this.bdmSerialNumberMatchRequired = serialNumberMatchRequired && (this.bdmSerialNumber != null);
   }

   public void setBdmSerialNumber(String serialNumber) {
      setBdmSerialNumber(serialNumber, false);
   }

   public boolean isBdmSerialNumberMatchRequired() {
      return bdmSerialNumberMatchRequired;
   }

   public void enableBdmSerialNumberMatchRequired(boolean required) {
      this.bdmSerialNumberMatchRequired = required;
   }

   public InterfaceType getInterfaceType() {
      return interfaceType;
   }

   public void setInterfaceType(InterfaceType interfaceType) {
      this.interfaceType              = interfaceType;
      if (extendedOptions != null) {
         this.extendedOptions.targetType = interfaceType.toTargetType();
      }
   }

   public int getGdbPortNumber() {
      return gdbPortNumber;
   }

   public void setGdbPortNumber(int port) {
      this.gdbPortNumber = port;
   }

   public String getDeviceName() {
      return deviceName;
   }

   public void setDeviceName(String deviceName) {
      this.deviceName = deviceName;
   }

   public boolean isExitOnClose() {
      return exitOnClose;
   }

   public void enableExitOnClose(boolean exitOnClose) {
      this.exitOnClose = exitOnClose;
   }

   public ExtendedOptions getExtendedOptions() {
      return extendedOptions;
   }

   public void setExtendedOptions(ExtendedOptions extendedOptions) {
      this.extendedOptions = extendedOptions;
   }
   
   public void setResetParameters(int resetDuration, int resetReleaseInterval, int resetRecoveryInterval) {
      extendedOptions.resetDuration          = resetDuration;
      extendedOptions.resetReleaseInterval   = resetReleaseInterval;
      extendedOptions.resetRecoveryInterval  = resetRecoveryInterval;
   }

   private String getResetParametersAsOption() {
      return String.format("-reset=%d,%d,%d", extendedOptions.resetDuration, extendedOptions.resetReleaseInterval, extendedOptions.resetRecoveryInterval);
   }

   public void setPowerParameters(int powerOffDuration, int powerOnRecoveryInterval) {
      extendedOptions.powerOffDuration        = powerOffDuration;
      extendedOptions.powerOnRecoveryInterval = powerOnRecoveryInterval;
   }

   private String getPowerParametersAsOption() {
      return String.format("-power=%d,%d", extendedOptions.powerOffDuration, extendedOptions.powerOnRecoveryInterval);
   }
   
   public EraseMethod getEraseMethod() {
      return eraseMethod;
   }

   public void setEraseMethod(EraseMethod eraseMethod) {
      this.eraseMethod = eraseMethod;
   }
   
   private String getEraseMethodAsOption() {
	      return "-erase="+eraseMethod.getOptionName();
	   }
	   
   private String getSecurityOptionAsOption() {
	      return "-security="+securityOption.getOptionName();
	   }
	   
   public TargetVddSelect getTargetVdd() {
      return extendedOptions.targetVdd;
   }

   public void setTargetVdd(TargetVddSelect targetVdd) {
      extendedOptions.targetVdd = targetVdd;
   }
   
   private String getTargetVddAsOption() {
      switch(extendedOptions.targetVdd) {
      case BDM_TARGET_VDD_3V3:
         return "-vdd=3V3";
      case BDM_TARGET_VDD_5V:
         return "-vdd=5V";
      default:
         return null;
      }
   }
   
   public int getInterfaceFrequency() {
      return extendedOptions.interfaceFrequency;
   }

   public void setInterfaceFrequency(int interfaceFrequency) {
      extendedOptions.interfaceFrequency = interfaceFrequency;
   }
   
   public int getClockTrimFrequency() {
      return clockTrimFrequency;
   }

   public void setClockTrimFrequency(int clockTrimFrequency) {
      this.clockTrimFrequency = clockTrimFrequency;
   }
   
   public long getNvmClockTrimLocation() {
      return nvmClockTrimLocation;
   }

   public void setNvmClockTrimLocation(int i) {
      this.nvmClockTrimLocation = ((long)i)&0xFFFFFFFFL;
   }

   public void setNvmClockTrimLocation(long l) {
      this.nvmClockTrimLocation = l;
   }

   public AutoConnect getAutoReconnect() {
      return extendedOptions.autoReconnect;
   }

   public void setAutoReconnect(AutoConnect autoConnect) {
      extendedOptions.autoReconnect = autoConnect;
   }

   public boolean isUsePstSignals() {
      return extendedOptions.usePstSignals;
   }

   public void enableUsePstSignals(boolean usePstSignals) {
      extendedOptions.usePstSignals = usePstSignals;
   }

   public boolean isUseReset() {
      return extendedOptions.useResetSignal;
   }

   public void enableUseReset(boolean useReset) {
      extendedOptions.useResetSignal = useReset;
   }

   public EraseMethod getPreferredEraseMethod() {
      return preferredEraseMethod;
   }

   protected void setPreferredEraseMethod(EraseMethod preferredEraseMethod) {
      this.preferredEraseMethod = preferredEraseMethod;
   }

   public SecurityOptions getSecurityOption() {
		return securityOption;
	}

	public void setSecurityOption(SecurityOptions securityOption) {
		this.securityOption = securityOption;
	}

   protected IPath getServerPath() {
      IPath serverPath = Usbdm.getApplicationPath();
      String exeSuffix = "";
      String os = System.getProperty("os.name");
      if ((os != null) && os.toUpperCase().contains("LINUX")) {
         exeSuffix = "";
      }
      else {
         exeSuffix = ".exe";
      }
      if (isUseDebugVersion()) {
         return serverPath.append(getInterfaceType().gdbDebugServer+exeSuffix);
      }
      else {
         return serverPath.append(getInterfaceType().gdbServer+exeSuffix);         
      }
   }

   protected IPath getSpritePath() {
      IPath spritePath = Usbdm.getApplicationPath();
      if (isUseDebugVersion()) {
         return spritePath.append(getInterfaceType().gdbDebugSprite+".exe");
      }
      else {
         return spritePath.append(getInterfaceType().gdbSprite+".exe");         
      }
   }

   public ResetType getPreferredResetType() {
      return preferredResetType;
   }

   public void setPreferredResetType(ResetType preferredResetType) {
      this.preferredResetType = preferredResetType;
   }
   
   /**
    * @param  method erase method to check
    * 
    * @return indicates if method is supported by target
    */
   public boolean isAllowedEraseMethod(EraseMethod method) {
      return (allowedEraseMethods&(1<<method.ordinal())) != 0;
   }

   protected void setAllowedEraseMethods(int eraseMethods) {
      this.allowedEraseMethods = eraseMethods;
   }

   public boolean isRequiredDialogueComponents(int component) {
      return (requiredDialogueComponents&component) != 0;
   }

   protected void setRequiredDialogueComponents(int requiredDialogueComponents) {
      this.requiredDialogueComponents = requiredDialogueComponents;
   }

   public boolean isTrimClock() {
      return trimClock;
   }

   public void enableTrimClock(boolean trimClock) {
      this.trimClock = trimClock;
   }

   public boolean isUseDebugVersion() {
      return useDebugVersion;
   }

   public void enableUseDebugVersion(boolean useDebugVersion) {
      this.useDebugVersion = useDebugVersion;
   }

   public GdbServerType getServerType() {
      return serverType;
   }

   public void setServerType(GdbServerType serverType) {
      this.serverType = serverType;
   }

   public static USBDMDeviceInfo getNulldevice() {
      return nullDevice;
   }

   String getKey(String base) {
      return "gdbServer."+getInterfaceType().name()+"."+base;
   }

   protected String escapeArg(String arg) {
      if (arg.indexOf(' ') >= 0) { 
         return '"' + arg + '"'; 
      }
      return arg;
   }

   protected String escapePath(String file) {
      if (file.indexOf('\\') >= 0) {
         return escapeArg(file.replace("\\", "\\\\"));
      }
      return escapeArg(file);
   }

   /*!
    *  Determines command line to pass to GDB described as a ArrayList.
    *  This will be either of these forms:
    *    ["| pipe-server-path", "device-name"]
    *  OR
    *    ["localhost:nnnn"]
    * 
    * @return as described above or null on error
    */
   public ArrayList<String> getCommandLine() {
      ArrayList<String> commandList =  new ArrayList<String>(20);
      if (getServerType() == GdbServerType.SERVER_PIPE) {
         commandList.add("| " + escapePath(getSpritePath().toOSString()));
         if (getDeviceName() != null) {
            commandList.add(escapeArg(getDeviceName()));
         }
         return commandList;
      }
      else if (getServerType() == GdbServerType.SERVER_SOCKET) {
         commandList.add("localhost:" + Integer.toString(getGdbPortNumber()));
         return commandList;
      }
      else {
         return null;
      }
   }
   
   /*!
    * @return ArrayList {"path-to-gdbServer", "args", ...}
    */
   public ArrayList<String> getServerCommandLine() {
      ArrayList<String> commandList = new ArrayList<String>(20);
      
      commandList.add(getServerPath().toPortableString());
      
      if (getDeviceName() != null) {
         commandList.add("-device="+getDeviceName());
      }
      commandList.add("-port="+getGdbPortNumber());
      if ((getBdmSerialNumber() != null) && !getBdmSerialNumber().equals(USBDMDeviceInfo.nullDevice.deviceSerialNumber)) {
         if (isBdmSerialNumberMatchRequired()) {
            commandList.add("-requiredBdm="+getBdmSerialNumber());
         }
         else {
            commandList.add("-bdm="+getBdmSerialNumber());
         }
      }
      if (isExitOnClose()) {
         commandList.add("-exitOnClose");
      }
      if (isTrimClock() && (getClockTrimFrequency() != 0)) {
         commandList.add("-trim="+getClockTrimFrequency());
         if (getNvmClockTrimLocation() > 0) {
            commandList.add("-nvloc="+getNvmClockTrimLocation());
         }
      }
      commandList.add(getPowerParametersAsOption());
      commandList.add(getResetParametersAsOption());
      String vdd = getTargetVddAsOption();
      if (vdd != null) {
         commandList.add(vdd);
      }
      commandList.add("-speed="+getInterfaceFrequency()/1000);
      commandList.add(getEraseMethodAsOption());
      commandList.add(getSecurityOptionAsOption());
      return commandList;
   }
   
   public void loadDefaultSettings() {
//      System.err.println("GdbServerParameters.loadDefaultSettings()\n");
      UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();
      
      setDeviceName(                                  settings.get(getKey(deviceNameKey),                   null));
      setBdmSerialNumber(                             settings.get(getKey(bdmSerialNumberKey),              null));
      enableBdmSerialNumberMatchRequired(             settings.get(getKey(bdmSerialNumberMatchRequiredKey), false));
      setGdbPortNumber(                               settings.get(getKey(portKey),                         1234));
      enableUseDebugVersion(                          settings.get(getKey(useDebugVersionKey),              false));
      enableExitOnClose(                              settings.get(getKey(exitOnCloseKey),                  false));
      setServerType(GdbServerType.valueOf(            settings.get(getKey(serverTypeKey),                   GdbServerType.SERVER_SOCKET.name())));
      setInterfaceFrequency(ClockSpeed.findSuitable(  settings.get(getKey(interfaceFrequencyKey),           4000000)).getFrequency());
      setAutoReconnect(AutoConnect.valueOf(           settings.get(getKey(autoReconnectKey),                AutoConnect.AUTOCONNECT_ALWAYS.name())));
      enableUseReset(                                 settings.get(getKey(useResetKey),                     false));
      enableUsePstSignals(                            settings.get(getKey(usePstSignalsKey),                false));
      setEraseMethod(EraseMethod.valueOf(             settings.get(getKey(eraseMethodKey),                  getPreferredEraseMethod().name())));
      setSecurityOption(SecurityOptions.valueOf(      settings.get(getKey(securityOptionKey),               SecurityOptions.SECURITY_SMART.name())));
      setTargetVdd(TargetVddSelect.valueOf(           settings.get(getKey(targetVddKey),                    TargetVddSelect.BDM_TARGET_VDD_OFF.name())));
      enableTrimClock(                                settings.get(getKey(trimClockKey),                    false));
      setClockTrimFrequency(                          settings.get(getKey(clockTrimFrequencyKey),           0));
      setNvmClockTrimLocation(                        settings.get(getKey(nvmClockTrimLocationKey),         0L));
//      System.err.println("GdbServerParameters.loadDefaultSettings(), getGdbPortNumber() = "+getGdbPortNumber());
      }
   
   public boolean saveSettings() {
      UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();

      settings.put(getKey(deviceNameKey),                   getDeviceName());
      settings.put(getKey(bdmSerialNumberKey),              getBdmSerialNumber());
      settings.put(getKey(bdmSerialNumberMatchRequiredKey), isBdmSerialNumberMatchRequired());
      settings.put(getKey(portKey),                         getGdbPortNumber());
      settings.put(getKey(useDebugVersionKey),              isUseDebugVersion());
      settings.put(getKey(exitOnCloseKey),                  isExitOnClose());
      settings.put(getKey(serverTypeKey),                   getServerType().name());
      settings.put(getKey(interfaceFrequencyKey),           getInterfaceFrequency());
      settings.put(getKey(autoReconnectKey),                getAutoReconnect().name());
      settings.put(getKey(useResetKey),                     isUseReset());
      settings.put(getKey(usePstSignalsKey),                isUsePstSignals());
      settings.put(getKey(eraseMethodKey),                  getEraseMethod().name());
      settings.put(getKey(securityOptionKey),               getSecurityOption().name());
      settings.put(getKey(targetVddKey),                    getTargetVdd().name());
      settings.put(getKey(trimClockKey),                    isTrimClock());
      settings.put(getKey(clockTrimFrequencyKey),           getClockTrimFrequency());
      settings.put(getKey(nvmClockTrimLocationKey),         getNvmClockTrimLocation());
      
      settings.flush();
      return true;
   }

   public String toString() {
      StringBuffer buff = new StringBuffer(2000);
      
      buff.append("{\n");
      buff.append("getInterfaceType =               " + getInterfaceType()+"\n");
      buff.append("getDeviceName =                  " + getDeviceName()+"\n");
      buff.append("bdmSerialNumberKey =             " + getBdmSerialNumber()+"\n");
      buff.append("isBdmSerialNumberMatchRequired = " + isBdmSerialNumberMatchRequired()+"\n");
      buff.append("getGdbPortNumber =               " + getGdbPortNumber()+"\n");
      buff.append("isUseDebugServer =               " + isUseDebugVersion()+"\n");
      buff.append("isExitOnClose =                  " + isExitOnClose()+"\n");
      buff.append("getServerType =                  " + getServerType()+"\n");
      buff.append("getInterfaceFrequency =          " + getInterfaceFrequency()+"\n");
      buff.append("getAutoReconnect =               " + getAutoReconnect()+"\n");
      buff.append("isUseReset =                     " + isUseReset()+"\n");
      buff.append("isUsePstSignals =                " + isUsePstSignals()+"\n");
      buff.append("getEraseMethod =                 " + getEraseMethod()+"\n");
      buff.append("getSecurityOption =              " + getSecurityOption()+"\n");
      buff.append("getTargetVdd =                   " + getTargetVdd()+"\n");
      buff.append("isTrimclock =                    " + isTrimClock()+"\n");
      buff.append("getClockTrimFrequency =          " + getClockTrimFrequency()+"\n");
      buff.append("getNvmClockTrimLocation =        " + getNvmClockTrimLocation()+"\n");
      buff.append("}\n");

      return buff.toString();
   }
   
   public void performApply(ILaunchConfigurationWorkingCopy configuration, String key) {
      // Save to settings 
      configuration.setAttribute((key+deviceNameKey),                    getDeviceName());
      configuration.setAttribute((key+bdmSerialNumberKey),               getBdmSerialNumber());
      configuration.setAttribute((key+bdmSerialNumberMatchRequiredKey),  isBdmSerialNumberMatchRequired());
      configuration.setAttribute((key+portKey),                          getGdbPortNumber());
      configuration.setAttribute((key+useDebugVersionKey),               isUseDebugVersion());
      configuration.setAttribute((key+exitOnCloseKey),                   isExitOnClose());
      configuration.setAttribute((key+serverTypeKey),                    getServerType().name());
      configuration.setAttribute((key+interfaceFrequencyKey),            getInterfaceFrequency());
      configuration.setAttribute((key+autoReconnectKey),                 getAutoReconnect().name());
      configuration.setAttribute((key+useResetKey),                      isUseReset());
      configuration.setAttribute((key+usePstSignalsKey),                 isUsePstSignals());
      configuration.setAttribute((key+eraseMethodKey),                   getEraseMethod().name());
      configuration.setAttribute((key+securityOptionKey),                getSecurityOption().name());
      configuration.setAttribute((key+targetVddKey),                     getTargetVdd().name());
      configuration.setAttribute((key+trimClockKey),                     isTrimClock());
      configuration.setAttribute((key+clockTrimFrequencyKey),            getClockTrimFrequency());
      configuration.setAttribute((key+nvmClockTrimLocationKey),          (int)getNvmClockTrimLocation());
   }
   
   private static final String key = UsbdmDebuggerTab.USBDM_LAUNCH_ATTRIBUTE_KEY;      

   public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
      
      // Update from settings 
      setDeviceName(            configuration.getAttribute((key+deviceNameKey),                    getDeviceName()));
      setBdmSerialNumber(       configuration.getAttribute((key+bdmSerialNumberKey),               getBdmSerialNumber()), true);
      enableBdmSerialNumberMatchRequired(  
                                configuration.getAttribute((key+bdmSerialNumberMatchRequiredKey),  isBdmSerialNumberMatchRequired()));
      setGdbPortNumber(         configuration.getAttribute((key+portKey),                          getGdbPortNumber()));
      enableUseDebugVersion(    configuration.getAttribute((key+useDebugVersionKey),               isUseDebugVersion()));
      enableExitOnClose(        configuration.getAttribute((key+exitOnCloseKey),                   isExitOnClose()));
      setServerType(GdbServerType.valueOf(
                                configuration.getAttribute((key+serverTypeKey),                    getServerType().name())));
      setInterfaceFrequency(    configuration.getAttribute((key+interfaceFrequencyKey),            getInterfaceFrequency()));
      setAutoReconnect(AutoConnect.valueOf( 
                                configuration.getAttribute((key+autoReconnectKey),                 getAutoReconnect().name())));
      enableUseReset(           configuration.getAttribute((key+useResetKey),                      isUseReset()));
      enableUsePstSignals(      configuration.getAttribute((key+usePstSignalsKey),                 isUsePstSignals()));
      setEraseMethod(EraseMethod.valueOf(  
              configuration.getAttribute((key+eraseMethodKey),                   getEraseMethod().name())));
      setSecurityOption(SecurityOptions.valueOf(  
              configuration.getAttribute((key+securityOptionKey),                getSecurityOption().name())));
      setTargetVdd(TargetVddSelect.valueOf( 
                                configuration.getAttribute((key+targetVddKey),                     getTargetVdd().name())));
      enableTrimClock(          configuration.getAttribute((key+trimClockKey),                     isTrimClock()));
      setClockTrimFrequency(    configuration.getAttribute((key+clockTrimFrequencyKey),            getClockTrimFrequency()));
      setNvmClockTrimLocation(  configuration.getAttribute((key+nvmClockTrimLocationKey),     (int)getNvmClockTrimLocation()));
   }
}
