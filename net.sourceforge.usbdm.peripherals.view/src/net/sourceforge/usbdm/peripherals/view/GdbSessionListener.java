package net.sourceforge.usbdm.peripherals.view;

import net.sourceforge.usbdm.peripherals.model.UsbdmDevicePeripheralsModel;

import org.eclipse.cdt.dsf.debug.service.IRunControl.StateChangeReason;
import org.eclipse.debug.core.DebugEvent;

public interface GdbSessionListener {

   enum EventType {
      EVT_BREAKPOINT,
      EVT_WATCHPOINT,
      EVT_STEP,
      EVT_OTHER,
      EVT_UNKNOWN;
      
      /**
       * Maps DSF StateChangeReason to EventType
       * 
       * @param reason Value to map
       * 
       * @return Corresponding EventType 
       */
      public static EventType getEventFromDsfEvent(StateChangeReason reason) {
         switch (reason) {
         case BREAKPOINT :
         case EVENT_BREAKPOINT:
            return EVT_BREAKPOINT;
         case STEP:
            return EVT_STEP;
         case CONTAINER:
         case ERROR:
         case EVALUATION:
         case EXCEPTION:
         case SHAREDLIB:
         case SIGNAL:
         case UNKNOWN:
         case USER_REQUEST:
            return EVT_OTHER;
         case WATCHPOINT:
            return EVT_WATCHPOINT;
         default:
            return EVT_UNKNOWN;
         }
      }
      
      /**
       * Maps MI Event Type to EventType
       * 
       * @param reason Value to map
       * 
       * @return Corresponding EventType 
       */
      public static EventType getEventFromMiEvent(int reason) {
         switch (reason) {
         case DebugEvent.SUSPEND :
            return EVT_BREAKPOINT;
         case DebugEvent.STEP_END :
            return EVT_STEP;
         default:
            return EVT_UNKNOWN;
         }
      }
   };
   
   /**
    * Session associated with this model has started execution
    * 
    * @param model Model associated with this session
    */
   public void sessionStarted(UsbdmDevicePeripheralsModel model);

   /**
    * Session associated with this model has terminated
    * 
    * @param model Model associated with this session
    */
   public void sessionTerminated(UsbdmDevicePeripheralsModel model);

   /**
    * Session associated with this model has been suspended
    * 
    * @param model Model associated with this session
    * @param reason  Reason for Suspension
    */
   public void sessionSuspended(UsbdmDevicePeripheralsModel model, GdbSessionListener.EventType reason);

}
