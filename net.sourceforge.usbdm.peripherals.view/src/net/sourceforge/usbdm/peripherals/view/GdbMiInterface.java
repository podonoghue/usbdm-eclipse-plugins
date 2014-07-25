package net.sourceforge.usbdm.peripherals.view;

import java.util.concurrent.TimeoutException;

import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIFormat;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.command.MIDataReadMemory;
import org.eclipse.cdt.debug.mi.core.command.MIDataWriteMemory;
import org.eclipse.cdt.debug.mi.core.output.MIMemory;
import org.eclipse.cdt.debug.mi.core.output.MIResultRecord;

public class GdbMiInterface implements GdbCommonInterface {

   private MISession miSession = null;

   GdbMiInterface(MISession miSession) {
      this.miSession = miSession;
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
    *  @return byte[size] containing the data read 
    */
   private byte[] readMemory(long address, int size) {
      System.err.println("GDBInterface.readMemory(MI)");
      if (miSession == null) {
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
    * Wrapper that handles DSF or MI memory reads
    * 
    * @param address       Address to read from
    * @param iByteCount    Number of bytes to read
    * @param accessWidth   Access size (1,2,4) to use
    * 
    * @return              Data read
    * 
    * @throws Exception 
    */
   @Override
   public byte[] readMemory(long address, int iByteCount, int accessWidth) throws Exception {
      
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
         return readMemory(address, iByteCount);
   }
   
   /**
    * @param address
    * @param data
    * 
    * @return Number of bytes written (-1 => error)
    */
   private int writeMemory(long address, byte[] data) {
      System.err.println("GDBInterface.writeMemory(MI)");

      if ((miSession == null)) {
         return -1;
      }
      StringBuffer buffer = new StringBuffer(10+(2*data.length));
      buffer.append("0x");

      for (int index = data.length - 1; index >= 0; index--) {
         buffer.append(String.format("%02X", data[index]));
      }

      String value = buffer.toString();
      
//      System.err.println(String.format("GDBInterface.writeMemory(MISession, 0x%08X %d %s)", address, data.length, value));
      
      int ret = -1;
      MIDataWriteMemory mem;
      mem = miSession.getCommandFactory().createMIDataWriteMemory(0L, Long.toString(address), MIFormat.HEXADECIMAL, data.length, value);
      try {
         miSession.postCommand(mem);
         if (mem.getMIOutput().getMIResultRecord().getResultClass().equals(MIResultRecord.ERROR)) {
            System.err.println(String.format("GDBInterface.writeMemory(MISession, 0x%08X %d %s) - failed", address, data.length, value));
            return -1;
         }
         ret = data.length;
      } catch (MIException localMIException) {
         System.err.println("GDBInterface.readMemory() - exception" + localMIException.getMessage());
      }
      return ret;
   }
       
   /**
    * Wrapper that handles DSF or MI memory writes
    * 
    * @param address    Address to write at
    * @param data       Data to write.  This must be 1, 2, 4 or 8 bytes due to limitations of underlying GDB command used
    * 
    * @throws TimeoutException
    */
   @Override
   public void writeMemory(long address, byte[] data, int accessWidth) throws TimeoutException {
      System.err.println(String.format("GDBInterface.writeMemory(0x%08X, %d)", address, data.length));
      writeMemory(address, data);
   }
}
