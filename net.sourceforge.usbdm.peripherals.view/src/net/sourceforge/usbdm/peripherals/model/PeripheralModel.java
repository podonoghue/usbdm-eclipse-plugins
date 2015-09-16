package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;

import net.sourceforge.usbdm.peripheralDatabase.AddressBlock;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripherals.view.GdbCommonInterface;

   /**
    * Model for a peripheral tree item
    *
    */
   /**
    * @author Peter
    *
    */
   public class PeripheralModel extends RegisterHolder implements UpdateInterface, MemoryBlockChangeListener {
    
      private ArrayList<MemoryBlockCache> fMemoryBlockCaches = new ArrayList<MemoryBlockCache>();
      private boolean                     fRefreshAll;  
      
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
         
         fRefreshAll  = peripheral.isRefreshAll();
         fAddress     = peripheral.getBaseAddress();
         
         // Use address blocks to create target memory cache
         // The Model is registered as a change listener
         for (AddressBlock memoryAddressBlock : peripheral.getAddressBlocks()) {
            MemoryBlockCache memoryBlockCache = new MemoryBlockCache(peripheral, memoryAddressBlock, gdbInterface);
            fMemoryBlockCaches.add(memoryBlockCache);
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
         for (MemoryBlockCache memoryBlockCache : fMemoryBlockCaches) {
            if (memoryBlockCache.isReadable()) {
               memoryBlockCache.setNeedsUpdate(needsUpdate);
            }
         }
      }

      /**
       *  Updates all storage associated with this peripheral from target as needed.
       */
      public void update() {
         for (MemoryBlockCache memoryBlockCache : fMemoryBlockCaches) {
            if (memoryBlockCache.isReadable()) {
               memoryBlockCache.update(this);
            }
         }
      }
      
      /**
       * Indicates that the current value of all registers in peripheral are to be
       *  used as the reference for future changed values.  
       */
      public void setChangeReference() {
         for (MemoryBlockCache memoryBlockCache : fMemoryBlockCaches) {
            memoryBlockCache.setChangeReference();
         }
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.RegisterHolder#findAddressBlock(long, long)
       */
      @Override
      public MemoryBlockCache findAddressBlock(long address, long sizeInBytes) {
         for (MemoryBlockCache memoryBlockCache : fMemoryBlockCaches) {
            if (memoryBlockCache.containsAddress(address, sizeInBytes)) {
               return memoryBlockCache;
            }
         }
         return null;
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.UpdateInterface#forceUpdate()
       */
      @Override
      public void forceUpdate() {
//         System.err.println("PeripheralModel.forceUpdate(), " + getName());
         for (MemoryBlockCache memoryBlockCache : fMemoryBlockCaches) {
            memoryBlockCache.retrieveValue(this);
         }
      }

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.MemoryBlockChangeListener#notifyMemoryChanged(net.sourceforge.usbdm.peripherals.model.MemoryBlockCache)
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

      /* (non-Javadoc)
       * @see net.sourceforge.usbdm.peripherals.model.RegisterHolder#registerChanged(net.sourceforge.usbdm.peripherals.model.RegisterModel)
       */
      @Override
      public void registerChanged(RegisterModel child) {
         if (fRefreshAll) {
            // Update all registers when a register changes
            forceUpdate();
         }
      }

   }