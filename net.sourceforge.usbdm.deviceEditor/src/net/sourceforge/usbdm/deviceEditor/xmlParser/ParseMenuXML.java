package net.sourceforge.usbdm.deviceEditor.xmlParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BinaryVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.NumericVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralConfigurationModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;
import net.sourceforge.usbdm.deviceEditor.peripherals.SelectionVariableModel;
import net.sourceforge.usbdm.jni.Usbdm;

public class ParseMenuXML extends XML_BaseParser {

   public static class Data {
      public final BaseModel        fRootModel;
      public final String               fTemplate;
      public final ArrayList<Validator> fValidators;
      
      public Data(BaseModel model, String template, ArrayList<Validator> validators) {
         fRootModel  = model;
         fTemplate   = template;
         fValidators = validators;
      }
   };
   
   private final PeripheralWithState fProvider;
   
   /**
    * Parse &lt;intOption&gt; element<br>
    * 
    * @param longElement
    */
   private void parseLongOption(BaseModel parent, Element longElement) {

      String name        = longElement.getAttribute("name");
      String description = longElement.getAttribute("description");
      String toolTip     = longElement.getAttribute("toolTip").replaceAll("\\\\n", "\n");
      long   min         = getLongAttribute(longElement, "min");
      long   max         = getLongAttribute(longElement, "max");
      long   step        = getLongAttribute(longElement, "step");
      String value       = longElement.getAttribute("value");
      
      fProvider.createVariable(name, value);
      NumericVariableModel model = new NumericVariableModel(parent, fProvider, name, description);
      model.setToolTip(toolTip);
      model.setMin(min);
      model.setMax(max);
      model.setStep(step);
   }

   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param choiceElement
    */
   private void parseChoiceOption(BaseModel parent, Element choiceElement) {
      String  name        = choiceElement.getAttribute("name");
      String  description = choiceElement.getAttribute("description");
      String  toolTip     = choiceElement.getAttribute("toolTip").replaceAll("\\\\n", "\n");

      ArrayList<String> choices = new ArrayList<String>();
      ArrayList<String> values  = new ArrayList<String>();
      String defaultValue = null;
      for (Node node = choiceElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "choice") {
            String value = element.getAttribute("value");
            choices.add(element.getAttribute("name"));
            values.add(value);
            if (defaultValue == null) {
               defaultValue = value;
            }
            if (element.getAttribute("isDefault").equalsIgnoreCase("true")) {
               defaultValue = value;
            }
         }
         else {
            throw new RuntimeException("Unexpected field in <choiceOption>, value = \'"+element.getTagName()+"\'");
         }
      }
      fProvider.createVariable(name, defaultValue);
      String theChoices[] = choices.toArray(new String[choices.size()]);
      String theValues[]  = values.toArray(new String[values.size()]);
      SelectionVariableModel model = new SelectionVariableModel(parent, fProvider, name, description);
      model.setToolTip(toolTip);;
      model.setChoices(theChoices);
      model.setValues(theValues);
   }
   
   /**
    * Class representing a validator parameter
    */
   static class Param {
   };
   
   static class LongParam extends Param {
      long   fValue;
      /**
       * COnstruct parameter with Long value
       * @param value
       */
      public LongParam(long value) {
         fValue = value;
      }
   };
   
   static class StringParam extends Param {
      String   fValue;
      /**
       * COnstruct parameter with String value
       * @param value
       */
      public StringParam(String value) {
         fValue = value;
      }
   };
   
   static class Validator {
      String fClassName;
      ArrayList<Param> params = new ArrayList<Param>();
      /**
       * Construct validator
       * 
       * @param className Name of class
       */
      public Validator(String className) {
         fClassName = className;
      }
      /**
       * Add parameter to validator
       * 
       * @param param
       */
      void addParam(Param param) {
         params.add(param);
      }
   }
   
   /**
    * Parse &lt;validate&gt; element<br>
    * 
    * @param validateElement
    */
   private static Validator parseValidate(Element validateElement) {
      Validator validator = new Validator(validateElement.getAttribute("class"));
      for (Node node = validateElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "param") {
            String  type   = element.getAttribute("type");
            if (type.equalsIgnoreCase("int")) {
               validator.addParam(new LongParam(getLongAttribute(element, "value")));
            }
            else if (type.equalsIgnoreCase("string")) {
               validator.addParam(new LongParam(getLongAttribute(element, "value")));
            }
            else {
               throw new RuntimeException("Unexpected type in <validate>, value = \'"+element.getTagName()+"\'");
            }
         }
         else {
            throw new RuntimeException("Unexpected field in <validate>, value = \'"+element.getTagName()+"\'");
         }
      }
      return validator;
   }

   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param binaryElement
    */
   private void parseBinaryOption(BaseModel parent, Element binaryElement) {
      String name        = binaryElement.getAttribute("name");
      String description = binaryElement.getAttribute("description");
      String toolTip     = binaryElement.getAttribute("toolTip").replaceAll("\\\\n", "\n");

      ArrayList<String> choices = new ArrayList<String>();
      ArrayList<String> values  = new ArrayList<String>();
      String defaultValue = null;
      for (Node node = binaryElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "choice") {
            String value = element.getAttribute("value");
            choices.add(element.getAttribute("name"));
            values.add(value);
            if (defaultValue == null) {
               defaultValue = value;
            }
            if (element.getAttribute("isDefault").equalsIgnoreCase("true")) {
               defaultValue = value;
            }
         }
         else {
            throw new RuntimeException("Unexpected field in <binaryOption>, value = \'"+element.getTagName()+"\'");
         }
      }
      if ((choices.size()==0)||(choices.size()>2)) {
         throw new RuntimeException("Wrong number of choices in <binaryOption>, value = "+choices.size());
      }
      fProvider.createVariable(name, defaultValue);
      
      BinaryVariableModel model = new BinaryVariableModel(parent, fProvider, name, description);
      model.setName(name);
      model.setToolTip(toolTip);
      model.setValue0(choices.get(0), values.get(0));
      model.setValue1(choices.get(1), values.get(1));
   }

   /**
    * Parse &lt;menu&gt; element<br>
    * 
    * @param menuElement
    * 
    * @throws Exception 
    */
   private BaseModel parsePageOrMenu(BaseModel parent, Element menuElement) throws Exception {

      BaseModel rootModel = null;

      String name = menuElement.getAttribute("name");
      if (name.equalsIgnoreCase("_instance")) {
         name = fProvider.getName();
      }
      String description = menuElement.getAttribute("description");
      if (menuElement.getTagName() == "page") {
         rootModel = new PeripheralConfigurationModel(parent, name, description);
      }
      else {
         rootModel = new CategoryModel(parent, name, description);
      }
      for (Node node = menuElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "menu") {
            parsePageOrMenu(rootModel, element);
         }
         else if (element.getTagName() == "intOption") {
            parseLongOption(rootModel, element);
         }
         else if (element.getTagName() == "binaryOption") {
            parseBinaryOption(rootModel, element);
         }
         else if (element.getTagName() == "choiceOption") {
            parseChoiceOption(rootModel, element);
         }
         else {
            throw new RuntimeException("Unexpected field in <menu>, value = \'"+element.getTagName()+"\'");
         }
      }
      return rootModel;
   }

   /**
    * 
    * @param provider
    */
   public ParseMenuXML(PeripheralWithState provider) {
      fProvider = provider;
   }
   
   /**
    * 
    * @param document
    * @param parent
    * @param provider
    * @return
    * @throws Exception
    */
   private static Data parse(Document document, BaseModel parent, PeripheralWithState provider) throws Exception {
      
      Element documentElement = document.getDocumentElement();

      if (documentElement == null) {
         throw new Exception("Failed to get documentElement");
      }
      
      BaseModel            rootModel   = null;
      String               template    = null;
      ArrayList<Validator> validators  = new ArrayList<Validator>();

      ParseMenuXML parser = new ParseMenuXML(provider);
      for (Node node = documentElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if ((element.getTagName() == "page") || (element.getTagName() == "menu")) {
            if (rootModel != null) {
               throw new RuntimeException("Multiple <page> or <menu> elements");
            }
            rootModel = parser.parsePageOrMenu(parent, element);
         }
         else if (element.getTagName() == "validate") {
            parseValidate(element);
         }
         else if (element.getTagName() == "template") {
            template = element.getTextContent().replaceAll("^\n\\s*","").replaceAll("(\\\\n|\\n)\\s*", "\n").replaceAll("\\\\t","   ");
         }
         else {
            throw new RuntimeException("Unexpected field in ROOT, value = \'"+element.getTagName()+"\'");
         }
      }
      return new Data(rootModel, template, validators);
   }
   
   /**
    * Parses document from top element
    * @param models 
    * @param deviceInfo 
    * @return 
    * 
    * @throws Exception
    */
   private static Data parseFile(Path path, BaseModel parent, PeripheralWithState provider) throws Exception {

      if (!path.toFile().exists()) {
         // Look in USBDM directory
         path = Paths.get(Usbdm.getUsbdmResourcePath()).resolve(path);
      }
      if (!path.toFile().exists()) {
         throw new RuntimeException("Unable to locate hardware description file " + path);
      }
      return parse(XML_BaseParser.parseXmlFile(path), parent, provider);
   }

   /**
    * Parses document from top element
    * @param models 
    * @param deviceInfo 
    * @return 
    * 
    * @throws Exception
    */
   public static Data parseFile(String name, BaseModel parent, PeripheralWithState provider) {
      
      // For debug try local directory
      Path path = Paths.get("hardware/peripherals").resolve(name+".xml");
      if (Files.isRegularFile(path)) {
         path = path.toAbsolutePath();
//         System.err.println("Opening debug file "+path);
      }
      else {
         path = Paths.get(DeviceInfo.USBDM_HARDWARE_LOCATION+"/peripherals/"+name+".xml");
      }
      try {
         return parseFile(path, parent, provider);
      } catch (Exception e) {
         BaseModel model = new BaseModel(null, name, "Failed to parse "+path) {
            @Override
            protected void removeMyListeners() {
            }
         };
         e.printStackTrace();
         return new Data(model, null, null);
      }
   }

   /**
    * Parses document from top element
    * @param models 
    * @param deviceInfo 
    * @return 
    * 
    * @throws Exception
    */
   public static Data parseString(String xmlString, BaseModel parent, PeripheralWithState provider) throws Exception {
      return parse(XML_BaseParser.parseXmlString(xmlString, Paths.get("")), parent, provider);
   }
}
