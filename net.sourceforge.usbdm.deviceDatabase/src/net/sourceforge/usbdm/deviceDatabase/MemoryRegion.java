package net.sourceforge.usbdm.deviceDatabase;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

/** 
 * Represents a collection of related memory ranges
 *
 * This may be used to represent a non-contiguous range of memory locations that are related
 * e.g. two ranges of Flash that are controlled by the same Flash controller as occurs in some HCS08s
 */
public class MemoryRegion {

   public static class MemoryRange implements Comparable<MemoryRange> {
      public  long   start;
      public  long   end;
      private String name;

      public MemoryRange(long memoryStartAddress, long memoryEndAddress) {
         this.start = memoryStartAddress;
         this.end   = memoryEndAddress;
         this.name  = null;
      }
      @Override
      public String toString() {
         return String.format("%s[0x%06X-0x%06X]", (name==null)?"":("("+name+")"), start, end);
      }
      
      public String getName() {
         return name;
      }
      public void setName(String name) {
         this.name = name;
      }
      @Override
      public int compareTo(MemoryRange other) {
         if ((this.start - other.start) < 0) {
            return -1;
         }
         if ((this.start - other.start) > 0) {
            return 1;
         }
         return 0;
      }
   };
   
   public final int DefaultPageNo = 0xFFFF;
   public final int NoPageNo      = 0xFFFE;

   private Vector<MemoryRegion.MemoryRange>  memoryRanges;  //!< Memory ranges making up this region
   private MemoryType                        type;          //!< Type of memory regions
   private String                            name;          //!< Optional name of region

   /**
    * Constructor
    */
   public MemoryRegion(MemoryType memType) {
      this.type    = memType;
      memoryRanges = new Vector<MemoryRegion.MemoryRange>();
      name         = null;
   }

   /** 
    * Add a memory range to this memory region
    *
    * @param startAddress - start address (inclusive)
    * @param endAddress   - end address (inclusive)
    * @param pageNo       - page number (if used)
    */
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

   /**  
    *  Add a memory range to this memory region
    * 
    *  @param startAddress - start address (inclusive)
    *  @param endAddress   - end address (inclusive)
    *  @param pageNo       - page number (if used)
    */ 
   public void addRange(int startAddress, int endAddress) {
      addRange(new MemoryRange(startAddress, endAddress));
   }

   public Iterator<MemoryRegion.MemoryRange> iterator() {
      return memoryRanges.iterator();
   }

   /** 
    *  Indicates if the memory region is of a programmable type e.g Flash, eeprom etc.
    *
    *  @return - true/false result
    */
   public boolean isProgrammableMemory() {
      return type.isProgrammable;
   }

   /**
    *   Get name of memory type
    */ 
   public String getMemoryTypeName() {
      return type.name;
   }

   /**  
    * Get type of memory
    */ 
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

   /**
    * Set the memory region name
    * 
    * @param name
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * @return the name
    */
   public String getName() {
      return name;
   }

}