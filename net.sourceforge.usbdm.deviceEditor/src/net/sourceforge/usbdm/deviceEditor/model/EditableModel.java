package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Model that can be edited
 * 
 * @author podonoghue
 */
public abstract class EditableModel extends BaseModel {

   /** Used to mark model as a constant i.e. can't be changed by editor */
   boolean fIsLocked = false;
   
   /**
    * Constructor
    * 
    * @param parent        Parent model
    * @param name          Display name
    * 
    * @note Added as child of parent if not null
    */
   public EditableModel(BaseModel parent, String name) {
      super(parent, name);
   }
   
   @Override
   public boolean canEdit() {
      return !fIsLocked;
   }

   /**
    * Used to set the model as locked
    * 
    * @param isLocked true to make item locked (i.e. canEdit() => false)
    */
   public void setLocked(boolean isLocked) {
      fIsLocked = isLocked;
   }
   
   /**
    * Set value of underlying data<br>
    * The string value given is the GUI displayed value and may need to be converted to suitable type
    * 
    * @param value Value used by GUI/Model. Will be converted for data.
    */
   public abstract void setValueAsString(String value);

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public EditableModel clone(BaseModel parentModel, VariableProvider provider, int index) throws CloneNotSupportedException {
      return (EditableModel)super.clone(parentModel, provider, index);
   }

}
