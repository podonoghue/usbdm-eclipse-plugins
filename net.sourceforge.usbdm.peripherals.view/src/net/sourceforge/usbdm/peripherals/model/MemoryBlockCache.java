/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | Moved byte sex related code to gdbInterface                                       | V4.10.6.250
===============================================================================================================
*/
package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

import net.sourceforge.usbdm.peripheralDatabase.AddressBlock;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripherals.view.GdbCommonInterface;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

public class MemoryBlockCache {
      private final long fAddress;
      private final long fSizeInBytes;
      private final long fWidthInBits;
      private boolean fWriteable;
      private boolean fReadable;
      
      // Data caching the value from target
      private byte[]             fData;
      // Reference copy of data for change indication
      private byte[]             fLastData;
      // Indicates block is inaccessible (access error)
      private boolean            fInaccessible;
      // Indicates data is out of date
      private boolean            fNeedsUpdate;
      // Update from target has been scheduled
      private boolean            fUpdatePending;
      // Interface to target
      private GdbCommonInterface fGdbInterface = null;
      // Listeners on changes in the block data
      private ArrayList<MemoryBlockChangeListener> fChangeListeners = new ArrayList<MemoryBlockChangeListener>();
          
      public MemoryBlockCache(Peripheral peripheral, AddressBlock addressBlock, GdbCommonInterface gdbInterface) {
         fAddress         = peripheral.getBaseAddress()+addressBlock.getOffset();
         fSizeInBytes     = addressBlock.getSizeInBytes();
         fWidthInBits     = addressBlock.getWidthInBits();
         fNeedsUpdate     = true;
         fUpdatePending   = false;
         fGdbInterface    = gdbInterface; 
         fWriteable       = false;
      }
      
      /**
       * Adds the listener to the objects receiving change notification 
       * 
       * @param listener
       */
      public void addChangeListener(MemoryBlockChangeListener listener) {
         fChangeListeners.add(listener);
      }
      
      /**
       * Notifies all change listeners
       */
      private void notifyAllChangeListeners() {
         for (MemoryBlockChangeListener changeListener : fChangeListeners) {
//            System.err.println("MemoryBlockCache.notifyAllChangeListeners(), changeListeners = " + changeListener.toString());
            changeListener.notifyMemoryChanged(this);
         }
      }
      
      private long get8bitValue(long address) throws MemoryException {
         long offset = address-fAddress;
         if ((offset<0) || (offset>=fData.length)) {
            throw new MemoryException(String.format("Invalid address range [%d..%d], range=[0..%d]", offset, offset, fData.length-1));
         }
         return fGdbInterface.getValue8bit(fData, (int)offset);
      }
      
      private long get16bitValue(long address) throws MemoryException {
         long offset = address-fAddress;
         if ((offset<0) || ((offset+1)>=fData.length)) {
            throw new MemoryException(String.format("Invalid address range [%d..%d], range=[0..%d]", offset, offset+1, fData.length-1));
         }
         return fGdbInterface.getValue16bit(fData, (int)offset);
      }

      private long get32bitValue(long address) throws MemoryException {
         long offset = address-fAddress;
         if ((offset<0) || ((offset+3)>=fData.length)) {
            throw new MemoryException(String.format("Invalid address range [%d..%d], range=[0..%d]", offset, offset+3, fData.length-1));
         }
         return fGdbInterface.getValue32bit(fData, (int)offset);
      }

      /**
       * Gets an 8, 16 or 32-bit value<br>
       * Does not trigger change listeners
       * 
       * @param address
       * @param sizeInBytes size in bytes!
       * 
       * @return value
       * @throws MemoryException 
       */
      public long getValue(long address, int sizeInBytes) throws MemoryException {
         allocateDataIfNeeded();
         if (fGdbInterface == null) {
            return 0;
         }
         switch (sizeInBytes) {
         case 1:  return get8bitValue(address);
         case 2:  return get16bitValue(address);
         case 4:  return get32bitValue(address);
         }
         throw new MemoryException("memoryBlockCache invalid data size");
      }

      /**
       * Set 8-bit value in data<br>
       * Does not trigger change listeners
       * 
       * @param address Start address
       * @param value   Value to set
       * 
       * @throws Exception
       */
      private void set8bitValue(long address, long value) throws Exception {
         long offset = address-fAddress;
         if ((offset<0) || (offset>=fSizeInBytes)) {
            throw new Exception(String.format("set8bitValue() - Invalid address range [%d..%d] for this[0..%d]", offset, offset+fSizeInBytes-1, fSizeInBytes-1));
         }
         fData[(int)offset] = (byte)value;
      }

      /**
       * Set 16-bit value in data<br>
       * Does not trigger change listeners
       * 
       * @param address Start address
       * @param value   Value to set
       * 
       * @throws Exception
       */
      private void set16bitValue(long address, long value) throws Exception {
         long offset = address-fAddress;
         long size = 2;
         if ((offset<0) || ((offset+size)>fSizeInBytes)) {
            throw new Exception(String.format("set16bitValue() - Invalid address range [%d..%d] for this[0..%d]", offset, offset+size-1, fSizeInBytes-1));
         }
         fData[(int)(offset++)] = (byte)(value);
         fData[(int)(offset++)] = (byte)(value>>8);
      }

      /**
       * Set 32-bit value in data<br>
       * Does not trigger change listeners
       * 
       * @param address Start address
       * @param value   Value to set
       * 
       * @throws Exception
       */
      private void set32bitValue(long address, long value) throws Exception {
         long offset = address-fAddress;
         long size = 4;
         if ((offset<0) || ((offset+size)>fSizeInBytes)) {
            throw new Exception(String.format("set32bitValue() - Invalid address range [%d..%d] for this[0..%d]", offset, offset+size-1, fSizeInBytes-1));
         }
         fData[(int)(offset++)] = (byte)(value);
         fData[(int)(offset++)] = (byte)(value>>8);
         fData[(int)(offset++)] = (byte)(value>>16);
         fData[(int)(offset++)] = (byte)(value>>24);
      }

      /**
       * Sets an 8, 16 or 32-bit value in block<br>
       * Does not trigger change listeners
       * 
       * @param address       Start address
       * @param value         Value to set
       * @param sizeInBytes   Size of value in bytes (1, 2 or 4)
       * 
       * @throws Exception 
       */
      public void setValue(long address, int sizeInBytes, long value) throws Exception {
//         System.err.println(String.format("MemoryBlockCache.setValue() a=0x%X, v=%d, s=%d bytes", address, value, sizeInBytes));
         allocateDataIfNeeded();
         switch (sizeInBytes) {
         case 1:  set8bitValue(address, value);     break;
         case 2:  set16bitValue(address, value);    break;
         case 4:  set32bitValue(address, value);    break;
         }
      }
      
      /**
       * Allocate data block if not already done
       */
      private void allocateDataIfNeeded() {
         if (fData == null) {
            fData = new byte[(int) getSize()];
            Arrays.fill(fData, (byte)0xAA);
         }
      }

      /**
       * Get 8-bit value from last data value reference
       * 
       * @param address Memory address (expected to lie within block)
       *  
       * @return Value 
       * 
       * @throws MemoryException
       */
      private long get8bitLastValue(long address) throws MemoryException {
         long offset = address-fAddress;
         if (fLastData == null) {
            throw new MemoryException("memoryBlockCache.get8bitLastValue() no data read");
         }
         if ((offset<0) || (offset>=fLastData.length)) {
            return 0;
         }
         return fGdbInterface.getValue8bit(fLastData, (int)offset);
      }
      
      /**
       * Get 16-bit value from last data value reference
       * 
       * @param address Memory address (expected to lie within block)
       *  
       * @return Value 
       * 
       * @throws MemoryException
       */
      private long get16bitLastValue(long address) throws MemoryException {
         long offset = address-fAddress;
         if (fLastData == null) {
            throw new MemoryException("memoryBlockCache.get16bitLastValue() no data read");
         }
         if ((offset<0) || (offset>=fLastData.length)) {
            return 0;
         }
         return fGdbInterface.getValue16bit(fLastData, (int)offset);
      }

      /**
       * Get 32-bit value from last data value reference
       * 
       * @param address Memory address (expected to lie within block)
       *  
       * @return Value 
       * 
       * @throws MemoryException
       */
      private long get32bitLastValue(long address) throws MemoryException {
         long offset = address-fAddress;
         if (fLastData == null) {
            throw new MemoryException("memoryBlockCache.get32bitLastValue() no data read");
         }
         if ((offset<0) || (offset>=fLastData.length)) {
            return 0;
         }
         return fGdbInterface.getValue32bit(fLastData, (int)offset);
      }

      /**
       * Get last value i.e. value recorded when setChangeReference() was last called
       * 
       * @param address       Memory address (expected to lie within block)
       * @param sizeInBytes   Size of data (1, 2 or 4 bytes)
       * 
       * @return Value
       * @throws MemoryException 
       */
      public long getLastValue(long address, int sizeInBytes) throws MemoryException {
         switch (sizeInBytes) {
         case 1:  return get8bitLastValue(address);
         case 2:  return get16bitLastValue(address);
         case 4:  return get32bitLastValue(address);
         }
         throw new MemoryException("memoryBlockCache.getLastValue() invalid size");
      }

      /**
       * @return the needsUpdate flag
       */
      public boolean isNeedsUpdate() {
         return fNeedsUpdate;
      }
      
      /**
       * Sets whether the data needs update from target<br>
       * Does not initiate update
       *  
       * @param needsUpdate 
       */
      public void setNeedsUpdate(boolean needsUpdate) {
         fNeedsUpdate = needsUpdate;
      }
      
      /**
       * Get start address
       * 
       * @return The start address of this block
       */
      public long getAddress() {
         return fAddress;
      }
      
      /**
       * Get size
       * 
       * @return The size (in bytes) of this block
       */
      public long getSize() {
         return fSizeInBytes;
      }

      /**
       * @return the writeable
       */
      public boolean isWriteable() {
         return fWriteable;
      }

      /**
       * @param writeable the writeable to set
       */
      public void setWriteable(boolean writeable) {
         fWriteable = writeable;
      }

      /**
       * @return the readable
       */
      public boolean isReadable() {
         return fReadable;
      }

      /**
       * @param readable the readable to set
       */
      public void setReadable(boolean readable) {
         fReadable = readable;
      }

      /**
       * Get width
       * 
       * @return The width (in bits) of this block's elements (memory access size)
       */
      public long getWidthInBits() {
         return fWidthInBits;
      }
      
      /**
       * Get data
       * 
       * @return The current data associated with this block
       */
      public byte[] getData() {
         return fData;
      }
      
      /**
       * Sets the data associated with this block
       * 
       * @param data The data to set
       */
      public void setData(byte[] data) {
         fData = data;
//         notifyAllChangeListeners();
      }
      
      /**
       * Check if an address range lies (entirely) within this address block
       * 
       * @param address Start of range
       * @param size    Size in bytes
       * @return
       */
      public boolean containsAddress(long address, long size) {
         return (address>=fAddress) && ((address+size) <= (fAddress+fSizeInBytes));
      }
      
      /**
       *  Updates the block contents from target if needed (according to needsUpdate()).
       */
      void update(BaseModel model) {
//         System.err.println(String.format("RegisterModel.update(%s)", getName()));
         if (isNeedsUpdate()) {
            retrieveValue(model);
         }
      }

      /**
       * Retrieves block contents from target (if update not already pending).<br>
       * Change listeners will be notified when complete
       * 
       * @note Done on worker thread
       */
      public synchronized void retrieveValue() {
         retrieveValue(null);
      }

      /**
       * Retrieves block contents from target (if update not already pending).<br>
       * Change listeners will be notified when complete
       * 
       * @param model
       * @note Done on worker thread
       */
      public synchronized void retrieveValue(final ObservableModel model) {
         if (!isReadable()) {
            // Ignore
            return;
         }
         fNeedsUpdate = true;

//         System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].retrieveValue", getAddress(), getAddress()+getSize()-1));
         if (fUpdatePending) {
            // Update already initiated
//            System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].retrieveValue - no action, update pending", getAddress(), getAddress()+getSize()-1));
            return;
         }
         allocateDataIfNeeded();
         fUpdatePending = true;

         Job job = new Job("Updating peripheral display") {
            protected synchronized IStatus run(IProgressMonitor monitor) {
               monitor.beginTask("Updating peripheral display...", 10);
               try {
                  fUpdatePending = false;
                  if (!fNeedsUpdate) {
                     // May have been updated since original request
//                     System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].retrieveValue.run() - no action", getAddress(), getAddress()+getSize()-1));
                     return Status.OK_STATUS;
                  }
//                  System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].retrieveValue.run() - reading from target", getAddress(), getAddress()+getSize()-1));
                  byte[] value = null;
                  try {
                     value = fGdbInterface.readMemory(getAddress(), (int)getSize(), (int)getWidthInBits());
                  } catch (Exception e) {
                     fInaccessible = true;
                     fNeedsUpdate  = false;
                     System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].retrieveValue.run() - Unable to access target", getAddress(), getAddress()+getSize()-1));
                     return Status.OK_STATUS;
                  }
//                  System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].retrieveValue.run() - read from target", getAddress(), getAddress()+getSize()-1));
                  setData(value);
                  fInaccessible = false;
                  fNeedsUpdate  = false;
//                  System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].retrieveValue.run() model = %s", getAddress(), getAddress()+getSize()-1,  model));
//                  if ((model == null) || !model.isRefreshPending()) {
//                     // No model or model needs refreshing
//                     if (model != null) {
//                        model.setRefreshPending(true);
//                     }
                     Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
//                           if (model != null) {
//                              model.setRefreshPending(false);
//                           }
                           notifyAllChangeListeners();
                        }
                     });
//                  }
               } finally {
                  monitor.done();
               }
               return Status.OK_STATUS;
            }};
//            job.setPriority(Job.SHORT);
            job.setUser(false);
            job.schedule();
         }

         /**
          * Checks if a memory range has changed since changes last reset by setChangeReference()
          * 
          * @param address       Start address of range to check
          * @param sizeInBytes   Size in bytes of range
          * @return
          */
         public boolean isChanged(long address, long sizeInBytes) {
            long offset = address-fAddress;
            if ((offset<0) || ((offset+sizeInBytes)>fSizeInBytes)) { 
               System.err.println(String.format("MemoryBlockCache.isChanged() - Invalid address range [0x%X..0x%x]", offset, offset+sizeInBytes-1));
               System.err.println(String.format("MemoryBlockCache.isChanged() - Should be within      [0x%X..0x%x]", 0,      fSizeInBytes-1));
               return false;
            }
            if (fLastData == null) {
               return (fData == null)?false:true;
            }
            if (fData == null) {
               return false;
            }
            for (int index=(int)offset; index<(offset+sizeInBytes); index++) {
               if (fData[index] != fLastData[index]) {
                  return true;
               }
            }
            return false;
         }

         /**
          * Indicates block is invalid due to access error
          * 
          * @return
          */
         public boolean isInaccessible() {
            return fInaccessible;
         }

         /**
          *  Set range to reset value - not yet implemented
          *  
          * @param address      Address of start of value
          * @param resetValue   Reset value
          * @param sizeInBytes  Size of value (in bytes)
          */
         public void loadResetValue(long address, long resetValue, int sizeInBytes) {
//          setValue(address, resetValue, size);
            setNeedsUpdate(false);
         }

         /**
          * Sets the current data value as the reference for determining changed values. 
          */
         public void setChangeReference() {
            if (fData != null) {
               fLastData = new byte[fData.length];
               System.arraycopy(fData, 0, fLastData, 0, fData.length);
            }
         }

         /**
          * Sets the current data value as the reference for determining changed values. 
          *  
          * @param address Start address of range
          * @param size    Size of range in bytes
          */
         public void setChangeReference(long address, int size) {
            if (fData == null) {
               System.err.println("MemoryBlockCache.setChangeReference() - Attempt to change reference before initial read");
               return;
            }
            if (fLastData == null) {
               // Update all
               setChangeReference();
               return;
            }
            long offset = address-fAddress;
            if ((offset<0)|| ((offset+size)>fSizeInBytes)) {
               System.err.println("MemoryBlockCache.setChangeReference() - Invalid address range");
               return;
            }
            for (int index = (int)offset; index<(offset+size); index++) {
               fLastData[index] = fData[index];
            }
         }

         /**
          * Synchronize cached value and target<br>
          * This consist of:<br>
          *  - Writing the range of data values to target<br>
          *  - Reading the entire block back from target (if readable)<br>
          * Change listeners will be notified when complete
          * 
          * @param address       Start address of data range
          * @param sizeInBytes   Size in bytes of data range
          */
         public void synchronizeValue(final long address, final int sizeInBytes) {
//            System.err.println(String.format("MemoryBlockCache.synchronizeValue([0x%X..0x%X])", address, address+sizeInBytes-1));
            if (!isWriteable()) {
//               System.err.println(String.format("MemoryBlockCache.synchronizeValue([0x%X..0x%X]) - not writeable", address, address+sizeInBytes-1));
               return;
            }
            long offset = address-fAddress;
            assert (offset>=0) : "Invalid address range";
            assert ((offset+sizeInBytes)<=fSizeInBytes) : "Invalid address";
            if (fData == null) {
               System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].synchronizeValue() - data null", address, address+sizeInBytes-1));
               return;
            }
            final byte[] writeData = new byte[sizeInBytes];
            System.arraycopy(fData, (int) offset, writeData, 0, sizeInBytes);
            
            Job job = new Job("Updating peripheral display") {
               protected IStatus run(IProgressMonitor monitor) {
                  monitor.beginTask("Updating peripheral display...", 10);
                  IStatus rv = Status.OK_STATUS;
                  try {
                     try {
//                        System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].synchronizeValue.run() - writing to target", address, address+sizeInBytes-1));
                        fGdbInterface.writeMemory(address, writeData, (int)getWidthInBits());
                     } catch (TimeoutException e1) {
//                        System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].synchronizeValue.run() - Unable to access target", address, address+sizeInBytes-1));
                     }
//                     System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].synchronizeValue.run() - request update", address, address+sizeInBytes-1));
                     retrieveValue();
//                     System.err.println(String.format("MemoryBlockCache[0x%X..0x%X].synchronizeValue.run() - complete", address, address+sizeInBytes-1));               
                  } finally {
                     monitor.done();
                  }
                  return rv;
               }};
               job.setPriority(Job.SHORT);
               job.setUser(false);
               job.schedule();
         }

      }