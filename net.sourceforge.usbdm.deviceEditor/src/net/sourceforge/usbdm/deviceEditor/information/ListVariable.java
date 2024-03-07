package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ListVariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class ListVariable extends StringVariable {

   public ListVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }

   @Override
   protected ListVariableModel privateCreateModel(BaseModel parent) {
      return new ListVariableModel(parent, this);
      }

}
