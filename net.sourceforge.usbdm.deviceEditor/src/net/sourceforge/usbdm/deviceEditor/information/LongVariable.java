package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.LongVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class LongVariable extends Variable {
   
   private long SCALE_FACTOR = 10000000;
   
   public enum Units {None, Hz, s};
   
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

   @Override
   public long getValueAsLong() {
      return isEnabled()?fValue:fDefault;
   }

   @Override
   public String getSubstitutionValue() {
      return Long.toString(getValueAsLong()+fOffset);
   }

   @Override
   public String getValueAsString() {
      switch(getUnits()) {
      default:
      case None:
         return Long.toString(getValueAsLong());
      case Hz:
         return EngineeringNotation.convert(getValueAsLong(), 5)+getUnits().toString();
      case s:
         return EngineeringNotation.convert(getValueAsLong()/((double)SCALE_FACTOR), 5)+getUnits().toString();
      }
   }

   public long getScaleFactor() {
      return SCALE_FACTOR;
   }
   @Override
   public boolean getValueAsBoolean() {
      return getValueAsLong() != 0;
   }

   /**
    * Set value as long
    * 
    * @param value Value in user format
    * 
    * @return True if variable actually changed value
    */
   public boolean setValue(Long value) {
      if (fValue == value) {
         return false;
      }
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
   public long translate(Object value) {
      if (value instanceof Long) {
         return (Long) value;
      }
      if (value instanceof Integer) {
         return (long)(Integer) value;
      }
      if (value instanceof String) {
         switch(getUnits()) {
         default:
         case None:
            return EngineeringNotation.parseAsLong((String) value);
         case Hz:
            return EngineeringNotation.parseAsLong((String) value);
         case s:
            return EngineeringNotation.parseAsLong((String) value)*SCALE_FACTOR;
         }
      }
      if ((value instanceof Boolean) && (fOffset == 0)) {
         return ((Boolean) value)?1L:0L;
      }
      throw new RuntimeException("Object "+ value + "(" + value.getClass()+") Not compatible with LongVariable");
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
    * @param min Minimum value in user format
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
    * @param min Maximum value in user format
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
      if (getUnits() != Units.None) {
         return EngineeringNotation.convert(fValue, 5)+getUnits().toString();
      }
      return Long.toString(fValue);
   }

   @Override
   public void setPersistentValue(String value) {
      fValue = translate(value);
   }
}
