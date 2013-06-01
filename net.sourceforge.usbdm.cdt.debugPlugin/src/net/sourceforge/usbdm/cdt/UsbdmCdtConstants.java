/*******************************************************************************
 * Copyright (c) 2013 Peter O'Donoghue.
 *
 * License GPL
 * 
 *******************************************************************************/
package net.sourceforge.usbdm.cdt;

import net.sourceforge.usbdm.jni.UsbdmJniConstants;

public class UsbdmCdtConstants {
   public final static String attributeKey             = "net.sourceforge.usbdm.cdt";   //$NON-NLS-1$
   public final static String attributeKey_Family      = attributeKey+".family";        //$NON-NLS-1$
   public final static String attributeKey_Device      = attributeKey+".device";        //$NON-NLS-1$
   public final static String attributeKey_DebugMode   = attributeKey+".debugMode";     //$NON-NLS-1$
   public final static String attributeKey_GdbBinPath  = attributeKey+".gdbBinPath";    //$NON-NLS-1$
   public final static String attributeKey_GdbCommand  = attributeKey+".gdbCommand";    //$NON-NLS-1$
   
   public final static String armPrefix                = "arm-none-eabi-";              //$NON-NLS-1$
   public final static String cfv1Prefix               = "m68k-elf-";                   //$NON-NLS-1$
   public final static String cfvxPrefix               = "m68k-elf-";                   //$NON-NLS-1$
   public final static String gdbName                  = "gdb";                         //$NON-NLS-1$
   public final static String usbdmArmGdbServer        = "usbdm-arm-gdbServer";         //$NON-NLS-1$
   public final static String usbdmArmGdbServerDebug   = "usbdm-arm-gdbServer-debug";   //$NON-NLS-1$
   public final static String usbdmCfv1GdbServer       = "usbdm-cfv1-gdbServer";        //$NON-NLS-1$
   public final static String usbdmCfv1GdbServerDebug  = "usbdm-cfv1-gdbServer-debug";  //$NON-NLS-1$
   public final static String usbdmCfvxGdbServer       = "usbdm-cfvx-gdbServer";        //$NON-NLS-1$
   public final static String usbdmCfvxGdbServerDebug  = "usbdm-cfvx-gdbServer-debug";  //$NON-NLS-1$
   
   public final static String usbdmInterfaceName       = "USBDM Interface";
   public final static String ARM_INTERFACE_NAME       = "ARM Interface";
   public final static String CFV1_INTERFACE_NAME      = "Coldfire V1 Interface";
   public final static String CFVx_INTERFACE_NAME      = "Coldfire V2,3,4 Interface";
   
   public enum InterfaceType {
      T_ARM   (ARM_INTERFACE_NAME,  UsbdmJniConstants.ARM_DEVICE_FILE,  armPrefix,  usbdmArmGdbServer,  usbdmArmGdbServerDebug  ),
      T_CFV1  (CFV1_INTERFACE_NAME, UsbdmJniConstants.CFV1_DEVICE_FILE, cfv1Prefix, usbdmCfv1GdbServer, usbdmCfv1GdbServerDebug ),
      T_CFVX  (CFVx_INTERFACE_NAME, UsbdmJniConstants.CFVX_DEVICE_FILE, cfvxPrefix, usbdmCfvxGdbServer, usbdmCfvxGdbServerDebug ),
      ;
      private final String legibleName;
      public  final String deviceFile;
      public  final String prefix;
      public  final String gdbSprite;
      public  final String gdbDebugSprite;
      public  final String gdbCommand;
      
      InterfaceType(String legibleName, String deviceFile, String prefix, String gdbSprite, String gdbDebugSprite) {
         this.legibleName     = legibleName;
         this.deviceFile      = deviceFile;
         this.prefix          = prefix;
         this.gdbSprite       = gdbSprite;
         this.gdbDebugSprite  = gdbDebugSprite;
         this.gdbCommand      = prefix + gdbName;
      }
      public String toString() {
         return legibleName;
      }
   };
}
