package net.sourceforge.usbdm.deviceEditor.parsers;

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable;
import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.CategoryVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.NumericListVariable;
import net.sourceforge.usbdm.deviceEditor.information.PinListVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.ChoiceData;
import net.sourceforge.usbdm.deviceEditor.information.Variable.Units;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;
import net.sourceforge.usbdm.deviceEditor.model.AliasPlaceholderModel;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.ErrorModel;
import net.sourceforge.usbdm.deviceEditor.model.ParametersModel;
import net.sourceforge.usbdm.deviceEditor.model.SectionModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;
import net.sourceforge.usbdm.deviceEditor.validators.PeripheralValidator;
import net.sourceforge.usbdm.deviceEditor.validators.Validator;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.packageParser.PackageParser;
import net.sourceforge.usbdm.packageParser.ProjectAction;
import net.sourceforge.usbdm.packageParser.ProjectActionList;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Value;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor;
import net.sourceforge.usbdm.packageParser.ProjectConstant;

public class ParseMenuXML extends XML_BaseParser {

   public final static String RESOURCE_PATH = "Stationery/Packages/180.ARM_Peripherals";

//   private static class TemplateIteration {
//      final String fVariable;
//      final String fEnumeration[];
//      
//      TemplateIteration(String variable, String enumeration) {
//         fVariable    = variable;
//         fEnumeration = enumeration.split("\\s*,\\s*");
//      }
//
//      public String getVariable() {
//         return fVariable;
//      }
//
//      public String[] getEnumeration() {
//         return fEnumeration;
//      }
//      
//      public String toString() {
//         StringBuilder sb = new StringBuilder();
//         for(String s : fEnumeration) {
//            sb.append(s);
//            sb.append(", ");
//         }
//         return "[" + fVariable + "= " + sb.toString() + "]";
//      }
//   }
   
   public static class MenuData {
      private final BaseModel                                     fRootModel;
      private final Map<String, ArrayList<TemplateInformation>>   fTemplatesList;
      private final ArrayList<ValidatorInformation>               fValidators;
      private final ProjectActionList                             fProjectActionList;
      
      /**
       * Create Menu data for peripheral
       * 
       * @param model               Display model
       * @param templateInfos       Associated templates (hashed by namespace and key)
       * @param validators          Associated validators
       * @param projectActionList   Associated action lists
       */
      public MenuData(BaseModel model, Map<String, ArrayList<TemplateInformation>> templateInfos, ArrayList<ValidatorInformation> validators, ProjectActionList projectActionList) {
         fRootModel  = model;
         fTemplatesList  = templateInfos;
         if (validators == null) {
            // Empty list rather than null
            fValidators = new ArrayList<ValidatorInformation>();
         }
         else {
            fValidators = validators;
         }
         fProjectActionList = projectActionList;
      }
      
      /**
       * Combines key and namespace to generate a unique key
       * 
       * @param key
       * @param namespace
       * 
       * @return
       */
      static public String makeKey(String key, String namespace) {
         if (!namespace.equals("all")) {
            key = namespace+"."+key;
         }
         return key;
      }
      
      /**
       * Indicates if the template key is absolute rather than relative to current peripheral
       * Note a template key encodes both namespace and name
       * 
       * @param key  Key to check
       * 
       * @return True is absolute
       */
      static public boolean isKeyAbsolute(String key) {
         return key.startsWith("/") && !key.endsWith(".");
      }
      
      /**
       * Get validators
       * 
       * @return
       */
      public ArrayList<ValidatorInformation> getValidators() {
         return fValidators;
      }
      
      /**
       * Get Action list
       * 
       * @return
       */
      public ProjectActionList getProjectActionList() {
         return fProjectActionList;
      }
      
      /**
       * Get root model
       * 
       * @return
       */
      public BaseModel getRootModel() {
         return fRootModel;
      }

      /**
       * Get map of all templates
       * 
       * @return
       */
      public Map<String, ArrayList<TemplateInformation>> getTemplates() {
         return fTemplatesList;
      }
      
      /**
       * Get template with given key in the given namespace
       * 
       * @param namespace  Namespace "info", "class", "usbdm", "all"
       * @param key        Key for template (may be "")
       * 
       * @return template value or empty string
       */
      public String getTemplate(String namespace, String key) {
         key = makeKey(key, namespace);
         ArrayList<TemplateInformation> templateList = fTemplatesList.get(key);
         if (templateList == null) {
            return "";
         }
         StringBuilder sb = new StringBuilder();
         for(TemplateInformation template:templateList) {
            sb.append(template.getExpandedText());
         }
         return sb.toString();
      }

      /**
       * Instantiates any aliases in the model
       * 
       * @param provider   Provider for variables (usually peripheral)
       * 
       * @throws Exception 
       */
      public void instantiateAliases(VariableProvider provider) throws Exception {
         ParseMenuXML.instantiateAliases(provider, fRootModel);
      }

      public void prune() {
         fRootModel.prune();
      }
      
   }
   
   private static class ForLoop {
      
      static private class ForloopException extends RuntimeException {
         private static final long serialVersionUID = 1L;

         public ForloopException(String errorMessage) {
             super(errorMessage);
         }
      }
      
      // Keys to replace
      private final String[] fKeys;
      
      // Set of values to use in each iteration
      private final String[] fValueList;
      
      // Set of values for current iteration 
      private String[] fValues = null;
      
      // Iteration count 0..(fValueList-1)
      private int iter = 0;

      /**
       * Construct for-loop element to keep track of substitutions
       * 
       * @param keys    List of keys e.g. "keyA,keyB"
       * @param values  List of values e.g. "valA0,valB0;valA1,valB1;valA2,valB2"
       */
      public ForLoop(String keys, String values) {
         fKeys       = keys.split(",");
         fValueList  = values.split(";");
      }
      
      /**
       * Do for-loop substitutions on string
       * 
       * @param text Text to process
       * 
       * @return  Modified text
       * 
       * @throws ForloopException If iteration completed or if keys and values are unmatched in length for this iteration
       */
      public String doSubstitution(String text) throws ForloopException {
         if (iter>=fValueList.length) {
            throw new ForloopException("doSubstitution() called after for-loop completed");
         }
         if (fValues == null) {
            fValues = fValueList[iter].split(",");
         }
         if (fValues.length != fKeys.length) {
            throw new ForloopException(
               "Number of values " + fValueList[iter]+
               "does not match number of keys"+fKeys.length);
         }
         for (int index=0; index<fKeys.length; index++) {
            text = text.replace("%("+fKeys[index].trim()+")", fValues[index].trim());
         }
         return text;
      }
      
      public boolean next() throws ForloopException {
         if (iter>=fValueList.length) {
            throw new ForloopException("next() called after for-loop completed");
         }
         fValues = null;
         if (iter < fValueList.length) {
            iter++;
         }
         return (iter < fValueList.length);
      }
   }
   
   private static class ForStack {
      
      /// Stack for for-loops
      private Stack<ForLoop> forStack = new Stack<ForLoop>();
      
      /**
       * Apply for-loop substitutions
       * 
       * @param text  Text to be modified. May be null.
       * 
       * @return  Modified text
       * 
       * @throws ForloopException If iteration completed
       */
      public String doForSubstitutions(String text) throws ForLoop.ForloopException {
         if (text == null) {
            return null;
         }
         for (ForLoop forLoop:forStack) {
            text = forLoop.doSubstitution(text);
         }
         return text;
      }
      
      /**
       * Add for-loop level
       *  
       * @param keys    List of keys e.g. "keyA,keyB"
       * @param values  List of values e.g. "valA0,valB0;valA1,valB1;valA2,valB2"
       * 
       * @throws Exception If keys and values are unmatched
       */
      public void createLevel(String keys, String values) throws Exception {
         ForLoop loop = new ForLoop(keys, values);
         forStack.push(loop);
      }
      
      /**
       * Discard for-loop level
       * 
       * @throws {@link EmptyStackException}
       */
      public void dropLevel() {
         forStack.pop();
      }

      public boolean next() {
         return forStack.lastElement().next();
      }
   }
  
   /** Parser for template conditions */
   private final TemplateConditionParser fTemplateConditionParser;

   /** Provider providing the variables used by the menu */
   private final VariableProvider  fProvider;

   /** Peripheral to add vectors etc to */
   private final PeripheralWithState fPeripheral;

   /** 
    * Templates being accumulated.
    * This is a map using (key + namespace) as map key.
    * Multiple matching templates are kept in a list rather than combined (to allow individual iteration). 
    */
   private final Map<String, ArrayList<TemplateInformation>> fTemplateInfos = new HashMap<String, ArrayList<TemplateInformation>>();

   /** Holds the validators found */
   private final ArrayList<ValidatorInformation> fValidators = new ArrayList<ValidatorInformation>();

   /** Actions associated with this Menu */
   private final ProjectActionList fProjectActionList;

   /** Used to record the first model encountered */
   private BaseModel fRootModel = null;

   /** Stack for for-loops */
   private final ForStack fForStack = new ForStack();
   
   /**
    * Convenience method to apply for-loop substitutions
    * 
    * @param text  Text to be modified. May be null.
    * 
    * @return  Modified text
    * 
    * @throws Exception
    */
   String doForSubstitutions(String text) throws Exception {
      return fForStack.doForSubstitutions(text);
   }
   
   /**
    * Get an attribute and apply for-loop substitutions
    * 
    * @param element    Element to obtain attribute from
    * @param attrName   Name of attribute
    * 
    * @return  modified attribute
    * 
    * @throws Exception If for-loop completed  
    */
   String getAttributeWithFor(Element element, String attrName) throws Exception {
      return fForStack.doForSubstitutions(element.getAttribute(attrName));
   }
   
   private void parseForLoop(BaseModel parentModel, Element element) throws Exception {
      String keys   = element.getAttribute("keys");
      String values = element.getAttribute("values");
      String dim    = element.getAttribute("dim");
      if (!dim.isBlank()) {
         if (!values.isBlank()) {
            throw new Exception("Both values and dim attribute given in <for> '" + keys +"'");
         }
         Long dimension = getLongAttributeWithVariableSubstitution(element, "dim");
         StringBuilder sb = new StringBuilder();
         for (int index=0; index<dimension; index++) {
            sb.append(index+";");
         }
         values=sb.toString();
      }
      if (keys.isBlank() || values.isBlank()) {
         throw new Exception("<for> requires keys and values attributes '"+keys+"', '"+values+"'");
      }
      fForStack.createLevel(keys, values);
      do {
         parseSectionsOrOtherContents(parentModel, element);
      } while (fForStack.next());
      fForStack.dropLevel();
   }

   /**
    * Constructor for menu parser
    * 
    * @param provider      Provider for variables
    * @param peripheral    Peripheral associated with menu data
    */
   private ParseMenuXML(VariableProvider provider, PeripheralWithState peripheral) {

      fProvider   = provider;
      fPeripheral = peripheral;

      fProjectActionList = new ProjectActionList(provider.getName()+" Action list");

      fTemplateConditionParser = new TemplateConditionParser(provider);
   }

   /**
    * Get variable with given key from provider
    * 
    * @param key     Key to lookup variable
    * 
    * @return variable or null if not found
    */
   private Variable safeGetVariable(String key) {
      return fProvider.safeGetVariable(key);
   }
   
   /**
    * Get choice variable with given key from provider
    * 
    * @param key     Key to lookup variable
    * 
    * @return variable or null if not found
    * 
    * @throws Exception if variable exists but is of wrong type
    */
   @SuppressWarnings("unused")
   private ChoiceVariable safeGetChoiceVariable(String key) {
      return (ChoiceVariable) fProvider.safeGetVariable(key);
   }
   
   /**
    * Removes a variable from provider
    * 
    * @param variable  Variable to remove
    * 
    * @throws Exception if variable does not exist
    */
   private void removeVariable(Variable variable) {
      fProvider.removeVariable(variable);
   }
   
   /**
    * Obtain an integer value from an attribute <br>
    * If the attribute is not a simple integer then it is used as a lookup for a longVariable.
    * 
    * @param element Element to examine
    * @param name    Name of attribute to retrieve
    * 
    * @return  Long value from attribute
    * 
    * @throws Exception if value is invalid or not found (including variable lookup)
    */
   protected Long getLongAttributeWithVariableSubstitution(Element element, String name) throws Exception {
      
      Long value = safeGetLongAttributeWithVariableSubstitution(element, name);
      if (value == null) {
         throw new Exception("Invalid long attribute = " + element + ", name = '" + name + "'");
      }
      return value;
   }

   /**
    * Obtain an integer value from an attribute <br>
    * If the attribute is not a simple integer then it is used as a lookup for a longVariable.
    * 
    * @param element Element to examine
    * @param name    Name of attribute to retrieve
    * 
    * @return Long value from attribute/variable or null if not found
    * 
    * @throws NumberFormatException If attribute found but invalid
    * @throws Exception If for-loop completed  
    */
   protected Long  safeGetLongAttributeWithVariableSubstitution(Element element, String name) throws Exception {
      if (!element.hasAttribute(name)) {
         return null;
      }
      String attr = element.getAttribute(name);
      Long value = null;
      if (Character.isDigit(attr.charAt(0))) {
         return safeGetLongAttribute(element, name);
      }
      if (value == null) {
         // Try variable
         String varName = getAttributeWithFor(element, name);
         Variable var = safeGetVariable(varName);
         if (var != null) {
            value = var.getValueAsLong();
         }
      }
      return value;
   }
   
   /**
    * Gets the toolTip attribute from the element and applies some simple transformations
    *  
    * @param element
    * 
    * @return Formatted toolTip
    * @throws Exception 
    */
   private String getToolTip(Element element) throws Exception {
      String text = doForSubstitutions(element.getAttribute("toolTip"));
      return text.replaceAll("\\\\n( +)", "\n").replaceAll("\\\\t", "  ");
   }

   /**
    * 
    * @param varElement    Element to obtain attributes from
    * @param clazz         Class of variable to create
    * 
    * @return Variable created (or existing one)
    * @throws Exception 
    */
   private Variable createVariable(Element varElement, Class<?> clazz) throws Exception {

      String  name         = getAttributeWithFor(varElement, "name");
      String  key          = getAttributeWithFor(varElement, "key");

      boolean replace = Boolean.valueOf(varElement.getAttribute("replace"));
      boolean modify  = Boolean.valueOf(varElement.getAttribute("modify"));
      
      if (key.isEmpty()) {
         key = name;
      }
      if (name.isEmpty()) {
         name = key;
      }
      key  = substituteKey(key);
      name = substituteKey(name);
      
      key  = fProvider.makeKey(key);
      
      Variable newVariable      = null;
      Variable existingVariable = safeGetVariable(key);
      
      if ((existingVariable != null) && replace) {
         // Replacing existing variable - just delete the one found
         removeVariable(existingVariable);
         existingVariable = null;
      }
      if (existingVariable == null) {
         // New variable
         try {
            newVariable = (Variable) clazz.getConstructor(String.class, String.class).newInstance(name, key);
            fProvider.addVariable(newVariable);
         } catch (Exception e) {
            throw new Exception("Unable to create variable!", e);
         }
      }
      else {
         // Existing variable to be modified - must be same class
         if (!existingVariable.getClass().equals(clazz)) {
            throw new Exception("Overridden variable\n   "+existingVariable.toString()+" has wrong type");
         }
         if (!modify) {
            throw new Exception("Overriding variable without 'modify' attribute\n " + existingVariable);
         }
         newVariable = existingVariable;
      }
      return newVariable;
   }

   /**
    * Check if element has derivedFrom 
    * 
    * @param   varElement  Element to parse
    * 
    * @return  Derived from variable if it exists
    * @throws Exception 
    */
   private Variable getDerived(Element varElement) throws Exception {      
      Variable otherVariable = null;
      String derivedFromName = varElement.getAttribute("derivedFrom");
      if (!derivedFromName.isEmpty()) {
         if (derivedFromName.endsWith(".")) {
            derivedFromName = derivedFromName.substring(0, derivedFromName.length()-1);
         }
         derivedFromName = fProvider.makeKey(derivedFromName);
         otherVariable = safeGetVariable(derivedFromName);
         if (otherVariable == null) {
            throw new Exception("derivedFromName variable not found for " + derivedFromName);
         }
      }
      return otherVariable;
   }
   
   /**
    * Parse attributes common to most variables<br>
    * Also creates model.
    * Processes the following attributes:
    * <li>name
    * <li>key
    * <li>description
    * <li>toolTip (processed)
    * <li>constant
    * <li>derived
    * <li>derivedFrom
    * <li>origin
    * <li>hidden
    * 
    * @param parent        Parent for model
    * @param varElement    Element obtain attributes from
    * @param clazz         Class of variable to create
    * 
    * @return Variable created (or existing one)
    * @throws Exception 
    */
   private VariableModel parseCommonAttributes(BaseModel parent, Element varElement, Variable variable) throws Exception {
      
      Variable otherVariable = getDerived(varElement);
      
      if (otherVariable != null) {
         variable.setDescription(otherVariable.getDescription());
         variable.setToolTip(otherVariable.getToolTip());
         variable.setOrigin(otherVariable.getRawOrigin());
         variable.setLocked(otherVariable.isLocked());
         variable.setDerived(otherVariable.getDerived());
      }
      if (varElement.hasAttribute("description")) {
         variable.setDescription(getAttributeWithFor(varElement, "description"));
      }
      if (varElement.hasAttribute("default")) {
         variable.setDefault(getAttributeWithFor(varElement, "default"));
      }
      if (varElement.hasAttribute("toolTip")) {
         variable.setToolTip(getToolTip(varElement));
      }
      if (varElement.hasAttribute("value")) {
         // Value is used as default and initial value
         String value = getAttributeWithFor(varElement, "value");
         variable.setValue(value);
         variable.setDefault(value);
         variable.setDisabledValue(value);
      }
      if (varElement.hasAttribute("disabledValue")) {
         // Value is used as disabled value
         variable.setDisabledValue(getAttributeWithFor(varElement, "disabledValue"));
      }
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(getAttributeWithFor(varElement, "origin"));
      }
      if (varElement.hasAttribute("derived")) {
         variable.setDerived(Boolean.valueOf(getAttributeWithFor(varElement, "derived")));
      }
      if (varElement.hasAttribute("data")) {
         // Internal data vale
         String value = getAttributeWithFor(varElement, "data");
         variable.setDataValue(value);
      }
      if (varElement.hasAttribute("target")) {
         variable.setTarget(getAttributeWithFor(varElement, "target"));
      }
      if (varElement.hasAttribute("clockSources")) {
         throw new Exception("clockSources no longer supported in "+varElement+", '"+variable.getName()+"'");
      }
      NodeList forNodes = varElement.getElementsByTagName("for");
      if (forNodes.getLength() > 0) {
         throw new Exception ("<for> no longer supported here "+varElement);
//         Element forElement = (Element)forNodes.item(0);
//         String  forVariable = forElement.getAttribute("var");
//         String  enumeration = forElement.getAttribute("enumeration");
//         variable.addForIteration(forVariable, enumeration);
      }
      VariableModel model = variable.createModel(parent);
      model.setConstant(Boolean.valueOf(varElement.getAttribute("constant")));
      model.setHidden(Boolean.valueOf(varElement.getAttribute("hidden")));
      return model;
   }

   /**
    * Parse &lt;longOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseLongOption(BaseModel parent, Element varElement) throws Exception {

      LongVariable variable = (LongVariable) createVariable(varElement, LongVariable.class);

      LongVariable otherVariable = (LongVariable)getDerived(varElement);
      if (otherVariable != null) {
         variable.setUnits(otherVariable.getUnits());
         variable.setStep(otherVariable.getStep());
         variable.setOffset(otherVariable.getOffset());
         variable.setDefault(otherVariable.getDefault());
         variable.setValue(otherVariable.getValueAsLong());
         variable.setMin(otherVariable.getMin());
         variable.setMax(otherVariable.getMax());
         variable.setUnits(((LongVariable)otherVariable).getUnits());
      }
      VariableModel model = parseCommonAttributes(parent, varElement, variable);
      try {
         if (varElement.hasAttribute("min")) {
            variable.setMin(getLongAttribute(varElement, "min"));
         }
         if (varElement.hasAttribute("max")) {
            variable.setMax(getLongAttribute(varElement, "max"));
         }
      } catch( NumberFormatException e) {
         throw new Exception("Illegal min/max value in " + variable.getName(), e);
      }
      if (varElement.hasAttribute("units")) {
         variable.setUnits(Units.valueOf(varElement.getAttribute("units")));
      }
      if (varElement.hasAttribute("step")) {
         variable.setStep(getLongAttribute(varElement, "step"));
      }
      if (varElement.hasAttribute("offset")) {
         variable.setOffset(getLongAttribute(varElement, "offset"));
      }
      parseForElement(parent, model);
   }

   /**
    * Parse &lt;doubleOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseDoubleOption(BaseModel parent, Element varElement) throws Exception {

      DoubleVariable variable = (DoubleVariable) createVariable(varElement, DoubleVariable.class);
      
      Variable otherVariable = getDerived(varElement);
      if ((otherVariable != null) && (otherVariable instanceof DoubleVariable)) {
         variable.setMin(((DoubleVariable)otherVariable).getMin());
         variable.setMax(((DoubleVariable)otherVariable).getMax());
         variable.setUnits(((DoubleVariable)otherVariable).getUnits());
      }
      parseCommonAttributes(parent, varElement, variable).getVariable();

      try {
         if (varElement.hasAttribute("min")) {
            variable.setMin(getDoubleAttribute(varElement, "min"));
         }
         if (varElement.hasAttribute("max")) {
            variable.setMax(getDoubleAttribute(varElement, "max"));
         }
      } catch( NumberFormatException e) {
         throw new Exception("Illegal min/max value in " + variable.getName(), e);
      }
      if (varElement.hasAttribute("units")) {
         variable.setUnits(Units.valueOf(varElement.getAttribute("units")));
      }
   }

   /**
    * Parse &lt;bitmaskOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseBitmaskOption(BaseModel parent, Element varElement) throws Exception {

      BitmaskVariable variable = (BitmaskVariable) createVariable(varElement, BitmaskVariable.class);
      VariableModel   model    = parseCommonAttributes(parent, varElement, variable);
      try {
         variable.setPermittedBits(getLongAttribute(varElement, "bitmask"));
         variable.setBitList(varElement.getAttribute("bitList"));
      } catch( NumberFormatException e) {
         throw new Exception("Illegal permittedBits value in " + variable.getName(), e);
      }
      parseForElement(parent, model);
   }

   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseChoiceOption(BaseModel parent, Element varElement) throws Exception {

      ChoiceVariable variable = (ChoiceVariable) createVariable(varElement, ChoiceVariable.class);
      VariableModel  model    = parseCommonAttributes(parent, varElement, variable);
      parseChoices(variable, varElement);
      parseForElement(parent, model);
   }


   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseAddChoices(BaseModel parent, Element varElement) throws Exception {

      String key = getAttributeWithFor(varElement, "key");
      key  = substituteKey(key);
      key  = fProvider.makeKey(key);
      
      ChoiceVariable variable = (ChoiceVariable) safeGetChoiceVariable(key);
      if (variable == null) {
         throw new Exception("Cannot find target in <addChoice>, key='"+key+"'");
      }
      ChoiceInformation info = parseChoiceData(varElement);
      variable.addChoices(info.entries, info.defaultEntry);
   }

   /**
    * Parse &lt;StringOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseStringOption(BaseModel parent, Element varElement) throws Exception {
      
      StringVariable variable = (StringVariable) createVariable(varElement, StringVariable.class);
      parseCommonAttributes(parent, varElement, variable);
   }

   private void parseCategory(BaseModel parent, Element varElement) throws Exception {
//      CategoryVariable      variable = (CategoryVariable) createVariable(varElement, CategoryVariable.class);
//      CategoryVariableModel model    = (CategoryVariableModel) parseCommonAttributes(parent, varElement, variable);
      
      CategoryModel model = new CategoryModel(parent, varElement.getAttribute("name"));
      boolean hidden = Boolean.parseBoolean(varElement.getAttribute("hidden"));
      model.setHidden(hidden);
      parseChildModels(model, varElement);
      if ((model.getChildren()==null)||(model.getChildren().size() == 0)) {
         // Empty category - discard
         parent.removeChild(model);
         return;
      }
   }

   private void parseCategoryOption(BaseModel parent, Element varElement) throws Exception {

      CategoryVariable      categoryVariable = (CategoryVariable)      createVariable(varElement, CategoryVariable.class);
      CategoryVariableModel categoryModel    = (CategoryVariableModel) parseCommonAttributes(parent, varElement, categoryVariable);
      
      categoryVariable.setValue(varElement.getAttribute("value"));
      Long dimension = safeGetLongAttributeWithVariableSubstitution(varElement, "dim");

      if (dimension != null) {
         throw new Exception("Dimension no longer supported in '" + varElement+", '"+dimension+"'");
      }
      
      parseChildModels(categoryModel, varElement);

      if ((categoryModel.getChildren() == null) || (categoryModel.getChildren().size() == 0)) {
         // Empty category - discard
         parent.removeChild(categoryModel);
         return;
      }
   }
   
   private void parseAliasCategoryOption(BaseModel parent, Element varElement) throws Exception {
      AliasPlaceholderModel model = parseAliasOption(parent, varElement);

      parseChildModels(model, varElement);

      if ((model.getChildren() == null) || (model.getChildren().size() == 0)) {
         // Empty category - discard
         parent.removeChild(model);
         return;
      }
   }
   
   /**
    * Parse &lt;NumericListOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseNumericListOption(BaseModel parent, Element varElement) throws Exception {
      
      NumericListVariable variable = (NumericListVariable) createVariable(varElement, NumericListVariable.class);
      
      NumericListVariable otherVariable = (NumericListVariable)getDerived(varElement);
      if (otherVariable != null) {
         variable.setMin(otherVariable.getMin());
         variable.setMax(otherVariable.getMax());
         variable.setMaxListLength(otherVariable.getMaxListLength());
         variable.setMinListLength(otherVariable.getMinListLength());
      }
      VariableModel model = parseCommonAttributes(parent, varElement, variable);
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
         throw new Exception("Illegal min/max/size value in " + variable.getName(), e);
      }
      parseForElement(parent, model);
   }

   /**
    * Parse &lt;binaryOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseBinaryOption(BaseModel parent, Element varElement) throws Exception {

      Variable variable = createVariable(varElement, BooleanVariable.class);
      if (variable == null) {
         return;
      }
      VariableModel model = parseCommonAttributes(parent, varElement, variable);
      parseChoices(variable, varElement);
      parseForElement(parent, model);
   }

   /**
    * Parse<br>
    * <pre>
    *  &lt;for var="instance" enumeration="0, 1, 2, 3" />
    * </pre>
    * 
    * @param parent
    * @param variable
    * @param model
    * @throws Exception
    */
   private void parseForElement(BaseModel parent, VariableModel model) throws Exception {
      Variable variable    = model.getVariable();
      String   forVariable = variable.getForVariable();
      if (forVariable != null) {
         throw new Exception("<for> no longer supported here '" + parent +"', '"+model+"'");
//         ISubstitutionMap symbols = new SubstitutionMap();
//         String[] names = variable.getForEnumeration().split("\\s*,\\s*");
//         parent.removeChild(model);
//         for(String name:names) {
//            name = name.trim();
//            symbols.addValue(forVariable, name);
//            Variable iteratedVariable = variable.clone(name, symbols);
//            iteratedVariable.createModel(parent);
//            fProvider.addVariable(iteratedVariable);
//         }
//         fProvider.removeVariable(variable);
      }
}

   /**
    * Parse &lt;irqOption&gt; element<br>
    * Expects:
    * <ul>
    * <li> name         Name of option
    * <li> description  Description of option
    * <li> tooltip      Tooltip to display
    * <li> pattern      Pattern to match against vector table entry. <br>
    * This is a regex.  In addition the following substitutions are done before matching:
    *    <ul>
    *    <li> %i replaced with peripheral instance e.g. FTM1 => 1, PTA => A
    *    <li> %b replaced with peripheral base name e.g. FTM1 => = FTM
    *    <li> %c replaced with peripheral C++ base class name e.g. FTM1 => = Ftm
    *    <li> _IRQHandler is appended
    *    </ul>
    * <li> classHandler Name of class method to handle interrupt <br>
    * This is a regex substitution pattern.  In addition the following substitutions are done before matching:
    *    <ul>
    *    <li> %i replaced with peripheral instance e.g. FTM1 => 1, PTA => A
    *    <li> %b replaced with peripheral base name e.g. FTM1 => = FTM
    *    <li> %c replaced with peripheral C++ base class name e.g. FTM1 => = Ftm
    *    </ul>
    * Regex substitution patterns may also be used.
    *    <ul>
    *    <li> $n reference to regex group in pattern
    *    </ul>
    * </ul>
    * 
    * @param  irqElement
    * 
    * @throws Exception 
    */
   private void parseIrqOption(BaseModel parent, Element irqElement) throws Exception {
      
      IrqVariable variable = (IrqVariable) createVariable(irqElement, IrqVariable.class);
      parseCommonAttributes(parent, irqElement, variable).getVariable();

      variable.setPattern(irqElement.getAttribute("pattern"));
      variable.setClassHandler(irqElement.getAttribute("classHandler"));
      
      fPeripheral.addIrqVariable(variable);
   }

   /**
    * Parse &lt;PinListOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parsePinListOption(BaseModel parent, Element varElement) throws Exception {

      PinListVariable variable = (PinListVariable) createVariable(varElement, PinListVariable.class);
      VariableModel model = parseCommonAttributes(parent, varElement, variable);
      variable.setPeripheral(fPeripheral);
      try {
         if (varElement.hasAttribute("size")) {
            variable.setMaxListLength(getLongAttribute(varElement, "size"));
         }
      } catch( NumberFormatException e) {
         throw new Exception("Illegal size value in " + variable.getName(), e);
      }
      parseForElement(parent, model);
   }

   /**
    * Does some simple substitutions on the key
    *  "$(_name)"     => fProvider.getName()
    *  "$(_instance)" => fPeripheral.getInstance()
    * 
    * @param key Key to modify
    * 
    * @return modified key
    */
   private String substituteKey(String key) {
      key = key.replaceAll("\\$\\(_name\\)", fProvider.getName());
      if (fPeripheral != null) {
         key = key.replaceAll("\\$\\(_instance\\)", fPeripheral.getInstance());
      }
      return key;
   }

   /**
    * Parse &lt;aliasOption&gt; element<br>
    * 
    * @param stringElement
    * @throws Exception 
    */
   private AliasPlaceholderModel parseAliasOption(BaseModel parent, Element stringElement) throws Exception {
      // Key and name are interchangeable
      // Name is an IDREF and can be used for validation checks within the file.
      // Key is used to refer to an external variable without validation error
      // DisplayName is used for GUI (model)
      String  name         = getAttributeWithFor(stringElement, "name");
      String  key          = getAttributeWithFor(stringElement, "key");
      String  displayName  = getAttributeWithFor(stringElement, "displayName");
      String  description  = getAttributeWithFor(stringElement, "description");
      String  toolTip      = getToolTip(stringElement);

      if (!key.isEmpty() && !name.isEmpty()) {
         throw new Exception("Both name and key provided for <alias>, key='" + key +"', name='" + name + "'");
      }
      if (key.isEmpty()) {
         key = name;
      }
      if (key.isEmpty()) {
         throw new Exception("Alias requires either name or key "+displayName);
      }
      key = substituteKey(key);
      key = fProvider.makeKey(key);

      displayName = substituteKey(displayName);
      
      boolean isConstant  = Boolean.valueOf(stringElement.getAttribute("constant"));
      boolean isOptional  = Boolean.valueOf(stringElement.getAttribute("optional"));
      
      AliasPlaceholderModel placeholderModel = new AliasPlaceholderModel(parent, displayName, description);
      placeholderModel.setkey(key);
      placeholderModel.setConstant(isConstant);
      placeholderModel.setOptional(isOptional);
      placeholderModel.setToolTip(toolTip);
      return placeholderModel;
   }

   /**
    * @param parentModel
    * @param element
    * @throws Exception 
    */
   private void parseConstant(BaseModel parentModel, Element element) throws Exception {
      // Key and name are interchangeable
      // Name is an ID and can be used for validation checks within the file.
      // Key is used to refer to an external variable without validation error
      String name       = element.getAttribute("name");
      String key        = element.getAttribute("key");
      String value      = element.getAttribute("value");
      boolean isWeak    = Boolean.valueOf(element.getAttribute("weak"));
      boolean isReplace = Boolean.valueOf(element.getAttribute("replace"));
      boolean isDerived = Boolean.valueOf(element.getAttribute("derived"));
      
      // Accept either key or name (prefer key)
      if (key.isEmpty()) {
         key = name;
      }
      if (name.isEmpty()) {
         name = key;
      }
      key  = fProvider.makeKey(key);
      key  = substituteKey(key);
      name = substituteKey(name);
      
      Variable var = safeGetVariable(key);
//      value = ReplacementParser.substitute(value, fPeripheral.get());
      if (var != null) {
         if (isWeak) {
            // Ignore constant
         }
         else if (isReplace) {
            // Replace constant value
            var.setValue(value);
            return;
         }
         else {
            throw new Exception("Constant multiply defined, name="+name+", key=" + key);
         }
      }
      else {
         var = new StringVariable(name, key);
         var.setValue(value);
         var.setDerived(isDerived);
         fProvider.addVariable(var);
      }
   }
   
   static class TemplateConditionParser {
      
      private final VariableProvider fProvider;
      
      /** Set of template enables. These are used to discard unused conditional templates */
//      private HashSet<String> templateEnables = new HashSet<String>();

      /** Index into current expression being parsed */
      private int    index;
      
      /** Current condition being evaluated */
      private String condition;
      
      /**
       * Constructor
       * 
       * @param provider Where to look for variables
       */
      public TemplateConditionParser(VariableProvider provider) {
         fProvider = provider;
      }

      /**
       * Add condition value
       * 
       * @param enable value of condition
       */
      public void addEnable(String enable) {
//         templateEnables.add(enable);
      }

      void skipSpace() {
         while ((index<condition.length()) && Character.isWhitespace(condition.charAt(index))) {
            index++;
         }
      }
      
      private boolean getId() throws Exception {
         
         skipSpace();
         
         boolean inverted = false;
         if (condition.charAt(index) == '!') {
            inverted = true;
            index++;
         }

         skipSpace();
         
         StringBuilder sb = new StringBuilder();
         boolean idFound = false;
         while ((index<condition.length()) && Character.isJavaIdentifierPart(condition.charAt(index))) {
            sb.append(condition.charAt(index++));
            idFound = true;
         }
         if (!idFound) {
            throw new Exception("Invalid identifier in template condition '"+condition+"'");
         }
         if ((index<condition.length()) && (condition.charAt(index) == '.')) {
            index++;
         }
         if (((index+1)<condition.length()) && (condition.substring(index,index+2).equals("[]"))) {
            index+=2;
         }
         
         boolean found = fProvider.safeGetVariable(fProvider.makeKey(sb.toString())) != null;
         return inverted?!found:found;
      }

      private String getOperator() throws Exception {
         
         skipSpace();
         
         // No operator is acceptable
         if (index == condition.length()) {
            return null;
         }
         // Must have an operator now
         String operator = null;
         if ((index+1)<condition.length()) {
            operator = condition.substring(index, index+2);
            index += 2;
            if (!operator.equals("||") && !operator.equals("&&")) {
               operator = null;
            }
         }
         if (operator == null) {
            throw new Exception("Invalid operator in template condition '"+condition+"'");
         }
         return operator;
      }
      
      /**
       * Check if a template is enabled
       * 
       * @param condition Value used to indicate condition
       * 
       * @return true if template is to be retained
       * @throws Exception 
       */
      public boolean checkTemplateCondition(String condition) throws Exception {
         if ((condition == null) || condition.isBlank()) {
            return true;
         }
         index = 0;
         this.condition = condition;
         boolean result;
         do {
            // Get identifier
            result = getId();
            do {
               String operator = getOperator();
               if (operator == null) {
                  return result;
               }
               if (operator.equals("||")) {
                  if (result) {
                     return true;
                  }
               }
               else if (operator.equals("&&")) {
                  if (!result) {
                     return false;
                  }
               }
               // Get next identifier
               result = getId();
            } while (true);
         } while (true);
      }
   }
      
   /**
    * Parse template enable. <br>
    * This is a list of enables in the text node of a template enable element
    * 
    * @param element    Template enable node &lt;templateEnable&gt;
    * 
    * @throws Exception
    */
   private void parseTemplateEnable(Element element) throws Exception {
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() == Node.TEXT_NODE) {
            String[] enables = node.getTextContent().split(",");
            for (String enable : enables) {
               fTemplateConditionParser.addEnable(enable.trim());
            }
            continue;
         }
      }
   }
   
   private static class Pair {
      final String left;
      final String right;
      
      Pair(String left ,String right) {
         this.left  = left;
         this.right = right;
      }
   }
   
   /**
    * Apply a set of template substitutions of form <b>%name</b> in template text
    * 
    * @param text          Text to modify
    * @param substitutions Substitutions to do
    * 
    * @return Modified test
    */
   private String doTemplateSubstitutions(String text, ArrayList<Pair> substitutions) {
      for (Pair p:substitutions) {
         if (p.right == null) {
            continue;
         }
         text = text.replace(p.left, p.right);
      }
      return text;
   }
   
   /**
    * Construct template substitutions
    * <li>%description              Description from controlVar
    * <li>%shortDescription         Short description from controlVar
    * <li>%tooltip                  Tool-tip from controlVar
    * <li>%defaultValueExpression   Of form <b>$(controlVarName)</b>
    * <li>%bareValueExpression      Of form <b>CONTROL_VAR_NAME($(control_var_name))</b>
    *  
    * @param element          Element 
    * @param controlVarName   Control var to obtain information from
    * 
    * @return  List of substitutions
    */
   ArrayList<Pair> getTemplateSubstitutions(Element element, String controlVarName) {
      
      ArrayList<Pair> substitutions = new ArrayList<Pair>();
      
      String description            = "'%description' not available in this template";
      String shortDescription       = "'%shortDescription' not available in this template";
      String tooltip                = "'%tooltip' not available in this template";
      String defaultValueExpression = "'%defaultValueExpression' not available in this template";
      String bareValueExpression    = "'%bareValueExpression' not available in this template";
      
      Variable controlVar = fProvider.safeGetVariable(fProvider.makeKey(controlVarName));
//      if (!controlVarName.isBlank() && (controlVar == null)) {
//         System.err.println("Unable to find variable " + controlVarName);
//      }
      if (!controlVarName.isBlank() && (controlVar != null)) {
         String macro = controlVarName.toUpperCase();
         if (macro.endsWith(".")) {
            macro = macro.substring(0, macro.length()-1);
         }
         if (macro.endsWith("[]")) {
            macro = macro.substring(0, macro.length()-2);
         }
         description       = controlVar.getDescriptionAsCode();
         shortDescription  = controlVar.getShortDescription();
         tooltip           = controlVar.getToolTipAsCode();

         bareValueExpression = "($("+controlVarName+"))";
         defaultValueExpression = macro+bareValueExpression;
      }
      substitutions.add(new Pair("%description",            description));
      substitutions.add(new Pair("%shortDescription",       shortDescription));
      substitutions.add(new Pair("%tooltip",                tooltip));
      substitutions.add(new Pair("%defaultValueExpression", defaultValueExpression));
      substitutions.add(new Pair("%bareValueExpression",    bareValueExpression));
      return substitutions;
   }
   
   /**
    * Expected attributes:<br>
    * <li>&lt;name&gt; - Used to place template at arbitrary location (creates variable of that name)
    * <li>&lt;namespace&gt; (info|usbdm|class|all) - Scope where template is available in
    * <li>&lt;condition&gt; - Condition made up existing <b>choice</b> or <b>binary</b> variable used as information source
    * 
    * &lt;condition&gt; Usually of form 'peripheral_register_bitfield' to allow deduction or can be boolean expresssion<br>
    * 
    * Substitutions in Text :<br>
    * <li>%description              Description from controlVar
    * <li>%shortDescription         Short description from controlVar
    * <li>%tooltip                  Tool-tip from controlVar
    * <li>%defaultValueExpression   Of form <b>$(controlVarName)</b>
    * <li>%bareValueExpression      Of form <b>CONTROL_VAR_NAME($(control_var_name))</b>
    * 
    * @param element    Element to parse
    * 
    * @throws Exception
    */
   private void parseTemplate(Element element) throws Exception {
      /**
       * namespace:
       *    class - Template is available in 
       */
      String name      = doForSubstitutions(element.getAttribute("name"));
      String namespace = doForSubstitutions(element.getAttribute("namespace"));
      String condition = doForSubstitutions(element.getAttribute("condition"));
      
      if (namespace.isBlank()) {
         throw new Exception("Template is missing namespace, name='" + name + "'");
      }
      if (!name.isBlank() && !namespace.equals("all")) {
         throw new Exception("Named templates must have 'all' namespace, name='" + name + "'");
      }
      if (name.isBlank() && namespace.equals("all")) {
         throw new Exception("Templates must be named in 'all' namespace, peripheral='" + fPeripheral.getName() + "'");
      }
      element.getNodeValue();
      if (!fTemplateConditionParser.checkTemplateCondition(condition)) {
         return;
      }
      ArrayList<Pair> substitutions = getTemplateSubstitutions(element, condition);
      
      TemplateInformation templateInfo = addTemplate(name, namespace);
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
            String bodyText = doForSubstitutions(node.getTextContent());
            bodyText = doTemplateSubstitutions(bodyText, substitutions);
            templateInfo.addText(bodyText);
            continue;
         }
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element childElement = (Element) node;
         String  forTagName   = childElement.getTagName();
         if (forTagName.equals("for")) {
            parseForElement(childElement, templateInfo);
         }
         else {
            throw new Exception("Unexpected child in <template>, value = \'"+forTagName+"\'");
         }
      }
   }
   
   /**
    * Expected attributes:<br>
    * <li>&lt;name&gt; - Used to place value at arbitrary location (creates variable of that name)
    * <li>&lt;namespace&gt; (info|usbdm|class|all) - Scope where template is available in
    * <li>&lt;condition&gt; - Name of existing <b>choice</b> or <b>binary</b> variable used as information source
    * <li>&lt;enumRoot&gt; - Stem of enum names e.g. SimUsbPower => SimUsbPower_xxx, SimUsbPower_yyy
    * <li>&lt;valueFormat&gt; - Used for writing enum value e.g. String.format("XXXX(%s)", enumName)
    * <li>&lt;template&gt; - Used for writing entire enum entry e.g. String.format("...%s...%s...%s", enumName, value, comment)<br><br>
    * 
    * &lt;condition&gt; Usually of form 'peripheral_register_bitfield' to allow deduction<br>
    * &lt;valueFormat&gt; defaults to upper-cased value based on &lt;condition&gt; if needed<br>
    * &lt;template&gt; and &lt;enumRoot&gt;+&lt;valueFormat&gt; are alternatives<br><br>
    * 
    * Substitutions in Text :<br>
    * <li>%enumClass  - Based on enumRoot with capitalised first letter
    * <li>%tooltip - From &lt;condition&gt; variable
    * <li>%description - From &lt;condition&gt; variable
    * <li>%body - Constructed enum clauses
    * 
    * @param element    Element to parse
    * 
    * @throws Exception
    */
   private void parseEnumTemplate(Element element) throws Exception {
      
      String name            = doForSubstitutions(element.getAttribute("name"));
      String namespace       = doForSubstitutions(element.getAttribute("namespace"));
      String condition       = doForSubstitutions(element.getAttribute("condition"));
      String template        = doForSubstitutions(element.getAttribute("template"));
      String enumRoot        = doForSubstitutions(element.getAttribute("enumRoot"));
      String valueFormat     = doForSubstitutions(element.getAttribute("valueFormat"));
      
      if (namespace.isBlank()) {
         throw new Exception("EnumTemplate is missing namespace, name='" + name + "'");
      }
      if (!name.isBlank() && !namespace.equals("all")) {
         throw new Exception("Named EnumTemplate must have 'all' namespace, name='" + name + "'");
      }
      if (name.isBlank() && namespace.equals("all")) {
         throw new Exception("EnumTemplate must be named in 'all' namespace, peripheral='" + fPeripheral.getName() + "'");
      }
      if (condition.isBlank()) {
         throw new Exception("EnumTemplate must have 'conditionAttr' attribute, peripheral='" + 
               fPeripheral.getName() + "', template= '" + template + "'");
      }
      if ((template.isBlank() && enumRoot.isBlank() || 
          (!template.isBlank() && !enumRoot.isBlank()) )) {
         throw new Exception("EnumTemplate must have either 'template' or 'enumRoot' attributes, peripheral='" + 
               fPeripheral.getName() + "', template= '" + template + "'");
      }
      if (!element.hasAttribute("valueFormat")) {
         // Default of form 'XXXX(%s)'
         valueFormat = condition.toUpperCase()+"(%s)";
      }
      VariableWithChoices choiceVar = (VariableWithChoices) safeGetVariable(fProvider.makeKey(condition));
      if (choiceVar == null) {
         return;
      }
      TemplateInformation templateInfo = addTemplate(name, namespace);
      
      StringBuilder body = new StringBuilder();
      ChoiceData[] choiceData = choiceVar.getData();
      
      String enumClass = "%enumClass not available in this template";

      if (template.isBlank()) {
         // Use enumRoot attribute
         enumClass  = Character.toUpperCase(enumRoot.charAt(0)) + enumRoot.substring(1);

         String[] enumName  = new String[choiceData.length];
         String[] values    = new String[choiceData.length];
         int enumNameMax    = 0;
         int valueMax       = 0;

         for (int index=0; index<choiceData.length; index++) {
            
            String enumValue = choiceData[index].getEnumName();
            if (enumValue == null) {
               throw new Exception("enumTemplate - enum data is incomplete in choice '" + choiceData[index].getName() + "' condition='"+condition+"'");
            }
            enumName[index] = enumClass+"_"+enumValue;
            values[index]   = String.format(valueFormat, choiceData[index].getValue())+",";
            enumNameMax     = Math.max(enumNameMax, enumName[index].length());
            valueMax        = Math.max(valueMax, values[index].length());
         }
         for (int index=0; index<choiceData.length; index++) {
            String comment  = choiceData[index].getName();
            body.append(String.format("\\t   %-"+enumNameMax+"s = %-"+valueMax+"s ///< %s\n", enumName[index], values[index], comment));
         }
      }
      else {
         // Use template attribute
         for (int index=0; index<choiceData.length; index++) {
            String value    = choiceData[index].getValue();
            String enumValue = choiceData[index].getEnumName();
            if (enumValue == null) {
               throw new Exception("enumTemplate - enum data is incomplete in choice '" + choiceData[index].getName() + "' condition='"+condition+"'");
            }
            String comment  = choiceData[index].getName();
            body.append(String.format(template+"\n", enumValue, value, comment));
         }
      }
      ArrayList<Pair> substitutions = getTemplateSubstitutions(element, condition);

      substitutions.add(0, new Pair("%body",     body.toString()));
      substitutions.add(new Pair("%enumClass",   enumClass));
      
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
            String bodyText = doForSubstitutions(node.getTextContent());
            bodyText = doTemplateSubstitutions(bodyText, substitutions);
            templateInfo.addText(bodyText);
            continue;
         }
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
      }
   }

   /**
    * Expected attributes:<br>
    * <li>&lt;name&gt; - Used to place value at arbitrary location (creates variable of that name)
    * <li>&lt;namespace&gt; (info|usbdm|class|all) - Scope where template is available in
    * <li>&lt;condition&gt; - Name of existing <b>choice</b> or <b>binary</b> variable used as information source
    * <li>&lt;enumRoot&gt; - Stem of enum names e.g. SimUsbPower => SimUsbPower_xxx, SimUsbPower_yyy
    * <li>&lt;returnFormat&gt; - Used for writing return value e.g. String.format("XXXX(%s)", enumName)<br>
    * <li>&lt;mask&gt; - Used for masking parameter to obtain case control value e.g. "SIM_SOPT_XXX" (_MASK is added)<br><br>
    * 
    * &lt;mask&gt; defaults to upper-cased value based on &lt;condition&gt;<br>
    * &lt;condition&gt; Usually of form 'peripheral_register_bitfield' to allow deduction
    * 
    * &lt;valueFormat&gt; default to upper-cased value based on &lt;condition&gt; if needed<br>
    * &lt;template&gt; and &lt;enumRoot&gt;+&lt;valueFormat&gt; are alternatives<br>
    * &lt;returnFormat&gt; is only required if %xxx is used<br><br>
    * 
    * Substitutions in Text :<br>
    * <li>%tooltip - From &lt;condition&gt; variable
    * <li>%description - From &lt;condition&gt; variable
    * <li>%enumClass  - Based on enumRoot with upper-case first letter
    * <li>%enumParam  - Based on enumRoot with lower-case first letter
    * <li>%mask - From &lt;mask&gt; or deduced from &lt;condition&gt;
    * <li>%defaultClockExpression - Based on condition etc. Similar to sim->SOPT2 = (sim->SOPT2&~%mask) | %enumParam;
    * <li>%defaultMaskingExpression - Based on condition etc. Similar to (sim->SOPT2&%mask)
    * <li>%body - Constructed case clauses
    * 
    * @param element
    * @throws Exception
    */
   private void parseClockCodeTemplate(Element element) throws Exception {
      
      String name         = doForSubstitutions(element.getAttribute("name"));
      String namespace    = doForSubstitutions(element.getAttribute("namespace"));
      String condition    = doForSubstitutions(element.getAttribute("condition"));
      String enumRoot     = doForSubstitutions(element.getAttribute("enumRoot"));
      String returnFormat = element.hasAttribute("returnFormat")?doForSubstitutions(element.getAttribute("returnFormat")):null;
      String mask         = doForSubstitutions(element.getAttribute("mask"));
      
      if (namespace.isBlank()) {
         throw new Exception("ClockCodeTemplate is missing namespace, name='" + name + "'");
      }
      if (!name.isBlank() && !namespace.equals("all")) {
         throw new Exception("Named ClockCodeTemplate must have 'all' namespace, name='" + name + "'");
      }
      if (name.isBlank() && namespace.equals("all")) {
         throw new Exception("ClockCodeTemplate must be named in 'all' namespace, peripheral='" + fPeripheral.getName() + "'");
      }
      if (condition.isBlank()) {
         throw new Exception("ClockCodeTemplate must have 'condition' attribute, peripheral='" + 
               fPeripheral.getName() + "', enumRootAttr= '" + enumRoot + "'");
      }
      if (enumRoot.isBlank()) {
         throw new Exception("ClockCodeTemplate must have 'enumRoot' attribute, peripheral='" + 
               fPeripheral.getName() + "', cond= '" + condition + "'");
      }
      element.getNodeValue();

      VariableWithChoices choiceVar = (VariableWithChoices) safeGetVariable(fProvider.makeKey(condition));
      if (choiceVar == null) {
         return;
      }
      TemplateInformation templateInfo = addTemplate(name, namespace);
      
      StringBuilder body = new StringBuilder();
      
      ChoiceData[] choiceData = choiceVar.getData();
      
      String[] enumNames      = new String[choiceData.length];
      String[] returnValues   = new String[choiceData.length];
      
      String   comment;
      int enumNameMax    = 0;
      int returnValueMax = 0;
      
      String enumClass  = Character.toUpperCase(enumRoot.charAt(0)) + enumRoot.substring(1);
      String enumParam  = Character.toLowerCase(enumRoot.charAt(0)) + enumRoot.substring(1);
      
      if (returnFormat != null) {
         // Create body for case statement if used
         for (int index=0; index<choiceData.length; index++) {

            String enumName  = choiceData[index].getEnumName();
            String codeValue = choiceData[index].getCodeValue();
            if ((enumName == null) || (codeValue == null)) {
               throw new Exception("Choice '"+choiceData[index].getName()+"' is missing enum/code value in "+choiceVar);
            }
            enumNames[index]     = enumRoot+"_"+enumName;
            enumNameMax          = Math.max(enumNameMax, enumNames[index].length());
            returnValues[index]  = String.format(returnFormat+";", codeValue);
            returnValueMax       = Math.max(returnValueMax, returnValues[index].length());
         }
         final String format = "\\t      case %-"+enumNameMax+"s : return %-"+returnValueMax+"s %s\n";
         for (int index=0; index<choiceData.length; index++) {
            comment  = "///< "+choiceData[index].getName();
            body.append(String.format(format, enumNames[index], returnValues[index], comment));
         }
      }
      if (mask.isBlank()) {
         mask = condition.toUpperCase()+"_MASK";
      }
      Pattern p = Pattern.compile("sim_sopt(\\d+)_.+");
      Matcher m = p.matcher(condition);
      String defaultClockExpression   = "%defaultClockExpression is not valid here";
      String defaultMaskingExpression = "%defaultMaskingExpression is not valid here";
      if (m.matches()) {
         defaultClockExpression   = "sim->SOPT"+m.group(1)+" = (sim->SOPT"+m.group(1)+"&~"+mask+") | "+enumParam+";";
         defaultMaskingExpression = "sim->SOPT"+m.group(1)+"&"+mask;
      }
      ArrayList<Pair> substitutions = getTemplateSubstitutions(element, condition);

      substitutions.add(0, new Pair("%body",       body.toString()));
      substitutions.add(new Pair("%enumClass",  enumClass));
      substitutions.add(new Pair("%enumParam",  enumParam));
      substitutions.add(new Pair("%mask",       mask));
      substitutions.add(new Pair("%defaultClockExpression",  defaultClockExpression));
      substitutions.add(new Pair("%defaultMaskingExpression",defaultMaskingExpression));

      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
            String bodyText = doForSubstitutions(node.getTextContent());
            bodyText = doTemplateSubstitutions(bodyText, substitutions);
            templateInfo.addText(bodyText);
            continue;
         }
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
      }
   }

   /**
    * Parse element: <ul>
    *   <li> &lt;fragment&gt; referencing only elements below
    *   <li> &lt;validate&gt;
    *   <li> &lt;template&gt;
    *   <li> &lt;projectActionList&gt; 
    *</ul>
    *   
    * Items found are recorded
    *
    * @param  menuElement  Menu element to parse
    * 
    * @throws Exception
    */
   private void parseControlItem(Element element) throws Exception {

      try {
         String tagName = element.getTagName();
         if (tagName == "fragment") {
            for (Node node = element.getFirstChild();
                  node != null;
                  node = node.getNextSibling()) {
               if (node.getNodeType() != Node.ELEMENT_NODE) {
                  continue;
               }
               parseControlItem((Element) node);
            }
         }
         else if (tagName == "validate") {
            fValidators.add(parseValidate(element));
         }
         else if (tagName == "clockCodeTemplate") {
            parseClockCodeTemplate(element);
         }
         else if (tagName == "enumTemplate") {
            parseEnumTemplate(element);
         }
         else if (tagName == "template") {
            parseTemplate(element);
         }
         else if (tagName == "dependency") {
            fPeripheral.addDependency(element.getAttribute("key"));
         }
         else if (tagName == "deleteOption") {
            parseDeleteOption(element);
         }
         else if (tagName == "projectActionList") {
            ProjectActionList pal = PackageParser.parseRestrictedProjectActionList(element, RESOURCE_PATH);
            pal.visit(new Visitor() {
               @Override
               public Result applyTo(ProjectAction action, Value result, IProgressMonitor monitor) {
                  if (action instanceof ProjectConstant) {
                     ProjectConstant constant = (ProjectConstant) action;
                     Variable var = new StringVariable(constant.getId(), constant.getId());
                     var.setValue(constant.getValue());
//                  System.err.println("Adding " + var);
                     fProvider.addVariable(var);
                  }
                  return Visitor.CONTINUE;
               }}, null);
            fProjectActionList.addProjectAction(pal);
         }
         else {
            throw new Exception("Unexpected tag in parseControlItem(), \'"+tagName+"\'");
         }
      } catch (Exception e) {
         throw new Exception("Failed to parse element "+element, e);
      }
   }
   
   private void parseDeleteOption(Element element) throws Exception {
      String name = element.getAttribute("name");
      String key  = element.getAttribute("key");
      boolean mustExist = Boolean.parseBoolean(element.getAttribute("mustExist"));
      
      if (key.isBlank()) {
         key = name;
      }
      if (key.isBlank()) {
         throw new Exception("'deleteOption' must have name or key " + element.toString());
      }
      boolean wasDeleted = fProvider.removeVariable(fProvider.makeKey(key));
      
      if (mustExist  && !wasDeleted) {
         throw new Exception("Variable '" + key + "' was not found to delete in deleteOption");
      }
   }

   /**
    * 
    * @param element          Element to parse
    * @param parentTemplate   Template that contains 'for'
    * 
    * @throws Exception 
    */
   private void parseForElement(
         Element              element, 
         TemplateInformation  parentTemplate) throws Exception {
      
      TemplateInformation templateInfo = new TemplateInformation("", "", 0);
      templateInfo.setIteration(element.getAttribute("var"), element.getAttribute("enumeration"));

      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
            templateInfo.addText(node.getTextContent());
            continue;
         }
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element childElement = (Element) node;
         String  forTagName   = childElement.getTagName();
         if (forTagName.equals("for")) {
            parseForElement(childElement, templateInfo);
         }
         else {
            throw new Exception("Unexpected child in <template>, value = \'"+forTagName+"\'");
         }
      }
      parentTemplate.addChild(templateInfo);
   }

   /**
    * Parse child elements containing: <ul>
    *   <li> &lt;fragment&gt; referencing only elements below
    *   <li> &lt;intOption&gt;
    *   <li> &lt;bitmaskOption&gt;
    *   <li> &lt;floatOption&gt; 
    *   <li> &lt;binaryOption&gt; 
    *   <li> &lt;irqOption&gt; 
    *   <li> &lt;choiceOption&gt; 
    *   <li> &lt;stringOption&gt; 
    *   <li> &lt;numericListOption&gt; 
    *   <li> &lt;pinListOption&gt; 
    *   <li> &lt;aliasOption&gt; 
    *   <li> &lt;constant&gt; 
    *   <li> &lt;section&gt; 
    *   <li> &lt;signals&gt; 
    *   <li> Control items...
    *</ul>
    *   
    * Elements found are added as children of the parentModel
    * 
    * @param  parentModel  Model to attach children to
    * @param  menuElement  Menu element to parse
    * 
    * @throws Exception
    */
   private void parseChildModels(BaseModel parentModel, Element menuElement) throws Exception {
      for (Node node = menuElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element    = (Element) node;
         parseChildModel(parentModel, element);
      }
   }

      /**
       * Parse element containing: <ul>
       *   <li> &lt;fragment&gt; referencing only elements below
       *   <li> &lt;intOption&gt;
       *   <li> &lt;bitmaskOption&gt;
       *   <li> &lt;floatOption&gt; 
       *   <li> &lt;binaryOption&gt; 
       *   <li> &lt;irqOption&gt; 
       *   <li> &lt;choiceOption&gt; 
       *   <li> &lt;stringOption&gt; 
       *   <li> &lt;numericListOption&gt; 
       *   <li> &lt;pinListOption&gt; 
       *   <li> &lt;aliasOption&gt; 
       *   <li> &lt;constant&gt; 
       *   <li> &lt;section&gt; 
       *   <li> &lt;signals&gt; 
       *   <li> Control items...
       *</ul>
       *   
       * Elements found are added as children of the parentModel
       * 
       * @param  parentModel  Model to attach children to
       * @param  menuElement  Menu element to parse
       * 
       * @throws Exception
       */
   private void parseChildModel(BaseModel parentModel, Element element) throws Exception {

      String tagName     = element.getTagName();
      String name        = element.getAttribute("name");
      String toolTip     = getToolTip(element);

      //         System.err.println("parseChildModel(): " + tagName + ", " + element.getAttribute("name"));
      if (tagName == "fragment") {
         parseChildModels(parentModel, element);
      }
      else if (tagName == "category") {
         parseCategory(parentModel, element);
      }
      else if (tagName == "categoryOption") {
         parseCategoryOption(parentModel, element);
      }
      else if (tagName == "aliasCategoryOption") {
         parseAliasCategoryOption(parentModel, element);
      }
      else if (tagName == "intOption") {
         parseLongOption(parentModel, element);
      }
      else if (tagName == "bitmaskOption") {
         parseBitmaskOption(parentModel, element);
      }
      else if (tagName == "floatOption") {
         parseDoubleOption(parentModel, element);
      }
      else if (tagName == "binaryOption") {
         parseBinaryOption(parentModel, element);
      }
      else if (tagName == "irqOption") {
         parseIrqOption(parentModel, element);
      }
      else if (tagName == "choiceOption") {
         parseChoiceOption(parentModel, element);
      }
      else if (tagName == "addChoices") {
         parseAddChoices(parentModel, element);
      }
      else if (tagName == "stringOption") {
         parseStringOption(parentModel, element);
      }
      else if (tagName == "numericListOption") {
         parseNumericListOption(parentModel, element);
      }
      else if (tagName == "pinListOption") {
         parsePinListOption(parentModel, element);
      }
      else if (tagName == "aliasOption") {
         parseAliasOption(parentModel, element);
      }
      else if (tagName == "constant") {
         parseConstant(parentModel, element);
      }
      else if (tagName == "for") {
         parseForLoop(parentModel, element);
      }
      else if (tagName == "section") {
         BaseModel model = new ParametersModel(parentModel, name, toolTip);
         parseChildModels(model, element);
      }
      else if (tagName == "list") {
         BaseModel model = new ListModel(parentModel, name);
         parseSectionsOrOther(model, element);
      }
      else if (tagName == "signals") {
         parseSignalsOption(parentModel, element);
      }
      else if (tagName == "templateEnable") {
         parseTemplateEnable(element);
      }
      else {
         parseControlItem( element);
      }
   }

   /**
    * Create and add template<br>
    * 
    * @param key        Key used to index template
    * @param namespace  Namespace for template (info, usbdm, class)
    * @param dimension  Dimension for array template
    * @param contents   Text contents for template
    * 
    * @throws Exception 
    */
   private TemplateInformation addTemplate(String name, String namespace, int dimension) throws Exception {
      
      TemplateInformation templateInfo = new TemplateInformation(name, namespace, dimension);

      String key = MenuData.makeKey(name, namespace);
      ArrayList<TemplateInformation> templateList = fTemplateInfos.get(key);
      if (templateList == null) {
         templateList = new ArrayList<TemplateInformation>();
         fTemplateInfos.put(key, templateList);
      }
      templateList.add(templateInfo);
      return templateInfo;
   }

   /**
    * Create and add template<br>
    * 
    * @param key        Key used to index template
    * @param namespace  Namespace for template (info, usbdm, class)
    * @param dimension  Dimension for array template
    * @param contents   Text contents for template
    * 
    * @throws Exception 
    */
   private TemplateInformation addTemplate(String name, String namespace) throws Exception {
      return addTemplate(name, namespace, 1);
   }

   /**
    * Parse the pin associated with the peripheral
    * 
    * @param parentModel
    * @param element
    * @throws UsbdmException 
    */
   private void parseSignalsOption(BaseModel parentModel, Element element) throws UsbdmException {
      // Initially assume pins refer to current peripheral
      Peripheral peripheral = fPeripheral;
      boolean optional = Boolean.valueOf(element.getAttribute("optional"));
      String peripheralName = element.getAttribute("name");
      if (!peripheralName.isEmpty()) {
         // Change to referenced peripheral
         peripheral = fPeripheral.getDeviceInfo().getPeripherals().get(peripheralName);
      }
      if (peripheral == null) {
         if (!optional) {
            throw new UsbdmException("Unable to find <signals> for peripheral '"+peripheralName+"'");
         }
         return;
      }
      String filter = element.getAttribute("filter");
      fPeripheral.addSignalsFromPeripheral(parentModel, peripheral, filter);
   }

   private static class ChoiceInformation {
      final ArrayList<ChoiceData> entries;
      final String                defaultEntry;
      
      public ChoiceInformation(ArrayList<ChoiceData> entries, String defaultEntry) {
         this.entries = entries;
         this.defaultEntry = defaultEntry;
      }
   }
   
   /**
    * Parse choice from an element
    * 
    * @param menuElement Element to parse
    * 
    * @return Array of choice data information
    * @throws Exception 
    */
   private ChoiceInformation parseChoiceData(Element menuElement) throws Exception {
      
      ArrayList<ChoiceData> entries = new ArrayList<ChoiceData>();
      String defaultValue = null;
      NodeList choiceNodes = menuElement.getElementsByTagName("choice");
      for(int index=0; index<choiceNodes.getLength(); index++) {
         Node node = choiceNodes.item(index);
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (!element.hasAttribute("name") || !element.hasAttribute("value")) {
            throw new Exception("<choice> must have name and value attributes "+element);
         }
         ChoiceData entry = new ChoiceData(
               doForSubstitutions(element.getAttribute("name")), 
               doForSubstitutions(element.getAttribute("value")), 
               doForSubstitutions(element.getAttribute("enum")),
               doForSubstitutions(element.getAttribute("code")),
               doForSubstitutions(element.getAttribute("ref"))
               );
         String requiredPeripheral = element.getAttribute("requiresPeripheral").toUpperCase();
         // Check if entry requires a peripheral to be present to be used
         if (!requiredPeripheral.isBlank()) {
            requiredPeripheral = doForSubstitutions(requiredPeripheral);
            Peripheral p = fPeripheral.getDeviceInfo().getPeripherals().get(requiredPeripheral);
            if (p == null) {
               // Discard choice
               continue;
            }
         }
         entries.add(entry);
         if (defaultValue == null) {
            // Assume 1st entry is default
            defaultValue = entry.getName();
         }
         if (element.getAttribute("isDefault").equalsIgnoreCase("true")) {
            // Explicit default set
            defaultValue = entry.getName();
         }
      }
      return new ChoiceInformation(entries, defaultValue);
   }
   
   /**
    * Parses the children of this element
    * 
    * @param  parentModel  Model to attach children to
    * @param  menuElement  Menu element to parse
    * 
    * @throws Exception
    */
   private void parseChoices(Variable variable, Element menuElement) throws Exception {
      
      ChoiceInformation info = parseChoiceData(menuElement);
      
      if (info.entries.size()==0) {
         /**
          * Should be another variable of the same type to copy from i.e. derivedFrom="" present
          */
         Variable otherVariable = getDerived(menuElement);
         if (otherVariable == null) {
            throw new Exception("No choices found in <"+menuElement.getTagName() + " name=\"" + variable.getName()+ "\">");
         }
         if (otherVariable.getClass() != variable.getClass()) {
            throw new Exception("Referenced variable of wrong type <"+menuElement.getTagName() + " derivedFrom=\"" + variable.getName()+ "\">");
         }
         if (variable instanceof BooleanVariable) {
            BooleanVariable otherVar = (BooleanVariable) otherVariable;
            BooleanVariable var      = (BooleanVariable) variable;
            var.setFalseValue(otherVar.getFalseValue());
            var.setTrueValue(otherVar.getTrueValue());
            var.setDefault(otherVar.getDefault());
            var.setValue(otherVar.getDefault());
         }
         else if (variable instanceof ChoiceVariable) {
            ChoiceVariable otherVar = (ChoiceVariable) otherVariable;
            ChoiceVariable var      = (ChoiceVariable) variable;
            var.setData(otherVar.getData());
            var.setDefault(otherVar.getDefault());
            var.setValue(otherVar.getDefault());
         }
      }
      else {
         // Set of choices provided
         if (variable instanceof BooleanVariable) {
            if (info.entries.size()>2) {
               throw new Exception("Wrong number of choices in <"+menuElement.getTagName() + " name=\"" + variable.getName()+ "\">");
            }
            BooleanVariable var = (BooleanVariable) variable;
            var.setFalseValue(info.entries.get(0));
            if (info.entries.size()<2) {
               var.setTrueValue(new ChoiceData("Unused", "Unused", "Unused", "Unused", "Unused"));
            }
            else {
               var.setTrueValue(info.entries.get(1));
            }
         }
         else if (variable instanceof ChoiceVariable) {      
            ChoiceVariable var = (ChoiceVariable)variable;
            var.setData(info.entries);
         }
         variable.setDefault(info.defaultEntry);
         variable.setValue(info.defaultEntry);
      }
   }

   public static class ValidatorInformation {
      private String            fClassName;
      private ArrayList<Object> fParams = new ArrayList<Object>();
      private int               fDimension;

      /**
       * Construct validator
       * 
       * @param className Name of class
       */
      ValidatorInformation(String className, int dimension) {
         fClassName = className;
         fDimension = dimension;
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
      public int getDimension() {
         return fDimension;
      }
   }

   /**
    * Parse &lt;validate&gt; element<br>
    * 
    * @param validateElement
    * @throws Exception 
    */
   private ValidatorInformation parseValidate(Element validateElement) throws Exception {
      //      System.err.println("================================");
      //      for (String k:paramMap.keySet()) {
      //         System.err.println(k + " => " + paramMap.get(k));
      //      }
      //      System.err.println("================================");
      long dimension = getLongAttributeWithVariableSubstitution(validateElement, "dim");
      ValidatorInformation validator = new ValidatorInformation(validateElement.getAttribute("class"), (int)dimension);
      
      for (Node node = validateElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "param") {
            String type  = element.getAttribute("type");
            String value = element.getAttribute("value");
            
            // Do substitutions on parameter
            value = fProvider.substitute(value);
            if (type.equalsIgnoreCase("long")) {
               validator.addParam(EngineeringNotation.parseAsLong(value));
            }
            else if (type.equalsIgnoreCase("string")) {
               validator.addParam(value);
            }
            else {
               throw new Exception("Unexpected type in <validate>, value = \'"+element.getTagName()+"\'");
            }
         }
         else {
            throw new Exception("Unexpected field in <validate>, value = \'"+element.getTagName()+"\'");
         }
      }
      return validator;
   }

   /**
    * Parse: <br>
    *    &lt;peripheralPage&gt;<br>
    *    &lt;list&gt;<br>
    *    &lt;section&gt;<br>
    *    &lt;fragment&gt;<br>
    * 
    * @param menuElement
    * 
    * @throws Exception 
    */
   private BaseModel parseSectionsOrOther(BaseModel parent, Element element) throws Exception {

      String name = element.getAttribute("name");
      if (name.equalsIgnoreCase("_instance")) {
         name = fProvider.getName();
      }
//      String description = element.getAttribute("description");
      String toolTip  = getToolTip(element);

      BaseModel model = null;
      
      if (element.getTagName() == "fragment") {
         /*
          * Parse fragment as if it was a continuation of the parent elements
          * This handles fragments that just include a href= include a <peripheralPage>
          */
         for (Node subNode = element.getFirstChild();
               subNode != null;
               subNode = subNode.getNextSibling()) {
            if (subNode.getNodeType() != Node.ELEMENT_NODE) {
               continue;
            }
            model = parseSectionsOrOther(parent, (Element) subNode);
         }
      }
      else if (element.getTagName() == "section") {
         model = new SectionModel(parent, name, toolTip);
         parseSectionsOrOtherContents(model, element);
      }
      else if (element.getTagName() == "list") {
         BaseModel tModel = new ListModel(parent, name);
         parseSectionsOrOtherContents(tModel, element);
         parent.addChild(tModel);
      }
      else {
         throw new Exception("Expected <section> or <list>, found = \'"+element.getTagName()+"\'");
      }
      //      else {
      //         if (model == null) {
      //            model = new ParametersModel(parent, name, description);
      //            parseChildModels(model, topElement);
      //            break;
      //         }
      //         else {
      //            parseControlItem(element);
      //         }
      //      }
      
      return model;
   }
   
   /**
    * Parse: <br>
    *    &lt;peripheralPage&gt;<br>
    *    &lt;list&gt;<br>
    *    &lt;section&gt;<br>
    *    &lt;fragment&gt;<br>
    * 
    * @param menuElement
    * 
    * @throws Exception 
    */
   private BaseModel parseSectionsOrOtherContents(BaseModel parent, Element topElement) throws Exception {
      
      String name = topElement.getAttribute("name");
      if (name.equalsIgnoreCase("_instance")) {
         name = fProvider.getName();
      }
//      String tagName = topElement.getTagName();
//      System.err.println("parseSectionsOrOther(<" + tagName + " name="+ name + ">)");
//      String description = topElement.getAttribute("description");
      BaseModel model = null;

      for (Node node = topElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         String tagName = element.getTagName();
//         System.err.println("AT " + element.getTagName());
         if (tagName == "fragment") {
            /*
             * Parse fragment as if it was a continuation of the parent elements
             */
            parseSectionsOrOtherContents(parent, element);
         }
         else if (tagName == "section") {
            if (model != null) {
               throw new Exception("Multiple top-level elements found "+ tagName);
            }
            String toolTip     = getToolTip(topElement);

            model = new SectionModel(parent, name, toolTip);
            parseSectionsOrOther(model, element);
         }
         else if (tagName == "list") {
            BaseModel tModel = new ListModel(parent, name);
            parseSectionsOrOther(tModel, element);
            parent.addChild(tModel);
         }
         else {
            parseChildModel(parent, element);
         }
      }
      return model;
   }

   /**
    * Parse: <br>
    * <ul>
    *    <li>&lt;fragment&gt; referencing a one of the below elements<br>
    *    <li>&lt;peripheralPage&gt;<br>
    *    <li>&lt;list&gt;<br>
    * </ul>
    * @param element    Element to parse
    *     
    * @throws Exception
    */
   private void parsePage(Element element) throws Exception {
      
      String name = element.getAttribute("name");
      if (name.equalsIgnoreCase("_instance")) {
         name = fProvider.getName();
      }
//      System.err.println("parsePage(<" + element.getTagName() + " name="+ name + ">)");

      String tooltip = getToolTip(element);
      
      String tagName = element.getTagName();
      try {
         if (tagName == "fragment") {
            /*
             * Parse fragment as if it was a continuation of the parent elements
             * This handles fragments that just include a href include a <peripheralPage>
             */
            for (Node node = element.getFirstChild();
                  node != null;
                  node = node.getNextSibling()) {
               if (node.getNodeType() != Node.ELEMENT_NODE) {
                  continue;
               }
               parsePage((Element) node);
            }
         }
         else if (tagName == "peripheralPage") {
            fRootModel = new ParametersModel(null, name, tooltip);
            parseSectionsOrOtherContents(fRootModel, element);
         }
         else if (tagName == "list") {
            fRootModel = new ListModel(null, name);
            parseSectionsOrOtherContents(fRootModel, element);
         }
         else {
            throw new Exception("Expected <peripheralPage> or <list>");
         }
      } catch (Exception e) {
         new ErrorModel(fRootModel, "Parse Error", e.getMessage());
         System.err.println("parse(): " + element.getTagName() + ", " + element.getAttribute("name"));
         e.printStackTrace();
      }
   }

   /**
    * @param  provider     Provider to look up variables
    * @param  parent       Parent model needed to replace child in
    * @param  aliasModel   Information for model to instantiate
    * 
    * @return New model created
    * 
    * @throws Exception
    */
   private static BaseModel createModelFromAlias(VariableProvider provider, BaseModel parent, AliasPlaceholderModel aliasModel) throws Exception {
      
      String  key        = aliasModel.getKey();
      boolean isOptional = aliasModel.isOptional();
      
      Variable variable = provider.safeGetVariable(key);
      if (variable == null) {
         if (!isOptional) {
            throw new Exception("Alias not found for '" + key + "' within '"+parent.getName() + "', provider = '"+provider+"'");
         }
         return null;
      }
      String description = aliasModel.getDescription();
      if (!description.isEmpty()) {
         if ((variable.getDescription() != null) && !variable.getDescription().isEmpty()) {
            throw new Exception("Alias tries to change description for " + key);
         }
         variable.setDescription(description);
      }
      String toolTip = aliasModel.getToolTip();
      if ((toolTip != null) && !toolTip.isEmpty()) {
         if ((variable.getDisplayToolTip() != null) && !variable.getDisplayToolTip().isEmpty()) {
            throw new Exception("Alias tries to change toolTip for " + key);
         }
         variable.setToolTip(toolTip);
      }
      VariableModel model = variable.createModel(null);
      boolean isConstant = aliasModel.isConstant();
      model.setConstant(isConstant);
      String displayName = aliasModel.getName();
      if (!displayName.isEmpty()) {
         model.setName(displayName);
      }
      return model;
   }
   
   /**
    * Visits all nodes of the model and instantiates any aliases. <br>
    * Empty categories are removed.
    * 
    * @param  provider  Provider to look up variables
    * @param  parent    Root of the model tree to visit
    * 
    * @throws Exception
    */
   private static void instantiateAliases(VariableProvider provider, BaseModel parent) throws Exception {
      if ((parent == null) || (parent.getChildren()==null)) {
         return;
      }
      ArrayList<BaseModel> children = parent.getChildren();
      ArrayList<Object>    deletedChildren = new ArrayList<Object>();
     
      for (int index=0; index<children.size(); index++) {
         BaseModel model = (BaseModel) children.get(index);
         if (model instanceof AliasPlaceholderModel) {
            AliasPlaceholderModel aliasModel = (AliasPlaceholderModel) model;
            BaseModel newModel = createModelFromAlias(provider, parent, aliasModel);
            // Note: createModelFromAlias() handles missing model errors
            if (newModel == null) {
               // Variable not found and model is optional - delete placeholder
               deletedChildren.add(model);
            }
            else {
               // Replace placeholder with new model
               children.set(index, newModel);
               newModel.setParentOnly(parent);
               if (newModel instanceof CategoryVariableModel) {
                  // Move children to new model
                  newModel.moveChildren(model.getChildren());
                  instantiateAliases(provider, newModel);
               }
            }
         }
         else {
            instantiateAliases(provider, model);
            if ((model instanceof CategoryVariableModel) || (model instanceof CategoryModel)) {
               if ((model.getChildren()==null)||(model.getChildren().isEmpty())) {
                  // Empty category - prune
                  deletedChildren.add(model);
               }
            }
         }
      }
      // Remove deleted children
      children.removeAll(deletedChildren);
   }
   
   /**
    * Parse configuration from document
    * 
    * @param document   Document to parse
    * @param provider   Provides the variables. New variables will be added to this provider
    * @param peripheral Peripheral associated with this document (if any)
    * 
    * @return MenuData containing parsed data
    * 
    * @throws Exception
    */
   private static MenuData parse(Document document, VariableProvider provider, PeripheralWithState peripheral) throws Exception {
      System.out.println("Loading document:" + document.getBaseURI());
      Element documentElement = document.getDocumentElement();
      if (documentElement == null) {
         throw new Exception("Failed to get documentElement");
      }
      ParseMenuXML parser = new ParseMenuXML(provider, peripheral);
      for (Node child = document.getFirstChild(); child != null; child = child.getNextSibling()) {
         if (child.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) child;
         parser.parsePage(element);
      }
      if (parser.fRootModel == null) {
         throw new Exception("No <peripheralPage> found in XML");
      }
      return new MenuData(parser.fRootModel, parser.fTemplateInfos, parser.fValidators, parser.fProjectActionList);
   }

   /**
    * Locate configuration file in USBDM installation
    * 
    * @param name Name of file to find
    * 
    * @return Path to file
    * 
    * @throws Exception if file not found
    */
   private static Path locateFile(String name) throws Exception {
      
      // Add USBDM hardware path
      Path path = Paths.get(DeviceInfo.USBDM_HARDWARE_LOCATION+"/peripherals/"+name);

//      System.err.println("Looking in " + path);
      // For debug try local directory
      if (Files.isRegularFile(path)) {
         return path;
      }
      // Look in USBDM installation\
      String p = Usbdm.getUsbdmResourcePath();
      path = Paths.get(p).resolve(path);
//      System.err.println("Looking in " + path);
      if (!Files.isRegularFile(path)) {
         throw new FileNotFoundException("Failed to find hardware file for '"+ name + "'");
      }
      return path;
   }
   
   /**
    * Parses document from top element
    * 
    * @param peripheralName   Name of peripheral (used for peripheral file name e.g. adc0_diff_a => adc0_diff_a.xml
    * @param peripheral       Provides the variables. New variables will be added to this peripheral
    * 
    * @return Data from model
    * @throws Exception 
    * 
    * Looks for the file in the following locations in order:
    * <li>Relative path : Stationery/Packages/180.ARM_Peripherals/Hardware/peripherals
    * <li>Relative path : "USBDM Resource Path"/Stationery/Packages/180.ARM_Peripherals/Hardware/peripherals
    */
   public static MenuData parsePeriperalFile(String peripheralName, PeripheralWithState peripheral) throws Exception {
      MenuData fData;
      
      try {
         // For debug try local directory
         Path path = locateFile(peripheralName+".xml");
         fData = parse(XML_BaseParser.parseXmlFile(path), peripheral, peripheral);
         fData.fRootModel.setToolTip(peripheralName);
      } catch (FileNotFoundException e) {
         // Some peripherals don't have templates yet - just warn
         throw new Exception("Failed to find peripheral file for "+peripheralName, e);
      } catch (Exception e) {
         e.printStackTrace();
         throw new Exception("Failed to process peripheral file for "+peripheralName, e);
      }
      for (ValidatorInformation v:fData.getValidators()) {
         try {
            // Get validator class
            Class<?> clazz = Class.forName(v.getClassName());
            int dimension = v.getDimension();
            PeripheralValidator validator;
            if (dimension>0) {
                  Constructor<?> constructor = clazz.getConstructor(PeripheralWithState.class, Integer.class, v.getParams().getClass());
                  validator = (PeripheralValidator) constructor.newInstance(peripheral, dimension, v.getParams());
            }
            else {
                  Constructor<?> constructor = clazz.getConstructor(PeripheralWithState.class, v.getParams().getClass());
                  validator = (PeripheralValidator) constructor.newInstance(peripheral, v.getParams());
            }
            peripheral.addValidator(validator);
         } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Failed to add validator '"+v.getClassName()+"' for PeripheralWithState '"+peripheral.getName()+"'", e);
         }
      }
      return fData;
   }

   /**
    * Parses document from top element
    * 
    * @param name                Name of file
    * @param variableProvider    Provides the variables. New variables will be added to this provider
    * 
    * @return Data from model
    * 
    * Looks for the file in the following locations in order:
    * <li>Relative path : Stationery/Packages/180.ARM_Peripherals/Hardware/peripherals
    * <li>Relative path : "USBDM Resource Path"/Stationery/Packages/180.ARM_Peripherals/Hardware/peripherals
    */
   public static MenuData parseMenuFile(String name, VariableProvider variableProvider) throws Exception {
      MenuData fData;
      try {
         // For debug try local directory
         Path path = locateFile(name+".xml");
         fData = parse(XML_BaseParser.parseXmlFile(path), variableProvider, null);
      } catch (Exception e) {
         throw new Exception("Failed to parse "+name+".xml", e);
      }
      for (ParseMenuXML.ValidatorInformation v:fData.getValidators()) {
         try {
            // Get validator class
            Class<?> clazz = Class.forName(v.getClassName());
            Constructor<?> constructor = clazz.getConstructor(PeripheralWithState.class, v.getParams().getClass());
            Validator validator = (Validator) constructor.newInstance(variableProvider, v.getParams());
            variableProvider.addValidator(validator);
         } catch (Exception e) {
            throw new Exception("Failed to add validator "+v.getClassName()+" for VariableProvider " + variableProvider.getName(), e);
         }
      }
      return fData;
   }
}
