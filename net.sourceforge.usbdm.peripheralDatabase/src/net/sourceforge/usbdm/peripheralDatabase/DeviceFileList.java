package net.sourceforge.usbdm.peripheralDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DeviceFileList  extends SVD_XML_BaseParser {
   
   private HashMap<String, String> deviceList = null;
   private boolean         valid             = false;

   DeviceFileList() {
      
   }
   
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
   
   public boolean isValid() {
      return valid;
   }

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
      valid = true;
   }

   /**
    *  Creates peripheral database for device
    * 
    *  @param deviceName device name e.g. "MKL25Z128M5"
    *  
    *  @return device peripheral description or null on error
    * @throws Exception 
    */
   public static DeviceFileList createDeviceFileList(IPath fileListPath) throws Exception {
      DeviceFileList deviceFileList = new DeviceFileList();
      
      // Parse the XML file into the XML internal DOM representation
      Document dom = parseXmlFile(fileListPath);
      
      // Get the root element
      Element documentElement = dom.getDocumentElement();
      
      //  Process XML contents and generate Device list
      deviceFileList.parseDocument(documentElement);
      
      return deviceFileList;
   }

   
   private static class PatternPair {
      Pattern p;
      String  m;
      public PatternPair(Pattern p, String m) {
         this.p = p;
         this.m = m;
      }
   }
   
   static ArrayList<PatternPair> mappedNames = null;

   /**
    * Maps raw device name to class name e.g. MK11DN512M5 -> MK11D5
    * 
    * @param originalName name to map
    * @return mapped name
    */
   public static String getMappedSvdName(String originalName) {
      if (mappedNames == null) {
         mappedNames = new ArrayList<PatternPair>();
         mappedNames.add(new PatternPair(Pattern.compile("^([^\\d]+\\d+[D|F|Z])[N|X]?\\d{2,}M(\\d+)$"), "$1$2"));  // MK11DN512M5 -> MK11D5
         mappedNames.add(new PatternPair(Pattern.compile("^([^\\d]+\\d+[D|F|Z])[N|X]?1M0M(\\d+)$"),     "$1$2"));  // MK10FN1M0M5 -> MK10D5
         mappedNames.add(new PatternPair(Pattern.compile("^([^\\d]+\\d+[D|F|Z])[N|X]?\\d{2,}Z$"),       "$1Z10")); // MK10DN512Z  -> MK10DZ10
         mappedNames.add(new PatternPair(Pattern.compile("^([^\\d]+\\d+[D|F|Z])[N|X]?\\d{2,}$"),        "$15"));   // MK10DN128   -> MK10D5
         mappedNames.add(new PatternPair(Pattern.compile("^([^\\d]+\\d+[D|F|Z])[N|X]?1M0$"),            "$112"));  // MK10FN1M0   -> MK10F12
      }
      for (PatternPair pair : mappedNames ) {
         Pattern p = pair.p;
         Matcher m = p.matcher(originalName);
         if (m.matches()) {
            return m.replaceAll(pair.m);
         }
      }
      return originalName;
   }

   /**
    *  Determine the filename of the SVD file for the device (without .xml extension)
    *  
    * @param deviceName
    * 
    * @return filename e.g.  MK11DN512M5 -> MK11D5.svd
    */
   public String getSvdFilename(String deviceName) {
      String mappedDeviceName = deviceList.get(deviceName);
      if (mappedDeviceName == null) {
         // Map the name using default mapping
         mappedDeviceName = getMappedSvdName(deviceName);
      }
      return mappedDeviceName+".svd";
   }

   /**
    *  Creates peripheral database for device
    * 
    *  @param devicenameOrFilename svd file path or device name
    *  
    *  @return device peripheral description or null on error
    * @throws Exception 
    */
   public static DeviceFileList createDeviceFileList(String devicenameOrFilename) throws Exception {
      DeviceFileList deviceFileList = new DeviceFileList();
      
      // Parse the XML file into the XML internal DOM representation
      Document dom = parseXmlFile(devicenameOrFilename);
      
      // Get the root element
      Element documentElement = dom.getDocumentElement();
      
      //  Process XML contents and generate Device list
      deviceFileList.parseDocument(documentElement);
      
      return deviceFileList;
   }

}
