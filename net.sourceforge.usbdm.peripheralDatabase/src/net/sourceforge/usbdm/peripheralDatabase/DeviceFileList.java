package net.sourceforge.usbdm.peripheralDatabase;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DeviceFileList extends SVD_XML_BaseParser {
   
   private HashMap<String, String> deviceList = new HashMap<String, String>();

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
   private HashMap<String, String> parseDeviceList(Element documentElement) throws Exception {
      if (documentElement == null) {
         System.out.println("DeviceDatabase.parseDocument() - failed to get documentElement");
         return null;
      }

      HashMap<String, String> deviceList = null;

      for (Node node = documentElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "device") {
            if (deviceList == null) {
               deviceList = new HashMap<String,String>();
            }
            deviceList.put(element.getAttribute("name"), element.getTextContent());
         }
         else {
            throw new Exception("Unexpected field in DEVICELIST', value = \'"+element.getTagName()+"\'");
         }
      }
      return deviceList;
   }
   
   public static class Pair {
      public String deviceName;
      public String mappedDeviceName;
      
      public Pair(String deviceName, String mappedDeviceName) {
         this.deviceName       = deviceName;
         this.mappedDeviceName = mappedDeviceName;
      }
   }
   
   public ArrayList<DeviceFileList.Pair> getArrayList() {
      ArrayList<DeviceFileList.Pair> arrayList = new ArrayList<DeviceFileList.Pair>();
      Iterator<Entry<String, String>> it = deviceList.entrySet().iterator();
      while (it.hasNext()) {
          Entry<String, String> pairs = it.next();
          arrayList.add(new DeviceFileList.Pair(pairs.getKey(), pairs.getValue()));
      }
      return arrayList;
   }
   
   /**
    * Get list of devices
    * 
    * @return List of devices
    */
   public Vector<String> getDeviceList() {
      Vector<String> list = new Vector<String>();
      Iterator<Entry<String, String>> it = deviceList.entrySet().iterator();
      while (it.hasNext()) {
          Entry<String, String> pairs = it.next();
          list.add(pairs.getKey());
      }
      return list;
   }
   
   /**
    * Determine the filename of the SVD file for the device (without any extension)
    * 
    * @param deviceName
    * 
    * @return filename if found e.g. MK11D5, or null if not found
    */
   public Path getSvdFilename(String deviceName) {
      String path = deviceList.get(deviceName);
      if (path == null) {
         return null;
      }
      return Paths.get(path);
   }

}
