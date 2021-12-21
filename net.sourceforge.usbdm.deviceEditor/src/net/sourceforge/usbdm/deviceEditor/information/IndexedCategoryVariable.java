package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IndexedCategoryModel;

public class IndexedCategoryVariable extends StringVariable {

   public IndexedCategoryVariable(String name, String key) {
      super(name, key);
   }

   @Override
   protected IndexedCategoryModel privateCreateModel(BaseModel parent) {
      return new IndexedCategoryModel(parent, this);
   }

}
