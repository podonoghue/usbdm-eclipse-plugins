package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ChoiceVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;

public class ChoiceVariable extends Variable {

   /** Name/choice pairs */
   private ChoiceData[] fData = null;
   
   /** List of choices */
   private String[] fChoices = null;

   /** Current value (user format i.e name) */
   private String fValue = null;
   
   /** Default value of variable */
   private String fDefaultValue = null;
   
   /** Default value (user format i.e name) */
   private String fDisabledValue = null;
   
   /**
    * Construct a variable representing a chpice value
    * 
    * @param name Name to display to user.
    * @param key  Key for variable
    */
   public ChoiceVariable(String name, String key) {
      super(name, key);
   }
   
   @Override
   public String toString() {
      return String.format("Variable(Name=%s, value=%s (%s))", getName(), getSubstitutionValue(), getValueAsString());
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new ChoiceVariableModel(parent, this);
   }
   
   /**
    * Convert object to suitable type for this variable
    * 
    * @param value
    * 
    * @return Converted object
    */
   private String translate(Object value) {
      if (value instanceof String) {
         return (String)value;
      }
      // Treat as index into values
      int index = -1;
      if (value instanceof Long) {
         index = ((Long)value).intValue();
      }
      else if (value instanceof Integer) {
         index = (Integer)value;
      }
      else if (value instanceof Boolean) {
         index = (Boolean)value?1:0;
      }
      else {
         throw new RuntimeException("Object "+ value + "(" + ((value!=null)?value.getClass():"null")+") Not compatible with ChoiceVariable " + getName());
      }
      if ((index<0) || (index>=getChoices().length)) {
         throw new RuntimeException("Object "+ value + "(" + ((value!=null)?value.getClass():"null")+") Produces invalid index for ChoiceVariable " + getName());
      }
      return fData[index].name;
   }

   /**
    * Sets variable value.<br>
    * Listeners are informed if the variable changes.<br>
    * Special strings "Reserved" and "Default" are translated to the {@link #fDefaultValue} value
    * 
    * @param value The value to set as a String
    * 
    * @return True if variable actually changed value
    */
   public boolean setValue(String value) {
      if ((fValue != null) && fValue.equals(value)) {
         return false;
      }
      if (isValid(value) != null) {
         System.err.println("Warning value is not valid "+this+", "+value);
         value = getChoices()[0];
      }
      if (value.equalsIgnoreCase("Reserved") ||
          value.equalsIgnoreCase("Default")) {
         // Quietly remove reserved values
         value = fDefaultValue;
      }
      super.debugPrint("ChoiceVariable["+this+"].setValue("+value+"), old "+value);
      fValue = value;
      notifyListeners();
      return true;
   }
   
   /**
    * Set value based on substitution value rather than user value.
    * 
    * @param fValue Substitution value to search for. (Converted to string)
    * 
    * @return True if variable actually changed value
    */
   public boolean setSubstitutionValue(int intValue) {
      String value = Integer.toString(intValue);
      int res = -1;
      for (int index=0; index<fData.length; index++) {
         if (fData[index].value.equalsIgnoreCase(value)) {
            res = index;
            break;
         }
      }
      if (res<0) {
         throw new RuntimeException("'"+intValue + "' is not compatible with ChoiceVariable " + getName());
      }
      return setValue(fData[res].name);
   }
   
   /**
    * Set value based on substitution value rather than user value.
    * 
    * @param value Substitution value to search for
    * 
    * @return True if variable actually changed value
    */
   public boolean setSubstitutionValue(String value) {
      int res = -1;
      for (int index=0; index<fData.length; index++) {
         if (fData[index].value.equalsIgnoreCase(value)) {
            res = index;
            break;
         }
      }
      if (res<0) {
         throw new RuntimeException("'"+value + "' is not compatible with ChoiceVariable " + getName());
      }
      return setValue(fData[res].name);
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
      for (int index=0; index<fData.length; index++) {
         if (fData[index].value.equalsIgnoreCase(value)) {
            fValue = fData[index].name;
            return;
         }
      }
      try {
         // Try as index number
         int index = Integer.parseInt(value);
         if ((index>=0) && (index<fData.length)) {
            fValue = fData[index].name;
            return;
         }
      } catch (NumberFormatException e) {
      }
      // Try as selected value
      if (isValid(value) == null) {
         fValue = value;
         return;
      }
      // Use default
      fValue = fDefaultValue;
      return;
   }

   /**
    * Get the variable value interpreted as a Long
    * 
    * @return
    */
   @Override
   public long getValueAsLong() {
      return getIndex(getValueAsString());
   }
   
   @Override
   public String getValueAsString() {
      return isEnabled()?fValue:fDisabledValue;
   }
   
   /**
    * Get index of current value in choice entries
    * 
    * @return index or -1 if not found
    */
   private int getIndex(String name) {
      for (int index=0; index<fData.length; index++) {
         if (fData[index].name.equalsIgnoreCase(name)) {
            return index;
         }
      }
      return -1;
   }
   
   @Override
   public String getSubstitutionValue() {
      int index = getIndex(getValueAsString());
      if (index<0) {
         return "["+getValueAsString()+" not found]";
      }
      return fData[index].value;
   }


   @Override
   public String getPersistentValue() {
      return getSubstitutionValue();
   }

   @Override
   public String isValid(String value) {
      String[] choices = getChoices();
      if (choices == null) {
         return null;
      }
      for (String choice:choices) {
         if (choice.equalsIgnoreCase(value)) {
            return null;
         }
      }
      return "Value is not valid";
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
   public void setDisabledValue(String disabledValue) {
      this.fDisabledValue = disabledValue;
   }

   /**
    * Get value used when disabled
    * 
    * @return
    */
   public String getDisabledValue() {
      return fDisabledValue;
   }
   
   @Override
   public void setDefault(Object value) {
      if (fDefaultValue != null) {
         throw new RuntimeException("Default already set for " + getName() + ", " + value.toString());
      }
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
   public String[] getChoices() {
//      if (getName().startsWith("mcg_c7_oscsel")) {
//         System.err.println("mcg_c7_oscsel.getData()");
//      }
      if (fChoices == null) {
         fChoices = new String[fData.length];
         for (int index=0; index<fData.length; index++) {
            if (fData[index].isHidden()) {
               fChoices[index] = "Unavailable";
            }
            else {
               fChoices[index] = fData[index].name;
            }
         }
         if (fValue == null) {
            // Value not set yet - set default
            fValue   = fChoices[0];
         }
         if (fDefaultValue == null) {
            // Default not set yet - set default
            fDefaultValue = fValue;
         }
         if (fDisabledValue == null) {
            // Default not set yet - set default
            fDisabledValue = fValue;
         }
      }
      return fChoices;
   }

   /**
    * Hide or show a choice
    * 
    * @param value   Value to change
    * 
    * @param hide True to hide
    */
   public void hideByValue(String value, boolean hide) {
      for (ChoiceData choice:fData) {
         if (choice.value.equalsIgnoreCase(value)) {
            choice.setHidden(hide);
            fChoices = null;
         }
      }
   }
   
   /**
    * Hide or show a choice
    * 
    * @param index   Index of item to change
    * 
    * @param hide True to hide
    */
   public void hideByIndex(int index, boolean hide) {
      fData[index].setHidden(hide);
      fChoices = null;
   }
   
   /**
    * @return the choices
    */
   public ChoiceData[] getData() {
      return fData;
   }

   /**
    * @param entries The name/value entries to set
    */
   public void setData(ChoiceData[] entries) {
      this.fData = entries;
      fChoices = null;
   }
   
   /**
    * @param entries The name/value entries to set
    */
   public void setData(ArrayList<ChoiceData> entries) {
      fData = entries.toArray(new ChoiceData[entries.size()]);
      fDefaultValue = null;
      fValue = null;
      fChoices = null;
   }

   @Override
   public Variable clone(String name, ISubstitutionMap symbols) throws CloneNotSupportedException {
      ChoiceVariable var = (ChoiceVariable) super.clone(name, symbols);
      this.fDefaultValue = var.fDefaultValue;
      return var;
   }

}