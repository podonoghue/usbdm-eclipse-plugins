package net.sourceforge.usbdm.jni;

public class UsbdmJniConstants {

   public static final String UsbdmJniLibraryName      = "UsbdmJniWrapper";
   public static final String UsbdmJniDebugLibraryName = "UsbdmJniWrapper-debug";

   //Names of libraries to load (Win32)
   //public static final String LibUsbLibraryName_dll     = "libusb-1.0";
   public static final String UsbdmLibGccName_dll       = "libgcc_s_dw2-1";
   public static final String UsbdmLibStdcName_dll      = "libstdc++-6";
   public static final String UsbdmLibraryName_dll      = "usbdm.4";
   public static final String UsbdmDebugLibraryName_dll = "usbdm-debug.4";
   
   //Names of libraries to load (Linux)
   public static final String LibUsbLibraryName_so     = "usb-1.0";
   public static final String UsbdmLibraryName_so      = "usbdm";
   public static final String UsbdmDebugLibraryName_so = "usbdm-debug";
   
   // Names of the Device information files
   public static final String ARM_DEVICE_FILE   = "arm_devices.xml";
   public static final String CFV1_DEVICE_FILE  = "cfv1_devices.xml";
   public static final String CFVX_DEVICE_FILE  = "cfvx_devices.xml";
   public static final String RS08_DEVICE_FILE  = "rs08_devices.xml";
   public static final String HCS08_DEVICE_FILE = "hcs08_devices.xml";
}