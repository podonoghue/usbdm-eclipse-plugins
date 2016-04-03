package net.sourceforge.usbdm.deviceEditor.model;

public class StringModel extends BaseModel {

   protected String fValue;
   
   public StringModel(BaseModel parent, String name, String description, String value) {
      super(parent, name, description);
      fValue = value;
   }

   @Override
   public String getValueAsString() {
      return fValue;
   }
   
   public void setValue(String value) {
      fValue = value;
      super.viewerUpdate(this, null); 
   }
}
