package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.Objects;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ChoiceVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;

public class ChoiceVariable extends VariableWithChoices {

   /** Name/choice pairs */
   private ChoiceData[] fData = null;
   
   /** Name/choice pairs */
   private ChoiceData[] fHiddenData = null;
   
   /** Current value - index into choices */
   private Integer fValue = null;
   
   /** Default value of variable - index into choices */
   private Integer fDefaultValue = null;
   
   /** Default value - index into choices */
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
      return String.format("ChoiceVariable(key=%s, value=%s=>(%s), en=%s)",
            getKey(), getSubstitutionValue(), getValueAsString(), isEnabled());
   }

   @Override
   protected VariableModel privateCreateModel(BaseModel parent) {
      return new ChoiceVariableModel(parent, this);
   }
   
   @Override
   public boolean setChoiceIndex(int index) {
      if ((index < 0) || (index>=getChoiceCount())) {
         return false;
      }
      if ((fValue != null)&&(fValue.equals(index))) {
         return false;
      }
      fValue = index;
      return true;
   }

   @Override
   public ChoiceData[] getChoiceData() {
      return fData;
   }

   @Override
   public ChoiceData[] getHiddenChoiceData() {
      return fHiddenData;
   }

   /**
    * Get the index of the currently selected choice (even if disabled)
    *
    * @return Index
    */
   Integer getChoiceIndex() {
      return fValue;
   }
   
   @Override
   public
   ChoiceData getCurrentChoice() {
      Integer index = getChoiceIndex();
      if ((index == null)||(index < 0)) {
         return null;
      }
      return getChoiceData()[index];
   }
   
   @Override
   public ChoiceData getEffectiveChoice() {
      Integer index = getChoiceIndex();
      if (!isEnabled()) {
         index = fDisabledValue;
      }
      if ((index == null)||(index < 0)) {
         return null;
      }
      return getChoiceData()[index];
   }
   
   /**
    * Get choice index by choice value
    * 
    * @param value   Value to look for
    * 
    * @return Index of choice found or -1 if none
    */
   int getChoiceIndexByValue(String value) {
      ChoiceData[] choices = getChoiceData();
      for (int index=0; index<choices.length; index++) {
         if (choices[index].getValue().equalsIgnoreCase(value)) {
            return index;
         }
      }
      return -1;
   }
   
   /**
    * Convert object to suitable type for this variable
    * 
    * @param value First treated as an index (String/Long/Int/Boolean) then directly as name
    * 
    * @return Converted object - Index into choices
    */
   protected Integer translate(Object value) {
      if (value == null) {
         return null;
      }
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
            try {
               // Try as string representing an index (integer)
               index = Integer.parseInt(sValue);
            } catch (NumberFormatException e) {
            }
            if ((index < 0)||(index>=getChoiceCount())) {
               // Treat as selection name
               index = getChoiceIndexByName(sValue);
               if (index < 0) {
                  // Treat as selection value
                  index = getChoiceIndexByValue(sValue);
               }
            }
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
      if ((index < 0) || (index>=getChoiceData().length)) {
         throw new RuntimeException("Object  "+ value + "(" + ((value!=null)?value.getClass():"null")+") Produces invalid index for ChoiceVariable " + getName());
      }
      return index;
   }

   /**
    * Sets variable value.
    * 
    * @param index The index of the choice to select
    * 
    * @return True if variable actually changed value
    */
   public boolean setValueQuietly(Integer index) {
      if (fLogging) {
         System.err.println(getName()+".setValue(C:"+index+")");
      }
      if (Objects.equals(fValue, index) ) {
         return false;
      }
      return setChoiceIndex(index);
   }

   /**
    * {@inheritDoc}<br>
    * @param value First treated as an index (String/Long/Int/Boolean) then as selection name
    */
   @Override
   public boolean setValueQuietly(Object value) {
      return setValueQuietly(translate(value));
   }
   
   /**
    * {@inheritDoc}<br>
    * This will be the index of the currently selected value if enabled or<br>
    * the disabled value index.
    * 
    * @return Current value as Integer
    */
   @Override
   public Object getValue() {
      return isEnabled()?fValue:fDisabledValue;
   }

   /**
    * {@inheritDoc}<br>
    * This will be the index of the currently selected value if enabled or<br>
    * the disabled value index.
    */
   @Override
   public long getValueAsLong() {
      return (int) getValue();
   }
   
   /**
    * {@inheritDoc}<br>
    * This will be the name of the currently selected value
    */
   @Override
   public String getValueAsString() {
      
      ChoiceData choice = getEffectiveChoice();
      if (choice == null) {
         return "--no selection--";
      }
      return choice.getName();
   }
   
   /**
    * Set value based on substitution value
    * 
    * @param fValue Substitution value => choice value
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
    * Set value based on substitution value
    * 
    * @param fValue Substitution value = choice value
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
      ChoiceData choice = getCurrentChoice();
      if (!isEnabled()) {
         choice = getChoiceData()[fDisabledValue];
      }
      else {
         checkChoiceIndex();
         choice = getCurrentChoice();
      }
      if (choice == null) {
         return "--no selection--";
      }
      return choice.getValue();
   }

   /**
    * {@inheritDoc}
    * 
    * @param value The value to restore - Treated as choice value, then choice index
    */
   @Override
   public void setPersistentValue(String value) throws Exception {
      int index = getChoiceIndexByValue(value);
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
   
   /**
    * {@inheritDoc}
    * 
    * @return Choice value
    */
   @Override
   public String getPersistentValue() {
      ChoiceData choice = getCurrentChoice();
      if (choice == null) {
         return null;
      }
      return choice.getValue();
   }

   @Override
   public String isValid(String value) {
      return (getChoiceIndexByName(value)>=0)?null:"Value is not valid";
   }

   /**
    * {@inheritDoc}
    * 
    * @param disabledValue Value to set. May be null to have no effect. <br>
    * First treated as an index (String/Long/Int/Boolean) then directly as name
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
   public void setDisabledValue(Integer disabledValue) {
      if (disabledValue == null) {
         return;
      }
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
   
   /**
    * {@inheritDoc}
    * 
    * @param value First treated as an index (String/Long/Int/Boolean) then directly as selection name (String)
    */
   @Override
   public void setDefault(Object value) {
      if ((fDefaultValue != null) && (fDefaultValue != value)) {
         throw new RuntimeException("Default already set for " + getName() + ", " + value.toString());
      }
      fDefaultValue = translate(value);
   }
   
   /**
    * {@inheritDoc}
    * 
    * @return The default value (Index into choices)
    */
   @Override
   public Object getDefault() {
      return fDefaultValue;
   }
   
   @Override
   public boolean isDefault() {
      return !defaultHasChanged &&
            Objects.equals(fValue, fDefaultValue);
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
   protected ChoiceData getdefaultChoice() {
      ChoiceData[] data = getChoiceData();
      if (data == null) {
         return null;
      }
      Integer index = fDefaultValue;
      if ((index==null) || (index < 0)) {
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
    * Get hidden data (choices)
    * 
    * @return
    */
   public ChoiceData[] getHiddenData() {
      return fHiddenData;
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
      if ((fDefaultValue == null) || (fDefaultValue < 0)) {
         throw new Exception("Failed to get default value for "+this);
      }
      return makeEnum(getChoiceData()[fDefaultValue].getEnumName());
   }

   @Override
   public Object getNativeValue() {
      return getValueAsString();
   }

//   private String displayValue = null;
//
//   public void setDisplayValue(String value) {
//      displayValue = value;
//   }
//
//   public String getDisplayValue() {
//      return displayValue;
//   }

}