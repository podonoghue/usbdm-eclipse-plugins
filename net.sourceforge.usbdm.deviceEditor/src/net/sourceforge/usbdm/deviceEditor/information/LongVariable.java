package net.sourceforge.usbdm.deviceEditor.information;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.LongVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;

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
    * Construct a variable representing a long value
    * 
    * @param name Name to display to user.
    * @param key  Key for variable
    */
   public LongVariable(String name, String key) {
      super(name, key);
   }

   /**
    * Constructor
    * 
    * @param name  Name to display to user.
    * @param key   Key for variable
    * @param value Initial value and default
    */
   public LongVariable(String name, String key, Object value) {
      super(name, key);
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
   public String formatParam(String paramName) {
      String valueFormat = getValueFormat();
      if (valueFormat != null) {
         paramName = String.format(valueFormat, paramName);
      }
      return paramName;
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
   public Object getEditValueAsString() {
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
   public boolean getValueAsBoolean() {
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
   public String getUsageValue() {
      
      String value = getSubstitutionValue();
      Units units = getUnits();
      if (units != Units.None) {
         return units.append(value);
      }
      String typeName = getTypeName();
      if (typeName != null) {
         // Don't provides cast to (signed) int
         Pattern pSpecial = Pattern.compile("(signed(\\sint)?)|(int)");
         Matcher mSpecial = pSpecial.matcher(typeName);
         if (!mSpecial.matches()) {
            return typeName+"("+value+")";
         }
      }
      return value;
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
   public void setMin(String attribute) throws Exception {
      
      fMinExpression = new Expression(attribute, getProvider());
      
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
   public void setMax(String attribute) throws Exception {
      
      fMaxExpression = new Expression(attribute, getProvider());
      
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

//   /**
//    * Triggers update of minimum value on next use
//    */
//   public void updateMin() {
//      if (fMinExpression != null) {
//         fMin = null;
//      }
//   }
//
//   /**
//    * Triggers update of maximum value on next use
//    */
//   public void updateMax() {
//      if (fMaxExpression != null) {
//         fMax = null;
//      }
//   }

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
   
   @Override
   public boolean update(Expression expression) {
      
      boolean changed = super.update(expression);
      
      boolean updateLimits = false;
      if (expression == fMinExpression) {
         fMin = null;
         updateLimits = true;
      }
      if (expression == fMaxExpression) {
         fMax = null;
         updateLimits = true;
      }
      if (changed||updateLimits) {
         changed = setStatusQuietly(isValid()) || changed;
      }
      return changed;
   }

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

}
