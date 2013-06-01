package net.sourceforge.usbdm.cdt.examplewizard;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * @author pgo
 *
 */
public class ExampleList {

   private Document dom;
   private boolean  valid = false;
   private ArrayList<ProjectInformation> projectList;
   private final String FOLDER_LOCATION = "Examples";
   private final String FILE_LOCATION   = FOLDER_LOCATION+"/Examples.xml";
   private String exampleFolderPath     = null;
   private String description;
   
   public ArrayList<ProjectInformation> getProjectList() {
      return projectList;
   }

   static public class Attribute {
      String key;
      String value;
      
      public Attribute(String key, String value) {
         this.key   = key;
         this.value = value;
      }
   }

   static public class ProjectInformation {
      private String description;
      private String longDescription;
      private IPath  path;
      private String family;
      private ArrayList<Attribute> attributes;
      
      public ProjectInformation(String description, String longDescription, IPath path, String family) {
         this.description     = description;
         this.longDescription = longDescription;
         this.path            = path;
         this.family          = family;
         this.attributes      = new ArrayList<Attribute>();
      }
      /**
       * @return the location
       */
      public IPath getPath() {
         return path;
      }
      /**
       * @return the family
       */
      public String getFamily() {
         return family;
      }
      /**
       * @return the description
       */
      public String getDescription() {
         return description;
      }
      /**
       * @return the long description
       */
      public String getLongDescription() {
         return longDescription;
      }
      /**
       * @return the attributes
       */
      public ArrayList<Attribute> getAttributes() {
         return attributes;
      }
      /**
       * @param attribute the attribute to add
       */
      public void addAttribute(String key, String value) {
         this.attributes.add(new Attribute(key, value));
      }
      /**
       * @param attribute the attribute to add
       */
      public void addAttribute(Attribute attribute) {
         this.attributes.add(attribute);
      }
   }
   
   /**
    * @param projectInfo    project info 
    * 
    * @param exampleElement <example> element
    * 
    * @throws UsbdmException
    */
   private void parseVariables(ProjectInformation projectInfo, Element exampleElement) throws UsbdmException {
      for (Node node = exampleElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         // element node for <example>
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() != "variable") {
            throw new UsbdmException("parseExampleList(" + node.getNodeName() + ") - not variable element");
         }
         String key   = element.getAttribute("name");
         String value = element.getTextContent();
         projectInfo.addAttribute(key, value);
      }
   }
   
   /**
    * Parse exampleList
    * 
    * @param exampleListElement <exampleList> element 
    * 
    * @return list of project elements
    *  
    * @throws UsbdmException 
    */
   private ArrayList<ProjectInformation> parseExampleList(Element exampleListElement) throws UsbdmException {
      
      ArrayList<ProjectInformation> projectList = new ArrayList<ProjectInformation>();
      for (Node node = exampleListElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         // element node for <example>
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() != "example") {
            throw new UsbdmException("parseExampleList(" + node.getNodeName() + ") - not example element");
         }
         String  description = element.getAttribute("description");
         String  longDescription = element.getTextContent().trim();
         IPath   path       = new Path(exampleFolderPath + "/" + element.getAttribute("path"));
//         File    file       = path.toFile();
//         if (!file.isFile() || !file.canRead()) {
//            throw new UsbdmException("Failed to find Example Project file: " + path.toString());
//         }         
         String family = element.getAttribute("family");
         ProjectInformation projectInfo = new ProjectInformation(description, longDescription, path, family);
         projectList.add(projectInfo);
         
         parseVariables(projectInfo, element);
      }
      return projectList;
   }
   
   /**
    * Parse the information from the document
    * 
    * @throws UsbdmException 
    */
   void parseDocument() throws UsbdmException {
      // Get the root element
      Element documentElement = dom.getDocumentElement();

      if (documentElement == null) {
         throw new UsbdmException("DeviceDatabase.parseDocument() - failed to get documentElement");
      }
      for (Node node = documentElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "description") {
            if (description != null) {
               throw new UsbdmException("Too many description elements");
            }
            description = node.getTextContent();
         }
         if (element.getTagName() == "exampleList") {
            if (projectList != null) {
               throw new UsbdmException("Too many exampleList elements");
            }
            projectList = parseExampleList(element);
         }
      }
   }

   /**
    * @param exampleListPath path to example list XML file
    * 
    * @throws UsbdmException 
    */
   void parseXmlFile(String exampleListPath) throws UsbdmException {

      // Get the factory
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      try {
         //  Using factory get an instance of document builder
         DocumentBuilder db = dbf.newDocumentBuilder();
         
         //  Parse using builder to get DOM representation of the XML file
         dom = db.parse(exampleListPath);
      }catch(ParserConfigurationException pce) {
         pce.printStackTrace();
         throw new UsbdmException(pce.getMessage());
      }catch(SAXException se) {
         se.printStackTrace();
         throw new UsbdmException(se.getMessage());
      }catch(IOException ioe) {
         ioe.printStackTrace();
         throw new UsbdmException(ioe.getMessage());
      }
   }
   
   /**
    * @return valid
    */
   public boolean isValid() {
      return valid;
   }

   public ExampleList() {
      valid = false;
      projectList = null;
      description = null;
      
      try {
         String applicationPath = Usbdm.getUsbdmApplicationPath();         

         exampleFolderPath = applicationPath + FOLDER_LOCATION;

         // Parse the xml file
         parseXmlFile(applicationPath + FILE_LOCATION);

         //  Process XML contents and generate Example list
         parseDocument();
      } catch (UsbdmException e) {
         e.printStackTrace();
      }
      valid = true;
   }
   
   /**
    * @param args
    */
   public static void main(String[] args) {
      // TODO Auto-generated method stub

   }
}
