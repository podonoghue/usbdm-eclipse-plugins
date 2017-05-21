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
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sourceforge.usbdm.jni.Usbdm.EraseMethod;
import net.sourceforge.usbdm.jni.Usbdm.EraseMethods;
import net.sourceforge.usbdm.jni.Usbdm.ResetMethod;
import net.sourceforge.usbdm.jni.Usbdm.ResetMethods;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.packageParser.FileList;
import net.sourceforge.usbdm.packageParser.PackageParser;
import net.sourceforge.usbdm.packageParser.ProjectActionList;

public class Device implements Cloneable {

   private String                   fName;
   private boolean                  fDefaultDevice;
   private String                   fAlias;
   private Vector<MemoryRegion>     fMemoryRegions;
   private FileList                 fFileList;
   private String                   fFamily;
   private String                   fSubFamily;
   private String                   fHardware;
   private long                     fSoptAddress;
   private TargetType               fTargetType;
   private ClockTypes               fClockType;
   private int                      fClockAddres;
   private int                      fClockNvAddress;
   private int                      fClockTrimFrequency;
   private boolean                  fHidden;
   
   /** Erase methods available */
   private EraseMethods             fEraseMethods;

   /** Available reset methods */
   private ResetMethods             fResetMethods;
   
   /**
    * Constructor
    * 
    * @param targetType Type of device
    * @param name       Name of device
    */
   public Device(TargetType targetType, String name) {
      fName         = name;
      fTargetType   = targetType;
      
      fEraseMethods = new EraseMethods();
      fResetMethods = new ResetMethods();
      
      setClockType(ClockTypes.INVALID);
      setClockAddres(0);
      
      fMemoryRegions      = new Vector<MemoryRegion>();
      fFamily             = null;
      fSubFamily          = null;
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
      return getDefaultClockTrimNVAddress(fTargetType, fClockType);
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
      return getDefaultClockTrimFreq(fClockType);
   }

   /**
    * Adds a memeory region to the device
    * 
    * @param memoryRegion
    */
   void addMemoryRegion(MemoryRegion memoryRegion) {
      fMemoryRegions.add(memoryRegion);
   }

   /**
    * Provides an iterator over the devices memory regions
    * 
    * @return
    */
   public Iterator<MemoryRegion> getMemoryRegionIterator() {
      return fMemoryRegions.iterator();
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      String rv = String.format("%-12s", fName+",");
      if (fSoptAddress != 0) {
         rv += String.format(",SOPT=0x%X", fSoptAddress);
      }
      rv += fMemoryRegions.toString();
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
      fDefaultDevice = true;
   }
   /**
    * Indicates if this device is the default
    * 
    * @return
    */
   public boolean isDefault() {
      return fDefaultDevice;
   }
   /**
    * @param name
    */
   public void setAlias(String name) {
      fAlias = name;
   }
   /**
    * @return
    */
   public boolean isAlias() {
      return fAlias != null;
   }
   /**
    * @return
    */
   public String getAlias() {
      return fAlias;
   }
   /**
    * @return
    */
   public String getName() {
      return fName;
   }
   /**
    * @param family
    */
   public void setFamily(String family) {
      this.fFamily = family;
   }
   /**
    * @return
    */
   public String getFamily() {
      return fFamily;
   }
   /**
    * @param subFamily
    */
   public void setSubFamily(String subFamily) {
      this.fSubFamily = subFamily;
   }
   /**
    * @return
    */
   public String getSubFamily() {
      return fSubFamily;
   }
   
   /**
    * @return the hardware
    */
   public String getHardware() {
      return fHardware;
   }

   /**
    * @param hardware the hardware to set
    */
   public void setHardware(String hardware) {
      this.fHardware = hardware;
   }

   /**
    * @param soptAddress
    */
   public void setSoptAddress(long soptAddress) {
      this.fSoptAddress = soptAddress;
   }
   /**
    * @return
    */
   public long getSoptAddress() {
      return fSoptAddress;
   }

   /**
    * @return
    */
   public ClockTypes getClockType() {
      return fClockType;
   }

   /**
    * @param clockType
    */
   public void setClockType(ClockTypes clockType) {
      this.fClockType = clockType;
   }

   /**
    * @return
    */
   public int getClockAddres() {
      return fClockAddres;
   }

   /**
    * @param clockAddres
    */
   public void setClockAddres(int clockAddres) {
      this.fClockAddres = clockAddres;
   }

   /**
    * @return
    */
   public int getClockNvAddress() {
      return fClockNvAddress;
   }

   /**
    * @param clockNvAddress
    */
   public void setClockNvAddress(int clockNvAddress) {
      this.fClockNvAddress = clockNvAddress;
   }

   /**
    * @return
    */
   public int getClockTrimFrequency() {
      return fClockTrimFrequency;
   }

   /**
    * @param clockTrimFrequency
    */
   public void setClockTrimFrequency(int clockTrimFrequency) {
      this.fClockTrimFrequency = clockTrimFrequency;
   }

   /**
    * @return
    */
   public boolean isDefaultDevice() {
      return fDefaultDevice;
   }

   /**
    * @return
    */
   public TargetType getTargetType() {
      return fTargetType;
   }

   /**
    * @return
    */
   public FileList getFileListMap() {
      return fFileList;
   }

   /**
    * @param fileList
    */
   public void addToFileList(FileList fileList) {
      if (getFileListMap() == null) {
         // New map
         this.fFileList = new FileList();
      }
      this.fFileList.add(fileList);
   }

   /**
    * @param hidden
    */
   public void setHidden(boolean hidden) {
      this.fHidden = hidden;
  }

   /**
    * @return
    */
   public boolean isHidden() {
      return fHidden;
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
      clone.fName = name;
      return clone;
   }

   /**
    * **************************************************
    */
   /**
    * Add reset method
    * 
    * @param method Method to add
    */
   public void setResetMethod(ResetMethods methods) {
      fResetMethods = methods;
   }
   
   /**
    * Get reset methods available
    * 
    * @return List of erase methods
    */
   public ResetMethods getResetMethods() {
      return fResetMethods;
   }

   /**
    * Get preferred reset method
    * 
    * @return method Preferred method
    */
   public ResetMethod getPreferredResetMethod() {
      return fResetMethods.getPreferredMethod();
   }

   /**
    * Set preferred reset method
    * 
    * @param method Preferred method
    */
   public void setPreferredResetMethod(ResetMethod method) {
      fResetMethods.setPreferredMethod(method);
   }

   /**
    * **************************************************
    */
   /**
    * Add erase method
    * 
    * @param method Method to add
    */
   public void setEraseMethod(EraseMethods methods) {
      fEraseMethods = methods;
   }
   
   /**
    * Get erase methods available for this memory
    * 
    * @return List of erase methods
    */
   public EraseMethods getEraseMethods() {
      return fEraseMethods;
   }

   /**
    * Get preferred erase method
    * 
    * @return method Preferred method
    */
   public EraseMethod getPreferredEraseMethod() {
      return fEraseMethods.getPreferredMethod();
   }

   /**
    * Set preferred erase method
    * 
    * @param method Preferred method
    */
   public void setPreferredEraseMethod(EraseMethod method) {
      fEraseMethods.setPreferredMethod(method);
   }

}
