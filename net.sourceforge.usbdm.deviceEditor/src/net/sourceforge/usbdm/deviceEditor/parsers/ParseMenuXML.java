package net.sourceforge.usbdm.deviceEditor.parsers;

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiConsumer;
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
    * Parse &lt;for&gt;
    * 
    * @param parentModel
    * @param element
    * @throws Exception
    */
   private void parseForLoop(BaseModel parentModel, Element element) throws Exception {

      if (!checkCondition(element)) {
         return;
      }
      String keys   = getAttribute(element, "keys");
      String values = getAttribute(element, "values");
      String dim    = getAttribute(element, "dim");
      if (dim != null) {
         if (values != null) {
            throw new Exception("Both values and dim attribute given in <for> '" + keys +"'");
         }
         String dims[] = dim.split(",");
         int start;
         int end;
         if (dims.length == 1) {
            start = 0;
            end   = getLongWithVariableSubstitution(dims[0]).intValue();
         }
         else if (dims.length == 2) {
            start = getLongWithVariableSubstitution(dims[0]).intValue();
            end   = getLongWithVariableSubstitution(dims[1]).intValue();
         }
         else {
            throw new Exception("Illegal dim value '"+dim+"' for <for> '"+keys+"'");
         }
         StringBuilder sb = new StringBuilder();
         for (int index=start; index<end; index++) {
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
    * Parse &lt;for&gt;
    * 
    * @param parentModel
    * @param element
    * @throws Exception
    */
   private void parseIfThen(BaseModel parentModel, Element element) throws Exception {
      
      if (!element.hasAttribute("condition")) {
         throw new Exception("<if> requires 'condition' attribute '"+element+"'");
      }
      
      Boolean processNodes  = checkCondition(element);
      Boolean skipRemainder = processNodes;
      
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element elem    = (Element) node;
         String  tagName = elem.getTagName();
         if (tagName == "else") {
            // Process nodes if not already found an active clause 
            processNodes = !skipRemainder;
            continue;
         }
         else if (tagName == "else_if") {
            if (!elem.hasAttribute("condition")) {
               throw new Exception("<else_if> requires 'condition' attribute '"+elem+"'");
            }
            // Get condition for this clause
            processNodes = !skipRemainder && checkCondition(elem);

            // Skip remainder if processing an active else_if clause 
            skipRemainder = skipRemainder||processNodes;
            continue;
         }
         if (processNodes) {
            parseSingleNode(parentModel, node);
         }
      }
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
      return fProvider.safeGetVariable(fProvider.makeKey(key));
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
   private ChoiceVariable safeGetChoiceVariable(String key) {
      return (ChoiceVariable) fProvider.safeGetVariable(fProvider.makeKey(key));
   }
   
   /**
    * Get VariableWithChoices variable with given key from provider
    * 
    * @param key     Key to lookup variable
    * 
    * @return variable or null if not found
    * 
    * @throws Exception if variable exists but is of wrong type
    */
   @SuppressWarnings("unused")
   private VariableWithChoices safeGetVariableWithChoices(String key) {
      return (VariableWithChoices) fProvider.safeGetVariable(fProvider.makeKey(key));
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
    * Get a long value from a string
    * 
    * @param value This may be a number string or the name of a variable
    * 
    * @return Value as Long
    * 
    * @throws NumberFormatException if format is invalid
    */
   protected Long getLongWithVariableSubstitution(String value) throws NumberFormatException {
      try {
         if (Character.isDigit(value.charAt(0))) {
            return parseLong(value);
         }
         // Try variable
         return safeGetVariable(value).getValueAsLong();
      } catch (NumberFormatException e) {
         throw new NumberFormatException("Number not found for " + value);
      }
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
      String attr = getAttribute(element, name);
      if (Character.isDigit(attr.charAt(0))) {
         return safeGetLongAttribute(element, name);
      }
      // Try variable
      Variable var = safeGetVariable(getAttribute(element, name));
      if (var != null) {
         return var.getValueAsLong();
      }
      return null;
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
      String text = doForSubstitutions(getAttribute(element, "toolTip"));
      if (text == null) {
         return text;
      }
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

      String key  = getKeyAttribute(varElement);
      String name = getAttribute(varElement, "name");
      if (name == null) {
         name = Variable.getNameFromKey(key);
      }
      
      boolean replace = Boolean.valueOf(getAttribute(varElement, "replace"));
      boolean modify  = Boolean.valueOf(getAttribute(varElement, "modify"));
      
      Variable newVariable      = null;
      Variable existingVariable = safeGetVariable(key);
      
      if ((existingVariable != null) && replace) {
         // Replacing existing variable - just delete the one found
         removeVariable(existingVariable);
         fPeripheral.removeClockSelector(existingVariable);
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
      String derivedFromName = getAttribute(varElement,"derivedFrom");
      if (derivedFromName != null) {
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
    * <li>description
    * <li>default
    * <li>toolTip (processed)
    * <li>value
    * <li>disabledValue
    * <li>origin
    * <li>derived
    * <li>data
    * <li>errorPropagate
    * <li>target
    * <li>enumStem
    * <li>clockSources
    * <li>constant
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
         variable.setTypeName(otherVariable.getTypeName());
      }
      if (varElement.hasAttribute("description")) {
         variable.setDescription(getAttribute(varElement, "description"));
      }
      if (varElement.hasAttribute("toolTip")) {
         variable.setToolTip(getToolTip(varElement));
      }
      if (varElement.hasAttribute("default")) {
         throw new Exception("default attribute not supported");
//         variable.setDefault(getAttribute(varElement, "default"));
      }
      if (varElement.hasAttribute("value")) {
         // Value is used as default and initial value
         String value = getAttribute(varElement, "value");
         variable.setValue(value);
         variable.setDefault(value);
         variable.setDisabledValue(value);
      }
      if (varElement.hasAttribute("valueFormat")) {
         // Value is used as disabled value
         variable.setValueFormat(getAttribute(varElement, "valueFormat"));
      }
      if (varElement.hasAttribute("disabledValue")) {
         // Value is used as disabled value
         variable.setDisabledValue(getAttribute(varElement, "disabledValue"));
      }
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(getAttribute(varElement, "origin"));
      }
      if (varElement.hasAttribute("derived")) {
         variable.setDerived(Boolean.valueOf(getAttribute(varElement, "derived")));
      }
      if (varElement.hasAttribute("data")) {
         // Internal data vale
         String value = getAttribute(varElement, "data");
         variable.setDataValue(value);
      }
      if (varElement.hasAttribute("errorPropagate")) {
         variable.setErrorPropagate(getAttribute(varElement, "errorPropagate").toUpperCase());
      }
      if (varElement.hasAttribute("target")) {
         variable.setTarget(getAttribute(varElement, "target"));
      }
      if (varElement.hasAttribute("register")) {
         variable.setRegister(getAttribute(varElement, "register"));
      }
      if (varElement.hasAttribute("enumStem")) {
         String enumStem = getAttribute(varElement, "enumStem");
         if (enumStem.isBlank()) {
            enumStem = null;
         }
         variable.setTypeName(enumStem);
      }
      if (varElement.hasAttribute("type")) {
         String type = getAttribute(varElement, "type");
         if (type.isBlank()) {
            type = null;
         }
         variable.setTypeName(type);
      }
      if (varElement.hasAttribute("clockSources")) {
         throw new Exception("clockSources no longer supported in "+varElement+", '"+variable.getName()+"'");
      }
      NodeList forNodes = varElement.getElementsByTagName("for");
      if (forNodes.getLength() > 0) {
         throw new Exception ("<for> no longer supported here "+varElement);
      }
      VariableModel model = variable.createModel(parent);
      model.setConstant(Boolean.valueOf(getAttribute(varElement, "constant")));
      model.setHidden(Boolean.valueOf(getAttribute(varElement, "hidden")));
      return model;
   }

   /**
    * Parse &lt;longOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseLongOption(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
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
         variable.setValueFormat(otherVariable.getValueFormat());
      }
      parseCommonAttributes(parent, varElement, variable);
      if (variable.getValueFormat() == null) {
         variable.setValueFormat(Variable.getBaseNameFromKey(variable.getKey()).toUpperCase()+"(%s)");
      }
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
         variable.setUnits(Units.valueOf(getAttribute(varElement, "units")));
      }
      if (varElement.hasAttribute("step")) {
         variable.setStep(getLongAttribute(varElement, "step"));
      }
      if (varElement.hasAttribute("offset")) {
         variable.setOffset(getLongAttribute(varElement, "offset"));
      }
   }

   /**
    * Parse &lt;doubleOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseDoubleOption(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
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
         variable.setUnits(Units.valueOf(getAttribute(varElement, "units")));
      }
   }

   /**
    * Parse &lt;bitmaskOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseBitmaskOption(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
      BitmaskVariable variable = (BitmaskVariable) createVariable(varElement, BitmaskVariable.class);
      parseCommonAttributes(parent, varElement, variable);
      try {
         variable.setPermittedBits(getLongAttribute(varElement, "bitmask"));
         variable.setBitList(getAttribute(varElement, "bitList"));
      } catch( NumberFormatException e) {
         throw new Exception("Illegal permittedBits value in " + variable.getName(), e);
      }
   }

   /** 
    * Check if condition attached to element
    * 
    * @param element Element to examing
    * 
    * @return true  Condition is true or not present
    * @return false Condition id present and false
    * 
    * @throws Exception
    */
   boolean checkCondition(Element element) throws Exception {
      String  condition     = getAttribute(element, "condition");
      return fTemplateConditionParser.evaluateVariablePresentCondition(condition);
   }
   
   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseChoiceOption(BaseModel parent, Element varElement) throws Exception {

      if (getKeyAttribute(varElement).contains("llwu_pe1_wupe0")) {
         System.err.println("Found " + getKeyAttribute(varElement));
      }
      if (!checkCondition(varElement)) {
         return;
      }
      ChoiceVariable variable = (ChoiceVariable) createVariable(varElement, ChoiceVariable.class);
      parseCommonAttributes(parent, varElement, variable);
      parseChoices(variable, varElement);
      
      if (variable.getTarget() != null) {
         // Add as clock selector
         fPeripheral.addClockSelector(variable);
      }
      if (variable.getTypeName() != null) {
         generateEnum(varElement, variable);
      }
   }

   /// Format string with parameters: description, tooltip, enumClass, body
   String enumTemplate = ""
         + "      \\t/**\n"
         + "      \\t * %s\n"
         + "      \\t *\n"
         + "      \\t * %s\n"
         + "      \\t */\n"
         + "      \\tenum %s%s {\n"
         + "      %s\n"
         + "      \\t};\\n\\n\n";
   
   private void generateEnum(Element varElement, VariableWithChoices variable) throws Exception {

      HashSet<String> enumTemplateMap = fPeripheral.getDeviceInfo().getEnumTemplateMap();
      
      String macroName       = Variable.getBaseNameFromKey(variable.getKey()).toUpperCase();

      String enumStem        = variable.getTypeName();
      if (enumTemplateMap.contains(enumStem)) {
         return;
      }
      enumTemplateMap.add(enumStem);

      String enumType = getAttribute(varElement, "enumType");
      if (enumType != null) {
         enumType = " : "+enumType;
      }
      else {
         enumType = "";
      }
      
      String namespace       = "usbdm";
      String description     = variable.getDescriptionAsCode();
      String tooltip         = variable.getToolTipAsCode();
      
      String valueFormat     = varElement.getAttribute("valueFormat");
      if (valueFormat.isBlank()) {
         valueFormat = macroName+"(%s)";
      }
      
      TemplateInformation templateInfo = addTemplate("", namespace);
      
      ChoiceData[] choiceData = variable.getData();
      
      StringBuilder body = new StringBuilder();
      
      // Use enumStem attribute
      if ((enumStem == null) || enumStem.isBlank()) {
         throw new Exception("enumStem is missing in " + variable);
      }
      String enumClass  = Character.toUpperCase(enumStem.charAt(0)) + enumStem.substring(1);

      String[] enumName  = new String[choiceData.length];
      String[] values    = new String[choiceData.length];
      int enumNameMax    = 0;
      int valueMax       = 0;

      for (int index=0; index<choiceData.length; index++) {

         String enumValue = choiceData[index].getEnumName();
         if ((enumValue == null) || enumValue.isBlank()) {
            throw new Exception("enumTemplate - enum data is incomplete in choice '" + choiceData[index].getName() + "' ='"+variable+"'");
         }
         enumName[index] = enumClass+"_"+enumValue;
         values[index]   = String.format(valueFormat, choiceData[index].getValue())+",";
         enumNameMax     = Math.max(enumNameMax, enumName[index].length());
         valueMax        = Math.max(valueMax, values[index].length());
      }
      // Create enums body
      for (int index=0; index<choiceData.length; index++) {
         String comment  = choiceData[index].getName();
         body.append(String.format("\\t   %-"+enumNameMax+"s = %-"+valueMax+"s ///< %s\n", enumName[index], values[index], comment));
      }
      
      // Create enum declaration
      String entireEnum = String.format(enumTemplate, description, tooltip, enumClass, enumType, body);
      templateInfo.addText(entireEnum);
   }

   /**
    * Get an attribute and apply usual substitutions
    *  <li>"$(_name)"         => e.g FTM2                    (fProvider.getName())
    *  <li>"$(_base_class)"   => e.g FTM0 => Ftm             (fPeripheral.getClassBaseName())
    *  <li>"$(_instance)"     => e.g FTM0 => 0, PTA => A     (fPeripheral.getInstance())
    *  <li>"$(_class)"        => e.g FTM2 => Ftm2            (fPeripheral.getClassName())
    *  <li>"$(_basename)"     => e.g FTM0 => FTM, PTA => PT  (fPeripheral.getBaseName())
    *  <li>For loop substitution
    *  
    * @param element    Element to obtain attribute from
    * @param attrName   Name of attribute
    * 
    * @return  modified attribute or null if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed  
    */
   String getAttribute(Element element, String attrName) throws Exception {
      
      if (!element.hasAttribute(attrName)) {
         return null;
      }
      String res = fForStack.doForSubstitutions(element.getAttribute(attrName));
      res = res.replace("$(_name)", fProvider.getName());
      if (fPeripheral != null) {
         res = res.replace("$(_base_class)", fPeripheral.getClassBaseName());
         res = res.replace("$(_instance)",   fPeripheral.getInstance());
         res = res.replace("$(_class)",      fPeripheral.getClassName());
         res = res.replace("$(_basename)",   fPeripheral.getBaseName());         
      }
      return res;
   }
   
   /**
    * Get element text and apply usual substitutions
    *  <li>"$(_name)"     => fProvider.getName()
    *  <li>"$(_instance)" => fPeripheral.getInstance()
    *  <li>For loop substitution
    *  
    * @param node    Element to obtain attribute from
    * 
    * @return  modified element text
    * 
    * @throws Exception If for-loop completed  
    */
   String getText(Node node) throws Exception {

      String bodyText = fForStack.doForSubstitutions(node.getTextContent());
      bodyText = bodyText.replace("$(_name)", fProvider.getName());
      if (fPeripheral != null) {
         bodyText = bodyText.replace("$(_instance)", fPeripheral.getInstance());
      }
      return bodyText;
   }
   
   /** 
    * Get attribute 'key' or 'name' and apply usual key transformations
    *  <li>"$(_name)"     => fProvider.getName()
    *  <li>"$(_instance)" => fPeripheral.getInstance()
    *  <li>For loop substitution
    *  <li>fprovider.MakeKey()
    * 
    * @param element    Element to examine
    * 
    * @return  Modified key
    * 
    * @throws Exception
    */
   String getKeyAttribute(Element element) throws Exception {
      String key = getAttribute(element, "key");
      if (key == null) {
         key = getAttribute(element, "name");
      }
      if (key == null) {
         return null;
      }
      return fProvider.makeKey(key);
   }
   
   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseAddChoices(BaseModel parent, Element varElement) throws Exception {

      String key = getKeyAttribute(varElement);
      if (key == null) {
         throw new Exception("<addChoice> must have key attribute");
      }
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
      
      if (!checkCondition(varElement)) {
         return;
      }
      StringVariable variable = (StringVariable) createVariable(varElement, StringVariable.class);
      parseCommonAttributes(parent, varElement, variable);
      
      if (variable.getTarget() != null) {
         // Add as clock selector
         fPeripheral.addClockSelector(variable);
      }
   }

   private void parseCategory(BaseModel parent, Element varElement) throws Exception {
      
      CategoryModel model = new CategoryModel(parent, getAttribute(varElement, "name"));
      boolean hidden = Boolean.parseBoolean(getAttribute(varElement, "hidden"));
      model.setHidden(hidden);
      model.setToolTip(getAttribute(varElement, "toolTip"));
      model.setSimpleDescription(getAttribute(varElement, "description"));
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
      
      categoryVariable.setValue(getAttribute(varElement, "value"));
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
      parseCommonAttributes(parent, varElement, variable);
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
   }

   /**
    * Parse &lt;binaryOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseBinaryOption(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
      BooleanVariable variable = (BooleanVariable)createVariable(varElement, BooleanVariable.class);
      if (variable == null) {
         return;
      }
      parseCommonAttributes(parent, varElement, variable);
      parseChoices(variable, varElement);
      if (variable.getTypeName() != null) {
         generateEnum(varElement, variable);
      }
      if (variable.getTarget() != null) {
         // Add as clock selector
         fPeripheral.addClockSelector(variable);
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
      
      if (!checkCondition(irqElement)) {
         return;
      }
      IrqVariable variable = (IrqVariable) createVariable(irqElement, IrqVariable.class);
      parseCommonAttributes(parent, irqElement, variable).getVariable();

      variable.setPattern(getAttribute(irqElement, "pattern"));
      variable.setClassHandler(getAttribute(irqElement, "classHandler"));
      
      fPeripheral.addIrqVariable(variable);
   }

   /**
    * Parse &lt;PinListOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parsePinListOption(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
      PinListVariable variable = (PinListVariable) createVariable(varElement, PinListVariable.class);
      parseCommonAttributes(parent, varElement, variable);
      variable.setPeripheral(fPeripheral);
      try {
         if (varElement.hasAttribute("size")) {
            variable.setMaxListLength(getLongAttribute(varElement, "size"));
         }
      } catch( NumberFormatException e) {
         throw new Exception("Illegal size value in " + variable.getName(), e);
      }
   }

   /**
    * Parse &lt;aliasOption&gt; element<br>
    * 
    * @param stringElement
    * @throws Exception 
    */
   private AliasPlaceholderModel parseAliasOption(BaseModel parent, Element stringElement) throws Exception {
      String  key          = getKeyAttribute(stringElement);
      String  displayName  = getAttribute(stringElement, "displayName");
      String  description  = getAttribute(stringElement, "description");
      String  toolTip      = getToolTip(stringElement);

      if (key.isEmpty()) {
         throw new Exception("Alias requires key "+displayName);
      }
      
      boolean isConstant  = Boolean.valueOf(getAttribute(stringElement, "constant"));
      boolean isOptional  = Boolean.valueOf(getAttribute(stringElement, "optional"));
      
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

      if (!checkCondition(element)) {
         return;
      }
      String key         = getKeyAttribute(element);
      String name        = getAttribute(element, "name");
      String value       = getAttribute(element, "value");
      String description = getAttribute(element, "description");
      boolean isWeak     = Boolean.valueOf(getAttribute(element, "weak"));
      boolean isReplace  = Boolean.valueOf(getAttribute(element, "replace"));
      if (key.isBlank()) {
         throw new Exception("<constant> must have 'key' attribute, value='"+value+"'");
      }
      if (name == null) {
         name = Variable.getNameFromKey(key);
      }
         
      boolean isDerived  = true;
      if (element.hasAttribute("derived")) {
         isDerived = Boolean.valueOf(getAttribute(element, "derived"));
      }
      boolean isHidden  = true;
      if (element.hasAttribute("hidden")) {
         isHidden = Boolean.valueOf(getAttribute(element, "hidden"));
      }
      if (value == null) {
         value="true";
      }
      Variable var = safeGetVariable(key);
      if (var != null) {
         if (isWeak) {
            // Ignore constant
         }
         else if (isReplace) {
            // Replace constant value
            var.setValue(value);
            if (element.hasAttribute("name")) {
               var.setName(name);
            }
            if (element.hasAttribute("description")) {
               var.setDescription(description);
            }
            return;
         }
         else {
            throw new Exception("Constant multiply defined, name="+name+", key=" + key);
         }
      }
      else {
         var = new StringVariable(name, key);
         var.setValue(value);
         var.setDescription(description);
         var.setDerived(isDerived);
         var.setHidden(isHidden);
         fProvider.addVariable(var);
      }
   }
   
   static class TemplateConditionParser {
      
      private final VariableProvider fProvider;
      
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

         while(index<condition.length()) {
            char ch = condition.charAt(index);
            if (Character.isJavaIdentifierPart(ch) || (ch == '/')) {
               sb.append(condition.charAt(index++));
               idFound = (ch!='/');
               continue;
            }
            break;
         };
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
       * Evaluate a variable present condition. <br>
       * If a variable is present is it considered true otherwise false. <br>
       * The variable is not evaluated. <br>
       * This is used before variable have a valid value.
       * 
       * @param condition Boolean expression used to indicate condition
       * 
       * @return true if condition is empty, null or evaluates as true
       * 
       * @return false otherwise
       * 
       * @throws Exception 
       */
      public boolean evaluateVariablePresentCondition(String condition) throws Exception {
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
      
   private static class BiconsumerReplacer implements BiConsumer<String, String> {
      String text;
      
      BiconsumerReplacer(String text) {
         this.text = text;
         if (text == null) {
            System.err.println("text is null");
         }
      }
      
      @Override
      public void accept(String key, String value) {
         if (key==null) {
            System.err.println("key is blank, value = "+value);
         }
         if (value==null) {
            System.err.println("value is blank, res = "+key);
         }
         text = text.replace(key, value);
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
   private String doTemplateSubstitutions(String text, Map<String, String> substitutions) {
      if (text == null) {
         return null;
      }
      // Replace body first!
      String bodyText = substitutions.get("%body");
      if (bodyText != null) {
         text = text.replace("%body", bodyText);
      }
      
      BiconsumerReplacer br = new BiconsumerReplacer(text);
      substitutions.forEach(br);
      return br.text;
   }
   
   /**
    * Try to determine entire register name e.g. sim->SOPT1
    * 
    * @param controlVar    Variable to obtain information from
    * 
    * @note The controlVar is used to obtain an (optional) register name.<br>
    *       The register attribute name may be necessary as some registers have '_' as part of their<br>
    *       name and slicing on '_' is ambiguous.  <br>
    *       If not provided, the register name is assumed to not contain '_'.
    *       
    * @return Full register name
    */
   String deduceRegister(Variable controlVar) {
      
      String register = null;
      String variableKey  = controlVar.getBaseNameFromKey();
      String registerName = controlVar.getRegister();
      
      if (registerName != null) {
         Pattern p = Pattern.compile("(.+)_"+registerName+"_(.+)");
         Matcher m = p.matcher(variableKey);
         if (m.matches()) {
            register = m.group(1)+"->"+registerName.toUpperCase();
         }
      }
      else {
         String peripherals[] = {
               fPeripheral.getName().toLowerCase(),      // e.g. FTM2
               fPeripheral.getBaseName().toLowerCase()}; // e.g. FTM0 => FTM, PTA => PT
         for (String peripheral:peripherals) {
            Pattern p = Pattern.compile(peripheral+"_([a-zA-Z0-9]*)_(.+)");
            Matcher m = p.matcher(variableKey);
            if (m.matches()) {
               register = peripheral+"->"+(m.group(1).toUpperCase());
               break;
            }
         }
      }
      return register;
   }
   
   /**
    * Construct template substitutions<br><br>
    * 
    * Values obtained from <b>controlVarName</b>
    * 
    * <br><br>
    * Substitutions obtained from <b>controlVarName</b>
    * <li>%description              Description from controlVar
    * <li>%shortDescription         Short description from controlVar
    * <li>%tooltip                  Tool-tip from controlVar
    * <li>%enumClass                Based on enumStem with upper-case first letter
    * <li>%enumParam                Based on enumStem with lower-case first letter
    * <li>%bareValueExpression      Of form <b>$(controlVarName)</b>
    * <li>%symbolicValueExpression  Of form <b>$(controlVarName.enum[])</b>
    * <li>%defaultValueExpression   Of form <b>CONTROL_VAR_NAME($(control_var_name))</b>
    * <li>%defaultClockExpression   Based on controlVarName etc. Similar to sim->SOPT2 = (sim->SOPT2&~%mask) | %enumParam;
    * <li>%defaultMaskingExpression Based on controlVarName etc. Similar to (sim->SOPT2&%mask)
    * <br><br>
    * Substitutions obtained from <b>element</b>
    * <li>%variable                 Variable name from condition
    * <li>%mask                     From &lt;mask&gt; or deduced from &lt;controlVarName&gt; e.g. "SIM_SOPT_REG_MASK" (_MASK is added)
    * <li>&lt;register&gt;          Used to help extract mask etc from condition if required e.g. pll_sic <br><br>
    *  
    * @param element          Element 
    * @param condition   Control var to obtain information from
    * 
    * @return  List of substitutions
    * @throws Exception 
    */
   Map<String,String> getTemplateSubstitutions(Element element) throws Exception {
      
      Map<String,String> substitutions = new HashMap<String,String>();
      
      String description                = "'%description' not available in this template";
      String shortDescription           = "'%shortDescription' not available in this template";
      String tooltip                    = "'%tooltip' not available in this template";
      String defaultValueExpression     = "'%defaultValueExpression' not available in this template";
      String bareValueExpression        = "'%bareValueExpression' not available in this template";
      String mask                       = "'%mask' not available in this template";
      String symbolicValueExpression    = "'%symbolicValueExpression' is not valid here"; 
      String enumParam                  = "'%enumParam' is not valid here";
      String enumClass                  = "'%enumClass' is not valid here";     
      String defaultClockExpression     = "'%defaultClockExpression' is not valid here";
      String defaultMaskingExpression   = "'%defaultMaskingExpression' is not valid here";

//      if (!controlVarName.isBlank() && (controlVar == null)) {
//         System.err.println("Unable to find variable " + controlVarName);
//      }
      String register = null;
      
      String variable_key = getAttribute(element, "variable");
      if (variable_key == null) {
            variable_key = "%variable is not available here";
         }
      else {
         String macro = variable_key.toUpperCase();
         if (macro.endsWith(".")) {
            throw new Exception("keys ending with '.' no longer accepted '"+variable_key+"'");
         }
         if (macro.endsWith("[]")) {
            macro = macro.substring(0, macro.length()-2);
         }
         if (macro.endsWith("[0]")) {
            macro = macro.substring(0, macro.length()-3);
         }
         mask = macro+"_MASK";
         if (element.hasAttribute("mask")) {
            mask = getAttribute(element, "mask");
         }
         bareValueExpression    = "($("+variable_key+"))";
         defaultValueExpression = macro+bareValueExpression;

         Variable controlVar = safeGetVariable(variable_key);
         if (controlVar != null) {
            String value = controlVar.getDescriptionAsCode();
            if (value != null) {
               description = value;
            }
            value = controlVar.getShortDescription();
            if (value != null) {
               shortDescription = value;
            }
            value = controlVar.getToolTipAsCode();
            if (value != null) {
               tooltip = value;
            }

            // enumStem can be on template or sorceVar
            String enumStem = controlVar.getTypeName();
            if (enumStem == null) {
               enumStem = getAttribute(element, "enumStem");
            }
            if (enumStem != null) {
               enumClass  = Character.toUpperCase(enumStem.charAt(0)) + enumStem.substring(1);
               enumParam  = Character.toLowerCase(enumStem.charAt(0)) + enumStem.substring(1);
            }
            symbolicValueExpression = "$("+variable_key+".enum[])";

            // Try to deduce register
            register = deduceRegister(controlVar);
            
            if (register != null) {
               defaultClockExpression   = register+" = ("+register+"&~"+mask+") | "+enumParam+";";
               defaultMaskingExpression = register+"&"+mask;
            }
         }
      }
      if (register == null) {
         register = "'%register' is not valid here";
      }
      if (element.hasAttribute("mask")) {
         mask = getAttribute(element, "mask");
      }
      
      substitutions.put("%variable",                  variable_key);
      substitutions.put("%mask",                      mask);
      substitutions.put("%enumClass",                 enumClass);
      substitutions.put("%enumParam",                 enumParam);
      substitutions.put("%description",               description);
      substitutions.put("%shortDescription",          shortDescription);
      substitutions.put("%tooltip",                   tooltip);
      substitutions.put("%defaultValueExpression",    defaultValueExpression);
      substitutions.put("%bareValueExpression",       bareValueExpression);
      substitutions.put("%symbolicValueExpression",   symbolicValueExpression);
      substitutions.put("%defaultClockExpression",    defaultClockExpression);
      substitutions.put("%defaultMaskingExpression",  defaultMaskingExpression);
      substitutions.put("%register",                  register);

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

      if (!checkCondition(element)) {
         return;
      }
      String key       = getKeyAttribute(element);
      String namespace = getAttribute(element, "namespace"); // info|usbdm|class|all
      String variable  = getAttribute(element, "variable");
      
      if (namespace.isBlank()) {
         throw new Exception("Template is missing namespace, key='" + key + "'");
      }
      if (!key.isBlank() && !namespace.equals("all")) {
         throw new Exception("Named templates must have 'all' namespace, key='" + key + "'");
      }
      if (key.isBlank() && namespace.equals("all")) {
         throw new Exception("Templates must have 'key' attribute in 'all' namespace, peripheral='" + fPeripheral.getName() + "'");
      }
      if ((variable != null) && (safeGetVariable(variable) == null)) {
         return;
      }
      
      Map<String,String> substitutions = getTemplateSubstitutions(element);
      
      TemplateInformation templateInfo = addTemplate(key, namespace);
      
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
            String bodyText = getText(node);
            bodyText = doTemplateSubstitutions(bodyText, substitutions);
            templateInfo.addText(bodyText);
            continue;
         }
      }
   }
   
   /**
    * 
    * Creates a method to set an entire register given list of variables to use for parameter generation
    * <p>
    * 
    * Extra attributes:
    * <li> &lt;variables&gt; List of variables being used<br><br>
    * <li> &lt;nonDefaultParams&gt; Number of parameters to not have default values (defaults to 1)<br><br>
    * 
    * Within the template:
    * <li>%comments     Generated Doxygen @param comments for variables
    * <li>%params       Generated comma-separated list of variables for use as parameters
    * <li>%expression   Generated '|'-separated list of variables for use as expression
    * 
    * @param element
    * @throws Exception
    */
   private void parseSetTemplate(Element element) throws Exception {
      if (!checkCondition(element)) {
         return;
      }
      String key       = getKeyAttribute(element);
      String namespace = getAttribute(element, "namespace"); // info|usbdm|class|all
      
      if (namespace.isBlank()) {
         throw new Exception("setTemplate is missing namespace, key='" + key + "'");
      }
      if ((key != null) && !namespace.equals("all")) {
         throw new Exception("Named setTemplates must have 'all' namespace, key='" + key + "'");
      }
      if ((key == null) && namespace.equals("all")) {
         throw new Exception("setTemplates must have 'key' attribute in 'all' namespace, peripheral='" + fPeripheral.getName() + "'");
      }
      Long numberOfDefaultParams = safeGetLongAttribute(element, "nonDefaultParams");
      
      String varNames[] = getAttribute(element, "variables").split(",");
      if (varNames.length == 0) {
         throw new Exception("setTemplate is missing 'variables' attribute, key='" + key + "'");
      }
      
      ArrayList<Variable> variables = new ArrayList<Variable>();
      int paramCount=0;
      for (String varName:varNames) {
         Variable choiceVar = safeGetVariable(varName.trim());
         if (choiceVar==null) {
            // Reduce numberOfDefaultParams if related parameter is missing 
            // but keep at least 1 non-default
            if ((numberOfDefaultParams>1) && (paramCount < numberOfDefaultParams)) {
               numberOfDefaultParams--;
            }
            continue;
         }
         variables.add(choiceVar);
      }
      if (variables.isEmpty()) {
         // No variables exist - don't generate method
         return;
      }
      int maxNameLength = 0;
      for (int index=0; index<variables.size(); index++) {
         maxNameLength = Math.max(maxNameLength, variables.get(index).getTypeName().length());
      }
      StringBuilder comments    = new StringBuilder();
      StringBuilder params      = new StringBuilder();
      StringBuilder expression  = new StringBuilder();
      StringBuilder mask        = new StringBuilder();
      
      for (int index=0; index<variables.size(); index++) {
         Variable var = variables.get(index); 
         String typeName  = var.getTypeName();
         String paramType = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);
         String paramName = Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
         String comment   = var.getToolTipAsCode();
         String defaultV  = var.getDefaultParameterValue();
         String name      = var.getName();
         if (index>0) {
            comments.append(",\n");
            params.append(",\n");
            expression.append('|');
            mask.append('|');
         }
         comments.append(String.format("\\t * @param %"+(-maxNameLength)+"s %s", paramName, comment));
         if (index<numberOfDefaultParams) {
            params.append(String.format("\\t      %"+(-maxNameLength)+"s %s", paramType, paramName));
         }
         else {
            params.append(String.format("\\t      %"+(-maxNameLength)+"s %"+(-maxNameLength)+"s = %s", paramType, paramName, defaultV));
         }
         expression.append(paramName);
         mask.append(name.toUpperCase()+"_MASK");
      }
      String register = deduceRegister(variables.get(0));
      
      Map<String,String> substitutions = getTemplateSubstitutions(element);
      substitutions.put("%comments",    comments.toString());
      substitutions.put("%params",      params.toString());
      substitutions.put("%expression",  expression.toString());
      substitutions.put("%mask",        mask.toString());
      substitutions.put("%register",    register);
      
      TemplateInformation templateInfo = addTemplate(key, namespace);
      
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
            String bodyText = getText(node);
            bodyText = doTemplateSubstitutions(bodyText, substitutions);
            templateInfo.addText(bodyText);
            continue;
         }
      }
   }

   /**
    * 
    * Creates a method to set an entire register given list of variables to use for parameter generation
    * <p>
    * 
    * Extra attributes:
    * <li> &lt;variables&gt; List of variables being used<br><br>
    * <li> &lt;nonDefaultParams&gt; Number of parameters to not have default values (defaults to 1)<br><br>
    * 
    * Within the template:
    * <li>%comments     Generated Doxygen @param comments for variables
    * <li>%params       Generated comma-separated list of variables for use as parameters
    * <li>%expression   Generated '|'-separated list of variables for use as expression
    * 
    * @param element
    * @throws Exception
    */
   private void parseInitialValueTemplate(Element element) throws Exception {
      
      if (!checkCondition(element)) {
         return;
      }
      String key       = getKeyAttribute(element);
      String namespace = getAttribute(element, "namespace"); // info|usbdm|class|all
      
      if (namespace.isBlank()) {
         throw new Exception("initialValueTemplate is missing namespace, key='" + key + "'");
      }
      if ((key != null) && !namespace.equals("all")) {
         throw new Exception("Named initialValueTemplate must have 'all' namespace, key='" + key + "'");
      }
      if ((key == null) && namespace.equals("all")) {
         throw new Exception("initialValueTemplate must have 'key' attribute in 'all' namespace, peripheral='" + fPeripheral.getName() + "'");
      }
      String terminator = ";";
      if (element.hasAttribute("terminator")) {
         terminator = getAttribute(element, "terminator");
      }
//      if (key.contains("McgClockInfoEntries")) {
//         System.err.println("parseInitialValueTemplate '"+key+"'");
//      }

      String varNames[] = getAttribute(element, "variables").split(",");
      if (varNames.length == 0) {
         throw new Exception("initialValueTemplate is missing 'variables' attribute, key='" + key + "'");
      }
      if (varNames[0].contains("sim_sopt1_osc32kout")) {
         System.err.println("Found " + varNames[0]);
      }
         
      ArrayList<Variable> variables = new ArrayList<Variable>();
      for (String varName:varNames) {
         Variable choiceVar = safeGetVariable(varName.trim());
         if (choiceVar==null) {
            continue;
         }
         variables.add(choiceVar);
      }
      if (variables.isEmpty()) {
         // No variables exist - don't generate method
         return;
      }

      StringBuilder expression  = new StringBuilder();
      
      Variable lastVar = variables.get(variables.size()-1);
      
      for (Variable var:variables) {
         String value;
         if (var instanceof VariableWithChoices) {
            value = "$("+var.getKey()+".enum[])";
         }
         else {
            value = "$("+var.getKey()+".formattedValue)";
//            value = "$("+var.getKey()+")";
         }
         String comment       = "$("+var.getKey()+".description)";

         expression.append("\n\\t   " + value);
         if (var == lastVar) {
            expression.append(terminator+"  // "); 
         }
         else {
            expression.append(" | // "); 
         }
         expression.append(comment);
      }
      
      String register = deduceRegister(variables.get(0));
      if (register == null) {
         register = "'%register' is not valid here";
      }
      
      Map<String,String> substitutions = getTemplateSubstitutions(element);
      substitutions.put("%expression",  expression.toString());
      substitutions.put("%register",    register);
      
      TemplateInformation templateInfo = addTemplate(key, namespace);
      
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
            String bodyText = getText(node);
            bodyText = doTemplateSubstitutions(bodyText, substitutions);
            templateInfo.addText(bodyText);
            continue;
         }
      }
   }

   /**
    * Expected attributes:<br>
    * <li>&lt;name&gt; - Used to place value at arbitrary location (creates variable of that name)
    * <li>&lt;namespace&gt; (info|usbdm|class|all) - Scope where template is available in
    * <li>&lt;&gt; - Name of existing <b>choice</b> or <b>binary</b> variable used as information source
    * <li>&lt;returnFormat&gt; - Used for writing return value e.g. String.format("XXXX(%s)", enumName)<br>
    * 
    * &lt;valueFormat&gt; default to upper-cased value based on &lt;&gt; if needed<br>
    * &lt;template&gt; and &lt;enumStem&gt;+&lt;valueFormat&gt; are alternatives<br>
    * &lt;returnFormat&gt; is only required if %xxx is used<br><br>
    * 
    * Substitutions in Text :<br>
    * <li>%tooltip - From &lt;&gt; variable
    * <li>%description - From &lt;&gt; variable
    * <li>%enumClass  - Based on enumStem with upper-case first letter
    * <li>%enumParam  - Based on enumStem with lower-case first letter
    * <li>%mask - From &lt;mask&gt; or deduced from &lt;&gt;
    * <li>%defaultClockExpression - Based on  etc. Similar to sim->SOPT2 = (sim->SOPT2&~%mask) | %enumParam;
    * <li>%defaultMaskingExpression - Based on  etc. Similar to (sim->SOPT2&%mask)
    * <li>%body - Constructed case clauses
    * 
    * @param element
    * @throws Exception
    */
   private void parseClockCodeTemplate(Element element) throws Exception {
      
      String key          = getKeyAttribute(element);
      if (!checkCondition(element)) {
         return;
      }
      String namespace    = getAttribute(element, "namespace");
      String variable     = getAttribute(element, "variable");
      String returnFormat = getAttribute(element, "returnFormat");

      if (namespace.isBlank()) {
         throw new Exception("ClockCodeTemplate is missing namespace, key='" + key + "'");
      }
      if (!key.isBlank() && !namespace.equals("all")) {
         throw new Exception("Named ClockCodeTemplate must have 'all' namespace, key='" + key + "'");
      }
      if (key.isBlank() && namespace.equals("all")) {
         throw new Exception("ClockCodeTemplate must be named in 'all' namespace, peripheral='" + fPeripheral.getName() + "'");
      }
      if (variable.isBlank()) {
         throw new Exception("ClockCodeTemplate must have 'variable' attribute, peripheral='" + 
               fPeripheral.getName() + "', key= '" + key + "'");
      }
      VariableWithChoices choiceVar = (VariableWithChoices) safeGetVariable(variable);
      if (choiceVar == null) {
         return;
      }
      String enumStem = choiceVar.getTypeName();
      if (enumStem == null) {
         enumStem = getAttribute(element, "enumStem");
      }
      if (variable.isBlank()) {
         throw new Exception("ClockCodeTemplate must have 'variable' attribute, peripheral='" + 
               fPeripheral.getName() + "', enumStemAttr= '" + enumStem + "'");
      }

      if (enumStem.isBlank()) {
         throw new Exception("ClockCodeTemplate must have 'enumStem' attribute, peripheral='" + 
               fPeripheral.getName() + "', cond= '" + variable + "'");
      }
      TemplateInformation templateInfo = addTemplate(key, namespace);
      
      StringBuilder body = new StringBuilder();
      
      ChoiceData[] choiceData = choiceVar.getData();
      
      String[] enumNames      = new String[choiceData.length];
      String[] returnValues   = new String[choiceData.length];
      
      String   comment;
      int enumNameMax    = 0;
      int returnValueMax = 0;
      
      if (returnFormat != null) {
         // Create body for case statement if used
         for (int index=0; index<choiceData.length; index++) {

            String enumName  = choiceData[index].getEnumName();
            String codeValue = choiceData[index].getCodeValue();
            if ((enumName == null) || (codeValue == null)) {
               throw new Exception("Choice '"+choiceData[index].getName()+"' is missing enum/code value in "+choiceVar);
            }
            enumNames[index]     = enumStem+"_"+enumName;
            enumNameMax          = Math.max(enumNameMax, enumNames[index].length());
            returnValues[index]  = String.format(returnFormat+";", codeValue);
            returnValueMax       = Math.max(returnValueMax, returnValues[index].length());
         }
//         if (choiceVar instanceof BooleanVariable) {
//            final String bFormat = 
//                  "\n"+
//                  "\\t   if (%s) {\n" +
//                  "\\t      return %s\n" +
//                  "\\t   }\n" +
//                  "\\t   else {\n" +
//                  "\\t      return %s\n" +
//                  "\\t   };\n";
//            body.append(String.format(bFormat, "(%defaultMaskingExpression) == "+enumNames[0], returnValues[0], returnValues[1]));
//         }
//         else {
            final String format = "\\t      case %-"+enumNameMax+"s : return %-"+returnValueMax+"s %s\n";
            for (int index=0; index<choiceData.length; index++) {
               comment  = "///< "+choiceData[index].getName();
               body.append(String.format(format, enumNames[index], returnValues[index], comment));
            }
//         }
      }
      Map<String, String> substitutions = getTemplateSubstitutions(element);
      substitutions.put("%body",                      body.toString());

      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
            String bodyText = getText(node);
            bodyText = doTemplateSubstitutions(bodyText, substitutions);
            templateInfo.addText(bodyText);
            continue;
         }
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
      }
   }

   private void parseDeleteOption(Element element) throws Exception {
      
      if (!checkCondition(element)) {
         return;
      }
      String key = getKeyAttribute(element);
      if (key.isBlank()) {
         throw new Exception("<deleteOption> must have key attribute");
      }
      fPeripheral.removeClockSelector(safeGetVariable(key));

      boolean mustExist = Boolean.parseBoolean(getAttribute(element, "mustExist"));
      boolean wasDeleted = fProvider.removeVariable(key);

      if (mustExist  && !wasDeleted) {
         throw new Exception("Variable '" + key + "' was not found to delete in deleteOption");
      }
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
      String key         = getKeyAttribute(element);
      String toolTip     = getToolTip(element);

      if (tagName == "fragment") {
         for (Node node = element.getFirstChild();
               node != null;
               node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
               continue;
            }
            parseChildModel(parentModel, (Element) node);
         }
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
      else if (tagName == "if") {
         parseIfThen(parentModel, element);
      }
      else if (tagName == "section") {
         BaseModel model = new ParametersModel(parentModel, key, toolTip);
         parseChildModels(model, element);
      }
      else if (tagName == "list") {
         BaseModel model = new ListModel(parentModel, key);
         parseSectionsOrOther(model, element);
      }
      else if (tagName == "signals") {
         parseSignalsOption(parentModel, element);
      }
      else if (tagName == "validate") {
         fValidators.add(parseValidate(element));
      }
      else if (tagName == "clockCodeTemplate") {
         parseClockCodeTemplate(element);
      }
      else if (tagName == "template") {
         parseTemplate(element);
      }
      else if (tagName == "initialValueTemplate") {
         parseInitialValueTemplate(element);
      }
      else if (tagName == "setTemplate") {
         parseSetTemplate(element);
      }
      else if (tagName == "addDependency") {
         fPeripheral.addDependency(getKeyAttribute(element));
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
//               System.err.println("Adding " + var);
                  fProvider.addVariable(var);
               }
               return Visitor.CONTINUE;
            }}, null);
         fProjectActionList.addProjectAction(pal);
      }
      else {
         throw new Exception("Unexpected tag in parseControlItem(), \'"+tagName+"\'");
      }
   }

   /**
    * Create and add template<br>
    * 
    * @param templateKey        Key used to index template
    * @param namespace  Namespace for template (info, usbdm, class)
    * @param dimension  Dimension for array template
    * @param contents   Text contents for template
    * 
    * @throws Exception 
    */
   private TemplateInformation addTemplate(String key, String namespace, int dimension) throws Exception {
      
      if (key == null) {
         key = "";
      }
      TemplateInformation templateInfo = new TemplateInformation(key, namespace, dimension);

      String templateKey = MenuData.makeKey(key, namespace);

      ArrayList<TemplateInformation> templateList = fTemplateInfos.get(templateKey);
      if (templateList == null) {
         templateList = new ArrayList<TemplateInformation>();
         fTemplateInfos.put(templateKey, templateList);
      }
      templateList.add(templateInfo);
      return templateInfo;
   }

   /**
    * Create and add template<br>
    * 
    * @param key        Key used to index template
    * @param namespace  Namespace for template (info, usbdm, class)
    * 
    * @throws Exception 
    */
   private TemplateInformation addTemplate(String key, String namespace) throws Exception {
      return addTemplate(key, namespace, 1);
   }

   /**
    * Parse the pin associated with the peripheral
    * 
    * @param parentModel
    * @param element
    * @throws Exception 
    */
   private void parseSignalsOption(BaseModel parentModel, Element element) throws Exception {
      
      // Initially assume pins refer to current peripheral
      Peripheral peripheral = fPeripheral;
      boolean optional = Boolean.valueOf(getAttribute(element, "optional"));
      String peripheralName = getAttribute(element, "name");
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
      String filter = getAttribute(element, "filter");
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
         // Check if entry has condition to be available for choice to be present
         Boolean keepChoice = fTemplateConditionParser.evaluateVariablePresentCondition(getAttribute(element, "condition"));
         
         // Check if entry requires peripherals to be available for choice to be present
         String requiredPeripherals = getAttribute(element, "requiredPeripherals");
         if (requiredPeripherals != null) {
            requiredPeripherals = requiredPeripherals.toUpperCase();
            String[] requiredPeriphs = requiredPeripherals.split(",");
            for (String p:requiredPeriphs) {
               Peripheral per = fPeripheral.getDeviceInfo().getPeripherals().get(p);
               if (per == null) {
                  keepChoice = false;
                  continue;
               }
            }
         }
         if (!keepChoice) {
            continue;
         }
         ChoiceData entry = new ChoiceData(
               getAttribute(element, "name"), 
               getAttribute(element, "value"), 
               getAttribute(element, "enum"),
               getAttribute(element, "code"),
               getAttribute(element, "ref")
               );
         entry.setHidden(Boolean.parseBoolean(getAttribute(element, "hidden")));
         entries.add(entry);
         if (defaultValue == null) {
            // Assume 1st entry is default
            defaultValue = entry.getName();
         }
         if (Boolean.parseBoolean(getAttribute(element, "isDefault"))) {
            // Explicit default set
            defaultValue = entry.getName();
         }
      }
      if (!entries.isEmpty()) {
         return new ChoiceInformation(entries, defaultValue);
      }
      return null;
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
      
      if (info == null) {
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

            if (info.entries.size()==2) {
               var.setFalseValue(info.entries.get(0));
               var.setTrueValue(info.entries.get(1));
            }
            else {
               ChoiceData choiceData = info.entries.get(0);
               if (Boolean.parseBoolean(choiceData.getValue()) ||
                   (Character.isDigit(choiceData.getValue().charAt(0)) && Integer.parseInt(choiceData.getValue())>0)) {
                  var.setTrueValue(choiceData);
               }
               else {
                  var.setFalseValue(choiceData);
               }
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
      ValidatorInformation validator = new ValidatorInformation(getAttribute(validateElement, "class"), (int)dimension);
      
      for (Node node = validateElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "param") {
            String type  =getAttribute(element, "type");
            String value =getAttribute(element, "value");
            
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

      String name = getAttribute(element, "name");
      if (name.equalsIgnoreCase("_instance")) {
         name = fProvider.getName();
      }
//      String description = getAttribute(element, "description");
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
      return model;
   }
   
   private void parseSingleNode(BaseModel parent, Node node) throws Exception {
      if (node.getNodeType() != Node.ELEMENT_NODE) {
         return;
      }
      Element element = (Element) node;
      String tagName  = element.getTagName();
      
      if (tagName == "fragment") {
         // Parse fragment as if it was a continuation of the parent elements
         parseSectionsOrOtherContents(parent, element);
      }
      else if (tagName == "list") {
         BaseModel tModel = new ListModel(parent, element.getAttribute("name"));
         parseSectionsOrOther(tModel, element);
         parent.addChild(tModel);
      }
      else {
         parseChildModel(parent, element);
      }
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
   private void parseSectionsOrOtherContents(BaseModel parent, Element topElement) throws Exception {
      
      String name = getAttribute(topElement, "name");
      if ((name != null) && name.equalsIgnoreCase("_instance")) {
         name = fProvider.getName();
      }
      for (Node node = topElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         parseSingleNode(parent, node);
      }
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
      
      String name = getAttribute(element, "name");
      if ((name != null) && name.equalsIgnoreCase("_instance")) {
         name = fProvider.getName();
      }
//      System.err.println("parsePage(<" + element.getTagName() + " name="+ name + ">)");

      String tooltip = getToolTip(element);
      
      String tagName = element.getTagName();
      try {
         if (tagName == "peripheralPage") {
            fRootModel = new ParametersModel(null, name, tooltip);
            parseSectionsOrOtherContents(fRootModel, element);
         }
         else if (tagName == "fragment") {
            /*
             * Parse fragment as if it was a continuation of the parent elements
             * This handles fragments that just include a href including a <peripheralPage>
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
         else if (tagName == "list") {
            fRootModel = new ListModel(null, name);
            parseSectionsOrOtherContents(fRootModel, element);
         }
         else {
            throw new Exception("Expected <peripheralPage>,  <fragment> or <list>");
         }
      } catch (Exception e) {
         new ErrorModel(fRootModel, "Parse Error", e.getMessage());
         System.err.println("parse(): " + element.getTagName() + ", " + getAttribute(element, "name"));
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
      
      Variable variable = provider.safeGetVariable(provider.makeKey(key));
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
   public static MenuData parsePeripheralFile(String peripheralName, PeripheralWithState peripheral) throws Exception {
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
