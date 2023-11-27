package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BooleanVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
 
public class BooleanVariable extends VariableWithChoices {
   
   private ChoiceData fTrue  = null;
   private ChoiceData fFalse = null;
   
   /** Current value */
   private Boolean fValue = null;
   
   /** Default value of variable */
   private Boolean fDefaultValue = null;
   
   /** Disabled value of variable */
   private Boolean fDisabledValue = false;
   
   /**
    * Construct a variable representing a boolean value
    * 
    * @param name Name to display to user.
    * @param key  Key for variable
    */
   public BooleanVariable(String name, String key) {
      super(name, key);
   }
   
   /**
    * Construct a variable representing a boolean value
    * 
    * @param name Name to display to user.
    * @param key  Key for variable
    * @param value Initial value and default
    */
   public BooleanVariable(String name, String key, Object value) {
      super(name, key);
      setValue(value);
      setDefault(value);
   }
   
   @Override
   public String toString() {
      return String.format("BooleanVariable(Name=%s, value=%s (%s))", getName(), getSubstitutionValue(), getValueAsString());
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
         Boolean res = null;
         if (fTrue != null) {
            res = sValue.equalsIgnoreCase(fTrue.getName())||
                  sValue.equalsIgnoreCase(fTrue.getValue()) ||
                  sValue.equalsIgnoreCase("true") ||
                  sValue.equalsIgnoreCase("1");
            return res;
         }
         if (fFalse != null) {
            res =  sValue.equalsIgnoreCase(fFalse.getName()) ||
                   sValue.equalsIgnoreCase(fFalse.getValue()) ||
                   sValue.equalsIgnoreCase("false") ||
                   sValue.equalsIgnoreCase("0");
            return !res;
         }
         if (sValue.equalsIgnoreCase("true")|| sValue.equalsIgnoreCase("1")) {
            return true;
         }
         if (sValue.equalsIgnoreCase("false")|| sValue.equalsIgnoreCase("0")) {
            return false;
         }
         
      }
      throw new RuntimeException("Object "+ value + "(" + value.getClass()+") Not compatible with BooleanVariable");
   }
   
   @Override
   public void setIndex(int index) {
      setValue(index != 0);
   }
   
   /**
    * Get current value or null if not yet set
    * 
    * @return
    */
   public Object getValue() {
      return isEnabled()?fValue:fDisabledValue;
   }
   
   @Override
   public long getValueAsLong() {
      return getValueAsBoolean()?1:0;
   }
   
   @Override
   public String getValueAsString() {
      if (getValueAsBoolean()) {
         return (fTrue==null)?"true":fTrue.getName();
      }
      return (fFalse==null)?"false":fFalse.getName();
   }
   
   @Override
   public String getSubstitutionValue() {
      if (getValueAsBoolean()) {
         return (fTrue==null)?"true":fTrue.getValue();
      }
      return (fFalse==null)?"false":fFalse.getValue();
   }

   @Override
   public void notifyListeners() {
      if (fValue != null) {
         updateTargets(fValue?fTrue:fFalse);
      }
      super.notifyListeners();
   }
   
   /**
    * Set variable value as Boolean<br>
    * Listeners are informed if the variable changes
    * 
    * @param value Value to set
    * 
    * @return True if variable actually changed value and listeners notified
    */
   public boolean setValueQuietly(Boolean value) {
      if ((fValue!= null) && (fValue == (boolean)value)) {
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
   
   @Override
   public String getPersistentValue() {
      return Boolean.toString(fValue);
   }

   @Override
   public boolean getValueAsBoolean() {
      return isEnabled()?fValue:fDisabledValue;
   }

   @Override
   public String getEnumValue() {
      if (getValueAsBoolean()) {
         if (fTrue == null) {
            throw new RuntimeException("T not set");
         }
         return makeEnum(fTrue.getEnumName());
      }
      if (fFalse == null) {
         throw new RuntimeException("F not set");
      }
      return makeEnum(fFalse.getEnumName());

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
      boolean v = translate(value);
      defaultHasChanged = (fDefaultValue != null) && (fDefaultValue != v);
      fDefaultValue = v;
   }
   
   @Override
   public Object getDefault() {
      return fDefaultValue;
   }
   
   @Override
   public boolean isDefault() {
      return !defaultHasChanged && (fValue == fDefaultValue);
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
    * Set the name/value representing the FALSE value
    * 
    * @param name/value for FALSE value
    */
   public void setTrueValue(String falseValue) {
      this.fTrue = new ChoiceData("True", falseValue);
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

   /**
    * Set the name/value representing the FALSE value
    * 
    * @param name/value for FALSE value
    */
   public void setFalseValue(String falseValue) {
      this.fFalse = new ChoiceData("False", falseValue);
   }

   @Override
   public ChoiceData[] getChoiceData() {
      ArrayList<ChoiceData> data = new ArrayList<ChoiceData>();
      if (fFalse != null) {
         data.add(fFalse);
      }
      if (fTrue != null) {
         data.add(fTrue);
      }
      return data.toArray(new ChoiceData[data.size()]);
   }

   /**
    * {@inheritDoc}<br>
    * No hidden data for BooleanVariables
    */
   @Override
   public ChoiceData[] getHiddenChoiceData() {
      return null;
   }

   @Override
   public String getDefaultParameterValue() throws Exception {
      Object t = getDefault();
      if (t==null) {
         return null;
      }
      if ((fTrue == null) || (fFalse == null)) {
         return "Default not defined";
      }
      return makeEnum((Boolean)t?fTrue.getEnumName():fFalse.getEnumName());
   }

   @Override
   public boolean isLocked() {
      return (fTrue==null) || (fFalse == null) || super.isLocked();
   }

   @Override
   public Object getNativeValue() {
      return getValueAsBoolean();
   }

   @Override
   protected ChoiceData getdefaultChoice() {
      return fDefaultValue?fTrue:fFalse;
   }

}
