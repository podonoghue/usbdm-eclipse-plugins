package net.sourceforge.usbdm.cdt.tools;

public class ArmArchiverCommandLineGenerator extends GccCommandLineGenerator {
   
   // Shared flags that are added to the command line 
   private static final String optionKeys[] = {
   };
   
   public ArmArchiverCommandLineGenerator() {
      super(UsbdmConstants.ARM_BUILDTOOLS_OPTIONS, optionKeys);
   }
}
