package net.sourceforge.usbdm.cdt.tools;

public class ColdfireGccCommandLineGenerator extends GccCommandLineGenerator {
 
   // Shared flags that are added to the command line 
   private static final String optionKeys[] = {
      UsbdmConstants.USBDM_GCC_PROC_COLDFIRE_MCPU_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_COLDFIRE_MFLOAT_ABI_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_COLDFIRE_MFPU_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_COLDFIRE_FSHORT_DOUBLE_OPTION_KEY,
      
      UsbdmConstants.USBDM_GCC_DEBUG_LEVEL_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_FORMAT_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_OTHER_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_PROF_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_GPROF_OPTION_KEY,
      
      UsbdmConstants.USBDM_GCC_OPTIMIZATION_LEVEL_OPTION_KEY,         
      UsbdmConstants.USBDM_GCC_OPTIMIZATION_PACKSTRUCTS_OPTION_KEY,   
      UsbdmConstants.USBDM_GCC_OPTIMIZATION_SHORTENUMS_OPTION_KEY,    
      UsbdmConstants.USBDM_GCC_OPTIMIZATION_FUNCTION_OPTION_KEY,      
      UsbdmConstants.USBDM_GCC_OPTIMIZATION_DATA_OPTION_KEY,          
      UsbdmConstants.USBDM_GCC_OPTIMIZATION_NORTTI_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_OPTIMIZATION_COMPILEROTHER_OPTION_KEY, 
   };
   
   public ColdfireGccCommandLineGenerator() {
      super(UsbdmConstants.COLDFIRE_BUILDTOOLS_OPTIONS, optionKeys);
   }
}
