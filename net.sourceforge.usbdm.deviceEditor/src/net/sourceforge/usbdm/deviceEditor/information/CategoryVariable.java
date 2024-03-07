package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryVariableModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class CategoryVariable extends StringVariable {

   public CategoryVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
      setValue("");
      setDefault("");
   }

   @Override
   protected CategoryVariableModel privateCreateModel(BaseModel parent) {
      return new CategoryVariableModel(parent, this);
   }

   @Override
   public boolean setValueQuietly(String value) {
      return super.setValueQuietly(value);
   }

}
