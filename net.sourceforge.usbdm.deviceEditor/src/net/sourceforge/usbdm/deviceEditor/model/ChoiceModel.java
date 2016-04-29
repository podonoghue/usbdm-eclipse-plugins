package net.sourceforge.usbdm.deviceEditor.model;

public class ChoiceModel extends SelectionModel {

   public ChoiceModel(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }

   @Override
   protected void removeMyListeners() {
   }

}
