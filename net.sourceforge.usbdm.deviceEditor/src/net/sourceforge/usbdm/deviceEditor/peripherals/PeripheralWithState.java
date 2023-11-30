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

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralSignalsVariable;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IModelEntryProvider;
import net.sourceforge.usbdm.deviceEditor.model.PeripheralSignalsModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.parsers.ParseMenuXML;
import net.sourceforge.usbdm.deviceEditor.parsers.ParseMenuXML.MenuData;
import net.sourceforge.usbdm.deviceEditor.parsers.TemplateInformation;
import net.sourceforge.usbdm.deviceEditor.parsers.XmlDocumentUtilities;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;
import net.sourceforge.usbdm.peripheralDatabase.Cluster;
import net.sourceforge.usbdm.peripheralDatabase.Field;
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry;
import net.sourceforge.usbdm.peripheralDatabase.Register;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

public abstract class PeripheralWithState extends Peripheral implements IModelEntryProvider {

   public static final String IRQ_HANDLER_INSTALLED_SYMBOL = "irqHandlingMethod";

   /** Data obtained from the Menu description file */
   protected MenuData fMenuData = null;
   
   /** Map of parameters for peripheral */
   protected ArrayList<String> fParamList = new ArrayList<String>();

   /** Status of the peripheral */
   protected Status fStatus = null;

   /** Variable representing signals associated with this peripheral */
   protected PeripheralSignalsVariable fPeripheralSignalsVar;

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
         fMenuData = ParseMenuXML.parsePeripheralFile(getPeripheralVersionName(), this);
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

   /**
    * Add peripheral as source for signals for this peripheral.
    * Actual signal models are created later.
    * 
    * @param peripheral Peripheral to obtain signals from
    * @param filter     Filer applied to select signals added
    * @param enabledBy  Expression enabling this GUI model (used later)
    * @param locked     Whether signal mappings can be modified directly through GUI
    * 
    * @throws Exception
    */
   public void addSignalsFromPeripheral(
         Peripheral peripheral,
         String     filter,
         String     enabledBy,
         Boolean    locked) throws Exception {
      
      fHasSignal = true;
      if (fPeripheralSignalsVar == null) {
         // Create variable associated with Signals list
         fPeripheralSignalsVar = new PeripheralSignalsVariable(this);
         fPeripheralSignalsVar.setLocked(locked);
         fPeripheralSignalsVar.setProvider(this);
         addVariable(fPeripheralSignalsVar);
      }
      if (peripheral == this) {
         if (enabledBy != null) {
            // Add enabled-by handling for signals
            fPeripheralSignalsVar.setEnabledBy(enabledBy);
         }
         // Don't add signals to self!
         return;
      }
      // Add associated signals from provided peripheral (other than self)
      if (fSignalPeripherals == null) {
         fSignalPeripherals = new ArrayList<PeripheralSignals>();
      }
      fSignalPeripherals.add(new PeripheralSignals(peripheral, filter));
   }

   /**
    * Create models representing the signals for this peripheral.<br>
    * <i><b>May add related signals e.g. RTC may contains OSC signals</b></i>
    * 
    * @param parent Model to attach PeripheralSignalsModel to
    * 
    * @return PeripheralSignalsModel containing signals or null if no signals are associated with this peripheral
    */
   public PeripheralSignalsModel createPeripheralSignalsModel(BaseModel parent) {
      if (!fHasSignal) {
         return null;
      }
      return new PeripheralSignalsModel(parent, fPeripheralSignalsVar);
   }

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

   /**
    * Write pin instances for peripheral pins.
    * This is useful for some devices such as I2C or SPI as it allows the pin levels or mapping to be
    * manually changed. (MK, MKL devices only)
    * 
    * @param pinMappingHeaderFile
    * @throws IOException
    */
   public void writeDefaultPinInstances(final DocumentUtilities pinMappingHeaderFile) throws IOException {

      final String pattern;

      // Check for explicit pattern form peripheral XML
      StringVariable patternVar = (StringVariable) safeGetVariable("/SYSTEM/$pcrPattern");
      if (patternVar != null) {
         pattern = patternVar.getValueAsString();
         // Suppressed output
         if (pattern.isBlank()) {
            return;
         }
      }
      else {
         pattern = "PcrTable_T<%cInfo,%t>";
      }
      for (int infoTableIndex=0; infoTableIndex<fInfoTable.table.size(); infoTableIndex++) {
         
         Signal signal = fInfoTable.table.get(infoTableIndex);
         if (signal == null) {
            continue;
         }
         MappingInfo pinMapping = signal.getFirstMappedPinInformation();
         if (!pinMapping.getPin().isAvailableInPackage()) {
            // Discard unmapped signals on this package
            continue;
         }
         if (pinMapping.getMux() == MuxSelection.unassigned) {
            // Reset selection - ignore
            continue;
         }
         if (pinMapping.getMux() == MuxSelection.fixed) {
            // Fixed pin mapping
            continue;
         }
         Pin pin = pinMapping.getPin();
         String type = expandTypePattern(pattern, pin, infoTableIndex, "ActiveHigh");
         pinMappingHeaderFile.write(String.format("using %-20s = %s;\n", signal.getName()+"_pin", type));
//         pinMappingHeaderFile.write(String.format("using %-20s = PcrTable_T<"+getClassName()+"Info,"+infoTableIndex+">;\n", signal.getName()+"_pin", infoTableIndex));
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
      String template = fMenuData.getTemplate("info", "", this);
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
      String template = fMenuData.getTemplate("class", "", this);
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
      String template = fMenuData.getTemplate("usbdm", "", this);
      if ((template != null) && (!template.isEmpty())) {
         documentUtilities.write(substitute(template));
      }
   }

   String expandTemplate(String key, ArrayList<TemplateInformation> fileTemplateList, ISubstitutionMap substitutionMap) {
      // Final template after substitutions
      StringBuffer sb = new StringBuffer();
      for (TemplateInformation fileTemplate:fileTemplateList) {
         String expandedTemplate = substitutionMap.substitute(fileTemplate.getExpandedText(this), fKeyMaker);
         sb.append(expandedTemplate);
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
    * 
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
    * TODO Modify listener approach for peripherals and validators
    */
   @Override
   public void addVariable(Variable variable) {
      super.addVariable(variable);
   }

   /**
    * Does variable substitution in a string using the device variable map<br>
    * The following names are automatically added:
    *  <li>"$(_NAME)"         => e.g FTM2 => FTM2            (fPeripheral.getName())
    *  <li>"$(_name)"         => e.g FTM2 => ftm2            (fPeripheral.getName().tolowercase())
    *  <li>"$(_BASENAME)"     => e.g FTM0 => FTM, PTA => PT  (fPeripheral.getBaseName())
    *  <li>"$(_basename)"     => e.g FTM0 => ftm, PTA => pt  (fPeripheral.getBaseName().tolowercase())
    *  <li>"$(_Class)"        => e.g FTM2 => Ftm2            (fPeripheral.getClassName())
    *  <li>"$(_Baseclass)"    => e.g FTM0 => Ftm             (fPeripheral.getClassBaseName())
    *  <li>"$(_instance)"     => e.g FTM0 => 0, PTA => A     (fPeripheral.getInstance())
    * 
    * @param input  String to process
    * 
    * @return Modified string or original if no changes
    */
   @Override
   public String substitute(String input) {
      ISubstitutionMap map = fDeviceInfo.getVariablesSymbolMap();
      map.addValue(makeKey("_NAME"),       getName());
      map.addValue(makeKey("_name"),       getName().toLowerCase());
      map.addValue(makeKey("_BASENAME"),   getBaseName());
      map.addValue(makeKey("_basename"),   getBaseName().toLowerCase());
      map.addValue(makeKey("_Class"),      getClassName());
      map.addValue(makeKey("_Baseclass"),  getClassBaseName());
      map.addValue(makeKey("_instance"),   getInstance());
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
                     /*Deleted prefix DeviceInfo.NAME_SPACE_USBDM_LIBRARY+"::"+*/
                     handlerName  = m.replaceAll(classHandler);
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

   @Override
   protected void writeExtraXMLDefinitions(XmlDocumentUtilities documentUtilities) throws IOException {
      Collections.sort(fParamList);
      for (String key:fParamList) {
         Variable var;
         try {
            var = getVariable(key);
            documentUtilities.writeParam(var);
         } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
//         documentUtilities.writeParam(var.getName(), var.getKey(), "String", var.getPersistentValue());
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

   // List of clock selectors to update on validation
   private ArrayList<Object> fMonitoredVariables = null;

   /**
    * Remove monitored variable
    * 
    * @param clockSelector Clock selector to add
    * @throws Exception
    */
   public void removeMonitoredVariable(Variable clockSelector) throws Exception {
      if (fMonitoredVariables == null) {
         return;
      }
      fMonitoredVariables.remove(clockSelector);
   }
   
   /**
    * Get list of clock selectors to update on validation
    * 
    * @return list of clock selectors
    */
   public ArrayList<Object> getMonitoredVariables() {
      return fMonitoredVariables;
   }

   /**
    * Create BooleanVariable variable with "true" value<br>
    * It is also added as a param<br>
    * If the constant already exists no action if taken
    * 
    * @param key Key for new variable
    * @throws Exception
    */
   public void addOrIgnoreParam(String key, Object value) throws Exception {
      key = makeKey(key);
      Variable var = safeGetVariable(key);
      if (var == null) {
         if (value == null) {
            var = new BooleanVariable(null, key);
            var.setValue(true);
         }
         else if (value instanceof Boolean) {
            var = new BooleanVariable(null, key);
            var.setValue(value);
         }
         else if ((value instanceof Long)||(value instanceof Integer)) {
            var = new LongVariable(null, key);
            var.setValue(value);
         }
         else if (value instanceof String) {
            var = new StringVariable(null, key);
            var.setValue(value);
         }
         else {
            throw new Exception("Unexpected type");
         }
         addVariable(var);
         fParamList.add(key);
      }
   }
   
   /**
    * Used to create arbitrary variable from strings<br>
    * It is also added as a param<br>
    * 
    * @param name    Name of variable (may be null to use name derived from key)
    * @param key     Key for variable
    * @param type    Type of variable must be e.g. "Long" => "LongVariable: etc
    * @param value   Initial value and default value for variable
    * 
    * @return     Variable created
    * 
    * @throws Exception
    */
   public void addParam(String name, String key, String type, Object value) throws Exception {
      key = makeKey(key);
      Variable var = Variable.createConstantWithNamedType(name, key, type, value);
      addVariable(var);
      fParamList.add(key);
   }
   
   public void extractRegisterFields(net.sourceforge.usbdm.peripheralDatabase.Peripheral dbPortPeripheral, String registerName) throws Exception {
      for(Cluster cl:dbPortPeripheral.getRegisters()) {
         if (!(cl instanceof Register)) {
            continue;
         }
         Register reg = (Register) cl;
         if (reg.getDimension()>0) {
            String key = makeKey(getBaseName().toLowerCase()+"_"+reg.getName().toLowerCase()+"_dim");
            addParam(null, key, "String", reg.getDimension());
         }
         if (reg.getName().equalsIgnoreCase(registerName)) {
            for (Field field:reg.getFields()) {
               String key = makeKey(getBaseName().toLowerCase()+"_"+reg.getName().toLowerCase()+"_"+field.getName().toLowerCase()+"_present");
               addOrIgnoreParam(key, true);
            }
         }
      }
   }
   
   protected void createPresentKey(String registerName) throws Exception {
      String key = makeKey(registerName+"_present");
      key = key.replace("%s", "");
      addOrIgnoreParam(key, true);
   }
   
   /**
    * Create present variables for each register field e.g. /SMC/smc_pmctrl_runm_present
    * 
    * @param dbPortPeripheral Associated database peripheral
    * @throws Exception
    */
   protected void extractClusterRegisterFields(Cluster cluster) throws Exception {

      if (cluster instanceof Register) {
         Register reg = (Register) cluster;
         if (!reg.isHidden()) {
            createPresentKey(getBaseName().toLowerCase()+"_"+reg.getName().toLowerCase());
            if (reg.getDimension()>0) {
               String key = makeKey(getBaseName().toLowerCase()+"_"+reg.getName().toLowerCase()+"_dim");
               key = key.replace("%s", "");
               addOrIgnoreParam(key, reg.getDimension());
            }
            for (Field field:reg.getFields()) {
               createPresentKey(getBaseName().toLowerCase()+"_"+reg.getName().toLowerCase()+"_"+field.getName().toLowerCase());
            }
         }
      }
      else {
         if (cluster.getDimension()>0) {
            String name = cluster.getName().split(",")[0].replace("%s", "");
            String key = makeKey(getBaseName().toLowerCase()+"_"+name.toLowerCase()+"_dim");
            addOrIgnoreParam(key, cluster.getDimension());
            System.err.println("Cluster = "+ name+"["+cluster.getDimension()+"]");
         }
         for(Cluster cl:cluster.getRegisters()) {
            extractClusterRegisterFields(cl);
         }
      }
   }
   
   /**
    * Create present variables for each register field e.g. /SMC/smc_pmctrl_runm_present
    * 
    * @param dbPortPeripheral Associated database peripheral
    * @throws Exception
    */
   protected void extractAllRegisterFields(net.sourceforge.usbdm.peripheralDatabase.Peripheral dbPortPeripheral) {

      try {
         String key = makeKey("_present");
         addOrIgnoreParam(key, null);

         // Create present variables for each register field e.g. /SMC/smc_pmctrl_runm_present
         for(Cluster cl:dbPortPeripheral.getRegisters()) {
            extractClusterRegisterFields(cl);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   /**
    * Extract information from device data base
    * 
    * @param dbPeripheral Database peripheral
    */
   public void extractHardwareInformation(net.sourceforge.usbdm.peripheralDatabase.Peripheral dbPeripheral) {
   }

//   public void addFigure(ClockSelectionFigure figure) {
//      fFigure = figure;
//   }
//
//   public ClockSelectionFigure getFigure() {
//      return fFigure;
//   }
}
