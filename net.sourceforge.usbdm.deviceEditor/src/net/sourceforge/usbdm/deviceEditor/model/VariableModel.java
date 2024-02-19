package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.CellEditorProvider;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Model for a variable maintained by a provider
 */
public abstract class VariableModel extends EditableModel implements IModelChangeListener, CellEditorProvider {

   // Variable being modelled
   protected Variable fVariable;
   
   // Signal associated with the variable modelled by this model
   private Signal fAssociatedSignal;
   
   /**
    * Constructor - Create model from variable
    * 
    * @param parent     Parent model
    * @param variable   Variable being modelled
    * 
    * @note Added as child of parent if not null
    */
   public VariableModel(BaseModel parent, Variable variable) {
      super(parent, "");
      fVariable = variable;
      fVariable.addListener(this);
   }

   @Override
   /**
    * Get name with modified indication (* as suffix)
    * 
    * @return Name
    */
   public String getName() {
      String name = super.getName();
      if ((name == null) || name.isBlank()) {
         name = fVariable.getName();
      }
      boolean modified = !fVariable.isDerived() && !fVariable.isDefault();
      return name + (modified?" *":"");
   }

   /**
    * @return the Variable associated with this model
    */
   public Variable getVariable() {
      return fVariable;
   }

   @Override
   public Object getEditValue() {
      return fVariable.getEditValueAsString();
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
   
   @Override
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
   protected Status getPropagatedStatus() {
      Status status = getStatus();
      if (status == null) {
         return null;
      }
      if (status.getSeverity().greaterThan(fVariable.getErrorPropagate())) {
       if (fVariable.isLogging()) {
          System.err.println(getName()+".getPropagatedStatus()" + status);
       }
         return status;
      }
      return null;
   }
   
   @Override
   Status getStatus() {
      Status rv =  super.getStatus();
      if ((rv == null) || rv.lessThan(Severity.INFO)) {
         rv = fVariable.getStatus();
      }
      return rv;
   }

   @Override
   public VariableModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException  {
      VariableModel model = (VariableModel) super.clone(parentModel, provider, index);
      // Clone the referenced variable
      model.fVariable = fVariable.clone(provider, index);
      model.fVariable.addListener(model);
      return model;
   }

   @Override
   public boolean isHidden() {
      return super.isHidden() || ((fVariable != null) && fVariable.isHidden());
   }
   
   @Override
   public void modelElementChanged(ObservableModelInterface model, int properties) {
      if (fVariable.isLogging()) {
         System.err.println("Logging: "+ this + ".modelElementChanged()");
      }
      if ((properties & PROPERTY_VALUE) != 0) {
         StructuredViewer viewer = getViewer();
         if (viewer != null) {
            viewer.update(this, new String[] {"Value"} );
         }
         updateAncestors();
      }
      if ((properties & PROPERTY_STATUS) != 0) {
         updateAncestors();
      }
      if ((properties & PROPERTY_MAPPING) != 0) {
      }
      if ((properties & PROPERTY_STRUCTURE) != 0) {
         StructuredViewer viewer = getViewer();
         if (viewer != null) {
//          if (fLogging) {
//          System.err.println("VariableModel: vm = "+this+", hidden = " + isHidden());
//       }
            viewer.update(this.getParent(), new String[] {"Structure"});
         }
      }
      if ((properties & PROPERTY_HIDDEN) != 0) {
         StructuredViewer viewer = getViewer();
         if (viewer != null) {
//          if (fLogging) {
//          System.err.println("VariableModel: vm = "+this+", hidden = " + isHidden());
//       }
            viewer.update(this.getParent(), new String[] {"Hidden"});
         }
      }
   }

   public Signal getAssociatedSignal() {
      
      // Check for signal associated with variable
      if (fAssociatedSignal == null) {
         Signal associatedSignal = fVariable.getAssociatedSignal();
         if (associatedSignal == null) {
            return null;
         }
         fAssociatedSignal = associatedSignal;
         fAssociatedSignal.addListener(this);
      }
      return fAssociatedSignal;
   }
   
 }
