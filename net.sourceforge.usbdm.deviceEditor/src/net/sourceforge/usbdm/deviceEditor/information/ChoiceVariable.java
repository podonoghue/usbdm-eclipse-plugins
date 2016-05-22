package net.sourceforge.usbdm.deviceEditor.information;

public class ChoiceVariable extends Variable {

   /** Name/choice pairs */
   private Pair[] fData = null;
   
   /** List of choices */
   String[] fChoices = null;

   /** Current value (user format) */
   String fValue = null;
   
   public ChoiceVariable(String name) {
      super(name);
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
   public boolean setValue(Object value) {
      if (value instanceof String) {
         return setValue((String)value);
      }
      if (value instanceof Long) {
         // Treat as index into values
         return setValue(fData[((Long)value).intValue()].name);
      }
      if (value instanceof Integer) {
         // Treat as index into values
         return setValue(fData[(Integer)value].name);
      }
      if (value instanceof Boolean) {
         // Treat as index into first two values
         return setValue(fData[(Boolean)value?1:0].name);
      }
      throw new RuntimeException("Object "+ value + "(" + ((value!=null)?value.getClass():"null")+") Not compatible with ChoiceVariable");
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
            fValue = fChoices[0];
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

   /**
    * Get index of current value in choice entries
    * 
    * @return index or -1 if not found
    */
   private int getIndex() {
      for (int index=0; index<fData.length; index++) {
         if (fData[index].name.equalsIgnoreCase(fValue)) {
            return index;
         }
      }
      return -1;
   }
   
   @Override
   public String getSubstitutionValue() {
      int index = getIndex();
      if (index<0) {
         return "["+fValue+" not found]";
      }
      return fData[index].value;
   }

   @Override
   public String getValueAsString() {
      return fValue;
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
   
}