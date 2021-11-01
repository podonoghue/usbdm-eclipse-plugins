package net.sourceforge.usbdm.deviceEditor.model;

public class ChoiceModel extends SelectionModel {

   public ChoiceModel(BaseModel parent, String name) {
      super(parent, name);
   }

   @Override
   protected void removeMyListeners() {
   }

}
