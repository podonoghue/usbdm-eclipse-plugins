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
package net.sourceforge.usbdm.gdb.service;

import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.RequestMonitorWithProgress;
import org.eclipse.cdt.dsf.concurrent.Sequence;
import org.eclipse.cdt.dsf.gdb.service.command.GDBControl_7_4;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.debug.core.ILaunchConfiguration;

import net.sourceforge.usbdm.gdb.launch.UsbdmGdbDsfFinalLaunchSequence;

public class UsbdmGdbControl_7_4 extends GDBControl_7_4 {

	public UsbdmGdbControl_7_4(DsfSession session, ILaunchConfiguration config, CommandFactory factory) {
		super(session, config, factory);
//      System.err.println("UsbdmGdbControl_7_4()");
	}

	@Override
	protected Sequence getCompleteInitializationSequence(Map<String,Object> attributes, RequestMonitorWithProgress rm) {
//      System.err.println("UsbdmGdbControl_7_4().getCompleteInitializationSequence");
		return new UsbdmGdbDsfFinalLaunchSequence(getSession(), attributes, rm);
	}
	
}