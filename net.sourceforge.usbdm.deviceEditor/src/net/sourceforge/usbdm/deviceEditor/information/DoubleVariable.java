package net.sourceforge.usbdm.deviceEditor.information;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.DoubleVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression.VariableUpdateInfo;

public class DoubleVariable extends Variable {
   
   /** Minimum permitted value (user view) */
   private Double fMin    = null;
   
   /** Maximum permitted value (user view) */
   private Double fMax    = null;

   /** Maximum permitted value as expression */
   private Expression fMinExpression = null;

   /** Minimum permitted value as expression */
   private Expression fMaxExpression = null;
   
   /** Units of the quantity the variable represents e.g. Frequency => Hz */
   private Units fUnits = Units.None;

   /** Value of variable */
   private double fValue = 0;
   
   /** Default value of variable */
   protected Double fDefaultValue = null;
   
   /** Disabled value of variable */
   private double fDisabledValue = 0;
   
   /**
    * Construct a variable representing a double value
    * 
    * @param name Name to display to user.
    * @param key  Key for variable
    */
   public DoubleVariable(String name, String key) {
      super(name, key);
   }

   /**
    * Constructor
    * 
    * @param name  Name to display to user.
    * @param key   Key for variable
    * @param value Initial value and default
    */
   public DoubleVariable(String name, String key, Object value) {
      super(name, key);
      setValue(value);
      setDefault(value);
   }
   
   @Override
   public String toString() {
      return String.format("Variable(Name=%s, value=%s (%s)", getName(), getSubstitutionValue(), getValueAsString());
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new DoubleVariableModel(parent, this);
   }
   
   private Object translationCache       = new Object();
   private Double translationCachedValue = null;
   
   /**
    * Convert object to suitable type for this variable
    * 
    * @param value
    * 
    * @return Converted object
    */
   private Double translate(Object value) {
      if (translationCache == value) {
         return translationCachedValue;
      }
      translationCache = value;
      
      if (value instanceof Double) {
         translationCachedValue = (Double) value;
         return translationCachedValue;
      }
      if (value instanceof Long) {
         Long res = (Long) value;
         translationCachedValue = res.doubleValue();
         return translationCachedValue;
      }
      if (value instanceof Integer) {
         translationCachedValue = ((Integer)value).doubleValue();
         return translationCachedValue;
      }
      if (value instanceof String) {
         Double numericValue = EngineeringNotation.parse((String) value);
         if (numericValue != null) {
            Units fromUnits = EngineeringNotation.parseUnits((String) value);
            Units toUnits   = getUnits();
            if ((fromUnits == toUnits)||((fromUnits == Units.None)&&(toUnits != Units.percent))) {
               // No translation needed
               translationCachedValue = numericValue;
               return translationCachedValue;
            }
            Double translatedValue = null;
            switch(toUnits) {
            case Hz:
               if (fromUnits == Units.s) {
                  translatedValue = 1/numericValue;
               }
               break;
            case s:
               if (fromUnits == Units.Hz) {
                  translatedValue = 1/numericValue;
               }
               break;
            case ticks:
               // Wrong units and no translation
               break;
            case percent:
               // No translation
               translatedValue = numericValue;
               if (!((String) value).contains("&")) {
                  translatedValue /= 100;
               }
               break;
            case None:
               // No translation
               translatedValue = numericValue;
               break;
            }
            if (translatedValue != null) {
               translationCachedValue = translatedValue;
               return translationCachedValue;
            }
         }
      }
//      System.err.println("Object '"+ value + "' (" + value.getClass()+") is not compatible with DoubleVariable or "+getUnits());
      translationCache = null;
      return translationCachedValue;
   }
   
   /**
    * Set variable value as double
    * 
    * @param value Value to set
    * 
    * @return True if variable actually changed value
    */
   public boolean setValueQuietly(Double value) {
      
      if ((value == null)||(fValue == value)) {
         return false;
      }
      fValue = value;
      
      return true;
   }
   
   @Override
   public boolean setValueQuietly(Object value) {
      Double res = translate(value);
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
    * @return String in appropriate form e.g. 100.0Hz (10.0ms)
    */
   public static String formatValueAsString(double value, Units units) {
      return formatValueAsString(value, units, 4);
   }

   /**
    * Formats the value as a string for GUI
    * 
    * @param value      Value to format
    * @param units      Units to use for formatting
    * @param sigDigits  Number of significant digits
    * 
    * @return String in appropriate form e.g. 100.0Hz (10.0ms)
    */
   public static String formatValueAsString(double value, Units units, int sigDigits) {
      switch(units) {
      default:
      case None:
         return Double.toString(value);
      case ticks:
         return Math.round(value)+"_"+units.toString();
      case percent:
         return String.format("%.2f%%", 100*value);
      case s:
      case Hz:
         return EngineeringNotation.convert(value, sigDigits).toString()+units.toString();
      }
   }

   public String formatValueAsString(double value) {

      if (!Double.isFinite(value)) {
         return "--";
      }
      double frequency = 1;
      double period    = 1;
      switch(getUnits()) {
      case Hz:
         // Primary value is frequency
         frequency = value;
         if (frequency == 0.0) {
            return formatValueAsString(frequency, Units.Hz);
         }
         return
               formatValueAsString(frequency, Units.Hz) + " (" +
               formatValueAsString(1/frequency, Units.s) + ")";
      case s:
         // Primary value is period
         period    = value;
         if (period == 0.0) {
            return
                  formatValueAsString(period, Units.s);
         }
         return
               formatValueAsString(period, Units.s) + " (" +
               formatValueAsString(1/period, Units.Hz) +")";
      case ticks:
         return formatValueAsString(value, Units.ticks);
      case percent:
         return formatValueAsString(value, Units.percent);
      case None:
      default:
         return Double.toString(value);
      }
   }

   @Override
   public String getValueAsString() {
      return formatValueAsString(getValueAsDouble());
   }
   
   @Override
   public String getValueAsBriefString() {
      return formatValueAsString(getValueAsDouble(), getUnits());
   }
   
   @Override
   public String getEditValueAsString() {
      return formatValueAsString(getValueAsDouble(), getUnits());
   }

   @Override
   public double getValueAsDouble() {
      return isEnabled()?fValue:fValue;
   }

   @Override
   public long getValueAsLong() {
      return Math.round(getValueAsDouble());
   }

   @Override
   public String getSubstitutionValue() {
//      return Long.toString(getValueAsLong())+'D';
      return Double.toString(getValueAsDouble());
   }

   @Override
   public String getPersistentValue() {
      return Double.toString(fValue);
   }

   @Override
   public long getRawValueAsLong() {
      return  Math.round(fValue);
   }

   @Override
   public double getRawValueAsDouble() {
      return  fValue;
   }

   @Override
   public void setDisabledValue(Object disabledValue) {
      Double res = translate(disabledValue);
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
   public void setDisabledValue(Double disabledValue) {
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
   public double getDisabledValue() {
      return fDisabledValue;
   }
   
   @Override
   public void setDefault(Object value) {
      Double res = translate(value);
      defaultHasChanged = (fDefaultValue != null) && (fDefaultValue != res);
      fDefaultValue = res;
   }
   
   @Override
   public Object getDefault() {
      return fDefaultValue;
   }
   
   @Override
   public boolean isDefault() {

      return !defaultHasChanged && ((Double)fValue).equals(fDefaultValue);
   }

   /**
    * Checks if the value is valid for assignment to this variable
    * 
    * @param value
    * 
    * @return Error message or null of valid
    */
   public String isValid(Double value) {
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
      return null;
   }

   @Override
   public String isValid(String value) {
      
      Double lValue = translate(value);
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
         if (getMin() != Double.NEGATIVE_INFINITY) {
            if (newLineNeeded) {
               sb.append("\n");
               newLineNeeded = false;
            }
            sb.append("min="+formatValueAsString(getMin())+" ");
         }
         if (getMax() != Double.POSITIVE_INFINITY) {
            if (newLineNeeded) {
               sb.append("\n");
               newLineNeeded = false;
            }
            sb.append("max="+formatValueAsString(getMax())+" ");
         }
      } catch (Exception e) {
         return "ERROR: " + e.getMessage();
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
   public void setMin(double min) throws Exception {
      
      // Check if error state changed
      boolean oldError = (fValue<getMin()) || (fValue>getMax());
      boolean newError = (fValue<min)      || (fValue>getMax());
      
      boolean statusChanged = oldError != newError;
      fMin = min;
      if ((fDefaultValue == null) || (fDefaultValue<fMin)) {
         setDefault(min);
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
   public void setMax(double max) throws Exception {

      // Check if error state changed
      boolean oldError = (fValue<getMin()) || (fValue>getMax());
      boolean newError = (fValue<getMin()) || (fValue>max);
      
      boolean statusChanged = oldError != newError;
      fMax = max;
      if ((fDefaultValue == null) || (fDefaultValue>fMax)) {
         setDefault(max);
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
    */
   public double getMin() throws Exception {
      if (fMin == null) {
         if (fMinExpression != null) {
            fMin = fMinExpression.getValueAsDouble();
         }
         else {
            fMin = Double.NEGATIVE_INFINITY;
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
   public double getMax() throws Exception {
      if (fMax == null) {
         if (fMaxExpression != null) {
            fMax = fMaxExpression.getValueAsDouble();
         }
         else {
            fMax = Double.POSITIVE_INFINITY;
         }
      }
      return fMax;
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

   @Override
   public Object getNativeValue() {
      return getValueAsDouble();
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      // TODO Auto-generated method stub
      return super.clone();
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
         info.properties |= IModelChangeListener.PROPERTY_STATUS;
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
   void update(VariableUpdateInfo info) throws Exception {
      super.update(info);
   }

   @Override
   public Object getValue() {
      return getValueAsDouble();
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
         return "Double_no_type";
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
//         System.err.println("Found it '"+paramType+"' => '"+m.group(2)+"'");
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

}
