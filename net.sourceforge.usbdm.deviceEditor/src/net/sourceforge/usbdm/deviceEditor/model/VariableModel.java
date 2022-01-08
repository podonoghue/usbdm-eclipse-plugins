package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.CellEditorProvider;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Model for a variable maintained by a provider
 */
public abstract class VariableModel extends EditableModel implements IModelChangeListener, CellEditorProvider {

   protected Variable fVariable;
   
   /**
    * Constructor - Create model from variable
    * 
    * @param parent     Parent model
    * @param variable   Variable being modelled
    * 
    * @note Added as child of parent if not null
    */
   public VariableModel(BaseModel parent, Variable variable) {
      super(parent, variable.getName());
      fVariable = variable;
      fVariable.addListener(this);
   }

   @Override
   public String getName() {
      boolean modified = !fVariable.isDerived() && !fVariable.isDefault();
      return super.getName() + (modified?" *":"");
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      // TODO Auto-generated method stub
      return super.clone();
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
   
   public abstract CellEditor createCellEditor(Tree tree);
   
   /**
    * Check if the string is valid as a value for this Variable
    * 
    * @param value Value as String
    * 
    * @return Description of error or null if valid
    */
   public String isValid(String value) {
      return fVariable.isValid(value);
   }

   /**
    * Used to validate initial text entry in dialogues<br>
    * Allows entry of illegal strings while editing even though current result is invalid
    * 
    * @param character Character to validate
    * 
    * @return Error message or null if valid
    */
   public String isValidKey(char character) {
      return fVariable.isValidKey(character);
   }

   @Override
   public void modelElementChanged(ObservableModel observableModel) {
      updateAncestors();
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
   public String getSimpleDescription() {
      return fVariable.getDescription();
   }

   @Override
   public String getToolTip() {
      return fVariable.getDisplayToolTip();
   }

   @Override
   Status getStatus() {
      Status rv =  super.getStatus();
      if ((rv != null) && rv.greaterThan(Severity.INFO)) {
         return rv;
      }
      return fVariable.getStatus();
   }

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
      updateAncestors();
   }
   
   @Override
   public VariableModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException  {
      VariableModel model = (VariableModel) super.clone(parentModel, provider, index);
      // Clone the referenced variable
      model.fVariable = (Variable) fVariable.clone(provider, index);
      model.fVariable.addListener(model);
      return model;
   }
}
