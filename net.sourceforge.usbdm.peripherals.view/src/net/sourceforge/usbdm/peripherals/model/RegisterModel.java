package net.sourceforge.usbdm.peripherals.model;

import net.sourceforge.usbdm.peripheralDatabase.Cluster;
import net.sourceforge.usbdm.peripheralDatabase.Register;

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
      public RegisterModel(PeripheralModel peripheral, Register register) {
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
      public RegisterModel(PeripheralModel peripheral, Register register, int registerIndex) throws Exception {
         super(peripheral, register.getName(registerIndex), register.getCDescription(registerIndex));
//         System.err.println(String.format("RegisterModel#1(%s)", peripheral.getName()));
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
      public RegisterModel(PeripheralModel peripheral, Cluster cluster, Register register) {
         super(peripheral, register.getName(), register.getCDescription());
//         System.err.println(String.format("RegisterModel#2(%s)", peripheral.getName()));
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
       * @param peripheral       Peripheral that contains cluster
       * @param cluster          Cluster that contains register
       * @param clusterIndex     Index of cluster within cluster array
       * @param register         Register being created
       * 
       * @throws Exception 
       */
      public RegisterModel(PeripheralModel peripheral, Cluster cluster, int clusterIndex, Register register) throws Exception {
         super(peripheral, cluster.getName(clusterIndex), register.getCDescription(clusterIndex));
//         System.err.println(String.format("RegisterModel#3(%s)", peripheral.getName()));
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
//         System.err.println(String.format("RegisterModel#4(%s)", peripheral.getName()));
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
       * @throws MemoryException 
       */
      public long getValue() throws MemoryException {
         if (memoryBlockCache == null) {
            throw new MemoryException("memoryBlockCache not set");
          }
          return memoryBlockCache.getValue(address, size);
       }
      
      /**
       * Get last value of register i.e. register value before last change
       * 
       * @return Value
       * @throws MemoryException 
       */
      public long getLastValue() throws MemoryException {
         if (memoryBlockCache == null) {
            return 0;
         }
         return memoryBlockCache.getLastValue(address, size);
      }

      /**
       *  Updates the register value from target if needed.
       */
      public void update() {
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
         try {
            if (getValue() == value) {
               // Quietly swallow non-changes
               return;
            }
         } catch (MemoryException e) {
            // Ignore
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
      public String getValueAsString() throws MemoryException {
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