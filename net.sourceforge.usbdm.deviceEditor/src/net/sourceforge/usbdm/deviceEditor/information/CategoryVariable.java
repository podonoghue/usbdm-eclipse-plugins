package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryVariableModel;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;

public class CategoryVariable extends StringVariable {

   public CategoryVariable(String name, String key) {
      super(name, key);
      setValue("");
      setDefault("");
   }

   @Override
   protected CategoryVariableModel privateCreateModel(BaseModel parent) {
      return new CategoryVariableModel(parent, this);
   }

   @Override
   public boolean update(Expression expression) {
      boolean oldHidden = isHidden();
      boolean changed = super.update(expression);
      changed = changed || (isHidden() != oldHidden);
      return changed;
   }

}
