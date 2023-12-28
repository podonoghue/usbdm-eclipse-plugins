package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class AliasPlaceholderModel extends BaseModel {

   private String  fKey = null;
   private boolean fIsOptional = false;
   private boolean fIsLocked = false;
   
   private VariableModel realModel = null;
   
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

   /**
    * @param  provider     Provider to look up variables
    * @param  aliasModel   Information for model to instantiate
    * 
    * @return New model created
    * 
    * @throws Exception
    */
   public BaseModel createModelFromAlias(VariableProvider provider) throws Exception {

      String  key        = getKey();
      boolean isOptional = isOptional();

      if (realModel != null) {
         throw new Exception("Trying to create multiple copies from Alias");
      }
      Variable variable = provider.safeGetVariable(provider.makeKey(key));
      if (variable == null) {
         if (!isOptional) {
            throw new Exception("Alias not found for '" + key + ", provider = '"+provider+"'");
         }
         return null;
      }
      String description = getSimpleDescription();
      if (!description.isEmpty()) {
         if ((variable.getDescription() != null) && !variable.getDescription().isEmpty()) {
            throw new Exception("Alias tries to change description for " + key);
         }
         variable.setDescription(description);
      }
      String toolTip = getRawToolTip();
      if ((toolTip != null) && !toolTip.isEmpty()) {
         if ((variable.getToolTip() != null) && !variable.getToolTip().isEmpty()) {
            throw new Exception("Alias tries to change toolTip for " + key + ", tooltip="+toolTip);
         }
         variable.setToolTip(toolTip);
      }
      realModel = variable.createModel(null);
      boolean isConstant = isLocked() || variable.isLocked();
      realModel.setLocked(isConstant);
      String displayName = getName();
      if (displayName != null) {
         realModel.setName(displayName);
      }
      return realModel;
   }

   public VariableModel getRealModel() {
      return realModel;
   }

}
