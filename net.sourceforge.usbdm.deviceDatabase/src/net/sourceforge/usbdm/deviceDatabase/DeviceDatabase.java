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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.custom.BusyIndicator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.usbdm.deviceDatabase.MemoryRegion.MemoryRange;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.jni.UsbdmJniConstants;
import net.sourceforge.usbdm.packageParser.FileList;
import net.sourceforge.usbdm.packageParser.ProjectActionList;

public class DeviceDatabase {

   private ArrayList<Device> deviceList;       // List of all devices (including aliases)
   private Device            defaultDevice;    // Default device

   private Document          dom;
   private boolean           valid = false;
   private TargetType        fTargetType;
   
   class SharedInformationMap extends HashMap<String, Object> {
      private static final long serialVersionUID = 1192713020204077765L;

      public MemoryRegion findSharedMemory(String key) throws Exception {
         Object item = get(key);
         if ((item == null) || !(item instanceof MemoryRegion)) {
            throw new Exception("findSharedMemory(): Item not found or incorrect type \'" + key + "\'");
         }
         return (MemoryRegion) item;
      }

      public FileList findSharedFileList(String key) throws Exception {
         Object item = get(key);
         if ((item == null) || !(item instanceof FileList)) {
            throw new Exception("findSharedFileList(): Item not found or incorrect type \'" + key + "\'");
         }
         return (FileList) item;
      }

      public ProjectActionList findSharedActionList(String key) throws Exception {
         Object item = get(key);
         if ((item == null) || !(item instanceof ProjectActionList)) {
            throw new Exception("findSharedFileList(): Item not found or incorrect type \'" + key + "\'");
         }
         return (ProjectActionList) item;
      }
   }
   
   SharedInformationMap sharedInformation;
   
   private long getIntAttribute(Element element, String name) throws Exception {
      String s = element.getAttribute(name);
      if ((s == null) || (s.length()==0)) {
         throw new Exception("Attribute \'"+name+"\'not found");
      }
      long value      = 0;
      long multiplier = 1;
      long bias       = 0;
      
      int wIndex = s.indexOf("w:");
      if (wIndex>=0) {
//       System.err.println("getIntAttribute("+s+"), K found");
       s = s.substring(wIndex+2);
       multiplier = 2;
       bias       = 1;
//       System.err.println("getIntAttribute("+s+"), K found");
    }
      wIndex = s.indexOf("W:");
      if (wIndex>=0) {
//       System.err.println("getIntAttribute("+s+"), K found");
       s = s.substring(wIndex+2);
       multiplier = 2;
       bias       = 1;
//       System.err.println("getIntAttribute("+s+"), K found");
    }

      int kIndex = s.lastIndexOf('K');
      int mIndex = s.lastIndexOf('M');
      if (kIndex>0) {
//       System.err.println("getIntAttribute("+s+"), K found");
       s = s.substring(0, kIndex);
       multiplier *= 1024;
       bias       *= 1024;
//       System.err.println("getIntAttribute("+s+"), K found");
    }
      if (mIndex>0) {
//         System.err.println("getIntAttribute("+s+"), M found");
         s = s.substring(0, mIndex);
         multiplier *= 1024*1024;
         bias       *= 1024*1024;
//         System.err.println("getIntAttribute("+s+"), M found");
      }
      try {
         value = Long.decode(s);
         if ((value&1) == 0) {
            bias = 0;
         }
         value = multiplier*Long.decode(s) + bias;
      } catch (NumberFormatException e) {
         throw new Exception("Failed to parse Int Attribute \'"+name+"\', value = \'"+element.getAttribute(name)+"\'");
      }
      return value;
   }

   private MemoryRange parseMemoryRange(Element element) throws Exception {
      
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
         addressOK = false;
      }
      if (!addressOK) {
         System.err.println("DeviceDatabase.parseMemoryRange() startGiven="+startGiven+" middleGiven="+middleGiven+" endGiven="+endGiven+" sizeGiven="+sizeGiven);
         System.err.println("DeviceDatabase.parseMemoryRange() memoryStartAddress="+memoryStartAddress+" memoryMiddleAddress="+
                             memoryMiddleAddress+" memoryEndAddress="+memoryEndAddress+" memorySize="+memorySize);
         throw new Exception("Illegal memory start/middle/end/size in memory region list");
      }
      MemoryRange memoryRange = new MemoryRange(memoryStartAddress, memoryEndAddress);
      // <name>
      String sName = element.getAttribute("name");
      if ((sName != null) && (sName.length()>0)) {
         memoryRange.setName(sName);
      }
      return memoryRange;
   }
   
   /**
    * Parse a <memory> element
    * 
    * @param    memoryElement <memory> element
    * 
    * @return   Memory region described 
    * @throws Exception 
    */
   private MemoryRegion parseMemoryElements(Element memoryElement) throws Exception {

      // <type>
      String sMemoryType = memoryElement.getAttribute("type");
      MemoryType memoryType = MemoryType.getTypeFromXML(sMemoryType);
      if (memoryType == null) {
         throw new Exception("DeviceXmlParser::parseDeviceXML() - invalid memory range type \""+sMemoryType+"\"");
      }
      MemoryRegion memoryRegion = new MemoryRegion(memoryType);

      // <name>
      String sName = memoryElement.getAttribute("name");
      if ((sName != null) && (sName.length()>0)) {
         memoryRegion.setName(sName);
      }
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
    * Parse a <device> element
    * 
    * @param    deviceElement <device> element
    * 
    * @return   Device described 
    * @throws Exception 
    */
   private Device parseDevice(Element deviceElement) throws Exception {
      String name = deviceElement.getAttribute("name");

      //  Create a new Device with the value read from the XML nodes
      Device device = new Device(fTargetType, name);

      if (deviceElement.hasAttribute("alias")) {
         String aliasName = deviceElement.getAttribute("alias");
         // Make shallow copy from aliased device
         Device aliasedDevice = getDevice(aliasName);
         if (aliasedDevice == null) {
            throw new Exception("Aliased device not found "+ aliasName);
         }
         device = Device.shallowCopy(name, aliasedDevice);
         device.setAlias(aliasName);
      }
      if (deviceElement.hasAttribute("isDefault")) {
         device.setDefault();
      }
      if (deviceElement.hasAttribute("hidden")) {
         device.setHidden(deviceElement.getAttribute("hidden").equalsIgnoreCase("true"));
      }
      if (deviceElement.hasAttribute("family")) {
         device.setFamily(deviceElement.getAttribute("family"));
      }
      if (deviceElement.hasAttribute("subfamily")) {
         device.setSubFamily(deviceElement.getAttribute("subfamily"));
      }
      if (deviceElement.hasAttribute("hardware")) {
         device.setHardware(deviceElement.getAttribute("hardware"));
      }
      for (Node node = deviceElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if ((element.getTagName() == "sdid") || (element.getTagName() == "sdidmask")) {
            // Ignored but OK for aliased device
         }
         else if (device.isAlias()) {
            throw new Exception("Aliased devices may not have full description");
         }
         else if (element.getTagName() == "clock") {
            device.setClockAddres(Integer.decode(element.getAttribute("registerAddress")));
            device.setClockType(ClockTypes.parse(element.getAttribute("type")));
            device.setClockNvAddress(device.getDefaultClockTrimNVAddress());
            device.setClockTrimFrequency(device.getDefaultClockTrimFreq());
         }
         else if (element.getTagName() == "memory") {
            device.addMemoryRegion(parseMemoryElements(element));
         }
         else if (element.getTagName() == "memoryRef") {
            device.addMemoryRegion(sharedInformation.findSharedMemory(element.getAttribute("ref")));
         }
         else if (element.getTagName() == "projectActionList") {
            throw new Exception("<projectActionList> is no longer supported in device database");
//            device.addToActionList(parseProjectActionListElement(element));
         }
         else if (element.getTagName() == "projectActionListRef") {
            throw new Exception("<projectActionListRef> no longer supported");
         }
         else if (element.getTagName() == "soptAddress") {
            device.setSoptAddress(getIntAttribute(element, "value"));
         }
      }
      return device;
   }

   /**
    * Parse a <sharedInformation> element into database
    * 
    * @param    sharedInformationElement <sharedInformation> element
    * 
    * @return   true success
    * @throws Exception 
    */
   private void parseSharedInformationElements(Element sharedInformationElement) throws Exception {
      for (Node node = sharedInformationElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "memory") {
            MemoryRegion memoryRegion = parseMemoryElements(element);
            String key = element.getAttribute("id");
            if (key == null) {
               throw new Exception("parseSharedInformationElements() - null key");
            }
            sharedInformation.put(key, memoryRegion);
         }
         else if (element.getTagName() == "projectActionList") {
            throw new Exception("<projectActionList> is no longer supported in device database");
//            ProjectActionList actionList = parseProjectActionListElement(element);
//            String key = element.getAttribute("id");
//            if (key == null) {
//               throw new Exception("parseSharedInformationElements() - null key");
//            }
//            sharedInformation.put(key, actionList);
         }
         // Ignore other node types <flashProgram> <tclScript> <securityEntry> <flexNVMInfo> 
      }
   }

   /**
    * Parse device list into database
    * 
    * @param deviceListElement <deviceList> element 
    * 
    * @return success/failure
    * @throws Exception 
    */
   private void parseDeviceList(Element deviceListElement) throws Exception {
      for (Node node = deviceListElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         // element node for <device>
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() != "device") {
            throw new Exception("parseDeviceList(" + node.getNodeName() + ") - not device element");
         }
         // Get the Device object
         Device device = parseDevice(element);
         // Add it to list
         if (device.isDefault()) {
            defaultDevice = device;
         }
         else {
            deviceList.add(device);
         }
      }
   }
   
   /**
    * 
    * @return
    * @throws Exception 
    */
   private void parseDocument() throws Exception {

      // Get the root element
      Element documentElement = dom.getDocumentElement();

      if (documentElement == null) {
         throw new Exception("DeviceDatabase.parseDocument() - failed to get documentElement");
      }
      for (Node node = documentElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "sharedInformation") {
            parseSharedInformationElements(element);
         }
         else if (element.getTagName() == "deviceList") {
            parseDeviceList(element);
         }
      }
      valid = true;
   }

   /**
    * List devices in database
    * 
    */
   public void listDevices(PrintStream stream, boolean printAliases) {
      stream.println("No of Devices '" + deviceList.size() + "'.");

      if (defaultDevice == null) {
         stream.println("No default device set");
      }
      else {
         stream.println(defaultDevice.toString());
      }
      for (Device device : deviceList) {
         if (printAliases || !device.isAlias()) {
            stream.println(device.toString());
         }
      }
   }
  
//  /**
//   * List devices in XML format
//   * 
//   */
// public void toXML(PrintStream xmlOut) {
//    toXML(xmlOut, false);
//  }
// 
// public void toXML(PrintStream xmlOut, boolean listAliasDevices) {
//    Iterator<Device> it = deviceList.iterator();
//    while(it.hasNext()) {
//       Device device = it.next();
//       if (listAliasDevices || !device.isAlias()) {
//          device.toXML(xmlOut, 0);
//       }
//    }
// }

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
      case T_MC56F80xx:
         filename = UsbdmJniConstants.MC56F_DEVICE_FILE;
         break;
      default:
         throw new UsbdmException("Unsupported target");
      };
      IPath usbdmResourcePath = Usbdm.getResourcePath();
      if (usbdmResourcePath == null) {
         return null;
      }
     usbdmResourcePath = usbdmResourcePath.append("/DeviceData/").append(filename);
//     System.err.println("DeviceDatabase.getXmlFilepath(): XML file path = " + usbdmResourcePath);
     
     return (Path) usbdmResourcePath;
   }
   
   /**
    * 
    * @return
    * @throws Exception 
    */
   private void parseXmlFile(Path databasePath) throws Exception {
      // Get the factory
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

      try {
         //  Using factory get an instance of document builder
         DocumentBuilder db = dbf.newDocumentBuilder();

         //  Parse using builder to get DOM representation of the XML file
         dom = db.parse(databasePath.toOSString());
      } catch (Exception e) {
         throw new Exception("Database creation failed", e);
      }
   }
  
  /**
    *  Constructor
    * 
    *  @param xmlFilename device database file name e.g. arm_devices.xml
    *  
    *  @throws Exception 
    */
   public DeviceDatabase(final TargetType targetType) {

      fTargetType   = targetType;

      deviceList        = new ArrayList<Device>();
      sharedInformation = new SharedInformationMap();
      valid             = false;

      BusyIndicator.showWhile(null, new Runnable() {
         public void run() {
            try {
               // Parse the xml file
               parseXmlFile(getXmlFilepath(targetType));

               //  Process XML contents and generate Device list
               parseDocument();
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      });
   }
   
   /**
    *  Constructor
    * 
    *  @param xmlFilename device database file name e.g. arm_devices.xml
    *  
    *  @throws Exception 
    */
   public DeviceDatabase(final TargetType targetType, final IDatabaseListener listener) {

      fTargetType       = targetType;
      deviceList        = new ArrayList<Device>();
      sharedInformation = new SharedInformationMap();
      valid             = false;

      Job job = new Job("Long Job") {
         protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask("Loading device database...", 10);
            
            try {
               // Parse the xml file
               parseXmlFile(getXmlFilepath(targetType));
               
               //  Process XML contents and generate Device list
               parseDocument();
            } catch (Exception e) {
               e.printStackTrace();
            }
            // Run long running task here
            monitor.done();
            listener.databaseLoaded(DeviceDatabase.this);
            return Status.OK_STATUS;
         }
      };
      job.setUser(true);
      job.schedule();
   }
   
   public TargetType getTargetType() {
      return fTargetType;
   }

final Pattern namePattern = Pattern.compile("^(.*)xxx([0-9]*)$");

 /**
  * Returns the device with the given name
  * 
  * @param name
  * @return
  */
 public Device getDevice(String name) {
    Device device = getExactDevice(name);
    if (device != null) {
       return device;
    }
    Matcher m = namePattern.matcher(name);
    if (!m.matches()) {
       return null;
    }
    name = m.group(1)+"M"+m.group(2);
    device = getExactDevice(name);
    return device;
 }
 
/**
* Returns the device with the given name
* 
* @param name
* @return
*/
public Device getExactDevice(String name) {
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

   public ArrayList<Device> getDeviceList() {
      return deviceList;
   }

}
