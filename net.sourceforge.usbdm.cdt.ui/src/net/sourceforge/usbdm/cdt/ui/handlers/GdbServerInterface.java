package net.sourceforge.usbdm.cdt.ui.handlers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class GdbServerInterface {

   Process             proc;
   GdbServerParameters serverParameters;

   GdbServerInterface(GdbServerParameters serverParameters) {
      proc                  = null;
      this.serverParameters = serverParameters; 
   }
   
   private boolean independentServerRunning(int portNum) {
      boolean success = true;
      
      final Socket sock = new Socket();
      final int timeOut = (int)TimeUnit.SECONDS.toMillis(1); // 1 sec wait period
      try {
         sock.connect(new InetSocketAddress("localhost", portNum), timeOut);
      } catch (IOException e) {
         success = false;
      }   
      try {
         sock.close();
      } catch (IOException e) {
         // Ignore
      }
      return success;
   }
   
   private boolean isServerRunning() {
      if (proc == null) {
//         System.out.println("proc == null\n");
         return false;
      }
      try {
         proc.exitValue();
//         System.out.println("proc rc = "+rc+"\n");
         return false;
      } catch (Exception e) {
//         System.out.println("proc exception\n");
         return true;
      }
   }

   /**
    *  Start USBDM GDB Server
    */
   public void startServer(Shell shell) {

      if (isServerRunning()) {
         MessageBox mbox = new MessageBox(shell,  SWT.ICON_QUESTION|SWT.YES |SWT.NO);
         mbox.setMessage("A controlled server is already running.\n" +
                         "Force restart?");
         mbox.setText("USBDM GDB Server Status");
         int yesNo = mbox.open();
         if (yesNo == SWT.YES) {
            proc.destroy();
            proc = null;
         }
         else {
            return;
         }
      }
      else {
         if (independentServerRunning(serverParameters.getGdbPortNumber())) {
            MessageBox mbox = new MessageBox(shell,  SWT.ICON_WARNING|SWT.OK);
            mbox.setMessage("An independent server is already running on selected socket.\n" +
            		          "Another server cannot be started");
            mbox.setText("USBDM GDB Server Status");
            mbox.open();
            return;
         }
      }
      try {
         String commandArray[] = serverParameters.getServerCommandLine();
         for (String s : commandArray) { 
            System.err.print(s + " ");
         }
         System.err.print("\n");

         Runtime rt = Runtime.getRuntime();
         proc = rt.exec(commandArray);
      } catch (Throwable t) {
         t.printStackTrace();
      }
      return;
   }

   /**
    *  Stop USBDM GDB Server
    */
   public void stopServer(Shell shell) {

      if (!isServerRunning()) {
         if (independentServerRunning(serverParameters.getGdbPortNumber())) {
            MessageBox mbox = new MessageBox(shell,  SWT.ICON_QUESTION|SWT.OK);
            mbox.setMessage("An independent server is running on the selected socket.\n" +
            		          "This server cannot be stopped from Eclipse.");
            mbox.setText("USBDM GDB Server Status");
            mbox.open();
            return;
         }
         MessageBox mbox = new MessageBox(shell,  SWT.ICON_INFORMATION|SWT.OK);
         mbox.setMessage("There are no servers running.");
         mbox.setText("USBDM GDB Server Status");
         mbox.open();
         return;
      }
      if (proc != null) {
         proc.destroy();
         proc = null;
      }
      return;
   }

   /**
    *  Close - this will only stop the server it created
    */
   public void dispose(Shell shell) {
      if (proc != null) {
         proc.destroy();
         proc = null;
      }
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      final Shell shell = new Shell(display);
      
      // Layout manager handle the layout
      // of the widgets in the container
      shell.setLayout(new FillLayout());
      shell.setSize(200, 100);
      
      final GdbServerParameters serverParameters  = new GdbServerParameters.ArmGdbServerParameters();
      final GdbServerInterface gdbServerInterface = new GdbServerInterface(serverParameters);
      serverParameters.setDeviceName("MK20DX128M5");
      
      Button startButton =  new Button(shell, SWT.PUSH);
      startButton.setText("Start Server");
      startButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            gdbServerInterface.startServer(shell);
         }
      }); 
      Button stopButton =  new Button(shell, SWT.PUSH);
      stopButton.setText("Stop Server");
      stopButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            gdbServerInterface.stopServer(shell);
         }
      }); 
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      gdbServerInterface.dispose(shell);
      display.dispose();      
   }

}

