package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.StringVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class StringVariable extends Variable {

   /** Value in user format */
   private String fValue = "Not assigned";
   
   /** Default value of variable */
   private String fDefault;
   
   /**
    * Constructor
    * 
    * @param name Name to display to user.
    * @param key  Key for variable
    */
   public StringVariable(String name, String key) {
      super(name, key);
   }

   @Override
   public String getSubstitutionValue() {
      return getValueAsString();
   }

   @Override
   public String getValueAsString() {
      return isEnabled()?fValue:fDefault;
   }

   @Override
   public boolean setValue(Object value) {
      if (fValue.equalsIgnoreCase(value.toString())) {
         return false;
      }
      fValue = value.toString();
      return true;
   }

   @Override
   public VariableModel createModel(BaseModel parent) {
      return new StringVariableModel(parent, this);
   }

   @Override
   public void setDefault(Object value) {
      fDefault = value.toString();
   }

   @Override
   public void setValueQuietly(Object value) {
      fValue = value.toString();
   }

   @Override
   public String getPersistentValue() {
      return fValue;
   }

   @Override
   public void setPersistentValue(String value) {
      fValue = value.toString();
   }
}
