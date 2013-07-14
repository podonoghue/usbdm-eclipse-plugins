package net.sourceforge.usbdm.cdt.ui.handlers;

import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;

import net.sourceforge.usbdm.constants.UsbdmSharedSettings;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.JTAGInterfaceData.ClockSpeed;
import net.sourceforge.usbdm.jni.Usbdm.AutoConnect;
import net.sourceforge.usbdm.jni.Usbdm.BdmInformation;
import net.sourceforge.usbdm.jni.Usbdm.EraseMethod;
import net.sourceforge.usbdm.jni.Usbdm.ExtendedOptions;
import net.sourceforge.usbdm.jni.Usbdm.ResetType;
import net.sourceforge.usbdm.jni.Usbdm.TargetVddSelect;
import net.sourceforge.usbdm.jni.Usbdm.USBDMDeviceInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * @author Peter
 *
 */
public class GdbServerParameters {

   private String             bdmSerialNumber;
   private boolean            bdmSerialNumberMatchRequired;
   private InterfaceType      interfaceType;
   private int                gdbPortNumber;
   private String             deviceName;
   private boolean            exitOnClose;
   private EraseMethod        eraseMethod;
   private int                clockTrimFrequency;
   private int                nvmClockTrimLocation;
   private boolean            trimClock;
                              
   private ExtendedOptions    extendedOptions;
                              
   private EraseMethod        preferredEraseMethod;
   private ResetType          preferredResetType;
   private int                allowedEraseMethods;
   
   private boolean            useDebugServer;
   
   private static final String   bdmSerialNumberKey              = "bdmSerialNumber";
//   private static final String bdmSerialNumberMatchRequiredKey = "bdmSerialNumberMatchRequired";
//   private static final String bdmInterfaceTypeKey             = "bdmInterfaceType";
   private static final String   interfaceFrequencyKey           = "interfaceFrequency";
   private static final String   portKey                         = "port";
   private static final String   useDebugServerKey               = "useDebugServerKey";
//   private static final String deviceKey                       = "device";
//   private static final String exitOnCloseKey                  = "exitOnClose";
   private static final String   eraseMethodKey                  = "eraseMethod";
   private static final String   trimClockKey                    = "trimClock";
   private static final String   clockTrimFrequencyKey           = "clockTrimFrequency";
   private static final String   nvmClockTrimLocationKey         = "nvmClockTrimLocation";
   private static final String   targetVddKey                    = "targetVdd";
   private static final String   autoReconnectKey                = "autoReconnect";
   private static final String   usePstSignalsKey                = "usePstSignals";
   private static final String   useResetKey                   = "driveReset";
   
   private static final int E_SELECTIVE_MASK  = (1<<EraseMethod.ERASE_SELECTIVE.ordinal());
   private static final int E_ALL_MASK        = (1<<EraseMethod.ERASE_ALL.ordinal());
   private static final int E_NONE_MASK       = (1<<EraseMethod.ERASE_NONE.ordinal());
   private static final int E_MASS_MASK       = (1<<EraseMethod.ERASE_MASS.ordinal());
   private static final int armMethods        = E_SELECTIVE_MASK|E_ALL_MASK|E_NONE_MASK|E_MASS_MASK;
   private static final int cfv1Methods       = E_SELECTIVE_MASK|E_ALL_MASK|E_NONE_MASK|E_MASS_MASK;
   private static final int cfvxMethods       = E_SELECTIVE_MASK|E_ALL_MASK|E_NONE_MASK;

   private static final  USBDMDeviceInfo nullDevice = new USBDMDeviceInfo("Generic BDM", "Any connected USBDM", new BdmInformation());

   static public class ArmGdbServerParameters extends GdbServerParameters {
      public ArmGdbServerParameters() {
         super(InterfaceType.T_ARM);
         setPreferredEraseMethod(EraseMethod.ERASE_MASS);
         setAllowedEraseMethods(armMethods);
         loadDefaultSettings();
      }
   }
   
   static public class Cfv1GdbServerParameters extends GdbServerParameters {
      public Cfv1GdbServerParameters() {
         super(InterfaceType.T_CFV1);
         setPreferredEraseMethod(EraseMethod.ERASE_MASS);
         setAllowedEraseMethods(cfv1Methods);
         loadDefaultSettings();
      }
   }
   
   static public class CfvxGdbServerParameters extends GdbServerParameters {
      public CfvxGdbServerParameters() {
         super(InterfaceType.T_CFVX);
         setPreferredEraseMethod(EraseMethod.ERASE_ALL);
         setAllowedEraseMethods(cfvxMethods);
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
      setExitOnClose(false);
      setNvmClockTrimLocation(-1);
      setClockTrimFrequency(0);
      setEraseMethod(EraseMethod.ERASE_MASS);
      try {
         extendedOptions = Usbdm.getDefaultExtendedOptions(interfaceType.toTargetType());
      } catch (UsbdmException e) {
         e.printStackTrace();
      }
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

   public void setBdmSerialNumberMatchRequired(boolean required) {
      this.bdmSerialNumberMatchRequired = required;
   }

   public InterfaceType getInterfaceType() {
      return interfaceType;
   }

   public void setInterfaceType(InterfaceType interfaceType) {
      this.interfaceType = interfaceType;
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

   public void setExitOnClose(boolean exitOnClose) {
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
      return "-erase="+eraseMethod.getName();
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
   
   public int getNvmClockTrimLocation() {
      return nvmClockTrimLocation;
   }

   public void setNvmClockTrimLocation(int nvmClockTrimLocation) {
      this.nvmClockTrimLocation = nvmClockTrimLocation;
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

   public void setPreferredEraseMethod(EraseMethod preferredEraseMethod) {
      this.preferredEraseMethod = preferredEraseMethod;
   }

   private IPath getServerPath() {
      IPath serverPath = Usbdm.getApplicationPath();
      if (isUseDebugServer()) {
         return serverPath.append(getInterfaceType().gdbDebugServer+".exe");
      }
      else {
         return serverPath.append(getInterfaceType().gdbServer+".exe");         
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

   public boolean isTrimclock() {
      return trimClock;
   }

   public void enableTrimClock(boolean trimClock) {
      this.trimClock = trimClock;
   }

   public boolean isUseDebugServer() {
      return useDebugServer;
   }

   public void enableUseDebugServer(boolean useDebugServer) {
      this.useDebugServer = useDebugServer;
   }

   public static USBDMDeviceInfo getNulldevice() {
      return nullDevice;
   }

   String getKey(String base) {
      return "gdbServer."+getInterfaceType().getName()+"."+base;
   }

   public void loadDefaultSettings() {
//      System.err.println("GdbServerParameters.loadDefaultSettings()\n");
      UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();
      setBdmSerialNumber(                             settings.get(getKey(bdmSerialNumberKey),      null));
      setGdbPortNumber(                               settings.get(getKey(portKey),                 1234));
      enableUseDebugServer(                           settings.get(getKey(useDebugServerKey),       false));
      setEraseMethod(EraseMethod.valueOf(             settings.get(getKey(eraseMethodKey),          getPreferredEraseMethod().name())));
      setInterfaceFrequency(ClockSpeed.findSuitable(  settings.get(getKey(interfaceFrequencyKey),   4000000)).getFrequency());
      setClockTrimFrequency(                          settings.get(getKey(clockTrimFrequencyKey),   0));
      setNvmClockTrimLocation(                        settings.get(getKey(nvmClockTrimLocationKey), 0));
      setTargetVdd(TargetVddSelect.valueOf(           settings.get(getKey(targetVddKey),            TargetVddSelect.BDM_TARGET_VDD_OFF.name())));
      setAutoReconnect(AutoConnect.valueOf(           settings.get(getKey(autoReconnectKey),        AutoConnect.AUTOCONNECT_ALWAYS.name())));
      enableUsePstSignals(                            settings.get(getKey(usePstSignalsKey),        false));
      enableUseReset(                                 settings.get(getKey(useResetKey),             false));
      enableTrimClock(                                settings.get(getKey(trimClockKey),            false));
//      System.err.println("GdbServerParameters.loadDefaultSettings(), getGdbPortNumber() = "+getGdbPortNumber());
      }
   
   public boolean saveSettings() {
      UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();

      settings.put(getKey(bdmSerialNumberKey),        getBdmSerialNumber());
      settings.put(getKey(portKey),                   getGdbPortNumber());
      settings.put(getKey(useDebugServerKey),         isUseDebugServer());
      settings.put(getKey(eraseMethodKey),            getEraseMethod().name());
      settings.put(getKey(interfaceFrequencyKey),     getInterfaceFrequency());
      settings.put(getKey(clockTrimFrequencyKey),     getClockTrimFrequency());
      settings.put(getKey(nvmClockTrimLocationKey),   getNvmClockTrimLocation());
      settings.put(getKey(targetVddKey),              getTargetVdd().name());
      settings.put(getKey(autoReconnectKey),          getAutoReconnect().name());
      settings.put(getKey(usePstSignalsKey),          isUsePstSignals());
      settings.put(getKey(useResetKey),               isUseReset());
      settings.put(getKey(trimClockKey),              isTrimclock());
      
      settings.flush();
      return true;
   }

   public String[] getServerCommandLine() {
      ArrayList<String> commandList = new ArrayList<String>(20);
      commandList.add(getServerPath().toPortableString());
      
      if (getDeviceName() != null) {
         commandList.add("-device="+getDeviceName());
      }
      commandList.add("-port="+getGdbPortNumber());
      if ((getBdmSerialNumber() != null) && !getBdmSerialNumber().equals(USBDMDeviceInfo.nullDevice.deviceSerialNumber)) {
         commandList.add("-bdm="+getBdmSerialNumber());
      }
      if (isExitOnClose()) {
         commandList.add("-exitOnClose");
      }
      if (getClockTrimFrequency() > 0) {
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
      
      String retVal[] = new String[commandList.size()];
      return commandList.toArray(retVal);
   }
}
