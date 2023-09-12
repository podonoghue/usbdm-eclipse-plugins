package net.sourceforge.usbdm.deviceEditor.parsers;

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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

import net.sourceforge.usbdm.deviceEditor.graphicModel.ClockSelectionFigure;
import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable;
import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.CategoryVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceData;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.ClockMultiplexorVariable;
import net.sourceforge.usbdm.deviceEditor.information.ClockSelectionVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.NumericListVariable;
import net.sourceforge.usbdm.deviceEditor.information.PinListVariable;
import net.sourceforge.usbdm.deviceEditor.information.RtcTimeVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.Units;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;
import net.sourceforge.usbdm.deviceEditor.model.AliasPlaceholderModel;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.ErrorModel;
import net.sourceforge.usbdm.deviceEditor.model.OpenGraphicModel;
import net.sourceforge.usbdm.deviceEditor.model.ParametersModel;
import net.sourceforge.usbdm.deviceEditor.model.SectionModel;
import net.sourceforge.usbdm.deviceEditor.model.TitleModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.parsers.SimpleExpressionParser.Mode;
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
      public String getTemplate(String namespace, String key, VariableProvider varProvider) {
         key = makeKey(key, namespace);
         ArrayList<TemplateInformation> templateList = fTemplatesList.get(key);
         if (templateList == null) {
            return "";
         }
         StringBuilder sb = new StringBuilder();
         for(TemplateInformation template:templateList) {
            sb.append(template.getExpandedText(varProvider));
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
       * @param fProvider
       * 
       * @param keys    List of keys e.g. "keyA,keyB"
       * @param values  List of values e.g. "valA0,valB0;valA1,valB1;valA2,valB2"
       * @param delimiter Delimiter to separate multiple keys/values
       * 
       * @throws Exception
       */
      public ForLoop(VariableProvider fProvider, String keys, String values, String delimiter) throws Exception {
         SimpleExpressionParser expressionParser =
               new SimpleExpressionParser(fProvider, SimpleExpressionParser.Mode.EvaluateFully);
         if (keys.contains(",")) {
            throw new ForloopException("Can't have ',' in keys '" + keys + "'");
         }
         fKeys       = keys.split(":");
         fValueList  = values.split(Pattern.quote(delimiter));
         for (int index=0; index<fValueList.length; index++) {
            if (fValueList[index].startsWith("@")) {
               fValueList[index] = expressionParser.evaluate(fValueList[index].substring(1)).toString();
            }
         }
      }
      
      enum TokenState { Normal, Quoted };
      
      String[] splitValues(String value) {
         
         ArrayList<String> valueList = new ArrayList<String>();
         
         StringBuilder sb = new StringBuilder();
         
         TokenState tokenState = TokenState.Normal;
         boolean trimSpaces = true;

         for (int index=0; index<value.length(); index++) {
            boolean skipSpaces = true;
            char ch = value.charAt(index);
            switch(tokenState) {
            case Normal:
               if (ch == '\'') {
                  tokenState = TokenState.Quoted;
                  trimSpaces = false;
                  skipSpaces = true;
                  continue;
               }
               if (ch == ':') {
                  String t = sb.toString();
                  if (trimSpaces) {
                     t = t.trim();
                  }
                  valueList.add(t);
                  sb = new StringBuilder();
                  trimSpaces = true;
                  skipSpaces = true;
                  continue;
               }
               if (skipSpaces && (ch ==' ')) {
                  continue;
               }
               skipSpaces = false;
               sb.append(ch);
               break;
            case Quoted:
               if (ch == '\'') {
                  tokenState = TokenState.Normal;
                  continue;
               }
               sb.append(ch);
               break;
            }
         }
         if (tokenState != TokenState.Normal) {
            throw new ForloopException("Unmatched single quotes in " + value);
         }
         String t = sb.toString();
         if (trimSpaces) {
            t = t.trim();
         }
         valueList.add(t);
         return valueList.toArray(new String[valueList.size()]);
      }
      
      /**
       * Do for-loop substitutions on string
       * 
       * @param text Text to process
       * 
       * @return  Modified text
       * @throws Exception
       */
      public String doSubstitution(String text) {
         if (iter>=fValueList.length) {
            throw new ForloopException("doSubstitution() called after for-loop completed");
         }
         if (fValues == null) {
            fValues = splitValues(fValueList[iter]);
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
       * @param fProvider
       * 
       * @param keys      List of keys e.g. "keyA,keyB"
       * @param values    List of values e.g. "valA0,valB0;valA1,valB1;valA2,valB2"
       * @param delimiter Delimiter to separate multiple keys/values
       * 
       * @throws Exception If keys and values are unmatched
       */
      public void createLevel(VariableProvider fProvider, String keys, String values, String delimiter) throws Exception {
         ForLoop loop = new ForLoop(fProvider, keys, values, delimiter);
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

      /**
       * Advance to next iteration
       * 
       * @return true if not completed or break encountered
       */
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
   private final SimpleExpressionParser  fExpressionParser;
   
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
    * Get a long attribute
    * 
    * @param element Element being examined
    * @param name    Attribute name
    * 
    * @return value parsed as Long or null if attribute not present
    *         If the attribute value starts with "=" the it is evaluated as an expression with
    *         variable substitution.
    * 
    * @throws Exception
    */
   protected Long getLongExpressionAttribute(Element element, String name) throws Exception {
      String attr = getAttributeAsString(element, name);
      if (attr.isBlank()) {
         return null;
      }
      if (attr.startsWith("=")) {
         SimpleExpressionParser expressionParser = new SimpleExpressionParser(fProvider, SimpleExpressionParser.Mode.EvaluateFully);
         Object res = expressionParser.evaluate(attr.substring(1));
         if (res instanceof String) {
            // If string result assume it is an expression
            res = expressionParser.evaluate((String)res);
         }
         return (Long)res;
      }
      try {
         return Long.decode(attr);
      } catch (NumberFormatException e) {
         throw new NumberFormatException("Failed to parse Long Attribute \'"+name+"\' in '"+element+"'");
      }
   }
   
   /**
    * Parse a long attribute
    * 
    * @param element Element being examined
    * @param name    Attribute name
    * 
    * @return value parsed as Long
    * 
    * @throws Exception if attribute not found
    * @throws NumberFormatException if attribute found but invalid
    */
   protected Long getRequiredLongExpressionAttribute(Element element, String name) throws Exception {
      Long value = getLongExpressionAttribute(element, name);
      if (value == null) {
         throw new Exception("Attribute '"+name+"', not found in '"+element+"'");
      }
      return value;
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
   private void parseForLoop(BaseModel parentModel, Element element, GraphicWrapper graphicWrapper) throws Exception {

      if (!checkCondition(element)) {
         return;
      }
      String keys       = getAttributeAsString(element, "keys");
      String values     = getAttributeAsString(element, "values");
      String dim        = getAttributeAsString(element, "dim");
      String delimiter  = getAttributeAsString(element, "delimiter", ";");
      
//      if (delimiter != ";") {
//         System.err.println("Found it, del = "+delimiter);
//      }
      if (keys.isBlank()) {
         throw new Exception("<for> requires keys = '"+keys+"', values = '"+values+"'");
      }

      SimpleExpressionParser expressionParser =
            new SimpleExpressionParser(fProvider, SimpleExpressionParser.Mode.EvaluateFully);
      if (dim != null) {
         if (values != null) {
            throw new Exception("Both values and dim attribute given in <for> '" + keys +"'");
         }
         String dims[] = dim.split(",");
         long start;
         long end;
         if (dims.length == 1) {
            start = 0;
            end   = getLongWithVariableSubstitution(dims[0]).intValue();
         }
         else if (dims.length == 2) {
            Object s = expressionParser.evaluate(dims[0]);
            if (s instanceof String) {
               s = expressionParser.evaluate((String) s);
            }
            start = (long) s;
            Object e = expressionParser.evaluate(dims[1]);
            if (e instanceof String) {
               e = expressionParser.evaluate((String) e);
            }
            end = (long) e;
         }
         else {
            throw new Exception("Illegal dim value '"+dim+"' for <for> '"+keys+"'");
         }
         StringBuilder sb = new StringBuilder();
         for (int index=(int)start; index<(int)end; index++) {
            sb.append(index+";");
         }
         values=sb.toString();
      }
      if (values.isBlank()) {
         // Empty loop
         return;
      }
      fForStack.createLevel(fProvider, keys, values, delimiter);
      do {
         if (graphicWrapper != null) {
            graphicWrapper.parseGraphicBoxOrGroup(parentModel, element);
         }
         else {
            parseSectionsOrOtherContents(parentModel, element);
         }
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

//      fTemplateConditionParser = new TemplateConditionParser(provider);
      fExpressionParser = new SimpleExpressionParser(provider, SimpleExpressionParser.Mode.CheckIdentifierExistance);
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
    * @throws Exception
    */
   protected Long getLongWithVariableSubstitution(String value) throws Exception {
      try {
         if (Character.isDigit(value.charAt(0))) {
            return parseLong(value);
         }
         // Try variable
         Variable var = safeGetVariable(value);
         if (var != null) {
            if (var instanceof LongVariable) {
               return var.getValueAsLong();
            }
            else {
               return Long.parseLong(var.getValueAsString());
            }
         }
         throw new NumberFormatException();
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
      String attr = getAttributeAsString(element, name);
      if (Character.isDigit(attr.charAt(0))) {
         return getLongAttribute(element, name);
      }
      // Try variable
      Variable var = safeGetVariable(getAttributeAsString(element, name));
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
    * @return Formatted toolTip or null if attribute doesn't exist
    * 
    * @throws Exception
    */
   private String getToolTip(Element element) throws Exception {
      String text = doForSubstitutions(getAttributeAsString(element, "toolTip"));
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
      String name = getAttributeAsString(varElement, "name");
      if (name == null) {
         name = Variable.getNameFromKey(key);
      }
      boolean replace = Boolean.valueOf(getAttributeAsString(varElement, "replace"));
      boolean modify  = Boolean.valueOf(getAttributeAsString(varElement, "modify"));
      
      if (key.contains("[")) {
         String baseKey = key.substring(0,key.length()-3);
         
         // XXX Eventually remove
         if (safeGetVariable(baseKey) != null) {
            throw new RuntimeException("Creating non-scalar after scaler variable"+baseKey);
         }
      }
      else {
         // XXX Eventually remove
         if (safeGetVariable(key+"[0]") != null) {
            throw new RuntimeException("Creating scalar after non-scaler variable"+key);
         }
      }
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
            newVariable.setProvider(fProvider);
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
            throw new Exception("Overriding variable without 'modify' attribute '" + existingVariable.getKey() +"'");
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
   private Variable getDerivedFrom(Element varElement) throws Exception {
      Variable otherVariable = null;
      String derivedFromName = getAttributeAsString(varElement,"derivedFrom");
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
    * Get default valueFormat.<br>
    * This is constructed from the variable name e.g.  sim_clkdiv1_outdiv1 => SIM_CLKDIV1_OUTDIV1(%s)
    * 
    * @param variable Variable to examine
    * 
    * @return Default value format
    */
   private String getDefaultValueFormat(Variable variable) {
      return Variable.getBaseNameFromKey(variable.getKey()).toUpperCase()+"(%s)";
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
      
      Variable otherVariable = getDerivedFrom(varElement);
      
      if (otherVariable != null) {
         variable.setDescription(otherVariable.getDescription());
         variable.setToolTip(otherVariable.getToolTip());
         variable.setOrigin(otherVariable.getRawOrigin());
         variable.setLocked(otherVariable.isLocked());
         variable.setDerived(otherVariable.getDerived());
         variable.setTypeName(otherVariable.getTypeName());
         String valueFormat     = otherVariable.getValueFormat();
         String autoValueFormat = getDefaultValueFormat(otherVariable);
         if (!autoValueFormat.equals(valueFormat)) {
            // Copy from other variable only if not default generated one
            variable.setValueFormat(otherVariable.getValueFormat());
         }
         Expression enabledBy = otherVariable.getEnabledBy();
         if (enabledBy != null) {
            variable.setEnabledBy(enabledBy);
         }
         variable.setRegister(otherVariable.getRegister());
      }
      if (varElement.hasAttribute("constant")) {
         System.err.println("'constant' attribute no longer supported when creating '" + variable.getName() + "'");
      }
      if (varElement.hasAttribute("locked")) {
         variable.setLocked(Boolean.valueOf(getAttributeAsString(varElement, "locked")));
      }
      if (varElement.hasAttribute("description")) {
         variable.setDescription(getAttributeAsString(varElement, "description"));
      }
      if (varElement.hasAttribute("toolTip")) {
         variable.setToolTip(getToolTip(varElement));
      }
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(getAttributeAsString(varElement, "origin"));
      }
      if (varElement.hasAttribute("derived")) {
         variable.setDerived(Boolean.valueOf(getAttributeAsString(varElement, "derived")));
      }
      if (varElement.hasAttribute("enumStem")) {
         variable.setTypeName(getAttributeAsString(varElement, "enumStem", null));
      }
      if (varElement.hasAttribute("typeName")) {
         variable.setTypeName(getAttributeAsString(varElement, "typeName", null));
      }
      if (varElement.hasAttribute("valueFormat")) {
         variable.setValueFormat(getAttributeAsString(varElement, "valueFormat"));
      }
      if (varElement.hasAttribute("ref")) {
         variable.setReference(getAttributeAsString(varElement, "ref"));
      }
      if (varElement.hasAttribute("enabledBy")) {
         variable.setEnabledBy(getAttributeAsString(varElement, "enabledBy"));
      }
      if (varElement.hasAttribute("errorIf")) {
         variable.setErrorIf(getAttributeAsString(varElement, "errorIf"));
      }
      if (varElement.hasAttribute("unlockedBy")) {
         variable.setUnlockedBy(getAttributeAsString(varElement, "unlockedBy"));
      }
      if (varElement.hasAttribute("disabledPinMap")) {
         variable.setDisabledPinMap(getAttributeAsString(varElement, "disabledPinMap"));
      }
      if (varElement.hasAttribute("pinMapEnable")) {
         variable.setPinMapEnable(getAttributeAsString(varElement, "pinMapEnable"));
      }
      variable.setRegister(getAttributeAsString(varElement, "register"));

      if (varElement.hasAttribute("value")) {
         // Value is used as default and initial value
         String value = getAttributeAsString(varElement, "value");
         variable.setValue(value);
         variable.setDefault(value);
         variable.setDisabledValue(value);
      }
      
      // Value is used as disabled value
      if (varElement.hasAttribute("disabledValue")) {
         variable.setDisabledValue(getAttributeAsString(varElement, "disabledValue"));
      }
      
      if (varElement.hasAttribute("errorPropagate")) {
         variable.setErrorPropagate(getAttributeAsString(varElement, "errorPropagate").toUpperCase());
      }
      if (varElement.hasAttribute("target")) {
         variable.setTarget(getAttributeAsString(varElement, "target"));
      }
      if (varElement.hasAttribute("isNamedClock")) {
         variable.setIsNamedClock(Boolean.valueOf(getAttributeAsString(varElement, "isNamedClock")));
      }
      if (variable.getValueFormat() == null) {
         variable.setValueFormat(getDefaultValueFormat(variable));
      }

      // Internal data value
      if (varElement.hasAttribute("data")) {
         throw new Exception("data attribute not supported");
      }
      
      // Old attributes
      if (varElement.hasAttribute("default")) {
         throw new Exception("default attribute not supported");
      }
      if (varElement.hasAttribute("clockSources")) {
         throw new Exception("clockSources attribute no longer supported in "+varElement+", '"+variable.getName()+"'");
      }
//      NodeList forNodes = varElement.getElementsByTagName("for");
//      if (forNodes.getLength() > 0) {
//         throw new Exception ("<for> no longer supported here "+varElement);
//      }
      
      VariableModel model = variable.createModel(parent);
      model.setLocked(Boolean.valueOf(getAttributeAsString(varElement, "locked")));
      model.setHidden(Boolean.valueOf(getAttributeAsString(varElement, "hidden")));
      return model;
   }

   /**
    * Parse &lt;longOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception
    */
   private void parseRtcTimeOption(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
      RtcTimeVariable variable = (RtcTimeVariable) createVariable(varElement, RtcTimeVariable.class);

      parseCommonAttributes(parent, varElement, variable);
      try {
         if (varElement.hasAttribute("disabledValue")) {
            variable.setDisabledValue(getRequiredLongAttribute(varElement, "disabledValue"));
         }
      } catch( NumberFormatException e) {
         throw new Exception("Illegal disabled value in " + variable.getName(), e);
      }
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

      LongVariable otherVariable = (LongVariable)getDerivedFrom(varElement);
      if (otherVariable != null) {
         variable.setUnits(otherVariable.getUnits());
         variable.setStep(otherVariable.getStep());
         variable.setOffset(otherVariable.getOffset());
         variable.setDefault(otherVariable.getDefault());
         variable.setValue(otherVariable.getValueAsLong());
         variable.setMinExpression(otherVariable.getMinExpression());
         variable.setMaxExpression(otherVariable.getMaxExpression());
         variable.setUnits(otherVariable.getUnits());
      }
      parseCommonAttributes(parent, varElement, variable);
//      if (variable.getValueFormat() == null) {
//         variable.setValueFormat(Variable.getBaseNameFromKey(variable.getKey()).toUpperCase()+"(%s)");
//      }
      try {
         if (varElement.hasAttribute("min")) {
            variable.setMin(getAttributeAsString(varElement, "min"));
         }
         if (varElement.hasAttribute("max")) {
            variable.setMax(getAttributeAsString(varElement, "max"));
         }
         if (varElement.hasAttribute("disabledValue")) {
            variable.setDisabledValue(getRequiredLongExpressionAttribute(varElement, "disabledValue"));
         }
      } catch(NumberFormatException e) {
         throw new Exception("Illegal min/max value in " + variable.getName(), e);
      }
      if (varElement.hasAttribute("units")) {
         variable.setUnits(Units.valueOf(getAttributeAsString(varElement, "units")));
      }
      if (varElement.hasAttribute("step")) {
         variable.setStep(getRequiredLongAttribute(varElement, "step"));
      }
      if (varElement.hasAttribute("offset")) {
         variable.setOffset(getRequiredLongAttribute(varElement, "offset"));
      }
      if (varElement.hasAttribute("enumType")) {
         generateEnum(varElement, variable);
      }
      if (varElement.hasAttribute("radix")) {
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
      
      Variable otherVariable = getDerivedFrom(varElement);
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
         variable.setUnits(Units.valueOf(getAttributeAsString(varElement, "units")));
      }
   }

   /**
    * Parse &lt;bitmaskOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception
    */
   private void parseBitmaskOption(BaseModel parent, Element varElement) throws Exception {
      long bitmask = 0;
      if (!checkCondition(varElement)) {
         return;
      }
      BitmaskVariable variable = (BitmaskVariable) createVariable(varElement, BitmaskVariable.class);
      parseCommonAttributes(parent, varElement, variable);
      try {
         bitmask = getLongExpressionAttribute(varElement, "bitmask");
         variable.setPermittedBits(bitmask);
         variable.setBitList(getAttributeAsString(varElement, "bitList"));
         variable.setPinMap(getAttributeAsString(varElement, "pinMap"));
      } catch( NumberFormatException e) {
         throw new Exception("Illegal permittedBits value in " + variable.getName(), e);
      }
      if (varElement.hasAttribute("enumType")) {
         generateEnum(varElement, variable);
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
//      String  condition     = getAttribute(element, "condition");
//      return (boolean) fExpressionParser.evaluate(condition);
      
      return Expression.checkCondition(getAttributeAsString(element, "condition"), fProvider);
   }
   
   /**
    * Parse &lt;clockSelectionOption&gt; element<br>
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
      parseChoices(variable, varElement);
      parseCommonAttributes(parent, varElement, variable);
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
   private void parseClockMultiplexorOption(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
      ClockMultiplexorVariable variable = (ClockMultiplexorVariable) createVariable(varElement, ClockMultiplexorVariable.class);

      parseChoices(variable, varElement);
      parseCommonAttributes(parent, varElement, variable);
      
      if (variable.getTypeName() != null) {
         generateEnum(varElement, variable);
      }
   }

   /// Format string with parameters: description, tool-tip, enumClass, body
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
      
      String enumStem  = variable.getTypeName();
      
      if ((fPeripheral != null) && fPeripheral.getDeviceInfo().addAndCheckIfRepeatedItem("$ENUM"+enumStem)) {
         // These are common!
         return;
      }

      String enumType = getAttributeAsString(varElement, "enumType");
      if (enumType != null) {
         enumType = " : "+enumType;
      }
      else {
         enumType = "";
      }

      String enumText = getAttributeAsString(varElement, "enumText", "");
      
      String namespace = "usbdm";
      String templateKey = getAttributeAsString(varElement, "templateKey");
      if (templateKey != null) {
         namespace = "all";
      }
      namespace = getAttributeAsString(varElement, "namespace", namespace);
      
      String description     = escapeString(variable.getDescriptionAsCode());
      String tooltip         = escapeString(variable.getToolTipAsCode());
      
      String valueFormat     = getAttributeAsString(varElement, "valueFormat");
      if (valueFormat == null) {
         valueFormat = macroName+"(%s)";
      }
      
      TemplateInformation templateInfo = addTemplate(templateKey, namespace, null);
      
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

      ArrayList<ChoiceData[]> lists = new ArrayList<ChoiceData[]>();
      lists.add(variable.getChoiceData());
      lists.add(variable.getHiddenChoiceData());
      for (ChoiceData[] choiceData:lists) {
         if (choiceData == null) {
            continue;
         }
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
//            valuesList.add(escapeString(completeValue));
            valuesList.add(completeValue);
            valueMax        = Math.max(valueMax, completeValue.length());
            
            commentsList.add(choiceData[index].getName());
         }

      }
      // Create enums body
      for (int index=0; index<enumNamesList.size(); index++) {
         body.append(String.format("\\t   %-"+enumNameMax+"s = %-"+valueMax+"s ///< %s\n", enumNamesList.get(index), valuesList.get(index), commentsList.get(index)));
      }
      body.append(enumText);
      // Create enum declaration
      String entireEnum = String.format(enumTemplate, description, tooltip, enumClass, enumType, body.toString());
      templateInfo.addText(entireEnum);
   }

   private void generateEnum(Element varElement, LongVariable variable) throws Exception {

      String enumStem  = variable.getTypeName();
      
      if ((fPeripheral != null) && fPeripheral.getDeviceInfo().addAndCheckIfRepeatedItem("$ENUM"+enumStem)) {
         // These are common!
         return;
      }

      String enumType = getAttributeAsString(varElement, "enumType");
      if (enumType != null) {
         enumType = " : "+enumType;
      }
      else {
         enumType = "";
      }

      String enumText = getAttributeAsString(varElement, "enumText", "");
      
      String namespace = "usbdm";
      String templateKey = getAttributeAsString(varElement, "templateKey");
      if (templateKey != null) {
         namespace = "all";
      }
      namespace = getAttributeAsString(varElement, "namespace", namespace);
      
      String description     = escapeString(variable.getDescriptionAsCode());
      String tooltip         = escapeString(variable.getToolTipAsCode());
      
      TemplateInformation templateInfo = addTemplate(templateKey, namespace, null);
      
      StringBuilder body = new StringBuilder();
      
      // Use enumStem attribute
      if ((enumStem == null) || enumStem.isBlank()) {
         throw new Exception("enumStem is missing in " + variable);
      }
      String enumClass  = Character.toUpperCase(enumStem.charAt(0)) + enumStem.substring(1);

      body.append(enumText);
      
      // Create enum declaration
      String entireEnum = String.format(enumTemplate, description, tooltip, enumClass, enumType, body.toString());
      templateInfo.addText(entireEnum);
   }

   /**
    * Do simple name substitutions:
    *  <li>"$(_NAME)"         => e.g FTM2 => FTM2            (fPeripheral.getName())
    *  <li>"$(_name)"         => e.g FTM2 => ftm2            (fPeripheral.getName().tolowercase())
    *  <li>"$(_BASENAME)"     => e.g FTM0 => FTM, PTA => PT  (fPeripheral.getBaseName())
    *  <li>"$(_basename)"     => e.g FTM0 => ftm, PTA => pt  (fPeripheral.getBaseName().tolowercase())
    *  <li>"$(_Class)"        => e.g FTM2 => Ftm2            (fPeripheral.getClassName())
    *  <li>"$(_Baseclass)"    => e.g FTM0 => Ftm             (fPeripheral.getClassBaseName())
    *  <li>"$(_instance)"     => e.g FTM0 => 0, PTA => A     (fPeripheral.getInstance())
    * 
    * @param text  Test to process
    * 
    * @return  modified attribute or null if attribute doesn't exist
    */
   String replaceCommonNames(String text) {

      if (fPeripheral != null) {
         text = text.replace("$(_NAME)",       fPeripheral.getName());
         text = text.replace("$(_name)",       fPeripheral.getName().toLowerCase());
         text = text.replace("$(_BASENAME)",   fPeripheral.getBaseName());
         text = text.replace("$(_basename)",   fPeripheral.getBaseName().toLowerCase());
         text = text.replace("$(_Baseclass)",  fPeripheral.getClassBaseName());
         text = text.replace("$(_Class)",      fPeripheral.getClassName());
         text = text.replace("$(_instance)",   fPeripheral.getInstance());
      }
      else {
         text = text.replace("$(_NAME)", fProvider.getName());
      }
      return text;
   }
   
   /**
    * Get element text and apply usual substitutions
    *  <li>"$(_NAME)"         => e.g FTM2 => FTM2            (fPeripheral.getName())
    *  <li>"$(_name)"         => e.g FTM2 => ftm2            (fPeripheral.getClassName())
    *  <li>"$(_BASENAME)"     => e.g FTM0 => FTM, PTA => PT  (fPeripheral.getBaseName())
    *  <li>"$(_basename)"     => e.g FTM0 => ftm, PTA => pt  (fPeripheral.getBaseName().tolowercase())
    *  <li>"$(_Class)"        => e.g FTM2 => Ftm2            (fPeripheral.getClassName())
    *  <li>"$(_Baseclass)"    => e.g FTM0 => Ftm             (fPeripheral.getClassBaseName())
    *  <li>"$(_instance)"     => e.g FTM0 => 0, PTA => A     (fPeripheral.getInstance())
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
      return replaceCommonNames(bodyText);
   }
   
   /**
    * Get an attribute and apply usual substitutions
    *  <li>"$(_NAME)"         => e.g FTM2 => FTM2            (fPeripheral.getName())
    *  <li>"$(_name)"         => e.g FTM2 => ftm2            (fPeripheral.getClassName())
    *  <li>"$(_BASENAME)"     => e.g FTM0 => FTM, PTA => PT  (fPeripheral.getBaseName())
    *  <li>"$(_basename)"     => e.g FTM0 => ftm, PTA => pt  (fPeripheral.getBaseName().tolowercase())
    *  <li>"$(_Class)"        => e.g FTM2 => Ftm2            (fPeripheral.getClassName())
    *  <li>"$(_Baseclass)"    => e.g FTM0 => Ftm             (fPeripheral.getClassBaseName())
    *  <li>"$(_instance)"     => e.g FTM0 => 0, PTA => A     (fPeripheral.getInstance())
    *  <li>For loop substitution
    *  <li>If the attribute starts with '=' it is evaluated as an immediate expression
    *      otherwise it is returned as a string
    * 
    * @param element    Element to obtain attribute from
    * @param attrName   Name of attribute
    * 
    * @return  Attribute as Object or null if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed
    */
   Object getAttribute(Element element, String attrName) throws Exception {
      if (!element.hasAttribute(attrName)) {
         return null;
      }
      String attribute = fForStack.doForSubstitutions(element.getAttribute(attrName));
      attribute = replaceCommonNames(attribute).trim();
      
//      if ("=_enable".equalsIgnoreCase(attribute)) {
//         System.err.println("Found it "+attribute);
//      }
      Object res = attribute;
      if (attribute.startsWith("=")) {
         res = Expression.evaluate(attribute.substring(1), fProvider);
//         Expression exp = new Expression(attribute.substring(1), fProvider, ExpressionParser.Mode.EvaluateFully);
//         exp.getValue();
//         res = SimpleExpressionParser.evaluate(attribute.substring(1), fProvider, Mode.EvaluateFully);
      }
      return res;
   }
   
   /**
    * Get an attribute and apply usual substitutions {@link #getAttribute(Element, String)}.
    * If the attribute is not present then the default parameter is returned.
    * 
    * @param element       Element to obtain attribute from
    * @param attrName      Name of attribute
    * @param defaultValue  Value to return if attribute is not present
    * 
    * @return  Attribute as Object or defaultValue if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed
    */
   Object getAttribute(Element element, String attrName, Object defaultValue) throws Exception {
      if (!element.hasAttribute(attrName)) {
         return defaultValue;
      }
      return getAttribute(element, attrName);
   }
   
   /**
    * Get an attribute and apply usual substitutions
    *  <li>"$(_NAME)"         => e.g FTM2 => FTM2            (fPeripheral.getName())
    *  <li>"$(_name)"         => e.g FTM2 => ftm2            (fPeripheral.getClassName())
    *  <li>"$(_BASENAME)"     => e.g FTM0 => FTM, PTA => PT  (fPeripheral.getBaseName())
    *  <li>"$(_basename)"     => e.g FTM0 => ftm, PTA => pt  (fPeripheral.getBaseName().tolowercase())
    *  <li>"$(_Class)"        => e.g FTM2 => Ftm2            (fPeripheral.getClassName())
    *  <li>"$(_Baseclass)"    => e.g FTM0 => Ftm             (fPeripheral.getClassBaseName())
    *  <li>"$(_instance)"     => e.g FTM0 => 0, PTA => A     (fPeripheral.getInstance())
    *  <li>For loop substitution
    *  <li>If the attribute starts with '=' it is evaluated as an immediate expression and converted to a string
    * 
    * @param element    Element to obtain attribute from
    * @param attrName   Name of attribute
    * 
    * @return  Attribute value converted to string or null if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed
    */
   String getAttributeAsString(Element element, String attrName) throws Exception {
      
      Object res = getAttribute(element, attrName);
            
      if (res == null) {
         return null;
      }
      return res.toString();
   }
   
   /**
    * Get an attribute and apply usual substitutions {@link #getAttributeAsString(Element, String)}.
    * If the attribute is not present then the default parameter is returned.
    * 
    * @param element       Element to obtain attribute from
    * @param attrName      Name of attribute
    * @param defaultValue  Value to return if attribute is not present
    * 
    * @return  Attribute value converted to string or defaultValue if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed
    */
   String getAttributeAsString(Element element, String attrName, String defaultValue) throws Exception {
      
      Object res = getAttribute(element, attrName, defaultValue);
            
      if (res == null) {
         return null;
      }
      return res.toString();
   }
   
   /**
    * Get an attribute as Boolean after applying usual substitutions see {@link #getAttribute(Element, String)}.
    * If the attribute is not present then the default parameter is returned.
    * 
    * @param element       Element to obtain attribute from
    * @param attrName      Name of attribute
    * @param defaultValue  Value to return if attribute is not present
    * 
    * @return  Attribute value converted to Boolean or defaultValue if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed
    */
   Boolean getAttributeAsBoolean(Element element, String attrName, Boolean defaultValue) throws Exception {
      
      Object res = getAttribute(element, attrName, defaultValue);
            
      if (res == null) {
         return null;
      }
      if (res instanceof Boolean) {
         return (Boolean)res;
      }
      return Boolean.valueOf(res.toString());
   }
   
   /**
    * Get an attribute as Boolean after applying usual substitutions see {@link #getAttribute(Element, String)}.
    * If the attribute is not present then the default parameter is returned.
    * 
    * @param element       Element to obtain attribute from
    * @param attrName      Name of attribute
    * @param defaultValue  Value to return if attribute is not present
    * 
    * @return  Attribute value converted to Long or defaultValue if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed
    */
   Long getAttributeAsLong(Element element, String attrName, Long defaultValue) throws Exception {
      
      Object res = getAttribute(element, attrName, defaultValue);
            
      if (res == null) {
         return null;
      }
      if (res instanceof Long) {
         return (Long)res;
      }
      if (res instanceof String) {
         res = SimpleExpressionParser.evaluate(res.toString(), fProvider, Mode.EvaluateFully);
      }
      try {
         return Long.valueOf(res.toString());
      } catch (NumberFormatException e) {
         e.printStackTrace();
         return Long.valueOf(2);
      }
   }
   
   /**
    * Get an attribute and apply usual substitutions
    *  <li>"$(_NAME)"         => e.g FTM2 => FTM2            (fPeripheral.getName())
    *  <li>"$(_name)"         => e.g FTM2 => ftm2            (fPeripheral.getClassName())
    *  <li>"$(_BASENAME)"     => e.g FTM0 => FTM, PTA => PT  (fPeripheral.getBaseName())
    *  <li>"$(_basename)"     => e.g FTM0 => ftm, PTA => pt  (fPeripheral.getBaseName().tolowercase())
    *  <li>"$(_Class)"        => e.g FTM2 => Ftm2            (fPeripheral.getClassName())
    *  <li>"$(_Baseclass)"    => e.g FTM0 => Ftm             (fPeripheral.getClassBaseName())
    *  <li>"$(_instance)"     => e.g FTM0 => 0, PTA => A     (fPeripheral.getInstance())
    *  <li>For loop substitution
    *  <li>If the attribute starts with '=' it is evaluated as an immediate expression and converted to a string
    * 
    * @param element    Element to obtain attribute from
    * @param attrName   Name of attribute
    * 
    * @return  Attribute value converted to Long or null if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed
    */
   Long getAttributeAsLong(Element element, String attrName) throws Exception {
      
      Object res = getAttribute(element, attrName);
            
      if (res == null) {
         return null;
      }
      if (res instanceof Long) {
         return (Long)res;
      }
      return Long.valueOf(res.toString());
   }
   
   /**
    * Get an attribute as Boolean after applying usual substitutions see {@link #getAttribute(Element, String)}.
    * If the attribute is not present then the default parameter is returned.
    * 
    * @param element       Element to obtain attribute from
    * @param attrName      Name of attribute
    * @param defaultValue  Value to return if attribute is not present
    * 
    * @return  Attribute value converted to Boolean or defaultValue if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed
    */
   Double getAttributeAsLong(Element element, String attrName, Double defaultValue) throws Exception {
      
      Object res = getAttribute(element, attrName, defaultValue);
            
      if (res == null) {
         return null;
      }
      if (res instanceof Double) {
         return (Double)res;
      }
      return Double.valueOf(res.toString());
   }
   
   /**
    * Get attribute with given name and treat as key and apply usual key transformations
    *  <li>"$(_NAME)"     => fProvider.getName()
    *  <li>"$(_instance)" => fPeripheral.getInstance()
    *  <li>For loop substitution
    *  <li>fprovider.MakeKey()
    * 
    * @param element          Element to examine
    * @param attrName    Name to use for attribute
    * 
    * @return  Modified key or null if not found
    * 
    * @throws Exception
    */
   String getKeyAttribute(Element element, String attrName) throws Exception {
      String key = getAttributeAsString(element, attrName);
      if (key == null) {
         return null;
      }
      return fProvider.makeKey(key);
   }
   
   /**
    * Get attribute 'key' or 'name' and apply usual key transformations
    *  <li>"$(_NAME)"     => fProvider.getName()
    *  <li>"$(_instance)" => fPeripheral.getInstance()
    *  <li>For loop substitution
    *  <li>fprovider.MakeKey()
    * 
    * @param element    Element to examine
    * 
    * @return  Modified key or null if not found
    * 
    * @throws Exception
    */
   String getKeyAttribute(Element element) throws Exception {
      
      String key = getAttributeAsString(element, "key");
      if (key == null) {
         key = getAttributeAsString(element, "name");
      }
      if (key == null) {
         return null;
      }
      return fProvider.makeKey(key);
   }
   
   /**
    * Get attribute 'name' or generate from attribute 'key' and apply usual key transformations
    *  <li>"$(_NAME)"     => fProvider.getName()
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
   String getNameAttribute(Element element) throws Exception {
      
      String name = getAttributeAsString(element, "name");
      if (name == null) {
         String key = getAttributeAsString(element, "key");
         if (key != null) {
            name = Variable.getNameFromKey(key);
         }
      }
      return name;
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
      ChoiceVariable variable = safeGetChoiceVariable(key);
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
   private void parseTitle(BaseModel parent, Element varElement) throws Exception {
      
      if (!checkCondition(varElement)) {
         return;
      }
      String name = getAttributeAsString(varElement, "name");
      TitleModel model = new TitleModel(parent, name);
      
      if (varElement.hasAttribute("toolTip")) {
         model.setToolTip(getAttributeAsString(varElement, "toolTip"));
      }
      if (varElement.hasAttribute("description")) {
         model.setSimpleDescription(getAttributeAsString(varElement, "description"));
      }
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
      
   }

   private void parseCategory(BaseModel parent, Element varElement) throws Exception {
      
      CategoryModel model = new CategoryModel(parent, getAttributeAsString(varElement, "name"));
      boolean hidden = Boolean.parseBoolean(getAttributeAsString(varElement, "hidden"));
      model.setHidden(hidden);
      model.setToolTip(getAttributeAsString(varElement, "toolTip"));
      model.setSimpleDescription(getAttributeAsString(varElement, "description"));
      parseChildModels(model, varElement);
      if ((model.getChildren()==null)||(model.getChildren().size()==0)) {
         // Empty category - discard
         parent.removeChild(model);
         return;
      }
   }

   private void parseCategoryOption(BaseModel parent, Element varElement) throws Exception {

      CategoryVariable      categoryVariable = (CategoryVariable)      createVariable(varElement, CategoryVariable.class);
      CategoryVariableModel categoryModel    = (CategoryVariableModel) parseCommonAttributes(parent, varElement, categoryVariable);
      
      categoryVariable.setValue(getAttributeAsString(varElement, "value"));
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
      
      NumericListVariable otherVariable = (NumericListVariable)getDerivedFrom(varElement);
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
   }

   /**
    * Parse &lt;binaryOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception
    */
   private void parsePinmapOption(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
      BooleanVariable variable = (BooleanVariable)createVariable(varElement, BooleanVariable.class);
      parseCommonAttributes(parent, varElement, variable);
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
      parseCommonAttributes(parent, irqElement, variable);
      
      variable.setPattern(getAttributeAsString(irqElement, "pattern"));
      variable.setClassHandler(getAttributeAsString(irqElement, "classHandler"));
      if (variable.getDefault() == null) {
         variable.setDefault(false);
      }
      if (variable.getValue() == null) {
         variable.setValue(false);
      }
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
      String  name         = getAttributeAsString(stringElement, "name");
      String  description  = getAttributeAsString(stringElement, "description");
      String  toolTip      = getToolTip(stringElement);

      if (key.isEmpty()) {
         throw new Exception("Alias requires key "+name);
      }
      boolean isConstant  = Boolean.valueOf(getAttributeAsString(stringElement, "locked",   "true"));
      boolean isOptional  = Boolean.valueOf(getAttributeAsString(stringElement, "optional", "false"));
      
      AliasPlaceholderModel placeholderModel = new AliasPlaceholderModel(parent, name, description);
      placeholderModel.setkey(key);
      placeholderModel.setLocked(isConstant);
      placeholderModel.setOptional(isOptional);
      placeholderModel.setToolTip(toolTip);
      return placeholderModel;
   }

//   private String[] split(String s, char separator) {
//
//      ArrayList<String> list = new ArrayList<>();
//      int index = 0;
//      boolean inQuote = false;
//      StringBuilder sb = new StringBuilder();
//
//      for (index=0; index<s.length(); index++) {
//         char currentChar = s.charAt(index);
//         if (currentChar == '"') {
//            inQuote = !inQuote;
//         }
//         if (inQuote) {
//            sb.append(currentChar);
//            continue;
//         }
//         if (currentChar == separator) {
//            list.add(sb.toString());
//            sb = new StringBuilder();
//            continue;
//         }
//         sb.append(currentChar);
//      }
//      list.add(sb.toString());
//      return list.toArray(new String[list.size()]);
//   }
   
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
      String name        = getNameAttribute(element);
      String type        = getAttributeAsString(element, "type");
      String value       = getAttributeAsString(element, "value");
      String description = getAttributeAsString(element, "description");
      boolean isWeak     = Boolean.valueOf(getAttributeAsString(element, "weak"));
      boolean isReplace  = Boolean.valueOf(getAttributeAsString(element, "replace"));
      if (key.isBlank()) {
         throw new Exception("<constant> must have 'key' attribute, value='"+value+"'");
      }
      boolean isHidden  = getAttributeAsBoolean(element, "hidden", true);
      
      if (value == null) {
         value="true";
      }
      Object result = Expression.getValue(value, fProvider);
      
      // Make into array even if a single item
      Object results[];
      if (result.getClass().isArray()) {
         // Array constant
         results = (Object[]) result;
      }
      else {
         // Simply constant
         results = new Object[1];
         results[0] = result;
      }
      
      for(int index=0; index<results.length; index++) {
         String indexedKey = key;
         if (results.length>1) {
            indexedKey = key+"["+index+"]";
         }
         Variable var = safeGetVariable(indexedKey);
         if (var != null) {
            if (isWeak) {
               // Ignore constant
            }
            else if (isReplace) {
               // Replace constant value
               var.setValue(results[index]);
               if (element.hasAttribute("name")) {
                  var.setName(name);
               }
               if (element.hasAttribute("description")) {
                  var.setDescription(description);
               }
               return;
            }
            else {
               throw new Exception("Constant multiply defined, name="+name+", key=" + indexedKey);
            }
         }
         else {
            if ("Integer".equalsIgnoreCase(type)) {
               System.err.println("Warning: Old style 'Integer' type for '" + key + "'");
               type = "Long";
            }
            var = Variable.createConstantWithNamedType(name, indexedKey, type+"Variable", results[index]);
            var.setDescription(description);
            var.setHidden(isHidden);
            fProvider.addVariable(var);
         }
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
    * @throws Exception
    */
   String deduceRegister(Variable controlVar) throws Exception {
      
      String register = null;
      String variableKey  = controlVar.getBaseNameFromKey();
      String registerName = controlVar.getRegister();
      
      if (registerName != null) {
         Pattern p = Pattern.compile("(.+)_"+registerName+"_(.+)");
         Matcher m = p.matcher(variableKey);
         if (m.matches()) {
            register = m.group(1)+"->"+registerName.toUpperCase();
         }
         else {
            throw new Exception("Unable to match register name "+registerName+" against "+variableKey);
         }
      }
      else {
         String peripherals[] = {
               "port",
               "nvic",
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
    * <br><br>
    * <li>%paramExpression                Parameters ORed together e.g. adcPretrigger|adcRefSel
    * <li>%valueExpression[index]         Formatted value as numeric e.g. 0x12
    * <li>%symbolicValueExpression[index] Symbolic formatted value e.g. AdcCompare_Disabled
    * <li>%variable[index]                Variable name /ADC0/adc_sc2_acfe
    * <li>%macro[index](value)            C register macro e.g. ADC_SC2_ACFGT(value)
    * <li>%description[index]             Description from controlVar e.g. Compare Function Enable
    * <li>%shortDescription[index]        Short description from controlVar e.g. Compare Function Enable
    * <li>%tooltip[index]                 Tool-tip from controlVar e.g. Each bit disables the GPIO function
    * <li>%params                         Formatted parameter list for function
    * <li>%paramDescription[index]        Tool-tip from controlVar formatted as param description @param ...
    * <li>%paramType[index]               Based on enumStem or typename e.g. AdcCompare (or uint32_t)
    * <li>%paramName[index]               Based on enumStem with lower-case first letter adcCompare
    * <li>%enumClass[index]               As for %paramType
    * <li>%enumParam[index]               As for %paramName
    * <li>%valueExpression                Numeric variable value e.g. 0x3
    * <li>%symbolicValueExpression        Symbolic variable value e.g. AdcCompare_Disabled
    * <li>%defaultClockExpression         Based on variable etc. Similar to %register = (%register&~%mask) | %paramName;
    * <li>%defaultMaskingExpression       Based on variable etc. Similar to (%register&%mask)
    * <li>%variable[index]                Variable name from condition
    * <li>%mask[index]                    From &lt;mask&gt; or deduced from &lt;controlVarName&gt; e.g. "SIM_SOPT_REG_MASK" (_MASK is added)
    * <li>%register[index]                Register associated with variable e.g. adc->APCTL1
    * <li>%registerName[index]            Name of corresponding register (lowercase for Init()) e.g. apctl1 <br><br>
    * 
    * @param element                 Element
    * @param variableAttributeName   Control var to obtain information from
    * 
    * @return  List of substitutions or null if variableAttributeName==null or no corresponding attribute found
    * 
    * @throws  Exception
    */
   List<StringPair> getTemplateSubstitutions(Element element, String variableAttributeName) throws Exception {
      
      boolean isConstructor = (element.getTagName().equalsIgnoreCase("constructorTemplate"));

      if (getAttributeAsString(element, "enumStem") != null) {
         throw new Exception(" 'enumStem' no longer accepted on template " + getAttributeAsString(element, "key"));
      }
      boolean useDefinitions = Boolean.valueOf(getAttributeAsString(element, "useDefinitions", "false"));

      String temp = getAttributeAsString(element, "params");
      List<String> paramOverride;
      if (temp != null) {
         String[] par = temp.split(",");
         for (int index=0; index<par.length; index++) {
            par[index] = par[index].trim();
         }
         paramOverride = new ArrayList<String>(Arrays.asList(par));
      }
      else {
         paramOverride = new ArrayList<String>();
      }

      temp = getAttributeAsString(element, "defaultParamValue");
      List<String> defaultValueOverride;
      if (temp != null) {
         String[] def = temp.split(",");
         for (int index=0; index<def.length; index++) {
            def[index] = def[index].trim();
         }
         defaultValueOverride = new ArrayList<String>(Arrays.asList(def));
      }
      else {
         defaultValueOverride = new ArrayList<String>();
      }
      
      if (variableAttributeName == null) {
         // Returns empty list to indicate template should still be processed
         return new ArrayList<StringPair>();
      }
      
      String variables = getAttributeAsString(element, variableAttributeName);
      if (variables == null) {
         // Returns empty list to indicate template should still be processed
         return new ArrayList<StringPair>();
      }

      ArrayList<StringPair> substitutions = new ArrayList<StringPair>();

      StringBuilder maskSb                    = new StringBuilder();  // Combined mask e.g. MASK1|MASK2
      StringBuilder valueExpressionSb         = new StringBuilder();  // Combined values $(var1)|$(var2)
      StringBuilder symbolicValueExpressionSb = new StringBuilder();  // Combined values $(var1.enum[])|$(var2.enum[])
      StringBuilder initExpressionSb          = new StringBuilder();  // Combined values $(var1.enum[])|, // comment ...
      StringBuilder paramExprSb               = new StringBuilder();  // Combined expression param1|param2
      StringBuilder paramsSb                  = new StringBuilder();  // Parameter list with defaults etc.
      StringBuilder paramDescriptionSb        = new StringBuilder();  // @param style comments for parameters

      // Padding applied to comments (before * @param)
      String linePadding    = getAttributeAsString(element, "linePadding",    "").replace("x", " ");
      String tooltipPadding = getAttributeAsString(element, "tooltipPadding", " *        ").replace("x", " ");
      
      // Terminator for initExpression
      String terminator     = getAttributeAsString(element, "terminator"    , ";");

      // Separator for initExpression
      String separator     = getAttributeAsString(element, "separator"    , "|");

      // No newline before initExpression (suitable for a single initialisation value)
      boolean initExpressionOnSameLine = getAttributeAsBoolean(element, "initExpressionOnSameLine", false);
      
      String  register           = null;
      String  registerName       = null;
      boolean registeNameChanged = false;

      String varNames[] = variables.split(",");

      Long numberOfNonDefaultParams = getLongAttribute(element, "nonDefaultParams", 1);
      
      ArrayList<Variable> variableList = new ArrayList<Variable>();
      ArrayList<Integer>  deletedParams = new ArrayList<Integer>();
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
            // Remove corresponding override
            deletedParams.add(0, paramCount);
            paramCount++;
            continue;
         }
         variableList.add(var);
         paramCount++;
      }
      // Fix lists for missing parameters
      for(int index:deletedParams) {
         if (paramOverride.size() > index) {
            paramOverride.remove(index);
         }
         if (defaultValueOverride.size() > index) {
            defaultValueOverride.remove(index);
         }
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
      // To differentiate 'nameless' params
      int valueSuffix = 0;
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
         String macro;
         
         // Value format string
         String valueFormat  = variable.getValueFormat();

         if (valueFormat != null) {
            macro  = valueFormat.replace("(%s)", "").replace(",", "|");
            mask  = valueFormat.replace(",(%s)", "").replace(",", "|").replace("(%s)", "_MASK");
         }
         else {
            macro  = Variable.getBaseNameFromKey(variableKey).toUpperCase();
            mask   = macro+"_MASK";
            valueFormat = mask+"(%s)";
         }
         maskSb.append(mask);

         if (mask.length() > 0) {
            boolean bracketsRequired = !mask.matches("[a-zA-Z0-9_]*");
            if (bracketsRequired) {
               mask = '('+mask+')';
            }
         }

         // Type from variable with upper-case 1st letter
         String paramName = "'%enumParam' is not valid here";
         
         String paramType = variable.getTypeName();
         if (paramType == null) {
            if (variable instanceof LongVariable) {
               paramType = ((LongVariable)variable).getUnits().getType();
            }
            else if (variable instanceof DoubleVariable) {
               paramType = ((DoubleVariable)variable).getUnits().getType();
            }
            if (paramType == null) {
               paramType = "'%enumClass' is not valid here";
            }
         }
         else {
            paramType = paramType.strip();
            Pattern p = Pattern.compile("(const\\s)?\\s*([a-zA-Z0-9_]+)\\s*(&)?");
            Matcher m = p.matcher(paramType);
            if (!m.matches()) {
               throw new Exception("Failed to match '" + paramType + "'");
            }
            String constPrefix = m.group(1);
            String type        = m.group(2);
            String reference   = m.group(3);
            if (constPrefix == null) {
               constPrefix = "";
            }
            if (reference == null) {
               reference = "";
            }
            if (!isConstructor) {
               // Strip references from type unless it is a constexpr constructor
               constPrefix = "";
               reference   = "";
            }
            paramType  = constPrefix + Character.toUpperCase(type.charAt(0)) + type.substring(1) + reference;
            paramName  = Character.toLowerCase(type.charAt(0)) + type.substring(1);
            
         }
         // Special cases pattern
         // This is used to identify C integers
         Pattern pSpecial = Pattern.compile("(u?int[0-9]+_t)|((un)?signed(\\sint)?)|(int)");

         Matcher mSpecial = pSpecial.matcher(paramName);
         boolean integerType = mSpecial.matches();
         if (integerType) {
            // Integer parameters get a name of 'value' by default
            paramType = paramName;
            paramName = "value";
            if (valueSuffix != 0) {
               paramName = paramName+valueSuffix;
            }
            valueSuffix++;
         }

         if ((paramOverride.size()>index) && !paramOverride.get(index).isBlank()) {
            paramName = paramOverride.get(index);
         }
         
         // $(variableKey)
         String valueExpression = "$("+variableKey+")";
         valueExpressionSb.append(valueExpression);
         
         String symbolicValueExpression;
         if (useDefinitions || (variable instanceof BitmaskVariable)) {
            symbolicValueExpression = "$("+variableKey+".definition)";
         }
         else {
            symbolicValueExpression = "$("+variableKey+".usageValue)";
         }
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
         String tooltip = "'%tooltip' not available in this template";
         temp = variable.getToolTipAsCode("\\t"+linePadding+tooltipPadding);
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

         initExpressionSb.append(symbolicValueExpression);
         if (index+1 == variableList.size()) {
            initExpressionSb.append(terminator+"  // ");
         }
         else {
            initExpressionSb.append(" "+separator+" // ");
         }

         initExpressionSb.append("$("+variableKey+".shortDescription)");
         if (variable instanceof VariableWithChoices) {
            initExpressionSb.append(" - $("+variableKey+".name[])");
         }
         
         String defaultParamV = variable.getDefaultParameterValue();
         if ((defaultValueOverride.size()>index) && !defaultValueOverride.get(index).isBlank()) {
            defaultParamV = defaultValueOverride.get(index);
         }
         paramExprSb.append(paramName);

         String paramDescriptionN = String.format("\\t"+linePadding+" * @param %"+(-maxNameLength)+"s %s", paramName, tooltip);
         paramDescriptionSb.append(paramDescriptionN);

         // Padding applied to parameters
         String paramPadding = (varNames.length<=1)?"":"\\t      "+linePadding;
         
         String param;
         if (index<numberOfNonDefaultParams) {
            param = String.format("%"+(-maxNameLength)+"s %s", paramType, paramName);
         }
         else {
            param = String.format("%"+(-maxNameLength)+"s %"+(-maxNameLength)+"s = %s", paramType, paramName, defaultParamV);
         }
         paramsSb.append(paramPadding + param);
         
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
         substitutions.add(0, new StringPair("%macro"+index,                   macro));
         substitutions.add(0, new StringPair("%mask"+index,                    mask));
         substitutions.add(0, new StringPair("%paramName"+index,               paramName));
         substitutions.add(0, new StringPair("%enumParam"+index,               paramName));
         substitutions.add(0, new StringPair("%paramType"+index,               paramType));
         substitutions.add(0, new StringPair("%enumClass"+index,               paramType));
         substitutions.add(0, new StringPair("%registerName"+index,            registerNameN));
         substitutions.add(0, new StringPair("%register"+index,                registerN));
         substitutions.add(0, new StringPair("%paramDescription"+index,        paramDescriptionN));
         substitutions.add(0, new StringPair("%param"+index,                   param));
         
         if (index == 0) {
            substitutions.add(new StringPair("%variable",                variableKey));
            substitutions.add(new StringPair("%description",             description));
            substitutions.add(new StringPair("%shortDescription",        shortDescription));
            substitutions.add(new StringPair("%tooltip",                 tooltip));
            substitutions.add(new StringPair("%paramName",               paramName));
            substitutions.add(new StringPair("%enumParam",               paramName));
            substitutions.add(new StringPair("%paramType",               paramType));
            substitutions.add(new StringPair("%enumClass",               paramType));
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
    * @param tag
    * @throws Exception
    */
   void templateBasicCheck(String namespace, String key, String tag) throws Exception {
      tag = "<"+tag+">";
      if (namespace.isBlank()) {
         throw new Exception(tag+" is missing namespace, key='" + key + "'");
      }
      if ((key != null) && !namespace.equals("all")) {
         throw new Exception("Named "+tag+" must have 'all' namespace, key='" + key + "'");
      }
      if ((key == null) && namespace.equals("all")) {
         throw new Exception(tag+" must have 'key' attribute in 'all' namespace, peripheral='" + fPeripheral.getName() + "'");
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
         String repeatKey = getKeyAttribute(element);
         if (repeatKey==null) {
            // Assume associated with Base class
            repeatKey = "_"+fPeripheral.getBaseName();
         }
         repeatKey = "$TEMPLATE"+repeatKey;

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

      if (!checkTemplateConditions(element)) {
         return;
      }
      String key           = getKeyAttribute(element);
      String namespace     = getAttributeAsString(element, "namespace", "info"); // info|usbdm|class|all
      if (key != null) {
         namespace = "all";
      }
      templateBasicCheck(namespace, key, element.getTagName());
      
      if (key != null) {
         // Text after '.' is used to give templates a
         // unique value for discardRepeats and is not actual part of the key
         int dotIndex = key.indexOf(".");
         if (dotIndex > 0) {
            key = key.substring(0, dotIndex);
         }
      }
      List<StringPair> substitutions = getTemplateSubstitutions(element, "variables");
      
      if (substitutions == null) {
         // Non-empty variable list and variables not found
         return;
      }
      TemplateInformation templateInfo = addTemplate(key, namespace, getAttributeAsString(element, "codeGenCondition"));
      
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

      if (var == null) {
         return caseBody + "(var not present)";
      }
      
      String returnFormat = getAttributeAsString(element, "returnFormat");
      if (returnFormat == null) {
         return caseBody + "(returnFormat not present)";
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
      ChoiceData[] choiceData = choiceVar.getChoiceData();

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
      String namespace    = getAttributeAsString(element, "namespace", "info");
      if (key != null) {
         namespace = "all";
      }

      templateBasicCheck(namespace, key, element.getTagName());
      
      String variable  = getAttributeAsString(element, "variable");
      String variables = getAttributeAsString(element, "variables");
      List<StringPair> substitutions;
      
      if (variable != null) {
         Variable var = safeGetVariable(variable);
         if (var == null) {
            return;
         }
         String caseBody = getTemplateCaseStatement(element, var);
         substitutions = getTemplateSubstitutions(element, "variable");
         substitutions.add(0, new StringPair("%body", caseBody));
      }
      else if (variables != null) {
         String[] t = variables.split(",");
         // Use 1st variable
         Variable    var = safeGetVariable(t[0]);
         if (var == null) {
            return;
         }
         String caseBody = getTemplateCaseStatement(element, var);
         substitutions = getTemplateSubstitutions(element, "variables");
         substitutions.add(0, new StringPair("%body", caseBody));
      }
      else {
         throw new Exception("<clockCodeTemplate> must have 'variable' or 'variables' attribute, key='" + key + "'");
      }
         
      TemplateInformation templateInfo = addTemplate(key, namespace, getAttributeAsString(element, "codeGenCondition"));
      
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

      boolean mustExist = Boolean.parseBoolean(getAttributeAsString(element, "mustExist"));
      boolean wasDeleted = fProvider.removeVariableByName(key);

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
      else if (tagName == "rtcTimeOption") {
         parseRtcTimeOption(parentModel, element);
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
      else if (tagName == "pinMapOption") {
         parsePinmapOption(parentModel, element);
      }
      else if (tagName == "irqOption") {
         parseIrqOption(parentModel, element);
      }
      else if (tagName == "clockSelectionOption") {
         parseClockSelectionOption(parentModel, element);
      }
      else if (tagName == "clockMultiplexorOption") {
         parseClockMultiplexorOption(parentModel, element);
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
      else if (tagName == "title") {
         parseTitle(parentModel, element);
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
         parseForLoop(parentModel, element, null);
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
         parseValidate(element);
      }
      else if (tagName == "clockCodeTemplate") {
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
      else if (tagName == "constructorTemplate") {
         if (!element.hasAttribute("variables")) {
            throw new Exception("<constructorTemplate> must have 'variables' attribute, key='" + key + "'");
         }
         parseTemplate(element);
      }
      else if (tagName == "setTemplate") {
         if (!element.hasAttribute("variables")) {
            throw new Exception("<setTemplate> must have 'variables' attribute, key='" + key + "'");
         }
         parseTemplate(element);
      }
      else if (tagName == "deleteOption") {
         parseDeleteOption(element);
      }
      else if (tagName == "equation") {
         parseEquation(element);
      }
      else if (tagName == "projectActionList") {
         if (checkCondition(element)) {
            ProjectActionList pal = PackageParser.parseRestrictedProjectActionList(element, RESOURCE_PATH);
            pal.visit(new Visitor() {
               @Override
               public Result applyTo(ProjectAction action, Value result, IProgressMonitor monitor) {
                  if (action instanceof ProjectConstant) {
                     ProjectConstant constant = (ProjectConstant) action;
                     Variable var = new StringVariable(constant.getId(), constant.getId());
                     var.setValue(constant.getValue());
                     fProvider.addVariable(var);
                  }
                  return Visitor.CONTINUE;
               }}, null);
            fProjectActionList.addProjectAction(pal);
         }
      }
      else if (tagName == "graphic") {
         parseGraphic(parentModel, element);
      }
      else {
         throw new Exception("Unexpected tag in parseControlItem(), \'"+tagName+"\'");
      }
   }

   /**
    * Parse an equation &lt;equation key="name" value="=1+12" /&gt;
    * 
    * @param element
    * @throws Exception
    */
   private void parseEquation(Element element) throws Exception {
      String key        = getKeyAttribute(element);
      Object expression = getAttribute(element, "value");
      boolean isConstant = getAttributeAsBoolean(element, "constant", false);
      Variable var = fProvider.safeGetVariable(key);
      
      if (var == null) {
         if (expression instanceof Long) {
            var = new LongVariable(expression.toString(), key);
         }
         else if (expression instanceof Double) {
            var = new DoubleVariable(expression.toString(), key);
         }
         else if (expression instanceof Boolean) {
            var = new BooleanVariable(expression.toString(), key);
         }
         else if (expression instanceof String) {
            var = new StringVariable(expression.toString(), key);
         }
         else {
            throw new Exception("Unexpected type for expression result. Type = "+expression.getClass()+", eqn = "+expression);
         }
         fProvider.addVariable(var);
         var.setConstant(isConstant);
         var.setDerived(true);
         var.setLocked(true);
      }
      var.setValue(expression);
   }

   static class GraphicWrapper {
      final int                  fBoxX, fBoxY;
      final ClockSelectionFigure fClockSelectionFigure;
      final ParseMenuXML         fParser;
      /**
       * 
       * @param boxX     Current X coordinate
       * @param boxY     Current Y coordinate
       * @param figure   Current figure
       */
      public GraphicWrapper(ParseMenuXML parser, int boxX, int boxY, ClockSelectionFigure figure) {
         fBoxX                   = boxX;
         fBoxY                   = boxY;
         fClockSelectionFigure   = figure;
         fParser                 = parser;
      }

      void parseGraphicBoxOrGroup(BaseModel parentModel, Element graphicElement) throws Exception {
         fParser.parseGraphicBoxOrGroup(parentModel, fBoxX, fBoxY, fClockSelectionFigure, graphicElement);
      }
   }
   
   /**
    * 
    * @param parentModel   Model to attache figure to
    * @param boxX          Current X coordinate
    * @param boxY          Current Y coordinate
    * @param figure        Current figure
    * @param boxElement    XML to parse
    * 
    * @throws Exception
    */
   private void parseGraphicBoxOrGroup(BaseModel parentModel, int boxX, int boxY, ClockSelectionFigure figure, Element boxElement) throws Exception {
      
      String boxId     = getAttributeAsString(boxElement, "id", "");
      String boxParams = getAttributeAsString(boxElement, "params", "");
      
      boxX +=  getAttributeAsLong(boxElement, "x", 0L);
      boxY +=  getAttributeAsLong(boxElement, "y", 0L);
      
      int x = boxX;
      int y = boxY;
      
      if (boxElement.getTagName().equals("graphicBox")) {
         figure.add(x,y, boxId, null, null, "box", null, boxParams);
      }
      
      for (Node node = boxElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element graphic = (Element) node;
         if (!checkCondition(graphic)) {
            // Discard element
            continue;
         }
         String tagName = graphic.getTagName();
         if (tagName == "graphicItem") {
            String id     = getAttributeAsString(graphic,     "id");
            String varKey = getKeyAttribute(graphic,  "var");
            String type   = getAttributeAsString(graphic,     "type");
            String edit   = getAttributeAsString(graphic,     "edit");
            String params = getAttributeAsString(graphic,     "params");
            String name   = getAttributeAsString(graphic,     "name");
            
            if ((name != null) && name.startsWith("@")) {
               String varName = name.substring(1);
               int index = varName.indexOf('.');
               int choiceIndex = -1;
               if (index >= 0) {
                  choiceIndex = Integer.valueOf(varName.substring(index+1));
                  varName = varName.substring(0,index);
               }
               ChoiceVariable var = safeGetChoiceVariable(varName);
               if (var == null) {
                  throw new Exception("Unable to find var " + varName);
               }
               if (choiceIndex >= 0) {
                  name = var.getChoiceData()[choiceIndex].getName();
               }
               else {
                  name = var.getValueAsString();
               }
            }
            figure.add(x, y, id, name, varKey, type, edit, params);
            continue;
         }
         if (tagName == "offset") {
            x = boxX + Integer.parseInt(getAttributeAsString(graphic, "x", "0"));
            y = boxY + Integer.parseInt(getAttributeAsString(graphic, "y", "0"));
            continue;
         }
         if (tagName == "graphicBox") {
            parseGraphicBoxOrGroup(parentModel, x, y, figure, graphic);
            continue;
         }
         if (tagName == "graphicGroup") {
            parseGraphicBoxOrGroup(parentModel, x, y, figure, graphic);
            continue;
         }
         if (tagName == "for") {
            GraphicWrapper dummy = new GraphicWrapper(this, x, y, figure);
            parseForLoop(parentModel, graphic, dummy);
            continue;
         }
         if (tagName == "equation") {
            parseEquation(graphic);
            continue;
         }
         throw new Exception("Expected tag = <graphicItem>/<offset>/<graphicBox>/<graphicGroup>, found = <"+tagName+">");
      }
   }

   private void parseGraphic(BaseModel parentModel, Element element) throws Exception {
      
      ClockSelectionFigure figure = new ClockSelectionFigure(fProvider, 0 /* getIntAttribute(element, "clockConfigIndex" */);
      
      OpenGraphicModel model = new OpenGraphicModel(
            parentModel,
            getKeyAttribute(element),
            fProvider.safeGetVariable(getKeyAttribute(element, "var")),
            figure);
      
      model.setToolTip(getAttributeAsString(element, "toolTip"));
      model.setSimpleDescription(getAttributeAsString(element, "description"));
      
      for (Node node = element.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element boxElement = (Element) node;
         if (!checkCondition(boxElement)) {
            // Discard element
            continue;
         }
         String tagName = boxElement.getTagName();
         if (tagName == "graphicBox") {
            parseGraphicBoxOrGroup(parentModel, 0, 0, figure, boxElement);
            continue;
         }
         if (tagName == "graphicGroup") {
            parseGraphicBoxOrGroup(parentModel, 0, 0, figure, boxElement);
            continue;
         }
         if (tagName == "equation") {
            parseEquation(boxElement);
            continue;
         }
         if (tagName == "for") {
            GraphicWrapper graphicWrapper = new GraphicWrapper(this, 0, 0, figure);
            parseForLoop(parentModel, boxElement, graphicWrapper);
            continue;
         }
         throw new Exception("Expected tag = <graphicBox>, found = <"+tagName+">");
      }
   }

   /**
    * 
    * @param key                       Key used to index template
    * @param namespace                 Namespace for template (info, usbdm, all)
    * @param codeGenerationCondition   Condition controlling code generation
    * 
    * @return
    * 
    * @throws Exception
    */
   private TemplateInformation addTemplate(String key, String namespace, String codeGenerationCondition) throws Exception {
      
      if (key == null) {
         key = "";
      }
      TemplateInformation templateInfo = new TemplateInformation(key, namespace, codeGenerationCondition);

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
      boolean optional  = Boolean.valueOf(getAttributeAsString(element, "optional"));
      String peripheralName = getAttributeAsString(element, "name");
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
      // Add information for later action
      String  filter    = getAttributeAsString(element, "filter");
      String  enabledBy = getAttributeAsString(element, "enabledBy");
      Boolean locked    = getAttributeAsBoolean(element, "locked", false);
      
      fPeripheral.addSignalsFromPeripheral(peripheral, filter, enabledBy, locked);
   }

   private static class ChoiceInformation {
      final ArrayList<ChoiceData> entries;
      final ArrayList<ChoiceData> hiddenEntries;
      final Integer               defaultEntry;
      
      public ChoiceInformation(ArrayList<ChoiceData> entries, ArrayList<ChoiceData> hiddenEntries, Integer defaultEntry) {
         this.entries         = entries;
         this.hiddenEntries   = hiddenEntries;
         this.defaultEntry    = defaultEntry;
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
      
      boolean defaultExplicitlySet = false;
      ArrayList<ChoiceData> entries       = new ArrayList<ChoiceData>();
      ArrayList<ChoiceData> hiddenEntries = new ArrayList<ChoiceData>();
      Integer defaultValue = null;
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
         Boolean keepChoice = (Boolean) fExpressionParser.evaluate(getAttributeAsString(element, "condition"));
         if (!keepChoice) {
            // Discard choice
            continue;
         }
         boolean hidden = false;
         if (element.hasAttribute("hidden")) {
            hidden = (Boolean) fExpressionParser.evaluate(getAttributeAsString(element, "hidden"));
         }
         ChoiceData entry = new ChoiceData(
               getAttributeAsString(element, "name"),
               getAttributeAsString(element, "value"),
               getAttributeAsString(element, "enum"),
               getAttributeAsString(element, "code"),
               getAttributeAsString(element, "ref"),
               getAttributeAsString(element, "enabledBy"),
               getAttributeAsString(element, "pinMap"),
               fProvider
               );
         if (hidden) {
            hiddenEntries.add(entry);
         }
         else {
            entries.add(entry);
         }
         if (defaultValue == null) {
            // Assume 1st entry is default
            defaultValue = 0;
         }
         if (element.hasAttribute("isDefault")) {
            if ((Boolean) fExpressionParser.evaluate(getAttributeAsString(element, "isDefault"))) {
               // Explicit default set
               if (defaultExplicitlySet) {
                  throw new Exception("Multiple default choices set in <"+menuElement.getTagName() + " name=\""+menuElement.getAttribute("name")+"\"> <choice name=\"" + getAttributeAsString(element, "name")+ "\">");
               }
               defaultExplicitlySet = true;
               defaultValue = entries.size()-1;
            }
         }
      }
      return new ChoiceInformation(entries, hiddenEntries, defaultValue);
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
      
      ChoiceInformation choiceInfo = parseChoiceData(menuElement);
      
      Variable otherVariable = getDerivedFrom(menuElement);
      if (choiceInfo.entries.isEmpty() && (otherVariable != null)) {
         /**
          * Should be another variable of the same type to copy from i.e. derivedFrom="" present
          */
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
            var.setData(otherVar.getChoiceData());
            var.setDefault(otherVar.getDefault());
            var.setValue(otherVar.getDefault());
         }
      }
      else {
         // Set of choices provided (may be empty!)
         if (variable instanceof BooleanVariable) {
            if (choiceInfo.entries.size()>2) {
               throw new Exception("Wrong number of choices in <"+menuElement.getTagName() + " name=\"" + variable.getName()+ "\">");
            }
            BooleanVariable booleanVar = (BooleanVariable) variable;

            if (choiceInfo.entries.size()==2) {
               booleanVar.setFalseValue(choiceInfo.entries.get(0));
               booleanVar.setTrueValue(choiceInfo.entries.get(1));
            }
            else {
               ChoiceData choiceData = choiceInfo.entries.get(0);
               if (Boolean.parseBoolean(choiceData.getValue()) ||
                   (Character.isDigit(choiceData.getValue().charAt(0)) && Integer.parseInt(choiceData.getValue())>0)) {
                  booleanVar.setTrueValue(choiceData);
               }
               else {
                  booleanVar.setFalseValue(choiceData);
               }
            }
            Object tmp;
            tmp = booleanVar.getDefault();
            if (tmp == null) {
               booleanVar.setDefault(choiceInfo.defaultEntry);
            }
            tmp = booleanVar.getValue();
            if (tmp == null) {
               booleanVar.setValue(choiceInfo.defaultEntry);
            }
         }
         else if (variable instanceof ChoiceVariable) {
            ChoiceVariable choiceVar = (ChoiceVariable)variable;
            choiceVar.setChoiceData(choiceInfo.entries, choiceInfo.hiddenEntries);
            if (choiceInfo.defaultEntry != null) {
               Object tmp;
               tmp = choiceVar.getDefault();
               if (tmp == null) {
                  // Set default if not set
                  choiceVar.setDefault(choiceInfo.defaultEntry);
               }
               tmp = choiceVar.getDisabledValue();
               if (tmp == null) {
                  // Set default if not set
                  choiceVar.setDisabledValue(choiceInfo.defaultEntry);
               }
               tmp = choiceVar.getValue();
               if (tmp == null) {
                  // Set current value if not set
                  choiceVar.setValue(choiceInfo.defaultEntry);
               }
            }
         }
      }
   }

   public static class ValidatorInformation {
      private String            fClassName;
      private ArrayList<Object> fParams = null;
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
         if (fParams == null) {
            fParams = new ArrayList<Object>();
         }
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
      ValidatorInformation validator = new ValidatorInformation(getAttributeAsString(validateElement, "class"), (int)dimension);
      
      SimpleExpressionParser parser = new SimpleExpressionParser(fProvider, Mode.EvaluateFully);
      
      for (Node node = validateElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == "param") {
            String type  =getAttributeAsString(element, "type");
            String value =getAttributeAsString(element, "value");
            
            // Do substitutions on parameter
            if (type.equalsIgnoreCase("long")) {
               validator.addParam(parser.evaluate(value));
            }
            else if (type.equalsIgnoreCase("string")) {
               validator.addParam(parser.evaluate(value));
            }
            else {
               throw new Exception("Unexpected type in <validate>, value = \'"+element.getTagName()+"\'");
            }
         }
         else {
            throw new Exception("Unexpected field in <validate>, value = \'"+element.getTagName()+"\'");
         }
      }
      fValidators.add(validator);
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

      String name = getAttributeAsString(element, "name");
      if (name.equalsIgnoreCase("_instance")) {
         name = fProvider.getName();
      }
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
      
      String name = getAttributeAsString(topElement, "name");
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
      
      String name = getAttributeAsString(element, "name");
      if ((name != null) && name.equalsIgnoreCase("_instance")) {
         name = fProvider.getName();
      }
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
         System.err.println("parse(): " + element.getTagName() + ", " + getAttributeAsString(element, "name"));
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
            throw new Exception("Alias not found for '" + key + "' within '"+parent + "', provider = '"+provider+"'");
         }
         return null;
      }
      String description = aliasModel.getSimpleDescription();
      if (!description.isEmpty()) {
         if ((variable.getDescription() != null) && !variable.getDescription().isEmpty()) {
            throw new Exception("Alias tries to change description for " + key);
         }
         variable.setDescription(description);
      }
      String toolTip = aliasModel.getRawToolTip();
      if ((toolTip != null) && !toolTip.isEmpty()) {
         if ((variable.getToolTip() != null) && !variable.getToolTip().isEmpty()) {
            throw new Exception("Alias tries to change toolTip for " + key + ", tooltip="+toolTip);
         }
         variable.setToolTip(toolTip);
      }
      VariableModel model = variable.createModel(null);
      boolean isConstant = aliasModel.isLocked() || variable.isLocked();
      model.setLocked(isConstant);
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
         BaseModel model = children.get(index);
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
         else if (model instanceof OpenGraphicModel) {
            OpenGraphicModel ogm = (OpenGraphicModel)model;
            ClockSelectionFigure figure = ogm.getFigure();
            figure.instantiateGraphics(provider);
//            figure.report();
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

      System.out.println("Loading document (for " + ((provider==null)?"null":provider.getName()) + ") : " + document.getBaseURI());
      
      // TODO Trace parsing peripheral files here
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

      // For debug try local directory
      if (Files.isRegularFile(path)) {
         return path;
      }
      // Look in USBDM installation\
      String p = Usbdm.getUsbdmResourcePath();
      path = Paths.get(p).resolve(path);
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
               if (v.getParams() != null) {
                  // peripheral+dim+args
                  Constructor<?> constructor = clazz.getConstructor(PeripheralWithState.class, Integer.class, v.getParams().getClass());
                  validator = (PeripheralValidator) constructor.newInstance(peripheral, dimension, v.getParams());
               }
               else {
                  // peripheral+dim
                  Constructor<?> constructor = clazz.getConstructor(PeripheralWithState.class, Integer.class);
                  validator = (PeripheralValidator) constructor.newInstance(peripheral, dimension);
               }
            }
            else {
               if (v.getParams() != null) {
                  // peripheral+args
                  Constructor<?> constructor = clazz.getConstructor(PeripheralWithState.class, v.getParams().getClass());
                  validator = (PeripheralValidator) constructor.newInstance(peripheral, v.getParams());
               }
               else {
                  // peripheral
                  Constructor<?> constructor = clazz.getConstructor(PeripheralWithState.class);
                  validator = (PeripheralValidator) constructor.newInstance(peripheral);
               }
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
         Document document = XML_BaseParser.parseXmlFile(path);
         fData = parse(document, variableProvider, null);

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
