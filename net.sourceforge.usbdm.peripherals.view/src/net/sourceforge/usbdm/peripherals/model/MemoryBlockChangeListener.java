package net.sourceforge.usbdm.peripherals.model;



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