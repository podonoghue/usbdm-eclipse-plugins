package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.DoubleVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class DoubleVariable extends Variable {
   
   /** Minimum permitted value (user view) */
   private double fMin    = Double.NEGATIVE_INFINITY;
   
   /** Maximum permitted value (user view) */
   private double fMax    = Double.POSITIVE_INFINITY;

   /** Units of the quantity the variable represents e.g. Frequency => Hz */
   private Units fUnits = Units.None;

   /** Value in user format */
   private double fValue = 0;

   /** Default value of variable */
   private double fDefault = 0;
   
   /**
    * Constructor
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
   public DoubleVariable(String name, String key, String value) {
      super(name, key);
      setValue(value);
      setDefault(value);
   }

   @Override
   public double getValueAsDouble() {
      return isEnabled()?fValue:fDefault;
   }

   @Override
   public long getValueAsLong() {
      return Math.round(getValueAsDouble());
   }

   @Override
   public String getSubstitutionValue() {
      return Long.toString(getValueAsLong())+'D';
   }

   /**
    * Converts the given string into a form appropriate for model
    * 
    * @param value Value to format
    * 
    * @return String in appropriate form e.g. 24.56MHz
    */
   public String getValueAsString(double value) {
      switch(getUnits()) {
      default:
      case None:
         return Double.toString(value);
      case s:
      case Hz:
         return EngineeringNotation.convert(value, 5).toString()+getUnits().toString();
      }
   }

   @Override
   public String getValueAsString() {
      return getValueAsString(getValueAsDouble());
   }
   
   /**
    * Set value as double
    * 
    * @param value Value to set
    * 
    * @return True if variable actually changed value and listeners notified
    */
   public boolean setValue(double value) {
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
   private double translate(Object value) {
      try {
         if (value instanceof Double) {
            return (Double) value;
         }
         if (value instanceof Long) {
            return (Long) value;
         }
         if (value instanceof Integer) {
            return (Integer) value;
         }
         if (value instanceof String) {
            return EngineeringNotation.parse((String) value);
         }
         throw new RuntimeException("Object "+ value + "(" + value.getClass()+") Not compatible with DoubleVariable");
      } catch (Exception e) {
         e.printStackTrace();
      }
      return fDefault;
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
   public String toString() {
      return String.format("Variable(Name=%s, value=%s (%s)", getName(), getSubstitutionValue(), getValueAsString());
   }

   /**
    * Set minimum value
    * 
    * @param min Minimum value in user format
    */
   public void setMin(double min) {
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
   public void setMax(double max) {
      fMax = max;
      if (fDefault>fMax) {
         fDefault = fMax;
      }
   }

   /**
    * Get minimum value
    * 
    * @return Minimum value
    */
   public double getMin() {
      return fMin;
   }

   /**
    * Get maximum value
    * 
    * @return Maximum value in user format
    */
   public double getMax() {
      return fMax;
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
   public String isValid(double value) {   
      if (value<getMin()) {
         return "Value too small";
      }
      if (value>getMax()) {
         return "Value too large";
      }
      return null;
   }

   @Override
   public String isValid(String value) {
    double lValue = 0;
    try {
       lValue = EngineeringNotation.parse(value);
    }
    catch (NumberFormatException e) {
       return "Illegal number";
    }
    return isValid(lValue);
 }
   
   @Override
   public VariableModel createModel(BaseModel parent) {
      return new DoubleVariableModel(parent, this);
   }

   @Override
   public void setValueQuietly(Object value) {
      fValue = translate(value);
   }

   @Override
   public double getRawValueAsDouble() {
      return  fValue;
   }

   @Override
   public long getRawValueAsLong() {
      return  Math.round(fValue);
   }

   @Override
   public String getPersistentValue() {
      return Double.toString(fValue);
   }

   @Override
   public void setPersistentValue(String value) {
      fValue = translate(value);
   }
}
