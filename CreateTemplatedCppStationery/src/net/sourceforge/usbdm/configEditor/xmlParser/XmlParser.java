package net.sourceforge.usbdm.configEditor.xmlParser;

import java.nio.file.Path;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.usbdm.configEditor.information.DeviceInfo;
import net.sourceforge.usbdm.configEditor.information.DevicePackage;
import net.sourceforge.usbdm.configEditor.information.MuxSelection;
import net.sourceforge.usbdm.configEditor.information.PinInformation;

public class XmlParser extends XML_BaseParser {
      
   /** Device Information */
   private DeviceInfo factory = null;
   
   /**
    * Parse &lt;pin&gt;
    * 
    * @param pinElement
    */
   private void parsePin(Element pinElement) {
      
      PinInformation pin = factory.createPin(pinElement.getAttribute("name"));
      
      for (Node node = pinElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "mux") {
            factory.createMapping(factory.findOrCreatePeripheralFunction(element.getAttribute("function")), pin, MuxSelection.valueOf(element.getAttribute("sel")));
         }
         else if (element.getTagName() == "reset") {
            pin.setResetPeripheralFunctions(MuxSelection.valueOf(element.getAttribute("sel")));
         }
         else if (element.getTagName() == "default") {
            pin.setDefaultPeripheralFunctions(MuxSelection.valueOf(element.getAttribute("sel")));
         }
         else {
            throw new RuntimeException("Unexpected field in PIN, value = \'"+element.getTagName()+"\'");
         }
      }
   }

   /**
    * Parse &lt;pins&gt;
    * 
    * @param pinsElement
    */
   private void parsePins(Element pinsElement) {
      
      for (Node node = pinsElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "pin") {
            parsePin(element);
         }
         else {
            throw new RuntimeException("Unexpected field in PINS, value = \'"+element.getTagName()+"\'");
         }
      }
   }
   
   /**
    * Parse &lt;family&gt;
    * 
    * @param familyElement
    */
   private void parseFamily(Element familyElement) {

      factory = new DeviceInfo("Unknown", familyElement.getAttribute("name"));
      factory.initialiseTemplates();

      for (Node node = familyElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "device") {
            String name        = element.getAttribute("name");
            String manual      = element.getAttribute("manual");
            String packageName = element.getAttribute("package");
            factory.createDeviceInformation(name, manual, packageName);
         }
         else {
            throw new RuntimeException("Unexpected field in FAMILYT, value = \'"+element.getTagName()+"\'");
         }
      }
   }

   /**
    * Parse &lt;package&gt;
    * 
    * @param packageElement
    */
   private void parsePackage(Element packageElement) {
      
      String packageName = packageElement.getAttribute("name");
      
      for (Node node = packageElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName()  == "placement") {
            PinInformation pin           = factory.findPin(element.getAttribute("pin"));
            String         location      = element.getAttribute("location");
            DevicePackage  devicePackage = factory.findDevicePackage(packageName);
            devicePackage.addPin(pin, location);
         }
         else {
            throw new RuntimeException("Unexpected field in PACKAGE, value = \'"+element.getTagName()+"\'");
         }
      }
   }

   /**
    * Parse &lt;packages&gt;
    * 
    * @param packagesElement
    */
   private void parsePackages(Element packagesElement) {
      
      for (Node node = packagesElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "package") {
            parsePackage(element);
         }
         else {
            throw new RuntimeException("Unexpected field in PACKAGES, value = \'"+element.getTagName()+"\'");
         }
      }
   }

   /**
    * Parses document from top element
    * @return 
    * 
    * @throws Exception
    */
   public DeviceInfo parseDocument(Path path) throws Exception {

      Document document = parseXmlFile(path);
      
      Element documentElement = document.getDocumentElement();

      if (documentElement == null) {
         System.out.println("Parser.parseDocument() - failed to get documentElement");
      }
      for (Node node = documentElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "family") {
            parseFamily(element);
         }
         else if (element.getTagName() == "pins") {
            parsePins(element);
         }
         else if (element.getTagName() == "packages") {
            parsePackages(element);
         }
         else if (element.getTagName() == "peripherals") {
         }
         else {
            throw new RuntimeException("Unexpected field in PIN, value = \'"+element.getTagName()+"\'");
         }
      }      
      return factory;
   }
}
