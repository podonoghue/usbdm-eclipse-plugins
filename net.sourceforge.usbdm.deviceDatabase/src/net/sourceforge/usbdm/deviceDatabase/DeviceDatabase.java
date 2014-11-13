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
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.usbdm.deviceDatabase.Device.ClockTypes;
import net.sourceforge.usbdm.deviceDatabase.Device.Condition;
import net.sourceforge.usbdm.deviceDatabase.Device.CreateFolderAction;
import net.sourceforge.usbdm.deviceDatabase.Device.ExcludeAction;
import net.sourceforge.usbdm.deviceDatabase.Device.FileAction;
import net.sourceforge.usbdm.deviceDatabase.Device.FileList;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryRegion;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryRegion.MemoryRange;
import net.sourceforge.usbdm.deviceDatabase.Device.MemoryType;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectActionList;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectCustomAction;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectOption;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectVariable;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.jni.UsbdmJniConstants;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DeviceDatabase {

   private ArrayList<Device> deviceList;       // List of all devices
   private Device            defaultDevice;    // Default device

   private Document          dom;
   private boolean           valid = false;
   private TargetType        targetType;
   
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
      return new MemoryRange(memoryStartAddress, memoryEndAddress);
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

      // <memory>
      String sMemoryType = memoryElement.getAttribute("type");
      MemoryType memoryType = MemoryType.getTypeFromXML(sMemoryType);
      if (memoryType == null) {
         throw new Exception("DeviceXmlParser::parseDeviceXML() - invalid memory range type \""+sMemoryType+"\"");
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
    * Parse a <projectOption> element
    * 
    * @param    element <projectOption> element
    * 
    * @return   element described 
    * 
    * @throws Exception 
    */
   private ProjectOption parseProjectOptionElement(Element optionElement) throws Exception {
      // <projectOption>
      String id       = optionElement.getAttribute("id");
      String path     = null;
      if (optionElement.hasAttribute("path")) {
         path = optionElement.getAttribute("path");
      }
      boolean replace = false;
      if (optionElement.hasAttribute("replace")) {
         replace = optionElement.getAttribute("replace").equalsIgnoreCase("true");
      }
      if (id.isEmpty()) {
         throw new Exception("<projectOption> is missing required attribute");
      }
      ArrayList<String> values = new ArrayList<String>();
      
      for (Node node = optionElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "value") {
            values.add(element.getTextContent().trim());
         }
      }
      if (values.size() == 0) {
         throw new Exception("Missing <value> element in <projectOption>");
      }
//      System.err.println("parseOptionElement() value = "+values.get(0));

      return new ProjectOption(id, path, values.toArray(new String[values.size()]), replace);
   }

   /**
    * Parse a <customAction> element
    * 
    * @param    element <customAction> element
    * 
    * @return   element described 
    * 
    * @throws Exception 
    */
   private ProjectCustomAction parseCustomActionElement(Element customActionElement) throws Exception {
      // <projectOption>
      String className  = customActionElement.getAttribute("class");
      if (className.isEmpty()) {
         throw new Exception("<customAction> is missing required attribute");
      }
      ArrayList<String> values = new ArrayList<String>();
      
      for (Node node = customActionElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "value") {
            values.add(element.getTextContent().trim());
         }
      }
//      System.err.println(String.format("parseCustomActionElement(%s, %s)", className, (values.size()==0)?"<empty>":values.get(0)));

      return new ProjectCustomAction(className, values.toArray(new String[values.size()]));
   }

   public enum FileType {
      NORMAL,
      LINK,
   }
   
    /**
    * Parse a <copy> element
    * 
    * @param    fileElement <fileList> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private FileAction parseCopyElement(Element element) throws Exception {
      // <copy  >
      String source     = element.getAttribute("source");
      String target     = element.getAttribute("target");
      if (source.isEmpty() || target.isEmpty()) {
         throw new Exception("Missing attribute in <file ...>");
      }
      String type       = element.getAttribute("type");
      FileType fileType = FileType.NORMAL;
      if (type.equalsIgnoreCase("link")) {
         fileType = FileType.LINK;
      }
      
      String sReplacable = element.getAttribute("replacable");
      boolean isReplacable = !sReplacable.equalsIgnoreCase("false");
      
      FileAction fileInfo = new FileAction(null, source, target, fileType, isReplacable);
      
      return fileInfo;
   }
     
   /**
   * Parse a <excludeFolder> element
   * 
   * @param    fileElement <excludeFolder> element
   * 
   * @return   File list described 
   * @throws Exception 
   */
  private ExcludeAction parseExcludeSourceFolderElement(Element element) throws Exception {
     // <excludeFolder target="..." excluded="..."  >
     String target     = element.getAttribute("target");
     boolean isExcluded = true;
     if (element.hasAttribute("excluded")) {
        isExcluded = !element.getAttribute("excluded").equalsIgnoreCase("false");
     }
     ExcludeAction fileInfo = new ExcludeAction(null, target, isExcluded, true);
     return fileInfo;
  }
    
  /**
  * Parse a <excludeFolder> element
  * 
  * @param    fileElement <excludeFolder> element
  * 
  * @return   File list described 
  * @throws Exception 
  */
 private ExcludeAction parseExcludeSourceFileElement(Element element) throws Exception {
    // <excludeFile target="..." excluded="..."  >
    String target     = element.getAttribute("target");
    boolean isExcluded = true;
    if (element.hasAttribute("excluded")) {
       isExcluded = !element.getAttribute("excluded").equalsIgnoreCase("false");
    }
    ExcludeAction fileInfo = new ExcludeAction(null, target, isExcluded, false);
    return fileInfo;
 }
   
   /**
    * Parse a <condition> element
    * The child nodes are added top the actionlist
    * 
    * @param    conditionElement <condition> element
    * @param    variableList 
    * @param    projectActionList The device to add action to
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private Condition parseConditionElement(
         Element conditionElement, 
         Map<String, ProjectVariable> variableList, 
         ProjectActionList projectActionList, 
         String root) throws Exception {

      String variableName = conditionElement.getAttribute("variable");
      ProjectVariable variable      = variableList.get(variableName);
      String          defaultValue  = "false";
      boolean         negated       = false;

      if (variable == null) {
         throw new Exception("Variable \'"+variableName+"\' not found in <condition>");
      }
      
      if (conditionElement.hasAttribute("defaultValue")) {
         defaultValue = conditionElement.getAttribute("defaultValue");
      }
      if (conditionElement.hasAttribute("negated")) {
         negated = Boolean.valueOf(conditionElement.getAttribute("negated"));
      }
      
      Device.Condition condition = new Device.Condition(variable, defaultValue, negated);
      
      // <condition>
      for (Node node = conditionElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         // element node for <fileList>
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
//         System.err.println("parseFileListElements() " + node.getNodeName());
         Element element = (Element) node;
         if (element.getTagName() == "excludeSourceFile") {
            ExcludeAction excludeAction = parseExcludeSourceFileElement(element);
            excludeAction.setCondition(condition);
            projectActionList.add(excludeAction);
         }
         else if (element.getTagName() == "excludeSourceFolder") {
            ExcludeAction excludeAction = parseExcludeSourceFolderElement(element);
            excludeAction.setCondition(condition);
            projectActionList.add(excludeAction);
         }
         else if (element.getTagName() == "createFolder") {
            CreateFolderAction action = parseCreateFolderElement(element);
            action.setCondition(condition);
            action.setRoot(root);
            projectActionList.add(action);
         }
         else if (element.getTagName() == "copy") {
            FileAction fileInfo = parseCopyElement(element);
            fileInfo.setRoot(root);
            fileInfo.setCondition(condition);
            projectActionList.add(fileInfo);
         }
         else if (element.getTagName() == "projectOption") {
            ProjectOption projectOption = parseProjectOptionElement(element);
            projectOption.setCondition(condition);
            projectActionList.add(projectOption);
         }
         else if (element.getTagName() == "customAction") {
            ProjectCustomAction action = parseCustomActionElement(element);
            action.setCondition(condition);
            projectActionList.add(action);
         }
         else {
            throw new Exception("Unexpected element \""+element.getTagName()+"\" in <condition>");
         }
      }
      return condition;
   }
   
   /**
    * Parse a <projectVariable> element
    * 
    * @param    <projectVariable> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private ProjectVariable parseVariableElement(Element projectVariableElement) {
      // <projectVariable>
      String id            = projectVariableElement.getAttribute("id");
      String name          = projectVariableElement.getAttribute("name");
      String description   = projectVariableElement.getAttribute("description");
      String defaultValue  = projectVariableElement.getAttribute("defaultValue");
      return new ProjectVariable(id, name, description, defaultValue);
   }

      /**
    * Parse a <createFolder> element
    * 
    * @param  createFolderElement <createFolder> element
    * 
    * @return   File list described 
    * @throws Exception 
    */
   private CreateFolderAction parseCreateFolderElement(Element createFolderElement) {
      // <createFolder target="..." type="..." >
      String target  = createFolderElement.getAttribute("target");
      String type    = createFolderElement.getAttribute("type");
      return new CreateFolderAction(target, type);
   }

   /**
    * Parse a <projectActionList> element
    * 
    * @param    projectActionListElement <projectActionList> element
    * 
    * @return   Action list described 
    * 
    * @throws Exception 
    */
   private ProjectActionList parseProjectActionListElement(Element projectActionListElement) throws Exception {
      return parseProjectActionListElement(projectActionListElement, new ProjectActionList(), new HashMap<String, ProjectVariable>());
   }
   
   /**
    * Parse a <projectActionList> element
    * 
    * @param    listElement <projectActionList> element
    * @param device 
    * 
    * @return   Action list described 
    * 
    * @throws Exception 
    */
   private ProjectActionList parseProjectActionListElement(Element listElement, ProjectActionList projectActionList, Map<String,ProjectVariable> variableList) throws Exception {

      String root = null;
      if (listElement.hasAttribute("root")) {
         root = listElement.getAttribute("root");
      }
      // <projectActionList>
      for (Node node = listElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         // child node for <projectActionList>
         Element element = (Element) node;
         if (element.getTagName() == "projectActionListRef") {
            projectActionList.add(sharedInformation.findSharedActionList(element.getAttribute("ref")));
         }
         else if (element.getTagName() == "projectActionList") {
            parseProjectActionListElement(element, projectActionList, variableList);
         }
         else if (element.getTagName() == "variable") {
            ProjectVariable variable = parseVariableElement(element);
            variableList.put(variable.getId(), variable);
         }
         else if (element.getTagName() == "condition") {
            parseConditionElement(element, variableList, projectActionList, root);
         }
         else if (element.getTagName() == "excludeSourceFile") {
            ExcludeAction excludeAction = parseExcludeSourceFileElement(element);
            projectActionList.add(excludeAction);
         }
         else if (element.getTagName() == "excludeSourceFolder") {
            ExcludeAction excludeAction = parseExcludeSourceFolderElement(element);
            projectActionList.add(excludeAction);
         }
         else if (element.getTagName() == "createFolder") {
            CreateFolderAction createFolderAction = parseCreateFolderElement(element);
            createFolderAction.setRoot(root);
            projectActionList.add(createFolderAction);
         }
         else if (element.getTagName() == "copy") {
            FileAction fileAction = parseCopyElement(element);
            fileAction.setRoot(root);
            projectActionList.add(fileAction);
         }
         else if (element.getTagName() == "customAction") {
            ProjectCustomAction projectCustomAction = parseCustomActionElement(element);
            projectActionList.add(projectCustomAction);
         }
         else if (element.getTagName() == "projectOption") {
            ProjectOption projectOption = parseProjectOptionElement(element);
            projectActionList.add(projectOption);
         }
         else {
            throw new Exception("Unexpected element \""+element.getTagName()+"\" in <projectActionList>");
         }
      }
      return projectActionList;
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
      Device device = new Device(targetType, name);

      if (deviceElement.hasAttribute("isDefault")) {
         device.setDefault();
      }
      if (deviceElement.hasAttribute("alias")) {
         device.setAlias(deviceElement.getAttribute("alias"));
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
            device.addMemoryRegion(parseMemoryElements(element));
         }
         else if (element.getTagName() == "memoryRef") {
            device.addMemoryRegion(sharedInformation.findSharedMemory(element.getAttribute("ref")));
         }
         else if (element.getTagName() == "projectActionList") {
            device.addToActionList(parseProjectActionListElement(element));
         }
         else if (element.getTagName() == "projectActionListRef") {
            device.addToActionList(sharedInformation.findSharedActionList(element.getAttribute("ref")));
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
            ProjectActionList actionList = parseProjectActionListElement(element);
            String key = element.getAttribute("id");
            if (key == null) {
               throw new Exception("parseSharedInformationElements() - null key");
            }
            sharedInformation.put(key, actionList);
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
      for (Device device : deviceList) {
         if (!device.isAlias()) {
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
      default:
         throw new UsbdmException("Device file not found");
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
   public DeviceDatabase(TargetType targetType) {
            
      this.targetType   = targetType;
      
      deviceList        = new ArrayList<Device>();
      sharedInformation = new SharedInformationMap();
      valid             = false;
      
      try {
         Path databasePath = getXmlFilepath(targetType);
         
         // Parse the xml file
         parseXmlFile(databasePath);
         
         //  Process XML contents and generate Device list
         parseDocument();
         valid = true;
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   public TargetType getTargetType() {
      return targetType;
   }

   private int nestingLimit = 0;
   public Device getDevice(String name) {
      if (nestingLimit++ > 10) {
         nestingLimit = 0;
         return null;
      }
      for (int index = 0; index<deviceList.size(); index++) {
         Device device = deviceList.get(index);
         if (device.getName().equals(name)) {
            if (device.isAlias()) {
               return getDevice(device.getAlias());
            }
            nestingLimit = 0;
            return device;
         }
      }
      nestingLimit = 0;
      return null;
   }
   
   public Device getDefaultDevice() {
      return defaultDevice;
   }

   public ArrayList<Device> getDeviceList() {
      return deviceList;
   }

}
