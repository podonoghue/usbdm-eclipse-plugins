package net.sourceforge.usbdm.deviceEditor.model;

public class BinaryModel extends VariableModel {

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
   
   public BinaryModel(BaseModel parent, IModelEntryProvider provider, String key, String description) {
      super(parent, provider, key, description);
   }

   public void setValue0(String n, String v) {
      value0 = new Pair(n, v);
   }
   
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

   public Boolean getBooleanValue() {
      return value1.value.equalsIgnoreCase(super.getValueAsString());
   }

   public void setBooleanValue(Boolean value) {
      if (value) {
         setValueAsString(value1.value);
      }
      else {
         setValueAsString(value0.value);
      }
   }

}
