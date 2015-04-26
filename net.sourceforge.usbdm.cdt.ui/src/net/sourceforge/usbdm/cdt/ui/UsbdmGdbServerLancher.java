package net.sourceforge.usbdm.cdt.ui;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.jni.Usbdm;

import org.eclipse.core.runtime.IPath;

public class UsbdmGdbServerLancher {

   static Process proc;

   private static boolean isRunning() {
      if (proc == null) {
         return false;
      }
      try {
         proc.exitValue();
         return false;
      } catch (Exception e) {
         return true;
      }
   }
   
   private static IPath getServerPath(InterfaceType serverType) {
      IPath serverPath = Usbdm.getApplicationPath();
      return serverPath.append(UsbdmSharedConstants.USBDM_GDB_GUI_SERVER);
   }
         
   /**
    *  Start USBDM GDB Server
    */
   public static Object execute() {

      if (isRunning()) {
//         IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
//         MessageBox mbox = new MessageBox(window.getShell(),  SWT.ICON_QUESTION|SWT.YES |SWT.NO);
//         mbox.setMessage("Server appears to be already running.\n" +
//                         "Force restart?");
//         mbox.setText("USBDM GDB Server Status");
//         int yesNo = mbox.open();
//         if (yesNo == SWT.YES) {
//            proc.destroy();
//         }
//         else {
//            return null;
//         }
      }
      try {
         Runtime rt = Runtime.getRuntime();
         String commandArray[] = {getServerPath(InterfaceType.T_ARM).toPortableString()};
         System.err.println(commandArray[0]);
         proc = rt.exec(commandArray);
      } catch (Throwable t) {
         t.printStackTrace();
      }
      return null;
   }

   /**
    * @param args
    */
   public static void main(String[] args) {

      execute();
   }
      
}
