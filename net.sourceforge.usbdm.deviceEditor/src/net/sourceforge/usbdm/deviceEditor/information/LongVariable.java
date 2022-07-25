package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.LongVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class LongVariable extends Variable {
   
   /** Minimum permitted value (user view) */
   private long fMin    = Long.MIN_VALUE;
   
   /** Maximum permitted value (user view) */
   private long fMax    = Long.MAX_VALUE;

   /** Step size value */
   private long fStep   = 1;

   /** Offset used when mapping value from user -> substitution */
   private long fOffset = 0;

   /** Units of the quantity the variable represents e.g. Frequency => Hz */
   private Units fUnits = Units.None;

   /** Value of variable */
   private long fValue = 0;
   
   /** Default value of variable */
   private long fDefaultValue = 0;
   
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
            return (long)(Integer) value;
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
         e.printStackTrace();
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
   public String getValueAsString(long value) {
      int sigDigits = 5;
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
      return getValueAsString(getValueAsLong());
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
      fDefaultValue = translate(value);
   }
   
   @Override
   public Object getDefault() {
      return fDefaultValue;
   }
   
   @Override
   public boolean isDefault() {
      return fValue == fDefaultValue;
   }

   /**
    * Checks if the value is valid for assignment to this variable
    * 
    * @param value
    * 
    * @return Error message or null of valid
    */
   public String isValid(Long value) {   
      if (value<getMin()) {
         return "Value too small";
      }
      if (value>getMax()) {
         return "Value too large";
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
      return isValid(lValue);
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
      
      if (getMin() != Long.MIN_VALUE) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("min="+getValueAsString(getMin())+" ");
      }
      if (getMax() != Long.MAX_VALUE) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("max="+getValueAsString(getMax())+" ");
      }
      if (getStep() != 1) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("step="+getValueAsString(getStep())+" ");
      }
      return (sb.length() == 0)?null:sb.toString();
   }

   /**
    * Set minimum value.<br>
    * Status listeners are informed of any change.
    * 
    * @param min Minimum value
    */
   public void setMin(long min) {
      boolean statusChanged = ((fValue>=fMin) && (fValue<min))||((fValue<fMin) && (fValue>=min));
      fMin = min;
      if (fDefaultValue<fMin) {
         fDefaultValue = fMin;
      }
      if (fDisabledValue<fMin) {
         fDisabledValue = fMin;
      }
      if (statusChanged) {
         notifyStatusListeners();
      }
   }

   /**
    * Get minimum value
    * 
    * @return Minimum value
    */
   public long getMin() {
      return fMin;
   }

   /**
    * Set maximum value.<br>
    * Status listeners are informed of any change.
    * 
    * @param max Maximum value
    */
   public void setMax(long max) {
      fMax = max;
      if (fDefaultValue>fMax) {
         fDefaultValue = fMax;
      }
      if (fDisabledValue>fMax) {
         fDisabledValue = fMax;
      }
      notifyStatusListeners();
   }

   /**
    * Get maximum value
    * 
    * @return Maximum value in user format
    */
   public long getMax() {
      return fMax;
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

}
