package net.sourceforge.usbdm.peripherals.view;

import java.util.concurrent.TimeoutException;

import org.eclipse.cdt.dsf.debug.service.IExpressions;
import org.eclipse.cdt.dsf.debug.service.IStack.IFrameDMContext;

public interface GdbCommonInterface {

   /**
    * Memory reads
    * 
    * @param address       Address to read from
    * @param iByteCount    Number of bytes to read
    * @param accessWidth   Access size (8, 16, 32 bits) to use
    * 
    * @return              Data read
    * 
    * @throws Exception 
    */
   public byte[] readMemory(long address, int iByteCount, int accessWidth) throws Exception;

   /**
    * Memory writes
    * 
    * @param address       Address to write at
    * @param data          Data to write.  This must be 1, 2, 4 or 8 bytes due to limitations of underlying GDB command used
    * @param accessWidth   Access size (8, 16, 32 bits) to use (Ignored)
    * 
    * @throws TimeoutException
    */
   public void writeMemory(long address, byte[] data, int accessWidth) throws TimeoutException;

   /**
    * Sets current stack frame
    * 
    * @param frameNum
    * @return
    * @throws Exception
    */
   long setFrame(int frameNum) throws Exception;

   /**
    * Get stack frame size
    * 
    * @return
    * @throws Exception
    */
   IFrameDMContext getExceptionStackFrameContext() throws Exception;

   long evaluateExpression(IExpressions expressionService, IFrameDMContext frame, String expression);
   
}
