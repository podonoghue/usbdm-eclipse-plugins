package net.sourceforge.usbdm.cdt.wizard;

public class UsbdmConstants {
   final static String PAGE_ID = "net.sourceforge.usbdm.cdt.wizardPage"; //$NON-NLS-1$

   final static String GCC_COMMAND_LINUX   = "gcc";             //$NON-NLS-1$
   final static String GCC_COMMAND_WINDOWS = "gcc.exe";         //$NON-NLS-1$

   final static String LINKER_MEMORY_MAP_COLDFIRE_V1 = 
         "  /* Default Map - Unknow device */\n" +              //$NON-NLS-1$
         "  rom (rx)  : ORIGIN = 0x00000000, LENGTH = 128K\n"+  //$NON-NLS-1$
         "  ram (rwx) : ORIGIN = 0x00800000, LENGTH = 24K\n"+   //$NON-NLS-1$
         "  gpio (rw) : ORIGIN = 0x00c00000, LENGTH = 16\n"+    //$NON-NLS-1$
         "  io (rw)   : ORIGIN = 0x00ff8000, LENGTH = 32K\n";   //$NON-NLS-1$
   final static String LINKER_MEMORY_MAP_COLDFIRE_Vx = 
         "  /* Default Map - Unknow device  */\n" +             //$NON-NLS-1$
         "  rom (rx)  : ORIGIN = 0x00000000, LENGTH = 128K\n"+  //$NON-NLS-1$
         "  ram (rwx) : ORIGIN = 0x00800000, LENGTH = 24K\n"+   //$NON-NLS-1$
         "  gpio (rw) : ORIGIN = 0x00c00000, LENGTH = 16\n"+    //$NON-NLS-1$
         "  io (rw)   : ORIGIN = 0x00ff8000, LENGTH = 32K\n";   //$NON-NLS-1$
   final static String LINKER_MEMORY_MAP_COLDFIRE_KINETIS = 
         "  /* Default Map - Unknow device  */\n" +             //$NON-NLS-1$
         "  rom (rx)  : ORIGIN = 0x00000000, LENGTH = 128K\n"+  //$NON-NLS-1$
         "  ram (rwx) : ORIGIN = 0x00800000, LENGTH = 24K\n"+   //$NON-NLS-1$
         "  gpio (rw) : ORIGIN = 0x00c00000, LENGTH = 16\n"+    //$NON-NLS-1$
         "  io (rw)   : ORIGIN = 0x00ff8000, LENGTH = 32K\n";   //$NON-NLS-1$
   
   // These constants are used for page data map keys
   final static String USBDM_DEBUG_MODE_KEY             = "usbdmDebugMode";         //$NON-NLS-1$
   final static String GDB_COMMAND_KEY                  = "gdbCommand";             //$NON-NLS-1$
   final static String USBDM_GDB_SPRITE_KEY             = "usbdmGdbSprite";         //$NON-NLS-1$
   final static String TARGET_DEVICE_FAMILY_KEY         = "targetDeviceFamily";     //$NON-NLS-1$
   final static String TARGET_DEVICE_SUBFAMILY_KEY      = "targetDeviceSubFamily";  //$NON-NLS-1$
   final static String TARGET_DEVICE_NAME_KEY           = "targetDeviceName";       //$NON-NLS-1$
   final static String LINKER_MEMORY_MAP_KEY            = "linkerMemoryMap";        //$NON-NLS-1$
   final static String EXTERNAL_HEADER_FILE_KEY         = "externalHeaderFile";     //$NON-NLS-1$
   final static String EXTERNAL_VECTOR_TABLE_KEY        = "externalVectorTable";    //$NON-NLS-1$
   final static String C_DEVICE_PARAMETERS              = "cDeviceParameters";      //$NON-NLS-1$
   
   // These constants are used both for the dialogue persistent storage AND the page data map keys
   final static String TARGET_DEVICE_KEY                = "targetDevice";               //$NON-NLS-1$
   final static String EXTERNAL_LINKER_SCRIPT_KEY       = "externalLinkerScript";       //$NON-NLS-1$
   final static String DONT_GENERATE_LINKER_SCRIPT_KEY  = "dontGenerateLinkerScript";   //$NON-NLS-1$ 
   
   final static String SHARED_DEFAULTS_PREFIX_KEY       = "prefix";                 //$NON-NLS-1$
   final static String SHARED_DEFAULTS_PATH_KEY         = "path";                   //$NON-NLS-1$
   final static String CROSS_COMMAND_PREFIX_KEY         = "crossCommandPrefix";     //$NON-NLS-1$
   final static String CROSS_COMMAND_PATH_KEY           = "crossCommandPath";       //$NON-NLS-1$

   // Locations to look for device stationery
   final static String STATIONERY_PATH     = "Stationery/";                         //$NON-NLS-1$
   final static String PROJECT_HEADER_PATH = STATIONERY_PATH+"Project_Headers/";    //$NON-NLS-1$
   final static String VECTOR_TABLE_PATH   = STATIONERY_PATH+"Vector_Table/";       //$NON-NLS-1$
   
   final static String SUB_FAMILY_CORTEX_M4 = "CortexM4";
   final static String SUB_FAMILY_CFV1      = "CFV1";
   final static String SUB_FAMILY_CFV1_PLUS = "CFV1Plus";
   final static String SUB_FAMILY_CFV2      = "CFV2";
   
   // These keys are used in the project options
   public final static String USBDM_GCC_PATH_OPTION_KEY          = "net.sourceforge.usbdm.cdt.toolchain.cross.codesourceryPath";
   public final static String USBDM_GCC_PREFIX_OPTION_KEY        = "net.sourceforge.usbdm.cdt.toolchain.cross.prefix";
   public final static String USBDM_GCC_MCPU_OPTION_KEY          = "net.sourceforge.usbdm.cdt.toolchain.processor.mcpu";
   public final static String USBDM_GCC_MTHUMB_OPTION_KEY        = "net.sourceforge.usbdm.cdt.toolchain.processor.mthumb";

   public final static String USBDM_GCC_DEBUG_LEVEL_OPTION_KEY   = "net.sourceforge.usbdm.cdt.toolchain.debug.debugLevel";
   public final static String USBDM_GCC_DEBUG_FORMAT_OPTION_KEY  = "net.sourceforge.usbdm.cdt.toolchain.debug.mdebugformat";
   public final static String USBDM_GCC_DEBUG_OTHER_OPTION_KEY   = "net.sourceforge.usbdm.cdt.toolchain.debug.other";
   public final static String USBDM_GCC_DEBUG_PROF_OPTION_KEY    = "net.sourceforge.usbdm.cdt.toolchain.debug.prof";
   public final static String USBDM_GCC_DEBUG_GPROF_OPTION_KEY   = "net.sourceforge.usbdm.cdt.toolchain.debug.gprof";

      // These keys are used in the templates
   public final static String CODESOURCERY_ARM_PATH_KEY        = "codesourcery_arm_path";
   public final static String CODESOURCERY_ARM_PREFIX_KEY      = "codesourcery_arm_prefix";
   public final static String CODESOURCERY_COLDFIRE_PATH_KEY   = "codesourcery_coldfire_path";
   public final static String CODESOURCERY_COLDFIRE_PREFIX_KEY = "codesourcery_coldfire_prefix";
   public final static String CODESOURCERY_MAKE_COMMAND_KEY    = "codesourcery_make_command";

}
