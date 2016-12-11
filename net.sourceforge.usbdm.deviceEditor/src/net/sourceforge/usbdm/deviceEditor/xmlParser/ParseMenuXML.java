package net.sourceforge.usbdm.deviceEditor.xmlParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable;
import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.NumericListVariable;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PinListVariable;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.Pair;
import net.sourceforge.usbdm.deviceEditor.information.Variable.Units;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BooleanVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.ParametersModel;
import net.sourceforge.usbdm.deviceEditor.model.SectionModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.deviceEditor.model.TabModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.packageParser.PackageParser;
import net.sourceforge.usbdm.packageParser.ProjectAction;
import net.sourceforge.usbdm.packageParser.ProjectActionList;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Value;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor;
import net.sourceforge.usbdm.packageParser.ProjectConstant;

public class ParseMenuXML extends XML_BaseParser {

   public final static String RESOURCE_PATH = "Stationery/Packages/180.ARM_Peripherals";

   public static class Data {
      public final BaseModel                    fRootModel;
      public final Map<String, CodeTemplate>    fTemplate;
      public final ArrayList<Validator>         fValidators;
      public final ProjectActionList            fProjectActionList;
      public Data(BaseModel model, HashMap<String, CodeTemplate> templates, ArrayList<Validator> validators, ProjectActionList projectActionList) {
         fRootModel  = model;
         fTemplate   = templates;
         if (validators == null) {
            // Empty list rather than null
            fValidators = new ArrayList<Validator>();
         }
         else {
            fValidators = validators;
         }
         fProjectActionList = projectActionList;
      }
   }

   /** Name of model (filename) */
   @SuppressWarnings("unused")
   private static String fName;
   
   /** Provider providing the variables used by the menu */
   private final PeripheralWithState  fProvider;
   
   /** Used to build the template */
   private final Map<String,StringBuilder>  fTemplates   = new HashMap<String,StringBuilder>();
   
   /** Used to record template dimensions */
   private final Map<String,Variable> fTemplateDimensions   = new HashMap<String,Variable>();
   
   /** Holds the validators found */
   private final ArrayList<Validator> fValidators = new ArrayList<Validator>();

   /** Actions associated with this Menu */
   private final ProjectActionList fProjectActionList;
   
   /** Used to record the first model encountered */
   BaseModel fRootModel = null;

   /** Used to record the Pins model */
   private CategoryModel fPinModel;

   /**
    * 
    * @param provider
    */
   private ParseMenuXML(PeripheralWithState provider) {
      fProvider = provider;
      fProjectActionList = new ProjectActionList(provider.getName()+" Action list");
   }

   /**
    * Gets the toolTip attribute from the element and applies some simple transformations
    *  
    * @param element
    * 
    * @return Formatted toolTip
    */
   String getToolTip(Element element) {
      return element.getAttribute("toolTip").replaceAll("\\\\n( +)", "\n");
   }
   
   /**
    * Parse &lt;intOption&gt; element<br>
    * 
    * @param varElement
    */
   private void parseLongOption(BaseModel parent, Element varElement) {

      String  name        = varElement.getAttribute("name");
      String  key         = varElement.getAttribute("key");
      if (key.isEmpty()) {
         key = fProvider.makeKey(name);
      }
      key  = substituteKey(key);
      name = substituteKey(name);
      boolean isConstant  = Boolean.valueOf(varElement.getAttribute("constant"));
      String  description = varElement.getAttribute("description");
      String  toolTip     = getToolTip(varElement);
      long    step        = getLongAttribute(varElement, "step");
      long    offset      = getLongAttribute(varElement, "offset");
      String  value       = varElement.getAttribute("value");
      String  units       = varElement.getAttribute("units");
      
      LongVariable variable = new LongVariable(name, key);
      fProvider.addVariable(variable);
      variable.setDescription(description);
      variable.setToolTip(toolTip);
      variable.setDerived(Boolean.valueOf(varElement.getAttribute("derived")));
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(varElement.getAttribute("origin"));
      }
      try {
         if (varElement.hasAttribute("min")) {
            variable.setMin(getLongAttribute(varElement, "min"));
         }
         if (varElement.hasAttribute("max")) {
            variable.setMax(getLongAttribute(varElement, "max"));
         }
      } catch( NumberFormatException e) {
         throw new RuntimeException("Illegal min/max value in " + name, e);
      }
      variable.setValue(value);
      variable.setUnits(Units.valueOf(units));
      variable.setStep(step);
      variable.setOffset(offset);

      VariableModel model = variable.createModel(parent);
      model.setConstant(isConstant);
   }

   /**
    * Parse &lt;intOption&gt; element<br>
    * 
    * @param varElement
    */
   private void parseBitmaskOption(BaseModel parent, Element varElement) {

      String  name        = varElement.getAttribute("name");
      String  key         = varElement.getAttribute("key");
      if (key.isEmpty()) {
         key = fProvider.makeKey(name);
      }
      key  = substituteKey(key);
      name = substituteKey(name);
      boolean isConstant  = Boolean.valueOf(varElement.getAttribute("constant"));
      String  description = varElement.getAttribute("description");
      String  toolTip     = getToolTip(varElement);
      String  value       = varElement.getAttribute("value");
      
      BitmaskVariable variable = new BitmaskVariable(name, key);
      fProvider.addVariable(variable);
      variable.setDescription(description);
      variable.setToolTip(toolTip);
      variable.setDerived(Boolean.valueOf(varElement.getAttribute("derived")));
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(varElement.getAttribute("origin"));
      }
      try {
         variable.setPermittedBits(getLongAttribute(varElement, "bitmask"));
         variable.setBitList(varElement.getAttribute("bitList"));
      } catch( NumberFormatException e) {
         throw new RuntimeException("Illegal permittedBits value in " + name, e);
      }
      variable.setValue(value);

      VariableModel model = variable.createModel(parent);
      model.setConstant(isConstant);
   }

   /**
    * Parse &lt;intOption&gt; element<br>
    * 
    * @param varElement
    */
   private void parseDoubleOption(BaseModel parent, Element varElement) {

      String  name        = varElement.getAttribute("name");
      String  key         = varElement.getAttribute("key");
      if (key.isEmpty()) {
         key = fProvider.makeKey(name);
      }
      key  = substituteKey(key);
      name = substituteKey(name);
      boolean isConstant  = Boolean.valueOf(varElement.getAttribute("constant"));
      String  description = varElement.getAttribute("description");
      String  toolTip     = getToolTip(varElement);
      String  value       = varElement.getAttribute("value");
      String  units       = varElement.getAttribute("units");
      
      DoubleVariable variable = new DoubleVariable(name, key);
      fProvider.addVariable(variable);
      variable.setDescription(description);
      variable.setToolTip(toolTip);
      variable.setDerived(Boolean.valueOf(varElement.getAttribute("derived")));
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(varElement.getAttribute("origin"));
      }
      try {
         variable.setMin(getLongAttribute(varElement, "min"));
      } catch( NumberFormatException e) {
      }
      try {
         variable.setMax(getLongAttribute(varElement, "max"));
      } catch( NumberFormatException e) {
      }
      variable.setValue(value);
      variable.setUnits(Units.valueOf(units));
      VariableModel model = variable.createModel(parent);
      model.setConstant(isConstant);
   }

   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseChoiceOption(BaseModel parent, Element varElement) throws Exception {
      String  name        = varElement.getAttribute("name");
      String  key         = varElement.getAttribute("key");
      if (key.isEmpty()) {
         key = fProvider.makeKey(name);
      }
      key  = substituteKey(key);
      name = substituteKey(name);
      boolean isConstant  = Boolean.valueOf(varElement.getAttribute("constant"));
      String  description = varElement.getAttribute("description");
      String  toolTip     = getToolTip(varElement);

//      if (name.startsWith("adc_cfg1_adiclk")) {
//         System.err.println("Name = "+ name);
//      }
      Variable variable = new ChoiceVariable(name, key);
      fProvider.addVariable(variable);
      variable.setDescription(description);
      variable.setToolTip(toolTip);
      variable.setDerived(Boolean.valueOf(varElement.getAttribute("derived")));
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(varElement.getAttribute("origin"));
      }
      parseChoices(variable, varElement);

      VariableModel model = variable.createModel(parent);
      model.setName(name);
      model.setConstant(isConstant);
   }
   
   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseStringOption(BaseModel parent, Element varElement) throws Exception {
      String  name        = varElement.getAttribute("name");
      String  key         = varElement.getAttribute("key");
      if (key.isEmpty()) {
         key = fProvider.makeKey(name);
      }
      key  = substituteKey(key);
      name = substituteKey(name);
      boolean isConstant  = Boolean.valueOf(varElement.getAttribute("constant"));
      String  description = varElement.getAttribute("description");
      String  value       = varElement.getAttribute("value");
      String  toolTip     = getToolTip(varElement);

      Variable variable = new StringVariable(name, key);
      variable.setDescription(description);
      variable.setToolTip(toolTip);
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(varElement.getAttribute("origin"));
      }
      fProvider.addVariable(variable);
      VariableModel model = variable.createModel(parent);
      model.setName(name);
      model.setConstant(isConstant);
      
      variable.setValue(value);
   }
   
   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseNumericListOption(BaseModel parent, Element varElement) throws Exception {
      String  name        = varElement.getAttribute("name");
      String  key         = varElement.getAttribute("key");
      if (key.isEmpty()) {
         key = fProvider.makeKey(name);
      }
      key  = substituteKey(key);
      name = substituteKey(name);
      boolean isConstant  = Boolean.valueOf(varElement.getAttribute("constant"));
      String  description = varElement.getAttribute("description");
      String  value       = varElement.getAttribute("value");
      String  toolTip     = getToolTip(varElement);

      NumericListVariable variable = new NumericListVariable(name, key);
      variable.setDescription(description);
      variable.setToolTip(toolTip);
      variable.setDerived(Boolean.valueOf(varElement.getAttribute("derived")));
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(varElement.getAttribute("origin"));
      }
      try {
         if (varElement.hasAttribute("min")) {
            variable.setMin(getLongAttribute(varElement, "min"));
         }
         if (varElement.hasAttribute("max")) {
            variable.setMax(getLongAttribute(varElement, "max"));
         }
         if (varElement.hasAttribute("size")) {
            variable.setMaxListLength(getLongAttribute(varElement, "size"));
         }
      } catch( NumberFormatException e) {
         throw new RuntimeException("Illegal min/max value in " + name, e);
      }
      
      fProvider.addVariable(variable);
      VariableModel model = variable.createModel(parent);
      model.setName(name);
      model.setConstant(isConstant);
      
      variable.setValue(value);
   }
   
   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parsePinListOption(BaseModel parent, Element varElement) throws Exception {
      String  name        = varElement.getAttribute("name");
      String  key         = varElement.getAttribute("key");
      if (key.isEmpty()) {
         key = fProvider.makeKey(name);
      }
      key  = substituteKey(key);
      name = substituteKey(name);
      boolean isConstant  = Boolean.valueOf(varElement.getAttribute("constant"));
      String  description = varElement.getAttribute("description");
      String  value       = varElement.getAttribute("value");
      String  toolTip     = getToolTip(varElement);

      PinListVariable variable = new PinListVariable(fProvider, name, key);
      variable.setDescription(description);
      variable.setToolTip(toolTip);
      variable.setDerived(Boolean.valueOf(varElement.getAttribute("derived")));
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(varElement.getAttribute("origin"));
      }
      try {
         if (varElement.hasAttribute("size")) {
            variable.setMaxListLength(getLongAttribute(varElement, "size"));
         }
      } catch( NumberFormatException e) {
         throw new RuntimeException("Illegal min/max value in " + name, e);
      }
      fProvider.addVariable(variable);
      VariableModel model = variable.createModel(parent);
      model.setName(name);
      model.setConstant(isConstant);
      variable.setValue(value);
   }
   
   /**
    * Does some simple substitutions on the key
    *  "$(_instance)" => fProvider.getInstance()
    *  "$(_name)"     => fProvider.getName()
    * 
    * @param key
    * 
    * @return
    */
   private String substituteKey(String key) {
      return key.replaceAll("\\$\\(_instance\\)", fProvider.getInstance()).replaceAll("\\$\\(_name\\)", fProvider.getName());
   }

   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param stringElement
    * @throws Exception 
    */
   private void parseAliasOption(BaseModel parent, Element stringElement) throws Exception {
      // Key and name are interchangeable
      // Name is an ID and can be used for validation checks within the file.
      // Key is used to refer to external variable without validation error
      String  name         = stringElement.getAttribute("name");
      String  key          = stringElement.getAttribute("key");
      if (key.isEmpty()) {
         key = name;
      }
      key  = substituteKey(key);
      name = substituteKey(name);
      if (name.isEmpty()) {
         name = key;
      }
      key = fProvider.makeKey(key);
      
      boolean isConstant  = Boolean.valueOf(stringElement.getAttribute("constant"));
      boolean isOptional  = Boolean.valueOf(stringElement.getAttribute("optional"));
      Variable variable = fProvider.safeGetVariable(key);
      if (variable == null) {
         if (!isOptional) {
            throw new RuntimeException("Alias not found for " + key);
         }
         return;
      }
      VariableModel model = variable.createModel(parent);
      model.setConstant(isConstant);
   }
   
   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseBinaryOption(BaseModel parent, Element varElement) throws Exception {
      String  name        = varElement.getAttribute("name");
      String  key         = varElement.getAttribute("key");
      if (key.isEmpty()) {
         key = fProvider.makeKey(name);
      }
      key  = substituteKey(key);
      name = substituteKey(name);
      boolean isConstant  = Boolean.valueOf(varElement.getAttribute("constant"));
      String  description = varElement.getAttribute("description");
      String  toolTip     = getToolTip(varElement);

      BooleanVariable variable = new BooleanVariable(name, key);
      variable.setDescription(description);
      variable.setToolTip(toolTip);
      variable.setDerived(Boolean.valueOf(varElement.getAttribute("derived")));
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(varElement.getAttribute("origin"));
      }
      parseChoices(variable, varElement);
      
      fProvider.addVariable(variable);
      BooleanVariableModel model = new BooleanVariableModel(parent, variable);
      model.setConstant(isConstant);
   }
   
   /**
    * 
    * @param parentModel
    * @param element
    */
   private void parseConstant(BaseModel parentModel, Element element) {
      String name       = element.getAttribute("name");
      String id         = element.getAttribute("id");
      String key        = element.getAttribute("key");
      String value      = element.getAttribute("value");
      // Accept either key or id (prefer key)
      if (key.isEmpty()) {
         key = id;
      }
      if (key.isEmpty()) {
         key = fProvider.makeKey(name);
      }
      if (name.isEmpty()) {
         name = key;
      }
      key  = substituteKey(key);
      name = substituteKey(name);
      StringVariable var = new StringVariable(name, key);
      fProvider.addVariable(var);
      var.setValue(value);
   }

   private void parseControlItem(Element element) throws Exception {
      
      if (element.getTagName() == "fragment") {
         for (Node node = element.getFirstChild();
               node != null;
               node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
               continue;
            }
            parseControlItem((Element) node);
         }
      }
      else if (element.getTagName() == "validate") {
         fValidators.add(parseValidate(element));
      }
      else if (element.getTagName() == "template") {
         String templateName = element.getAttribute("name");
         Variable dimension = null;
         if (element.hasAttribute("dim")) {
            String dimName = element.getAttribute("dim");
            String key = fProvider.makeKey(dimName);
            dimension = fProvider.safeGetVariable(key);
            if (dimension == null) {
               throw new RuntimeException("Alias not found for " + key);
            }
         }
         addTemplate(templateName, dimension,
               element.getTextContent().
               replaceAll("^\n\\s*","").
               replaceAll("(\\\\n|\\n)\\s*", "\n").
               replaceAll("\\\\t","   "));
//         System.err.println(fTemplate.toString().substring(0, 40)+"\n");
      }
      else if (element.getTagName() == "projectActionList") {
         ProjectActionList pal = PackageParser.parseRestrictedProjectActionList(element, RESOURCE_PATH);
         pal.visit(new Visitor() {
            @Override
            public Result applyTo(ProjectAction action, Value result, IProgressMonitor monitor) {
               if (action instanceof ProjectConstant) {
                  ProjectConstant constant = (ProjectConstant) action;
                  Variable var = new StringVariable(constant.getId(), constant.getId());
                  var.setValue(constant.getValue());
                  System.err.println("Adding " + var);
                  fProvider.addVariable(var);
               }
               return Visitor.CONTINUE;
            }}, null);
         fProjectActionList.addProjectAction(pal);
      }
      else {
         throw new RuntimeException("Unexpected field in parseChildModels(), value = \'"+element.getTagName()+"\'");
      }

   }
   /**
    * Parses the children of this element
    * 
    * @param  parentModel  Model to attach children to
    * @param  menuElement  Menu element to parse
    * 
    * @throws Exception
    */
   void parseChildModels(BaseModel parentModel, Element menuElement) throws Exception {
      
      for (Node node = menuElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         
         //         System.err.println("parseChildModels(): " + element.getTagName() + ", " + element.getAttribute("name"));
         if (element.getTagName() == "fragment") {
            parseChildModels(parentModel, element);
         }
         else if (element.getTagName() == "category") {
            String name        = element.getAttribute("name");
            String description = element.getAttribute("description");
            String toolTip     = element.getAttribute("toolTip");
            BaseModel model = new CategoryModel(parentModel, name, description);
            model.setToolTip(toolTip);
            parseChildModels(model, element);
         }
//         else if (element.getTagName() == "section") {
//            String name        = element.getAttribute("name");
//            String toolTip     = element.getAttribute("toolTip");
//            BaseModel model = new SectionModel(parentModel, name, toolTip);
//            parseChildModels(model, element);
//         }
//         else if (element.getTagName() == "tab") {
//            String name        = element.getAttribute("name");
//            String toolTip     = element.getAttribute("toolTip");
//            BaseModel model = new TabModel(parentModel, name, toolTip);
//            parseChildModels(model, element);
//         }
         else if (element.getTagName() == "intOption") {
            parseLongOption(parentModel, element);
         }
         else if (element.getTagName() == "bitmaskOption") {
            parseBitmaskOption(parentModel, element);
         }
         else if (element.getTagName() == "floatOption") {
            parseDoubleOption(parentModel, element);
         }
         else if (element.getTagName() == "binaryOption") {
            parseBinaryOption(parentModel, element);
         }
         else if (element.getTagName() == "choiceOption") {
            parseChoiceOption(parentModel, element);
         }
         else if (element.getTagName() == "stringOption") {
            parseStringOption(parentModel, element);
         }
         else if (element.getTagName() == "numericListOption") {
            parseNumericListOption(parentModel, element);
         }
         else if (element.getTagName() == "pinListOption") {
            parsePinListOption(parentModel, element);
         }
         else if (element.getTagName() == "aliasOption") {
            parseAliasOption(parentModel, element);
         }
         else if (element.getTagName() == "constant") {
            parseConstant(parentModel, element);
         }
         else if (element.getTagName() == "section") {
            BaseModel model = new ParametersModel(parentModel, "Title", "Section");
            parseChildModels(model, element);
         }
         else if (element.getTagName() == "tab") {
            BaseModel model = new ParametersModel(parentModel, "Title", "Section");
            parseChildModels(model, element);
         }
         else if (element.getTagName() == "signals") {
            parseSignalsOption(parentModel, element);
         }
         else if (element.getTagName() == "fragment") {
            parseChildModels(parentModel, element);
         }
         else {
            parseControlItem( element);
         }
      }
   }
   
   /**
    * Add template<br>
    * If the template exists then the text is appended otherwise it is created.
    * 
    * @param key        Key used to index templates
    * @param dimension  Dimension for array templates
    * @param contents   Text for template
    * 
    * @throws Exception 
    */
   private void addTemplate(String key, Variable dimension, String contents) throws Exception {
      StringBuilder sb = fTemplates.get(key);
      if (sb == null) {
         sb = new StringBuilder();
         fTemplates.put(key, sb);
      }
      if (dimension != null) {
         if (fTemplateDimensions.put(key, dimension) != null) {
            throw new Exception("Template has multiple dimensions");
         }
      }
      sb.append(contents);
   }

   /**
    * Parse the pin associated with the peripheral
    * 
    * @param parentModel
    * @param element
    */
   private void parseSignalsOption(BaseModel parentModel, Element element) {
      Peripheral peripheral = fProvider;
      String peripheralName = element.getAttribute("name");
      if (!peripheralName.isEmpty()) {
         peripheral = fProvider.getDeviceInfo().getPeripherals().get(peripheralName);
      }
      if (peripheral != null) {
         if (fPinModel == null) {
            fPinModel = new CategoryModel(parentModel, "Signals", "Signals for this peripheral");
         }
         TreeMap<String, Signal> peripheralSignals = peripheral.getSignals();
         for (String signalName:peripheralSignals.keySet()) {
            Signal signal = peripheralSignals.get(signalName);
            if (signal.isAvailableInPackage()) {
               new SignalModel(fPinModel, signal);
            }
         }
      }
   }

   /**
    * Parses the children of this element
    * 
    * @param  parentModel  Model to attach children to
    * @param  menuElement  Menu element to parse
    * 
    * @throws Exception
    */
   void parseChoices(Variable variable, Element menuElement) throws Exception {
      ArrayList<Pair> entries = new ArrayList<Pair>();
      String defaultValue = null;

      for (Node node = menuElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "category") {
         }
         else if (element.getTagName() == "intOption") {
         }
         else if (element.getTagName() == "binaryOption") {
         }
         else if (element.getTagName() == "choiceOption") {
         }
         else if (element.getTagName() == "stringOption") {
         }
         else if (element.getTagName() == "aliasOption") {
         }
         else if (element.getTagName() == "choice") {
            Pair entry = new Pair(element.getAttribute("name"), element.getAttribute("value"));
            entries.add(entry);
            if (defaultValue == null) {
               defaultValue = entry.name;
            }
            if (element.getAttribute("isDefault").equalsIgnoreCase("true")) {
               defaultValue = entry.name;
            }
         }
         else if (element.getTagName() == "signals") {
         }
         else {
            throw new RuntimeException("Unexpected field in <menu>, value = \'"+element.getTagName()+"\'");
         }
      }
      if (variable instanceof BooleanVariable) {
         if ((entries.size()==0)||(entries.size()>2)) {
            throw new RuntimeException("Wrong number of choices in <binaryOption>, value = "+entries.size());
         }
         BooleanVariable var = (BooleanVariable) variable;
         var.setFalseValue(entries.get(0));
         var.setTrueValue(entries.get(1));
      }
      else if (variable instanceof ChoiceVariable) {      
         Pair theEntries[] = entries.toArray(new Pair[entries.size()]);
         ChoiceVariable var = (ChoiceVariable)variable;
         var.setData(theEntries);
      }
      variable.setDefault(defaultValue);
      variable.setValue(defaultValue);
   }

   public static class Validator {
      private String            fClassName;
      private ArrayList<Object> fParams = new ArrayList<Object>();
      
      /**
       * Construct validator
       * 
       * @param className Name of class
       */
      Validator(String className) {
         fClassName = className;
      }
      /**
       * Add parameter to validator
       * 
       * @param param
       */
      void addParam(Object param) {
         fParams.add(param);
      }
 
      /**
       * Get list of parameters
       * 
       * @return
       */
      public ArrayList<Object> getParams() {
         return fParams;
      }
      
      /** 
       * Get class name of validator
       * 
       * @return
       */
      public String getClassName() {
         return fClassName;
      }
   }
   
   /**
    * Parse &lt;validate&gt; element<br>
    * 
    * @param validateElement
    */
   private Validator parseValidate(Element validateElement) {
//      System.err.println("================================");
      Map<String, String> paramMap = fProvider.getParamMap();
//      for (String k:paramMap.keySet()) {
//         System.err.println(k + " => " + paramMap.get(k));
//      }
//      System.err.println("================================");
      Validator validator = new Validator(validateElement.getAttribute("class"));
      for (Node node = validateElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "param") {
            String  type = element.getAttribute("type");
            String value = fProvider.substitute(element.getAttribute("value"), paramMap);
            if (type.equalsIgnoreCase("long")) {
               validator.addParam(EngineeringNotation.parseAsLong(value));
            }
            else if (type.equalsIgnoreCase("string")) {
               validator.addParam(value);
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
   
//   /**
//    * Parse: <br>
//    *    &lt;fragment&gt;<br>
//    *    &lt;devicePage&gt;<br>
//    *    &lt;menu&gt;<br>
//    * 
//    * @param menuElement
//    * 
//    * @throws Exception 
//    */
//   private void parseSections(BaseModel parent, Element sectionsElement) throws Exception {
//      for (Node node = sectionsElement.getFirstChild();
//            node != null;
//            node = node.getNextSibling()) {
//         if (node.getNodeType() != Node.ELEMENT_NODE) {
//            continue;
//         }
//         Element element = (Element) node;
//         if (element.getTagName() == "section") {
//            BaseModel model = new ParametersModel(parent, "Title", "Section");
//            parseChildModels(model, element);
//         }
//         else if (element.getTagName() == "tab") {
//            BaseModel model = new TabModel(parent, "Title", "Tab");
//            parseChildModels(model, element);
//         }
//         else {
//            throw new RuntimeException("Unexpected field in <sections>, value = \'"+element.getTagName()+"\'");
//         }
//      }
//   }
   
   /**
    * Parse: <br>
    *    &lt;devicePage&gt;<br>
    * 
    * @param menuElement
    * 
    * @throws Exception 
    */
   private BaseModel parseSectionsOrOther(BaseModel parent, Element topElement) throws Exception {
      
      String name = topElement.getAttribute("name");
      if (name.equalsIgnoreCase("_instance")) {
         name = fProvider.getName();
      }
      String description = topElement.getAttribute("description");
      String toolTip     = getToolTip(topElement);

      BaseModel model = null;

      for (Node node = topElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "section") {
            if (model == null) {
               model = new SectionModel(parent, name, toolTip);
            }
            parseSectionsOrOther(model, element);
         }
         else if (element.getTagName() == "tab") {
            if (model == null) {
               model = new TabModel(parent, name, toolTip);
            }
            parseSectionsOrOther(model, element);
         }
         else {
            if (model == null) {
               model = new ParametersModel(parent, name, description);
               parseChildModels(model, topElement);
               break;
            }
            else {
               parseControlItem(element);
            }
         }
      }
      return model;
   }
   
   /**
    * Parse: <br>
    *    &lt;fragment&gt;<br>
    *    &lt;devicePage&gt;<br>
    * 
    * @param menuElement
    * 
    * @throws Exception 
    */
   private void parsePage(BaseModel parent, Element element) throws Exception {
      String name = element.getAttribute("name");
      if (name.equalsIgnoreCase("MCG")) {
         System.err.println("MCG");
      }
      if (element.getTagName() == "fragment") {
         for (Node node = element.getFirstChild();
               node != null;
               node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
               continue;
            }
            parsePage(parent, (Element) node);
         }
      }
      else {
         fRootModel = parseSectionsOrOther(null, element);
      }
   }
   
   public static class CodeTemplate {
      private final String   fTemplate;
      private final Variable fDimension;
      
      public CodeTemplate(String   template, Variable dimension) {
         fTemplate  = template;
         fDimension = dimension;
      }

      /**
       * Get template string
       * 
       * @return String
       */
      public String getTemplate() {
         return fTemplate;
      }

      /**
       * Get template dimension variable
       * 
       * @return Variable or null if not array
       */
      public Variable getDimension() {
         return fDimension;
      }
      
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
      ParseMenuXML parser = new ParseMenuXML(provider);
      for (Node child = document.getFirstChild(); child != null; child = child.getNextSibling()) {
         if (child.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) child;
//         System.err.println("parse(): " + element.getTagName() + ", " + element.getAttribute("name"));
         parser.parsePage(parent, element);
      }
      if (parser.fRootModel == null) {
         throw new Exception("No <devicePage> found in XML");
      }
      HashMap<String, CodeTemplate> templates = new HashMap<String, CodeTemplate>();
      for (String key:parser.fTemplates.keySet()) {
         templates.put(key, new CodeTemplate(parser.fTemplates.get(key).toString(), parser.fTemplateDimensions.get(key)));
      }
      return new Data(parser.fRootModel, templates, parser.fValidators, parser.fProjectActionList);
   }
   
   /**
    * Parses document from top element
    * 
    * @param path       Path to model
    * @param parent     Parent for model
    * @param provider   Provider for variables used etc.
    * 
    * @return Data from document
    * @throws Exception 
    */
   private static Data parseFile(Path path, BaseModel parent, PeripheralWithState provider) throws Exception {
      if (!path.toFile().exists()) {
         // Look in USBDM directory
         path = Paths.get(Usbdm.getUsbdmResourcePath()).resolve(path);
      }
      if (!path.toFile().exists()) {
         throw new Exception("Unable to locate hardware description file " + path);
      }
      return parse(XML_BaseParser.parseXmlFile(path), parent, provider);
   }

   /**
    * Parses document from top element
    * 
    * @param name       Name of model (filename)
    * @param parent     Parent for model
    * @param provider   Provider for variables used etc.
    * 
    * @return  Data from model
    * @throws Exception 
    */
   public static Data parseFile(String name, BaseModel parent, PeripheralWithState provider) throws Exception {
      fName = name;
      try {
         // For debug try local directory
         Path path = Paths.get("hardware/peripherals").resolve(name+".xml");
         if (Files.isRegularFile(path)) {
            path = path.toAbsolutePath();
         }
         else {
            path = Paths.get(DeviceInfo.USBDM_HARDWARE_LOCATION+"/peripherals/"+name+".xml");
         }
         return parseFile(path, parent, provider);
      } catch (Exception e) {
         throw new Exception("Failed to parse "+name, e);
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
