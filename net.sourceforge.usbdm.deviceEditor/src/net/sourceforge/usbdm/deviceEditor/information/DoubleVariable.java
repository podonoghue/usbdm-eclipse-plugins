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

   /** Value of variable */
   private double fValue = 0;
   
   /** Default value of variable */
   private Double fDefaultValue = null;
   
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
   
   /**
    * Convert object to suitable type for this variable
    * 
    * @param value
    * 
    * @return Converted object
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
      return fDefaultValue;
   }
   
   /**
    * Set variable value as double
    * 
    * @param value Value to set
    * 
    * @return True if variable actually changed value
    */
   public boolean setValueQuietly(double value) {
      if (!isDerived()) {
         if (value>fMax) {
            value = fMax;
         }
         if (value<fMin) {
            value = fMin;
         }
      }
      if (fValue == value) {
         return false;
      }
      fValue = value;
      return true;
   }
   
   @Override
   public boolean setValueQuietly(Object value) {
      return setValueQuietly(translate(value));
   }
   
   @Override
   public void setPersistentValue(String value) {
      fValue = translate(value);
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
   public void setDisabledValue(Object value) {
      setDisabledValue(translate(value));
   }

   /**
    * Set value used when disabled
    * 
    * @param fDisabledValue
    */
   public void setDisabledValue(double disabledValue) {
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
      Double v = translate(value);
      defaultHasChanged = (fDefaultValue != null) && (fDefaultValue != v);
      fDefaultValue = v;
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
      
      if (getMin() != Double.NEGATIVE_INFINITY) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("min="+getValueAsString(getMin())+" ");
      }
      if (getMax() != Double.POSITIVE_INFINITY) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("max="+getValueAsString(getMax())+" ");
      }
      return (sb.length() == 0)?null:sb.toString();
   }

   /**
    * Set minimum value.<br>
    * Status listeners are informed of any change.
    * 
    * @param min Minimum value
    */
   public void setMin(double min) {
      boolean statusChanged = ((fValue>=fMin) && (fValue<min))||((fValue<fMin) && (fValue>=min));
      fMin = min;
      if ((fDefaultValue == null) || (fDefaultValue<fMin)) {
         setDefault(min);
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
   public double getMin() {
      return fMin;
   }

   /**
    * Set maximum value.<br>
    * Status listeners are informed of any change.
    * 
    * @param max Maximum value
    */
   public void setMax(double max) {
      boolean statusChanged = ((fValue<=fMax) && (fValue>max))||((fValue>fMax) && (fValue<=max));
      fMax = max;
      if ((fDefaultValue == null) || (fDefaultValue>fMax)) {
         setDefault(max);
      }
      if (statusChanged) {
         notifyStatusListeners();
      }
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
   
   
}
