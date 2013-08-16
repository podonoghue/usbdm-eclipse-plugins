package net.sourceforge.usbdm.deviceDatabase;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import net.sourceforge.usbdm.jni.Usbdm.TargetType;

public class Device {

   //! RS08/HCS08/CFV1 clock types
   //!
   public static enum ClockTypes {
      INVALID        (-1, "Invalid"      ),
      EXTERNAL        (0, "External"     ),
      S08ICGV1        (1, "S08ICGV1"     ),
      S08ICGV2        (2, "S08ICGV2"     ),
      S08ICGV3        (3, "S08ICGV3"     ),
      S08ICGV4        (4, "S08ICGV4"     ),
      S08ICSV1        (5, "S08ICSV1"     ),
      S08ICSV2        (6, "S08ICSV2"     ),
      S08ICSV2x512    (7, "S08ICSV2x512" ),
      S08ICSV3        (8, "S08ICSV3"     ),
      S08ICSV4        (9, "S08ICSV4"     ),
      RS08ICSOSCV1   (10, "RS08ICSOSCV1" ),
      RS08ICSV1      (11, "RS08ICSV1"    ),
      S08MCGV1       (12, "S08MCGV1"     ),
      S08MCGV2       (13, "S08MCGV2"     ),
      S08MCGV3       (14, "S08MCGV3"     ),
      ;

      private final int    mask;
      private final String name;

      // Used for reverse lookup of frequency (Hz)
      private static final Map<String,ClockTypes> lookupString 
         = new HashMap<String, ClockTypes>();

      static {
         for(ClockTypes ct : ClockTypes.values()) {
            lookupString.put(ct.name, ct);
         }
      }

      ClockTypes(int mask, String name) {
         this.mask = mask;
         this.name = name;
      }
      public int getMask() {
         return mask;
      }
      public String getName() {
         return name;
      }
      /**
       *   Get matching ClockType
       *   
       *   @param name Readable name of ClockType
       * 
       *   @return ClockSpeed matching (exactly) the frequency given or the default value if not found.
       */
      public static ClockTypes parse(String name) {
         ClockTypes rv = lookupString.get(name);
         if (rv == null) {
            rv = INVALID;
         }
         return  rv;
      }   
   };

   //! Returns the default non-volatile flash location for the clock trim value
   //!
   //! @param  clockType - clock type being queried
   //!
   //! @return Address of clock trim location(s)
   //!
   static public int getDefaultClockTrimNVAddress(TargetType targetType, ClockTypes clockType) {
      if (targetType == TargetType.T_RS08) {
         return 0x3FFA;
      }
      else if (targetType == TargetType.T_CFV1) {
         return 0x3FE;
      }
      else if (targetType == TargetType.T_HCS08) {
         switch (clockType) {
         case S08ICGV1 :
         case S08ICGV2 :
         case S08ICGV3 :
         case S08ICGV4 :      
            return 0xFFBE;

         case S08ICSV1 :
         case S08ICSV2 :
         case S08ICSV2x512 :
         case S08ICSV3 :
         case RS08ICSOSCV1 :
         case RS08ICSV1 :     
            return 0xFFAE;

         case S08ICSV4 :      
            return 0xFF6E;

         case S08MCGV1 :
         case S08MCGV2 :
         case S08MCGV3 :      
            return 0xFFAE;

         case INVALID :
         case EXTERNAL :
         default :            
            return 0;
         }
      }
      else {
         return 0;
      }
   }

   //! Returns the default non-volatile flash location for the clock trim value
   //!
   //! @return Address of clock trim location(s) for the current clock type
   //!
   public int getDefaultClockTrimNVAddress() {
      return getDefaultClockTrimNVAddress(targetType, clockType);
   }

   //! Returns the default (nominal) trim frequency for the currently selected clock
   //!
   //! @return clock trim frequency in Hz.
   //!
   static public int getDefaultClockTrimFreq(ClockTypes clockType) {
      switch (clockType) {
      case S08ICGV1 :
      case S08ICGV2 :
      case S08ICGV3 :
      case S08ICGV4 :
         return 243000;

      case S08ICSV1 :
      case S08ICSV2 :
      case S08ICSV2x512 :
      case S08ICSV3 :
      case S08ICSV4 :
      case RS08ICSOSCV1 :
      case RS08ICSV1 :
         return 31250;

      case S08MCGV1 :
      case S08MCGV2 :
      case S08MCGV3 :
         return 31250;

      case INVALID :
      case EXTERNAL :
      default :
         return 0;
      }
   }

   //! Returns the default (nominal) trim frequency for the currently selected clock
   //!
   //! @return clock trim frequency in Hz.
   //!
   public int getDefaultClockTrimFreq()  {
      return getDefaultClockTrimFreq(clockType);
   }

   /**
    * Selects erase method
    */
   public static enum EraseMethod {
      ERASE_NONE        (0x00, "None"),       //!< No erase is done
      ERASE_MASS        (0x01, "Mass"),       //!< A Mass erase operation is done
      ERASE_ALL         (0x02, "All"),        //!< All Flash is erased (by Flash Block)
      ERASE_SELECTIVE   (0x03, "Selective"),  //!< A selective erase (by sector) is done
      ;
      private final int    mask;
      private final String name;
      EraseMethod(int mask, String name) {
         this.mask = mask;
         this.name = name;
      }
      public int getMask() {
         return mask;
      }
      public static EraseMethod valueOf(int mask) {
         for (EraseMethod type:values()) {
            if (type.mask == mask) {
               return type;
            }
         }
         return ERASE_MASS;
      }
      public String getName() {
         return name;
      }
   };

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

   public static class FileInfo {
      String id;
      String source;
      String target;
      
      public FileInfo(String id, String source, String target) {
         super();
         this.id = id;
         this.source = source;
         this.target = target;
      }
      public String getId() {
         return id;
      }
      public void setId(String id) {
         this.id = id;
      }
      public String getSource() {
         return source;
      }
      public void setSource(String source) {
         this.source = source;
      }
      public String getTarget() {
         return target;
      }
      public void setTarget(String copyPath) {
         this.target = copyPath;
      }
      public boolean isReplaceable() {
         return true;
      }
      public String toString() {
         StringBuffer buffer = new StringBuffer(2000);
         buffer.append(" source=\""+source+"\" => ");
         buffer.append(" target=\""+target+"\"");
         return buffer.toString();
      }
      public String toXML() {
         StringBuffer buffer = new StringBuffer(2000);
         buffer.append("<element>\n");
         buffer.append("   <simple name=\"source\"      value=\""+source+"\" />\n");
         buffer.append("   <simple name=\"target\"      value=\""+target+"\" />\n");
         buffer.append("   <simple name=\"replaceable\" value=\"true\" />\n");
         buffer.append("</element>\n");
         return buffer.toString();
      }
   }
   
   public static class GnuInfo {
      String id;
      String value;
      String path;
      String text;
      String name;
      String command;

      public GnuInfo(String id, String value, String path, String name, String command, String text) {
         super();
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

   public static class FileList extends ArrayList<FileInfo> {

      private static final long serialVersionUID = 3622396071313657392L;
      
      void put(FileInfo info) {
         this.add(info);
//         System.err.println("FileList.put("+info.toString()+")");
      }
      FileInfo find(String key) {
         Iterator<FileInfo> it = this.iterator();
         while(it.hasNext()) {
            FileInfo fileInfo = it.next();
            if (fileInfo.getId().equals(key)) {
               return fileInfo;
            }
         }     
         return null;
      }
      public String toString() {
         StringBuffer buffer = new StringBuffer();
         Iterator<FileInfo> it = this.iterator();
         while(it.hasNext()) {
            FileInfo fileInfo = it.next();
            buffer.append(fileInfo.toString());
         }
         return buffer.toString();     
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
   private FileList                 fileListMap;
   private String                   family;
   private long                     soptAddress;
   private TargetType               targetType;
   private ClockTypes               clockType;
   private int                      clockAddres;
   private int                      clockNvAddress;
   private int                      clockTrimFrequency;
   
   public Device(TargetType targetType, String name) {
      this.name          = name;
      this.defaultDevice = false;
      this.targetType    = targetType;
      
      setClockType(ClockTypes.INVALID);
      setClockAddres(0);
      
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

   public ClockTypes getClockType() {
      return clockType;
   }

   public void setClockType(ClockTypes clockType) {
      this.clockType = clockType;
   }

   public int getClockAddres() {
      return clockAddres;
   }

   public void setClockAddres(int clockAddres) {
      this.clockAddres = clockAddres;
   }

   public int getClockNvAddress() {
      return clockNvAddress;
   }

   public void setClockNvAddress(int clockNvAddress) {
      this.clockNvAddress = clockNvAddress;
   }

   public int getClockTrimFrequency() {
      return clockTrimFrequency;
   }

   public void setClockTrimFrequency(int clockTrimFrequency) {
      this.clockTrimFrequency = clockTrimFrequency;
   }

   public boolean isDefaultDevice() {
      return defaultDevice;
   }

   public TargetType getTargetType() {
      return targetType;
   }

   public FileList getFileListMap() {
      return fileListMap;
   }

   public void setFileListMap(FileList fileListMap) {
      this.fileListMap = fileListMap;
   }
}
