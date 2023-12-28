package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ListVariableModel;

public class ListVariable extends StringVariable {

   public ListVariable(String name, String key) {
      super(name, key);
   }

   @Override
   protected ListVariableModel privateCreateModel(BaseModel parent) {
      return new ListVariableModel(parent, this);
      }

}
