package net.sourceforge.usbdm.deviceEditor.model;

public class TitleModel extends BaseModel {

   public TitleModel(BaseModel parent, String name) {
      super(parent, name);
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public void addChild(BaseModel model) {
      throw new RuntimeException();
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      throw new RuntimeException();
   }

   @Override
   public String getValueAsString() {
      return getDescription();
   }

}
