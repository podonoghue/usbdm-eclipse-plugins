package net.sourceforge.usbdm.cdt.tools;

public class ColdfireArchiverCommandLineGenerator extends GccCommandLineGenerator {
 
   // Shared flags that are added to the command line 
   private static final String optionKeys[] = {
   };
   
   public ColdfireArchiverCommandLineGenerator() {
      super(UsbdmConstants.COLDFIRE_BUILDTOOLS_OPTIONS, optionKeys);
   }
}
