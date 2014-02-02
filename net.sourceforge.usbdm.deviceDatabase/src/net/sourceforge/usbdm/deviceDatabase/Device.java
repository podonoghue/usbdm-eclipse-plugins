/*
 Change History
+===================================================================================
| Revision History
+===================================================================================
| 16 Nov 13 | Added subfamily field                                       4.10.6.100
+===================================================================================
*/
package net.sourceforge.usbdm.deviceDatabase;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase.FileType;
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

      @Override
      public String toString() {
         return name;
      }
   };

   public static class Condition {
      private final ProjectVariable  variable;
      private final String  value;
      private final boolean negated;
      
      public Condition(ProjectVariable variable, String value, boolean negated) {
         this.variable     = variable;
         this.value        = value;
         this.negated      = negated;
      }
      public ProjectVariable getVariable() {
         return variable;
      }
      public String getValue() {
         return value;
      }
      public boolean isNegated() {
         return negated;
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
      @Override
      public String toString() {
         StringBuffer buffer = new StringBuffer();
         Iterator<FileInfo> it = this.iterator();
         while(it.hasNext()) {
            FileInfo fileInfo = it.next();
            buffer.append(fileInfo.toString());
         }
         return buffer.toString();     
      }
      public void add(FileList fileList) {
         for(FileInfo i : fileList) {
            this.add(i);
         }

      }
   }
   
   public static class FileInfo extends ProjectAction {
      private final String    source;
      private final String    target;
      private       String    root;
      private final FileType  fileType;
      private final boolean   replaceable;

      public FileInfo(String id, String source, String target, FileType fileType, boolean isReplaceable) {
         super(id);
         this.source      = source;
         this.target      = target;
         this.fileType    = fileType;
         this.replaceable = isReplaceable;
         this.root        = null;
      }
      public String getSource() {
         return source;
      }
      public String getTarget() {
         return target;
      }
      public boolean isReplaceable() {
         return replaceable;
      }
      
      @Override
      public String toString() {
         StringBuffer buffer = new StringBuffer(2000);
         buffer.append(" source=\""+root+source+"\" => ");
         buffer.append(" target=\""+target+"\"");
         return buffer.toString();
      }

      public void setRoot(String root) {
         this.root = root;
      }

      public String getRoot() {
         return root;
      }
      
      public FileType getFileType() {
         return fileType;
      }
   }
   
   public static class ProjectOption extends ProjectAction {
      private final String   path;
      private final String[] value;

      public ProjectOption(String id, String path,  String[] value) {
         super(id);
         this.path     = path;
         this.value    = value;
//         System.err.println("ProjectOption() value = "+value[0]);
      }
      
      @Override
      public String toString() {
         return "ProjectOption["+getId()+", "+path+", "+value[0]+"]";
      }
      
      public String[]  getValue() {
         return value;
      }
      public String getPath() {
         return path;
      }
   }

   public static class ProjectVariable extends ProjectAction {
      private final String name;
      private final String description;
      private final String defaultValue;
      private String value;
      
      public ProjectVariable(String id, String name, String description, String defaultValue) {
         super(id);
         this.name         = name;
         this.description  = description;
         this.defaultValue = defaultValue;
         this.value        = defaultValue;
      }
      public String getName() {
         return name;
      }
      public String getDescription() {
         return description;
      }
      public String getDefaultValue() {
         return defaultValue;
      }
      public String getValue() {
         return value;
      }
      public void setValue(String value) {
         this.value = value;
      }
   }

   public static class CreateFolderAction extends ProjectAction {
      
      private String    target;
      private String    type;
      private String    root;
  
      public CreateFolderAction(String target, String type) {
         super(null);
         this.target = target;
         this.type   = type;
      }
      
      public String getTarget() {
         return target;
      }
      
      public String getType() {
         return type;
      }

      public void setRoot(String root) {
         this.root = root;
      }

      public String getRoot() {
         return root;
      }
   }
   
   public static class ProjectAction {
      private final String id;
      private Condition condition;

      public ProjectAction(String id) {
         this.id        = id;
         this.condition = null;
      }
      
      public String getId() {
         return id;
      }
      
      public Condition getCondition() {
         return condition;
      }
      
      public void setCondition(Condition condition) {
         this.condition = condition;
      }
   }
   
   public static class ProjectActionList extends ArrayList<ProjectAction> {

      private static final long serialVersionUID = -1493390145777975399L;
      
      public String toString(String root) {
         StringBuffer buffer = new StringBuffer();
         Iterator<ProjectAction> it = this.iterator();
         while(it.hasNext()) {
            ProjectAction action = it.next();
            buffer.append(action.toString());
         }
         return buffer.toString();     
      }
      
      public void add(ProjectActionList actionList) {
         for(ProjectAction i : actionList) {
            this.add(i);
         }
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
         @Override
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

      @Override
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
   private FileList                 fileList;
   private ProjectActionList        projectActionList;
   private String                   family;
   private String                   subFamily;
   private long                     soptAddress;
   private TargetType               targetType;
   private ClockTypes               clockType;
   private int                      clockAddres;
   private int                      clockNvAddress;
   private int                      clockTrimFrequency;
   private boolean                  hidden;
   
   public Device(TargetType targetType, String name) {
      this.name          = name;
      this.defaultDevice = false;
      this.targetType    = targetType;
      
      setClockType(ClockTypes.INVALID);
      setClockAddres(0);
      
      memoryRegions      = new Vector<Device.MemoryRegion>();
      family             = null;
      subFamily          = null;
      projectActionList  = null;
   }

   void addMemoryRegion(MemoryRegion memoryRegion) {
      memoryRegions.add(memoryRegion);
   }

   public Iterator<MemoryRegion> getMemoryRegionIterator() {
      return memoryRegions.iterator();
   }

   @Override
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
      doIndent(xmlOut, indent);
      xmlOut.print("</device>\n");
      return xmlOut;
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
   public void setSubFamily(String subFamily) {
      this.subFamily = subFamily;
   }
   public String getSubFamily() {
      return subFamily;
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
      return fileList;
   }

   public void addToFileList(FileList fileList) {
      if (getFileListMap() == null) {
         // New map
         this.fileList = new FileList();
      }
      this.fileList.add(fileList);
   }

   public void setHidden(boolean hidden) {
      this.hidden = hidden;
  }

   public boolean isHidden() {
      return hidden;
  }

   public void addToActionList(ProjectActionList actionList) {
      if (getProjectActionList() == null) {
         // New map
         this.projectActionList = new ProjectActionList();
      }
      this.projectActionList.add(actionList);
   }

   public void addToActionList(ProjectAction action) {
      if (getProjectActionList() == null) {
         // New map
         this.projectActionList = new ProjectActionList();
      }
      this.projectActionList.add(action);
   }
   
   public ProjectActionList getProjectActionList() {
      return projectActionList;
   }
}
