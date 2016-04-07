package net.sourceforge.usbdm.deviceEditor.xmlParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DevicePackage;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Pin;

public class ParseFamilyXML extends XML_BaseParser {

   /** Device Information */
   private DeviceInfo factory = null;

   /**
    * Parse &lt;pin&gt;
    * 
    * @param pinElement
    * @throws Exception 
    */
   private void parsePin(Element pinElement) {

      Pin pin = factory.createPin(pinElement.getAttribute("name"));

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
            MuxSelection muxSelection = MuxSelection.valueOf(element.getAttribute("sel"));
            pin.setResetValue(muxSelection);
         }
         else if (element.getTagName() == "default") {
            pin.setDefaultValue(MuxSelection.valueOf(element.getAttribute("sel")));
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
    * 
    * @throws Exception 
    */
   private void parseFamily(Path path, Element familyElement) throws Exception {

      factory = new DeviceInfo(path);
      factory.initialiseTemplates();

      String familyName = familyElement.getAttribute("name");
      factory.setFamilyName(familyName);
      
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
            Pin pin           = factory.findPin(element.getAttribute("pin"));
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

   private void parsePeripherals(Element peripheralsElement) throws Exception {
      for (Node node = peripheralsElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "peripheral") {
            parsePeripheral(element);
         }
         else {
            throw new RuntimeException("Unexpected field in PERIPHERALS, value = \'"+element.getTagName()+"\'");
         }
      }
   }

   // Peripherals not implied by the pins
   ArrayList<Pattern> predefinedPeripherals = new ArrayList<Pattern>();
   
   private void parsePeripheral(Element peripheralElement) throws Exception {
      if (predefinedPeripherals.size()==0) {
         predefinedPeripherals.add(Pattern.compile("(DMAMUX)(\\d*)"));
         predefinedPeripherals.add(Pattern.compile("(PIT)(\\d*)"));
      }
      String baseName = peripheralElement.getAttribute("baseName");
      String instance = peripheralElement.getAttribute("instance");

      Peripheral peripheral = null;
      
      for (Node node = peripheralElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "handler") {
            if (peripheral!=null) {
               throw new RuntimeException("Peripheral already created");
            }
            peripheral = factory.createPeripheral(baseName, instance, element.getAttribute("class"), element.getAttribute("parameters"));
         }
         else if (element.getTagName() == "clock") {
            if (peripheral==null) {
               peripheral = factory.createPeripheral(baseName, instance);
            }
            peripheral.setClockInfo(element.getAttribute("clockReg"), element.getAttribute("clockMask"));
         }
         else if (element.getTagName() == "irq") {
            peripheral.addIrqNum(element.getAttribute("num"));
         }
         else if (element.getTagName() == "dma") {
            parseDma(element, peripheral);
         }
         else {
            throw new RuntimeException("Unexpected field in PERIPHERAL, value = \'"+element.getTagName()+"\'");
         }
      }
   }

   private void parseDma(Element dmaElement, Peripheral peripheral) {
      if (predefinedPeripherals.size()==0) {
         predefinedPeripherals.add(Pattern.compile("(DMAMUX)(\\d*)"));
         predefinedPeripherals.add(Pattern.compile("(PIT)(\\d*)"));
      }
      for (Node node = dmaElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "slot") {
            peripheral.addDmaChannel(getIntAttribute(element,"num"), element.getAttribute("source"));
         }
         else {
            throw new RuntimeException("Unexpected field in DMA, value = \'"+element.getTagName()+"\'");
         }
      }
   
   }

   /**
    * Parses document from top element
    * @return 
    * 
    * @throws Exception
    */
   public DeviceInfo parseFile(Path path) throws Exception {

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
            parseFamily(path, element);
         }
         else if (element.getTagName() == "pins") {
            parsePins(element);
         }
         else if (element.getTagName() == "packages") {
            parsePackages(element);
         }
         else if (element.getTagName() == "peripherals") {
            parsePeripherals(element);
         }
         else {
            throw new RuntimeException("Unexpected field in ROOT, value = \'"+element.getTagName()+"\'");
         }
      }      
      factory.consistencyCheck();
      return factory;
   }

}
