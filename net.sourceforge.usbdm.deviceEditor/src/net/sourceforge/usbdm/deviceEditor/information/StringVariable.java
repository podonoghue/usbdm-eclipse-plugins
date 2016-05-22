package net.sourceforge.usbdm.deviceEditor.information;

public class StringVariable extends Variable {

   String fValue = "Not assigned";
   
   public StringVariable(String name) {
      super(name);
   }

   @Override
   public String getSubstitutionValue() {
      return fValue;
   }

   @Override
   public String getValueAsString() {
      return fValue;
   }

   @Override
   public boolean setValue(Object value) {
      if (fValue.equalsIgnoreCase(value.toString())) {
         return false;
      }
      fValue = value.toString();
      return true;
   }

}
