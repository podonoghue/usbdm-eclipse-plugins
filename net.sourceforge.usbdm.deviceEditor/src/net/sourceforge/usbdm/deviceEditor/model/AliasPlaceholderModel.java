package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class AliasPlaceholderModel extends BaseModel {

   private String  fKey = null;
   private boolean fIsOptional = false;
   private boolean fIsLocked = false;
   
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

   /**
    * Allows the alias to be locked or editable
    * 
    * @param isLocked
    */
   public void setLocked(boolean isLocked) {
      fIsLocked = isLocked;
   }

   /**
    * Indicates if the alias is to be locked or editable
    * 
    * @param isLocked
    */
   public boolean isLocked() {
      return fIsLocked;
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

   @Override
   public String toString() {
      return getClass().getSimpleName()+"("+fName+", k="+getKey()+", d="+getDescription()+")";
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      return super.clone();
   }

}
