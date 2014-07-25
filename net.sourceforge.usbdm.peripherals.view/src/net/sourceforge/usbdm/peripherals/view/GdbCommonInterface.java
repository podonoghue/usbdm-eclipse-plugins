package net.sourceforge.usbdm.peripherals.view;

import java.util.concurrent.TimeoutException;

public interface GdbCommonInterface {

   /**
    * Memory reads
    * 
    * @param address       Address to read from
    * @param iByteCount    Number of bytes to read
    * @param accessWidth   Access size (1,2,4) to use
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
    * @param accessWidth   Ignored - Should agree with data size
    * 
    * @throws TimeoutException
    */
   public void writeMemory(long address, byte[] data, int accessWidth) throws TimeoutException;
}
