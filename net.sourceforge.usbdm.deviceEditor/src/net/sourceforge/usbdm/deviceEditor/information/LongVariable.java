package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;

public class LongVariable extends Variable {
   
   public enum Units {None, Hz, Seconds};
   
   /** Minimum permitted value (user view) */
   private long fMin    = Long.MIN_VALUE;
   
   /** Maximum permitted value (user view) */
   private long fMax    = Long.MAX_VALUE;

   /** Step size value */
   private long fStep   = 1;
   
   /** Offset used when mapping value from user -> substitution */
   private long fOffset = 0;

   /** Units of the quantity the variable represents e.g. Frequency => Hz */
   private Units fUnits;

   /** Value in user format */
   private long  fValue;
   
   /**
    * Create a variable representing along quantity
    * 
    * @param name
    */
   public LongVariable(String name) {
      super(name);
   }

   @Override
   public String getSubstitutionValue() {
      return Long.toString(fValue-fOffset);
   }

   @Override
   public String getValueAsString() {
      if (getUnits() != Units.None) {
         return EngineeringNotation.convert(fValue, 5)+getUnits().toString();
      }
      return Long.toString(fValue);
   }

   @Override
   public long getValueAsLong() {
      return fValue;
   }

   @Override
   public boolean getValueAsBoolean() {
      return fValue != 0;
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
   public boolean setValue(Object value) {
      if (value instanceof Long) {
         return setValue((Long) value);
      }
      if (value instanceof Integer) {
         return setValue((long)(Integer) value);
      }
      if (value instanceof String) {
         return setValue(EngineeringNotation.parseAsLong((String) value));
      }
      if ((value instanceof Boolean) && (fOffset == 0)) {
         return setValue(((Boolean) value)?1L:0L);
      }
      throw new RuntimeException("Object "+ value + "(" + value.getClass()+") Not compatible with LongVariable");
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
   }

   /**
    * Set maximum value
    * 
    * @param min Maximum value in user format
    */
   public void setMax(long max) {
      fMax = max;
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
}
