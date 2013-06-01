package net.sourceforge.usbdm.cdt.wizard;

import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.IValueVariableInitializer;

public class CodesourceryMakeVariableInitializer implements
      IValueVariableInitializer {

   @Override
   public void initialize(IValueVariable variable) {
      String os = System.getProperty("os.name");
      if ((os != null) && os.toUpperCase().contains("LINUX")) {
         variable.setValue("make");
      }
      else {
         variable.setValue("cs-make");
      }
      variable.setDescription("Make command for Codesourcery tools");
   }

}
