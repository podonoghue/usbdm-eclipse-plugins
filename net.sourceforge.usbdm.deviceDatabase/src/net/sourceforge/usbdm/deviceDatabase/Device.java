package net.sourceforge.usbdm.deviceDatabase;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

public class Device {

   public enum MemoryType {
      MemInvalid  ("MemInvalid", "",        false, false ), 
      MemRAM      ("MemRAM",     "ram",     false, true  ), 
      MemEEPROM   ("MemEEPROM",  "eeprom",  true,  false ), 
      MemFLASH    ("MemFLASH",   "flash",   true,  false ), 
      MemFlexNVM  ("MemFlexNVM", "flexNVM", true,  false ), 
      MemFlexRAM  ("MemFlexRAM", "flexRAM", false, true  ), 
      MemROM      ("MemROM",     "rom",     false, false ), 
      MemIO       ("MemIO",      "io",      false, false ), 
      MemPFlash   ("MemPFlash",  "pFlash",  true,  false ), 
      MemDFlash   ("MemDFlash",  "dFlash",  true,  false ), 
      MemXRAM     ("MemXRAM",    "xRAM",    false, true  ),    // DSC
      MemPRAM     ("MemPRAM",    "pRAM",    false, true  ),    // DSC
      MemXROM     ("MemXROM",    "xROM",    true,  false ),    // DSC
      MemPROM     ("MemPROM",    "pROM",    true,  false ),    // DSC
      ;
      public final String  name;
      public final String  xmlName;
      public final boolean isProgrammable;
      public final boolean isRam;

      MemoryType(String name, String xmlName, boolean isProgrammable, boolean isRam) {
         this.name = name;
         this.xmlName        = xmlName;
         this.isProgrammable = isProgrammable;
         this.isRam          = isRam;
      }
      
      static public MemoryType getTypeFromXML(String memoryType) {
//         System.err.println("getTypeFromXML(" + memoryType + ")");
         MemoryType[] memoryTypes = MemoryType.values();
         for (int index=0; index<memoryTypes.length; index++) {
//            System.err.println("getTypeFromXML() checking \"" + memoryTypes[index].xmlName + "\"");
            if (memoryType.equals(memoryTypes[index].xmlName)) {
//               System.err.println("getTypeFromXML() found it \"" + memoryTypes[index].xmlName + "\"");
                return memoryTypes[index];
            }
         }
         return null;
      }
      
      public String toString() {
         return name;
      }
   };

   public static class GnuInfo {
      String id;
      String value;
      String path;
      String text;
      String name;
      String command;
      
      public GnuInfo(String id, String value, String path, String name, String command, String text) {
         this.id        = id;
         this.value     = value;
         this.path      = path;
         this.name      = name;
         this.command   = command;
         this.text      = text;
      }
      public String toString() {
         return "GnuInfo["+id+", "+value+", "+path+", "+name+", "+command+", "+text+"]";
      }
      public PrintStream toXML(PrintStream xmlOut, int indent) {
         doIndent(xmlOut, indent);
         xmlOut.format("<gnuInfo id=\""+getId()+"\"");
         String value = getValue();
         if ((value != null) && !value.isEmpty()) { 
            xmlOut.format(" value=\""+getId()+"\"");
         }
         xmlOut.print(">\n");
         return xmlOut;
      }
      public PrintStream toOptionXML(PrintStream xmlOut) {
         xmlOut.print("<enumeratedOptionValue");
         xmlOut.print(" name=\"" + name + "\"");
         xmlOut.print(" id=\"" + value + "\"");
         xmlOut.print(" command=\"" +command + "\"");
         xmlOut.print("/>\n");
         return xmlOut;
      }

      public String getId() {
         return id;
      }
      public String getValue() {
         return value;
      }
      public String getPath() {
         return path;
      }
      public String getText() {
         return text;
      }
      public String getName() {
         return name;
      }
      public String getCommand() {
         return command;
      }
   }
   
   public static class GnuInfoList extends ArrayList<GnuInfo> {
      private static final long serialVersionUID = -3917653198490307939L;

      void put(GnuInfo info) {
         this.add(info);
//         System.err.println("GnuInfoList.put("+info.toString()+")");
      }
      GnuInfo find(String key) {
         Iterator<GnuInfo> it = this.iterator();
         while(it.hasNext()) {
            GnuInfo gnuInfo = it.next();
            if (gnuInfo.getId().equals(key)) {
               return gnuInfo;
            }
         }     
         return null;
      }
      public PrintStream toXML(PrintStream xmlOut, int indent) {
         doIndent(xmlOut, indent);
         xmlOut.print("<gnuInfoList");
         // No attributes
         xmlOut.print(">\n");
         Iterator<GnuInfo> it = this.iterator();
         while(it.hasNext()) {
            GnuInfo gnuInfo = it.next();
            gnuInfo.toXML(xmlOut, indent+3);
         }     
         doIndent(xmlOut, indent);
         xmlOut.print("</gnuInfoList>\n");
         return xmlOut;
      }
   }
   
   // Represents a collection of related memory ranges
   //
   // This may be used to represent a non-contiguous range of memory locations that are related
   // e.g. two ranges of Flash that are controlled by the same Flash controller as occurs in some HCS08s
   //
   public static class MemoryRegion {

      public static class MemoryRange {
         public long  start;
         public long  end;

         public MemoryRange(long memoryStartAddress, long memoryEndAddress) {
            this.start = memoryStartAddress;
            this.end   = memoryEndAddress;
         }
         public String toString() {
            return "[0x" + Long.toString(start,16) + ",0x" + Long.toString(end,16) + "]";
         }
      };
      public final int DefaultPageNo = 0xFFFF;
      public final int NoPageNo      = 0xFFFE;

      Vector<MemoryRange>      memoryRanges;           //!< Memory ranges making up this region
      MemoryType               type;                   //!< Type of memory regions

      /**
       * Constructor
       */
      public MemoryRegion(MemoryType memType) {
         this.type = memType;
         memoryRanges = new Vector<Device.MemoryRegion.MemoryRange>();
      }
      
      //! Add a memory range to this memory region
      //!
      //! @param startAddress - start address (inclusive)
      //! @param endAddress   - end address (inclusive)
      //! @param pageNo       - page number (if used)
      //!
      public void addRange (MemoryRange memoryRange) {
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

      public Iterator<MemoryRange> iterator() {
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

      public String toString() {
         String rv = type.name + "(";
         ListIterator<MemoryRange> it = memoryRanges.listIterator();
         while (it.hasNext()) {
            MemoryRange memoryRange = it.next();
            rv += memoryRange.toString();
         }
         rv += ")";
         return rv; 
      }
   };   

   private String                   name;
   private boolean                  defaultDevice;
   private String                   alias;
   private Vector<MemoryRegion>     memoryRegions;
   private GnuInfoList              gnuInfoMap;
   private String                   family;
   private long                     soptAddress;
   
   public Device(String name) {
      this.name          = name;
      this.defaultDevice = false;
      memoryRegions      = new Vector<Device.MemoryRegion>();
      gnuInfoMap         = null;
      family             = null;
   }
   
   void addMemoryRegion(MemoryRegion memoryRegion) {
      memoryRegions.add(memoryRegion);
   }
   
   public Iterator<MemoryRegion> getMemoryRegionIterator() {
      return memoryRegions.iterator();
   }

   public String toString() {
      String rv = name;
      if (soptAddress != 0) {
         rv += String.format(",SOPT=0x%X", soptAddress);
      }
      rv += memoryRegions.toString();
      if (defaultDevice) {
         rv += ("(default)");
      }
      return rv;
   }

   public static PrintStream doIndent(PrintStream xmlOut, int indent) {
      for(int index=indent; index-->0;) {
         xmlOut.print(" ");
      }
      return xmlOut;
   }
   
   public PrintStream toXML(PrintStream xmlOut, int indent) {
      doIndent(xmlOut, indent);
      xmlOut.print("<device name=\"" + getName() + "\"");
      if (isDefault()) {
         doIndent(xmlOut, indent);
         xmlOut.print(" default=\"true\"");
      }
      if (isAlias()) {
         doIndent(xmlOut, indent);
         xmlOut.print(" alias=\"" + getAlias() + "\"");
      }
      xmlOut.print(">\n");
      GnuInfoList gnuInfoList = getGnuInfoMap();
      if (gnuInfoList != null) {
         gnuInfoList.toXML(xmlOut, indent+3);
      }
      doIndent(xmlOut, indent);
      xmlOut.print("</device>\n");
      return xmlOut;
   }
   
   public void addOptionXML(HashMap<String, GnuInfo> map) {
      final String ID_GNU_MCPU="net.sourceforge.usbdm.cdt.toolchain.gcc.mcpu";
      GnuInfoList gnuInfoList = getGnuInfoMap();
      if (gnuInfoList != null) {
         GnuInfo gnuInfo = gnuInfoList.find(ID_GNU_MCPU);
         if (gnuInfo != null) {
            String value   = gnuInfo.getValue();
            if (!map.containsKey(value)) {
               map.put(value, gnuInfo);
            }
         }
      }
   }

   public void setDefault(boolean isDefault) {
      defaultDevice = isDefault;
   }
   public void setDefault() {
      defaultDevice = true;
   }
   public boolean isDefault() {
      return defaultDevice;
   }
   public void setAlias(String name) {
      alias = name;
   }
   public boolean isAlias() {
      return alias != null;
   }
   public String getAlias() {
      return alias;
   }
   public String getName() {
      return name;
   }
   public void setFamily(String family) {
      this.family = family;
   }
   public String getFamily() {
      return family;
   }
   /**
    * @return the map
    */
   public GnuInfoList getGnuInfoMap() {
      return gnuInfoMap;
   }
   public void setGnuInfoMap(GnuInfoList map) {
      gnuInfoMap = map;
   }
   public void setSoptAddress(long soptAddress) {
      this.soptAddress = soptAddress;
   }
   public long getSoptAddress() {
      return soptAddress;
   }
}
