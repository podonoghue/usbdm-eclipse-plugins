package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BooleanVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
 
public class BooleanVariable extends VariableWithChoices {
   
   private ChoiceData fTrue  = new ChoiceData("true",  "true");
   private ChoiceData fFalse = new ChoiceData("false", "false");
   
   /** Current value (user format i.e name) */
   private boolean fValue = false;
   
   /** Default value of variable */
   private boolean fDefaultValue = false;
   
   /** Disabled value of variable */
   private boolean fDisabledValue = false;
   
   /**
    * Construct a variable representing a boolean value
    * 
    * @param name Name to display to user.
    * @param key  Key for variable
    */
   public BooleanVariable(String name, String key) {
      super(name, key);
   }
   
   @Override
   public String toString() {
      return String.format("Variable(Name=%s, value=%s (%s))", getName(), getSubstitutionValue(), getValueAsString());
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new BooleanVariableModel(parent, this);
   }
   
   /**
    * Convert object to suitable type for this variable
    * 
    * @param value
    * 
    * @return Converted object
    */
   public boolean translate(Object value) {
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
         return (sValue.equalsIgnoreCase(fTrue.getName()))||
                (sValue.equalsIgnoreCase(fTrue.getValue()))||
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
   public void setValueQuietly(Object value) {
      fValue = translate(value);
   }

   @Override
   public void setPersistentValue(String value) {
      fValue = translate(value);
   }
   
   /**
    * Set variable value as Boolean<br>
    * Listeners are informed if the variable changes
    * 
    * @param value Value to set
    * 
    * @return True if variable actually changed value and listeners notified
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
   
   @Override
   public String getValueAsString() {
      return getValueAsBoolean()?fTrue.getName():fFalse.getName();
   }
   
   @Override
   public boolean getValueAsBoolean() {
      return isEnabled()?fValue:fDefaultValue;
   }

   @Override
   public long getValueAsLong() {
      return getValueAsBoolean()?1:0;
   }
   
   @Override
   public String getSubstitutionValue() {
      return getValueAsBoolean()?fTrue.getValue():fFalse.getValue();
   }

   @Override
   public String getPersistentValue() {
      return Boolean.toString(fValue);
   }

   @Override
   public boolean getRawValueAsBoolean() {
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
   public void setDisabledValue(boolean disabledValue) {
      this.fDisabledValue = disabledValue;
   }

   /**
    * Get value used when disabled
    * 
    * @return
    */
   public boolean getDisabledValue() {
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
   
   /*
    * Special operations
    */
   /**
    * @return the choices
    */
   public void getChoices() {
   }

   /**
    * Get the name/value representing the TRUE value
    * 
    * @return name/value for TRUE value
    */
   public ChoiceData getTrueValue() {
      return fTrue;
   }

   /**
    * Set the name/value representing the TRUE value
    * 
    * @param trueValue name/value for TRUE value
    */
   public void setTrueValue(ChoiceData trueValue) {
      this.fTrue = trueValue;
   }

   /**
    * Get the name/value representing the FALSE value
    * 
    * @return the name/value for FALSE value
    */
   public ChoiceData getFalseValue() {
      return fFalse;
   }

   /**
    * Set the name/value representing the FALSE value
    * 
    * @param name/value for FALSE value
    */
   public void setFalseValue(ChoiceData falseValue) {
      this.fFalse = falseValue;
   }

   @Override
   public ChoiceData[] getData() {
      ChoiceData[] choices = {fFalse, fTrue};
      return choices;
   }

}
