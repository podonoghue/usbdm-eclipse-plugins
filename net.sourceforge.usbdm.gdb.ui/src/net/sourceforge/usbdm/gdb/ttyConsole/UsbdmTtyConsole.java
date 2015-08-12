package net.sourceforge.usbdm.gdb.ttyConsole;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.IOConsole;

public class UsbdmTtyConsole extends IOConsole {
   protected int fPortNum;
   
   public UsbdmTtyConsole(String name, ImageDescriptor imageDescriptor, int portNum) {
      super(name, imageDescriptor);
      fPortNum = portNum;
   }

   @Override
   protected void dispose() {
      MyConsoleInterface.stopServer(fPortNum);
      super.dispose();
   }

   @Override
   protected void init() {
      super.init();
   }

}
