package net.sourceforge.usbdm.deviceDatabase;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

// Represents a collection of related memory ranges
//
// This may be used to represent a non-contiguous range of memory locations that are related
// e.g. two ranges of Flash that are controlled by the same Flash controller as occurs in some HCS08s
//
public class MemoryRegion {

   public static class MemoryRange {
      public long  start;
      public long  end;

      public MemoryRange(long memoryStartAddress, long memoryEndAddress) {
         this.start = memoryStartAddress;
         this.end   = memoryEndAddress;
      }
      @Override
      public String toString() {
         return "[0x" + Long.toString(start,16) + ",0x" + Long.toString(end,16) + "]";
      }
   };
   public final int DefaultPageNo = 0xFFFF;
   public final int NoPageNo      = 0xFFFE;

   Vector<MemoryRegion.MemoryRange>      memoryRanges;           //!< Memory ranges making up this region
   MemoryType               type;                   //!< Type of memory regions

   /**
    * Constructor
    */
   public MemoryRegion(MemoryType memType) {
      this.type = memType;
      memoryRanges = new Vector<MemoryRegion.MemoryRange>();
   }

   //! Add a memory range to this memory region
   //!
   //! @param startAddress - start address (inclusive)
   //! @param endAddress   - end address (inclusive)
   //! @param pageNo       - page number (if used)
   //!
   public void addRange (MemoryRegion.MemoryRange memoryRange) {
      if (memoryRanges.size() == memoryRanges.capacity()) {
         int newCapacity = 2*memoryRanges.capacity();
         if (newCapacity < 8) {
            newCapacity = 8;
         }
         memoryRanges.ensureCapacity(newCapacity);
      }
      memoryRanges.add(memoryRanges.size(), memoryRange);
   }

   //! Add a memory range to this memory region
   //!
   //! @param startAddress - start address (inclusive)
   //! @param endAddress   - end address (inclusive)
   //! @param pageNo       - page number (if used)
   //!
   public void addRange(int startAddress, int endAddress) {
      addRange(new MemoryRange(startAddress, endAddress));
   }

   public Iterator<MemoryRegion.MemoryRange> iterator() {
      return memoryRanges.iterator();
   }

   //! Indicates if the memory region is of a programmable type e.g Flash, eeprom etc.
   //!
   //! @return - true/false result
   //!
   public boolean isProgrammableMemory() {
      return type.isProgrammable;
   }

   //! Get name of memory type
   //!
   public String getMemoryTypeName() {
      return type.name;
   }

   //! Get type of memory
   //!
   public MemoryType getMemoryType() {
      return type;
   }

   @Override
   public String toString() {
      String rv = type.name + "(";
      ListIterator<MemoryRegion.MemoryRange> it = memoryRanges.listIterator();
      while (it.hasNext()) {
         MemoryRegion.MemoryRange memoryRange = it.next();
         rv += memoryRange.toString();
      }
      rv += ")";
      return rv; 
   }
}