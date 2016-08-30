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
   private static final String  LAUNCH_ATTRIBUTE_KEY                  = "net.sourceforge.usbdm.gdb.";                    //$NON-NLS-1$
                                                                                                                         
   public static final String  LAUNCH_DEVICE_NAME_KEY                = LAUNCH_ATTRIBUTE_KEY+"deviceName";               //$NON-NLS-1$

   public static final String  ATTR_PROGRAM_TARGET                   = LAUNCH_ATTRIBUTE_KEY+"loadImage";                //$NON-NLS-1$

   public static final String  ATTR_USE_EXTERNAL_FILE                = LAUNCH_ATTRIBUTE_KEY+"useExternalFile";          //$NON-NLS-1$
   public static final String  ATTR_EXTERNAL_FILE_NAME               = LAUNCH_ATTRIBUTE_KEY+"externalFileName";         //$NON-NLS-1$

   // Not used
   public static final String  ATTR_USE_BINARY_OFFSET                = LAUNCH_ATTRIBUTE_KEY+"useBinaryOffset";          //$NON-NLS-1$
   public static final String  ATTR_BINARY_OFFSET_VALUE              = LAUNCH_ATTRIBUTE_KEY+"binaryOffset";             //$NON-NLS-1$

   public static final String  ATTR_SET_PC_REGISTER                  = LAUNCH_ATTRIBUTE_KEY+"setPcRegister";            //$NON-NLS-1$
   public static final String  ATTR_PC_REGISTER_VALUE                = LAUNCH_ATTRIBUTE_KEY+"pcRegisterValue";          //$NON-NLS-1$
   
   public static final String  ATTR_DO_STOP_AT_MAIN                  = LAUNCH_ATTRIBUTE_KEY+"doStopAtMain";             //$NON-NLS-1$
   public static final String  ATTR_STOP_AT_MAIN_ADDRESS             = LAUNCH_ATTRIBUTE_KEY+"stopAtMainAddress";        //$NON-NLS-1$

   public static final String  ATTR_DO_RESET                         = LAUNCH_ATTRIBUTE_KEY+"doReset";                  //$NON-NLS-1$
   public static final String  ATTR_DO_RESUME                        = LAUNCH_ATTRIBUTE_KEY+"doResume";                 //$NON-NLS-1$
   public static final String  ATTR_DO_HALT                          = LAUNCH_ATTRIBUTE_KEY+"doHalt";                   //$NON-NLS-1$
   
   public static final String  ATTR_INIT_COMMANDS                    = LAUNCH_ATTRIBUTE_KEY+"initCommands";             //$NON-NLS-1$
   public static final String  ATTR_RUN_COMMANDS                     = LAUNCH_ATTRIBUTE_KEY+"runCommands";              //$NON-NLS-1$
   
   public static final String  ATTR_USE_EXTERNAL_SYMBOL_FILE         = LAUNCH_ATTRIBUTE_KEY+"useExternalSymbolFile";    //$NON-NLS-1$
   public static final String  ATTR_EXTERNAL_SYMBOL_FILE_NAME        = LAUNCH_ATTRIBUTE_KEY+"externalSymbolFileName";   //$NON-NLS-1$

   // Not used
   public static final String  ATTR_USE_SYMBOLS_OFFSET               = LAUNCH_ATTRIBUTE_KEY+"useSymbolsOffset";         //$NON-NLS-1$
   public static final String  ATTR_SYMBOLS_OFFSET_VALUE             = LAUNCH_ATTRIBUTE_KEY+"symbolsOffset";            //$NON-NLS-1$

   public static final String  ATTR_RESTART_USES_STARTUP             = LAUNCH_ATTRIBUTE_KEY+"restartUsesStartup";       //$NON-NLS-1$
   
   public static final String  ATTR_RESTART_SET_PC_REGISTER          = LAUNCH_ATTRIBUTE_KEY+"restartSetPcRegister";     //$NON-NLS-1$
   public static final String  ATTR_RESTART_PC_REGISTER_VALUE        = LAUNCH_ATTRIBUTE_KEY+"restartPcRegisterValue";   //$NON-NLS-1$
   public static final String  ATTR_RESTART_DO_STOP_AT_MAIN          = LAUNCH_ATTRIBUTE_KEY+"restartDoStopAtMain";      //$NON-NLS-1$
   public static final String  ATTR_RESTART_STOP_AT_MAIN_ADDRESS     = LAUNCH_ATTRIBUTE_KEY+"restartStopAtMainAdress";  //$NON-NLS-1$
   public static final String  ATTR_RESTART_DO_RESUME                = LAUNCH_ATTRIBUTE_KEY+"restartDoResume";         //$NON-NLS-1$

   // Default - Load binary image
   public static final boolean DEFAULT_PROGRAM_TARGET                = true;

   public static final boolean DEFAULT_USE_EXTERNAL_FILE             = false;
   public static final String  DEFAULT_EXTERNAL_FILE_NAME            = "";                                              //$NON-NLS-1$

   // Not used
   public static final boolean DEFAULT_USE_BINARY_OFFSET             = false;
   public static final String  DEFAULT_BINARY_OFFSET_VALUE           = "";                                              //$NON-NLS-1$
                                                                     
   // Default - Don't set PC                                         
   public static final boolean DEFAULT_SET_PC_REGISTER               = false;
   public static final String  DEFAULT_PC_REGISTER_VALUE             = "__HardReset";                                   //$NON-NLS-1$
                                                                     
   // Default - stop at main                                         
   public static final boolean DEFAULT_DO_STOP_AT_MAIN               = true;
   public static final String  DEFAULT_STOP_AT_MAIN_ADDRESS          = "main";                                          //$NON-NLS-1$
                                                                     
   // Default - Resume execution after load or connect               
   public static final boolean DEFAULT_DO_RESET                      = true;
   public static final boolean DEFAULT_DO_RESUME                     = true;
   public static final boolean DEFAULT_DO_HALT                       = false;
                                                                     
   // Default - Load symbols                                         
   public static final boolean DEFAULT_USE_EXTERNAL_SYMBOL_FILE      = false;
   public static final String  DEFAULT_EXTERNAL_SYMBOL_FILE_NAME     = DEFAULT_EXTERNAL_FILE_NAME;
                                                                     
   // Not used                                                       
   public static final boolean DEFAULT_USE_SYMBOLS_OFFSET            = false;
   public static final String  DEFAULT_SYMBOLS_OFFSET_VALUE          = "";                                              //$NON-NLS-1$
                                                                     
   public static final boolean DEFAULT_RESTART_USES_STARTUP          = true;
   public static final boolean DEFAULT_RESTART_SET_PC_REGISTER       = DEFAULT_SET_PC_REGISTER;
   public static final String  DEFAULT_RESTART_PC_REGISTER_VALUE     = DEFAULT_PC_REGISTER_VALUE;
   public static final boolean DEFAULT_RESTART_DO_STOP_AT_MAIN       = DEFAULT_DO_STOP_AT_MAIN;
   public static final String  DEFAULT_RESTART_STOP_AT_MAIN_ADDRESS  = DEFAULT_STOP_AT_MAIN_ADDRESS;
   public static final boolean DEFAULT_RESTART_DO_RESUME             = DEFAULT_DO_RESUME;
                                                                     
   public static final String  DEFAULT_INIT_COMMANDS                 = "";                                              //$NON-NLS-1$
   public static final String  DEFAULT_RUN_COMMANDS                  = "";                                              //$NON-NLS-1$

   // IDs used in plugin.xml etc
   public static final String USBDM_ARM_BUILD_TOOL_ID                     = "net.sourceforge.usbdm.cdt.arm.toolchain.buildtools";                                //$NON-NLS-1$
   public static final String USBDM_COLDFIRE_BUILD_TOOL_ID                = "net.sourceforge.usbdm.cdt.coldfire.toolchain.buildtools";                           //$NON-NLS-1$
   public static final String ARMLTD_ARM_BUILD_ID                         = "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.armLtdGnuToolsForARM"; //$NON-NLS-1$
   public static final String CODESOURCERY_ARM_BUILD_ID                   = "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.codesourceryARM";      //$NON-NLS-1$
   public static final String CODESOURCERY_COLDFIRE_BUILD_ID              = "net.sourceforge.usbdm.cdt.toolchain.processor.usbdmConfigure.codesourceryColdfire"; //$NON-NLS-1$
                                                                           
   // Keys used for Eclipse dynamic variables as settings files            
   public static final String ARMLTD_ARM_PATH_VAR                         = "usbdm_armLtd_arm_path";                 //$NON-NLS-1$
   public static final String ARMLTD_ARM_PREFIX_VAR                       = "usbdm_armLtd_arm_prefix";               //$NON-NLS-1$
                                                                           
   public static final String CODESOURCERY_ARM_PATH_VAR                   = "usbdm_codesourcery_arm_path";           //$NON-NLS-1$
   public static final String CODESOURCERY_ARM_PREFIX_VAR                 = "usbdm_codesourcery_arm_prefix";         //$NON-NLS-1$
                                                                           
   public static final String CODESOURCERY_COLDFIRE_PATH_VAR              = "usbdm_codesourcery_coldfire_path";      //$NON-NLS-1$
   public static final String CODESOURCERY_COLDFIRE_PREFIX_VAR            = "usbdm_codesourcery_coldfire_prefix";    //$NON-NLS-1$
                                                                           
   public static final String USBDM_MAKE_COMMAND_VAR                      = "usbdm_make_command";                    //$NON-NLS-1$
   public static final String USBDM_RM_COMMAND_VAR                        = "usbdm_rm_command";                      //$NON-NLS-1$
   public static final String USBDM_APPLICATION_PATH_VAR                  = "usbdm_application_path";                //$NON-NLS-1$
   public static final String USBDM_RESOURCE_PATH_VAR                     = "usbdm_resource_path";                   //$NON-NLS-1$
   public static final String USBDM_KSDK_PATH                             = "usbdm_kds_path";                        //$NON-NLS-1$

   // Names of external programs
   public final static String GDB_NAME                                     = "gdb";                                  //$NON-NLS-1$
   public final static String USBDM_GDB_GUI_SERVER                         = "UsbdmGdbServer";                       //$NON-NLS-1$
   public final static String USBDM_GDB_GUI_SERVER_DEBUG                   = "UsbdmGdbServer-debug";                 //$NON-NLS-1$
                                                                                                                    
   public final static String USBDM_ARM_SERVER_OPTION                      = "-target=arm";                          //$NON-NLS-1$
   public final static String USBDM_CFV1_SERVER_OPTION                     = "-target=cfv1";                         //$NON-NLS-1$
   public final static String USBDM_CFVx_SERVER_OPTION                     = "-target=cfvx";                         //$NON-NLS-1$
                                                                                                                    
   // Descriptive names                                                                                             
   public final static String USBDM_INTERFACE_NAME                         = "USBDM Interface";                      //$NON-NLS-1$
   public final static String ARM_INTERFACE_NAME                           = "ARM Interface";                        //$NON-NLS-1$
   public final static String CFV1_INTERFACE_NAME                          = "Coldfire V1 Interface";                //$NON-NLS-1$
   public final static String CFVx_INTERFACE_NAME                          = "Coldfire V2,3,4 Interface";            //$NON-NLS-1$
   
   // Information about GDB interfaces (launching mostly)
   public enum InterfaceType {
      
      T_ARM   (TargetType.T_ARM,  ARM_INTERFACE_NAME,   UsbdmJniConstants.ARM_DEVICE_FILE,    USBDM_ARM_SERVER_OPTION ),
      T_CFV1  (TargetType.T_CFV1, CFV1_INTERFACE_NAME,  UsbdmJniConstants.CFV1_DEVICE_FILE,   USBDM_CFV1_SERVER_OPTION ),
      T_CFVX  (TargetType.T_CFVx, CFVx_INTERFACE_NAME,  UsbdmJniConstants.CFVX_DEVICE_FILE,   USBDM_CFVx_SERVER_OPTION ),
      ;
      private final String legibleName;
      public  final String deviceFile;
      public  final String gdbServerOption;
      public  final TargetType targetType;
      
      InterfaceType(TargetType targetType, String legibleName, String deviceFile, String gdbServerOption) {
         this.targetType      = targetType;
         this.legibleName     = legibleName;
         this.deviceFile      = deviceFile;
         this.gdbServerOption = gdbServerOption;
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
      
   public static ImageDescriptor getUsbdmIcon() {
      return Activator.getImageDescriptor(Activator.ID_USB_ICON_IMAGE);        //$NON-NLS-1$
   }

}
