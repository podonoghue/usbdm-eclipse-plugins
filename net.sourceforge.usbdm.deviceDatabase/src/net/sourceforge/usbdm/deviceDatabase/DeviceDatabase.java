package net.sourceforge.usbdm.deviceDatabase;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sourceforge.usbdm.deviceDatabase.Device.ClockTypes;
import net.sourceforge.usbdm.deviceDatabase.Device.FileInfo;
import net.sourceforge.usbdm.deviceDatabase.Device.FileList;
import net.sourceforge.usbdm.deviceDatabase.Device.GnuInfo;
import net.sourceforge.usbdm.deviceDatabase.Device.GnuInfoList;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryRegion;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryRegion.MemoryRange;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryType;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.jni.UsbdmJniConstants;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class DeviceDatabase {

   private ArrayList<Device> deviceList;       // List of all devices
   private Device            defaultDevice;    // Default device

   private Document          dom;
   private boolean           valid = false;
   private TargetType        targetType;
   
   class SharedInformationMap extends HashMap<String, Object> {
      private static final long serialVersionUID = 1192713020204077765L;

      public MemoryRegion findSharedMemory(String key) throws Throwable {
         Object item = get(key);
         if ((item == null) || !(item instanceof MemoryRegion)) {
            throw new Throwable("findSharedMemory(): Item not found or incorrect type" + key);
         }
         return (MemoryRegion) item;
      }
      
      public GnuInfoList findSharedGnuInfoMap(String key) throws Throwable {
         Object item = get(key);
         if ((item == null) || !(item instanceof GnuInfoList)) {
            throw new Throwable("findSharedGnuInfoMap(): Item not found or incorrect type" + key);
         }
         return (GnuInfoList) item;
      }

      public FileList findSharedFileList(String key) throws Throwable {
         Object item = get(key);
         if ((item == null) || !(item instanceof FileList)) {
            throw new Throwable("findSharedFileList(): Item not found or incorrect type" + key);
         }
         return (FileList) item;
      }
   }
   
   SharedInformationMap sharedInformation;
   
   /**
    * Parse a <device> element
    * 
    * @param    deviceElement <device> element
    * 
    * @return   Device described 
    */
   private Device parseDevice(Element deviceElement) {
      String name = deviceElement.getAttribute("name");
      
      //  Create a new Device with the value read from the xml nodes
      Device device = new Device(targetType, name);

      if (deviceElement.hasAttribute("isDefault")) {
         device.setDefault();
      }
      if (deviceElement.hasAttribute("alias")) {
         String alias = deviceElement.getAttribute("alias");
         device.setAlias(alias);
      }
      if (deviceElement.hasAttribute("family")) {
         String family = deviceElement.getAttribute("family");
         device.setFamily(family);
      }
      try {
         for (Node node = deviceElement.getFirstChild();
               node != null;
               node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
               continue;
            }
            Element element = (Element) node;
            if (element.getTagName() == "clock") {
               device.setClockAddres(Integer.decode(element.getAttribute("registerAddress")));
               device.setClockType(ClockTypes.parse(element.getAttribute("type")));
               device.setClockNvAddress(device.getDefaultClockTrimNVAddress());
               device.setClockTrimFrequency(device.getDefaultClockTrimFreq());
            }
            else if (element.getTagName() == "memory") {
               MemoryRegion memoryRegion = parseMemoryElements(element);
               device.addMemoryRegion(memoryRegion);
            }
            else if (element.getTagName() == "memoryRef") {
               String key = element.getAttribute("ref"); 
               MemoryRegion memoryRegion = sharedInformation.findSharedMemory(key);
               device.addMemoryRegion(memoryRegion);
            }
            else if (element.getTagName() == "gnuInfoList") {
               GnuInfoList gnuInfoMap = parseGnuInfoListElements(element);
               device.setGnuInfoMap(gnuInfoMap);
            }
            else if (element.getTagName() == "gnuInfoListRef") {
               String key = element.getAttribute("ref"); 
               GnuInfoList gnuInfoMap = sharedInformation.findSharedGnuInfoMap(key);
               device.setGnuInfoMap(gnuInfoMap);
            }
            else if (element.getTagName() == "fileList") {
               FileList fileMap = parseFileListElements(element);
               device.setFileListMap(fileMap);
            }
            else if (element.getTagName() == "fileListRef") {
               String key = element.getAttribute("ref"); 
               FileList fileMap = sharedInformation.findSharedFileList(key);
               device.setFileListMap(fileMap);
            }
            else if (element.getTagName() == "soptAddress") {
               long soptAddress = getIntAttribute(element, "value");
               device.setSoptAddress(soptAddress);
//               System.err.println(String.format("SOPTAddress=0x%X",soptAddress));
            }
         }
      } catch (Throwable e) {
         e.printStackTrace();
         return null;
      }
//      System.err.println("parseDevice(): " + device.toString());
      return device;
   }

   private long getIntAttribute(Element element, String name) throws Exception {
      String s = element.getAttribute(name);
      if ((s == null) || (s.length()==0)) {
         throw new Exception("Attribute \'"+name+"\'not found");
      }
      long value      = 0;
      long multiplier = 1;
      int kIndex = s.lastIndexOf('K');
      int mIndex = s.lastIndexOf('M');
      if (kIndex>0) {
//         System.err.println("getIntAttribute("+s+"), K found");
         s = s.substring(0, kIndex);
         multiplier = 1024;
//         System.err.println("getIntAttribute("+s+"), K found");
      }
      if (mIndex>0) {
//         System.err.println("getIntAttribute("+s+"), M found");
         s = s.substring(0, mIndex);
         multiplier = 1024*1024;
//         System.err.println("getIntAttribute("+s+"), M found");
      }
      try {
         value = multiplier*Long.decode(s);
      } catch (NumberFormatException e) {
//         System.err.println("getIntAttribute("+s+"), failed");
         e.printStackTrace();
         throw new Exception("Failed to parse Int Attribute \'"+name+"\', value = \'"+element.getAttribute(name)+"\'");
//         throw e;
      }
      return value;
   }

   private MemoryRange parseMemoryRange(Element element) throws Throwable {
      
      boolean startGiven  = element.hasAttribute("start");
      boolean middleGiven = element.hasAttribute("middle");
      boolean endGiven    = element.hasAttribute("end");
      boolean sizeGiven   = element.hasAttribute("size");


      long memoryStartAddress  = 0;
      long memoryMiddleAddress = 0;
      long memoryEndAddress    = 0;
      long memorySize          = 0;
      boolean addressOK = true;
      try {
         if (startGiven) {
            memoryStartAddress  = getIntAttribute(element, "start");
         }
         if (middleGiven) {
            memoryMiddleAddress  = getIntAttribute(element, "middle");
         }
         if (endGiven) {
            memoryEndAddress  = getIntAttribute(element, "end");
         }
         if (sizeGiven) {
            memorySize  = getIntAttribute(element, "size");
         }

         if (startGiven && endGiven) {
            addressOK           = !middleGiven && !sizeGiven && (memoryStartAddress <= memoryEndAddress);
         }
         else if (startGiven && sizeGiven) {
            addressOK           = !middleGiven && !endGiven && (memorySize > 0);
            memoryEndAddress    = memoryStartAddress + memorySize - 1;
         }
         else if (middleGiven && sizeGiven) {
            addressOK           = (memorySize >= 2) && ((memorySize & 0x01) == 0);
            memoryStartAddress  = memoryMiddleAddress - (memorySize/2);
            memoryEndAddress    = memoryMiddleAddress + (memorySize/2)-1;
         }
         else if (endGiven && sizeGiven) {
            addressOK           = !startGiven && !middleGiven && (memorySize > 0);
            memoryStartAddress = memoryEndAddress - (memorySize - 1);
         }
      } catch (Exception e) {
         e.printStackTrace();
         addressOK = false;
      }
      if (!addressOK) {
         System.err.println("DeviceDatabase.parseMemoryRange() startGiven="+startGiven+" middleGiven="+middleGiven+" endGiven="+endGiven+" sizeGiven="+sizeGiven);
         System.err.println("DeviceDatabase.parseMemoryRange() memoryStartAddress="+memoryStartAddress+" memoryMiddleAddress="+
                             memoryMiddleAddress+" memoryEndAddress="+memoryEndAddress+" memorySize="+memorySize);
         throw new Throwable("Illegal memory start/middle/end/size in memory region list");
      }
      return new MemoryRange(memoryStartAddress, memoryEndAddress);
   }
   
   /**
    * Parse a <memory> element
    * 
    * @param    memoryElement <memory> element
    * 
    * @return   Memory region described 
    * @throws Throwable 
    */
   private MemoryRegion parseMemoryElements(Element memoryElement) throws Throwable {

      // <memory>
      String sMemoryType = memoryElement.getAttribute("type");
      MemoryType memoryType = MemoryType.getTypeFromXML(sMemoryType);
      if (memoryType == null) {
         throw new Throwable("DeviceXmlParser::parseDeviceXML() - invalid memory range type \""+sMemoryType+"\"");
      }
      MemoryRegion memoryRegion = new MemoryRegion(memoryType);

      for (Node node = memoryElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         // element node for <device>
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         //         System.err.println("parseMemoryElements() " + node.getNodeName());
         Element element = (Element) node;
         if (element.getTagName() == "memoryRange") {
            MemoryRange memoryRange = parseMemoryRange(element);
            //               System.err.println("parseMemoryElements() " + memoryRange.toString());
            memoryRegion.addRange(memoryRange);
         }
         // Ignore other node types <flexNVMInfoRef> etc. 
      }
      return memoryRegion;
   }
   
   /**
    * Parse a <GnuInfoMap> element
    * 
    * @param    gnuInfoListElement <gnuInfoList> element
    * 
    * @return   GNU information described 
    * @throws Throwable 
    */
   private Device.GnuInfoList parseGnuInfoListElements(Element gnuInfoListElement) throws Throwable {

      Device.GnuInfoList list = new Device.GnuInfoList();
      
      // <GnuInfoMap>
      for (Node node = gnuInfoListElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         // element node for <gnuInfo>
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
//         System.err.println("parseGnuInfoListElements() " + node.getNodeName());
         Element element = (Element) node;
         if (element.getTagName() != "gnuInfo") {
            throw new Throwable("Unexpected element \""+element.getTagName()+"\" in GnuInfoMap");
         }
         String id       = element.getAttribute("id");
         String value    = element.getAttribute("value");
         String path     = element.getAttribute("path");
         String name     = element.getAttribute("name");
         String command  = element.getAttribute("command");
         String text     = element.getTextContent();
         GnuInfo gnuInfo = new Device.GnuInfo(id, value, path, name, command, text);
         list.put(gnuInfo);
      }
      return list;
   }
   
   /**
    * Parse a <GnuInfoMap> element
    * 
    * @param    fileListElement <fileList> element
    * 
    * @return   File list described 
    * @throws Throwable 
    */
   private Device.FileList parseFileListElements(Element fileListElement) throws Throwable {

      Device.FileList list = new Device.FileList();
      
      // <GnuInfoMap>
      for (Node node = fileListElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         // element node for <gnuInfo>
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
//         System.err.println("parseFileListElements() " + node.getNodeName());
         Element element = (Element) node;
         if (element.getTagName() != "file") {
            throw new Throwable("Unexpected element \""+element.getTagName()+"\" in fileList");
         }
         String id        = element.getAttribute("id");
         String source    = element.getAttribute("source");
         String target    = element.getAttribute("target");
         FileInfo gnuInfo = new Device.FileInfo(id, source, target);
         list.put(gnuInfo);
      }
      return list;
   }
   
   /**
    * Parse a <sharedInformation> element into database
    * 
    * @param    memoryElement <sharedInformation> element
    * 
    * @return   Memory region described 
    */
   private boolean parseSharedInformationElements(Element sharedInformationElement) {
      try {
         for (Node node = sharedInformationElement.getFirstChild();
              node != null;
              node = node.getNextSibling()) {
            // element node for <device>
            if (node.getNodeType() != Node.ELEMENT_NODE) {
               continue;
            }
//            System.err.println("parseSharedInformationElements() child = " + node.getNodeName());
            Element element = (Element) node;
            if (element.getTagName() == "memory") {
               MemoryRegion memoryRegion = parseMemoryElements(element);
               String key = element.getAttribute("id");
               if (key == null) {
                  throw new Throwable("parseSharedInformationElements() - null key");
               }
               sharedInformation.put(key, memoryRegion);
//               System.err.println("parseSharedInformationElements() Memory:" + memoryRegion.toString());
            }
            if (element.getTagName() == "gnuInfoList") {
               GnuInfoList gnuInfoList = parseGnuInfoListElements(element);
               String key = element.getAttribute("id");
               if (key == null) {
                  throw new Throwable("parseSharedInformationElements() - null key");
               }
               sharedInformation.put(key, gnuInfoList);
//               System.err.println("parseSharedInformationElements() GnuInfoMap:" + key);
            }
            if (element.getTagName() == "fileList") {
               FileList fileList = parseFileListElements(element);
               String key = element.getAttribute("id");
               if (key == null) {
                  throw new Throwable("parseSharedInformationElements() - null key");
               }
               sharedInformation.put(key, fileList);
//               System.err.println("parseSharedInformationElements() GnuInfoMap:" + key);
            }
            // Ignore other node types <flashProgram> <tclScript> <securityEntry> <flexNVMInfo> 
         }
         return true;
      } catch (Throwable e) {
         e.printStackTrace();
         return false;
      }
   }

   /**
    * Parse device list into database
    * 
    * @param deviceListElement <deviceList> element 
    * 
    * @return success/failure
    */
   private boolean parseDeviceList(Element deviceListElement) {
      for (Node node = deviceListElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         // element node for <device>
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() != "device") {
            System.err.println("parseDeviceList(" + node.getNodeName() + ") - not device element");
            return false;
         }
         // Get the Device object
         Device device = parseDevice(element);
         if (device == null) {
            return false;
         }
         // Add it to list
         if (device.isDefault()) {
            defaultDevice = device;
         }
         else {
            deviceList.add(device);
         }
      }
      return true;
   }
   
   /**
    * 
    * @return
    */
   private boolean parseXmlFile(Path databasePath) {
      boolean valid = true;
      // Get the factory
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      
      try {
         //  Using factory get an instance of document builder
         DocumentBuilder db = dbf.newDocumentBuilder();
         
         //  Parse using builder to get DOM representation of the XML file
         dom = db.parse(databasePath.toOSString());
      }catch(ParserConfigurationException pce) {
         pce.printStackTrace();
         valid = false;
      }catch(SAXException se) {
         se.printStackTrace();
         valid = false;
      }catch(IOException ioe) {
         ioe.printStackTrace();
         valid = false;
      }
      return valid;
   }
  
   /**
    * 
    * @return
    * @throws Throwable 
    */
   private boolean parseDocument() {
      boolean valid = true;
      // Get the root element
      Element documentElement = dom.getDocumentElement();

      if (documentElement == null) {
         System.err.println("DeviceDatabase.parseDocument() - failed to get documentElement");
         return false;
      }
      for (Node node = documentElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
//         System.err.println("parseDocument() child = " + node.getNodeName());
         Element element = (Element) node;
         if (element.getTagName() == "sharedInformation") {
            if (!parseSharedInformationElements(element)) {
               valid = false;
            }
         }
         else if (element.getTagName() == "deviceList") {
            if (!parseDeviceList(element)) {
               valid = false;
            }
         }
      }
      return valid;
   }

   /**
    * Get device iterator
    * 
    * @return iterator for devices
    */
   public Iterator<Device> iterator() {
      return deviceList.iterator();
   }

   /**
    * List devices in database
    * 
    */
   public void listDevices(PrintStream stream) {
      stream.println("No of Devices '" + deviceList.size() + "'.");

      if (defaultDevice == null) {
         stream.println("No default device set");
      }
      else {
         stream.println("Default device = " + defaultDevice.toString());
      }
      Iterator<Device> it = deviceList.iterator();
      while(it.hasNext()) {
         Device device = it.next();
         if (!device.isAlias()) {
            stream.println(device.toString());
         }
      }
   }
  
  /**
   * List devices in XML format
   * 
   */
 public void toXML(PrintStream xmlOut) {
    toXML(xmlOut, false);
  }
 
 public void toXML(PrintStream xmlOut, boolean listAliasDevices) {
    Iterator<Device> it = deviceList.iterator();
    while(it.hasNext()) {
       Device device = it.next();
       if (listAliasDevices || !device.isAlias()) {
          device.toXML(xmlOut, 0);
       }
    }
 }

 public void toOptionXML(PrintStream xmlOut) {
    HashMap<String, GnuInfo> optionMap = new HashMap<String, GnuInfo>(); 
    Iterator<Device> it = deviceList.iterator();
    while(it.hasNext()) {
       Device device = it.next();
       if (!device.isAlias()) {
          device.addOptionXML(optionMap);
       }
    }
    Iterator<Entry<String, GnuInfo>> optionIt = optionMap.entrySet().iterator();
    while (optionIt.hasNext()) {
       Map.Entry<String, GnuInfo> pairs = (Map.Entry<String, GnuInfo>)optionIt.next();
       pairs.getValue().toOptionXML(xmlOut);
   }
 }
 
  /**
   * Indicates if database is valid (loaded from XML correctly)
   *  
   * @return true/false
   */
   public boolean isValid() {
      return valid;
   }

   /*
   T_HCS12     (0),   //!< HC12 or HCS12 target
   T_HCS08     (1),   //!< HCS08 target
   T_RS08      (2),   //!< RS08 target
   T_CFV1      (3),   //!< Coldfire Version 1 target
   T_CFVx      (4),   //!< Coldfire Version 2,3,4 target
   T_JTAG      (5),   //!< JTAG target - TAP is set to \b RUN-TEST/IDLE
   T_EZFLASH   (6),   //!< EzPort Flash interface (SPI?)
   T_MC56F80xx (7),   //!< JTAG target with MC56F80xx optimised subroutines
   T_ARM_JTAG  (8),   //!< ARM target using JTAG
   T_ARM_SWD   (9),   //!< ARM target using SWD
   T_ARM       (10),  //!< ARM target using either SWD (preferred) or JTAG as supported
   T_OFF       (0xFF),
*/
  
   Path getXmlFilepath(TargetType targetType) throws Exception {
      String filename;
      switch (targetType) {
      case T_ARM:
      case T_ARM_JTAG:
      case T_ARM_SWD:
         filename = UsbdmJniConstants.ARM_DEVICE_FILE;
         break;
      case T_CFV1:
         filename = UsbdmJniConstants.CFV1_DEVICE_FILE;
         break;
      case T_CFVx:
         filename = UsbdmJniConstants.CFVX_DEVICE_FILE;
         break;
      default:
         throw new UsbdmException("Device file not found");
      };
      IPath usbdmResourcePath = Usbdm.getResourcePath();
      if (usbdmResourcePath == null) {
         return null;
      }
     usbdmResourcePath = usbdmResourcePath.append("/DeviceData/").append(filename);
     System.err.println("DeviceDatabase.getXmlFilepath(): XML file path = " + usbdmResourcePath);
     
     return (Path) usbdmResourcePath;
   }
   
   /**
    *  Constructor
    * 
    *  @param xmlFilename device database file name e.g. arm_devices.xml
    * @throws Exception 
    */
   public DeviceDatabase(TargetType targetType) {
            
      this.targetType   = targetType;
      
      deviceList        = new ArrayList<Device>();
      sharedInformation = new SharedInformationMap();
      
      try {
         Path databasePath = getXmlFilepath(targetType);
         // Parse the xml file
         if (!parseXmlFile(databasePath)) {
            return;
         }
         //  Process XML contents and generate Device list
         if (!parseDocument()) {
            return;
         }
         valid = true;
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   public TargetType getTargetType() {
      return targetType;
   }

   public Device getDevice(String name) {
      for (int index = 0; index<deviceList.size(); index++) {
         Device device = deviceList.get(index);
         if (device.getName().equals(name)) {
            return device;
         }
      }
      return null;
   }
   
   public Device getDefaultDevice() {
      return defaultDevice;
   }

}
