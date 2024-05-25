package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BooleanVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;
 
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
   public BooleanVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }
   
   /**
    * Construct a variable representing a boolean value
    * 
    * @param name Name to display to user.
    * @param key  Key for variable
    * @param value Initial value and default
    */
   public BooleanVariable(VariableProvider provider, String name, String key, Object value) {
      super(provider, name, key);
      setValue(value);
      setDefault(value);
   }
   
   @Override
   public String toString() {
      return String.format("BooleanVariable(Name=%s, value=%s))", getName(), fValue);
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new BooleanVariableModel(parent, this);
   }
   
   @Override
   public boolean setChoiceIndex(int index) {
      ChoiceData[] data = getChoiceData();
      if ((index<0) || (index>=data.length)) {
         return false;
      }
      boolean newValue = Boolean.parseBoolean(data[index].getValue());
      if (newValue == fValue) {
         return false;
      }
      fValue = newValue;
      return true;
   }

//   @Override
//   public int getChoiceIndex() {
//      ChoiceData[] data = getChoiceData();
//      for (ChoiceData choice:data) {
//         if (choice.getValue())
//      }
//      return fValue;
//   }
   
   @Override
   ChoiceData getCurrentChoice() {
      
      if (fValue == null) {
         return null;
      }
      return fValue?fTrue:fFalse;
   }

   @Override
   public ChoiceData getEffectiveChoice() {
      
      if (fValue == null) {
         return null;
      }
      return fValue?fTrue:fFalse;
   }

   /**
    * Convert object to suitable type for this variable
    * 
    * @param value "true"/"false"/true/false/0/1/choice name/choice value
    * 
    * @return Converted object
    */
   public Boolean translate(Object value) {
      if (value == null) {
         return null;
      }
      if (isLogging()) {
         System.err.println("Logging: "+this.toString()+".translate("+value+")");
      }
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
         String sValue = (String)value;
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
   
   /**
    * Get current value or null if not yet set
    * 
    * @return
    */
   @Override
   public Object getValue() {
      return getValueAsBoolean();
   }
   
   @Override
   public long getValueAsLong() {
      return getValueAsBoolean()?1:0;
   }
   
   @Override
   public String getValueAsString() {
      Boolean value = getValueAsBoolean();
      if (value == null) {
         return "(null)";
      }
      if (value) {
         return (fTrue==null)?"true":fTrue.getName();
      }
      return (fFalse==null)?"false":fFalse.getName();
   }
   
   @Override
   public String getSubstitutionValue() {
      Boolean value = getValueAsBoolean();
      if (value == null) {
         return "(null)";
      }
      if (value) {
         return (fTrue==null)?"true":fTrue.getValue();
      }
      return (fFalse==null)?"false":fFalse.getValue();
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
      if (fValue == value) {
         return false;
      }
      fValue = value;
      return true;
   }
   
   /**
    * {@inheritDoc}
    * 
    * @param value "true"/"false"/true/false/0/1/choice name/choice value
    */
   @Override
   public boolean setValueQuietly(Object value) {
      return setValueQuietly(translate(value));
   }
   
   /**
    * {@inheritDoc}
    * 
    * @param value "true"/"false"
    */
   @Override
   public void setPersistentValue(String value) {
      fValue = Boolean.parseBoolean(value);
   }
   
   /**
    * {@inheritDoc}
    * 
    * @return "true" or "false"
    */
   @Override
   public String getPersistentValue() {
      return Boolean.toString(fValue);
   }

   @Override
   public Boolean getValueAsBoolean() {
      if ((fValue == null) && (getReference() != null)) {
         try {
            fValue = getReference().getValueAsBoolean();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return isEnabled()?fValue:fDisabledValue;
   }

   @Override
   public String getEnumValue() {
      
      if (getValueAsBoolean()) {
         if (fTrue == null) {
            throw new RuntimeException("fTrue value not set");
         }
         return makeEnum(fTrue.getEnumName());
      }
      if (fFalse == null) {
         throw new RuntimeException("fFalse value not set");
      }
      return makeEnum(fFalse.getEnumName());

   }

   @Override
   public boolean getRawValueAsBoolean() {
      return  fValue;
   }

   /**
    * {@inheritDoc}
    * 
    * @param disabledValue Value to set. May be null. <br>
    * Checked as "true"/"false"/true/false/0/1/choice name/choice value
    */
   @Override
   public void setDisabledValue(Object disabledValue) {
      setDisabledValue(translate(disabledValue));
   }

   /**
    * Set value used when disabled
    *
    * @param disabledValue Value to set. May be null to have no effect.
    */
   public void setDisabledValue(Boolean disabledValue) {
      if (disabledValue==null) {
         return;
      }
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
   
   /**
    * {@inheritDoc}
    * 
    * @param value "true"/"false"/true/false/0/1/choice name/choice value
    */
   @Override
   public void setDefault(Object value) {
      Boolean v = translate(value);
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

   @Override
   public ChoiceData[] getHiddenChoiceData() {
      return null;
   }

   @Override
   public String getDefaultParameterValue() throws Exception {
      Object t = getDefault();
      if (isLogging()) {
         System.err.println("Logging: "+this+".getDefaultParameterValue() => "+t);
      }
      if (t==null) {
         return null;
      }
      if ((Boolean)t) {
         if (fTrue == null) {
            return "Default not defined (true)";
         }
         return makeEnum(fTrue.getEnumName());
      }
      else {
         if (fFalse == null) {
            return "Default not defined (false)";
         }
         return makeEnum(fFalse.getEnumName());
      }
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

   @Override
   public boolean isZero() {
      return fValue != true;
   }

}
