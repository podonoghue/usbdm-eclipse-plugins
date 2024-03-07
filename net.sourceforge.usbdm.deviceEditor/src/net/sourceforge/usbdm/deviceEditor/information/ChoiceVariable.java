package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ChoiceVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;

public class ChoiceVariable extends VariableWithChoices {

   /** Name/choice pairs */
   private ChoiceData[] fData = null;
   
   /** Name/choice pairs (hidden in GUI, used for code generation only) */
   private ChoiceData[] fHiddenData = null;
   
   /** Current value - index into choices */
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
   public ChoiceVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }
   
   @Override
   public String toString() {
      return String.format("ChoiceVariable(key=%s, value=%s=>(%s))", getKey(), getSubstitutionValue(), getValueAsString());
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
   protected int translate(Object value) {
      int index = -1;
      // Treat as one of the available values
      if (value instanceof String) {
         String sValue = (String)value;
         if (  ("Reserved".equalsIgnoreCase(sValue)) ||
               ("Default".equalsIgnoreCase(sValue))) {
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
         throw new RuntimeException("Object  "+ value + "(" + ((value!=null)?value.getClass():"null")+") Produces invalid index for ChoiceVariable " + getName());
      }
      return index;
   }

   @Override
   public void setIndex(int index) {
      setValue(index);
   }
   
//   public void notifyListenersX() {
//      ArrayList<ChoiceData> choices = getVisibleChoiceData();
//      if (fValue>=choices.size()) {
//         System.err.println("Opps");
//      }
//      else {
//         updateTargets(choices.get(fValue));
//      }
//      super.notifyListeners();
//   }
   
   /**
    * Sets variable value.
    * 
    * @param index The index of the choice to select
    * 
    * @return True if variable actually changed value
    */
   public boolean setValueQuietly(int index) {
      if (fLogging) {
         System.err.println(getName()+".setValue(C:"+index+")");
      }
      if ((fValue != null) && fValue.equals(index)) {
         return false;
      }
      ArrayList<ChoiceData> choices = getVisibleChoiceData();
      if ((index<0) || (index>choices.size())) {
         System.err.println("setValue("+index+") - Illegal value for choice");
         index = 0;
      }
      fValue = index;
      return true;
   }

   /**
    * Get current value or null if not yet set<br>
    * This will be the index of the currently selected value
    * 
    * @return
    */
   @Override
   public Object getValue() {
      return isEnabled()?fValue:fDisabledValue;
   }

   /**
    * Get the variable value interpreted as a Long<br>
    * This will be the index of the currently selected value
    * 
    * @return
    */
   @Override
   public long getValueAsLong() {
      return (int) getValue();
   }
   
   @Override
   public String getValueAsString() {
      String[] choices = getVisibleChoiceNames();
      int index = (int) getValueAsLong();
//      if ((index<0) || (index>=choices.length)) {
//         System.err.println("getValueAsString() illegal index, "+getName()+" ind = "+index);
//         index = 0;
//      }
      if ((index<0) || (index>=choices.length)) {
         return "Illegal choice value";
      }
      return choices[index];
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
   public String getSubstitutionValue() {
      if (fValue == null) {
         return null;
      }
      if ((fData == null)||(fData.length==0)) {
         return null;
      }
      return fData[fValue].getValue();
   }

   @Override
   public boolean setValueQuietly(Object value) {
      return setValueQuietly(translate(value));
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
      throw new Exception("Value '"+value+"' Not suitable for choice variable "+getName());
   }
   
   @Override
   public String getPersistentValue() {
      return getSubstitutionValue();
   }

   @Override
   protected int getChoiceIndex(String value) {
      
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
      String[] choices = getVisibleChoiceNames();
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
      if ((fDefaultValue != null) && (fDefaultValue != value)) {
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
   
   @Override
   public String[] getVisibleChoiceNames() {
      String[] choices = super.getVisibleChoiceNames();
      if (fValue == null) {
         // Value not set yet - set value
         fValue   = 0;
      }
      if (fDefaultValue == null) {
         // Default value not set yet - set default
         fDefaultValue = 0;
      }
      if (fDisabledValue == null) {
         // Disabled value not set yet - set disabled value
         fDisabledValue = fDefaultValue;
      }
      return choices;
   }
   
   @Override
   public ChoiceData[] getChoiceData() {
      return fData;
   }

   @Override
   public ChoiceData[] getHiddenChoiceData() {
      return fHiddenData;
   }

   @Override
   protected ChoiceData getdefaultChoice() {
      ChoiceData[] data = getChoiceData();
      if (data == null) {
         return null;
      }
      Integer index = fDefaultValue;
      if ((index==null) || (index<0)) {
         return null;
      }
      return data[index];
   }
   
   /**
    * @param entries The name/value entries to set
    */
   public void setData(ChoiceData[] entries) {
      this.fData = entries;
      clearCachedChoiceInformation();
   }
   
   /**
    * Set choice data (both visible and hidden)
    * 
    * @param entries The name/value entries to set
    */
   public void setChoiceData(ArrayList<ChoiceData> entries, ArrayList<ChoiceData> hiddenEntries) {
      
      fData = entries.toArray(new ChoiceData[entries.size()]);
      if (fDefaultValue != null) {
         defaultHasChanged = true;
      }
      fDefaultValue  = null;
      fValue         = null;
      if ((hiddenEntries != null) && (hiddenEntries.size()>0)) {
         fHiddenData = hiddenEntries.toArray(new ChoiceData[hiddenEntries.size()]);;
      }
      clearCachedChoiceInformation();
   }

   /**
    * Set visible choice data
    * 
    * @param entries The name/value entries to set
    */
   public void setChoiceData(ArrayList<ChoiceData> entries) {
      setChoiceData(entries, null);
   }
   
   @Override
   public Variable clone(String name, ISubstitutionMap symbols) throws Exception {
      ChoiceVariable var = (ChoiceVariable) super.clone(name, symbols);
      this.fDefaultValue = var.fDefaultValue;
      return var;
   }

   @Override
   public boolean isLocked() {
      return super.isLocked() || (getVisibleChoiceNames().length <= 1);
   }

   @Override
   public String getDefaultParameterValue() throws Exception {
      Integer index = fDefaultValue;
      if ((index==null) || (index<0)) {
         throw new Exception("Failed to get default value for "+this);
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