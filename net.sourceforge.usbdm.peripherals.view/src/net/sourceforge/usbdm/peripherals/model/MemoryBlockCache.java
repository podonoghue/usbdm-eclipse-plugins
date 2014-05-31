package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import net.sourceforge.usbdm.peripheralDatabase.AddressBlock;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripherals.view.GdbCommonInterface;

import org.eclipse.swt.widgets.Display;

public class MemoryBlockCache {
      public final long address;
      public final long size;
      public final long width;
      
      private byte[]             data;
      private byte[]             lastData;
      private boolean            needsUpdate;
      private boolean            updatePending;
      private GdbCommonInterface gdbInterface = null;
      
      private ArrayList<MemoryBlockChangeListener> changeListeners = new ArrayList<MemoryBlockChangeListener>();
          
      public MemoryBlockCache(Peripheral peripheral, AddressBlock memoryBlockCache, GdbCommonInterface gdbInterface) {
         this.address       = peripheral.getBaseAddress()+memoryBlockCache.getOffset();
         this.size          = memoryBlockCache.getSize();
         this.width         = memoryBlockCache.getWidth();
         this.needsUpdate   = true;
         this.updatePending = false;
         this.gdbInterface  = gdbInterface; 
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
      
      private long get8bitValue(long address) {
         long offset = address-this.address;
         if ((data == null) || (offset<0) || (offset>=data.length)) {
//            System.err.println("MemoryBlockCache.get8bitValue() - Data null or invalid address range");
            return 0xAAL;
         }
         return BaseModel.getValue8bit(data, (int)offset);
      }
      
      private long get16bitValue(long address) {
         long offset = address-this.address;
         if ((data == null) || (offset<0) || ((offset+1)>=data.length)) {
//            System.err.println("MemoryBlockCache.get16bitValue() - Data null or invalid address range");
            return 0xAAAAL;
         }
         return BaseModel.getValue16bit(data, (int)offset);
      }

      private long get32bitValue(long address) {
         long offset = address-this.address;
         if ((data == null) || (offset<0) || ((offset+3)>=data.length)) {
//            System.err.println("MemoryBlockCache.get32bitValue() - Data null or invalid address range");
            return 0xCCCCCCC1L;
         }
         return BaseModel.getValue32bit(data, (int)offset);
      }

      /**
       * Gets an 8, 16 or 32-bit value - does not trigger an update
       * 
       * @param address
       * @param value
       * @param size  size in bytes!
       * 
       * @return value
       */
      public long getValue(long address, int size) {
         switch ((size+7)/8) {
         case 1:  return get8bitValue(address);
         case 2:  return get16bitValue(address);
         case 4:  return get32bitValue(address);
         }
         return 0xCCCCCCC2L;
      }

      private void set8bitValue(long address, long value) {
         if (data == null) {
            System.err.println("MemoryBlockCache.set8bits() - Attempt to modify memory before initial read");
            return;
         }
         long offset = address-this.address;
         if ((offset<0) || (offset>=this.size)) {
            System.err.println("set8bitValue() - Invalid address range");
            return;
         }
         data[(int)offset] = (byte)value;
      }

      private void set16bitValue(long address, long value) {
         if (data == null) {
            System.err.println("MemoryBlockCache.set16bits() - Attempt to modify memory before initial read");
            return;
         }
         long offset = address-this.address;
         long size = 2;
         if ((offset<0) || ((offset+size)>this.size)) {
            System.err.println("set16bitValue() - Invalid address range");
            return;
         }
         data[(int)(offset++)] = (byte)(value);
         data[(int)(offset++)] = (byte)(value>>8);
      }

      private void set32bitValue(long address, long value) {
         if (data == null) {
            System.err.println("MemoryBlockCache.set32bits() - Attempt to modify memory before initial read");
            return;
         }
         long offset = address-this.address;
         long size = 4;
         if ((offset<0) || ((offset+size)>this.size)) {
            System.err.println("set32bitValue() - Invalid address range");
            return;
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
       * @param size  size in bytes!
       */
      public void setValue(long address, long value, int size) {
         switch (size) {
         case 1:  set8bitValue(address, value);
         case 2:  set16bitValue(address, value);
         case 4:  set32bitValue(address, value);
         }
      }
      
      private long get8bitLastValue(long address) {
         long offset = address-this.address;
         if ((lastData == null) || (offset<0) || (offset>=lastData.length)) {
//            System.err.println("MemoryBlockCache.get8bitLastValue() - lastData null or invalid address range");
//            System.err.println(String.format("MemoryBlockCache.get8bitLastValue() - lastData=%s, offset=0x%x, lastData.length=0x%X",
//                  (lastData==null)?"null":"non-null", offset, this.size));
            return 0xAAL;
         }
         return BaseModel.getValue8bit(lastData, (int)offset);
      }
      
      private long get16bitLastValue(long address) {
         long offset = address-this.address;
         if ((lastData == null) || (offset<0) || ((offset+1)>=lastData.length)) {
//            System.err.println("MemoryBlockCache.get16bitLastValue() - lastData null or invalid address range");
            return 0xAAAAL;
         }
         return BaseModel.getValue16bit(lastData, (int)offset);
      }

      private long get32bitLastValue(long address) {
         long offset = address-this.address;
         if ((lastData == null) || ((offset<0) || (offset+3)>=lastData.length)) {
//            System.err.println("MemoryBlockCache.get32bitLastValue() - lastData null or invalid address range");
            return 0xCCCCCCC4L;
         }
         return BaseModel.getValue32bit(lastData, (int)offset);
      }

      /**
       * Get last value i.e. value recorded when setChangeReference() was last called
       * 
       * @param address
       * @param value
       * @param size    Size in bytes!
       * 
       * @return Value
       */
      public long getLastValue(long address, int size) {
         switch ((size+7)/8) {
         case 1:  return get8bitLastValue(address);
         case 2:  return get16bitLastValue(address);
         case 4:  return get32bitLastValue(address);
         }
         return 0xCCCCCCC3L;
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
         return size;
      }

      /**
       * Get width
       * 
       * @return The width (in bits) of this block's elements (memory access size)
       */
      public long getWidth() {
         return width;
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
         return (address>=this.address) && ((address+size) <= (this.address+this.size));
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
         updatePending = true;
         System.err.println(String.format("MemoryBlockCache([0x%X..0x%X]).retrieveValue schedule action", getAddress(), getAddress()+getSize()-1));
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
                  value = gdbInterface.readMemory(getAddress(), (int)getSize(), (int)getWidth());
               } catch (Exception e) {
                  System.err.println("Unable to access target");
                  value = new byte[(int) getSize()];
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
       * @param address Start address of range to check
       * @param size    Size in bytes of range
       * @return
       */
      public boolean isChanged(long address, long size) {
         long offset = address-this.address;
         if ((offset<0) || ((offset+size)>this.size)) { 
            System.err.println(String.format("MemoryBlockCache.isChanged() - Invalid address range [0x%X..0x%x]", offset, offset+size-1));
            System.err.println(String.format("MemoryBlockCache.isChanged() - Should be within      [0x%X..0x%x]", 0,      this.size-1));
            return false;
         }
         if (lastData == null) {
            return (data == null)?false:true;
         }
         if (data == null) {
            return false;
         }
         for (int index=(int)offset; index<(offset+size); index++) {
            if (data[index] != lastData[index]) {
               return true;
            }
         }
         return false;
      }

      /**
       *  Set range to reset value - not yet implemented
       *  
       * @param address    Address of start of value
       * @param resetValue Reset value
       * @param size       Size of value (in bytes)
       */
      public void loadResetValue(long address, long resetValue, int size) {
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
         if ((offset<0)|| ((offset+size)>this.size)) {
            System.err.println("MemoryBlockCache.setChangeReference() - Invalid address range");
            return;
         }
         for (int index = (int)offset; index<(offset+size); index++) {
            lastData[index] = data[index];
         }
      }

      /**
       * Synchronize a value between cached value and target
       * This consist of:\n
       *  - Writing the range of data values to target
       *  - Reading the entire peripheral state back from target
       * 
       * @param address Start address of data range
       * @param size    Size in bytes of data range
       */
      public void synchronizeValue(final long address, final int size) {
         long offset = address-this.address;
         assert (offset>=0)                : "Invalid address range";
         assert ((offset+size)<=this.size) : "Invalid address";
         final byte[] writeData = new byte[size];
         System.arraycopy(data, (int) offset, writeData, 0, size);
         Runnable r = new Runnable() {
            public void run() {
//               System.err.println(String.format("MemoryBlockCache([0x%X..0x%X]).synchronizeValue.run() - writing to target", address, address+size-1));
               try {
                  gdbInterface.writeMemory(address, writeData, (int)getWidth());
               } catch (TimeoutException e1) {
                  System.err.println("Unable to access target");
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