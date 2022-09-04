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
import java.util.List;
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
import net.sourceforge.usbdm.deviceEditor.information.ClockSelectionVariable;
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
         if (keys.contains(",")) {
            throw new ForloopException("Can't have ' in keys '" + keys + "'");
         }
         fKeys       = keys.split(":");
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
            fValues = fValueList[iter].split(":");
            for (int index=0; index<fKeys.length; index++) {
               String s = fValues[index].trim();
               if (s.startsWith("'") && s.endsWith("'")) {
                  s = s.substring(1,s.length()-1);
               }
               fValues[index] = s;
            }
         }
         if (fValues.length != fKeys.length) {
            throw new ForloopException(
               "Number of values '" + fValueList[iter]+
               "' does not match number of keys = "+fKeys.length);
         }
         for (int index=0; index<fKeys.length; index++) {
            text = text.replace("%("+fKeys[index].trim()+")", fValues[index]);
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
      private boolean continueFound = false;
      private boolean breakFound    = false;
      
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
         breakFound    = false;
         continueFound = false;
      }

      public boolean next() {
         continueFound = false;
         boolean res = forStack.lastElement().next();
         return res && !breakFound;
      }

      public boolean skipRemanderOfLoop() {
         return breakFound || continueFound;
      }
      
      public void setBreakFound() {
         breakFound = true;
      }
      
      public void setContinueFound() {
         continueFound = true;
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
   
   /** Accumulates items that should not be repeated between peripherals etc */
   private final HashSet<String> repeatedItems = new HashSet<>();
   
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
   private void parseBreak(BaseModel parentModel, Element element) throws Exception {
      fForStack.setBreakFound();
   }
   
   /**
    * Parse &lt;for&gt;
    * 
    * @param parentModel
    * @param element
    * @throws Exception
    */
   private void parseContinue(BaseModel parentModel, Element element) throws Exception {
      fForStack.setContinueFound();
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
         return getLongAttribute(element, name);
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
         fPeripheral.removeMonitoredVariable(existingVariable);
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
      if (varElement.hasAttribute("ref")) {
         variable.setReference(getAttribute(varElement, "ref"));
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
      if (varElement.hasAttribute("typeName")) {
         String type = getAttribute(varElement, "typeName");
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
            variable.setMin(getRequiredLongAttribute(varElement, "min"));
         }
         if (varElement.hasAttribute("max")) {
            variable.setMax(getRequiredLongAttribute(varElement, "max"));
         }
      } catch( NumberFormatException e) {
         throw new Exception("Illegal min/max value in " + variable.getName(), e);
      }
      if (varElement.hasAttribute("units")) {
         variable.setUnits(Units.valueOf(getAttribute(varElement, "units")));
      }
      if (varElement.hasAttribute("step")) {
         variable.setStep(getRequiredLongAttribute(varElement, "step"));
      }
      if (varElement.hasAttribute("offset")) {
         variable.setOffset(getRequiredLongAttribute(varElement, "offset"));
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
      if (variable.getReference() != null) {
         // Add as clock selector
         fPeripheral.addMonitoredVariable(variable);
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
         variable.setPermittedBits(getRequiredLongAttribute(varElement, "bitmask"));
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
   private void parseClockSelectionOption(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
      ChoiceVariable variable = (ChoiceVariable) createVariable(varElement, ClockSelectionVariable.class);

      parseCommonAttributes(parent, varElement, variable);
      parseChoices(variable, varElement);
      
      if (variable.getTarget() != null) {
         // Add as clock selector
         fPeripheral.addMonitoredVariable(variable);
      }
      if (variable.getTypeName() != null) {
         generateEnum(varElement, variable);
      }
   }

   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseChoiceOption(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
      ChoiceVariable variable = (ChoiceVariable) createVariable(varElement, ChoiceVariable.class);

      parseCommonAttributes(parent, varElement, variable);
      parseChoices(variable, varElement);
      
      if (variable.getTarget() != null) {
         // Add as clock selector
         fPeripheral.addMonitoredVariable(variable);
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

      String macroName = Variable.getBaseNameFromKey(variable.getKey()).toUpperCase();
      
      String templateKey = getAttribute(varElement, "templateKey");
      
      String enumStem  = variable.getTypeName();
      
      if (fPeripheral.getDeviceInfo().addAndCheckIfRepeatedItem("$ENUM"+enumStem)) {
         // These are common!
         return;
      }

      String enumType = getAttribute(varElement, "enumType");
      if (enumType != null) {
         enumType = " : "+enumType;
      }
      else {
         enumType = "";
      }

      String enumText = getAttribute(varElement, "enumText", "");
      
      String namespace       = "usbdm";
      if (templateKey != null) {
         namespace = "all";
      }
      String description     = variable.getDescriptionAsCode();
      String tooltip         = variable.getToolTipAsCode();
      
      String valueFormat     = getAttribute(varElement, "valueFormat");
      if (valueFormat == null) {
         valueFormat = macroName+"(%s)";
      }
      
      TemplateInformation templateInfo = addTemplate(templateKey, namespace);
      ChoiceData[] choiceData = variable.getData();
      
      StringBuilder body = new StringBuilder();
      
      // Use enumStem attribute
      if ((enumStem == null) || enumStem.isBlank()) {
         throw new Exception("enumStem is missing in " + variable);
      }
      String enumClass  = Character.toUpperCase(enumStem.charAt(0)) + enumStem.substring(1);

      ArrayList<String> enumNamesList = new ArrayList<String>();
      ArrayList<String> valuesList    = new ArrayList<String>();
      ArrayList<String> commentsList  = new ArrayList<String>();
      int enumNameMax    = 0;
      int valueMax       = 0;

      for (int index=0; index<choiceData.length; index++) {

         String enumName = choiceData[index].getEnumName();
         if ((enumName == null) || enumName.isBlank()) {
            throw new Exception("enumTemplate - enum data is incomplete in choice '" + choiceData[index].getName() + "' ='"+variable+"'");
         }
         if (enumName.equals("-deleted-")) {
            continue;
         }
         String completeEnumName = enumClass+"_"+enumName;
         enumNamesList.add(completeEnumName);
         enumNameMax     = Math.max(enumNameMax, completeEnumName.length());
         
         String[] valueFormats = valueFormat.split(",");
         String[] vals         = choiceData[index].getValue().split(",");
         if (valueFormats.length != vals.length) {
            throw new Exception("valueFormat '"+valueFormat+"' does not match value '"+vals[index]+"'" );
         }
         StringBuilder sb = new StringBuilder();
         for(int valIndex=0; valIndex<valueFormats.length; valIndex++) {
            if (valIndex>0) {
               sb.append('|');
            }
            sb.append(String.format(valueFormats[valIndex], vals[valIndex]));
         }
         String completeValue = sb.toString()+",";
         valuesList.add(completeValue);
         valueMax        = Math.max(valueMax, completeValue.length());
         
         commentsList.add(choiceData[index].getName());
      }
      // Create enums body
      for (int index=0; index<enumNamesList.size(); index++) {
         body.append(String.format("\\t   %-"+enumNameMax+"s = %-"+valueMax+"s ///< %s\n", enumNamesList.get(index), valuesList.get(index), commentsList.get(index)));
      }
      body.append(enumText);
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
    * Get an attribute and apply usual substitutions @ref getAttribute.
    * If the attribute is not present then the default parameter is returned.
    *  
    * @param element       Element to obtain attribute from
    * @param attrName      Name of attribute
    * @param defaultValue  Value to return if attribute is not present
    * 
    * @return  modified attribute or null if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed  
    */
   String getAttribute(Element element, String attrName, String defaultValue) throws Exception {
      if (!element.hasAttribute(attrName)) {
         return defaultValue;
      }
      return getAttribute(element, attrName);
   }
   
   /**
    * Get an attribute and apply usual substitutions @ref getAttribute.
    * If the attribute is not present then the default parameter is returned.
    *  
    * @param element       Element to obtain attribute from
    * @param attrName      Name of attribute
    * @param defaultValue  Value to return if attribute is not present
    * 
    * @return  modified attribute or null if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed  
    */
   boolean getBooleanAttribute(Element element, String attrName, boolean defaultValue) throws Exception {
      if (!element.hasAttribute(attrName)) {
         return defaultValue;
      }
      return Boolean.valueOf(getAttribute(element, attrName));
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
    * @return  Modified key or null if nor found
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
         fPeripheral.addMonitoredVariable(variable);
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
            variable.setMin(getRequiredLongAttribute(varElement, "min"));
         }
         if (varElement.hasAttribute("max")) {
            variable.setMax(getRequiredLongAttribute(varElement, "max"));
         }
         if (varElement.hasAttribute("size")) {
            variable.setMaxListLength(getRequiredLongAttribute(varElement, "size"));
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
         fPeripheral.addMonitoredVariable(variable);
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
            variable.setMaxListLength(getRequiredLongAttribute(varElement, "size"));
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
      String  name         = getAttribute(stringElement, "name");
      String  description  = getAttribute(stringElement, "description");
      String  toolTip      = getToolTip(stringElement);

      if (key.isEmpty()) {
         throw new Exception("Alias requires key "+name);
      }
      boolean isConstant  = Boolean.valueOf(getAttribute(stringElement, "constant", "true"));
      boolean isOptional  = Boolean.valueOf(getAttribute(stringElement, "optional", "false"));
      
      AliasPlaceholderModel placeholderModel = new AliasPlaceholderModel(parent, name, description);
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
      
   /**
    * Apply a set of template substitutions of form <b>%name</b> in template text
    * 
    * @param text          Text to modify
    * @param substitutions Substitutions to do
    * 
    * @return Modified test
    */
   private String doTemplateSubstitutions(String text, List<StringPair> substitutions) {
      if (text == null) {
         return null;
      }
      if (substitutions == null) {
         return text;
      }
      for (StringPair p:substitutions) {
         if (p.key==null) {
            System.err.println("key is null, value = "+p.value);
         }
         if (p.value==null) {
            System.err.println("value is null, res = "+p.key);
         }
         text = text.replace(p.key, p.value);
      }
      return text;
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
    * @return Full register name or null if not deduced
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
               "port",
               fPeripheral.getName().toLowerCase(),      // e.g. FTM2
               fPeripheral.getBaseName().toLowerCase()}; // e.g. FTM0 => FTM, PTA => PT
         for (String peripheral:peripherals) {
            Pattern p = Pattern.compile("^"+peripheral+"_([a-zA-Z0-9]*)(_(.+))?$");
            Matcher m = p.matcher(variableKey);
            if (m.matches()) {
               register = peripheral+"->"+(m.group(1).toUpperCase());
               break;
            }
         }
      }
      return register;
   }
   
   class StringPair {
      String key;
      String value;
      StringPair(String key, String value) {
         this.key   = key;
         this.value  = value;
      }
   };
   
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
    * <li>%valueExpression          Of form <b>$(controlVarName)</b>
    * <li>%symbolicValueExpression  Of form <b>$(controlVarName.enum[])</b>
    * <li>%defaultClockExpression   Based on controlVarName etc. Similar to sim->SOPT2 = (sim->SOPT2&~%mask) | %enumParam;
    * <li>%defaultMaskingExpression Based on controlVarName etc. Similar to (sim->SOPT2&%mask)
    * <br><br>
    * Substitutions obtained from <b>element</b>
    * <li>%variable                 Variable name from condition
    * <li>%mask                     From &lt;mask&gt; or deduced from &lt;controlVarName&gt; e.g. "SIM_SOPT_REG_MASK" (_MASK is added)
    * <li>&lt;register&gt;          Used to help extract mask etc from condition if required e.g. pll_sic <br><br>
    *  
    * @param element          Element 
    * @param fExpression   Control var to obtain information from
    * 
    * @return  List of substitutions or null if variableAttributeName==null or no corresponding attribute found
    * 
    * @throws  Exception 
    */
   List<StringPair> getTemplateSubstitutions(Element element, String variableAttributeName) throws Exception {

      if (getAttribute(element, "enumStem") != null) {
         throw new Exception(" 'enumStem' no longer accepted on template " + getAttribute(element, "key"));
      }

      if (variableAttributeName == null) {
         // Returns empty list to indicate template should still be processed
         return new ArrayList<StringPair>();
      }
      
      String variables = getAttribute(element, variableAttributeName);
      if (variables == null) {
         // Returns empty list to indicate template should still be processed
         return new ArrayList<StringPair>();
      }

      String temp;

      ArrayList<StringPair> substitutions = new ArrayList<StringPair>();

      StringBuilder maskSb                    = new StringBuilder();  // Combined mask e.g. MASK1|MASK2
      StringBuilder valueExpressionSb         = new StringBuilder();  // Combined values $(var1)|$(var2)
      StringBuilder symbolicValueExpressionSb = new StringBuilder();  // Combined values $(var1.enum[])|$(var2.enum[])
      StringBuilder initExpressionSb          = new StringBuilder();  // Combined values $(var1.enum[])|, // comment ...
      StringBuilder paramExprSb               = new StringBuilder();  // Combined expression param1|param2
      StringBuilder paramsSb                  = new StringBuilder();  // Parameter list with defaults etc. 
      StringBuilder paramDescriptionSb        = new StringBuilder();  // @param style comments for parameters

      // Padding applied to comments (before * @param)
      String linePadding = getAttribute(element, "linePadding", "").replace("x", " ");
      
      // Terminator for initExpression
      String terminator     = getAttribute(element, "terminator"    , ";");

      // Separator for initExpression
      String separator     = getAttribute(element, "separator"    , "|");

      // No newline before initExpression (suitable for a single initialisation value)
      boolean initExpressionOnSameLine = getBooleanAttribute(element, "initExpressionOnSameLine", false);
      
      String  register           = null;
      String  registerName       = null;
      boolean registeNameChanged = false;

      String varNames[] = variables.split(",");

      Long numberOfNonDefaultParams = getLongAttribute(element, "nonDefaultParams", 1);
      
      String defaultParamValue = getAttribute(element, "defaultParamValue");

      ArrayList<Variable> variableList = new ArrayList<Variable>();
      int paramCount=0;
      for (String varName:varNames) {
         String variableKey = fProvider.makeKey(varName.trim());
         Variable var = safeGetVariable(variableKey);
         if (var==null) {
            // Reduce numberOfDefaultParams if related parameter is missing 
            // but keep at least 1 non-default
            if ((numberOfNonDefaultParams>1) && (paramCount < numberOfNonDefaultParams)) {
               numberOfNonDefaultParams--;
            }
            continue;
         }
         variableList.add(var);
      }

      if (variableList.isEmpty()) {
         // No variables exist - don't generate method
         return null;
      }
      // Find maximum name length
      int maxNameLength = 4;
      for (int index=0; index<variableList.size(); index++) {
         String typeName = variableList.get(index).getTypeName();
         if (typeName == null) {
            continue;
         }
         maxNameLength = Math.max(maxNameLength, typeName.length());
      }

      for (int index=0; index<variableList.size(); index++) {

         Variable variable    = variableList.get(index); 
         String   variableKey = variable.getKey();

         if (index > 0) {
            maskSb.append('|');
            valueExpressionSb.append(separator);
            symbolicValueExpressionSb.append(separator);
            initExpressionSb.append("\n");
            paramExprSb.append(separator);
            paramsSb.append(",\n");
            paramDescriptionSb.append("\n");
         }

         // Mask created from variable name e.g. MACRO_MASK or deduced from valueFormat attribute
         String mask;

         // Value format string 
         String valueFormat  = variable.getValueFormat();


         if (valueFormat != null) {
            mask = valueFormat.replace(",", "|").replace("(%s)", "_MASK");
         }
         else {
            mask = Variable.getBaseNameFromKey(variableKey).toUpperCase()+"_MASK"; 
         }
         maskSb.append(mask);

         // $(variableKey)
         String valueExpression = "$("+variableKey+")";
         valueExpressionSb.append(valueExpression);

         // $(variableKey.enum[])
         String symbolicValueExpression = "$("+variableKey+".enum[])";
         symbolicValueExpressionSb.append(symbolicValueExpression);

         // Description from variable
         String description = "'%description' not available in this template";
         temp = variable.getDescription();
         if (temp != null) {
            description = temp;
         }

         // Short description from variable
         String shortDescription = "'%shortDescription' not available in this template";
         temp = variable.getShortDescription();
         if (temp != null) {
            shortDescription = temp;
         }

         // Tool-tip from variable
         String tooltip                    = "'%tooltip' not available in this template";
         temp = variable.getToolTipAsCode(linePadding+" *        ");
         if (temp != null) {
            tooltip = temp;
         }
         if (index == 0) {
            if (!initExpressionOnSameLine) {
               initExpressionSb.append("\n\\t   "+linePadding);
            }
         }
         else {
            initExpressionSb.append("\\t   "+linePadding);
         }

//         if (!initExpressionOnSameLine) {
//            if (index == 0) {
//               initExpressionSb.append("\n\\t   "+linePadding);
//            }
//            else {
//               initExpressionSb.append("\\t   "+linePadding);
//            }
//         }
         if (variable instanceof VariableWithChoices) {
            initExpressionSb.append(symbolicValueExpression);
         }
         else {
            initExpressionSb.append("$("+variableKey+".formattedValue)");
            //               value = "$("+var.getKey()+")";
         }
         if (index+1 == variableList.size()) {
            initExpressionSb.append(terminator+"  // ");
         }
         else {
            initExpressionSb.append(" "+separator+" // ");
         }

         initExpressionSb.append("$("+variableKey+".shortDescription)");

         // Type from variable with lower-case 1st letter 
         String enumClass = "'%enumClass' is not valid here";

         // Type from variable with upper-case 1st letter 
         String enumParam = "'%enumParam' is not valid here";

         String enumStem = variable.getTypeName();
         if (enumStem != null) {
            enumClass  = Character.toUpperCase(enumStem.charAt(0)) + enumStem.substring(1);
            enumParam  = Character.toLowerCase(enumStem.charAt(0)) + enumStem.substring(1);
         }

         String comment = "'%comment' is not valid here";

         temp = variable.getToolTipAsCode(linePadding+" *        ");
         if (temp != null) {
            comment = temp;
         }
//         comment = comment.replace("", "");
         
         String defaultParamV = variable.getDefaultParameterValue();
         if (defaultParamValue != null) {
            defaultParamV = defaultParamValue;
         }

         paramExprSb.append(enumParam);

         String paramDescriptionN = String.format("\\t"+linePadding+" * @param %"+(-maxNameLength)+"s %s", enumParam, comment); 
         paramDescriptionSb.append(paramDescriptionN);

         // Padding applied to parameters
         String paramPadding = (varNames.length<=1)?"":"\\t      "+linePadding;
         
         if (index<numberOfNonDefaultParams) {
            paramsSb.append(String.format(paramPadding + "%"+(-maxNameLength)+"s %s", enumClass, enumParam));
         }
         else {
            paramsSb.append(String.format(paramPadding + "%"+(-maxNameLength)+"s %"+(-maxNameLength)+"s = %s", enumClass, enumParam, defaultParamV));
         }

         String registerN     = "'register' is not valid here";
         String registerNameN = "'registerName' is not valid here";
         
         // Try to deduce register
         temp = deduceRegister(variable);
         if (temp != null) {
            registerN     = temp;
            registerNameN = temp.replaceAll("([a-zA-Z0-9]*)->", "").toLowerCase();
            if (!registeNameChanged) {
               if (register == null) {
                  register     = registerN;
                  registerName = registerNameN;
               }
               else if (!temp.equals(register)) {
                  registeNameChanged = true;
                  register     = "'register' is conflicted";
                  registerName = "'registerName' is conflicted";
               }
            }
         }
         substitutions.add(0, new StringPair("%valueExpression"+index,         valueExpression));
         substitutions.add(0, new StringPair("%symbolicValueExpression"+index, symbolicValueExpression));

         substitutions.add(0, new StringPair("%variable"+index,                variableKey));
         substitutions.add(0, new StringPair("%description"+index,             description));
         substitutions.add(0, new StringPair("%shortDescription"+index,        shortDescription));
         substitutions.add(0, new StringPair("%tooltip"+index,                 tooltip));
         substitutions.add(0, new StringPair("%mask"+index,                    mask));
         substitutions.add(0, new StringPair("%enumParam"+index,               enumParam));
         substitutions.add(0, new StringPair("%enumClass"+index,               enumClass));
         substitutions.add(0, new StringPair("%registerName"+index,            registerNameN));
         substitutions.add(0, new StringPair("%register"+index,                registerN));
         substitutions.add(0, new StringPair("%paramDescription"+index,        paramDescriptionN));
         
         if (index == 0) {
            substitutions.add(new StringPair("%variable",                variableKey));
            substitutions.add(new StringPair("%description",             description));
            substitutions.add(new StringPair("%shortDescription",        shortDescription));
            substitutions.add(new StringPair("%tooltip",                 tooltip));
            substitutions.add(new StringPair("%enumParam",               enumParam));
            substitutions.add(new StringPair("%enumClass",               enumClass));
            substitutions.add(new StringPair("%registerName",            registerNameN));
         }
      }

      String mask = null;
      if (maskSb.length() > 0) {
         mask = maskSb.toString();
         boolean bracketsRequired = !mask.matches("[a-zA-Z0-9_]*");
         if (bracketsRequired) {
            mask = '('+mask+')';
         }
      }
      if (element.hasAttribute("mask")) {
         throw new Exception("mask no longer supported on template");
      }
      String expression = "'expression' is not valid here";
      if (valueExpressionSb.length()>0) {
         expression = valueExpressionSb.toString();
      }

      String paramExpr = "'paramExpr' is not valid here";
      if (paramExprSb.length()>0) {
         paramExpr = paramExprSb.toString();
      }

      String defaultMaskingExpression   = "'defaultMaskingExpression' is not valid here";
      String defaultFieldExpression     = "'defaultFieldExpression' is not valid here";
      if ((register != null) && (mask != null)) {
         defaultMaskingExpression = register+"&"+mask;
         if (expression != null) {
            defaultFieldExpression   = register+" = ("+register+"&~"+mask+") | "+paramExpr+";";
         }
      }
      if (register == null) {
         register     = "'register' is not valid here";
         registerName = "'registerName' is not valid here";
      }
      if (mask == null) {
         mask = "'mask' not available in this template";
      }
      String params = "'params' is not valid here";
      if (paramsSb.length()>0) {
         params = paramsSb.toString();
      }
      String paramDescription = "'comments' is not valid here";
      if (paramDescriptionSb.length()>0) {
         paramDescription = paramDescriptionSb.toString();
      }

      String initExpression = "'initExpression' is not valid here";
      if (initExpressionSb.length()>0) {
         initExpression = initExpressionSb.toString();
      }

      substitutions.add(new StringPair("%initExpression",            initExpression));
      substitutions.add(new StringPair("%expression",                expression));
      substitutions.add(new StringPair("%valueExpression",           valueExpressionSb.toString()));
      substitutions.add(new StringPair("%symbolicValueExpression",   symbolicValueExpressionSb.toString()));
      substitutions.add(new StringPair("%paramExpression",           paramExpr));
      substitutions.add(new StringPair("%mask",                      mask));
      substitutions.add(new StringPair("%defaultFieldExpression",    defaultFieldExpression));
      substitutions.add(new StringPair("%defaultClockExpression",    defaultFieldExpression));
      substitutions.add(new StringPair("%defaultMaskingExpression",  defaultMaskingExpression));
      substitutions.add(new StringPair("%register",                  register));
      substitutions.add(new StringPair("%registerName",              registerName));
      substitutions.add(new StringPair("%params",                    params));
      substitutions.add(new StringPair("%comments",                  paramDescription));
      substitutions.add(new StringPair("%paramDescription",          paramDescription));

      return substitutions;
   }
   
   /**
    * Does a basic check on template attributes
    * 
    * @param namespace
    * @param key
    * @param title
    * @throws Exception
    */
   void templateBasicCheck(String namespace, String key, String title) throws Exception {
      title = "<"+title+">";
      if (namespace.isBlank()) {
         throw new Exception(title+" is missing namespace, key='" + key + "'");
      }
      if ((key != null) && !namespace.equals("all")) {
         throw new Exception("Named "+title+" must have 'all' namespace, key='" + key + "'");
      }
      if ((key == null) && namespace.equals("all")) {
         throw new Exception(title+" must have 'key' attribute in 'all' namespace, peripheral='" + fPeripheral.getName() + "'");
      }
   }
   
   /**
    * Processes 'discardRepeats' and 'condition' attributes
    * 
    * @param element  Element to obtain attributes from
    * 
    * @return  true if template should be use
    * @return  false if template should be discarded
    * 
    * @throws Exception
    */
   private boolean checkTemplateConditions(Element element) throws Exception {
      if (!checkCondition(element)) {
         return false;
      }
      if (element.hasAttribute("discardRepeats")) {
         
         // Discard repeated templates rather than accumulate
         String repeatKey = "$TEMPLATE"+getKeyAttribute(element);

         // Already blocked?
         if (fPeripheral.getDeviceInfo().checkIfRepeatedItem(repeatKey)) {
            // These are common!
            return false;
         }
         
         // Add to list for later blocking
         repeatedItems.add(repeatKey);
      }
      return true;
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

      String key           = getKeyAttribute(element);
      String namespace     = getAttribute(element, "namespace"); // info|usbdm|class|all

//      String variables = getAttribute(element, "variables");
//      if ((variables != null) && variables.contains("ftm_sc_clks")) {
//         System.err.println("Found '"+variables + "', key '"+key+"', namespace '"+namespace+"'");
//      }
      if (!checkTemplateConditions(element)) {
         return;
      }

      templateBasicCheck(namespace, key, "templates");
      
      List<StringPair> substitutions = getTemplateSubstitutions(element, "variables");
      
      if (substitutions == null) {
         // Non-empty variable list and variables not found
         return;
      }
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
   
   String getTemplateCaseStatement(Element element, Variable var) throws Exception {

      String caseBody = "%body (case statement) not available here";

      String returnFormat = getAttribute(element, "returnFormat");
      if (returnFormat == null) {
         return caseBody;
      }
      
      if (var == null) {
         return caseBody + "(var not present)";
      }
      if (!(var instanceof VariableWithChoices)) {
         return caseBody + "(Var not of correct type)";
      }
      VariableWithChoices choiceVar = (VariableWithChoices) var;

      StringBuilder caseBodySb = new StringBuilder();
      String enumStem = choiceVar.getTypeName();
      if (enumStem.isBlank()) {
         return caseBody + "(No enumStem)";
      }
      ChoiceData[] choiceData = choiceVar.getData();

      String[] enumNames      = new String[choiceData.length];
      String[] returnValues   = new String[choiceData.length];

      String   comment;
      int enumNameMax    = 0;
      int returnValueMax = 0;

      // Create body for case statement
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
         caseBodySb.append(String.format(format, enumNames[index], returnValues[index], comment));
      }
      return caseBodySb.toString();
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
      
      if (!checkTemplateConditions(element)) {
         return;
      }
      String key          = getKeyAttribute(element);
      String namespace    = getAttribute(element, "namespace");

      templateBasicCheck(namespace, key, "clockCodeTemplate");
      
      String variable     = getAttribute(element, "variable");
      Variable var = safeGetVariable(variable);
      if (var == null) {
         return;
      }
      String caseBody = getTemplateCaseStatement(element, var);
      
      List<StringPair> substitutions = getTemplateSubstitutions(element, "variable");
      
      substitutions.add(0, new StringPair("%body", caseBody));

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
      fPeripheral.removeMonitoredVariable(safeGetVariable(key));

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
      else if (tagName == "clockSelectionOption") {
         parseClockSelectionOption(parentModel, element);
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
      else if (tagName == "break") {
         parseBreak(parentModel, element);
      }
      else if (tagName == "continue") {
         parseContinue(parentModel, element);
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
         if (!element.hasAttribute("variable")) {
            throw new Exception("<clockCodeTemplate> must have 'variables' attribute, key='" + key + "'");
         }
         parseClockCodeTemplate(element);
      }
      else if (tagName == "template") {
         parseTemplate(element);
      }
      else if (tagName == "initialValueTemplate") {
         if (!element.hasAttribute("variables")) {
            throw new Exception("<initialValueTemplate> must have 'variables' attribute, key='" + key + "'");
         }
         parseTemplate(element);
      }
      else if (tagName == "setTemplate") {
         if (!element.hasAttribute("variables")) {
            throw new Exception("<setTemplate> must have 'variables' attribute, key='" + key + "'");
         }
         parseTemplate(element);
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
    * @param templateKey   Key used to index template
    * @param namespace     Namespace for template (info, usbdm, class, all)
    * 
    * @throws Exception 
    */
   private TemplateInformation addTemplate(String key, String namespace) throws Exception {
      
      if (key == null) {
         key = "";
      }
      TemplateInformation templateInfo = new TemplateInformation(key, namespace);

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
         
         if(fForStack.skipRemanderOfLoop()) {
            break;
         }
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
      for (String s:repeatedItems) {
         fPeripheral.getDeviceInfo().addAndCheckIfRepeatedItem(s);
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
      if (displayName != null) {
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
