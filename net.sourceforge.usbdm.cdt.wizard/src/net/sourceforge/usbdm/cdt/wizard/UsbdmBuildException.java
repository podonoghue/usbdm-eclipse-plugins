package net.sourceforge.usbdm.cdt.wizard;

import org.eclipse.cdt.managedbuilder.core.BuildException;

public class UsbdmBuildException extends BuildException {
   private static final long serialVersionUID = 2323156808760169607L;
   
   public UsbdmBuildException(String reason) {
      super(reason);
   }
}
