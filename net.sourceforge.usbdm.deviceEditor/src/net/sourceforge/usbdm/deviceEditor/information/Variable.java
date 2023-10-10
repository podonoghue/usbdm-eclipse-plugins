package net.sourceforge.usbdm.deviceEditor.information;

import java.lang.reflect.Constructor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression.VariableUpdateInfo;
import net.sourceforge.usbdm.deviceEditor.parsers.IExpressionChangeListener;
import net.sourceforge.usbdm.deviceEditor.parsers.XML_BaseParser;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;

public abstract class Variable extends ObservableModel implements Cloneable, IExpressionChangeListener {
   
   static DeviceInfo fDeviceInfo=null;
   
   protected boolean defaultHasChanged = false;
   
   /**
    * Set device info object used to register variables
    * 
    * @param deviceInfo
    */
   static void setDeviceInfo(DeviceInfo deviceInfo) {
      fDeviceInfo = deviceInfo;
   }
   
   /**
    * Get device info object used to register variables
    * 
    * @return
    */
   public static DeviceInfo getDeviceInfo() {
      return fDeviceInfo;
   }
   /**
    * Units for physical quantities
    */
   public enum Units {
      None, Hz, s, ticks;

      public String getType() {
         switch (this) {
         case None:  return "int";
         case Hz:    return "Hertz";
         case s:     return "Seconds";
         case ticks: return "Ticks";
         }
         return "IllegalUnits";
      }
      
      public String append(String value) {
         if (this == None) {
            return value;
         };
         return value+"_"+this.toString();
      }
   };

   /** Name of variable visible to user */
   private  String  fName;
   
   /** Key used to identify variable */
   private  String  fKey;
   
   /** Indicates that the variable is locked and cannot be edited by user */
   private boolean fLocked = false;

   /** Indicates the variable is disabled */
   private boolean fEnabled = true;

   /** Description of variable */
   private String fDescription = null;

   /** Tool tip for this variable */
   private String fToolTip = null;
   
   /** Status of variable */
   private Status fStatus = null;
   
   /** Origin of variable value */
   private String fOrigin = null;

   /** Indicates this variable is derived (calculated) from other variables */
   private boolean fIsDerived = false;

   protected boolean fIsConstant = false;

   private boolean fIsHidden;

   private String fDataValue;

   /** Target for clock selector */
   private String fTarget = null;

   /** stem for enum generation e.g. XXXX => XXXX_yyyy enums */
   private String fTypeName = null;

   /** Format for printing value e.g. CMP_CR0_FILTER_CNT(%s) */
   private String fValueFormat = null;
   
   /** Whether to propagate errors form this variable up through categories */
   private Severity fSeverityPropagate;

   /** Register name if known */
   private String fRegister = null;

   /** Variable is dependent on expression i.e. ref="..." */
   private Expression fReference = null;

   /** Condition for enabling this variable i.e. enabledBy="..." */
   private Expression fEnabledBy = null;

   /** Condition for forcing error on this variable i.e. errorIf="..." */
   private Expression fErrorIf = null;

   /** Condition for unlocking this variable i.e. unlockIf="..." */
   private Expression fUnlockedBy;

   private VariableProvider fProvider;

   private Boolean fIsNamedClock = false;

   /** Pin mapping applied when option is disabled */
   private String fDisabledPinMap = null;

   /** Controls when pin-map is enabled */
   private Expression fpinMapEnable = null;

   /** Dynamically hides an item */
   private Expression fHiddenBy = null;

   /**
    * Constructor
    * 
    * @param name Name to display to user. If null then default value is derived from key.
    * @param key  Key for variable.
    */
   public Variable(String name, String key) {
      if (name == null) {
         name = getBaseNameFromKey(key);
      }
      fName = name;
      fKey  = key;
   }

   /**
    * Get the variable name
    * 
    * @return Name
    */
   public String getName() {
      return fName;
   }

   /**
    * Set the variable name
    * 
    * @param name Name to set
    */
   public void setName(String name) {
      fName = name;
   }

   /**
    * @return the key
    */
   public String getKey() {
      return fKey;
   }

//   /**
//    * @param key The key to set
//    */
//   public void setKey(String key) {
//      this.fKey = key;
//   }

   /**
    * Get variable value as a string suitable for user display
    * 
    * @return String for display
    */
   public abstract String getValueAsString();
   
   /**
    * Sets variable value<br>
    * Listeners are informed if the variable changes
    * 
    * @param value The value to set
    * 
    * @return True if variable actually changed value
    */
   public final boolean setValue(Object value) {
      if (!setValueQuietly(value)) {
         return false;
      }
      notifyListeners();
      return true;
   }

   /**
    * Sets variable value and origin<br>
    * Listeners are informed if the variable changes
    * 
    * @param value  Value to set
    * @param origin Origin of change
    * 
    * @return True if variable actually changed value
    */
   public final boolean setValueAndOrigin(Object value, String origin) {
      Boolean modified = false;
      modified = setValueQuietly(value);
      modified = setOriginQuietly(origin) || modified;
      if (modified) {
         notifyListeners();
      }
      return modified;
   }

   /**
    * Sets variable value without affecting listeners
    * 
    * @param value The value to set
    */
   public abstract boolean setValueQuietly(Object value);

   /**
    * Get the variable value as a string for use in saving state
    * 
    * @return the Value
    */
   public abstract String getPersistentValue();

   /**
    * Set the variable value from a string used in restoring state<br>
    * Listeners are not affected
    * 
    * @param value The value to restore
    * @throws Exception
    */
   public abstract void setPersistentValue(String value) throws Exception;

   /**
    * Sets variable default value
    * 
    * @param value The value to set
    */
   public abstract void setDefault(Object value);

   /**
    * Sets variable disabled value
    * 
    * @param value The value to set
    */
   public abstract void setDisabledValue(Object value);

   /**
    * Gets variable default value
    * 
    * @return The default value
    */
   public abstract Object getDefault();

   private String getSimpleClassName() {
      String s = getClass().toString();
      int index = s.lastIndexOf(".");
      return s.substring(index+1, s.length());
   }
   
   @Override
   public String toString() {
      String value = getSubstitutionValue();
      boolean wrap = value.length()>40;
      return String.format(getSimpleClassName()+
            "(Key=%s,"+(wrap?"\n":" ")+"value=%s"+(wrap?"\n":" ")+"(%s))",
            getKey(), getSubstitutionValue(), getValueAsString());
   }
   
   /**
    * Set error status of variable
    * 
    * @param message as String
    * 
    * @return true if status changed
    */
   public boolean setStatusQuietly(String message) {
      if (message == null) {
         return setStatusQuietly((Status)null);
      }
      else {
         return setStatusQuietly(new Status(message));
      }
   }

   /**
    * Set error status of variable
    * 
    * @param message as String
    * 
    * @return true if status changed and listeners notified
    */
   public boolean setStatus(String message) {
      if (message == null) {
         return setStatus((Status)null);
      }
      else {
         return setStatus(new Status(message));
      }
   }

   /**
    * Set status of variable
    * 
    * @param message as Status
    * 
    * @return true if status changed
    */
   public boolean setStatusQuietly(Status message) {
      if ((fStatus == null) && (message == null)) {
         // No change
         return false;
      }
      if ((fStatus != null) && (message != null) && fStatus.equals(message)) {
         // No significant change
         return false;
      }
      fStatus = message;
      return true;
   }
   
   /**
    * Set status of variable
    * 
    * @param message as Status
    * 
    * @return true if status changed and listeners notified
    */
   public boolean setStatus(Status message) {
      if (!setStatusQuietly(message)) {
         // No change
         return false;
      }
      notifyListeners();
      return true;
   }
   
   /**
    * Clear status of variable
    */
   public void clearStatus() {
      setStatus((Status) null);
   }
   
   /**
    * Get status of variable
    * 
    * @return
    */
   public Status getStatus() {
      if (!isEnabled()) {
//         if (fStatus != null) {
//            return fStatus;
//         }
         return null;
      }
      String status = isValid();
      if (status != null) {
         return new Status(status, Severity.ERROR);
      }
      return fStatus;
   }

   /**
    * Get status of variable<br>
    * Filters out status messages with less than INFO severity
    * 
    * @return
    */
   public Status getFilteredStatus() {
      if ((fStatus != null) && (fStatus.getSeverity().greaterThan(Severity.INFO))) {
         return fStatus;
      }
      return null;
   }

   /**
    * Get the origin of variable value<br>
    * This is intended to indicate how the value originated or is derived (calculated)<br>
    * Defaults to the description if not explicitly set by setOrigin().
    * 
    * @return The origin
    */
   public String getOrigin() {
      if (!fEnabled) {
         return "Disabled";
      }
      return (fOrigin!=null)?fOrigin:fDescription;
   }

   /**
    * Get the origin of variable value<br>
    * This is intended to indicate how the value originated or is derived (calculated)
    * 
    * @return The origin
    */
   public String getRawOrigin() {
      return fOrigin;
   }

   /**
    * Set the origin of variable value
    * 
    * @param origin The origin to set
    * 
    * @return true if origin changed
    */
   public boolean setOriginQuietly(String origin) {
      if ((fOrigin == null) && (origin == null)) {
         // No change
         return false;
      }
      if ((fOrigin != null) && (fOrigin.equalsIgnoreCase(origin))) {
         // No significant change
         return false;
      }
      fOrigin = origin;
      return true;
   }

   /**
    * Set the origin of variable value
    * 
    * @param origin The origin to set
    * 
    * @return true if origin changed and listeners notified
    */
   public boolean setOrigin(String origin) {
      if (!setOriginQuietly(origin)) {
         // No change
         return false;
      }
      notifyListeners();
      return true;
   }

   /** Set if the variable is locked and cannot be edited by user
    * 
    * @return the locked
    */
   public boolean isLocked() {
      return fLocked;
   }

   /** Indicates if the variable is locked and cannot be edited in the GUI
    * 
    * @param locked The locked state to set
    * 
    * @return True if variable actually changed lock state
    */
   public boolean setLockedQuietly(boolean locked) {
      if (fLocked == locked) {
         return false;
      }
      fLocked = locked;
      return true;
   }

   /** Indicates if the variable is locked and cannot be edited in the GUI
    * 
    * @param locked The locked state to set
    * 
    * @return True if variable actually changed lock state and listeners notified
    */
   public boolean setLocked(boolean locked) {
      if (!setLockedQuietly(locked)) {
         return false;
      }
      notifyListeners();
      return true;
   }

   /**
    * Mark variable as a constant!
    */
   public void setConstant() {
      fIsConstant = true;
   }
   
   /**
    * Mark variable as a constant!
    */
   public void setConstant(boolean isConstant) {
      fIsConstant = isConstant;
   }
   
   /**
    * Check if variable is actually a constant!
    * 
    * @return
    */
   public boolean isConstant() {
      return fIsConstant;
   }
   
   /**
    * Get value as a boolean
    * 
    * @return Value as boolean
    */
   public boolean getValueAsBoolean() {
      throw new RuntimeException(this+"("+getClass()+") is not compatible with boolean" );
   }

   /**
    * Get value as a boolean without reference to whether it is enabled
    * 
    * @return value as boolean
    */
   public boolean getRawValueAsBoolean() {
      throw new RuntimeException(this+"("+getClass()+") is not compatible with boolean" );
   }

   /**
    * Get the value as a long
    * 
    * @return Value as long (if supported)
    */
   public long getValueAsLong() {
      throw new RuntimeException(this+"("+getClass()+") is not compatible with long" );
      }

   /**
    * Get variable value as long without reference to whether it is enabled
    * 
    * @return Raw value as long (if supported)
    */
   public long getRawValueAsLong() {
      throw new RuntimeException(this+"("+getClass()+") is not compatible with long" );
   }

   /**
    * Get value as a double if representable
    * 
    * @return Value as double (if supported)
    */
   public double getValueAsDouble() {
      throw new RuntimeException(this+"("+getClass()+") is not compatible with double" );
   }

   /**
    * Get value as a double without reference to whether it is enabled
    * 
    * @return Raw value as double (if supported)
    */
   public double getRawValueAsDouble() {
      throw new RuntimeException(this+"("+getClass()+") is not compatible with double" );
   }

   /**
    * Checks if the value is valid for assignment to this variable
    * 
    * @param value String to validate
    * 
    * @return Error message or null if valid
    */
   public String isValid(String value) {
      return null;
   }

   /**
    * Checks if the current variable value is valid
    * 
    * @return Error message or null if valid
    */
   public String isValid() {
      return null;
   }
   
   /**
    * Checks is a character is 'plausible' for this variable<br>
    * Used to validate initial text entry in dialogues<br>
    * Used to restrict key entry when editing.
    * 
    * @param character Character to validate
    * 
    * @return Error message or null if valid
    */
   public String isValidKey(char character) {
      return null;
   }

   
   /**
    * Set the enabled state of variable
    * 
    * @param enabled State to set
    * 
    * @return true if the enabled state changed
    */
   public boolean enableQuietly(boolean enabled) {
      if (fEnabled == enabled) {
         return false;
      }
      fEnabled = enabled;
      return true;
   }

   /**
    * Set the enabled state of variable
    * 
    * @param enabled State to set
    * 
    * @return true if the enabled state changed and listenera notified
    */
   public boolean enable(boolean enabled) {
      if (!enableQuietly(enabled)) {
         return false;
      }
      notifyListeners();
      notifyStatusListeners();
      return true;
   }

   /**
    * @return The enabled state of variable
    */
   public boolean isEnabled() {
      return fEnabled;
   }

   /**
    * Gets description of variable
    * 
    * @return string
    */
   public String getDescription() {
      return fDescription;
   }

   /**
    * Set description of variable
    * 
    * @param description
    */
   public void setDescription(String description) {
      fDescription = description;
   }

   /**
    * Set tool tip
    * 
    * @param toolTip
    */
   public void setToolTip(String toolTip) {
      fToolTip = toolTip;
   }

   /**
    * Get tool tip
    * 
    * @return toolTip
    */
   public String getToolTip() {
      return fToolTip;
   }

   /**
    * Get tool tip.<br>
    * This will be constructed from:
    * <li>Status e.g. warning etc. {@link #setStatus(Status)}
    * <li>Explicitly set Tooltip {@link #setToolTip(String)}
    * <li>Origin {@link #setOrigin(String origin)})
    * 
    * @return String
    */
   public String getDisplayToolTip() {
      StringBuilder sb = new StringBuilder();
      if (fToolTip != null) {
         sb.append(fToolTip);
      }
      Status status = getStatus();
      if (status != null) {
         if (sb.length() != 0) {
            sb.append("\n\n");
         }
         if (status.greaterThan(Status.Severity.WARNING)) {
            sb.append(status.getText());
         }
         else if (status != null) {
            sb.append(status.getSimpleText());
         }
      }
      if (fOrigin != null) {
         if (sb.length() != 0) {
            sb.append("\n\n");
         }
         sb.append("Origin: ");
         sb.append(fOrigin);
      }
      return (sb.length()==0)?null:sb.toString();
   }

   /**
    * Creates model for displaying this variable
    * 
    * @param parent Parent for the new model
    * 
    * @return {@link VariableModel}
    */
   protected abstract VariableModel privateCreateModel(BaseModel parent);

   /**
    * Creates model for displaying this variable
    * 
    * @param parent Parent for the new model (unless hidden)
    * 
    * @return {@link VariableModel}
    */
   public VariableModel createModel(BaseModel parent) {
      if (isHidden()) {
         parent = null;
      }
      VariableModel model = privateCreateModel(parent);
      if (isHidden()) {
         model.setHidden(true);
      }
      return model;
   }
   
   /**
    * Set if this variable is derived (calculated) from other variables
    * 
    * @param derived
    */
   public void setDerived(boolean derived) {
      fIsDerived = derived;
   }
   
   /**
    * Get if this variable is derived (calculated) from other variables
    * 
    * @return true if derived
    */
   public boolean getDerived() {
      return fIsDerived;
   }
   
   /**
    * Get if this variable is derived (calculated) from other variables
    * 
    * @return
    */
   public boolean isDerived() {
      return fIsDerived;
   }

   /**
    * Indicates that the variable value corresponds to the default.
    * 
    * @return
    */
   public abstract boolean isDefault();

   /**
    * Return clone of object
    * 
    * @param name
    * @param symbols
    * 
    * @return Clone
    * @throws Exception
    * 
    * @note All listeners are removed from clone
    */
   public Variable clone(String name, ISubstitutionMap symbols) throws Exception {
      Variable var = null;
      // Create cloned variable
      var = (Variable) super.clone();
      var.removeAllListeners();
      var.fName         = symbols.substitute(fName);
      var.fKey          = symbols.substitute(fKey);
      if (var.fKey == fKey) {
         throw new Exception("Clone has the same key '" + fKey + "'");
      }
      var.fToolTip      = symbols.substitute(fToolTip);
      var.fDescription  = symbols.substitute(fDescription);
      var.fOrigin       = symbols.substitute(fOrigin);
      return var;
   }

   /**
    * Return indexed clone of object (if necessary)
    * 
    * @param provider Provider to register cloned variable with
    * @param index    Index used to modify variable name
    * 
    * @return Clone with modified name or original object if name it is not indexed
    * 
    * @throws CloneNotSupportedException
    * 
    * @note All listeners are removed from clone
    */
   public Variable clone(VariableProvider provider, int index) throws CloneNotSupportedException {
      if (!fName.matches("^.*\\[\\d+\\]$")) {
         // Not indexed - just return itself
         return this;
      }
      Variable var = null;
      String key = fKey.replaceAll("\\[\\d+\\]$", "["+index+"]");
      var = fDeviceInfo.safeGetVariable(key);
      if (var != null) {
         // Use existing variable - probably generated by an alias
         return var;
      }
      // Create cloned variable
      var = (Variable) super.clone();
      var.fName = fName.replaceAll("\\[\\d+\\]$", "["+index+"]");
      var.fKey  = fKey.replaceAll("\\[\\d+\\]$", "["+index+"]");
      var.removeAllListeners();
      provider.addVariable(var);
      return var;
   }

   /**
    * Reset to default value
    */
   public void reset() {
      setValue(getDefault());
   }
   
   public void setHidden(boolean isHidden) {
      boolean changed = (fIsHidden != isHidden );
      fIsHidden = isHidden;
      if (changed) {
         notifyStructureChangeListeners();
      }
   }
   
   public boolean isHidden() {
      return fIsHidden;
   }

   /**
    * Set data value
    * 
    * @param value
    */
   public void setDataValue(String value) {
      fDataValue = value;
   }

   /**
    * Get data value
    * 
    * @return data value
    */
   public String getDataValue() {
      return fDataValue;
   }

   /**
    * Get tool-tip as multi-line comment
    * 
    * @param padding  This is the padding string to apply to start of additional comment lines e.g. " * "
    * 
    * @return tool-tip modified or empty string if none
    */
   public String getToolTipAsCode(String padding) {
      String tooltip = XML_BaseParser.escapeString(getToolTip());
      if (tooltip == null) {
         return "";
      }
      tooltip = tooltip.replace("\n", "\n"+padding);
      return tooltip;
   }
   
   /**
    * Get tool-tip as multi-line comment
    * 
    * @return
    */
   public String getToolTipAsCode() {
      return getToolTipAsCode("\\t * ");
   }
   
   public String getDescriptionAsCode() {
      String description = XML_BaseParser.escapeString(getDescription());
      if (description != null) {
         description = description.replace("\n", "\n\\t * ");
      }
      return description;
   }
   
   public String getShortDescription() {
      String description = XML_BaseParser.escapeString(getDescription());
      if (description != null) {
         int eol = description.indexOf("\n");
         if (eol>=0) {
            description = description.substring(0, eol);
         }
         eol = description.indexOf("\\n");
         if (eol>=0) {
            description = description.substring(0, eol);
         }
      }
         return description;
   }
      
   /**
    * Get the variable value as a string for use in substitutions in C code<br>
    * This is a fundamental value not requiring any generated code support e.g. "123" or "true"
    * 
    * @return String for text substitutions (in C code)
    */
   public abstract String getSubstitutionValue();

   /**
    * Get value for use in C code<br>
    * This value is in the format required for defining a value e.g. "OSC_CR_OSCEN(0)" for use in declaring an ENUM
    * 
    * @return String for text substitutions (in C code)
    */
   public String getDefinitionValue() {

      String format = getValueFormat();
      String value  = getSubstitutionValue();
      if (format == null) {
         return value;
      }
      return String.format(format, value);
   }
   
   /**
    * Get value for use in C code<br>
    * This value is in the final usage format e.g. "I2cSmbAddress_Enabled" or "1234_ticks"<br>
    * This value <b>may need</b> manipulation for use with hardware e.g. integer value wrapped in a macro.
    * 
    * @return String for text substitutions (in C code)
    */
   public String getUsageValue() {
      String format = getValueFormat();
      if (format == null) {
         return getSubstitutionValue();
      }
      try {
         return String.format(format, getSubstitutionValue());
      } catch (Exception e) {
         return "Illegal_format";
      }
   }
   
   /**
    * Get value associated with field from variable
    * 
    * @param field Field used to select type of value to return e.g. "tooltip", "code" ...
    * 
    * @return Nominated value as string
    */
   public String getField(String field) {
      String rv = "field '"+field+"' not found in variable '"+getKey()+"'";
      if (field.equals("tooltip")) {
         rv = getToolTipAsCode();
      }
      if (field.equals("description")) {
         rv = getDescriptionAsCode();
      }
      if (field.equals("shortDescription")) {
         rv = getShortDescription();
      }
      if (field.equals("value")) {
         rv = getSubstitutionValue();
      }
      if (field.equals("data")) {
         rv = getDataValue();
      }
      if (field.equals("definition")) {
         rv = getDefinitionValue();
      }
      if (field.equals("formattedValue")) {
         // deprecated
         System.err.println("Deprecated use of field "+getKey()+".formattedValue");
         rv = getUsageValue();
      }
      if (field.equals("usageValue")) {
         rv = getUsageValue();
      }
      if ((rv==null)||rv.isBlank()) {
         rv = "Unknown field";
      }
      return rv;
   }

   /**
    * Set target for clock selector
    * 
    * @param target
    */
   public void setTarget(String target) {
      fTarget = target;
   }

   /**
    * Get target for updates
    * 
    * @return
    */
   public String getTarget() {
      return fTarget;
   }

   /**
    * Set reference for dependent variable
    * This is the expression the variable depends on
    * 
    * @param reference
    * @throws Exception
    */
   public void setReference(String reference) throws Exception {
      fReference = new Expression(reference, fProvider);
   }
  
   /**
    * Get reference for dependent variable
    * This is the expression the variable depends on
    * 
    * @return reference or null if none
    */
   public Expression getReference() {
      return fReference;
   }
  
   /**
    * Set type/stem for enum code generation e.g. tttt => <br>
    *    <pre>enum Tttt {
    *       Tttt_aaa,
    *       Tttt_bbb,
    *       Tttt_ccc,
    *       }; </pre>
    * 
    * @param enumType Enumeration type/stem
    */
   public void setTypeName(String enumType) {
      fTypeName = enumType;
   }
   
   /**
    * Get type for enum code generation e.g. tttt => <br>
    * <pre>
    *    enum Tttt {
    *       Tttt_aaa,
    *       Tttt_bbb,
    *       Tttt_ccc,
    *       };
    * </pre>
    * 
    * @return Enumeration type/stem
    */
   public String getTypeName() {
      return fTypeName;
   }

   public void setErrorPropagate(String attributeWithFor) {
      fSeverityPropagate = Severity.valueOf(attributeWithFor);
   }
   
   public Severity getErrorPropagate() {
      return fSeverityPropagate;
   }

   /**
    * Set Register name
    * 
    * @param register
    */
   public void setRegister(String register) {
      fRegister=register;
      
   }
   
   /**
    * Get Register name if set, null otherwise
    * 
    * @param register
    */
   public String getRegister() {
      return fRegister;
   }
   
   /**
    * Get value to use as default for parameter in generated parameter lists in C code
    * 
    * @return Default parameter value
    * 
    * @throws Exception
    */
   public String getDefaultParameterValue() throws Exception {
      Object t = getDefault();
      if (t==null) {
         return null;
      }
      String tValue = t.toString();
      String format = getValueFormat();
      if (format == null) {
         return tValue;
      }
      return String.format(format, tValue);
   }

   /**
    * Get format for printing value e.g. CMP_CR0_FILTER_CNT(%s)
    * 
    * @return
    */
   public String getValueFormat() {
      return fValueFormat;
   }

   /**
    * Set format for printing value e.g. CMP_CR0_FILTER_CNT(%s)
    * 
    * @param valueFormat
    */
   public void setValueFormat(String valueFormat) {
      fValueFormat = valueFormat;
   }

   /**
    * Get name from key e.g. /SIM/system_usb_clkin_clock[2] => system_usb_clkin_clock[2]
    * 
    * @param key
    * 
    * @return
    */
   public String getNameFromKey() {
      int index = fKey.lastIndexOf('/');
      if (index<0) {
         return fKey;
      }
      return fKey.substring(index+1);
   }
   
   /**
    * Get base name from key e.g. /SIM/system_usb_clkin_clock[2] => system_usb_clkin_clock
    * 
    * @param key
    * 
    * @return
    */
   public String getBaseNameFromKey() {
      String key = getNameFromKey();
      if (key.matches(".*\\[\\d\\]$")) {
         key = key.substring(0, key.length()-3);
      }
      return key;
   }
   /**
    * Get name from key e.g. /SIM/system_usb_clkin_clock[2] => system_usb_clkin_clock[2]
    * 
    * @param key
    * 
    * @return
    */
   public static String getNameFromKey(String key) {
      int index = key.lastIndexOf('/');
      if (index<0) {
         return key;
      }
      return key.substring(index+1);
   }
   
   /**
    * Get base name from key e.g. /SIM/system_usb_clkin_clock[2] => system_usb_clkin_clock
    * 
    * @param key
    * 
    * @return
    */
   public static String getBaseNameFromKey(String key) {
      key = getNameFromKey(key);
      if (key.matches(".*\\[\\d\\]$")) {
         key = key.substring(0, key.length()-3);
      }
      return key;
   }

   public abstract Object getNativeValue();

   /**
    * Set condition for enabling this variable
    * 
    * @param enabledBy
    * @throws Exception
    */
   public void setEnabledBy(String enabledBy) throws Exception {
      fEnabledBy = new Expression(enabledBy, fProvider);
   }

   /**
    * Set condition for enabling this variable
    * 
    * @param enabledBy
    */
   public void setEnabledBy(Expression enabledBy) {
      fEnabledBy = enabledBy;
   }

   /**
    * Get condition for enabling this variable
    * 
    * @param enabledBy
    */
   public Expression getEnabledBy() {
      return fEnabledBy;
   }

   /**
    * Set provider associated with this variable
    * 
    * @param provider
    */
   public void setProvider(VariableProvider provider) {
      fProvider = provider;
   }

   /**
    * Get variable provider associated with this variable
    * 
    * @return
    */
   public VariableProvider getProvider() {
      return fProvider;
   }

   public void setIsNamedClock(Boolean isNamedClock) {
      fIsNamedClock = isNamedClock;
   }

   public boolean isNamedClock() {
      return fIsNamedClock;
   }

   /**
    * Sets expression to dynamically unlock an item
    * 
    * @param hiddenBy   Expression hiding item
    * 
    * @throws Exception
    */
   public void setUnlockedBy(String unlockedBy) throws Exception {
      fUnlockedBy = new Expression(unlockedBy, fProvider);
   }

   /**
    * Gets expression that dynamically unlocks an item
    * 
    * @return   Expression unlocking item or null if none
    */
   public Expression getUnlockedBy() {
      return fUnlockedBy;
   }

   /**
    * Sets expression to dynamically hide an item
    * 
    * @param hiddenBy   Expression hiding item
    * 
    * @throws Exception
    */
   public void setHiddenBy(String hiddenBy) throws Exception {
      fHiddenBy = new Expression(hiddenBy, fProvider);
   }

   /**
    * Gets expression that dynamically hides an item
    * 
    * @return   Expression hiding item or null if none
    */
   public Expression getHiddenBy() {
      return fHiddenBy;
   }

   /**
    * Set condition for forcing error on this variable
    * 
    * @param errorIf
    * @throws Exception
    */
   public void setErrorIf(String errorIf) throws Exception {
//      if (this.getName().contains("mcgClockMode[1]")) {
//         System.err.println("Found it ");
//      }
      fErrorIf = new Expression(errorIf, fProvider);
   }

   /**
    * Get condition for forcing error on this variable
    * 
    * @param enabledBy
    */
   public Expression getErrorIf() {
      if (fErrorIf == null) {
         return null;
      }
      return fErrorIf;
   }

   /**
    * Get index from variable
    * 
    * @return Index or -1 if not indexed
    */
   public int getIndex() {
      String name = getName();
      Pattern p = Pattern.compile("\\w+\\[(\\d+)\\]");
      Matcher m = p.matcher(name);
      if (!m.matches()) {
         if (name.contains("[")) {
            System.err.println("Fix me!");
         }
         return -1;
      }
      return Integer.parseInt(m.group(1));
   }

   public void addInternalListeners() throws Exception {
      
//      if (getKey().contains("osc_cr_range")) {
//         System.err.println("Found it "+getKey());
//      }
      if (fReference != null) {
         fReference.addListener(this);
      }
      if (fEnabledBy != null) {
         fEnabledBy.addListener(this);
      }
      if (fErrorIf != null) {
         fErrorIf.addListener(this);
      }
      if (fUnlockedBy != null) {
         fUnlockedBy.addListener(this);
      }
      if (fpinMapEnable != null) {
         fpinMapEnable.addListener(this);
      }
      if (fHiddenBy != null) {
         fHiddenBy.addListener(this);
      }
   }

   /**
    * Update variable from calculated info + local modifiers (enabledBy, lockedIf, errorIf etc)
    * 
    * @param info       Original information for update (will be modified as above)
    * 
    * @throws Exception
    */
   void update(VariableUpdateInfo info) throws Exception {
//      Boolean foundIt = false;
//      if (this.getName().contains("mcgClockMode[1]")) {
//         foundIt = true;
//      }
//      if (foundIt) {
//         System.err.println("Found it, Inherited status change " + info.status);
//      }
      
      if (fUnlockedBy != null) {
         setLocked(fUnlockedBy.getValueAsBoolean());
      }
      if (fHiddenBy != null) {
         Boolean hidden = fHiddenBy.getValueAsBoolean();
         setHidden(hidden);

         // Cumulative enable
         info.enable = info.enable && !hidden;
      }
      if (fEnabledBy != null) {
         // Cumulative enable
         Boolean enabled = fEnabledBy.getValueAsBoolean();
         info.enable = info.enable && enabled;
         if (!enabled) {
            info.status = new Status(fEnabledBy.getMessage("Disabled by "), Severity.OK);
         }
      }
      enable(info.enable);
      
      if (fErrorIf != null) {
         if  (fErrorIf.getValueAsBoolean()) {
            // Forced error status
            info.status = new Status(fErrorIf.getMessage("Error "));
//            if (foundIt) {
//               System.err.println("Found it - forced status change " + info.status);
//            }
         }
      }
      setStatus(info.status);

//      if ((info.value != null)&&(info.origin != null)) {
//         setValueAndOrigin(info.value, info.origin);
//      }
//      else {
         if (info.value != null) {
            setValue(info.value);
         }
         if (info.origin != null) {
            setOrigin(info.origin);
         }
//      }
//      if (foundIt) {
//         System.err.println("Found it - status " + info.status);
//      }
   }
   
   @Override
   public void expressionChanged(Expression expression) {
//      if (this.getName().contains("mcgClockMode[1]==VLPR")) {
//         System.err.println("Found it ");
//      }
      try {
         VariableUpdateInfo info = new VariableUpdateInfo();

         // Assume enabled (may be later disabled by enabledBy etc.)
         info.enable = true;

         determineReferenceUpdate(info, getReference());
         update(info);

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Determine updates from the reference.  This includes:
    *  <li> .value  = The value of expression
    *  <li> .status = Status from primary variable in expression
    *  <li> .enable = Enable from primary variable in expression
    *  <li> .origin = Origin from expression
    * 
    * @param info Cumulative variable update information from evaluating expression etc.
    *        <li>Always updated if reference present: .value, .origin
    *        <li>May be updated if reference present: .status, .enable
    * 
    * @param expression Expression controlling updates
    * 
    * @return
    * <li>true  Updates are necessary
    * <li>false No updates needed
    * 
    * @throws Exception
    */
   protected boolean determineReferenceUpdate(VariableUpdateInfo info, Expression expression) throws Exception {

      if (expression != null) {
         info.value = expression.getValue();

//         Variable primaryVariableInExpression = expression.getPrimaryVar();

//         if (primaryVariableInExpression != null) {
//            // Get status and enable from primary variable
//            info.status   = primaryVariableInExpression.getStatus();
//            info.enable   = primaryVariableInExpression.isEnabled();
//         }
         info.origin   = expression.getOriginMessage();
         
         // Update needed
         return true;
      }
      // No update needed
      return false;
   }

   /**
    * Set the active pin mapping Signal => Pin
    * 
    * @param signalName Name of signal
    * @param pinName    Name of pin
    */
   protected void setActivePinMapping(String signalName, String pinName) {
      try {
         Signal signal = getProvider().getDeviceInfo().findSignal(signalName);
         if ((pinName==null)||(pinName.isBlank())) {
            signal.setMappedPin(MappingInfo.UNASSIGNED_MAPPING);
         }
         else {
            Pin pin = null;
            if (pinName.equals("*")) {
               pin = signal.getOnlyMappablePin();
               if (pin == null) {
                  throw new Exception("Can't use '*' if no or multiple pins available for mapping");
               }
            }
            else {
               pin = getProvider().getDeviceInfo().findPin(pinName);
            }
            signal.mapPin(pin);
         }
      } catch (Exception e) {
         System.err.println("Signal mapping change failed for " + signalName + ", reason=" + e.getMessage());
      }
   }

   /**
    * Set active pin mappings
    * 
    * @param activePinMap list of pin mappings e.g. 'SWD_DIO,PTA4;SWD_CLK,PTC4'
    */
   protected void setActivePinMappings(String activePinMap) {
      if (activePinMap == null) {
         return;
      }
      String[] pinMaps = activePinMap.split(";");
      for (String pinMapEntry:pinMaps) {

         // Signal => pin
         String[] map = pinMapEntry.split(",");
         if (map.length<2) {
            setActivePinMapping(map[0], null);
         }
         else {
            setActivePinMapping(map[0], map[1]);
         }
      }
   }
   
   /**
    * Set active pin mappings
    * 
    * @param activePinMap list of pin mappings e.g. 'SWD_DIO,PTA4;SWD_CLK,PTC4'
    */
   protected void releaseActivePinMappings(String activePinMap) {
      if (activePinMap == null) {
         return;
      }
      String[] pinMaps = activePinMap.split(";");
      for (String pinMapEntry:pinMaps) {

         // Signal => pin
         String[] map = pinMapEntry.split(",");
         setActivePinMapping(map[0], null);
      }
   }
   
   /**
    * Set disabled pin-map
    * 
    * @param pinMap  Pin mapping string with optional enable expression e.g. 'I2C0_SCL,PTA3;I2C0_SDA,PTA2#/I2C0/enablePeripheralSupport'
    * @throws Exception
    */
   public void setDisabledPinMap(String pinMap) throws Exception {
      String[] t = pinMap.split("#");
      fDisabledPinMap = t[0];
      if (t.length>1) {
         setPinMapEnable(t[1]);
      }
   }
   
   /**
    * Get disabled pin-map
    * 
    * @return Pin-map to use when disabled.  null if none.
    */
   public String getDisabledPinMap() {
      return fDisabledPinMap;
   }

   /**
    * Set when pin-map is enabled
    * 
    * @param pinMap
    * @throws Exception
    */
   public void setPinMapEnable(String pinMapEnable) throws Exception {
      fpinMapEnable = new Expression(pinMapEnable, fProvider);
   }
   
   /**
    * Get when pin-map is enabled
    * 
    * @return pinMap enable expression
    */
   public Expression getPinMapEnable() {
      return fpinMapEnable;
   }
   
   /**
    * Used to create arbitrary variable from strings
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
   public static Variable createConstantWithNamedType(String name, String key, String type, Object value) throws Exception {
      
      Variable var = null;
      type = "net.sourceforge.usbdm.deviceEditor.information."+type;
      try {
         Class<?> varClass = Class.forName(type);
         Constructor<?> constructor = varClass.getConstructor(String.class, String.class, Object.class);
         var = (Variable) constructor.newInstance(name, key, value);
      } catch (Exception e) {
         // Most likely reason
         throw new Exception("Failed to create variable with type '" + type + "'", e);
      }
      var.setDerived(true);
      var.setConstant();
      return var;
   }

}