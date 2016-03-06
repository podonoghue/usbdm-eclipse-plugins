package net.sourceforge.usbdm.dialogues;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Test extends XML_BaseParser {

   static final String ROOT_TAG        = "root";
   static final String PAGE_TAG        = "page";
   static final String LABEL_TAG       = "label";
   static final String BUTTON_TAG      = "button";
   static final String GROUP_TAG       = "group";
   static final String COMBO_TAG       = "combo";
   static final String LAYOUT_TAG      = "layout";
   static final String CHECKBOX_TAG    = "checkbox";
   static final String RADIO_TAG       = "radio";
   static final String CHOICES_TAG     = "choices";
   static final String TITLE_ATTR      = "title";

   static final String NAME_ATTR       = "name";
   static final String TYPE_ATTR       = "type";
   static final String LAYOUT_ATTR     = "layout";
   static final String COLUMNS_ATTR    = "columns";
   static final String ROWS_ATTR       = "rows";
   static final String DIRECTION_ATTR  = "direction";
   static final String BORDER_ATTR     = "border";
   static final String SELECTION_ATTR  = "selection";

   /**
    * Parses document from top element
    * 
    * @throws Exception
    */
   private static DynamicDialogue parseDocument(Path path) throws Exception {
      
      Document document = parseXmlFile(path);
      
      Element documentElement = document.getDocumentElement();

      if (documentElement == null) {
         System.out.println("DeviceDatabase.parseDocument() - failed to get documentElement");
      }
      DynamicDialogue dialogue = new DynamicDialogue();
      
      String name = documentElement.getAttribute(NAME_ATTR);
      if (name != null) {
         dialogue.setName(name);
      }
      for (Node node = documentElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == PAGE_TAG) {
            dialogue.addPage(parsePage(element));
         }
         else {
            throw new Exception(String.format("Unexpected Tag value \'%s\', in \'%s\'", element.getTagName(), "root"));
         }
      }
      return dialogue;
   }

   private static DynamicPage parsePage(Element element) throws Exception {
      
      DynamicPage page = new DynamicPage();
      page.setName(element.getAttribute(NAME_ATTR));
      page.setTitle(element.getAttribute(TITLE_ATTR));
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element childElement = (Element) node;
         if (childElement.getTagName() == GROUP_TAG) {
            page.addControl(parseGroup(childElement));
         }
         else {
            throw new Exception(String.format("Unexpected Tag value \'%s\', in \'%s\'", childElement.getTagName(), element.getTagName()));
         }
      }
      return page;
   }

   private static DynamicLayout parseLayout(Element element) throws Exception {

      DynamicLayout layout = new DynamicLayout(element.getAttribute(TYPE_ATTR));
      if (element.hasAttribute(COLUMNS_ATTR)) {
         layout.setColumns((int)getIntAttribute(element, COLUMNS_ATTR));
      }
      if (element.hasAttribute(ROWS_ATTR)) {
         layout.setRows((int)getIntAttribute(element, ROWS_ATTR));
      }
      if (element.hasAttribute(DIRECTION_ATTR)) {
         layout.setDirection(element.getAttribute(DIRECTION_ATTR));
      }
      return layout;
   }

   private static DynamicGroup parseGroup(Element element) throws Exception {
      
      DynamicGroup composite = new DynamicGroup();
      composite.setName(element.getAttribute(NAME_ATTR));
      composite.setTitle(element.getAttribute(TITLE_ATTR));
      composite.setBorder(element.getAttribute(BORDER_ATTR));
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element childElement = (Element) node;
         if (childElement.getTagName() == LAYOUT_TAG) {
            composite.setLayout(parseLayout(childElement));
         }
         else if (childElement.getTagName() == LABEL_TAG) {
            composite.addControl(parseLabel(childElement));
         }
         else if (childElement.getTagName() == BUTTON_TAG) {
            composite.addControl(parseButton(childElement));
         }
         else if (childElement.getTagName() == COMBO_TAG) {
            composite.addControl(parseCombo(childElement));
         }
         else if (childElement.getTagName() == RADIO_TAG) {
            composite.addControl(parseRadio(childElement));
         }
         else if (childElement.getTagName() == CHECKBOX_TAG) {
            composite.addControl(parseCheckbox(childElement));
         }
         else if (childElement.getTagName() == GROUP_TAG) {
            composite.addControl(parseGroup(childElement));
         }
         else {
            throw new Exception(String.format("Unexpected Tag value \'%s\', in \'%s\'", childElement.getTagName(), element.getTagName()));
         }
      }
      return composite;
   }

   private static DynamicCheckbox parseCheckbox(Element element) {
      DynamicCheckbox control = new DynamicCheckbox();
      control.setName(element.getAttribute(NAME_ATTR));
      control.setTitle(element.getAttribute(TITLE_ATTR));
      return control;
   }

   private static DynamicRadio parseRadio(Element element) {
      DynamicRadio control = new DynamicRadio();
      control.setName(element.getAttribute(NAME_ATTR));
      control.setTitle(element.getAttribute(TITLE_ATTR));
      return control;
   }

   private static DynamicControl parseLabel(Element element) {
      
      DynamicControl control = new DynamicLabel();
      control.setName(element.getAttribute(NAME_ATTR));
      control.setTitle(element.getAttribute(TITLE_ATTR));
      return control;
   }

   private static DynamicCombo parseCombo(Element element) throws Exception {
      
      DynamicCombo control = new DynamicCombo();
      control.setName(element.getAttribute(NAME_ATTR));
      control.setTitle(element.getAttribute(TITLE_ATTR));
      control.setSelection((int)getIntAttribute(element, SELECTION_ATTR));
      String[] choices = element.getTextContent().split(",");
      for (String s:choices) {
         control.addChoice(s);
      }
      return control;
   }

   private static DynamicControl parseButton(Element element) {
      
      DynamicControl control = new DynamicButton();
      control.setName(element.getAttribute(NAME_ATTR));
      control.setTitle(element.getAttribute(TITLE_ATTR));
      return control;
   }

   public static void main(String[] args) {         

      DynamicDialogue dialogue = null;
      try {
         dialogue = parseDocument(Paths.get("data/FTM.xml"));
      } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      if (dialogue == null) {
         System.err.println("Empty document");
      }

      dialogue.writeSVD(System.out);
         
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setText("Task List - TableViewer Example");
      shell.setLayout(new FillLayout());

      dialogue.construct(shell);

      shell.setBackground(new Color(display, 255, 0, 0));

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();

   }

}
