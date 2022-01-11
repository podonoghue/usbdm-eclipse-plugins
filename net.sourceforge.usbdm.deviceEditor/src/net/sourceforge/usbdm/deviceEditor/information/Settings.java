package net.sourceforge.usbdm.deviceEditor.information;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.usbdm.deviceEditor.parsers.XML_BaseParser;
import net.sourceforge.usbdm.deviceEditor.parsers.XmlDocumentUtilities;

/**
 * Class used to persist settings
 */
public class Settings {
   
   private final String        fDefaultSection;
   private Map<String, String> fMap = new TreeMap<String, String>();
   XmlDocumentUtilities        fDocumentUtilities = null;
   
   /**
    * Constructor
    * 
    * @param defaultSection Name of default section
    */
   public Settings(String defaultSection) {
      fDefaultSection = defaultSection;
   }

   /**
    * Load persistent settings from path
    * 
    * @param path
    * @throws Exception
    */
   public void load(Path path) throws Exception {
      try {
         load (XML_BaseParser.parseXmlFile(path));
      } catch (Exception e) {
         e.printStackTrace();
         throw new Exception("Failed to load settings from "+path, e);
      }
   }

   /**
    * Save persistent settings to path
    * 
    * @param path
    */
   public void save(Path path) {
      try {
         BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
         fDocumentUtilities = new XmlDocumentUtilities(writer);
         fDocumentUtilities.writeXmlFilePreamble(
               path.getFileName().toString(), 
               null, 
               "Settings");
         save();
         writer.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   /**
    * Add item to settings
    * 
    * @param key
    * @param value
    */
   public void put(String key, String value) {
      fMap.put(key, value);
   }

   /**
    * Retrieve item from settings
    * 
    * @param key
    * @return
    */
   public String get(String key) {
      String value = fMap.get(key);
      if (value != null) {
         return value;
      }
      // Compatibility
      if (key.startsWith("$signal$")) {
         return fMap.get(key.substring(8));
      }
      if (key.startsWith("$$")) {
         return fMap.get(key.substring(2));
      }
      return null;
   }

   /**
    * Load a section from persistent storage
    * 
    * @param sectionElement
    * @throws Exception
    */
   private void loadSection(Element sectionElement) throws Exception {
      for (Node node = sectionElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element)node;
         if (element.getTagName().equals("fragment")) {
            loadSection(element);
         }
         else if (!element.getTagName().equals("item")) {
            throw new Exception("Unexpected element " + element.getTagName());
         }
         fMap.put(element.getAttribute("key"), element.getAttribute("value"));
      }      
   }

   /**
    * Load settings from document
    * 
    * @param document
    * @throws Exception
    */
   private void load(Document document) throws Exception {
      Element documentElement = document.getDocumentElement();
      if (documentElement == null) {
         throw new Exception("Failed to get documentElement");
      }
      for (Node node = document.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element)node;
         if (!element.getTagName().equals("section")) {
            throw new Exception("Unexpected element " + element.getTagName());
         }
         loadSection(element);
      }      
   }

   /**
    * Save a key/value pair
    * 
    * @param key
    * @param value
    * @throws IOException
    */
   private void saveValue(String key, String value) throws IOException {
      fDocumentUtilities.openTag("item");
      fDocumentUtilities.writeAttribute("key", key);
      fDocumentUtilities.writeAttribute("value", XML_BaseParser.escapeString(value));
      fDocumentUtilities.closeTag();
   }
   
   /**
    * Save settings
    * 
    * @throws IOException
    */
   private void save() throws IOException {
      fDocumentUtilities.setAttrWidth(50);
      fDocumentUtilities.openTag("section");
      fDocumentUtilities.writeAttribute("name", fDefaultSection);
      for (String key:fMap.keySet()) {
         saveValue(key, fMap.get(key));
      }
      fDocumentUtilities.closeTag();
   }

   public Set<String> getKeys() {
      return fMap.keySet();
   }

}
