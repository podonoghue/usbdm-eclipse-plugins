package net.sourceforge.usbdm.peripherals.view;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.cdt.dsf.debug.service.IRunControl.IExitedDMEvent;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IStartedDMEvent;
import org.eclipse.cdt.dsf.debug.service.IRunControl.ISuspendedDMEvent;
import org.eclipse.cdt.dsf.service.DsfServiceEventHandler;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.dsf.service.DsfSession.SessionEndedListener;
import org.eclipse.cdt.dsf.service.DsfSession.SessionStartedListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.peripheralDatabase.SVDIdentifier;
import net.sourceforge.usbdm.peripherals.model.DeviceModel;
import net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel;
import net.sourceforge.usbdm.peripherals.usbdm.UsbdmPeripheralDescriptionProvider;

public class GdbDsfSessionListener implements SessionStartedListener, SessionEndedListener {

   /**
    * Singleton
    */
   private static GdbDsfSessionListener  singleton = null;
   /**
    * Used to track DSF sessions and associated device information
    */
   private HashMap<String, UsbdmDevicePeripheralsModel> dsfSessions = null;
   /**
    * Used to track listeners to this GdbDsfSessionListener
    */
   private ArrayList<GdbSessionListener> gdbSessionListeners = null;
   
   private GdbDsfSessionListener() {
//      System.err.println("GdbDsfSessionListener()");      
      gdbSessionListeners = new ArrayList<GdbSessionListener>();
   }

   /**
    * @return DSF session listener singleton
    */
   public static synchronized GdbDsfSessionListener getListener() {
      if (singleton == null) {
         singleton = new GdbDsfSessionListener();
      }
      return singleton;
   }

   /**
    * Determines device name from dsfSession
    * 
    * @param dsfSession DSFSession
    * 
    * @return the device name e.g. MK20DX128M5
    */
   private String getDeviceName(DsfSession dsfSession) {
      if (dsfSession == null) {
         return null;
      }
      String deviceName = null;
      try {
         ILaunch launch = (ILaunch)dsfSession.getModelAdapter(ILaunch.class);
         ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
         deviceName = launchConfiguration.getAttribute(UsbdmSharedConstants.LAUNCH_DEVICE_NAME_KEY, "");
      } catch (CoreException e) {
//         e.printStackTrace();
      }
//      System.err.println("GdbDsfSessionListener.getDeviceName() - deviceName = " + deviceName);
      return deviceName;
   }

   /**
    * Adds existing sessions
    */
   void addExistingSessions() {
      // Add any existing sessions
      dsfSessions = new HashMap<String, UsbdmDevicePeripheralsModel>();
      for (DsfSession dsfSession : DsfSession.getActiveSessions()) {
         addSession(dsfSession.getId());
         sessionStarted(dsfSession);
      }
   }
   
   /*
    * (non-Javadoc)
    * @see org.eclipse.cdt.dsf.service.DsfSession.SessionStartedListener#sessionStarted(org.eclipse.cdt.dsf.service.DsfSession)
    */
   @Override
   public void sessionStarted(DsfSession newDsfSession) {
//      System.err.println("sessionStarted(DsfSession) : deviceName = " + getDeviceName(newDsfSession));      
//      System.err.println("sessionStarted(DsfSession) : ID = " + newDsfSession.getId());      
      newDsfSession.addServiceEventListener(this,  null);
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.cdt.dsf.service.DsfSession.SessionEndedListener#sessionEnded(org.eclipse.cdt.dsf.service.DsfSession)
    */
   @Override
   public void sessionEnded(DsfSession dsfSession) {
//      System.err.println("sessionEnded(DsfSession) : ID = " + dsfSession.getId());
      dsfSession.removeServiceEventListener(this);

      // Should have been done by handleDsfEvent(ISuspendedDMEvent event)
      UsbdmDevicePeripheralsModel deviceModel = dsfSessions.get(dsfSession.getId());
      if (deviceModel != null) {
         for (GdbSessionListener sessionListener : gdbSessionListeners) {
            sessionListener.sessionTerminated(deviceModel);
         }
      }
      dsfSessions.remove(dsfSession.getId());
   }

   /**
    * Add a listener for GDB events
    * 
    * @param listener
    */
   public void addListener(final GdbSessionListener listener) {

      gdbSessionListeners.add(listener);
      if (gdbSessionListeners.size() == 1) {
         // First listener - add session hooks
//               System.err.println("addListener(GdbSessionListener) : adding session listeners");      
         addExistingSessions();
         DsfSession.addSessionStartedListener(GdbDsfSessionListener.this);
         DsfSession.addSessionEndedListener(GdbDsfSessionListener.this);
      }
      // Notify of any existing sessions
      for (DsfSession dsfSession : DsfSession.getActiveSessions()) {
         UsbdmDevicePeripheralsModel model = dsfSessions.get(dsfSession.getId());
         listener.sessionStarted(model);
      }
   }

   /**
    * Remove a listener for GDB events
    * 
    * @param listener Listener to remove
    */
   public void removeListener(GdbSessionListener listener) {
//      System.err.println("removeListener(GdbSessionListener) : " + listener);      
      gdbSessionListeners.remove(listener);
      if (gdbSessionListeners.size() == 0) {
//         System.err.println("removeListener(GdbSessionListener) : removing session listeners");      
         // Last listener removed - remove session hooks
         for (DsfSession dsfSession : DsfSession.getActiveSessions()) {
            dsfSession.removeServiceEventListener(this);
         }
         dsfSessions = null;
         DsfSession.removeSessionStartedListener(this);
         DsfSession.removeSessionEndedListener(this);   
      }
   }

   /*
    * ========================================================
    *  DSF Session handling
    * ========================================================
    */
   /**
    * Adds a session and associated model to dsfSessions
    * 
    * @param sessionId DSF Session id used to track session
    *  
    * @return true => new session was added, 
    *     <br>false => session already exists
    *     
    * @note This can be time consuming as model is loaded from disk
    */
   private boolean addSession(String sessionId) {
      if (!dsfSessions.containsKey(sessionId)) {
         // New session
         DsfSession dsfSession = DsfSession.getSession(sessionId);
         String     deviceName = getDeviceName(dsfSession);
//         System.err.println("GdbDsfSessionListener.addSession(), deviceName="+deviceName);
         SVDIdentifier svdId = new SVDIdentifier(UsbdmPeripheralDescriptionProvider.ID, deviceName);
//         System.err.println("GdbDsfSessionListener.addSession(), svdId="+svdId);
         UsbdmDevicePeripheralsModel peripheralModel = UsbdmDevicePeripheralsModel.createModel(new GdbDsfInterface(dsfSession), svdId);
         dsfSessions.put(sessionId, peripheralModel);
         return true;
      }
      return false;
   }
   
   /**
    * 
    */
   @DsfServiceEventHandler
   public void handleDsfEvent(IStartedDMEvent event) {
      String sessionId = event.getDMContext().getSessionId();
      
//      System.err.println(String.format("handleDsfEvent(IStartedDMEvent, s=%s, r=%s)", sessionId, event.toString()));

      if (addSession(sessionId)) {
         UsbdmDevicePeripheralsModel model = dsfSessions.get(sessionId);
         for (GdbSessionListener sessionListener : gdbSessionListeners) {
            sessionListener.sessionStarted(model);
         }
      }
   }

   /**
    * 
    */
   @DsfServiceEventHandler
   public void handleDsfEvent(IExitedDMEvent event) {
      String sessionId = event.getDMContext().getSessionId();
//      System.err.println(String.format("handleDsfEvent(IExitedDMEvent, s=%s, r=%s)", sessionId, event.toString()));

      UsbdmDevicePeripheralsModel deviceModel = dsfSessions.get(sessionId);
      if (deviceModel != null) {
         for (GdbSessionListener sessionListener : gdbSessionListeners) {
            sessionListener.sessionTerminated(deviceModel);
         }
      }
   }

   /**
    * 
    */
   @DsfServiceEventHandler
   public void handleDsfEvent(ISuspendedDMEvent event) {
      String sessionId = event.getDMContext().getSessionId();
            
      UsbdmDevicePeripheralsModel devicePeripheralsModel = dsfSessions.get(sessionId);
      if (devicePeripheralsModel != null) {
         DeviceModel deviceModel = devicePeripheralsModel.getModel();
         if (deviceModel != null) {
            // Set current register values as the 'reference' for changed values
            deviceModel.setChangeReference();
            // Set all registers as stale
            deviceModel.setNeedsUpdate(true);
         }
      }
      for (GdbSessionListener sessionListener : gdbSessionListeners) {
         sessionListener.sessionSuspended(devicePeripheralsModel, GdbSessionListener.EventType.getEventFromDsfEvent(event.getReason()));
      }
   }

   /**
    * 
    */
//   @DsfServiceEventHandler
//   public void handleDsfEvent(IResumedDMEvent event) {
//      System.err.println(String.format("handleDsfEvent(IResumedDMEvent) : reason = " + event.getReason()));
//   }

}