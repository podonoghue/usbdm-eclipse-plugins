package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.StringVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class StringVariable extends Variable {

//   /** Value in user format - created as needed */
//   private StringBuilder fValue = null;
   
//   /** Value in user format (cached) */
//   protected String fCachedValue = "Not initialised";
//
   String fValue = "Not initialised";
   
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

//   /**
//    * Adds additional text to the variable.
//    *
//    * @param additionalText Text to append
//    */
//   public void append(Object additionalText) {
//      if (fValue == null) {
//         fValue = new StringBuilder(fCachedValue);
//      }
//      fValue.append(additionalText.toString());
//      fCachedValue = null;
//   }
   
   @Override
   public String getSubstitutionValue() {
      return getValueAsString();
   }

   @Override
   public String getValueAsString() {
      return isEnabled()?getPersistentValue():fDisabledValue;
   }

   @Override
   public Boolean getValueAsBoolean() {
      return Boolean.valueOf(getPersistentValue());
   }

   @Override
   public long getValueAsLong() {
      return Long.valueOf(getPersistentValue());
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
    * @return True if variable actually changed value
    */
   public boolean setValueQuietly(String value) {
      if (getPersistentValue() == value) {
         return false;
      }
      fValue = value;
//      fCachedValue = value;
      return true;
   }
   
   /**
    * If value is a {@link StringBuilder} then it will be used as the internal representation otherwise value.ToString() will be used.
    * 
    * @return True if variable actually changed value
    */
   @Override
   public boolean setValueQuietly(Object value) {
//      System.err.println(getName()+".setValueQuietly("+value+")");
      if (value == null) {
         return setValueQuietly((String)null);
      }
      if (value instanceof StringBuilder) {
         throw new RuntimeException("Opps");
//         fValue = (StringBuilder)value;
//         fCachedValue = null;
//         return true;
      }
      return setValueQuietly(value.toString());
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new StringVariableModel(parent, this);
   }

   @Override
   public void setDefault(Object value) {
      String val = null;
      if (value != null) {
         val = value.toString();
      }
      defaultHasChanged = (fDefault != null) && !(fDefault.equals(val));
      fDefault = val;
   }

   @Override
   public String getPersistentValue() {
      if ((getReference() != null) && (getReference().isNeverCached())) {
         try {
            fValue = getReference().getValueAsString();
         } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
      }
      return fValue;
   }

   @Override
   public void setPersistentValue(String value) {
      setValueQuietly(value);
   }

   @Override
   public boolean isDefault() {
      return !defaultHasChanged;
   }

   @Override
   public void setDisabledValue(Object value) {
      fDisabledValue = value.toString();
   }

   @Override
   public Object getNativeValue() {
      return getValueAsString();
   }

   @Override
   public Object getValue() {
      return getValueAsString();
   }

}
