package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.peripheralDatabase.AddressBlock;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripherals.view.GdbCommonInterface;

   /**
    * Model for a peripheral tree item
    *
    */
   public class PeripheralModel extends RegisterHolder implements UpdateInterface, MemoryBlockChangeListener {
    
      private ArrayList<MemoryBlockCache> memoryBlockCaches = new ArrayList<MemoryBlockCache>();
      private boolean                     refreshAll;  
      
      /**
       * Constructor
       * 
       * @param parent      Parent of this element in tree
       * @param peripheral  Underlying peripheral
       * @throws Exception 
       */
      public PeripheralModel(BaseModel parent, Peripheral peripheral, GdbCommonInterface gdbInterface) {
         super(parent, peripheral.getName(), peripheral.getDescription());

         assert(parent != null) : "parent can't be null";
         
         refreshAll  = peripheral.isRefreshAll();
         address     = peripheral.getBaseAddress();
         
         // Use address blocks to create target memory cache
         // The Model is registered as a change listener
         for (AddressBlock memoryAddressBlock : peripheral.getAddressBlocks()) {
            MemoryBlockCache memoryBlockCache = new MemoryBlockCache(peripheral, memoryAddressBlock, gdbInterface);
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
            if (memoryBlockCache.isReadable()) {
               memoryBlockCache.setNeedsUpdate(needsUpdate);
            }
         }
      }

      /**
       *  Updates all storage associated with this peripheral from target as needed.
       */
      public void update() {
         for (MemoryBlockCache memoryBlockCache : memoryBlockCaches) {
            if (memoryBlockCache.isReadable()) {
               memoryBlockCache.update(this);
            }
         }
      }
      
      /**
       *  Retrieves storage contents from target (if update not already pending). This may trigger a view update.
       */
      public void retrieveValue() {
         for (MemoryBlockCache memoryBlockCache : memoryBlockCaches) {
            if (memoryBlockCache.isReadable()) {
               memoryBlockCache.retrieveValue(this);
            }
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
       * @param sizeInBytes in bytes!
       * 
       * @return null if not found, block otherwise
       */
      @Override
      public MemoryBlockCache findAddressBlock(long address, long sizeInBytes) {
         for (MemoryBlockCache memoryBlockCache : memoryBlockCaches) {
            if (memoryBlockCache.containsAddress(address, sizeInBytes)) {
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

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.BaseModel#safeGetValueAsString()
       */
      @Override
      public String safeGetValueAsString() {
         return "";
      }

      /**
       * Receive notification that the child register has changed
       * 
       * @param child
       */
      @Override
      public void registerChanged(RegisterModel child) {
         if (refreshAll) {
            forceUpdate();
         }
      }

   }