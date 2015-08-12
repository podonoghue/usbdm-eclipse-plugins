package net.sourceforge.usbdm.cdt.ui.handlers;

import net.sourceforge.usbdm.gdb.ttyConsole.MyConsoleInterface;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class OpenGdbConsoleHandler extends AbstractHandler {

   static String portNum = "4321";
   
   public Object execute(ExecutionEvent event) throws ExecutionException {
      IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
      InputDialog      dialog = new InputDialog(
            window.getShell(), "TTY Port Number", "Port number for TTY console", portNum, new IInputValidator() {
         @Override
         public String isValid(String arg0) {
            try {
               int num = Integer.parseInt(arg0);
               if (num <= 0) {
                  return "Must not be zero";
               }
               if (num >= 65536) {
                  return "Too large";
               }
            } catch (Exception e) {
               return "Invalid integer";
            }
            return null;
         }
      });
      int rc = dialog.open();
      if (rc != InputDialog.OK) {
         return null;
      }
      portNum = dialog.getValue();
      try {
         MyConsoleInterface.startServer(Integer.parseInt(portNum));
      } catch (Exception e) {
         MessageBox mb = new MessageBox(window.getShell());
         mb.setMessage(e.getMessage());
         mb.setText("Error opening TTY");
         mb.open();
      }
      return null;
   }
}
