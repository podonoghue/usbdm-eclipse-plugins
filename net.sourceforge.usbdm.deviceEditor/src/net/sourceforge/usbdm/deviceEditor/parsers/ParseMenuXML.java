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
import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable.BitInformation;
import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable.BitInformationEntry;
import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.CategoryVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceData;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.ClipboardVariable;
import net.sourceforge.usbdm.deviceEditor.information.ClockMultiplexorVariable;
import net.sourceforge.usbdm.deviceEditor.information.ClockSelectionVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.ListVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
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
import net.sourceforge.usbdm.deviceEditor.parsers.TemplateContentBuilder.BitmaskEnumBuilder;
import net.sourceforge.usbdm.deviceEditor.parsers.TemplateContentBuilder.ChoiceEnumBuilder;
import net.sourceforge.usbdm.deviceEditor.parsers.TemplateContentBuilder.ClockTemplateBuilder;
import net.sourceforge.usbdm.deviceEditor.parsers.TemplateContentBuilder.TemplateBuilder;
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

   private boolean debugGuards = false;
   
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
      public MenuData(
            BaseModel model,
            Map<String, ArrayList<TemplateInformation>> templateInfos,
            ArrayList<ValidatorInformation> validators,
            ProjectActionList projectActionList) {
         
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
       * Get text from templates with given key in the given namespace
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

   /**
    * Split a string at delimiters with escaping of strings and characters
    * 
    * @param value               String to split
    * @param delimiter           Delimiter to use
    * @param removeOuterQuotes   Whether to remove outer quotes
    * 
    * @return  Array of values
    * 
    * @throws Exception
    */
   static String[] splitValues(String value, char delimiter, boolean removeOuterQuotes) throws Exception {
      
      enum TokenState { Normal, Quoted, DoubleQuoted, EscapedDelimiter, LeadingSpaces, TrailingSpaces };

      ArrayList<String> valueList = new ArrayList<String>();

      StringBuilder sb          = new StringBuilder();
      StringBuilder spaceBuffer = new StringBuilder();

      TokenState tokenState = TokenState.LeadingSpaces;

      for (int index=0; index<value.length(); index++) {
         char ch = value.charAt(index);
         
//         System.err.println("ch           = '" + ch + "'");
//         System.err.println("sb           = '" + sb.toString() + "'");
//         System.err.println("spaceBuffer  = '" + spaceBuffer.toString() + "'");
//         System.err.println("tokenState   = " + tokenState.toString());
//         System.err.println("valueList    = " + Arrays.toString(valueList.toArray(new String[valueList.size()])));
         
         if (tokenState == TokenState.LeadingSpaces) {
            if (Character.isWhitespace(ch)) {
               // Discard leading spaces
               continue;
            }
            tokenState = TokenState.Normal;
            // Fall through
         }
         
         if (tokenState == TokenState.TrailingSpaces) {
            if (Character.isSpaceChar(ch)) {
               // Accumulate trailing spaces
               spaceBuffer.append(ch);
               continue;
            }
            tokenState = TokenState.Normal;
            if (ch != delimiter) {
               // Put back the trailing spaces unless end of token
               sb.append(spaceBuffer.toString());
            }
            spaceBuffer = new StringBuilder();
            // Fall through
         }
         
         switch(tokenState) {
         
         case LeadingSpaces:
         case TrailingSpaces:
            // Can't happen
            break;
            
         case EscapedDelimiter:
            if (ch != delimiter) {
               sb.append('\\');
            }
            sb.append(ch);
            tokenState = TokenState.Normal;
            break;
            
         case Normal:
            if (ch == '\\') {
               tokenState = TokenState.EscapedDelimiter;
               continue;
            }
            if (ch == '\'') {
               tokenState = TokenState.Quoted;
               if (!removeOuterQuotes) {
                  sb.append(ch);
               }
               continue;
            }
            if (ch == '"') {
               tokenState = TokenState.DoubleQuoted;
               if (!removeOuterQuotes) {
                  sb.append(ch);
               }
               continue;
            }
            if (Character.isSpaceChar(ch)) {
               // Accumulate trailing spaces
               spaceBuffer.append(ch);
               tokenState = TokenState.TrailingSpaces;
               continue;
            }
            if (ch == delimiter) {
               // Process text so far
               String t = sb.toString();
               valueList.add(t);
               sb = new StringBuilder();
               tokenState = TokenState.LeadingSpaces;
               continue;
            }
            sb.append(ch);
            break;
         case Quoted:
            if (ch == '\'') {
               tokenState = TokenState.Normal;
               if (!removeOuterQuotes) {
                  sb.append(ch);
               }
               continue;
            }
            sb.append(ch);
            break;
         case DoubleQuoted:
            if (ch == '"') {
               tokenState = TokenState.Normal;
               if (!removeOuterQuotes) {
                  sb.append(ch);
               }
               continue;
            }
            sb.append(ch);
            break;
         }
      }
      if ((tokenState == TokenState.Quoted)||(tokenState == TokenState.DoubleQuoted)) {
         throw new Exception("Unexpected state at end of processing text '"+tokenState.toString()+"'");
      }
      else {
         String t = sb.toString();
         valueList.add(t);
      }
      return valueList.toArray(new String[valueList.size()]);
   }
   
   public static class ForLoop {

      static private class ForloopException extends RuntimeException {
         private static final long serialVersionUID = 1L;

         public ForloopException(String errorMessage) {
            super(errorMessage);
         }
         public ForloopException(Exception e) {
            super(e);
         }
      }

      // Keys to replace
      private final String[] fKeys;

      // Set of values to use in each iteration
      private final String[] fValueList;

      // Set of values for current iteration
      private String[] fValues = null;

      // Iteration count 0..(fValueList-1)
      private int fIterationCount = 0;

      private VariableProvider fProvider;

      private String fIterationVar;

      /**
       * Construct for-loop element to keep track of substitutions
       * @param provider
       * 
       * @param keys          List of keys e.g. "keyA,keyB"
       * @param values        List of values e.g. "valA0,valB0;valA1,valB1;valA2,valB2"
       * @param delimiter     Delimiter to separate multiple keys/values
       * @param iterationVar  Name to use for iteration count
       * 
       * @throws Exception
       */
      public ForLoop(VariableProvider provider, String keys, Object values, char delimiter, String iterationVar) throws Exception {
         if (keys.contains(",")) {
            throw new ForloopException("Can't have ',' in keys '" + keys + "'");
         }
         fProvider      = provider;
         fKeys          = splitValues(keys, ':', true);
         fIterationVar  = (iterationVar!=null)?iterationVar.trim():null;
         
         if (values instanceof String) {
            fValueList  = splitValues((String)values, delimiter, false);
//            fValueList  = ((String)values).split(Pattern.quote(delimiter));
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
            }
         }
      }

      /**
       * Do for-loop substitutions on string
       * 
       * @param text Text to process
       * 
       * @return  Modified text
       * @throws Exception
       */
      public String doSubstitution(String text) throws Exception {
         if (fIterationCount>=fValueList.length) {
            throw new ForloopException("doSubstitution() called after for-loop completed");
         }
         if (fValues == null) {
            fValues = splitValues(fValueList[fIterationCount], ':', true);
         }
         if (fValues.length != fKeys.length) {
            System.err.println("fValueList = \n" + Arrays.toString(fValueList));
            System.err.println("fKeys      = \n" + Arrays.toString(fKeys));
            throw new ForloopException(
                  "Number of values '" + fValueList[fIterationCount]+
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
         if (fIterationVar != null) {
            text = text.replace("%("+fIterationVar+")", Integer.toString(fIterationCount));
         }
         return text;
      }

      public boolean next() throws ForloopException {
         if (fIterationCount>=fValueList.length) {
            throw new ForloopException("next() called after for-loop completed");
         }
         fValues = null;
         if (fIterationCount < fValueList.length) {
            fIterationCount++;
         }
         return (fIterationCount < fValueList.length);
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
         try {
            if (text == null) {
               return null;
            }
            for (ForLoop forLoop:forStack) {
               text = forLoop.doSubstitution(text);
            }
            return text;
         } catch (Exception e) {
            throw new ForLoop.ForloopException(e);
         }
      }

      /**
       * Add for-loop level
       * @param fProvider
       * 
       * @param keys          List of keys e.g. "keyA,keyB"
       * @param values        List of values e.g. "valA0,valB0;valA1,valB1;valA2,valB2"
       * @param delimiter     Delimiter to separate multiple keys/values
       * @param iterationVar  Name to use for iteration count
       * 
       * @throws Exception If keys and values are unmatched
       */
      public void createLevel(VariableProvider fProvider, String keys, Object values, char delimiter, String iterationVar) throws Exception {
         ForLoop loop = new ForLoop(fProvider, keys, values, delimiter, iterationVar);
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
   
   private ArrayList<Variable> fEquationVariables = null;

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

      if (attr instanceof String) {
         try {
            return Expression.getValueAsLong((String)attr, fProvider);
         } catch (Exception e) {
            throw new Exception("Failed to parse Long Attribute \'"+name+"\' value '"+attr+"'", e);
         }
      }
      throw new NumberFormatException("Failed to parse Long Attribute \'"+name+"\' value '"+attr+"'");
      //      try {
      //         return Long.decode(attr.toString());
      //      } catch (NumberFormatException e) {
      //         throw new NumberFormatException("Failed to parse Long Attribute \'"+name+"\' value '"+attr+"'");
      //      }
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
      if(checkCondition(element)) {
         fForStack.setContinueFound();
      }
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
      String    keys           = getAttributeAsString(element, "keys");
      String    iterationVar   = getAttributeAsString(element, "iterationVar");
      Object    values         = getAttribute(element, "values");
      Character delimiter      = getAttributeAsCharacter(element, "delimiter", ';');
      Boolean   stripSemicolon = getAttributeAsBoolean(element, "stripSemicolon", false);
      
      if (keys.isBlank()) {
         throw new Exception("<for>  requires keys = '"+keys+"', values = '"+values+"'");
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
            end   = dims[0];
         }
         else if (dims.length == 2) {
            start = dims[0];
            end   = dims[1] + 1;
         }
         else {
            throw new Exception("Illegal dim value '"+dims+"' for <for> '"+keys+"'");
         }
         StringBuilder sb = new StringBuilder();
         for (int index=start; index<end; index++) {
            if (!sb.isEmpty()) {
               sb.append(";");
            }
            sb.append(index);
         }
         values=sb.toString();
      }
      if (values instanceof String) {
         String t = ((String)values);
         if (t.isBlank()) {
            // Empty loop
            return;
         }
         t = t.trim();
         if (t.endsWith(";")) {
            if (stripSemicolon) {
               t = t.substring(0, t.length()-1);
            }
            else {
               System.err.println("Warning values end with semi-colon, values = '"+t+"'");
            }
            values = t;
         }
      }
      
      fForStack.createLevel(fProvider, keys, values, delimiter, iterationVar);
      do {
         if (graphicWrapper != null) {
            // For loop inside graphic
            graphicWrapper.parseGraphicBoxOrGroup(parentModel, element);
         }
         else {
            parseSectionsOrOtherContents(parentModel, element);
         }
      } while (fForStack.next());
      fForStack.dropLevel();
   }

   private void parseImmediateValue(BaseModel parentModel, Element element, GraphicWrapper graphicWrapper) throws Exception {
      
      if (!checkCondition(element)) {
         return;
      }
      String    keys           = getAttributeAsString(element, "keys");
      Object    values         = getAttribute(element, "values");
//      Character delimiter      = getAttributeAsCharacter(element, "delimiter", ':');
      Boolean   stripSemicolon = getAttributeAsBoolean(element, "stripSemicolon", false);
      
      
      if (keys.isBlank() || (values == null)) {
         throw new Exception("<immediateValue>  requires keys = '"+keys+"', values = '"+values+"'");
      }
      String[] keyArray = keys.split(",");
      Object[] keyValues;
      if (values.getClass().isArray()) {
         keyValues = (Object[]) values;
      }
      else {
         String t = values.toString();
         t = t.trim();
         if (t.endsWith(";")) {
            if (stripSemicolon) {
               t = t.substring(0, t.length()-1);
            }
            else {
               System.err.println("Warning values end with semi-colon, values = '"+t+"'");
            }
            values = t;
         }
         keyValues = new String[1];
         keyValues[0] = t;
      }
      if (keyValues.length != keyArray.length) {
         throw new Exception("<immediateValue>  unmatched key-value length keys = '"+keys+"', values = '"+values+"'");
      }
      StringBuilder valueSb = new StringBuilder();
      StringBuilder keySb   = new StringBuilder();
      for (int index=0; index<keyArray.length; index++) {
         if (!valueSb.isEmpty()) {
            // For loops expect values delimited with colon
            valueSb.append(":");
            keySb.append(":");
         }
         valueSb.append(keyValues[index]);
         keySb.append(keyArray[index]);
//         System.err.println("key= '"+keyArray[index]+"', value= '"+keyValues[index]+"'");
      }
//      System.err.println("values= '"+valueSb.toString()+"'");
      fForStack.createLevel(fProvider, keySb.toString(), valueSb.toString(), ';', null);
      if (graphicWrapper != null) {
         graphicWrapper.parseGraphicBoxOrGroup(parentModel, element);
      }
      else {
         parseSectionsOrOtherContents(parentModel, element);
      }
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

   /**
    * Split text into lines
    * 
    * @param text       Text to split
    * @param maxColumn  Maximum line length
    * 
    * @return  Modified text
    */
   static String wrapText(String text, int maxColumn) {
      int column = 0;
      StringBuilder lineBuffer = new StringBuilder();
      StringBuilder wordBuffer = new StringBuilder();
      
//      if (text.indexOf("\n") != -1) {
//         // Has explicit line breaks - don't auto-split
//         maxColumn=0;
//      }
      boolean newLine = true;
      
      for (int chIndex=0; chIndex<text.length(); chIndex++) {
         char ch = text.charAt(chIndex);
         if (newLine && (ch=='\t')) {
            // Expand tab at start of line
            lineBuffer.append("    ");
            newLine = false;
            continue;
         }
         if (newLine && Character.isWhitespace(ch)) {
            // Discard other white space at start of line
            continue;
         }
         newLine = false;
         if (Character.isWhitespace(ch) && !wordBuffer.isEmpty()) {
            // Append current word to line
            lineBuffer.append(wordBuffer.toString());
            wordBuffer = new StringBuilder();
         }
         if (((maxColumn>0)&&(column>maxColumn))||(ch=='\n')) {
            // Start new line with current (partial) word
            lineBuffer.append("\n");
            column = wordBuffer.length();
            newLine = (column==0);
            if (ch=='\n') {
               continue;
            }
         }
         if (Character.isWhitespace(ch)) {
            lineBuffer.append(ch);
         }
         else {
            wordBuffer.append(ch);
         }
         column++;
      }
      if (wordBuffer != null) {
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
      final int MAX_COLUMN = 110;
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
            Constructor<?> constructor = clazz.getConstructor(VariableProvider.class, String.class, String.class);
            newVariable = (Variable) constructor.newInstance(fProvider, name, key);
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
         variable.setTypeName(otherVariable.getTypeName());
         variable.setBaseType(otherVariable.getBaseType());
         variable.setEnabledBy(otherVariable.getEnabledBy());
         variable.setRegister(otherVariable.getRegister());
//         variable.setGenerateOperators(otherVariable.getGenerateOperators());
//         variable.setGenerateConstants(otherVariable.isGenerateAsConstants());
      }
      String addToVarNames = getAttributeAsString(varElement, "addToVar");
      if (addToVarNames != null) {
         for (String varName:addToVarNames.split(",")) {
            Variable addToVar = safeGetVariable(varName);
            if (addToVar == null) {
               // Create new list
               addToVar = new StringVariable(fProvider, null, fProvider.makeKey(varName));
               addToVar.setDerived(true);
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
      // Be careful not to overwrite derived values unless new value present
      if (varElement.hasAttribute("generateAsConstants")) {
         variable.setGenerateConstants(getAttributeAsBoolean(varElement, "generateAsConstants", false));
      }
      if (varElement.hasAttribute("generateOperators")) {
         variable.setGenerateOperators(getAttributeAsBoolean(varElement, "generateOperators", false));
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
      if (varElement.hasAttribute("locked")) {
         variable.setLocked(Boolean.valueOf(getAttributeAsString(varElement, "locked")));
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
      if (varElement.hasAttribute("enumClass")) {
         variable.setUseEnumClass(getAttributeAsBoolean(varElement, "enumClass", false));
      }
      if (varElement.hasAttribute("valueFormat")) {
         variable.setValueFormat(getAttributeAsString(varElement, "valueFormat"));
      }
      if (varElement.hasAttribute("baseType")) {
         variable.setBaseType(getAttributeAsString(varElement, "baseType", null));
      }
      if (varElement.hasAttribute("enabledBy")) {
         variable.setEnabledBy(getAttributeAsString(varElement, "enabledBy"));
      }
      if (varElement.hasAttribute("register")) {
         variable.setRegister(getAttributeAsString(varElement, "register"));
      }
      if (varElement.hasAttribute("constant")) {
         System.err.println("'constant' attribute no longer supported when creating '" + variable.getName() + "'");
      }
      variable.setLogging(getAttributeAsBoolean(varElement, "logging", false));
      
      if (varElement.hasAttribute("ref")) {
         variable.setReference(getAttributeAsString(varElement, "ref"));
      }
      variable.setAssociatedSignalName(getAttributeAsString(varElement, "signal"));
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
            String autoValueFormat = getDefaultValueFormat(otherVariable);
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
            variable.setMin(getAttribute(varElement, "min"));
         }
         if (varElement.hasAttribute("max")) {
            variable.setMax(getAttribute(varElement, "max"));
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
    * Get bit file bitnum attribute from element
    * 
    * @param element    Element to examine
    * 
    * @return  bitnum from attribute or null if not found
    * 
    * @throws Exception On illegal attribute
    */
   private Integer getBitFieldBitnum(Element element) throws Exception {
      
      String bit   = getAttributeAsString(element, "bit");
      Integer indexValue;
      if (bit == null) {
         return null;
      }
      if (bit.equalsIgnoreCase("all")) {
         indexValue = BitmaskVariable.BIT_INDEX_ALL;
      }
      else if (bit.equalsIgnoreCase("none")) {
         indexValue = BitmaskVariable.BIT_INDEX_NONE;
      }
      else {
         indexValue = getAttributeAsLong(element,   "bit", 0L).intValue();
      }
      return indexValue;
   }
   
   private BitInformation parseBitFields(Element varElement) throws Exception {

      ArrayList<BitmaskVariable.BitInformationEntry> entries = new ArrayList<BitmaskVariable.BitInformationEntry>();

      Long permittedBits = getAttributeAsLong(varElement, "bitmask");
      for (Node subNode = varElement.getFirstChild();
            subNode != null;
            subNode = subNode.getNextSibling()) {

         if (subNode.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) subNode;
         if (element.getTagName().equals("bitField")) {

            // Check if entry has condition to be available for field to be present
            Boolean keepField = checkCondition(element);
            if (!keepField) {
               // Discard choice
               continue;
            }
            BitInformationEntry bitInformationEntry;

            String var = getAttributeAsString(element, "var");
            if (var != null) {
               VariableWithChoices vwc = (VariableWithChoices) safeGetVariable(var);
               if (vwc == null) {
                  throw new Exception("Unable to find referenced variable '"+var+"' within bitField '"+getKeyAttribute(varElement)+"'");
               }
               String name = vwc.getTypeName();

               // Remove peripheral name prefix if present
               Pattern p = Pattern.compile(fPeripheral.getClassBaseName()+"(.+)");
               Matcher m = p.matcher(name);
               if (m.matches()) {
                  name = m.group(1);
               }
               Integer bitnum = getBitFieldBitnum(element);
               bitInformationEntry =
                     new BitInformationEntry(name, vwc.getDescription(), bitnum, vwc.generateMask().mask);
            }
            else {
               String index = getAttributeAsString(element, "bit", "");
               Integer indexValue;
               if (index.equalsIgnoreCase("all")) {
                  indexValue = BitmaskVariable.BIT_INDEX_ALL;
               }
               else if (index.equalsIgnoreCase("none")) {
                  indexValue = BitmaskVariable.BIT_INDEX_NONE;
               }
               else {
                  indexValue = getAttributeAsLong(element, "bit", 0L).intValue();
               }
               bitInformationEntry = new BitInformationEntry(
                     getAttributeAsString(element, "name"),
                     getAttributeAsString(element, "description"),
                     indexValue,
                     getAttributeAsString(element, "macro"));
            }
            entries.add(bitInformationEntry);
         }
         else if (element.getTagName().equals("bitFieldExpansion")) {

            if (!element.hasAttribute("bit") || !element.hasAttribute("name")) {
               throw new Exception("<bitFieldExpansion> must have name and bit attributes "+element);
            }
            if (!element.hasAttribute("keys") || !(element.hasAttribute("dim") || element.hasAttribute("values"))) {
               throw new Exception("<bitFieldExpansion> must have keys with either dim and values attributes "+element);
            }
            String iterVariable  = getAttributeAsString(element, "iterationVar", "i");
            String keys          = getAttributeAsString(element, "keys");
            Object values        = getAttribute(element, "values");
            Integer[] dims       = getAttributeAsListOfInteger(element, "dim");
            Character delimiter  = getAttributeAsCharacter(element, "delimiter", ';');

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
                  throw new Exception("Illegal dim value '"+dims+"' in <bitFieldExpansion> '"+keys+"'");
               }
               // Create values list
               StringBuilder sb = new StringBuilder();
               for (int index=start; index<end; index++) {
                  if (!sb.isEmpty()) {
                     sb.append(";");
                  }
                  sb.append(index);
               }
               values=sb.toString();
            }
            if ((values instanceof String) && ((String)values).isBlank()) {
               // Empty loop
               continue;
            }

            Integer index = 0;
            fForStack.createLevel(fProvider, keys, values, delimiter, iterVariable);
            do {
               if (checkCondition(element)) {
                  Integer bitnum       = getBitFieldBitnum(element);
                  String  name         = getAttributeAsString(element, "name",        "Choice "+index.toString());
                  String  description  = getAttributeAsString(element, "description", "");
                  String  macro        = getAttributeAsString(element, "macro",       "");
                  // TODO var, signal
                  BitInformationEntry bitInformationEntry = new BitInformationEntry(
                        name,
                        description,
                        bitnum,
                        macro);
//                  String associatedHardware = getAttributeAsString(element, "signal");
//                  entry.setAssociatedHardware(associatedHardware);
                  entries.add(bitInformationEntry);
               }
               index++;
            } while (fForStack.next());

            fForStack.dropLevel();
         }
      }
      if (entries.isEmpty()) {
         return null;
      }
      BitInformation bi = new BitInformation(entries.toArray(new BitmaskVariable.BitInformationEntry[entries.size()]), permittedBits);
      return bi;
   }
   
   /**
    * Parse &lt;bitmaskOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception
    */
   private void parseBitfieldOption(BaseModel parent, Element varElement) throws Exception {
      if (!checkCondition(varElement)) {
         return;
      }
      
      String derivedFromName = getAttributeAsString(varElement, "derivedFrom");

      BitmaskVariable variable = (BitmaskVariable) createVariable(varElement, BitmaskVariable.class);
      parseCommonAttributes(parent, varElement, variable);
      
      BitInformation references = parseBitFields(varElement);
      if (references != null) {
         if (derivedFromName != null) {
            throw new Exception("derivedFrom with entries in bitfieldOption '"+variable.getKey()+"'");
         }
         variable.init(references);
      }
      else if (derivedFromName != null) {
         BitmaskVariable other = (BitmaskVariable) safeGetVariable(derivedFromName);
         variable.init(other);
      }
      else {
         throw new Exception("Missing information for bitfieldOption '"+variable.getKey()+"'");
      }
      variable.setPinMap(getAttributeAsString(varElement, "pinMap"));
      variable.setRadix(16);
      generateEnum(varElement, variable);
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
      
      BitmaskVariable variable = (BitmaskVariable) createVariable(varElement, BitmaskVariable.class);
      
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

   private void parseClipboard(BaseModel parent, Element varElement) throws Exception {
      
      if (!checkCondition(varElement)) {
         return;
      }
      ClipboardVariable variable = (ClipboardVariable)createVariable(varElement, ClipboardVariable.class);
      parseCommonAttributes(parent, varElement, variable);
      variable.setDerived(true);
      variable.setLocked(true);
      
      String text = getText(varElement).strip();
      if (text.startsWith("=")) {
         text = Expression.getValueAsString(text.substring(1), fProvider);
      }
//    List<StringPair> substitutions = getTemplateSubstitutions(varElement, null);
//      text = doTemplateSubstitutions(text, substitutions);
      text = text.replaceAll("(\n)?\\s*?\\\\t", "$1   ");
      variable.setValue(text);
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
      if (variable.isLogging()) {
         System.err.println("Found " + variable);
      }
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

   /**
    * Generate table of information attached to choice
    * 
    * @param varElement
    * @param variable
    * @throws Exception
    */
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
//      lists.add(variable.getHiddenChoiceData());
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
   
   /**
    * Generate full enum for variable with choices
    * 
    * @param varElement
    * @param variable
    * @throws Exception
    */
   private void generateEnum(Element varElement, VariableWithChoices variable) throws Exception {

      String typeName  = variable.getTypeName();
      if ((typeName == null) || typeName.isBlank()) {
         return;
      }
      // Defaults to generate enum
      String doEnum = getAttributeAsString(varElement, "generateEnum", "true");
      if ("false".equalsIgnoreCase(doEnum)) {
         return;
      }
      // Check for repeated enums in USBDM namespace
      String where = getAttributeAsString(varElement, "where", "usbdm");
      if (("usbdm".equals(where) &&
         !variable.isGenerateAsConstants() &&
         (fPeripheral != null)) &&
         getDeviceInfo().addAndCheckIfRepeatedItem("$ENUM"+typeName)) {
         // These are common!
         return;
      }

      TemplateInformation templateInfo = createEmptyTemplateInfo(varElement, true);
      if (templateInfo == null) {
         // Indicates repeated template on other namespace e.g. Info class
         return;
      }
      ChoiceEnumBuilder ceb = new ChoiceEnumBuilder(this, varElement, variable);
      templateInfo.setBuilder(ceb);
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
      if ((typeName == null) || typeName.isBlank()) {
         return;
      }
      // Defaults to generate enum
      String doEnum = getAttributeAsString(varElement, "generateEnum", "true");
      if ("false".equalsIgnoreCase(doEnum)) {
         return;
      }
      // Check for repeated enums in USBDM namespace
      String where = getAttributeAsString(varElement, "where", "usbdm");
      if ("usbdm".equals(where) &&
         !variable.isGenerateAsConstants() &&
         (fPeripheral != null) &&
         fPeripheral.getDeviceInfo().addAndCheckIfRepeatedItem("$ENUM"+typeName)) {
         // These are common!
         return;
      }
      TemplateInformation templateInfo = createEmptyTemplateInfo(varElement, true);

      BitmaskEnumBuilder meb = new BitmaskEnumBuilder(this, varElement, variable);
      templateInfo.setBuilder(meb);
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
      // Defaults to generate enum
      String doEnum = getAttributeAsString(varElement, "generateEnum", "true");
      if ("false".equalsIgnoreCase(doEnum)) {
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

      TemplateInformation templateInfo = createEmptyTemplateInfo(varElement, true);

      String description     = escapeString(variable.getDescriptionAsCode());
      String tooltip         = escapeString(variable.getToolTipAsCode());

      StringBuilder body = new StringBuilder();

      String enumClass  = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

      body.append(enumText);

      // Create enum declaration
      String entireEnum;

      entireEnum = String.format(ChoiceEnumBuilder.fullEnumTemplate, description, variable.getName(), tooltip, enumClass, baseType, body.toString());
      String enumGuard = getAttributeAsString(varElement, "enumGuard");
      if (enumGuard != null) {
         // Add guard
         entireEnum = String.format(ChoiceEnumBuilder.guardedEnumTemplate, enumGuard, entireEnum);
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
   public static String makeSafeIdentifierName(String name) {
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
    * 
    *  $(_Structname)   => e.g FTM2 => Ftm or FtmQuad etc
    *  $(_STRUCTNAME)   => e.g FTM2 => FTM or FTMQUAD etc
    *  $(_NAME)         => e.g FTM2 => FTM2
    *  $(_name)         => e.g FTM2 => ftm2
    *  $(_BASENAME)     => e.g FTM0 => FTM, PTA => PT
    *  $(_basename)     => e.g FTM0 => ftm, PTA => pt
    *  $(_Class)        => e.g FTM2 => Ftm2
    *  $(_Baseclass)    => e.g FTM0 => Ftm
    *  $(_instance)     => e.g FTM0 => 0, PTA => A
    *
    * $(_Type)         $(_STRUCTNAME)_Type      => FTM_Type
    * $(_Type)         $(_STRUCTNAME)_Type      => FTMQUAD_Type
    
    * $(_Info)            = $(_Class)Info            => Ftm0Info      Per instance name of class
    * $(_InfoGuard)       = enablePeripheralSupport                   Per instance - peripheral enabled
    *                       irqHandlingMethod                         Enable interrupts on peripheral
    
    * $(_BasicInfo)           = $(_Structname)BasicInfo  => FtmBasicInfo      Shared by instances with common hardware (STRUCT)
    *                                                    => FtmquadBasicInfo  Shared by instances with common hardware (STRUCT)
    * $(_BasicInfoGuard)      = /$(_STRUCTNAME)/_BasicInfoGuard               OR of enablePeripheralSupport for instances using same STRUCT
    * $(_BasicInfoIrqGuard)   = /$(_STRUCTNAME)/_BasicInfoIrqGuard            Per instance - Interrupts enabled for this instance

    * $(_CommonInfo)          = $(_Basename)CommonInfo   => FtmCommonInfo     Shared by all instances (common methods irrespective of hardware)
    * $(_CommonInfoGuard)     = /$(_BASENAME)/_CommonInfoGuard                OR of enablePeripheralSupport for all instances of peripheral irrespective of hardware
    * $(_CommonInfoIrqGuard)  = /$(_BASENAME)/_CommonInfoIrqGuard             Shared by all instances (common methods irrespective of hardware)
    *
    * @param text  Test to process
    * 
    * @return  modified attribute or null if attribute doesn't exist
    */
   String replaceCommonNames(String text) {

//      if (text.contains("$(_InfoIrqGuard)")) {
//         System.err.println("Found it '"+text+"'");
//      }
      if (fPeripheral != null) {
         text = text.replace("$(_Info)",            fPeripheral.getClassName()+"Info");
         text = text.replace("$(_InfoGuard)",       "enablePeripheralSupport");
         text = text.replace("$(_InfoIrqGuard)",    "irqHandlingMethod");
         
         text = text.replace("$(_CommonInfo)",         fPeripheral.getClassBaseName()+"CommonInfo");
         text = text.replace("$(_CommonInfoGuard)",    "/"+fPeripheral.getBaseName()+"/_CommonInfoGuard");
         text = text.replace("$(_CommonInfoIrqGuard)", "/"+fPeripheral.getBaseName()+"/_CommonInfoIrqGuard");
         
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
         text = text.replace("$(_Type)",           var.getValueAsString()+"_Type");
         
         text = text.replace("$(_BasicInfo)",         makePrettyName(var.getValueAsString())+"BasicInfo");
         text = text.replace("$(_BasicInfoGuard)",    "/"+var.getValueAsString()+"/_BasicInfoGuard");
         text = text.replace("$(_BasicInfoIrqGuard)", "/"+var.getValueAsString()+"/_BasicInfoIrqGuard");
         
         text = text.replace("$(_Structname)",     makePrettyName(var.getValueAsString()));
         text = text.replace("$(_STRUCTNAME)",     var.getValueAsString());
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
      try {
         attribute = fForStack.doForSubstitutions(attribute);
         attribute = replaceCommonNames(attribute).trim();

         if (attribute.startsWith("\\=")) {
            // Escaped =
            return attribute.substring(2);
         }
         Object res = attribute;
         if (attribute.startsWith("=")) {
            // Immediate evaluate
            res = Expression.getValue(attribute.substring(1), fProvider);
//            if (attribute.contains("I2S0_Tx_IRQn")) {
//               System.err.println("Found! '"+attribute+"' => '"+res+"'");
//            }
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
    * Get an attribute and apply usual substitutions {@link #getAttributeAsString(Element, String)}.
    * If the attribute is not present then the default parameter is returned.
    * 
    * @param element       Element to obtain attribute from
    * @param attrName      Name of attribute
    * @param defaultValue  Value to return if attribute is not present
    * 
    * @return  Attribute value as character or defaultValue if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed etc
    */
   Character getAttributeAsCharacter(Element element, String attrName, Character defaultValue) throws Exception {

      Object res = getAttribute(element, attrName);
      if (res == null) {
         return defaultValue;
      }
      if (res instanceof String) {
         String s = (String) res;
         if (s.length() == 1) {
            return s.charAt(0);
         }
      }
      throw new Exception("Character expected for '" + element.getAttribute(attrName) + "', found '" + res + "'");
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
      return Long.decode(res.toString());
   }

   /**
    * Get an attribute as Long after applying usual substitutions see {@link #getAttribute(Element, String)}.
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
   Double getAttributeAsDouble(Element element, String attrName, Double defaultValue) throws Exception {

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
    * Get an attribute as Long after applying usual substitutions see {@link #getAttribute(Element, String)}.
    * If the attribute is not present then the default parameter is returned.
    * 
    * @param element       Element to examine
    * @param attrName      Name of attribute to retrieve
    * 
    * @return  Attribute value converted to Boolean or null if attribute doesn't exist
    * 
    * @throws Exception If for-loop completed
    */
   Double getAttributeAsDouble(Element element, String attrName) throws Exception {

      return getAttributeAsDouble(element, attrName, null);
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
    * @return  Modified key or null if not found or blank
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
      if (key.isBlank()) {
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

//   /**
//    * Parse &lt;choiceOption&gt; element<br>
//    *
//    * @param varElement
//    * @throws Exception
//    */
//   private void parseAddChoices(BaseModel parent, Element varElement) throws Exception {
//
//      String key = getKeyAttribute(varElement);
//      if (key == null) {
//         throw new Exception("<addChoice> must have key attribute");
//      }
//      ChoiceVariable variable = safeGetChoiceVariable(key);
//      if (variable == null) {
//         throw new Exception("Cannot find target in <addChoice>, key='"+key+"'");
//      }
//      ChoiceInformation info = parseChoiceData(varElement);
//      variable.addChoices(info.entries, info.defaultEntry);
//   }

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
   private void parsePrint(BaseModel parent, Element varElement) throws Exception {

      if (!checkCondition(varElement)) {
         return;
      }
      String text = getAttributeAsString(varElement, "text");
      System.err.println(text);
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
      String   key = getKeyAttribute(varElement);
      Variable var = fProvider.safeGetVariable(key);
      System.err.print(String.format("%-40s", "<printVar> key ='"+key+"', "));
      if (var == null) {
         System.err.println("Not found");
      }
      else {
         System.err.print("value = ");
         String split = getAttributeAsString(varElement, "split");
         String value = var.getValueAsString().replace("\\n", "\n").replace("\\t", "   ");
         if (var.getValue() instanceof String) {
            value = "\"" + value + "\"";
         }
         if (split != null) {
            String[] ar = value.split(split);
            System.err.println();
            for (String s:ar) {
               System.out.println(s);
            }
            System.err.println();
         }
         else {
            if (value.length()>50) {
               System.err.println();
            }
            else {
               System.err.print("'");
            }
            System.err.print(value);
            if (value.length()>50) {
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
      
      // Removed as secondary dependencies may not exist yet.
//      if (var != null) {
//
//         // Immediately instantiate model for referenced variable
//         BaseModel newModel = placeholderModel.createModelFromAlias(fProvider);
//
//         // Replace alias with new model
//         parent.replaceChild(placeholderModel, newModel);
//         return newModel;
//      }
      
      return placeholderModel;
   }

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
      String value       = getAttributeAsString(element, "value");
      String description = getAttributeAsString(element, "description");
      
      Variable var = Variable.createVariableWithNamedType(fProvider, name, key, type+"Variable", value);
      fProvider.addVariable(var);
      
      var.setLogging(getAttributeAsBoolean(element, "logging", false));
      var.setReference(expression);
      var.setDerived(true);
      var.setLocked(true);
      var.setDescription(description);
   }

   private void parseSignal(Element element) throws Exception {
      if (!checkCondition(element)) {
         return;
      }
      String signalName = getAttributeAsString(element, "name");
      String pinName    = getAttributeAsString(element, "pin", null);
      
      Pattern p = Pattern.compile("^([a-zA-Z]+?)(\\d?)_([a-zA-Z]+?\\d?)$");
      Matcher m = p.matcher(signalName);
      if (!m.matches()) {
         throw new Exception("Invalid name for signal, '"+signalName+"'");
      }
      // String name, String baseName, String instance, String signalName
      Signal signal = fPeripheral.getDeviceInfo().createSignal(signalName,m.group(1),m.group(2),m.group(3));
      if (pinName != null) {
         Pin pin = fPeripheral.getDeviceInfo().createPin(pinName);
         fPeripheral.getDeviceInfo().createMapping(signal, pin, MuxSelection.fixed);
      }
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
            var = Variable.createVariableWithNamedType(fProvider, indexedName, indexedKey, type+"Variable", results[index]);
            var.setDescription(description);
            var.setHidden(isHidden);
            var.setDerived(true);
            var.setConstant();
            fProvider.addVariable(var);
         }
      }
   }

   static class StringPair {
      String key;
      String value;
      StringPair(String key, String value) {
         this.key   = key;
         this.value  = value;
      }
   };

   /**
    * Class to collect template information for later substitutions
    */
   static class TemplateSubstitutionInfo {

      /** Indicates a variable list was provided but none of the variables were found */
      final boolean variableNeededButNoneFound;
      Peripheral peripheral;
      boolean multipleParamsOnNewline ;
      List<String> paramOverride;
      List<String> paramTypesOverride;
      List<String> defaultValueOverride;
      Long numberOfNonDefaultParams;
      ArrayList<Variable> variableList;

      // Padding applied to comments (before * @param)
      String linePadding;

      // Padding applied to tool-tips
      String tooltipPadding;
      
      // Pad out expressions to this width when doing %initExpression
      int padToComments;
      
      // Terminator for initExpression
      String terminator;

      // Separator for initExpression
      String separator;

      // No newline before initExpression (suitable for a single initialisation value)
      boolean initExpressionOnSameLine;
      
      String immediateVariables;

      // Context (register prefix e.g. SP[mpuSlavePortNum].)
      String context;
      
      TemplateSubstitutionInfo(ParseMenuXML parser, Element element, String variableAttributeName) throws Exception {
         
         peripheral = parser.fPeripheral;
         
         multipleParamsOnNewline = Boolean.valueOf(parser.getAttributeAsString(element, "multipleParamsOnNewline", "true"));
         immediateVariables      = parser.getAttributeAsString(element, "immediateVariables");

         context = parser.getAttributeAsString(element, "context");
         String temp = parser.getAttributeAsString(element, "params");
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

         temp = parser.getAttributeAsString(element, "paramTypes");
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

         temp = parser.getAttributeAsString(element, "defaultParamValue");
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

         // Number of non-default params - may be trimmed if some variables don't exist
         numberOfNonDefaultParams = getLongAttribute(element, "nonDefaultParams", 1);

         // Check for variable list to process
         String variablesAttribute = null;
         if (variableAttributeName != null) {
            variablesAttribute = parser.getAttributeAsString(element, variableAttributeName);
         }

         if (variablesAttribute == null) {
            // No variable needed
            variableNeededButNoneFound = false;
            return;
         }

         if (variablesAttribute.isEmpty()) {
            String text = parser.getText(element);
            throw new Exception("Empty '"+variableAttributeName+"' attribute in template\n"+text);
         }
         
         // Create variable list
         String varNames[] = variablesAttribute.split(",");

         // List of variables to actually process
         variableList  = new ArrayList<Variable>();

         ArrayList<Integer>  deletedParams = new ArrayList<Integer>();

         int paramCount=0;
         for (String varName:varNames) {
            String variableKey = parser.fProvider.makeKey(varName.trim());
            Variable var = parser.safeGetVariable(variableKey);
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
            // Discard template
            variableNeededButNoneFound = true;
            return;
         }
         
         variableNeededButNoneFound = false;
         
         if (!variableList.isEmpty()) {
            // Padding applied to comments (before * @param)
            linePadding    = parser.getAttributeAsString(element, "linePadding",    "").replace("x", " ");
            tooltipPadding = parser.getAttributeAsString(element, "tooltipPadding", "x*xxxxxxxx").replace("x", " ");
            padToComments  = parser.getAttributeAsLong(element, "padToComments", 0L).intValue();
            
            // Terminator for initExpression
            terminator     = parser.getAttributeAsString(element, "terminator"    , ";");

            // Separator for initExpression
            separator     = parser.getAttributeAsString(element, "separator"    , "|");

            // No newline before initExpression (suitable for a single initialisation value)
            initExpressionOnSameLine = parser.getAttributeAsBoolean(element, "initExpressionOnSameLine", false);
         }
      }
   };
   
   
   
//   List<StringPair> getTemplateSubstitutions(Element element, String variableAttributeName) throws Exception {
//
//      TemplateSubstitutionInfo info = new TemplateSubstitutionInfo(this, element, variableAttributeName);
//
//      return getTemplateSubstitutions(info);
//   }
   
   /**
    * Applies immediate variables and equation variables
    * 
    * @param element
    * @param text
    * @return
    * @throws Exception
    */
   String doImmediateVariableSubstitution(Element element, String text) throws Exception {

      ArrayList<StringPair> substitutions =  new ArrayList<ParseMenuXML.StringPair>();
      
      String immediateVariables = getAttributeAsString(element, "immediateVariables");
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
      // Equation variables are applied immediately as they do not exist during code generation
      if (fEquationVariables != null) {
         for (Variable equationVariable:fEquationVariables) {
            String from = "$("+equationVariable.getName()+")";
            String to   = equationVariable.getSubstitutionValue();
            substitutions.add(new StringPair(from , to));
         }
      }
      String newText = TemplateContentBuilder.doTemplateSubstitutions(text, substitutions);
      return newText;
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
   private boolean checkTemplateConditions(Element element, String discardRepeats, boolean forEnum) throws Exception {
      
      if (!forEnum && !checkCondition(element)) {
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

      // Already blocked?
      if (fPeripheral.getDeviceInfo().checkIfRepeatedItem(repeatKey)) {
         // These are common!
         return false;
      }

      // Add to list for blocking AFTER parsing this file
      repeatedItems.add(repeatKey);
      return true;
   }

   /**
    * Does some common processing of attributes and creates an empty template info (without text)
    * The template is added to the template list
    * 
    * @param element    XML element being parsed
    * @param forEnum    Used to indicate if generating an enum template associated with an Option element e.g. choiceOption
    * 
    * @return           Template info or null if conditions associated with element are not satisfied
    * 
    * @throws Exception
    */
   TemplateInformation createEmptyTemplateInfo(Element element, boolean forEnum) throws Exception {

      String key;
      if (forEnum) {
         key = getAttributeAsString(element, "templateKey");
      }
      else {
         key = getKeyAttribute(element);
      }
//      Variable inHeader = safeGetVariable("definitionsInHeader");
//      boolean placeInHeader = ((inHeader != null) && inHeader.getValueAsBoolean());

      String discardRepeats      = getAttributeAsString(element, "discardRepeats", "false");
      String namespace           = getAttributeAsString(element, "namespace", forEnum?"definitions":"info");
      String codeGenCondition    = getAttributeAsString(element, "codeGenCondition", "");
      
      /*
      <!-- Where to place generated code -->
      all         - Available anywhere controlled by key substitution <br>
      definitions - Before peripheral class (early) in pinmapping.h <br>
      usbdm       - Before peripheral class (later) in pinmapping.h <br>
      commonInfo  - Within peripheral commonInfo class in pinmapping.h <br>
      basicInfo   - Within peripheral basicInfo class in pinmapping.h <br>
      info        - Within peripheral info class in pinmapping.h (default if not provided) <br>
      static      - In hardware.cpp
      
      forceInfo   - forces Info namespace and clears key and discardRepeats <br>
      */
      String where = getAttributeAsString(element, "where", null); // (info|basicInfo|commonInfo)
      if (where == null) {
         // 'location' is a more permissive alias for where in templates
         where = getAttributeAsString(element, "location", null); // (info|basicInfo|commonInfo)
      }
      if (where != null) {
         if ((key != null) && !"all".equalsIgnoreCase(where)) {
            throw new Exception("Attribute 'key="+key+"' used with 'where='"+where+"' attribute (should be where='all')");
         }
         if ("static".equals(where)) {
            // Static objects in hardware.cpp
            key            = "/HARDWARE/StaticObjects";
            namespace      = "all"; // Anywhere (but ends up in hardware.cpp)
         }
         else if ("usbdm".equals(where)) {
            // Before peripheral class in pinmapping.h
            key            = null;
            namespace      = "usbdm"; // Before peripheral class in pinmapping.h
            // discardRepeats is individual ?
         }
         else if ("definitions".equals(where)) {
            // Before peripheral class in pin_mapping.h
            // Usually enums etc.
            key            = null;
            namespace      = "definitions"; // Before peripheral class in pinmapping.h
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
               throw new Exception("Attribute 'key' or 'templateKey' missing when 'where=all' ");
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
         else if (("forceUsbdm".equals(namespace))) {
            key = null;
            namespace="usbdm";
         }
         else if (("baseClass".equals(namespace))) {
            key = null;
            namespace="basicInfo";
            discardRepeats = fProvider.getVariable("structName").getValueAsString();
         }
         else if (key != null) {
            namespace="all";
         }
      }
      
      // Do some basic check on the template guard expressions
      if (debugGuards) {
         do {
            if (codeGenCondition.isBlank()) {
               continue;
            }
            if ("info".equals(namespace)) {
               if (codeGenCondition.contains("enablePeripheralSupport")) {
                  continue;
               }
               if (codeGenCondition.contains("irqHandlingMethod")) {
                  continue;
               }
               System.err.println("Guard '"+codeGenCondition+"' not expected with namespace 'info'");
            }
            Variable var = fProvider.safeGetVariable("structName");
            String expectedGuard1 = (var==null)?"":"/"+var.getValueAsString()+"/_BasicInfoGuard";
            String expectedGuard2 = (var==null)?"":"/"+var.getValueAsString()+"/_BasicInfoIrqGuard";
            if ("basicInfo".equals(namespace)) {
               if (codeGenCondition.contains(expectedGuard1)) {
                  continue;
               }
               if (codeGenCondition.contains(expectedGuard2)) {
                  continue;
               }
               if (codeGenCondition.contains("enableGettersAndSetters")) {
                  continue;
               }
               System.err.println("Guard '"+codeGenCondition+"' not expected with namespace 'basicInfo'");
            }
            String expectedGuard3 = (var==null)?"":"/"+var.getValueAsString()+"/_CommonInfoGuard";
            String expectedGuard4 = (var==null)?"":"/"+var.getValueAsString()+"/_CommonInfoIrqGuard";
            if ("commonInfo".equals(namespace)) {
               if (codeGenCondition.contains(expectedGuard3)) {
                  continue;
               }
               if (codeGenCondition.contains(expectedGuard4)) {
                  continue;
               }
               if (codeGenCondition.contains("enableGettersAndSetters")) {
                  continue;
               }
               System.err.println("Guard '"+codeGenCondition+"' not expected with namespace 'commonInfo'");
            }
         } while (false);
      }
      // Some checks
      String tag = "<"+element.getTagName()+">";
      if (namespace.isBlank()) {
         throw new Exception(tag+" is missing namespace, key='" + key + "'");
      }
      if ((key != null) && !namespace.equals("all")) {
         throw new Exception("Named "+tag+" must have 'all' namespace, key='" + key + "'");
      }
      if ((key == null) && namespace.equals("all")) {
         throw new Exception(tag+" must have 'key' attribute in 'all' namespace, peripheral='" + fPeripheral.getName() + "'");
      }
      
      if (key != null) {
         // Text after '.' is used to give templates a
         // unique value for discardRepeats and is not actual part of the key
         int dotIndex = key.indexOf(".");
         if (dotIndex > 0) {
            throw new Exception("Dots in jeys no longer supported");
//            key = key.substring(0, dotIndex);
         }
      }
      if (!checkTemplateConditions(element, discardRepeats, forEnum)) {
         return null;
      }

      return addTemplate(key, namespace, codeGenCondition);
   }
   
   /**
    * Parse clockTemplate
    * 
    * @param element
    * @throws Exception
    */
   private void parseClockCodeTemplate(Element element) throws Exception {
      
      TemplateSubstitutionInfo tsi = new TemplateSubstitutionInfo(this, element, "variable");
      
      if (tsi.variableNeededButNoneFound) {
         return;
      }
      
      TemplateInformation templateInfo = createEmptyTemplateInfo(element, false);
      
      if (templateInfo != null) {
         String text = doImmediateVariableSubstitution(element, getText(element));
         ClockTemplateBuilder tb = new ClockTemplateBuilder(tsi, text);
         
         templateInfo.setBuilder(tb);
      }
   }

   /**
    * Parse template element
    * 
    * @param element    Element to parse
    * 
    * @throws Exception
    */
   private void parseTemplate(Element element) throws Exception {

      TemplateSubstitutionInfo tsi = new TemplateSubstitutionInfo(this, element, "variables");
      
      if (tsi.variableNeededButNoneFound) {
         return;
      }
      
      TemplateInformation templateInfo = createEmptyTemplateInfo(element, false);
      
      if (templateInfo != null) {
         String text = doImmediateVariableSubstitution(element, getText(element));
         TemplateBuilder tb = new TemplateBuilder(tsi, text);
         
         templateInfo.setBuilder(tb);
      }
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
         Variable var = safeGetVariable(variable);
         if (var != null) {
            fPeripheral.removeMonitoredVariable(var);
            fEquationVariables.remove(var);
         }
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
      else if (tagName == "bitfieldOption") {
         parseBitfieldOption(parentModel, element);
      }
      else if (tagName == "floatOption") {
         parseDoubleOption(parentModel, element);
      }
      else if (tagName == "binaryOption") {
         parseBinaryOption(parentModel, element);
      }
      else if (tagName == "clipboard") {
         parseClipboard(parentModel, element);
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
      else if (tagName == "bitfieldOption") {
         parseBitfieldOption(parentModel, element);
      }
      else if (tagName == "option") {
         parseOption(parentModel, element);
      }
      else if (tagName == "choiceOption") {
         parseChoiceOption(parentModel, element);
      }
//      else if (tagName == "addChoices") {
//         parseAddChoices(parentModel, element);
//      }
      else if (tagName == "stringOption") {
         parseStringOption(parentModel, element);
      }
      else if (tagName == "print") {
         parsePrint(parentModel, element);
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
      else if (tagName == "signal") {
         parseSignal(element);
      }
      else if (tagName == "for") {
         parseForLoop(parentModel, element, null);
      }
      else if (tagName == "immediateValue") {
         parseImmediateValue(parentModel, element, null);
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
            String codeGenCondition = getAttributeAsString(element, "codeGenCondition", null);
            if (codeGenCondition != null) {
               pal.setUserData(new Expression(codeGenCondition, fProvider));
            }
            pal.visit(new Visitor() {
               @Override
               public Result applyTo(ProjectAction action, Value result, IProgressMonitor monitor) {
                  if (action instanceof ProjectConstant) {
                     ProjectConstant constant = (ProjectConstant) action;
                     Variable var = new StringVariable(fProvider, constant.getId(), constant.getId());
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

   private void parseOption(BaseModel parentModel, Element element) throws Exception {
      String key = getKeyAttribute(element);
      if ((key != null) && key.endsWith("debugGuards")) {
         debugGuards = getAttributeAsImmediateBoolean(element, "value", true);
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
         if (expression == null) {
            var = new StringVariable(fProvider, null, key);
            expression = "-empty-";
         }
         else if (expression instanceof String) {
            var = new StringVariable(fProvider, null, key);
         }
         else if (expression instanceof Long) {
            var = new LongVariable(fProvider, null, key);
         }
         else if (expression instanceof Double) {
            var = new DoubleVariable(fProvider, null, key);
         }
         else if (expression instanceof Boolean) {
            var = new BooleanVariable(fProvider, null, key);
         }
         else {
            throw new Exception("Unexpected type for expression result. Type = "+expression.getClass()+", eqn = "+expression);
         }
         if (fEquationVariables == null) {
            fEquationVariables = new ArrayList<Variable>();
         }
         fEquationVariables.add(var);
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
      final int fX;
      final int fY;
      /**
       * 
       * @param boxX     Current X coordinate
       * @param boxY     Current Y coordinate
       * @param figure   Current figure
       */
      public GraphicWrapper(ParseMenuXML parser, int boxX, int boxY, int x, int y, ClockSelectionFigure figure) {
         fBoxX                   = boxX;
         fBoxY                   = boxY;
         fClockSelectionFigure   = figure;
         fParser                 = parser;
         fX = x;
         fY = y;
      }

      /**
       * Parse graphicElement
       * 
       * @param parentModel      Model being created
       * @param graphicElement   Element to parse
       * 
       * @throws Exception
       */
      void parseGraphic(BaseModel parentModel, Element graphicElement) throws Exception {
//         String tag = graphicElement.getTagName();
//         System.err.println("Parsing: "+tag);
//         if (tag.equals("graphicItem")) {
//            System.err.println("      + "+graphicElement.getAttribute("type"));
//         }
         fParser.parseGrahicElement(parentModel, graphicElement, fClockSelectionFigure, fBoxX, fBoxY, fX, fY);
      }
      
      /**
       * Parse children of graphicElement
       * 
       * @param parentModel           Model being created
       * @param graphicElementOwner   Element containing children to parse
       * 
       * @throws Exception
       */
      void parseGraphicBoxOrGroup(BaseModel parentModel, Element graphicElementOwner) throws Exception {

         String tag = graphicElementOwner.getTagName();
         System.err.println("Parsing: "+tag);
         if (tag.equals("graphicItem")) {
            System.err.println("      + "+graphicElementOwner.getAttribute("type"));
         }
         if (!fParser.checkCondition(graphicElementOwner)) {
            return;
         }

         for (Node node = graphicElementOwner.getFirstChild();
               node != null;
               node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
               continue;
            }
            Element element = (Element) node;
            parseGraphic(parentModel, element);
         }
      }
      
   }

   /**
    * Parse &lt;for&gt;
    * 
    * @param parentModel
    * @param element
    * @throws Exception
    */
   private void parseGraphicIfThen(BaseModel parentModel, Element element, GraphicWrapper graphicWrapper) throws Exception {

//      if (!element.hasAttribute("condition")) {
//         throw new Exception("<if> requires 'condition' attribute '"+element+"'");
//      }
//
//      String t = element.getAttribute("condition");
//      if (t.equals("true")) {
//         System.err.println("Found : "+t);
//      }
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
            graphicWrapper.parseGraphic(parentModel, elem);
         }
      }
   }

private void parseGrahicElement(BaseModel parentModel, Element graphic, ClockSelectionFigure figure, int boxX, int boxY, int x, int y) throws Exception {
   
   if (!checkCondition(graphic)) {
      // Discard element
      return;
   }
   String tagName = graphic.getTagName();
   if (tagName == "graphicItem") {
      String id     = getAttributeAsString(graphic,     "id");
      String varKey = getKeyAttribute(graphic,          "var");
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
      return;
   }
   if (tagName == "offset") {
      x = boxX + Integer.parseInt(getAttributeAsString(graphic, "x", "0"));
      y = boxY + Integer.parseInt(getAttributeAsString(graphic, "y", "0"));
      return;
   }
   if (tagName == "graphicBox") {
      parseGraphicBoxOrGroup(parentModel, x, y, figure, graphic);
      return;
   }
   if (tagName == "graphicGroup") {
      parseGraphicBoxOrGroup(parentModel, x, y, figure, graphic);
      return;
   }
   if (tagName == "equation") {
      parseEquation(graphic);
      return;
   }
   if (tagName == "for") {
      GraphicWrapper wrapper = new GraphicWrapper(this, boxX, boxY, x, y, figure);
      parseForLoop(parentModel, graphic, wrapper);
      return;
   }
   if (tagName == "immediateValue") {
      GraphicWrapper wrapper = new GraphicWrapper(this, boxX, boxY, x, y, figure);
      parseImmediateValue(parentModel, graphic, wrapper);
      return;
   }
   if (tagName == "if") {
      GraphicWrapper dummy = new GraphicWrapper(this, boxX, boxY, x, y, figure);
      parseGraphicIfThen(parentModel, graphic, dummy);
      return;
   }
   throw new Exception("Expected tag = <graphicItem>/<offset>/<graphicBox>/<graphicGroup>, found = <"+tagName+">");
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
         parseGrahicElement(parentModel, graphic, figure, boxX, boxY, x, y);
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
            GraphicWrapper graphicWrapper = new GraphicWrapper(this, 0, 0, 0, 0, figure);
            parseForLoop(parentModel, boxElement, graphicWrapper);
            continue;
         }
         if (tagName == "immediateValue") {
            GraphicWrapper graphicWrapper = new GraphicWrapper(this, 0, 0, 0, 0, figure);
            parseImmediateValue(parentModel, boxElement, graphicWrapper);
            continue;
         }
         throw new Exception("Expected tag = <graphicBox>, found = <"+tagName+">");
      }
   }

   /**
    * Creates TemplateInformation and adds to template list
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
      TemplateInformation templateInfo = new TemplateInformation(fProvider, key, namespace, codeGenerationCondition);

      String templateKey = MenuData.makeKey(key, namespace);

      ArrayList<TemplateInformation> templateList = fTemplateInfos.get(templateKey);
      if (templateList == null) {
         templateList = new ArrayList<TemplateInformation>();
         addOpeningTemplate(templateKey, templateList);
         fTemplateInfos.put(templateKey, templateList);
      }
      templateList.add(templateInfo);
      return templateInfo;
   }

   private void addClosingTemplate() throws Exception {
      ArrayList<TemplateInformation> templateList = fTemplateInfos.get("commonInfo.");
      if (templateList != null) {
         // Add closing template
         final String TEMPLATE=""
               + "}; /* class $(_Baseclass)CommonInfo */ \\n\\n";
         String text = fForStack.doForSubstitutions(TEMPLATE);
         text = replaceCommonNames(text).trim();

         TemplateInformation templateInfo = new TemplateInformation(fProvider, null, "commonInfo", null);
         templateInfo.addText(text);
         templateList.add(templateInfo);
      }

      templateList = fTemplateInfos.get("basicInfo.");
      if (templateList != null) {
         // Add closing template
         final String TEMPLATE=""
               + "}; // class $(_Structname)BasicInfo \\n\\n";
         String text = fForStack.doForSubstitutions(TEMPLATE);
         text = replaceCommonNames(text).trim();

         TemplateInformation templateInfo = new TemplateInformation(fProvider,  null, "basicInfo", null);
         templateInfo.addText(text);
         templateList.add(templateInfo);
      }
   }
   
   private void addOpeningTemplate(String templateKey, ArrayList<TemplateInformation> templateList) throws Exception {
      
         final String TEMPLATE=""
               + "class %decl {\n"
               + "\n"
               + "public:\\n\n"
               + "\n";
               
      if (templateKey.startsWith("commonInfo.")) {
         // Add opening template
         String decl = "$(_CommonInfo)";
         Variable baseClassDeclaration = safeGetVariable("_commonInfo_declaration");
         if (baseClassDeclaration != null) {
            decl = baseClassDeclaration.getValueAsString();
         }
         String text = TEMPLATE.replace("%decl", decl);
         text = fForStack.doForSubstitutions(text);
         text = replaceCommonNames(text).trim();

         TemplateInformation templateInfo = new TemplateInformation(null,  null, "commonInfo", null);
         templateInfo.addText(text);
         templateList.add(templateInfo);
      }
      else if (templateKey.startsWith("basicInfo.")) {
         // Add opening template
         String decl = "$(_BasicInfo)";
         Variable baseClassDeclaration = safeGetVariable("_basicInfo_declaration");
         if (baseClassDeclaration != null) {
            decl = baseClassDeclaration.getValueAsString();
         }
         String text = TEMPLATE.replace("%decl", decl);
         text = fForStack.doForSubstitutions(text);
         text = replaceCommonNames(text).trim();

         TemplateInformation templateInfo = new TemplateInformation(null,  null, "basicInfo", null);
         templateInfo.addText(text);
         templateList.add(templateInfo);
      }
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
      final Integer               defaultIndex;

      public ChoiceInformation(ArrayList<ChoiceData> entries, ArrayList<ChoiceData> hiddenEntries, Integer defaultEntry) {
         this.entries         = entries;
         this.hiddenEntries   = hiddenEntries;
         this.defaultIndex    = defaultEntry;
      }
      
      public ChoiceData getDefaultChoice() {
         if (defaultIndex != null) {
            return entries.get(defaultIndex);
         }
         return null;
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

      boolean defaultExplicitlySet        = false;
      ArrayList<ChoiceData> entries       = new ArrayList<ChoiceData>();
      ArrayList<ChoiceData> hiddenEntries = new ArrayList<ChoiceData>();
      Integer  defaultValue               = null;

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
            entry.setAssociatedHardware(getAttributeAsString(element, "signal", null));
            entry.setToolTip(getToolTip(element));
            if (hidden) {
               hiddenEntries.add(entry);
            }
            else {
               if (defaultValue == null) {
                  // Assume 1st entry is default
                  defaultValue = entries.size();
               }
               if (getAttributeAsBoolean(element, "isDefault", false)) {
                  // Explicit default set
                  if (defaultExplicitlySet) {
                     throw new Exception("Multiple default choices set in <"+menuElement.getTagName() + " name=\""+menuElement.getAttribute("name")+"\"> <choice name=\"" + getAttributeAsString(element, "name")+ "\">");
                  }
                  defaultExplicitlySet = true;
                  defaultValue = entries.size();
               }
               entries.add(entry);
            }
         }
         else if (element.getTagName().equals("choiceExpansion")) {
            if (!element.hasAttribute("value") || !element.hasAttribute("name")) {
               throw new Exception("<choiceExpansion> must have name and value attributes "+element);
            }
            if (!element.hasAttribute("keys") || !(element.hasAttribute("dim") || element.hasAttribute("values"))) {
               throw new Exception("<choiceExpansion> must have keys with either dim and values attributes "+element);
            }
            String iterVariable  = getAttributeAsString(element, "iterationVar", "i");
            String keys          = getAttributeAsString(element, "keys");
            Object values        = getAttribute(element, "values");
            Integer[] dims       = getAttributeAsListOfInteger(element, "dim");
            Character delimiter  = getAttributeAsCharacter(element, "delimiter", ';');

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
                  if (!sb.isEmpty()) {
                     sb.append(";");
                  }
                  sb.append(index);
               }
               values=sb.toString();
            }
            if ((values instanceof String) && ((String)values).isBlank()) {
               // Empty loop
               continue;
            }

            Integer index = 0;
            fForStack.createLevel(fProvider, keys, values, delimiter, iterVariable);
            do {
               if (checkCondition(element)) {
                  String value      = getAttributeAsString(element, "value",      index.toString());
                  String name       = getAttributeAsString(element, "name",       "Choice "+index.toString());
                  String eNum       = getAttributeAsString(element, "enum",       "");
                  String enabledBy  = getAttributeAsString(element, "enabledBy",  null);
                  ChoiceData entry = new ChoiceData(
                        name,
                        value,
                        eNum,
                        null,
                        null,
                        enabledBy,
                        null,
                        fProvider
                        );
                  String associatedHardware = getAttributeAsString(element, "signal");
                  entry.setAssociatedHardware(associatedHardware);
                  if (defaultValue == null) {
                     // Assume 1st entry is default
                     defaultValue = entries.size();
                  }
                  if (getAttributeAsBoolean(element, "isDefault", false)) {
                     // Explicit default set
                     if (defaultExplicitlySet) {
                        throw new Exception("Multiple default choices set in <"+menuElement.getTagName() + " name=\""+menuElement.getAttribute("name")+"\"> <choice name=\"" + getAttributeAsString(element, "name")+ "\">");
                     }
                     defaultExplicitlySet = true;
                     defaultValue = entries.size();
                  }
                  entries.add(entry);
               }
               index++;
            } while (fForStack.next());
            
            fForStack.dropLevel();
         }
      }
      return new ChoiceInformation(entries, hiddenEntries, defaultValue);
   }

   /**
    * Parses the children of this element (binaryOption)
    * 
    * @param  parentModel  Model to attach children to
    * @param  varElement   Menu element to parse (choiceOption)
    * 
    * @throws Exception
    */
   private void parseChoices(BooleanVariable variable, Element varElement) throws Exception {

      ChoiceInformation choiceInfo = parseChoiceData(varElement);

      String disabledValue   = getAttributeAsString(varElement, "disabledValue");
      Variable otherVariable = getDerivedFrom(varElement);

      if (choiceInfo.entries.isEmpty() && (otherVariable != null)) {
         /**
          * Should be another variable of the same type to copy from i.e. derivedFrom="" present
          */
         if (otherVariable.getClass() != variable.getClass()) {
            throw new Exception("Referenced variable of wrong type <"+varElement.getTagName() + " derivedFrom=\"" + variable.getName()+ "\">");
         }
         BooleanVariable otherVar = (BooleanVariable) otherVariable;
         BooleanVariable var      = variable;
         var.setFalseValue(otherVar.getFalseValue());
         var.setTrueValue(otherVar.getTrueValue());
         var.setDefault(otherVar.getDefault());
         var.setDisabledValue(otherVar.getDisabledValue());
         var.setValue(otherVar.getDefault());
         var.setTableName(otherVar.getTableName());
         return;
      }
      
      // Set of choices provided (may be empty!)
      if (choiceInfo.entries.size()>2) {
         throw new Exception("Wrong number of choices in <binaryOption key=\"" + variable.getKey()+ "\">");
      }
      if (choiceInfo.entries.size()==2) {
         variable.setFalseValue(choiceInfo.entries.get(0));
         variable.setTrueValue(choiceInfo.entries.get(1));
      }
      else {
         // As only has a single choice don't save it
         variable.setDerived(true);
         ChoiceData choiceData = choiceInfo.entries.get(0);
         if (Boolean.parseBoolean(choiceData.getValue()) ||
               (Character.isDigit(choiceData.getValue().charAt(0)) && Integer.parseInt(choiceData.getValue())>0)) {
            variable.setTrueValue(choiceData);
         }
         else {
            variable.setFalseValue(choiceData);
         }
      }
      
      // Set up default, disabled and current values
      if (choiceInfo.defaultIndex != null) {
         Object tmp;
         tmp = variable.getDefault();
         if (tmp == null) {
            // Set default if not set
            variable.setDefault(choiceInfo.getDefaultChoice().getValue());
         }
         if (disabledValue != null) {
            // Disabled value explicitly set
            variable.setDisabledValue(disabledValue);
         }
         else {
            // Use default value
            variable.setDisabledValue(variable.getDefault());
         }
         tmp = variable.getEffectiveChoice();
         if (tmp == null) {
            // Set current value if not set
            variable.setValueQuietly(choiceInfo.getDefaultChoice().getValue());
         }
      }
   }

   /**
    * Parses the children of this element (choiceOption)
    * 
    * @param  parentModel  Model to attach children to
    * @param  varElement   Menu element to parse (choiceOption)
    * 
    * @throws Exception
    */
   private void parseChoices(ChoiceVariable variable, Element varElement) throws Exception {

      ChoiceInformation choiceInfo = parseChoiceData(varElement);

      String disabledValue   = getAttributeAsString(varElement, "disabledValue");
      Variable otherVariable = getDerivedFrom(varElement);

      if (choiceInfo.entries.isEmpty()) {
         if (otherVariable == null) {
            // Should be another choice to copy from
            throw new Exception("No choices found for choiceOption");
         }
         /**
          * Should be another variable of the same type to copy from i.e. derivedFrom="" present
          */
         if (otherVariable.getClass() != variable.getClass()) {
            throw new Exception("Referenced variable of wrong type <"+varElement.getTagName() + " derivedFrom=\"" + variable.getName()+ "\">");
         }
         ChoiceVariable otherVar = (ChoiceVariable) otherVariable;
         variable.setData(otherVar.getChoiceData());
         variable.setDefault(otherVar.getDefault());
         variable.setDisabledValue(otherVar.getDisabledValue());
         variable.setValue(otherVar.getDefault());
         variable.setTableName(otherVar.getTableName());
         return;
      }
      
      // Set of choices provided (may be empty!)
      variable.setChoiceData(choiceInfo.entries, choiceInfo.hiddenEntries);
      
      // Set up default, disabled and current values
      if (choiceInfo.defaultIndex != null) {
         Object tmp;
         tmp = variable.getDefault();
         if (tmp == null) {
            // Set default if not set
            variable.setDefault(choiceInfo.defaultIndex);
         }
         if (disabledValue != null) {
            variable.setDisabledValue(disabledValue);
         }
         else {
            variable.setDisabledValue(variable.getDefault());
         }
         tmp = variable.getCurrentChoice();
         if (tmp == null) {
            // Set current value if not set
            variable.setValueQuietly(choiceInfo.defaultIndex);
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
//         }
//      }
      children.removeAll(deletedChildren);
   }

   void deleteEquationVariables() {
      if (fEquationVariables == null) {
         return;
      }
      for (Variable var:fEquationVariables) {
         boolean present = safeGetVariable(var.getKey()) != null;
         if (!present) {
            System.err.println("Failed to remove variable "+var.getName());
            return;
         }
         fProvider.removeVariable(var);
      }
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
      parser.addClosingTemplate();
      parser.deleteEquationVariables();
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

   public DeviceInfo getDeviceInfo() {
      return fPeripheral.getDeviceInfo();
   }
}
