package net.sourceforge.usbdm.peripherals.view;

import java.util.ArrayList;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;
import net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel;

import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.debug.internal.core.model.CDebugElement;
import org.eclipse.cdt.debug.internal.core.model.CDebugTarget;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.cdi.model.Target;
import org.eclipse.cdt.launch.AbstractCLaunchDelegate;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IProcess;

@SuppressWarnings("restriction")
public class GdbMiSessionListener implements IDebugEventSetListener {

   /*
    *
    */
   private static GdbMiSessionListener      singleton             = null;
   private ArrayList<GdbSessionListener>    gdbSessionListeners   = null;
   
   // Current session
   private MISession session = null;
   
   // Only a single device model is supported
   private UsbdmDevicePeripheralsModel peripheralsModel  = null;

   private GdbMiSessionListener() {
//      System.err.println("GdbMiSessionListener()");
      gdbSessionListeners = new ArrayList<GdbSessionListener>();
      
      if (DebugPlugin.getDefault() != null) {
         DebugPlugin.getDefault().addDebugEventListener(this);
      }
   }

   /**
    * @return listener singleton
    */
   public static synchronized GdbMiSessionListener getListener() {
      if (singleton == null) {
         singleton = new GdbMiSessionListener();
      }
      return singleton;
   }

   /**
    * Determines device name from Event source
    * 
    * @param source Event source (from DebugEvent.getSource())
    * 
    * @return the device name e.g. MK20DX128M5
    */
   private String getDeviceName(DebugEvent event) {
      if (event == null) {
	     return null;
	  }
      String deviceName = null;
      Object source = event.getSource();
      if (source instanceof org.eclipse.cdt.debug.internal.core.model.CDebugTarget) {
         deviceName = ((org.eclipse.cdt.debug.internal.core.model.CDebugTarget)(source)).getCDISession().getAttribute(UsbdmSharedConstants.LAUNCH_DEVICE_NAME_KEY);
      }
      else if (source instanceof org.eclipse.cdt.debug.internal.core.model.CThread) {
         deviceName = ((org.eclipse.cdt.debug.internal.core.model.CThread)source).getCDISession().getAttribute(UsbdmSharedConstants.LAUNCH_DEVICE_NAME_KEY);
      }
//      System.err.println("getDeviceName(DebugEvent) => " + ((deviceName==null)?"null":deviceName));
      return deviceName;
   }

   /*
    * ========================================================
    *  Listeners on this DSF session interface
    * ========================================================
    */
   /**
    * Add a listener for GDB events
    */
   public void addListener(GdbSessionListener listener) {
      gdbSessionListeners.add(listener);
   }

   /**
    * Remove a listener for GDB events
    */
   public void removeListener(GdbSessionListener listener) {
      gdbSessionListeners.remove(listener);
   }

   /**
    * Provides a readable mapping of event Kind
    */
   String getKind(int kind) {
      switch(kind) {
      case DebugEvent.RESUME               : return "RESUME";
      case DebugEvent.SUSPEND              : return "SUSPEND";
      case DebugEvent.CREATE               : return "CREATE";
      case DebugEvent.TERMINATE            : return "TERMINATE";
      case DebugEvent.CHANGE               : return "CHANGE";
      case DebugEvent.MODEL_SPECIFIC       : return "MODEL_SPECIFIC";
      default                              : return "UKNOWN_EVENT_KIND";
      }
   }

   /**
    * Provides a readable mapping of event Detail
    */
   String getDetail(int detail) {
      switch(detail) {
      case DebugEvent.STEP_INTO            : return "STEP_INTO";
      case DebugEvent.STEP_OVER            : return "STEP_OVER";
      case DebugEvent.STEP_RETURN          : return "STEP_RETURN";
      case DebugEvent.STEP_END             : return "STEP_END";
      case DebugEvent.BREAKPOINT           : return "BREAKPOINT";
      case DebugEvent.CLIENT_REQUEST       : return "CLIENT_REQUEST";
      case DebugEvent.EVALUATION           : return "EVALUATION";
      case DebugEvent.EVALUATION_IMPLICIT  : return "EVALUATION_IMPLICIT";
      default                              : return "UKNOWN_DETAIL";
      }
   }
 
   /**
    * Gets the org.eclipse.cdt.debug.mi.core.cdi.model.Target from various contexts
    * 
    * @param context
    * 
    * @return the CDebugTarget found or null if none found
    */
   public static CDebugTarget getCDebugTarget(Object context) {
     Object target = null;
     if (context == null) {
       return null;
     }
     if ((context instanceof IProcess)) {
       context = ((IProcess)context).getLaunch();
     }
     if ((context instanceof AbstractCLaunchDelegate.CLaunch)) {
       target = ((AbstractCLaunchDelegate.CLaunch)context).getDebugTarget();
     } else if ((context instanceof CDebugElement)) {
       target = ((CDebugElement)context).getDebugTarget();
     }
     if (target == null) {
       return null;
     }
     if ((target instanceof CDebugTarget)) {
       return (CDebugTarget)target;
     }
     return null;
   }
   
   /**
    * Gets the org.eclipse.cdt.debug.mi.core.cdi.model.Target from a org.eclipse.cdt.debug.mi.core.cdi.model.Target
    * 
    * @param debugtarget
    * @return the Target found or null if none found
    */
   public static Target getMiTarget(CDebugTarget debugtarget) {
     if (debugtarget == null) {
       return null;
     }
     ICDISession cdiSession = debugtarget.getCDISession();
     if (cdiSession != null)
     {
       ICDITarget mitarget = debugtarget.getCDITarget();
       if ((mitarget != null) && ((mitarget instanceof Target))) {
         return (Target)mitarget;
       }
     }
     return null;
   }
   
   /**
    * Returns the session corresponding to the context given
    * 
    * @param context
    * 
    * @return MISession corresponding to the context or null
    */
   public static MISession getSession(Object context) {
      if (context == null) {
         return null;
      }
      if (context instanceof MISession) {
         return (MISession)context;
      }
      if ((context instanceof IProcess)) {
         context = ((IProcess) context).getLaunch();
      }
      if (((context instanceof CDebugElement)) || ((context instanceof AbstractCLaunchDelegate.CLaunch))) {
         Target miTarget = getMiTarget(getCDebugTarget(context));
         if (miTarget != null) {
            return miTarget.getMISession();
         }
      }
      return null;
   }
   
   /*
    * ===================================================================================================================================================
    * ===================================================================================================================================================
    * ===================================================================================================================================================
    */

   /**
    * Handles debug events
    * 
    * @param events - The debug events
    * 
    * Session CREATE    - Saves session ID & notifies gdbSessionListeners.SessionCreate
    * Session TERMINATE - Notifies gdbSessionListeners.SessionCreate
    * Session SUSPEND   - Notifies gdbSessionListeners.SessionSuspend
    */
   @Override
   public void handleDebugEvents(DebugEvent[] events) {

      for (DebugEvent event : events) {
         Object source = event.getSource();
         
         MISession sourceSession = getSession(source);
         if (sourceSession == null) {
            // Can't find the session - ignore it (may be DSF session)
//            System.err.println("handleDebugEvents() sourceSession = NULL");
            return;
         }

//         System.err.println("================================================================================================");
//         System.err.println(String.format("handleDebugEvents() DebugEvent = (K=%s, D=%s)", 
//               getKind(event.getKind()), getDetail(event.getDetail())));
//         System.err.println(              "handleDebugEvents() DebugEvent = " + event.toString());
//         System.err.println(String.format("handleDebugEvents() Source     = (S=%s, C=%s)", source, source.getClass()));

         if ((session != null) && (sourceSession != session)) {
            // Not from the session we are interested in - ignore it
//            System.err.println("handleDebugEvents() sourceSession differs = " + sourceSession);
            return;
         }
//         System.err.println("handleDebugEvents() sourceSession            = " + sourceSession);
//         System.err.println("handleDebugEvents() sourceSession.getclass() = " +  sourceSession.getClass());
         if (session == null) {
            // Attach to session - Either a new session of we have just become interested (view opened)
            session = sourceSession;
            String deviceName = getDeviceName(event); 
            if (deviceName != null) {
//               System.err.println("handleDebugEvents() Device Name = " + deviceName);
               peripheralsModel = new UsbdmDevicePeripheralsModel(deviceName, new GdbMiInterface(session));
            }
//            System.err.println(String.format("handleDebugEvents() new session on the fly, device = \'%s\', session = \'%s\'", deviceName, session.toString()));
         }
         switch (event.getKind()) {
         case DebugEvent.SUSPEND : 
            if (peripheralsModel != null) {
               // Set current register values as the 'reference' for changed values
               peripheralsModel.getModel().setChangeReference();
               // Set all registers as stale
               peripheralsModel.getModel().setNeedsUpdate(true);
            }
            for (GdbSessionListener sessionListener : gdbSessionListeners) {
               sessionListener.sessionSuspended(peripheralsModel, GdbSessionListener.EventType.getEventFromMiEvent(event.getKind()));
            }
            break;
         case DebugEvent.CREATE : 
            {
            for (GdbSessionListener sessionListener : gdbSessionListeners) {
               sessionListener.sessionStarted(peripheralsModel);
            }
            }
            break;
         case DebugEvent.TERMINATE : 
            for (GdbSessionListener sessionListener : gdbSessionListeners) {
               sessionListener.sessionTerminated(peripheralsModel);
            }
            session          = null;
            peripheralsModel = null;
            break;
         case DebugEvent.RESUME : 
         case DebugEvent.CHANGE : 
         case DebugEvent.MODEL_SPECIFIC : 
         default :
            break;
         }
      }
   }

   /**
    * Queries if there is a current active session
    * 
    * @return true if session is active
    */
   protected boolean isSessionActive() {
      return session != null;
   }

   /**
    * 
    */
   public void dispose() {
      //      currentSession = null;
      if (DebugPlugin.getDefault() != null) {
         DebugPlugin.getDefault().removeDebugEventListener(this);
      }
   }
   
}
