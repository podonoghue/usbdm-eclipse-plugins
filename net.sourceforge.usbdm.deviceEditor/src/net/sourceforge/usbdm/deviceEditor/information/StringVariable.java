package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.StringVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

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
    * @param provider   Provider holding this variable
    * @param name       Name to display to user. (If null then default value is derived from key).
    * @param key        Key for variable.
    */
   public StringVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }

   /**
    * Constructor
    * 
    * @param provider   Provider holding this variable
    * @param name       Name to display to user. (If null then default value is derived from key).
    * @param key        Key for variable.
    * @param value      Value to use may be a StringBuilder otherwise value.toString() is used.
    */
   public StringVariable(VariableProvider provider, String name, String key, Object value) {
      super(provider, name, key);
      setValueQuietly(value);
      if (value != null) {
         setDefault(value.toString());
      }
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
      if (isLogging()) {
         System.err.println("S: setValueQuietly("+value+")");
      }
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
      if ((getReference() != null) && ((fValue == null)||(getReference().isNeverCached()))) {
         try {
            fValue = getReference().getValueAsString();
         } catch (Exception e) {
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
      return !defaultHasChanged && (fValue != null) && (fValue.equalsIgnoreCase(fDefault));
   }

   /**
    * {@inheritDoc}
    * 
    * @param disabledValue Value to set. May be null to have no effect.
    */
   @Override
   public void setDisabledValue(Object disabledValue) {
      if (disabledValue == null) {
         return;
      }
      fDisabledValue = disabledValue.toString();
   }

   @Override
   public Object getNativeValue() {
      return getValueAsString();
   }

   @Override
   public Object getValue() {
      return getValueAsString();
   }

   @Override
   public boolean isZero() {
      if (fValue == null) {
         return true;
      }
      // Try interpreting it as a number
      long value;
      try {
         value = Long.parseLong(fValue);
      } catch (NumberFormatException e) {
         return false;
      }
      return value == 0L;
   }

   @Override
   public String getUsageValue() {
      // TODO Auto-generated method stub
      return super.getUsageValue();
   }

}
