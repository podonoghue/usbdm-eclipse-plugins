package testmem.dsf;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExitedDMEvent;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IResumedDMEvent;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IStartedDMEvent;
import org.eclipse.cdt.dsf.debug.service.IRunControl.ISuspendedDMEvent;
import org.eclipse.cdt.dsf.debug.service.command.ICommand;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.mi.service.command.output.MIDataReadMemoryInfo;
import org.eclipse.cdt.dsf.service.DsfServiceEventHandler;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.dsf.service.DsfSession.SessionEndedListener;
import org.eclipse.cdt.dsf.service.DsfSession.SessionStartedListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.model.MemoryByte;

public class GdbDsfSessionListener implements SessionStartedListener, SessionEndedListener {

   DsfSession fDsfSession = null;

   public GdbDsfSessionListener() {
      DsfSession.addSessionStartedListener(this);
      DsfSession.addSessionEndedListener(this);
   }

   public void dispose() {
      DsfSession.removeSessionStartedListener(this);
      DsfSession.removeSessionEndedListener(this);
   }

   @Override
   public void sessionStarted(DsfSession dsfSession) {
      if (fDsfSession == null) {
         System.err.println(String.format("sessionStarted(IStartedDMEvent, s=%s) monitored", dsfSession.getId()));
         fDsfSession = dsfSession;
         dsfSession.addServiceEventListener(this,  null);
      }
      else {
         System.err.println(String.format("sessionStarted(IStartedDMEvent, s=%s) ignored", dsfSession.getId()));
      }
   }

   @Override
   public void sessionEnded(DsfSession dsfSession) {
      if (fDsfSession == dsfSession) {
         fDsfSession = null;
         dsfSession.removeServiceEventListener(this);
         System.err.println(String.format("sessionEnded(IStartedDMEvent, s=%s) stop monitoring", dsfSession.getId()));
      }
      else {
         System.err.println(String.format("sessionEnded(IStartedDMEvent, s=%s) ignored", dsfSession.getId()));
      }
   }

   /**
    * 
    */
   @DsfServiceEventHandler
   public void handleDsfEvent(IStartedDMEvent event) {
      String sessionId = event.getDMContext().getSessionId();
      System.err.println(String.format("handleDsfEvent(IStartedDMEvent, s=%s, r=%s)", sessionId, event.toString()));
   }

   /**
    * 
    */
   @DsfServiceEventHandler
   public void handleDsfEvent(IExitedDMEvent event) {
      String sessionId = event.getDMContext().getSessionId();
      System.err.println(String.format("handleDsfEvent(IExitedDMEvent, s=%s, r=%s)", sessionId, event.toString()));
   }

   /**
    * 
    */
   @DsfServiceEventHandler
   public void handleDsfEvent(ISuspendedDMEvent event) {
      String sessionId = event.getDMContext().getSessionId();
      System.err.println(String.format("handleDsfEvent(ISuspendedDMEvent, s=%s, r=%s)", sessionId, event.toString()));
   }

   /**
    * 
    */
   @DsfServiceEventHandler
   public void handleDsfEvent(IResumedDMEvent event) {
      String sessionId = event.getDMContext().getSessionId();
      System.err.println(String.format("handleDsfEvent(IResumedDMEvent, s=%s) : reason = %s", sessionId, event.getReason()));
   }

   public void readMemory(long address, DataRequestMonitor<Long> drm) {
      if (fDsfSession == null) {
         drm.done(new Status(IStatus.ERROR, testmem.Activator.getPluginId(), "No session open"));
         return;
      }
   }

   /**
    *  Read from target memory
    *  
    *  @param address   address in target memory
    *  @param size      number of bytes to read
    *  
    *  @return byte[size] containing the data read or null on failure
    */
   public void readMemory(long address, int size, final DataRequestMonitor<byte[]> drm) {
      System.err.println(String.format("GdbDsfInterface.readMemory(0x%X, %d)", address, size));

      DsfServicesTracker  tracker = null;
      if (fDsfSession != null) {
         tracker = new DsfServicesTracker(testmem.Activator.getBundleContext(), fDsfSession.getId());
      }
      //      System.err.println("GdbDsfInterface.readMemory()");
      if (tracker == null) {
         System.err.println("GdbDsfInterface.readMemory() tracker = null");
         drm.done(new Status(IStatus.ERROR, testmem.Activator.getPluginId(), "tracker = null"));
         return;
      }
      final IGDBControl fGdbControl = (IGDBControl) tracker.getService(IGDBControl.class);
      if (fGdbControl == null) {
         drm.done(new Status(IStatus.ERROR, testmem.Activator.getPluginId(), "fGdbControl = null"));
         return;
      }
      CommandFactory factory = fGdbControl.getCommandFactory();
      final ICommand<MIDataReadMemoryInfo> info_rm = factory.createMIDataReadMemory(fGdbControl.getContext(), 0L, Long.toString(address), 0, 1, 1, size, null);

      DataRequestMonitor<MIDataReadMemoryInfo> dataRequestMonitor = new DataRequestMonitor<MIDataReadMemoryInfo>(fGdbControl.getExecutor(), drm) {
         @Override
         protected void handleFailure() {
            System.err.println("GdbDsfSessionListener.readMemory().query.handleFailure() pid = " + Thread.currentThread().getId());
            drm.done(getStatus());
         }
         @Override
         protected void handleSuccess() {
            System.err.println("GdbDsfSessionListener.readMemory().query.handleCompleted() pid = " + Thread.currentThread().getId());
            MemoryByte[] bytes = this.getData().getMIMemoryBlock();
            byte[] arraybytes = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
               arraybytes[i] = bytes[i].getValue();
            }
            drm.setData(arraybytes);
            drm.done();
         }
      };
      fGdbControl.queueCommand(info_rm, dataRequestMonitor);
      tracker.dispose();
      return;
   }



}