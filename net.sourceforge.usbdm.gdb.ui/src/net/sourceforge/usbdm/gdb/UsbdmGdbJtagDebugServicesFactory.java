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
import org.eclipse.cdt.dsf.gdb.service.command.CommandFactory_6_8;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * @since 8.0
 */
public class UsbdmGdbJtagDebugServicesFactory extends GdbDebugServicesFactory {


	public UsbdmGdbJtagDebugServicesFactory(String version) {
		super(version);
	}

	@Override
	protected ICommandControl createCommandControl(DsfSession session, ILaunchConfiguration config) {
	   
      String newDeviceName = null;
      try {
         newDeviceName = config.getAttribute("net.sourceforge.usbdm.gdb.deviceName", (String)null);
      } catch (CoreException e) {
      }
      if (newDeviceName != null) {
         System.err.println("UsbdmGdbJtagDebugServicesFactory.createCommandControl() adding adapter");
         UsbdmGdbLaunchInformation launchInformation = new UsbdmGdbLaunchInformation();
         launchInformation.addValue("net.sourceforge.usbdm.gdb.deviceName", newDeviceName);
         session.registerModelAdapter(UsbdmGdbLaunchInformation.class, launchInformation);
      }
      else {
         System.err.println("UsbdmGdbJtagDebugServicesFactory.createCommandControl() failed to get device name");
      }

	   GdbServerParameters gdbServerParameters = GdbServerParameters.getInitializedServerParameters(config);
	   
	   if (GDB_7_4_VERSION.compareTo(getVersion()) <= 0) {
	      return new UsbdmGDBJtagControl_7_4(session, config, new CommandFactory_6_8(), gdbServerParameters);
	   }
	   if (GDB_7_2_VERSION.compareTo(getVersion()) <= 0) {
	      return new UsbdmGDBJtagControl_7_2(session, config, new CommandFactory_6_8(), gdbServerParameters);
	   }
	   if (GDB_7_0_VERSION.compareTo(getVersion()) <= 0) {
	      return new UsbdmGDBJtagControl_7_0(session, config, new CommandFactory_6_8(), gdbServerParameters);
	   }
	   if (GDB_6_8_VERSION.compareTo(getVersion()) <= 0) {
	      return new UsbdmGDBJtagControl(session, config, new CommandFactory_6_8(), gdbServerParameters);
	   }
	   return new UsbdmGDBJtagControl(session, config, new CommandFactory(), gdbServerParameters);
	}
}
