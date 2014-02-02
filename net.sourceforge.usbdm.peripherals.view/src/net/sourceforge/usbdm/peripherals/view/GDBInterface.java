package net.sourceforge.usbdm.peripherals.view;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.debug.internal.core.model.CDebugElement;
import org.eclipse.cdt.debug.internal.core.model.CDebugTarget;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIFormat;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.cdi.model.Target;
import org.eclipse.cdt.debug.mi.core.command.MIDataReadMemory;
import org.eclipse.cdt.debug.mi.core.command.MIDataWriteMemory;
import org.eclipse.cdt.debug.mi.core.output.MIMemory;
import org.eclipse.cdt.debug.mi.core.output.MIResultRecord;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.cdt.dsf.concurrent.Query;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.command.ICommand;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.mi.service.command.commands.CLICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIDataReadMemoryInfo;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.dsf.ui.viewmodel.datamodel.IDMVMContext;
import org.eclipse.cdt.launch.AbstractCLaunchDelegate;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.ui.DebugUITools;

@SuppressWarnings("restriction")
public class GDBInterface implements IDebugEventSetListener {

   //   private Object currentSession = null;

   private ArrayList<GDBSessionListener> gdbSessionListeners;
   
   GDBInterface() {
//      System.err.println("GDBInterface()");
      gdbSessionListeners = new ArrayList<GDBSessionListener>();
   }

   void addListener(GDBSessionListener listener) {
      gdbSessionListeners.add(listener);
   }

   void removeListener(GDBSessionListener listener) {
      gdbSessionListeners.remove(listener);
   }

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

   String getDetail(int kind) {
      switch(kind) {
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
    * @param context
    * @return
    */
   public static CDebugTarget getCDebugTarget(Object context)
   {
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
    * @param debugtarget
    * @return
    */
   public static Target getMiTarget(CDebugTarget debugtarget)
   {
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
    * @param context
    * @return
    */
   public static Object getSession(Object context) {
      Object session = null;
      if (context != null) {
         if (((context instanceof DsfSession)) || ((context instanceof MISession))) {
            session = context;
         }
         if ((context instanceof IProcess)) {
            context = ((IProcess) context).getLaunch();
         }
         if ((context instanceof GdbLaunch)) {
            session = ((GdbLaunch) context).getSession();
         } else if ((context instanceof IDMVMContext)) {
            IDMContext dmc = ((IDMVMContext) context).getDMContext();
            if (dmc == null) {
               return null;
            }
            session = DsfSession.getSession(dmc.getSessionId());
         } else if (((context instanceof CDebugElement)) || ((context instanceof AbstractCLaunchDelegate.CLaunch))) {
            Target miTarget = getMiTarget(getCDebugTarget(context));
            if (miTarget != null) {
               session = miTarget.getMISession();
            }
         }
      }
      return session;
   }

   /**
    * Determines device name from Event source
    * 
    * @param source Event source (from DebugEvent.getSource())
    * @return
    */
   private String getDeviceName(Object source) {
      String deviceName = null;
      if (source instanceof org.eclipse.cdt.debug.internal.core.model.CDebugTarget) {
         deviceName = ((org.eclipse.cdt.debug.internal.core.model.CDebugTarget)(source)).getCDISession().getAttribute("net.sourceforge.usbdm.gdb.deviceName");
      }
      else if (source instanceof org.eclipse.cdt.debug.internal.core.model.CThread) {
         deviceName = ((org.eclipse.cdt.debug.internal.core.model.CThread)source).getCDISession().getAttribute("net.sourceforge.usbdm.gdb.deviceName");
      }
//      System.err.println("getDeviceName() => " + deviceName);
      return deviceName;
   }
   
   public String getDeviceName() {
      return this.deviceName;
   }
   
   // May be DsfSession or MISession
   private Object session = null;
   private String deviceName = null;

   public class GdbDebugEvent {
      
   }
   
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
//      System.err.println("handleDebugEvents(DebugEvent[])");

      for (DebugEvent event : events) {
         Object source = event.getSource();
//         System.err.println("================================================================================================");
//         System.err.println(String.format("handleDebugEvents() DebugEvent = (K=%s, D=%s)", 
//               getKind(event.getKind()), getDetail(event.getDetail())));
//         System.err.println(              "handleDebugEvents() DebugEvent = " + event.toString());
//         System.err.println(String.format("handleDebugEvents() Source     = (S=%s, C=%s)", source, source.getClass()));
         
//         if (source instanceof org.eclipse.cdt.debug.internal.core.model.CDebugTarget) {
//            String newDeviceName = ((org.eclipse.cdt.debug.internal.core.model.CDebugTarget)(source)).getCDISession().getAttribute("net.sourceforge.usbdm.gdb.deviceName");
//            if (newDeviceName != null) {
//               deviceName = newDeviceName;
//               System.err.println("handleDebugEvents() New device = " + deviceName);
//            }
//         }
//         else if (source instanceof org.eclipse.cdt.debug.mi.core.GDBProcess) {
//            org.eclipse.cdt.debug.mi.core.GDBProcess process = (org.eclipse.cdt.debug.mi.core.GDBProcess) source;
////            xxx = process.getAttribute("net.sourceforge.usbdm.gdb.deviceName");
////            ICDISession session = (ICDISession) process.getAdapter(ICDISession.class);
////            if (session != null) {
////               System.err.println("handleDebugEvents() session.getAttribute(\"device\") = " + session.getAttribute("device"));
////            }
//            try {
//               String newDeviceName = process.getLaunch().getLaunchConfiguration().getAttribute("net.sourceforge.usbdm.gdb.deviceName", (String)null);
//               if (newDeviceName != null) {
//                  deviceName = newDeviceName;
//                  System.err.println("handleDebugEvents() New device = " + deviceName);
//               }
//            } catch (CoreException e) {
//            }
//         }
//         else if (source instanceof org.eclipse.cdt.dsf.gdb.launching.GDBProcess) {
//            org.eclipse.cdt.dsf.gdb.launching.GDBProcess process = (org.eclipse.cdt.dsf.gdb.launching.GDBProcess) source;
//            try {
//               String newDeviceName = process.getLaunch().getLaunchConfiguration().getAttribute("net.sourceforge.usbdm.gdb.deviceName", (String)null);
//               if (newDeviceName != null) {
//                  deviceName = newDeviceName;
//                  System.err.println("handleDebugEvents() New device = " + deviceName);
//               }
//            } catch (CoreException e) {
//            }
//         }
//         
//         System.err.println("handleDebugEvents() Device Name = " + deviceName);
         Object sourceSession = getSession(source);
         if (sourceSession == null) {
            // Can't find the session - ignore it
//            System.err.println("handleDebugEvents() sourceSession = NULL");
            return;
         }
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
            String newDeviceName = getDeviceName(source); 
            if ((newDeviceName != null) && !newDeviceName.equalsIgnoreCase(deviceName)) {
               deviceName = newDeviceName;
            }
//            System.err.println(String.format("handleDebugEvents() new session on the fly, device = \'%s\', session = \'%s\'", deviceName, session.toString()));
         }
         switch (event.getKind()) {
         case DebugEvent.SUSPEND              : 
            for (GDBSessionListener sessionListener : gdbSessionListeners) {
               sessionListener.SessionSuspend(event);
            }
            break;
         case DebugEvent.CREATE               : 
//            session = sourceSession;
//            String newDeviceName = getDeviceName(source); 
//            if ((newDeviceName != null) && !newDeviceName.equalsIgnoreCase(deviceName)) {
//               deviceName = newDeviceName;
//            }
//            System.err.println(String.format("handleDebugEvents() CREATE, device = \'%s\', session = \'%s\'", deviceName, session.toString()));
            for (GDBSessionListener sessionListener : gdbSessionListeners) {
               sessionListener.SessionCreate(this);
            }
            break;
         case DebugEvent.TERMINATE            : 
            for (GDBSessionListener sessionListener : gdbSessionListeners) {
               sessionListener.SessionTerminate(event);
            }
            session    = null;
            deviceName = null;
            break;
         case DebugEvent.RESUME               : 
         case DebugEvent.CHANGE               : 
         case DebugEvent.MODEL_SPECIFIC       : 
         default                              :
            break;
         }
//         System.err.println("================================================================================================");
      }
   }

   /**
    * @return
    */
   protected boolean isSessionActive() {
      return session != null;
   }
   
   /**
    * 
    */
   protected void initialise() {
      if (DebugPlugin.getDefault() != null) {
         DebugPlugin.getDefault().addDebugEventListener(this);
      }
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
   
   /**
    * @param session
    * @param cLICommand
    * @param maximumTimeToWait
    * @return
    * @throws TimeoutException
    */
   public static MIInfo postCLICommand(DsfSession session, String cLICommand, int maximumTimeToWait)
         throws TimeoutException
       {
         if (!session.isActive()) {
           throw new TimeoutException("Session is terminated");
         }
         DsfServicesTracker tracker = new DsfServicesTracker(GdbPlugin.getBundleContext(), session.getId());
         IGDBControl fGdb = (IGDBControl)tracker.getService(IGDBControl.class);
         if (fGdb == null) {
           return null;
         }
         CLICommand<MIInfo> info = new CLICommand<MIInfo>(fGdb.getContext(), cLICommand);
         
         return executeQuery(info, "postCLICommand", "cLICommand=" + cLICommand, session, tracker, fGdb, maximumTimeToWait);
       }

   /**
    * @param command
    * @param functionName
    * @param details
    * @param session
    * @param tracker
    * @param fGdb
    * @param maximumTimeToWait
    * @return
    * @throws TimeoutException
    */
   private static MIInfo executeQuery(final ICommand<MIInfo> command, String functionName, String details,
                                      final DsfSession session, DsfServicesTracker tracker, final IGDBControl fGdb, int maximumTimeToWait)
                                            throws TimeoutException {

      Query<MIInfo> query = new Query<MIInfo>() {
         protected void execute(final DataRequestMonitor<MIInfo> rm) {
            fGdb.queueCommand(command, new DataRequestMonitor<MIInfo>(session.getExecutor(), null) {
               protected void handleCompleted()
               {
                  rm.setData((MIInfo)getData());
                  rm.done();
               }
            });
         }};
         ImmediateExecutor.getInstance().execute(query);
         MIInfo data = null;
         try {
            data = (MIInfo)query.get(maximumTimeToWait, TimeUnit.MILLISECONDS);
            if (data.isError()) {
               return null;
            }
         }
         catch (InterruptedException localInterruptedException) {}
         catch (ExecutionException localExecutionException) {}
         catch (TimeoutException localTimeoutException)
         {
            String message = functionName + " - failed, ErrorMessage: waiting time of " + maximumTimeToWait + " ms passed";
            throw new TimeoutException(message);
         }
         finally
         {
            tracker.dispose();
         }
         tracker.dispose();

         return data;
   }

   /**
    *  Read from target memory
    *  
    *  @param miSession 
    *  @param address   address in target memory
    *  @param size      number of bytes to read
    *  
    *  @return byte[size] containing the data read 
    */
   public static byte[] readMemory(MISession miSession, long address, int size) {
      if ((miSession == null)) {
         return null;
      }
      MIDataReadMemory mem;
      mem = miSession.getCommandFactory().createMIDataReadMemory(0L, Long.toString(address), MIFormat.HEXADECIMAL, 1, 1, size, null);
      byte[] ret = null;
      try {
         miSession.postCommand(mem);
         if (mem.getMIOutput().getMIResultRecord().getResultClass().equals(MIResultRecord.ERROR)) {
            return null;
         }
         MIMemory[] memoryData = mem.getMIDataReadMemoryInfo().getMemories();
         long[] tempData = memoryData[0].getData();
         ret = new byte[size];
         for (int index=0; (index<tempData.length)&&(index<size); index++) {
            ret[index] = (byte) (memoryData[0].getData()[index]);
         }
//         System.err.print(String.format("readMemory(%08X) => ", address));
//         for (int index=0; index<size; index++) {
//            System.err.print(String.format("%02X ", ret[index]));
//         }
//         System.err.println();
      } catch (MIException localMIException) {
         System.err.println("GDBInterface.readMemory() - exception" + localMIException.getMessage());
      }
      return ret;
   }
   
   /**
    * @param dsfSession
    * @param address
    * @param size
    * @param maximumTimeToWait
    * @return
    * @throws TimeoutException
    */
   public static byte[] readMemory(DsfSession dsfSession, long address, int size, int maximumTimeToWait)
         throws TimeoutException {
      if (!dsfSession.isActive()) {
         return null;
      }
      byte[] ret = null;
      DsfServicesTracker tracker = new DsfServicesTracker(GdbPlugin.getBundleContext(), dsfSession.getId());

      final IGDBControl fGdb = (IGDBControl) tracker.getService(IGDBControl.class);
      if (fGdb == null) {
         return null;
      }
      CommandFactory factory = fGdb.getCommandFactory();
      final ICommand<MIDataReadMemoryInfo> info_rm = 
            factory.createMIDataReadMemory(fGdb.getContext(), 0L, Long.toString(address), 0, 1, 1, size, null);

      Query<MIDataReadMemoryInfo> query = new Query<MIDataReadMemoryInfo>() {
         @Override
         protected void execute(final DataRequestMonitor<MIDataReadMemoryInfo> rm) {
            DataRequestMonitor<MIDataReadMemoryInfo> dataRequestMonitor = new DataRequestMonitor<MIDataReadMemoryInfo>(fGdb.getExecutor(), null) {
               protected void handleCompleted() {
                  rm.setData((MIDataReadMemoryInfo) getData());
                  rm.done();
               }
            };
            fGdb.queueCommand(info_rm, dataRequestMonitor);
         }
      };
      ImmediateExecutor.getInstance().execute(query);
      MIDataReadMemoryInfo data = null;
      try {
         data = (MIDataReadMemoryInfo) query.get(maximumTimeToWait,
               TimeUnit.MILLISECONDS);
         if (data.isError()) {
            data = null;
         }
         if (data != null) {
            MemoryByte[] bytes = data.getMIMemoryBlock();
            byte[] arraybytes = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
               arraybytes[i] = bytes[i].getValue();
            }
            ret = arraybytes;
         }
      } catch (InterruptedException localInterruptedException) {
      } catch (ExecutionException localExecutionException) {
      } catch (TimeoutException localTimeoutException) {
         throw new TimeoutException("readMemory - failed, ErrorMessage: waiting time of " + maximumTimeToWait + " ms passed");
      } finally {
         tracker.dispose();
      }
      return ret;
   }
   
   /**
    * @return
    */
   public static Object getSession() {
      Object session = DebugUITools.getDebugContext();
//      System.err.println("GDBInterface.getSession(), session.getClass() = " + session.getClass());
      session = getSession(session);
//      System.err.println("GDBInterface.getSession(), modified session.getClass() = " + session.getClass());
      return session;
   }

   public static int maxWaitTimeInInMilliseconds = 5000;

   /**
    * @param address
    * @param iByteCount
    * @param accessWidth
    * 
    * @return
    * @throws Exception 
    */
   public static byte[] readMemory(long address, int iByteCount, int accessWidth) throws Exception {
      
      // TODO - This is a horrible hack - damn you GDB!
      switch (accessWidth) {
      case 8:  
         // Check if either address or size is odd
         // This will be OK as treated as byte access by USBDM
         if (((address|iByteCount)&0x1) == 0) {
            // Must split block otherwise seen as word/half-word size access
            byte[] data1 = readMemory(address,              1, accessWidth);
            byte[] data2 = readMemory(address+1, iByteCount-1, accessWidth);
            byte[] data = new byte[iByteCount];
            data[0] = data1[0];
            System.arraycopy(data2, 0, data, 1, iByteCount-1);
            return data;
         }
         break;
      case 16:
         // Check if either address or size is not a multiple of 4 - 
         // This will be OK as treated as byte/half-word access by USBDM
         if (((address|iByteCount)&0x3) == 0) {
            // Must split block otherwise seen as word size access
            byte[] data1 = readMemory(address,              2, accessWidth);
            byte[] data2 = readMemory(address+2, iByteCount-2, accessWidth);
            byte[] data = new byte[iByteCount];
            data[0] = data1[0];
            data[1] = data1[1];
            System.arraycopy(data2, 0, data, 2, iByteCount-2);
            return data;
         }
         break;
      case 32:
         // Always considered OK
         break;
      default: 
         throw new Exception("Illegal access size"); 
      }
//      System.err.println(String.format("GDBInterface.readMemory(0x%08X, %d)", address, iByteCount));
      Object session = getSession();
      if (session instanceof DsfSession) {
         return readMemory((DsfSession)session, address, iByteCount, maxWaitTimeInInMilliseconds);
      }
      else if (session instanceof MISession) {
         return readMemory((MISession)session, address, iByteCount);
      }
//      else {
//         System.err.println("GDBInterface.readMemory() - no suitable session: " + ((session==null)?"null":session.getClass().toString()));
//      }
      return null;
   }

   /**
    * Returns the value shifted by offset
    * @param value
    * @param offset
    * @return
    */
   static long unsignedShift(byte value, int offset) {
      
      return (((long)value) & 0xFFL)<<offset;
   }
   
   /**
    * @param session
    * @param address
    * @param data
    * 
    * @return
    */
   private static void writeMemory(MISession miSession, long address, byte[] data) {
      
      if ((miSession == null)) {
         return;
      }
      StringBuffer buffer = new StringBuffer(10+(2*data.length));
      buffer.append("0x");
      
      // TODO - depends on endianess
      for (int index=data.length-1; index>=0; index--) {
         buffer.append(String.format("%02X", data[index]));
      }

      String value = buffer.toString();
      
//      System.err.println(String.format("GDBInterface.writeMemory(MISession, 0x%08X %d %s)", address, data.length, value));
      
      MIDataWriteMemory mem;
      mem = miSession.getCommandFactory().createMIDataWriteMemory(0L, Long.toString(address), MIFormat.HEXADECIMAL, data.length, value);
      try {
         miSession.postCommand(mem);
         if (mem.getMIOutput().getMIResultRecord().getResultClass().equals(MIResultRecord.ERROR)) {
            System.err.println(String.format("GDBInterface.writeMemory(MISession, 0x%08X %d %s) - failed", address, data.length, value));
            return;
         }
      } catch (MIException localMIException) {
         System.err.println("GDBInterface.readMemory() - exception" + localMIException.getMessage());
      }
   }
   
   /**
    * @param session
    * @param address
    * @param data
    * @param maxWaitTimeInInMilliseconds
    * 
    * @return
    */
   private static void writeMemory(DsfSession session, long address, byte[] data, int maxWaitTimeInInMilliseconds) {
      
   }

   /**
    * @param address    Address to write at
    * @param data       Data to write.  This must be 1, 2, 4 or 8 bytes dues to limitations of underlying GDB command used
    * 
    * @throws TimeoutException
    */
   public static void writeMemory(long address, byte[] data, int accessWidth) throws TimeoutException {
      //    System.err.println(String.format("GDBInterface.readMemory(0x%08X, %d)", address, iByteCount));
      Object session = getSession();
      if (session instanceof DsfSession) {
         writeMemory((DsfSession)session, address, data, maxWaitTimeInInMilliseconds);
      }
      else if (session instanceof MISession) {
         writeMemory((MISession)session, address, data);
      }
      else {
         System.err.println("GDBInterface.readMemory() - not suitable session: " + ((session==null)?"null":session.getClass().toString()));
      }
   }

}
