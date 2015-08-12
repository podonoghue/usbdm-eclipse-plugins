/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | Moved byte sex dependent code to gdbInterface                                     | V4.10.6.250
===============================================================================================================
*/
package net.sourceforge.usbdm.peripherals.view;

import java.util.concurrent.TimeoutException;

import org.eclipse.cdt.dsf.debug.service.IExpressions;
import org.eclipse.cdt.dsf.debug.service.IStack.IFrameDMContext;

public abstract class GdbCommonInterface {

   private boolean littleEndian;

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
   public abstract byte[] readMemory(long address, int iByteCount, int accessWidth) throws Exception;

   /**
    * Memory writes
    * 
    * @param address       Address to write at
    * @param data          Data to write.  This must be 1, 2, 4 or 8 bytes due to limitations of underlying GDB command used
    * @param accessWidth   Access size (8, 16, 32 bits) to use (Ignored)
    * 
    * @throws TimeoutException
    */
   public abstract void writeMemory(long address, byte[] data, int accessWidth) throws TimeoutException;

   /**
    * Sets current stack frame
    * 
    * @param frameNum
    * @return
    * @throws Exception
    */
   abstract long setFrame(int frameNum) throws Exception;

   /**
    * Get stack frame size
    * 
    * @return
    * @throws Exception
    */
   abstract IFrameDMContext getExceptionStackFrameContext() throws Exception;

   abstract long evaluateExpression(IExpressions expressionService, IFrameDMContext frame, String expression);

   /**
    * Indicates if the target is littleEndian
    * 
    * @return
    */
   public boolean isLittleEndian() {
      return littleEndian;
   }

   /**
    * Set the endianess of the target
    * 
    * @param littleEndian
    */
   public void setLittleEndian(boolean littleEndian) {
//      System.err.println("GdbCommonInterface.setLittleEndian(" + littleEndian + ")");
      this.littleEndian = littleEndian;
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
    * Calculates a 32-bit unsigned value from the 1st four element of a byte[]
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public long getValue32bit(byte[] bytes) {
      return getValue32bit(bytes, 0);
   }

   /**
    * Calculates a 16-bit unsigned value from the 1st two elements of a byte[]
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public long getValue16bit(byte[] bytes) {
      return getValue16bit(bytes, 0);
   }

   /**
    * Calculates a 32-bit unsigned value from the 1st element of a byte[]
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public long getValue8bit(byte[] bytes) {
      return getValue8bit(bytes, 0);
   }

   /**
    * Calculates a 32-bit unsigned value from the four elements of a byte[]
    * 
    * @param bytes   Bytes to process
    * @param offset  Offset of required bytes
    * 
    * @return converted value
    */
   public long getValue32bit(byte[] bytes, int offset) {
      //      System.err.println("BaseModel.getValue32bit("+bytes+")");
      long value = 0x0BADF00D;
      if (bytes != null) {
         if (isLittleEndian()) {
            value = unsignedShift(bytes[offset+0], 0)+unsignedShift(bytes[offset+1], 8)+unsignedShift(bytes[offset+2], 16)+unsignedShift(bytes[offset+3], 24);
            //            System.err.println(String.format("BaseModel.getValue32bit(), littleEndian => 0x%08X",value));
         }
         else {
            value = unsignedShift(bytes[offset+0], 24)+unsignedShift(bytes[offset+1], 16)+unsignedShift(bytes[offset+2], 8)+unsignedShift(bytes[offset+3], 0);
            //            System.err.println(String.format("BaseModel.getValue32bit(), bigEndian => 0x%08X",value));
         }
      }
      return value;
   }

   /**
    * Calculates a 16-bit unsigned value from the 1st two element of a byte[]
    * 
    * @param bytes   Bytes to process
    * @param offset  Offset of required bytes
    * 
    * @return converted value
    */
   public long getValue16bit(byte[] bytes, int offset) {
      long value = 0x0BADF00D;
      if (bytes != null) {
         if (isLittleEndian()) {
            value = unsignedShift(bytes[offset+0], 0)+unsignedShift(bytes[offset+1], 8);
         }
         else {
            value = unsignedShift(bytes[offset+0], 8)+unsignedShift(bytes[offset+1], 0);
         }
      }
      return value;
   }

   /**
    * Calculates a 32-bit unsigned value from the given element of a byte[]
    * 
    * @param bytes   Bytes to process
    * @param offset  Offset of required byte
    * 
    * @return converted value
    */
   public long getValue8bit(byte[] bytes, int offset) {
      long value = 0x0BADF00D;
      if (bytes != null) {
         value = unsignedShift(bytes[offset], 0);
      }
      return (value);
   }

   /**
    * Creates a byte[1] from 8-bit unsigned value
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public byte[] convertValue8bit(long value) {
      byte[] data = new byte[1];
      data[0] = (byte)value;
      return data;
   }

   /**
    * Creates a byte[2] from 16-bit unsigned value
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public byte[] convertValue16bit(long value) {
      byte[] data = new byte[2];
      if (isLittleEndian()) {
         data[0] = (byte)value;
         data[1] = (byte)(value>>8);
      }
      else {
         data[0] = (byte)(value>>8);
         data[1] = (byte)value;
      }
      return data;
   }

   /**
    * Creates a byte[4] from 32-bit unsigned value
    * 
    * @param bytes to process
    * 
    * @return converted value
    */
   public byte[] convertValue32bit(long value) {
      byte[] data = new byte[4];
      if (isLittleEndian()) {
         data[0] = (byte)value;
         data[1] = (byte)(value>>8);
         data[2] = (byte)(value>>16);
         data[3] = (byte)(value>>24);
      }
      else {
         data[0] = (byte)(value>>24);
         data[1] = (byte)(value>>16);
         data[2] = (byte)(value>>8);
         data[3] = (byte)value;
      }
      return data;
   }

}
