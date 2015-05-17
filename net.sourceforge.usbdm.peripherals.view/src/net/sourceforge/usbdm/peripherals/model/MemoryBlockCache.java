/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | Moved byte sex related code to gdbInterface                                       | V4.10.6.250
===============================================================================================================
*/
package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import net.sourceforge.usbdm.peripheralDatabase.AddressBlock;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripherals.view.GdbCommonInterface;

import org.eclipse.swt.widgets.Display;

public class MemoryBlockCache {
      private final long address;
      private final long sizeInBytes;
      private final long widthInBits;
      private boolean writeable;
      private boolean readable;
      
      // Data caching the value from target
      private byte[]             data;
      // Reference copy of data for change indication
      private byte[]             lastData;
      // Indicates data is invalid (out of date)
      private boolean            needsUpdate;
      // Update from target has been scheduled
      private boolean            updatePending;
      // Interface to target
      private GdbCommonInterface gdbInterface = null;
      // Listeners on changes in the block data
      private ArrayList<MemoryBlockChangeListener> changeListeners = new ArrayList<MemoryBlockChangeListener>();
          
      public MemoryBlockCache(Peripheral peripheral, AddressBlock addressBlock, GdbCommonInterface gdbInterface) {
         this.address         = peripheral.getBaseAddress()+addressBlock.getOffset();
         this.sizeInBytes     = addressBlock.getSizeInBytes();
         this.widthInBits     = addressBlock.getWidthInBits();
         this.needsUpdate     = true;
         this.updatePending   = false;
         this.gdbInterface    = gdbInterface; 
         this.writeable       = false;
      }
      
      /**
       * Adds the listener to the objects receiving change notification 
       * 
       * @param listener
       */
      public void addChangeListener(MemoryBlockChangeListener listener) {
         changeListeners.add(listener);
      }
      
      /**
       * Notifies all change listeners
       */
      private void notifyAllChangeListeners() {
         for (MemoryBlockChangeListener changeListener : changeListeners) {
//            System.err.println("MemoryBlockCache.notifyAllChangeListeners(), changeListeners = " + changeListener.toString());
            changeListener.notifyMemoryChanged(this);
         }
      }
      
      private long get8bitValue(long address) throws MemoryException {
         long offset = address-this.address;
         if ((offset<0) || (offset>=data.length)) {
            throw new MemoryException(String.format("Invalid address range [%d..%d], range=[0..%d]", offset, offset, data.length-1));
         }
         return gdbInterface.getValue8bit(data, (int)offset);
      }
      
      private long get16bitValue(long address) throws MemoryException {
         long offset = address-this.address;
         if ((offset<0) || ((offset+1)>=data.length)) {
            throw new MemoryException(String.format("Invalid address range [%d..%d], range=[0..%d]", offset, offset+1, data.length-1));
         }
         return gdbInterface.getValue16bit(data, (int)offset);
      }

      private long get32bitValue(long address) throws MemoryException {
         long offset = address-this.address;
         if ((offset<0) || ((offset+3)>=data.length)) {
            throw new MemoryException(String.format("Invalid address range [%d..%d], range=[0..%d]", offset, offset+3, data.length-1));
         }
         return gdbInterface.getValue32bit(data, (int)offset);
      }

      /**
       * Gets an 8, 16 or 32-bit value - does not trigger an update
       * 
       * @param address
       * @param sizeInBytes size in bytes!
       * 
       * @return value
       * @throws MemoryException 
       */
      public long getValue(long address, int sizeInBytes) throws MemoryException {
         allocateDataIfNeeded();
         switch (sizeInBytes) {
         case 1:  return get8bitValue(address);
         case 2:  return get16bitValue(address);
         case 4:  return get32bitValue(address);
         }
         throw new MemoryException("memoryBlockCache invalid data size");
      }

      private void set8bitValue(long address, long value) throws Exception {
         long offset = address-this.address;
         if ((offset<0) || (offset>=this.sizeInBytes)) {
            throw new Exception(String.format("set8bitValue() - Invalid address range [%d..%d] for this[0..%d]", offset, offset+sizeInBytes-1, this.sizeInBytes-1));
         }
         data[(int)offset] = (byte)value;
      }

      private void set16bitValue(long address, long value) throws Exception {
         long offset = address-this.address;
         long size = 2;
         if ((offset<0) || ((offset+size)>this.sizeInBytes)) {
            throw new Exception(String.format("set16bitValue() - Invalid address range [%d..%d] for this[0..%d]", offset, offset+size-1, this.sizeInBytes-1));
         }
         data[(int)(offset++)] = (byte)(value);
         data[(int)(offset++)] = (byte)(value>>8);
      }

      private void set32bitValue(long address, long value) throws Exception {
         long offset = address-this.address;
         long size = 4;
         if ((offset<0) || ((offset+size)>this.sizeInBytes)) {
            throw new Exception(String.format("set32bitValue() - Invalid address range [%d..%d] for this[0..%d]", offset, offset+size-1, this.sizeInBytes-1));
         }
         data[(int)(offset++)] = (byte)(value);
         data[(int)(offset++)] = (byte)(value>>8);
         data[(int)(offset++)] = (byte)(value>>16);
         data[(int)(offset++)] = (byte)(value>>24);
      }

      /**
       * Sets an 8, 16 or 32-bit value in block - does not trigger updates
       * 
       * @param address
       * @param value
       * @param sizeInBytes
       * @throws Exception 
       */
      public void setValue(long address, long value, int sizeInBytes) throws Exception {
//         System.err.println(String.format("MemoryBlockCache.setValue() a=0x%X, v=%d, s=%d bytes", address, value, sizeInBytes));
         allocateDataIfNeeded();
         switch (sizeInBytes) {
         case 1:  set8bitValue(address, value);     break;
         case 2:  set16bitValue(address, value);    break;
         case 4:  set32bitValue(address, value);    break;
         }
      }
      
      private void allocateDataIfNeeded() {
         if (data == null) {
            data = new byte[(int) getSize()];
         }
      }

      private long get8bitLastValue(long address) throws MemoryException {
         long offset = address-this.address;
         if (lastData == null) {
            throw new MemoryException("memoryBlockCache.get8bitLastValue() no data read");
         }
         if ((offset<0) || (offset>=lastData.length)) {
            return 0;
         }
         return gdbInterface.getValue8bit(lastData, (int)offset);
      }
      
      private long get16bitLastValue(long address) throws MemoryException {
         long offset = address-this.address;
         if (lastData == null) {
            throw new MemoryException("memoryBlockCache.get16bitLastValue() no data read");
         }
         if ((offset<0) || (offset>=lastData.length)) {
            return 0;
         }
         return gdbInterface.getValue16bit(lastData, (int)offset);
      }

      private long get32bitLastValue(long address) throws MemoryException {
         long offset = address-this.address;
         if (lastData == null) {
            throw new MemoryException("memoryBlockCache.get32bitLastValue() no data read");
         }
         if ((offset<0) || (offset>=lastData.length)) {
            return 0;
         }
         return gdbInterface.getValue32bit(lastData, (int)offset);
      }

      /**
       * Get last value i.e. value recorded when setChangeReference() was last called
       * 
       * @param address
       * @param sizeInBytes
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
         return needsUpdate;
      }
      
      /**
       * Sets whether the data needs update from target
       *  
       * @param needsUpdate 
       */
      public void setNeedsUpdate(boolean needsUpdate) {
         this.needsUpdate = needsUpdate;
      }
      
      /**
       * Get start address
       * 
       * @return The start address of this block
       */
      public long getAddress() {
         return address;
      }
      
      /**
       * Get size
       * 
       * @return The size (in bytes) of this block
       */
      public long getSize() {
         return sizeInBytes;
      }

      /**
       * @return the writeable
       */
      public boolean isWriteable() {
         return writeable;
      }

      /**
       * @param writeable the writeable to set
       */
      public void setWriteable(boolean writeable) {
         this.writeable = writeable;
      }

      /**
       * @return the readable
       */
      public boolean isReadable() {
         return readable;
      }

      /**
       * @param readable the readable to set
       */
      public void setReadable(boolean readable) {
         this.readable = readable;
      }

      /**
       * Get width
       * 
       * @return The width (in bits) of this block's elements (memory access size)
       */
      public long getWidthInBits() {
         return widthInBits;
      }
      
      /**
       * Get data
       * 
       * @return The current data associated with this block
       */
      public byte[] getData() {
         return data;
      }
      
      /**
       * Sets the data associated with this block
       * 
       * @param data the data to set
       */
      public void setData(byte[] data) {
         this.data = data;
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
         return (address>=this.address) && ((address+size) <= (this.address+this.sizeInBytes));
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
       * Retrieves block contents from target (if update not already pending).
       * This may trigger a view update.
       */
      public void retrieveValue(final ObservableModel model) {
         if (updatePending) {
            return;
         }
         allocateDataIfNeeded();
         updatePending = true;
//         System.err.println(String.format("MemoryBlockCache([0x%X..0x%X]).retrieveValue schedule action", getAddress(), getAddress()+getSize()-1));
         Runnable r = new Runnable() {
            public void run() {
               if (!isNeedsUpdate()) {
                  // May have been updated since original request
//                  System.err.println(String.format("MemoryBlockCache([0x%X..0x%X]).retrieveValue.run() - no action", getAddress(), getAddress()+getSize()-1));
                  updatePending = false;
                  return;
               }
//               System.err.println(String.format("MemoryBlockCache([0x%X..0x%X]).retrieveValue.run() - reading from target", getAddress(), getAddress()+getSize()-1));
               byte[] value = null;
               try {
                  value = gdbInterface.readMemory(getAddress(), (int)getSize(), (int)getWidthInBits());
               } catch (Exception e) {
                  System.err.println("Unable to access target");
                  value = null;
               }
//               System.err.println(String.format("MemoryBlockCache([0x%X..0x%X]).retrieveValue.run() - read from target", getAddress(), getAddress()+getSize()-1));
               setData(value);
               needsUpdate   = false;
               updatePending = false;
//               System.err.println("MemoryBlockCache([0x%X..0x%X]).retrieveValue.timerExec() model = "+ model);
               if (model != null) {
                  if (model.isRefreshPending()) {
//                     System.err.println(String.format("MemoryBlockCache([0x%X..0x%X]).retrieveValue.timerExec() refreshPending", getAddress(), getAddress()+getSize()-1));
                     return;
                  }
                  model.setRefreshPending(true);
               }
               Display.getDefault().asyncExec(new Runnable () {
                  @Override
                  public void run () {
                     if (model != null) {
                        model.setRefreshPending(false);
                     }
                     notifyAllChangeListeners();
                  }
               });
            }
         };
         Display.getDefault().asyncExec(r);
      }
      
      /**
       * Indicates that the current value of the data is to be used as the reference
       * for determining changed values. 
       */
      public void setChangeReference() {
         if (data != null) {
            lastData = new byte[data.length];
            System.arraycopy(data, 0, lastData, 0, data.length);
         }
      }
      
      /**
       * Checks if a memory range has changed since changes last reset by setChangeReference()
       * 
       * @param address       Start address of range to check
       * @param sizeInBytes   Size in bytes of range
       * @return
       */
      public boolean isChanged(long address, long sizeInBytes) {
         long offset = address-this.address;
         if ((offset<0) || ((offset+sizeInBytes)>this.sizeInBytes)) { 
            System.err.println(String.format("MemoryBlockCache.isChanged() - Invalid address range [0x%X..0x%x]", offset, offset+sizeInBytes-1));
            System.err.println(String.format("MemoryBlockCache.isChanged() - Should be within      [0x%X..0x%x]", 0,      this.sizeInBytes-1));
            return false;
         }
         if (lastData == null) {
            return (data == null)?false:true;
         }
         if (data == null) {
            return false;
         }
         for (int index=(int)offset; index<(offset+sizeInBytes); index++) {
            if (data[index] != lastData[index]) {
               return true;
            }
         }
         return false;
      }

      /**
       *  Set range to reset value - not yet implemented
       *  
       * @param address      Address of start of value
       * @param resetValue   Reset value
       * @param sizeInBytes  Size of value (in bytes)
       */
      public void loadResetValue(long address, long resetValue, int sizeInBytes) {
//         setValue(address, resetValue, size);
         setNeedsUpdate(false);
      }

      /**
       *  Sets a range of addresses as not needing update
       *  
       * @param address Start address of range
       * @param size    Size of range in bytes
       */
      public void setChangeReference(long address, int size) {
         if (data == null) {
            System.err.println("MemoryBlockCache.setChangeReference() - Attempt to change reference before initial read");
            return;
         }
         if (lastData == null) {
            // Update all
            setChangeReference();
            return;
         }
         long offset = address-this.address;
         if ((offset<0)|| ((offset+size)>this.sizeInBytes)) {
            System.err.println("MemoryBlockCache.setChangeReference() - Invalid address range");
            return;
         }
         for (int index = (int)offset; index<(offset+size); index++) {
            lastData[index] = data[index];
         }
      }

      /**
       * Synchronize a value between cached value and target
       * This consist of:<br>
       *  - Writing the range of data values to target<br>
       *  - Reading the entire peripheral state back from target (if readable)<br>
       * 
       * @param address       Start address of data range
       * @param sizeInBytes   Size in bytes of data range
       */
      public void synchronizeValue(final long address, final int sizeInBytes) {
//         System.err.println(String.format("MemoryBlockCache.synchronizeValue([0x%X..0x%X])", address, address+sizeInBytes-1));
         if (!isWriteable()) {
            System.err.println(String.format("MemoryBlockCache.synchronizeValue([0x%X..0x%X]) - not writeable", address, address+sizeInBytes-1));
            return;
         }
         long offset = address-this.address;
         assert (offset>=0)                : "Invalid address range";
         assert ((offset+sizeInBytes)<=this.sizeInBytes) : "Invalid address";
         if (data == null) {
            System.err.println(String.format("MemoryBlockCache.synchronizeValue([0x%X..0x%X]) - data null", address, address+sizeInBytes-1));
            return;
         }
         final byte[] writeData = new byte[sizeInBytes];
         System.arraycopy(data, (int) offset, writeData, 0, sizeInBytes);
         Runnable r = new Runnable() {
            public void run() {
//               System.err.println(String.format("MemoryBlockCache.synchronizeValue([0x%X..0x%X]).run() - writing to target", address, address+sizeInBytes-1));
               try {
                  gdbInterface.writeMemory(address, writeData, (int)getWidthInBits());
               } catch (TimeoutException e1) {
                  System.err.println(String.format("MemoryBlockCache.synchronizeValue([0x%X..0x%X]).run() - Unable to access target", address, address+sizeInBytes-1));
               }
//               System.err.println(String.format("MemoryBlockCache([0x%X..0x%X]).synchronizeValue.run() - reading from target", address, address+size-1));
               setNeedsUpdate(true);
               retrieveValue(null);
//               System.err.println("MemoryBlockCache([0x%X..0x%X]).synchronizeValue.run() - complete");               
            }
         };
         Display.getDefault().asyncExec(r);
      }

   }