package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.Variable;

/**
 * Model for a variable maintained by a provider
 */
public class VariableModel extends EditableModel implements IModelChangeListener {

   protected final IModelEntryProvider fProvider;
   protected final String              fKey;
   protected final Variable            fVariable;
   
   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param provider      Provider that owns the variable
    * @param key           Key used to access the variable
    * @param description   Description for the display
    */
   public VariableModel(BaseModel parent, IModelEntryProvider provider, String key, String description) {
      super(parent, key, description);
      fProvider      = provider;
      fKey           = key;
      fVariable      = fProvider.getVariable(key);
      fVariable.addListener(this);
   }

   @Override
   public String getValueAsString() {
      String value =  fProvider.getVariableValue(fKey);
//      System.err.println("VariableModel.getValueAsString("+fName+"=> "+value+")");
      return value;
   }

   @Override
   public void setValueAsString(String value) {
//      System.err.println("VariableModel.setValueAsString("+fName+", "+value+")");
      fProvider.setVariableValue(fKey, value);
      viewerUpdate(getParent(), null);
   }

   @Override
   protected void removeMyListeners() {
   }
   
   /**
    * Check if the string is valid as a value for this Variable
    * 
    * @param value Value as String
    * 
    * @return Description of error or null if valid
    */
   public String isValid(String value) {
      return null;
   }

   @Override
   Message getMessage() {
      Message msg = fVariable.getMessage();
      if (msg != null) {
         return msg;
      }
      return super.getMessage();
   }

   @Override
   public void modelElementChanged(ObservableModel observableModel) {
      viewerUpdate(this, null);
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
      viewerUpdate(this, null);
   }

   @Override
   public boolean isEnabled() {
      return (!(fParent instanceof BinaryVariableModel) || ((BinaryVariableModel)fParent).getBooleanValue());
   }

   @Override
   public boolean canEdit() {
      return super.canEdit() && isEnabled() && !fVariable.isLocked();
   }
   
}
