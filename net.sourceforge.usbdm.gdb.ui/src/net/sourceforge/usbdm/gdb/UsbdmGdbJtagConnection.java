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

import net.sourceforge.usbdm.jni.Usbdm;

import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConnection;
import org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.DefaultGDBJtagDeviceImpl;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/*
 * Used for DSF connection
 */
/**
 * @since 4.12
 */
public class UsbdmGdbJtagConnection extends DefaultGDBJtagDeviceImpl implements IGDBJtagConnection {

   private static IPath usbdmApplicationPath = null;
   
   private IPath getUsbdmApplicationPath() {
      if (usbdmApplicationPath == null) {
         usbdmApplicationPath = Usbdm.getApplicationPath();
         if (usbdmApplicationPath == null) {
            usbdmApplicationPath = new Path("USBDM APPLICATION PATH NOT FOUND");
         }
      }
      return usbdmApplicationPath;
   }

   @Override
   public void doRemote(String ip, int port, Collection<String> commands) {
//      System.err.println("UsbdmGdbJtagConnection.doRemote(ip, port, commands)");
      super.doRemote(ip, port, commands);
   }

   @Override
   public void doLoadSymbol(String symbolFileName, String symbolOffset,
         Collection<String> commands) {
//      System.err.println("UsbdmGdbJtagConnection.doLoadSymbol()");
      super.doLoadSymbol(symbolFileName, symbolOffset, commands);
   }

   protected String connection = null;

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice#doDelay(int, java.util.Collection)
    */
   @Override
   public final void setDefaultDeviceConnection(String connection) {
//      System.err.println("UsbdmGdbJtagConnection::setDefaultDeviceConnection(\'"+connection+"\')");
      this.connection = connection;
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice#doRemote(java.lang.String, java.util.Collection)
    */
   @Override
   public void doRemote(String connection, Collection<String> commands) {
      addCmd(commands, "set remotetimeout 3000");                                      //$NON-NLS-1$
//      System.err.println("UsbdmGdbJtagConnection.doRemote(\'"+connection+"\')");
      if (connection != null) {
         String trimmedConnection = connection.trim();
         int separatorIndex = trimmedConnection.lastIndexOf(' ');
         String spriteName;
         String targetDevice;
         if (separatorIndex < 0) {
            spriteName   = trimmedConnection;
            targetDevice = "";
         }
         else {
            spriteName   = trimmedConnection.substring(0, separatorIndex).trim();
            targetDevice = trimmedConnection.substring(separatorIndex+1);
         }
         IPath spritePath = getUsbdmApplicationPath().append(spriteName);
         String completeCommandLine = "-target-select remote | " + escapeScpaces(spritePath.toString())  + " " + targetDevice; 
         addCmd(commands, completeCommandLine);
//         System.err.println("UsbdmGdbJtagConnection.doRemote(" + completeCommandLine +")");
      }
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice#getDefaultDeviceConnection()
    */
   @Override
   public String getDefaultDeviceConnection() {
      String retVal = connection;
//       System.err.println("UsbdmGdbJtagConnection::getDefaultDeviceConnection() => \'"+retVal+"\'");
      return retVal;
   }

   @Override
   public String getDefaultIpAddress() {
//      System.err.println("UsbdmGdbJtagConnection::getDefaultIpAddress()");
      throw new UnsupportedOperationException();
   }

   @Override
   public String getDefaultPortNumber() {
//      System.err.println("UsbdmGdbJtagConnection::getDefaultPortNumber()");
      throw new UnsupportedOperationException();
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.DefaultGDBJtagDeviceImpl#doStopAt(java.lang.String, java.util.Collection)
    */
   @Override
   public void doStopAt(String stopAt, Collection<String> commands) {
      addCmd(commands, "-break-insert -t " + stopAt); //$NON-NLS-1$
   }

   @Override
   public void doReset(Collection<String> commands) {
//      System.err.println("UsbdmGdbJtagConnection.doReset()");
      addCmd(commands, "monitor reset run");
      addCmd(commands, "-exec-step");  //$NON-NLS-1$
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.DefaultGDBJtagDeviceImpl#doStopAt(java.lang.String, java.util.Collection)
    */
   @Override
   public void doSetPC(String pcValue, Collection<String> commands) {
      addCmd(commands, "set $pc=" + pcValue); //$NON-NLS-1$
      addCmd(commands, "-exec-step");  //$NON-NLS-1$
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice#doHalt(java.util.Collection)
    */
   @Override
   public void doHalt(Collection<String> commands) {
      addCmd(commands, "-exec-interrupt"); //$NON-NLS-1$
      addCmd(commands, "-exec-step");  //$NON-NLS-1$
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.IGDBJtagDevice#doContinue(java.util.Collection)
    */
   @Override
   public void doContinue(Collection<String> commands) {
      addCmd(commands, "-exec-continue"); //$NON-NLS-1$
   }

   /* (non-Javadoc)
    * @see org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.DefaultGDBJtagDeviceImpl#getDefaultDelay()
    */
   @Override
   public int getDefaultDelay() {
      return 0;
   }

}
