/*******************************************************************************
 * Copyright (c) 2013 Peter O'Donoghue and others.
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
package net.sourceforge.usbdm.gdb.ui;

import net.sourceforge.usbdm.gdb.UsbdmGdbServer;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class UsbdmDebuggerTab extends AbstractLaunchConfigurationTab {
   
   private static final String   TAB_NAME = "Debugger";
   private static final String   TAB_ID   = "net.sourceforge.usbdm.gdb.ui.usbdmDebuggerTab";

   UsbdmDebuggerPanel            usbdmDebuggerPanel;
   
   public UsbdmDebuggerTab() {
      super();
      usbdmDebuggerPanel = new UsbdmDebuggerPanel();
   }

   @Override
   public String getName() {
      return TAB_NAME;
   }

   @Override
   public Image getImage() {
      ImageDescriptor imageDescriptor = UsbdmGdbServer.getDefault().getImageDescriptor(UsbdmGdbServer.ID_BUG_IMAGE);
      return imageDescriptor.createImage();
   }

   public String getId() {
      return TAB_ID;
   }

   private void doUpdate() {
      try {
         scheduleUpdateJob();
      } catch (NoClassDefFoundError e) {
         // Ignore for debugging
      }      
   }

   @Override
   public void createControl(Composite parent) {
      createContents(parent);
   }
   
   public Control createContents(Composite parent) {
      Control control = usbdmDebuggerPanel.createContents(parent, true);
      usbdmDebuggerPanel.addListener(SWT.CHANGED, new Listener() {
         @Override
         public void handleEvent(Event event) {
            doUpdate();
         }
      });
      setControl(control);
      return control;
   }

   @Override
   public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
       usbdmDebuggerPanel.setDefaults(configuration);
   }

   @Override
   public void initializeFrom(ILaunchConfiguration configuration) {
      try {
         usbdmDebuggerPanel.initializeFrom(configuration);
      } catch (Exception e) {
         System.err.println("UsbdmDebuggerTab.initializeFrom()");
         e.printStackTrace();
      }
   }

   @Override
   public void performApply(ILaunchConfigurationWorkingCopy configuration) {
      usbdmDebuggerPanel.performApply(configuration);
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);

      shell.setLayout(new FillLayout());

      shell.setSize(600, 450);

      UsbdmDebuggerTab usbdmTab = new UsbdmDebuggerTab();

      usbdmTab.createControl(shell);
      usbdmTab.initializeFrom(null);
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose(); 
   }
}
