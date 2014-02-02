package net.sourceforge.usbdm.peripherals.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

import net.sourceforge.usbdm.peripheralDatabase.AddressBlock;
import net.sourceforge.usbdm.peripheralDatabase.Cluster;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.Enumeration;
import net.sourceforge.usbdm.peripheralDatabase.Field;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripheralDatabase.Register;
import net.sourceforge.usbdm.peripheralDatabase.SVD_XML_Parser;

import org.eclipse.swt.widgets.Display;

/**
 *  Model used to represent the peripheral data from target
 */
public class UsbdmDevicePeripheralsModel {

   /**
    * Interface that allows triggering updates
    */
   interface UpdateInterface {
      /**
       * Forces the model to update from target
       */
      void forceUpdate();
   }
   
   /**
    * Model for a device tree item (ROOT element)
    *
    */
   public class DeviceModel extends BaseModel {
      
      public DeviceModel(String deviceName) {
         super(null, deviceName, deviceName);
      }

      /**
       * Sets all peripherals 'needsUpdate' status
       * 
       * @param needsUpdate
       */
      public void setNeedsUpdate(boolean needsUpdate) {
         for (Object peripheralModel : getChildren()) {
            ((PeripheralModel)peripheralModel).setNeedsUpdate(needsUpdate);
         }
      }
      
      /**
       * Resets the model register values to their expected reset values  
       */
      public void loadResetValues() {
         for (Object peripheralModel : getChildren()) {
            ((PeripheralModel)peripheralModel).loadResetValues();
         }
      }
      
      /**
       * Indicates that the current value of all peripheral registers are to be used as the reference
       * for determining changed values. 
       */
      public void setChangeReference() {
         for (Object peripheralModel : getChildren()) {
            ((PeripheralModel)peripheralModel).setChangeReference();
         }
      }
      
   }
   
   /**
    * Interface that allows listeners for changes in the memory block
    *
    */
   public interface MemoryBlockChangeListener {
      
      /**
       * Receive notification when the address block changes
       * 
       * @param memoryBlockCache
       */
      void notifyMemoryChanged(MemoryBlockCache memoryBlockCache);
   }
   
   public class MemoryBlockCache {
      public final long address;
      public final long size;
      public final long width;
      
      private byte[]    data;
      private byte[]    lastData;
      private boolean   needsUpdate;
      private boolean   updatePending;
      
      private ArrayList<MemoryBlockChangeListener> changeListeners = new ArrayList<MemoryBlockChangeListener>();
      
      public MemoryBlockCache(long startAddress, long size, long width) {
         this.address       = startAddress;
         this.size          = size;
         this.width         = width;
         this.needsUpdate   = true;
         this.updatePending = false;
      }
      
      public MemoryBlockCache(Peripheral peripheral, AddressBlock memoryBlockCache) {
         this.address       = peripheral.getBaseAddress()+memoryBlockCache.getOffset();
         this.size          = memoryBlockCache.getSize();
         this.width         = memoryBlockCache.getWidth();
         this.needsUpdate   = true;
         this.updatePending = false;
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
//         System.err.println(String.format("MemoryBlockCache([0x%X..0x%X]).retrieveValue schedule action", getAddress(), getAddress()+getSize()-1));
//         System.err.println(String.format("MemoryBlockCache(%s).retrieveValue()", getName()));
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
                  value = GDBInterface.readMemory(getAddress(), (int)getSize(), (int)getWidth());
               } catch (Exception e) {
                  e.printStackTrace();
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
                  GDBInterface.writeMemory(address, writeData, (int)getWidth());
               } catch (TimeoutException e1) {
                  e1.printStackTrace();
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
   
   /**
    * Model for a peripheral tree item
    *
    */
   public class PeripheralModel extends BaseModel implements UpdateInterface, MemoryBlockChangeListener {
    
      ArrayList<MemoryBlockCache> memoryBlockCaches = new ArrayList<UsbdmDevicePeripheralsModel.MemoryBlockCache>();
      
      /**
       * Constructor
       * 
       * @param parent      Parent of this element in tree
       * @param peripheral  Underlying peripheral
       */
      public PeripheralModel(BaseModel parent, Peripheral peripheral) {
         super(parent, peripheral.getName(), peripheral.getCDescription());
         assert(parent != null) : "parent can't be null";
         address = peripheral.getBaseAddress();
         
         // Use address blocks to create target memory cache
         // The Model registers as change listener
         for (AddressBlock memoryAddressBlock : peripheral.getAddressBlocks()) {
            MemoryBlockCache memoryBlockCache = new MemoryBlockCache(peripheral, memoryAddressBlock);
            memoryBlockCaches.add(memoryBlockCache);
            memoryBlockCache.addChangeListener(this);
         }
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.ObservableModel#notifyListeners()
       */
      @Override
      protected void notifyListeners() {
//         System.err.println("PeripheralModel.notifyListeners()");
         super.notifyListeners();
      }

      /**
       * Resets the peripheral register values to their expected reset values  
       */
      public void loadResetValues() {
         for (Object registerModel : getChildren()) {
            ((RegisterModel)registerModel).loadResetValues();
         }         
      }

      /**
       * Sets all storage associated with this peripheral as stale (needing update from target)
       * 
       * @param needsUpdate
       */
      public void setNeedsUpdate(boolean needsUpdate) {
         for (MemoryBlockCache memoryBlockCache : memoryBlockCaches) {
            memoryBlockCache.setNeedsUpdate(needsUpdate);
         }
      }

      /**
       *  Updates all storage associated with this peripheral from target as needed.
       */
      void update() {
         for (MemoryBlockCache memoryBlockCache : memoryBlockCaches) {
            memoryBlockCache.update(this);
         }
      }
      
      /**
       *  Updates all storage associated with this peripheral from target.
       */
      public void retrieveValue() {
         for (MemoryBlockCache memoryBlockCache : memoryBlockCaches) {
            memoryBlockCache.retrieveValue(this);
         }
      }

      /**
       * Indicates that the current value of all registers in peripheral are to be
       *  used as the reference for future changed values.  
       */
      public void setChangeReference() {
         for (MemoryBlockCache memoryBlockCache : memoryBlockCaches) {
            memoryBlockCache.setChangeReference();
         }
      }

      /**
       * Finds the MemoryBlockCache containing this register
       * 
       * @param address
       * @param size in bytes!
       * 
       * @return null if not found, block otherwise
       */
      public MemoryBlockCache findAddressBlock(long address, long size) {
         for (MemoryBlockCache memoryBlockCache : memoryBlockCaches) {
            if (memoryBlockCache.containsAddress(address, size)) {
               return memoryBlockCache;
            }
         }
         return null;
      }

      /**
       * Force update from target
       */
      @Override
      public void forceUpdate() {
         setNeedsUpdate(true);
         retrieveValue();
      }

      /** 
       * Memory blocks use this method to notify changes.
       * It then calls notifyListeners().
       */
      @Override
      public void notifyMemoryChanged(MemoryBlockCache memoryBlockCache) {
//         System.err.println("PeripheralModel.notifyMemoryChanged()");
         notifyListeners();
      }

   }

   /**
    * Model for a register within a peripheral
    */
   public class RegisterModel extends BaseModel implements MemoryBlockChangeListener, UpdateInterface {
      protected final long               resetMask;
      protected final long               resetValue;
      protected final int                size;
      private   final String             accessMode;
      private   final MemoryBlockCache   memoryBlockCache;
      private         boolean            haveReportedChanged = false;

      /**
       * Constructor - applicable to simple register
       * 
       * @param peripheral       Peripheral that contains register
       * @param register         Register being created
       * 
       * @throws Exception 
       */
      private RegisterModel(PeripheralModel peripheral, Register register) {
         super(peripheral, register.getName(), register.getCDescription());
         assert(parent != null) : "parent can't be null";
         assert(register.getDimension() == 0) : "Only applicable to simple register";
         this.address      = peripheral.getAddress() + register.getAddressOffset();
         this.size         = (int)register.getWidth();
         this.resetValue   = register.getResetValue();
         this.resetMask    = register.getResetMask();
         this.accessMode   = register.getAccessType().getAbbreviatedName();
         this.memoryBlockCache = peripheral.findAddressBlock(address, (size+7)/8);
         if (memoryBlockCache == null) {
            System.err.println("RegisterModel() - No memoryBlockCache found");
         }
      }

      /**
       * Constructor - applicable to register simple array
       * 
       * @param peripheral       Peripheral that contains register
       * @param register         Register being created
       * @param registerIndex    Index of register within register array
       * 
       * @throws Exception 
       */
      private RegisterModel(PeripheralModel peripheral, Register register, int registerIndex) throws Exception {
         super(peripheral, register.getName(registerIndex), register.getCDescription(registerIndex));
         assert(parent != null) : "parent can't be null";
         assert(register.getDimension() != 0) : "Only applicable to register array";
         this.address    = peripheral.getAddress() + register.getAddressOffset(registerIndex);
         this.size       = (int)register.getWidth();
         this.resetValue = register.getResetValue();
         this.resetMask  = register.getResetMask();
         this.accessMode   = register.getAccessType().getAbbreviatedName();
         this.memoryBlockCache = peripheral.findAddressBlock(address, (size+7)/8);
         if (memoryBlockCache == null) {
            System.err.println("RegisterModel() - No memoryBlockCache found");
         }
      }

      /**
       * Constructor - applicable to simple register within simple cluster
       * 
       * @param peripheral       Peripheral that contains register
       * @param cluster          Cluster that contains register
       * @param register         Register being created
       * 
       * @throws Exception 
       */
      private RegisterModel(PeripheralModel peripheral, Cluster cluster, Register register) {
         super(peripheral, register.getName(), register.getCDescription());
         assert(parent != null) : "parent can't be null";
         assert(cluster.getDimension() == 0) : "Only applicable to simple cluster";
         assert(register.getDimension() == 0) : "Only applicable to simple register";
         this.address      = peripheral.getAddress() + cluster.getAddressOffset()+register.getAddressOffset();
         this.size         = (int)register.getWidth();
         this.resetValue   = register.getResetValue();
         this.resetMask    = register.getResetMask();
         this.accessMode   = register.getAccessType().getAbbreviatedName();
         this.memoryBlockCache = peripheral.findAddressBlock(address, (size+7)/8);
         if (memoryBlockCache == null) {
            System.err.println("RegisterModel() - No memoryBlockCache found");
         }
      }

      /**
       * Constructor - applicable to simple register within a cluster array
       * 
       * @param peripheral       Peripheral that contains register
       * @param cluster          Cluster that contains register
       * @param clusterIndex     Index of cluster within cluster array
       * @param register         Register being created
       * 
       * @throws Exception 
       */
      public RegisterModel(PeripheralModel peripheral, Cluster cluster, int clusterIndex, Register register) throws Exception {
         super(peripheral, cluster.getName(clusterIndex), register.getCDescription());
         assert(parent != null) : "parent can't be null";
         assert(cluster.getDimension() != 0) : "Only applicable to array cluster";
         assert(register.getDimension() == 0) : "Only applicable to simple register";
         this.address    = peripheral.getAddress() + cluster.getAddressOffset(clusterIndex)+register.getAddressOffset();
         this.size       = (int)register.getWidth();
         this.resetValue = register.getResetValue();
         this.resetMask  = register.getResetMask();
         this.accessMode   = register.getAccessType().getAbbreviatedName();
         this.memoryBlockCache = peripheral.findAddressBlock(address, (size+7)/8);
         if (memoryBlockCache == null) {
            System.err.println("RegisterModel() - No memoryBlockCache found");
         }
      }

      /**
       * Constructor - applicable to register array within a cluster array
       * 
       * @param peripheral       Peripheral that contains register
       * @param cluster          Cluster that contains register
       * @param clusterIndex     Index of cluster within cluster array
       * @param register         Register being created
       * @param registerIndex    Index of register within register array
       * 
       * @throws Exception 
       */
      public RegisterModel(PeripheralModel peripheral, Cluster cluster, int clusterIndex, Register register, int registerIndex) throws Exception {
         super(peripheral, cluster.getName(clusterIndex), register.getCDescription(registerIndex));
         assert(parent != null) : "parent can't be null";
         assert(cluster.getDimension() != 0) : "Only applicable to array cluster";
         assert(register.getDimension() != 0) : "Only applicable to simple register";
         this.address    = peripheral.getAddress() + cluster.getAddressOffset(clusterIndex)+register.getAddressOffset(registerIndex);
         this.size       = (int)register.getWidth();
         this.resetValue = register.getResetValue();
         this.resetMask  = register.getResetMask();
         this.accessMode   = register.getAccessType().getAbbreviatedName();
         this.memoryBlockCache = peripheral.findAddressBlock(address, (size+7)/8);
         if (memoryBlockCache == null) {
            System.err.println("RegisterModel() - No memoryBlockCache found");
         }
      }

      /**
       * Resets the model register values to their expected reset values  
       */
      public void loadResetValues() {
         if (memoryBlockCache != null) {
            memoryBlockCache.loadResetValue(address, resetValue, size);
         }
      }

      /**
       * Get value of register
       * 
       * @return Value
       */
      public long getValue() {
         if (memoryBlockCache == null) {
            return 0x11111115L;
         }
         return memoryBlockCache.getValue(address, size);
      }
      
      /**
       * Get last value of register i.e. register value before last change
       * 
       * @return Value
       */
      public long getLastValue() {
         if (memoryBlockCache == null) {
            return 0x11111113L;
         }
         return memoryBlockCache.getLastValue(address, size);
      }

      /**
       *  Updates the register value from target if needed.
       */
      void update() {
//         System.err.println(String.format("RegisterModel.update(%s)", getName()));
         if (memoryBlockCache != null) {
            memoryBlockCache.update(parent);
         }
      }
      
      /**
       * Retrieves register value from target.
       * This may trigger a view update.
       */
      public void retrieveValue() {
         if (memoryBlockCache != null) {
            memoryBlockCache.retrieveValue(this);
         }
      }
      
      /**
       * Synchronizes register value to target.
       * This may trigger a view update
       */
      public void synchronizeValue() {
         if (memoryBlockCache != null) {
            memoryBlockCache.synchronizeValue(address, (size+7)/8);
         }
      }
      
      /**
       * Notifies listeners of a change in value
       */
      private void notifyAllListeners() {
         notifyListeners();
      }
      
      /**
       * Set the value of the register quietly - doesn't synchronize with target
       * May triggers view update
       * Updates last value
       * 
       * @param value - Value to set
       * @throws Exception 
       */
      public void setValueQuiet(long value) throws Exception {
         if (memoryBlockCache != null) {
            memoryBlockCache.setValue(address, value, (size+7)/8);
         }
      }

      /**
       * Set the value of the register.
       * Synchronizes value with target
       * Triggers view update.
       * 
       * @param value - Value to set
       * @throws Exception 
       */
      public void setValue(long value) {
         if (getValue() == value) {
            // Quietly swallow non-changes
            return;
         }
         if (memoryBlockCache != null) {
            memoryBlockCache.setValue(address, value, (size+7)/8);
         }
//         System.err.println(String.format("setValue(0x%08X), lastValue = 0x%08X, changed=%b", value, this.getLastValue(), isChanged()));
         synchronizeValue();
//         notifyAllListeners();
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#isChanged()
       */
      @Override
      public boolean isChanged() {
         if (memoryBlockCache == null) {
            return false;
         }
         return memoryBlockCache.isChanged(address, (size+7)/8);
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getValueAsString()
       */
      @Override
      public String getValueAsString() {
         if (isNeedsUpdate()) {
            update();
//            System.err.println(String.format("RegisterModel.getValueAsString(%s), update", getName()));
         }
         else {
//            System.err.println(String.format("RegisterModel.getValueAsString(%s), no update", getName()));
         }
         // Always return as HEX string
         return getValueAsHexString(getValue(), size);
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getAccessMode()
       */
      @Override
      public String getAccessMode() {
         return accessMode;
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#hasChanged()
       */
      @Override
      public boolean isNeedsUpdate() {
         if (memoryBlockCache == null) {
            return false;
         }
         return memoryBlockCache.isNeedsUpdate();
      }

      public void setName(String name) {
         this.name = name;
      }

      /**
       * Used by the address block to notify of changes.
       * In turn, if there is a change within the register range, this is passed on to the register change listeners.
       */
      @Override
      public void notifyMemoryChanged(MemoryBlockCache memoryBlockCache) {
         if (memoryBlockCache.isChanged(address, (size+7)/8)) {
            // Always report if changed
//            System.err.println("RegisterModel.notifyMemoryChanged() - Changed - notifying listeners");
            haveReportedChanged = true;
            notifyAllListeners();
         }
         else if (haveReportedChanged) {
            // Only report if need to remove highlight
//            System.err.println("RegisterModel.notifyMemoryChanged() - Not changed but clearing highlight - notifying listeners");
            haveReportedChanged = false;
            notifyAllListeners();
         }
         else {
            // Nothing of interest changed
//            System.err.println("RegisterModel.notifyMemoryChanged() - Not changed - no action");
         }
      }

      @Override
      public void forceUpdate() {
         if (memoryBlockCache != null) {
            memoryBlockCache.setNeedsUpdate(true);
         }
      }
   }

   /**
    * Model for a field within a register within a peripheral
    *
    */
   public class FieldModel extends BaseModel implements UpdateInterface {
      protected  int                    size;
      private    int                    bitOffset;
      private    ArrayList<Enumeration> enumerations;
      private    final String           accessMode;

      public FieldModel(RegisterModel parent, Field field) {
         super(parent, field.getName(), field.getCDescription());
         assert(parent != null) : "parent can't be null";
         size                  = (int)field.getBitwidth();
         bitOffset             = (int)field.getBitOffset();
         enumerations          = null;
         accessMode            = field.getAccessType().getAbbreviatedName();
         setEnumeratedDescription(field.getEnumerations());
      }
      
      public FieldModel(RegisterModel parent, Field field, int index) throws Exception {
         super(parent, field.getName(index), field.getCDescription(index));
         assert(parent != null) : "parent can't be null";
         size                  = (int)field.getBitwidth();
         bitOffset             = (int)field.getBitOffset();
         enumerations          = null;
         accessMode            = field.getAccessType().getAbbreviatedName();
         setEnumeratedDescription(field.getEnumerations());
      }
      
      /**
       * Gets offset of bit field with register
       * 
       * @return offset (from right)
       */
      public int getBitOffset() {
         return bitOffset;
      }

      /**
       * Get value of register
       * 
       * @return Value
       */
      public long getValue() {
         RegisterModel parent = (RegisterModel) this.parent;
         return ((1l<<size)-1)&(parent.getValue()>>bitOffset);
      }
      
      /**
       * Get last value of register
       * 
       * @return Value
       */
      public long getLastValue() {
         RegisterModel parent = (RegisterModel) this.parent;
         return ((1l<<size)-1)&(parent.getLastValue()>>bitOffset);
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#isChanged()
       */
      @Override
      public boolean isChanged() {
         return getValue() != getLastValue();
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getValueAsString()
       */
      @Override
      public String getValueAsString() {
         if ((enumerations != null) || (size<9)) {
            return super.getValueAsBinaryString(getValue(), size);
         }
         else {
            return super.getValueAsHexString(getValue(), size);
         }
      }

      /**
       * Sets description of the various meanings of this field
       * 
       * @param enumeratedDescription description to set
       */
      public void setEnumeratedDescription(ArrayList<Enumeration> enumeratedDescription) {
         this.enumerations = enumeratedDescription;
      }

      /**
       * Sets description of the various meanings of this field
       * 
       * @return description
       */
      public ArrayList<Enumeration> getEnumeratedDescription() {
         return enumerations;
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getAddressAsString()
       */
      @Override
      public String getAddressAsString() {
         if (size == 1) {
            return String.format("[%d]", bitOffset);
         }
         else {
            return String.format("[%d:%d]", bitOffset+size-1, bitOffset);
         }
      }

      /**
       * Sets the value of this bit-field within the register
       * Note this actually modifies the owning register
       * May trigger view updates
       * 
       * @param bitField New bit-field value
       * @throws Exception 
       */
      public void setValue(Long bitField) {
         Long currentValue = ((RegisterModel)(this.parent)).getValue();
         Long mask         = ((1l<<size)-1)<<bitOffset;
         ((RegisterModel)(this.parent)).setValue((currentValue&~mask)|((bitField<<bitOffset)&mask));
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getAccessMode()
       */
      @Override
      public String getAccessMode() {
         return accessMode;
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#hasChanged()
       */
      @Override
      public boolean isNeedsUpdate() {
         return parent.isNeedsUpdate();
      }

      @Override
      public void forceUpdate() {
         // Pass to parent - entire register is updated
         ((RegisterModel)parent).forceUpdate();
      }

   }

   /**
    * Creates the tree entries for the bit-fields within the register
    * 
    * @param registerModel The model that represents the register and owns the bit-fields
    * @param register      The register to obtain bit-field information from
    */
   private void createFieldModels(RegisterModel registerModel, Register register) {
      
      ArrayList<Field> fieldList = register.getFields();
      if (fieldList != null) {
         for (Field field : fieldList) {
            FieldModel fieldModel = new FieldModel(registerModel, field);
            fieldModel.setEnumeratedDescription(field.getEnumerations());
         }
      }
   }

   /**
    * Creates the tree entries for the bit-fields within the register
    * 
    * @param registerModel The model that represents the register and owns the bit-fields
    * @param register      The register to obtain bit-field information from
    * @throws Exception 
    */
   private void createFieldModels(RegisterModel registerModel, Register register, int index) throws Exception {
      
      ArrayList<Field> fieldList = register.getFields();
      if (fieldList != null) {
         for (Field field : fieldList) {
            new FieldModel(registerModel, field, index);
         }
      }
   }

   /**
    * Creates the tree entry or entries for the register.  
    * Note there may be multiple as a register entry may represent multiple similar registers.
    * 
    * @param peripheralModel The model that represents the peripheral and owns the registers
    * @param register        The register to obtain register information from
    * @throws Exception 
    */
   private void createRegisterModels(PeripheralModel peripheralModel, Register register) throws Exception {
      
      if (register.getDimension() == 0) {
         // Simple register
         RegisterModel registerModel = new RegisterModel(peripheralModel, register);
         createFieldModels(registerModel, register);
      }
      else {
         // The register represents multiple registers
         for (int regIndex=0; regIndex<register.getDimension(); regIndex++) {
            RegisterModel registerModel = new RegisterModel(peripheralModel, register, regIndex);
            createFieldModels(registerModel, register, regIndex);
         }
      }
   }
   
   /**
    * Adds a set of entries for a Cluster of registers
    * 
    * @param device      The model that represents the device (entire tree) that owns the peripherals
    * @param peripheral  The peripheral to obtain register information from
    * 
    * @throws Exception 
    */
   private void createCluster(PeripheralModel peripheralModel, Cluster cluster) throws Exception {
      String nameFormat = cluster.getNameMacroFormat();
      if (cluster.getDimension()>0) {
         for(int clusterIndex=0; clusterIndex<cluster.getDimension(); clusterIndex++) {
            for (Register register : cluster.getRegisters()) {
               String name;
               name = nameFormat.replaceAll("@f", register.getName());
               name = name.replaceAll("@i", String.format("%d", clusterIndex));
               name = name.replaceAll("@a", cluster.getBaseName());
               name = name.replaceAll("@p", "");//peripheralModel.getName());
               if (register.getDimension()>0) {
                  for (int registerIndex=0; registerIndex<register.getDimension(); registerIndex++) {
                     RegisterModel registerModel = new RegisterModel(peripheralModel, cluster, clusterIndex, register, registerIndex);
                     registerModel.setName(register.format(name, registerIndex));
                     createFieldModels(registerModel, register);
                  }
               }
               else {
                  RegisterModel registerModel = new RegisterModel(peripheralModel, cluster, clusterIndex, register);
                  registerModel.setName(name);
                  createFieldModels(registerModel, register);
               }
            }
         }
      }
      else {
         for (Register register : cluster.getRegisters()) {
//            String name;
//            name = register.getName();
            RegisterModel registerModel = new RegisterModel(peripheralModel, cluster, register);
//            registerModel.setName(name);
            createFieldModels(registerModel, register);
         }
      }
   }
   
   /**
    * Creates the tree entries for a peripheral
    * 
    * @param device      The model that represents the device (entire tree) that owns the peripherals
    * @param peripheral  The peripheral to obtain register information from
    * 
    * @throws Exception 
    */
   private void createPeripheralModel(DeviceModel device, Peripheral peripheral) throws Exception {
      
      // Create peripheral mode
      PeripheralModel peripheralModel = new PeripheralModel(device, peripheral);
      
      // Add registers
      for (Cluster cluster : peripheral.getRegisters()) {
         if (cluster instanceof Register) {
            createRegisterModels(peripheralModel, (Register)cluster);
         }
         else {
            createCluster(peripheralModel, cluster);
         }
      }
   }

   HashSet<String> excludedPeripherals = null;
   
   /** 
    * Indicates devices in the database which are NOT to be displayed
    * 
    * @param name Device name to check
    * @return true => should be excluded from display
    */
   private boolean isExcludedPeripheral(String name) {
      if (excludedPeripherals == null) {
         excludedPeripherals = new HashSet<String>();
         excludedPeripherals.add("AIPS0");
         excludedPeripherals.add("AIPS1");
         excludedPeripherals.add("AXBS");
         excludedPeripherals.add("ETF");
      }
      return excludedPeripherals.contains(name);
   }
   
   /**
    * Creates the tree entries for a peripheral
    * 
    * @param deviceName    Name of the device to load peripheral description for
    * @throws Exception 
    */
   private DeviceModel createDeviceModel(String deviceName) {
      
      DeviceModel deviceModel = new DeviceModel(deviceName);
      
      if (deviceName == null) {
         // Return empty model
         return deviceModel;
      }
      // Get description of all peripherals for device
      DevicePeripherals devicePeripherals = createDevicePeripheralsDatabase(deviceName);
      
      try {
         if (devicePeripherals != null) {
            // Add the peripherals
            for (Peripheral peripheral : devicePeripherals.getPeripherals()) {
               if (isExcludedPeripheral(peripheral.getName())) {
                  continue;
               }
               createPeripheralModel(deviceModel, peripheral);     
            }
         }
      } catch (Exception e) {
         System.err.println("Error in UsbdmDevicePeripheralsModel.createDeviceModel(), reason : "+e.getMessage());
      }
      return deviceModel;
   }
   
   /**
    * Create the peripheral database
    * 
    * @return Database created
    * @throws Exception 
    */
   private static DevicePeripherals createDevicePeripheralsDatabase(String deviceName) {
      
      return SVD_XML_Parser.createDatabase(deviceName);
   }

   /**
    * Create the tree model
    * 
    * @param deviceName Name of the device (to locate database)
    * 
    * @return The root of the tree model created
    */
   public static DeviceModel createModel(String deviceName) {
      
      UsbdmDevicePeripheralsModel model = new UsbdmDevicePeripheralsModel();
      
      try {
         return model.createDeviceModel(deviceName);
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   /**
    * The constructor.
    */
   public UsbdmDevicePeripheralsModel() {
   }
   
}
