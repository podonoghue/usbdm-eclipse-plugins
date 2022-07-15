package net.sourceforge.usbdm.deviceEditor.parsers;

import java.nio.file.Path;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.Mode;
import net.sourceforge.usbdm.deviceEditor.information.DevicePackage;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class ParseFamilyXML extends XML_BaseParser {

   /** Device Information */
   private DeviceInfo fDeviceInfo = null;

   /**
    * Parse &lt;pin&gt;
    * 
    * @param pinElement
    * @throws Exception 
    */
   private void parsePin(Element pinElement) throws Exception {

      Pin pin = fDeviceInfo.createPin(pinElement.getAttribute("name"));
      for (Node node = pinElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "mux") {
//            factory.createMapping(factory.findSignal(element.getAttribute("signal")), pin, MuxSelection.valueOf(element.getAttribute("sel")));
            Signal       signal       = fDeviceInfo.findOrCreateSignal(element.getAttribute("signal"));
            MuxSelection muxSelection = MuxSelection.valueOf(element.getAttribute("sel"));
            fDeviceInfo.createMapping(signal, pin, muxSelection);
            if (signal.getName().startsWith("GPIO")) {
               pin.setPort(signal);
            }
         }
         else if (element.getTagName() == "reset") {
            MuxSelection muxSelection = MuxSelection.valueOf(element.getAttribute("sel"));
            pin.setResetValue(muxSelection);
         }
         else {
            throw new Exception("Unexpected field in PIN, value = \'"+element.getTagName()+"\'");
         }
      }
   }

   /**
    * Parse &lt;pins&gt;
    * 
    * @param pinsElement
    * @throws Exception 
    */
   private void parsePins(Element pinsElement) throws Exception {

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
            throw new Exception("Unexpected field in PINS, value = \'"+element.getTagName()+"\'");
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
   private void parseFamily(Element familyElement) throws Exception {

      fDeviceInfo.initialiseTemplates();

      String familyName = familyElement.getAttribute("name");
      fDeviceInfo.setDeviceSubFamily(familyName);
      
      for (Node node = familyElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "device") {
            fDeviceInfo.parseDeviceInformationXML(element);
         }
         else {
            throw new Exception("Unexpected field in FAMILY, value = \'"+element.getTagName()+"\'");
         }
      }
   }

   /**
    * Parse &lt;package&gt;
    * 
    * @param packageElement
    * @throws Exception 
    */
   private void parsePackage(Element packageElement) throws Exception {

      String packageName = packageElement.getAttribute("name");

      for (Node node = packageElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName()  == "placement") {
            String pinName = element.getAttribute("pin");
            Pin pin           = fDeviceInfo.findPin(pinName);
            String         location      = element.getAttribute("location");
            DevicePackage  devicePackage = fDeviceInfo.findDevicePackage(packageName);
            devicePackage.addPin(pin, location);
         }
         else {
            throw new Exception("Unexpected field in PACKAGE, value = \'"+element.getTagName()+"\'");
         }
      }
   }

   /**
    * Parse &lt;packages&gt;
    * 
    * @param packagesElement
    * @throws Exception 
    */
   private void parsePackages(Element packagesElement) throws Exception {

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
            throw new Exception("Unexpected field in PACKAGES, value = \'"+element.getTagName()+"\'");
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
            throw new Exception("Unexpected field in PERIPHERALS, value = \'"+element.getTagName()+"\'");
         }
      }
   }

   private void parseSignals(Element signalsElement) throws Exception {
      for (Node node = signalsElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "signal") {
            parseSignal(element);
         }
         else {
            throw new Exception("Unexpected field in SIGNALS, value = \'"+element.getTagName()+"\'");
         }
      }
   }

   private void parseSignal(Element peripheralElement) throws Exception {
      String signalName     = peripheralElement.getAttribute("name");
      String peripheralName = peripheralElement.getAttribute("peripheral");

      fDeviceInfo.findPeripheral(peripheralName, Mode.fail);
      fDeviceInfo.findOrCreateSignal(signalName);
   }


   private void parsePeripheral(Element peripheralElement) throws Exception {
      String baseName = peripheralElement.getAttribute("baseName");
      String instance = peripheralElement.getAttribute("instance");
      String version  = peripheralElement.getAttribute("version");

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
               throw new Exception("Peripheral already created");
            }
            peripheral = fDeviceInfo.createPeripheral(baseName, instance, element.getAttribute("class"), element.getAttribute("parameters"));
         }
         else if (element.getTagName() == "clock") {
            if (peripheral==null) {
               peripheral = fDeviceInfo.createPeripheral(baseName, instance);
            }
            peripheral.setClockControlInfo(element.getAttribute("clockEnable"), element.getAttribute("clockDisable"));
         }
         else if (element.getTagName() == "irq") {
            peripheral.addIrqNum(element.getAttribute("num"));
         }
         else if (element.getTagName() == "dma") {
            peripheral.addDmaChannel(getIntAttribute(element,"num"), element.getAttribute("source"));
         }
         else if (element.getTagName() == "param") {
            String name  = element.getAttribute("name");
            String key   = element.getAttribute("key");
            String value = element.getAttribute("value");
            if (name.isBlank()) {
               String[] pathElements = key.split("/");
               name = pathElements[pathElements.length-1];
            }
            PeripheralWithState periph = (PeripheralWithState) peripheral;
            StringVariable var = new StringVariable(name, periph.makeKey(key), value);
            var.setDefault(value);
            periph.addVariable(var);
            periph.addParam(key);
//            if (key.equals(periph.makeKey("version"))) {
//               // Override peripheral file
//               version = value;
//            }
         }
         else {
            throw new Exception("Unexpected field in PERIPHERAL, value = \'"+element.getTagName()+"\'");
         }
      }
      if (!version.isBlank()) {
         peripheral.setPeripheralVersion(version);
      }
   }

   /**
    * Parses Device <b>xxx.usbdmHardware</b> document from top element
    * 
    * @param deviceInfo
    *  
    * @return 
    * 
    * @throws Exception
    */
   public void parseFile(DeviceInfo deviceInfo, Path path) throws Exception {
      fDeviceInfo = deviceInfo;
      Document document = parseXmlFile(path);

      Element documentElement = document.getDocumentElement();

      if (documentElement == null) {
         throw new Exception("Failed to get documentElement for " + path);
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
         else if (element.getTagName() == "peripherals") {
            parsePeripherals(element);
         }
         else if (element.getTagName() == "signals") {
            parseSignals(element);
         }
         else if (element.getTagName() == "pins") {
            parsePins(element);
         }
         else if (element.getTagName() == "packages") {
            parsePackages(element);
         }
         else {
            throw new Exception("Unexpected field in ROOT, value = \'"+element.getTagName()+"\'");
         }
      }      
      for (String key:fDeviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(key);
         peripheral.addLinkedSignals();
      }
      fDeviceInfo.consistencyCheck();
   }

}
