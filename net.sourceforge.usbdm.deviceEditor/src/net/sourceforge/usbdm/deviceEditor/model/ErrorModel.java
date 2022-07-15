package net.sourceforge.usbdm.deviceEditor.model;

public class ErrorModel extends BaseModel {

   public ErrorModel(BaseModel parent, String name, String message) {
      super(parent, name);
      this.setStatus(new Status(message));
   }

   @Override
   protected void removeMyListeners() {
   }

}
