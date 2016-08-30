/*******************************************************************************
 * Based on
 * 
 * Copyright (c) 2011 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Ericsson - initial API and implementation
 *     Marc Khouzam (Ericsson) - Added support for the different GDBControl versions (Bug 324101)
 *******************************************************************************/
package net.sourceforge.usbdm.gdb.service;

import org.eclipse.cdt.dsf.debug.service.command.ICommandControl;
import org.eclipse.cdt.dsf.gdb.service.GdbDebugServicesFactory;
import org.eclipse.cdt.dsf.gdb.service.command.GDBControl;
import org.eclipse.cdt.dsf.gdb.service.command.GDBControl_7_0;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.debug.core.ILaunchConfiguration;

import net.sourceforge.usbdm.gdb.service.command.UsbdmCommandFactory_6_8;

public class UsbdmGdbDebugServicesFactory extends GdbDebugServicesFactory {

   public UsbdmGdbDebugServicesFactory(String version) {
      super(version);
   }

   public UsbdmGdbDebugServicesFactory(String version, ILaunchConfiguration config) {
      super(version);
//      super(version, config);
//      System.err.println("UsbdmGdbDebugServicesFactory(...)");
   }

   @Override
   protected ICommandControl createCommandControl(DsfSession session, ILaunchConfiguration config) {
//      System.err.println("UsbdmGdbDebugServicesFactory.createCommandControl(...) gdb version = " + getVersion());
      
//      if (compareVersions(GDB_7_7_VERSION, getVersion()) <= 0) {
//         System.err.println("...gdb.UsbdmGdbDebugServicesFactory.createCommandControl(...) GDB_7_7_VERSION");
//         return new UsbdmGdbControl_7_7(session, config, new UsbdmCommandFactory_6_8());
//      }
      if (compareVersions(GDB_7_4_VERSION, getVersion()) <= 0) {
//         System.err.println("...gdb.UsbdmGdbDebugServicesFactory.createCommandControl(...) GDB_7_4_VERSION");
         return new UsbdmGdbControl_7_4(session, config, new UsbdmCommandFactory_6_8());
      }
      if (compareVersions(GDB_7_2_VERSION, getVersion()) <= 0) {
//         System.err.println("...gdb.UsbdmGdbDebugServicesFactory.createCommandControl(...) GDB_7_2_VERSION");
         return new UsbdmGdbControl_7_2(session, config, new UsbdmCommandFactory_6_8());
      }
      if (compareVersions(GDB_7_0_VERSION, getVersion()) <= 0) {
//         System.err.println("...gdb.UsbdmGdbDebugServicesFactory.createCommandControl(...) GDB_7_0_VERSION");
         return new GDBControl_7_0(session, config, new UsbdmCommandFactory_6_8());
      }
      if (compareVersions(GDB_6_8_VERSION, getVersion()) <= 0) {
//         System.err.println("...gdb.UsbdmGdbDebugServicesFactory.createCommandControl(...) GDB_6_8_VERSION");
         return new GDBControl(session, config, new UsbdmCommandFactory_6_8());
      }
//      System.err.println("...gdb.UsbdmGdbDebugServicesFactory.createCommandControl(...) --");
      return new GDBControl(session, config, new UsbdmCommandFactory_6_8());
   }

   @Override
   public <V> V createService(Class<V> clazz, DsfSession session, Object... optionalArguments) {
      if (IUsbdmExtendedFunctions.class.isAssignableFrom(clazz)) {
         return clazz.cast(createExtendedService(session));
      }
      return super.createService(clazz, session, optionalArguments);
   }

   protected IUsbdmExtendedFunctions createExtendedService(DsfSession session) {
//      System.err.println("UsbdmGdbDebugServicesFactory.createExtendedService(...)");
      return new UsbdmExtendedFunctions(session);
   }

   /**
    * Compares two version numbers.
    * Returns -1, 0, or 1 if v1 is less than, equal to, or greater than v2 respectively
    * @param v1 The first version
    * @param v2 The second version
    * @return -1, 0, or 1 if v1 is less than, equal to, or greater than v2 respectively
    * @since 4.8
    * 
    * Borrowed from a later version of LaunchUtils - to be replaced later
    */
   public static int compareVersions(String v1, String v2) {
      if (v1 == null || v2 == null) throw new NullPointerException();
      
      String[] v1Parts = v1.split("\\."); //$NON-NLS-1$
      String[] v2Parts = v2.split("\\."); //$NON-NLS-1$
      for (int i = 0; i < v1Parts.length && i < v2Parts.length; i++) {        
         try {
            int v1PartValue = Integer.parseInt(v1Parts[i]);
            int v2PartValue = Integer.parseInt(v2Parts[i]);

            if (v1PartValue > v2PartValue) {
               return 1;
            } else if (v1PartValue < v2PartValue) {
               return -1;
            }
         } catch (NumberFormatException e) {
            // Non-integer part, ignore it
            continue;
         }
      }
      
      // If we get here is means the versions are still equal
      // but there could be extra parts to examine
      
      if (v1Parts.length < v2Parts.length) {
         // v2 has extra parts, which implies v1 is a lower version (e.g., v1 = 7.9 v2 = 7.9.1)
         // unless each extra part is 0, in which case the two versions are equal (e.g., v1 = 7.9 v2 = 7.9.0)
         for (int i = v1Parts.length; i < v2Parts.length; i++) {
            try {
               if (Integer.parseInt(v2Parts[i]) != 0) {
                  return -1;
               }
            } catch (NumberFormatException e) {
               // Non-integer part, ignore it
               continue;
            }
         }
      }
      if (v1Parts.length > v2Parts.length) {
         // v1 has extra parts, which implies v1 is a higher version (e.g., v1 = 7.9.1 v2 = 7.9)
         // unless each extra part is 0, in which case the two versions are equal (e.g., v1 = 7.9.0 v2 = 7.9)
         for (int i = v2Parts.length; i < v1Parts.length; i++) {
            try {
               if (Integer.parseInt(v1Parts[i]) != 0) {
                  return 1;
               }
            } catch (NumberFormatException e) {
               // Non-integer part, ignore it
               continue;
            }
         }
      }

      return 0;
   }

}
