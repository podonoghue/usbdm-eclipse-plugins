package net.sourceforge.usbdm.gdb.ui;

import org.eclipse.cdt.launch.ui.CMainTab;

public class UsbdmMainTab extends CMainTab {

   private static final String ID = "net.sourceforge.usbdm.gdb.CMainTab";
   
   public UsbdmMainTab() {
      super();
   }

   @Override
   public String getId() {
      
      return ID;
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
   }
}
