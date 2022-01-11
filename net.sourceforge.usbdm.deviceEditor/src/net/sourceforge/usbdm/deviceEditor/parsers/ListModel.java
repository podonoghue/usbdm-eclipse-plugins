package net.sourceforge.usbdm.deviceEditor.parsers;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class ListModel extends BaseModel {

   public ListModel(BaseModel parent, String name) {
      super(parent, name);
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

   @Override
   public ListModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      return (ListModel) super.clone(parentModel, provider, index);
   }

}
