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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.packageParser.FileList;
import net.sourceforge.usbdm.packageParser.PackageParser;
import net.sourceforge.usbdm.packageParser.ProjectActionList;

public class Device implements Cloneable {

   private String                   name;
   private boolean                  defaultDevice;
   private String                   alias;
   private Vector<MemoryRegion>     memoryRegions;
   private FileList                 fileList;
   private String                   family;
   private String                   subFamily;
   private String                   hardware;
   private long                     soptAddress;
   private TargetType               targetType;
   private ClockTypes               clockType;
   private int                      clockAddres;
   private int                      clockNvAddress;
   private int                      clockTrimFrequency;
   private boolean                  hidden;
   
   /** List of erase methods available for this memory type */
   private ArrayList<EraseMethod>   eraseMethods = new ArrayList<EraseMethod>();

   /**
    * Constructor
    * 
    * @param targetType Type of device
    * @param name       Name of device
    */
   public Device(TargetType targetType, String name) {
      this.name          = name;
      this.defaultDevice = false;
      this.targetType    = targetType;
      
      setClockType(ClockTypes.INVALID);
      setClockAddres(0);
      
      memoryRegions      = new Vector<MemoryRegion>();
      family             = null;
      subFamily          = null;
   }

   /** Returns the default non-volatile flash location for the clock trim value
    *
    * @param  targetType - Type of device being queried
    * @param  clockType  - clock type being queried
    *
    * @return Address of clock trim location(s)
    */
   static private int getDefaultClockTrimNVAddress(TargetType targetType, ClockTypes clockType) {
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

   /**
    * Returns the default non-volatile flash location for the clock trim value
    *
    * @return Address of clock trim location(s) for the current clock type
    *
    */
   public int getDefaultClockTrimNVAddress() {
      return getDefaultClockTrimNVAddress(targetType, clockType);
   }

   /**
    * Returns the default (nominal) trim frequency for the given clock type
    *
    * @param  clockType - Clock type being queried
    * 
    * @return clock trim frequency in Hz.
    *
    */
   static private int getDefaultClockTrimFreq(ClockTypes clockType) {
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

   /**
    * Returns the default (nominal) trim frequency for the currently selected clock
    *
    * @return clock trim frequency in Hz.
    *
    */
   public int getDefaultClockTrimFreq()  {
      return getDefaultClockTrimFreq(clockType);
   }

   /**
    * Adds a memeory region to the device
    * 
    * @param memoryRegion
    */
   void addMemoryRegion(MemoryRegion memoryRegion) {
      memoryRegions.add(memoryRegion);
   }

   /**
    * Provides an iterator over the devices memory regions
    * 
    * @return
    */
   public Iterator<MemoryRegion> getMemoryRegionIterator() {
      return memoryRegions.iterator();
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      String rv = String.format("%-12s", name+",");
      if (soptAddress != 0) {
         rv += String.format(",SOPT=0x%X", soptAddress);
      }
      rv += memoryRegions.toString();
//      if (defaultDevice) {
//         rv += ("(default)");
//      }
      return rv;
   }

   /**
    * Indent to the given level
    * 
    * @param xmlOut - Stream to write to
    * @param indent - Current XML indent
    * 
    * @return the stream
    */
   public static PrintStream doIndent(PrintStream xmlOut, int indent) {
      for(int index=indent; index-->0;) {
         xmlOut.print(" ");
      }
      return xmlOut;
   }

   /**
    * Writes XML describing device
    * 
    * @param xmlOut - Stream to write to
    * @param indent - Current XML indent
    * 
    * @return the stream
    */
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

   /**
    * Sets this device as the default
    * 
    * Note: not checked
    */
   public void setDefault() {
      defaultDevice = true;
   }
   /**
    * Indicates if this device is the default
    * 
    * @return
    */
   public boolean isDefault() {
      return defaultDevice;
   }
   /**
    * @param name
    */
   public void setAlias(String name) {
      alias = name;
   }
   /**
    * @return
    */
   public boolean isAlias() {
      return alias != null;
   }
   /**
    * @return
    */
   public String getAlias() {
      return alias;
   }
   /**
    * @return
    */
   public String getName() {
      return name;
   }
   /**
    * @param family
    */
   public void setFamily(String family) {
      this.family = family;
   }
   /**
    * @return
    */
   public String getFamily() {
      return family;
   }
   /**
    * @param subFamily
    */
   public void setSubFamily(String subFamily) {
      this.subFamily = subFamily;
   }
   /**
    * @return
    */
   public String getSubFamily() {
      return subFamily;
   }
   
   /**
    * @return the hardware
    */
   public String getHardware() {
      return hardware;
   }

   /**
    * @param hardware the hardware to set
    */
   public void setHardware(String hardware) {
      this.hardware = hardware;
   }

   /**
    * @param soptAddress
    */
   public void setSoptAddress(long soptAddress) {
      this.soptAddress = soptAddress;
   }
   /**
    * @return
    */
   public long getSoptAddress() {
      return soptAddress;
   }

   /**
    * @return
    */
   public ClockTypes getClockType() {
      return clockType;
   }

   /**
    * @param clockType
    */
   public void setClockType(ClockTypes clockType) {
      this.clockType = clockType;
   }

   /**
    * @return
    */
   public int getClockAddres() {
      return clockAddres;
   }

   /**
    * @param clockAddres
    */
   public void setClockAddres(int clockAddres) {
      this.clockAddres = clockAddres;
   }

   /**
    * @return
    */
   public int getClockNvAddress() {
      return clockNvAddress;
   }

   /**
    * @param clockNvAddress
    */
   public void setClockNvAddress(int clockNvAddress) {
      this.clockNvAddress = clockNvAddress;
   }

   /**
    * @return
    */
   public int getClockTrimFrequency() {
      return clockTrimFrequency;
   }

   /**
    * @param clockTrimFrequency
    */
   public void setClockTrimFrequency(int clockTrimFrequency) {
      this.clockTrimFrequency = clockTrimFrequency;
   }

   /**
    * @return
    */
   public boolean isDefaultDevice() {
      return defaultDevice;
   }

   /**
    * @return
    */
   public TargetType getTargetType() {
      return targetType;
   }

   /**
    * @return
    */
   public FileList getFileListMap() {
      return fileList;
   }

   /**
    * @param fileList
    */
   public void addToFileList(FileList fileList) {
      if (getFileListMap() == null) {
         // New map
         this.fileList = new FileList();
      }
      this.fileList.add(fileList);
   }

   /**
    * @param hidden
    */
   public void setHidden(boolean hidden) {
      this.hidden = hidden;
  }

   /**
    * @return
    */
   public boolean isHidden() {
      return hidden;
  }

   /**
    * Creates the package list that applies to this device
    * 
    * @param variableMap Variables to use when evaluation conditions
    * 
    * @return ArrayList of ProjectActionLists (an empty list if none)
    */
   public ProjectActionList getProjectActionList(Map<String, String> variableMap) {
      return PackageParser.getDevicePackageList(this, variableMap);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#clone()
    */
   @Override
   protected Object clone() throws CloneNotSupportedException {
      return super.clone();
   }

   public static Device shallowCopy(String name, Device aliasedDevice) throws CloneNotSupportedException {
      Device clone = (Device)aliasedDevice.clone();
      clone.name = name;
      return clone;
   }

   /**
    * Add erase method
    * 
    * @param method Method to add
    */
   public void addEraseMethod(String method) {
      eraseMethods.add(EraseMethod.valueOf(method));
   }
   
   /**
    * Add erase method
    * 
    * @param method Method to add
    */
   public void addEraseMethod(EraseMethod method) {
      eraseMethods.add(method);
   }
   
   /**
    * Get erase methods available for this memory
    * 
    * @return List of erase methods
    */
   public List<EraseMethod> getEraseMethods() {
      return eraseMethods;
   }

}
