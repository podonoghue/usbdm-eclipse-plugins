package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class AliasPlaceholderModel extends BaseModel {

   private String  fKey = null;
   private boolean fIsOptional = false;
   private boolean fIsConstant = false;
   
   public AliasPlaceholderModel(BaseModel parent, String name, String description) {
      super(parent, name);
      setSimpleDescription(description);
   }

   @Override
   protected void removeMyListeners() {
   }
   
   public void setkey(String key) {
      fKey = key;
   }

   public void setOptional(boolean isOptional) {
      fIsOptional = isOptional;
   }

   public boolean isOptional() {
      return fIsOptional;
   }

   public void setConstant(boolean isConstant) {
      fIsConstant = isConstant;
   }

   public boolean isConstant() {
      return fIsConstant;
   }

   public void setKey(String fKey) {
      this.fKey = fKey;
   }

   public String getKey() {
      return fKey;
   }

   @Override
   public AliasPlaceholderModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      AliasPlaceholderModel model = (AliasPlaceholderModel) super.clone(parentModel, provider, index);
      model.fKey  = fKey.replaceAll("\\[\\d+\\]$", "["+index+"]");
      return model;
   }

}
