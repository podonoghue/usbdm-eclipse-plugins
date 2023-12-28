package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public abstract class StringModel extends BaseModel {

   protected String fValue;
   
   public StringModel(BaseModel parent, String name, String description, String value) {
      super(parent, name);
      setSimpleDescription(description);
      fValue = value;
   }

   @Override
   public String getValueAsString() {
      return fValue;
   }
   
   public void setValue(String value) {
      fValue = value;
      update(new String[] {"Value"});
   }

   @Override
   public StringModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      return (StringModel) super.clone(parentModel, provider, index);
   }

}
