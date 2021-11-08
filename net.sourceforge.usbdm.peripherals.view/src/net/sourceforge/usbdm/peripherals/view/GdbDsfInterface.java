/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | Moved byte sex dependent code to gdbInterface                                     | V4.10.6.250
===============================================================================================================
*/
package net.sourceforge.usbdm.peripherals.view;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.cdt.dsf.concurrent.Query;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IExpressions;
import org.eclipse.cdt.dsf.debug.service.IExpressions.IExpressionDMContext;
import org.eclipse.cdt.dsf.debug.service.IExpressions.IExpressionDMData;
import org.eclipse.cdt.dsf.debug.service.IFormattedValues;
import org.eclipse.cdt.dsf.debug.service.IFormattedValues.FormattedValueDMContext;
import org.eclipse.cdt.dsf.debug.service.IFormattedValues.FormattedValueDMData;
import org.eclipse.cdt.dsf.debug.service.IFormattedValues.IFormattedDataDMContext;
import org.eclipse.cdt.dsf.debug.service.IRegisters;
import org.eclipse.cdt.dsf.debug.service.IRegisters.IRegisterDMContext;
import org.eclipse.cdt.dsf.debug.service.IRegisters.IRegisterDMData;
import org.eclipse.cdt.dsf.debug.service.IRegisters.IRegisterGroupDMContext;
import org.eclipse.cdt.dsf.debug.service.IStack;
import org.eclipse.cdt.dsf.debug.service.IStack.IFrameDMContext;
import org.eclipse.cdt.dsf.debug.service.IStack.IFrameDMData;
import org.eclipse.cdt.dsf.debug.service.command.ICommand;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.MIFormat;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.mi.service.command.output.MIDataReadMemoryInfo;
import org.eclipse.cdt.dsf.mi.service.command.output.MIDataWriteMemoryInfo;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.ui.DebugUITools;

public class GdbDsfInterface extends GdbCommonInterface {

   //   private static final int maxResetWaitTimeInMilliseconds      = 10000;  
   private static final int defaultMemoryWaitTimeInMilliseconds = 1000; // Usual time to wait for memory accesses
   private static final int reducedMemoryWaitTimeInMilliseconds =  100; // Reduced time to wait after an initial failure
   private int              memoryWaitTimeInMilliseconds        = defaultMemoryWaitTimeInMilliseconds; // Active memory wait time

   private DsfSession          fDsfSession  = null;
//   private DsfServicesTracker  tracker     = null;

   GdbDsfInterface(DsfSession dsfSession) {
      this.fDsfSession  = dsfSession;
//      if (fDsfSession != null) {
//         tracker = new DsfServicesTracker(Activator.getBundleContext(), fDsfSession.getId());
//      }
   }

   /*
    * ========================================================
    *  Target access (via GDB)
    * ========================================================
    */
   /**
    *  Read from target memory
    *  
    *  @param address   address in target memory
    *  @param size      number of bytes to read
    *  
    *  @return byte[size] containing the data read or null on failure
    */
   private synchronized byte[] readMemory(long address, int size) throws TimeoutException {
//      System.err.println(String.format("GdbDsfInterface.readMemory(0x%X, %d)", address, size));
      
      DsfServicesTracker  tracker = null;
      if (fDsfSession != null) {
         tracker = new DsfServicesTracker(Activator.getBundleContext(), fDsfSession.getId());
      }
//      System.err.println("GdbDsfInterface.readMemory()");
      if (tracker == null) {
         System.err.println("GdbDsfInterface.readMemory() tracker = null");
         return null;
      }
      byte[] ret = null;
      final IGDBControl fGdb = (IGDBControl) tracker.getService(IGDBControl.class);
      if (fGdb == null) {
         System.err.println("GdbDsfInterface.readMemory() fGdb = null");
         return null;
      }
      CommandFactory factory = fGdb.getCommandFactory();
      final ICommand<MIDataReadMemoryInfo> info_rm = factory.createMIDataReadMemory(fGdb.getContext(), 0L, Long.toString(address), 0, 1, 1, size, null);

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
      MIDataReadMemoryInfo result = null;
      try {
         result = (MIDataReadMemoryInfo) query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
         // Reset wait time
         memoryWaitTimeInMilliseconds = defaultMemoryWaitTimeInMilliseconds;
         if ((result != null) && !result.isError()) {
            MemoryByte[] bytes = result.getMIMemoryBlock();
            byte[] arraybytes = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
               arraybytes[i] = bytes[i].getValue();
            }
            ret = arraybytes;
         }
         else {
            System.err.println("GdbDsfInterface.readMemory(DSF) - failed, result == null or result.isError()");
         }
      } catch (TimeoutException localTimeoutException) {
         System.err.println("GdbDsfInterface.readMemory() localTimeoutException - waiting time of " + memoryWaitTimeInMilliseconds + " ms exceeded");
         // Reduced wait on next failure
         memoryWaitTimeInMilliseconds = reducedMemoryWaitTimeInMilliseconds;
      } catch (Exception e) {
         System.err.println("GdbDsfInterface.readMemory() - failed, ErrorMessage: " + e.getMessage());
      } finally {
         tracker.dispose();
      }
//      System.err.println(String.format("GdbDsfInterface.readMemory() " + ret[0]));
      return ret;
   }

   /**
    * Wrapper that handles DSF or MI memory reads
    * 
    * @param address       Address to read from
    * @param byteCount     Number of bytes to read
    * @param accessWidth   Access size (8, 16, 32 bits) to use
    * 
    * @return              Data read
    * 
    * @throws Exception 
    */
   @Override
   public byte[] readMemory(long address, int byteCount, int accessWidth) throws Exception {
      // There doesn't seem to be a mechanism for indicating the width of a write via GDB. Why??
      // TODO This is a horrible hack - damn you GDB!
      switch (accessWidth) {
      case 8:  
         // Check if either address or size is odd
         // This will be OK as treated as byte access by USBDM
         if (((address|byteCount)&0x1) == 0) {
            // Must split block otherwise seen as word/half-word size access
            byte[] data1 = readMemory(address,              1, accessWidth);
            byte[] data2 = readMemory(address+1, byteCount-1, accessWidth);
            byte[] data = new byte[byteCount];
            data[0] = data1[0];
            System.arraycopy(data2, 0, data, 1, byteCount-1);
            return data;
         }
         break;
      case 16:
         // Check if either address or size is not a multiple of 4 - 
         // This will be OK as treated as byte/half-word access by USBDM
         if (((address|byteCount)&0x3) == 0) {
            // Must split block otherwise seen as word size access
            byte[] data1 = readMemory(address,              2, accessWidth);
            byte[] data2 = readMemory(address+2, byteCount-2, accessWidth);
            byte[] data = new byte[byteCount];
            data[0] = data1[0];
            data[1] = data1[1];
            System.arraycopy(data2, 0, data, 2, byteCount-2);
            return data;
         }
         break;
      case 32:
         // Always considered OK
         break;
      default: 
         throw new Exception("Illegal access size"); 
      }
      //      System.err.println(String.format("GdbDsfInterface.readMemory(0x%08X, %d)", address, iByteCount));
      return readMemory(address, byteCount);
   }

   /**
    * @param address
    * @param data
    * 
    * @return Number of bytes written (-1 => error)
    */
   private int writeMemory(long address, byte[] data) {
      
      StringBuffer buffer = new StringBuffer(10+(2*data.length));
      buffer.append("0x");
      for (int index = data.length - 1; index >= 0; index--) {
         buffer.append(String.format("%02X", data[index]));
      }
      String value = buffer.toString();
      
      DsfServicesTracker  tracker = null;
      if (fDsfSession != null) {
         tracker = new DsfServicesTracker(Activator.getBundleContext(), fDsfSession.getId());
      }
      if (tracker == null) {
         System.err.println("GdbDsfInterface.writeMemory(DSF) tracker = null");
         return 0;
      }
      final IGDBControl fGdb = tracker.getService(IGDBControl.class);
      if (fGdb == null) {
         return -1;
      }
      CommandFactory factory = fGdb.getCommandFactory();
      int format;
      if (value.contains("x")) {
         format = MIFormat.HEXADECIMAL;
      } else {
         format = MIFormat.DECIMAL;
      }
      final ICommand<MIDataWriteMemoryInfo> info_wm = 
            factory.createMIDataWriteMemory(fGdb.getContext(), 0L, Long.toString(address), format, data.length, value);
      
      Query<MIDataWriteMemoryInfo> query = new Query<MIDataWriteMemoryInfo>() {
         protected void execute(
               final DataRequestMonitor<MIDataWriteMemoryInfo> rm) {
            fGdb.queueCommand( info_wm, new DataRequestMonitor<MIDataWriteMemoryInfo>(fGdb.getExecutor(), null) {
               protected void handleCompleted() {
                  rm.setData((MIDataWriteMemoryInfo) getData());
                  rm.done();
               }
            });
         }
      };
      ImmediateExecutor.getInstance().execute(query);
      int ret = -1;
      try {
         MIDataWriteMemoryInfo result = (MIDataWriteMemoryInfo) query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
         // Reset wait time
         memoryWaitTimeInMilliseconds = defaultMemoryWaitTimeInMilliseconds;
         if ((result != null) && !result.isError()) {
            ret = data.length;
         }
         else {
            System.err.println("GdbDsfInterface.writeMemory() - failed, result == null or result.isError()");
         }
      } catch (TimeoutException localTimeoutException) {
//         System.err.println("GdbDsfInterface.readMemory(DSF) localTimeoutException - waiting time of " + memoryWaitTimeInMilliseconds + " ms exceeded");
         // Reduced wait on next failure
         memoryWaitTimeInMilliseconds = reducedMemoryWaitTimeInMilliseconds;
      } catch (Exception e) {
         System.err.println("GdbDsfInterface.setFrame() - failed, ErrorMessage: " + e.getMessage());
      } finally {
         tracker.dispose();
      }
      return ret;
   }

   /**
    * Wrapper that handles memory writes
    * 
    * @param address      Address to write at
    * @param data         Data to write.  This must be 1, 2, 4 or 8 bytes due to limitations of underlying GDB command used
    * @param accessWidth  Access size (8, 16, 32 bits) to use
    * 
    * @throws TimeoutException
    */
   @Override
   public void writeMemory(long address, byte[] data, int accessWidth) throws TimeoutException {
      //      System.err.println(String.format("GdbDsfInterface.writeMemory(0x%08X, %d)", address, data.length));
      writeMemory(address, data);
   }

   //   Pattern stringPattern = Pattern.compile("[\\s]*?((0x)?[0-9]+)[\\s]*");
   Pattern stringPattern = Pattern.compile(".*?((0x([0-9a-fA-F]+))|(0[0-7]*)|([1-9][0-9]*)).*?");

//   @Override
//   public long evaluateExpression(String expression) throws Exception {
//      //      System.err.println(String.format("GdbDsfInterface.evaluateExpression(%s)", expression));
//
//      if ((fDsfSession == null)) {
//         return 0;
//      }
//      DsfServicesTracker tracker = new DsfServicesTracker(Activator.getBundleContext(), fDsfSession.getId());
//      final IGDBControl fGdb = tracker.getService(IGDBControl.class);
//      if (fGdb == null) {
//         return 0;
//      }
//      CommandFactory factory = fGdb.getCommandFactory();
//      final ICommand<MIDataEvaluateExpressionInfo> info_wm = 
//            factory.createMIDataEvaluateExpression(fGdb.getContext(), expression);
//
//      Query<MIDataEvaluateExpressionInfo> query = new Query<MIDataEvaluateExpressionInfo>() {
//         @Override
//         protected void execute(
//               final DataRequestMonitor<MIDataEvaluateExpressionInfo> rm) {
//            fGdb.queueCommand(info_wm, new DataRequestMonitor<MIDataEvaluateExpressionInfo>(fGdb.getExecutor(), null) {
//               @Override
//               protected void handleCompleted() {
//                  rm.setData((MIDataEvaluateExpressionInfo) getData());
//                  rm.done();
//               }
//            });
//         }
//      };
//      ImmediateExecutor.getInstance().execute(query);
//      long rv = 0xDEADBEEF;
//      try {
//         MIDataEvaluateExpressionInfo result = 
//               (MIDataEvaluateExpressionInfo) query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
//         // Reset wait time
//         if ((result != null) && !result.isError()) {
//            //            System.err.println("GdbDsfInterface.readRegister() - ok");
//            //            System.err.println("GdbDsfInterface.readRegister() - result = "+result.toString());
//            //            System.err.println("GdbDsfInterface.readRegister() - result.getValue() = "+result.getValue());
//            Matcher matcher = stringPattern.matcher(result.getValue());
//            if (matcher.matches()) {
//               //               System.err.println(String.format("group 0 = "+matcher.group(0)));
//               //               System.err.println(String.format("group 1 = "+matcher.group(1)));
//               //               System.err.println(String.format("group 2 = "+matcher.group(2)));
//               //               System.err.println(String.format("group 3 = "+matcher.group(3)));
//               //               System.err.println(String.format("group 4 = "+matcher.group(4)));
//               //               System.err.println(String.format("group 5 = "+matcher.group(5)));
//               if ((matcher.group(3) != null) && (!matcher.group(3).isEmpty())) {
//                  //                  System.err.println(String.format("group 3 = "+matcher.group(3)));
//                  rv = Long.parseLong(matcher.group(3), 16);
//               }
//               else if ((matcher.group(4) != null) && (!matcher.group(4).isEmpty())) {
//                  //                  System.err.println(String.format("group 4 = "+matcher.group(4)));
//                  rv = Long.parseLong(matcher.group(4), 8);
//               }
//               else if ((matcher.group(5) != null) && (!matcher.group(5).isEmpty())) {
//                  //                  System.err.println(String.format("group 5 = "+matcher.group(5)));
//                  rv = Long.parseLong(matcher.group(5), 10);
//               }
//            }
//            //            System.err.println(String.format("rv = 0x%X (%d)", rv, rv));
//         }
//         else {
//            System.err.println("GdbDsfInterface.evaluateExpression() - failed, result == null or result.isError()");
//         }
//      } catch (TimeoutException localTimeoutException) {
//         System.err.println("GdbDsfInterface.readMemory(DSF) localTimeoutException - waiting time of " + memoryWaitTimeInMilliseconds + " ms exceeded");
//         // Reduced wait on next failure
//         memoryWaitTimeInMilliseconds = reducedMemoryWaitTimeInMilliseconds;
//      } catch (Exception e) {
//         System.err.println("GdbDsfInterface.setFrame() - failed, ErrorMessage: " + e.getMessage());
//      } finally {
//         tracker.dispose();
//      }
//      System.err.println(String.format("GdbDsfInterface.evaluateExpression(%4s) => 0x%08X (%d)", expression, rv, rv));
//      return rv;
//   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripherals.view.GdbCommonInterface#readRegister(int)
    */
   @Override
   public long setFrame(int frameNum) throws Exception {
      DsfServicesTracker  tracker = null;
      if (fDsfSession != null) {
         tracker = new DsfServicesTracker(Activator.getBundleContext(), fDsfSession.getId());
      }
      if ((tracker == null)) {
         return 0;
      }
      final IGDBControl fGdb = tracker.getService(IGDBControl.class);
      if (fGdb == null) {
         return 0;
      }
      CommandFactory factory = fGdb.getCommandFactory();
      final ICommand<MIInfo> info_wm = factory.createMIStackSelectFrame(fGdb.getContext(), frameNum);
      Query<MIInfo> query = new Query<MIInfo>() {
         @Override
         protected void execute(
               final DataRequestMonitor<MIInfo> rm) {
            fGdb.queueCommand(info_wm, new DataRequestMonitor<MIInfo>(fGdb.getExecutor(), null) {
               @Override
               protected void handleCompleted() {
                  rm.setData((MIInfo) getData());
                  rm.done();
               }
            });
         }
      };
      ImmediateExecutor.getInstance().execute(query);
      int rv = -1;
      try {
         MIInfo result = (MIInfo) query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
         if ((result != null) && !result.isError()) {
            rv = 0;
         }
         else {
            System.err.println("GdbDsfInterface.setFrame() - failed, result == null or result.isError()");
            rv = -1;
         }
      } catch (Exception e) {
         System.err.println("GdbDsfInterface.setFrame() - failed, ErrorMessage: " + e.getMessage());
      } finally {
         tracker.dispose();
      }
      System.err.println(String.format("GdbDsfInterface.setFrame(%d) => %d", frameNum, rv));
      return rv;
   }

   private IDMContext getExecutionContext() {
      IDMContext execDmc = null;
      IAdaptable debugContext = DebugUITools.getDebugContext();
      if (debugContext != null) {
         execDmc = (IDMContext) debugContext.getAdapter(IDMContext.class);
//         IDMContext idmContext = (IDMContext)debugContext.getAdapter(IDMContext.class);
//         if (idmContext != null) {
//            execDmc = DMContexts.getAncestorOfType(idmContext, IMIExecutionDMContext.class);
//         }
      }
      return execDmc;
   }

   @SuppressWarnings("unused")
   private int getStackDepth(final IStack fStack) {
      Query<Integer> query = new Query<Integer>() {
         @Override
         protected void execute(DataRequestMonitor<Integer> rm) {
            fStack.getStackDepth(getExecutionContext(), 4, rm);
         }
      };
      ImmediateExecutor.getInstance().execute(query);
      Integer result = null;
      try {
         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (result == null) {
         System.err.println("GdbDsfInterface.getStackDepth(): result = null");
      }
//      else {
//         System.err.println("GdbDsfInterface.getStackDepth(): result = " + result);
//      }
      return result;
   }

   private IFrameDMContext getTopFrame(final IStack fStack) {
      Query<IFrameDMContext> query = new Query<IFrameDMContext>() {
         @Override
         protected void execute(DataRequestMonitor<IFrameDMContext> rm) {
            fStack.getTopFrame(getExecutionContext(), rm);
         }
      };
      ImmediateExecutor.getInstance().execute(query);
      IFrameDMContext result = null;
      try {
         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (result == null) {
         System.err.println("GdbDsfInterface.getTopFrame(): frameDMContext = null");
      }
//      else {
//         System.err.println("GdbDsfInterface.getTopFrame(): frameDMContext = " + result + "," + result.getClass());
//      }
      return result;
   }

   private IFrameDMContext[] getFrames(final IStack stackService) {
      
      Query<IFrameDMContext[]> query = new Query<IFrameDMContext[]>() {
         @Override
         protected void execute(DataRequestMonitor<IFrameDMContext[]> rm) {
            stackService.getFrames(getExecutionContext(), rm);
         }
      };
      ImmediateExecutor.getInstance().execute(query);
      IFrameDMContext[] result = null;
      try {
         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (result == null) {
         System.err.println("GdbDsfInterface.getFrames(): frameDMContexts[] = null");
      }
//      else {
//         System.err.println("GdbDsfInterface.getFrames(): frameDMContexts[] = " + result + "," + result.getClass());
//      }
      return result;
   }

   private IFrameDMData getFrameDMData(final IStack fStack, final IFrameDMContext context) {
      Query<IFrameDMData> query = new Query<IFrameDMData>() {
         @Override
         protected void execute(DataRequestMonitor<IFrameDMData> rm) {
            fStack.getFrameData(context, rm);
         }
      };
      ImmediateExecutor.getInstance().execute(query);
      IFrameDMData result = null;
      try {
         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (result == null) {
         System.err.println("GdbDsfInterface.getFrameDMData(): result = null");
      }
//      else {
//         System.err.println("GdbDsfInterface.getFrameDMData(): result = " + result + "," + result.getClass());
//      }
      return result;
   }

   private IRegisterGroupDMContext[] getRegisterGroups(final IDMContext context, final IRegisters fRegister) {
      
      Query<IRegisterGroupDMContext[]> query = new Query<IRegisterGroupDMContext[]>() {
         @Override
         protected void execute(DataRequestMonitor<IRegisterGroupDMContext[]> rm) {
            fRegister.getRegisterGroups(context, rm);
         }
      };
      ImmediateExecutor.getInstance().execute(query);
      IRegisterGroupDMContext[] result = null;
      try {
         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (result == null) {
         System.err.println("GdbDsfInterface.getRegisterGroups(): result = null");
      }
//      else {
//         System.err.println("GdbDsfInterface.getRegisterGroups(): result = " + result + "," + result.getClass());
//      }
      return result;
   }

   private IRegisterDMContext[] getRegisters(final IRegisters fRegister, final IRegisterGroupDMContext registerGroup) {
      
      Query<IRegisterDMContext[]> query = new Query<IRegisterDMContext[]>() {
         @Override
         protected void execute(DataRequestMonitor<IRegisterDMContext[]> rm) {
            fRegister.getRegisters(registerGroup, rm);
         }
      };
      ImmediateExecutor.getInstance().execute(query);
      IRegisterDMContext[] result = null;
      try {
         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (result == null) {
         System.err.println("GdbDsfInterface.getRegisters(): result = null");
      }
//      else {
//         System.err.println("GdbDsfInterface.getRegisters(): result = " + result + "," + result.getClass());
//      }
      return result;
   }

   private IRegisterDMData getRegisterData(final IRegisters fRegister, final IRegisterDMContext context) {
      Query<IRegisterDMData> query = new Query<IRegisterDMData>() {
         @Override
         protected void execute(DataRequestMonitor<IRegisterDMData> rm) {
            fRegister.getRegisterData(context, rm);
         }
      };
      ImmediateExecutor.getInstance().execute(query);
      IRegisterDMData result = null;
      try {
         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (result == null) {
         System.err.println("GdbDsfInterface.getRegisterData(): result = null");
      }
//      else {
//         System.err.println("GdbDsfInterface.getRegisterData(): result = " + result + "," + result.getClass());
//      }
      return result;
   }

   @SuppressWarnings("unused")
   private IExpressionDMData getExpressionDMData(final IExpressions expressionService, final IFrameDMContext frame) {

      final IExpressionDMContext fpExpression = expressionService.createExpression(frame, "$fp");

      Query<IExpressionDMData> query = new Query<IExpressionDMData>() {
         @Override
         protected void execute(DataRequestMonitor<IExpressionDMData> rm) {
            expressionService.getExpressionData(fpExpression, rm);
         }
      };
      ImmediateExecutor.getInstance().execute(query);

      IExpressionDMData result = null;
      try {
         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (result == null) {
         System.err.println("GdbDsfInterface.getExpressionDMData(): result = null");
      }
//      else {
//         System.err.println("GdbDsfInterface.getExpressionDMData(): result = " + result + "," + result.getClass());
//      }
      return result;
   }

   private FormattedValueDMData getFormattedValueDMData(final IExpressions expressionService, final IDMContext frame, String expression) {

      final IExpressionDMContext expressionDMContext = 
            expressionService.createExpression(frame, expression);
      final FormattedValueDMContext formattedValueDMContext =
            expressionService.getFormattedValueContext(expressionDMContext, IFormattedValues.DECIMAL_FORMAT);
      
      Query<FormattedValueDMData> query = new Query<FormattedValueDMData>() {
         @Override
         protected void execute(DataRequestMonitor<FormattedValueDMData> rm) {
            expressionService.getFormattedExpressionValue(formattedValueDMContext, rm);
         }
      };
      ImmediateExecutor.getInstance().execute(query);

      FormattedValueDMData result = null;
      try {
         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (result == null) {
         System.err.println("GdbDsfInterface.getFormattedValueDMData(): result = null");
      }
//      else {
//         System.err.println("GdbDsfInterface.getFormattedValueDMData(): result = " + result + "," + result.getClass());
//      }
      return result;
   }

   @SuppressWarnings("unused")
   private IRegisterDMContext findRegister(final IRegisters registerService, final IDMContext context, final String registerName) {

      Query<IRegisterDMContext> query = new Query<IRegisterDMContext>() {
         @Override
         protected void execute(DataRequestMonitor<IRegisterDMContext> rm) {
            registerService.findRegister(context, registerName, rm);
         }
      };
      ImmediateExecutor.getInstance().execute(query);

      IRegisterDMContext result = null;
      try {
         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (result == null) {
         System.err.println("GdbDsfInterface.findregister(): result = null");
      }
      else {
         System.err.println("GdbDsfInterface.findregister(): result = " + result + "," + result.getClass());
      }
      return result;
   }

   private FormattedValueDMData getFormattedExpressionValue(
         final IRegisters registerService, 
         final FormattedValueDMContext fvDMContext) {

      Query<FormattedValueDMData> query = new Query<FormattedValueDMData>() {
         
         @Override
         protected boolean isExecutionRequired() {
            return true;
         }

         @Override
         protected void execute(DataRequestMonitor<FormattedValueDMData> rm) {
            registerService.getFormattedExpressionValue(fvDMContext, rm);
            FormattedValueDMData res = rm.getData();
            if (res == null) {
               System.err.println("GdbDsfInterface.getFormattedExpressionValue(): res = null");
            }
            else {
               System.err.println("GdbDsfInterface.getFormattedExpressionValue(): res = " + 
                     res + ",\n     getFormattedValue() = " + res.getFormattedValue());
            }
         }
      };
      registerService.getExecutor().execute(query);

      FormattedValueDMData result = null;
      try {
         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
         System.err.println("GdbDsfInterface.getFormattedExpressionValue(): query.isDone() = " + query.isDone());
         System.err.println("GdbDsfInterface.getFormattedExpressionValue(): query.getSubmitted() = " + query.getSubmitted());
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (result == null) {
         System.err.println("GdbDsfInterface.getFormattedExpressionValue(): result = null");
      }
      else {
         System.err.println("GdbDsfInterface.getFormattedExpressionValue(): result = " + result + ",\n     class = " + result.getClass());
      }
      return result;
   }

   @Override
   public long evaluateExpression(IExpressions expressionService, final IFrameDMContext frame, String expression) {
      if (expressionService == null) {
//         System.err.println("evaluateExpression() expressionService = null");
         if ((fDsfSession == null)) {
            return 0;
         }
         DsfServicesTracker tracker = new DsfServicesTracker(Activator.getBundleContext(), fDsfSession.getId());
         expressionService = tracker.getService(IExpressions.class);
      }
      FormattedValueDMData formattedData = getFormattedValueDMData(expressionService, frame, expression);
      String value = formattedData.getFormattedValue();
//      System.err.println(String.format("evaluateExpression() \'%s\' => \'%s\'", expression, value));
      return Long.parseLong(value);
   }
   
   @Override
   public IFrameDMContext getExceptionStackFrameContext() {
      
      System.err.println(String.format("GdbDsfInterface.getExceptionStackFrameContext()"));
      if ((fDsfSession == null)) {
         System.err.println("GdbDsfInterface.getExceptionStackFrameContext() fDsfSession = null");
      }
      final DsfServicesTracker tracker = new DsfServicesTracker(Activator.getBundleContext(), fDsfSession.getId());
      final IExpressions expressionService = tracker.getService(IExpressions.class);
      if (expressionService == null) {
         System.err.println("getExceptionStackFrameContext() expressionService = null");
      }
      final IStack stackService = tracker.getService(IStack.class);
      if (stackService == null) {
         System.err.println("GdbDsfInterface.getExceptionStackFrameContext() stackService = null");
      }
//      System.err.println("GdbDsfInterface.getExceptionStackFrameContext() stackService = " + 
//            stackService + ", " + stackService.getClass());

      IFrameDMContext[] frames = getFrames(stackService);
      IFrameDMContext exceptionFrameContext = null;
      for(IFrameDMContext frame:frames) {
         IFrameDMData frameData = getFrameDMData(stackService, frame);
//         long pc = evaluateExpression(expressionService, frame, "$pc");
//         long lr = evaluateExpression(expressionService, frame, "$lr");
//         long fp = evaluateExpression(expressionService, frame, "$fp");
//         long sp = evaluateExpression(expressionService, frame, "$sp");
//         System.err.println(String.format("@=0x%08X, pc=0x%08X, lr=0x%08X, fp=0x%08X, sp=0x%08X", 
//               frameData.getAddress().getValue().longValue(), 
//               pc, lr, fp, sp
//               ));
         if ((frameData.getAddress().getValue().longValue()&0xFFFFFF00L) == 0xFFFFFF00L) {
            exceptionFrameContext = frame;
            break;
         }
      }
      return exceptionFrameContext;
   }


//   private IRegisterDMContext writeRegister(
//         final IRegisters registerService, 
//         final IRegisterDMContext registerDMContext, 
//         final String value,
//         final String format) {
//
//      DsfRunnable query = new DsfRunnable() {
//         @Override
//         public void run() {
//            registerService.writeRegister(registerDMContext, value, format, null);
//         }
//      };
//      ImmediateExecutor.getInstance().execute(query);
//
//      IRegisterDMContext result = null;
//      try {
//         result = query.get(memoryWaitTimeInMilliseconds, TimeUnit.MILLISECONDS);
//      }
//      catch (Exception e) {
//         e.printStackTrace();
//      }
//      if (result == null) {
//         System.err.println("GdbDsfInterface.findregister(): result = null");
//      }
//      else {
//         System.err.println("GdbDsfInterface.findregister(): result = " + result + "," + result.getClass());
//      }
//      return result;
//   }

   @SuppressWarnings("unused")
   private Object tryIt() {
      System.err.println(String.format("GdbDsfInterface.tryIt()"));
      if ((fDsfSession == null)) {
         return null;
      }
      final DsfServicesTracker tracker = new DsfServicesTracker(Activator.getBundleContext(), fDsfSession.getId());      
      final IExpressions expressionService = tracker.getService(IExpressions.class);
      if (expressionService == null) {
         System.err.println("GdbDsfInterface.tryIt() expressionService = null");
         return null;
      }
      final IRegisters registerService = tracker.getService(IRegisters.class);
      if (registerService == null) {
         System.err.println("GdbDsfInterface.tryIt() registerService = null");
         return null;
      }
      final IStack stackService = tracker.getService(IStack.class);
      if (stackService == null) {
      System.err.println("GdbDsfInterface.tryIt():1 stackService = null");
      return null;
      }
      IFrameDMContext topFrame = getTopFrame(stackService);

      IRegisterGroupDMContext[] registerGroups     = getRegisterGroups(topFrame, registerService);
      IRegisterDMContext[]      registers          = getRegisters(registerService, registerGroups[0]);
      IRegisterDMContext        registerDMContext  = registers[13];
      
      IRegisterDMData registerData = getRegisterData(registerService, registerDMContext);
      System.err.println(String.format("GdbDsfInterface.tryIt() registerData = (name,%s), (description,%s)", 
            registerData.getName(),
            registerData.getDescription()
            ));
      
      

      IFormattedDataDMContext fDMContext = DMContexts.getAncestorOfType(registerDMContext, IFormattedDataDMContext.class);
      if (fDMContext == null) {
         System.err.println("GdbDsfInterface.tryIt() fDMContext = null");
      }
      System.err.println("GdbDsfInterface.tryIt() fDMContext = " + fDMContext.toString());
      FormattedValueDMContext fvDMContext = registerService.getFormattedValueContext(fDMContext, IFormattedValues.NATURAL_FORMAT);
      if (fvDMContext == null) {
         System.err.println("GdbDsfInterface.tryIt() fvDMContext = null");
      }
      System.err.println("GdbDsfInterface.tryIt() fvDMContext = " + fvDMContext.toString());
      
      FormattedValueDMData res = getFormattedExpressionValue(registerService, fvDMContext);
      System.err.println("GdbDsfInterface.tryIt() res = " + res.toString());
      
      System.err.println(String.format("GdbDsfInterface.tryIt() (registerData.getName(),%s), (res.getFormattedValue(),%s) (res.getEditableValue(),%s)", 
            registerData.getName(),
            res.getEditableValue(),
            res.getFormattedValue()
            ));
      return null;
   }

}

