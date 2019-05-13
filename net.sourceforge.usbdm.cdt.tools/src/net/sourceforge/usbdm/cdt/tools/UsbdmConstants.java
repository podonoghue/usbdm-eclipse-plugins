package net.sourceforge.usbdm.cdt.tools;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;

public class UsbdmConstants {
   public final static String EXE_PROJECT_TYPE_ID              = "net.sourceforge.usbdm.cdt.newProjectType.exe";                 //$NON-NLS-1$
   public final static String STATIC_LIB_PROJECT_TYPE_ID       = "net.sourceforge.usbdm.cdt.newProjectType.staticLib";           //$NON-NLS-1$

//   public final static String PROJECT_OPTIONS_PAGE_ID          = "net.sourceforge.usbdm.cdt.wizardPage.UsbdmProjectOptionsPage"; //$NON-NLS-1$
   public final static String PROJECT_OPTIONS_PAGE_NAME        = "UsbdmProjectOptionsPage";                                      //$NON-NLS-1$
   public final static String CMSIS_OPTIONS_PAGE_ID            = "net.sourceforge.usbdm.cdt.wizardPage.CMSISOptionsPage";        //$NON-NLS-1$
   public final static String CMSIS_OPTIONS_PAGE_NAME          = "CMSISOptionsPage";                                             //$NON-NLS-1$

   public final static String GCC_COMMAND_LINUX                = "gcc";                    //$NON-NLS-1$
   public final static String GCC_COMMAND_WINDOWS              = "gcc.exe";                //$NON-NLS-1$
   
   // These constants are used for project template and paramMap keys
   public final static String PROJECT_ARTIFACT_KEY                  = "projectArtifactName";       //$NON-NLS-1$
   public final static String BIN_PATH_KEY                          = "binPath";                   //$NON-NLS-1$
   public final static String PROJECT_HOME_PATH_KEY                 = "projectHomePath";           //$NON-NLS-1$
   public final static String PROJECT_OUTPUT_TYPE_KEY               = "outputType";                //$NON-NLS-1$
   public final static String PROJECT_NAME_KEY                      = "projectName";               //$NON-NLS-1$
   public final static String INTERFACE_TYPE_KEY                    = "interfaceType";             //$NON-NLS-1$
   public final static String SEMI_HOSTING_TYPE_KEY                 = "semiHosting";               //$NON-NLS-1$
   public final static String TARGET_DEVICE_KEY                     = "targetDevice";              //$NON-NLS-1$
   public final static String TARGET_DEVICE_NAME_KEY                = "targetDeviceName";          //$NON-NLS-1$
   public final static String HAS_CC_NATURE_KEY                     = "hasCCNature";               //$NON-NLS-1$
   public final static String LIBRARY_PROJECT_KEY                   = "libraryProject";            //$NON-NLS-1$
   public final static String BUILD_TOOLS_ID_KEY                    = "buildToolsId";              //$NON-NLS-1$
   public final static String BUILD_TOOLS_BIN_PATH_KEY              = "buildToolsBinPath";         //$NON-NLS-1$
   public final static String GDB_COMMAND_KEY                       = "gdbCommand";                //$NON-NLS-1$
   public final static String USBDM_DEBUG_MODE_KEY                  = "usbdmDebugMode";            //$NON-NLS-1$
   public final static String USBDM_GDB_SPRITE_KEY                  = "usbdmGdbSprite";            //$NON-NLS-1$
   public final static String TARGET_DEVICE_SUBFAMILY_KEY           = "targetDeviceSubFamily";     //$NON-NLS-1$
   public final static String TARGET_DEVICE_FAMILY_KEY              = "targetDeviceFamily";        //$NON-NLS-1$
   public final static String LINKER_INFORMATION_KEY                = "linkerInformation";         //$NON-NLS-1$
   public final static String LINKER_FLASH_SIZE_KEY                 = "linkerFlashSize";           //$NON-NLS-1$
   public final static String LINKER_RAM_SIZE_KEY                   = "linkerRamSize";             //$NON-NLS-1$
   public final static String LINKER_STACK_SIZE_KEY                 = "linkerStackSize";           //$NON-NLS-1$
   public final static String LINKER_HEAP_SIZE_KEY                  = "linkerHeapSize";            //$NON-NLS-1$
   public final static String LINKER_EXTRA_REGION_KEY               = "linkerExtraRegions";        //$NON-NLS-1$
   public final static String EXTERNAL_HEADER_FILE_KEY              = "externalHeaderFile";        //$NON-NLS-1$
   public final static String EXTERNAL_HEADER_FILENAME_KEY          = "externalHeaderFilename";    //$NON-NLS-1$
   public final static String ERASE_METHOD_KEY                      = "eraseMethod";               //$NON-NLS-1$
   public final static String RESET_METHOD_KEY                      = "resetMethod";               //$NON-NLS-1$
   public final static String EXTERNAL_VECTOR_TABLE_KEY             = "externalVectorTable";       //$NON-NLS-1$
   public final static String C_DEVICE_PARAMETERS_KEY               = "cDeviceParameters";         //$NON-NLS-1$
   public final static String USBDM_APPLICATION_PATH_KEY            = "usbdmApplicationPath";      //$NON-NLS-1$
   public final static String CLOCK_TRIM_FREQUENCY_KEY              = "clockTrimFrequency";        //$NON-NLS-1$
   public final static String NVM_CLOCK_TRIM_LOCATION_KEY           = "nvmClockTrimLocation";      //$NON-NLS-1$
   public final static String PATH_SEPARATOR_KEY                    = "pathSeparator";             //$NON-NLS-1$
   public final static String CONDITION_PREFIX_KEY                  = "projectOptionValue";        //$NON-NLS-1$
   public final static String FILE_COPY_LIST                        = "fileCopyList";              //$NON-NLS-1$
   public final static String C_VECTOR_TABLE_KEY                    = "cVectorTable";              //$NON-NLS-1$
   public final static String C_VECTOR_TABLE_INCLUDES_KEY           = "VectorsIncludeFiles";       //$NON-NLS-1$
   public final static String USE_FLOATINGPOINT_IN_SCANF_KEY        = "useFloatingpointInScanf";   //$NON-NLS-1$
   public final static String USE_FLOATINGPOINT_IN_PRINTF_KEY       = "useFloatingpointInPrintf";  //$NON-NLS-1$
   
   public final static String EXCLUDED_FILES_PATTERN_KEY            = "excludedFilesPattern";      //$NON-NLS-1$
   public final static String LINKER_FILE_KEY                       = "linkerFile";                //$NON-NLS-1$
   public final static String ARM_LTD_STARTUP_S_FILE_KEY            = "startup_ARMLtdGCC_S";       //$NON-NLS-1$
   
   // These constants are used both for the dialogue persistent storage AND the page data map keys
   static final String SHARED_DEFAULTS_PREFIX_KEY              = "prefix";                 //$NON-NLS-1$
   static final String SHARED_DEFAULTS_PATH_KEY                = "path";                   //$NON-NLS-1$

   // Locations to look for device stationery
   final static String STATIONERY_PATH                         = "Stationery/";                         //$NON-NLS-1$
   public final static String PROJECT_HEADER_PATH              = STATIONERY_PATH+"Project_Headers/";    //$NON-NLS-1$
   public final static String VECTOR_TABLE_PATH                = STATIONERY_PATH+"Vector_Table/";       //$NON-NLS-1$
   
   public final static String SUB_FAMILY_CORTEX_M4             = "CortexM4";
   public final static String SUB_FAMILY_CFV1                  = "CFV1";
   public final static String SUB_FAMILY_CFV1_PLUS             = "CFV1Plus";
   public final static String SUB_FAMILY_CFV2                  = "CFV2";
   
   // These keys are used in the project options
   public final static String USBDM_GCC_PATH_OPTION_KEY                        = "net.sourceforge.usbdm.cdt.toolchain.cross.codesourceryPath";
   public final static String USBDM_GCC_PREFIX_OPTION_KEY                      = "net.sourceforge.usbdm.cdt.toolchain.cross.prefix";

   public final static String USBDM_GCC_PROC_ARM_MCPU_OPTION_KEY               = "net.sourceforge.usbdm.cdt.arm.toolchain.mcpu";
   public final static String USBDM_GCC_PROC_ARM_MTHUMB_OPTION_KEY             = "net.sourceforge.usbdm.cdt.arm.toolchain.mthumb";
   public final static String USBDM_GCC_PROC_ARM_MFLOAT_ABI_OPTION_KEY         = "net.sourceforge.usbdm.cdt.arm.toolchain.mfloat-abi";
   public final static String USBDM_GCC_PROC_ARM_MFPU_OPTION_KEY               = "net.sourceforge.usbdm.cdt.arm.toolchain.mfpu";
   public final static String USBDM_GCC_PROC_ARM_FSHORT_DOUBLE_OPTION_KEY      = "net.sourceforge.usbdm.cdt.arm.toolchain.short-double";

   public final static String USBDM_GCC_PROC_COLDFIRE_MCPU_OPTION_KEY          = "net.sourceforge.usbdm.cdt.coldfire.toolchain.mcpu";
   public final static String USBDM_GCC_PROC_COLDFIRE_MFLOAT_ABI_OPTION_KEY    = "net.sourceforge.usbdm.cdt.coldfire.toolchain.mfloat-abi";
   public final static String USBDM_GCC_PROC_COLDFIRE_MFPU_OPTION_KEY          = "net.sourceforge.usbdm.cdt.coldfire.toolchain.mfpu";
   public final static String USBDM_GCC_PROC_COLDFIRE_FSHORT_DOUBLE_OPTION_KEY = "net.sourceforge.usbdm.cdt.coldfire.toolchain.short-double";

   public final static String USBDM_GCC_DEBUG_LEVEL_OPTION_KEY                 = "net.sourceforge.usbdm.cdt.toolchain.debug.debugLevel";
   public final static String USBDM_GCC_DEBUG_FORMAT_OPTION_KEY                = "net.sourceforge.usbdm.cdt.toolchain.debug.mdebugformat";
   public final static String USBDM_GCC_DEBUG_OTHER_OPTION_KEY                 = "net.sourceforge.usbdm.cdt.toolchain.debug.other";
   public final static String USBDM_GCC_DEBUG_PROF_OPTION_KEY                  = "net.sourceforge.usbdm.cdt.toolchain.debug.prof";
   public final static String USBDM_GCC_DEBUG_GPROF_OPTION_KEY                 = "net.sourceforge.usbdm.cdt.toolchain.debug.gprof";
   public final static String USBDM_GCC_DEBUG_SEMIHOSTING_OPTION_KEY           = "net.sourceforge.usbdm.cdt.toolchain.debug.rdimon";

   public final static String USBDM_GCC_OPTIMIZATION_LEVEL_OPTION_KEY          = "net.sourceforge.usbdm.cdt.toolchain.optimization.level";
   public final static String USBDM_GCC_OPTIMIZATION_PACKSTRUCTS_OPTION_KEY    = "net.sourceforge.usbdm.cdt.toolchain.optimization.packStructs";
   public final static String USBDM_GCC_OPTIMIZATION_SHORTENUMS_OPTION_KEY     = "net.sourceforge.usbdm.cdt.toolchain.optimization.shortEnumerations";
   public final static String USBDM_GCC_OPTIMIZATION_FUNCTION_OPTION_KEY       = "net.sourceforge.usbdm.cdt.toolchain.optimization.functionSections";
   public final static String USBDM_GCC_OPTIMIZATION_DATA_OPTION_KEY           = "net.sourceforge.usbdm.cdt.toolchain.optimization.dataSections";
   public final static String USBDM_GCC_OPTIMIZATION_NORTTI_OPTION_KEY         = "net.sourceforge.usbdm.cdt.toolchain.optimization.noRTTI";
   public final static String USBDM_GCC_OPTIMIZATION_DEADCODE_OPTION_KEY       = "net.sourceforge.usbdm.cdt.toolchain.optimization.deadCodeElimination";
   public final static String USBDM_GCC_OPTIMIZATION_COMPILEROTHER_OPTION_KEY  = "net.sourceforge.usbdm.cdt.toolchain.optimization.compilerOther";
   public final static String USBDM_GCC_OPTIMIZATION_LINKEROTHER_OPTION_KEY    = "net.sourceforge.usbdm.cdt.toolchain.optimization.linkerOther";

   public final static String USBDM_GCC_WARNING_PEDANTIC_OPTION_KEY            = "net.sourceforge.usbdm.cdt.toolchain.warnings.pedantic";
   public final static String USBDM_GCC_WARNING_ALLWARN_OPTION_KEY             = "net.sourceforge.usbdm.cdt.toolchain.warnings.allwarn"; 
   public final static String USBDM_GCC_WARNING_EXTRAWARN_OPTION_KEY           = "net.sourceforge.usbdm.cdt.toolchain.warnings.extrawarn"; 
   public final static String USBDM_GCC_WARNING_WCONVERSION_OPTION_KEY         = "net.sourceforge.usbdm.cdt.toolchain.warnings.wconversion"; 

   // These keys are used as keys to CDT Variables                 
   public final static String ARM_BUILDTOOLS_OPTIONS               = UsbdmSharedConstants.USBDM_ARM_BUILD_TOOL_ID;
   public final static String COLDFIRE_BUILDTOOLS_OPTIONS          = UsbdmSharedConstants.USBDM_COLDFIRE_BUILD_TOOL_ID;
   
   public final static String PATH_NOT_SET      = UsbdmSharedConstants.PATH_NOT_SET;
   public final static String COMMAND_NOT_SET   = UsbdmSharedConstants.COMMAND_NOT_SET;
   public final static String PREFIX_NOT_SET    = UsbdmSharedConstants.PREFIX_NOT_SET;

   // Fixed paths within created projects
//   public final static String PROJECT_PROJECT_HEADERS_PATH  = "Project_Headers";
//   public final static String PROJECT_PROJECT_SETTINGS_PATH = "Project_Settings";
//   public final static String PROJECT_SOURCES_PATH          = "Sources";
//   public final static String PROJECT_SNIPPETS_PATH         = "Snippets";
   public final static String PROJECT_START_UP_CODE_PATH    = "Startup_Code";
   public final static String PROJECT_VECTOR_C_PATH         = PROJECT_START_UP_CODE_PATH+"/vectors.c";
   public final static String PROJECT_VECTOR_CPP_PATH       = PROJECT_START_UP_CODE_PATH+"/vectors.cpp";
   public final static String PROJECT_INCLUDE_FOLDER        = "Project_Headers";
   public final static String PROJECT_SOURCES_FOLDER        = "Sources";
   
}
