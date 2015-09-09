package net.sourceforge.usbdm.gdb.ttyConsole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * @since 4.12
 */
public class MyConsoleInterface {

   public static final String    CONSOLE_NAME = "Usbdm GDB Semihosting Console (Port #%d)";

   static final String HOST_NAME    = "localhost";
   static final HashMap<Integer, Server> servers = new HashMap<Integer, MyConsoleInterface.Server>();
   
   /**
    * Locates TTY console for given port number. Creates one if necessary
    * 
    * @return console found
    */
   static public UsbdmTtyConsole getConsole(int ttyPortNum) {
      String           consoleName  = String.format(CONSOLE_NAME, ttyPortNum);
      UsbdmTtyConsole  console      = null;
      ConsolePlugin    plugin       = ConsolePlugin.getDefault();
      IConsoleManager  conMan       = plugin.getConsoleManager();
      
      IConsole[] existing = conMan.getConsoles();
      for (int i = 0; i < existing.length; i++) {
         if (existing[i].getName().equals(consoleName)) {
            console = (UsbdmTtyConsole) existing[i];
            console.activate();
            return console;
         }
      }
      // No console found, so create a new one
      console = new UsbdmTtyConsole(consoleName, null, ttyPortNum);
      conMan.addConsoles(new IConsole[]{console});
      return console;
   }
   
   /**
    * Closes TTY console for given port number.
    * 
    * @return console found
    */
   static public void closeConsole(int ttyPortNum) {
      Server server = servers.get(ttyPortNum);
      if (server != null) {
         server.stopServer();
      }
      String consoleName   = String.format(CONSOLE_NAME, ttyPortNum);
      ConsolePlugin plugin = ConsolePlugin.getDefault();
      IConsoleManager conMan = plugin.getConsoleManager();
      IConsole[] existing = conMan.getConsoles();
      for (int i = 0; i < existing.length; i++) {
         if (existing[i].getName().equals(consoleName)) {
            conMan.removeConsoles(new IConsole[] {existing[i]});
         }
      }
   }
   
   /**
    * Class to implement the TTY server
    * 
    * @author podonoghue
    */
   static class Server implements Runnable {
      
      private int     fTtyPortNum;
      
      private volatile Thread       thread         = null;      
      private volatile ServerSocket serverSocket   = null;
      
      private UsbdmTtyConsole  console      = null;
      
      public Server(int ttyPortNum) throws Exception {
         fTtyPortNum = ttyPortNum;
         serverSocket = new ServerSocket(fTtyPortNum);
         console      = getConsole(fTtyPortNum);
         if (console == null) {
            throw new Exception("Unable to open console");
         }
      }
      void startServer() {
         thread = new Thread(this);
         thread.start();
      }
      void stopServer() {
         try {
            serverSocket.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
         thread = null;
      }
      @Override
      public void run() {
         try {
            while(thread == Thread.currentThread()) {
               Socket clientSocket = null;
               try {
                  clientSocket = serverSocket.accept();
                  clientSocket.setSoTimeout(5);
                  BufferedReader  socketInputStream   = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                  final IOConsoleOutputStream  out = console.newOutputStream();
                  final OutputStreamWriter consoleOutputStream = new OutputStreamWriter(out);
                  int ch;
                     do {
                        try {
                           ch = socketInputStream.read();
                           if (ch<0) {
                              break;
                           }
                           out.write(ch);
                           console.activate();
                        } catch (java.net.SocketTimeoutException e) {
                           // Ignore timeouts - they are here to prevent indefinite wait when asked to stop
                        }
                     } while (thread == Thread.currentThread());
                  socketInputStream.close();
                  consoleOutputStream.close();
               } catch (IOException e) {
                  if (clientSocket != null) {
                     clientSocket.close();
                  }
               }
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
         try {
            serverSocket.close();
            serverSocket = null;
         } catch (IOException e) {
            e.printStackTrace();
         }
//         System.err.println("Server closed");
      }

      public boolean isStopping() {
         return (thread == null);
      }
      public boolean hasStopped() {
         return (serverSocket == null);
      }
   }
   
   static public void startServer(int ttyPortNum) throws Exception {
//      for (Entry<Integer, Server> s:servers.entrySet()) {
//         System.err.println(String.format("# %d", s.getKey()));
//      }
      Server server = servers.get(ttyPortNum);
      if (server != null) {
         if (server.hasStopped()) {
            servers.remove(ttyPortNum);
         }
         else if (server.isStopping()) {
            throw new Exception("Previous server is shutting down\n   - Please try again later");
         }
         else {
//            System.err.println("Found existing server");
            getConsole(ttyPortNum).activate();
            return;
         }
      }
//      System.err.println("Creating new server");
      server = new Server(ttyPortNum);
      server.startServer();
      servers.put(ttyPortNum, server);
      getConsole(ttyPortNum).activate();
   }
   
   static public void stopServer(int ttyPortNum) {
      Server server = servers.get(ttyPortNum);
      if (server != null) {
         server.stopServer();
      }
   }
}
