package net.sourceforge.usbdm.peripherals.model;

import net.sourceforge.usbdm.peripheralDatabase.Cluster;
import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;
import net.sourceforge.usbdm.peripheralDatabase.Register;

/**
    * Model for a register within a peripheral
    */
   public class RegisterModel extends BaseModel implements MemoryBlockChangeListener, UpdateInterface {
      protected long               resetMask;
      protected long               resetValue;
      protected int                sizeInBits;
      private   String             accessMode;
      private   MemoryBlockCache   memoryBlockCache;
      private   boolean            haveReportedChanged = false;
      private   AccessType         accessType;
      
      private void initCommon(PeripheralModel peripheral, Register register) throws Exception {
         if (register.isHidden()) {
            throw new Exception("Creating hidden register!!!");
         }
         this.sizeInBits   = (int)register.getWidth();
         this.resetValue   = register.getResetValue();
         this.resetMask    = register.getResetMask();
         this.accessType   = register.getAccessType();
         this.accessMode   = this.accessType.getAbbreviatedName();
         this.memoryBlockCache = peripheral.findAddressBlock(address, (sizeInBits+7)/8);
         if (memoryBlockCache == null) {
            throw new MemoryException(String.format("RegisterModel() %s - No memoryBlockCache found", getName()));
         }
         if (this.accessType.isWriteable()) {
            memoryBlockCache.setWriteable(true);
         }
         if (this.accessType.isReadable()) {
            memoryBlockCache.setReadable(true);
         }
      }

      public boolean isReadable() {
         return (memoryBlockCache != null) && (memoryBlockCache.isReadable()); 
      }

      public boolean isWriteable() {
         return (memoryBlockCache != null) && (memoryBlockCache.isWriteable()); 
      }
      /**
       * Constructor - applicable to simple register
       * 
       * @param peripheral       Peripheral that contains register
       * @param register         Register being created
       * @throws Exception 
       */
      public RegisterModel(PeripheralModel peripheral, Register register) throws Exception {
         super(peripheral, register.getName(), register.getCDescription());
         assert(parent != null) : "parent can't be null";
         assert(register.getDimension() == 0) : "Only applicable to simple register";
         this.address      = peripheral.getAddress() + register.getAddressOffset();
         initCommon(peripheral, register);
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
         initCommon(peripheral, register);
      }

      /**
       * Constructor - applicable to simple register within simple cluster
       * 
       * @param peripheral       Peripheral that contains register
       * @param cluster          Cluster that contains register
       * @param register         Register being created
       * @throws Exception 
       */
      public RegisterModel(PeripheralModel peripheral, Cluster cluster, Register register) throws Exception {
         super(peripheral, register.getName(), register.getCDescription());
//         System.err.println(String.format("RegisterModel#2(%s)", peripheral.getName()));
         assert(parent != null) : "parent can't be null";
         assert(cluster.getDimension() == 0) : "Only applicable to simple cluster";
         assert(register.getDimension() == 0) : "Only applicable to simple register";
         this.address      = peripheral.getAddress() + cluster.getAddressOffset()+register.getAddressOffset();
         initCommon(peripheral, register);
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
         initCommon(peripheral, register);
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
         initCommon(peripheral, register);
      }

      /**
       * Resets the model register values to their expected reset values  
       */
      public void loadResetValues() {
         if (memoryBlockCache != null) {
            memoryBlockCache.loadResetValue(address, resetValue, (sizeInBits+7)/8);
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
          return memoryBlockCache.getValue(address, (sizeInBits+7)/8);
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
         return memoryBlockCache.getLastValue(address, (sizeInBits+7)/8);
      }

      /**
       *  Updates the register value from target if needed.
       */
      public void update() {
//         System.err.println(String.format("RegisterModel.update(%s)", getName()));
         if (accessType.isReadable() && (memoryBlockCache != null)) {
            memoryBlockCache.update(parent);
         }
      }
      
      /**
       * Retrieves register value from target.
       * This may trigger a view update.
       */
      public void retrieveValue() {
         if (accessType.isReadable() && (memoryBlockCache != null)) {
            memoryBlockCache.retrieveValue(this);
         }
      }
      
      /**
       * Synchronizes register value to target.
       * This may trigger a view update
       */
      public void synchronizeValue() {
//         System.err.println(String.format("synchronizeValue()"));
         if (memoryBlockCache != null) {
            memoryBlockCache.synchronizeValue(address, (sizeInBits+7)/8);
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
         if (accessType.isWriteable() && (memoryBlockCache != null)) {
            memoryBlockCache.setValue(address, value, (sizeInBits+7)/8);
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
         if (accessType == AccessType.ReadOnly) {
            // Ignore writes to read-only register
//            System.err.println(String.format("RegisterModel.setValue() - readOnly n=%s, a=0x%X, s=%d bytes", this.getName(), address, (sizeInBits+7)/8));
            return;
         }
         try {
            if (memoryBlockCache != null) {
//               System.err.println(String.format("RegisterModel.setValue() n=%s, a=0x%X, v=%d, s=%d ", this.getName(), address, value, (sizeInBits+7)/8));
               memoryBlockCache.setValue(address, value, (sizeInBits+7)/8);
               synchronizeValue();
//         notifyAllListeners();
            }
         } catch (Exception e) {
            System.err.println(String.format("RegisterModel.setValue() n=%s, a=0x%X, s=%d bytes", this.getName(), address, (sizeInBits+7)/8));
            e.printStackTrace();
         }
         ((PeripheralModel)(this.parent)).registerChanged(this);
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#isChanged()
       */
      @Override
      public boolean isChanged() {
         if (!accessType.isReadable()) {
            return false;
         }
         if (memoryBlockCache == null) {
            return false;
         }
         return memoryBlockCache.isChanged(address, (sizeInBits+7)/8);
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getValueAsString()
       */
      @Override
      public String getValueAsString() throws MemoryException {
         if (!accessType.isReadable()) {
            return "-- not readable -- ";
         }
         if (isNeedsUpdate()) {
            update();
//            System.err.println(String.format("RegisterModel.getValueAsString(%s), update", getName()));
         }
         else {
//            System.err.println(String.format("RegisterModel.getValueAsString(%s), no update", getName()));
         }
         // Always return as HEX string
         return getValueAsHexString(getValue(), sizeInBits);
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.BaseModel#getValueAsBinaryString()
       */
      @Override
      public String getValueAsBinaryString() {
         if (!accessType.isReadable()) {
            return "-- not readable -- ";
         }
         try {
            return super.getValueAsBinaryString(getValue(), sizeInBits);
         } catch (MemoryException e) {
            e.printStackTrace();
            return "-- invalid --";
         }
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.BaseModel#getValueAsHexString()
       */
      @Override
      public String getValueAsHexString() {
         if (!accessType.isReadable()) {
            return "-- not readable -- ";
         }
         try {
            return super.getValueAsHexString(getValue(), sizeInBits);
         } catch (MemoryException e) {
            e.printStackTrace();
            return "-- invalid --";
         }
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
         if (!accessType.isReadable()) {
            return false;
         }
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
         if (memoryBlockCache.isChanged(address, (sizeInBits+7)/8)) {
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