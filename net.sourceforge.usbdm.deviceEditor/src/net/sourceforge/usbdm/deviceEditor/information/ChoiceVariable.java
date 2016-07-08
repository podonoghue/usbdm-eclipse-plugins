package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ChoiceVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class ChoiceVariable extends Variable {

   /** Name/choice pairs */
   private Pair[] fData = null;
   
   /** List of choices */
   String[] fChoices = null;

   /** Current value (user format i.e name) */
   String fValue = null;

   /** Default value of variable */
   private String fDefault = null;
   
   /**
    * Constructor
    * 
    * @param name Name to display to user.
    * @param key  Key for variable
    */
   public ChoiceVariable(String name, String key) {
      super(name, key);
   }

   /**
    * Sets variable value
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
         value = fChoices[0];
      }
      if (value.equalsIgnoreCase("Reserved") ||
          value.equalsIgnoreCase("Default")) {
         // Quietly remove reserved values
         value = fDefault;
      }
      super.debugPrint("ChoiceVariable["+this+"].setValue("+value+"), old "+value);
      fValue = value;
      notifyListeners();
      return true;
   }

   @Override
   public void setDefault(Object value) {
//      System.err.println("Setting default for " + getName() + " of " + value.toString());
      if (fDefault != null) {
         throw new RuntimeException("Default already set for " + getName() + ", " + value.toString());
      }
      fDefault = translate(value); 
   }
   
   /**
    * Convert object to required type
    * 
    * @param value
    * @return
    */
   public String translate(Object value) {
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
      if ((index<0) || (index>=fChoices.length)) {
         throw new RuntimeException("Object "+ value + "(" + ((value!=null)?value.getClass():"null")+") Produces invalid index for ChoiceVariable " + getName());
      }
      return fData[index].name;
   }

   /**
    * Set value based on Raw value i.e. Substitution value rather than user value
    */
   public void setRawValue(int pllCalcValue) {
      String value = Integer.toString(pllCalcValue);
      int res = -1;
      for (int index=0; index<fData.length; index++) {
         if (fData[index].value.equalsIgnoreCase(value)) {
            res = index;
            break;
         }
      }
      if (res<0) {
         throw new RuntimeException(pllCalcValue + " is not compatible with ChoiceVariable " + getName());
      }
      fValue = fData[res].name;
   }
   
   @Override
   public boolean setValue(Object value) {
      return setValue(translate(value));
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
   
   /**
    * @return the choices
    */
   public String[] getChoices() {
      if (fChoices == null) {
         fChoices = new String[fData.length];
         for (int index=0; index<fData.length; index++) {
            fChoices[index] = fData[index].name;
         }
         if (fValue == null) {
            // Value not set yet - set default
            fValue   = fChoices[0];
         }
         if (fDefault == null) {
            // Default not set yet - set default
            fDefault = fValue;
         }
      }
      return fChoices;
   }

   /**
    * @return the choices
    */
   public Pair[] getData() {
      return fData;
   }

   /**
    * @param entries The name/value entries to set
    */
   public void setData(Pair[] entries) {
      this.fData = entries;
   }

   @Override
   public String getValueAsString() {
      return isEnabled()?fValue:fDefault;
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
   public VariableModel createModel(BaseModel parent) {
      return new ChoiceVariableModel(parent, this);
   }


   @Override
   public void setValueQuietly(Object value) {
      fValue = translate(value);
   }

   @Override
   public String getPersistentValue() {
      return getSubstitutionValue();
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
      fValue = fDefault;
      return;
   }
}