package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.IModelEntryProvider;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.parsers.ParseMenuXML;
import net.sourceforge.usbdm.deviceEditor.parsers.ParseMenuXML.MenuData;
import net.sourceforge.usbdm.deviceEditor.parsers.TemplateInformation;
import net.sourceforge.usbdm.deviceEditor.parsers.XmlDocumentUtilities;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

public abstract class PeripheralWithState extends Peripheral implements IModelEntryProvider, IModelChangeListener {

   public static final String IRQ_HANDLER_INSTALLED_SYMBOL = "irqHandlingMethod";

   /** Data obtained from the Menu description file */
   protected MenuData fMenuData = null;
   
   /** Map of parameters for peripheral */
   protected ArrayList<String> fParamList = new ArrayList<String>();

   /** Status of the peripheral */
   protected Status fStatus = null;

   protected PeripheralWithState(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
//      System.err.println("Creating "+basename+instance+" "+this.getClass());
   }

   /**
    * Load the models and validators for this class of peripheral
    * @return 
    * 
    * @return
    * @throws Exception 
    */
   public MenuData loadModels() throws Exception {
      try {
         fMenuData = ParseMenuXML.parsePeriperalFile(getPeripheralVersionName(), this);
      } catch (Exception e) {
         System.err.println("Warning: Failed to load model "+getPeripheralVersionName()+" for peripheral " + getName() + ", Reason: " + e.getMessage());
      }
      return fMenuData;
   }

   @Override
   public Status getStatus() {
      return fStatus;
   }
   
   @Override
   public BaseModel getModel(BaseModel parent) {
      return null;
   }

   /** Owning model for signal models belonging to this peripheral */
   PeripheralSignalsModel fSignalsModel = null;
   
   @Override
   public void getModels(BaseModel parent) {
      if (fMenuData == null) {
         return;
      }
      BaseModel rootModel = fMenuData.getRootModel();
      if (rootModel != null) {
         rootModel.setParent(parent);
      }    
      if (fSignalsModel != null) {
         fSignalsModel.removeChildren();
         removeListener(fSignalsModel);
         rootModel.removeChild(fSignalsModel);
      }
      fSignalsModel = createPeripheralSignalsModel(rootModel);
   }

   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      writeInfoTemplate(pinMappingHeaderFile);
   }

   public void writeDefaultPinInstances(final DocumentUtilities pinMappingHeaderFile) throws IOException {
      for (int index=0; index<fInfoTable.table.size(); index++) { 
         Signal signal = fInfoTable.table.get(index);

         MappingInfo mappingInfo = signal.getFirstMappedPinInformation();
         if (!mappingInfo.getPin().isAvailableInPackage()) {
            // Discard unmapped signals on this package 
            continue;
         }
         if (mappingInfo.getMux() == MuxSelection.unassigned) {
            // Reset selection - ignore
            continue;
         }
         if (mappingInfo.getMux() == MuxSelection.fixed) {
            // Fixed pin mapping
            continue;
         }
         pinMappingHeaderFile.write(String.format("using %-20s = PcrTable_T<"+getClassName()+"Info,"+index+">;\n", signal.getName()+"_pin", index));
      }
      pinMappingHeaderFile.write("\n");
   }


   /**
    * Writes template-based C code to be included in the information class describing the peripheral<br>
    * 
    * <b>Example:</b>
    * <pre>
    *    // Template:usb0_mk
    *
    *    //! Hardware base address as uint32_t 
    *    static constexpr uint32_t baseAddress = USB0_BasePtr;
    *    ...
    * </pre>
    * 
    * @param pinMappingHeaderFile   Where to write definitions
    * 
    * @throws IOException 
    * @throws Exception 
    */
   public void writeInfoTemplate(DocumentUtilities pinMappingHeaderFile) throws IOException  {
      pinMappingHeaderFile.writeBanner("   ", "Template:" + getPeripheralVersionName());
      if (fMenuData == null) {
//         System.err.println("No fData for " + getName());
         return;
      }
//      System.err.println("fData for " + getName());
      // Get default template for info class
      String template = fMenuData.getTemplate("info", "");
      if (template != null) {
         pinMappingHeaderFile.write(substitute(template));
      }
   }
   
   /**
    * Add variable containing the template-based C code to be included in<br>
    * the peripheral class describing the peripheral<br>
    * 
    * <b>Example:</b>
    * <pre>
    *    // Template:usb0_mk
    *    ...
    * </pre>
    * 
    * @throws IOException 
    * @throws Exception 
    */
   @Override
   public void addClassTemplates() {
      if (fMenuData == null) {
//         System.err.println("No fData for " + getName());
         return;
      }
//      System.err.println("fData for " + getName());
      // Get default template for info class
      String template = fMenuData.getTemplate("class", "");
      if (!template.isBlank()) {
         // Create or replace variable
         fDeviceInfo.addOrUpdateStringVariable("Class Info", "/"+getBaseName()+"/classInfo", substitute(template), true);
      }
   }
   
   @Override
   public void writeNamespaceInfo(DocumentUtilities documentUtilities) throws IOException {
      super.writeNamespaceInfo(documentUtilities);
      if (fMenuData == null) {
         return;
      }
      String template = fMenuData.getTemplate("usbdm", "");
      if ((template != null) && (!template.isEmpty())) {
         documentUtilities.write(substitute(template));
      }
   }

   String expandTemplate(String key, ArrayList<TemplateInformation> fileTemplateList, ISubstitutionMap substitutionMap) {
      // Final template after substitutions
      StringBuffer sb = new StringBuffer();
      for (TemplateInformation fileTemplate:fileTemplateList) {

         // Check for dimension
         int dimension = fileTemplate.getDimension();
         if (dimension > 0) {
            for (int index=0; index<dimension; index++) {
               String expandedTemplate = substitutionMap.substitute(fileTemplate.getExpandedText(), new IndexKeyMaker(fKeyMaker, index));
               sb.append(expandedTemplate);
               //                     sb.append(ReplacementParser.substitute(fileTemplate.getExpandedText(), substitutionMap, new IndexKeyMaker(index)));
            }
         }
         else {
            String expandedTemplate = substitutionMap.substitute(fileTemplate.getExpandedText(), fKeyMaker);
            sb.append(expandedTemplate);
            //                  sb.append(ReplacementParser.substitute(fileTemplate.getExpandedText(), substitutionMap, fKeyMaker));
         }
      }
      return sb.toString();
   }
   
   /**
    * Check if a template is OK to include in output code
    * 
    * @param key Key of template to check
    * 
    * @return true to include
    */
   protected boolean okTemplate(String key) {
      return true;
   }
   
   /**
    * Add shared templates from peripheral
    * @param sharedTemplates 
    */
   protected void updateSharedVariables(ISubstitutionMap substitutionMap, Map<String, String> sharedTemplates) {
      if (fMenuData == null) {
         return;
      }
      substitutionMap.addValue("_instance",   getInstance());       // FTM0 => 0
      substitutionMap.addValue("_name",       getName());           // FTM0 => FTM0
      substitutionMap.addValue("_class",      getClassName());      // FTM0 => Ftm0
      substitutionMap.addValue("_base_class", getClassBaseName());  // FTM0 => Ftm

      fMenuData.getTemplates().forEach(new BiConsumer<String, ArrayList<TemplateInformation>>() {

         @Override
         public void accept(String key, ArrayList<TemplateInformation> fileTemplateList) {
            if (key.endsWith(".") || !MenuData.isKeyAbsolute(key) || !okTemplate(key)) {
               // Skip unnamed or non-global templates
               return;
            }
            
            String t = expandTemplate(key, fileTemplateList, substitutionMap);
            String existingTemplate = sharedTemplates.get(key);
            if (existingTemplate != null) {
               t = existingTemplate + t;
            }
            sharedTemplates.put(key, t);
         }
      });
   }
   
   /**
    * @param processProjectActions 
    * @param project
    * @param monitor
    * 
    * @throws Exception
    */
   public void regenerateProjectFiles(StringBuilder actionRecord, ProcessProjectActions processProjectActions, IProject project, IProgressMonitor monitor) throws Exception {
      if (fMenuData == null) {
         return;
      }
      ISubstitutionMap symbolMap = addTemplatesToSymbolMap(fDeviceInfo.getVariablesSymbolMap());
      processProjectActions.process(actionRecord, project, fMenuData.getProjectActionList(), symbolMap, monitor);
   }

   /**
    * Add named templates to symbol map before doing other substitutions <br>
    * Absolute templates are skipped
    * 
    * @param substitutionMap  Map to symbols add to
    *  
    * @return Modified map
    * @throws Exception 
    */
   protected ISubstitutionMap addTemplatesToSymbolMap(ISubstitutionMap substitutionMap) {
      substitutionMap.addValue("_instance",   getInstance());       // FTM0 => 0
      substitutionMap.addValue("_name",       getName());           // FTM0 => FTM0
      substitutionMap.addValue("_class",      getClassName());      // FTM0 => Ftm0
      substitutionMap.addValue("_base_class", getClassBaseName());  // FTM0 => Ftm

      if (fMenuData == null) {
         return substitutionMap;
      }

      fMenuData.getTemplates().forEach(new BiConsumer<String, ArrayList<TemplateInformation>>() {

         @Override
         public void accept(String key, ArrayList<TemplateInformation> fileTemplateList) {
            if (key.endsWith(".") || MenuData.isKeyAbsolute(key)) {
               // Skip unnamed templates
               return;
            }
            String t = expandTemplate(key, fileTemplateList, substitutionMap);
            substitutionMap.addValue(fKeyMaker.makeKey(key), t);
         }
      });
      return substitutionMap;
   }
   
   /**
    * Adds a variable with this as listener
    * 
    * @param variable  Variable to add
    * 
    * @throws Exception if variable already exists
    */
   public void addVariable(Variable variable) {
      super.addVariable(variable);
      variable.addListener(this);
   }

   /**
    * Does variable substitution in a string using the device variable map<br>
    * The following names are automatically added:
    *    <li> _instance       Name/number of the peripheral instance e.g. FTM0 => 0, PTA => A
    *    <li> _name           Name used to identify this provider e.g. FTM2
    *    <li> _class          Name of C peripheral class e.g. Ftm2
    *    <li> _base_class     Base name of C peripheral class e.g. Ftm
    *    <li> _base_name      Base name of C peripheral class e.g. Ftm
    * 
    * @param input  String to process
    * 
    * @return Modified string or original if no changes
    */
   public String substitute(String input) {
      ISubstitutionMap map = fDeviceInfo.getVariablesSymbolMap();
      map.addValue(makeKey("_instance"),   getInstance());
      map.addValue(makeKey("_name"),       getName());
      map.addValue(makeKey("_class"),      getClassName());
      map.addValue(makeKey("_base_class"), getClassBaseName());
      map.addValue(makeKey("_base_name"),  getBaseName());
//      map.substitute(input, fKeyMaker);
      return substitute(input, map);
   }
   
   @Override
   public void modifyVectorTable(VectorTable vectorTable) {
      try {
         for (IrqVariable var : irqVariables) {
            modifyVectorTable(vectorTable, var, getClassBaseName());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Search vector table for handler and replace with class static method name
    * 
    * @param vectorTable  Vector table to search
    * @param irqVariable  Describes interrupt including: <br>
    * 
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
    * @param className  Base name of C peripheral class e.g. Ftm 
    * @throws Exception 
    */
   public void modifyVectorTable(VectorTable vectorTable, IrqVariable irqVariable, String className) throws Exception {

      if ((irqVariable==null) || (irqVariable.getValueAsLong() == 0)) {
         // No modification
         return;
      }
      final String headerFileName = getBaseName().toLowerCase()+".h";
      boolean classMemberHandlerSet = false;
      String p  = substitute(irqVariable.getPattern());
      String ch = substitute(irqVariable.getClassHandler());
      String patterns[]      = p.split(";");
      String classHandlers[] = ch.split(";");
      if (patterns.length != classHandlers.length) {
         throw new Exception(getClassName() + ": Pattern and classHandler have different lengths in irqOption");
      }
      for (int index=0; index<patterns.length; index++) {
         String pattern = patterns[index];
         pattern = pattern.replaceAll("%b", getBaseName());
         pattern = pattern.replaceAll("%i", getInstance());
         pattern = pattern.replaceAll("%c", className);
         String classHandler = classHandlers[index];
         
         Pattern px = Pattern.compile(pattern);
         for (InterruptEntry entry:vectorTable.getEntries()) {
            if (entry != null) {
               Matcher m = px.matcher(entry.getName());
               if (m.matches()) {
                  String handlerName;
                  switch (irqVariable.getMode()) {
                  case ClassMethod:
                     // Replace with name of class static method
                     classHandler = classHandler.replaceAll("%b", getBaseName());
                     classHandler = classHandler.replaceAll("%i", getInstance());
                     classHandler = classHandler.replaceAll("%c", className);
                     handlerName  = DeviceInfo.NAME_SPACE_USBDM_LIBRARY+"::"+m.replaceAll(classHandler);
                     classMemberHandlerSet = true;
                     break;
                  case NotInstalled:
                  default:
                     handlerName  = "Default_Handler";
                     break;
                  }
                  entry.setHandlerName(handlerName);
                  entry.setHandlerMode(irqVariable.getMode());
               }
            }
            if (classMemberHandlerSet) {
               // Add include file
               vectorTable.addIncludeFile(headerFileName);
            }
         }
      }
   }
   
   @Override
   public void modelElementChanged(ObservableModel observableModel) {
      if (observableModel instanceof Variable) {
         variableChanged((Variable)observableModel);
      }
      
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
   }
   
   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
   }

   /**
    * Get priority - a number used to order the instantiation of peripherals
    * 
    * @return Priority, larger is higher priority.
    */
   public int getPriority() {
      return 0;
   }

   @Override
   public String toString() {
      return this.getClassName()+"("+getName()+", "+getPeripheralVersionName()+")";
   }

   /**
    * Add parameter
    * 
    * @param name    Display name
    * @param key     If not absolute it is made relative to peripheral
    * @param value   Value for param
    */
   public void addParam(String key) {
      fParamList.add(makeKey(key));
   }

   @Override
   protected void writeExtraXMLDefinitions(XmlDocumentUtilities documentUtilities) throws IOException {
      Collections.sort(fParamList);
      for (String key:fParamList) {
         Variable var = safeGetVariable(key);
         documentUtilities.openTag("param");
         documentUtilities.writeAttribute("name",  var.getName());
         documentUtilities.writeAttribute("key",   var.getKey());
         documentUtilities.writeAttribute("value", var.getPersistentValue());
         documentUtilities.closeTag();
      }
   }
   
   protected ArrayList<IrqVariable> irqVariables = new ArrayList<IrqVariable>();
   
   /**
    * Add a Variable describing a IRQ handler setting
    * 
    * @param variable
    */
   public void addIrqVariable(IrqVariable variable) {
      irqVariables.add(variable);
   }

   /**
    * Get list of Variables describing IRQ handler settings
    * 
    * @param variable
    */
   public List<IrqVariable> getIrqVariables(IrqVariable variable) {
      return irqVariables;
   }

   public void instantiateAliases() throws Exception {
      if (fMenuData != null) {
         fMenuData.instantiateAliases(this);
      }
   }
   
   /**
    * Converts a pin name to more pretty form e.g. PTC6 => Ptc6, VREF_OUT => VrefOut
    * 
    * @param pin Original name
    * 
    * @return Converted name
    */
   public String prettyPinName(String pin) {
      char[] p = pin.toLowerCase().toCharArray();
      StringBuffer sb = new StringBuffer();
      
      // Upper-case 1st character
      boolean convertFlag = true;
      for (int index=0; index<p.length; index++) {
         if (p[index]=='_') {
            // Discard and upper-case next character
            convertFlag = true;
         }
         else if (convertFlag) {
            sb.append(Character.toUpperCase(p[index]));
            convertFlag = false;
         }
         else {
            sb.append(p[index]);
         }
      }
      return sb.toString();
   }
   
   /**
    * Checks if signals are mapped to pins.
    * 
    * @param  requiredSignals Array of required signals as indices into the signal table
    * @param  table           Peripheral signal table to use
    * 
    * Updates fStatus
    * 
    * @throws Exception
    */
   protected void validateMappedPins(int requiredSignals[], Vector<Signal> table) {
      
      final Status UNMAPPED_PIN_STATUS = new Status("Not all common signals are mapped to pins", Severity.WARNING);

      if (getCodeIdentifier().isBlank()) {
         // Ignore incomplete pin mapping if peripheral has no user name
         fStatus = null;
         return;
      }
      Status status = null;
      for (int pinNum:requiredSignals) {
         Signal signal = table.get(pinNum);
         if ((signal == null) || (signal.getFirstMappedPinInformation().getPin() == Pin.UNASSIGNED_PIN)) {
            status = UNMAPPED_PIN_STATUS;
            break;
         }
      }
      fStatus = status;
   }

   @Override
   public void validateMappedPins() {
      super.validateMappedPins();
      if (fStatus != null) {
         return;
      }
      ArrayList<InfoTable> signalTables = getSignalTables();
      for (InfoTable signalTable:signalTables) {
         for (Signal signal:signalTable.table) {
            if (signal == null) {
               continue;
            }
            Status status = signal.getStatus();
            if (status != null) {
               fStatus = status;
               return;
            }
         }
      }
   }

   ArrayList<String> fDepenedencies;
   
   public void addDependency(String dependencyVarName) {
      if (fDepenedencies == null) {
         fDepenedencies = new ArrayList<String>();
      }
      fDepenedencies.add(dependencyVarName);
   }
   
   public ArrayList<String> getDepenedencies() {
      return fDepenedencies;
   }

   // List of clock selectors to update on validation
   private ArrayList<Variable> fClockSelectorVariables = null;
   
   /**
    * Add variable as clock selector
    * 
    * @param clockSelector Clock selector to add
    */
   public void addClockSelector(Variable clockSelector) {
      if (fClockSelectorVariables == null) {
         fClockSelectorVariables = new ArrayList<Variable>();
      }
      if (!fClockSelectorVariables.contains(clockSelector)) {
         fClockSelectorVariables.add(clockSelector);
      }
   }
   
   /**
    * Add variable as clock selector
    * 
    * @param clockSelector Clock selector to add
    * @throws Exception 
    */
   public void removeClockSelector(Variable clockSelector) throws Exception {
      if (fClockSelectorVariables == null) {
         return;
      }
      fClockSelectorVariables.remove(clockSelector);
   }
   
   /**
    * Get list of clock selectors to update on validation
    * 
    * @return list of clock selectors
    */
   public ArrayList<Variable> getClockSelectors() {
      return fClockSelectorVariables;
   }

}
