package net.sourceforge.usbdm.peripherals.model;

import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;
import net.sourceforge.usbdm.peripheralDatabase.Register;
import net.sourceforge.usbdm.peripheralDatabase.RegisterException;

/**
    * Model for a register within a peripheral
    */
   public class RegisterModel extends BaseModel implements MemoryBlockChangeListener, UpdateInterface {
      protected long               fResetMask;
      protected long               fResetValue;
      protected int                fSizeInBits;
      private   String             fAccessMode;
      private   MemoryBlockCache   fMemoryBlockCache;
//      private   boolean            fHaveReportedChanged = false;
      private   AccessType         fAccessType;
      
      private void initCommon(RegisterHolder peripheral, Register register) throws RegisterException {
         if (register.isHidden()) {
            throw new RegisterException("Creating hidden register!!!");
         }
         fSizeInBits   = (int)register.getWidth();
         fResetValue   = register.getResetValue();
         fResetMask    = register.getResetMask();
         fAccessType   = register.getAccessType();
         fAccessMode   = fAccessType.getAbbreviatedName();
         fMemoryBlockCache = peripheral.findAddressBlock(fAddress, (fSizeInBits+7)/8);
         if (fMemoryBlockCache == null) {
//            System.err.println(String.format("initCommon() reg=%s adr=0x%X", register.getName(), address));
            throw new RegisterException(String.format("RegisterModel() %s - No memoryBlockCache found", getName()));
         }
         if (fAccessType.isWriteable()) {
            fMemoryBlockCache.setWriteable(true);
         }
         if (fAccessType.isReadable()) {
            fMemoryBlockCache.setReadable(true);
         }
      }

      /**
       * Indicates if the register is readable
       * 
       * @return true/false
       */
      public boolean isReadable() {
         return (fMemoryBlockCache != null) && (fMemoryBlockCache.isReadable()); 
      }

      /**
       * Indicates if the register is writable
       * 
       * @return true/false
       */
      public boolean isWritable() {
         return (fMemoryBlockCache != null) && (fMemoryBlockCache.isWriteable()); 
      }
      
      /**
       * Constructor - applicable to simple register (which may be part of register array)
       * 
       * @param peripheral       Peripheral that contains register
       * @param register         Register being created
       * 
       * @throws RegisterException 
       */
      public RegisterModel(RegisterHolder peripheral, ModelInformation information) throws RegisterException {
         super(peripheral, information.getRegisterName(), information.getDescription());
         assert(fParent != null) : "parent can't be null";
         fAddress = information.getRegisterAddress();
         initCommon(peripheral, information.getRegister());
      }

      /**
       * Resets the model register values to their expected reset values  
       */
      public void loadResetValues() {
         if (fMemoryBlockCache != null) {
            fMemoryBlockCache.loadResetValue(fAddress, fResetValue, (fSizeInBits+7)/8);
         }
      }

      /**
       * Get value of register
       * 
       * @return Value
       * @throws MemoryException 
       */
      public long getValue() throws MemoryException {
         if (fMemoryBlockCache == null) {
            throw new MemoryException("memoryBlockCache not set");
          }
          return fMemoryBlockCache.getValue(fAddress, (fSizeInBits+7)/8);
       }
      
      /**
       * Get last value of register i.e. register value before last change
       * 
       * @return Value
       * @throws MemoryException 
       */
      public long getLastValue() throws MemoryException {
         if (fMemoryBlockCache == null) {
            return 0;
         }
         return fMemoryBlockCache.getLastValue(fAddress, (fSizeInBits+7)/8);
      }

      /**
       *  Updates the register value from target if needed.
       */
      public void update() {
//         System.err.println(String.format("RegisterModel.update(%s)", getName()));
         if (fAccessType.isReadable() && (fMemoryBlockCache != null)) {
            fMemoryBlockCache.update(fParent);
         }
      }
      
      /**
       * Set the value of the register quietly<br>
       * Doesn't synchronize with target<br>
       * Does not trigger change listeners 
       * 
       * @param value - Value to set
       * @throws Exception 
       */
      public void setValueQuiet(long value) throws Exception {
         if (!fAccessType.isWriteable() || (fMemoryBlockCache == null)) {
            // Ignore write
            return;
         }
         fMemoryBlockCache.setValue(fAddress, (fSizeInBits+7)/8, value);
      }

      /**
       * Set the value of the register.<br>
       * Synchronizes value with target.<br>
       * Triggers view update.
       * 
       * @param value - Value to set
       * @throws Exception 
       */
      public void setValue(long value) {
         if (!fAccessType.isWriteable() || (fMemoryBlockCache == null)) {
            System.err.println(String.format("RegisterModel.setValue() n=%s, a=0x%X, s=%d bytes - not writable", getName(), fAddress, (fSizeInBits+7)/8));
            return;
         }
         try {
//            System.err.println(String.format("RegisterModel.setValue() n=%s, a=0x%X, v=%d, s=%d ", getName(), fAddress, value, (fSizeInBits+7)/8));
            fMemoryBlockCache.setValue(fAddress, (fSizeInBits+7)/8, value);
            fMemoryBlockCache.synchronizeValue(fAddress, (fSizeInBits+7)/8);
//            BaseModel parent = getParent();
//            if (parent instanceof ClusterModel) {
//               parent = parent.getParent();
//            }
//            if (parent instanceof PeripheralModel) {
//               PeripheralModel peripheral = (PeripheralModel) parent;
//               peripheral.setNeedsUpdate(true);
//            }
//            notifyAllListeners();
         } catch (Exception e) {
            System.err.println(String.format("RegisterModel.setValue() n=%s, a=0x%X, s=%d bytes", getName(), fAddress, (fSizeInBits+7)/8));
            e.printStackTrace();
         }
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#isChanged()
       */
      @Override
      public boolean isChanged() {
         if (!fAccessType.isReadable()) {
            return false;
         }
         if (fMemoryBlockCache == null) {
            return false;
         }
         return fMemoryBlockCache.isChanged(fAddress, (fSizeInBits+7)/8);
      }

      /**
       * Gets status string indicating busy invalid etc.
       * 
       * @return String or null if data is available
       */
      private String getStatus() {
         if (!fAccessType.isReadable()) {
            return "<not readable>";
         }
         if (fMemoryBlockCache.isInaccessible()) {
            return "<inaccessible>";
         }
         if (fMemoryBlockCache.isNeedsUpdate()) {
            return "<pending>";
         }
         if (isNeedsUpdate()) {
          return "<pending>";
         }
         return null;
      }
      
      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getValueAsString()
       */
      @Override
      public String getValueAsString() throws MemoryException {
         // Default to HEX
         return getValueAsHexString();
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.BaseModel#getValueAsBinaryString()
       */
      @Override
      public String getValueAsBinaryString() {
         String rv = getStatus();
         if (rv != null) {
            return rv;
         }
         try {
            return super.getValueAsBinaryString(getValue(), fSizeInBits);
         } catch (MemoryException e) {
            e.printStackTrace();
            return "<invalid>";
         }
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.BaseModel#getValueAsHexString()
       */
      @Override
      public String getValueAsHexString() {
         String rv = getStatus();
         if (rv != null) {
            return rv;
         }
         try {
            return super.getValueAsHexString(getValue(), fSizeInBits);
         } catch (MemoryException e) {
            e.printStackTrace();
            return "<invalid>";
         }
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.BaseModel#getValueAsDecimalString()
       */
      @Override
      public String getValueAsDecimalString() {
         String rv = getStatus();
         if (rv != null) {
            return rv;
         }
         try {
            return String.format("%d", getValue());
         } catch (MemoryException e) {
            e.printStackTrace();
            return "<invalid>";
         }
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#getAccessMode()
       */
      @Override
      public String getAccessMode() {
         return fAccessMode;
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.ui.UsbdmDevicePeripheralsModel.BaseModel#hasChanged()
       */
      @Override
      public boolean isNeedsUpdate() {
         if (!fAccessType.isReadable()) {
            return false;
         }
         if (fMemoryBlockCache == null) {
            return false;
         }
         return fMemoryBlockCache.isNeedsUpdate();
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.MemoryBlockChangeListener#notifyMemoryChanged(net.sourceforge.usbdm.peripherals.model.MemoryBlockCache)
       */
      @Override
      public void notifyMemoryChanged(MemoryBlockCache memoryBlockCache) {

         ((RegisterHolder)(fParent)).registerChanged(this);
         //XXX
         notifyListeners();

//         if (memoryBlockCache.isChanged(fAddress, (fSizeInBits+7)/8)) {
//            // Always report if changed
////            System.err.println("RegisterModel.notifyMemoryChanged() - Changed - notifying listeners");
//            fHaveReportedChanged = true;
//            notifyListeners();
//         }
//         else if (fHaveReportedChanged) {
//            // Only report if need to remove highlight
////            System.err.println("RegisterModel.notifyMemoryChanged() - Not changed but clearing highlight - notifying listeners");
//            fHaveReportedChanged = false;
//            notifyListeners();
//         }
//         else {
//            // Nothing of interest changed
////            System.err.println("RegisterModel.notifyMemoryChanged() - Not changed - no action");
//         }
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.UpdateInterface#forceUpdate()
       */
      @Override
      public void forceUpdate() {
         if (fMemoryBlockCache != null) {
            fMemoryBlockCache.retrieveValue();
         }
      }
   }