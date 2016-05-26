package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.ChoiceVariableModel;

public class ChoiceVariable extends Variable {

   /** Name/choice pairs */
   private Pair[] fData = null;
   
   /** List of choices */
   String[] fChoices = null;

   /** Current value (user format) */
   String fValue = null;

   /** Default value of variable */
   private String fdefault = null;
   
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
      fValue = value;
      notifyListeners();
      return false;
   }

   @Override
   public void setDefault(Object value) {
      fdefault = translate(value); 
   }
   
   public String translate(Object value) {
      if (value instanceof String) {
         return (String)value;
      }
      if (value instanceof Long) {
         // Treat as index into values
         return fData[((Long)value).intValue()].name;
      }
      if (value instanceof Integer) {
         // Treat as index into values
         return fData[(Integer)value].name;
      }
      if (value instanceof Boolean) {
         // Treat as index into first two values
         return fData[(Boolean)value?1:0].name;
      }
      throw new RuntimeException("Object "+ value + "(" + ((value!=null)?value.getClass():"null")+") Not compatible with ChoiceVariable");
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
      return getIndex();
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
            fdefault = fValue;
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
      return isEnabled()?fValue:fdefault;
   }

   /**
    * Get index of current value in choice entries
    * 
    * @return index or -1 if not found
    */
   private int getIndex() {
      for (int index=0; index<fData.length; index++) {
         if (fData[index].name.equalsIgnoreCase(getValueAsString())) {
            return index;
         }
      }
      return -1;
   }
   
   @Override
   public String getSubstitutionValue() {
      int index = getIndex();
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

}