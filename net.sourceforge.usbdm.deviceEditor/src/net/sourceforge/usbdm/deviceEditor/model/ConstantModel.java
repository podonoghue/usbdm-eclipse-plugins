package net.sourceforge.usbdm.deviceEditor.model;

public class ConstantModel extends BaseModel {

   private final String fValue;
   
   public ConstantModel(BaseModel parent, String name, String value, String description) {
      super(parent, name, description);
      fValue = value;
   }

   @Override
   public String getValueAsString() {
      return fValue;
   }

   
}
