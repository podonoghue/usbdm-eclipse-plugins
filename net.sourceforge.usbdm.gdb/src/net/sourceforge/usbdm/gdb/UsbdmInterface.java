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

public class UsbdmInterface extends DefaultGDBJtagDeviceImpl implements IGDBJtagConnection {

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
   public void doRemote(String connection, Collection<String> commands) {
//	   System.err.println("UsbdmInterface.doRemote(\'"+connection+"\')");
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
         String spriteCommand = "-target-select remote | " + escapeScpaces(spritePath.toString())  + " " + targetDevice; 
//	      System.err.println("UsbdmInterface.doRemote(" + spriteCommand);
         addCmd(commands, "set remotetimeout 3000");                                      //$NON-NLS-1$
         addCmd(commands, spriteCommand);
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

}
