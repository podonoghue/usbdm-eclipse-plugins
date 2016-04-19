package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.model.IModelEntryProvider.VariableInfo;

public class VariableModel extends NumericModel {

   private final IModelEntryProvider fProvider;
   private final String fKey;
   private final VariableInfo variableInfo;
   
   public VariableModel(BaseModel parent, IModelEntryProvider provider, String key, String description) {
      super(parent, key, description);
      fProvider      = provider;
      fKey           = key;
      variableInfo   = provider.getVariableInfo(key);
   }

   @Override
   public String getValueAsString() {
      return fProvider.getValueAsString(fKey);
   }

   @Override
   public boolean canEdit() {
      return true;
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public void setValueAsString(String value) {
      fProvider.setValue(fKey, value);
   }

   @Override
   public long min() {
      return variableInfo.min;
   }

   @Override
   public long max() {
      return variableInfo.max;
   }
   
}
