/*******************************************************************************
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
package net.sourceforge.usbdm.gdb;

import org.eclipse.cdt.dsf.debug.service.command.ICommandControl;
import org.eclipse.cdt.dsf.gdb.service.GdbDebugServicesFactory;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * @since 4.10
 */
public class UsbdmGdbDebugServicesFactory extends GdbDebugServicesFactory {

	public UsbdmGdbDebugServicesFactory(String version) {
		super(version);
	}

	@Override
	protected ICommandControl createCommandControl(DsfSession session, ILaunchConfiguration config) {
	   GdbServerParameters gdbServerParameters = GdbServerParameters.getInitializedServerParameters(config);
	   
//      if (GDB_7_7_VERSION.compareTo(getVersion()) <= 0) {
//         return new UsbdmGdbControl_7_7(session, config, new UsbdmCommandFactory_6_8(), gdbServerParameters);
//      }
      if (GDB_7_4_VERSION.compareTo(getVersion()) <= 0) {
         return new UsbdmGdbControl_7_4(session, config, new UsbdmCommandFactory_6_8(), gdbServerParameters);
      }
	   if (GDB_7_2_VERSION.compareTo(getVersion()) <= 0) {
	      return new UsbdmGdbControl_7_2(session, config, new UsbdmCommandFactory_6_8(), gdbServerParameters);
	   }
	   if (GDB_7_0_VERSION.compareTo(getVersion()) <= 0) {
	      return new UsbdmGdbControl_7_0(session, config, new UsbdmCommandFactory_6_8(), gdbServerParameters);
	   }
	   if (GDB_6_8_VERSION.compareTo(getVersion()) <= 0) {
	      return new UsbdmGdbControl(session, config, new UsbdmCommandFactory_6_8(), gdbServerParameters);
	   }
	   return new UsbdmGdbControl(session, config, new UsbdmCommandFactory_6_8(), gdbServerParameters);
	}
}
