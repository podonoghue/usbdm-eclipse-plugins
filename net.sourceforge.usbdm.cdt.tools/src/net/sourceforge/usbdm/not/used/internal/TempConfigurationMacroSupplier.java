package net.sourceforge.usbdm.not.used.internal;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacro;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.macros.IConfigurationBuildMacroSupplier;

public class TempConfigurationMacroSupplier implements
      IConfigurationBuildMacroSupplier {

   public TempConfigurationMacroSupplier() {
   }

   @Override
   public IBuildMacro getMacro(String              macroName, 
                               IConfiguration      configuration,
                               IBuildMacroProvider provider) {
   
      if (macroName.equals("USBDM_MACRO")) {
         IBuildMacro xx = new UsbdmBuildMacro();
         return xx;
      }
      return null;
   }

   @Override
   public IBuildMacro[] getMacros(IConfiguration      configuration,
                                  IBuildMacroProvider provider) {
      
      return null;
   }

   class UsbdmBuildMacro implements IBuildMacro {

      /* (non-Javadoc)
       * @see org.eclipse.cdt.core.cdtvariables.ICdtVariable#getName()
       */
      @Override
      public String getName() {
         return "USBDM_MACRO";
      }

      /* (non-Javadoc)
       * @see org.eclipse.cdt.core.cdtvariables.ICdtVariable#getValueType()
       */
      @Override
      public int getValueType() {
         return 100;
      }

      /* (non-Javadoc)
       * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacro#getMacroValueType()
       */
      @Override
      public int getMacroValueType() {
         return 0;
      }

      /* (non-Javadoc)
       * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacro#getStringValue()
       */
      @Override
      public String getStringValue() throws BuildMacroException {
         return "a macro";
      }

      /* (non-Javadoc)
       * @see org.eclipse.cdt.managedbuilder.macros.IBuildMacro#getStringListValue()
       */
      @Override
      public String[] getStringListValue() throws BuildMacroException {
         return new String[] { getStringValue() };
      }
      
   }
}
