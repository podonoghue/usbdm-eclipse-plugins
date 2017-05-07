package net.sourceforge.usbdm.peripheralDatabase;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DeviceFileList extends SVD_XML_BaseParser {
   
   private ArrayList<DeviceSvdInfo> deviceList = new ArrayList<DeviceSvdInfo>();

   /**
    *  Creates device name map from deviceFileList file
    * 
    *  @param filePath Path to file
    *  
    *  @return device peripheral description
    *  @throws Exception 
    */
   public DeviceFileList(Path filePath) throws Exception {
      // Parse the XML file into the XML internal DOM representation
      Document dom = parseXmlFile(filePath);
      
      //  Process XML contents and generate Device list
      parseDocument(dom.getDocumentElement());
   }
   
   /**
    * Parses document and creates the hash-map
    * 
    * @param documentElement
    * 
    * @throws Exception
    */
   private void parseDocument(Element documentElement) throws Exception {
      if (documentElement == null) {
         System.out.println("DeviceDatabase.parseDocument() - failed to get documentElement");
         return;
      }

      for (Node node = documentElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "description") {
            continue;
         }
         else if (element.getTagName() == "deviceList") {
            deviceList = parseDeviceList(element);
         }
         else {
            throw new Exception("Unexpected field in ROOT', value = \'"+element.getTagName()+"\'");
         }
      }
   }
   
   /**
    * Parses deviceList element and creates hash-map
    * 
    * @param documentElement
    * 
    * @return Map of device names to SVD file name
    * 
    * @throws Exception
    */
   private ArrayList<DeviceSvdInfo> parseDeviceList(Element documentElement) throws Exception {
      if (documentElement == null) {
         System.out.println("DeviceDatabase.parseDocument() - failed to get documentElement");
         return null;
      }

      ArrayList<DeviceSvdInfo> deviceList = null;

      for (Node node = documentElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "device") {
            if (deviceList == null) {
               deviceList = new ArrayList<DeviceSvdInfo>();
            }
            deviceList.add(new DeviceSvdInfo(element.getAttribute("name"), element.getAttribute("svdFileName"), element.getAttribute("pattern")));
         }
         else {
            throw new Exception("Unexpected field in DEVICELIST', value = \'"+element.getTagName()+"\'");
         }
      }
      return deviceList;
   }
   
   public static class DeviceSvdInfo {
      public String deviceName;
      public String deviceNamePattern;
      public String svdName;
      
      public DeviceSvdInfo(String deviceName, String svdName, String deviceNamePattern) {
         this.deviceName        = deviceName;
         this.svdName           = svdName;
         this.deviceNamePattern = deviceNamePattern;
      }
   }
   
   public ArrayList<DeviceSvdInfo> getArrayList() {
      return deviceList;
   }
   
   /**
    * Get list of devices
    * 
    * @return List of devices
    */
   public Vector<String> getDeviceList() {
      Vector<String> list = new Vector<String>();
      for (DeviceSvdInfo entry:deviceList) {
         list.add(entry.deviceName);
      }
      return list;
   }
   
   /**
    * Determine the base file name for the deviceName.<br>
    * This can be used to construct the name of either the header file or the SVD file.
    * 
    * @param deviceName
    * 
    * @return filename if found e.g. MK11D5, or null if not found
    */
   public String getBaseFilename(String deviceName) {
      for (DeviceSvdInfo entry:deviceList) {
         // Use pattern if it exists
         if (!entry.deviceNamePattern.isEmpty() && deviceName.matches(entry.deviceNamePattern)) {
            return entry.svdName;
         }
         // Try name directly
         if (deviceName.equalsIgnoreCase(entry.deviceName)) {
            return entry.svdName;
         }
      }
      return null;
   }

   /**
    * Determine the mapped device name for the deviceName e.g. MK10DxxxM5
    * 
    * @param deviceName
    * 
    * @return Device name if found e.g. MK10DxxxM5, or null if device is not found
    */
   public String getMappedDeviceName(String deviceName) {
      for (DeviceSvdInfo entry:deviceList) {
         // Use pattern if it exists
         if (!entry.deviceNamePattern.isEmpty() && deviceName.matches(entry.deviceNamePattern)) {
            return entry.deviceName;
         }
         // Try name directly
         if (deviceName.equalsIgnoreCase(entry.deviceName)) {
            return entry.deviceName;
         }
      }
      return null;
   }

}
