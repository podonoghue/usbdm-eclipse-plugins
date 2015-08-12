/*******************************************************************************
 * Copyright (c) 2013 Peter O'Donoghue.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

/**
 * @author Peter O'Donoghue
 *         based upon work by Doug Schaefer, Adrian Petrescu
 * 
 */
package net.sourceforge.usbdm.gdb;

import java.util.Collection;
import java.util.List;

import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConnection;
import org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.DefaultGDBJtagDeviceImpl;

/*
 * Used for USBDM connection
 */
/**
 * @since 4.10
 */
public class UsbdmGdbInterface extends DefaultGDBJtagDeviceImpl implements IGDBJtagConnection {

   /**
    * Utility method to format and add commands
    * 
    * @param commands Collection of commands
    * @param cmd      Command to add to collection
    */
   @Override
   protected void addCmd(Collection<String> commands, String cmd) {
//      System.err.println(String.format("addCmd(%s)", cmd));
      commands.add(cmd);
   }
   
   /**
    * Protect file path if needed
    * 
    * @param file    File path to protect
    * @return        Protected path (original if no modification needed) 
    */
   protected String escapeSpaces(String file) {
      if (file.indexOf(' ') >= 0) {
         return '"' + file + '"'; 
      }
      return file;
   }

   /**
    * Load image from file
    * 
    * @param imageFileName  Path of file to load
    * @param imageOffset    Offset for image
    * @param commands       Collection to add commands to
    */
   @Override
   public void doLoadImage(String imageFileName, String imageOffset, Collection<String> commands) {
//      System.err.println("UsbdmGdbInterface.doLoadImage()");
      addCmd(commands, "-file-exec-file " + escapeSpaces(imageFileName));  //$NON-NLS-1$  //$NON-NLS-2$
      addCmd(commands, "-target-download");  //$NON-NLS-1$
   }

   /**
    * Load symbols from file
    * 
    * @param symbolFileName Path of file to load
    * @param symbolOffset   Offset for symbols
    * @param commands       Collection to add commands to
    */
   @Override
   public void doLoadSymbol(String symbolFileName, String symbolOffset, Collection<String> commands) {
//      System.err.println("UsbdmGdbInterface.doLoadSymbol()");
      addCmd(commands, "-file-symbol-file " + escapeSpaces(symbolFileName)); //$NON-NLS-1$
   }

   protected String connection = "localhost:1234";

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice#doDelay(int, java.util.Collection)
    */
   @Override
   public final void setDefaultDeviceConnection(String connection) {
//      System.err.println("UsbdmGdbInterface::setDefaultDeviceConnection(\'"+connection+"\')");
      this.connection = connection;
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice#getDefaultDeviceConnection()
    */
   @Override
   public String getDefaultDeviceConnection() {
//       System.err.println("UsbdmGdbInterface::getDefaultDeviceConnection() => \'"+connection+"\'");
      return connection;
   }

   /**
    * 
    * @param ip
    * @param port
    * @param commands       Collection to add commands to
    */
   @Override
   public void doRemote(String ip, int port, Collection<String> commands) {
//      System.err.println(String.format("UsbdmGdbInterface.doRemote(ip=%s, port=%d)", ip, port));
      addCmd(commands, "-gdb-set remotetimeout 3000"); //$NON-NLS-1$
      addCmd(commands, "-target-select remote " + ip + ":" + String.valueOf(port)); //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * 
    * @param commandLine   GDB command line
    */
   @Override
   public void doRemote(String commandLine, Collection<String> commands) {
      addCmd(commands, "-gdb-set remotetimeout 3000"); //$NON-NLS-1$
//      System.err.println("UsbdmGdbInterface.doRemote(\'"+"-gdb-set remotetimeout 3000"+"\')");
      if (commandLine != null) {
         addCmd(commands, "-target-select remote " + commandLine);//$NON-NLS-1$
//         System.err.println("UsbdmGdbInterface.doRemote(\'"+"-target-select remote " +commandLine+"\')");
      }
   }

   @Override
   public String getDefaultIpAddress() {
//      System.err.println("UsbdmGdbInterface::getDefaultIpAddress()");
      throw new UnsupportedOperationException();
   }

   @Override
   public String getDefaultPortNumber() {
//      System.err.println("UsbdmGdbInterface::getDefaultPortNumber()");
      throw new UnsupportedOperationException();
   }

   /**
    * Add commands to set a temporary breakpoint
    * 
    * @param stopAt     GDB expression to stop at e.g. main, main.c:123 etc
    * @param commands   Collection to add commands to
    */
   @Override
   public void doStopAt(String stopAt, Collection<String> commands) {
//      System.err.println(String.format("UsbdmGdbInterface.doStopAt(%s)", stopAt));
      addCmd(commands, "-break-insert -t " + stopAt); //$NON-NLS-1$
   }

   /**
    * Add commands to reset target
    * 
    * @param commands    Collection to add commands to
    */
   @Override
   public void doReset(Collection<String> commands) {
//      System.err.println("UsbdmGdbInterface.doReset()");
      addCmd(commands, "monitor reset halt"); //$NON-NLS-1$
   }

   /**
    * Add commands to step target
    * 
    * @param commands    Collection to add commands to
    */
   public void doStep(Collection<String> commands) {
//      System.err.println("UsbdmGdbInterface.doStep()");
      addCmd(commands, "-exec-step");  //$NON-NLS-1$
   }

   /**
    * Add  commands to set PC
    * 
    * @param pcValue    PC value to use
    * @param commands   Collection to add commands to
    */
   @Override
   public void doSetPC(String pcValue, Collection<String> commands) {
//      System.err.println(String.format("UsbdmGdbInterface.doSetPC(%s)", pcValue));
      addCmd(commands, "-gdb-set $pc=" + pcValue); //$NON-NLS-1$
   }

   /**
    * Add  commands to halt target
    * 
    * @param commands   Collection to add commands to
    */
   @Override
   public void doHalt(Collection<String> commands) {
//      System.err.println("UsbdmGdbInterface.doHalt()");
      addCmd(commands, "-exec-interrupt"); //$NON-NLS-1$
   }

   /**
    * Add  commands to have target continue from current PC
    * 
    * @param commands   Collection to add commands to
    */
   @Override
   public void doContinue(Collection<String> commands) {
//     System.err.println("UsbdmGdbInterface.doContinue()");
      // Use 'continue' so we don't wait for acknowledgement
//      addCmd(commands, "continue"); //$NON-NLS-1$
      addCmd(commands, "-exec-continue"); //$NON-NLS-1$
   }

   /**
    * Add commands to have target run from current PC
    * This does NOT load breakpoints
    * 
    * @param commands   Collection to add commands to
    */
   public void doRun(Collection<String> commands) {
//      System.err.println("UsbdmGdbInterface.doRun()");
      // Use 'continue' so we don't wait for acknowledgement
      addCmd(commands, "monitor run"); //$NON-NLS-1$
//      addCmd(commands, "-exec-continue"); //$NON-NLS-1$
   }

   /**
    * Get default reset delay
    * 
    * @return delay in ms
    */
   @Override
   public int getDefaultDelay() {
      return 0;
   }

   public void doDetach(List<String> commands) {
//      System.err.println("UsbdmGdbInterface.doDetach()");
      addCmd(commands, "-target-detach"); //$NON-NLS-1$
   }

}
