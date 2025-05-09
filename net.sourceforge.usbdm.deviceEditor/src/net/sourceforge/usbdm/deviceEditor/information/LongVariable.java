package net.sourceforge.usbdm.deviceEditor.information;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.LongVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression.VariableUpdateInfo;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class LongVariable extends Variable {
   
   /** Minimum permitted value (user view) */
   private Long fMin    = null;
   
   /** Maximum permitted value (user view) */
   private Long fMax    = null;

   /** Maximum permitted value as expression */
   private Expression fMinExpression = null;

   /** Minimum permitted value as expression */
   private Expression fMaxExpression = null;
   
   /** Step size value */
   private long fStep   = 1;

   /** Offset used when mapping value from user -> substitution */
   private long fOffset = 0;

   /** Units of the quantity the variable represents e.g. Frequency => Hz */
   private Units fUnits = Units.None;

   /** Value of variable */
   private long fValue = 0;
   
   /** Default value of variable */
   private Long fDefaultValue = null;
   
   /** Disabled value of variable */
   private long fDisabledValue = 0;
   
   /** Radix for displaying numbers */
   private int fRadix = 10;
   
   /**
    * Constructor
    * 
    * @param provider   Provider holding this variable
    * @param name       Name to display to user. (If null then default value is derived from key).
    * @param key        Key for variable.
    */
   public LongVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }
   
   /**
    * Constructor
    * 
    * @param provider   Provider holding this variable
    * @param name       Name to display to user. (If null then default value is derived from key).
    * @param key        Key for variable.
    * @param value      Initial value and default
    */
   public LongVariable(VariableProvider provider, String name, String key, Object value) {
      super(provider, name, key);
      setValue(value);
      setDefault(value);
   }
   
   @Override
   public String toString() {
      return String.format("Variable(Name=%s, key=%s, value=%s (%s)", getName(), getKey(), getSubstitutionValue(), getValueAsString());
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new LongVariableModel(parent, this);
   }
   
   private Object translationCache       = new Object();
   private Long   translationCachedValue = null;
   
   /**
    * Convert object to suitable type for this variable
    * 
    * @param value
    * 
    * @return Converted object or null if failed
    */
   private Long translate(Object value) {
      if (translationCache == value) {
         return translationCachedValue;
      }
      translationCache = value;
      
      if (value instanceof Double) {
         translationCachedValue = Math.round((Double) value);
         return translationCachedValue;
      }
      if (value instanceof Long) {
         translationCachedValue = (Long) value;
         return translationCachedValue;
      }
      if (value instanceof Integer) {
         translationCachedValue = ((Integer)value).longValue();
         return translationCachedValue;
      }
      if ((value instanceof Boolean) && (fOffset == 0)) {
         translationCachedValue = ((Boolean) value)?1L:0L;
         return translationCachedValue;
      }
      if (value instanceof String) {
         Double numericValue = EngineeringNotation.parse((String) value);
         if (numericValue != null) {
            Units fromUnits = EngineeringNotation.parseUnits((String) value);
            Units toUnits = getUnits();
            if ((fromUnits == toUnits)||(fromUnits == Units.None)) {
               // No translation needed
               translationCachedValue = Math.round(numericValue);
               return translationCachedValue;
            }
            Double translatedValue = null;
            switch(toUnits) {
            case Hz:
               if (fromUnits == Units.s) {
                  translatedValue = 1.0/numericValue;
               }
               break;
            case s:
               if (fromUnits == Units.Hz) {
                  translatedValue = 1.0/numericValue;
               }
               break;
            case ticks:
               // Wrong units and no translation
               break;
            case None:
            case percent:
               // No translation
               translationCachedValue = Math.round(numericValue);
               return translationCachedValue;
            default:
               break;
            }
            if (translatedValue != null) {
               translationCachedValue = Math.round(translatedValue);
               return translationCachedValue;
            }
         }
      }
//      System.err.println("Object '"+ value + "' (" + value.getClass()+") is not compatible with LongVariable or "+getUnits());
      translationCache = null;
      return translationCachedValue;
   }
   
   /**
    * Set variable value as long
    * 
    * @param value Value to set
    * 
    * @return True if variable actually changed value
    */
   public boolean setValueQuietly(Long value) {
//      System.err.println(getName()+".setValueQuietly("+value+")");
      if ((value == null)||(fValue == value)) {
         return false;
      }
      fValue = value;
      
      // Update range checks
      setStatusQuietly(isValid());

      return true;
   }
   
   @Override
   public boolean setValueQuietly(Object value) {
      if (fLogging) {
         System.err.println(getName()+".setValueQuietly(L:"+value+")");
      }
      Long res = translate(value);
      return setValueQuietly(res);
   }
   
   @Override
   public void setPersistentValue(String value) {
      setValueQuietly(value);
   }
   
   /**
    * Formats the value as a string for GUI
    * 
    * @param value Value to format
    * @param units Units to use for formatting
    * 
    * @return String in appropriate form e.g. 24.56MHz
    */
   public static String formatValueAsString(double value, Units units) {
      return formatValueAsString(value, units, 10);
   }
   
   /**
    * Formats the value as a string for GUI
    * 
    * @param value Value to format
    * @param units Units to use for formatting
    * @param radix Radix to use for simple values
    * 
    * @return String in appropriate form e.g. 24.56MHz
    */
   public static String formatValueAsString(double value, Units units, int radix) {
      
      int sigDigits = 4;
      switch(units) {
      default:
      case None:
         if (radix == 16) {
            return "0x"+Long.toHexString(Math.round(value));
         }
         if (radix == 2) {
            return "0b"+Long.toBinaryString(Math.round(value));
         }
         return Long.toString(Math.round(value));
      case ticks:
         return Long.toString(Math.round(value))+"_"+units.toString();
      case s:
      case Hz:
         if (value <= 1) {
            sigDigits = 1;
         }
         else if (value <= 10) {
            sigDigits = 2;
         }
         else if (value <= 100) {
            sigDigits = 3;
         }
         else if (value <= 1000) {
            sigDigits = 4;
         }
         return EngineeringNotation.convert(value, sigDigits).toString()+units.toString();
      }
   }

   @Override
   public String formatValueForRegister(String paramName) {
      String valueFormat = getValueFormat();
      if (valueFormat != null) {
         return String.format(valueFormat, paramName);
      }
      return paramName;
   }
   
   @Override
   public String fieldExtractFromRegister(String registerValue) {
      String valueFormat = getValueFormat();
      String returnType  = getReturnType();
      boolean hasOuterBrackets = false;
      if ((valueFormat != null) && !valueFormat.matches("^\\(?%s\\)?$")) {
         String parts[] = valueFormat.split(",");
         StringBuilder sb = new StringBuilder();
         boolean needsBrackets = false;
         for(String format:parts) {
            Pattern p = Pattern.compile("^([a-zA-Z0-9_]*)\\(?\\%s\\)?");
            Matcher m = p.matcher(format);
            if (!m.matches()) {
               return "Illegal use of formatParam - unexpected pattern '"+valueFormat+"'";
            }
            String macro = m.group(1);
            if (!sb.isEmpty()) {
               sb.append("|");
               needsBrackets = true;
            }
            sb.append(String.format("((%s&%s_MASK)>>%s_SHIFT)",registerValue, macro, macro));
         }
         registerValue = sb.toString();
         if (needsBrackets) {
            registerValue = "("+registerValue+")";
         }
         hasOuterBrackets = true;
      }
      if (returnType != null) {
         if (!hasOuterBrackets) {
            registerValue = "("+registerValue+")";
         }
         registerValue = String.format("%s%s", returnType, registerValue);
      }
//      if (foundIt) {
//         System.err.println(" => " + registerValue);
//      }
      return registerValue;
   }
   
   /**
    * Converts the given string into a form appropriate for model
    * 
    * @param value Value to format
    * 
    * @return String in appropriate form e.g. 100Hz (10.0ms)
    */
   public String formatValueAsString(long value) {

      double frequency = 1;
      double period    = 1;
      switch(getUnits()) {
      case Hz:
         // Primary value is frequency
         frequency = value;
         if (frequency==0) {
            return formatValueAsString(frequency, Units.Hz);
         }
         return
               formatValueAsString(frequency, Units.Hz) + " (" +
               DoubleVariable.formatValueAsString(1/frequency, Units.s,4) + ")";
      case s:
         // Primary value is period
         period    = value;
         if (period==0) {
            return formatValueAsString(period, Units.s);
         }
         return
               formatValueAsString(period, Units.s) + " (" +
               DoubleVariable.formatValueAsString(1/period, Units.Hz,4) +")";
      case ticks:
         return formatValueAsString(value, Units.ticks);
      case None:
      default:
         if (fRadix != 10) {
            return formatValueAsString(value, fUnits, fRadix) +
                   " ("+Long.toString(value)+")";
         }
         return formatValueAsString(value, fUnits, fRadix);
      }
   }

   @Override
   public String getEditValueAsString() {
      return formatValueAsString(getValueAsLong(), getUnits(), fRadix);
   }

   @Override
   public String getValueAsString() {
      return formatValueAsString(getValueAsLong());
   }
   
   @Override
   public String getValueAsBriefString() {
      return formatValueAsString(getValueAsLong(), getUnits());
   }
   
   @Override
   public Boolean getValueAsBoolean() {
      return getValueAsLong() != 0;
   }
   
   @Override
   public double getValueAsDouble() {
      return getValueAsLong();
   }

   @Override
   public long getValueAsLong() {
      return isEnabled()?fValue:fDisabledValue;
   }

   @Override
   public String getSubstitutionValue() {
      if (fRadix == 16) {
         return "0x"+Long.toString(getValueAsLong()+fOffset, fRadix);
      }
      return Long.toString(getValueAsLong()+fOffset);
   }

   @Override
   public String formatUsageValue(String value) {
      
      Units units = getUnits();
      String typeName = getTypeName();
      if (typeName != null) {
         // Don't provides cast to (signed) int
         Pattern pSpecial = Pattern.compile("(signed(\\sint)?)|(int)");
         Matcher mSpecial = pSpecial.matcher(typeName);
         if (!mSpecial.matches()) {
            return typeName+"("+value+")";
         }
      }
      if (units != Units.None) {
         return units.append(value);
      }
      return value;
   }
   
   @Override
   public String getUsageValue() {
      
      return formatUsageValue(getSubstitutionValue());
   }
   
   @Override
   public String getDefaultParameterValue() {
      if (fDefaultValue == null) {
         return "no_default";
      }
      Units units = getUnits();
      if (units != Units.None) {
         return units.append(Long.toString(fDefaultValue+fOffset));
      }
      String rv;
      if (fRadix == 16) {
         rv = "0x"+Long.toString(getValueAsLong()+fOffset, fRadix);
      }
      else {
         rv = Long.toString(getValueAsLong()+fOffset);
      }
      String typeName = getTypeName();
      if (typeName != null) {
         return typeName+"("+rv+")";
      }
      return rv;
   }
   
   @Override
   public String getPersistentValue() {
      return Long.toString(fValue);
   }

   @Override
   public long getRawValueAsLong() {
      return  fValue;
   }

   /**
    * {@inheritDoc}
    * 
    * @param disabledValue Value to set. May be null to have no effect. <br>
    * Accepts Integer/Long/Double (rounded) or String
    */
   @Override
   public void setDisabledValue(Object disabledValue) {
      Long res = translate(disabledValue);
      if (res == null) {
         System.err.println("Failed to translate value '"+disabledValue+"'");
      }
      setDisabledValue(res);
   }

   /**
    * Set value used when disabled
    * 
    * @param fDisabledValue
    */
   public void setDisabledValue(Long disabledValue) {
      if (disabledValue == null) {
         return;
      }
      this.fDisabledValue = disabledValue;
   }

   /**
    * Get value used when disabled
    * 
    * @return
    */
   public long getDisabledValue() {
      return fDisabledValue;
   }
   
   @Override
   public void setDefault(Object value) {
      Long res = translate(value);
      defaultHasChanged = (fDefaultValue != null) && (fDefaultValue != res);
      fDefaultValue = res;
   }
   
   @Override
   public Object getDefault() {
      return fDefaultValue;
   }
   
   @Override
   public boolean isDefault() {
      return !defaultHasChanged && ((Long)fValue).equals(fDefaultValue);
   }

   /**
    * Checks if the value is valid for assignment to this variable
    * 
    * @param value
    * 
    * @return Error message or null if valid
    */
   public String isValid(Long value) {
      try {
         if (value<getMin()) {
            return "Value too small ["+getName()+"<"+formatValueAsString(getMin())+"]";
         }
         if (value>getMax()) {
            return "Value too large ["+getName()+">"+formatValueAsString(getMax())+"]";
         }
      } catch (Exception e) {
         return e.getMessage();
      }
      long remainder = value % getStep();
      if (remainder != 0) {
         return "Value not a multiple of " + getStep();
      }
      return null;
   }

   @Override
   public String isValid(String value) {
      
      Long lValue = translate(value);
      if (lValue != null) {
         return null;
      }
      return "Invalid value";
   }
   
   @Override
   public String isValid() {
      return isValid(fValue);
   }

   /*
    * Special operations
    */
   @Override
   public String getDisplayToolTip() {

      StringBuffer sb = new StringBuffer();
      sb.append(super.getDisplayToolTip());
      boolean newLineNeeded = sb.length()>0;
      
      try {
         if (getMin() != Long.MIN_VALUE) {
            if (newLineNeeded) {
               sb.append("\n");
               newLineNeeded = false;
            }
            sb.append("min="+formatValueAsString(getMin())+" ");
         }
         if (getMax() != Long.MAX_VALUE) {
            if (newLineNeeded) {
               sb.append("\n");
               newLineNeeded = false;
            }
            sb.append("max="+formatValueAsString(getMax())+" ");
         }
      } catch (Exception e) {
         return "ERROR: " + e.getMessage();
      }
      if (getStep() != 1) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("step="+formatValueAsString(getStep())+" ");
      }
      return (sb.length() == 0)?null:sb.toString();
   }

   //============== MIN/MAX handling ==================
   /**
    * Set minimum value.<br>
    * Status listeners are informed of any change.
    * 
    * @param min Minimum value
    * @throws Exception
    */
   public void setMin(long min) throws Exception {
      
      // Check if error state changed
      boolean oldError = (fValue<getMin()) || (fValue>getMax());
      boolean newError = (fValue<min)      || (fValue>getMax());
      
      boolean statusChanged = oldError != newError;
      fMin = min;
      if ((fDefaultValue == null) || (fDefaultValue<fMin)) {
         setDefault(fMin);
      }
      if (fDisabledValue<fMin) {
         fDisabledValue = fMin;
      }
      if (statusChanged) {
         notifyListeners();
      }
   }

   /**
    * Set maximum value.<br>
    * Status listeners are informed of any change.
    * 
    * @param max Maximum value
    * @throws Exception
    */
   public void setMax(long max) throws Exception {
      
      // Check if error state changed
      boolean oldError = (fValue<getMin()) || (fValue>getMax());
      boolean newError = (fValue<getMin()) || (fValue>max);
      
      boolean statusChanged = oldError != newError;
      fMax = max;
      if ((fDefaultValue == null) ||(fDefaultValue>fMax)) {
         setDefault(fMax);
      }
      if (fDisabledValue>fMax) {
         fDisabledValue = fMax;
      }
      if (statusChanged) {
         notifyListeners();
      }
   }

   /**
    * Set maximum value as expression.
    * 
    * @param max Maximum value as expression
    * 
    * @throws Exception
    */
   public void setMin(Object attribute) throws Exception {
      
      if (attribute instanceof Expression) {
         fMinExpression = (Expression) attribute;
      }
      else {
         fMinExpression = new Expression(attribute.toString(), getProvider());
      }
      
      // Initially assume dynamic evaluation
      fMin = null;
   }

   /**
    * Set maximum value as expression.
    * 
    * @param max Maximum value as expression
    * 
    * @throws Exception
    */
   public void setMax(Object attribute) throws Exception {
      
      if (attribute instanceof Expression) {
         fMaxExpression = (Expression) attribute;
      }
      else {
         fMaxExpression = new Expression(attribute.toString(), getProvider());
      }
      
      // Initially assume dynamic evaluation
      fMax = null;
   }
   
   /**
    * Get minimum value
    * 
    * @return Minimum value
    * @throws Exception
    */
   public Long getMin() throws Exception {
      if (fMin == null) {
         if (fMinExpression != null) {
            fMin = fMinExpression.getValueAsLong();
         }
         else {
            fMin = Long.MIN_VALUE;
         }
      }
      return fMin;
   }

   /**
    * Get maximum value
    * 
    * @return Maximum value in user format
    * @throws Exception
    */
   public long getMax() throws Exception {
      if (fMax == null) {
         if (fMaxExpression != null) {
            fMax = fMaxExpression.getValueAsLong();
         }
         else {
            fMax = Long.MAX_VALUE;
         }
      }
      return fMax;
   }

   /**
    * Sets expression for dynamic min value
    * 
    * @param expression
    */
   public void setMinExpression(Expression expression) {
      fMinExpression = expression;
   }
   
   /**
    * Sets expression for dynamic max value
    * 
    * @param expression
    */
   public void setMaxExpression(Expression expression) {
      fMaxExpression = expression;
   }
   
   /**
    * Gets expression for dynamic min value
    * 
    * @return Expression or null if not dynamic
    */
   public Expression getMinExpression() {
      return fMinExpression;
   }
   
   /**
    * Gets expression for dynamic max value
    * 
    * @return Expression or null if not dynamic
    */
   public Expression getMaxExpression() {
      return fMaxExpression;
   }
   
   /**
    * Set set step size value
    * 
    * @param step Step size
    */
   public void setStep(long step) {
      fStep = step;
   }

   /**
    * Get step size value
    * 
    * @return Step size
    */
   public long getStep() {
      return fStep;
   }

   /**
    * Set offset value
    * 
    * @param offset
    */
   public void setOffset(long offset) {
      fOffset = offset;
   }

   /**
    * Get offset value
    * 
    * @return Offset value
    */
   public long getOffset() {
      return fOffset;
   }

   /**
    * @param units The units to set
    */
   public void setUnits(Units units) {
      fUnits = units;
   }

   /**
    * @return the units
    */
   public Units getUnits() {
      return fUnits;
   }

   public int getRadix() {
      return fRadix;
   }

   public void setRadix(long radix) {
      fRadix = (int) radix;
   }

   @Override
   public Object getNativeValue() {
      return getValueAsLong();
   }
   
   /**
    * {@inheritDoc}<br>
    * <br>
    * Adds handling changes in:
    * <li> fMin
    * <li> fMax<br><br>
    */
   @Override
   public void update(VariableUpdateInfo info, Expression expression) {
      if (fLogging) {
         System.err.println("LongVariable.update");
      }
      boolean updateLimits = false;
      if (info.doFullUpdate || ((fMinExpression != null)&&(expression == fMinExpression))) {
         fMin = null;
         updateLimits = true;
      }
      if (info.doFullUpdate || ((fMaxExpression != null)&&(expression == fMaxExpression))) {
         fMax = null;
         updateLimits = true;
      }
      if (updateLimits && setStatusQuietly(isValid())) {
         if ((getErrorPropagate()==null)||getErrorPropagate().greaterThan(Severity.OK)) {
            info.properties |= IModelChangeListener.PROPERTY_STATUS;
         }
      }
      super.update(info, expression);
   }

   /**
    * {@inheritDoc}
    * 
    * <li>fMinExpression
    * <li>fMaxExpression
    */
   @Override
   public void addInternalListeners() throws Exception {
      if (fMinExpression != null) {
         fMinExpression.addListener(this);
      }
      if (fMaxExpression != null) {
         fMaxExpression.addListener(this);
      }
      super.addInternalListeners();
   }

   @Override
   public Object getValue() {
      return getValueAsLong();
   }
   
   @Override
   public String getParamType() {
      String paramType = getTypeName();
      if (paramType == null) {
         Units units = getUnits();
         if (units != Units.None) {
            return "const "+getUnits().getType()+"&";
         }
         paramType = super.getBaseType();
         if (paramType != null) {
            return paramType;
         }
         return "Long_no_type";
      }
      if (isIntegerTypeInC(paramType)) {
         // Return type unchanged
         return paramType;
      }
      return super.getParamType();
   }
   
   @Override
   public String getParamName() {
      String typeName = getTypeName();
      if (typeName == null) {
         Units units = getUnits();
         if (units != Units.None) {
            typeName = units.getType();
            if (typeName == null) {
               return "no_name";
            }
            return typeName.substring(0,1).toLowerCase()+typeName.substring(1);
         }
      }
      return super.getParamName();
   }

   @Override
   public String getReturnType() {
      String paramType = getParamType();
      Pattern p = Pattern.compile("^(const)?\\s+([a-zA-Z0-9]+)\\s*&?$");
      Matcher m = p.matcher(paramType);
      if (m.matches()) {
         return m.group(2);
      }
      return super.getReturnType();
   }

   @Override
   public String getBaseType() {
      String baseType = super.getBaseType();
      if (baseType != null) {
         return baseType;
      }
      return getReturnType();
   }


   @Override
   public boolean isZero() {
      return fValue == 0L;
   }
}
