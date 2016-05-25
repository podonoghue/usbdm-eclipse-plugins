package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;

/**
 * Model for a variable maintained by a provider
 */
public class VariableModel extends EditableModel implements IModelChangeListener {

   protected final String              fKey;
   protected final Variable            fVariable;
   
   /**
    * 
    * @param parent        Parent model
    * @param provider      Provider that owns the variable
    * @param key           Key used to access the variable
    * @param description   Description for the display
    */
   public VariableModel(BaseModel parent, Variable variable, String key) {
      super(parent, key, null);
      fKey           = key;
      fVariable      = variable;
      fVariable.addListener(this);
   }

   /**
    * @return the Variable associated with this model
    */
   public Variable getVariable() {
      return fVariable;
   }

   @Override
   public String getValueAsString() {
      return fVariable.getValueAsString();
   }

   @Override
   public void setValueAsString(String value) {
      fVariable.setValue(value);
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
   public void modelElementChanged(ObservableModel observableModel) {
      update();
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
      update();
   }

   @Override
   public boolean isEnabled() {
      return fVariable.isEnabled() && 
            (!(fParent instanceof BooleanVariableModel) || ((BooleanVariableModel)fParent).getValueAsBoolean());
   }

   @Override
   public boolean canEdit() {
      return super.canEdit() && isEnabled() && !fVariable.isLocked();
   }

   @Override
   public String getDescription() {
      String description = super.getDescription();
      if (description == null) {
         description = fVariable.getDescription();
      }
      return description;
   }

   @Override
   public String getToolTip() {
      return fVariable.getToolTip();
   }

   @Override
   Message getMessage() {
      Message rv =  super.getMessage();
      if ((rv != null) && rv.greaterThan(Severity.INFO)) {
         return rv;
      }
      return fVariable.getMessage();
   }
   
}
