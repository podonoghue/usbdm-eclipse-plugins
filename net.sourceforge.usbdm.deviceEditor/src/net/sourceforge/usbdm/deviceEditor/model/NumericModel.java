package net.sourceforge.usbdm.deviceEditor.model;

public abstract class NumericModel extends BaseModel {

   public NumericModel(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }

   abstract void setValue(String value);
}
