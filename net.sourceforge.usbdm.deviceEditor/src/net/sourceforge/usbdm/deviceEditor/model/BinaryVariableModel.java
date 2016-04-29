package net.sourceforge.usbdm.deviceEditor.model;

public class BinaryVariableModel extends VariableModel {

   /**
    * Class to hold the Name/Value pair
    */
   static class Pair {
      final String name;
      final String value;
      
      Pair(String name, String value) {
         this.name  = name;
         this.value = value;
      }
   }

   Pair value0 = new Pair("false", "false");
   Pair value1 = new Pair("true",  "true");
   
   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param provider      Provider that owns the variable
    * @param key           Key used to access the variable
    * @param description   Description for the display
    */
   public BinaryVariableModel(BaseModel parent, IModelEntryProvider provider, String key, String description) {
      super(parent, provider, key, description);
   }

   /**
    * Set the value for 1st option
    * 
    * @param n String to display
    * @param v Value to return if this option is selected
    */
   public void setValue0(String n, String v) {
      value0 = new Pair(n, v);
   }
   
   /**
    * Set the value for 2nd option
    * 
    * @param n String to display
    * @param v Value to return if this option is selected
    */
   public void setValue1(String n, String v) {
      value1 = new Pair(n, v);
   }
   
   @Override
   public String getValueAsString() {
      String value = super.getValueAsString();
      if (value1.value.equalsIgnoreCase(value)) {
         return value1.name;
      }
      return value0.name;
   }

   /**
    * Get value as boolean
    * 
    * @return
    */
   public Boolean getBooleanValue() {
      return value1.value.equalsIgnoreCase(super.getValueAsString());
   }

   /**
    * Set value as boolean
    * 
    * @param value
    */
   public void setBooleanValue(Boolean value) {
      if (value) {
         setValueAsString(value1.value);
      }
      else {
         setValueAsString(value0.value);
      }
   }

}
