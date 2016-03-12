package net.sourceforge.usbdm.dialogues;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Test extends XML_BaseParser {

   static final String ROOT_TAG        = "root";
   static final String DEVICE_TAG      = "device";
   static final String PIN_TAG         = "pin";
   static final String MUX_TAG         = "mux";
   static final String PACKAGE_TAG     = "package";
   static final String PERIPHERAL_TAG  = "peripheral";
   static final String SIGNAL_TAG      = "signal";
   
   static final String NAME_ATTR       = "name";
   static final String TITLE_ATTR      = "title";
   static final String MUX_ATTR        = "mux";
   static final String PKG_ATTR        = "pkgd";
   static final String PIN_ATTR        = "pin";

   /**
    * Parses document from top element
    * 
    * @throws Exception
    */
   private static Device parseDocument(Path path) throws Exception {
      
      Document document = parseXmlFile(path);
      
      Element documentElement = document.getDocumentElement();

      if (documentElement == null) {
         System.out.println("DeviceDatabase.parseDocument() - failed to get documentElement");
      }
      for (Node node = documentElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == DEVICE_TAG) {
            return parseDevice(element);
         }
         else {
            throw new Exception(String.format("Unexpected Tag value \'%s\', in \'%s\'", documentElement.getTagName(), element.getTagName()));
         }
      }
      return null;
   }

   /**
    * Parses document from top element
    * 
    * @throws Exception
    */
   private static Device parseDevice(Element element) throws Exception {
      
      String name  = element.getAttribute(NAME_ATTR);
      String title = element.getAttribute(TITLE_ATTR);
      Device device = new Device(name, title);
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element childElement = (Element) node;
         if (childElement.getTagName() == PIN_TAG) {
            device.addPin(parsePin(childElement));
         }
         else if (childElement.getTagName() == PERIPHERAL_TAG) {
            device.addPeripheral(parsePeripheral(childElement));
         }
         else {
            throw new Exception(String.format("Unexpected Tag value \'%s\', in \'%s\'", childElement.getTagName(), element.getTagName()));
         }
      }
      return device;
   }

   private static Pin parsePin(Element element) throws Exception {
      
      String name  = element.getAttribute(NAME_ATTR);
      Pin pin = Pin.factory(name);
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element childElement = (Element) node;
         if (childElement.getTagName() == MUX_TAG) {
            pin.addMux(parseMux(childElement));
         }
         else if (childElement.getTagName() == PACKAGE_TAG) {
            pin.addPackage(parsePackage(childElement));
         }
         else {
            throw new Exception(String.format("Unexpected Tag value \'%s\', in \'%s\'", childElement.getTagName(), element.getTagName()));
         }
      }
      return pin;
   }

   private static Package parsePackage(Element childElement) {
      return Package.factory(childElement.getAttribute(PKG_ATTR), childElement.getAttribute(PIN_ATTR));
   }

   private static Mux parseMux(Element element) throws Exception {
      return new Mux(Signal.factory(element.getAttribute(SIGNAL_TAG)), MuxSelection.valueOf((int)getIntAttribute(element,MUX_ATTR)));
   }

   private static Peripheral parsePeripheral(Element element) throws Exception {
      
      String name  = element.getAttribute(NAME_ATTR);
      Peripheral pin = new Peripheral(name);
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element childElement = (Element) node;
         if (childElement.getTagName() == SIGNAL_TAG) {
            pin.addSignal(Signal.factory(childElement.getAttribute(NAME_ATTR)));
         }
         else {
            throw new Exception(String.format("Unexpected Tag value \'%s\', in \'%s\'", childElement.getTagName(), element.getTagName()));
         }
      }
      return pin;
   }

   public static void main(String[] args) {         

      Device device = null;
      try {
         device = parseDocument(Paths.get("data/MK20DX.xml"));
      } catch (Exception e) {
         e.printStackTrace();
      }

      if (device == null) {
         System.err.println("Empty document");
         return;
      }

      device.writeSVD(System.out);
      Signal.list(System.out);
      Pin.list(System.out);
      Package.list(System.out);
      
//      Display display = new Display();
//
//      Shell shell = new Shell(display);
//      shell.setText("Task List - TableViewer Example");
//      shell.setLayout(new FillLayout());
//
//      dialogue.construct(shell);
//
//      shell.setBackground(new Color(display, 255, 0, 0));
//
//      shell.open();
//      while (!shell.isDisposed()) {
//         if (!display.readAndDispatch())
//            display.sleep();
//      }
//      display.dispose();

   }

}
