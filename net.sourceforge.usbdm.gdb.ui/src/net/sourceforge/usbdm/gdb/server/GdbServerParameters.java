/*
 * Used to package USBDM GDB server parameters.
 * 
 */
package net.sourceforge.usbdm.gdb.server;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.constants.UsbdmSharedSettings;
import net.sourceforge.usbdm.gdb.ui.UsbdmDebuggerPanel;
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

/**
 * @author Peter
 * @since 4.12
 *
 */
public class GdbServerParameters {

   private InterfaceType         interfaceType = InterfaceType.T_ARM;
   private String                deviceName = "";
   private String                bdmSerialNumber = "";
   private boolean               bdmSerialNumberMatchRequired;
   private int                   gdbServerPortNumber;
   private int                   gdbTtyPortNumber;
   private boolean               useSemiHosting;
   private boolean               useDebugVersion;
   private boolean               exitOnClose;
   
   private EraseMethod           eraseMethod = EraseMethod.ERASE_MASS;
   private boolean               trimClock;
   private int                   clockTrimFrequency;
   private long                  nvmClockTrimLocation;
   private boolean               catchVLLSxEvents;
   private boolean               maskInterrupts;
                                 
   private ExtendedOptions       extendedOptions;
                                 
   private EraseMethod           preferredEraseMethod;
   private ResetType             preferredResetType;
   private int                   allowedEraseMethods;
   private int                   requiredDialogueComponents;
   
   private SecurityOptions		   securityOption;
   private int                   connectionTimeout;
   
   private static final String   deviceNameKey                   = "deviceName";
   private static final String   bdmSerialNumberKey              = "bdmSerialNumber";
   private static final String   bdmSerialNumberMatchRequiredKey = "bdmSerialNumberMatchRequired";
   private static final String   SERVER_PORT_KEY                 = "port";
   public  static final String   TTY_PORT_KEY                    = "ttyPort";
   public  static final String   USE_SEMI_HOSTING_KEY            = "useSemiHosting";
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
   private static final String   connectionTimeoutKey            = "connectionTimeout";
   private static final String   catchVLLSxEventsKey             = "catchVLLSxEvents";
   private static final String   maskInterruptsKey               = "maskInterrupts";
   
   private static final int      E_SELECTIVE_MASK  = (1<<EraseMethod.ERASE_SELECTIVE.ordinal());
   private static final int      E_ALL_MASK        = (1<<EraseMethod.ERASE_ALL.ordinal());
   private static final int      E_NONE_MASK       = (1<<EraseMethod.ERASE_NONE.ordinal());
   private static final int      E_MASS_MASK       = (1<<EraseMethod.ERASE_MASS.ordinal());
   private static final int      armMethods        = E_SELECTIVE_MASK|E_ALL_MASK|E_NONE_MASK|E_MASS_MASK;
   private static final int      cfv1Methods       = E_SELECTIVE_MASK|E_ALL_MASK|E_NONE_MASK|E_MASS_MASK;
   private static final int      cfvxMethods       = E_SELECTIVE_MASK|E_ALL_MASK|E_NONE_MASK;
                                 
   public  static final int      NEEDS_SPEED       = 1<<0;  // Mask indicating Interface requires Speed selection
   public  static final int      NEEDS_RESET       = 1<<1;  // Mask indicating Interface requires Reset selection
   public  static final int      NEEDS_PST         = 1<<2;  // Mask indicating Interface requires PST selection 
   public  static final int      NEEDS_CLOCK       = 1<<3;  // Mask indicating Interface requires Clock selection   
   public  static final int      NEEDS_VLLSCATCH   = 1<<4;  // Mask indicating Interface requires VLLSCatch selection
   public  static final int      NEEDS_CLKTRIM     = 1<<5;  // Mask indicating Interface requires VLLSCatch selection
   public  static final int      NEEDS_MASKINTS    = 1<<6;  // Mask indicating Interface requires VLLSCatch selection
   
   private static final int      armDialogueNeeds  = NEEDS_RESET|NEEDS_SPEED|NEEDS_VLLSCATCH|NEEDS_MASKINTS;
   private static final int      cfv1DialogueNeeds = NEEDS_RESET|NEEDS_CLOCK|NEEDS_CLKTRIM;
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
    * 
    * @throws Exception 
    */
   public static GdbServerParameters getDefaultServerParameters(InterfaceType interfaceType) throws Exception {
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
   
   /**
    * Create a GdbServerParameters from a map of launch settings
    * 
    * @param attributes Map to load settings from
    * 
    * @return GdbServerParameters or null on error
    */
   public static GdbServerParameters getInitializedServerParameters(Map<String, Object> attributes) {
      if (attributes == null) {
         return null;
      }
      String interfaceName = null;
      interfaceName = CDebugUtils.getAttribute(attributes, UsbdmDebuggerPanel.USBDM_GDB_INTERFACE_TYPE_KEY, "");
      if (interfaceName.isEmpty()) {
         return null;
      }
      GdbServerParameters gdbServerParameters = null;
      try {
         InterfaceType interfaceType = InterfaceType.valueOf(interfaceName);
         if (interfaceType == null) {
            return null;
         }
         gdbServerParameters = GdbServerParameters.getDefaultServerParameters(interfaceType);
         gdbServerParameters.initializeFrom(attributes, UsbdmDebuggerPanel.USBDM_LAUNCH_ATTRIBUTE_KEY);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return gdbServerParameters;
   }

   protected static class ArmGdbServerParameters extends GdbServerParameters {
      public ArmGdbServerParameters() throws Exception {
         super(InterfaceType.T_ARM);
         setPreferredEraseMethod(EraseMethod.ERASE_MASS);
         setAllowedEraseMethods(armMethods);
         setRequiredDialogueComponents(armDialogueNeeds);
         loadDefaultSettings();
      }
   }
   
   protected static class Cfv1GdbServerParameters extends GdbServerParameters {
      public Cfv1GdbServerParameters() throws Exception {
         super(InterfaceType.T_CFV1);
         setPreferredEraseMethod(EraseMethod.ERASE_MASS);
         setAllowedEraseMethods(cfv1Methods);
         setRequiredDialogueComponents(cfv1DialogueNeeds);
         loadDefaultSettings();
      }
   }
   
   protected static class CfvxGdbServerParameters extends GdbServerParameters {
      public CfvxGdbServerParameters() throws Exception {
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
      setGdbServerPortNumber(1234);
      setGdbTtyPortNumber(4321);
      setDeviceName(null);
      enableExitOnClose(false);
      setNvmClockTrimLocation(-1);
      setClockTrimFrequency(0);
      setEraseMethod(EraseMethod.ERASE_MASS);
      enableCatchVLLSxEvents(false);
      try {
         extendedOptions = Usbdm.getDefaultExtendedOptions(interfaceType.toTargetType());
      } catch (UsbdmException e) {
         // Ignore if we can't load defaults
         e.printStackTrace();
         extendedOptions = new ExtendedOptions(interfaceType.toTargetType());
      }
      setInterfaceType(interfaceType);
   }

   public String getBdmSerialNumber() {
      return bdmSerialNumber;
   }

   public void setBdmSerialNumber(String serialNumber, boolean serialNumberMatchRequired ) {
      this.bdmSerialNumber = "";
      if (serialNumber != null) {
         serialNumber = serialNumber.trim();
         if (!serialNumber.isEmpty()) {
            this.bdmSerialNumber = serialNumber;
         }
      }
      this.bdmSerialNumberMatchRequired = serialNumberMatchRequired && !this.bdmSerialNumber.isEmpty();
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

   public void setGdbServerPortNumber(int serverPort) {
      this.gdbServerPortNumber = serverPort;
   }

   public int getGdbServerPortNumber() {
      return gdbServerPortNumber;
   }

   public void setGdbTtyPortNumber(int ttyPort) {
      this.gdbTtyPortNumber = ttyPort;
   }

   public int getGdbTtyPortNumber() {
      return gdbTtyPortNumber;
   }

   public String getGdbServerPortNumberAsOption() {
      return "-port="+getGdbServerPortNumber();
   }

   public String getGdbTtyPortNumberAsOption() {
      return "-tty="+getGdbTtyPortNumber();
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
   
   public void setEraseMethod(EraseMethod eraseMethod) {
      this.eraseMethod = eraseMethod;
   }
   
   public EraseMethod getEraseMethod() {
      return eraseMethod;
   }

   private String getEraseMethodAsOption() {
	      return "-erase="+eraseMethod.getOptionName();
	   }
	   
   public void setTargetVdd(TargetVddSelect targetVdd) {
      extendedOptions.targetVdd = targetVdd;
   }
   
   public TargetVddSelect getTargetVdd() {
      return extendedOptions.targetVdd;
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
   
   public void setInterfaceFrequency(int interfaceFrequency) {
      extendedOptions.interfaceFrequency = interfaceFrequency;
   }

   public int getInterfaceFrequency() {
      return extendedOptions.interfaceFrequency;
   }

   public String getInterfaceFrequencyAsOption() {
      return "-speed="+getInterfaceFrequency()/1000;
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

   public String getAutoReconnectAsOption() {
      return "-auto="+extendedOptions.autoReconnect.getOptionName();
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

   public boolean isCatchVLLSxEvents() {
      return catchVLLSxEvents;
   }

   public void enableCatchVLLSxEvents(boolean catchVLLSxEvents) {
      this.catchVLLSxEvents = catchVLLSxEvents;
   }

   public boolean isMaskInterrupts() {
      return maskInterrupts;
   }

   public void enableMaskInterrupts(boolean maskInterrupts) {
      this.maskInterrupts = maskInterrupts;
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

   public void setConnectionTimeout(int connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
   }

   public int getConnectionTimeout() {
      return connectionTimeout;
   }

   private String getConnectionTimeoutParametersAsOption() {
      return String.format("-timeout=%d", getConnectionTimeout());
   }
   
   public void setSecurityOption(SecurityOptions securityOption) {
      this.securityOption = securityOption;
   }

   public SecurityOptions getSecurityOption() {
		return securityOption;
	}

   private String getSecurityOptionAsOption() {
      return "-security="+securityOption.getOptionName();
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
         return serverPath.append(UsbdmSharedConstants.USBDM_GDB_GUI_SERVER_DEBUG+exeSuffix);         
      }
      else {
         return serverPath.append(UsbdmSharedConstants.USBDM_GDB_GUI_SERVER+exeSuffix);
      }
   }

   protected String getServerOption() {
      return getInterfaceType().gdbServerOption;
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
//      return serverType;
      return GdbServerType.SERVER_SOCKET;
   }

   public void setServerType(GdbServerType serverType) {
//      this.serverType = serverType;
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

   /**
    *  Determines command line to pass to GDB described as a ArrayList.
    *  This will be either of these forms:
    *    ["| pipe-server-path", "device-name"]
    *  OR
    *    ["localhost:nnnn"]
    * 
    * @return as described above or null on error
    */
   public String getCommandLine() {
      if (getServerType() == GdbServerType.SERVER_SOCKET) {
         return "localhost:" + Integer.toString(getGdbServerPortNumber());
      }
      else {
         return null;
      }
   }
   
   /**
    * @return ArrayList {"path-to-gdbServer", "args", ...}
    */
   public ArrayList<String> getServerCommandLine() {
      ArrayList<String> commandList = new ArrayList<String>(20);
      
      commandList.add(getServerPath().toPortableString());

      commandList.add(getServerOption());

      if (getDeviceName() != null) {
         commandList.add("-device="+getDeviceName());
      }
      commandList.add(getGdbServerPortNumberAsOption());
      commandList.add(getGdbTtyPortNumberAsOption());
      if (!getBdmSerialNumber().isEmpty() && !getBdmSerialNumber().equals(USBDMDeviceInfo.nullDevice.deviceSerialNumber)) {
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
      commandList.add(getInterfaceFrequencyAsOption());
      commandList.add(getEraseMethodAsOption());
      commandList.add(getSecurityOptionAsOption());
      commandList.add(getConnectionTimeoutParametersAsOption());
      commandList.add(getAutoReconnectAsOption());
      
      if (isCatchVLLSxEvents()) {
         commandList.add("-catchvlls");
      }
      if (isMaskInterrupts()) {
         commandList.add("-maskInterrupts");
      }
      if (isUseReset()) {
         commandList.add("-useReset");
      }
      return commandList;
   }
   
   public String getServerCommandLineAsString() {
      ArrayList<String> cl = getServerCommandLine();
      StringBuffer clb = new StringBuffer();
      for (String s:cl) {
         clb.append(s);
         clb.append(' ');
      }
      return clb.toString();
   }
   
   /**
    * Load the GDB settings from the system default
    * 
    * Assumes the interface type has been set 
    * 
    * @throws Exception if interface type has not already been set
    */
   public void loadDefaultSettings() throws Exception {
//      System.err.println("GdbServerParameters.loadDefaultSettings()\n");
      if (interfaceType == null) {
         throw new Exception("Interface type must be set before loading defaults");
      }
      extendedOptions = Usbdm.getDefaultExtendedOptions(interfaceType.targetType);
      
      UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();

      setDeviceName(                                  settings.get(getKey(deviceNameKey),                   ""));
      setBdmSerialNumber(                             settings.get(getKey(bdmSerialNumberKey),              ""));
      enableBdmSerialNumberMatchRequired(             settings.get(getKey(bdmSerialNumberMatchRequiredKey), false));
      setGdbServerPortNumber(                         settings.get(getKey(SERVER_PORT_KEY),                 1234));
      setGdbTtyPortNumber(                            settings.get(getKey(TTY_PORT_KEY),                    4321));
      enableUseSemiHosting(                           settings.get(getKey(USE_SEMI_HOSTING_KEY),            false));
      enableUseDebugVersion(                          settings.get(getKey(useDebugVersionKey),              false));
      enableExitOnClose(                              settings.get(getKey(exitOnCloseKey),                  false));
      setServerType(GdbServerType.valueOf(            settings.get(getKey(serverTypeKey),                   GdbServerType.SERVER_SOCKET.name())));
      setInterfaceFrequency(ClockSpeed.findSuitable(  settings.get(getKey(interfaceFrequencyKey),           4000000)).getFrequency());
      setAutoReconnect(AutoConnect.valueOf(           settings.get(getKey(autoReconnectKey),                AutoConnect.AUTOCONNECT_ALWAYS.name())));
      enableUseReset(                                 settings.get(getKey(useResetKey),                     isUseReset()));
      enableUsePstSignals(                            settings.get(getKey(usePstSignalsKey),                isUsePstSignals()));
      setEraseMethod(EraseMethod.valueOf(             settings.get(getKey(eraseMethodKey),                  getPreferredEraseMethod().name())));
      setSecurityOption(SecurityOptions.valueOf(      settings.get(getKey(securityOptionKey),               SecurityOptions.SECURITY_SMART.name())));
      setTargetVdd(TargetVddSelect.valueOf(           settings.get(getKey(targetVddKey),                    TargetVddSelect.BDM_TARGET_VDD_OFF.name())));
      enableTrimClock(                                settings.get(getKey(trimClockKey),                    false));
      setClockTrimFrequency(                          settings.get(getKey(clockTrimFrequencyKey),           0));
      setNvmClockTrimLocation(                        settings.get(getKey(nvmClockTrimLocationKey),         0L));
      setConnectionTimeout(                           settings.get(getKey(connectionTimeoutKey),            10));
      enableCatchVLLSxEvents(                         settings.get(getKey(catchVLLSxEventsKey),             false));
      }
   
   /**
    * Save the GDB settings as the system default for the target type
    * 
    * @return
    * @throws Exception 
    */
   public boolean saveSettingsAsDefault() throws Exception {
      if (interfaceType == null) {
         throw new Exception("Interface type must be set before saving defaults");
      }
      UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();

      settings.put(getKey(deviceNameKey),                   getDeviceName());
      settings.put(getKey(bdmSerialNumberKey),              getBdmSerialNumber());
      settings.put(getKey(bdmSerialNumberMatchRequiredKey), isBdmSerialNumberMatchRequired());
      settings.put(getKey(SERVER_PORT_KEY),                 getGdbServerPortNumber());
      settings.put(getKey(TTY_PORT_KEY),                    getGdbTtyPortNumber());
      settings.put(getKey(USE_SEMI_HOSTING_KEY),            isUseSemihosting());
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
      settings.put(getKey(connectionTimeoutKey),            getConnectionTimeout());
      settings.put(getKey(catchVLLSxEventsKey),             isCatchVLLSxEvents());
      settings.put(getKey(maskInterruptsKey),               isMaskInterrupts());
      
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
      buff.append("getGdbServerPortNumber =         " + getGdbServerPortNumber()+"\n");
      buff.append("getGdbTtyPortNumber =            " + getGdbTtyPortNumber()+"\n");
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
      buff.append("getConnectionTimeout =           " + getConnectionTimeout()+"\n");
      buff.append("isCatchVLLSxEvents =             " + isCatchVLLSxEvents()+"\n");
      buff.append("isMaskInterrupts =               " + isMaskInterrupts()+"\n");
      buff.append("}\n");

      return buff.toString();
   }
   
   public void performApply(ILaunchConfigurationWorkingCopy configuration, String key) {
      // Save to settings 
      configuration.setAttribute((key+deviceNameKey),                    getDeviceName());
      configuration.setAttribute((key+bdmSerialNumberKey),               getBdmSerialNumber());
      configuration.setAttribute((key+bdmSerialNumberMatchRequiredKey),  isBdmSerialNumberMatchRequired());
      configuration.setAttribute((key+SERVER_PORT_KEY),                  getGdbServerPortNumber());
      configuration.setAttribute((key+TTY_PORT_KEY),                     getGdbTtyPortNumber());
      configuration.setAttribute((key+USE_SEMI_HOSTING_KEY),             isUseSemihosting());
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
      configuration.setAttribute((key+connectionTimeoutKey),             getConnectionTimeout());
      configuration.setAttribute((key+catchVLLSxEventsKey),              isCatchVLLSxEvents());
      configuration.setAttribute((key+maskInterruptsKey),                isMaskInterrupts());
      System.err.println("GdbServerParameters.performApply() "+key+catchVLLSxEventsKey+" = "+isCatchVLLSxEvents());

   }
   
   /**
    * Initialises from the launch configuration
    * 
    * @param config Launch configuration to load settings from
    * @param key    Root key to use for parameters
    * 
    * @throws CoreException
    */
   public void initializeFrom(ILaunchConfiguration config, final String key) throws CoreException {
      initializeFrom(config.getAttributes(), key);
   }
   
   /**
    * Initialises from a map of settings
    * 
    * @param attributes Map to load settings from
    * @param key        Root key to use for parameters
    * 
    * @throws CoreException
    */
   private void initializeFrom(Map<String, Object> attributes, final String key) throws CoreException {
      
      System.err.println("GdbServerParameters.initializeFrom() " + key + catchVLLSxEventsKey + " = " +
         CDebugUtils.getAttribute(attributes, (key+catchVLLSxEventsKey),              isCatchVLLSxEvents()));

      // Update from settings 
      setDeviceName(            CDebugUtils.getAttribute(attributes, (key+deviceNameKey),                    getDeviceName()));
      // Map KDS internal name to USBDM name e.g. MK20DN512xxx10 => MK20DN512M10
      final Pattern namePattern = Pattern.compile("^(.*)xxx([0-9]*)$");
      Matcher m = namePattern.matcher(getDeviceName());
      if (m.matches()) {
         setDeviceName(m.group(1)+"M"+m.group(2));
      }
      setBdmSerialNumber(       CDebugUtils.getAttribute(attributes, (key+bdmSerialNumberKey),               getBdmSerialNumber()), true);
      enableBdmSerialNumberMatchRequired(  
                                CDebugUtils.getAttribute(attributes, (key+bdmSerialNumberMatchRequiredKey),  isBdmSerialNumberMatchRequired()));
      setGdbServerPortNumber(   CDebugUtils.getAttribute(attributes, (key+SERVER_PORT_KEY),                  getGdbServerPortNumber()));
      setGdbTtyPortNumber(      CDebugUtils.getAttribute(attributes, (key+TTY_PORT_KEY),                     getGdbTtyPortNumber()));
      enableUseSemiHosting(     CDebugUtils.getAttribute(attributes, (key+USE_SEMI_HOSTING_KEY),             isUseSemihosting()));
      enableUseDebugVersion(    CDebugUtils.getAttribute(attributes, (key+useDebugVersionKey),               isUseDebugVersion()));
      enableExitOnClose(        CDebugUtils.getAttribute(attributes, (key+exitOnCloseKey),                   isExitOnClose()));
      setServerType(GdbServerType.valueOf(
                                CDebugUtils.getAttribute(attributes, (key+serverTypeKey),                    getServerType().name())));
      setInterfaceFrequency(    CDebugUtils.getAttribute(attributes, (key+interfaceFrequencyKey),            getInterfaceFrequency()));
      setAutoReconnect(AutoConnect.valueOf( 
                                CDebugUtils.getAttribute(attributes, (key+autoReconnectKey),                 getAutoReconnect().name())));
      enableUseReset(           CDebugUtils.getAttribute(attributes, (key+useResetKey),                      isUseReset()));
      enableUsePstSignals(      CDebugUtils.getAttribute(attributes, (key+usePstSignalsKey),                 isUsePstSignals()));
      setEraseMethod(EraseMethod.valueOf(  
              CDebugUtils.getAttribute(attributes, (key+eraseMethodKey),                   getEraseMethod().name())));
      setSecurityOption(SecurityOptions.valueOf(  
              CDebugUtils.getAttribute(attributes, (key+securityOptionKey),                getSecurityOption().name())));
      setTargetVdd(TargetVddSelect.valueOf( 
                                CDebugUtils.getAttribute(attributes, (key+targetVddKey),                     getTargetVdd().name())));
      enableTrimClock(          CDebugUtils.getAttribute(attributes, (key+trimClockKey),                     isTrimClock()));
      setClockTrimFrequency(    CDebugUtils.getAttribute(attributes, (key+clockTrimFrequencyKey),            getClockTrimFrequency()));
      setNvmClockTrimLocation(  CDebugUtils.getAttribute(attributes, (key+nvmClockTrimLocationKey),     (int)getNvmClockTrimLocation()));
      setConnectionTimeout(     CDebugUtils.getAttribute(attributes, (key+connectionTimeoutKey),             getConnectionTimeout()));
      enableCatchVLLSxEvents(   CDebugUtils.getAttribute(attributes, (key+catchVLLSxEventsKey),              isCatchVLLSxEvents()));
      enableMaskInterrupts(     CDebugUtils.getAttribute(attributes, (key+maskInterruptsKey),                isMaskInterrupts()));
      System.err.println("GdbServerParameters.initializeFrom() VLLS = " + isCatchVLLSxEvents());
   }

   public boolean isUseSemihosting() {
      return useSemiHosting;
   }

   public void enableUseSemiHosting(boolean selection) {
      useSemiHosting = selection;
   }
}
