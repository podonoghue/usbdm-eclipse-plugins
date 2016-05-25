package net.sourceforge.usbdm.deviceEditor.information;

public class BooleanVariable extends Variable {
   
   private Pair fTrue  = new Pair("true",  "true");
   private Pair fFalse = new Pair("false", "false");
   
   private boolean fValue;
   
   /**
    * Construct a variable representing a boolean value
    * 
    * @param name
    * @param value
    */
   public BooleanVariable(String name) {
      super(name);
      fValue = false;
   }

   /**
    * Set value as boolean
    * 
    * @param value Value as boolean
    * 
    * @return True if variable actually changed value
    */
   public boolean setValue(Boolean value) {
      if (fValue == (boolean)value) {
         return false;
      }
//      System.err.println(this+"setValue("+value+")");
      fValue = value;
      notifyListeners();
      return true;
   }
   
   @Override
   public boolean setValue(Object value) {
      if (value instanceof Boolean) {
         return setValue((Boolean)value);
      }
      if (value instanceof Integer) {
         return setValue((Integer)value != 0);
      }
      if (value instanceof Long) {
         return setValue((Long)value != 0);
      }
      if (value instanceof String) {
         String sValue = value.toString();
         return setValue(
               (sValue.equalsIgnoreCase(fTrue.name))||
               (sValue.equalsIgnoreCase("true"))||
               (sValue.equalsIgnoreCase("1")));
      }
      throw new RuntimeException("Object "+ value + "(" + value.getClass()+") Not compatible with BooleanVariable");
   }

   @Override
   public String getSubstitutionValue() {
      return fValue?fTrue.value:fFalse.value;
   }

   @Override
   public String getValueAsString() {
      return fValue?fTrue.name:fFalse.name;
   }

   @Override
   public boolean getValueAsBoolean() {
      return fValue;
   }

   @Override
   public long getValueAsLong() {
      return fValue?1:0;
   }

   /**
    * Get the name/value representing the TRUE value
    * 
    * @return name/value for TRUE value
    */
   public Pair getTrueValue() {
      return fTrue;
   }

   /**
    * Set the name/value representing the TRUE value
    * 
    * @param trueValue name/value for TRUE value
    */
   public void setTrueValue(Pair trueValue) {
      this.fTrue = trueValue;
   }

   /**
    * Get the name/value representing the FALSE value
    * 
    * @return the name/value for FALSE value
    */
   public Pair getFalseValue() {
      return fFalse;
   }

   /**
    * Set the name/value representing the FALSE value
    * 
    * @param name/value for FALSE value
    */
   public void setFalseValue(Pair falseValue) {
      this.fFalse = falseValue;
   }
   
}
