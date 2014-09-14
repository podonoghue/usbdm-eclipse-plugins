package net.sourceforge.usbdm.cdt.tools;

public class ArmLinkerCommandLineGenerator extends GccCommandLineGenerator {
   
   // Shared flags that are added to the command line 
   private static final String optionKeys[] = {
      UsbdmConstants.USBDM_GCC_PROC_ARM_MCPU_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_ARM_MTHUMB_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_ARM_MFLOAT_ABI_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_ARM_MFPU_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_PROC_ARM_FSHORT_DOUBLE_OPTION_KEY,
      
      UsbdmConstants.USBDM_GCC_DEBUG_LEVEL_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_FORMAT_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_OTHER_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_PROF_OPTION_KEY,
      UsbdmConstants.USBDM_GCC_DEBUG_GPROF_OPTION_KEY,
      
      UsbdmConstants.USBDM_GCC_OPTIMIZATION_LEVEL_OPTION_KEY,         
      UsbdmConstants.USBDM_GCC_OPTIMIZATION_LINKEROTHER_OPTION_KEY,   
      UsbdmConstants.USBDM_GCC_OPTIMIZATION_DEADCODE_OPTION_KEY,      
   };
   
   public ArmLinkerCommandLineGenerator() {
      super(UsbdmConstants.ARM_BUILDTOOLS_OPTIONS, optionKeys);
   }
}
