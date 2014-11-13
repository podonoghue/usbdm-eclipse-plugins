/*******************************************************************************
 * Copyright (c) 2013 Peter O'Donoghue.
 *
 * License GPL
 * 
 *******************************************************************************/
package net.sourceforge.usbdm.constants;

import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.jni.UsbdmJniConstants;

import org.eclipse.jface.resource.ImageDescriptor;

public class UsbdmSharedConstants {

   // Keys used in GDB Launch configurations 
   private static final String  LAUNCH_ATTRIBUTE_KEY                       = "net.sourceforge.usbdm.gdb.";  //$NON-NLS-1$
   
   public  static final String  LAUNCH_ATTR_RESTART_USES_STARTUP           = LAUNCH_ATTRIBUTE_KEY+"restartUsesStartup";  //$NON-NLS-1$
   public  static final String  LAUNCH_ATTR_RESTART_SET_PC_REGISTER        = LAUNCH_ATTRIBUTE_KEY+"restartSetPC";  //$NON-NLS-1$
   public  static final String  LAUNCH_ATTR_RESTART_PC_REGISTER            = LAUNCH_ATTRIBUTE_KEY+"restartPC";  //$NON-NLS-1$
   public  static final String  LAUNCH_ATTR_RESTART_SET_STOP_AT            = LAUNCH_ATTRIBUTE_KEY+"restartSetStopAt";  //$NON-NLS-1$
   public  static final String  LAUNCH_ATTR_RESTART_STOP_AT                = LAUNCH_ATTRIBUTE_KEY+"restartStopAt";  //$NON-NLS-1$
   public  static final String  LAUNCH_ATTR_RESTART_SET_RESUME             = LAUNCH_ATTRIBUTE_KEY+"restartSetResume";  //$NON-NLS-1$

   // Default - Load binary image
   public  static final boolean LAUNCH_DEFAULT_LOAD_IMAGE                  = true;
   public  static final boolean LAUNCH_DEFAULT_USE_PROJ_BINARY_FOR_IMAGE   = true;
   public  static final boolean LAUNCH_DEFAULT_USE_FILE_FOR_IMAGE          = false;
   public  static final String  LAUNCH_DEFAULT_IMAGE_FILE_NAME             = "";  //$NON-NLS-1$
   public  static final String  LAUNCH_DEFAULT_IMAGE_OFFSET                = "";  //$NON-NLS-1$

   // Default - Load symbols from same place as image
   public  static final boolean LAUNCH_DEFAULT_LOAD_SYMBOLS                = true;
   public  static final boolean LAUNCH_DEFAULT_USE_PROJ_BINARY_FOR_SYMBOLS = LAUNCH_DEFAULT_USE_PROJ_BINARY_FOR_IMAGE;
   public  static final boolean LAUNCH_DEFAULT_USE_FILE_FOR_SYMBOLS        = LAUNCH_DEFAULT_USE_FILE_FOR_IMAGE;
   public  static final String  LAUNCH_DEFAULT_SYMBOLS_FILE_NAME           = LAUNCH_DEFAULT_IMAGE_FILE_NAME;
   public  static final String  LAUNCH_DEFAULT_SYMBOLS_OFFSET              = LAUNCH_DEFAULT_IMAGE_OFFSET;

   // Default - Resume execution after load or connect
   public  static final boolean LAUNCH_DEFAULT_DO_RESET                    = false;
   public  static final boolean LAUNCH_DEFAULT_DO_HALT                     = false;
   public  static final boolean LAUNCH_DEFAULT_SET_RESUME                  = true;
   
   // Default - Don't set PC
   public  static final boolean LAUNCH_DEFAULT_SET_PC_REGISTER             = false;
   public  static final String  LAUNCH_DEFAULT_PC_REGISTER                 = "";
   
   // Default - stop at main
   public  static final boolean LAUNCH_DEFAULT_SET_STOP_AT                 = true;
   public  static final String  LAUNCH_DEFAULT_STOP_AT                     = "main";//$NON-NLS-1$

   public  static final boolean LAUNCH_DEFAULT_RESTART_USES_STARTUP        = true;
   public  static final boolean LAUNCH_DEFAULT_RESTART_SET_PC_REGISTER     = LAUNCH_DEFAULT_SET_PC_REGISTER;
   public  static final String  LAUNCH_DEFAULT_RESTART_PC_REGISTER         = LAUNCH_DEFAULT_PC_REGISTER;
   public  static final boolean LAUNCH_DEFAULT_RESTART_SET_STOP_AT         = LAUNCH_DEFAULT_SET_STOP_AT;
   public  static final String  LAUNCH_DEFAULT_RESTART_STOP_AT             = LAUNCH_DEFAULT_STOP_AT;
   public  static final boolean LAUNCH_DEFAULT_RESTART_SET_RESUME          = LAUNCH_DEFAULT_SET_RESUME;

   public  static final String  LAUNCH_DEFAULT_INIT_COMMANDS               = "";
   public  static final String  LAUNCH_DEFAULT_RUN_COMMANDS                = "";

   public  static final String  LAUNCH_DEVICE_NAME_KEY                     = LAUNCH_ATTRIBUTE_KEY+"deviceName";
   
   // IDs used in plugin.xml etc
   public  static final String USBDM_ARM_BUILD_TOOL_ID              = "net.sourceforge.usbdm.cdt.arm.toolchain.buildtools";  //$NON-NLS-1$
   public  static final String USBDM_COLDFIRE_BUILD_TOOL_ID         = "net.sourceforge.usbdm.cdt.coldfire.toolchain.buildtools";  //$NON-NLS-1$
   public  static final String ARMLTD_ARM_BUILD_ID                  = "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.armLtdGnuToolsForARM";  //$NON-NLS-1$
   public  static final String CODESOURCERY_ARM_BUILD_ID            = "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.codesourceryARM";  //$NON-NLS-1$
   public  static final String CODESOURCERY_COLDFIRE_BUILD_ID       = "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.codesourceryColdfire";  //$NON-NLS-1$

   // Keys used for Eclipse dynamic variables as settings files
   public  static final String ARMLTD_ARM_PATH_VAR                  = "usbdm_armLtd_arm_path";  //$NON-NLS-1$
   public  static final String ARMLTD_ARM_PREFIX_VAR                = "usbdm_armLtd_arm_prefix";  //$NON-NLS-1$

   public  static final String CODESOURCERY_ARM_PATH_VAR            = "usbdm_codesourcery_arm_path";  //$NON-NLS-1$
   public  static final String CODESOURCERY_ARM_PREFIX_VAR          = "usbdm_codesourcery_arm_prefix";  //$NON-NLS-1$

   public  static final String CODESOURCERY_COLDFIRE_PATH_VAR       = "usbdm_codesourcery_coldfire_path";  //$NON-NLS-1$
   public  static final String CODESOURCERY_COLDFIRE_PREFIX_VAR     = "usbdm_codesourcery_coldfire_prefix";  //$NON-NLS-1$

   public  static final String USBDM_MAKE_COMMAND_VAR               = "usbdm_make_command";  //$NON-NLS-1$
   public  static final String USBDM_RM_COMMAND_VAR                 = "usbdm_rm_command";  //$NON-NLS-1$
   public  static final String USBDM_APPLICATION_PATH_VAR           = "usbdm_application_path";  //$NON-NLS-1$
   public  static final String USBDM_RESOURCE_PATH_VAR              = "usbdm_resource_path";  //$NON-NLS-1$
//   public static final String USBDM_COMPILER_FLAGS_VAR             = "usbdm_compiler_flags";  //$NON-NLS-1$

   // Names of external programs
   public final static String GDB_NAME                        = "gdb";                             //$NON-NLS-1$
   public final static String USBDM_ARM_GDB_SERVER            = "usbdm-arm-gdbPipeServer";         //$NON-NLS-1$
   public final static String USBDM_ARM_GDB_SERVER_DEBUG      = "usbdm-arm-gdbPipeServer-debug";   //$NON-NLS-1$
   public final static String USBDM_CFV1_GDB_SERVER           = "usbdm-cfv1-gdbPipeServer";        //$NON-NLS-1$
   public final static String USBDM_CFV1_GDB_SERVER_DEBUG     = "usbdm-cfv1-gdbPipeServer-debug";  //$NON-NLS-1$
   public final static String USBDM_CFVX_GDB_SERVER           = "usbdm-cfvx-gdbPipeServer";        //$NON-NLS-1$
   public final static String USBDM_CFVX_GDB_SERVER_DEBUG     = "usbdm-cfvx-gdbPipeServer-debug";  //$NON-NLS-1$
   public final static String USBDM_ARM_GDB_GUI_SERVER        = "ARM_GDBServer";  //$NON-NLS-1$
   public final static String USBDM_ARM_GDB_GUI_SERVER_DEBUG  = "ARM_GDBServer-debug";  //$NON-NLS-1$
   public final static String USBDM_CFV1_GDB_GUI_SERVER       = "CFV1_GDBServer";  //$NON-NLS-1$
   public final static String USBDM_CFV1_GDB_GUI_SERVER_DEBUG = "CFV1_GDBServer-debug";  //$NON-NLS-1$
   public final static String USBDM_CFVX_GDB_GUI_SERVER       = "CFVx_GDBServer";  //$NON-NLS-1$
   public final static String USBDM_CFVX_GDB_GUI_SERVER_DEBUG = "CFVx_GDBServer-debug";  //$NON-NLS-1$
   
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
      /**  Returns a legible name for use in GUI
       */
      public String toString() {
         return legibleName;
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
