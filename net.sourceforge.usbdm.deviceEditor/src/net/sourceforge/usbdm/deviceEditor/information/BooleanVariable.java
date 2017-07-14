package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BooleanVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class BooleanVariable extends Variable {
   
   private Pair fTrue  = new Pair("true",  "true");
   private Pair fFalse = new Pair("false", "false");
   
   /** Value of variable */
   private boolean fValue = false;
   
   /** Default value of variable */
   private boolean fDefault = false;
   
   /**
    * Construct a variable representing a boolean value
    * 
    * @param name
    * @param value
    */
   public BooleanVariable(String name, String key) {
      super(name, key);
   }

   /**
    * Sets variable value<br>
    * Listeners are informed if the variable changes
    * 
    * @param value The value to set
    * 
    * @return True if variable actually changed value
    */
   public boolean setValue(Boolean value) {
      if (fValue == (boolean)value) {
         return false;
      }
      super.debugPrint("BooleanVariable["+this+"].setValue("+value+"), old "+value);
      fValue = value;
      notifyListeners();
      return true;
   }
   
   /**
    * Convert object to suitable type for this variable
    * 
    * @param value
    * 
    * @return Converted object
    */
   private boolean translate(Object value) {
      if (value instanceof Boolean) {
         return (Boolean)value;
      }
      if (value instanceof Integer) {
         return (Integer)value != 0;
      }
      if (value instanceof Long) {
         return (Long)value != 0;
      }
      if (value instanceof String) {
         String sValue = value.toString();
         return (sValue.equalsIgnoreCase(fTrue.name))||
                (sValue.equalsIgnoreCase("true"))||
                (sValue.equalsIgnoreCase("1"));
      }
      throw new RuntimeException("Object "+ value + "(" + value.getClass()+") Not compatible with BooleanVariable");
   }
   
   @Override
   public boolean setValue(Object value) {
      return setValue(translate(value));
   }

   @Override
   public void setDefault(Object value) {
      fDefault = translate(value);
   }
   
   @Override
   public Object getDefault() {
      return fDefault;
   }

   @Override
   public boolean getValueAsBoolean() {
      return isEnabled()?fValue:fDefault;
   }

   @Override
   public String getSubstitutionValue() {
      return getValueAsBoolean()?fTrue.value:fFalse.value;
   }

   @Override
   public String getValueAsString() {
      return getValueAsBoolean()?fTrue.name:fFalse.name;
   }

   @Override
   public long getValueAsLong() {
      return getValueAsBoolean()?1:0;
   }

   /**
    * Get the name/value representing the TRUE value
    * 
    * @return name/value for TRUE value
    */
   public Pair getTrueValue() {
      return fTrue;
   }

   /**
    * Set the name/value representing the TRUE value
    * 
    * @param trueValue name/value for TRUE value
    */
   public void setTrueValue(Pair trueValue) {
      this.fTrue = trueValue;
   }

   /**
    * Get the name/value representing the FALSE value
    * 
    * @return the name/value for FALSE value
    */
   public Pair getFalseValue() {
      return fFalse;
   }

   /**
    * Set the name/value representing the FALSE value
    * 
    * @param name/value for FALSE value
    */
   public void setFalseValue(Pair falseValue) {
      this.fFalse = falseValue;
   }

   @Override
   public VariableModel createModel(BaseModel parent) {
      return new BooleanVariableModel(parent, this);
   }

   @Override
   public void setValueQuietly(Object value) {
      fValue = translate(value);
   }

   @Override
   public String getPersistentValue() {
      return Boolean.toString(fValue);
   }

   @Override
   public void setPersistentValue(String value) {
      fValue = translate(value);
   }

   @Override
   public boolean getRawValueAsBoolean() {
      return fValue;
   }

   public void getChoices() {
   }

}
