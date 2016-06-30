/*******************************************************************************
 * Based on org.eclipse.cdt.examples.dsf.gdb
 *  
 * Copyright (c) 2014 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Marc Khouzam (Ericsson) - initial API and implementation
 *******************************************************************************/

package net.sourceforge.usbdm.gdb.launch;

import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.gdb.launching.ServicesLaunchSequence;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.commands.IRestartHandler;

import net.sourceforge.usbdm.gdb.commands.UsbdmGdbRestartTargetHandler;
import net.sourceforge.usbdm.gdb.service.UsbdmExtendedFunctions;

public class UsbdmGdbServicesLaunchSequence extends ServicesLaunchSequence {

   private final GdbLaunch  fLaunch;
   private final DsfSession fSession;
   
   public UsbdmGdbServicesLaunchSequence(DsfSession session, GdbLaunch launch, IProgressMonitor pm) {
      super(session, launch, pm);
      System.err.println("...gdb.UsbdmGdbServicesLaunchSequence()");
      fLaunch = launch;
      fSession = session;
      session.registerModelAdapter(IRestartHandler.class, new UsbdmGdbRestartTargetHandler(session, launch));
   }

   @Override
   public Step[] getSteps() { 
//      System.err.println("...gdb.UsbdmGdbServicesLaunchSequence.getSteps()");
      // Add an extra step at the end to create the new service
      Step[] steps     = super.getSteps();
      Step[] moreSteps = new Step[steps.length + 1];
      System.arraycopy(steps, 0, moreSteps, 0, steps.length);
      moreSteps[steps.length] = new Step() {
         @Override
         public void execute(RequestMonitor requestMonitor) {
            fLaunch.getServiceFactory().createService(UsbdmExtendedFunctions.class, fSession).initialize(requestMonitor);
         }
      };
      return moreSteps;
   }

}
