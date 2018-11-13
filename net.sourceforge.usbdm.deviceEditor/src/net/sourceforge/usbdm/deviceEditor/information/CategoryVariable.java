package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryVariableModel;

public class CategoryVariable extends StringVariable {

   public CategoryVariable(String name, String key) {
      super(name, key);
      setValue("");
      setDefault("");
   }

   @Override
   public CategoryVariableModel createModel(BaseModel parent) {
      return new CategoryVariableModel(parent, this);
   }
}
