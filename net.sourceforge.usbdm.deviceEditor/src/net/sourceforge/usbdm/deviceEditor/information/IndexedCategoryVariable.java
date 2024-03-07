package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IndexedCategoryModel;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class IndexedCategoryVariable extends StringVariable {

   public IndexedCategoryVariable(VariableProvider provider, String name, String key) {
      super(provider, name, key);
   }

   @Override
   protected IndexedCategoryModel privateCreateModel(BaseModel parent) {
      return new IndexedCategoryModel(parent, this);
   }

}
