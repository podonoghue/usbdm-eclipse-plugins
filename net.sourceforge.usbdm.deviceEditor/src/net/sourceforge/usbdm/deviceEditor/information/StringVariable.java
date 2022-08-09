package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.StringVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class StringVariable extends Variable {

   /** Value in user format - created as needed */
   private StringBuilder fValue = null;
   
   /** Value in user format (cached) */
   protected String fCachedValue = "Not initialised";
   
   /** Default value of variable */
   protected String fDefault;
   
   /** Default value of variable */
   protected String fDisabledValue;
   
   /**
    * Constructor
    * 
    * @param name Name to display to user. If null then default value is derived from key.
    * @param key  Key for variable
    */
   public StringVariable(String name, String key) {
      super(name, key);
   }

   /**
    * Constructor
    * 
    * @param name    Name to display to user.
    * @param key     Key for variable
    * @param value   Value to use may be a StringBuilder otherwise value.toString() is used.
    */
   public StringVariable(String name, String key, Object value) {
      super(name, key);
      setValueQuietly(value);
      setDefault(value.toString());
   }

   /**
    * Adds additional text to the variable.
    * 
    * @param additionalText Text to append
    */
   public void append(Object additionalText) {
      if (fValue == null) {
         fValue = new StringBuilder(fCachedValue);
      }
      fValue.append(additionalText.toString());
      fCachedValue = null;
   }
   
   @Override
   public String getSubstitutionValue() {
      return getValueAsString();
   }

   @Override
   public String getValueAsString() {
      return isEnabled()?getPersistentValue():fDisabledValue;
   }

   @Override
   public boolean getValueAsBoolean() {
      return Boolean.valueOf(getPersistentValue());
   }

   @Override
   public Object getDefault() {
      return fDefault;
   }
   /**
    * Set value as String
    * 
    * @param value Value to set
    * 
    * @return True if variable actually changed value and listeners notified
    */
   public boolean setValue(String value) {
      if (getPersistentValue() == value) {
         return false;
      }
      super.debugPrint("StringVariable["+this+"].setValue("+value+"), old "+value);
      setValueQuietly(value);
      notifyListeners();
      return true;
   }
   
   @Override
   public boolean setValue(Object value) {
      return setValue(value.toString());
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new StringVariableModel(parent, this);
   }

   @Override
   public void setDefault(Object value) {
      defaultHasChanged = (fDefault != null) && (fDefault != value.toString());
      fDefault = value.toString();
   }

   /**
    * If value is a {@link StringBuilder} then it will be used as the internal representation otherwise value.ToString() will be used.
    */
   @Override
   public void setValueQuietly(Object value) {
      if (value instanceof StringBuilder) {
         fValue = (StringBuilder)value;
         fCachedValue = null;
      }
      else {
         fValue = null;
         fCachedValue = value.toString();
      }
   }

   @Override
   public String getPersistentValue() {
      if ((fCachedValue == null) && (fValue != null)) {
         fCachedValue = fValue.toString();
      }
      return fCachedValue;
   }

   @Override
   public void setPersistentValue(String value) {
      setValueQuietly(value);
   }

   @Override
   public boolean isDefault() {
      return !defaultHasChanged && getPersistentValue().equals(fDefault);
   }

   @Override
   public void setDisabledValue(Object value) {
      fDisabledValue = value.toString();
   }
   
}
