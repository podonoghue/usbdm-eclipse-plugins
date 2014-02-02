package net.sourceforge.usbdm.peripherals.view;

import org.eclipse.debug.core.DebugEvent;

public interface GDBSessionListener {

      public void SessionTerminate(DebugEvent source);
      
      public void SessionSuspend(DebugEvent event);
      
      public void SessionCreate(GDBInterface source);
}
