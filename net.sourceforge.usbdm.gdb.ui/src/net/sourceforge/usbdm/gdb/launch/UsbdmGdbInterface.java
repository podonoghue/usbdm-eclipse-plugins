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
package net.sourceforge.usbdm.gdb.launch;

import java.util.Collection;
import java.util.List;

/**
 * Used for USBDM connection
 */
public class UsbdmGdbInterface {

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
   public void doLoadImage(String imageFileName, String imageOffset, Collection<String> commands) {
//      System.err.println("UsbdmGdbInterface.doLoadImage()");
      commands.add("-file-exec-file " + escapeSpaces(imageFileName));  //$NON-NLS-1$
      commands.add("-target-download");  //$NON-NLS-1$
   }

   /**
    * Load symbols from file
    * 
    * @param symbolFileName Path of file to load
    * @param symbolOffset   Offset for symbols
    * @param commands       Collection to add commands to
    */
   public void doLoadSymbol(String symbolFileName, String symbolOffset, Collection<String> commands) {
//      System.err.println("UsbdmGdbInterface.doLoadSymbol()");
      commands.add("-file-symbol-file " + escapeSpaces(symbolFileName)); //$NON-NLS-1$
   }

   /**
    * 
    * @param commandLine   GDB command line
    */
   public void doRemote(String commandLine, Collection<String> commands) {
      commands.add("-gdb-set remotetimeout 3000"); //$NON-NLS-1$
//      System.err.println("UsbdmGdbInterface.doRemote(\'"+"-gdb-set remotetimeout 3000"+"\')");
      if (commandLine != null) {
         commands.add("-target-select remote " + commandLine);//$NON-NLS-1$
//         System.err.println("UsbdmGdbInterface.doRemote(\'"+"-target-select remote " +commandLine+"\')");
      }
   }

   /**
    * Add commands to set a temporary breakpoint
    * 
    * @param stopAt     GDB expression to stop at e.g. main, main.c:123 etc
    * @param commands   Collection to add commands to
    */
   public void doStopAt(String stopAt, Collection<String> commands) {
//      System.err.println(String.format("UsbdmGdbInterface.doStopAt(%s)", stopAt));
      commands.add("-break-insert -t " + stopAt); //$NON-NLS-1$
   }

   /**
    * Add commands to reset target
    * 
    * @param commands    Collection to add commands to
    */
   public void doReset(Collection<String> commands) {
//      System.err.println("UsbdmGdbInterface.doReset()");
      commands.add("monitor reset halt"); //$NON-NLS-1$
   }

   /**
    * Add commands to step target
    * 
    * @param commands    Collection to add commands to
    */
   public void doStep(Collection<String> commands) {
//      System.err.println("UsbdmGdbInterface.doStep()");
      commands.add("-exec-step");  //$NON-NLS-1$
   }

   /**
    * Add  commands to set PC
    * 
    * @param pcValue    PC value to use
    * @param commands   Collection to add commands to
    */
   public void doSetPC(String pcValue, Collection<String> commands) {
//      System.err.println(String.format("UsbdmGdbInterface.doSetPC(%s)", pcValue));
      commands.add("-gdb-set $pc=" + pcValue); //$NON-NLS-1$
   }

   /**
    * Add  commands to halt target
    * 
    * @param commands   Collection to add commands to
    */
   public void doHalt(Collection<String> commands) {
//      System.err.println("UsbdmGdbInterface.doHalt()");
      commands.add("-exec-interrupt"); //$NON-NLS-1$
   }

   /**
    * Add  commands to have target continue from current PC
    * 
    * @param commands   Collection to add commands to
    */
   public void doContinue(Collection<String> commands) {
//     System.err.println("UsbdmGdbInterface.doContinue()");
      // Use 'continue' so we don't wait for acknowledgement
//      commands.add("continue"); //$NON-NLS-1$
      commands.add("-exec-continue"); //$NON-NLS-1$
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
      commands.add("monitor run"); //$NON-NLS-1$
//      commands.add("-exec-continue"); //$NON-NLS-1$
   }

   public void doDetach(List<String> commands) {
//      System.err.println("UsbdmGdbInterface.doDetach()");
      commands.add("-target-detach"); //$NON-NLS-1$
   }

}
