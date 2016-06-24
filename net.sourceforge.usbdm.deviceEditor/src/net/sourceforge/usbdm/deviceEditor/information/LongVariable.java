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

   /** Value in user format */
   private long fValue = 0;

   /** Default value of variable */
   private long fDefault = 0;
   
   /**
    * Constructor
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
   public double getValueAsDouble() {
      return getValueAsLong();
   }

   @Override
   public long getValueAsLong() {
      return isEnabled()?fValue:fDefault;
   }

   @Override
   public String getSubstitutionValue() {
      return Long.toString(getValueAsLong()+fOffset);
   }

   /**
    * Converts the given string into a form appropriate for model
    * 
    * @param value Value to format
    * 
    * @return String in appropriate form e.g. 24.56MHz
    */
   public String getValueAsString(long value) {
      switch(getUnits()) {
      default:
      case None:
         return Long.toString(value);
      case s:
      case Hz:
         return EngineeringNotation.convert(value, 5).toString()+getUnits().toString();
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

   /**
    * Set value as long
    * 
    * @param value Value to set
    * 
    * @return True if variable actually changed value and listeners notified
    */
   public boolean setValue(Long value) {
      if (fValue == value) {
         return false;
      }
      super.debugPrint("LongVariable["+this+"].setValue("+value+"), old "+value);
      fValue = value;
      notifyListeners();
      return true;
   }

   @Override
   public void setDefault(Object value) {
      fDefault = translate(value);
   }

   /**
    * Convert object to required type
    * 
    * @param value
    * @return
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
            return EngineeringNotation.parseAsLong((String) value);
         }
         if ((value instanceof Boolean) && (fOffset == 0)) {
            return ((Boolean) value)?1L:0L;
         }
         throw new Exception("Object "+ value + "(" + value.getClass()+") Not compatible with LongVariable");
      } catch (Exception e) {
//         e.printStackTrace();
      }
      return fDefault;
   }

   @Override
   public boolean setValue(Object value) {
      return setValue(translate(value));
   }
   
   @Override
   public String toString() {
      return String.format("Variable(Name=%s, value=%s (%s)", getName(), getSubstitutionValue(), getValueAsString());
   }

   /**
    * Set minimum value
    * 
    * @param min Minimum value
    */
   public void setMin(long min) {
      fMin = min;
      if (fDefault<fMin) {
         fDefault = fMin;
      }
   }

   /**
    * Set maximum value
    * 
    * @param min Maximum value
    */
   public void setMax(long max) {
      fMax = max;
      if (fDefault>fMax) {
         fDefault = fMax;
      }
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
    * Set offset value
    * 
    * @param offset
    */
   public void setOffset(long offset) {
      fOffset = offset;
   }

   /**
    * Get minimum value
    * 
    * @return Minimum value in user format
    */
   public long getMin() {
      return fMin;
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
    * Get step size value
    * 
    * @return Step size
    */
   public long getStep() {
      return fStep;
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
    * @return the units
    */
   public Units getUnits() {
      return fUnits;
   }

   /**
    * @param units The units to set
    */
   public void setUnits(Units units) {
      fUnits = units;
   }

   @Override
   public String isValid() {
      return isValid(fValue);
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
   public VariableModel createModel(BaseModel parent) {
      return new LongVariableModel(parent, this);
   }

   @Override
   public void setValueQuietly(Object value) {
      fValue = translate(value);
   }

   @Override
   public long getRawValueAsLong() {
      return  fValue;
   }

   @Override
   public String getPersistentValue() {
      return Long.toString(fValue);
   }

   @Override
   public void setPersistentValue(String value) {
      fValue = translate(value);
   }
}
