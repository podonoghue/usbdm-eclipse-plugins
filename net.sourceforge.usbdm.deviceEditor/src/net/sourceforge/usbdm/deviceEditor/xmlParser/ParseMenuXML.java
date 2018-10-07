package net.sourceforge.usbdm.deviceEditor.xmlParser;

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable;
import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.IndexedCategoryVariable;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.NumericListVariable;
import net.sourceforge.usbdm.deviceEditor.information.PinListVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.Pair;
import net.sourceforge.usbdm.deviceEditor.information.Variable.Units;
import net.sourceforge.usbdm.deviceEditor.model.AliasPlaceholderModel;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.IndexedCategoryModel;
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
      private final BaseModel                         fRootModel;
      private final Map<String, TemplateInformation>  fTemplates;
      private final ArrayList<ValidatorInformation>   fValidators;
      private final ProjectActionList                 fProjectActionList;
      
      public MenuData(BaseModel model, Map<String, TemplateInformation> fTemplateInfos, ArrayList<ValidatorInformation> validators, ProjectActionList projectActionList) {
         fRootModel  = model;
         fTemplates  = fTemplateInfos;
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
      public Map<String, TemplateInformation> getTemplates() {
         return fTemplates;
      }
      /**
       * Get template with given key in the given namespace
       * 
       * @param namespace  Namespace "info", "class", "usbdm", "all"
       * @param key        Key for template (may be "")
       * @return
       */
      public String getTemplate(String namespace, String key) {
         key = makeKey(key, namespace);
         TemplateInformation template = fTemplates.get(key);
         if (template == null) {
            return "";
         }
         return template.getContents();
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
      
   }

   /** Name of model (filename) */
   @SuppressWarnings("unused")
   private static String fName;

   /** Provider providing the variables used by the menu */
   private final VariableProvider  fProvider;

   /** Peripheral to add vectors etc to */
   private PeripheralWithState fPeripheral;

   /** Used to build the template */
   private final Map<String, TemplateInformation>  fTemplateInfos   = new HashMap<String,TemplateInformation>();

   /** Holds the validators found */
   private final ArrayList<ValidatorInformation> fValidators = new ArrayList<ValidatorInformation>();

   /** Actions associated with this Menu */
   private final ProjectActionList fProjectActionList;

   /** Used to record the first model encountered */
   BaseModel fRootModel = null;

   /** Indicates the index for variables */
   private int fIndex = 0;

   /**
    * 
    * @param provider
    * @param peripheral 
    */
   private ParseMenuXML(VariableProvider provider, PeripheralWithState peripheral) {
      fProvider = provider;
      fPeripheral = peripheral;
      fProjectActionList = new ProjectActionList(provider.getName()+" Action list");
   }

   /**
    * Get variable with given key
    * 
    * @param key     Key to lookup variable
    * 
    * @return variable
    * @throws Exception
    */
   Variable getVariable(String key) throws Exception {
      return fProvider.getVariable(key);
   }

   /**
    * Get variable with given key
    * 
    * @param key     Key to lookup variable
    * 
    * @return variable or null if not found
    */
   Variable safeGetVariable(String key) {
      return fProvider.safeGetVariable(key);
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
    * 
    * @param varElement    Element to obtain attributes from
    * @param clazz         Class of variable to create
    * 
    * @return Variable created (or existing one)
    * @throws Exception 
    */
   private Variable createVariable(Element varElement, Class<?> clazz) throws Exception {

      String  name = varElement.getAttribute("name");
      String  key  = varElement.getAttribute("key");

      String indexSuffix = "";
      indexSuffix = "["+Integer.toString(fIndex)+"]";
      if (key.isEmpty()) {
         key = name;
      }
      key  = substituteKey(key);
      name = substituteKey(name);

      key = key.replaceAll("\\.$", indexSuffix);
      name = name.replaceAll("\\.$", indexSuffix);

      key = fProvider.makeKey(key);
      
      Variable newVariable = null;
      Variable existingVariable = safeGetVariable(key);
      if (existingVariable == null) {
         // New variable
         try {
            newVariable = (Variable) clazz.getConstructor(String.class, String.class).newInstance(name, key);
            fProvider.addVariable(newVariable);
         } catch (Exception e) {
            throw new Exception("Unable to create variable!");
         }
      }
      else {
         if (!existingVariable.getClass().equals(clazz)) {
            throw new Exception("Overridden variable "+key+" has wrong type");
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
   Variable getDerived(Element varElement) throws Exception {      
      Variable otherVariable = null;
      String derivedFromName = varElement.getAttribute("derivedFrom");
      if (!derivedFromName.isEmpty()) {
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
    * 
    * @param parent        Parent for model
    * @param varElement    Element obtain attributes from
    * @param clazz         Class of variable to create
    * 
    * @return Variable created (or existing one)
    * @throws Exception 
    */
   private VariableModel parseCommonAttributes(BaseModel parent, Element varElement, Class<?> clazz) throws Exception {
      
      Variable variable      = createVariable(varElement, clazz);
      Variable otherVariable = getDerived(varElement);
      
      if (otherVariable != null) {
         variable.setDescription(otherVariable.getDescription());
         variable.setToolTip(otherVariable.getToolTip());
         variable.setOrigin(otherVariable.getRawOrigin());
      }
      if (varElement.hasAttribute("description")) {
         variable.setDescription(varElement.getAttribute("description"));
      }
      if (varElement.hasAttribute("toolTip")) {
         variable.setToolTip(getToolTip(varElement));
      }
      if (varElement.hasAttribute("value")) {
         // Value is used as default and initial value
         String value = varElement.getAttribute("value");
         variable.setValue(value);
         variable.setDefault(value);
         variable.setDisabledValue(value);
      }
      if (varElement.hasAttribute("disabledValue")) {
         // Value is used as default and initial value
         String value = varElement.getAttribute("disabledValue");
         variable.setDisabledValue(value);
      }
      if (varElement.hasAttribute("origin")) {
         variable.setOrigin(varElement.getAttribute("origin"));
      }
      if (varElement.hasAttribute("hidden")) {
         // Value is used as default and initial value
         if (Boolean.valueOf(varElement.getAttribute("hidden"))) {
            parent = null;
         }
      }
      variable.setDerived(Boolean.valueOf(varElement.getAttribute("derived")));

      VariableModel model = variable.createModel(parent);
      model.setConstant(Boolean.valueOf(varElement.getAttribute("constant")));
      
      return model;
   }

   /**
    * Parse &lt;longOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseLongOption(BaseModel parent, Element varElement) throws Exception {

      LongVariable variable = (LongVariable) parseCommonAttributes(parent, varElement, LongVariable.class).getVariable();

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
      variable.setUnits(Units.valueOf(varElement.getAttribute("units")));
      variable.setStep(getLongAttribute(varElement, "step"));
      variable.setOffset(getLongAttribute(varElement, "offset"));
   }

   /**
    * Parse &lt;doubleOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseDoubleOption(BaseModel parent, Element varElement) throws Exception {

      DoubleVariable variable = (DoubleVariable) parseCommonAttributes(parent, varElement, DoubleVariable.class).getVariable();

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
      variable.setUnits(Units.valueOf(varElement.getAttribute("units")));
   }

   /**
    * Parse &lt;bitmaskOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseBitmaskOption(BaseModel parent, Element varElement) throws Exception {

      BitmaskVariable variable = (BitmaskVariable) parseCommonAttributes(parent, varElement, BitmaskVariable.class).getVariable();

      try {
         variable.setPermittedBits(getLongAttribute(varElement, "bitmask"));
         variable.setBitList(varElement.getAttribute("bitList"));
      } catch( NumberFormatException e) {
         throw new Exception("Illegal permittedBits value in " + variable.getName(), e);
      }
   }

   /**
    * Parse &lt;choiceOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseChoiceOption(BaseModel parent, Element varElement) throws Exception {

      ChoiceVariable variable = (ChoiceVariable) parseCommonAttributes(parent, varElement, ChoiceVariable.class).getVariable();
      parseChoices(variable, varElement);
   }

   /**
    * Parse &lt;StringOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseStringOption(BaseModel parent, Element varElement) throws Exception {
      
      parseCommonAttributes(parent, varElement, StringVariable.class).getVariable();
   }

   private void parseIndexedCategoryOption(BaseModel parent, Element element) throws Exception {

      IndexedCategoryModel model = (IndexedCategoryModel) parseCommonAttributes(parent, element, IndexedCategoryVariable.class);

      IndexedCategoryVariable variable = model.getVariable();
      
      variable.setValue(element.getAttribute("value"));

      int dimension = getIntAttribute(element, "dim");
      model.setDimension(dimension);
      
      parseChildModels(model, element);

      for (int index=1; index<dimension; index++) {
         IndexedCategoryModel newModel = model.clone(parent, fProvider, index);
         if (newModel.getName() == model.getName()) {
            newModel.setName(model.getName()+"["+index+"]");
         }
         IndexedCategoryVariable newVariable = newModel.getVariable();
         newVariable.setValue(newVariable.getValueAsString().replaceAll("\\.$", Integer.toString(index)));
      }
      variable.setValue(variable.getValueAsString().replaceAll("\\.$", Integer.toString(0)));
   }

   /**
    * Parse &lt;NumericListOption&gt; element<br>
    * 
    * @param varElement
    * @throws Exception 
    */
   private void parseNumericListOption(BaseModel parent, Element varElement) throws Exception {
      
      NumericListVariable variable = (NumericListVariable) parseCommonAttributes(parent, varElement, NumericListVariable.class).getVariable();

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

      BooleanVariable variable = (BooleanVariable) parseCommonAttributes(parent, varElement, BooleanVariable.class).getVariable();
      parseChoices(variable, varElement);
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
      
      IrqVariable variable = (IrqVariable) parseCommonAttributes(parent, irqElement, IrqVariable.class).getVariable();
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

      PinListVariable variable = (PinListVariable) parseCommonAttributes(parent, varElement, PinListVariable.class).getVariable();
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
    * Does some simple substitutions on the key
    *  "$(_name)"     => fProvider.getName()
    *  "$(_instance)" => fPeripheral.getInstance()
    * 
    * @param key
    * 
    * @return modified key
    */
   private String substituteKey(String key) {
      String newKey = key.replaceAll("\\$\\(_name\\)", fProvider.getName());
      if (fPeripheral != null) {
         newKey = key.replaceAll("\\$\\(_instance\\)", fPeripheral.getInstance());
      }
      newKey.replaceAll("\\.$", "");
      return newKey;
   }

   /**
    * Parse &lt;aliasOption&gt; element<br>
    * 
    * @param stringElement
    * @throws Exception 
    */
   private void parseAliasOption(BaseModel parent, Element stringElement) throws Exception {
      // Key and name are interchangeable
      // Name is an IDREF and can be used for validation checks within the file.
      // Key is used to refer to an external variable without validation error
      // DisplayName is used for GUI (model)
      String  name         = stringElement.getAttribute("name");
      String  key          = stringElement.getAttribute("key");
      String  displayName  = stringElement.getAttribute("displayName");
      String  description  = stringElement.getAttribute("description");
      String  toolTip      = getToolTip(stringElement);

      String indexSuffix = "";
      indexSuffix = "["+Integer.toString(fIndex)+"]";
      
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
      key = key.replaceAll("\\.$", indexSuffix);
      key = fProvider.makeKey(key);

      boolean isConstant  = Boolean.valueOf(stringElement.getAttribute("constant"));
      boolean isOptional  = Boolean.valueOf(stringElement.getAttribute("optional"));
      
      AliasPlaceholderModel placeholderModel = new AliasPlaceholderModel(parent, displayName, description);
      placeholderModel.setkey(key);
      placeholderModel.setConstant(isConstant);
      placeholderModel.setOptional(isOptional);
      placeholderModel.setToolTip(toolTip);
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
         fProvider.addVariable(var);
         var.setValue(value);
         var.setDerived(isDerived);
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
      else if (tagName == "template") {
         /**
          * namespace:
          *    class - Template is available in 
          */
         String name = element.getAttribute("name");
         String namespace = element.getAttribute("namespace");
         
         if (namespace.isEmpty()) {
            throw new Exception("Template is missing namespace, name='" + name + "'");
         }
         if (!name.isEmpty() && !namespace.equals("all")) {
            throw new Exception("Named templates must have 'all' namespace, name='" + name + "'");
         }
         int dimension = getIntAttribute(element, "dim");
         addTemplate(name, namespace, dimension,
               element.getTextContent().
               replaceAll("^\n\\s*","").
               replaceAll("(\\\\n|\\n)\\s*", "\n").
               replaceAll("\\\\t","   "));
         //         System.err.println(fTemplate.toString().substring(0, 40)+"\n");
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
                  System.err.println("Adding " + var);
                  fProvider.addVariable(var);
               }
               return Visitor.CONTINUE;
            }}, null);
         fProjectActionList.addProjectAction(pal);
      }
      else {
         throw new Exception("Unexpected field in parseControlItem(), value = \'"+tagName+"\'");
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
   void parseChildModels(BaseModel parentModel, Element menuElement) throws Exception {
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
   void parseChildModel(BaseModel parentModel, Element element) throws Exception {

      String tagName     = element.getTagName();
      String name        = element.getAttribute("name");
      String toolTip     = getToolTip(element);

      //         System.err.println("parseChildModel(): " + tagName + ", " + element.getAttribute("name"));
      if (tagName == "fragment") {
         parseChildModels(parentModel, element);
      }
      else if (tagName == "category") {
         BaseModel model = new CategoryModel(parentModel, name, toolTip);
         parseChildModels(model, element);
      }
      else if (tagName == "indexedCategory") {
         parseIndexedCategoryOption(parentModel, element);
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
      else {
         parseControlItem( element);
      }
   }

   /**
    * Add template<br>
    * If the template exists then the text is appended otherwise it is created.
    * 
    * @param key        Key used to index template
    * @param namespace  Namespace for template (info, usbdm, class)
    * @param dimension  Dimension for array template
    * @param contents   Text contents for template
    * 
    * @throws Exception 
    */
   private void addTemplate(String key, String namespace, int dimension, String contents) throws Exception {
      key = MenuData.makeKey(key, namespace);

      // Check for existing template
      TemplateInformation templateInfo = fTemplateInfos.get(key);
      if (templateInfo == null) {
         // Create new template
         templateInfo = new TemplateInformation(key, namespace, contents, dimension);
         fTemplateInfos.put(key, templateInfo);
      }
      else {
         String previousNamespace = templateInfo.getNamespace();
         if (!previousNamespace.equals(namespace)) {
            throw new Exception("Partial template has inconsistent namespace, expected='"+previousNamespace+"', found='"+namespace+"'");
         }
         int previousDimension = templateInfo.getDimension();
         if ((dimension != 0) && (previousDimension != dimension)) {
            throw new Exception("Partial template has inconsistent dimension, expected='"+previousDimension+"', found='"+dimension+"'");
         }
         templateInfo.addToContents(contents);
      }
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
            throw new UsbdmException("Unable to find peripheral '"+peripheralName+"' for pins");
         }
         return;
      }
      fPeripheral.addSignals(parentModel, peripheral);
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
      NodeList choiceNodes = menuElement.getElementsByTagName("choice");
      for(int index=0; index<choiceNodes.getLength(); index++) {
         Node node = choiceNodes.item(index);
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         Pair entry = new Pair(element.getAttribute("name"), element.getAttribute("value"));
         entries.add(entry);
         if (defaultValue == null) {
            defaultValue = entry.name;
         }
         if (element.getAttribute("isDefault").equalsIgnoreCase("true")) {
            defaultValue = entry.name;
         }
      }
      if (entries.size()==0) {
         /**
          * Should be another variable of the same type to copy from
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
         if (variable instanceof BooleanVariable) {
            if (entries.size()>2) {
               throw new Exception("Wrong number of choices in <"+menuElement.getTagName() + " name=\"" + variable.getName()+ "\">");
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
   }

   public static class TemplateInformation {
      private final String            fKey;
      private final String            fNameSpace;
      private final StringBuilder     fBuilder;
      private final int               fDimension;
      private String            fText = null;
      
      public TemplateInformation(String key, String nameSpace, String contents, int dimension) {
         fKey        = key;
         fNameSpace  = nameSpace;
         fBuilder    = new StringBuilder(contents); 
         fDimension  = dimension;
      }
      
      public String getKey() {
         return fKey;
      }

      public String getContents() {
         if (fText == null) {
            fText = fBuilder.toString();
         }
         return fText;
      }

      public void addToContents(String contents) {
         fBuilder.append(contents);
         fText = null;
      }

      public int getDimension() {
         return fDimension;
      }

      public String getNamespace() {
         return fNameSpace;
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
      Map<String, String> paramMap = null;
      if (fPeripheral != null) {
         paramMap = fPeripheral.getParamMap();
      }
      //      for (String k:paramMap.keySet()) {
      //         System.err.println(k + " => " + paramMap.get(k));
      //      }
      //      System.err.println("================================");
      int dimension = getIntAttribute(validateElement, "dim");
      ValidatorInformation validator = new ValidatorInformation(validateElement.getAttribute("class"), dimension);
      
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
            if (paramMap != null) {
               // Do substitutions on parameter if a map available
               value = fProvider.substitute(value, paramMap);
            }
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
      String toolTip     = getToolTip(topElement);

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
   static BaseModel createModelFromAlias(VariableProvider provider, BaseModel parent, AliasPlaceholderModel aliasModel) throws Exception {
      
      String  key        = aliasModel.getKey();
      boolean isOptional = aliasModel.isOptional();
      
      Variable variable = provider.safeGetVariable(key);
      if (variable == null) {
         if (!isOptional) {
            throw new Exception("Alias not found for " + key + " within "+parent.getName() + ", provider = "+provider);
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
    * Visits all nodes of the model and instantiates any aliases
    * 
    * @param  provider  Provider to look up variables
    * @param  parent    Root of the model tree to visit
    * 
    * @throws Exception
    */
   static void instantiateAliases(VariableProvider provider, BaseModel parent) throws Exception {
      if ((parent == null) || (parent.getChildren()==null)) {
         return;
      }
      ArrayList<Object> children = parent.getChildren();
      
      ArrayList<Object> deletedChildren = new ArrayList<Object>();
      
      for (int index=0; index<children.size(); index++) {
         Object model = children.get(index);
         if (model instanceof AliasPlaceholderModel) {
            BaseModel newModel = createModelFromAlias(provider, parent, (AliasPlaceholderModel) model);
            if (newModel == null) {
               // Variable not found and model is optional - delete placeholder
               deletedChildren.add(model);
            }
            else {
               // Replace placeholder with new model
               children.set(index, newModel);
               newModel.setParentOnly(parent);
            }
         }
         else {
            instantiateAliases(provider, (BaseModel) model);
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
      Element documentElement = document.getDocumentElement();
      if (documentElement == null) {
         throw new Exception("Failed to get documentElement");
      }
      ParseMenuXML parser = new ParseMenuXML(provider, peripheral);
//      document.getDocumentElement();
      for (Node child = document.getFirstChild(); child != null; child = child.getNextSibling()) {
         if (child.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) child;
//         System.err.println("parse(): " + element.getTagName() + ", " + element.getAttribute("name"));
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
   static Path locateFile(String name) throws Exception {
      
      // Add USBDM hardware path
      Path path = Paths.get(DeviceInfo.USBDM_HARDWARE_LOCATION+"/peripherals/"+name);

//      System.err.println("Looking in " + path);
      // For debug try local directory
      if (Files.isRegularFile(path)) {
         return path;
      }
      // Look in USBDM installation
      path = Paths.get(Usbdm.getUsbdmResourcePath()).resolve(path);
//      System.err.println("Looking in " + path);
      if (!Files.isRegularFile(path)) {
         throw new FileNotFoundException("Failed to find hardware file for '"+ name + "'");
      }
      return path;
   }
   
   /**
    * Parses document from top element
    * 
    * @param name         Name of peripheral (used for peripheral file name e.g. adc0_diff_a => adc0_diff_a.xml
    * @param peripheral   Provides the variables. New variables will be added to this peripheral
    * 
    * @return Data from model
    * @throws Exception 
    * 
    * Looks for the file in the following locations in order:
    * <li>Relative path : Stationery/Packages/180.ARM_Peripherals/Hardware/peripherals
    * <li>Relative path : "USBDM Resource Path"/Stationery/Packages/180.ARM_Peripherals/Hardware/peripherals
    */
   public static MenuData parsePeriperalFile(String name, PeripheralWithState peripheral) throws Exception {
      fName = name;
      MenuData fData;
      try {
         // For debug try local directory
         Path path = locateFile(name+".xml");
         fData = parse(XML_BaseParser.parseXmlFile(path), peripheral, peripheral);
         fData.fRootModel.setToolTip(name);
      } catch (FileNotFoundException e) {
         // Some peripherals don't have templates yet - just warn
         throw new Exception("Failed to find peripheral file for "+name, e);
      } catch (Exception e) {
         e.printStackTrace();
         throw new Exception("Failed to process peripheral file for "+name, e);
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
    * @param peripheral          Peripheral associated with this document (if any)
    * 
    * @return Data from model
    * 
    * Looks for the file in the following locations in order:
    * <li>Relative path : Stationery/Packages/180.ARM_Peripherals/Hardware/peripherals
    * <li>Relative path : "USBDM Resource Path"/Stationery/Packages/180.ARM_Peripherals/Hardware/peripherals
    */
   public static MenuData parseMenuFile(String name, VariableProvider variableProvider) throws Exception {
      fName = name;
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
