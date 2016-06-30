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
import org.eclipse.cdt.dsf.gdb.service.command.GDBControl_7_2;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.debug.core.ILaunchConfiguration;

import net.sourceforge.usbdm.gdb.server.GdbServerParameters;
import net.sourceforge.usbdm.gdb.service.command.UsbdmCommandFactory_6_8;

public class UsbdmGdbDebugServicesFactory extends GdbDebugServicesFactory {

   public UsbdmGdbDebugServicesFactory(ILaunchConfiguration config, String version) {
      super(version);
      System.err.println("...gdb.UsbdmGdbDebugServicesFactory(...)");
   }

   @SuppressWarnings("unchecked")
   @Override
   public <V> V createService(Class<V> clazz, DsfSession session, Object... optionalArguments) {
      System.err.println("...gdb.UsbdmGdbDebugServicesFactory.createService(...)");
      if (IUsbdmExtendedFunctions.class.isAssignableFrom(clazz)) {
         return (V)createExtendedService(session);
      }
      return super.createService(clazz, session, optionalArguments);
   }

   @Override
   protected ICommandControl createCommandControl(DsfSession session, ILaunchConfiguration config) {
      System.err.println("...gdb.UsbdmGdbDebugServicesFactory.createCommandControl(...)");
      GdbServerParameters gdbServerParameters = GdbServerParameters.getInitializedServerParameters(config);

      if (GDB_7_7_VERSION.compareTo(getVersion()) <= 0) {
         return new UsbdmGdbControl(session, config, new UsbdmCommandFactory_6_8(), gdbServerParameters);
      }
      if (GDB_7_4_VERSION.compareTo(getVersion()) <= 0) {
         return new UsbdmGdbControl(session, config, new UsbdmCommandFactory_6_8(), gdbServerParameters);
      }
      if (GDB_7_2_VERSION.compareTo(getVersion()) <= 0) {
         return new GDBControl_7_2(session, config, new UsbdmCommandFactory_6_8());
      }
      if (GDB_7_0_VERSION.compareTo(getVersion()) <= 0) {
         return new GDBControl_7_0(session, config, new UsbdmCommandFactory_6_8());
      }
      if (GDB_6_8_VERSION.compareTo(getVersion()) <= 0) {
         return new GDBControl(session, config, new UsbdmCommandFactory_6_8());
      }
      return new GDBControl(session, config, new UsbdmCommandFactory_6_8());
   }

   protected IUsbdmExtendedFunctions createExtendedService(DsfSession session) {
      System.err.println("...gdb.UsbdmGdbDebugServicesFactory.createExtendedService(...)");
      return new UsbdmExtendedFunctions(session);
   }

}
