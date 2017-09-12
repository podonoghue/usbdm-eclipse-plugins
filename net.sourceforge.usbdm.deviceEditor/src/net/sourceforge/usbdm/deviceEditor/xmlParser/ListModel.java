package net.sourceforge.usbdm.deviceEditor.xmlParser;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

public class ListModel extends BaseModel {

   public ListModel(BaseModel parent, String name) {
      super(parent, name, "");
   }

   @Override
   protected void removeMyListeners() {
   }

   public void addChildrenToParent(BaseModel parent) {
      if (fChildren == null) {
         return;
      }
      for (Object child : fChildren) {
         parent.addChild((BaseModel) child);
      }
   }

}
