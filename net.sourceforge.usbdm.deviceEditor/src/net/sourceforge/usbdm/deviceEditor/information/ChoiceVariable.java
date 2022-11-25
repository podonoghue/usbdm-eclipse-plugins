package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ChoiceVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;

public class ChoiceVariable extends VariableWithChoices {

   @Override
   protected Object clone() throws CloneNotSupportedException {
      // TODO Auto-generated method stub
      return super.clone();
   }

   /** Name/choice pairs */
   private ChoiceData[] fData = null;
   
   /** Current value (user format i.e name) */
   private Integer fValue = null;
   
   /** Default value of variable */
   private Integer fDefaultValue = null;
   
   /** Default value (user format i.e name) */
   private Integer fDisabledValue = null;
   
   /**
    * Construct a variable representing a choice value
    * 
    * @param name Name to display to user.
    * @param key  Key for variable
    */
   public ChoiceVariable(String name, String key) {
      super(name, key);
   }
   
   @Override
   public String toString() {
      return String.format("Variable(key=%s, value=%s=>(%s))", getKey(), getSubstitutionValue(), getValueAsString());
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
   private int translate(Object value) {
      int index = -1;
      // Treat as one of the available values
      if (value instanceof String) {
         String sValue = (String)value;
         if (sValue.equalsIgnoreCase("Reserved") || sValue.equalsIgnoreCase("Default")) {
            // Quietly translate reserved values
            index = fDefaultValue;
         }
         else {
            index = getChoiceIndex(sValue);
         }
      }
      else if (value instanceof Long) {
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
      if ((index<0) || (index>=getChoiceData().length)) {
         throw new RuntimeException("Object "+ value + "(" + ((value!=null)?value.getClass():"null")+") Produces invalid index for ChoiceVariable " + getName());
      }
      return index;
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
   public boolean setValue(int value) {
      if ((fValue != null) && fValue.equals(value)) {
         return false;
      }
//      // XX Delete me
//      if (getName().contains("mcg_c1_frdiv[1]") || getName().contains("range0[1]")) {
//         System.err.println(getName()+"setValue(int="+value+"), cv=" + fValue);
//      }
      if (value<0) {
         System.err.println("Warning value is not valid "+this+", "+value);
         fValue = 0;
      }
      getChoices();
      fValue = value;
      notifyListeners();
      return true;
   }

   /**
    * Get current value or null if not yet set
    * 
    * @return
    */
   public Object getValue() {
      return fValue;
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
         if (fData[index].getValue().equalsIgnoreCase(value)) {
            res = index;
            break;
         }
      }
      if (res<0) {
         throw new RuntimeException("'"+intValue + "' is not compatible with ChoiceVariable " + getName());
      }
      return setValue(fData[res].getName());
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
         if (fData[index].getValue().equalsIgnoreCase(value)) {
            res = index;
            break;
         }
      }
      if (res<0) {
         throw new RuntimeException("'"+value + "' is not compatible with ChoiceVariable " + getName());
      }
      return setValue(fData[res].getName());
   }
   
   @Override
   public boolean setValue(Object value) {
//      // XX Delete me
//      if (getName().contains("mcg_c1_frdiv")) {
//         System.err.println("setValue(obj="+value+")");
//      }
      return setValue(translate(value));
   }
   
   @Override
   public void setValueQuietly(Object value) {
      fValue = translate(value);
   }

   @Override
   public void setPersistentValue(String value) throws Exception {
      int index = getChoiceIndex(value);
      if (index>=0) {
         fValue = index;
         return;
      }
      try {
         // Try as index number
         index = Integer.parseInt(value);
         if ((index>=0) && (index<fData.length)) {
            fValue = index;
            return;
         }
      } catch (NumberFormatException e) {
      }
      throw new Exception("Value '"+value+"' Not suitable for choice variable");
   }

   /**
    * Get the variable value interpreted as a Long
    * This will be the index of the currently selected value
    * 
    * @return
    */
   @Override
   public long getValueAsLong() {
      return fValue;
   }
   
   @Override
   public String getValueAsString() {
      String[] choices = getChoices();
      int index = isEnabled()?fValue:fDisabledValue;
      if (index>=choices.length) {
         index = 0;
      }
//       XX Delete me
//      if (getName().contains("mcg_c1_frdiv[1]")) {
//         System.err.println("getValueAsString() => +["+index+"], "+choices[index]);
//      }
      return choices[index];
   }
   
   @Override
   public String getSubstitutionValue() {
      return fData[fValue].getValue();
   }

   @Override
   public String getPersistentValue() {
      return getSubstitutionValue();
   }

   int getChoiceIndex(String value) {
      
      ChoiceData[] choiceData = getChoiceData();
      if (choiceData == null) {
         return -1;
      }
      
      int index = 0;
      for (ChoiceData choice:choiceData) {
         if (choice.getValue().equalsIgnoreCase(value)) {
            return index;
         }
         index++;
      }
      index = 0;
      for (ChoiceData choice:choiceData) {
         if (choice.getName().equalsIgnoreCase(value)) {
            return index;
         }
         index++;
      }
      String[] choices = getChoices();
      if (choices == null) {
         return -1;
      }
      index = 0;
      for (String choice:choices) {
         if (choice.equalsIgnoreCase(value)) {
            return index;
         }
         index++;
      }
      return -1;
   }
   
   @Override
   public String isValid(String value) {
      return (getChoiceIndex(value)>=0)?null:"Value is not valid";
   }

   @Override
   public void setDisabledValue(Object value) {
      setDisabledValue(translate(value));
   }

   /**
    * Set value used when disabled
    *
    * @param disabledValue
    */
   public void setDisabledValue(int disabledValue) {
      // XXX Delete me
//      if (getName().equals("cmp_cr0_filter_cnt")) {
//         System.err.println("Found "+getName());
//      }
      this.fDisabledValue = disabledValue;
   }

   /**
    * Get value used when disabled
    *
    * @return
    */
   public Integer getDisabledValue() {
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
      return !defaultHasChanged && (fValue == fDefaultValue);
   }
   
   /*
    * Special operations
    */
   /**
    * Hide or show a choice
    * 
    * @param value   Value to change
    * 
    * @param hide True to hide
    */
   public void hideByValue(String value, boolean hide) {
      for (ChoiceData choice:fData) {
         if (choice.getValue().equalsIgnoreCase(value)) {
            choice.setHidden(hide);
            clearChoices();
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
      clearChoices();
   }
   
   @Override
   public String[] getChoices() {
      String[] choices = super.getChoices();
      if (fValue == null) {
         // Value not set yet - set value
         fValue   = 0;
      }
      if (fDefaultValue == null) {
         // Default not set yet - set default
         fDefaultValue = 0;
      }
      if (fDisabledValue == null) {
         // Default not set yet - set disabled value
         fDisabledValue = 0;
      }
      return choices;
   }
   
   /**
    * @return the choices
    */
   @Override
   public ChoiceData[] getChoiceData() {
      return fData;
   }

   /**
    * @param entries The name/value entries to set
    */
   public void setData(ChoiceData[] entries) {
      this.fData = entries;
      clearChoices();
   }
   
   /**
    * Set choice data
    * 
    * @param entries The name/value entries to set
    */
   public void setChoiceData(ArrayList<ChoiceData> entries) {
      fData          = entries.toArray(new ChoiceData[entries.size()]);
      if (fDefaultValue != null) {
         defaultHasChanged = true;
      }
      fDefaultValue  = null;
      fValue         = null;
      clearChoices();
   }

   /**
    * Adds choice data to existing data
    * 
    * @param entries       Entries to add
    * @param defaultValue  New default value (null to leave unchanged)
    */
   public void addChoices(ArrayList<ChoiceData> entries, Integer defaultValue) {
      
      ArrayList<ChoiceData> consolidatedEntries = new ArrayList<ChoiceData>();
      for (ChoiceData item:fData) {
         consolidatedEntries.add(item);
      }
      consolidatedEntries.addAll(entries);

      if (defaultValue == null) {
         // Preserve default value
         defaultValue = fDefaultValue;
      }
      setChoiceData(consolidatedEntries);
      fDefaultValue = defaultValue;
      fValue        = defaultValue;
   }

   @Override
   public Variable clone(String name, ISubstitutionMap symbols) throws Exception {
      ChoiceVariable var = (ChoiceVariable) super.clone(name, symbols);
      this.fDefaultValue = var.fDefaultValue;
      return var;
   }

   @Override
   public boolean isLocked() {
      return super.isLocked() || (getChoices().length <= 1);
   }

   @Override
   public String getDefaultParameterValue() throws Exception {
      Integer index = fDefaultValue;
      if ((index==null) || (index<0)) {
         throw new Exception("Failed to get default");
      }
      return makeEnum(getChoiceData()[index].getEnumName());
   }

   @Override
   public Object getNativeValue() {
      return getValueAsString();
   }

   private String displayValue = null;
   
   public void setDisplayValue(String value) {
      displayValue = value;
   }
   
   public String getDisplayValue() {
      return displayValue;
   }

}