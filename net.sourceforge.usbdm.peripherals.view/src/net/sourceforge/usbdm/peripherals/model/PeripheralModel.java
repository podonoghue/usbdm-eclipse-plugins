package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.peripheralDatabase.AddressBlock;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripherals.view.GdbCommonInterface;

/**
    * Model for a peripheral tree item
    *
    */
   public class PeripheralModel extends BaseModel implements UpdateInterface, MemoryBlockChangeListener {
    
      ArrayList<MemoryBlockCache> memoryBlockCaches = new ArrayList<MemoryBlockCache>();
      
      /**
       * Constructor
       * 
       * @param parent      Parent of this element in tree
       * @param peripheral  Underlying peripheral
       */
      public PeripheralModel(BaseModel parent, Peripheral peripheral, GdbCommonInterface gdbInterface) {
         super(parent, peripheral.getName(), peripheral.getCDescription());
         assert(parent != null) : "parent can't be null";
         address = peripheral.getBaseAddress();
         
         // Use address blocks to create target memory cache
         // The Model registers as change listener
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