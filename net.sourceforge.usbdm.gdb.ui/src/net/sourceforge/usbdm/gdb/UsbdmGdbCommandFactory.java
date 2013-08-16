package net.sourceforge.usbdm.gdb;

import org.eclipse.cdt.debug.mi.core.command.CommandFactory;

/**
 *  Wrapper for default GDB CommandFactory 
 *
 */
public class UsbdmGdbCommandFactory extends CommandFactory {

   /**
    * 
    */
   public UsbdmGdbCommandFactory() {
   }

   /**
    * @param miVersion
    */
   public UsbdmGdbCommandFactory(String miVersion) {
      super(miVersion);
   }

}

