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

import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConnection;
import org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.DefaultGDBJtagDeviceImpl;

/*
 * Used for non-DSF connection
 */
public class UsbdmGdbInterface extends DefaultGDBJtagDeviceImpl implements IGDBJtagConnection {
   
   @Override
   public void doReset(Collection<String> commands) {
//      System.err.println("UsbdmInterface.doReset()");
      super.doReset(commands);
   }

   @Override
   public void doRemote(String ip, int port, Collection<String> commands) {
//      System.err.println("UsbdmInterface.doRemote(ip, port, commands)");
      super.doRemote(ip, port, commands);
   }

   @Override
   public void doLoadSymbol(String symbolFileName, String symbolOffset,
         Collection<String> commands) {
//      System.err.println("UsbdmInterface.doLoadSymbol()");
      super.doLoadSymbol(symbolFileName, symbolOffset, commands);
   }

   protected String connection = null;

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice#doDelay(int, java.util.Collection)
    */
   @Override
   public final void setDefaultDeviceConnection(String connection) {
//      System.err.println("UsbdmInterface::setDefaultDeviceConnection(\'"+connection+"\')");
      this.connection = connection;
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice#doRemote(java.lang.String, java.util.Collection)
    */
   @Override
   public void doRemote(String commandLine, Collection<String> commands) {
      addCmd(commands, "set remotetimeout 3000");                                      //$NON-NLS-1$
//      System.err.println("UsbdmGdbInterface.doRemote(\'"+commandLine+"\')");
      if (commandLine != null) {
         String completeCommandLine = "-target-select remote " + commandLine; 
         addCmd(commands, completeCommandLine);
//         System.err.println("UsbdmGdbInterface.doRemote(" + completeCommandLine +")");
      }
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice#getDefaultDeviceConnection()
    */
   @Override
   public String getDefaultDeviceConnection() {
      String retVal = connection;
//       System.err.println("UsbdmInterface::getDefaultDeviceConnection() => \'"+retVal+"\'");
      return retVal;
   }

   @Override
   public String getDefaultIpAddress() {
//      System.err.println("UsbdmInterface::getDefaultIpAddress()");
      throw new UnsupportedOperationException();
   }

   @Override
   public String getDefaultPortNumber() {
//      System.err.println("UsbdmInterface::getDefaultPortNumber()");
      throw new UnsupportedOperationException();
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.DefaultGDBJtagDeviceImpl#doStopAt(java.lang.String, java.util.Collection)
    */
   @Override
   public void doStopAt(String stopAt, Collection<String> commands) {
      String cmd = "-break-insert -t " + stopAt; //$NON-NLS-1$
//      String cmd = "tbreak " + stopAt; //$NON-NLS-1$
      addCmd(commands, cmd);
   }

}
