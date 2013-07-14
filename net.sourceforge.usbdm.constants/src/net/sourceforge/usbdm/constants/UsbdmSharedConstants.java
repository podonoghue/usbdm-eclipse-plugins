/*******************************************************************************
 * Copyright (c) 2013 Peter O'Donoghue.
 *
 * License GPL
 * 
 *******************************************************************************/
package net.sourceforge.usbdm.constants;

import org.eclipse.jface.resource.ImageDescriptor;

import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.jni.UsbdmJniConstants;

public class UsbdmSharedConstants {

   // IDs used in plugin.xml etc
   public static final String ARMLTD_ARM_BUILD_ID                  = "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.armLtdGnuToolsForARM";
   public static final String CODESOURCERY_ARM_BUILD_ID            = "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.codesourceryARM";
   public static final String CODESOURCERY_COLDFIRE_BUILD_ID       = "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.codesourceryColdfire";

   // Keys used for Eclipse dynamic variables as settings files
   public static final String ARMLTD_ARM_PATH_VAR                  = "usbdm_armLtd_arm_path";
   public static final String ARMLTD_ARM_PREFIX_VAR                = "usbdm_armLtd_arm_prefix";

   public static final String CODESOURCERY_ARM_PATH_VAR            = "usbdm_codesourcery_arm_path";
   public static final String CODESOURCERY_ARM_PREFIX_VAR          = "usbdm_codesourcery_arm_prefix";

   public static final String CODESOURCERY_COLDFIRE_PATH_VAR       = "usbdm_codesourcery_coldfire_path";
   public static final String CODESOURCERY_COLDFIRE_PREFIX_VAR     = "usbdm_codesourcery_coldfire_prefix";

   public static final String USBDM_MAKE_COMMAND_VAR               = "usbdm_make_command";
   public static final String USBDM_RM_COMMAND_VAR                 = "usbdm_rm_command";
   public static final String USBDM_APPLICATION_PATH_VAR           = "usbdm_application_path";

   // Keys used in GDB Launch configurations 
   public final static String attributeKey             = "net.sourceforge.usbdm.gdb";   //$NON-NLS-1$
   public final static String attributeKey_Family      = attributeKey+".family";        //$NON-NLS-1$
   public final static String attributeKey_Device      = attributeKey+".device";        //$NON-NLS-1$
   public final static String attributeKey_DebugMode   = attributeKey+".debugMode";     //$NON-NLS-1$
   public final static String attributeKey_GdbBinPath  = attributeKey+".gdbBinPath";    //$NON-NLS-1$
   public final static String attributeKey_GdbCommand  = attributeKey+".gdbCommand";    //$NON-NLS-1$
   public final static String attributeKey_BuildToolId = attributeKey+".buildToolId";   //$NON-NLS-1$
   
   // Names of external programs
   public final static String GDB_NAME                        = "gdb";                         //$NON-NLS-1$
   public final static String USBDM_ARM_GDB_SERVER            = "usbdm-arm-gdbPipeServer";         //$NON-NLS-1$
   public final static String USBDM_ARM_GDB_SERVER_DEBUG      = "usbdm-arm-gdbPipeServer-debug";   //$NON-NLS-1$
   public final static String USBDM_CFV1_GDB_SERVER           = "usbdm-cfv1-gdbPipeServer";        //$NON-NLS-1$
   public final static String USBDM_CFV1_GDB_SERVER_DEBUG     = "usbdm-cfv1-gdbPipeServer-debug";  //$NON-NLS-1$
   public final static String USBDM_CFVX_GDB_SERVER           = "usbdm-cfvx-gdbPipeServer";        //$NON-NLS-1$
   public final static String USBDM_CFVX_GDB_SERVER_DEBUG     = "usbdm-cfvx-gdbPipeServer-debug";  //$NON-NLS-1$
   public final static String USBDM_ARM_GDB_GUI_SERVER        = "ARM_GDBServer";
   public final static String USBDM_ARM_GDB_GUI_SERVER_DEBUG  = "ARM_GDBServer-debug";
   public final static String USBDM_CFV1_GDB_GUI_SERVER       = "CFV1_GDBServer";
   public final static String USBDM_CFV1_GDB_GUI_SERVER_DEBUG = "CFV1_GDBServer-debug";
   public final static String USBDM_CFVX_GDB_GUI_SERVER       = "CFVx_GDBServer";
   public final static String USBDM_CFVX_GDB_GUI_SERVER_DEBUG = "CFVx_GDBServer-debug";
   
   // Descriptive names
   public final static String USBDM_INTERFACE_NAME     = "USBDM Interface";             //$NON-NLS-1$
   public final static String ARM_INTERFACE_NAME       = "ARM Interface";               //$NON-NLS-1$
   public final static String CFV1_INTERFACE_NAME      = "Coldfire V1 Interface";       //$NON-NLS-1$
   public final static String CFVx_INTERFACE_NAME      = "Coldfire V2,3,4 Interface";   //$NON-NLS-1$
   
   // Information about GDB interfaces (launching mostly)
   public enum InterfaceType {
      T_ARM   (TargetType.T_ARM, ARM_INTERFACE_NAME,
            UsbdmJniConstants.ARM_DEVICE_FILE,
            USBDM_ARM_GDB_SERVER,
            USBDM_ARM_GDB_SERVER_DEBUG,
            USBDM_ARM_GDB_GUI_SERVER,
            USBDM_ARM_GDB_GUI_SERVER_DEBUG ),
      T_CFV1  (TargetType.T_CFV1, CFV1_INTERFACE_NAME, 
            UsbdmJniConstants.CFV1_DEVICE_FILE, 
            USBDM_CFV1_GDB_SERVER, 
            USBDM_CFV1_GDB_SERVER_DEBUG,
            USBDM_CFV1_GDB_GUI_SERVER,
            USBDM_CFV1_GDB_GUI_SERVER_DEBUG ),
      T_CFVX  (TargetType.T_CFVx, CFVx_INTERFACE_NAME, 
            UsbdmJniConstants.CFVX_DEVICE_FILE, 
            USBDM_CFVX_GDB_SERVER, 
            USBDM_CFVX_GDB_SERVER_DEBUG,
            USBDM_CFVX_GDB_GUI_SERVER,
            USBDM_CFVX_GDB_GUI_SERVER_DEBUG ),
      ;
      private final String legibleName;
      public  final String deviceFile;
      public  final String gdbSprite;
      public  final String gdbDebugSprite;
      public  final String gdbServer;
      public  final String gdbDebugServer;
      public  final TargetType targetType;
      
      InterfaceType(TargetType targetType, String legibleName, String deviceFile,
                    String gdbSprite, String gdbDebugSprite, 
                    String gdbServer, String gdbDebugServer) {
         this.targetType      = targetType;
         this.legibleName     = legibleName;
         this.deviceFile      = deviceFile;
         this.gdbSprite       = gdbSprite;
         this.gdbDebugSprite  = gdbDebugSprite;
         this.gdbServer       = gdbServer;
         this.gdbDebugServer  = gdbDebugServer;
      }
      public String toString() {
         return legibleName;
      }
      public String getName() {
         // name = enumerated name
         return name();
      }
      public TargetType toTargetType() {
         return targetType;
      }
   };
      
   private static ImageDescriptor usbdmIcon = null;

   public static ImageDescriptor getUsbdmIcon() {
      if (usbdmIcon == null) {
         usbdmIcon = Activator.getImageDescriptor("/icons/usbdm.png");
      }
      return usbdmIcon;
   }

}
