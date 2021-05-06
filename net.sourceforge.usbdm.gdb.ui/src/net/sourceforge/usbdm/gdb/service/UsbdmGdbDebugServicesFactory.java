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
import org.eclipse.cdt.dsf.gdb.launching.LaunchUtils;
import org.eclipse.cdt.dsf.gdb.service.GdbDebugServicesFactory;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.debug.core.ILaunchConfiguration;

import net.sourceforge.usbdm.gdb.service.command.UsbdmCommandFactory_6_8;

public class UsbdmGdbDebugServicesFactory extends GdbDebugServicesFactory {

   public UsbdmGdbDebugServicesFactory(String version, ILaunchConfiguration config) {
      super(version, config);
      //      System.err.println("UsbdmGdbDebugServicesFactory(...)");
   }

	@Override
	protected ICommandControl createCommandControl(DsfSession session, ILaunchConfiguration config) {
	   if (compareVersionWith(GDB_7_12_VERSION) >= 0) {
//	      System.err.println("UsbdmGdbDebugServicesFactory.createCommandControl(...) GDB_7_12_VERSION");
         return new UsbdmGdbControl_HEAD(session, config, new UsbdmCommandFactory_6_8());
      }
      if (compareVersionWith(GDB_7_7_VERSION) >= 0) {
//         System.err.println("UsbdmGdbDebugServicesFactory.createCommandControl(...) GDB_7_7_VERSION");
         return new UsbdmGdbControl_7_7(session, config, new UsbdmCommandFactory_6_8());
      }
		if (compareVersionWith(GDB_7_4_VERSION) <= 0) {
			return new UsbdmGdbControl_7_4(session, config, new UsbdmCommandFactory_6_8());
		}
		if (compareVersionWith(GDB_7_2_VERSION) <= 0) {
			return new UsbdmGdbControl_7_2(session, config, new UsbdmCommandFactory_6_8());
		}
		if (compareVersionWith(GDB_7_0_VERSION) <= 0) {
			return new UsbdmGdbControl_7_0(session, config, new UsbdmCommandFactory_6_8());
		}
		return new UsbdmGdbControl(session, config, new UsbdmCommandFactory_6_8());
	}

	@Override
	public <V> V createService(Class<V> clazz, DsfSession session, Object... optionalArguments) {
		if (IUsbdmExtendedFunctions.class.isAssignableFrom(clazz)) {
			return clazz.cast(createExtendedService(session));
		}
		return super.createService(clazz, session, optionalArguments);
	}

	protected IUsbdmExtendedFunctions createExtendedService(DsfSession session) {
		return new UsbdmExtendedFunctions(session);
	}

	/**
    * Compares the GDB version of the current debug session with the one specified by
    * parameter 'version'.  Returns -1, 0, or 1 if the current version is less than,
    * equal to, or greater than the specified version, respectively.
    * @param version The version to compare with
    * @return -1, 0, or 1 if the current version is less than, equal to, or greater than
    *          the specified version, respectively.
    * @since 4.8
    */
   protected int compareVersionWith(String version) {
      return LaunchUtils.compareVersions(getVersion(), version);
   }

}
