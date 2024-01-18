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
import net.sourceforge.usbdm.deviceEditor.information.ListVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.NumericListVariable;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.PinListVariable;
import net.sourceforge.usbdm.deviceEditor.information.RtcTimeVariable;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.TimeVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.Units;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;
import net.sourceforge.usbdm.deviceEditor.model.AliasPlaceholderModel;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.ErrorModel;
import net.sourceforge.usbdm.deviceEditor.model.ListVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.OpenGraphicModel;
import net.sourceforge.usbdm.deviceEditor.model.ParametersModel;
import net.sourceforge.usbdm.deviceEditor.model.SignalPinMapping;
import net.sourceforge.usbdm.deviceEditor.model.TitleModel;
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

/**
 * Notes
 * <ul>
 * <li> Some attributes are treated as immediate expressions e.g. condition="".<br>
 *      This means they are immediately evaluated (<b>during XML parsing</b>) <br>
 *      Unless prefixed by '@', variables referenced are only checked for existence:<br>
 *      - existent => true, nonexistent => false <br>
 *      Variables prefixed by '@' are fully evaluated and nonexistence is an error.
 * <li> Attributes prefixed by '=' are treated as expressions.<br>
 *      They are immediately evaluated (<b>during XML parsing</b>) but variables are fully evaluated and nonexistence is an error.
 * <li> Attributes starting with \= are escaped to start with = and are treated as a simple string.
 * <li> Other attributes are treated as a simple string and their purpose depends on context.
 * </ul>
 */
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

      /**
       * Prune hidden children
       */
      public void prune() {
         fRootModel.prune();
      }

   }

   public static class ForLoop {

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

      private VariableProvider fProvider;

      /**
       * Construct for-loop element to keep track of substitutions
       * @param provider
       * 
       * @param keys    List of keys e.g. "keyA,keyB"
       * @param values  List of values e.g. "valA0,valB0;valA1,valB1;valA2,valB2"
       * @param delimiter Delimiter to separate multiple keys/values
       * 
       * @throws Exception
       */
      public ForLoop(VariableProvider provider, String keys, Object values, String delimiter) throws Exception {
         if (keys.contains(",")) {
            throw new ForloopException("Can't have ',' in keys '" + keys + "'");
         }
         fProvider = provider;
         fKeys       = keys.split(":");
         if (values instanceof String) {
            fValueList  = ((String)values).split(Pattern.quote(delimiter));
         }
         else if(values.getClass().isArray()) {
            fValueList = new String[((Object[]) values).length];
            int index = 0;
            for (Object obj:(Object[]) values) {
               fValueList[index++] = obj.toString();
            }
         }
         else {
            fValueList = new String[1];
            fValueList[0] = values.toString();
         }
         for (int index=0; index<fValueList.length; index++) {
            if (fValueList[index].startsWith("@")) {
               //               String t = fValueList[index];
               fValueList[index] = Expression.getValue(fValueList[index].substring(1), provider).toString();
               //               System.err.println("Found it "+t + " => " + fValueList[index]);
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
            System.err.println("fValueList = \n" + Arrays.toString(fValueList));
            System.err.println("fKeys      = \n" + Arrays.toString(fKeys));
            throw new ForloopException(
                  "Number of values '" + fValueList[iter]+
                  "' does not match number of keys = "+fKeys.length);
         }
         for (int index=0; index<fKeys.length; index++) {
            // Do substitutions
            String value = fValues[index];
            if (value.startsWith("\\=")) {
               value = value.substring(1);
            }
            else if (value.startsWith("=")) {
               try {
                  value = Expression.getValue(value.substring(1),fProvider).toString();
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
            text = text.replace("%("+fKeys[index].trim()+")", value);
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
      public void createLevel(VariableProvider fProvider, String keys, Object values, String delimiter) throws Exception {
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
   //   private final SimpleExpressionParser  fExpressionParser;

   /** Provider providing the variables used by the menu */
   private final VariableProvider  fProvider;

   /** Peripheral to add vectors etc to */
   private final PeripheralWithState fPeripheral;

   /**
    * Templates being accumulated.
    * This is a map using (key + namespace) as map key.
    * Multiple matching templates are kept in a list rather than combined to allow individual conditions etc.
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

   private long uniqueNameCounter = 0;
   
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
    * @return value as Long or defaultValue if attribute not present<br>
    *         If the attribute is a simple string it is parsed as a Long.<br>
    *         If the attribute value starts with "=" then it is evaluated as an immediate expression
    *         with variable substitution.  If this results in a Long it is returned, otherwise it is
    *         converted to a string and parsed as a Long.
    * 
    * @throws Exception
    */
   protected Long getLongExpressionAttribute(Element element, String name, Long defaultValue) throws Exception {
      Object attr = getAttribute(element, name);
      if (attr == null) {
         return defaultValue;
      }
      if (attr instanceof Long) {
         return (Long) attr;
      }
      try {
         return Long.decode(attr.toString());
      } catch (NumberFormatException e) {
         throw new NumberFormatException("Failed to parse Long Attribute \'"+name+"\' value '"+attr+"'");
      }
   }

   /**
    * Get a long attribute
    * 
    * @param element Element being examined
    * @param name    Attribute name
    * 
    * @return value as Long or null if attribute not present<br>
    *         If the attribute is a simple string it is parsed as a Long.<br>
    *         If the attribute value starts with "=" then it is evaluated as an immediate expression
    *         with variable substitution.  If this results in a Long it is returned, otherwise it is
    *         converted to a string and parsed as a Long.
    * 
    * @throws Exception
    */
   protected Long getLongExpressionAttribute(Element element, String name) throws Exception {
      return getLongExpressionAttribute(element, name, null);
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
      Object values     = getAttribute(element, "values");
      String delimiter  = getAttributeAsString(element, "delimiter", ";");

      //      if (delimiter != ";") {
      //         System.err.println("Found it, del = "+delimiter);
      //      }
      if (keys.isBlank()) {
         throw new Exception("<for> requires keys = '"+keys+"', values = '"+values+"'");
      }


      Integer[] dims = getAttributeAsListOfInteger(element, "dim");
      if (dims != null) {
         if (values != null) {
            throw new Exception("Both values and dim attribute given in <for> '" + keys +"'");
         }
         int start;
         int end;
         if (dims.length == 1) {
            start = 0;
            end   = dims[0].intValue();
         }
         else if (dims.length == 2) {
            start = dims[0];
            end   = dims[1]; // +1 fix!
         }
         else {
            throw new Exception("Illegal dim value '"+dims+"' for <for> '"+keys+"'");
         }
         StringBuilder sb = new StringBuilder();
         for (int index=start; index<end; index++) {
            sb.append(index+";");
         }
         values=sb.toString();
      }
      if ((values instanceof String) && ((String)values).isBlank()) {
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

   static String wrapText(String text, final int maxColumn) {
      int column = 0;
      StringBuilder lineBuffer = new StringBuilder();
      StringBuilder wordBuffer = new StringBuilder();
      
      boolean newLine = true;
      
      for (int chIndex=0; chIndex<text.length(); chIndex++) {
         char ch = text.charAt(chIndex);
         if (column>maxColumn) {
            lineBuffer.append("\n");
            column = wordBuffer.length();
            newLine = true;
         }
         else if (Character.isWhitespace(ch)) {
            if (wordBuffer.length()>0) {
               if (!newLine) {
                  lineBuffer.append(" ");
               }
               lineBuffer.append(wordBuffer);
               newLine = false;
               wordBuffer = new StringBuilder();
            }
            if (ch=='\n') {
               lineBuffer.append("\n");
               column = 0;
               newLine = true;
               continue;
            }
            if ((ch=='\t') && newLine) {
               lineBuffer.append("    ");
               continue;
            }
            continue;
         }
         wordBuffer.append(ch);
         column++;
      }
      if (wordBuffer != null) {
         if (!newLine) {
            lineBuffer.append(" ");
         }
         lineBuffer.append(wordBuffer);
      }
      return lineBuffer.toString();
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
      final int MAX_COLUMN = 100;
      String text = doForSubstitutions(getAttributeAsString(element, "toolTip"));
      if (text == null) {
         return text;
      }
      text = text.replaceAll("\\\\n( +)", "\n").replaceAll("\\\\t", "  ");
      text = wrapText(text, MAX_COLUMN);
      return text;
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
      String name = getNameAttribute(varElement);

      boolean replace = Boolean.valueOf(getAttributeAsString(varElement, "replace"));
      boolean modify  = Boolean.valueOf(getAttributeAsString(varElement, "modify"));

      if (key.contains("[")) {
         String baseKey = key.substring(0,key.length()-3);
         if (safeGetVariable(baseKey) != null) {
            throw new RuntimeException("Creating non-scalar after scaler variable "+baseKey);
         }
      }
      else {
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
            throw new Exception(
                  "Overridden variable\n"
                  + "   "+existingVariable.toString()+" is has different type to \n"
                  + "     new type = " + clazz.getSimpleName());
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
    * <li>typeName
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
         Expression enabledBy = otherVariable.getEnabledBy();
         if (enabledBy != null) {
            variable.setEnabledBy(enabledBy);
         }
         variable.setRegister(otherVariable.getRegister());
      }
      String addToVarNames = getAttributeAsString(varElement, "addToVar");
      if (addToVarNames != null) {
         for (String varName:addToVarNames.split(",")) {
            Variable addToVar = safeGetVariable(varName);
            if (addToVar == null) {
               // Create new list
               addToVar = new StringVariable(null, fProvider.makeKey(varName));
               fProvider.addVariable(addToVar);
               addToVar.setValue(variable.getName());
//               System.err.println("'addToVar' target variable not found, '" + addToVarName);
            }
            else {
               // Add to existing list
               addToVar.setValue(addToVar.getValueAsString()+";"+variable.getName());
            }
         }
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
      variable.setToolTip(getToolTip(varElement));
      
      variable.setLogging(getAttributeAsBoolean(varElement, "logging", false));
      
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(getAttributeAsString(varElement, "origin"));
      }
      if (varElement.hasAttribute("derived")) {
         variable.setDerived(Boolean.valueOf(getAttributeAsString(varElement, "derived")));
      }
      if (varElement.hasAttribute("typeName")) {
         variable.setTypeName(getAttributeAsString(varElement, "typeName", null));
         String check = getAttributeAsString(varElement, "typeName", null);
         if (check.contains("Ticks")) {
            System.err.println("Illegal value for typeName '"+ check + "'");
         }
      }
      if (varElement.hasAttribute("baseType")) {
         variable.setBaseType(getAttributeAsString(varElement, "baseType", null));
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
      if (varElement.hasAttribute("hiddenBy")) {
         variable.setHiddenBy(getAttributeAsString(varElement, "hiddenBy"));
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
         Object value = getAttribute(varElement, "value");
         variable.setValueQuietly(value);
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


      if (otherVariable != null) {
         do {
            String currentValueFormat = getAttributeAsString(varElement, "valueFormat");
            if (currentValueFormat != null) {
               // valueFormat explicitly set - don't change
               break;
            }
            String otherValueFormat   = otherVariable.getValueFormat();
            if (otherValueFormat != null) {
               // no valueFormat to copy
               break;
            }
            String autoValueFormat    = getDefaultValueFormat(otherVariable);
            if (autoValueFormat.equals(otherValueFormat) ) {
               // Other valueFormat was auto generated - don't copy
               break;
            }
            // Copy from other variable
            System.err.println("Copying format '"+otherValueFormat+"' from " + otherVariable.getName() +
                  " in favour of '" + getDefaultValueFormat(variable));
            variable.setValueFormat(otherVariable.getValueFormat());
         } while(false);
      }

      VariableModel model = variable.createModel(parent);
      model.setLocked(getAttributeAsBoolean(varElement, "locked", false));
      model.setHidden(getAttributeAsBoolean(varElement, "hidden", false));
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

   private void parseTimeOption(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }

      TimeVariable variable = (TimeVariable) createVariable(varElement, TimeVariable.class);

      parseCommonAttributes(parent, varElement, variable);

      if (varElement.hasAttribute("periodEquation")) {
         variable.setPeriodExpression(getAttributeAsString(varElement, "periodEquation"));
      }
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

      switch(variable.getUnits()) {
      case None:
         throw new Exception("<timeOption> must have units");
      case ticks:
         if (variable.getPeriodExpression() == null) {
            throw new Exception("<timeOption> must have periodEquation if using ticks");
         }
      case Hz:
      case s:
      case percent:
         break;
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
      if (varElement.hasAttribute("radix")) {
         variable.setRadix(getRequiredLongAttribute(varElement, "radix"));
      }
      parseCommonAttributes(parent, varElement, variable);
      generateEnum(varElement, variable);
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
            variable.setMin(getAttributeAsString(varElement, "min"));
         }
         if (varElement.hasAttribute("max")) {
            variable.setMax(getAttributeAsString(varElement, "max"));
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
      if (!checkCondition(varElement)) {
         return;
      }
      BitmaskVariable variable = (BitmaskVariable) createVariable(varElement, BitmaskVariable.class);
      
      Long   bitmask        = null;
      String bitList        = null;
      String bitDescription = null;

      String uses = getAttributeAsString(varElement, "uses");
      if (uses != null ) {
         ChoiceVariable cv = safeGetChoiceVariable(uses);
         ChoiceData[] cd = cv.getChoiceData();
         StringBuilder bitListSb = new StringBuilder();
         StringBuilder bitDescriptionSb = new StringBuilder();
         for (int index=0; index<cd.length; index++) {
            if (!bitListSb.isEmpty()) {
               bitListSb.append(",");
               bitDescriptionSb.append(",");
            }
            bitListSb.append(cd[index].getEnumName());
            bitDescriptionSb.append(cd[index].getName());
         }
         bitList        = bitListSb.toString();
         bitDescription = bitDescriptionSb.toString();
      }
      
      try {
         parseCommonAttributes(parent, varElement, variable);
         variable.init(
               getLongExpressionAttribute(varElement, "bitmask", bitmask),
               getAttributeAsString(varElement, "bitList", bitList),
               getAttributeAsString(varElement, "bitDescription", bitDescription)
               );
      } catch( Exception e) {
         throw new Exception("Illegal permittedBits value in " + variable.getName(), e);
      }
      variable.setPinMap(getAttributeAsString(varElement, "pinMap"));
      variable.setRadix(16);
      generateEnum(varElement, variable);
   }

   /**
    * Check if condition attached to element
    * 
    * @param element Element to examine
    * 
    * @return true  Condition is true or <b>not present</b>
    * @return false Condition is present and false
    * 
    * @throws Exception
    */
   boolean checkCondition(Element element) throws Exception {

      return getAttributeAsImmediateBoolean(element, "condition", Boolean.TRUE);
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

      generateEnum(varElement, variable);
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
      generateEnum(varElement, variable);
      generateTable(varElement, variable);
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
      
      variable.setTableName(getAttributeAsString(varElement, "tableName", null));
      
      parseChoices(variable, varElement);
      parseCommonAttributes(parent, varElement, variable);
      generateEnum(varElement, variable);
      generateTable(varElement, variable);
   }

   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception
    */
   private void parseDynamicSignalMapping(Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
      String signalName = getAttributeAsString(varElement, "signal", null);
      if (signalName == null) {
         throw new Exception("<dynamicSignalMapping> must have signal");
      }
      Signal signal = fPeripheral.getDeviceInfo().findSignal(signalName);
      SignalPinMapping signalPinMapping = new SignalPinMapping(signal);

      fPeripheral.getDeviceInfo().addDynamicSignalMapping(signalPinMapping);

      for (Node subNode = varElement.getFirstChild();
            subNode != null;
            subNode = subNode.getNextSibling()) {
         if (subNode.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) subNode;
         if (element.getTagName().equals("signalMapping")) {
            if (!checkCondition(element)) {
               continue;
            }
            String pinName = getAttributeAsString(element, "pin", null);
            if (pinName == null) {
               throw new Exception("<dynamicSignalMapping> must have 'pin='");
            }
            Pin pin;
            if ("-".equals(pinName)) {
               pin = Pin.UNASSIGNED_PIN;
            }
            else if ("*".equals(pinName)) {
               pin = signal.getOnlyMappablePin();
            }
            else {
               pin = fPeripheral.getDeviceInfo().findPin(pinName);
            }
            if (pin == null) {
               throw new Exception("<dynamicSignalMapping> Pin '"+pinName+"' not found for signal " + signalName);
            }
            String expression = getAttributeAsString(element, "expression", "true");
            signalPinMapping.addMapping(pin, expression, fProvider);
         }
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

      generateEnum(varElement, variable);
   }

   /// Format string with parameters: description, tool-tip, body
   String tableTypeDefinitionTemplate = ""
         + "      \\t/**\n"
         + "      \\t * %s\n"
         + "      \\t *\n"
         + "      \\t * %s\n"
         + "      \\t */\n"
         + "      %s\\n\\n\n";

   /// Format string with parameters: description, tool-tip, typeName, tableName, body
   String tableTemplate = ""
         + "      \\t/**\n"
         + "      \\t * %s\n"
         + "      \\t *\n"
         + "      \\t * %s\n"
         + "      \\t */\n"
         + "      \\tstatic constexpr %s %s[] = {\n"
         + "      %s"
         + "      \\t};\\n\\n\n";

   /// Format string with parameters: enum-guard description, tool-tip, typeName, tableName, body
   String guardedTableTemplate = ""
         + "#if %s\n"
         + "      \\t/**\n"
         + "      \\t * %s\n"
         + "      \\t *\n"
         + "      \\t * %s\n"
         + "      \\t */\n"
         + "      \\tstatic constexpr %s %s[] = {\n"
         + "      %s"
         + "      \\t};\n"
         + "#endif\\n\\n\n";

   private void generateTable(Element varElement, VariableWithChoices variable) throws Exception {

      String tableName = getAttributeAsString(varElement, "tableName", null);
      if (tableName==null) {
         return;
      }
      
      String tableType  = getAttributeAsString(varElement, "tableType", null);
      if (tableType==null) {
         throw new Exception("Table "+variable.getName()+" is missing tableType");
      }
      
      String typeName  = getAttributeAsString(varElement, "typeName", null);
      if (typeName==null) {
         throw new Exception("Table "+variable.getName()+" is missing typeName");
      }
      
      String tableDefinition  = getAttributeAsString(varElement, "tableDefinition", "");
      tableDefinition = tableDefinition.replace("%(tableType)", tableType);
      tableDefinition = tableDefinition.replace("%(typeName)", typeName);

      String templateKey = getAttributeAsString(varElement, "templateKey");
      
      String description     = escapeString(variable.getDescriptionAsCode());
      String tooltip         = escapeString(variable.getToolTipAsCode());

      TemplateInformation dataTemplateInfo = addTemplate(templateKey, "info",    null);

      StringBuilder enumBody = new StringBuilder();
      
      ArrayList<ChoiceData[]> lists = new ArrayList<ChoiceData[]>();
      lists.add(variable.getChoiceData());
      lists.add(variable.getHiddenChoiceData());
      for (ChoiceData[] choiceData:lists) {
         if (choiceData == null) {
            continue;
         }
         for (int index=0; index<choiceData.length; index++) {
            if (choiceData[index].getCodeValue() == null) {
               continue;
            }
            enumBody.append("\\t   "+choiceData[index].getCodeValue()+",\n");
         }
      }
      dataTemplateInfo.addText(String.format(tableTypeDefinitionTemplate, description, tooltip, tableDefinition));
      
      // Create enum declaration
      String enumGuard = getAttributeAsString(varElement, "enumGuard");
      String entireTable = null;
      if (enumGuard != null) {
         // Add guard
         entireTable = String.format(guardedTableTemplate, enumGuard, description, tooltip, tableType, tableName, enumBody.toString());
      }
      else {
         entireTable = String.format(tableTemplate, description, tooltip, tableType, tableName, enumBody.toString());
      }
      dataTemplateInfo.addText(entireTable);
   }
   
   /// Format string with parameters: description, tool-tip, enumClass, body
   String enumTemplate = ""
       /*                  */ + " \\t/**\n"
       /*  Description     */ + " \\t * %s\n"
       /*  Variable names  */ + " \\t * (%s)\n"
       /*                  */ + " \\t *\n"
       /*  Tooltip         */ + " \\t * %s\n"
       /*                  */ + " \\t */\n"
       /*  type,enumtype   */ + " \\tenum %s%s {\n"
       /*  body            */ + " %s"
       /*                  */ + " \\t};\\n\\n\n";

   /// Format string with parameters: description, tool-tip, enumClass, body
   String constantTemplate = ""
         + "      \\t/**\n"
         + "      \\t * %s\n"
         + "      \\t *\n"
         + "      \\t * %s\n"
         + "      \\t */\n"
         + "      %s\\n\\n\n";
   
   String guardedEnumTemplate = ""
         + "#if %s\n"
         + "%s"
         + "#endif\\n\\n\n";

   /**
    * Generate full enum for variable with choices
    * 
    * @param varElement
    * @param variable
    * @throws Exception
    */
   private void generateEnum(Element varElement, VariableWithChoices variable) throws Exception {

      String typeName  = variable.getTypeName();
      if (typeName==null) {
         return;
      }
      boolean createAsConstants = getAttributeAsBoolean(varElement, "createAsConstants", false);
      
      String macroName = Variable.getBaseNameFromKey(variable.getKey()).toUpperCase();

      if (!createAsConstants &&
         (fPeripheral != null) &&
         fPeripheral.getDeviceInfo().addAndCheckIfRepeatedItem("$ENUM"+typeName)) {
         // These are common!
         return;
      }

      String baseType = getAttributeAsString(varElement, "baseType");
      if (baseType != null) {
         baseType = " : "+baseType;
      }
      else {
         baseType = "";
      }
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

      // Use typeName attribute
      if ((typeName == null) || typeName.isBlank()) {
         throw new Exception("typeName is missing in " + variable);
      }
      String enumClass  = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

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
            if (createAsConstants) {
               completeEnumName = enumName;
            }
            else {
               completeEnumName = enumClass+"_"+enumName;
            }
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
            String completeValue = sb.toString()+(createAsConstants?";":",");
            //            valuesList.add(escapeString(completeValue));
            valuesList.add(completeValue);
            valueMax        = Math.max(valueMax, completeValue.length());

            commentsList.add(choiceData[index].getName());
         }

      }
      
      // Create enums body
      for (int index=0; index<enumNamesList.size(); index++) {
         if (createAsConstants) {
            body.append(String.format("\\tconstexpr %s %-"+enumNameMax+"s = %-"+valueMax+"s ///< %s\n", typeName, enumNamesList.get(index), valuesList.get(index), commentsList.get(index)));
         }
         else {
            body.append(String.format("\\t   %-"+enumNameMax+"s = %-"+valueMax+"s ///< %s\n", enumNamesList.get(index), valuesList.get(index), commentsList.get(index)));
         }
      }
      String enumText = getAttributeAsString(varElement, "enumText", null);
      if (enumText != null) {
         enumText = enumText.replace("%(typeName)",  typeName);
         enumText = enumText.replaceAll("\\\\n",  "XXXX");
         
         body.append(enumText+"\n");
      }
      // Create enum declaration
      String entireEnum;
      if (createAsConstants) {
         entireEnum = String.format(constantTemplate, description, tooltip, body.toString());
      }
      else {
         entireEnum = String.format(enumTemplate, description, variable.getName(), tooltip, enumClass, baseType, body.toString());
      }
      String enumGuard = getAttributeAsString(varElement, "enumGuard");
      if (enumGuard != null) {
         // Add guard
         entireEnum = String.format(guardedEnumTemplate, enumGuard, entireEnum);
      }
      templateInfo.addText(entireEnum);
   }

   /**
    * Generate full enum for variable with choices
    * 
    * @param varElement
    * @param variable
    * @throws Exception
    */
   private void generateEnum(Element varElement, BitmaskVariable variable) throws Exception {

      String typeName  = variable.getTypeName();
      if (typeName==null) {
         return;
      }

      String macroName = Variable.getBaseNameFromKey(variable.getKey()).toUpperCase();

      if ((fPeripheral != null) && fPeripheral.getDeviceInfo().addAndCheckIfRepeatedItem("$ENUM"+typeName)) {
         // These are common!
         return;
      }

      String doEnum = getAttributeAsString(varElement, "generateEnum", "true");
      if ("false".equalsIgnoreCase(doEnum)) {
         return;
      }
      boolean emptyEnum = "empty".equalsIgnoreCase(doEnum);
      
      String baseType = getAttributeAsString(varElement, "baseType");
      if (baseType != null) {
         baseType = " : "+baseType;
      }
      else {
         baseType = "";
      }

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

      String enumClass  = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

      String[]  bitNames = variable.getBitNames();
      String[]  bitDesc  = variable.getBitDescriptions();
      Integer[] bitMap   = variable.getBitMapping();

      StringBuilder body = new StringBuilder();
      
      if (!emptyEnum) {
         // Check length of enums
         int enumNameMax    = 0;
         for (int index=0; index<bitNames.length; index++) {
            bitNames[index] = makeSafeIdentifierName(bitNames[index]);
            String enumName = enumClass+"_"+bitNames[index];
            enumNameMax     = Math.max(enumNameMax, enumName.length());
         }

         // Create enums body
         for (int index=0; index<bitNames.length; index++) {
            String completeEnumName = enumClass+"_"+bitNames[index];
            String value = String.format(valueFormat, "1U<<"+bitMap[index])+",";
            
            body.append(String.format("\\t   %-"+enumNameMax+"s = %-10s %s\n", completeEnumName, value, (bitDesc==null)?"":("///< " +bitDesc[index])));
         }
      }
      String enumText = getAttributeAsString(varElement, "enumText", null);
      if (enumText != null) {
         enumText = enumText.replace("%(typeName)",  typeName);
         enumText = enumText.replaceAll("\\\\n[ ]*\\\\t",  "\n\\\\t");
         
         body.append(enumText+"\n");
      }
      // Create enum declaration
      String entireEnum = String.format(enumTemplate, description, variable.getName(), tooltip, enumClass, baseType, body.toString());

      String enumGuard = getAttributeAsString(varElement, "enumGuard");
      if (enumGuard != null) {
         // Add guard
         entireEnum = String.format(guardedEnumTemplate, enumGuard, entireEnum);
      }
      templateInfo.addText(entireEnum);
   }

   /**
    * Generate basic enum wrapper for integer type
    * 
    * @param varElement
    * @param variable
    * @throws Exception
    */
   private void generateEnum(Element varElement, LongVariable variable) throws Exception {

      String baseType = getAttributeAsString(varElement, "baseType");
      if (baseType == null) {
         return;
      }
      // Use typeName attribute
      String typeName  = variable.getTypeName();
      if (typeName == null) {
         return;
      }
      baseType = " : "+baseType;

      if ((fPeripheral != null) && fPeripheral.getDeviceInfo().addAndCheckIfRepeatedItem("$ENUM"+typeName)) {
         // These are common!
         return;
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

      String enumClass  = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

      body.append(enumText);

      // Create enum declaration
      String entireEnum;

      entireEnum = String.format(enumTemplate, description, variable.getName(), tooltip, enumClass, baseType, body.toString());
      String enumGuard = getAttributeAsString(varElement, "enumGuard");
      if (enumGuard != null) {
         // Add guard
         entireEnum = String.format(guardedEnumTemplate, enumGuard, entireEnum);
      }
      templateInfo.addText(entireEnum);
   }

   /**
    * Crude make a name pretty e.g. "FRED" -> "Fred"
    * 
    * @param name  Name to convert
    * 
    * @return  Converted name
    */
   static String makePrettyName(String name) {
      return Character.toUpperCase(name.charAt(0))+name.substring(1).toLowerCase();
   }

   /**
    * Crude create a C identifier name form a string e.g. FTM0/CMP1 => FTM0_CMP1
    * 
    * @param name  Name to convert
    * 
    * @return  Converted name
    */
   static String makeSafeIdentifierName(String name) {
      // Treat a list of pins etc as a special case
      String[] parts = name.split("/");
      if (parts.length>1) {
         StringBuilder sb = new StringBuilder();
         for (int index=0; index<parts.length; index++) {
            String part = parts[index];
            if (!sb.isEmpty()) {
               sb.append("_");
            }
            sb.append(part);
         }
         return sb.toString();
      }
      return name.replaceAll("[\\s]", "_");
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
    *  <li>"$(_instanceList)" => e.g FTM0 => FTM0;FTM1..     (fPeripheral.getInstanceList())
    *  <li>"$(_structName)"   => e.g FTM0 => FTM_Type
    * 
    * @param text  Test to process
    * 
    * @return  modified attribute or null if attribute doesn't exist
    */
   String replaceCommonNames(String text) {

      if (fPeripheral != null) {
         text = text.replace("$(_NAME)",         fPeripheral.getName());
         text = text.replace("$(_name)",         fPeripheral.getName().toLowerCase());
         text = text.replace("$(_BASENAME)",     fPeripheral.getBaseName());
         text = text.replace("$(_basename)",     fPeripheral.getBaseName().toLowerCase());
         text = text.replace("$(_Baseclass)",    fPeripheral.getClassBaseName());
         text = text.replace("$(_Class)",        fPeripheral.getClassName());
         text = text.replace("$(_instance)",     fPeripheral.getInstance());
      }
      else {
         text = text.replace("$(_NAME)", fProvider.getName());
      }
      Variable var = fProvider.safeGetVariable("structName");
      if (var != null) {
         text = text.replace("$(_Structname)", makePrettyName(var.getValueAsString()));
         text = text.replace("$(_STRUCTNAME)", var.getValueAsString());
      }
      var = fProvider.safeGetVariable("_instanceList");
      if (var != null) {
         text = text.replace("$(_instanceList)", var.getValueAsString());
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
      String attribute = element.getAttribute(attrName);
//      if (attribute.contains("_Flash0K_eeprom32K")) {
//         System.err.println("Found it");
//      }
      try {
         attribute = fForStack.doForSubstitutions(attribute);
         attribute = replaceCommonNames(attribute).trim();

         if (attribute.startsWith("\\=")) {
            return attribute.substring(2);
         }
         Object res = attribute;
         if (attribute.startsWith("=")) {
            res = Expression.getValue(attribute.substring(1), fProvider);
         }
         return res;
      } catch (Exception e) {
         String message = "Failed to get attribute '"+attrName+"'";
         throw new Exception(message, e);
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
    *  <li>If the attribute starts with '=' it is evaluated as an immediate expression
    *      otherwise it is returned as a string
    * 
    * @param element       Element to obtain attribute from
    * @param attrName      Name of attribute
    * 
    * @return  Attribute as Object or null if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed
    */
   Object getAttribute(Element element, String attrName) throws Exception {

      return getAttribute(element, attrName, null);
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

      Object res = getAttribute(element, attrName);
      if (res == null) {
         return defaultValue;
      }
      if (res instanceof String) {
         return (String) res;
      }
      if (res instanceof Object[]) {
         Object[] resArray = (Object[]) res;
         StringBuilder sb = new StringBuilder();
         boolean needComma = false;
         for (Object el:resArray) {
            if (needComma) {
               sb.append(",");
            }
            needComma = true;
            sb.append((String)el);
         }
         return sb.toString();
      }
      System.err.println("Warning string expected for '" + element.getAttribute(attrName) + "' => " + res);
      return res.toString().trim();
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
      return getAttributeAsString(element, attrName, null);
   }

   /**
    * Get an attribute as Boolean after applying usual substitutions see {@link #getAttribute(Element, String)}.
    * If the attribute is not present then the default parameter is returned.
    * 
    * @param element       Element to examine
    * @param attrName      Name of attribute to retrieve
    * @param defaultValue  Value to return if attribute is not present
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
    * @param element       Element to examine
    * @param attrName      Name of attribute to retrieve
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
         res = Expression.getValue(res.toString(), fProvider);
      }
      try {
         return Long.valueOf(res.toString());
      } catch (NumberFormatException e) {
         e.printStackTrace();
         return Long.valueOf(0);
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
    * @param element       Element to examine
    * @param attrName      Name of attribute to retrieve
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
    * @param element       Element to examine
    * @param attrName      Name of attribute to retrieve
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
    * @param element       Element to examine
    * @param attrName      Name of attribute to retrieve
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
    * Get value of attribute as an immediately evaluated expression yielding a Boolean<br>
    * This constructs an expression from the attribute and immediately evaluates it.<br>
    * Variable values are <b>not used</b> unless prefixed with <b>@</b>.<br>
    * If a variable exists then it evaluates as <b>true</b>, otherwise <b>false</b>.<br>
    * An empty expression evaluates as <b>true</b>;
    * 
    * @param element       Element to examine
    * @param attrName      Name of attribute to retrieve
    * @param defaultValue  Value to return if attribute is not present
    * 
    * @return Evaluated expression as boolean or null if the attribute doesn't exist.
    * 
    * @throws Exception
    */
   Boolean getAttributeAsImmediateBoolean(Element element, String attrName, Boolean defaultValue) throws Exception {

      Object attr = getAttribute(element, attrName);
      if (attr==null) {
         return defaultValue;
      }
      if (attr instanceof Boolean) {
         // Already immediately evaluated expression
         return (Boolean)attr;
      }
      if (attr instanceof String) {
         return Expression.checkCondition(attr.toString(), fProvider);
      }
      String exp = element.getAttribute(attrName);
      if (attr instanceof Object[]) {
         Object[] obj = (Object[])attr;
         throw new Exception("Expected boolean value for immediate expression. '"+exp+"' evaluated to '"+Arrays.toString(obj)+"'");
      }
      throw new Exception("Expected boolean value for immediate expression. '"+exp+"' evaluated to '"+attr.toString()+"'");
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
      if ("*".equalsIgnoreCase(key)) {
         key = "_Unique"+ uniqueNameCounter++;
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
      if ("*".equalsIgnoreCase(name)) {
         name = "_Unique"+ uniqueNameCounter++;
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
      model.setToolTip(getToolTip(varElement));
      model.setSimpleDescription(getAttributeAsString(varElement, "description", null));
      model.setHiddenBy(getAttributeAsString(varElement, "hiddenBy", null), fProvider);
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
   /**
    * Parse &lt;StringOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception
    */
   private void parsePrintVar(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
      String key = getKeyAttribute(varElement);
      Variable var = fProvider.safeGetVariable(key);
      System.err.print(String.format("%-40s", "<printVar> key ='"+key+"', "));
      if (var == null) {
         System.err.println("Not found");
      }
      else {
         System.err.print("value = ");
         String split = getAttributeAsString(varElement, "split");
         String value = var.getValueAsString().replace("\\n", "\n").replace("\\t", "   ");
         if (split != null) {
            String[] ar = value.split(split);
            System.err.println();
            for (String s:ar) {
               System.out.println(s);
            }
            System.err.println();
         }
         else {
            if (value.length()>30) {
               System.err.println();
            }
            else {
               System.err.print("'");
            }
            System.err.print(value);
            if (value.length()>30) {
               System.err.println();
            }
            else {
               System.err.println("'");
            }
         }
      }
   }
   
   private void parseCategory(BaseModel parent, Element varElement) throws Exception {

      CategoryModel model = new CategoryModel(parent, getAttributeAsString(varElement, "name"));
      boolean hidden = getAttributeAsBoolean(varElement, "hidden", false);
      if (hidden) {
         // Permanently hide by removing from tree
         parent.removeChild(model);
      }
      model.setToolTip(getToolTip(varElement));
      model.setSimpleDescription(getAttributeAsString(varElement, "description"));
      parseChildModels(model, varElement);
      if ((model.getChildren()==null)||(model.getChildren().size()==0)) {
         // Empty category - discard
         parent.removeChild(model);
         return;
      }
      else {
         boolean allHidden = true;
         for (BaseModel m : model.getChildren()) {
            if (!m.isHidden()) {
               allHidden = false;
               break;
            }
         }
         model.setHidden(allHidden);
      }
   }

   private void parseCategoryOption(BaseModel parent, Element varElement) throws Exception {

      CategoryVariable      categoryVariable = (CategoryVariable)      createVariable(varElement, CategoryVariable.class);
      CategoryVariableModel categoryModel    = (CategoryVariableModel) parseCommonAttributes(parent, varElement, categoryVariable);

      categoryVariable.setValue(getAttributeAsString(varElement, "value"));
      categoryVariable.setLocked(getAttributeAsBoolean(varElement, "locked", true));
      //      categoryVariable.setHiddenBy(getAttributeAsString(varElement, "hiddenBy"));
      parseChildModels(categoryModel, varElement);

      if ((categoryModel.getChildren() == null) || (categoryModel.getChildren().size() == 0)) {
         // Empty category - discard
         parent.removeChild(categoryModel);
         return;
      }
   }

   private void parseAliasCategoryOption(BaseModel parent, Element varElement) throws Exception {
      BaseModel model = parseAliasOption(parent, varElement);
      if (model == null) {
         // Disabled
         return;
      }
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
    * Parse &lt;pinMapOption&gt; element<br>
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
   private BaseModel parseAliasOption(BaseModel parent, Element stringElement) throws Exception {
      if (!checkCondition(stringElement)) {
         return null;
      }

      String  key          = getKeyAttribute(stringElement);
      String  name         = getAttributeAsString(stringElement, "name");
      String  description  = getAttributeAsString(stringElement, "description");
      String  toolTip      = getToolTip(stringElement);

      if (key.isEmpty()) {
         throw new Exception("Alias requires key "+name);
      }
      
      boolean isConstant  = Boolean.valueOf(getAttributeAsString(stringElement, "locked",   "true"));
      String  optional    = getAttributeAsString(stringElement, "optional", "required");

      boolean isOptional         = "true".equalsIgnoreCase(optional);
      boolean discardImmediately = "discard".equalsIgnoreCase(optional);
      
      Variable var = safeGetVariable(key);
      if ((var == null) && discardImmediately) {
         return null;
      }
      
      AliasPlaceholderModel placeholderModel = new AliasPlaceholderModel(parent, name, description);
      placeholderModel.setkey(key);
      placeholderModel.setLocked(isConstant);
      placeholderModel.setOptional(isOptional);
      placeholderModel.setToolTip(toolTip);
      
      if (var != null) {
         
         // Immediately instantiate model for referenced variable
         BaseModel newModel = placeholderModel.createModelFromAlias(fProvider);
         
         // Replace alias with new model
         parent.replaceChild(placeholderModel, newModel);
         return newModel;
      }
      
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
    * Parse an variable &lt;variable key="name" expression="=1+12" type="Boolean" /&gt;
    * 
    * @param element
    * @throws Exception
    */
   private void parseVariable(Element element) throws Exception {

      if (!checkCondition(element)) {
         return;
      }
      String key         = getKeyAttribute(element);
      String name        = getAttributeAsString(element, "name");
      String type        = getAttributeAsString(element, "type", "Boolean");
      String expression  = getAttributeAsString(element, "expression");
      
      Variable var = Variable.createVariableWithNamedType(name, key, type+"Variable", 0);
      fProvider.addVariable(var);
      
      var.setReference(expression);
      var.setDerived(true);
      var.setLocked(true);
      
   }

   /**
    * @param parentModel
    * @param element
    * @throws Exception
    */
   private void parseConstant(Element element) throws Exception {

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
      //      if (key.equalsIgnoreCase("/FTM2/faultPinList")) {
      //         System.err.println("Found it "+key);
      //      }

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
         String indexedKey  = key;
         String indexedName = name;
         if (results.length>1) {
            indexedKey  = key+"["+index+"]";
            indexedName = name+"["+index+"]";
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
                  var.setName(indexedName);
               }
               if (element.hasAttribute("description")) {
                  var.setDescription(description);
               }
               return;
            }
            else {
               throw new Exception("Constant multiply defined, name="+indexedName+", key=" + indexedKey);
            }
         }
         else {
            if ("Integer".equalsIgnoreCase(type)) {
               System.err.println("Warning: Old style 'Integer' type for '" + key + "'");
               type = "Long";
            }
            var = Variable.createVariableWithNamedType(indexedName, indexedKey, type+"Variable", results[index]);
            var.setDescription(description);
            var.setHidden(isHidden);
            var.setDerived(true);
            var.setConstant();
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

//      if (controlVar.getName().contains("comswap")) {
//         System.err.println("Found it '"+controlVar+"'");
//      }
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
    * <ul>
    * <li>%baseType[index]                Underlying type for enum

    * <li>%configFieldAssignment          Expression of form '%register     <= (%register & ~%mask)|%registerName
    * <li>%configRegAssignment            Expression of form '%register     <= %registerName
    * <li>%constructorFieldAssignment     Expression of form '%registerName <= (%registerName & ~%mask)|%paramExpression
    * <li>%constructorRegAssignment       Expression of form '%registerName <= %paramExpression

    * <li>%defaultValue[index]            Default value of variable
    * <li>%description[index]             Description from variable e.g. Compare Function Enable

    * <li>%fieldExtract                   Expression of form '(%register & %mask)
    * <li>%fieldAssignment                Expression of form '%register     <= (%register & ~%mask)|%paramExpression

    * <li>%initExpression                 Based on variables etc. Similar to (%register&%mask)
    
    * <li>%macro[index]                   From &lt;mask&gt; or deduced from &lt;controlVarName&gt; e.g. "SIM_SOPT_REG"
    * <li>%mask[index]                    From &lt;mask&gt; or deduced from &lt;controlVarName&gt; e.g. "SIM_SOPT_REG_MASK" (_MASK is added)
    * <li>%maskingExpression              Based on variable etc. Similar to (%register&%mask)
    * <li>%multilineDescription           Brief description of all variables

    * <li>%param[index]                   Formatted parameter for function
    * <li>%paramDescription[index]        Tool-tip from controlVar formatted as param description @param ...
    * <li>%paramExpression                Parameters ORed together e.g. adcPretrigger|adcRefSel
    * <li>%paramName[index]               Based on typeName with lower-case first letter adcCompare
    * <li>%params                         Formatted parameter list for function
    * <li>%paramType[index]               Based on typeName e.g. AdcCompare (or uint32_t)

    * <li>%regAssignment                  Expression of form '%register     <= %paramExpression
    * <li>%register[index]                Register associated with variable e.g. adc->APCTL1
    * <li>%registerName[index]            Name of corresponding register (lower-case for Init()) e.g. apctl1
    * <li>%registerNAME[index]            Name of corresponding register (upper-case for Init()) e.g. APCTL1
    * <li>%returnType[index]              Based on typeName e.g. AdcCompare (or uint32_t) (references and const stripped)

    * <li>%shortDescription[index]        Short description from controlVar e.g. Compare Function Enable
    * <li>%symbolicExpression[index]      Symbolic formatted value e.g. AdcCompare_Disabled

    * <li>%tooltip[index]                 Tool-tip from controlVar e.g. Each bit disables the GPIO function

    * <li>%valueExpression                Numeric variable value e.g. 0x3
    * <li>%variable[index]                Variable name e.g. /ADC0/adc_sc2_acfe
    * </ul>
    * 
    * @param element                 Element
    * @param variableAttributeName   Control var to obtain information from
    * 
    * @return  List of substitutions or null if variableAttributeName==null or no corresponding attribute found
    * 
    * @throws  Exception
    */
   List<StringPair> getTemplateSubstitutions(Element element, String variableAttributeName) throws Exception {

      boolean useDefinitions          = Boolean.valueOf(getAttributeAsString(element, "useDefinitions", "false"));
      boolean multipleParamsOnNewline = Boolean.valueOf(getAttributeAsString(element, "multipleParamsOnNewline", "true"));
      
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

      temp = getAttributeAsString(element, "paramTypes");
      List<String> paramTypesOverride;
      if (temp != null) {
         String[] par = temp.split(",");
         for (int index=0; index<par.length; index++) {
            par[index] = par[index].trim();
         }
         paramTypesOverride = new ArrayList<String>(Arrays.asList(par));
      }
      else {
         paramTypesOverride = new ArrayList<String>();
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

      // List of variables to process - may be trimmed if variable doesn't exist
      ArrayList<Variable> variableList = null;
      
      // Number of non-default params - may be trimmed if some variables don't exist
      Long numberOfNonDefaultParams = getLongAttribute(element, "nonDefaultParams", 1);
      
      // Check for variable list to process
      String variablesAttribute = null;
      if (variableAttributeName != null) {
         variablesAttribute = getAttributeAsString(element, variableAttributeName);
      }
      
      if (variablesAttribute != null) {
         
         if (variablesAttribute.isEmpty()) {
            String text = getText(element);
            throw new Exception("Empty '"+variableAttributeName+"' attribute in template\n"+text);
         }
         // Create variable list
         String varNames[] = variablesAttribute.split(",");
   
         // List of variables to actually process
         variableList  = new ArrayList<Variable>();
         
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
            if (paramTypesOverride.size() > index) {
               paramTypesOverride.remove(index);
            }
         }
         if (variableList.isEmpty()) {
            // No requested variables exist - don't generate method at all
            return null;
         }
      }
      
      ArrayList<StringPair> substitutions = new ArrayList<StringPair>();
      
      boolean allowEmptyParameterList = false;
      
      if (variableList != null) {
         
         // Process variables found
         StringBuilder maskSb               = new StringBuilder();  // Combined mask e.g. MASK1|MASK2
         StringBuilder valueExpressionSb    = new StringBuilder();  // Combined values $(var1)|$(var2)
         StringBuilder symbolicExpressionSb = new StringBuilder();  // Combined values $(var1.enum[])|$(var2.enum[])
         StringBuilder initExpressionSb     = new StringBuilder();  // Combined values $(var1.enum[])|, // comment ...
         StringBuilder paramExprSb          = new StringBuilder();  // Combined expression param1|param2
         StringBuilder paramsSb             = new StringBuilder();  // Parameter list with defaults etc.
         StringBuilder paramDescriptionSb   = new StringBuilder();  // @param style comments for parameters
         StringBuilder descriptionSb        = new StringBuilder();  // @param style comments for parameters

         // Accumulates the description for all parameters as block comment
         StringBuilder multilineDescription = new StringBuilder();

         boolean       parmsOnNewLine       = false;                 // Indicates there are multiple parameters
          
         // Padding applied to comments (before * @param)
         String linePadding    = getAttributeAsString(element, "linePadding",    "").replace("x", " ");
         String tooltipPadding = getAttributeAsString(element, "tooltipPadding", "x*xxxxxxxx").replace("x", " ");

         // Terminator for initExpression
         String terminator     = getAttributeAsString(element, "terminator"    , ";");

         // Separator for initExpression
         String separator     = getAttributeAsString(element, "separator"    , "|");

         // No newline before initExpression (suitable for a single initialisation value)
         boolean initExpressionOnSameLine = getAttributeAsBoolean(element, "initExpressionOnSameLine", false);

         if (variableAttributeName == null) {
            // Returns empty list to indicate template should still be processed
            return new ArrayList<StringPair>();
         }

         // Find maximum name length
         int maxTypeNameLength = 4;
         for (int index=0; index<variableList.size(); index++) {
            if ((paramOverride.size()>index) && (paramOverride.get(index).equals("*"))) {
               continue;
            }
            String typeName = variableList.get(index).getTypeName();
            if (typeName == null) {
               continue;
            }
            maxTypeNameLength = Math.max(maxTypeNameLength, typeName.length());
         }
         String  register           = null;
         String  registerName       = null;
         boolean registeNameChanged = false;

         // Padding applied to parameters
         String paramPadding = (variableList.size()<=1)?"":"\\t      "+linePadding;
         

         // To differentiate 'nameless' params
         int valueSuffix = 0;
         for (int index=0; index<variableList.size(); index++) {

            Variable variable    = variableList.get(index);
            String   variableKey = variable.getKey();
            
            if (index > 0) {
               valueExpressionSb.append(separator);
               symbolicExpressionSb.append(separator);
               initExpressionSb.append("\n");
            }

            // Mask created from variable name e.g. MACRO_MASK or deduced from valueFormat attribute
            String mask;
            String macro;

            // Value format string
            String valueFormat  = variable.getValueFormat();

            if (valueFormat != null) {
               
               String[] formats = valueFormat.split(",");
               StringBuilder sb = new StringBuilder();
               boolean multipleElements = false;
               for (String format:formats) {
                  /*
                   * (%s)                 => ""
                   * (%s),xxx(%s)         => xxx_MASK
                   * (%s),xxx(%s),yyy(%s) => xxx_MASK|yyy_MASK
                   * 
                   */
                  format = format.trim();
                  if (format.matches("^\\(?\\%s\\)?$")) {
                     // Discard non=macro values
                     continue;
                  }
                  if (format.isBlank()) {
                     continue;
                  }
                  format = format.replace("(%s)", "_MASK");
                  if (!sb.isEmpty()) {
                     sb.append("|");
                     multipleElements = true;
                  }
                  sb.append(format);
               }
               if (sb.isEmpty()) {
                  mask  = "";
                  macro = "";
               }
               else if (multipleElements) {
                  mask  = sb.toString();
                  macro = "";
               }
               else {
                  mask  = sb.toString();
                  macro = mask.replace("_MASK", "");
               }
            }
            else {
               macro = Variable.getBaseNameFromKey(variableKey).toUpperCase();
               mask  = macro+"_MASK";
            }
            
            if (!mask.isBlank()) {
               
               if (maskSb.length()>0) {
                  maskSb.append('|');
               }
               maskSb.append(mask);
               
               boolean bracketsRequired = !mask.matches("[a-zA-Z0-9_]*");
               if (bracketsRequired) {
                  mask = '('+mask+')';
               }
            }

            String baseType = "'%baseType' is not valid here";
            
            String baseTypeValue = variable.getBaseType();
            String typeNameValue = variable.getTypeName();
            if (baseTypeValue != null) {
               baseType = baseTypeValue;
            }
            else if (typeNameValue != null) {
               baseType = typeNameValue;
            }
            
            // Type from variable with upper-case 1st letter
            String paramName  = variable.getParamName();
            String paramType  = variable.getParamType();
            // Check for given value
            String returnType = getAttributeAsString(element, "returnType", null);
            if (returnType == null ) {
               // Get return type from variable
               returnType = variable.getReturnType();
            }
//            if (variable.isLogging() && (returnType.equals("null"))) {
//               System.err.println("Found it null " + variable);
//            }
//            if (returnType.equals("null" )) {
//               System.err.println("Found it null " + variable);
//            }
//            if (paramName.substring(0,1).matches("[A-Z]")) {
//               System.err.println("Found it, paramName=" + paramName +", var ="+variable);
//            }
//            if (paramType.substring(0,1).matches("[a-z]") &&
//               !paramType.startsWith("const") &&
//               !Variable.isIntegerTypeInC(paramType)) {
//               System.err.println("Found it, paramType= " + paramType +", var ="+variable);
//            }
            
            if (Variable.isIntegerTypeInC(paramType)) {
               // Integer parameters get a name of 'value' by default
               paramName = "value";
               if (valueSuffix != 0) {
                  paramName = paramName+valueSuffix;
               }
               valueSuffix++;
            }
            
            if ((paramOverride.size()>index) && !paramOverride.get(index).isBlank()) {
               if (paramOverride.get(index).equals("*")) {
                  // Exclude variable from parameter list
                  paramName               = null;
                  allowEmptyParameterList = true;
               }
               else {
                  paramName = paramOverride.get(index);
               }
            }

            if ((paramTypesOverride.size()>index) && !paramTypesOverride.get(index).isBlank()) {
               paramType = paramTypesOverride.get(index);
            }

            // $(variableKey)
            String valueExpression = "$("+variableKey+")";
            valueExpressionSb.append(valueExpression);

            String symbolicExpression;
            if (useDefinitions) {
               symbolicExpression = "$("+variableKey+".definition)";
            }
            else {
               symbolicExpression = "$("+variableKey+".usageValue)";
            }
            symbolicExpressionSb.append(symbolicExpression);

            // Description from variable
            String description = "'%description' not available in this template";
            temp = variable.getDescription();
            if (temp != null) {
               description = temp;
               if (!descriptionSb.isEmpty()) {
                  if ((index+1)==variableList.size()) {
                     descriptionSb.append(" and ");
                  }
                  else {
                     descriptionSb.append(", ");
                  }
               }
               descriptionSb.append(description);
            }
            if (temp == null) {
               System.err.println("Warning: no description for '"+variable.getName()+"'");
            }

            // Short description from variable
            String shortDescription = "'%shortDescription' not available in this template";
            temp = variable.getShortDescription();
            if (temp != null) {
               shortDescription = temp;
            }
            String pad = "\\t   // ";
            if (!multilineDescription.isEmpty()) {
               multilineDescription.append("\\n");
               pad = "\\t"+linePadding+"// ";
            }
            multilineDescription.append(pad + shortDescription);
            multilineDescription.append(" ("+variable.getName()+")");
            
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

            initExpressionSb.append(symbolicExpression);
            if (index+1 == variableList.size()) {
               initExpressionSb.append(terminator+"  // ");
            }
            else {
               initExpressionSb.append(" "+separator+" // ");
            }
            
            initExpressionSb.append("("+variable.getName()+") ");
            
            initExpressionSb.append("$("+variableKey+".shortDescription)");
            if (variable instanceof VariableWithChoices) {
               initExpressionSb.append(" - $("+variableKey+".name[])");
            }

            String defaultParamV = variable.getDefaultParameterValue();
            if ((defaultValueOverride.size()>index) && !defaultValueOverride.get(index).isBlank()) {
               defaultParamV = defaultValueOverride.get(index);
            }
            if (paramName != null) {
               if (paramExprSb.length()>0) {
                  paramExprSb.append(separator);
               }
               paramExprSb.append(variable.formatValueForRegister(paramName));
            }

//            if (variable.getName().contains("nvic_irqLevel")) {
//               System.err.println("Found it, " + variable);
//            }
            String defaultValue = "%defaultValue"+index+" not available";
            if (defaultParamV != null) {
               defaultValue = defaultParamV;
            }
            String paramDescriptionN = "%paramDescription not available";
            if (paramName != null) {
               paramDescriptionN = String.format("\\t"+linePadding+" * @param %"+(-maxTypeNameLength)+"s %s", paramName, tooltip);
               if (paramDescriptionSb.length()>0) {
                  paramDescriptionSb.append("\n");
               }
               paramDescriptionSb.append(paramDescriptionN);
            }
            
            String param = "%param"+index+" not available";
            if (paramName != null) {
               if (index<numberOfNonDefaultParams) {
                  param = String.format("%"+(-maxTypeNameLength)+"s %s", paramType, paramName);
               }
               else {
                  param = String.format("%"+(-maxTypeNameLength)+"s %"+(-maxTypeNameLength)+"s = %s", paramType, paramName, defaultParamV);
               }
               if (paramsSb.length()>0) {
                  paramsSb.append(",");
                  
                  // Indicates newline is needed as newline on multi-params was requested
                  parmsOnNewLine = multipleParamsOnNewline;
                  
                  if (multipleParamsOnNewline) {
                     paramsSb.append("\n"+paramPadding);
                  }
               }
               paramsSb.append(param);
            }
            String registerN     = "'register' is not valid here";
            String registerNameN = "'registerName' is not valid here";
            String registerNAMEN = "'registerNAME' is not valid here";

            // Try to deduce register
            temp = deduceRegister(variable);
            if (temp != null) {
               registerN     = temp;
               registerNameN = temp.replaceAll("([a-zA-Z0-9]*)->", "").toLowerCase();
               registerNAMEN = registerNameN.toUpperCase();
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
            if (paramName == null) {
               paramName = "%paramName"+index+" not available";
            }

            substitutions.add(0, new StringPair("%baseType"+index,                baseType));
            substitutions.add(0, new StringPair("%defaultValue"+index,            defaultValue));
            substitutions.add(0, new StringPair("%description"+index,             description));
            substitutions.add(0, new StringPair("%macro"+index,                   macro));
            substitutions.add(0, new StringPair("%mask"+index,                    mask));
            substitutions.add(0, new StringPair("%paramDescription"+index,        paramDescriptionN));
            substitutions.add(0, new StringPair("%paramName"+index,               paramName));
            substitutions.add(0, new StringPair("%paramType"+index,               paramType));
            substitutions.add(0, new StringPair("%param"+index,                   param));
            substitutions.add(0, new StringPair("%registerName"+index,            registerNameN));
            substitutions.add(0, new StringPair("%registerNAME"+index,            registerNAMEN));
            substitutions.add(0, new StringPair("%register"+index,                registerN));
            substitutions.add(0, new StringPair("%returnType"+index,              returnType));
            substitutions.add(0, new StringPair("%shortDescription"+index,        shortDescription));
            substitutions.add(0, new StringPair("%symbolicExpression"+index,      symbolicExpression));
            substitutions.add(0, new StringPair("%tooltip"+index,                 tooltip));
            substitutions.add(0, new StringPair("%valueExpression"+index,         valueExpression));
            substitutions.add(0, new StringPair("%variable"+index,                variableKey));
            
            if (index == 0) {
               substitutions.add(new StringPair("%baseType",                baseType));
               substitutions.add(new StringPair("%defaultValue",            defaultValue));
               substitutions.add(new StringPair("%paramName",               paramName));
               substitutions.add(new StringPair("%paramType",               paramType));
               substitutions.add(new StringPair("%returnType",              returnType));
               substitutions.add(new StringPair("%registerName",            registerNameN));
               substitutions.add(new StringPair("%registerNAME",            registerNAMEN));
               substitutions.add(new StringPair("%shortDescription",        shortDescription));
               substitutions.add(new StringPair("%tooltip",                 tooltip));
               substitutions.add(new StringPair("%variable",                variableKey));
            }
         }
         substitutions.add(new StringPair("%multilineDescription",             multilineDescription.toString()));

         String mask = null;
         if (maskSb.length() > 0) {
            mask = maskSb.toString();
            // If not a simple name/number add brackets
            boolean bracketsRequired = !mask.matches("[a-zA-Z0-9_]*");
            if (bracketsRequired) {
               mask = '('+mask+')';
            }
         }
         String paramExpr = "'paramExpr' is not valid here";
         if (paramExprSb.length()>0) {
            paramExpr = paramExprSb.toString();
         }

         String maskingExpression            = "'maskingExpression' is not valid here";
         String fieldExtract                 = "'fieldExtract' is not valid here";
         String fieldAssignment              = "'fieldAssignment' is not valid here";
         String constructorFieldAssignment   = "'constructorFieldAssignment' is not valid here";
         String configFieldAssignment        = "'constructorFieldAssignment' is not valid here";
         String regAssignment                = "'regAssignment' is not valid here";
         String constructorRegAssignment     = "'constructorRegAssignment' is not valid here";
         String configRegAssignment          = "'constructorRegAssignment' is not valid here";
         if (register != null) {
            if (variableList.size()==1) {
               // LongVariable   => ((SIM_SCG_DEL_MASK&<b>registerValue</b>)>>SIM_SCG_DEL_SHIFT)
               // ChoiceVariable => (SIM_SCG_DEL_MASK&<b>registerValue</b>)
               fieldExtract       = variableList.get(0).fieldExtractFromRegister(register);
            }
            if (mask != null) {
               maskingExpression = register+"&"+mask;
               
               //  %register = (%register&~%mask) | %paramExpression;
               fieldAssignment    = register+" = "+"("+register+"&~"+mask+")"+" | "+paramExpr;
               
               //  %registerName = (%registerName&~%mask) | %paramExpression;
               constructorFieldAssignment = registerName+" = ("+registerName+"&~"+mask+") | "+paramExpr;
               
               //  %register = (%register&~%mask) | %registerName;
               configFieldAssignment = register+" = ("+register+"&~"+mask+") | "+"init."+registerName;
               
               //  %register = %paramExpression;
               regAssignment       = register+" = "+paramExpr;
               
               //  %registerName =  %paramExpression;
               constructorRegAssignment = registerName+" = "+paramExpr;
               
               //  %register =  %registerName;
               configRegAssignment = register+" = "+"init."+registerName;
            }
            else {
               
               //  %register = %paramExpression;
               fieldAssignment       = register+" = "+paramExpr;
               
               //  %registerName = %paramExpression;
               constructorFieldAssignment = registerName+" = "+paramExpr;
               
               //  %registerName = %paramExpression;
               configFieldAssignment = register+" = "+"init."+registerName;
               
               //  %register = %paramExpression;
               regAssignment       = register+" = "+paramExpr;
               
               //  %registerName = %paramExpression;
               constructorRegAssignment = registerName+" = "+paramExpr;
               
               //  %registerName = %paramExpression;
               configRegAssignment = register+" = "+"init."+registerName;
            }
         }
         if (register == null) {
            register     = "'%register' is not valid here";
            registerName = "'%registerName' is not valid here";
         }
         if (mask == null) {
            mask = "'%mask' not available in this template";
         }
         String params = "'%params' is not valid here";
         if (paramsSb.length()>0) {
            if (parmsOnNewLine) {
               paramsSb.insert(0,"\n"+paramPadding);
            }
            params = paramsSb.toString();
         }
         else if (allowEmptyParameterList) {
            params="";
         }
         String paramDescription = "'%comments' is not valid here";
         if (paramDescriptionSb.length()>0) {
            paramDescription = paramDescriptionSb.toString();
         }
         else if (allowEmptyParameterList) {
            paramDescription = "";
         }

         String initExpression = "'%initExpression' is not valid here";
         if (initExpressionSb.length()>0) {
            initExpression = initExpressionSb.toString();
         }

         String description = "'%description' is not valid here";
         if (descriptionSb.length()>0) {
            description = descriptionSb.toString();
         }
         substitutions.add(new StringPair("%configFieldAssignment",      configFieldAssignment));
         substitutions.add(new StringPair("%configRegAssignment",        configRegAssignment));
         substitutions.add(new StringPair("%constructorFieldAssignment", constructorFieldAssignment));
         substitutions.add(new StringPair("%constructorRegAssignment",   constructorRegAssignment));
         
         substitutions.add(new StringPair("%fieldExtract",               fieldExtract));
         substitutions.add(new StringPair("%fieldAssignment",            fieldAssignment));
         
         substitutions.add(new StringPair("%description",                description));
         
         substitutions.add(new StringPair("%initExpression",             initExpression));
         substitutions.add(new StringPair("%maskingExpression",          maskingExpression));
         substitutions.add(new StringPair("%mask",                       mask));
         substitutions.add(new StringPair("%paramDescription",           paramDescription));
         substitutions.add(new StringPair("%paramExpression",            paramExpr));
         substitutions.add(new StringPair("%params",                     params));
         
         substitutions.add(new StringPair("%registerName",               registerName));
         substitutions.add(new StringPair("%regAssignment",              regAssignment));
         substitutions.add(new StringPair("%register",                   register));
         
         substitutions.add(new StringPair("%symbolicExpression",         symbolicExpressionSb.toString()));
         
         substitutions.add(new StringPair("%valueExpression",            valueExpressionSb.toString()));
      }
      
      String immediateVariables  = getAttributeAsString(element, "immediateVariables");
      
      if (immediateVariables != null) {
         for (String immediateVariable:immediateVariables.split(",")) {
            Variable var = safeGetVariable(immediateVariable);
            if (var == null) {
               throw new Exception("Variable not found for immediateVariables, var='" + immediateVariable + "'");
            }
            String from = "$("+immediateVariable+")";
            String to   = var.getSubstitutionValue();
            substitutions.add(new StringPair(from , to));
         }
      }
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
   private boolean checkTemplateConditions(Element element, String discardRepeats) throws Exception {
      if (!checkCondition(element)) {
         return false;
      }
      if (discardRepeats.equalsIgnoreCase("false")) {
         return true;
      }

      // Try using discardRepeats attribute as key for discarding repeats
      String repeatKey = discardRepeats;

      if (repeatKey.equalsIgnoreCase("true")) {
         // If 'true', get the key from the element key
         repeatKey = getKeyAttribute(element);
      }
      if (repeatKey==null) {
         // Assume associated with Base class
         repeatKey = "_"+fPeripheral.getBaseName();
      }
      repeatKey = "$TEMPLATE_"+repeatKey;

//      String structName = fProvider.getVariable("structName").getValueAsString();
//      if (structName.contains("FTM")) {
//         System.err.println("Found it "+structName);
//      }

      // Already blocked?
      if (fPeripheral.getDeviceInfo().checkIfRepeatedItem(repeatKey)) {
//         if (structName.contains("FTM")) {
//            System.err.println("Found it Blocking "+structName+", key ="+repeatKey);
//         }
         // These are common!
         return false;
      }

      // Add to list for blocking AFTER parsing this file
      repeatedItems.add(repeatKey);
      return true;
   }

   /**
    * Expected attributes:<br>
    * <li>&lt;name&gt; - Used to place value at arbitrary location (creates variable of that name)
    * <li>&lt;namespace&gt; (info|usbdm|class|all) - Scope where template is available in
    * <li>&lt;&gt; - Name of existing <b>choice</b> or <b>binary</b> variable used as information source
    * <li>&lt;returnFormat&gt; - Used for writing return value e.g. String.format("XXXX(%s)", enumName)<br>
    * 
    * &lt;valueFormat&gt; default to upper-cased value based on &lt;&gt; if needed<br>
    * &lt;template&gt; and &lt;typeName&gt;+&lt;valueFormat&gt; are alternatives<br>
    * &lt;returnFormat&gt; is only required if %xxx is used<br><br>
    * 
    * Substitutions in Text :<br>
    * <li>%tooltip - From &lt;&gt; variable
    * <li>%description - From &lt;&gt; variable
    * <li>%typeName  - Based on typeName with upper-case first letter
    * <li>%mask - From &lt;mask&gt; or deduced from &lt;&gt;
    * <li>%maskingExpression - Based on  etc. Similar to (sim->SOPT2&%mask)
    * <li>%body - Constructed case clauses
    * 
    * @param element
    * @throws Exception
    */
   private void parseClockCodeTemplate(Element element) throws Exception {
      
      String discardRepeats   = getAttributeAsString(element, "discardRepeats", "false");
      String key              = getKeyAttribute(element);
      String namespace        = getAttributeAsString(element, "namespace", "info");
      String codeGenCondition = getAttributeAsString(element, "codeGenCondition");
      /*
      <!-- Where to place generated code -->
      <!ATTLIST clockCodeTemplate where                     (info|basicInfo|commonInfo)  #IMPLIED >
      
      info  - Within peripheral info class in pinmapping.h (default if not provided)<br>
      usbdm - Before peripheral class in pinmapping.h  (USBDM namespace)<br>
      all   - Explicitly available anywhere controlled by key substitution<br>
      forceInfo - forces Info namespace and clears key and discardRepeats
      baseClass - In base class for peripheral (causes discard repeats using StructName as key)
      
      */
      String where = getAttributeAsString(element, "where", null); // (info|basicInfo|commonInfo)
      if (where != null) {
         if ((key != null) && !"all".equalsIgnoreCase(where)) {
            throw new Exception("Attribute 'key="+key+"' used with 'where='"+where+"' attribute (should be where='all')");
         }
         if (("usbdm".equals(where))) {
            // Before peripheral class in pinmapping.h
            // Usually enums etc.
            key            = null;
            namespace      = "usbdm"; // Before peripheral class in pinmapping.h
            // discardRepeats is individual ?
         }
         else if (("commonInfo".equals(where))) {
            // One for each type of peripheral
            // e.g. FtmCommonInfo  ($(_Baseclass)CommonInfo)
            key            = null;
            namespace      = "commonInfo"; // Within peripheral info class in pinmapping.h
            discardRepeats = fPeripheral.getBaseName();
         }
         else if (("basicInfo".equals(where))) {
            // e.g. FtmBasicInfo ($(_Structname)BasicInfo)
            // One for each variant (structure type) of a peripheral
            key            =  null;
            namespace      = "basicInfo"; // Before peripheral class in pinmapping.h
            discardRepeats = fProvider.getVariable("structName").getValueAsString();
         }
         else if (("info".equals(where))) {
            // e.g. Ftm0Info ($(_Class)Info)
            // One for each instance of a peripheral
            key            = null;     // No key
            namespace      = "info";   // Within peripheral info class in pinmapping.h
            discardRepeats = "false";  // Can't repeat anyway as unique to peripheral
         }
         else if (("all".equals(where))) {
            // Arbitrary location
            // Key necessary
            namespace      = "all"; // Arbitrary location
            // discardRepeats unmodified
         }
         else if (key != null) {
            namespace="all";
         }
      }
      
      if (key != null) {
         namespace = "all";
      }
      if (("baseClass".equals(namespace))) {
         key = null;
         namespace="usbdm";
         discardRepeats = fProvider.getVariable("structName").getValueAsString();
      }
      if (!checkTemplateConditions(element, discardRepeats)) {
         return;
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
         
      TemplateInformation templateInfo = addTemplate(key, namespace, codeGenCondition);
      
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

      String discardRepeats      = getAttributeAsString(element, "discardRepeats", "false");
      String key                 = getKeyAttribute(element);
      String namespace           = getAttributeAsString(element, "namespace", "info");
      String codeGenCondition    = getAttributeAsString(element, "codeGenCondition");
      
      /*
      <!-- Where to place generated code -->
      <!ATTLIST clockCodeTemplate where                     (info|basicInfo|commonInfo)  #IMPLIED >
      
      info  - Within peripheral info class in pinmapping.h (default if not provided)<br>
      usbdm - Before peripheral class in pinmapping.h  (USBDM namespace)<br>
      all   - Explicitly available anywhere controlled by key substitution<br>
      forceInfo - forces Info namespace and clears key and discardRepeats
      baseClass - In base class for peripheral (causes discard repeats using StructName as key)
      
      */
      String where = getAttributeAsString(element, "where", null); // (info|basicInfo|commonInfo)
      if (where != null) {
         if ((key != null) && !"all".equalsIgnoreCase(where)) {
            throw new Exception("Attribute 'key="+key+"' used with 'where='"+where+"' attribute (should be where='all')");
         }
         if ("usbdm".equals(where)) {
            // Before peripheral class in pinmapping.h
            // Usually enums etc.
            key            = null;
            namespace      = "usbdm"; // Before peripheral class in pinmapping.h
            // discardRepeats is individual ?
         }
         else if ("commonInfo".equals(where)) {
            // One for each type of peripheral
            // e.g. FtmCommonInfo  ($(_Baseclass)CommonInfo)
            key            = null;
            namespace      = "commonInfo"; // Within peripheral CommonInfo class in pinmapping.h
            discardRepeats = fPeripheral.getBaseName();
         }
         else if ("basicInfo".equals(where)) {
            // e.g. FtmBasicInfo ($(_Structname)BasicInfo)
            // One for each variant (structure type) of a peripheral
//            String structName = fProvider.getVariable("structName").getValueAsString();
//            if (structName.equals("FTM")) {
//               System.err.println("Found it "+structName);
//            }
            key            =  null;
            namespace      = "basicInfo"; // Before peripheral class in pinmapping.h
            discardRepeats = fProvider.getVariable("structName").getValueAsString()+"_BasicInfo";
         }
         else if ("info".equals(where)) {
            // e.g. Ftm0Info ($(_Class)Info)
            // One for each instance of a peripheral
            key            = null;     // No key
            namespace      = "info";   // Within peripheral Info class in pinmapping.h
            discardRepeats = "false";  // Can't repeat anyway as unique to peripheral
         }
         else if ("all".equals(where)) {
            // Arbitrary location
            // Key necessary
            if (key==null) {
               throw new Exception("Attribute 'key' missing when 'where=all' ");
            }
            namespace      = "all"; // Arbitrary location
            // discardRepeats unmodified
         }
         else {
            throw new Exception("Unexpected 'where' attribute value '" + where + "'");
         }
      }
      else {
         if (("forceInfo".equals(namespace))) {
            key = null;
            namespace="info";
            discardRepeats = "false";
         }
         if (("forceUsbdm".equals(namespace))) {
            key = null;
            namespace="usbdm";
         }
         if (("baseClass".equals(namespace))) {
            key = null;
            namespace="usbdm";
            discardRepeats = fProvider.getVariable("structName").getValueAsString();
         }
         if (key != null) {
            namespace="all";
         }
      }
      if (!checkTemplateConditions(element, discardRepeats)) {
         return;
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
      TemplateInformation templateInfo = addTemplate(key, namespace, codeGenCondition);

      String text = doTemplateSubstitutions(getText(element), substitutions);
      templateInfo.addText(text);
      //      for (Node node = element.getFirstChild();
      //            node != null;
      //            node = node.getNextSibling()) {
      //         if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
      //            String bodyText = getText(node);
      //            bodyText = doTemplateSubstitutions(bodyText, substitutions);
      //            templateInfo.addText(bodyText);
      //         }
      //      }
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
      String typeName = choiceVar.getTypeName();
      if ((typeName==null)||typeName.isBlank()) {
         return caseBody + "(No typeName)";
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
         enumNames[index]     = typeName+"_"+enumName;
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

   private void parseDeleteVariables(Element element) throws Exception {

      if (!checkCondition(element)) {
         return;
      }
      boolean mustExist = getAttributeAsBoolean(element, "mustExist", true);
      
      String variables = getAttributeAsString(element, "variables", null);
      if (variables == null) {
         throw new Exception("<deleteVariables> must have 'variables' attribute");
      }
      for (String variable:variables.split(",")) {
         
         fPeripheral.removeMonitoredVariable(safeGetVariable(variable));

         boolean wasDeleted = fProvider.removeVariableByName(variable);

         if (!wasDeleted) {
//            System.err.println("Variable '" + variable + "' was not found to delete in deleteVariables");
            if (mustExist) {
               throw new Exception("Variable '" + variable + "' was not found to delete in deleteVariables");
            }
         }
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
      // Check for IF before condition check
      if (tagName == "if") {
         parseIfThen(parentModel, element);
         return;
      }
      if (!checkCondition(element)) {
         return;
      }

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
      else if (tagName == "timeOption") {
         parseTimeOption(parentModel, element);
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
      else if (tagName == "printVar") {
         parsePrintVar(parentModel, element);
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
         parseConstant(element);
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
      else if (tagName == "section") {
         BaseModel model = new ParametersModel(parentModel, key, toolTip);
         parseChildModels(model, element);
      }
      else if (tagName == "list") {
         parseList(parentModel, element);
      }
      else if (tagName == "signals") {
         parseSignalsOption(parentModel, element);
      }
      else if (tagName == "validate") {
         parseValidate(element);
      }
      else if (tagName == "dynamicSignalMapping") {
         parseDynamicSignalMapping(element);
      }
      else if (tagName == "clockCodeTemplate") {
         parseClockCodeTemplate(element);
      }
      else if (tagName == "template") {
         parseTemplate(element);
      }
      else if (tagName == "variableTemplate") {
         if (!element.hasAttribute("variables")) {
            throw new Exception("<variableTemplate> must have 'variables' attribute, key='" + key + "'");
         }
         parseTemplate(element);
      }
      else if (tagName == "deleteVariables") {
         parseDeleteVariables(element);
      }
      else if (tagName == "equation") {
         parseEquation(element);
      }
      else if (tagName == "variable") {
         parseVariable(element);
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
         throw new Exception("Unexpected tag in parseChildModel(), \'"+tagName+"\'");
      }
   }

   /**
    * Parse an equation &lt;equation key="name" value="=1+12" /&gt;
    * 
    * @param element
    * @throws Exception
    */
   private void parseEquation(Element element) throws Exception {

      if (!checkCondition(element)) {
         return;
      }
      String key         = getKeyAttribute(element);
      Object expression  = getAttribute(element, "value");
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

   //   private void parseDialogue(BaseModel parentModel, Element element) throws Exception {
   //
   //      ClockSelectionFigure figure = new ClockSelectionFigure(fProvider, 0 /* getIntAttribute(element, "clockConfigIndex" */);
   //
   //      ButtonModel bm = new ButtonModel(parentModel, "A Button");
   //      return;
   //
   //      OpenGraphicModel model = new OpenGraphicModel(
   //            parentModel,
   //            getKeyAttribute(element),
   //            fProvider.safeGetVariable(getKeyAttribute(element, "var")),
   //            figure);
   //
   //      model.setToolTip(getToolTip(element));
   //      model.setSimpleDescription(getAttributeAsString(element, "description"));
   //
   //      for (Node node = element.getFirstChild();
   //            node != null;
   //            node = node.getNextSibling()) {
   //         if (node.getNodeType() != Node.ELEMENT_NODE) {
   //            continue;
   //         }
   //         Element boxElement = (Element) node;
   //         if (!checkCondition(boxElement)) {
   //            // Discard element
   //            continue;
   //         }
   //         String tagName = boxElement.getTagName();
   //         if (tagName == "graphicBox") {
   //            parseGraphicBoxOrGroup(parentModel, 0, 0, figure, boxElement);
   //            continue;
   //         }
   //         if (tagName == "graphicGroup") {
   //            parseGraphicBoxOrGroup(parentModel, 0, 0, figure, boxElement);
   //            continue;
   //         }
   //         if (tagName == "equation") {
   //            parseEquation(boxElement);
   //            continue;
   //         }
   //         if (tagName == "for") {
   //            GraphicWrapper graphicWrapper = new GraphicWrapper(this, 0, 0, figure);
   //            parseForLoop(parentModel, boxElement, graphicWrapper);
   //            continue;
   //         }
   //         throw new Exception("Expected tag = <graphicBox>, found = <"+tagName+">");
   //      }
   //   }


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

      model.setToolTip(getToolTip(element));
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

//      if (namespace.equalsIgnoreCase("baseClass")) {
//         namespace = "usbdm";
//      }
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
      Boolean locked    = getAttributeAsImmediateBoolean(element, "locked", false);

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
    * Evaluate as list of long values
    * 
    * @param value   Object. May be Long or Long[] or String
    * 
    * @return           Long[]
    * 
    * @throws Exception
    */
   Long[] convertToListOfLong(Object value) throws Exception {

      Long[] res = null;
      try {
         if (value instanceof String) {
            // Break up and convert to long
            String dim = (String) value;
            String dims[] = dim.split(",");
            res = new Long[dims.length];
            for (int index=0; index<dims.length; index++) {
               res[index] = Long.parseLong(dims[index]);
            }
         }
         else if (value instanceof Object[]) {
            // Each element should be Long
            Object[] dims = (Object[])value;
            res = new Long[dims.length];
            for (int index=0; index<dims.length; index++) {
               res[index] = (Long)dims[index];
            }
         }
         else if (value instanceof Long) {
            // Single value
            res = new Long[1];
            res[0] = (Long) value;
         }
         else {
            throw new Exception("Expected list of Long values found '"+ value + "'");
         }
      } catch (NumberFormatException e) {
         throw new Exception("Expected list of Long values found '"+ value + "'", e);
      }
      return res;
   }

   /**
    * Get attribute as a list of long values
    * 
    * @param element    Element to examine
    * @param name       Name of attribute to retrieve
    * 
    * @return           Long[] or null if missing
    * 
    * @throws Exception
    */
   Long[] getAttributeAsListOfLong(Element element, String name) throws Exception {

      Object value = getAttribute(element, name);
      if (value == null) {
         return null;
      }
      return convertToListOfLong(value);
   }

   /**
    * Get attribute as a list of long values
    * 
    * @param element    Element to examine
    * @param name       Name of attribute to retrieve
    * 
    * @return           Long[] or null if missing
    * 
    * @throws Exception
    */
   Integer[] getAttributeAsListOfInteger(Element element, String name) throws Exception {

      Long[] vals = getAttributeAsListOfLong(element, name);
      if (vals == null) {
         return null;
      }
      Integer[] res = new Integer[(vals.length)];
      for (int index=0; index<vals.length; index++) {
         res[index] = vals[index].intValue();
      }
      return res;
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

      for (Node subNode = menuElement.getFirstChild();
            subNode != null;
            subNode = subNode.getNextSibling()) {
         
         if (subNode.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) subNode;
         if (element.getTagName().equals("choice")) {
            if (!element.hasAttribute("name") || !element.hasAttribute("value")) {
               throw new Exception("<choice> must have name and value attributes "+element);
            }
            // Check if entry has condition to be available for choice to be present
            Boolean keepChoice = getAttributeAsImmediateBoolean(element, "condition", true);
            if (!keepChoice) {
               // Discard choice
               continue;
            }
            Boolean hidden = getAttributeAsImmediateBoolean(element, "hidden", false);
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
            entry.setToolTip(getToolTip(element));
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
               if (getAttributeAsBoolean(element, "isDefault", true)) {
                  // Explicit default set
                  if (defaultExplicitlySet) {
                     throw new Exception("Multiple default choices set in <"+menuElement.getTagName() + " name=\""+menuElement.getAttribute("name")+"\"> <choice name=\"" + getAttributeAsString(element, "name")+ "\">");
                  }
                  defaultExplicitlySet = true;
                  defaultValue = entries.size()-1;
               }
            }
         }
         else if (element.getTagName().equals("choiceExpansion")) {

            if (!checkCondition(element)) {
               continue;
            }
            if (!element.hasAttribute("value") || !element.hasAttribute("name")) {
               throw new Exception("<choiceExpansion> must have name and value attributes "+element);
            }
            if (!element.hasAttribute("keys") || !(element.hasAttribute("dim") || element.hasAttribute("values"))) {
               throw new Exception("<choiceExpansion> must have keys with either dim and values attributes "+element);
            }
            
            String keys       = getAttributeAsString(element, "keys");
            Object values     = getAttribute(element, "values");
            Integer[] dims    = getAttributeAsListOfInteger(element, "dim");
            String delimiter  = getAttributeAsString(element, "delimiter", ";");
            
            if (dims != null) {
               if (values != null) {
                  throw new Exception("Both values and dim attribute given in <choiceExpansion> '" + keys +"'");
               }
               
               // Iterated range used to create values
               int start;
               int end;
               if (dims.length == 1) {
                  start = 0;
                  end   = dims[0].intValue();
               }
               else if (dims.length == 2) {
                  start = dims[0];
                  end   = dims[1]+1;
               }
               else {
                  throw new Exception("Illegal dim value '"+dims+"' in <choiceExpansion> '"+keys+"'");
               }
               // Create values list
               StringBuilder sb = new StringBuilder();
               for (int index=start; index<end; index++) {
                  sb.append(index+delimiter);
               }
               values=sb.toString();
            }
            if ((values instanceof String) && ((String)values).isBlank()) {
               // Empty loop
               continue;
            }

            Integer index = 0;
            fForStack.createLevel(fProvider, keys, values, delimiter);
            do {
               String value  = getAttributeAsString(element, "value", "%(i)").replace("%(i)", index.toString());
               String name   = getAttributeAsString(element, "name",  "Choice %(i)").replace("%(i)", index.toString());
               String eNum   = getAttributeAsString(element, "enum",  "").replace("%(i)", index.toString());;

               ChoiceData entry = new ChoiceData(
                     name,
                     value,
                     eNum,
                     null,
                     null,
                     null,
                     null,
                     fProvider
                     );
               entries.add(entry);
               index++;
            } while (fForStack.next());
            
            fForStack.dropLevel();
            
            // Use 1st as default
            defaultValue = 0;
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
            var.setTableName(otherVar.getTableName());
         }
         else if (variable instanceof ChoiceVariable) {
            ChoiceVariable otherVar = (ChoiceVariable) otherVariable;
            ChoiceVariable var      = (ChoiceVariable) variable;
            var.setData(otherVar.getChoiceData());
            var.setDefault(otherVar.getDefault());
            var.setValue(otherVar.getDefault());
            var.setTableName(otherVar.getTableName());
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
      long dimension = getAttributeAsLong(validateElement, "dim");
      ValidatorInformation validator = new ValidatorInformation(getAttributeAsString(validateElement, "class"), (int)dimension);

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
               validator.addParam(Expression.getValue(value, fProvider));
            }
            else if (type.equalsIgnoreCase("string")) {
               validator.addParam(Expression.getValue(value, fProvider));
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
//   private BaseModel parseSectionsOrOther(BaseModel parent, Element element) throws Exception {
//
////      String toolTip  = getToolTip(element);
//
//      BaseModel model = null;
//
//      if (element.getTagName() == "fragment") {
//         /*
//          * Parse fragment as if it was a continuation of the parent elements
//          * This handles fragments that just include a href= include a <peripheralPage>
//          */
//         for (Node subNode = element.getFirstChild();
//               subNode != null;
//               subNode = subNode.getNextSibling()) {
//            if (subNode.getNodeType() != Node.ELEMENT_NODE) {
//               continue;
//            }
//            model = parseSectionsOrOther(parent, (Element) subNode);
//         }
//      }
////      else if (element.getTagName() == "section") {
////         model = new SectionModel(parent, name, toolTip);
////         parseSectionsOrOtherContents(model, element);
////      }
//      else if (element.getTagName() == "list") {
//         parseList(parent, element);
//      }
//      else {
//         throw new Exception("Expected <section> or <list>, found = \'"+element.getTagName()+"\'");
//      }
//      return model;
//   }

   private void parseList(BaseModel parent, Element varElement) throws Exception {
      if (!checkCondition(varElement)) {
         return;
      }
      ListVariable variable = (ListVariable) createVariable(varElement, ListVariable.class);
      variable.setDerived(true);
      ListVariableModel hiddenListModel = (ListVariableModel) parseCommonAttributes(parent, varElement, variable);
      parseSectionsOrOtherContents(hiddenListModel, varElement);
      parent.removeChild(hiddenListModel);
      hiddenListModel.addChildrenTo(parent);
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
         parseList(parent, element);
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
//         else if (tagName == "list") {
//            parseList(fRootModel, element);
//            fRootModel = new ListModel(null, name);
//            parseSectionsOrOtherContents(fRootModel, element);
//         }
         else if (tagName == "deleteVariables") {
            parseDeleteVariables(element);
         }
         else if (tagName == "constant") {
            parseConstant(element);
         }
         else {
            throw new Exception("Expected <peripheralPage>,  <fragment> or <list>, found tag='" + tagName +"'");
         }
      } catch (Exception e) {
         new ErrorModel(fRootModel, "Parse Error", e.getMessage());
         try {
            System.err.println("parse(): " + element.getTagName() + ", " + getAttributeAsString(element, "name"));
         } catch (Exception e1) {
            System.err.println("parse(): " + element.getTagName());
            // Ignore nested exception
         }
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
//   private static BaseModel createModelFromAlias(VariableProvider provider, BaseModel parent, AliasPlaceholderModel aliasModel) throws Exception {
//
//      String  key        = aliasModel.getKey();
//      boolean isOptional = aliasModel.isOptional();
//
//      Variable variable = provider.safeGetVariable(provider.makeKey(key));
//      if (variable == null) {
//         if (!isOptional) {
//            throw new Exception("Alias not found for '" + key + "' within '"+parent + "', provider = '"+provider+"'");
//         }
//         return null;
//      }
//      String description = aliasModel.getSimpleDescription();
//      if (!description.isEmpty()) {
//         if ((variable.getDescription() != null) && !variable.getDescription().isEmpty()) {
//            throw new Exception("Alias tries to change description for " + key);
//         }
//         variable.setDescription(description);
//      }
//      String toolTip = aliasModel.getRawToolTip();
//      if ((toolTip != null) && !toolTip.isEmpty()) {
//         if ((variable.getToolTip() != null) && !variable.getToolTip().isEmpty()) {
//            throw new Exception("Alias tries to change toolTip for " + key + ", tooltip="+toolTip);
//         }
//         variable.setToolTip(toolTip);
//      }
//      VariableModel model = variable.createModel(null);
//      boolean isConstant = aliasModel.isLocked() || variable.isLocked();
//      model.setLocked(isConstant);
//      String displayName = aliasModel.getName();
//      if (displayName != null) {
//         model.setName(displayName);
//      }
//      return model;
//   }

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
      ArrayList<BaseModel> children        = parent.getChildren();
      ArrayList<BaseModel> deletedChildren = new ArrayList<BaseModel>();

//      Boolean traceDelete = false;
      
      for (int index=0; index<children.size(); index++) {
         BaseModel model = children.get(index);
         if (model instanceof AliasPlaceholderModel) {

            AliasPlaceholderModel aliasModel = (AliasPlaceholderModel) model;
            BaseModel             newModel   = aliasModel.createModelFromAlias(provider);
            
            // Note: createModelFromAlias() handles missing model errors
            if (newModel == null) {
               // Variable not found and model is optional - delete placeholder
               deletedChildren.add(model);
//               traceDelete = true;
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
               boolean allHidden = true;
               if (model.getChildren() != null) {
                  for (BaseModel m : model.getChildren()) {
                     if (!m.isHidden()) {
                        allHidden = false;
                        break;
                     }
                  }
               }
               if (allHidden) {
                  // Entirely hidden category - prune as items initially hidden are never shown (alias of hidden??)
                  deletedChildren.add(model);
               }

            }
         }
      }
      // Remove deleted children
//      if (traceDelete) {
//         for (BaseModel child:deletedChildren) {
//            if (!(child instanceof AliasPlaceholderModel)) {
//               continue;
//            }
//            AliasPlaceholderModel apm = (AliasPlaceholderModel) child;
//            String name = apm.getKey();
//            System.err.println("Removing non-existemt alias '" + name + "'");
//            if ((name != null) && (name.contains("ftm_filter_ch4fval_paired"))) {
//               System.err.println("Found it");
//            }
//         }
//      }
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
