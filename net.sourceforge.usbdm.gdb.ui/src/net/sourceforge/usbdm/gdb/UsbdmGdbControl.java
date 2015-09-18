/*******************************************************************************
 * Copyright (c) 2011 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Ericsson - initial API and implementation
 *******************************************************************************/
package net.sourceforge.usbdm.gdb;

import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.RequestMonitorWithProgress;
import org.eclipse.cdt.dsf.concurrent.Sequence;
import org.eclipse.cdt.dsf.gdb.service.command.GDBControl;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Jtag control service which selects the Jtag CompleteInitializationSequence.
 * Use for GDB < 7.0
 * @since 4.12
 */
public class UsbdmGdbControl extends GDBControl {

   GdbServerParameters fGdbServerParameters;

	public UsbdmGdbControl(DsfSession session, ILaunchConfiguration config, CommandFactory factory, GdbServerParameters gdbServerParameters) {
		super(session, config, factory);
//      System.err.println("UsbdmGDBJtagControl()");
      fGdbServerParameters = gdbServerParameters;
	}

	@Override
	protected Sequence getCompleteInitializationSequence(Map<String,Object> attributes, RequestMonitorWithProgress rm) {
//      System.err.println("UsbdmGDBJtagControl.getCompleteInitializationSequence()");
		return new UsbdmGdbDsfFinalLaunchSequence(getSession(), attributes, rm, fGdbServerParameters);
	}
}