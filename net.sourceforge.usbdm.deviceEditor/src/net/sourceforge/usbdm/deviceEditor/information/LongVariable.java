package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.LongVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.parsers.SimpleExpressionParser;
import net.sourceforge.usbdm.deviceEditor.parsers.SimpleExpressionParser.Mode;

public class LongVariable extends Variable {
   
   /** Minimum permitted value (user view) */
   private Long fMin    = Long.MIN_VALUE;
   
   /** Maximum permitted value (user view) */
   private Long fMax    = Long.MAX_VALUE;

   /** Maximum permitted value as expression */
   private String fMinExpression = null;

   /** Minimum permitted value as expression */
   private String fMaxExpression = null;
   
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
   public LongVariable(String name, String key, String value) {
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
   
   /**
    * Convert object to suitable type for this variable
    * 
    * @param value
    * 
    * @return Converted object
    */
   private long translate(Object value) {
      try {
         if (value instanceof Double) {
            return Math.round((Double) value);
         }
         if (value instanceof Long) {
            return (Long) value;
         }
         if (value instanceof Integer) {
            return (Integer) value;
         }
         if (value instanceof String) {
            if (((String) value).isEmpty()) {
               value = getDefault().toString();
            }
            return EngineeringNotation.parseAsLong((String) value);
         }
         if ((value instanceof Boolean) && (fOffset == 0)) {
            return ((Boolean) value)?1L:0L;
         }
         throw new Exception("Object "+ value + "(" + value.getClass()+") Not compatible with LongVariable");
      } catch (Exception e) {
//         Activator.log(e.getMessage());
      }
      return fDefaultValue;
   }
   
   @Override
   public boolean setValue(Object value) {
      try {
         return setValue(translate(value));
      } catch (Exception e) {
         e.printStackTrace();
      }
      return false;
   }
   
   @Override
   public void setValueQuietly(Object value) {
      fValue = translate(value);
   }

   @Override
   public void setPersistentValue(String value) {
      fValue = translate(value);
   }
   
   /**
    * Set variable value as long<br>
    * Listeners are informed if the variable changes
    * 
    * @param value Value to set
    * 
    * @return True if variable actually changed value and listeners notified
    */
   public boolean setValue(Long value) {
      
      if (fDebug) {
         System.err.println("LongVariable["+this+"].setValue("+value+"), old "+value);
      }
      if (fValue == value) {
         return false;
      }
      super.debugPrint("LongVariable["+this+"].setValue("+value+"), old "+value);
      fValue = value;
      notifyListeners();
      return true;
   }
   
   /**
    * Converts the given string into a form appropriate for model
    * 
    * @param value Value to format
    * 
    * @return String in appropriate form e.g. 24.56MHz
    */
   public String formatValueAsString(long value) {
      int sigDigits = 4;
      switch(getUnits()) {
      default:
      case None:
         if (fRadix == 16) {
            return "0x"+Long.toString(value, fRadix) + " (" + Long.toString(value) + ')';
         }
         return Long.toString(value);
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
         return EngineeringNotation.convert(value, sigDigits).toString()+getUnits().toString();
      }
   }

   @Override
   public String getValueAsString() {
      return formatValueAsString(getValueAsLong());
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
   public String getPersistentValue() {
      return Long.toString(fValue);
   }

   @Override
   public long getRawValueAsLong() {
      return  fValue;
   }

   @Override
   public void setDisabledValue(Object value) {
      setDisabledValue(translate(value));
   }

   /**
    * Set value used when disabled
    * 
    * @param fDisabledValue
    */
   public void setDisabledValue(long disabledValue) {
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
      Long v = translate(value);
      defaultHasChanged = (fDefaultValue != null) && (fDefaultValue != v);
      fDefaultValue = v;
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
    * @return Error message or null of valid
    * @throws Exception
    */
   public String isValid(Long value) {
      try {
         getUnits();
         getFormattedValue();
         if (value<getMin()) {
            return "Value too small [<"+formatValueAsString(getMin())+"]";
         }
         if (value>getMax()) {
            return "Value too large [>"+formatValueAsString(getMax())+"]";
         }
      } catch (Exception e) {
         return "ERROR: " + e.getMessage();
      }
      long remainder = value % getStep();
      if (remainder != 0) {
         return "Value not a multiple of " + getStep();
      }
      return null;
   }

   @Override
   public String isValid(String value) {
      long lValue = 0;
      try {
         lValue = Math.round(EngineeringNotation.parse(value));
      }
      catch (NumberFormatException e) {
         return "Illegal number";
      }
      try {
         return isValid(lValue);
      } catch (Exception e) {
         return "ERROR: " + e.getMessage();
      }
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
         fDefaultValue = fMin;
      }
      if (fDisabledValue<fMin) {
         fDisabledValue = fMin;
      }
      if (statusChanged) {
         notifyListeners();
//         notifyStatusListeners();
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
//         notifyStatusListeners();
      }
   }

   /**
    * Set maximum value as expression.<br>
    * Status listeners are informed of any change.
    * 
    * @param max Maximum value as expression
    * 
    * @return true => Dynamic expression
    * 
    * @throws Exception
    */
   public boolean setMin(String attribute) throws Exception {
      // Assume dynamic evaluation
      fMin = null;
      
      try {
         // Try as simply expression
         SimpleExpressionParser parser = new SimpleExpressionParser(null, Mode.EvaluateFully);
         fMin = (Long) parser.evaluate(attribute);
         // Remove dynamic expression
         fMaxExpression = null;
         notifyListeners();
         return false;
         
      } catch (Exception e) {
         // Assume evaluated dynamically - save for later
         fMinExpression = attribute;
      }
      // Dynamic expression
      return true;
   }

   /**
    * Set maximum value as expression.<br>
    * Status listeners are informed of any change.
    * 
    * @param max Maximum value as expression
    * 
    * @return true => Dynamic expression
    * 
    * @throws Exception
    */
   public boolean setMax(String attribute) throws Exception {
      // Assume dynamic evaluation
      fMax = null;
      
      // Try as simply expression
      SimpleExpressionParser parser = new SimpleExpressionParser(null, Mode.EvaluateFully);
      try {
         fMax = (Long) parser.evaluate(attribute);
         // Remove dynamic expression
         fMaxExpression = null;
         notifyListeners();
         return false;
      } catch (Exception e) {
         // Assume evaluated dynamically - save for later
         fMaxExpression = attribute;
      }
      // Dynamic expression
      return true;
   }
   
   /**
    * Get minimum value
    * 
    * @return Minimum value
    * @throws Exception
    */
   public long getMin() throws Exception {
      if (fMin == null) {
         SimpleExpressionParser parser = new SimpleExpressionParser(getProvider(), Mode.EvaluateFully);
         fMin = (Long) parser.evaluate(fMinExpression);
         notifyStatusListeners();
      }
      return fMin;
   }

   /**
    * Triggers update of minimum value on next use
    */
   public void updateMin() {
      if (fMinExpression != null) {
         fMin = null;
      }
   }

   /**
    * Triggers update of maximum value on next use
    */
   public void updateMax() {
      if (fMaxExpression != null) {
//         if (getName().contains("system_lpuart_clock")) {
//            System.err.println("updateMax() Found "+getName());
//         }
         fMax = null;
      }
   }

   /**
    * Get maximum value
    * 
    * @return Maximum value in user format
    * @throws Exception
    */
   public long getMax() throws Exception {
      if (fMax == null) {
//         if (getName().contains("system_lpuart_clock")) {
//            System.err.println("getMax() Found "+getName());
//         }
         SimpleExpressionParser parser = new SimpleExpressionParser(getProvider(), Mode.EvaluateFully);
         fMax = (Long) parser.evaluate(fMaxExpression);
         notifyStatusListeners();
      }
      return fMax;
   }

   /**
    * Gets expression for dynamic min value
    * 
    * @return Expression or null if not dynamic
    */
   public String getMinExpression() {
      return fMinExpression;
   }
   
   /**
    * Gets expression for dynamic max value
    * 
    * @return Expression or null if not dynamic
    */
   public String getMaxExpression() {
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

   public void setRadix(int radix) {
      fRadix = radix;
   }

   @Override
   public Object getNativeValue() {
      return getValueAsLong();
   }

}
