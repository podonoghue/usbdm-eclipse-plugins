/*
 * Provides an interface that allows creating and monitoring of USBDM GDB socket-based server.
 */
package net.sourceforge.usbdm.gdb;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
import net.sourceforge.usbdm.gdb.GdbServerParameters;
import net.sourceforge.usbdm.jni.UsbdmException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * @since 4.10
 */
public class GdbServerInterface {

   Process             proc;
   GdbServerParameters serverParameters;

   public GdbServerInterface(GdbServerParameters serverParameters) {
      proc                  = null;
      this.serverParameters = serverParameters; 
   }
   
   /**
    *   Check for server by opening a brief connection on socket
    *   
    * @param portNum  The port to check on
    * 
    * @return         true => Server appears to be running
    */
   private boolean independentServerRunning(int portNum) {
      boolean isRunning = true;
      
      final Socket sock = new Socket();
      final int timeOut = (int)TimeUnit.SECONDS.toMillis(1); // 1 sec wait period
      try {
         // Open socket - only wait a short while
         sock.connect(new InetSocketAddress("localhost", portNum), timeOut);
      } catch (IOException e) {
         isRunning = false;
      }   
      try {
         // Immediately close it
         sock.close();
         try {
            // Give socket time to close?
            Thread.sleep(100);
         } catch (InterruptedException e) {
            // Ignore
         }
      } catch (IOException e) {
         // Ignore
      }
      return isRunning;
   }
   
   /**
    *   Checks if owned server is still running 
    *   
    * @return true if owned server is running
    */
   private boolean isServerRunning() {
      if (proc == null) {
//         System.out.println("proc == null\n")
         // No server started
         return false;
      }
      try {
         proc.exitValue();
         // Server has completed
//         System.out.println("proc rc = "+rc+"\n");
         return false;
      } catch (Exception e) {
         System.out.println("process exception \n" + e.toString());
         // Todo check e is 'expected' exception
         // No exit value so process is still running
         return true;
      }
   }

   /**
    *  Start USBDM GDB Server
    *  
    *  @param shell - The shell to open error dialogues on
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
      else if (independentServerRunning(serverParameters.getGdbPortNumber())) {
         MessageBox mbox = new MessageBox(shell,  SWT.ICON_WARNING|SWT.OK);
         mbox.setMessage("An independent server is already running on selected socket.\n" +
         		          "Another server cannot be started");
         mbox.setText("USBDM GDB Server Status");
         mbox.open();
         return;
      }
      try {
         ArrayList<String> commandList= serverParameters.getServerCommandLine();
         String[] commandArray = new String[commandList.size()];
         commandArray = commandList.toArray(commandArray);
//         for (String s : commandArray) { 
//            System.err.print(s + " ");
//         }
//         System.err.print("\n");

         Runtime rt = Runtime.getRuntime();
         proc = rt.exec(commandArray);
      } catch (Throwable t) {
         t.printStackTrace();
      }
      return;
   }

   /**
    *  Start USBDM GDB Server
    *  
    * @throws UsbdmException if server cannot be started
    * 
    * @note It is not considered an error if the server is already running 
    */
   public void startServer() throws UsbdmException {

//      System.err.println("GdbServerInterface.startServer()");
      if (isServerRunning()) {
//         System.err.println("GdbServerInterface.startServer() - Server already running");
//         throw new UsbdmException("Server already running");
         return;
      }
      else if (independentServerRunning(serverParameters.getGdbPortNumber())) {
//         System.err.println("GdbServerInterface.startServer() - Independent server already running");
//         throw new UsbdmException("Independent server already running");
         return;
      }
      try {
         ArrayList<String> commandList= serverParameters.getServerCommandLine();
         String[] commandArray = new String[commandList.size()];
         commandArray = commandList.toArray(commandArray);
//         for (String s : commandArray) { 
//            System.err.print(s + " ");
//         }
//         System.err.print("\n");

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
      
      final GdbServerParameters serverParameters  = GdbServerParameters.getDefaultServerParameters(InterfaceType.T_ARM);
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

