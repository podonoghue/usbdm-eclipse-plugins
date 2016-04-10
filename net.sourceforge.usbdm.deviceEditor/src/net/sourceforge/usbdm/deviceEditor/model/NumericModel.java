package net.sourceforge.usbdm.deviceEditor.model;

public abstract class NumericModel extends EditableModel {

   public NumericModel(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }
   
   public abstract long min();
   public abstract long max();
}
