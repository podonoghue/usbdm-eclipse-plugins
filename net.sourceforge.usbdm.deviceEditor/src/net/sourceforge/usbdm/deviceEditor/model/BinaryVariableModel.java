package net.sourceforge.usbdm.deviceEditor.model;

public class BinaryVariableModel extends VariableModel {

   /**
    * Class to hold the Name/Value pair
    */
   static class Pair {
      /** Name used by GUI/model */
      final String name;
      /** Value used by data */
      final String value;
      
      /**
       * 
       * @param name  Name used by GUI/model
       * @param value Value used by data
       */
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
      String name;
      if (value1.value.equalsIgnoreCase(value)) {
         name = value1.name;
      }
      else {
         name = value0.name;
      }
//      System.err.println("BinaryVariableModel.getValueAsString("+fName+", "+value+"->"+name+")");
      return name;
   }

   @Override
   public void setValueAsString(String name) {
      String value;
      if (value1.name.equalsIgnoreCase(name)) {
         value = value1.value;
      }
      else {
         value = value0.value;
      }
//      System.err.println("BinaryVariableModel.setValueAsString("+fName+", "+name+"->"+value+")");
      super.setValueAsString(value);
   }

   /**
    * Get value as boolean
    * 
    * @return
    */
   public Boolean getBooleanValue() {
      String value = super.getValueAsString();
      boolean bValue = value1.value.equalsIgnoreCase(value);
//      System.err.println("BinaryVariableModel.getBooleanValue("+fName+", "+value+"->"+bValue+")");
      return bValue;
   }

   /**
    * Set value as boolean
    * 
    * @param value
    */
   public void setBooleanValue(Boolean bValue) {
      String value;
      if (bValue) {
         value = value1.value;
      }
      else {
         value = value0.value;
      }
//      System.err.println("BinaryVariableModel.setBooleanValue("+fName+", "+bValue+"->"+value+")");
      super.setValueAsString(value);
      
      // Refresh children in case boolean category
      refreshChildren();
   }

   private void refreshChildren() {
      if (fChildren != null) {
         for (Object obj:fChildren) {
            if (obj instanceof VariableModel) {
               VariableModel child = (VariableModel) obj;
               viewerUpdate(child, null);
            }
         }
      }
   }
   
}
